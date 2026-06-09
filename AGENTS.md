# AGENTS.md — Campus Lost & Found System
> This file is for AI coding agents and developers to fully understand the current project scope, architecture, and implementation before writing any code. It reflects the **as-built** state of the app, not the original spec.

---

## 1. PROJECT OVERVIEW

**App Name:** Campus Lost & Found Hub
**Platform:** Android (Native Java)
**Minimum SDK:** API 26 (Android 8.0)
**Target SDK:** API 35 (Android 15)
**Architecture:** MVVM (Model-View-ViewModel)
**Backend:** Firebase Realtime Database, Firebase Storage, Firebase Auth, Firebase Cloud Messaging
**Local Storage:** Room (SQLite)
**Background Work:** WorkManager
**Build Tool:** Gradle (Kotlin DSL)
**Theme:** Material Design 3, DayNight (follows system dark/light mode)

---

## 2. PROJECT PURPOSE

A mobile app for RVCE (RV College of Engineering) students to report and recover lost and found items on campus. Login is restricted to `rvce.edu.in` Google accounts only. Replaces inefficient WhatsApp groups and notice boards with a structured, location-aware, notification-driven system.

---

## 3. IMPLEMENTED FEATURES

### 3.1 Authentication
- Google Sign-In via Firebase Authentication
- Domain restriction: only `rvce.edu.in` accounts are accepted; others are signed out immediately with an error message
- On login, user profile is written to `/users/{uid}` in Firebase RTDB (name, email, photoUrl, college, fcmToken)
- Session stored in SharedPreferences via `SessionManager`
- On logout, session is cleared and user is sent to `LoginActivity`

### 3.2 Home Feed (MainActivity)
- RecyclerView showing all lost + found items from Room cache
- Filter chips: All / Lost / Found / My Posts — centered in a single row
- Search bar filters by title, category, description
- Navigation drawer (hamburger) with: Map, My Posts, Logout
- Bottom navigation bar: Feed, Map, My Posts
- FAB (+) opens a BottomSheetDialog to choose Report Lost or Report Found
- Offline banner shown when no network; Room cache displayed instead
- NetworkReceiver triggers Firebase sync on connectivity restore
- Items without photos show no image placeholder

### 3.3 Report Lost / Report Found
- Form fields: Title, Category (dropdown), Description, Location (pin-on-map), Phone Number (mandatory), Photo (optional)
- Location picked via `LocationPickerActivity` — user drops a pin on Google Maps, reverse geocoding provides address string
- Phone number validated with regex `[0-9+\-\s]{7,15}` — must be provided
- On submit: item written to Firebase RTDB under `lost_items` or `found_items`; `postedByName` stored at write time from `SessionManager.getUserName()`
- Photo uploaded to Firebase Storage; URL stored in item

### 3.4 Item Detail Screen
- Full item details: photo (if any), title, type badge, status, category, description, location, date, map pin, posted-by name
- Map pin shown at zoom 16; gestures disabled
- **Contact on WhatsApp** button (visible to non-owners): opens WhatsApp with pre-filled contextual message based on item type
- **Mark as Resolved** button (visible to owner only): sets status to `resolved` in Firebase + Room
- **Share** button: shares item details via Android share intent
- `postedByName` read directly from item; falls back to `/users/{uid}` Firebase lookup for old items that predate the field

### 3.5 Map View (MapActivity)
- Shows all cached items as custom circular markers (red = lost, green = found)
- Default camera: RVCE campus at zoom 16
- Tapping a marker info window opens ItemDetailActivity
- Camera stays at RVCE — does not jump to last item's position

### 3.6 Location Picker (LocationPickerActivity)
- Google Maps with tap-to-drop-pin interaction
- Default center: RVCE campus (`12.9231, 77.4987`) at zoom 16
- Reverse geocoding via `Geocoder` on background thread
- Returns `EXTRA_LAT`, `EXTRA_LNG`, `EXTRA_LOCATION_NAME` to calling activity

### 3.7 My Posts (MyPostsActivity)
- RecyclerView of the current user's items filtered by userId
- CoordinatorLayout + AppBarLayout + MaterialToolbar layout
- Uses `Theme.LostAndFound` (NoActionBar) to avoid toolbar conflict

### 3.8 Push Notifications
- FCM topic `new_items`: all users subscribed on app launch
- **WorkManager** `NewItemWorker` runs every 15 minutes: queries Firebase for items newer than `last_check_ts` in SharedPreferences, fires local Android notifications, updates timestamp
- `FCMService` handles incoming FCM messages and deep-links to `ItemDetailActivity`
- No notifications tab or notification history screen — notifications are delivery-only

### 3.9 Smart Matching
- `MatchingService` runs on item post and compares new item's category + keywords against existing items of the opposite type
- On match, sends FCM notification to the matching item's owner

