// Mechanical export: retains the geometry and pixel painting saved in Blockbench.
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import assert from 'node:assert/strict';

const root = new URL('./', import.meta.url);
const assets = new URL('../../src/main/resources/assets/aeronautics_fly_by_instruments/', root);
const project = JSON.parse(readFileSync(new URL('fms_module_2x3.bbmodel', root), 'utf8'));
assert.equal(project.meta.model_format, 'java_block');
const textures = { particle: 'aeronautics_fly_by_instruments:block/fms_module' };
for (const [index, texture] of project.textures.entries()) {
  const name = texture.name.replace(/\.png$/i, '');
  textures[String(index)] = `aeronautics_fly_by_instruments:block/${name}`;
  assert.ok(texture.source.startsWith('data:image/png;base64,'));
  const target = new URL(`textures/block/${name}.png`, assets);
  mkdirSync(new URL('./', target), { recursive: true });
  writeFileSync(target, Buffer.from(texture.source.split(',')[1], 'base64'));
}
const elements = project.elements.filter(cube => cube.export !== false).map(cube => {
  assert.equal(cube.type, 'cube');
  assert.ok(cube.from.every((v, axis) => v < cube.to[axis]), `Degenerate cube: ${cube.name}`);
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
    result.rotation = { origin: cube.origin, axis: ['x', 'y', 'z'][axis], angle: cube.rotation[axis], rescale: !!cube.rescale };
  }
  return result;
});
const low = [0, 1, 2].map(axis => Math.min(...elements.map(e => e.from[axis])));
const high = [0, 1, 2].map(axis => Math.max(...elements.map(e => e.to[axis])));
assert.equal(low[1], 0, 'Mounting face must remain at Y=0');
assert.equal((low[0] + high[0]) / 2, 8, 'X center must remain 8');
assert.equal((low[2] + high[2]) / 2, 8, 'Z center must remain 8');
assert.ok(high[0] - low[0] < 16 * 2 / 3 && high[2] - low[2] < 16, 'Model must fit inside the 2x3 footprint');
const model = { credit: 'Created and refined in Blockbench for Aeronautics: Fly by Instruments',
  render_type: 'minecraft:solid', ambientocclusion: true, textures, elements };
const target = new URL('models/block/fms_module_2x3.json', assets);
mkdirSync(new URL('./', target), { recursive: true });
writeFileSync(target, `${JSON.stringify(model, null, 2)}\n`);
console.log(`Exported ${elements.length} cubes and ${project.textures.length} texture(s); bounds ${low} to ${high}.`);
