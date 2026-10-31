# Android data and synonym sync — 29 October 2026

Copied the seven shared JSON datasets from the iOS app's Resources directory. Android now contains 2,240 general items, 8,265 municipality rules, 98 municipalities, 352 recycling centres and 12 waste categories, plus the current type impact factor catalogue and UI translations.

The six non-UI datasets match iOS exactly. UI translations incorporate the shared updates while retaining Android-only entries and Android privacy/device wording. Achievement translations now match the iOS levels, including singular descriptions. Android's separate fraction enhancements remain intact. Source URLs and check dates are copied without changing their meaning.

Synonym lookup supports `Synonymer`, `Synonymer_en` and `Synonymer_ar`. Canonical names take priority over aliases. Text search, descriptions, local rules, history identifiers and pictograms resolve through the same canonical item. Autocomplete and fuzzy fallbacks show one matching name per canonical item, without secondary grey labels. The matched text remains the result title. Recent history can reopen old names that were merged into synonyms.

Image recognition compares evidence against canonical names and synonyms in all three languages, but displays canonical names only. Recycling-centre alternatives remain last in returned sorting destinations.

Validation: all local rules match general items; all previous Danish item names remain canonical or searchable aliases; non-UI JSON matches iOS; Java regression tests cover every localized synonym and canonical-name precedence.

Future data sync:

```sh
python3 scripts/sync-ios-data.py --source '/path/to/iOS/Resources'
```
