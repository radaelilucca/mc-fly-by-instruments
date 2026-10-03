// Bootstrap from the existing cable dimensions; refine and save in Blockbench.
import {writeFileSync} from 'node:fs';
import {deflateSync} from 'node:zlib';
import {randomUUID} from 'node:crypto';
function crc32(bytes){let c=0xffffffff;for(const b of bytes){c^=b;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0);}return(c^0xffffffff)>>>0;}
function chunk(type,bytes){const d=Buffer.concat([Buffer.from(type),bytes]),n=Buffer.alloc(4),c=Buffer.alloc(4);n.writeUInt32BE(bytes.length);c.writeUInt32BE(crc32(d));return Buffer.concat([n,d,c]);}
function png(pixel){const rows=Buffer.alloc(32*129),h=Buffer.alloc(13);for(let y=0;y<32;y++)for(let x=0;x<32;x++)rows.set([...pixel(x,y),255],y*129+1+x*4);h.writeUInt32BE(32);h.writeUInt32BE(32,4);h[8]=8;h[9]=6;return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',h),chunk('IDAT',deflateSync(rows)),chunk('IEND',Buffer.alloc(0))]);}
const textures=[];
function texture(name,pixel){const id=textures.length;textures.push({name,namespace:'aeronautics_fly_by_instruments',folder:'block',id:String(id),uuid:randomUUID(),width:32,height:32,uv_width:32,uv_height:32,internal:true,saved:false,particle:id===0,source:`data:image/png;base64,${png(pixel).toString('base64')}`});return id;}
const jacket=texture('avionics_cable_jacket',(x,y)=>{const edge=x%8===0||x%8===7;const v=edge?32:42+(y%8===0?5:0);return[v,v+5,v+8];});
const hub=texture('avionics_cable_hub',(x,y)=>{if(x<2||y<2)return[81,89,98];if(x>29||y>29)return[24,29,32];if(y>=14&&y<=17&&x>=5&&x<=26)return[49,101,126];if((x===5||x===26)&&(y===5||y===26))return[157,161,163];return[52,59,69];});
const rubber=texture('avionics_cable_relief',(x,y)=>{const v=y%8<2?30:48;return[v,v+4,v+6];});
const ferrule=texture('avionics_cable_ferrule',(x,y)=>{if(y<3)return[231,187,103];if(y>28)return[91,67,35];const n=(x%8===0?7:0);return[177+n,133+n,62+n];});
const elements=[],groups=[];
function group(name){const g={name,uuid:randomUUID(),origin:[8,8,8],children:[]};groups.push(g);return g;}
function cube(g,name,from,to,t,visible=true){const faces=Object.fromEntries(['north','east','south','west','up','down'].map(side=>[side,{uv:[0,0,32,32],texture:t}]));const c={name,type:'cube',uuid:randomUUID(),from,to,origin:[8,8,8],rotation:[0,0,0],box_uv:false,autouv:0,rescale:false,shade:true,visibility:visible,faces};elements.push(c);g.children.push(c.uuid);return c;}
cube(group('hub'),'center_hub',[6,6,6],[10,10,10],hub);
cube(group('straight_arm'),'cable_to_cable',[6,6,8],[10,10,16],jacket,false);
cube(group('flexible_arm'),'flexible_jacket',[6,6,8],[10,10,13],jacket);
const terminal=group('connector');
cube(terminal,'strain_relief',[5.875,5.875,13],[10.125,10.125,14.5],rubber);
cube(terminal,'metal_ferrule',[5.5,5.5,14.5],[10.5,10.5,15.25],ferrule);
cube(terminal,'socket_tip',[6.25,6.25,15.25],[9.75,9.75,16],rubber);
const p={meta:{format_version:'4.10',model_format:'java_block',box_uv:false},name:'avionics_cable',java_block_version:'1.9',ambientocclusion:true,visible_box:[1,1,0],resolution:{width:32,height:32},elements,outliner:groups,textures};
writeFileSync(new URL('./avionics_cable.bbmodel',import.meta.url),JSON.stringify(p));
console.log('Prepared hub, hidden straight arm, flexible arm and stepped connector for native refinement.');
