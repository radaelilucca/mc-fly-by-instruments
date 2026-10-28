// Bootstrap of the v2.1 FMS Compass (item fms_navigator): a round pocket compass in the FMS module's
// language. Like the vanilla compass, it reads as a disc with a needle; like the FMS, its face is an
// industrial iron rim, a brass bezel and a dark lens with the white heading index. Round = stepped:
// five integer cuboids whose outline matches the painted circle pixel for pixel. Brass lug on the
// north edge. Lies flat: rests on Y 0, disc centred on X/Z 8. Writes fms_navigator.bbmodel; do NOT
// rerun over a refined source. Linked variant: a second atlas (fms_navigator_linked) where only the
// needle head and the lens glint change, like the sensors' _powered atlas (guide §8.5).
import { texture, face, cube, group, writeProject, px, canvas, blit, checkMap } from '../_tools/bbtools.mjs';

// --- Own texture (16 x 16, pixels = model units) ---------------------------------------------
// Face 12 x 12 (x 2..14, z 2..14; '.' lies outside the disc). Iron rim lit top/left, brass bezel,
// dark lens with a blue glint, white index at north, needle from south-west (iron tail) to
// north-east (brass head), as the vanilla compass needle at rest.
const FACE = checkMap('face', [
  '...vvvvvv...',
  '.vv777777qn.',
  '.v7LLKKLL4n.',
  'v7MMMMMM6M4n',
  'v7MYMMM6MM4n',
  'v6MMMM5MMM4n',
  'v6MMMWMMMM3n',
  'q5MMWMMMMM3n',
  'q5MvMMMMMM2n',
  '.q4MMMMMM3n.',
  '.qn333333nn.',
  '...nnnnnn...',
], 12, 12);
// Linked to an FMS module: the brass needle head lights up (tip brightest) and the lens glint grows to
// two pixels, as if the lens were lit. Same pixels everywhere else; the outline never changes.
const LINKED_FACE = checkMap('linkedFace', [
  '...vvvvvv...',
  '.vv777777qn.',
  '.v7LLKKLL4n.',
  'v7MYMMMM8M4n',
  'v7MYMMM7MM4n',
  'v6MMMM6MMM4n',
  'v6MMMWMMMM3n',
  'q5MMWMMMMM3n',
  'q5MvMMMMMM2n',
  '.q4MMMMMM3n.',
  '.qn333333nn.',
  '...nnnnnn...',
], 12, 12);
const mapsFor = face => ({
  face: face.map(row => row.replaceAll('.', 'n')),
  lugSide: ['76', '43'],
  lugTop: ['87', '65'],
});
const UV = { face: [0, 0, 12, 12], lugSide: [12, 0, 14, 2], lugTop: [14, 0, 16, 2] };
function atlas(face) {
  const img = canvas(16, 16, 'n');
  for (const [name, map] of Object.entries(mapsFor(face))) {
    const [u0, v0, u1, v1] = UV[name];
    blit(img, checkMap(name, map, u1 - u0, v1 - v0), u0, v0);
  }
  return img;
}

// Create's assets are All Rights Reserved: reference them by id, never embed their pixels in this
// repo. For the Blockbench preview, keep a local copy in reference/create-textures/ (gitignored).
const textures = [
  texture('fms_navigator', 16, { source: atlas(FACE) }),                              // 0
  texture('industrial_iron_block', 16, { namespace: 'create', particle: true,
    relativePath: '../../reference/create-textures/industrial_iron_block.png' }),      // 1
  texture('fms_navigator_linked', 16, { source: atlas(LINKED_FACE) }),               // 2 (variant only)
];
const own = region => face(px(16, region), 0);
const iron = region => face(px(16, region), 1);
const OFF = face([0, 0, 0, 0], null);

// One slab of the stepped disc: top = its own crop of the painted face, rim sides and bottom =
// crops of the Create texture (its dark edge rows 14..16 on the sides, as on the FMS housing).
// `inner` lists the faces that sit against a neighbouring slab: left disabled.
function slab(name, [x0, z0], [x1, z1], inner = []) {
  const faces = {
    up: own([x0 - 2, z0 - 2, x1 - 2, z1 - 2]), down: iron([x0, z0, x1, z1]),
    north: iron([x0, 14, x1, 16]), south: iron([x0, 14, x1, 16]),
    east: iron([z0, 14, z1, 16]), west: iron([z0, 14, z1, 16]),
  };
  for (const side of inner) faces[side] = OFF;
  return cube(name, [x0, 0, z0], [x1, 2, z1], faces, 0);
}
const elements = [
  slab('disc', [2, 5], [14, 11]),
  slab('disc_north', [3, 3], [13, 5], ['south']),
  slab('disc_south', [3, 11], [13, 13], ['north']),
  slab('disc_north_cap', [5, 2], [11, 3], ['south']),
  slab('disc_south_cap', [5, 13], [11, 14], ['north']),
  // South face sits against the disc: left disabled.
  cube('lug', [7, 0, 0], [9, 2, 2], { north: own(UV.lugSide), east: own(UV.lugSide), west: own(UV.lugSide),
    up: own(UV.lugTop), down: own(UV.lugTop) }, 3),
];
const groups = [group('case', [8, 0, 8], elements.slice(0, 5)), group('lug', [8, 1, 1], elements.slice(5), 3)];
writeProject(new URL('fms_navigator.bbmodel', import.meta.url), { name: 'fms_navigator', elements, groups, textures });
console.log(`Wrote fms_navigator.bbmodel: ${elements.length} cubes, ${textures.length} textures.`);
