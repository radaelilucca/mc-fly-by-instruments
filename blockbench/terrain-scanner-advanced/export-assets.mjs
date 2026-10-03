// Export the saved native editor source. Shared family textures are read-only.
import {readFileSync,writeFileSync} from 'node:fs';
import assert from 'node:assert/strict';
const root=new URL('./',import.meta.url);
const assets=new URL('../../src/main/resources/assets/aeronautics_fly_by_instruments/',root);
const p=JSON.parse(readFileSync(new URL('terrain_scanner_advanced.bbmodel',root),'utf8'));
assert.equal(p.meta.model_format,'java_block');
const textures={particle:'aeronautics_fly_by_instruments:block/terrain_scanner_advanced_case'};
for(const[id,t]of p.textures.entries()){
  const name=t.name.replace(/\.png$/,'');
  textures[String(id)]=`aeronautics_fly_by_instruments:block/${name}`;
  assert.ok(t.source.startsWith('data:image/png;base64,'));
  const bytes=Buffer.from(t.source.split(',')[1],'base64'),target=new URL(`textures/block/${name}.png`,assets);
  assert.equal(bytes.readUInt32BE(16),32);assert.equal(bytes.readUInt32BE(20),32);
  if(name.startsWith('terrain_scanner_advanced_'))writeFileSync(target,bytes);
  else assert.ok(bytes.equals(readFileSync(target)),`Shared ${name} changed; do not overwrite Basic/Altimeter.`);
}
const basic=JSON.parse(readFileSync(new URL('../terrain-scanner-basic/terrain_scanner_basic.bbmodel',root),'utf8'));
assert.equal(p.elements.length,basic.elements.length);
const elements=p.elements.map(c=>{
  const source=basic.elements.find(e=>e.name===c.name);assert.ok(source);
  assert.deepEqual(c.from,source.from);assert.deepEqual(c.to,source.to);
  assert.equal(c.type,'cube');assert.ok(!c.rotation?.some(v=>v!==0));
  const faces={};
  for(const[side,f]of Object.entries(c.faces)){
    if(f.texture===null||f.texture===false||f.texture===undefined)continue;
    const t=p.textures[f.texture];assert.ok(t);
    faces[side]={uv:f.uv.map((v,i)=>v*16/(i%2?t.uv_height:t.uv_width)),texture:`#${f.texture}`};
    if(f.rotation)faces[side].rotation=f.rotation;
  }return{name:c.name,from:c.from,to:c.to,faces};
});
writeFileSync(new URL('models/block/terrain_scanner_advanced.json',assets),`${JSON.stringify({credit:'Duplicated from Basic and refined in Blockbench for Aeronautics: Fly by Instruments',parent:'minecraft:block/block',ambientocclusion:true,textures,elements},null,2)}\n`);
console.log(`Exported Advanced: ${elements.length} preserved cubes, own 32x32 textures.`);
