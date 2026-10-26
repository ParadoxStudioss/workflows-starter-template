#!/usr/bin/env python3
"""End-to-end dedicated server generation probe; Python standard library only."""
import os
import re
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
        self.s.settimeout(45)
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
    # Generation elevation now follows sampled ground instead of fixed Y=80.
    # Force-load all 3x3 station chunks so its large platforms can be built.
    connection.run("forceload add 48 -16 96 32")
    def height_at(x,z):
        response=connection.run(f"freight railway inspect {x} {z}")
        m=re.search(r"terrain-height:\s*(\d+)",response)
        if m is None:raise AssertionError("Cannot read planned track height: "+response)
        return int(m.group(1))
    trackY=height_at(72,8)
    print("New terrain-aware mainline altitude at station:",trackY,flush=True)
    ready=False
    for i in range(75):
        time.sleep(1)
        response=connection.run(f"execute if block 72 {trackY} 8 create:track")
        if "Test passed" in response:
            ready=True
            break
    if not ready:raise AssertionError("Terrain Create rail was not generated at Y="+str(trackY)+": "+response)
    print("OK: terrain-following Create rail exists at Y="+str(trackY),flush=True)
    trackY2=height_at(73,8)
    response=connection.run(f"execute if block 73 {trackY2} 8 create:track")
    if "Test passed" not in response:raise AssertionError("Missing adjacent track: "+response)
    print("OK: adjacent track is graded at Y="+str(trackY2),flush=True)
    ready=False
    for i in range(60):
        response=connection.run(f"execute if block 72 {trackY} 13 create:track_station")
        if "Test passed" in response:
            ready=True;break
        time.sleep(1)
    if not ready:raise AssertionError("Large Create Train Station was not generated: "+response)
    print("OK: terrain-level Create station installed",flush=True)
    response=connection.run(f"execute if block 72 {trackY-1} 11 minecraft:stone_bricks")
    if "Test passed" not in response:raise AssertionError("Station passenger platform missing: "+response)
    print("OK: larger passenger platform exists",flush=True)
    print("Checking actual Create railway graph across the x=63 / x=64 chunk seam...",flush=True)
    connection.run("forceload add 63 8")
    joined=False
    for i in range(35):
        time.sleep(1)
        result=connection.run("freight railway verify 63 8")
        if "CONNECTED" in result:
            joined=True;print("OK: actual Create rail graph connects across chunks.",flush=True);break
    if not joined:raise AssertionError("Create track graphs did not connect: "+result)
    # Four native Create Bezier paths are attached to the junction's
    # west and east turnouts. Confirm the actual TrackBlockEntities hold them.
    connection.run("forceload add 8 8")
    present=False
    for attempt in range(50):
        time.sleep(1)
        reply=connection.run("freight railway junction 8 8")
        match=re.search(r"Bezier connections:\s*(\d+)",reply)
        if match and int(match.group(1))>=4:
            present=True
            print("OK: four real Create Bezier turnouts at interchange",flush=True)
            break
    if not present:raise AssertionError("Create junction has no functional Bezier connections: "+reply)
    roof=connection.run(f"execute if block 72 {trackY+6} 11 minecraft:cut_copper")
    if "Test passed" not in roof:raise AssertionError("Large station canopy missing: "+roof)
    print("OK: covered large passenger platform",flush=True)
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
