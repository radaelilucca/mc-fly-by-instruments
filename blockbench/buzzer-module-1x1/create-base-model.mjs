// Prepare editable geometry/materials for import and refinement in Blockbench.
// Run only when creating a fresh project; export-assets.mjs preserves UI edits.
import { writeFileSync } from 'node:fs';
import { deflateSync } from 'node:zlib';
import { randomUUID } from 'node:crypto';

const namespace = 'aeronautics_fly_by_instruments';
const radius = 1.5;
const apothem = radius * Math.cos(Math.PI / 16);
const half = radius * Math.sin(Math.PI / 16);
const normals = Array.from({ length: 16 }, (_, i) => [Math.sin(i * Math.PI / 8), -Math.cos(i * Math.PI / 8)]);
const inShell = (x, z) => normals.every(n => n[0] * x + n[1] * z <= apothem);

function crc32(bytes) {
  let crc = 0xffffffff;
  for (const byte of bytes) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function chunk(type, bytes) {
  const data = Buffer.concat([Buffer.from(type), bytes]);
  const size = Buffer.alloc(4), crc = Buffer.alloc(4);
  size.writeUInt32BE(bytes.length); crc.writeUInt32BE(crc32(data));
  return Buffer.concat([size, data, crc]);
}

function png(pixel) {
  const rows = Buffer.alloc(64 * (64 * 4 + 1));
  for (let y = 0; y < 64; y++) for (let x = 0; x < 64; x++) {
    rows.set(pixel(x, y), y * 257 + 1 + x * 4);
  }
  const header = Buffer.alloc(13);
  header.writeUInt32BE(64, 0); header.writeUInt32BE(64, 4); header[8] = 8; header[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk('IHDR', header), chunk('IDAT', deflateSync(rows)), chunk('IEND', Buffer.alloc(0))]);
}

const textureSpecs = [
  ['buzzer_housing', (x, y) => {
    let v = 12 + Math.round(18 * Math.exp(-(((x - 19) / 9) ** 2)));
    if (y < 3 || y > 60) v = 8;
    else if (y === 3 || y === 59) v = 28;
    return [v, v + 1, v + 2, 255];
  }],
  ['buzzer_top', (x, y) => {
    const px = ((x + .5) / 64 - .5) * 3, pz = ((y + .5) / 64 - .5) * 3;
    if (!inShell(px, pz) || Math.hypot(px, pz) < .27) return [0, 0, 0, 0];
    const seed = ((x * 73856093) ^ (y * 19349663)) >>> 0;
    let v = 26 + seed % 17;
    if (Math.max(...normals.map(n => n[0] * px + n[1] * pz)) > apothem - .055) v = 15;
    if (Math.hypot(px, pz) < .34) v = 10;
    return [v, v, v, 255];
  }],
  ['buzzer_base', (x, y) => {
    const px = ((x + .5) / 64 - .5) * 3, pz = ((y + .5) / 64 - .5) * 3;
    return inShell(px, pz) ? [10, 11, 12, 255] : [0, 0, 0, 0];
  }],
  ['buzzer_hole', () => [4, 4, 5, 255]],
];
const textures = textureSpecs.map(([name, pixel], id) => ({
  name, namespace, folder: 'block', id: String(id), uuid: randomUUID(),
  width: 64, height: 64, uv_width: 64, uv_height: 64,
  particle: id === 0, internal: true, saved: false,
  source: `data:image/png;base64,${png(pixel).toString('base64')}`,
}));
const elements = [];
function face(name, from, to, side, texture, angle = 0, origin = [8, 0, 8]) {
  const faces = Object.fromEntries(['north', 'east', 'south', 'west', 'up', 'down'].map(key =>
    [key, { uv: [0, 0, 64, 64], texture: key === side ? texture : null }]));
  elements.push({ name, type: 'cube', uuid: randomUUID(), from, to, origin,
    rotation: [0, angle, 0], box_uv: false, autouv: 0, rescale: false, shade: true, faces });
}
function ring(prefix, a, tangentHalf, y0, y1, texture, inward) {
  for (let i = 0; i < 16; i++) {
    const angle = i * 22.5, theta = angle * Math.PI / 180;
    const cardinal = Math.floor((angle + 45) / 90) % 4;
    let residual = angle - cardinal * 90;
    if (residual > 180) residual -= 360;
    const x = 8 + a * Math.sin(theta), z = 8 - a * Math.cos(theta);
    const horizontal = cardinal === 0 || cardinal === 2;
    const side = (inward ? ['south', 'west', 'north', 'east'] : ['north', 'east', 'south', 'west'])[cardinal];
    face(`${prefix}_${String(i + 1).padStart(2, '0')}`,
      horizontal ? [x - tangentHalf, y0, z] : [x, y0, z - tangentHalf],
      horizontal ? [x + tangentHalf, y1, z] : [x, y1, z + tangentHalf],
      side, texture, -residual, [x, 0, z]);
  }
}
ring('piano_side', apothem, half, 0, 2, 0, false);
face('porous_cap_with_hole', [6.5, 2, 6.5], [9.5, 2, 9.5], 'up', 1);
face('solid_mounting_base', [6.5, 0, 6.5], [9.5, 0, 9.5], 'down', 2);
ring('recess_wall', .27 * Math.cos(Math.PI / 16), .27 * Math.sin(Math.PI / 16), 1.5, 2, 3, true);
face('dark_recess_floor', [7.65, 1.5, 7.65], [8.35, 1.5, 8.35], 'up', 3);

const project = {
  meta: { format_version: '4.10', model_format: 'java_block', box_uv: false },
  name: 'buzzer_module_1x1', model_identifier: '', visible_box: [1, 1, 0],
  java_block_version: '1.9', ambientocclusion: true,
  resolution: { width: 64, height: 64 }, elements,
  outliner: [{ name: 'buzzer_1x1', uuid: randomUUID(), origin: [8, 0, 8], children: elements.map(e => e.uuid) }],
  textures,
};
writeFileSync(new URL('./buzzer_module_1x1.bbmodel', import.meta.url), JSON.stringify(project));
console.log('Prepared 35 editable Java surfaces and four embedded 64x64 textures for Blockbench.');
