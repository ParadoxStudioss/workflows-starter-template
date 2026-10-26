#!/usr/bin/env python3
"""End-to-end dedicated server generation probe; Python standard library only."""
import os
import socket
import struct
import subprocess
import sys
import time
from pathlib import Path

LOG=Path("smoke.log")
HOST=("127.0.0.1",25575)
SECRET="local-smoke-railway-only"

def recv_exact(sock,n):
    b=b""
    while len(b)<n:
        part=sock.recv(n-len(b))
        if not part:raise OSError("Unexpected end of RCON packet")
        b+=part
    return b

class Rcon:
    def __init__(self):
        self.s=socket.create_connection(HOST,timeout=5)
        self.s.settimeout(5)
        self.send(41,3,SECRET)
        result=self.receive()
        if result[0]!=41:
            raise ValueError("RCON authentication rejected: "+repr(result))
    def send(self,request,typ,text):
        payload=struct.pack("<ii",request,typ)+text.encode()+b"\x00\x00"
        self.s.sendall(struct.pack("<i",len(payload))+payload)
    def receive(self):
        size=struct.unpack("<i",recv_exact(self.s,4))[0]
        if size<10 or size>1024*128:raise ValueError("Invalid RCON packet size")
        data=recv_exact(self.s,size)
        ident,typ=struct.unpack("<ii",data[:8])
        return ident,typ,data[8:-2].decode(errors="replace")
    def run(self,text):
        self.send(42,2,text)
        for unused in range(5):
            ident,typ,reply=self.receive()
            if ident==42:return reply
        raise ValueError("Missing RCON command response")
    def close(self):self.s.close()

start=time.monotonic()
connection=None
while time.monotonic()-start<195:
    try:
        if LOG.exists() and "Done (" in LOG.read_text(errors="replace"):
            connection=Rcon()
            break
    except (OSError,ValueError):
        pass
    time.sleep(1)
if connection is None:
    print("Server did not reach ready state / accept RCON.",flush=True)
    if LOG.exists():print(LOG.read_text(errors="replace")[-3500:],flush=True)
    sys.exit(1)
print("Server and RCON are ready.",flush=True)

try:
    reply=connection.run("forceload add 72 8")
    print("Requested generation of station chunk:",reply,flush=True)
    # The scheduled chunk generation service may need several server ticks.
    ready=False
    for i in range(35):
        time.sleep(1)
        r=connection.run("execute if block 72 80 8 create:track")
        if "Test passed" in r or "test passed" in r.lower():
            ready=True
            break
    if not ready:raise AssertionError("Create track at planned station coordinate was not generated; response: "+r)
    checks=[
        ("Create track at adjacent x=73", "execute if block 73 80 8 create:track"),
        ("Real Create Train Station at z=13", "execute if block 72 80 13 create:track_station"),
        ("Stone platform at z=11", "execute if block 72 79 11 minecraft:stone_bricks")
    ]
    for description,cmd in checks:
        response=connection.run(cmd)
        if "Test passed" not in response and "test passed" not in response.lower():
            raise AssertionError(description+" failed: "+response)
        print("OK:",description,flush=True)
    print("Checking actual Create graph connectivity over the x=63 / x=64 chunk seam...",flush=True)
    connection.run("forceload add 63 8")
    joined=False
    for i in range(25):
        time.sleep(1)
        result=connection.run("freight railway verify 63 8")
        if "CONNECTED" in result:
            joined=True
            print("OK: Create graph is connected between neighboring chunks.",flush=True)
            break
    if not joined:raise AssertionError("Create graph did not join across chunk seam: "+result)
    text=LOG.read_text(errors="replace")
    if "Parsing error loading recipe freightlogistics:" in text:
        raise AssertionError("Invalid Freight & Logistics recipe was reported at runtime.")
    if "Generated railway segment" not in text:
        raise AssertionError("No generated railway segments were logged.")
    print("SUCCESS: new chunk contained Create tracks, a station and a platform.",flush=True)
finally:
    try: connection.run("stop")
    except OSError: pass
    connection.close()
