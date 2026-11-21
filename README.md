# ZEUS Garage — Batch 5 (v0.5.0)

Native Android app for truck maintenance and vehicle records. The **Ruby Red + white** UI uses the large BoreLab-style tile grid from the approved ZEUS Garage concept. Works without a network connection or paid server.

## Batch 5 — Navigation cleanup (v0.5.0)

- Removed the five-item bottom navigation and top-left hamburger menu to reclaim screen space.
- **Right-edge pull-tab works on every page**: tap to open, swipe left to open, or drag it up/down to a comfortable finger position. The saved position is screen-size-independent and persists when ZEUS restarts.
- **Compact menu search** is a single short line, with a shorter placeholder. Removed the extra instructional text beneath it.
- The drawer's existing large shortcut cards and fixed Quick Add Record action remain as before.
- **Unchanged:** truck hero image/card, the rest of the dashboard, features, database, and saved records. No new permission requests or data migrations.
- Phone testing: open a nested page via the launcher, move the edge tab, relaunch ZEUS, check it stays there. Upgrade in place, do not uninstall.

## Batch 4 — Previous navigation redesign

- BoreLab-style **Garage Menu** slides in from the right instead of the plain sidebar.
- Large Ruby Red + white grouped shortcuts, shortcut-only search, close button, active truck heading, and fixed **Quick Add Record** button.
- In v0.5.0 the **movable right-edge pull-tab** replaces the older hamburger and bottom More entry points. Press Back, tap outside, or choose a shortcut to close the launcher.
- **Unchanged:** original header, truck information, F-250 artwork, dashboard tiles, existing feature pages, stored records, and database schema.
- Upgrade over the installed v0.3.0 app to keep saved records. Do not uninstall.

## What's working in ZEUS Garage

- **My Garage:** Default vehicle is **ZEUS**, 2019 Ford F-250 Super Duty, 6.7L Power Stroke, Ruby Red, 6-inch lift, 37-inch tires. Edit vehicle details, manually enter mileage, add other vehicles, and switch which vehicle is active. Different trucks get separate records.
- **Service schedules:** Add and edit mileage-based maintenance schedules. Initially they have *no assumed intervals* and no fabricated service history. Log completed service with date, mileage, cost and notes. A logged service can update its linked schedule's last-done mileage. Reminders calculate from your actual odometer.
- **Diagnostics:** Search an offline starter DTC glossary (P2457, P0401, P0673, P0299, P0087, P0300, P0420). Manually save observed or resolved codes, symptoms and notes. There is **no live OBD connection** in this version; the code library is informational only.
- **Fuel:** Log date, odometer, gallons, price and full/partial fill-ups. Total fuel expenses are computed from actual entries. Estimated fill-to-fill MPG appears only when two full-tank readings provide enough data.
- **Repairs:** Track parts, problems, cost, mileage and repair notes.
- **Upgrades:** Two **planned** ideas are seeded: Raptor-style steering wheel and larger touchscreen radio. Edit, add or mark them installed; only installed upgrade amounts are counted in expenses.
- **Photos and notes:** Save a title, note, date and one selected photo per note. Images are copied to private app storage (max 20 MB) and displayed in the notes list. No cloud uploads.
- **History / expenses:** Unified timeline of logged service, fuel, repairs, diagnostics and notes. Actual expense totals (fuel + logged service + logged repairs + installed upgrades). **Export expense CSV** using Android's document save picker.

### Batch 3 — Smart Health Center + Parts & Stores + Sidebar

