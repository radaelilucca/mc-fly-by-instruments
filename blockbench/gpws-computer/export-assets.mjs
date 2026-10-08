import {readFileSync,writeFileSync} from 'node:fs';
import assert from 'node:assert/strict';
const root=new URL('./',import.meta.url),assets=new URL('../../src/main/resources/assets/aeronautics_fly_by_instruments/',root);
const p=JSON.parse(readFileSync(new URL('gpws_computer.bbmodel',root),'utf8'));
assert.equal(p.meta.model_format,'java_block');assert.equal(p.textures.length,1);
const t=p.textures[0];assert.equal(t.name.replace(/\.png$/i,''),'gpws_computer');assert.ok(t.source.startsWith('data:image/png;base64,'));
const elements=p.elements.filter(c=>c.export!==false).map(c=>{
 assert.equal(c.type,'cube');assert.ok(!c.rotation?.some(v=>v!==0));
 assert.ok(c.from.every((v,a)=>v>=0&&v<c.to[a]&&c.to[a]<=16),c.name);
 return {name:c.name,from:c.from,to:c.to,faces:Object.fromEntries(Object.entries(c.faces).filter(([,f])=>f.texture===0).map(([side,f])=>[side,{uv:f.uv.map((v,i)=>v*16/(i%2?t.uv_height:t.uv_width)),texture:'#0',...(f.rotation?{rotation:f.rotation}:{})}]))};
});
const model={credit:'Modeled and pixel-painted in Blockbench for Aeronautics: Fly by Instruments',parent:'minecraft:block/block',render_type:'minecraft:solid',ambientocclusion:true,textures:{particle:'aeronautics_fly_by_instruments:block/gpws_computer','0':'aeronautics_fly_by_instruments:block/gpws_computer'},elements};
writeFileSync(new URL('models/block/gpws_computer.json',assets),JSON.stringify(model,null,2)+'\n');
writeFileSync(new URL('textures/block/gpws_computer.png',assets),Buffer.from(t.source.split(',')[1],'base64'));
console.log(`Exported ${elements.length} opaque cubes and embedded texture.`);
