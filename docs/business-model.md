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
