// Mechanical export of the projects authored in the Blockbench UI.
// Does not modify the .bbmodel files or install assets into the running mod.
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';

const root = dirname(fileURLToPath(import.meta.url));
const namespace = 'aeronautics_fly_by_instruments';
const assetRoot = join(root, 'export', 'assets', namespace);
const baseGeometry = new Map();
const roles = {
  housing_opaque: 'housing',
  lens_tint: 'lens',
  emitter_intensity_0_15: 'emitter',
};
const sharedTextures = new Map();
let referenceGeometry;

for (const { source, footprint, variant } of [
  { source: 'lamp_module_1x1.bbmodel', footprint: '1x1', variant: 'off' },
  { source: 'lamp_module_1x1_on.bbmodel', footprint: '1x1', variant: 'on' },
  { source: '../lamp-module-2x1/lamp_module_2x1_off.bbmodel', footprint: '2x1', variant: 'off' },
  { source: '../lamp-module-2x1/lamp_module_2x1_on.bbmodel', footprint: '2x1', variant: 'on' },
]) {
  const project = JSON.parse(readFileSync(join(root, source), 'utf8'));
  assert.equal(project.meta.model_format, 'java_block');
  assert.deepEqual(project.resolution, { width: 16, height: 16 });
  assert.equal(project.elements.length, 3);
  const textures = {};
  const elements = project.elements.map(cube => {
    const role = roles[cube.name];
    assert.ok(role, `Unknown element: ${cube.name}`);
    assert.equal(cube.type, 'cube');
    cube.from.forEach((value, axis) => assert.ok(value < cube.to[axis]));
    const faces = {};
    for (const [side, face] of Object.entries(cube.faces)) {
      assert.equal(face.uv.length, 4);
      assert.ok(face.uv.every(value => value >= 0 && value <= 16));
      const texture = project.textures[face.texture];
      assert.ok(texture, `Missing texture for ${cube.name}/${side}`);
      const name = role === 'emitter' ? `lamp_emitter_${variant}` : texture.name;
      const reference = `${namespace}:block/${name}`;
      assert.ok(!textures[role] || textures[role] === reference);
      textures[role] = reference;
      assert.ok(texture.source.startsWith('data:image/png;base64,'));
      const bytes = Buffer.from(texture.source.split(',')[1], 'base64');
      assert.equal(bytes.subarray(1, 4).toString(), 'PNG');
      assert.equal(bytes.readUInt32BE(16), 16);
      assert.equal(bytes.readUInt32BE(20), 16);
      if (sharedTextures.has(name)) assert.ok(sharedTextures.get(name).equals(bytes));
      sharedTextures.set(name, bytes);
      faces[side] = { uv: face.uv, texture: `#${role}` };
      if (face.rotation) faces[side].rotation = face.rotation;
    }
    return { from: cube.from, to: cube.to, faces };
  });
  const geometry = elements.map(({ from, to }) => ({ from, to }));
  if (footprint === '1x1') {
    if (baseGeometry.has(footprint)) assert.deepEqual(geometry, baseGeometry.get(footprint));
    else baseGeometry.set(footprint, geometry);
  } else {
    const expected = baseGeometry.get('1x1').map(({ from, to }) => ({
      from: [Number((8 + (from[0] - 8) * 2).toFixed(4)), from[1], from[2]],
      to: [Number((8 + (to[0] - 8) * 2).toFixed(4)), to[1], to[2]],
    }));
    assert.deepEqual(geometry, expected, '2x1 geometry must stretch the 1x1 model along local X');
    if (baseGeometry.has(footprint)) assert.deepEqual(geometry, baseGeometry.get(footprint));
    else baseGeometry.set(footprint, geometry);
  }
  const model = {
    credit: 'Created in Blockbench for Aeronautics: Fly by Instruments',
    render_type: 'minecraft:translucent',
    textures: { particle: textures.housing, ...textures },
    elements,
  };
  const path = join(assetRoot, 'models', 'block', `lamp_module_${footprint}_${variant}.json`);
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, `${JSON.stringify(model, null, 2)}\n`);
  console.log(`${footprint} ${variant}: validated 3 cubes, 18 UV faces, embedded 16x16 textures`);
}

for (const [name, bytes] of sharedTextures) {
  const path = join(assetRoot, 'textures', 'block', `${name}.png`);
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, bytes);
}
console.log('Exported four models and four PNG textures; runtime assets unchanged.');
