// Mechanical export from the saved, refined Blockbench source; no regeneration.
import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import assert from 'node:assert/strict';
const root=new URL('./',import.meta.url);
const assets=new URL('../../src/main/resources/assets/aeronautics_fly_by_instruments/',root);
const project=JSON.parse(readFileSync(new URL('radar_altimeter.bbmodel',root),'utf8'));
assert.equal(project.meta.model_format,'java_block');
const textures={particle:'aeronautics_fly_by_instruments:block/radar_altimeter_case'};
for(const [id,t] of project.textures.entries()) {
  textures[String(id)]=`aeronautics_fly_by_instruments:block/${t.name.replace(/\.png$/,'')}`;
  assert.ok(t.source.startsWith('data:image/png;base64,'));
  const target=new URL(`textures/block/${t.name.replace(/\.png$/,'')}.png`,assets);
  mkdirSync(new URL('./',target),{recursive:true});
  writeFileSync(target,Buffer.from(t.source.split(',')[1],'base64'));
}
const elements=project.elements.map(cube=>{
  assert.equal(cube.type,'cube');
  assert.ok(!cube.rotation?.some(v=>v!==0),'This model uses unrotated Minecraft cuboids.');
  const faces={};
  for(const [side,f] of Object.entries(cube.faces)) {
    if(f.texture===null||f.texture===false||f.texture===undefined) continue;
    const t=project.textures[f.texture]; assert.ok(t);
    faces[side]={uv:f.uv.map((v,i)=>v*16/(i%2?t.uv_height:t.uv_width)),texture:`#${f.texture}`};
    if(f.rotation)faces[side].rotation=f.rotation;
  }
  return {name:cube.name,from:cube.from,to:cube.to,faces};
});
const model={credit:'Prepared locally and refined in Blockbench for Aeronautics: Fly by Instruments',
  parent:'minecraft:block/block',ambientocclusion:true,textures,elements};
writeFileSync(new URL('models/block/radar_altimeter.json',assets),`${JSON.stringify(model,null,2)}\n`);
console.log(`Exported ${elements.length} cubes and ${project.textures.length} textures.`);
