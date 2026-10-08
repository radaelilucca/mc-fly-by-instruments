// Bootstrap only: use export-assets.mjs after editing in Blockbench.
import {writeFileSync} from 'node:fs';
import {deflateSync} from 'node:zlib';
import {randomUUID} from 'node:crypto';
const size=64, pixels=Buffer.alloc(size*size*4);
function rect(x,y,w,h,c){for(let j=y;j<y+h;j++)for(let i=x;i<x+w;i++)pixels.set([...c,255],(j*size+i)*4);}
rect(0,0,64,64,[24,29,32]);
const islands={metal:[0,0,16,16],blue:[16,0,32,16],dark:[32,0,48,16],socket:[48,0,64,16],screw:[0,16,4,20],brass:[4,16,8,20],red:[8,16,12,20],amber:[12,16,16,20],green:[16,16,20,20],vent:[24,16,40,32],label:[40,16,56,24]};
rect(0,0,16,16,[157,161,163]);rect(0,0,16,1,[209,213,215]);rect(0,1,1,15,[186,189,191]);rect(15,1,1,15,[106,112,116]);rect(1,15,14,1,[126,132,135]);
rect(16,0,16,16,[49,101,126]);rect(16,0,16,1,[76,126,147]);rect(16,1,1,15,[65,116,138]);rect(31,1,1,15,[30,58,73]);rect(17,15,14,1,[35,76,94]);
rect(32,0,16,16,[47,52,55]);rect(32,0,16,1,[76,83,88]);
rect(48,0,16,16,[24,29,32]);rect(48,0,16,2,[107,116,122]);rect(48,14,16,2,[61,68,73]);rect(48,2,2,12,[97,106,112]);rect(62,2,2,12,[61,68,73]);
rect(52,4,8,8,[4,7,9]);rect(54,6,4,4,[177,133,62]);rect(55,7,2,2,[231,187,103]);
rect(0,16,4,4,[129,135,138]);rect(1,17,2,2,[47,52,55]);rect(4,16,4,4,[177,133,62]);rect(5,16,2,1,[231,187,103]);
rect(8,16,4,4,[100,32,37]);rect(9,17,2,2,[155,55,60]);rect(12,16,4,4,[133,97,32]);rect(13,17,2,2,[181,133,44]);rect(16,16,4,4,[36,76,62]);rect(17,17,2,2,[57,110,88]);
rect(24,16,16,16,[47,52,55]);for(let y=18;y<31;y+=3)rect(26,y,12,1,[15,20,23]);
rect(40,16,16,8,[30,58,73]);rect(42,18,12,1,[133,173,187]);rect(42,21,8,1,[86,131,148]);
function crc(b){let c=0xffffffff;for(const v of b){c^=v;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0);}return(c^0xffffffff)>>>0;}
function chunk(t,b){const d=Buffer.concat([Buffer.from(t),b]),n=Buffer.alloc(4),c=Buffer.alloc(4);n.writeUInt32BE(b.length);c.writeUInt32BE(crc(d));return Buffer.concat([n,d,c]);}
const rows=Buffer.alloc(size*(size*4+1));for(let y=0;y<size;y++)pixels.copy(rows,y*(size*4+1)+1,y*size*4,(y+1)*size*4);
const header=Buffer.alloc(13);header.writeUInt32BE(size);header.writeUInt32BE(size,4);header[8]=8;header[9]=6;
const png=Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',deflateSync(rows)),chunk('IEND',Buffer.alloc(0))]);
const elements=[],outliner=[];
function group(name,build){const start=elements.length;build();outliner.push({name,uuid:randomUUID(),origin:[8,8,8],children:elements.slice(start).map(e=>e.uuid)});}
function cube(name,from,to,material,overrides={}){elements.push({name,type:'cube',uuid:randomUUID(),from,to,origin:[8,8,8],rotation:[0,0,0],box_uv:false,autouv:0,rescale:false,shade:true,faces:Object.fromEntries(['north','east','south','west','up','down'].map(side=>[side,{uv:islands[overrides[side]??material],texture:0}]))});}
group('robust_metal_housing',()=>{
 cube('cabinet',[1,1,1],[15,15,15],'metal');
 cube('support_base',[2,.25,2],[14,1.25,14],'dark');
 cube('blue_left_panel',[.85,2,2],[1,14,14],'blue');
 cube('blue_right_panel',[15,2,2],[15.15,14,14],'blue');
 cube('blue_top_panel',[2,15,2],[14,15.15,14],'blue');
 cube('rear_vent_panel',[2,2,15],[14,14,15.15],'vent');
 cube('front_faceplate',[2,2,.75],[14,14,1],'blue');
});
group('front_controls_and_handle',()=>{
 cube('handle_left_mount',[3,11.5,.35],[4.5,13.5,.75],'metal');
 cube('handle_right_mount',[11.5,11.5,.35],[13,13.5,.75],'metal');
 cube('handle_left_arm',[3.5,12,0],[4.5,13,.35],'metal');
 cube('handle_right_arm',[11.5,12,0],[12.5,13,.35],'metal');
 cube('handle_crossbar',[4.5,12,0],[11.5,13,.35],'metal');
 cube('identification_plate',[4,3,.55],[9.5,4.5,.75],'label');
 cube('test_button_surround',[3,5,.4],[5,7,.75],'dark');
 cube('test_button',[3.4,5.4,.15],[4.6,6.6,.4],'brass');
 cube('mode_knob',[10.5,4.5,.15],[12.5,6.5,.75],'dark',{north:'screw'});
 for(const [index,m]of ['green','amber','red'].entries()){
  const y=7.5+index*1.35;
  cube(`indicator_frame_${m}`,[11,y,.5],[12.5,y+1,.75],'dark');
  cube(`decorative_indicator_${m}`,[11.2,y+.2,.35],[12.3,y+.8,.5],m);
 }
 for(const x of [2.4,12.8])for(const y of [2.4,13])cube(`front_screw_${x}_${y}`,[x,y,.55],[x+.8,y+.8,.75],'screw');
});
group('centered_avionics_sockets',()=>{
 cube('socket_north',[6,6,0],[10,10,.75],'dark',{north:'socket'});
 cube('socket_up',[6,15.15,6],[10,16,10],'dark',{up:'socket'});
 cube('socket_down',[6,0,6],[10,1,10],'dark',{down:'socket'});
});
group('panel_fasteners',()=>{
 for(const x of [2.5,12.7])for(const z of [2.5,12.7])cube(`top_screw_${x}_${z}`,[x,15.15,z],[x+.8,15.3,z+.8],'screw');
});
const project={meta:{format_version:'4.10',model_format:'java_block',box_uv:false},name:'gpws_computer',java_block_version:'1.9',ambientocclusion:true,resolution:{width:size,height:size},elements,outliner,textures:[{name:'gpws_computer',namespace:'aeronautics_fly_by_instruments',folder:'block',id:'0',uuid:randomUUID(),width:size,height:size,uv_width:size,uv_height:size,particle:true,internal:true,saved:false,source:`data:image/png;base64,${png.toString('base64')}`}]};
writeFileSync(new URL('gpws_computer.bbmodel',import.meta.url),JSON.stringify(project));
console.log(`Created ${elements.length} GPWS cuboids; north-facing, within 0–16.`);
