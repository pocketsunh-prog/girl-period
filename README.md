# Girl Period

A beautiful Android period-tracking app with a Studio Ghibli-inspired cube girl UI. Track your menstrual cycle with lunar calendar info, real-time weather data, events, and more — all wrapped in a soft pastel aesthetic.

![Studio Ghibli Inspired](https://img.shields.io/badge/style-Studio%20Ghibli-pink)
![Android](https://img.shields.io/badge/platform-Android-green)
![Min SDK](https://img.shields.io/badge/minSdk-24-blue)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

---

## Features

### User Authentication
- **SQLite local database** for user accounts
- **Fingerprint login** using AndroidX BiometricPrompt
- Secure session management via SharedPreferences

### User Profile
- **Profile image** - Pick from gallery, saved to app storage
- **Date of Birth** - Ghibli-styled date picker with year selection
- **Update password** - Change password with current password verification
- **Reset password** - Password reset via email (ready for integration)

### Calendar & Tracking
- **Monthly calendar grid** with period day highlighting
- **Chinese Lunar calendar** date conversion
- **Year selection** - Jump to any year (100 years range)
- **Today button** - Quick jump to current date
- **Period prediction** with fertile window and ovulation estimation
- **Event management** - Add/edit/delete events with reminders
- **Event highlighting** - Orange highlight on days with events
- Day detail dialog with full lunar + weather + event info

### Real-Time Weather (HKO API)
- **Live weather data** from [Hong Kong Observatory Open Data](https://www.hko.gov.hk/en/abouthko/opendata_intro.htm)
- **Temperature, Humidity, Wind speed, Rainfall, UV Index**
- **Manual refresh button** - Tap to update weather anytime
- **Auto fallback** to mock data when offline
- **7-day weather forecast** support

### Ghibli DatePicker
- **Custom calendar dialog** with soft pastel styling
- **Year picker** - Tap month/year to select year
- **Customizable appearance**:
  - Font family (sans-serif, serif, monospace, etc.)
  - Font size (12-24sp)
  - Font color (24 preset colors + custom hex)
  - Border color (24 preset colors + custom hex)
  - Selected day color (24 preset colors + custom hex)

### Studio Ghibli Cube Girl UI
- **5 switchable color themes**: Sakura (pink), Matcha (green), Sky (blue), Lavender (purple), Peach (orange)
- **4 calendar styles**: Default, Compact, Rounded, Minimal
- **Customizable calendar colors**: Text color + background color
- Custom-drawn cube girl mascot that changes color with the theme
- **Long cat icon** for today's date highlight
- Soft pastel Ghibli-inspired aesthetic throughout

### Analysis & Charts
- **Line chart**: cycle length over time
- **Bar chart**: period duration over time
- **Pie chart**: menstrual phase distribution
- Statistics: average/shortest/longest cycle, average duration

### Notifications
- **Period reminders** - Gentle Ghibli-style reminders when period expected within 2 days
- **Event reminders** - Notifications for upcoming events (1 day before, 2 days before, etc.)
- Daily check via AlarmManager + BroadcastReceiver
- Notification channels for Android O+

---

## Screenshots

| Login | Calendar | Charts | Settings |
|-------|----------|--------|----------|
| Cube girl mascot + fingerprint | Lunar + weather + period highlight | Line/Bar/Pie analysis | 5 color themes + calendar styles |

| DatePicker | Profile | Events | Weather |
|------------|---------|--------|---------|
| Ghibli-styled with customization | Image + DOB + password | Add/edit/delete with reminders | Real-time HKO data |

---

## Project Structure

```
girl-period/
├── app/
│   ├── build.gradle              # App-level build config with signing
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/girlperiod/app/
│       │   ├── LoginActivity.java
│       │   ├── RegisterActivity.java
│       │   ├── MainActivity.java          # Calendar with lunar + weather + events
│       │   ├── ChartActivity.java         # MPAndroidChart analysis
│       │   ├── AddPeriodActivity.java     # Add/edit period records
│       │   ├── ProfileActivity.java       # Profile image, DOB, password
│       │   ├── EventActivity.java         # Add/edit/delete events
│       │   ├── SettingsActivity.java      # Theme + calendar + datepicker styles
│       │   ├── SessionManager.java        # SharedPreferences session
│       │   ├── LunarCalendar.java         # Chinese lunar conversion
│       │   ├── WeatherService.java        # Mock + real weather (HKO API)
│       │   ├── HkoWeatherService.java     # HKO Open Data API integration
│       │   ├── PeriodPredictor.java       # Next period / fertile window
│       │   ├── PeriodNotificationService.java
│       │   ├── EventNotificationService.java
│       │   ├── NotificationReceiver.java
│       │   ├── EventNotificationReceiver.java
│       │   ├── ui/GhibliTheme.java        # 5 color themes + calendar styles
│       │   ├── ui/CubeGirlView.java       # Custom cube girl drawing
│       │   ├── ui/GhibliDatePickerDialog.java  # Custom Ghibli-styled date picker
│       │   └── data/
│       │       ├── DatabaseHelper.java    # SQLite users + period_records + events
│       │       ├── User.java              # User model with DOB + profile image
│       │       ├── PeriodRecord.java
│       │       └── Event.java             # Event model with reminder
│       └── res/
│           ├── layout/           # 10+ activity layouts
│           ├── drawable/         # 60+ backgrounds, icons, cube girl mascots
│           ├── values/           # colors, strings, styles, themes
│           ├── menu/             # Bottom navigation menu
│           ├── color/            # Nav color selector
│           └── mipmap-anydpi-v26/ # Adaptive launcher icons
├── build.gradle                  # Root build config
├── settings.gradle               # Includes JitPack for MPAndroidChart
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── girlperiod-release-key.jks    # Release signing key
└── README.md
```

---

## Build & Release

### Prerequisites
- Android Studio or JDK 17+
- Android SDK with compileSdk 34
- Gradle 8.2+

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
The release build is configured with signing. The keystore is at the project root:

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Signing Configuration
The release keystore was generated with:
```bash
keytool -genkeypair -v \
  -keystore girlperiod-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias girlperiod-key
```

**Install on device:**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/release/app-release.apk
```

> **Note:** For production, replace the debug keystore with your own and update the credentials in `app/build.gradle`. Never commit real keystore passwords to version control.

---

## Tech Stack

| Component | Library |
|-----------|---------|
| UI | AndroidX AppCompat, Material Components |
| Charts | [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) v3.1.0 |
| Biometrics | AndroidX Biometric 1.1.0 |
| Database | SQLite (Android built-in) |
| Weather API | [HKO Open Data](https://www.hko.gov.hk/en/abouthko/opendata_intro.htm) |
| Date/Time | [ThreeTenABP](https://github.com/JakeWharton/ThreeTenABP) 1.4.6 |
| Build | Gradle 8.2, AGP 8.1.0 |

---

## Architecture

- **Data Layer**: `DatabaseHelper` manages SQLite with three tables (`users`, `period_records`, `events`)
- **Session**: `SessionManager` persists login state via SharedPreferences
- **Prediction**: `PeriodPredictor` calculates next period, fertile window, and ovulation from historical records
- **Weather**: `HkoWeatherService` fetches real data from HKO API with mock fallback via `WeatherService`
- **Theme**: `GhibliTheme` manages 5 color palettes + calendar styles + date picker customization
- **Events**: Full CRUD with reminder notifications via `EventNotificationService`

---

## Weather API Reference

Weather data is sourced from the [Hong Kong Observatory Open Data](https://www.hko.gov.hk/en/abouthko/opendata_intro.htm):

| Data | API Endpoint |
|------|-------------|
| Current Weather | `https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=rhrread&lang=en` |
| Weather Forecast | `https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=fnd&lang=en` |
| UV Index | `https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=uvindex&lang=en` |

---

## License

MIT License. Feel free to use and modify.