- **BoreLab-style Ruby Red sidebar:** Tap the menu icon at the upper left to open organized My Truck, Garage, and Records sections. The original bottom navigation remains available.
- **Smart Vehicle Health:** Summarizes open, manually recorded DTCs and symptoms, overdue service tasks, tasks due within 500 miles and recorded repair spending. No fabricated ECU telemetry or arbitrary health percentage.
- **Active Issues:** One list of manually recorded active DTCs and symptom observations.
- **Symptom Tracker:** Save the date, symptom name, severity, intermittent/constant occurrence, notes, and open/resolved status. Entries are stored separately for each vehicle.
- **Parts & Stores:** NAPA, O'Reilly, AutoZone, Advance Auto, Walmart Auto, RockAuto, eBay Motors, and Amazon Automotive. Shop opens an official retailer website and may hand off to a compatible installed app. Navigate opens a Google Maps *search* for nearby physical stores; select the desired store and tap Directions. Online-only retailers don't offer Navigate.
- **No location permission:** The app does not read GPS coordinates; Google Maps handles locating nearby stores. Results may not be ordered by distance, and ZEUS does not know store stock.
- **Existing records remain intact:** SQLite database schema v2 adds the symptom table via non-destructive migration. **Do not uninstall** when upgrading.
- **GitHub BuildDeck workflow:** Code is hosted at [ZEUSGarage](https://github.com/luischairez12-ui/ZEUSGarage); changes can arrive via BuildDeck's AI Sync Inbox for review, followed by an explicitly confirmed on-phone build.

**What is not implemented:** manufacturer live fuel percentage, vehicle location, unlock/lock, remote start, OBD scan tool, connected diagnostic telemetry, document/receipt OCR, cloud accounts, multi-device sync, vehicle-photo recognition, export/restore of full private data, or production release signing. Numbers only come from your input.

## Build with BuildDeck on your phone

1. Open **BuildDeck → Projects**, then open the existing **GitHub-connected ZEUSGarage** project.
2. Tap **Fetch changes from GitHub** or open AI Sync Inbox. Review/approve the incoming changes; no code is applied automatically.
3. Tap **Build APK** in that project to build from the updated source using your configured Termux/Gradle/Android SDK 35 installation.
4. Once the build actually succeeds, look in **Downloads**, or **Downloads/BuildDeck/Out** if BuildDeck's copy succeeded. If the APK is not in the Vault, choose **Vault → + Import APK** and associate it with ZEUS Garage.
5. Install the APK. Android will always show its own installer confirmation.

**Do not uninstall the app to update it once you start entering records**: this version stores its data in a private local SQLite database. Use the same Termux signing key for future builds. Uninstalling the app removes local data (Android cloud backup is disabled).

## Build with GitHub Actions

1. The source lives in **luischairez12-ui/ZEUSGarage**; `.github/workflows/android-apk.yml` builds on PRs and merges into `main` and can also be dispatched manually.
2. Download the debug APK artifact from the completed workflow. **GitHub Actions' debug signing key may differ from your Termux key**, so do not install the CI build over a Termux-signed version containing important data.
3. Import the repository through **BuildDeck → Projects → GitHub tools → Import GitHub source** so future ChatGPT commits can appear in the AI Sync Inbox.

## Developer handoff

- Application ID: `com.zeusgarage.mobile`
- Java/Kotlin target: 17; Kotlin 2.0.21; AGP 8.7.3; Gradle 8.9; SDK 35; minimum Android 10 (API 29).
- Main entry point: `app/src/main/java/com/zeusgarage/mobile/MainActivity.kt`.
- Screens: `ZeusUi.kt` and `ZeusPages.kt`; dialogs: `ZeusForms.kt`.
- Data: `ZeusModel.kt`, `ZeusStore.kt` (SQLite schema v2 with v1 migration), `ExpenseExport.kt`.
- Hero artwork cropped from the **approved concept image generated for ZEUS Garage**, at `drawable-nodpi/zeus_truck.webp`.
- Data migration: **never drop tables or reset the database** when updating schema versions. Implement proper forward-only migrations before modifying existing records.
- Safety: Diagnostics are manual and informational. Never represent mock vehicle telemetry or remote vehicle controls as real.

## Phone acceptance checklist

- [ ] Header, truck hero image/details and home dashboard match the previous version exactly.
- [ ] There is no bottom Home/Garage/Service/History/More bar and no top-left hamburger.
- [ ] The right-edge tab opens the launcher on Home AND every other page.
- [ ] Drag the tab up and down; it stays on-screen and remembers its position after restarting ZEUS.
- [ ] Compact search is a single line without the old explanatory hint; it finds pages such as 'parts', 'fuel', or 'health'.
- [ ] A shortcut opens the existing page; Close, Back and outside-tap dismiss the menu.
- [ ] Quick Add Record opens the existing add-record dialog; saved records remain intact.


- [ ] App opens with ZEUS's prefilled truck information, **without a fake odometer or fuel status**.
- [ ] Edit current mileage and see it on the Home card.
- [ ] Add a service interval and save a matching completed service; reminder math changes.
- [ ] Add a full-tank fuel log, then another full-tank fuel log at higher mileage; verify fuel cost and MPG.
- [ ] Add a repair with parts and a cost; verify it appears in History and CSV.
- [ ] Change one upgrade from Planned to Installed; verify expense totals update.
- [ ] Add a note with a selected photo; close and reopen the app to verify it persists.
- [ ] Save a DTC as Observed and change it to Resolved; confirm no fake scanner connection.
- [ ] Add a second vehicle and verify its records don't appear under ZEUS.
- [ ] Tap the right-edge pull-tab and navigate with the slide-out Garage Menu.
- [ ] Open Parts & Stores and verify all eight retailer Shop links; online shops must have no Navigate button.
- [ ] Tap Navigate for AutoZone/NAPA/O'Reilly and choose a nearby result in Google Maps. No ZEUS location permission required.
- [ ] Add a symptom, change severity, mark resolved; verify Vehicle Health's open count changes and records persist after restart.
- [ ] Upgrade an existing v0.1.0 installation *without uninstalling*, verify pre-existing maintenance, fuel, repairs, notes, photos and vehicle data remain.

## Source checks performed before delivery

- Earlier source checks validated the original XML and SQLite tables. Current release includes database v2 and its non-destructive migration; GitHub Actions verifies Android compilation.
- Compiled the dependency-free Kotlin domain model with `kotlinc`, and passed smoke tests for maintenance due/overdue math, cash amounts, and full-to-full MPG with an intermediate partial fill-up.
- Verified the ZIP contents and CRC. **Build status:** GitHub Actions compiles the Android APK; the user separately builds and installs with BuildDeck/Termux on their phone. GitHub builds alone do not establish phone functionality.

Optional development checks (with Python 3 and Kotlin compiler installed):

```sh
python tests/check_source.py
kotlinc app/src/main/java/com/zeusgarage/mobile/ZeusModel.kt tests/DomainSmoke.kt -include-runtime -d /tmp/zeus-domain.jar
java -jar /tmp/zeus-domain.jar
```
