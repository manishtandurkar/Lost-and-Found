# AGENTS.md — College Lost & Found System
> This file is intended for AI coding agents and developers to fully understand the project scope, architecture, features, and implementation guidelines before writing any code.

---

## 1. PROJECT OVERVIEW

**App Name:** College Lost & Found System
**Platform:** Android (Native)
**Language:** Java (preferred) or Kotlin
**Minimum SDK:** API 21 (Android 5.0 Lollipop)
**Target SDK:** API 34 (Android 14)
**Architecture Pattern:** MVVM (Model-View-ViewModel)
**Backend:** Firebase (Realtime Database, Storage, Authentication, Cloud Messaging)
**Local Storage:** SQLite via Room Persistence Library
**Build Tool:** Gradle

---

## 2. PROJECT PURPOSE

The College Lost & Found System is an Android mobile application that provides a centralized, real-time platform for college students to report lost or found items on campus. It replaces inefficient methods like WhatsApp groups and physical notice boards with a structured, verified, and location-aware system.

---

## 3. USER ROLES

### 3.1 Student (General User)
- Can register and log in using their college Google account
- Can post lost item reports with photo, description, and map location
- Can post found item reports with photo, description, and map location
- Can browse the live feed of all lost and found items
- Can receive notifications when a matching item is posted
- Can initiate and participate in in-app chat with other users
- Can view their own post history

### 3.2 Admin
- Has all Student permissions
- Can mark any item post as Resolved or Closed
- Can delete spam or fake posts
- Can send broadcast notifications to all users
- Has access to a basic admin dashboard showing item statistics

---

## 4. CORE FEATURES (MVP — MUST BUILD)

These are the features that must be built for the minimum viable product:

### 4.1 Authentication
- Google Sign-In using Firebase Authentication
- Domain restriction — only college Google accounts are permitted to log in
- On first login, create a user profile document in Firebase Realtime Database
- Store login session in Shared Preferences to skip login on relaunch
- On logout, clear session from Shared Preferences

### 4.2 Home Feed (MainActivity)
- Display all active lost and found item posts in a RecyclerView
- Each card in the feed must show: item photo thumbnail, title, category, location name, date posted, and status (Lost or Found)
- Feed must be fetched from Firebase Realtime Database using a real-time listener
- Feed must also load from SQLite local cache when the device is offline
- A Broadcast Receiver must detect network connectivity changes and trigger a Firebase sync when connection is restored
- Provide filter options: All, Lost Only, Found Only, My Posts

### 4.3 Report Lost Item
- Dedicated Activity with a form containing the following fields:
  - Item Title (text)
  - Category (dropdown: Electronics, Accessories, Books, ID Card, Clothing, Bag, Other)
  - Description (multiline text)
  - Last Seen Location (selected via Google Maps picker)
  - Photo (optional — selected from gallery via Content Provider or captured via Camera Intent)
  - Contact Preference (In-App Chat or Phone Number)
- On submission, data is written to Firebase Realtime Database under the lost_items node
- The item is also cached in SQLite locally

### 4.4 Report Found Item
- Same form structure as Report Lost Item with the following differences:
  - Location field label changes to "Found At Location"
  - Photo is strongly encouraged (UI hint shown)
  - Photo is uploaded to Firebase Storage and the download URL is saved in the database
- On submission, the Matching Algorithm is triggered (see Section 4.6)

### 4.5 Item Detail Screen
- Launched when user taps any item card in the feed
- Shows full item details: large photo, title, category, full description, map pin of location, posted by (name + profile photo), date and time
- Shows a Contact button which opens ChatActivity if the viewer is not the post owner
- Shows a Mark as Resolved button if the viewer is the post owner

### 4.6 Smart Matching (Basic)
- Runs as a Background Service whenever a new item is posted
- Matching logic: compare the category and keywords in the title/description of the new post against all existing posts of the opposite type
- Example: A new Found post with category "Electronics" and title "black earphones" should match against Lost posts with category "Electronics"
- If a match is found, send a Push Notification via Firebase Cloud Messaging to the owner of the matching post

