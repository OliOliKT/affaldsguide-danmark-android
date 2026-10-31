# Android app reviews

“Bedøm appen” appears in the More menu, using the same text colour as other menu items and a green outlined star. It opens the Google Play listing directly, with a browser fallback if the Play Store app is unavailable. The button does not rely on the quota-limited in-app-review flow.

Automatic reviews use the official Google Play Review 2.0.2 SDK. Timing matches iOS:

- First use at least seven calendar days ago.
- Use on at least three different days.
- At least one successfully completed search, foreground calendar fetch or new sorting registration in the current process.
- No launch attempt for the current displayed app version.
- At least 120 calendar days since the last attempt, even across version updates.

After a completed task, a request waits three seconds. Touch input, navigation or leaving the activity cancels it. The activity must be resumed with window focus, no keyboard/drawer/achievement banner, and a suitable result/calendar/overview destination. Achievement dismissal can defer a waiting request until the banner is gone. A network response is checked again before launch so a request cannot appear on a screen the person has already left.

Engagement and attempts are stored only in private `app_review_v1` preferences. A manual store visit also suppresses automatic prompting. SDK request failures are silent and limited to once per process. No review status or rating is inferred from API completion. There is no sentiment gate, reward or preliminary rating dialog.

Existing users begin accumulating review timing after installing this update, avoiding an immediate prompt on update.

## Verification

Unit tests cover the first-week boundary, active-day counting, completed-task requirement, per-version suppression, 120-day cooldown and daylight-saving transitions. Build and all 41 unit tests pass.

Actual review UI must be tested using Google Play's internal testing track or internal app sharing; a locally installed APK alone cannot verify that the review dialog appears. Google controls UI display and the API does not report whether a review was submitted.

Official references:
- https://developer.android.com/guide/playcore/in-app-review/kotlin-java
- https://developer.android.com/guide/playcore/in-app-review/test
