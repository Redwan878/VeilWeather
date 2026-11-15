#!/usr/bin/env python3
"""
VeilWeather Backend Server
Minimal relay service handling only ciphertext with rate limiting and auto-expiring queues.
"""

import asyncio
import json
import os
import time
import hmac
import hashlib
import secrets
from datetime import datetime, timedelta
from typing import Dict, Set, Optional
from dataclasses import dataclass, field
from aiohttp import web
import aiohttp_cors

# Configuration
SECRET_KEY = os.environ.get('SECRET_KEY') or secrets.token_hex(32)
RATE_LIMIT_WINDOW = 60  # seconds
RATE_LIMIT_MAX = 30  # messages per window
TTL_MESSAGES = 3600  # 1 hour TTL for undelivered messages


@dataclass
class RateLimit:
    timestamps: list = field(default_factory=list)
    
    def add(self):
        now = time.time()
        self.timestamps.append(now)
        # Clean old entries
        self.timestamps = [t for t in self.timestamps if now - t < RATE_LIMIT_WINDOW]
    
    def is_limited(self) -> bool:
        return len(self.timestamps) >= RATE_LIMIT_MAX


@dataclass
class User:
    user_id: str
    token: str
    username: str
    password_hash: str = ''
    websocket: Optional[web.WebSocketResponse] = None
    last_seen: float = 0
    rate_limit: RateLimit = field(default_factory=RateLimit)
    pending_messages: list = field(default_factory=list)
    
    def mark_seen(self):
        self.last_seen = time.time()
    
    def is_online(self) -> bool:
        return time.time() - self.last_seen < 300  # 5 min timeout


class MessageStore:
    def __init__(self):
        self.messages: Dict[str, list] = {}  # chat_id -> list of messages
        self.expiry_check_interval = 300  # 5 minutes
    
    async def store_message(self, chat_id: str, message: dict):
        if chat_id not in self.messages:
            self.messages[chat_id] = []
        
        msg_with_expiry = {
            **message,
            'created_at': time.time(),
            'expiry': time.time() + TTL_MESSAGES
        }
        self.messages[chat_id].append(msg_with_expiry)
        
        # Clean expired messages
        await self._cleanup_expired()
    
    async def get_pending(self, user_id: str) -> list:
        pending = []
        for chat_id, msgs in self.messages.items():
            for msg in msgs:
                if msg.get('to') == user_id and not msg.get('read'):
                    pending.append(msg)
        return pending
    
    async def _cleanup_expired(self):
        now = time.time()
        for chat_id in list(self.messages.keys()):
            self.messages[chat_id] = [
                m for m in self.messages[chat_id]
                if m['expiry'] > now
            ]


class AuthManager:
    def __init__(self):
        self.users: Dict[str, User] = {}
        self.tokens: Dict[str, str] = {}  # token -> user_id
    
    async def register_user(self, username: str, password: str) -> tuple:
        """Register new user, return (user_id, token)"""
        user_id = secrets.token_hex(16)
        token = secrets.token_hex(32)
        
        # Hash password
        password_hash = hashlib.sha256(
            f"{username}:{password}:{SECRET_KEY}".encode()
        ).hexdigest()
        
        self.users[user_id] = User(
            user_id=user_id,
            token=token,
            username=username,
            password_hash=password_hash
        )
        self.tokens[token] = user_id
        
        return user_id, token
    
    async def authenticate(self, username: str, password: str) -> tuple:
        """Authenticate user, return (user_id, token) or (None, None)"""
        for user in self.users.values():
            password_hash = hashlib.sha256(
                f"{user.username}:{password}:{SECRET_KEY}".encode()
            ).hexdigest()
            
            if hmac.compare_digest(password_hash, user.password_hash):
                return user.user_id, user.token
        
        return None, None
    
    def get_user(self, token: str) -> Optional[User]:
        user_id = self.tokens.get(token)
        return self.users.get(user_id)


class PairingManager:
    def __init__(self):
        self.codes: Dict[str, str] = {}  # code -> user_id
        self.pending_pairs: Dict[str, str] = {}  # code -> partner_user_id
    
    async def create_pairing_code(self, user_id: str) -> str:
        code = secrets.token_hex(4)  # 8 char code
        self.codes[code] = user_id
        return code
    
    async def use_pairing_code(self, code: str, user_id: str) -> Optional[str]:
        """Use pairing code, return partner user_id or None if invalid"""
        if code not in self.codes:
            return None
        
        partner_id = self.codes[code]
        if partner_id == user_id:
            return None
        
        # Create bidirectional link
        self.pending_pairs[f"{user_id}:{partner_id}"] = code
        self.pending_pairs[f"{partner_id}:{user_id}"] = code
        
        return partner_id


