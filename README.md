# cloud-itonami-isic-2620

Open Business Blueprint for **ISIC Rev.5 2620**: manufacture of
computers and peripheral equipment -- final-assembly, end-of-line
quality screening and Declaration-of-Conformity issuance for a
community computer/peripheral final-assembly plant.

This repository publishes a device-final-assembly actor -- device-unit
intake, per-jurisdiction EMC/product-safety compliance rules
verification, end-of-line-defect screening, robot burn-in-cell
verification and Declaration-of-Conformity finalization -- as an OSS
business that any qualified computer/peripheral assembly plant can
fork, deploy, run, improve and sell, so a plant keeps its own build and
compliance history instead of renting a closed MES / quality SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet -- here it is **Device Assembly Advisor
⊣ Assembly Governor**.

## Scope note: final assembly, not component fabrication

This repository is scoped to **assembling** finished computers and
peripheral equipment (laptops, desktops, servers, monitors) out of
already-fabricated components -- final-assembly, end-of-line quality,
compliance evidence and shipment/Declaration-of-Conformity issuance.
It is not a wafer/component-fab vertical. Distinct from:

- `cloud-itonami-isic-2610` -- semiconductor and electronics
  **component/wafer fabrication** (fab lot intake, process-safety
  verification, process-defect screening, yield-audit finalization).
  isic-2610 is one tier UPSTREAM of this repo in the value chain: it
  fabricates the chips/boards; this repo assembles those fabricated
  components into a shippable finished device-unit. A device-unit here
  consumes 2610-fabricated components as an already-verified input, not
  as something this actor re-fabricates or re-verifies at the wafer
  level.
- `cloud-itonami-isic-2910` -- manufacture of **motor vehicles**
  (structurally the closest sibling pattern -- vehicle assembly,
  type-approval/homologation, dual actuation -- but a different final
  product and a different regulatory regime: EMC/product-safety
  self-declaration here, vs. vehicle type-approval/homologation there).

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here robots (functional test rig,
burn-in stress chamber, EMC pre-scan) operate under an actor that
proposes actions and an independent **Assembly Governor** that gates
them. The governor never issues a Declaration of Conformity itself;
`:high`/`:safety-critical` actions (`:actuation/ship-device-unit`,
`:actuation/issue-declaration-of-conformity`) require human sign-off.

**Robot process simulation is concrete, not just a flag**
(ADR-2607150200, extending ADR-2607142800 and ADR-2607011000):
`deviceassembly.robotics` walks every device-unit through a robot-
executed burn-in-cell verification mission (`kotoba.robotics`
mission/action/telemetry-proof contracts) -- automated functional-test
rig, thermal burn-in stress test, robotic EMC pre-scan -- before
`:actuation/ship-device-unit` is proposable. The Assembly Governor
independently re-derives the device-unit's own thermal-margin
tolerance from ground-truth fields, never trusting the mission's
self-reported verdict alone.

**A second, REAL time-stepped physics check rides alongside it**
(ADR-2607991500): the mission's fourth step actually time-steps a
`kotoba-lang/physics-2d` rigid-body world for a CONNECTOR MATING/
INSERTION-FORCE TEST -- a real computer/peripheral-equipment QA
procedure (IEC 60512-13-1-style: a connector plug approaches and seats
into its receptacle at a controlled rate, and the peak force at
seating is read off the actual simulated trajectory, never invented).
This is additional to, not a replacement for, the burn-in/EMC
thermal-margin check above -- the two verify unrelated QA domains. The
Assembly Governor independently re-derives BOTH the thermal-margin
verdict and the connector-mating-force verdict from ground-truth
fields before `:actuation/ship-device-unit` may commit.

## Core contract

```text
device-unit intake + compliance-rules verify + end-of-line quality screen
  -> Device Assembly Advisor proposal
  -> Assembly Governor (HARD holds un-overridable)
  -> phase gate (actuation always escalates)
  -> human approval for high stakes
  -> append-only ledger + draft records
```

## Actuation honesty

Shipping a device-unit and issuing a Declaration of Conformity produce
**unsigned draft records and ledger facts only**. This actor does not
talk to real plant control systems or compliance-authority/notified-
body portals. Signature and hardware/logistics dispatch are the
device-assembly plant's own acts.

## Ops

| Op | Effect |
|---|---|
| `:device-unit/intake` | normalize device-unit directory patch (phase 3 may auto-commit when clean) |
| `:compliance-rules/verify` | per-jurisdiction EMC/product-safety compliance evidence checklist (always human) |
| `:end-of-line-quality/screen` | end-of-line defect screen (HARD hold if unresolved) |
| `:robotics/simulate-burn-in-cell` | robot burn-in-cell verification mission -- thermal burn-in/EMC pre-scan AND (ADR-2607991500) a real `physics-2d` connector mating/insertion-force test (always human; required on file before shipment) |
| `:actuation/ship-device-unit` | draft device-unit-shipment record (always human; HARD hold if robotics-sim missing, or either the thermal-margin or the connector-mating-force reading is independently out-of-tolerance) |
| `:actuation/issue-declaration-of-conformity` | draft Declaration-of-Conformity record (always human) |

## Social / regulatory hand-off

```clojure
(require '[deviceassembly.store :as store]
         '[deviceassembly.export :as export])

(def db (store/seed-db))
(export/audit-package db)           ;; EDN maps for compliance/market-surveillance hand-off
(export/package->csv-bundle db)     ;; CSV bundle (device-units/ledger/shipments/declarations-of-conformity)
```

Operator console (static sample): `docs/samples/operator-console.html`.

## Develop

```bash
clojure -M:dev:test
clojure -M:lint
clojure -M:dev:run
```

## License

AGPL-3.0-or-later — see `LICENSE`.

## Operator console (Pages)

After enabling GitHub Pages (Settings → Pages → GitHub Actions), the
static console is at:

https://cloud-itonami.github.io/cloud-itonami-isic-2620/

Local: open `docs/index.html` or `docs/samples/operator-console.html`.

## Export audit package (CLI)

```bash
clojure -M:dev:export
# or: clojure -M:dev:export /tmp/audit-2620
```

Writes CSV files under `out/audit-package/` (or the given directory).
