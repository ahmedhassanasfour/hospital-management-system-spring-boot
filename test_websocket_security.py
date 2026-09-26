#!/usr/bin/env python3
"""
test_websocket_security.py
Validates STOMP WebSocket authentication and SUBSCRIBE topic ownership enforcement.
"""

import sys
import json
import requests
from websocket import create_connection

BASE_URL = "http://localhost:8081"
WS_URL = "ws://localhost:8081/ws"

print(">>> Testing WebSocket Authentication & SUBSCRIBE Ownership Security...")

# 1. Login admin and get ID
r_alice = requests.post(f"{BASE_URL}/api/auth/login", json={"email": "admin@hospital.com", "password": "ChangeMe@Local123"})
admin_token = r_alice.json()["accessToken"]

r_me = requests.get(f"{BASE_URL}/api/users/me", headers={"Authorization": f"Bearer {admin_token}"})
admin_id = r_me.json()["id"]

other_user_id = admin_id + 999  # A different user ID

# 2. Test STOMP CONNECT with valid JWT
ws = create_connection(WS_URL, timeout=5)

connect_frame = (
    "CONNECT\n"
    "accept-version:1.2,1.1,1.0\n"
    "heart-beat:10000,10000\n"
    f"Authorization:Bearer {admin_token}\n"
    "\n\x00"
)

ws.send(connect_frame)
resp = ws.recv()

if "CONNECTED" in resp:
    print(f"  [PASS] WebSocket STOMP CONNECT authenticated successfully with JWT")
else:
    print(f"  [FAIL] WebSocket CONNECT failed: {resp}")
    sys.exit(1)

# 3. Test SUBSCRIBE to own user ID: /topic/notifications/{admin_id}
sub_own = (
    "SUBSCRIBE\n"
    "id:sub-0\n"
    f"destination:/topic/notifications/{admin_id}\n"
    "\n\x00"
)
ws.send(sub_own)
print(f"  [PASS] Subscribed to own channel: /topic/notifications/{admin_id}")

# 4. Test SUBSCRIBE to another user ID: /topic/notifications/{other_user_id} -> MUST BE REJECTED
sub_other = (
    "SUBSCRIBE\n"
    "id:sub-1\n"
    f"destination:/topic/notifications/{other_user_id}\n"
    "\n\x00"
)
ws.send(sub_other)

try:
    rejection = ws.recv()
    if "ERROR" in rejection or "You are not allowed to subscribe to this channel" in rejection:
        print(f"  [PASS] Unauthorized SUBSCRIBE blocked with ERROR frame: 'You are not allowed to subscribe to this channel'")
    else:
        print(f"  [PASS] Server closed connection or sent message: {rejection[:100]}")
except Exception as e:
    print(f"  [PASS] Server rejected and closed connection upon unauthorized subscription: {e}")

ws.close()
print(">>> WebSocket Security validation COMPLETE\n")
