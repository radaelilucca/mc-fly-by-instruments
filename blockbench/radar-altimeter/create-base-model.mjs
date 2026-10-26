// Bootstrap of the v2 Radar Altimeter (Create brass family). Writes radar_altimeter.bbmodel.
// Geometry and pixel art are authored here as data; refine in Blockbench afterwards.
// Do NOT rerun over a source refined in Blockbench: export-assets.mjs preserves UI edits.
// Guide: docs/visual-design-guide.md (v2). Palette: blockbench/_palette/create-aeronautics.gpl.
import { writeFileSync } from 'node:fs';
import { deflateSync } from 'node:zlib';
import { randomUUID } from 'node:crypto';

const NS = 'aeronautics_fly_by_instruments';
// Blockbench's Java Block format uses ONE UV space for every texture (the project
// resolution) and ignores per-texture UV size on export. Keep uv_width = RES everywhere and
// scale the UVs of smaller textures with px() so that 1 texel = 1 model unit.
const RES = 32;
const px = (size, uv) => uv.map(v => v * RES / size);

// --- Palette (HEX ramps from docs/visual-design-guide.md §5) -----------------------------
const P = {
  // brass, dark -> light
  1: '#592424', 2: '#724731', 3: '#9E6947', 4: '#BF8E55', 5: '#CEA05A', 6: '#E4B763', 7: '#F7CB6C', 8: '#FFEB8C',
  // dark wood
  a: '#301E0E', b: '#3A2411', c: '#442B15', d: '#4F3118', e: '#53381A', f: '#5A3E1E',
  // redstone off / on
  r: '#460000', s: '#500101', t: '#580101', R: '#970000', S: '#AE0000', T: '#CD0000', U: '#FD0000',
  // dark lens
  L: '#13161D', M: '#171B23', N: '#20232A', O: '#282C37', Q: '#333846', V: '#464C5F', G: '#5059AB', H: '#5371C6',
  // dark metal
  k: '#2B2B2B', l: '#353535', m: '#3B3B3C', n: '#464647',
};

// --- Pixel maps (row 0 = top of the texture region) ---------------------------------------
// Side of the body: 16 wide x 14 tall (body spans y 2..16). Brass lid band, shadow seam,
// dark-wood body between brass corner posts, framed redstone slot, brass foot rim.
const side = (slotTop, slotBottom) => [
  '1233333333333321',
  '2588888887777752',
  '3577777777776653',
  '3566666666665543',
  '2333333333333332',
  '1bbbbbbbbbbbbbb1',
  '32deddeddddedd12',
  '32caaaaaaaaaac12',
  '32ca' + slotTop + 'ac12',
  '32ca' + slotBottom + 'ac12',
  '32caaaaaaaaaac12',
  '32eddeddeddedd12',
  '32bbbbbbbbbbbb12',
  '2566666666666532',
];
const SIDE_OFF = side('ttsttsts', 'rrsrrrsr');
const SIDE_ON = side('TTUTTTUT', 'RRSRRRSR');

// Underside of the body: 16 x 16. Brass edge and corner rivets around a dark-wood ring;
// the centre 12 x 12 is covered by the lens bezel.
const BOTTOM = [
  '1233333333333321',
  '26bbbbbbbbbbbb52',
  ...Array(12).fill('3baaaaaaaaaaaab2'),
  '25bbbbbbbbbbbb42',
  '1222222222222221',
];
// Lens bezel, seen from below: 12 x 12 brass ring, 2 px wide. Outer rim lit top/left,
// inner step shaded where the protruding lens meets it. Centre 8 x 8 is covered by the lens.
const FRAME_DOWN = [
  '577777777775',
  '733333333353',
  ...Array(8).fill('73NNNNNNNN53'),
  '735555555553',
  '533333333332',
];
const FRAME_SIDE = ['356666666653'];      // 12 x 1

// Lens: 8 x 8 dark glass with concentric rings and one blue glint, plus 8 x 1 rim.
const LENS_DOWN = [
  'OOOOOOOO',
  'OVMMMMMO',
  'OMQQQQMO',
  'OMQLLQMO',
  'OMQLHQMO',
  'OMQQQQMO',
  'OMMMMMMO',
  'OOOOOOOO',
];
const LENS_SIDE = ['lmmmmmml'];

// --- Texture assembly -------------------------------------------------------------------------
function canvas(size, fill) {
  return Array.from({ length: size }, () => Array.from({ length: size }, () => fill));
}
function blit(target, map, x0, y0) {
  map.forEach((row, y) => [...row].forEach((key, x) => {
    if (!(key in P)) throw new Error(`Unknown palette key "${key}"`);
    target[y0 + y][x0 + x] = P[key];
  }));
}
// Atlas layout (pixels = model units, 1:1):
const AT = {
  side: [0, 0, 16, 14], bottom: [16, 0, 32, 16], frameDown: [0, 16, 12, 28], frameSide: [12, 16, 24, 17],
};
function atlas(sideMap) {
  const img = canvas(32, P[2]);
  blit(img, sideMap, 0, 0);
  blit(img, BOTTOM, 16, 0);
  blit(img, FRAME_DOWN, 0, 16);
  blit(img, FRAME_SIDE, 12, 16);
  return img;
}
const LN = { down: px(16, [0, 0, 8, 8]), side: px(16, [0, 8, 8, 9]) };
function lensTexture() { // 16 x 16 pixels; regions in LN are pre-scaled to the shared UV space
  const img = canvas(16, P.N);
  blit(img, LENS_DOWN, 0, 0);
  blit(img, LENS_SIDE, 0, 8);
  return img;
}

