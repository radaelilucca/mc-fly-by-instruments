// Mechanical export of the saved Blockbench project, retaining its edits.
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import assert from 'node:assert/strict';

const root = new URL('./', import.meta.url);
const assets = new URL('../../src/main/resources/assets/aeronautics_fly_by_instruments/', root);
const project = JSON.parse(readFileSync(new URL('buzzer_module_1x1.bbmodel', root), 'utf8'));
assert.equal(project.meta.model_format, 'java_block');
const textures = { particle: 'aeronautics_fly_by_instruments:block/buzzer_housing' };
for (const [id, texture] of project.textures.entries()) {
  textures[String(id)] = `aeronautics_fly_by_instruments:block/${texture.name}`;
  assert.ok(texture.source.startsWith('data:image/png;base64,'));
  const target = new URL(`textures/block/${texture.name}.png`, assets);
  mkdirSync(new URL('./', target), { recursive: true });
  writeFileSync(target, Buffer.from(texture.source.split(',')[1], 'base64'));
}
const elements = project.elements.map(cube => {
  assert.equal(cube.type, 'cube');
  const faces = {};
  for (const [side, face] of Object.entries(cube.faces)) {
    if (face.texture === null || face.texture === false || face.texture === undefined) continue;
    const texture = project.textures[face.texture];
    assert.ok(texture);
    const uv = face.uv.map((v, i) => v * 16 / (i % 2 ? texture.uv_height : texture.uv_width));
    faces[side] = { uv, texture: `#${face.texture}` };
    if (face.rotation) faces[side].rotation = face.rotation;
  }
  const result = { name: cube.name, from: cube.from, to: cube.to, faces };
  if (cube.rotation?.some(v => v !== 0)) {
    const axis = cube.rotation.findIndex(v => v !== 0);
    assert.equal(cube.rotation.filter(v => v !== 0).length, 1);
    assert.ok([-45, -22.5, 22.5, 45].includes(cube.rotation[axis]));
    result.rotation = { origin: cube.origin, axis: ['x', 'y', 'z'][axis], angle: cube.rotation[axis], rescale: false };
  }
  return result;
});
const model = {
  credit: 'Created and refined in Blockbench for Aeronautics: Fly by Instruments',
  render_type: 'minecraft:cutout', ambientocclusion: true, textures, elements,
};
const target = new URL('models/block/buzzer_module_1x1.json', assets);
mkdirSync(new URL('./', target), { recursive: true });
writeFileSync(target, `${JSON.stringify(model, null, 2)}\n`);
console.log(`Exported ${elements.length} surfaces and ${project.textures.length} textures from the saved Blockbench project.`);
