# Operator Guide

## First Deployment
1. Register compliance engineers, plants, device-units, personnel and robots.
2. Import historical device-unit / end-of-line / compliance-rules records.
3. Run read-only validation and robot mission dry-runs.
4. Configure compliance evidence checklists and human sign-off paths.
5. Publish a dry-run audit export.

## Minimum Production Controls
- governor gate on every robot action before shipment
- human sign-off for `:high`/`:safety-critical` robot actions (e.g. burn-in/functional-test on safety-relevant device-units, Declaration-of-Conformity issuance)
- audit export for every shipment, sign-off and disclosure
- backup manual process

## Certification
Certified operators must prove robot-safety integrity, evidence-backed
records and human review for safety-affecting actions.

## Operating states
intake : compliance-rules-verify : end-of-line-quality-screen : approve : ship-device-unit : issue-declaration-of-conformity : audit

## Audit export (social operation)

After a production session, export the append-only package for
compliance inspectors or internal compliance:

```clojure
(require '[deviceassembly.store :as store]
         '[deviceassembly.export :as export])
(export/audit-package store)        ; EDN maps
(export/package->csv-bundle store)  ; CSV files as string map
```

Drafts remain **unsigned** — signing and submission to a market-
surveillance authority or notified body are the device-assembly
manufacturer's own acts (see README Actuation honesty).

Static UI sample: `docs/samples/operator-console.html`.