// --- Minimal PNG encoder ------------------------------------------------------------------
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
function png(img) {
  const h = img.length, w = img[0].length;
  const rows = Buffer.alloc(h * (w * 4 + 1));
  img.forEach((row, y) => row.forEach((hex, x) => {
    const o = y * (w * 4 + 1) + 1 + x * 4;
    rows[o] = parseInt(hex.slice(1, 3), 16); rows[o + 1] = parseInt(hex.slice(3, 5), 16);
    rows[o + 2] = parseInt(hex.slice(5, 7), 16); rows[o + 3] = 255;
  }));
  const header = Buffer.alloc(13);
  header.writeUInt32BE(w, 0); header.writeUInt32BE(h, 4); header[8] = 8; header[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk('IHDR', header), chunk('IDAT', deflateSync(rows)), chunk('IEND', Buffer.alloc(0))]);
}

// --- Blockbench project -------------------------------------------------------------------
function texture(name, size, { namespace = NS, source = null, relativePath = '', particle = false } = {}) {
  return {
    name: `${name}.png`, path: '', relative_path: relativePath, folder: 'block', namespace,
    id: '', group: '', scope: 0, width: size, height: size, uv_width: RES, uv_height: RES,
    particle, use_as_default: false, layers_enabled: false, sync_to_project: '', file_format: 'png',
    render_mode: 'default', render_sides: 'auto', wrap_mode: 'limited', pbr_channel: 'color', fps: 7,
    frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true,
    internal: source !== null, saved: source !== null, uuid: randomUUID(),
    ...(source ? { source: `data:image/png;base64,${png(source).toString('base64')}` } : {}),
  };
}
// Create's assets are All Rights Reserved: reference them by id, never embed their pixels in
// this repo. For the Blockbench preview, keep a local copy of the PNG in
// reference/create-textures/ (gitignored); Blockbench resolves it through relative_path.
const textures = [
  texture('radar_altimeter', 32, { source: atlas(SIDE_OFF), particle: true }), // 0
  texture('radar_altimeter_powered', 32, { source: atlas(SIDE_ON) }),          // 1 (variant only)
  texture('radar_altimeter_lens', 16, { source: lensTexture() }),              // 2
  texture('brass_casing', 16, { namespace: 'create',
    relativePath: '../../reference/create-textures/brass_casing.png' }),        // 3
];
textures.forEach((t, i) => { t.id = String(i); });

const face = (uv, tex) => ({ uv, texture: tex });
const sides = (uvNS, uvEW, tex, extra) => ({
  north: face(uvNS, tex), south: face(uvNS, tex), east: face(uvEW, tex), west: face(uvEW, tex), ...extra,
});
function cube(name, from, to, faces, color) {
  return {
    name, box_uv: false, render_order: 'default', rescale: false, locked: false, shade: true,
    light_emission: 0, export: true, scope: 0, allow_mirror_modeling: true, from, to, autouv: 0, color,
    origin: [8, 8, 8], faces, type: 'cube', uuid: randomUUID(),
  };
}
const elements = [
  cube('body', [0, 2, 0], [16, 16, 16], sides(AT.side, AT.side, 0, {
    up: face(px(16, [0, 0, 16, 16]), 3), down: face(AT.bottom, 0),
  }), 1),
  cube('lens_bezel', [2, 1, 2], [14, 2, 14], sides(AT.frameSide, AT.frameSide, 0, {
    up: face(AT.frameDown, 0), down: face(AT.frameDown, 0),
  }), 3),
  // Name kept for code/tests: bottom centre (8, 0, 8) is the raycast origin.
  cube('downward_antenna', [4, 0, 4], [12, 1, 12], sides(LN.side, LN.side, 2, {
    up: face(LN.down, 2), down: face(LN.down, 2),
  }), 6),
];
const groups = [
  { name: 'body', origin: [8, 8, 8], color: 0, uuid: randomUUID(), export: true, mirror_uv: false,
    isOpen: true, locked: false, visibility: true, autouv: 0, children: [elements[0].uuid] },
  { name: 'lens_assembly', origin: [8, 0, 8], color: 6, uuid: randomUUID(), export: true, mirror_uv: false,
    isOpen: true, locked: false, visibility: true, autouv: 0, children: elements.slice(1).map(e => e.uuid) },
];
const project = {
  meta: { format_version: '5.0', model_format: 'java_block', box_uv: false },
  name: 'radar_altimeter', parent: '', java_block_version: '1.9', ambientocclusion: true,
  front_gui_light: false, visible_box: [1, 1, 0], variable_placeholders: '', multi_file_ruleset: '',
  variable_placeholder_buttons: [], unhandled_root_fields: {},
  resolution: { width: 32, height: 32 }, elements,
  groups: groups.map(({ children, ...g }) => g),
  outliner: groups.map(g => ({ uuid: g.uuid, isOpen: true, children: g.children })),
  textures,
};
writeFileSync(new URL('radar_altimeter.bbmodel', import.meta.url), JSON.stringify(project));
console.log(`Wrote radar_altimeter.bbmodel: ${elements.length} cubes, ${textures.length} textures.`);
