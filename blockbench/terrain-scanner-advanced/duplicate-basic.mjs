// One-time bootstrap: duplicate the saved Basic, never rebuild its geometry.
// Native Blockbench refinements are preserved by export-assets.mjs.
import {readFileSync, writeFileSync, existsSync} from 'node:fs';
import {inflateSync, deflateSync} from 'node:zlib';
import {randomUUID, createHash} from 'node:crypto';
import assert from 'node:assert/strict';
const root = new URL('./', import.meta.url);
const target = new URL('terrain_scanner_advanced.bbmodel', root);
assert.ok(!existsSync(target), 'Do not overwrite an edited Advanced source.');
const source = readFileSync(new URL('../terrain-scanner-basic/terrain_scanner_basic.bbmodel', root));
const p = JSON.parse(source);
p.name = 'terrain_scanner_advanced';
for (const group of p.groups ?? []) group.name = group.name.replace('basic', 'advanced');
function crc32(bytes) { let c=0xffffffff; for(const b of bytes){c^=b;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0);}return(c^0xffffffff)>>>0; }
function chunk(type,bytes){const d=Buffer.concat([Buffer.from(type),bytes]),n=Buffer.alloc(4),c=Buffer.alloc(4);n.writeUInt32BE(bytes.length);c.writeUInt32BE(crc32(d));return Buffer.concat([n,d,c]);}
function decode(t) {
  const b=Buffer.from(t.source.split(',')[1],'base64'), idat=[]; let offset=8;
  while(offset<b.length){const n=b.readUInt32BE(offset),type=b.toString('ascii',offset+4,offset+8);if(type==='IDAT')idat.push(b.subarray(offset+8,offset+8+n));offset+=n+12;}
  const raw=inflateSync(Buffer.concat(idat)),out=Buffer.alloc(4096),stride=128;
  for(let y=0;y<32;y++)for(let x=0;x<stride;x++){
    const filter=raw[y*129],v=raw[y*129+1+x],a=x>=4?out[y*stride+x-4]:0,b=y?out[(y-1)*stride+x]:0,c=x>=4&&y?out[(y-1)*stride+x-4]:0;
    const q=a+b-c,pa=Math.abs(q-a),pb=Math.abs(q-b),pc=Math.abs(q-c);
    const add=filter===0?0:filter===1?a:filter===2?b:filter===3?Math.floor((a+b)/2):pa<=pb&&pa<=pc?a:pb<=pc?b:c;
    out[y*stride+x]=(v+add)&255;
  }return out;
}
function png(rgba){const rows=Buffer.alloc(32*129),h=Buffer.alloc(13);for(let y=0;y<32;y++)rgba.copy(rows,y*129+1,y*128,(y+1)*128);h.writeUInt32BE(32,0);h.writeUInt32BE(32,4);h[8]=8;h[9]=6;return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',h),chunk('IDAT',deflateSync(rows)),chunk('IEND',Buffer.alloc(0))]);}
function pixels(t, fn) {const rgba=decode(t);for(let y=0;y<32;y++)for(let x=0;x<32;x++){const v=fn(x,y,[...rgba.subarray((y*32+x)*4,(y*32+x)*4+4)]);rgba.set(v,(y*32+x)*4);}return png(rgba);}
const blue=[49,101,126,255];
function replace(index,name,bytes){const t=p.textures[index];t.name=name;t.path='';t.saved=false;t.internal=true;t.source=`data:image/png;base64,${bytes.toString('base64')}`;}
replace(5,'terrain_scanner_advanced_case',pixels(p.textures[5],(x,y,v)=>[27,30].includes(x)&&y>=2&&y<=29?blue:v));
replace(7,'terrain_scanner_advanced_top',pixels(p.textures[7],(x,y,v)=>[28,30].includes(y)&&x>=2&&x<=29?blue:v));
const antenna=decode(p.textures[6]);
for(let y=0;y<32;y++)for(let x=0;x<32;x++){
  let color=(x<2||x>29||y<2||y>29)?[24,29,32,255]:[48,53,56,255];
  // 25 separate four-pixel pads, with two-pixel gutters; no higher resolution.
  for(let row=0;row<5;row++)for(let col=0;col<5;col++){
    const left=2+col*6,top=2+row*6;
    if(x>=left&&x<left+4&&y>=top&&y<top+4){color=[177,133,62,255];if(x>left&&x<left+3&&y>top&&y<top+3)color=[216,213,202,255];if(y===top)color=[231,187,103,255];}
  }antenna.set(color,(y*32+x)*4);
}
replace(6,'terrain_scanner_advanced_antenna',png(antenna));
function append(name,bytes){const t=structuredClone(p.textures[0]),i=p.textures.length;t.name=name;t.uuid=randomUUID();t.id=String(i);t.particle=false;t.path='';t.saved=false;t.source=`data:image/png;base64,${bytes.toString('base64')}`;p.textures.push(t);return i;}
const rear=append('terrain_scanner_advanced_rear',pixels(p.textures[0],(x,y,v)=>[4,7].includes(x)&&y>=4&&y<=27?blue:v));
const bottom=append('terrain_scanner_advanced_bottom',pixels(p.textures[0],(x,y,v)=>[1,4].includes(y)&&x>=2&&x<=29?blue:v));
const lidSide=append('terrain_scanner_advanced_lid_side',pixels(p.textures[0],(x,y,v)=>[28,30].includes(x)?blue:v));
const casing=p.elements.find(e=>e.name==='metal_case');casing.faces.south.texture=rear;casing.faces.down.texture=bottom;
const lid=p.elements.find(e=>e.name==='mounting_lid');lid.faces.west.texture=lidSide;lid.faces.east.texture=lidSide;lid.faces.east.uv=[32,0,0,32];
p.advanced_source={basic_sha256:createHash('sha256').update(source).digest('hex'),note:'Exact saved Basic geometry duplicated; 5x5 pads and static double-blue identification.'};
writeFileSync(target,JSON.stringify(p));
console.log(`Duplicated ${p.elements.length} existing cuboids; Basic untouched; all textures 32x32.`);
