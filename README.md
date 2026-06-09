# Lost & Found — Campus Recovery Hub

An Android app that helps RVCE students report, discover, and recover lost items on campus — with real-time feed, map-based discovery, WhatsApp contact, and push notifications.

---

## Screenshots

> _Add screenshots here_

---

## Features

- **Google Sign-In** restricted to `rvce.edu.in` domain — no outsider access
- **Real-time feed** of lost & found items with filter chips (All / Lost / Found / My Posts)
- **Pin-on-map location picker** — drop a pin on Google Maps when reporting an item
- **Google Maps view** — all items plotted as custom color-coded markers (red = lost, green = found), centered on RVCE campus
- **WhatsApp contact** — tap Contact on any item to open WhatsApp with a pre-filled message to the poster
- **Mandatory phone number** on every report — ensures contactability
- **Only the poster can mark items as Resolved**
- **Push notifications** via Firebase Cloud Messaging — all users get notified on every new post (WorkManager polls every 15 min as a local fallback; FCM topic `new_items` used for server-side delivery)
- **Smart matching** — when a found item is reported, the app automatically notifies users whose lost items match by category and keywords
- **Offline support** — Room SQLite cache keeps the feed available without a connection; syncs automatically when back online
- **Photo uploads** to Firebase Storage; items without photos show no placeholder
- **DayNight theme** — follows system dark/light mode throughout the entire app
- **Navigation drawer** + bottom navigation bar

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 15 (API 35) |
| Architecture | MVVM + Repository |
| UI | Material Design 3 (DayNight), ConstraintLayout |
| Auth | Firebase Auth + Google Sign-In |
| Database | Firebase Realtime Database |
| Local cache | Room (SQLite) |
| Storage | Firebase Storage |
| Messaging | Firebase Cloud Messaging (topic: `new_items`) |
| Background work | WorkManager (periodic 15 min polling) |
| Maps | Google Maps SDK |
| Image loading | Glide |
| Reactive data | LiveData + ViewModel |

---

## Architecture

```
UI (Activities)
    │
    ▼
ViewModels (FeedViewModel, ReportViewModel)
    │
    ▼
Repositories (ItemRepository, UserRepository)
    │            │
    ▼            ▼
Firebase     Room DB
(RTDB)       (cache)
```

Background components:
- **NewItemWorker** (WorkManager) — polls Firebase every 15 min for new items, fires local notifications
- **MatchingService** — finds lost↔found keyword/category matches, sends FCM notifications
- **FCMService** — handles incoming push notifications, deep-links to item details
- **NetworkReceiver** — triggers a sync when connectivity is restored after being offline

---

## Firebase Database Structure

```
root/
├── users/
│   └── {userId}/
│       ├── name
│       ├── email
│       ├── profilePhotoUrl
│       ├── college
│       └── fcmToken
│
├── lost_items/
│   └── {itemId}/
│       ├── title
│       ├── category
│       ├── description
│       ├── locationName
│       ├── latitude
│       ├── longitude
│       ├── photoUrl
│       ├── postedBy        (userId)
│       ├── postedByName    (display name stored at post time)
│       ├── contactPreference (phone number)
│       ├── status          (active / resolved)
│       └── timestamp
│
└── found_items/
    └── {itemId}/           (same fields as lost_items)
```

---

## Project Structure

```
app/src/main/java/com/example/lostandfound/
├── activities/       LoginActivity, MainActivity, ReportLostActivity,
│                     ReportFoundActivity, ItemDetailActivity,
│                     MapActivity, LocationPickerActivity, MyPostsActivity
├── adapters/         ItemAdapter
├── database/         AppDatabase, ItemDao, ItemEntity
├── models/           Item, User
├── notifications/    FCMService
├── receivers/        NetworkReceiver
├── repository/       ItemRepository, UserRepository
├── services/         MatchingService
├── utils/            Constants, SessionManager, NetworkUtils
├── viewmodels/       FeedViewModel, ReportViewModel
├── workers/          NewItemWorker
└── LostAndFoundApp.java
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17
- A Firebase project with Realtime Database, Auth, Storage, and FCM enabled
- Google Maps API key

### Setup

1. **Clone the repo**
   ```bash
   git clone https://github.com/manishtandurkar/Lost-and-Found.git
   cd Lost-and-Found
   ```

2. **Add Firebase config**

   Download `google-services.json` from your Firebase project console and place it at `app/google-services.json`.

3. **Add your API key**

   In `app/src/main/res/values/strings.xml`, set:
   ```xml
   <string name="google_maps_key">YOUR_MAPS_API_KEY</string>
   ```

4. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or hit **Run** in Android Studio.

---

## CI / CD

GitHub Actions builds a debug APK on every push to `main` or any `claude/**` branch, and automatically creates a GitHub Release with an unsigned release APK on every push to `main`.

Workflow: [.github/workflows/build-release.yml](.github/workflows/build-release.yml)

---

## Requirements

- **Min SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 15 (API 35)
- Internet connection for real-time features; offline cache available without one

---

## License

MIT
