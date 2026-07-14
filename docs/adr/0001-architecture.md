# ADR-0001: Device Assembly Advisor ⊣ Assembly Governor architecture

- Status: Accepted (2026-07-14)
- Repository: `cloud-itonami-isic-2620` (ISIC Rev.5 `2620`)

## Context

Computer/peripheral final-assembly manufacturing (device-unit assembly,
end-of-line quality inspection, EMC/product-safety compliance
evidence, Declaration-of-Conformity issuance) needs the same
governed-actor pattern as the rest of the cloud-itonami fleet: an
untrusted advisor proposes; an independent governor may HOLD;
high-stakes actuation never auto-commits.

A 2026-07-14 value-chain survey of the computer/electronics industry
found `cloud-itonami-isic-2610` (semiconductor/electronic-component
fab) implemented, but the FINAL-PRODUCT-ASSEMBLY tier (assembling a
laptop/PC/server out of fabricated components) had no actor at all --
a mid-chain gap between component fabrication and downstream retail/
distribution verticals. This repository closes that gap, modeled
closely on `cloud-itonami-isic-2910`'s (motor vehicles) Automotive
Advisor ⊣ Automotive Governor shape -- the closest prior sibling in
structure (intake -> evidence-checklist verify -> defect screen ->
robotics mission -> dual actuation) even though the regulatory regime
and final product differ.

## Decision

1. Namespaces live under `deviceassembly.*` with the standard
   facts / registry / store / governor / phase / advisor / operation /
   sim / robotics shape.
2. Entity is a **device-unit** (a laptop/desktop/server/monitor final-
   assembly build), not a vehicle, wafer lot or aircraft assembly.
3. Dual actuation on the same entity:
   - `:actuation/ship-device-unit` (device-unit-shipment draft)
   - `:actuation/issue-declaration-of-conformity` (CE-DoC/FCC-SDoC-style
     Declaration-of-Conformity draft)
4. Double-actuation guards use dedicated booleans
   (`:device-unit-shipped?`, `:declaration-issued?`), never a status
   lifecycle (ADR-2607071320 / 6492 lesson).
5. `device-unit-emc-emission-out-of-range?` continues the fleet
   two-sided range check family (after testlab / conservation / water /
   steelworks / turbine / automotive), applied here to a device-unit's
   own measured EMC emission-level deviation against its own recorded
   regulatory-limit bounds.
6. **Robot process simulation is concrete** (ADR-2607142800 fleet
   pattern, established by `cloud-itonami-isic-2910`'s `automotive.
   robotics`): `deviceassembly.robotics` walks every device-unit
   through a robot burn-in-cell verification mission (automated
   functional-test rig / thermal burn-in stress test / robotic EMC
   pre-scan), and `deviceassembly.governor`'s
   `robotics-simulation-violations` HARD-holds `:actuation/ship-
   device-unit` if the mission never ran, OR if an INDEPENDENT recheck
   of the device-unit's own recorded thermal-margin fields
   (`simulation-out-of-tolerance?`) disagrees with the mission's
   stored `:passed?` verdict -- the same "ground truth, not
   self-report" discipline check 4 (EMC emissions) uses.
7. End-of-line defect unresolved is evaluated unconditionally so
   `:end-of-line-quality/screen` itself can HARD-hold (parksafety
   ADR-2607071922 Decision 5 discipline, carried forward via
   `automotive`'s (cloud-itonami-isic-2910) `end-of-line-defect-
   unresolved-violations`).
8. Compliance spec-basis catalog seeds JPN (VCCI + PSE/METI) / USA
   (FCC Part 15 self-certification) / GBR (OPSS UKCA self-declaration)
   / DEU (Bundesnetzagentur / EU CE-marking under the EMC + RoHS
   Directives) only; missing jurisdictions are uncovered, never
   fabricated. `IEC 62368-1` (audio/video/IT-equipment safety) is
   cited as a shared international safety-standard anchor across
   jurisdictions.

## Consequences

(+) Computer/peripheral final-assembly manufacturing gains a forkable
OSS operating stack with auditable governor holds, closing the
mid-chain gap the 2026-07-14 value-chain survey identified between
`cloud-itonami-isic-2610` (component fab) and downstream retail/
distribution verticals.
(+) Reuses langgraph + store dual-backend parity and the
ADR-2607142800 robotics-process-simulation pattern without new
physics or a bespoke governor shape.
(−) No physical plant digital-twin tick in this repo (follow-up domain
data, e.g. a giemon-factory-style layout, is out of scope here).
(−) Compliance-authority coverage is a starting catalog (4
jurisdictions), not exhaustive.

## Related

- Superproject fleet ADR for this promotion: ADR-2607150200
  (isic-2620 device-assembly)
- ADR-2607142800 (robotics premise → concrete process simulation,
  fleet pattern)
- ADR-2607011000 (original robotics premise + ISIC section coverage)
- Sibling architecture: `cloud-itonami-isic-2910` docs/adr/0001
  (closest structural sibling), `cloud-itonami-isic-2610` docs/adr/0001
  (upstream component-fab sibling)
