# VeilWeather Backend Server

Minimal relay service for the VeilWeather encrypted messenger.

## Features

- WebSocket connections for real-time messaging
- Rate limiting (30 messages/minute per user)
- Auto-expiring message storage (1 hour TTL)
- Anonymous pairing system
- No phone numbers or emails stored

## Setup

```bash
cd backend
pip install -r requirements.txt
python server.py
```

## API Endpoints

### Authentication

```
POST /auth/register
Body: { "username": "...", "password": "..." }
Returns: { "user_id": "...", "token": "...", "username": "..." }

POST /auth/login
Body: { "username": "...", "password": "..." }
Returns: { "user_id": "...", "token": "..." }
```

### WebSocket

```
GET /ws?token=...
Real-time messaging channel
```

### Messaging

```
POST /message/send
Headers: Authorization: Bearer {token}
Body: { "chat_id": "...", "content": "...", "to": "..." }
Returns: { "status": "delivered" | "stored" }
```

### Pairing

```
POST /pairing/create
Headers: Authorization: Bearer {token}
Returns: { "pairing_code": "abcd1234" }

POST /pairing/used
Headers: Authorization: Bearer {token}
Body: { "code": "abcd1234" }
Returns: { "partner_id": "..." }
```

## Security

- All messages are encrypted client-side before reaching server
- Server stores only ciphertext
- No metadata (last seen, typing status, read receipts)
- Anonymous accounts (no PII)
- Rate limiting prevents abuse
