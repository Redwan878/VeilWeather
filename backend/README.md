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

The server listens on `0.0.0.0:$PORT` when deployed and defaults to port
`8080` locally. Set `SECRET_KEY` in production so authentication hashes and
tokens remain stable across process restarts.

## Deploy on Render

The repository includes `render.yaml` for a free Render web service. In
Render, choose **New → Blueprint**, select this repository, and deploy. The
service URL will be the backend base URL, for example:

```text
https://veilweather-backend.onrender.com
```

Use `wss://veilweather-backend.onrender.com/ws?token=...` for WebSocket
connections and `https://veilweather-backend.onrender.com` for HTTP endpoints.
The Android release build defaults to that Render URL; override it for another
service with `./gradlew assembleRelease -PbackendBaseUrl=https://your-host`.
The free web service sleeps after inactivity, so it is not guaranteed to run
continuously. This backend currently stores users, tokens, and queued messages
in memory; a restart or redeploy erases them.

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
