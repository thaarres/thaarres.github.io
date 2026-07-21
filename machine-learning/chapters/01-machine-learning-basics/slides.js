/* ==========================================================================
   Chapter 1 — Machine Learning Basics
   Edit this array to change the deck. This chapter also doubles as a
   reference for every slide `type` the shared engine supports:

     title    { type:"title", title, subtitle, byline:[...] }
     section  { type:"section", kicker, title }              // dark divider
     content  { type:"content", title, text, bullets:[...], reveal:true|false }
     code     { type:"code", title, lang, code }
     quote    { type:"quote", text, cite }
     image    { type:"image", title, src, alt, caption }      // external image file
     figure   { type:"figure", title, svg:"<svg>...</svg>", caption }   // inline SVG diagram
     split    { type:"split", left:{title,text,bullets,code,lang}, right:{svg} }  // two-column

   `bullets` reveal one-by-one on each right-arrow press unless
   `reveal:false` is set (then they all show at once).
   ========================================================================== */

window.SLIDES = [
  {
    type: "title",
    title: "Machine Learning Basics",
    subtitle: "Chapter 1",
    byline: ["Thea Aarrestad", "Machine Learning", "ETH Zürich"],
  },
  {
    type: "section",
    kicker: "Warm-up",
    title: "What is learning, really?",
  },
  {
    type: "content",
    title: "By the end of this chapter",
    bullets: [
      "Supervised vs. unsupervised vs. reinforcement learning",
      "Loss functions and what they optimize for",
      "Train / validation / test splits, and why they matter",
      "Overfitting, underfitting, and the bias–variance tradeoff",
    ],
  },
  {
    type: "quote",
    text: "All models are wrong, but some are useful.",
    cite: "George Box",
  },
  {
    type: "code",
    title: "A one-neuron model",
    lang: "python",
    code: `import numpy as np

def predict(x, w, b):
    return w * x + b

def loss(y_pred, y_true):
    return np.mean((y_pred - y_true) ** 2)
`,
  },
  {
    type: "content",
    title: "Coming soon",
    text: "Replace the slides above with real chapter content — this file is plain data, no HTML editing required.",
    reveal: false,
  },
];
