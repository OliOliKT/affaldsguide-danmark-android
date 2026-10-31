# Android: Mit affaldsoverblik

The overview is available under Mere and from the climate summary on the search page. Result pages offer “Jeg har sorteret dette” for supported waste categories and recycling-centre destinations. Washing, pharmacies, police and other unsupported destinations are excluded.

## Parity with iOS

- Six preset weights (10, 50, 100, 250, 500 and 1,000 g), plus custom grams/kilograms from 1 g to 100 kg. The same picker edits logged weights.
- Material/type factors come from `type_impact_factors.json`, with the same narrow product exceptions as iOS. General item Type data is used even when a local rule determines the destination.
- Recycling centre is omitted from the choice when a supported material destination exists. Two or three material destinations can split the total weight equally. Recycling-centre-only, residual and hazardous items can be logged with zero estimated savings and an explanation.
- Six monthly or ISO-week climate bars, category weight and climate charts, a monthly sorting rhythm, and recent registrations with category and sorting controls. Six entries are shown initially. Item names are resolved from the current-language database when displayed and exported.
- Eight rotating illustrative comparisons use the same constants and eligibility ranges as iOS.
- Successful save feedback includes confirmation text, a short haptic and the iOS sound (only in normal ringer mode). Logged-item and CO₂e achievement levels now receive actual log progress.
- A saved entry retains its factors and calculation version; weight editing scales its original estimate. Deletion updates totals without revoking achievement stars.

## Storage and export

`SortingLogStore` stores the JSON log in private `sorting_impact_v1` preferences. No logging/analytics request is sent. Android's configured device-backup behaviour still applies. CSV export uses Android's system document picker, including UTF-8 BOM, quoted cells and spreadsheet-formula protection. Export includes the complete log regardless of history filters. The Android privacy page describes this storage and export.

## Verification

Regression tests cover type/destination and product-specific factors, unsupported destinations, equal allocation, custom-weight validation, persistence, editing, deletion, achievement progress callbacks, failed storage and CSV escaping. Build and unit tests pass. Full-project lint currently reports a pre-existing `UseAppTint` error in `one_row_onboarding_municipality.xml`; it reports no errors in the overview implementation. No emulator was available for visual/device verification.
