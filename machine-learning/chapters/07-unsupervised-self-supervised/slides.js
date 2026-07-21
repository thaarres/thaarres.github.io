/* ==========================================================================
   Chapter 7 — Unsupervised and Self-Supervised Architectures
   Part 1: anomaly detection, clustering, PCA, autoencoders, VAEs, and their
   use across particle physics (overdensity search, real-time triggering,
   detector monitoring). (Contrastive / masked self-supervised pre-training
   to follow in a later pass.)

   Sources adapted for teaching from:
   - T. Aarrestad, ETH anomaly-detection lecture (AD.pdf)
   - V. Belis, P. Odagiu, T.K. Aarrestad, "Machine learning for anomaly
     detection in particle physics", Reviews in Physics 12 (2024) 100091
     (open access, CC BY-NC-ND) — Fig. 3 reused with the authors' own work.
   Quantum anomaly detection (Section 6 of the review) is intentionally
   skipped here.
   ========================================================================== */

window.SLIDES = [
  {
    type: "title",
    title: "Unsupervised and Self-Supervised Architectures",
    subtitle: "Chapter 7",
    byline: ["Thea Aarrestad", "Machine Learning", "ETH Zürich"],
  },

  // ==================================================================
  // PART 1 — FOUNDATIONS
  // ==================================================================
  {
    type: "section",
    kicker: "Warm-up",
    title: "What is anomaly detection?",
  },
  {
    type: "content",
    title: "What is anomaly detection?",
    bullets: [
      "We rarely have labeled examples of the thing we're actually looking for",
      "Idea: learn what “normal” looks like from data, then flag anything that doesn't fit",
      "In practice: learn (an approximation of) the probability density p(x) of the background, and look for datapoints that sit in its tails",
      "Core to rare-event search: fraud detection, network intrusion, new physics at the LHC…",
    ],
  },
  {
    type: "content",
    title: "Types of models",
    bullets: [
      "Classification — predict a label from a finite set {1, …, C}; trained on (input, label) pairs",
      "Regression — predict a continuous-valued output; trained on (input, value) pairs",
      "Density modeling — learn p(x) directly from unlabeled data; no ground truth needed at all",
    ],
  },
  {
    type: "content",
    title: "Types of learning",
    bullets: [
      "Supervised — every example has a ground-truth label",
      "Semi-supervised — a few labels, plus pseudo-labels for the rest",
      "Weakly-supervised — labels exist, but are noisy or imprecise",
      "Unsupervised — only features, no labels at all",
      "Self-supervised — labels are constructed automatically from the data itself (e.g. an autoencoder's target is its own input)",
    ],
  },
  {
    type: "content",
    title: "Why particle physics leans on this",
    text: "Collision data can't be labeled the way images or text can — every event is secretly a signal process, a background process, or a mixture of both, and we don't know which.",
    bullets: [
      "Simulation provides a labeled surrogate — but simulation never perfectly matches real data",
      "Weakly-, self- and unsupervised methods let us train directly on real, unlabeled data",
      "They also reduce model-dependence: no need to assume what the signal looks like in advance",
    ],
  },

  {
    type: "section",
    kicker: "Two flavors of anomaly detection",
    title: "Outlier vs. overdensity",
  },
  {
    type: "split",
    left: {
      title: "Outlier detection",
      bullets: [
        "Find individual out-of-distribution datapoints (“nonresonant”)",
        "Often built on (variational) autoencoders",
        "Fast to evaluate — good for real-time triggering / selection",
      ],
    },
    right: {
      title: "Finding overdensities",
      bullets: [
        "Find a bump of similar anomalous events on top of a smooth background (“resonant”)",
        "Built on density-estimation / weakly-supervised methods",
        "Needs the full dataset at once — good for offline analysis",
      ],
    },
  },

  // ==================================================================
  // PART 2 — OVERDENSITY ESTIMATION (weakly-supervised bump hunting)
  // ==================================================================
  {
    type: "section",
    kicker: "Model-agnostic new physics searches",
    title: "Overdensity estimation",
  },
  {
    type: "content",
    title: "The ideal (and impossible) discriminator",
    text: "$$R(x) = \\dfrac{p_{\\text{data}}(x)}{p_{\\text{bg}}(x)}$$",
    bullets: [
      "If we could learn this likelihood ratio exactly, it would be the most powerful model-agnostic anomaly detector — by the Neyman–Pearson lemma",
      "p_data mixes background and (possibly) signal: p_data = (1−ε)·p_bg + ε·p_sig",
      "We can't compute this directly — but we can approximate it by training a classifier",
    ],
    reveal: false,
  },
  {
    type: "content",
    title: "CWoLa — Classification Without Labels",
    bullets: [
      "Build two mixed samples with different signal fractions instead of clean signal/background labels",
      "Classic setup: a “signal region” near a candidate resonance mass, vs. “sideband” regions next to it",
      "Lemma: the classifier that best separates the two mixed samples is also the best signal-vs-background classifier",
      "Deployed in an ATLAS search for generic dijet resonances — the first model-agnostic search built on weak supervision",
      "Caveat: only works for narrow resonances, and features used must be uncorrelated with the resonance mass (or the background gets “sculpted”)",
    ],
  },
  {
    type: "content",
    title: "Interpolating the background: ANODE, CATHODE, CURTAINS",
    bullets: [
      "ANODE — fit a conditional density estimator to the sidebands, interpolate it into the signal region, and compare to the observed density there",
      "CATHODE — same idea, but sample synthetic background events from the interpolated density, then train a classifier against the real signal-region data",
      "CURTAINS — instead of density estimation, learn an invertible mapping that morphs sideband events directly into signal-region-like events",
      "All three assume the sidebands are signal-free and statistically similar to the signal region",
    ],
  },
  {
    type: "content",
    title: "Normalizing flows, briefly",
    text: "A normalizing flow learns a chain of invertible transformations that turn a simple distribution (usually a Gaussian) into a complex one that matches the data — invertibility is what makes density evaluation and sampling both tractable.",
    bullets: [
      "This is the workhorse density estimator behind ANODE and CATHODE",
      "Diffusion models are now also being explored as a more expressive alternative for both overdensity and outlier detection",
    ],
    reveal: false,
  },
  {
    type: "content",
    title: "Simulation-assisted: SALAD & FETA",
    bullets: [
      "SALAD — train a reweighting function so simulation matches data in the sidebands, then interpolate that reweighting into the signal region",
      "FETA — a hybrid: a normalizing flow learns to map simulation to data in the sidebands, then applies that same map to simulation in the signal region",
      "Both trade a dependence on simulation for better control over the background model",
    ],
  },

  // ==================================================================
  // PART 3 — UNSUPERVISED LEARNING TOOLKIT
  // ==================================================================
  {
    type: "section",
    kicker: "Unsupervised learning",
    title: "Finding structure without labels",
  },
  {
    type: "content",
    title: "Unsupervised learning",
    bullets: [
      "Given: unlabeled data only",
      "Goal: find hidden structure — clusters, a compact representation, or the full distribution p(x)",
      "Two workhorses: clustering (K-means, hierarchical) and dimensionality reduction (PCA, autoencoders, t-SNE)",
    ],
  },
  {
    type: "figure",
    title: "K-means (centroid) clustering",
    svg: DeckDiagrams.scatter({
      width: 400, height: 280,
      points: [
        [60,60,0],[75,50,0],[50,80,0],[85,90,0],[65,100,0],[90,65,0],
        [320,65,1],[340,55,1],[305,90,1],[350,85,1],[330,105,1],[300,55,1],
        [190,215,2],[210,205,2],[180,240,2],[220,245,2],[200,255,2],[230,220,2],
      ],
      centroids: [[70,74,0],[324,77,1],[205,230,2]],
    }),
    caption: "Assign each point to its nearest centroid, recompute centroids as the cluster mean, repeat until stable.",
  },
  {
    type: "content",
    title: "K-means, briefly",
    bullets: [
      "Pick K initial centroids (often at random)",
      "Assign every point to its nearest centroid",
      "Recompute each centroid as the mean of its assigned points",
      "Repeat until the assignments stop changing",
      "Works well when clusters are roughly spherical and the data is fairly linear",
    ],
  },
  {
    type: "content",
    title: "K-means as compression",
    text: "A neat side-effect of clustering: the centroids are a compact summary of the data, which you can exploit for compression.",
    bullets: [
      "A pixel stores 3 channels × 8 bits = 24 bits → 16.7M possible colors",
      "Run K-means with K = 16 on all the pixel colors in an image",
      "Store a 4-bit palette index per pixel instead: log₂(16) = 4 bits",
      "6× smaller on disk, at the cost of some color fidelity",
    ],
  },

  {
    type: "section",
    kicker: "Dimensionality reduction",
    title: "From projections to autoencoders",
  },
  {
    type: "split",
    left: {
      title: "PCA",
      bullets: [
        "Find the M orthogonal directions of maximum variance in N-dimensional data",
        "Represent each point by its projection onto those M principal directions",
        "Reconstruct using the mean along the discarded directions",
        "Reconstruction error is minimized by dropping the lowest-variance directions first",
      ],
    },
    right: {
      svg: DeckDiagrams.scatter({
        width: 380, height: 300,
        points: [
          [60,250],[90,230],[120,215],[100,190],[150,180],[170,160],[140,140],
          [190,130],[210,110],[180,90],[230,80],[250,60],[220,50],[270,40],
        ],
        axis: { x1: 40, y1: 270, x2: 300, y2: 30 },
        projections: [[100,190],[180,90],[220,50]],
      }),
    },
  },
  {
    type: "split",
    left: {
      title: "PCA as a neural network",
      bullets: [
        "Train a network to reproduce its own input, through a bottleneck",
        "That's exactly what PCA does",
        "The M bottleneck units span the same subspace as the first M principal components",
        "Not the same weights (axes can be rotated/skewed, not orthogonal, unequal variance) — but the same space",
        "SGD is a much less efficient way to get there than PCA — so why use a network at all?",
      ],
    },
    right: {
      svg: DeckDiagrams.mlp({ layers: [5, 2, 5], highlight: [1] }),
    },
  },

  // ==================================================================
  // PART 4 — AUTOENCODERS AS SELF-SUPERVISED OUTLIER DETECTORS
  // ==================================================================
  {
    type: "section",
    kicker: "Going non-linear",
    title: "What autoencoders can do that PCA can't",
  },
  {
    type: "split",
    left: {
      title: "Linear vs. non-linear",
      bullets: [
        "PCA finds the best-fitting hyperplane — a linear subspace",
        "Add non-linear activations before and after the bottleneck, and the network can learn a curved, non-linear manifold instead",
        "This is what makes an autoencoder strictly more general than PCA",
        "Encoder: input coordinates → coordinates on the manifold. Decoder: the inverse mapping",
      ],
    },
    right: {
      svg: DeckDiagrams.mlp({ layers: [6, 4, 2, 4, 6], highlight: [2] }),
    },
  },
  {
    type: "content",
    title: "Under-complete vs. over-complete",
    bullets: [
      "Under-complete — bottleneck is smaller than the input, forcing compression (the usual case)",
      "Over-complete — bottleneck is the same size or larger than the input; needs regularization (e.g. L1 / sparsity) so it doesn't just learn the identity function",
      "Either way, the goal is to force the network to learn something more useful than “copy the input”",
    ],
  },
  {
    type: "section",
    kicker: "Outlier detection",
    title: "Autoencoders as self-supervised anomaly detectors",
  },
  {
    type: "content",
    title: "Autoencoders are self-supervised",
    text: "$$L_{\\text{MSE}} = \\big(x - f(z,\\theta)\\big)^2$$",
    bullets: [
      "Self-supervised: a supervisory signal is generated from the data itself, no external labels needed",
      "For an autoencoder, the “label” for each example is simply the example itself — reconstruct x from its own compressed code z",
      "The reconstruction loss trains encoder and decoder jointly — the latent space and the reconstruction improve together",
      "Sits between unsupervised and supervised learning: no labels, but a well-defined per-example target",
    ],
    reveal: false,
  },
  {
    type: "image",
    title: "Anomaly detection with an autoencoder",
    src: "img/fig3-autoencoder.png",
    alt: "An autoencoder encodes the input to a lower-dimensional embedded space and decodes it to reconstruct the input; the reconstruction error (MSE) is used as an anomaly score.",
    caption: "Fig. 3, Belis, Odagiu & Aarrestad, Reviews in Physics 12 (2024) 100091 (CC BY-NC-ND).",
  },
  {
    type: "content",
    title: "Why this works (and when it doesn't)",
    bullets: [
      "The network learns to reconstruct events it sees often; rare events reconstruct poorly — high MSE ⇒ anomalous",
      "The bottleneck (embedded space, dimension k < n×m) prevents memorization — it must learn, not copy",
      "Autoencoders are built for event-by-event outliers, not overdensities — complementary to the weakly-supervised methods above",
      "Caveat: for data with nontrivial topology, high reconstruction error doesn't always mean “anomalous” — some normal points just reconstruct poorly",
    ],
  },
  {
    type: "content",
    title: "Other unsupervised outlier methods",
    bullets: [
      "One-class SVMs, Isolation Forests, Gaussian Mixture Models — classic unsupervised outlier detectors, less common in HEP than autoencoders",
      "UCluster — an attention-based GNN learns an embedding where similar events cluster together; the nearest cluster to an anomalous one gives a natural background estimate",
      "Tree-based autoencoders — a self-supervised decision-tree analogue of the neural autoencoder",
      "A recurring issue: anomaly scores can correlate with the variable you're searching over, sculpting the very bump you're hunting for — decorrelation techniques help",
    ],
  },

  // ==================================================================
  // PART 5 — VARIATIONAL AUTOENCODERS
  // ==================================================================
  {
    type: "section",
    kicker: "A probabilistic twist",
    title: "Variational Autoencoders",
  },
  {
    type: "content",
    title: "Why go variational?",
    bullets: [
      "A plain autoencoder's latent space has no structure — nearby codes don't have to mean anything, and there are “holes” that decode to garbage",
      "A VAE maps each input to a distribution over latent codes, not a single point",
      "That distribution is regularized to look like a standard normal, N(0, I) — so the latent space becomes smooth, continuous, and sample-able",
    ],
  },
  {
    type: "figure",
    title: "The reparameterization trick",
    svg: `<svg viewBox="0 0 600 260" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <marker id="vae-arrow" markerWidth="8" markerHeight="8" markerUnits="userSpaceOnUse" refX="6" refY="3" orient="auto">
          <path d="M0,0 L6,3 L0,6 Z" class="dg-point"/>
        </marker>
      </defs>
      <text x="2" y="134" class="dg-label">x</text>
      <line x1="18" y1="130" x2="48" y2="130" class="dg-axis" marker-end="url(#vae-arrow)"/>
      <rect x="50" y="90" width="110" height="80" rx="8" class="dg-node"/>
      <text x="105" y="134" text-anchor="middle" class="dg-label">encoder</text>
      <line x1="160" y1="112" x2="208" y2="86" class="dg-edge"/>
      <line x1="160" y1="150" x2="208" y2="174" class="dg-edge"/>
      <circle cx="222" cy="82" r="11" class="dg-node-hl"/>
      <text x="222" y="65" text-anchor="middle" class="dg-label">μ</text>
      <circle cx="222" cy="178" r="11" class="dg-node-hl"/>
      <text x="222" y="203" text-anchor="middle" class="dg-label">σ</text>
      <path d="M272,232 q10,-32 20,0 q10,-32 20,0" class="dg-edge" fill="none"/>
      <text x="300" y="250" text-anchor="middle" class="dg-label">ε ~ N(0,1)</text>
      <line x1="233" y1="86" x2="332" y2="124" class="dg-edge"/>
      <line x1="233" y1="174" x2="332" y2="136" class="dg-edge"/>
      <line x1="302" y1="222" x2="332" y2="132" class="dg-edge"/>
      <circle cx="346" cy="130" r="13" class="dg-node"/>
      <text x="346" y="110" text-anchor="middle" class="dg-label">z</text>
      <line x1="359" y1="130" x2="392" y2="130" class="dg-axis" marker-end="url(#vae-arrow)"/>
      <rect x="394" y="90" width="110" height="80" rx="8" class="dg-node"/>
      <text x="449" y="134" text-anchor="middle" class="dg-label">decoder</text>
      <line x1="504" y1="130" x2="540" y2="130" class="dg-axis" marker-end="url(#vae-arrow)"/>
      <text x="546" y="134" class="dg-label">x̂</text>
      <text x="346" y="245" text-anchor="middle" class="dg-label">z = μ + σ · ε</text>
    </svg>`,
    caption: "Sampling z directly isn't differentiable. Rewriting it as z = μ + σ·ε lets gradients flow back through μ and σ into the encoder.",
  },
  {
    type: "content",
    title: "The KL divergence term",
    text: "$$D_{KL}(\\vec\\mu,\\vec\\sigma) = -\\dfrac{1}{2}\\sum_i \\left(\\log \\sigma_i^2 - \\sigma_i^2 - \\mu_i^2 + 1\\right)$$",
    bullets: [
      "Closed-form KL divergence between the encoder's Gaussian N(μ, σ) and the standard normal prior N(0, I)",
      "Pulls every latent dimension i toward zero mean, unit variance — regularizing the code",
      "The KL divergence can be numerically unstable; the Wasserstein distance is a more robust alternative (→ Wasserstein Autoencoders)",
    ],
    reveal: false,
  },
  {
    type: "content",
    title: "The VAE loss",
    text: "$$\\mathcal{L} = (1-\\beta)\\,\\text{MSE}(\\hat{x}, x) \\;+\\; \\beta \\, D_{KL}(\\vec\\mu,\\vec\\sigma)$$",
    bullets: [
      "MSE term — how well the reconstruction x̂ matches the input x",
      "KL term — how close the latent distribution stays to the N(0, I) prior",
      "β ∈ [0, 1] trades off reconstruction fidelity against a tidy, generative latent space",
      "A weakly-supervised classifier learns the likelihood ratio directly; a VAE only ever learns the background density — an event is anomalous if it's unlikely under that learned latent distribution",
    ],
    reveal: false,
  },

  // ==================================================================
  // PART 6 — PARAMETRIZING THE ALTERNATIVE HYPOTHESIS
  // ==================================================================
  {
    type: "section",
    kicker: "A fully model-independent approach",
    title: "Parametrizing the alternative hypothesis",
  },
  {
    type: "content",
    title: "New Physics Learning Machine (NPLM)",
    bullets: [
      "Given a data sample and a reference sample (simulation, or a data sideband), let a neural network parametrize the alternative hypothesis itself — as a small perturbation away from the reference",
      "The loss is (proportional to) the log-likelihood, so training the network is equivalent to a maximum-likelihood fit to the data",
      "Output: the ratio of the best-fit data distribution to the reference distribution — used directly as a test statistic for hypothesis testing",
      "Main difficulty: choosing a reference sample that's both signal-free and statistically faithful to the real data — CURTAINS/CATHODE-style interpolation can help construct one",
    ],
  },

  // ==================================================================
  // PART 7 — CODE
  // ==================================================================
  {
    type: "code",
    title: "A minimal MLP autoencoder in JAX",
    lang: "python",
    code: `import jax
import jax.numpy as jnp
from jax import random

def init_layer(key, m, n):
    w_key, _ = random.split(key)
    w = random.normal(w_key, (m, n)) * jnp.sqrt(2.0 / m)
    b = jnp.zeros(n)
    return w, b

def init_mlp(key, sizes):
    keys = random.split(key, len(sizes) - 1)
    return [init_layer(k, m, n) for k, m, n in zip(keys, sizes[:-1], sizes[1:])]

def mlp_forward(params, x, final_activation=None):
    for w, b in params[:-1]:
        x = jax.nn.relu(x @ w + b)
    w, b = params[-1]
    x = x @ w + b
    return final_activation(x) if final_activation else x

# 784 -> 256 -> 32 (bottleneck) -> 256 -> 784
key = random.PRNGKey(0)
enc_key, dec_key = random.split(key)
params = {
    "encoder": init_mlp(enc_key, [784, 256, 32]),
    "decoder": init_mlp(dec_key, [32, 256, 784]),
}

def autoencoder(params, x):
    z = mlp_forward(params["encoder"], x)                     # latent code
    x_hat = mlp_forward(params["decoder"], z, jax.nn.sigmoid)  # reconstruction
    return x_hat, z

def loss_fn(params, x):
    x_hat, _ = autoencoder(params, x)
    return jnp.mean((x_hat - x) ** 2)  # reconstruction error

# gradients come for free via autograd — no backprop by hand
grads = jax.grad(loss_fn)(params, batch)`,
  },

  // ==================================================================
  // PART 8 — REAL-TIME ANOMALY DETECTION
  // ==================================================================
  {
    type: "section",
    kicker: "Anomaly detection meets the trigger",
    title: "Real-time anomaly detection",
  },
  {
    type: "content",
    title: "Why real time?",
    bullets: [
      "The LHC collides bunches every 25 ns — tens of TB/s, far more than can ever be stored",
      "The Level-1 trigger: fully hardware (FPGA)-based, ~1 μs total latency, reduces 40 MHz → 100 kHz",
      "The High-Level Trigger: software-based, ~100 ms latency, reduces 100 kHz → 1 kHz",
      "Conventional triggers prioritize high-energy events — they're less sensitive to signatures like many low-momentum particles",
      "Model-agnostic outlier detection at the trigger level can catch what fixed rules miss",
    ],
  },
  {
    type: "content",
    title: "VAEs on FPGAs",
    bullets: [
      "hls4ml / FINN automatically translate a trained network into FPGA firmware, combined with quantization-aware training to shrink it further",
      "Trick: use only the KL-divergence term as the anomaly score — skip decoding and MSE entirely, avoiding both Gaussian sampling and buffering the input",
      "This runs in under 100 ns using a small fraction of the available FPGA resources",
      "AXOL1TL: this approach is deployed in the CMS Level-1 trigger — 50 ns latency, <1% of FPGA resources, up to 46% better signal efficiency across a range of BSM signals",
    ],
  },

  // ==================================================================
  // PART 9 — DETECTOR MONITORING
  // ==================================================================
  {
    type: "section",
    kicker: "Beyond physics searches",
    title: "Anomaly detection for detector monitoring",
  },
  {
    type: "content",
    title: "Automated data quality monitoring",
    bullets: [
      "Traditional data-quality monitoring relies on hand-crafted rules and thresholds — expensive to build and maintain",
      "ML-based detector monitoring automates this: compare supervised, semi-supervised and self-supervised approaches for spotting detector failures",
      "Autoencoder-based anomaly detection is already integrated into the CMS Experiment's online monitoring",
      "Same caveat as before: the “anomalous” subset can be simpler than the training data and reconstruct just fine — VAEs and latent-space scoring help mitigate this",
    ],
  },

  // ==================================================================
  // WRAP-UP
  // ==================================================================
  {
    type: "content",
    title: "Open challenges",
    bullets: [
      "There's no ground truth for “new physics” — validating an anomaly detector's real-world performance remains an open problem",
      "Robustness against detector-effect artifacts, and decorrelating scores from the search variable, are still active research areas",
      "As the HL-LHC arrives with 10× the data, automated, model-independent methods only become more essential",
    ],
  },
  {
    type: "content",
    title: "Recap",
    text: "Coming next: self-supervised pre-training — contrastive learning, SimCLR-style embeddings, and masked (particle) modeling.",
    bullets: [
      "Anomaly detection = learn “normal”, flag what doesn't fit — as an outlier or an overdensity",
      "Overdensity search leans on weakly-supervised classifiers (CWoLa) and density estimation (ANODE/CATHODE/CURTAINS)",
      "K-means and PCA summarize data by clustering or projecting it; autoencoders generalize PCA to non-linear manifolds",
      "VAEs regularize the bottleneck into a smooth latent distribution — efficient enough to run in 50 ns on an FPGA",
    ],
    reveal: false,
  },
];
