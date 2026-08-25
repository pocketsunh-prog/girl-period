# Girl Period

A beautiful Android period-tracking app with a Studio Ghibli-inspired cube girl UI. Track your menstrual cycle with lunar calendar info, weather data, UV index, humidity, and rainfall — all wrapped in a soft pastel aesthetic.

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

### Calendar & Tracking
- **Monthly calendar grid** with period day highlighting
- **Chinese Lunar calendar** date conversion
- **Weather data**: temperature, UV index, humidity, rainfall
- **Period prediction** with fertile window and ovulation estimation
- Day detail dialog with full lunar + weather info

### Studio Ghibli Cube Girl UI
- **5 switchable color themes**: Sakura (pink), Matcha (green), Sky (blue), Lavender (purple), Peach (orange)
- Custom-drawn cube girl mascot that changes color with the theme
- Soft pastel Ghibli-inspired aesthetic throughout

### Analysis & Charts
- **Line chart**: cycle length over time
- **Bar chart**: period duration over time
- **Pie chart**: menstrual phase distribution
- Statistics: average/shortest/longest cycle, average duration

### Notifications
- Gentle Ghibli-style reminders when your period is expected within 2 days
- Daily check via AlarmManager + BroadcastReceiver
- Notification channel for Android O+

---

## Screenshots

| Login | Calendar | Charts | Settings |
|-------|----------|--------|----------|
| Cube girl mascot + fingerprint | Lunar + weather + period highlight | Line/Bar/Pie analysis | 5 color themes |

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
│       │   ├── MainActivity.java          # Calendar with lunar + weather
│       │   ├── ChartActivity.java         # MPAndroidChart analysis
│       │   ├── AddPeriodActivity.java     # Add/edit period records
│       │   ├── SettingsActivity.java      # Theme + notification toggles
│       │   ├── SessionManager.java        # SharedPreferences session
│       │   ├── LunarCalendar.java         # Chinese lunar conversion
│       │   ├── WeatherService.java        # Mock weather with seasonal model
│       │   ├── PeriodPredictor.java       # Next period / fertile window
│       │   ├── PeriodNotificationService.java
│       │   ├── NotificationReceiver.java
│       │   ├── ui/GhibliTheme.java        # 5 color themes
│       │   ├── ui/CubeGirlView.java       # Custom cube girl drawing
│       │   └── data/
│       │       ├── DatabaseHelper.java    # SQLite users + period_records
│       │       ├── User.java
│       │       └── PeriodRecord.java
│       └── res/
│           ├── layout/           # 8 activity layouts
│           ├── drawable/         # 50+ backgrounds, icons, cube girl mascots
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
| Build | Gradle 8.2, AGP 8.1.0 |

---

## Architecture

- **Data Layer**: `DatabaseHelper` manages SQLite with two tables (`users`, `period_records`)
- **Session**: `SessionManager` persists login state via SharedPreferences
- **Prediction**: `PeriodPredictor` calculates next period, fertile window, and ovulation from historical records
- **Weather**: `WeatherService` generates consistent mock data using city coordinates + seasonal model
- **Theme**: `GhibliTheme` manages 5 color palettes with SharedPreferences persistence

---

## License

MIT License. Feel free to use and modify.
