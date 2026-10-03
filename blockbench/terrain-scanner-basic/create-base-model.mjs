// Bootstrap only: import into Blockbench and refine through its native UI.
// export-assets.mjs preserves the saved editor source; never rerun this over it.
import {readFileSync,writeFileSync} from 'node:fs';
import {deflateSync} from 'node:zlib';
import {randomUUID} from 'node:crypto';
const assets=new URL('../../src/main/resources/assets/aeronautics_fly_by_instruments/',import.meta.url);
function crc32(bytes){let c=0xffffffff;for(const b of bytes){c^=b;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0);}return(c^0xffffffff)>>>0;}
function chunk(type,bytes){const d=Buffer.concat([Buffer.from(type),bytes]),n=Buffer.alloc(4),c=Buffer.alloc(4);n.writeUInt32BE(bytes.length);c.writeUInt32BE(crc32(d));return Buffer.concat([n,d,c]);}
function png(pixel){
  const rows=Buffer.alloc(32*129),h=Buffer.alloc(13);
  for(let y=0;y<32;y++)for(let x=0;x<32;x++)rows.set([...pixel(x,y),255],y*129+1+x*4);
  h.writeUInt32BE(32,0);h.writeUInt32BE(32,4);h[8]=8;h[9]=6;
  return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',h),chunk('IDAT',deflateSync(rows)),chunk('IEND',Buffer.alloc(0))]);
}
const textures=[];
function texture(name,bytes){const id=textures.length;textures.push({name,namespace:'aeronautics_fly_by_instruments',folder:'block',id:String(id),uuid:randomUUID(),width:32,height:32,uv_width:32,uv_height:32,particle:id===0,internal:true,saved:false,source:`data:image/png;base64,${bytes.toString('base64')}`});return id;}
// Shared family materials remain byte-identical to the existing altimeter assets.
const metal=texture('radar_altimeter_metal',readFileSync(new URL('textures/block/radar_altimeter_metal.png',assets)));
const frame=texture('radar_altimeter_frame',readFileSync(new URL('textures/block/radar_altimeter_frame.png',assets)));
const brass=texture('radar_altimeter_brass',readFileSync(new URL('textures/block/radar_altimeter_brass.png',assets)));
const label=texture('radar_altimeter_label',readFileSync(new URL('textures/block/radar_altimeter_label.png',assets)));
const socket=texture('radar_altimeter_socket',readFileSync(new URL('textures/block/radar_altimeter_socket.png',assets)));
const noise=(x,y)=>((x*13+y*7)%5)-2;
const casing=texture('terrain_scanner_case',png((x,y)=>{
  let v=186+noise(x,y);
  if(x<2||x>29||y<2||y>29)v=125;
  else if(x===2||y===2)v=224;
  else if(x===29||y===29)v=149;
  if([4,27].includes(x)&&[4,27].includes(y))return[65,70,74];
  if(y>=7&&y<=11&&y%2===1&&x>=6&&x<=25)return[67,75,80];
  // Leftward arrow in the WEST face UV; mirrored UVs on EAST also point NORTH.
  if((x>=11&&x<=23&&y>=19&&y<=22)||(x>=6&&x<=12&&Math.abs(y-20.5)<=x-6))return[228,169,47];
  return[v,v+3,v+5];
}));
const antenna=texture('terrain_scanner_antenna',png((x,y)=>{
  const edge=x<2||x>29||y<2||y>29;
  if(edge)return[24,29,32];
  for(const cx of[7,16,25])for(const cy of[7,16,25]){
    const r=Math.max(Math.abs(x-cx),Math.abs(y-cy));
    if(r<=2){const v=216+noise(x,y);return[v,v-3,v-14];}
    if(r<=3)return[185,130,47];
  }
  return[47+noise(x,y),52+noise(x,y),55+noise(x,y)];
}));
const top=texture('terrain_scanner_top',png((x,y)=>{
  let v=157+noise(x,y);
  if(x<2||y<2)v=209;
  if(x>29||y>29)v=101;
  if([4,27].includes(x)&&[4,27].includes(y))return[55,59,61];
  // Arrow towards the canonical front (negative Z, top UV's upper edge).
  if((y>=5&&y<=12&&x>=14&&x<=17)||(y>=3&&y<=8&&Math.abs(x-15.5)<=y-3))return[228,169,47];
  return[v,v+4,v+6];
}));
const elements=[];
function cube(name,from,to,t,overrides={}){
  const faces=Object.fromEntries(['north','east','south','west','up','down'].map(side=>[side,{uv:[0,0,32,32],texture:overrides[side]??t}]));
  elements.push({name,type:'cube',uuid:randomUUID(),from,to,origin:[8,8,8],rotation:[0,0,0],box_uv:false,autouv:0,rescale:false,shade:true,faces});
}
cube('metal_case',[1,2,3],[15,14,15],metal,{west:casing,east:casing});
elements.at(-1).faces.east.uv=[32,0,0,32];
cube('antenna_backing',[2,3,1],[14,13,3],frame);
cube('forward_antenna',[2,3,0],[14,13,1],brass,{north:antenna});
cube('bezel_top',[1,13,0],[15,14,3],frame);
cube('bezel_bottom',[1,2,0],[15,3,3],frame);
cube('bezel_left',[1,3,0],[2,13,3],frame);
cube('bezel_right',[14,3,0],[15,13,3],frame);
cube('mounting_lid',[.5,14,0],[15.5,15,15.5],metal,{up:top});
cube('mount_left',[0,0,6],[3,2,14],metal);
cube('mount_right',[13,0,6],[16,2,14],metal);
cube('radar_emblem',[5,15,8],[11,15.25,13],frame,{up:label});
cube('rear_socket_mount',[5,5,15],[11,11,15.5],frame);
cube('rear_data_connector',[6,6,15.5],[10,10,16],metal,{south:socket});
const project={meta:{format_version:'4.10',model_format:'java_block',box_uv:false},name:'terrain_scanner_basic',java_block_version:'1.9',ambientocclusion:true,visible_box:[1,1,0],resolution:{width:32,height:32},elements,outliner:[{name:'terrain_scanner_basic',uuid:randomUUID(),origin:[8,8,8],children:elements.map(e=>e.uuid)}],textures};
writeFileSync(new URL('./terrain_scanner_basic.bbmodel',import.meta.url),JSON.stringify(project));
console.log(`Prepared ${elements.length} cubes with five shared materials and three dedicated textures.`);