### 4.7 In-App Chat
- Simple one-to-one chat between two users regarding a specific item
- Chat data stored in Firebase Realtime Database under a chats node
- Messages display sender name, message text, and timestamp
- RecyclerView used to display the chat thread
- Chat is initiated from the Item Detail Screen

### 4.8 Push Notifications
- Firebase Cloud Messaging used to deliver notifications
- Notification types:
  - Match Alert: Sent when a potential match is found for a user's post
  - Claim Request: Sent when someone initiates a chat about your post
  - Admin Broadcast: Sent by admin to all users
- Tapping a notification must deep-link to the relevant Item Detail Screen using an explicit Intent

### 4.9 Google Maps Integration
- Used in the Report Lost and Report Found forms as a location picker
- Used in the Item Detail Screen to show a static map pin of the item's location
- Used in a dedicated Map View screen showing all active item pins
- Lost items shown as red pins, Found items shown as green pins

### 4.10 Offline Support
- SQLite database (via Room) caches all feed items locally
- When the device is offline, the app loads from SQLite and shows an offline banner
- When connectivity is restored (detected via Broadcast Receiver), Firebase sync is triggered and SQLite is updated

---

## 5. SECONDARY FEATURES (BUILD IF TIME PERMITS)

- Search bar in the feed to search items by title or keyword
- Profile screen showing user's name, photo, and their post history
- Item expiry — posts older than 30 days are automatically archived
- Notification history screen showing past alerts
- Share item — share item details via Android Share Intent to WhatsApp or other apps

---

## 6. FEATURES TO SKIP (OUT OF SCOPE)

The following are explicitly out of scope and must not be built:

- Admin dashboard with complex analytics
- AlarmManager-based periodic background recheck
- SMS fallback messaging
- Tablet or multi-window form factor support
- Payment or reward system
- Machine learning-based image matching

---

## 7. SCREENS AND NAVIGATION

### Screen List
| Screen Name | Activity/Fragment | Description |
|---|---|---|
| Splash Screen | SplashActivity | Checks Shared Preferences for session, routes to Login or Main |
| Login Screen | LoginActivity | Google Sign-In button, handles OAuth flow |
| Home Feed | MainActivity | RecyclerView feed, filter tabs, FAB for reporting |
| Report Lost Item | ReportLostActivity | Form for reporting a lost item |
| Report Found Item | ReportFoundActivity | Form for reporting a found item |
| Item Detail | ItemDetailActivity | Full details of a single item post |
| Map View | MapActivity | Google Maps with all item pins |
| Chat | ChatActivity | One-to-one chat between two users |
| My Posts | MyPostsActivity | List of the current user's posts |
| Notifications | NotificationActivity | History of received notifications |

### Navigation Flow
- SplashActivity checks session → routes via Intent to LoginActivity or MainActivity
- MainActivity has a Bottom Navigation Bar with: Feed, Map, My Posts, Notifications
- Floating Action Button on MainActivity opens a dialog to choose Report Lost or Report Found
- Tapping any feed item opens ItemDetailActivity via explicit Intent, passing the item ID
- Contact button in ItemDetailActivity opens ChatActivity via explicit Intent
- Tapping a Push Notification opens ItemDetailActivity directly via deep link Intent

---

## 8. FIREBASE STRUCTURE

### 8.1 Realtime Database Schema

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
│       ├── postedBy (userId)
│       ├── contactPreference
│       ├── status (active / resolved)
│       └── timestamp
│
├── found_items/
│   └── {itemId}/
│       ├── title
│       ├── category
│       ├── description
│       ├── locationName
│       ├── latitude
│       ├── longitude
│       ├── photoUrl
│       ├── postedBy (userId)
│       ├── contactPreference
│       ├── status (active / resolved)
│       └── timestamp
│
└── chats/
    └── {chatId}/
        ├── participants[] (userId array)
        ├── itemId (reference to the item being discussed)
        └── messages/
            └── {messageId}/
                ├── senderId
                ├── text
                └── timestamp
