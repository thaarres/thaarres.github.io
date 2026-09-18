# Sources / provenance

## User-supplied deck
`edge_inference_irfu_paris.pptx`

Used for the main data-flow narrative and the quantitative anchors already present in the talk, including 25 ns / 40 MHz, 63 Tb/s, FPGA trigger processing, scouting, hardware-aware ML, HGCAL, and SmartPixel material.

## LEAP-40 Part B2
User-supplied `LeEAP40_B2.pdf`.

Important Fig. 4 values used in the scouting sequence:
- Detector → L1T: 40 MHz, ~200 kB/event, 63 Tb/s
- L1T → 40 MHz scouting: 40 MHz, ~10 kB/event, 7.65 Tb/s
- conventional detector → HLT: 750 kHz, ~1.5 MB/event, 10 Tb/s
- scouting offline target: all crossings, max ~5 GB/s

The proposal describes the scouting system as a parasitic readout of reconstructed Level-1 objects, operating in parallel with the normal L1 trigger and bypassing the trigger-selection loss.

## ATLAS + CMS HL-LHC projections
ATLAS/CMS, *Highlights of the HL-LHC physics projections by ATLAS and CMS*, ATL-PHYS-PUB-2025-018 / CMS-HIG-25-002 (31 March 2025).

https://cms-results.web.cern.ch/cms-results/public-results/publications/HIG-25-002/

Numbers used:
- H→μμ coupling precision: 3%
- H→Zγ coupling precision: 7%
- main Higgs couplings: 1.6–3.6%
- SM di-Higgs observation: >7σ
- Higgs trilinear self-coupling λ3: better than 30%
- longitudinal VBS cross section: better than 20%
- four-top production: 6%

## CMS HGCAL ECON-T
Yu-Wei Kao, *ECON ASICs for the CMS High Granularity Calorimeter*, CERN/CMS conference material (2025).

Key public specifications used:
- ECON-T processes trigger data at 40 MHz
- latency <0.4 μs
- up to 13 output e-links at 1.28 Gb/s
- programmable selection/compression algorithms

## ePIC dRICH
User-supplied IEEE TNS draft, *Online Data Reduction for the ePIC dRICH Using a Multi-FPGA Neural Network* (2026 draft).

Values / architecture used:
- ~320k continuously streaming detector channels
- 30 FPGA Data Aggregation Modules (DAMs)
- ~3 Tb/s aggregate bandwidth constraint
- 30 parallel local MLPs
- local network emits 8 features
- six sector MLPs emit 4 features each
- global aggregation NN performs binary classification
- physics-bearing fragments are kept; noise-only buffered fragments are flushed

## FCC-ee / SmartPixel
User-linked fastML ESPP talk:
https://agenda.infn.it/event/44943/contributions/266390/attachments/137401/206588/fastML_espp.pdf

The current scene deliberately makes only qualitative claims about FCC-ee vertex-detector pressure. Exact rate estimates should be inserted from this source during the next content pass rather than inferred.
