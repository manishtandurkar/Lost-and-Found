# Campus Lost & Found — Android Development Explained

This document explains every Android concept used in this app, starting from the very basics and building up progressively. If you understand basic programming (variables, functions, classes), you can follow everything here.

---

## Table of Contents

- [How an Android App is Structured — The Big Picture](#how-an-android-app-is-structured--the-big-picture)
- [1. AndroidManifest.xml — The App's ID Card](#1-androidmanifestxml--the-apps-id-card)
- [2. Activity Lifecycle — An Activity is Not Always "Running"](#2-activity-lifecycle--an-activity-is-not-always-running)
- [3. XML Layouts — Drawing the UI](#3-xml-layouts--drawing-the-ui)
- [4. Intents — How Screens Talk to Each Other](#4-intents--how-screens-talk-to-each-other)
- [5. SharedPreferences — Remembering Small Pieces of Data](#5-sharedpreferences--remembering-small-pieces-of-data)
- [6. MVVM Architecture — Keeping Code Organized](#6-mvvm-architecture--keeping-code-organized)
- [7. RecyclerView — Displaying a Scrollable List Efficiently](#7-recyclerview--displaying-a-scrollable-list-efficiently)
- [8. Room Database — Offline Data Storage](#8-room-database--offline-data-storage)
- [9. Firebase Realtime Database — Cloud Storage](#9-firebase-realtime-database--cloud-storage)
- [10. Firebase Authentication — Who Is the User?](#10-firebase-authentication--who-is-the-user)
- [11. Firebase Cloud Messaging (FCM) — Push Notifications](#11-firebase-cloud-messaging-fcm--push-notifications)
- [12. WorkManager — Reliable Background Tasks](#12-workmanager--reliable-background-tasks)
- [13. BroadcastReceiver — Listening for System Events](#13-broadcastreceiver--listening-for-system-events)
- [14. Service — Background Processing](#14-service--background-processing)
- [15. Google Maps SDK — Interactive Maps](#15-google-maps-sdk--interactive-maps)
- [16. Material Design 3 and Theming](#16-material-design-3-and-theming)
- [17. The Application Class — App-wide Initialization](#17-the-application-class--app-wide-initialization)
- [18. Permissions — Requesting Access to Device Features](#18-permissions--requesting-access-to-device-features)
- [19. Content Provider — Accessing the Photo Gallery](#19-content-provider--accessing-the-photo-gallery)
- [20. Threading — Keeping the UI Smooth](#20-threading--keeping-the-ui-smooth)
- [21. CoordinatorLayout — Smart Layout Coordination](#21-coordinatorlayout--smart-layout-coordination)
- [22. Navigation — Drawer, Bottom Nav, and Back Stack](#22-navigation--drawer-bottom-nav-and-back-stack)
- [23. Gradle — The Build System](#23-gradle--the-build-system)
- [24. Image Loading — Glide](#24-image-loading--glide)
- [25. BottomSheetDialog — The Report Options Popup](#25-bottomsheetdialog--the-report-options-popup)
- [Putting It All Together — A Complete User Journey](#putting-it-all-together--a-complete-user-journey)

---

## How an Android App is Structured — The Big Picture

Before diving into concepts, here is the mental model you need.

An Android app is a collection of **screens**. Each screen is an independent unit called an **Activity**. Activities are Java classes that the Android OS starts, pauses, and stops as the user navigates around.

When the user taps your app icon, the OS starts one Activity. When the user taps a button to go to another screen, your code starts another Activity. When the user presses Back, the OS destroys the top Activity and reveals the previous one.

The OS keeps track of all open Activities in a **back stack** — exactly like a stack of papers. The top paper is what the user sees. Back removes the top paper.

```
User sees:     [ItemDetailActivity]   ← currently on screen
               [MainActivity]
               [SplashActivity]       ← bottom of stack
```

This app has 9 Activities:

| Activity | What the user sees |
|---|---|
| `SplashActivity` | Launch screen — checks if already logged in |
| `LoginActivity` | "Sign in with Google" screen |
| `MainActivity` | The main feed of lost/found items |
| `ReportLostActivity` | Form to report a lost item |
| `ReportFoundActivity` | Form to report a found item |
| `ItemDetailActivity` | Full details of one item |
| `MapActivity` | Map with all item pins |
| `LocationPickerActivity` | Drop a pin to pick a location |
| `MyPostsActivity` | The current user's own posts |

---

## 1. AndroidManifest.xml — The App's ID Card

**What it is:**  
Before Android can run your app, it needs to know what's inside it. The `AndroidManifest.xml` file is a declaration file that tells the OS everything it needs to know before running a single line of Java code.

**What it contains in this app:**

- Every Activity is listed here. The OS cannot start an Activity that is not declared in the manifest — it simply won't exist as far as Android is concerned.
- The **entry point** (first screen) is marked with a special `intent-filter`:
  ```xml
  <activity android:name=".activities.SplashActivity" android:exported="true">
      <intent-filter>
          <action android:name="android.intent.action.MAIN" />
          <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
  </activity>
  ```
  `MAIN` + `LAUNCHER` means "this is the Activity to start when the user taps the app icon."

- Every **permission** the app needs is declared here (camera, internet, location). Without declaring a permission, the OS will deny the request even if you ask at runtime.

- The **Application class** (explained later), **Services**, **BroadcastReceivers**, and **FCM service** are all registered here.

- The **Google Maps API key** is stored here as metadata — the Maps SDK reads it automatically.

- `android:theme="@style/Theme.LostAndFound"` sets the default visual theme for all Activities. Individual Activities can override this.

---

## 2. Activity Lifecycle — An Activity is Not Always "Running"

**The problem it solves:**  
On a phone, things happen while your app is open — calls come in, the user gets a notification, they rotate the screen, they switch to another app. Android needs to control your Activity when these events happen. The **Activity Lifecycle** is the system Android uses to notify your code about these state changes.

**The key lifecycle methods:**

```
onCreate  →  onStart  →  onResume  (Activity is visible and interactive)
                            ↓
                         onPause   (Another Activity comes to foreground)
                            ↓
                         onStop    (Activity is no longer visible)
                            ↓
                         onDestroy (Activity is removed from memory)
```

**How this app uses lifecycle methods:**

`onCreate(Bundle savedInstanceState)` — Called once when the Activity is first created. This is where we:
- Call `setContentView(R.layout.activity_main)` to inflate the XML layout and show it on screen
- Find views with `findViewById(R.id.rvFeed)` to get references to UI elements
- Set up click listeners, RecyclerView adapters, and observe data

`onResume()` — Called every time the Activity comes back to the foreground — after creation AND after the user returns from another Activity. In `MainActivity`, we use this to reset the bottom navigation bar back to the Feed tab:
```java
@Override
protected void onResume() {
    super.onResume();
    bottomNav.setSelectedItemId(R.id.nav_feed);
}
```
This ensures that when the user goes to MapActivity and presses Back, the Feed tab appears selected rather than Map.

`onActivityResult(requestCode, resultCode, data)` — Called when another Activity you started returns a result. Used when:
- `LocationPickerActivity` returns the picked latitude, longitude, and address
- The image picker returns the selected photo's URI

`onSupportNavigateUp()` — Called when the user taps the back arrow in the toolbar. We override it to call `finish()` which destroys the current Activity and returns to the previous one.

**Screen rotation:** When the user rotates the phone, Android destroys and recreates the Activity. This would normally wipe all loaded data. The ViewModel pattern (explained in Section 6) solves this.

---

## 3. XML Layouts — Drawing the UI

**What it is:**  
Every Activity's visual appearance is defined in an XML file stored in `res/layout/`. XML layouts describe the hierarchy of views (buttons, text fields, images) and their properties.

**How an Activity connects to its layout:**
```java
// In onCreate():
setContentView(R.layout.activity_main);
```
Android inflates (parses and creates) the view hierarchy from the XML and displays it.

**Finding views in Java:**
```java
RecyclerView rv = findViewById(R.id.rvFeed);
TextView tvTitle = findViewById(R.id.tvDetailTitle);
```
`R.id.rvFeed` is an auto-generated integer constant. When you give a view `android:id="@+id/rvFeed"` in XML, Android generates `R.id.rvFeed` at build time.

**Key layout containers used:**
- `ConstraintLayout` — positions views using constraints (like "this button is 16dp below that text"). Used in LoginActivity.
- `LinearLayout` — stacks views horizontally or vertically in order. Used inside cards and dialogs.
- `CoordinatorLayout` — a smart FrameLayout that coordinates behaviour between child views (e.g., RecyclerView auto-adjusts its top padding to account for the toolbar height). Used in MainActivity, MyPostsActivity, ItemDetailActivity.
- `ScrollView` — wraps content that may be taller than the screen. Used in ItemDetailActivity so the user can scroll through all item details.

---

## 4. Intents — How Screens Talk to Each Other

**What it is:**  
An `Intent` is a message object that describes an operation to be performed. It is the primary way Activities communicate — either to start another Activity or to ask another app to do something.

### Explicit Intents — Going to a specific screen
You know exactly which Activity you want to open. You specify the class directly:
```java
Intent intent = new Intent(this, ItemDetailActivity.class);
intent.putExtra("item_id", "abc123");    // attach data
intent.putExtra("item_type", "lost");
startActivity(intent);
```
`putExtra` attaches key-value data to the intent. The receiving Activity reads it:
```java
String itemId = getIntent().getStringExtra("item_id");
```

### Implicit Intents — Asking the OS to find an app
You describe what you want done but don't specify which app does it. The OS shows a chooser if multiple apps can handle it.

**Open WhatsApp with a pre-filled message:**
```java
Intent intent = new Intent(Intent.ACTION_VIEW,
    Uri.parse("https://wa.me/919876543210?text=Hi!"));
intent.setPackage("com.whatsapp"); // specifically target WhatsApp
startActivity(intent);
```
If WhatsApp isn't installed, `ActivityNotFoundException` is thrown — we catch it and fall back to opening the URL in a browser.

**Pick an image from the gallery:**
```java
Intent intent = new Intent(Intent.ACTION_PICK,
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
startActivityForResult(intent, 200); // 200 = request code to identify this request
```

**Share text to any app:**
```java
Intent shareIntent = new Intent(Intent.ACTION_SEND);
shareIntent.putExtra(Intent.EXTRA_TEXT, "Lab record found at RVCE...");
shareIntent.setType("text/plain");
startActivity(Intent.createChooser(shareIntent, "Share via"));
```

### Getting results back — startActivityForResult + onActivityResult
When you start an Activity and need a result back (like a selected photo or a picked location), use `startActivityForResult()`. When that Activity calls `setResult(RESULT_OK, data)` and `finish()`, your `onActivityResult` is called:
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == 203 && resultCode == RESULT_OK) {
        double lat = data.getDoubleExtra("extra_lat", 0);
        double lng = data.getDoubleExtra("extra_lng", 0);
        String address = data.getStringExtra("extra_location_name");
    }
}
```
The `requestCode` tells you which Activity returned (203 = LocationPickerActivity, 200 = image picker).

### Deep Link Intent — Opening from a notification
`ItemDetailActivity` is declared with a URI scheme in the manifest:
```xml
<data android:scheme="lostandfound" android:host="item" />
```
This means the OS can open this Activity using a URL like `lostandfound://item?id=abc&type=lost`. FCM notifications use this to open a specific item when tapped:
```java
// In ItemDetailActivity onCreate:
Uri data = getIntent().getData();
if (data != null) {
    itemId = data.getQueryParameter("id");
    itemType = data.getQueryParameter("type");
}
```

---

## 5. SharedPreferences — Remembering Small Pieces of Data

**What it is:**  
`SharedPreferences` is Android's built-in key-value store for small persistent data. Think of it as a persistent `HashMap<String, Object>` that survives app restarts.

**Why we need it:**  
When the user logs in with Google, we don't want them to have to log in again every time they open the app. We save their session details to SharedPreferences.

**How `SessionManager` uses it:**
```java
// Saving session after login:
SharedPreferences.Editor editor = prefs.edit();
editor.putString("userId", uid);
editor.putString("userName", "Manish Tandurkar");
editor.putBoolean("isLoggedIn", true);
editor.apply(); // saves asynchronously
```

```java
// Reading on next app launch:
boolean loggedIn = prefs.getBoolean("isLoggedIn", false); // false = default
String userId = prefs.getString("userId", null);
```

**SplashActivity** reads `isLoggedIn` and routes accordingly:
- `true` → go to `MainActivity` (skip login)
- `false` → go to `LoginActivity`

`NewItemWorker` also uses SharedPreferences to store the timestamp of the last notification check, so it only notifies about truly new items.

---

## 6. MVVM Architecture — Keeping Code Organized

**The problem without architecture:**  
If you put everything in an Activity — network calls, database queries, UI updates, business logic — the Activity becomes enormous (1000+ lines), hard to test, and breaks on screen rotation.

**MVVM (Model-View-ViewModel)** separates concerns into three layers:

```
┌─────────────────────────────────┐
│  VIEW (Activity)                │  ← Only handles UI: shows data, receives taps
│  Observes LiveData              │
└────────────────┬────────────────┘
                 │ observes
┌────────────────▼────────────────┐
│  VIEWMODEL                      │  ← Holds UI data, survives rotation
│  Calls Repository               │
└────────────────┬────────────────┘
                 │ calls
┌────────────────▼────────────────┐
│  REPOSITORY                     │  ← Decides: Firebase or Room cache?
└───────────┬─────────────────────┘
            │              │
   ┌────────▼──────┐  ┌────▼──────────┐
   │  Firebase     │  │  Room (local) │
   └───────────────┘  └───────────────┘
```

**ViewModel** — A class that holds data for the UI. The crucial property: it **survives screen rotation**. When you rotate the phone, Android destroys and recreates the Activity, but the ViewModel is kept alive. The Activity simply re-observes the same ViewModel.

```java
// Activity gets the ViewModel (same instance after rotation):
FeedViewModel vm = new ViewModelProvider(this).get(FeedViewModel.class);
```

**LiveData** — An observable data wrapper. The Activity registers as an observer:
```java
vm.getAllCachedItems().observe(this, items -> {
    // This runs whenever 'items' changes
    adapter.setItems(items);
});
```
LiveData is **lifecycle-aware** — it only delivers updates when the Activity is visible (STARTED/RESUMED state). When the Activity is destroyed, the observer is automatically removed, preventing memory leaks. You never need to manually unsubscribe.

**MutableLiveData** — A LiveData that the ViewModel can set values on. Used in `ReportViewModel` to tell the Activity whether the post succeeded or failed:
```java
// In ViewModel:
public MutableLiveData<String> postStatus = new MutableLiveData<>();

// After Firebase write succeeds:
postStatus.postValue("success:" + itemId);
```
```java
// In Activity:
vm.postStatus.observe(this, status -> {
    if (status.startsWith("success")) {
        finish(); // go back
    } else {
        showError(status);
    }
});
```

---

## 7. RecyclerView — Displaying a Scrollable List Efficiently

**The problem:**  
The feed may have 100+ items. Creating 100 views at once would use enormous memory. `RecyclerView` solves this by only creating enough views to fill the screen (~10), then **recycling** (reusing) them as the user scrolls. When a card scrolls off the top, it is moved to the bottom and its data is updated.

**Three parts of RecyclerView:**

### LayoutManager
Decides how items are arranged. We use `LinearLayoutManager` for a vertical scrolling list:
```java
recyclerView.setLayoutManager(new LinearLayoutManager(this));
```

### ViewHolder
A ViewHolder holds references to the views inside one list item card. Without it, `findViewById()` would be called on every single bind — slow. With ViewHolder, references are cached once per card view:
```java
static class ItemViewHolder extends RecyclerView.ViewHolder {
    TextView tvTitle, tvCategory, tvLocation, tvDate, tvType, tvStatus;
    ImageView imgPhoto;
    
    ItemViewHolder(View itemView) {
        super(itemView);
        tvTitle = itemView.findViewById(R.id.tvItemTitle); // cached once
        tvCategory = itemView.findViewById(R.id.tvItemCategory);
        // ...
    }
}
```

### Adapter
The adapter is the bridge between the data list and the RecyclerView. It has three jobs:
1. **`onCreateViewHolder`** — inflate the XML card layout and create a ViewHolder (called ~10 times to fill screen)
2. **`onBindViewHolder`** — fill a recycled card with new data (called every time a card becomes visible)
3. **`getItemCount`** — tell RecyclerView how many items exist

```java
@Override
public void onBindViewHolder(ItemViewHolder holder, int position) {
    ItemEntity item = items.get(position);
    holder.tvTitle.setText(item.title);
    holder.tvCategory.setText(item.category);
    // set colors, load image, etc.
}
```

### DiffUtil — Smart list updates
Instead of calling `notifyDataSetChanged()` (which redraws every card), we use `DiffUtil` to compute exactly which items were added, removed, or changed. Only those specific cards are redrawn, giving smooth animations:
```java
// DiffUtil compares old and new lists:
// "Item at position 2 changed status" → only that card animates
DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback);
result.dispatchUpdatesTo(this); // triggers targeted notify calls
```

---

## 8. Room Database — Offline Data Storage

**What it is:**  
Room is Android's official library for storing structured data in a local SQLite database. SQLite is a lightweight database that lives entirely on the phone — no internet required.

**Why this app needs it:**  
When the user has no internet connection, the app still shows previously loaded items. Room stores a local copy (cache) of all Firebase items.

**Three components of Room:**

### Entity — What to store
An Entity is a Java class that maps to a database table. Each field maps to a column:
```java
@Entity(tableName = "items_cache")
public class ItemEntity {
    @PrimaryKey @NonNull
    public String id;          // column: id (primary key)
    
    public String title;       // column: title
    public String type;        // column: type ("lost" or "found")
    public String status;      // column: status ("active" or "resolved")
    public String postedByName;// column: posted_by_name
    public long timestamp;     // column: timestamp
    // ... more fields
}
```

### DAO (Data Access Object) — How to query
The DAO is an interface where you define your database operations as annotated methods. Room generates the actual SQL implementation at compile time — you never write raw SQL:
```java
@Dao
public interface ItemDao {
    @Query("SELECT * FROM items_cache ORDER BY timestamp DESC")
    LiveData<List<ItemEntity>> getAllItems(); // returns LiveData — auto-updates UI!
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ItemEntity> items); // insert or update
    
    @Query("DELETE FROM items_cache")
    void deleteAll();
    
    @Query("SELECT * FROM items_cache WHERE posted_by = :userId")
    LiveData<List<ItemEntity>> getItemsByUser(String userId);
}
```
The `@Query` with `LiveData` return type is powerful — Room automatically re-runs the query and emits a new value whenever the table data changes. The UI observing this LiveData updates automatically.

### Database — The database instance
```java
@Database(entities = {ItemEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ItemDao itemDao();
    
    public static AppDatabase getInstance(Context context) {
        // Singleton pattern — only one database instance per app
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context, AppDatabase.class, "lost_and_found_db")
                .fallbackToDestructiveMigration() // on schema change, wipe and re-sync
                .build();
        }
        return INSTANCE;
    }
}
```
`version = 2` — the schema version number. If you add a new column (like we added `posted_by_name`), you must increment this. With `fallbackToDestructiveMigration()`, Room simply drops and recreates the table — fine here since everything can be re-synced from Firebase.

**Important:** Room does not allow database access on the main (UI) thread. It would throw an `IllegalStateException`. All Room writes use an `Executor` to run on a background thread (see Section 17).

---

## 9. Firebase Realtime Database — Cloud Storage

**What it is:**  
Firebase Realtime Database (RTDB) is a cloud-hosted NoSQL database. Data is stored as a JSON tree and synchronized in real time across all connected clients.

**Structure used in this app:**
```
root/
├── users/
│   └── {userId}/           ← one entry per user
│       ├── name: "Manish"
│       └── fcmToken: "abc..."
│
├── lost_items/
│   └── {auto-generated-id}/
│       ├── title: "Pen"
│       ├── status: "active"
│       ├── postedBy: "uid123"
│       ├── postedByName: "Manish Tandurkar"
│       └── timestamp: 1718000000000
│
└── found_items/
    └── {auto-generated-id}/   ← same structure
```

**Writing an item:**
```java
DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference();
DatabaseReference itemRef = ref.child("lost_items").push(); // generates unique ID
item.setId(itemRef.getKey()); // store the ID back in the object
itemRef.setValue(item);       // serializes all getXxx() methods to JSON
```
Firebase serializes the `Item` Java object by calling all getter methods. `getTitle()` → stores as `"title"` in JSON. `getPostedByName()` → stores as `"postedByName"`. This is why getter method names must match field names exactly.

**Reading an item once:**
```java
ref.child("lost_items").child(itemId)
    .addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot snapshot) {
            Item item = snapshot.getValue(Item.class); // deserializes JSON → Item
            item.setId(snapshot.getKey());
        }
        @Override
        public void onCancelled(DatabaseError error) { }
    });
```

**Updating one field:**
```java
ref.child("lost_items").child(itemId).child("status").setValue("resolved");
```
This only updates the `status` field — it does not overwrite the entire item.

---

## 10. Firebase Authentication — Who Is the User?

**The flow:**

1. User taps "Sign in with Google"
2. `GoogleSignInClient` opens Google's account picker (a system screen, not our UI)
3. User selects their Google account
4. Google returns an `idToken` to our app in `onActivityResult`
5. We pass the token to Firebase: `firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))`
6. Firebase validates the token with Google's servers and creates a Firebase user session
7. We check the email domain — only `rvce.edu.in` is allowed:
```java
if (!email.endsWith("@rvce.edu.in")) {
    firebaseAuth.signOut();
    // show error: "Only rvce.edu.in accounts are allowed"
    return;
}
```
8. If allowed, we save the user's name, email, and photo to `/users/{uid}` in Firebase and to `SessionManager` (SharedPreferences)

This domain restriction ensures that only RVCE students can use the app. However, this check runs in the app code (client side). Firebase Security Rules should also enforce this on the server side for complete protection.

---

## 11. Firebase Cloud Messaging (FCM) — Push Notifications

**What it is:**  
FCM is Google's service for sending push notifications to Android devices. Notifications can be sent even when the app is closed.

**Two components in this app:**

### Topic subscription
All users subscribe to the topic `new_items` when the app starts:
```java
FirebaseMessaging.getInstance().subscribeToTopic("new_items");
```
A "topic" is like a mailing list. Any message sent to `new_items` is delivered to all subscribed devices. This means when a new item is posted, every app user gets a notification.

### FCMService
`FCMService extends FirebaseMessagingService`. This service runs in the background and is called by the Firebase SDK when a message arrives.

```java
@Override
public void onMessageReceived(RemoteMessage remoteMessage) {
    // Build and show a notification:
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(remoteMessage.getNotification().getTitle())
        .setContentText(remoteMessage.getNotification().getBody())
        .setContentIntent(pendingIntent); // opens ItemDetailActivity when tapped
    
    NotificationManagerCompat.from(this).notify(notificationId, builder.build());
}
```

**Notification Channel** — Android 8.0+ requires all notifications to belong to a `NotificationChannel`. Channels let users control notification behaviour (sound, vibration, importance) per category. This is created once:
```java
NotificationChannel channel = new NotificationChannel(
    "lost_found_channel",
    "Lost & Found Notifications",
    NotificationManager.IMPORTANCE_DEFAULT
);
notificationManager.createNotificationChannel(channel);
```

**Device token** — Each device has a unique FCM token. When the token is refreshed, `onNewToken(String token)` is called. We save it to `/users/{uid}/fcmToken` so the server can send notifications to a specific device.

---

## 12. WorkManager — Reliable Background Tasks

**The problem:**  
We want the app to periodically check Firebase for new items and show a notification — even when the app is not open. Simply starting a `Thread` or `Service` won't work reliably: Android kills background processes to save battery (Doze mode), and the task is lost after a device restart.

**WorkManager** is Android's solution for guaranteed background work. It respects battery optimization, survives app restarts, and reschedules tasks after a reboot.

**NewItemWorker:**
```java
public class NewItemWorker extends Worker {
    @Override
    public Result doWork() {
        // This runs on a background thread, guaranteed by WorkManager
        
        // 1. Get the timestamp of our last check
        SharedPreferences prefs = getApplicationContext()
            .getSharedPreferences("NewItemWorkerPrefs", MODE_PRIVATE);
        long lastCheck = prefs.getLong("last_check_ts", 15_minutes_ago);
        
        // 2. Query Firebase for items newer than lastCheck
        // 3. Show a notification for each new item
        // 4. Save the current time as the new lastCheck
        
        return Result.success();
    }
}
```

**Scheduling it:**
```java
WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "new_item_check",                       // unique name — only one instance runs at a time
    ExistingPeriodicWorkPolicy.KEEP,        // if already scheduled, don't reschedule
    new PeriodicWorkRequest.Builder(NewItemWorker.class, 15, TimeUnit.MINUTES).build()
);
```
15 minutes is the minimum interval Android allows. The OS may delay slightly to batch with other background work.

**CountDownLatch — Synchronizing async Firebase calls:**  
`doWork()` runs synchronously (must return `Result` when done). But Firebase calls are asynchronous (callbacks). We use `CountDownLatch` to bridge them:
```java
CountDownLatch latch = new CountDownLatch(2); // wait for 2 things

// Query 1: lost_items
db.child("lost_items").addListenerForSingleValueEvent(new ValueEventListener() {
    public void onDataChange(DataSnapshot s) {
        // process data...
        latch.countDown(); // signal: query 1 done
    }
});
// Query 2: found_items
db.child("found_items").addListenerForSingleValueEvent(new ValueEventListener() {
    public void onDataChange(DataSnapshot s) {
        // process data...
        latch.countDown(); // signal: query 2 done
    }
});

latch.await(20, TimeUnit.SECONDS); // block until both queries complete (max 20s)
```

---

## 13. BroadcastReceiver — Listening for System Events

**What it is:**  
A `BroadcastReceiver` listens for system-wide events (broadcasts) sent by Android or other apps. Your code runs when the event occurs.

**NetworkReceiver** in this app listens for two events:

1. `android.net.conn.CONNECTIVITY_CHANGE` — fired whenever the network state changes (connected/disconnected)
2. `android.intent.action.BOOT_COMPLETED` — fired when the device finishes booting

```java
public class NetworkReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (NetworkUtils.isOnline(context)) {
            // Device just got internet back → sync Firebase to Room
        }
    }
}
```

Registered in `AndroidManifest.xml` (static registration) — so it receives broadcasts even when the app is not running:
```xml
<receiver android:name=".receivers.NetworkReceiver">
    <intent-filter>
        <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

**Important:** `onReceive()` runs on the main (UI) thread and must complete in under 10 seconds. If we need to do longer work (like a Firebase sync), we start a `Service` or `WorkManager` job from here.

---

## 14. Service — Background Processing

**What it is:**  
A `Service` runs code in the background without a UI. Unlike WorkManager (which is for periodic tasks), a `Service` is started explicitly to do a specific job.

**MatchingService** runs the item-matching algorithm. When a new item is posted, `ReportViewModel` starts this service:
```java
Intent matchIntent = new Intent(context, MatchingService.class);
matchIntent.putExtra("new_item_id", item.getId());
context.startService(matchIntent);
```

The service queries Firebase for items of the opposite type, compares categories and keywords, and sends an FCM notification to the owner of any matching item.

**Thread warning:** A `Service` runs on the main thread by default — long operations (network calls) must be moved to a background thread using `Executor` or a worker thread to avoid freezing the UI.

---

## 15. Google Maps SDK — Interactive Maps

**Embedding a map:**  
Maps are added to layouts using `SupportMapFragment`. The fragment handles all the map tile loading. You request the `GoogleMap` object asynchronously:

```java
SupportMapFragment mapFragment =
    (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
mapFragment.getMapAsync(this); // calls onMapReady when ready
```

```java
@Override
public void onMapReady(GoogleMap map) {
    // The map is loaded and ready to use
    googleMap = map;
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
        new LatLng(12.9231, 77.4987), // RVCE campus coordinates
        16f                            // zoom level (higher = more zoomed in)
    ));
}
```

**Adding markers:**
```java
googleMap.addMarker(new MarkerOptions()
    .position(new LatLng(lat, lng))
    .title("Lab Record")
    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
```

**Custom markers in MapActivity:**  
Instead of the default red teardrop, we draw our own circular markers programmatically using Android's `Canvas` and `Paint` API:
```java
Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
Canvas canvas = new Canvas(bitmap);
Paint paint = new Paint();
paint.setColor(Color.RED);
canvas.drawCircle(48, 48, 44, paint); // draw a red circle
// Add text "?" or "✓" on top...
BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(bitmap);
```

**LocationPickerActivity** — pin-drop interaction:
```java
googleMap.setOnMapClickListener(latLng -> {
    googleMap.clear();                              // remove previous pin
    googleMap.addMarker(new MarkerOptions().position(latLng)); // place new pin
    reverseGeocode(latLng);                         // get address from coordinates
});
```

**Reverse Geocoding** — converting coordinates to a readable address:
```java
Geocoder geocoder = new Geocoder(this, Locale.getDefault());
List<Address> results = geocoder.getFromLocation(lat, lng, 1);
String address = results.get(0).getAddressLine(0); // "RVCE, Mysore Road..."
```
This is a network call and runs on a background `Thread`, then posts the result back to the main thread with `runOnUiThread()`.

---

## 16. Material Design 3 and Theming

**What is Material Design?**  
Material Design is Google's design language — a set of rules for how apps should look and behave (spacing, colors, typography, animations). Material 3 is the latest version, built into the `com.google.android.material` library.

**Components used:**
- `MaterialToolbar` — the top app bar with title and navigation icon
- `MaterialButton` — styled buttons (filled, outlined, text variants)
- `MaterialCardView` — cards with rounded corners and elevation
- `Chip` + `ChipGroup` — the filter pills (All / Lost / Found / My Posts)
- `BottomNavigationView` — the bottom tab bar
- `NavigationView` — the side drawer panel
- `Snackbar` — the pop-up message bar at the bottom (with optional action button)
- `BottomSheetDialog` — the panel that slides up from the bottom

**DayNight Theme — automatic dark/light mode:**

The app theme is `Theme.Material3.DayNight.NoActionBar`. "DayNight" means Material3 automatically switches between light and dark color schemes based on the phone's system setting.

To make the app follow the system setting:
```java
// In LostAndFoundApp (runs before any Activity):
AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
```

**Color variants for light and dark mode:**

We define two versions of every color:
```
res/values/colors.xml        → used when phone is in LIGHT mode
res/values-night/colors.xml  → used when phone is in DARK mode
```

Example:
```xml
<!-- res/values/colors.xml (light mode) -->
<color name="app_background">#FAFAFA</color>  <!-- near white -->
<color name="text_on_surface_primary">#212121</color>  <!-- near black -->

<!-- res/values-night/colors.xml (dark mode) -->
<color name="app_background">#121212</color>  <!-- near black -->
<color name="text_on_surface_primary">#FFFFFF</color>  <!-- white -->
```

In layouts, we always use the named color: `android:background="@color/app_background"`. Android picks the right file automatically — we never hardcode `#121212` or `#FFFFFF` in layouts.

**NoActionBar** — The `NoActionBar` part means the theme does not provide a built-in action bar. Each Activity sets up its own `MaterialToolbar` with `setSupportActionBar(toolbar)`. If you forget `NoActionBar` but still call `setSupportActionBar()`, the app crashes at runtime with `IllegalStateException: This Activity already has an action bar`.

---

## 17. The Application Class — App-wide Initialization

**What it is:**  
`Application` is a base class that Android instantiates before any Activity, Service, or Receiver. There is exactly one instance per app process. Its `onCreate()` is the earliest point where your code can run.

**`LostAndFoundApp extends Application`:**
```java
public class LostAndFoundApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
}
```

Setting the night mode here ensures it is applied before any Activity's window is created — preventing a brief flash of the wrong theme.

Registered in `AndroidManifest.xml`:
```xml
<application android:name=".LostAndFoundApp" ...>
```
Without this line, Android instantiates the default `Application` class and our `LostAndFoundApp.onCreate()` is never called.

---

## 18. Permissions — Requesting Access to Device Features

Android divides permissions into two groups:

**Normal permissions** — Low risk. Granted automatically at install. No user prompt needed.
- `INTERNET` — access the network
- `ACCESS_NETWORK_STATE` — check if online/offline
- `RECEIVE_BOOT_COMPLETED` — receive the boot broadcast

**Dangerous permissions** — Access private data or hardware. Must be requested at runtime; user sees a dialog.
- `ACCESS_FINE_LOCATION` — GPS location (for map)
- `CAMERA` — take photos
- `READ_MEDIA_IMAGES` — access gallery photos (API 33+)
- `POST_NOTIFICATIONS` — show notifications (API 33+)

Runtime permission request:
```java
if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
        new String[]{Manifest.permission.CAMERA}, RC_CAMERA_PERMISSION);
}
```

Result callback:
```java
@Override
public void onRequestPermissionsResult(int requestCode,
        String[] permissions, int[] grantResults) {
    if (requestCode == RC_CAMERA_PERMISSION) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            // user denied — show explanation
        }
    }
}
```

**`READ_MEDIA_IMAGES` vs `READ_EXTERNAL_STORAGE`:**  
Android 13 (API 33) replaced the broad `READ_EXTERNAL_STORAGE` with the more specific `READ_MEDIA_IMAGES`. Both are declared in the manifest; the correct one is requested based on `Build.VERSION.SDK_INT` at runtime.

---

## 19. Content Provider — Accessing the Photo Gallery

**What it is:**  
Android apps are sandboxed — they cannot directly access each other's files. A `ContentProvider` is the official mechanism for sharing data between apps. The Gallery app exposes photos through a ContentProvider; your app accesses them via a URI.

**Picking a photo:**
```java
Intent intent = new Intent(Intent.ACTION_PICK,
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
startActivityForResult(intent, RC_IMAGE_PICK);
```
The Gallery returns a URI like `content://media/external/images/media/42`. This is not a file path — it is a reference into the Gallery's ContentProvider.

**Using it with Firebase Storage:**  
Firebase Storage's `putFile(uri)` reads bytes directly from this ContentProvider URI via the `ContentResolver`. We never need to know the actual file path.

**FileProvider for camera:**  
When capturing a photo, we need to give the Camera app a location to save the file. We cannot give it a path inside our private app folder. `FileProvider` is a special ContentProvider that wraps our private file and shares it safely:
```java
Uri photoUri = FileProvider.getUriForFile(this,
    getPackageName() + ".fileprovider",
    new File(getCacheDir(), "photo.jpg")
);
camerIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
```

---

## 20. Threading — Keeping the UI Smooth

**The golden rule:** Never do slow work on the main (UI) thread.

Android draws the UI at 60 frames per second (16ms per frame). If the main thread is busy (network call, database query, file read), frames are dropped and the UI freezes. If the main thread is blocked for more than 5 seconds, Android shows an "App Not Responding" (ANR) dialog.

**Three threading tools used in this app:**

**`Executor`** — A thread pool for running Runnables on background threads. Used in `ItemRepository` for all Room database writes:
```java
Executor executor = Executors.newSingleThreadExecutor();

executor.execute(() -> {
    // This runs on a background thread
    itemDao.deleteAll();
    itemDao.insertAll(entities);
    // callback.onSuccess() must be called on the right thread
});
```

**`new Thread()`** — A simple one-shot background thread. Used in `LocationPickerActivity` for `Geocoder`:
```java
new Thread(() -> {
    String address = geocoder.getFromLocation(lat, lng, 1).get(0).getAddressLine(0);
    runOnUiThread(() -> {
        // Back on main thread — safe to update UI
        tvAddress.setText(address);
    });
}).start();
```

**WorkManager's thread** — `doWork()` in `NewItemWorker` is already called on a WorkManager background thread. No additional threading needed.

**`postValue()` vs `setValue()` on LiveData:**
- `setValue()` — must be called from the main thread
- `postValue()` — can be called from any thread (posts to main thread internally)

In `ReportViewModel`, after Firebase callbacks (which run on the main thread), we use `postValue()` defensively since `isLoading` and `postStatus` may be updated from executor threads too.

---

## 21. CoordinatorLayout — Smart Layout Coordination

**The problem it solves:**  
In screens with a toolbar at the top and a list below, content naturally goes under the toolbar. `CoordinatorLayout` solves this by coordinating behaviour between its children.

**Used in MainActivity, MyPostsActivity, ItemDetailActivity, MapActivity:**
```xml
<CoordinatorLayout>
    <AppBarLayout>
        <MaterialToolbar />       <!-- the toolbar -->
    </AppBarLayout>
    
    <RecyclerView
        app:layout_behavior="@string/appbar_scrolling_view_behavior" />
        <!-- ↑ This attribute tells the RecyclerView to start below the AppBarLayout -->
</CoordinatorLayout>
```

`appbar_scrolling_view_behavior` is a built-in `CoordinatorLayout.Behavior` that automatically sets the RecyclerView's top offset to equal the AppBarLayout's height. The content is never hidden under the toolbar.

`BottomNavigationView` inside `CoordinatorLayout` makes `Snackbar` slide up above it automatically — without any extra code.

---

## 22. Navigation — Drawer, Bottom Nav, and Back Stack

**NavigationDrawer:**  
`DrawerLayout` is the root container. It holds the main content AND the drawer panel side by side. The drawer starts off-screen and slides in from the left when the user taps the hamburger icon.

```java
// Hamburger icon toggles the drawer:
ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
    this, drawerLayout, toolbar, R.string.open, R.string.close);
drawerLayout.addDrawerListener(toggle);
toggle.syncState(); // animates hamburger ↔ arrow based on drawer state
```

Returning `false` from the item selected listener prevents the tapped item from staying highlighted:
```java
navView.setNavigationItemSelectedListener(item -> {
    startActivity(new Intent(this, MapActivity.class));
    drawerLayout.closeDrawer(GravityCompat.START);
    return false; // don't highlight this item as "selected"
});
```

**BottomNavigationView:**  
Three tabs — Feed, Map, My Posts. Since Map and My Posts are separate Activities (not Fragments), the selected tab is manually reset to Feed in `onResume()` whenever the user returns.

---

## 23. Gradle — The Build System

**What it does:**  
Gradle compiles your Java code, processes resources (XML, drawables), packages everything into an APK, and manages third-party library dependencies.

**Version catalog (`gradle/libs.versions.toml`):**  
All library versions are declared in one place:
```toml
[versions]
firebase-bom = "32.7.0"
workmanager = "2.9.1"
glide = "4.16.0"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebase-bom" }
work-runtime = { group = "androidx.work", name = "work-runtime", version.ref = "workmanager" }
```

In `app/build.gradle.kts`:
```kotlin
implementation(libs.work.runtime)
implementation(platform(libs.firebase.bom)) // BOM manages all Firebase versions
implementation(libs.firebase.database)
```

**`google-services` plugin:**  
Reads `app/google-services.json` (downloaded from Firebase Console) and automatically injects your Firebase project's configuration (project ID, API keys, etc.) into the build. Without this file, Firebase cannot connect to your project.

---

## 24. Image Loading — Glide

**The problem:**  
Loading images from URLs involves network calls, decoding, memory management, and caching — all of which are complex to do correctly. `Glide` handles all of this in one line.

```java
Glide.with(context)
    .load(item.getPhotoUrl())             // URL from Firebase Storage
    .placeholder(R.drawable.ic_image_placeholder) // shown while loading
    .into(imageView);                     // target view
```

Glide automatically:
- Downloads on a background thread
- Caches the decoded bitmap in memory (so scrolling is fast)
- Caches the downloaded file on disk (so reopening is fast)
- Cancels the load if the view is recycled (important in RecyclerView)

For the user's profile photo in the nav drawer, Glide applies a circular crop:
```java
Glide.with(this).load(photoUrl).circleCrop().into(ivNavPhoto);
```

---

## 25. BottomSheetDialog — The Report Options Popup

When the user taps the `+` FAB, a `BottomSheetDialog` slides up from the bottom of the screen. This is a Material Design dialog that appears anchored to the bottom edge — less intrusive than a full dialog.

```java
private void showReportDialog() {
    BottomSheetDialog sheet = new BottomSheetDialog(this);
    
    // Manually inflate the layout (unlike Activities, we inflate manually here)
    View view = getLayoutInflater().inflate(R.layout.dialog_report_options, null);
    sheet.setContentView(view);
    
    // Set up click listeners on the cards inside:
    view.findViewById(R.id.cardReportLost).setOnClickListener(v -> {
        sheet.dismiss();
        startActivity(new Intent(this, ReportLostActivity.class));
    });
    
    sheet.show();
}
```

The sheet automatically:
- Shows a drag handle at the top
- Dismisses when the user swipes down or taps outside
- Respects the system gesture navigation insets

---

## Putting It All Together — A Complete User Journey

Here is a complete walkthrough of one user action — posting a lost item — and which concept handles each step:

1. **User opens app** → `SplashActivity.onCreate()` reads `SessionManager` (SharedPreferences) → routes to `LoginActivity` or `MainActivity`

2. **User logs in** → `LoginActivity` → Google Sign-In (Implicit Intent) → Firebase Auth → domain check → `UserRepository` writes to Firebase → `SessionManager.saveSession()` → explicit Intent to `MainActivity`

3. **Feed loads** → `MainActivity` observes `FeedViewModel.getAllCachedItems()` (LiveData from Room) → `ItemRepository.syncFromFirebase()` pulls Firebase data → writes to Room on Executor thread → Room LiveData emits → RecyclerView adapter updates via DiffUtil

4. **User taps +** → `BottomSheetDialog` shows → user taps "Report Lost" → explicit Intent to `ReportLostActivity`

5. **User picks location** → explicit Intent to `LocationPickerActivity` (startActivityForResult) → user drops pin → `Geocoder` on background Thread → `setResult(RESULT_OK, intent)` → `onActivityResult` in `ReportLostActivity` receives lat/lng/address

6. **User picks photo** → implicit Intent `ACTION_PICK` → Gallery ContentProvider returns URI → stored as `selectedPhotoUri`

7. **User submits form** → `ReportViewModel.submitItem()` → `ItemRepository.postItem()` → Firebase Storage uploads photo → `itemRef.setValue(item)` writes to RTDB → Room cache updated on Executor thread → WorkManager notified → `MatchingService` started

8. **Friend receives notification** → Firebase sends to topic `new_items` → `FCMService.onMessageReceived()` → `NotificationCompat.Builder` shows notification → user taps notification → deep-link Intent opens `ItemDetailActivity` → Firebase lookup → `UserRepository.getUser()` for poster name → `ItemAdapter` shows "Posted by: Manish"

---

*End of EXPLANATION.md*
