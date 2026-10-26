// Monta a cena da logo (Generic Model) a partir dos .bbmodel dos três sensores.
// Uso: node blockbench/_logo/compose-logo-scene.mjs [layout.json]
// Cada sensor vira um grupo raiz com pivô no centro da base, então dá para girar e
// mover cada um no Blockbench sem tocar nas fontes. Nada é copiado do Create: a
// textura de latão continua apontando para reference/create-textures (fora do git).
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';

const here = path.dirname(new URL(import.meta.url).pathname);
const root = path.resolve(here, '..');

// A câmera olha a frente dos scanners (de -Z para +Z), então +X aparece à esquerda:
// Basic à esquerda, Advanced à direita, altímetro no centro, mais alto e mais atrás.
const DEFAULT_LAYOUT = {
  sensors: [
    { name: 'terrain_scanner_basic', file: 'terrain-scanner-basic/terrain_scanner_basic.bbmodel', at: [22, 0, -3], yaw: 30 },
    { name: 'radar_altimeter', file: 'radar-altimeter/radar_altimeter.bbmodel', at: [0, 4, 5], yaw: 45 },
    { name: 'terrain_scanner_advanced', file: 'terrain-scanner-advanced/terrain_scanner_advanced.bbmodel', at: [-22, 0, -3], yaw: -30 },
  ],
};

const layout = process.argv[2] ? JSON.parse(fs.readFileSync(process.argv[2], 'utf8')) : DEFAULT_LAYOUT;
const out = layout.out ? path.resolve(layout.out) : path.join(here, 'logo_scene.bbmodel');
const uuid = () => crypto.randomUUID();
const add = (a, b) => a.map((v, i) => v + b[i]);

const scene = {
  meta: { format_version: '5.0', model_format: 'free', box_uv: false },
  name: 'logo_scene',
  model_identifier: '',
  visible_box: [1, 1, 0],
  variable_placeholders: '',
  variable_placeholder_buttons: [],
  unhandled_root_fields: {},
  resolution: { width: 32, height: 32 },
  elements: [],
  groups: [],
  outliner: [],
  textures: [],
};

for (const s of layout.sensors) {
  const src = JSON.parse(fs.readFileSync(path.join(root, s.file), 'utf8'));
  // Java Block usa o canto do bloco como origem; aqui o centro da base fica em `at`.
  const shift = [s.at[0] - 8, s.at[1], s.at[2] - 8];
  const texBase = scene.textures.length;
  const ids = new Map(); // uuid antigo -> novo, para repetir o mesmo sensor na cena
  const id = (old) => { if (!ids.has(old)) ids.set(old, uuid()); return ids.get(old); };

  for (const t of src.textures) {
    const copy = { ...t, uuid: uuid() };
    if (copy.relative_path) copy.relative_path = path.relative(here, path.resolve(path.dirname(path.join(root, s.file)), copy.relative_path)).split(path.sep).join('/');
    delete copy.path;
    scene.textures.push(copy);
  }
  for (const e of src.elements) {
    const faces = Object.fromEntries(Object.entries(e.faces).map(([k, f]) => [k,
      { ...f, texture: f.texture === null || f.texture === undefined ? f.texture : f.texture + texBase }]));
    scene.elements.push({ ...e, uuid: id(e.uuid), from: add(e.from, shift), to: add(e.to, shift), origin: add(e.origin, shift), faces });
  }
  for (const g of src.groups) scene.groups.push({ ...g, uuid: id(g.uuid), origin: add(g.origin, shift) });

  const pivot = uuid();
  scene.groups.push({
    name: s.name, origin: [s.at[0], s.at[1], s.at[2]], rotation: [s.pitch ?? 0, s.yaw ?? 0, s.roll ?? 0],
    color: 0, uuid: pivot, export: true, mirror_uv: false, isOpen: true, locked: false, visibility: true, autouv: 0,
  });
  const remap = (node) => typeof node === 'string' ? id(node) : { ...node, uuid: id(node.uuid), children: node.children.map(remap) };
  scene.outliner.push({ uuid: pivot, isOpen: true, children: src.outliner.map(remap) });
}

fs.writeFileSync(out, JSON.stringify(scene, null, 2));
console.log(`cena: ${path.relative(process.cwd(), out)} (${scene.elements.length} elementos, ${scene.textures.length} texturas)`);