class BackgroundTasks:
    def __init__(self, message_store: MessageStore, users: Dict[str, User]):
        self.message_store = message_store
        self.users = users
        self.running = True
    
    async def run(self):
        """Background task to clean up old data"""
        while self.running:
            await asyncio.sleep(60)
            await self.cleanup_sessions()
    
    async def cleanup_sessions(self):
        """Remove offline users and their data"""
        now = time.time()
        for user_id in list(self.users.keys()):
            user = self.users[user_id]
            if not user.is_online() and user.websocket is None:
                # Remove user
                del self.users[user_id]
                print(f"[Cleanup] Removed offline user: {user_id}")


class VeilServer:
    def __init__(self):
        self.auth = AuthManager()
        self.pairing = PairingManager()
        self.messages = MessageStore()
        self.users: Dict[str, User] = {}
        self.app = web.Application()
        self._setup_routes()
    
    def _setup_routes(self):
        app = self.app
        app.router.add_get('/healthz', self.handle_health)
        app.router.add_post('/auth/register', self.handle_register)
        app.router.add_post('/auth/login', self.handle_login)
        app.router.add_get('/ws', self.handle_ws)
        app.router.add_post('/message/send', self.handle_send)
        app.router.add_post('/pairing/create', self.handle_create_pairing)
        app.router.add_post('/pairing/used', self.handle_use_pairing)
        app.router.add_post('/notification/register', self.handle_register_notification)
        
        # CORS
        cors = aiohttp_cors.setup(app, defaults={
            "*": aiohttp_cors.ResourceOptions(
                allow_credentials=True,
                expose_headers="*",
                allow_headers="*",
            )
        })
        
        for route in list(app.router.routes()):
            cors.add(route)

    async def handle_health(self, request: web.Request) -> web.Response:
        return web.json_response({'status': 'ok'})
    
    async def handle_register(self, request: web.Request) -> web.Response:
        try:
            data = await request.json()
            username = data.get('username', '')
            password = data.get('password', '')
            
            if not username or not password:
                return web.json_response(
                    {'error': 'username and password required'},
                    status=400
                )
            
            user_id, token = await self.auth.register_user(username, password)
            
            # Create user record
            self.users[user_id] = User(
                user_id=user_id,
                token=token,
                username=username
            )
            
            return web.json_response({
                'user_id': user_id,
                'token': token,
                'username': username
            })
        
        except Exception as e:
            return web.json_response(
                {'error': str(e)},
                status=500
            )
    
    async def handle_login(self, request: web.Request) -> web.Response:
        try:
            data = await request.json()
            username = data.get('username', '')
            password = data.get('password', '')
            
            user_id, token = await self.auth.authenticate(username, password)
            
            if user_id is None:
                return web.json_response(
                    {'error': 'Invalid credentials'},
                    status=401
                )
            
            return web.json_response({
                'user_id': user_id,
                'token': token
            })
        
        except Exception as e:
            return web.json_response(
                {'error': str(e)},
                status=500
            )
    
    async def handle_ws(self, request: web.Request) -> web.Response:
        ws = web.WebSocketResponse()
        await ws.prepare(request)
        
        token = request.query.get('token', '')
        user = self.auth.get_user(token)
        
        if not user:
            await ws.close(code=1008, message=b"Unauthorized")
            return ws
        
        user.websocket = ws
        user.mark_seen()
        self.users[user.user_id] = user
        
        # Send welcome and pending messages
        pending = await self.messages.get_pending(user.user_id)
        await ws.send_json({
            'type': 'welcome',
            'user_id': user.user_id,
            'pending_count': len(pending)
        })
        
        # Send pending messages
        for msg in pending[:50]:  # Limit to 50 pending
            await ws.send_json({
                'type': 'pending_message',
                'data': msg
            })
        
        try:
            while True:
                msg = await ws.receive()
                
                if msg.type == web.WSMsgType.TEXT:
                    data = json.loads(msg.data)
                    await self.handle_client_message(user, data, ws)
                
                elif msg.type in (web.WSMsgType.CLOSED, web.WSMsgType.ERROR):
                    break
        
        finally:
            user.websocket = None
            user.mark_seen()
        
        return ws
    
    async def handle_client_message(self, user: User, data: dict, ws: web.WebSocketResponse):
        # Rate limiting
        if user.rate_limit.is_limited():
            await ws.send_json({'type': 'rate_limited'})
            return
        
        user.rate_limit.add()
        
        msg_type = data.get('type')
        
        if msg_type == 'send':
            chat_id = data.get('chat_id', '')
            content = data.get('content', '')
            
            # Store message
            message = {
                'id': secrets.token_hex(8),
                'chat_id': chat_id,
                'sender_id': user.user_id,
                'content': content,
                'timestamp': time.time(),
                'to': data.get('to'),
                'read': False
            }
            
            await self.messages.store_message(chat_id, message)
            
            # Forward to recipient
            recipient_id = data.get('to')
            if recipient_id and recipient_id in self.users:
                recipient = self.users[recipient_id]
                if recipient.websocket:
                    await recipient.websocket.send_json({
                        'type': 'message',
                        'data': message,
                        'disguised': True  # Show as weather notification
                    })
            
            await ws.send_json({'type': 'sent', 'message_id': message['id']})
        
        elif msg_type == 'ping':
            await ws.send_json({'type': 'pong'})
        
        elif msg_type == 'typing':
            recipient_id = data.get('to')
            if recipient_id and recipient_id in self.users:
                recipient = self.users[recipient_id]
                if recipient.websocket:
                    await recipient.websocket.send_json({
                        'type': 'typing',
                        'from': user.user_id
                    })
    
    async def handle_send(self, request: web.Request) -> web.Response:
        # FCM-style push endpoint
        token = request.headers.get('Authorization', '').replace('Bearer ', '')
        user = self.auth.get_user(token)
        
        if not user:
            return web.json_response({'error': 'Unauthorized'}, status=401)
        
        data = await request.json()
        
        # Route through WebSocket if recipient is online
        recipient_id = data.get('to')
        if recipient_id in self.users:
            recipient = self.users[recipient_id]
            if recipient.websocket:
                await recipient.websocket.send_json({
                    'type': 'message',
                    'data': data
                })
                return web.json_response({'status': 'delivered'})
        
        # Otherwise store for later delivery
        message = {
            'id': secrets.token_hex(8),
            'chat_id': data.get('chat_id', ''),
            'sender_id': user.user_id,
            'content': data.get('content', ''),
            'timestamp': time.time(),
            'to': recipient_id,
            'read': False
        }
        
        await self.messages.store_message(data.get('chat_id', ''), message)
        
        return web.json_response({'status': 'stored'})
    
    async def handle_create_pairing(self, request: web.Request) -> web.Response:
        token = request.headers.get('Authorization', '').replace('Bearer ', '')
        user = self.auth.get_user(token)
        
        if not user:
            return web.json_response({'error': 'Unauthorized'}, status=401)
        
        code = await self.pairing.create_pairing_code(user.user_id)
        
        return web.json_response({'pairing_code': code})
    
    async def handle_use_pairing(self, request: web.Request) -> web.Response:
        token = request.headers.get('Authorization', '').replace('Bearer ', '')
        user = self.auth.get_user(token)
        
        if not user:
            return web.json_response({'error': 'Unauthorized'}, status=401)
        
        data = await request.json()
        code = data.get('code', '')
        
        partner_id = await self.pairing.use_pairing_code(code, user.user_id)
        
        if not partner_id:
            return web.json_response({'error': 'Invalid or expired code'}, status=400)
        
        return web.json_response({'partner_id': partner_id})
    
    async def handle_register_notification(self, request: web.Request) -> web.Response:
        # Register FCM token for push notifications
        data = await request.json()
        return web.json_response({'status': 'registered'})
    
    async def start_background_tasks(self):
        bg = BackgroundTasks(self.messages, self.users)
        asyncio.create_task(bg.run())
    
    def run(self, host='0.0.0.0', port=None):
        port = port or int(os.environ.get('PORT', '8080'))
        print(f"Starting VeilWeather backend on {host}:{port}")
        web.run_app(self.app, host=host, port=port)


if __name__ == '__main__':
    server = VeilServer()
    asyncio.run(server.start_background_tasks())
    server.run()
