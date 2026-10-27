#!/usr/bin/env python3
"""Live Minecraft+Create NPC assembly smoke test, stdlib only."""
import socket
import struct
import sys
import time
from pathlib import Path

HOST=("127.0.0.1",25576)
PASSWORD="local-npc-rail-smoke"
LOG=Path("npc-smoke.log")

def exact(sock, count):
    result=b""
    while len(result)<count:
        data=sock.recv(count-len(result))
        if not data:raise OSError("RCON disconnected")
        result+=data
    return result

class Client:
    def __init__(self):
        self.sock=socket.create_connection(HOST,timeout=10)
        self.sock.settimeout(90)
        self.send(11,3,PASSWORD)
        response=self.recv()
        if response[0]!=11:raise RuntimeError("RCON authentication failed")
    def send(self,id,kind,text):
        payload=struct.pack("<ii",id,kind)+text.encode()+b"\0\0"
        self.sock.sendall(struct.pack("<i",len(payload))+payload)
    def recv(self):
        count=struct.unpack("<i",exact(self.sock,4))[0]
        if count<10 or count>128000:raise RuntimeError("Invalid RCON message size")
        data=exact(self.sock,count)
        id,kind=struct.unpack("<ii",data[:8])
        return id,data[8:-2].decode(errors="replace")
    def run(self,text):
        self.send(12,2,text)
        for _ in range(8):
            id,reply=self.recv()
            if id==12:return reply
        raise RuntimeError("RCON response missing")

start=time.monotonic()
r=None
while time.monotonic()-start<240:
    try:
        if LOG.exists() and "Done (" in LOG.read_text(errors="replace"):
            r=Client()
            break
    except (OSError,RuntimeError):pass
    time.sleep(2)
if r is None:
    print("Server did not become ready",flush=True)
    sys.exit(1)
try:
    print("Minecraft and Create server online",flush=True)
    # All demo structures are at Y=210, safely above vanilla terrain.
    # Create the chunks first, preventing command-time worldgen surprises.
    print("Loading test yard chunks...",flush=True)
    print(r.run("forceload add 256 320 448 336"),flush=True)
    time.sleep(12)
    result=r.run("execute in minecraft:overworld positioned 256 210 320 run freight npc demo")
    print("Demo yard:",result,flush=True)
    if "Two independent freight yards generated" not in result:
        raise AssertionError("Could not construct Create test yard: "+result)
    ok=False
    last=""
    for i in range(145):
        time.sleep(2)
        last=r.run("freight npc status")
        if i%15==0:print("NPC fleet:",last,flush=True)
        if "2 real Create trains active" in last and "NPC Item Freight" in last and "NPC Fluid Tanker" in last:
            ok=True
            break
    if not ok:
        print(LOG.read_text(errors="replace")[-12000:],flush=True)
        raise AssertionError("Both Create locomotive blueprints failed to assemble: "+last)
    print("SUCCESS: both uploaded schematics assembled as REAL Create Train entities.",flush=True)
    # At least one should have navigation or a current station, not simply a
    # block structure. Track motion separately from registry creation.
    moved=False
    for i in range(80):
        time.sleep(2)
        s=r.run("freight npc status")
        if "moving" in s or "in transit" in s:
            moved=True
            break
    if not moved:
        print("NOTE: trains assembled, but no train was observed in transit in this window",flush=True)
    else:
        print("SUCCESS: at least one NPC train is actively navigating.",flush=True)
    log=LOG.read_text(errors="replace")
    if "Unable to build railway" in log or "Exception ticking" in log:
        print(log[-10000:],flush=True)
        raise AssertionError("Minecraft world reported a railway error")
finally:
    try:r.run("stop")
    except Exception:pass
    r.sock.close()