```

### 8.2 Firebase Storage Structure
```
gs://your-app.appspot.com/
└── item_photos/
    └── {itemId}.jpg
```

---

## 9. LOCAL DATABASE (SQLite via Room)

### 9.1 Tables

**items_cache table**
- id (primary key, text)
- type (lost or found)
- title
- category
- description
- location_name
- latitude
- longitude
- photo_url
- posted_by
- status
- timestamp
- last_synced_at

**user_session table**
- user_id (primary key)
- name
- email
- profile_photo_url
- fcm_token

---

## 10. ANDROID CONCEPTS USED (SYLLABUS MAPPING)

This section maps each Android concept to where it is used in the app:

| Android Concept | Where Used in App |
|---|---|
| Activity & Activity Lifecycle | All screens are separate Activities with proper lifecycle handling |
| Intents (Explicit) | Navigation between all screens |
| Intents (Implicit) | Camera intent for photo capture, Share intent |
| Shared Preferences | Storing and checking login session |
| RecyclerView + Adapter + ViewHolder | Home Feed, Chat thread, My Posts list |
| Content Provider | Accessing device gallery for photo selection |
| SQLite / Room Database | Offline caching of feed items |
| Background Service | Running the item matching algorithm |
| Broadcast Receiver | Detecting network connectivity changes |
| AsyncTask / Executor | Performing network and database operations off the main thread |
| Notifications | Push notifications via Firebase Cloud Messaging |
| Google Maps API | Location picker, item pin display, Map View screen |
| Firebase Authentication | Google Sign-In |
| Firebase Realtime Database | Live feed, chat messages, user profiles |
| Firebase Storage | Storing item photos |
| Firebase Cloud Messaging | Delivering push notifications |
| Material Design | All UI components — Cards, FAB, Bottom Navigation, Snackbar |
| Styles and Themes | App-wide light theme with college color branding |

---

## 11. PERMISSIONS REQUIRED

The following permissions must be declared in AndroidManifest.xml:

- INTERNET — for all network operations
- ACCESS_NETWORK_STATE — for connectivity checks in Broadcast Receiver
- ACCESS_FINE_LOCATION — for Google Maps location picker
- ACCESS_COARSE_LOCATION — fallback location permission
- CAMERA — for capturing photos of found items
- READ_EXTERNAL_STORAGE — for selecting photos from gallery
- WRITE_EXTERNAL_STORAGE — for saving temporary image files (API < 29)
- POST_NOTIFICATIONS — for displaying push notifications (API 33+)
- RECEIVE_BOOT_COMPLETED — for restarting services after device reboot

---

## 12. UI / UX GUIDELINES

- Follow Material Design 3 guidelines throughout
- Color scheme: Primary color should reflect college branding (suggest deep blue or maroon)
- Lost item indicators: Red accent color
- Found item indicators: Green accent color
- Typography: Use Roboto font throughout
- All forms must have proper input validation with error messages shown inline
- All network operations must show a loading spinner
- Empty states must be handled — show a helpful illustration when the feed is empty
- The app must handle configuration changes (screen rotation) gracefully using ViewModel

---

## 13. ERROR HANDLING GUIDELINES

- All Firebase read/write operations must have failure listeners
- If Firebase write fails, show a Snackbar with a retry option
- If photo upload fails, allow the user to submit the post without a photo
- If Google Maps fails to load, show a text input fallback for location entry
- If the device is offline on launch, show a banner and load from SQLite cache
- Authentication errors must redirect the user back to the Login screen cleanly

---

## 14. SECURITY GUIDELINES

- Firebase Security Rules must be configured so that:
  - Only authenticated users can read or write any data
  - Users can only edit or delete their own posts
  - Admin role is determined by a flag in the user's Firebase profile
- College domain restriction must be enforced both on the client side and via Firebase Security Rules
- User phone numbers must never be stored in Firebase — only used for the contact preference field if the user explicitly provides it
- Chat messages must only be readable by the two participants of that chat

---

## 15. TESTING REQUIREMENTS

- Test Google Sign-In with a valid college Google account
- Test posting a lost item with and without a photo
- Test posting a found item and verify the matching algorithm triggers
- Test push notification delivery on a real device
- Test the app fully in offline mode — all cached items must be visible
- Test network restoration — feed must sync automatically when internet returns
- Test chat between two separate user accounts
- Test admin broadcast notification
- Test all form validations with empty and invalid inputs

---

## 16. DEVELOPMENT PHASES

### Phase 1 — Setup & Auth
- Create Android project with MVVM structure
- Configure Firebase project and connect to app
- Implement Splash screen with Shared Preferences session check
- Implement Google Sign-In with domain restriction
- Create user profile in Firebase on first login

### Phase 2 — Core Feed
- Design and implement the RecyclerView feed in MainActivity
- Connect feed to Firebase Realtime Database with live listener
- Implement SQLite Room database for offline caching
- Implement Broadcast Receiver for network change detection

### Phase 3 — Reporting
- Build Report Lost Item form with Google Maps location picker
- Build Report Found Item form with photo upload via Content Provider
- Connect both forms to Firebase Realtime Database and Storage

### Phase 4 — Matching & Notifications
- Implement Background Service for matching algorithm
- Integrate Firebase Cloud Messaging for push notifications
- Implement notification deep linking to Item Detail screen

### Phase 5 — Chat & Polish
- Build ChatActivity with Firebase Realtime Database
- Implement Map View screen with all item pins
- Add filter tabs to feed
- Polish UI with Material Design components, animations, and empty states
- Fix all bugs and test on real device

---

## 17. FOLDER STRUCTURE

The project must follow this package structure:

- activities — all Activity classes
- fragments — all Fragment classes
- adapters — RecyclerView adapters
- models — data model classes
- viewmodels — ViewModel classes for MVVM
- repository — data access layer (Firebase + Room)
- database — Room database, DAOs, and entity classes
- services — Background Service classes
- receivers — Broadcast Receiver classes
- utils — helper and utility classes
- notifications — FCM service and notification builder classes
- ui/theme — color, typography, and theme files

---

## 18. THIRD-PARTY DEPENDENCIES

The following libraries must be added to the Gradle build file:

- Firebase BOM (Bill of Materials) — for managing all Firebase library versions
- Firebase Authentication
- Firebase Realtime Database
- Firebase Storage
- Firebase Cloud Messaging
- Google Maps SDK for Android
- Google Places SDK for Android
- Google Sign-In SDK
- Glide — for image loading and caching
- Room — for local SQLite database
- Retrofit — optional, only if any REST API is added later
- Material Components for Android

---

## 19. KNOWN CONSTRAINTS

- The app is designed for a single college campus — no multi-campus support needed
- The matching algorithm is category-based only — no NLP or ML required
- Chat is one-to-one only — no group chats
- There is no in-app payment or reward mechanism
- The app is in English only — no localization needed
- Admin role is manually assigned — no admin registration flow

---

## 20. DEMO SCRIPT (FOR EVALUATION)

When demonstrating the app to an evaluator, follow this script:

1. Open the app — Splash screen checks session
2. Log in with a college Google account
3. Browse the home feed — show RecyclerView with item cards
4. Turn off internet — show that cached items still load from SQLite
5. Turn internet back on — show automatic sync
6. Post a Found Item (e.g., "black earphones, Electronics category, Library")
7. On a second device logged in with a different account, have a Lost Item post for "earphones, Electronics" already submitted
8. Show the Push Notification arriving on the second device
9. Tap the notification — show it deep-links to the Item Detail screen
10. Tap Contact — open ChatActivity and send a message
11. Show the message arriving in real-time on the first device
12. Mark the item as Resolved
13. Show the pin on the Map View screen

---

*End of AGENTS.md*
