// Avionics Wire for documentation renders: a port of WireGeometry, WireAttachPoints and the tube built by
// WireRenderer (1x1 texel square section, frame carried along the wire, strip texture repeating along v).
// Keep the constants in step with the Java classes; WireGeometryTest is the reference.
import { texture } from './render-scene.mjs';
import { NS } from './bbtools.mjs';

const STRAIGHT = 1 / 16, LEAD = 2 / 16, LEAD_SWEEP = 3 / 16, BEND = 2 / 16;
const SAG_PER_BLOCK = 0.015, MAX_SAG = 0.3, STEP = 0.25, MAX_STEPS = 64, BEND_STEPS = 4;
const CONNECTOR_MOUTH = 4, SUPPORT_WIRE = 2;

const add = (a, b) => [a[0] + b[0], a[1] + b[1], a[2] + b[2]];
const sub = (a, b) => [a[0] - b[0], a[1] - b[1], a[2] - b[2]];
const mul = (a, k) => [a[0] * k, a[1] * k, a[2] * k];
const len = a => Math.hypot(...a);
const norm = a => mul(a, 1 / (len(a) || 1));
const dot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
const lerp = (a, b, t) => add(a, mul(sub(b, a), t));

export const NORMALS = { up: [0, 1, 0], down: [0, -1, 0], north: [0, 0, -1], south: [0, 0, 1], west: [-1, 0, 0], east: [1, 0, 0] };
const alongNormal = (facing, pixels) => add([0.5, 0.5, 0.5], mul(NORMALS[facing], pixels / 16 - 0.5));

/** Wire nodes from route anchors: { at: [x,y,z], facing, connector: bool, offset?: socket shift in blocks }. */
export function wireNodes(anchors) {
  return anchors.map(a => ({
    point: add(add(a.at, alongNormal(a.facing, a.connector ? CONNECTOR_MOUTH : SUPPORT_WIRE)), a.offset ?? [0, 0, 0]),
    exit: a.connector ? NORMALS[a.facing] : null,
  }));
}

const pathLength = points => points.slice(1).reduce((sum, p, i) => sum + len(sub(p, points[i])), 0);
const sag = (a, b) => Math.min(MAX_SAG, SAG_PER_BLOCK * Math.hypot(b[0] - a[0], b[2] - a[2]));
function sagSpan(a, b) {
  const depth = sag(a, b), length = len(sub(b, a));
  const steps = depth < 1e-4 ? 1 : Math.max(2, Math.min(MAX_STEPS, Math.ceil(length / STEP)));
  return Array.from({ length: steps + 1 }, (_, i) => { const t = i / steps; return sub(lerp(a, b, t), [0, 4 * depth * t * (1 - t), 0]); });
}
function trimStart(points, distance) {
  let remaining = distance;
  for (let i = 1; i < points.length; i++) {
    const segment = len(sub(points[i], points[i - 1]));
    if (segment >= remaining && segment > 0) return [lerp(points[i - 1], points[i], remaining / segment), ...points.slice(i)];
    remaining -= segment;
  }
  return [points.at(-1), points.at(-1)];
}
const trimEnd = (points, distance) => trimStart([...points].reverse(), distance).reverse();
const bezier = (a, c, b, t) => add(add(mul(a, (1 - t) ** 2), mul(c, 2 * (1 - t) * t)), mul(b, t * t));

/** Centre line, as WireGeometry.centreLine. */
export function centreLine(nodes) {
  const controls = [];
  nodes.forEach((node, i) => {
    const lead = node.exit ? add(node.point, mul(norm(node.exit), LEAD)) : null;
    if (lead && i === nodes.length - 1) controls.push(lead);
    controls.push(node.point);
    if (lead && i === 0) controls.push(lead);
  });
  const startLead = !!nodes[0].exit, endLead = !!nodes.at(-1).exit;
  const isLead = span => (startLead && span === 0) || (endLead && span === controls.length - 2);
  const spans = controls.slice(0, -1).map((c, i) => isLead(i) ? [c, controls[i + 1]] : sagSpan(c, controls[i + 1]));
  const result = [];
  let current = spans[0];
  for (let i = 1; i < spans.length; i++) {
    const next = spans[i], leadIn = isLead(i - 1), leadOut = isLead(i);
    let into, out;
    if (leadIn || leadOut) {
      const sweep = Math.min(LEAD_SWEEP, 0.45 * pathLength(leadIn ? next : current));
      into = leadIn ? LEAD - STRAIGHT : sweep;
      out = leadOut ? LEAD - STRAIGHT : sweep;
    } else {
      into = out = Math.min(BEND, 0.45 * Math.min(pathLength(current), pathLength(next)));
    }
    const incoming = trimEnd(current, into), outgoing = trimStart(next, out);
    result.push(...incoming.slice(0, -1));
    const from = incoming.at(-1), corner = controls[i], to = outgoing[0];
    for (let step = 0; step < BEND_STEPS; step++) result.push(bezier(from, corner, to, step / BEND_STEPS));
    current = outgoing;
  }
  result.push(...current);
  return result.filter((p, i) => i === 0 || len(sub(p, result[i - 1])) > 1e-5);
}

/** Tube quads around a centre line: 1 texel square section, strip columns 1, 5, 9, 13 (side, bottom, side, top). */
export function tubeQuads(points, { half = 0.5 / 16 } = {}) {
  const strip = texture(`${NS}:entity/avionics_wire`);
  const frames = [];
  let up = null;
  points.forEach((p, i) => {
    const t = norm(sub(points[Math.min(i + 1, points.length - 1)], points[Math.max(i - 1, 0)]));
    if (!up) up = Math.abs(t[1]) > 0.99 ? [1, 0, 0] : [0, 1, 0];
    up = norm(sub(up, mul(t, dot(up, t)))); // carried along, no twist
    const side = norm(cross(t, up));
    frames.push({ up: cross(side, t), side });
  });
  const quads = [];
  let v = 0;
  for (let i = 0; i < points.length - 1; i++) {
    const a = points[i], b = points[i + 1], fa = frames[i], fb = frames[i + 1];
    const vb = v + len(sub(b, a));
    // Faces: [normal, lateral, strip column]; lateral x tangent = normal.
    const faces = [
      [f => f.up, f => f.side, 13], [f => f.side, f => mul(f.up, -1), 1],
      [f => mul(f.up, -1), f => mul(f.side, -1), 5], [f => mul(f.side, -1), f => f.up, 9],
    ];
    for (const [n, l, column] of faces) {
      const corner = (p, f, s) => add(add(p, mul(n(f), half)), mul(l(f), s * half));
      quads.push({
        corners: [corner(a, fa, -1), corner(b, fb, -1), corner(b, fb, 1), corner(a, fa, 1)],
        uvs: [[column / 16, v], [column / 16, vb], [(column + 1) / 16, vb], [(column + 1) / 16, v]],
        texture: strip, wrapV: true,
      });
    }
    v = vb;
  }
  return quads;
}

export const wireQuads = anchors => tubeQuads(centreLine(wireNodes(anchors)));
