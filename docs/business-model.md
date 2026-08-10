# Business Model: Manufacture of Computers and Peripheral Equipment

## Classification
- Repository: `cloud-itonami-isic-2620`
- ISIC Rev.5: `2620` — manufacture of computers and peripheral equipment — final-assembly, end-of-line quality screening and Declaration-of-Conformity issuance
- Social impact: product-safety, supply-resilience, industrial-jobs

## Customer
- independent computer/peripheral final-assembly manufacturers and contract assemblers needing auditable compliance and production records
- contract plants assembling laptops, desktops, servers or monitors for multiple brands/OEMs
- plant operators needing verifiable build and end-of-line history for produced device-units
- market-surveillance authorities and notified bodies needing verifiable EMC/product-safety compliance and conformity evidence
- programs that cannot accept closed, unauditable manufacturing-execution platforms

## Offer
- EMC/product-safety compliance-rules and jurisdiction-scope version management
- robotics-assisted burn-in, functional-test and end-of-line inspection records
- device-unit EMC-emission-deviation and end-of-line chain-of-custody history
- Declaration-of-Conformity drafts and disclosure records
- role-based access and immutable audit ledger
- CSV/EDN audit package export for inspectors

## Revenue
- self-host setup fee
- managed hosting subscription per plant / assembly line
- support retainer with SLA
- burn-in/functional-test/end-of-line robot integration and maintenance

| Package | Customer | Price shape |
|---|---|---|
| Self-host | any qualified assembly plant | AGPL-3.0-or-later, free |
| Managed Starter | one final-assembly plant (1 site, 1–2 assembly lines, 10–20 QA/production-engineering seats; typically 50–200 employees) | ¥35,000/月 flat |
| Support retainer / robot integration | as scoped | quoted separately, not part of the Starter tier |

**Market-anchored (2026-08-10)**: benchmarked against 7 real products in the
manufacturing quality/execution SaaS market. **5 of the 7 publish an entry
price on their own site**; the 2 that do not are the enterprise MES tier.
Published anchors, converted at ~¥150/$ for the assumed plant size above:

| Product | Published price (as printed) | ≈ JPY/月 at assumed size |
|---|---|---|
| [UM SaaS Cloud](https://www.umsaascloud.jp/price/) (シナプスイノベーション) | UM基本パック Business Edition 月額35,000円（税抜・10ID）／業務モジュール UM工程進捗・UM販売購買 各15,000円/月（10ID単位）。「UM基本パック＋UM工程進捗」で月額50,000円の例を掲示 | ¥35,000–50,000 |
| [QC-One Lite](http://www.uis-inf.co.jp/products/qc-one/) (ユーアイエス、クラウド版) | 月額5万円（オンプレミス版と初期費用は非公開） | ¥50,000 |
| [i-Reporter](https://i-reporter.jp/price/) (シムトップス) | クラウド版 月額42,000円〜＋初期55,000円／オンプレ サブスク版 月額37,500円〜。税抜・最小5ユーザー | ¥42,000〜 |
| [Katana Cloud Inventory](https://katanamrp.com/pricing/) | Core "starts at $299/month"; add-ons Traceability $249/mo, Manufacturing Management $199/mo | ¥44,850 (Core) – ¥82,200 (Core+Traceability) |
| [Tulip](https://tulip.co/pricing/) | Essentials "$100/mo per interface billed annually", Professional "$250/mo per interface", both 10-interface minimum. Enterprise / Regulated Industries: contact sales | ¥150,000 (Essentials × 10 interfaces) |

**Not disclosed** — recorded here because opacity is itself an observation,
not a gap in this survey: [MachineMetrics](https://www.machinemetrics.com/pricing)
publishes three tiers (Core Platform / Intelligent MES / Enterprise) with no
figures at all, and **Siemens Opcenter** and **Rockwell Plex** are custom
enterprise quote only. The disclosure line in this market falls neatly between
SMB tooling (transparent) and plant-wide MES (opaque).

The measured band at the assumed plant size is therefore **¥35,000–¥150,000/月**,
with a dense Japanese cluster at ¥42,000–¥50,000 — that cluster, not Tulip's
upper end, is what a Japanese mid-size plant actually pays today for
quality/record tooling. **¥35,000/月 sits deliberately at the observed floor.**
This actor is narrower than every product above: it has no inventory/MRP
(Katana), no shop-floor app authoring (Tulip, i-Reporter), no automatic
ingestion from inspection instruments — which is QC-One's principal selling
point — and no 工程進捗/販売購買 (UM). It does compliance-rules and
jurisdiction-scope versioning, the end-of-line HARD hold, robot burn-in and
connector-insertion-force evidence, the Declaration-of-Conformity draft, and
the inspector-facing audit export, and nothing else. Pricing just under the
cluster would not read as a cheaper alternative; sitting at the floor puts
visible daylight (−30% vs QC-One, −17% vs i-Reporter) against products that do
strictly more. It is not priced lower than that, because it carries one thing
none of the five comparators has structurally: an independent Assembly
Governor whose HARD holds cannot be overridden by any human approval, and an
immutable conformity trail that can be handed to a market-surveillance
authority. For a plant carrying its own EMC/product-safety self-declaration
liability, that is the substance of what is being bought.

Setup fee, SLA support retainer and robot integration remain separately
quoted (see the bullets above) — **¥35,000/月 is the managed-hosting tier
alone**, not a bundled all-in price.

**Subscribe (2026-08-10)**: a live Stripe Payment Link for the Managed
Starter tier (¥35,000/月 flat) is available now —
[**subscribe to Managed Starter**](https://buy.stripe.com/aFa00k72v7pmd5K922eEo09).
This is a no-code Stripe-hosted checkout on Gftd Japan 株式会社's live
account; nothing in this repo's actor code changed, and managed-tenant setup
is manual fulfillment today with no automated onboarding. **No plant has
claimed or subscribed to this tier yet — this is a working checkout with zero
paid tenants, not a claim of existing revenue.**

## Trust Controls
- out-of-spec device-units are blocked; a Declaration of Conformity is mandatory for release paths; device-unit history is immutable
- a robot action the governor refuses is never dispatched to hardware
- every shipment, hold, approval and disclosure path is auditable
- sensitive design and production data stays outside Git
- a fabricated compliance-rules citation, incomplete evidence, an
  out-of-spec EMC emission deviation, or an unresolved end-of-line
  defect -- each forces a hold, not an override
- Declaration-of-Conformity issuance is logged and escalated, and
  cannot be finalized twice for the same device-unit
