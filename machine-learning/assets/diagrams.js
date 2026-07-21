/* ==========================================================================
   Diagram helpers — generate small inline-SVG figures for slides, so
   chapters don't need external image files for simple schematic drawings.

   Used via `figure` / `split` slides, e.g.:
     { type:"figure", svg: DeckDiagrams.mlp({ layers:[6,4,2,4,6], highlight:[2] }) }

   Styling comes from the `.dg-*` utility classes in deck.css, so diagrams
   automatically match the deck's colors — no inline styling needed here.
   Reused across chapters (e.g. "Architectures: MLPs to Transformers").
   ========================================================================== */

(function (root) {
  "use strict";

  // ---- layered network diagram (MLP / autoencoder / any feed-forward net) --
  // layers: [6,4,2,4,6]  -> node counts per layer, left to right
  // highlight: indices of layers to draw as filled/accent (e.g. the bottleneck)
  function mlp({ layers, width = 520, height = 220, highlight = [] } = {}) {
    const n = layers.length;
    const xStep = width / (n + 1);
    const usableH = height * 0.78;

    const positions = layers.map((count, li) => {
      const x = xStep * (li + 1);
      const ys = [];
      for (let i = 0; i < count; i++) {
        const y = count === 1 ? height / 2 : height / 2 - (usableH / 2) + (i * usableH) / (count - 1);
        ys.push(y);
      }
      return { x, ys };
    });

    let edges = "";
    for (let li = 0; li < n - 1; li++) {
      const a = positions[li], b = positions[li + 1];
      for (const ay of a.ys) for (const by of b.ys) {
        edges += `<line class="dg-edge" x1="${a.x}" y1="${ay}" x2="${b.x}" y2="${by}"/>`;
      }
    }

    let nodes = "";
    const r = Math.max(5, Math.min(13, usableH / (Math.max(...layers) * 2.4)));
    positions.forEach((p, li) => {
      const cls = highlight.includes(li) ? "dg-node-hl" : "dg-node";
      p.ys.forEach((y) => { nodes += `<circle class="${cls}" cx="${p.x}" cy="${y}" r="${r}"/>`; });
    });

    return `<svg viewBox="0 0 ${width} ${height}" xmlns="http://www.w3.org/2000/svg">${edges}${nodes}</svg>`;
  }

  // ---- generic 2D scatter: clusters, centroids, an axis line, projections --
  // points: [[x,y,clusterIndex?], ...] in viewBox coordinate space
  // centroids: [[x,y], ...]
  // axis: {x1,y1,x2,y2} draws a line (e.g. a principal axis)
  // projections: [[x,y], ...] draws a dashed drop-line from each point to `axis`
  function scatter({
    width = 420, height = 300, points = [], centroids = [],
    axis = null, projections = [], pointClasses = ["dg-c1", "dg-c2", "dg-c3"],
  } = {}) {
    let out = "";

    if (axis) {
      out += `<line class="dg-axis" x1="${axis.x1}" y1="${axis.y1}" x2="${axis.x2}" y2="${axis.y2}"/>`;
    }

    if (projections.length && axis) {
      const { x1, y1, x2, y2 } = axis;
      const dx = x2 - x1, dy = y2 - y1;
      const len2 = dx * dx + dy * dy;
      projections.forEach(([px, py]) => {
        const t = ((px - x1) * dx + (py - y1) * dy) / len2;
        const fx = x1 + t * dx, fy = y1 + t * dy;
        out += `<line class="dg-proj" x1="${px}" y1="${py}" x2="${fx}" y2="${fy}"/>`;
        out += `<circle class="dg-proj-pt" cx="${fx}" cy="${fy}" r="3"/>`;
      });
    }

    points.forEach(([x, y, c]) => {
      const cls = c === undefined ? "dg-point" : pointClasses[c % pointClasses.length];
      out += `<circle class="${cls}" cx="${x}" cy="${y}" r="5"/>`;
    });

    centroids.forEach(([x, y], i) => {
      const cls = pointClasses[i % pointClasses.length];
      out += `<circle class="dg-centroid ${cls}-stroke" cx="${x}" cy="${y}" r="9"/>`;
      out += `<line class="dg-centroid-cross" x1="${x - 6}" y1="${y}" x2="${x + 6}" y2="${y}"/>`;
      out += `<line class="dg-centroid-cross" x1="${x}" y1="${y - 6}" x2="${x}" y2="${y + 6}"/>`;
    });

    return `<svg viewBox="0 0 ${width} ${height}" xmlns="http://www.w3.org/2000/svg">${out}</svg>`;
  }

  root.DeckDiagrams = { mlp, scatter };
})(typeof window !== "undefined" ? window : globalThis);
