#!/usr/bin/env python3
"""Generate original, dependency-free 16x16 pixel textures and block drops for Freight & Logistics."""
from pathlib import Path
import json
import struct
import zlib

ROOT=Path("src/generated/resources")
PARTS=("freight_manifest","train_purchase_contract","industrial_cpu","memory_module","storage_drive","network_card","cooling_unit","railway_credit")
BLOCKS=("management_computer","server_rack","factory_controller","warehouse_controller","depot_controller","freight_terminal","display_relay","signal_controller","railway_shop","crossing_signal")
COLORS={
    "management_computer":("#222c38","#617681","#6ac9c2"),
    "server_rack":("#1c2833","#5a6e7d","#81bed1"),
    "factory_controller":("#423329","#98714c","#f1b25f"),
    "warehouse_controller":("#42372a","#886a49","#e3c58a"),
    "depot_controller":("#253a32","#57775f","#95d8a5"),
    "freight_terminal":("#4b352b","#ac7650","#f1bd75"),
    "display_relay":("#282747","#655e9c","#a8aaf3"),
    "signal_controller":("#432c2a","#9d5242","#efac76"),
    "railway_shop":("#473720","#aa9049","#ffe18e"),
    "crossing_signal":("#151d25","#5c6870","#d74e4b")
}

def rgb(s):
    h=s.lstrip("#")
    return tuple(bytes.fromhex(h))+(255,)

def canvas():
    return [[(0,0,0,0) for x in range(16)] for y in range(16)]

def rect(p,x,y,w,h,c):
    color=rgb(c)
    for iy in range(max(0,y),min(16,y+h)):
        for ix in range(max(0,x),min(16,x+w)):
            p[iy][ix]=color

def icon(name):
    p=canvas()
    r=lambda x,y,w,h,c:rect(p,x,y,w,h,c)
    if name=="industrial_cpu":
        r(3,3,10,10,"#26333d");r(4,4,8,8,"#98a4a8");r(5,5,6,6,"#26485c");r(7,7,2,2,"#d6b777")
        for i in range(4,12,2):
            r(i,1,1,2,"#cbb56c");r(i,13,1,2,"#cbb56c");r(1,i,2,1,"#cbb56c");r(13,i,2,1,"#cbb56c")
    elif name=="memory_module":
        r(1,5,14,7,"#243c31");r(2,6,12,5,"#51a078")
        for i in (3,6,9,12):
            r(i,7,2,3,"#172a38")
            r(i,11,1,3,"#d9bc73")
    elif name=="storage_drive":
        r(3,1,10,14,"#475967");r(4,2,8,12,"#c1c7be");r(5,3,6,4,"#6ca3b8");r(5,9,6,1,"#586b77");r(6,11,4,1,"#586b77")
    elif name=="network_card":
        r(2,2,12,12,"#1d534b");r(3,3,10,10,"#48a080");r(5,4,6,6,"#c1c4a2");r(6,5,4,4,"#263947")
        for i in range(3,13,3):r(i,13,1,2,"#e8ca76")
    elif name=="cooling_unit":
        r(2,2,12,12,"#2b3f49");r(3,3,10,10,"#9bc4c7");r(4,4,8,8,"#395b70")
        r(7,5,2,6,"#cee4dc");r(5,7,6,2,"#cee4dc");r(7,7,2,2,"#33465c")
    elif name=="freight_manifest":
        r(3,1,10,14,"#775840");r(4,2,8,12,"#f2e1bb");r(6,1,4,3,"#aa9162")
        for y in (6,8,10):r(5,y,6,1,"#878579")
        r(9,12,2,2,"#d15e56")
    elif name=="train_purchase_contract":
        r(2,2,12,12,"#806344");r(3,3,10,10,"#eddba6")
        r(5,6,6,3,"#435869");r(6,5,4,1,"#435869")
        r(5,10,6,1,"#98775b");r(10,11,2,2,"#d35b48")
    elif name=="railway_credit":
        r(5,1,6,14,"#a8732f");r(3,3,10,10,"#e8aa40");r(4,4,8,8,"#f4d275")
        r(6,5,5,1,"#916633");r(6,6,1,5,"#916633");r(6,10,5,1,"#916633")
    return p

