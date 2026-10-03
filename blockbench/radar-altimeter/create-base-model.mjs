// Initial editable geometry and pixel materials; refine and save in Blockbench.
// Do not rerun over the refined source project. export-assets.mjs preserves UI edits.
import { writeFileSync } from 'node:fs';
import { deflateSync } from 'node:zlib';
import { randomUUID } from 'node:crypto';

function crc32(bytes) {
  let crc = 0xffffffff;
  for (const byte of bytes) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0);
  }
  return (crc ^ 0xffffffff) >>> 0;
}
function chunk(type, bytes) {
  const data = Buffer.concat([Buffer.from(type), bytes]);
  const size = Buffer.alloc(4), crc = Buffer.alloc(4);
  size.writeUInt32BE(bytes.length); crc.writeUInt32BE(crc32(data));
  return Buffer.concat([size, data, crc]);
}
function png(pixel) {
  const rows = Buffer.alloc(32 * 129);
  for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) rows.set([...pixel(x, y), 255], y * 129 + 1 + x * 4);
  const header = Buffer.alloc(13);
  header.writeUInt32BE(32, 0); header.writeUInt32BE(32, 4); header[8] = 8; header[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk('IHDR', header), chunk('IDAT', deflateSync(rows)), chunk('IEND', Buffer.alloc(0))]);
}
const noise = (x,y) => ((x * 13 + y * 7) % 5) - 2;
const specs = [
  ['radar_altimeter_case', (x,y) => {
    let v = 186 + noise(x,y);
    if (x < 2 || x > 29 || y < 2 || y > 29) v = 125;
    else if (x === 2 || y === 2) v = 224;
    else if (x === 29 || y === 29) v = 149;
    // Corner rivets and cooling slots. These are not sensing faces.
    if ([4,27].includes(x) && [4,27].includes(y)) return [65,70,74];
    if (y >= 7 && y <= 11 && y % 2 === 1 && x >= 6 && x <= 25) return [67,75,80];
    // Large downward arrow with a shaft and stepped head.
    if ((y >= 15 && y <= 23 && x >= 14 && x <= 17) ||
        (y >= 21 && y <= 27 && Math.abs(x-15.5) <= (27-y))) return [228,169,47];
    return [v,v+3,v+5];
  }],
  ['radar_altimeter_metal', (x,y) => {
    let v = 157 + noise(x,y);
    if (x < 2 || y < 2) v = 209;
    if (x > 29 || y > 29) v = 101;
    if ([4,27].includes(x) && [4,27].includes(y)) v = 55;
    return [v,v+4,v+6];
  }],
  ['radar_altimeter_frame', (x,y) => {
    const edge = x < 2 || y < 2 || x > 29 || y > 29;
    let v = edge ? 24 : 47 + noise(x,y);
    if (y === 2) v = 78;
    return [v,v+5,v+8];
  }],
  ['radar_altimeter_antenna', (x,y) => {
    // A unique downward radome: off-white ceramic, brass stepped target, dark centre.
    const r = Math.max(Math.abs(x-15.5), Math.abs(y-15.5));
    if (r > 13) return [71,78,80];
    if ((r >= 10 && r < 12) || (r >= 5 && r < 7)) return [185,130,47];
    if (r < 2) return [62,72,75];
    if ((x === 15 || x === 16 || y === 15 || y === 16) && r > 7) return [185,130,47];
    const v = 216 + noise(x,y);
    return [v, v-3, v-14];
  }],
  ['radar_altimeter_socket', (x,y) => {
    const r = Math.max(Math.abs(x-15.5), Math.abs(y-15.5));
    if (r > 12) return [161,118,51];
    if (r > 10) return [231,187,103];
    if (r > 8) return [81,69,45];
    if (x % 6 === 3 && y % 6 === 3) return [204,160,74];
    return [23,29,34];
  }],
  ['radar_altimeter_brass', (x,y) => {
    const v = noise(x,y);
    if (x < 2 || y < 2) return [233,195,120];
    if (x > 29 || y > 29) return [99,75,38];
    return [177+v,133+v,62+v];
  }],
  ['radar_altimeter_label', (x,y) => {
    if (x < 2 || y < 2 || x > 29 || y > 29) return [30,58,73];
    // Abstract pixel radar emblem, deliberately no real-world branding.
    if ((y >= 8 && y <= 10 && x >= 7 && x <= 24) ||
        (y >= 14 && y <= 16 && x >= 10 && x <= 21) ||
        (y >= 20 && y <= 22 && x >= 14 && x <= 17)) return [207,227,227];
    return [49,101,126];
  }],
];
const textures = specs.map(([name,pixel],id) => ({
  name, namespace:'aeronautics_fly_by_instruments', folder:'block', id:String(id), uuid:randomUUID(),
  width:32, height:32, uv_width:32, uv_height:32, particle:id===0, internal:true, saved:false,
  source:`data:image/png;base64,${png(pixel).toString('base64')}`,
}));
const elements=[];
function cube(name,from,to,texture,overrides={}) {
  const faces=Object.fromEntries(['north','east','south','west','up','down'].map(side=>
    [side,{uv:[0,0,32,32],texture:overrides[side]??texture}]));
  elements.push({name,type:'cube',uuid:randomUUID(),from,to,origin:[8,8,8],rotation:[0,0,0],
    box_uv:false,autouv:0,rescale:false,shade:true,faces});
}
cube('metal_case',[1,3,1],[15,14,15],0,{up:1,down:1});
cube('lid_seam',[1,14,1],[15,14.5,15],2);
cube('mounting_lid',[.5,14.5,.5],[15.5,15.5,15.5],1);
cube('radar_emblem',[4,15.5,5],[12,15.75,11],6,{north:2,east:2,south:2,west:2,down:2});
cube('lower_protective_frame',[1,1,1],[15,3,15],2);
cube('downward_antenna',[3,0,3],[13,1,13],1,{down:3});
cube('socket_mount',[2.5,8.5,.5],[8.5,13.5,1],2);
cube('data_connector',[3,9,0],[8,13,.5],1,{north:4});
cube('coax_mount',[10,5,.5],[14,12.5,1],1);
cube('coax_tx',[10.75,9.75,0],[13.25,12.25,.5],5,{north:4});
cube('coax_rx',[10.75,5.25,0],[13.25,7.75,.5],5,{north:4});
// A chunky silhouette with mounting shoulders, all within the original block cell.
cube('mount_left',[0,12.5,3],[1,14,13],1);
cube('mount_right',[15,12.5,3],[16,14,13],1);

const project={meta:{format_version:'4.10',model_format:'java_block',box_uv:false},
  name:'radar_altimeter',model_identifier:'radar_altimeter',visible_box:[1,1,0],
  java_block_version:'1.9',ambientocclusion:true,resolution:{width:32,height:32},elements,
  outliner:[{name:'radar_altimeter',uuid:randomUUID(),origin:[8,0,8],children:elements.map(e=>e.uuid)}],textures};
writeFileSync(new URL('./radar_altimeter.bbmodel',import.meta.url),JSON.stringify(project));
console.log(`Prepared ${elements.length} editable cubes and ${textures.length} pixel materials for Blockbench.`);
