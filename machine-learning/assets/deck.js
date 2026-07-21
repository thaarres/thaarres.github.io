/* ==========================================================================
   Shared slide-deck engine — Machine Learning course
   Every chapter defines `window.SLIDES` (array) and calls `initDeck(meta)`.
   Edit THIS file to change behavior for every chapter at once.
   Edit a chapter's slides.js to change that chapter's content.
   ========================================================================== */

(function () {
  "use strict";

  function initDeck(meta) {
    const slides = window.SLIDES || [];
    const state = { i: 0, frag: 0 };

    // ---- one-time DOM scaffold ----------------------------------------
    const stage = document.createElement("div");
    stage.className = "stage";
    const frame = document.createElement("div");
    frame.className = "slide-frame";
    stage.appendChild(frame);

    const header = document.createElement("div");
    header.className = "slide-header";
    header.innerHTML = `
      <a class="crumb" href="${meta.tocHref || "../../"}">${meta.course || "Course"} · ${meta.chapterTitle || ""}</a>
      <div class="counter-wrap">
        <div class="counter"><span data-cur></span> / <span data-total></span></div>
        <div class="counter-label">Chapter ${meta.chapterNum || "?"} of ${meta.chapterTotal || "?"}</div>
      </div>`;
    frame.appendChild(header);

    const body = document.createElement("div");
    body.className = "slide-body";
    frame.appendChild(body);

    const progress = document.createElement("div");
    progress.className = "progress";
    progress.innerHTML = `<div class="progress-fill" data-fill></div>`;
    frame.appendChild(progress);

    const hint = document.createElement("div");
    hint.className = "nav-hint";
    hint.textContent = "← →  navigate";
    frame.appendChild(hint);

    document.body.appendChild(stage);

    const curEl = header.querySelector("[data-cur]");
    const totalEl = header.querySelector("[data-total]");
    const fillEl = progress.querySelector("[data-fill]");
    totalEl.textContent = slides.length;

    // ---- rendering -------------------------------------------------------
    function renderSlide(idx, fragStep) {
      const s = slides[idx];
      if (!s) return;

      frame.classList.toggle("dark", s.type === "section" || s.dark === true);
      curEl.textContent = idx + 1;
      fillEl.style.width = ((idx + 1) / slides.length) * 100 + "%";

      let html = "";
      switch (s.type) {
        case "title":
          html = `
            <div class="type-title">
              <h1>${s.title || ""}</h1>
              ${s.subtitle ? `<div class="subtitle">${s.subtitle}</div>` : ""}
              ${s.byline ? `<div class="byline">${s.byline.join(" &middot; ")}</div>` : ""}
            </div>`;
          break;

        case "section":
          html = `
            <div class="type-section">
              <div>
                ${s.kicker ? `<span class="kicker">${s.kicker}</span>` : ""}
                <h2>${s.title || ""}</h2>
              </div>
            </div>`;
          break;

        case "content": {
          const items = s.bullets || [];
          const showAll = s.reveal === false;
          html = `
            <div class="type-content">
              ${s.title ? `<h3 class="content-title">${s.title}</h3>` : ""}
              ${s.text ? `<p class="body-text">${s.text}</p>` : ""}
              ${items.length ? `<ol class="bullets">${items
                .map((b, n) => {
                  const hidden = !showAll && n > fragStep;
                  return `<li class="${hidden ? "fragment-hidden" : ""}"><span class="idx">${String(n + 1).padStart(2, "0")}</span><span>${b}</span></li>`;
                })
                .join("")}</ol>` : ""}
            </div>`;
          break;
        }

        case "code":
          html = `
            <div class="type-code">
              ${s.title ? `<h3 class="content-title">${s.title}</h3>` : ""}
              <pre><code class="language-${s.lang || "python"}">${escapeHtml(s.code || "")}</code></pre>
            </div>`;
          break;

        case "quote":
          html = `
            <div class="type-quote">
              <blockquote>${s.text || ""}</blockquote>
              ${s.cite ? `<cite>${s.cite}</cite>` : ""}
            </div>`;
          break;

        case "image":
          html = `
            <div class="type-image">
              ${s.title ? `<h3 class="content-title">${s.title}</h3>` : ""}
              <div class="img-wrap"><img src="${s.src}" alt="${s.alt || ""}"></div>
              ${s.caption ? `<div class="caption">${s.caption}</div>` : ""}
            </div>`;
          break;

        case "figure":
          // Full-slide diagram. `svg` is raw, trusted SVG markup authored by us
          // (no external image asset needed — keeps chapters self-contained).
          html = `
            <div class="type-figure">
              ${s.title ? `<h3 class="content-title">${s.title}</h3>` : ""}
              <div class="figure-wrap">${s.svg || ""}</div>
              ${s.caption ? `<div class="caption">${s.caption}</div>` : ""}
            </div>`;
          break;

        case "split":
          // Text/bullets on one side, a diagram (or code) on the other.
          // s.left / s.right: { title, text, bullets, svg, code, lang }
          html = `
            <div class="type-split">
              <div class="split-text">${renderPane(s.left)}</div>
              <div class="split-figure">${renderPane(s.right)}</div>
            </div>`;
          break;

        default:
          html = `<div class="body-text">Unknown slide type: ${s.type}</div>`;
      }

      body.classList.add("switching");
      setTimeout(() => {
        body.innerHTML = html;
        body.classList.remove("switching");
        if (window.hljs) body.querySelectorAll("pre code").forEach((el) => window.hljs.highlightElement(el));
        if (window.renderMathInElement) {
          window.renderMathInElement(body, {
            delimiters: [
              { left: "$$", right: "$$", display: true },
              { left: "$", right: "$", display: false },
            ],
          });
        }
      }, 120);

      history.replaceState(null, "", "#" + (idx + 1));
    }

    function renderPane(p) {
      // Small helper for `split` slides: renders one side (text/bullets/svg/code).
      if (!p) return "";
      if (p.svg) return p.svg;
      let out = "";
      if (p.title) out += `<h3 class="content-title">${p.title}</h3>`;
      if (p.text) out += `<p class="body-text">${p.text}</p>`;
      if (p.bullets) out += `<ol class="bullets">${p.bullets.map((b, n) => `<li><span class="idx">${String(n + 1).padStart(2, "0")}</span><span>${b}</span></li>`).join("")}</ol>`;
      if (p.code) out += `<pre><code class="language-${p.lang || "python"}">${escapeHtml(p.code)}</code></pre>`;
      return out;
    }

    function escapeHtml(str) {
      return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
    }

    function fragCount(idx) {
      const s = slides[idx];
      if (s && s.type === "content" && s.reveal !== false && s.bullets) return s.bullets.length;
      return 1;
    }

    function next() {
      const max = fragCount(state.i) - 1;
      if (state.frag < max) {
        state.frag++;
        renderSlide(state.i, state.frag);
      } else if (state.i < slides.length - 1) {
        state.i++;
        state.frag = 0;
        renderSlide(state.i, 0);
      }
    }

    function prev() {
      if (state.i > 0) {
        state.i--;
        state.frag = fragCount(state.i) - 1; // land on fully-revealed previous slide
        renderSlide(state.i, state.frag);
      }
    }

    function goto(idx) {
      state.i = Math.max(0, Math.min(slides.length - 1, idx));
      state.frag = 0;
      renderSlide(state.i, 0);
    }

    // ---- input -------------------------------------------------------
    window.addEventListener("keydown", (e) => {
      if (["ArrowRight", "PageDown", " "].includes(e.key)) { e.preventDefault(); next(); }
      else if (["ArrowLeft", "PageUp"].includes(e.key)) { e.preventDefault(); prev(); }
      else if (e.key === "Home") { e.preventDefault(); goto(0); }
      else if (e.key === "End") { e.preventDefault(); goto(slides.length - 1); }
    });

    frame.addEventListener("click", (e) => {
      const rect = frame.getBoundingClientRect();
      const x = e.clientX - rect.left;
      if (x < rect.width * 0.3) prev();
      else next();
    });

    // deep-link on load, e.g. chapter/#5
    const startHash = parseInt(location.hash.replace("#", ""), 10);
    const startIdx = Number.isFinite(startHash) ? startHash - 1 : 0;
    goto(startIdx);
  }

  window.initDeck = initDeck;
})();
