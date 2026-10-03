// Export the saved Blockbench materials/parts, preserving native editor refinements.
import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import assert from 'node:assert/strict';
const root=new URL('./',import.meta.url),assets=new URL('../../src/main/resources/assets/aeronautics_fly_by_instruments/',root);
const p=JSON.parse(readFileSync(new URL('avionics_cable.bbmodel',root),'utf8'));
assert.equal(p.meta.model_format,'java_block');
const textures={particle:'aeronautics_fly_by_instruments:block/avionics_cable_jacket'};
for(const[id,t]of p.textures.entries()){
  const name=t.name.replace(/\.png$/,'');assert.ok(name.startsWith('avionics_cable_'));
  textures[String(id)]=`aeronautics_fly_by_instruments:block/${name}`;
  assert.ok(t.source.startsWith('data:image/png;base64,'));
  const target=new URL(`textures/block/${name}.png`,assets);mkdirSync(new URL('./',target),{recursive:true});
  writeFileSync(target,Buffer.from(t.source.split(',')[1],'base64'));
}
const cubes=new Map(p.elements.map(c=>[c.uuid,c]));
function part(groupName){
  const group=p.groups?.find(g=>g.name===groupName);
  const g=p.outliner.find(g=>g.name===groupName || (group && g.uuid===group.uuid));
  assert.ok(g,`Missing group ${groupName}`);return g.children.map(id=>{
  const c=cubes.get(id);assert.equal(c.type,'cube');assert.ok(!c.rotation?.some(v=>v!==0));
  const faces={};
  for(const[side,f]of Object.entries(c.faces)){
    if(f.texture===null||f.texture===undefined)continue;
    // These faces mate with other geometry or the device, so do not create coplanar caps.
    if(c.name==='socket_tip'&&side==='south')continue;
    if(c.name==='flexible_jacket'&&side==='south')continue;
    if(c.name==='strain_relief'&&side==='north')continue;
    const t=p.textures[f.texture];
    faces[side]={uv:f.uv.map((v,i)=>v*16/(i%2?t.uv_height:t.uv_width)),texture:`#${f.texture}`};
    if(f.rotation)faces[side].rotation=f.rotation;
  }
  return{name:c.name,from:c.from,to:c.to,faces};
});}
function model(name,elements,display){writeFileSync(new URL(`models/block/${name}.json`,assets),JSON.stringify({credit:'Modeled and refined in Blockbench for Aeronautics: Fly by Instruments',parent:'minecraft:block/block',ambientocclusion:true,textures,elements,...(display?{display}:{})},null,2)+'\n');}
const hub=part('hub'),arm=part('straight_arm');
model('avionics_cable_core',hub);
model('avionics_cable_flex',part('flexible_arm'));
model('avionics_cable_terminal',part('connector'));
for(const[direction,axis,angle]of [['south','y',0],['north','y',180],['east','y',90],['west','y',270],['up','x',270],['down','x',90]]){
  if(angle===0){model(`avionics_cable_${direction}`,arm);continue;}
  // Java blockstate rotation supplies six straight variants without changing authored UVs.
  const radians=angle*Math.PI/180;
  const rotate=point=>{const[x,y,z]=point.map(v=>v-8);const c=Math.round(Math.cos(radians)),s=Math.round(Math.sin(radians));return axis==='y'?[c*x+s*z+8,y+8,-s*x+c*z+8]:[x+8,c*y-s*z+8,s*y+c*z+8];};
  const normals={north:[8,8,0],south:[8,8,16],west:[0,8,8],east:[16,8,8],up:[8,16,8],down:[8,0,8]};
  const sideMap=Object.fromEntries(Object.entries(normals).map(([side,n])=>[side,Object.keys(normals).find(k=>normals[k].every((v,i)=>v===rotate(n)[i]))]));
  model(`avionics_cable_${direction}`,arm.map(e=>{const a=rotate(e.from),b=rotate(e.to);return{...e,from:a.map((v,i)=>Math.min(v,b[i])),to:a.map((v,i)=>Math.max(v,b[i])),faces:Object.fromEntries(Object.entries(e.faces).map(([side,f])=>[sideMap[side],f]))};}));
}
// The item shows a small lead and its connector rather than an unidentifiable cube.
model('avionics_cable_item',[...hub,...part('flexible_arm'),...part('connector')],{
  gui:{rotation:[30,225,0],translation:[0,0,0],scale:[1.2,1.2,1.2]},
  ground:{rotation:[0,0,0],translation:[0,2,0],scale:[.7,.7,.7]},
  fixed:{rotation:[0,180,0],translation:[0,0,0],scale:[1,1,1]},
  thirdperson_righthand:{rotation:[75,45,0],translation:[0,2.5,0],scale:[.6,.6,.6]},
  firstperson_righthand:{rotation:[0,45,0],translation:[0,0,0],scale:[.8,.8,.8]}
});
writeFileSync(new URL('models/item/avionics_cable.json',assets),JSON.stringify({parent:'aeronautics_fly_by_instruments:block/avionics_cable_item'},null,2)+'\n');
console.log('Exported cable hub, six straight arms, flexible jacket, connector and item from saved Blockbench source.');
