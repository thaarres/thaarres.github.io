# edge-ai-collider-physics

Browser-native 16:9 presentation for **Machine Learning at the Edge of Scale and Speed**.

## Run locally

No build system is required.

```bash
cd edge-ai-collider-physics
python3 -m http.server 8000
```

Open `http://localhost:8000`.

You can also open `index.html` directly in most browsers.

## Present

- `→`, `Space`, `Enter`, `PageDown`: next
- `←`, `PageUp`, `Backspace`: previous
- `Home` / `End`: first / last
- `F`: fullscreen
- Click right ~70% of screen: next
- Click left ~30%: previous
- Each scene has a stable hash URL, e.g. `#14`

## Publish

Copy this entire directory into:

`thaarres.github.io/edge-ai-collider-physics/`

Then commit and push to the `master` branch. It should appear at:

`https://thaarres.github.io/edge-ai-collider-physics/`

## Design

The deck deliberately uses no external JS framework, webfont, image CDN, or runtime dependency. This makes it reliable on conference Wi-Fi and easy to cache/offline.

The current version establishes the visual narrative and covers:
1. data scale
2. HL-LHC precision physics
3. 25 ns / 40 MHz
4. CMS trigger pipeline
5. CMS 40 MHz Level-1 scouting
6. QKeras / HGQ / hls4ml
7. ECON-T and SmartPixels
8. FCC-ee transition
9. ePIC triggerless streaming and distributed inference
10. future trajectory

## Scientific-source notes

The presentation content is based on the supplied PowerPoint and PDFs, plus the public ATLAS/CMS HL-LHC projections and ECON-T material.

The FCC-ee scene is intentionally qualitative in this first web version because the exact vertex-detector rate assumptions should be copied from the supplied fastML ESPP source rather than guessed.

See `SOURCES.md`.
