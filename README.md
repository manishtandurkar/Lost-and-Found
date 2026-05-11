# Lost & Found — Campus Recovery Hub

An Android app that helps college students report, discover, and recover lost items on campus — with real-time matching, in-app chat, and map-based item discovery.

---

## Screenshots

> _Add screenshots here_

---

## Features

- **Google Sign-In** restricted to `rvce.edu.in` domain — no outsider access
- **Real-time feed** of lost & found items with filter chips (All / Lost / Found / My Posts)
- **Smart matching** — when a found item is reported, the app automatically notifies users whose lost items match by category and keywords
- **Google Maps view** — all items plotted as color-coded markers (red = lost, green = found)
- **In-app chat** — contact item posters directly without sharing personal info
- **Push notifications** via Firebase Cloud Messaging for matches and messages
- **Offline support** — Room SQLite cache keeps the feed available without a connection; syncs automatically when back online
- **Location autocomplete** via Google Places API when reporting items
- **Photo uploads** to Firebase Storage

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java |
| Architecture | MVVM + Repository |
| UI | Material Design 3, ConstraintLayout |
| Auth | Firebase Auth + Google Sign-In |
| Database | Firebase Realtime Database |
| Local cache | Room (SQLite) |
| Storage | Firebase Storage |
| Messaging | Firebase Cloud Messaging |
| Maps | Google Maps SDK + Places API |
| Image loading | Glide |
| Reactive data | LiveData + ViewModel |

---

## Architecture

```
UI (Activities)
    │
    ▼
ViewModels (FeedViewModel, ReportViewModel, ChatViewModel)
    │
    ▼
Repositories (ItemRepository, ChatRepository, UserRepository)
    │            │
    ▼            ▼
Firebase     Room DB
(RTDB)       (cache)
```

Background services:
- **MatchingService** — finds lost↔found keyword/category matches, sends FCM notifications
- **FCMService** — handles incoming push notifications, deep-links to item details
- **NetworkReceiver** — triggers a sync when connectivity is restored after being offline

---

## Project Structure

```
app/src/main/java/com/example/lostandfound/
├── activities/          # All screens (Login, Main, Report, Detail, Map, Chat…)
├── adapters/            # RecyclerView adapters
├── database/            # Room entity, DAO, and database class
├── models/              # Item, User, Message, NotificationItem
├── repository/          # Data access layer
├── services/            # MatchingService, FCMService, NetworkReceiver
├── utils/               # Constants, SessionManager, NetworkUtils
└── viewmodel/           # FeedViewModel, ReportViewModel, ChatViewModel
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17
- A Firebase project with Realtime Database, Auth, Storage, and FCM enabled
- Google Maps & Places API key

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
