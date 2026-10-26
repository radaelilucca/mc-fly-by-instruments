// Mechanical export from the saved Blockbench source (radar_altimeter.bbmodel); no regeneration.
// Writes models/block/radar_altimeter.json, models/block/radar_altimeter_powered.json and the
// PNGs of this mod's own textures. Textures in another namespace (create:block/...) are
// referenced by id only: Create's assets are All Rights Reserved and load from Create's jar.
// Enforces docs/visual-design-guide.md v2: integer cuboids, no rotation, 1 texel per unit.
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import assert from 'node:assert/strict';

const NS = 'aeronautics_fly_by_instruments';
const root = new URL('./', import.meta.url);
const assets = new URL(`../../src/main/resources/assets/${NS}/`, root);
const project = JSON.parse(readFileSync(new URL('radar_altimeter.bbmodel', root), 'utf8'));
assert.equal(project.meta.model_format, 'java_block');

const ids = project.textures.map(t => `${t.namespace || NS}:${t.folder || 'block'}/${t.name.replace(/\.png$/, '')}`);
const byName = name => project.textures.findIndex(t => t.name.replace(/\.png$/, '') === name);
const ATLAS = byName('radar_altimeter');
const POWERED = byName('radar_altimeter_powered');
assert.ok(ATLAS >= 0 && POWERED >= 0, 'Atlas and powered atlas must exist');

for (const t of project.textures) {
  if ((t.namespace || NS) !== NS) continue;
  assert.ok(t.source?.startsWith('data:image/png;base64,'), `${t.name} must be embedded`);
  const target = new URL(`textures/block/${t.name.replace(/\.png$/, '')}.png`, assets);
  mkdirSync(new URL('./', target), { recursive: true });
  writeFileSync(target, Buffer.from(t.source.split(',')[1], 'base64'));
}

const AXES = { north: [0, 1], south: [0, 1], east: [2, 1], west: [2, 1], up: [0, 2], down: [0, 2] };
const used = new Set();
const elements = project.elements.map(cube => {
  assert.equal(cube.type, 'cube');
  assert.ok(!cube.rotation?.some(v => v !== 0), `${cube.name}: carcass cuboids stay unrotated`);
  for (const v of [...cube.from, ...cube.to]) assert.ok(Number.isInteger(v), `${cube.name}: integer coordinates`);
  const size = [0, 1, 2].map(i => cube.to[i] - cube.from[i]);
  const faces = {};
  for (const [side, f] of Object.entries(cube.faces)) {
    if (f.texture === null || f.texture === false || f.texture === undefined) continue;
    const t = project.textures[f.texture]; assert.ok(t, `${cube.name}.${side}: missing texture`);
    const rotated = f.rotation === 90 || f.rotation === 270;
    let [gu, gv] = AXES[side].map(a => size[a]);
    if (rotated) [gu, gv] = [gv, gu];
    const texelsU = Math.abs(f.uv[2] - f.uv[0]) * t.width / t.uv_width;
    const texelsV = Math.abs(f.uv[3] - f.uv[1]) * t.height / t.uv_height;
    assert.equal(texelsU, gu, `${cube.name}.${side}: 1 texel per unit (u)`);
    assert.equal(texelsV, gv, `${cube.name}.${side}: 1 texel per unit (v)`);
    faces[side] = { uv: f.uv.map((v, i) => v * 16 / (i % 2 ? t.uv_height : t.uv_width)), texture: `#${f.texture}` };
    if (f.rotation) faces[side].rotation = f.rotation;
    used.add(f.texture);
  }
  return { name: cube.name, from: cube.from, to: cube.to, faces };
});

function model(atlasId) {
  const textures = { particle: ids[atlasId] };
  for (const index of [...used].sort()) textures[String(index)] = index === ATLAS ? ids[atlasId] : ids[index];
  return { credit: 'Made with Blockbench for Aeronautics: Fly by Instruments',
    parent: 'minecraft:block/block', ambientocclusion: true, textures, elements };
}
const out = name => new URL(`models/block/${name}.json`, assets);
writeFileSync(out('radar_altimeter'), `${JSON.stringify(model(ATLAS), null, 2)}\n`);
writeFileSync(out('radar_altimeter_powered'), `${JSON.stringify(model(POWERED), null, 2)}\n`);
console.log(`Exported ${elements.length} cubes; textures: ${[...used].map(i => ids[i]).join(', ')} (+ powered atlas).`);
