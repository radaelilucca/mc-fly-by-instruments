// Derives editable Blockbench projects and runtime JSON from the v1 1x1 lamp kept in base-v1/.
// Legacy until the 2x1 gets its own v2.1 model (docs/visual-design-guide.md §9).
import { randomUUID } from 'node:crypto';
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';

const root = dirname(fileURLToPath(import.meta.url));
const oneByOneRoot = join(root, 'base-v1');
const namespace = 'aeronautics_fly_by_instruments';
const exportModelRoot = join(oneByOneRoot, 'export', 'assets', namespace, 'models', 'block');
const runtimeModelRoot = join(root, '..', '..', 'src', 'main', 'resources', 'assets', namespace, 'models', 'block');
const uuid = () => randomUUID();
const stretch = value => Number((8 + (value - 8) * 2).toFixed(4));
const roles = { housing_opaque: 'housing', lens_tint: 'lens', emitter_intensity_0_15: 'emitter' };

for (const variant of ['off', 'on']) {
  const projectPath = join(oneByOneRoot, variant === 'off' ? 'lamp_module_1x1_v1.bbmodel' : 'lamp_module_1x1_v1_on.bbmodel');
  const project = JSON.parse(readFileSync(projectPath, 'utf8'));
  assert.equal(project.meta.model_format, 'java_block');
  assert.equal(project.elements.length, 3);

  const textures = {};
  const elements = project.elements.map(cube => {
    const role = roles[cube.name];
    assert.ok(role, `Unknown element: ${cube.name}`);
    const faces = {};
    for (const [side, face] of Object.entries(cube.faces)) {
      const texture = project.textures[face.texture];
      assert.ok(texture, `Missing texture for ${cube.name}/${side}`);
      const textureName = role === 'emitter' ? `lamp_emitter_${variant}` : texture.name;
      const reference = `${namespace}:block/${textureName}`;
      assert.ok(!textures[role] || textures[role] === reference);
      textures[role] = reference;
      faces[side] = { uv: face.uv, texture: `#${role}` };
      if (face.rotation) faces[side].rotation = face.rotation;
    }
    return { from: cube.from.map((value, axis) => axis === 0 ? stretch(value) : value),
      to: cube.to.map((value, axis) => axis === 0 ? stretch(value) : value), faces };
  });
  const model = {
    credit: 'Created in Blockbench for Aeronautics: Fly by Instruments',
    render_type: 'minecraft:translucent',
    textures: { particle: textures.housing, ...textures },
    elements,
  };

  const uuidMap = new Map();
  for (const element of project.elements) {
    uuidMap.set(element.uuid, uuid());
    element.uuid = uuidMap.get(element.uuid);
    element.from[0] = stretch(element.from[0]);
    element.to[0] = stretch(element.to[0]);
    element.origin[0] = stretch(element.origin[0]);
  }
  for (const texture of project.textures) {
    uuidMap.set(texture.uuid, uuid());
    texture.uuid = uuidMap.get(texture.uuid);
  }
  project.outliner = project.outliner.map(entry => uuidMap.get(entry) ?? entry);
  project.name = `lamp_module_2x1_${variant}`;
  project.visible_box = [2, 1, 0];

  const sourcePath = join(root, `lamp_module_2x1_${variant}.bbmodel`);
  writeFileSync(sourcePath, `${JSON.stringify(project)}\n`);

  for (const modelRoot of [exportModelRoot, runtimeModelRoot]) {
    const outputPath = join(modelRoot, `lamp_module_2x1_${variant}.json`);
    mkdirSync(dirname(outputPath), { recursive: true });
    writeFileSync(outputPath, `${JSON.stringify(model, null, 2)}\n`);
  }
  console.log(`Generated editable Blockbench source and runtime model: 2x1 ${variant}`);
}
