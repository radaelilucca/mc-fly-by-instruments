import assert from 'node:assert/strict';
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { deflateSync } from 'node:zlib';

const projectDir = dirname(fileURLToPath(import.meta.url));
const modRoot = join(projectDir, '..', '..');
const modAssets = join(modRoot, 'src', 'main', 'resources', 'assets', 'aeronautics_fly_by_instruments');
const pixel = Buffer.from([212, 29, 56, 156]);

function crc32(buffer) {
  let crc = 0xffffffff;
  for (const byte of buffer) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const typeBytes = Buffer.from(type, 'ascii');
  const result = Buffer.alloc(12 + data.length);
  result.writeUInt32BE(data.length, 0);
  typeBytes.copy(result, 4);
  data.copy(result, 8);
  result.writeUInt32BE(crc32(result.subarray(4, 8 + data.length)), 8 + data.length);
  return result;
}

function uniformRedLens() {
  const header = Buffer.alloc(13);
  header.writeUInt32BE(16, 0);
  header.writeUInt32BE(16, 4);
  header[8] = 8;
  header[9] = 6; // RGBA
  const rows = Buffer.alloc(16 * (1 + 16 * 4));
  for (let y = 0; y < 16; y++) {
    const offset = y * (1 + 16 * 4);
    rows[offset] = 0; // PNG filter: None
    for (let x = 0; x < 16; x++) pixel.copy(rows, offset + 1 + x * 4);
  }
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk('IHDR', header),
    chunk('IDAT', deflateSync(rows)),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}

const lensPng = uniformRedLens();
for (const projectName of ['lamp_module_1x1.bbmodel', 'lamp_module_1x1_on.bbmodel']) {
  const projectPath = join(projectDir, projectName);
  const project = JSON.parse(readFileSync(projectPath, 'utf8'));
  const lens = project.textures.find(texture => texture.name === 'lamp_lens_red');
  assert.ok(lens, `Missing lamp_lens_red in ${projectName}`);
  assert.equal(lens.width, 16);
  assert.equal(lens.height, 16);
  lens.source = `data:image/png;base64,${lensPng.toString('base64')}`;
  writeFileSync(projectPath, `${JSON.stringify(project)}\n`);
}

for (const target of [
  join(projectDir, 'export', 'assets', 'aeronautics_fly_by_instruments', 'textures', 'block', 'lamp_lens_red.png'),
  join(modAssets, 'textures', 'block', 'lamp_lens_red.png'),
]) {
  writeFileSync(target, lensPng);
}

console.log('Updated both Blockbench projects and synchronized uniform red translucent lens textures.');
