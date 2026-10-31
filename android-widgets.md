# Android widgets

## iOS layout parity

- Search: 2×2 by default, the exact transparent iOS WidgetLogo tinted white, “Sortér dit affald”, and stacked “Søg” / “Tag billede” buttons. The green background is #76AD61 and the buttons use white at 18% opacity, matching iOS. Short existing widgets use a compact horizontal arrangement; the widget can be resized.
- Calendar: 2×2 by default, resizable to a wider layout. Four upcoming collections are shown with bullets and short dates. Rows share the available vertical space. The wider layout adds a calendar icon and a larger title. No selected address shows “Vælg adresse”. The entire widget opens the calendar.
- Both widgets use the selected app language and RTL layout for Arabic. Calendar collection labels share the current iOS translation dictionary in `calendar_label_names.json`.

Android launchers control cell dimensions and outer spacing, so these are native Android equivalents rather than pixel-identical iOS widgets. Existing search widgets may need to be resized or re-added to take the new default dimensions.

## Data and refresh

`CalendarWidgetData` stores an address-scoped cache in the existing calendar preferences. Cached dates are shared with the app, survive restarts and are retained on a failed fetch. Past dates are filtered out. Switching address clears the old cache only after the new address is saved.

While at least one calendar widget is installed and an address is selected, WorkManager requests a refresh approximately hourly with a network constraint. Adding a widget requests an immediate refresh. Android may defer work for battery saving. The widget-provider update interval also redraws the cached state; changing language, address or fetching fresh dates in the app updates installed widgets.

Responses are checked against the currently selected municipality/address before cache writes. Cached update timestamps prevent an older request overwriting a newer successful response. Worker reminder scheduling and address replacement use the same lock so an old address cannot reintroduce its reminders after switching.

Successful widget refreshes also refresh enabled collection reminders. Removing the last calendar widget cancels its refresh jobs. Widget actions use distinct URI identities to avoid camera/search/calendar PendingIntents replacing each other.

## Verification

Android debug build and all 35 unit tests pass, including new coverage for the next four pickups, stale/past dates, address isolation, cache serialization and translated compound/provider labels. No launcher/emulator visual verification was performed.
