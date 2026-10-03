// Mechanical export of the saved Blockbench source, including native UI edits.
import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import assert from 'node:assert/strict';
const root=new URL('./',import.meta.url);
const assets=new URL('../../src/main/resources/assets/aeronautics_fly_by_instruments/',root);
const p=JSON.parse(readFileSync(new URL('terrain_scanner_basic.bbmodel',root),'utf8'));
assert.equal(p.meta.model_format,'java_block');
const textures={particle:'aeronautics_fly_by_instruments:block/terrain_scanner_case'};
for(const[id,t]of p.textures.entries()){
  const name=t.name.replace(/\.png$/,'');
  textures[String(id)]=`aeronautics_fly_by_instruments:block/${name}`;
  assert.ok(t.source.startsWith('data:image/png;base64,'));
  const bytes=Buffer.from(t.source.split(',')[1],'base64'),target=new URL(`textures/block/${name}.png`,assets);
  if(name.startsWith('radar_altimeter_')){
    assert.ok(bytes.equals(readFileSync(target)),`Shared ${name} was changed in the editor; do not overwrite the altimeter.`);
  }else{
    assert.ok(name.startsWith('terrain_scanner_'));
    mkdirSync(new URL('./',target),{recursive:true});writeFileSync(target,bytes);
  }
}
const elements=p.elements.map(c=>{
  assert.equal(c.type,'cube');assert.ok(!c.rotation?.some(v=>v!==0));
  const faces={};
  for(const[side,f]of Object.entries(c.faces)){
    if(f.texture===null||f.texture===false||f.texture===undefined)continue;
    const t=p.textures[f.texture];assert.ok(t);
    faces[side]={uv:f.uv.map((v,i)=>v*16/(i%2?t.uv_height:t.uv_width)),texture:`#${f.texture}`};
    if(f.rotation)faces[side].rotation=f.rotation;
  }
  return{name:c.name,from:c.from,to:c.to,faces};
});
const model={credit:'Prepared locally and refined in Blockbench for Aeronautics: Fly by Instruments',parent:'minecraft:block/block',ambientocclusion:true,textures,elements};
writeFileSync(new URL('models/block/terrain_scanner.json',assets),`${JSON.stringify(model,null,2)}\n`);
console.log(`Exported ${elements.length} cubes; preserved all shared altimeter textures.`);