### 3.10 Offline Support
- Room DB (`items_cache` table, schema version 2) caches all feed items
- `FeedViewModel.getAllCachedItems()` observes Room LiveData
- `ItemRepository.syncFromFirebase()` replaces all Room rows on each sync
- Offline banner shown in feed when `NetworkUtils.isOnline()` returns false

---

## 4. REMOVED FEATURES (originally planned, now deleted)

| Feature | Reason removed |
|---|---|
| In-app chat (ChatActivity, ChatAdapter, ChatViewModel, ChatRepository) | Replaced by WhatsApp contact |
| Notifications tab / NotificationActivity | Removed; push delivery kept |
| Google Places Autocomplete text search | Replaced by pin-on-map picker |
| Firebase Cloud Functions | Requires Blaze plan; WorkManager used instead |

---

## 5. FIREBASE DATABASE STRUCTURE

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
│       ├── postedBy          (userId)
│       ├── postedByName      (display name, stored at write time)
│       ├── contactPreference (phone number — mandatory)
│       ├── status            ("active" or "resolved")
│       ├── type              ("lost")
│       └── timestamp         (epoch ms)
│
└── found_items/
    └── {itemId}/             (same fields, type = "found")
```

---

## 6. ROOM DATABASE

**Table: `items_cache`** (schema version 2)

| Column | Type |
|---|---|
| id | TEXT (PK) |
| type | TEXT |
| title | TEXT |
| category | TEXT |
| description | TEXT |
| location_name | TEXT |
| latitude | REAL |
| longitude | REAL |
| photo_url | TEXT |
| posted_by | TEXT |
| posted_by_name | TEXT |
| contact_preference | TEXT |
| status | TEXT |
| timestamp | INTEGER |
| last_synced_at | INTEGER |

`AppDatabase` uses `fallbackToDestructiveMigration()` — schema bumps wipe and re-sync.

---

## 7. KEY CLASSES

| Class | Purpose |
|---|---|
| `LostAndFoundApp` | Application subclass; sets `MODE_NIGHT_FOLLOW_SYSTEM` |
| `SessionManager` | SharedPreferences wrapper for user session |
| `Constants` | All string keys, DB node names, request codes |
| `ItemRepository` | Firebase read/write + Room cache sync |
| `UserRepository` | `/users/{uid}` read/write |
| `FeedViewModel` | LiveData for feed and my-posts lists |
| `ReportViewModel` | Submits new items to Firebase via `ItemRepository` |
| `NewItemWorker` | WorkManager worker; polls Firebase for new items, fires local notifications |
| `FCMService` | Handles incoming FCM push messages |
| `MatchingService` | Background service for lost↔found keyword matching |
| `NetworkReceiver` | BroadcastReceiver for connectivity changes |
| `LocationPickerActivity` | Pin-drop map picker; returns lat/lng/address |

---

## 8. THEMING

- Base theme: `Theme.Material3.DayNight.NoActionBar`
- `values/colors.xml` — light mode semantic colors (`app_background`, `card_background`, `text_on_surface_primary`, etc.)
- `values-night/colors.xml` — dark mode overrides for the same color names
- All layouts use named colors — no hardcoded hex except inside dialog cards that have permanently light backgrounds

---

## 9. NAVIGATION

```
SplashActivity
    │
    ├── (not logged in) → LoginActivity → MainActivity
    └── (logged in)     → MainActivity
                              │
                    ┌─────────┼──────────┐
                    │         │          │
                 MapActivity  │   MyPostsActivity
                              │
                         ItemDetailActivity
                              │
                         (WhatsApp / Share)
```

Bottom nav: Feed · Map · My Posts
Drawer: Map · My Posts · Logout
FAB: opens BottomSheetDialog → ReportLostActivity / ReportFoundActivity

---

## 10. PERMISSIONS

```xml
INTERNET
ACCESS_NETWORK_STATE
ACCESS_FINE_LOCATION
ACCESS_COARSE_LOCATION
CAMERA
READ_EXTERNAL_STORAGE        (maxSdkVersion 32)
READ_MEDIA_IMAGES
WRITE_EXTERNAL_STORAGE       (maxSdkVersion 28)
POST_NOTIFICATIONS
RECEIVE_BOOT_COMPLETED
```

---

## 11. KNOWN CONSTRAINTS

- `postedByName` only populated for items posted after the field was added; old items fall back to `/users/{uid}` lookup
- WorkManager minimum interval is 15 minutes (Android OS enforced)
- No Firebase Cloud Functions (Spark plan limitation) — all background work is client-side
- Domain restriction enforced client-side only; Firebase Security Rules should also enforce it
- No admin dashboard, no multi-campus support, English only

---

*End of AGENTS.md*