def block(name):
    p=canvas()
    d,m,l=COLORS[name]
    r=lambda x,y,w,h,c:rect(p,x,y,w,h,c)
    r(0,0,16,16,d);r(1,1,14,14,m);r(2,2,12,12,d)
    if name=="server_rack":
        for y in (3,6,9,12):
            r(4,y,8,2,m);r(5,y,2,1,l);r(10,y,1,1,"#b4dd77")
    elif name=="crossing_signal":
        r(4,2,8,12,"#15151c");r(5,3,6,10,"#374650")
        r(6,4,4,3,l);r(6,9,4,3,l)
    elif name=="railway_shop":
        r(4,3,8,10,"#3d372d");r(5,4,6,3,l);r(5,9,6,1,l);r(5,11,6,1,l)
    else:
        r(3,4,10,7,"#182733");r(4,5,8,5,l);r(5,6,6,3,d);r(4,12,8,1,l)
    return p

def write_png(path,pixels):
    raw=b"".join(b"\x00"+b"".join(bytes(px) for px in row) for row in pixels)
    def ck(kind,payload):
        data=kind+payload
        return struct.pack(">I",len(payload))+data+struct.pack(">I",zlib.crc32(data)&0xffffffff)
    b=b"\x89PNG\r\n\x1a\n"
    b+=ck(b"IHDR",struct.pack(">IIBBBBB",16,16,8,6,0,0,0))
    b+=ck(b"IDAT",zlib.compress(raw,9))
    b+=ck(b"IEND",b"")
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(b)

def write_json(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(obj,indent=2)+"\n",encoding="utf-8")

for name in PARTS:
    write_png(ROOT/"assets/freightlogistics/textures/item"/(name+".png"),icon(name))
for name in BLOCKS:
    write_png(ROOT/"assets/freightlogistics/textures/block"/(name+".png"),block(name))
    if name!="management_computer":
        write_json(ROOT/"data/freightlogistics/loot_table/blocks"/(name+".json"),{
            "type":"minecraft:block",
            "pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"freightlogistics:"+name}]}]
        })
    if name not in ("management_computer","server_rack","factory_controller","depot_controller"):
        write_json(ROOT/"data/freightlogistics/recipe"/(name+".json"),{
            "type":"minecraft:crafting_shaped",
            "pattern":["IRI","RCR","III"],
            "key":{"I":"minecraft:iron_ingot","R":"minecraft:redstone","C":"minecraft:comparator"},
            "result":{"id":"freightlogistics:"+name,"count":1}
        })
print("Generated",len(PARTS),"original item textures and",len(BLOCKS),"original block textures")

# Generate small but valid NBT structure files for in-game Create/Ponder tutorials.
# Each blueprint has a 5x5 baseplate and the featured mod block at 2,1,2.
import gzip

def _short(i): return struct.pack(">H",i)
def _int(i): return struct.pack(">i",i)
def _name(s):
    b=s.encode("utf-8")
    return _short(len(b))+b
def _tag(kind,name,payload): return bytes([kind])+_name(name)+payload
def _list(name,element_type,entries):
    return _tag(9,name,bytes([element_type])+_int(len(entries))+b"".join(entries))
def _int_list(name,values): return _list(name,3,[_int(v) for v in values])
def _compound(name,items): return _tag(10,name,b"".join(items)+b"\x00")
def _string(name,value): return _tag(8,name,_name(value))
def ponder_blueprint(block_name):
    palette=[_compound("",[_string("Name","minecraft:polished_andesite")]),
             _compound("",[_string("Name","freightlogistics:"+block_name),
                           _compound("Properties",[_string("facing","north")])])]
    if block_name=="crossing_signal":
        palette[1]=_compound("",[_string("Name","freightlogistics:crossing_signal"),
                     _compound("Properties",[_string("powered","false")])])
    blocks=[]
    for x in range(5):
        for z in range(5):
            blocks.append(_compound("",[_int_list("pos",[x,0,z]),_tag(3,"state",_int(0))]))
    blocks.append(_compound("",[_int_list("pos",[2,1,2]),_tag(3,"state",_int(1))]))
    root=_compound("",[
        _int_list("size",[5,3,5]),
        _list("palette",10,[v[3:] for v in palette]),
        _list("blocks",10,[b[3:] for b in blocks]),
        _list("entities",10,[])
    ])
    path=ROOT/"assets/freightlogistics/ponder/guides"/(block_name+".nbt")
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(gzip.compress(root,mtime=0))
for _id in ("management_computer","server_rack","depot_controller","warehouse_controller",
            "railway_shop","factory_controller","crossing_signal"):
    ponder_blueprint(_id)
print("Generated seven built-in Ponder blueprints")
