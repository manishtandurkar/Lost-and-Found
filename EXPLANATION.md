# Android Application Development — Technical Explanation
## Campus Lost & Found Hub

This document explains every Android development concept applied in this project in technical detail, with direct references to how each concept is implemented in the codebase.

---

## 1. Application Architecture — MVVM

### What is MVVM?
Model-View-ViewModel (MVVM) is an architectural pattern that separates the UI (View) from the business logic and data (Model) through an intermediary layer (ViewModel). Android Jetpack's `ViewModel` and `LiveData` classes provide the framework-level support for MVVM.

### How it is applied here

**Model** — `Item`, `User`, `ItemEntity`  
Plain Java objects (POJOs) that represent data. `Item` is the Firebase model. `ItemEntity` is the Room entity (local database model). They are separate because Firebase and Room have different serialization requirements.

**View** — Activities (`MainActivity`, `ItemDetailActivity`, `ReportLostActivity`, etc.)  
Activities only know how to display data and respond to user interaction. They observe LiveData from the ViewModel and update the UI when data changes. They never talk to Firebase or Room directly.

**ViewModel** — `FeedViewModel`, `ReportViewModel`  
Holds UI-related data and survives configuration changes (screen rotation). Communicates with the Repository layer.

```
Activity (observes LiveData)
    ↕ LiveData
ViewModel (holds state, calls repository)
    ↕ method calls / callbacks
Repository (abstracts data source)
    ↕                  ↕
Firebase RTDB       Room DB
```

**Repository** — `ItemRepository`, `UserRepository`  
Single source of truth for data. Decides whether to fetch from Firebase or Room. The ViewModel never knows which source was used.

### Why MVVM over MVC/MVP?
- ViewModel survives configuration changes; an Activity-based controller does not
- LiveData is lifecycle-aware — no memory leaks from observers left after Activity destruction
- Repository pattern makes the data layer independently testable

---

## 2. Activity and Activity Lifecycle

### What is an Activity?
An Activity is a single screen with a user interface. It is a subclass of `AppCompatActivity` (which extends `Activity`). Android manages Activities in a back stack.

### Activities in this project

| Activity | Purpose |
|---|---|
| `SplashActivity` | Entry point; checks session and routes to Login or Main |
| `LoginActivity` | Google Sign-In flow |
| `MainActivity` | Home feed, navigation hub |
| `ReportLostActivity` | Form to report a lost item |
| `ReportFoundActivity` | Form to report a found item |
| `ItemDetailActivity` | Full details of a single item |
| `MapActivity` | Full-screen map of all items |
| `LocationPickerActivity` | Pin-drop map picker |
| `MyPostsActivity` | Current user's own posts |

### Lifecycle methods used

**`onCreate(Bundle)`**  
Called once when the Activity is created. Used to call `setContentView()`, bind views with `findViewById()`, set up RecyclerView adapters, observe LiveData, and attach click listeners.

**`onResume()`**  
Called every time the Activity comes to the foreground — after creation and after returning from another Activity. Used in `MainActivity.onResume()` to reset the bottom navigation selected item to Feed tab, so when the user navigates back from MapActivity or MyPostsActivity, the Feed tab is always shown as active.

**`onActivityResult(int, int, Intent)`**  
Called when a started Activity returns a result. Used in `ReportLostActivity` and `ReportFoundActivity` to receive the result from `LocationPickerActivity` (lat/lng/address) and image picker (photo URI). The request code distinguishes which Activity returned.

**`onSupportNavigateUp()`**  
Called when the user taps the Up (back arrow) button in the toolbar. Overridden in every secondary Activity to call `finish()`, which pops the Activity off the back stack and returns to the previous screen.

### Configuration changes
ViewModels survive rotation because they are stored in `ViewModelStore`, which is not destroyed on configuration changes. This means the fetched item list in `FeedViewModel` is retained across screen rotation without re-fetching from Firebase.

---

## 3. Intents

### Explicit Intents
Used to navigate between Activities within the same app. The target component class is specified directly.

```java
// Navigating to ItemDetailActivity with data
Intent intent = new Intent(this, ItemDetailActivity.class);
intent.putExtra(Constants.EXTRA_ITEM_ID, item.id);
intent.putExtra(Constants.EXTRA_ITEM_TYPE, item.type);
startActivity(intent);
```

The receiving Activity reads extras with `getIntent().getStringExtra(key)`.

### Implicit Intents
Used to invoke functionality from other apps without knowing which app handles it.

**Image picker** — `ReportLostActivity` / `ReportFoundActivity`:
```java
Intent intent = new Intent(Intent.ACTION_PICK,
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
startActivityForResult(intent, RC_IMAGE_PICK);
```
The OS presents a chooser; the user picks an image; the URI is returned to `onActivityResult`.

**Camera capture**:
```java
Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
startActivityForResult(intent, RC_IMAGE_CAPTURE);
```

**WhatsApp contact** — `ItemDetailActivity`:
```java
Intent intent = new Intent(Intent.ACTION_VIEW,
    Uri.parse("https://wa.me/" + phone + "?text=" + Uri.encode(message)));
intent.setPackage("com.whatsapp"); // targets WhatsApp specifically
startActivity(intent);
```
If WhatsApp is not installed, `ActivityNotFoundException` is caught and the URL is opened in the browser instead.

**Share intent** — `ItemDetailActivity`:
```java
Intent sendIntent = new Intent(Intent.ACTION_SEND);
sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
sendIntent.setType("text/plain");
startActivity(Intent.createChooser(sendIntent, "Share Item Details via"));
```

### Deep-link Intent
`ItemDetailActivity` is declared with an intent filter in `AndroidManifest.xml`:
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:scheme="lostandfound" android:host="item" />
</intent-filter>
```
This allows FCM notifications to open a specific item by URI: `lostandfound://item?id=abc&type=lost`. The Activity reads these from `getIntent().getData()`.

---

## 4. ViewModel and LiveData

### ViewModel
`ViewModel` is a Jetpack class that stores and manages UI-related data in a lifecycle-conscious way. It is scoped to the Activity/Fragment and survives configuration changes.

```java
FeedViewModel vm = new ViewModelProvider(this).get(FeedViewModel.class);
```

`ViewModelProvider` returns the same ViewModel instance for the same scope, so calling this in `onCreate` after rotation returns the existing ViewModel with the data already loaded.

### LiveData
`LiveData` is an observable data holder that is lifecycle-aware. Observers registered with `observe(lifecycleOwner, observer)` are only called when the lifecycle owner (Activity) is in the STARTED or RESUMED state. They are automatically removed when the lifecycle owner is destroyed — preventing memory leaks.

```java
vm.getAllCachedItems().observe(this, items -> {
    adapter.setItems(items);
    tvEmptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
});
```

`getAllCachedItems()` returns `LiveData<List<ItemEntity>>` from Room. Room automatically emits new values whenever the underlying table changes — the UI updates without any polling.

### MutableLiveData
Used in `ReportViewModel` to communicate submission status back to the Activity:
```java
public final MutableLiveData<String> postStatus = new MutableLiveData<>();
public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
```
The Activity observes `postStatus` and shows success/error feedback.

---

## 5. RecyclerView, Adapter, and ViewHolder

### RecyclerView
`RecyclerView` is the modern replacement for `ListView`. It recycles (reuses) item views as the user scrolls, making it memory-efficient even for very large lists.

Setup in `MainActivity`:
```java
recyclerView.setLayoutManager(new LinearLayoutManager(this));
recyclerView.setAdapter(adapter);
```

`LinearLayoutManager` arranges items in a vertical scrolling list.

### Adapter
`ItemAdapter` extends `RecyclerView.Adapter<ItemAdapter.ItemViewHolder>`. It is responsible for:
1. Creating ViewHolder instances (`onCreateViewHolder`) — inflates `item_feed_card.xml`
2. Binding data to views (`onBindViewHolder`) — sets text, colors, image
3. Reporting item count (`getItemCount`)

### ViewHolder Pattern
The ViewHolder holds references to the views within a single list item. Without the ViewHolder pattern, `findViewById()` would be called on every bind — expensive. With ViewHolder, `findViewById()` is called only once per item view creation.

```java
static class ItemViewHolder extends RecyclerView.ViewHolder {
    TextView tvTitle, tvStatus, tvCategory, tvLocation, tvDate, tvType;
    ImageView imgPhoto;
    // References cached here — not looked up again on every bind
}
```

### DiffUtil
`ItemAdapter` uses `DiffUtil` in `setItems()` to compute the minimal set of changes between the old and new list. Instead of calling `notifyDataSetChanged()` (which redraws everything), DiffUtil triggers targeted `notifyItemInserted`, `notifyItemRemoved`, `notifyItemChanged` calls — resulting in smooth animations.

```java
DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
    @Override public int getOldListSize() { return oldList.size(); }
    @Override public int getNewListSize() { return newList.size(); }
    @Override public boolean areItemsTheSame(int o, int n) {
        return oldList.get(o).id.equals(newList.get(n).id);
    }
    @Override public boolean areContentsTheSame(int o, int n) {
        return oldList.get(o).status.equals(newList.get(n).status);
    }
});
result.dispatchUpdatesTo(this);
```

---

## 6. Room Database (SQLite Persistence)

### What is Room?
Room is a Jetpack abstraction layer over SQLite. It provides compile-time SQL verification, LiveData integration, and a clean DAO (Data Access Object) pattern.

### Components

**Entity — `ItemEntity`**  
A Java class annotated with `@Entity(tableName = "items_cache")`. Each field annotated with `@ColumnInfo` maps to a database column. The `@PrimaryKey` annotation marks the primary key.

```java
@Entity(tableName = "items_cache")
public class ItemEntity {
    @PrimaryKey @NonNull
    @ColumnInfo(name = "id") public String id;
    @ColumnInfo(name = "posted_by_name") public String postedByName;
    // ...
}
```

**DAO — `ItemDao`**  
An interface annotated with `@Dao`. Methods annotated with `@Query`, `@Insert`, `@Delete`, `@Update` define database operations. Room generates the implementation at compile time.

```java
@Dao
public interface ItemDao {
    @Query("SELECT * FROM items_cache ORDER BY timestamp DESC")
    LiveData<List<ItemEntity>> getAllItems();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ItemEntity> items);

    @Query("DELETE FROM items_cache") void deleteAll();
}
```

**Database — `AppDatabase`**  
Singleton class annotated with `@Database`. The `version` field must be incremented when the schema changes. `fallbackToDestructiveMigration()` means a schema change wipes and re-syncs the cache rather than running a migration script — appropriate here since the cache can always be re-populated from Firebase.

```java
@Database(entities = {ItemEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ItemDao itemDao();
}
```

### Why separate `Item` and `ItemEntity`?
`Item` is a POJO deserialized by Firebase using getter/setter reflection. `ItemEntity` is a Room entity. They have different requirements (Firebase needs a no-arg constructor and specific getter names; Room needs `@ColumnInfo` annotations), so keeping them separate avoids coupling the two data layers.

---

## 7. Firebase Realtime Database

### Structure
Data is stored as a JSON tree. Two top-level nodes: `lost_items` and `found_items`. Each item is a child node with a Firebase auto-generated push key as its ID.

### Writing data — `setValue()`
```java
DatabaseReference itemRef = dbRef.child("lost_items").push();
item.setId(itemRef.getKey()); // auto-generated push key
itemRef.setValue(item);       // serializes all getter values to JSON
```
Firebase serializes the `Item` object by calling all `getXxx()` methods and using the property name (lowercase first letter) as the JSON key. This is why getter names must match field names exactly — `getPostedByName()` → `postedByName` in the database.

### Reading data — `addListenerForSingleValueEvent()`
```java
dbRef.child("lost_items").child(itemId)
    .addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot snapshot) {
            Item item = snapshot.getValue(Item.class);
            // Firebase deserializes JSON → Item using setter methods
        }
        @Override
        public void onCancelled(DatabaseError error) { }
    });
```

`addListenerForSingleValueEvent` reads once. `addValueEventListener` would subscribe to real-time updates.

### Concurrency in sync
`ItemRepository.syncFromFirebase()` fires two Firebase queries simultaneously (lost_items + found_items) using a counter array `int[] pendingRequests = {2}`. When both callbacks complete (counter hits 0), `persistAndNotify()` writes to Room on a background thread via `Executor`.

---

## 8. Firebase Authentication + Google Sign-In

### Flow
1. `GoogleSignInClient` is configured with `requestIdToken` pointing to the Firebase web client ID
2. `startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN)` launches Google's account picker
3. `onActivityResult` receives the result; `GoogleSignIn.getSignedInAccountFromIntent(data)` extracts the account
4. Domain restriction: `email.endsWith("@rvce.edu.in")` — if false, `firebaseAuth.signOut()` is called immediately
5. `firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))` exchanges the Google token for a Firebase session
6. On success, `UserRepository.createOrUpdateUser()` writes the profile to `/users/{uid}`, then `SessionManager.saveSession()` stores the user in SharedPreferences

### SharedPreferences Session
`SessionManager` wraps `SharedPreferences` to persist the user's ID, name, email, and photo URL across app restarts. `SplashActivity` reads `isLoggedIn` from SharedPreferences and skips the login screen if the session is active.

---

## 9. Firebase Cloud Messaging (FCM)

### Topic-based messaging
All users subscribe to topic `new_items` on app launch:
```java
FirebaseMessaging.getInstance().subscribeToTopic("new_items");
```
When a Cloud Function or server sends a message to this topic, all subscribed devices receive it.

### FCMService
`FCMService extends FirebaseMessagingService`. Two overridden methods:

**`onMessageReceived(RemoteMessage)`**  
Called when a message arrives while the app is in the foreground. Builds and displays a local `NotificationCompat.Builder` notification. The notification's tap Intent deep-links to `ItemDetailActivity` using the `lostandfound://item` URI scheme.

**`onNewToken(String)`**  
Called when FCM generates a new registration token. The token is saved to `/users/{uid}/fcmToken` in Firebase so the server can target this specific device for direct messages.

### Notification Channel
Android 8.0+ (API 26+) requires a `NotificationChannel` before notifications can be shown. The channel is created in `FCMService.onCreate()`:
```java
NotificationChannel channel = new NotificationChannel(
    Constants.CHANNEL_ID,
    Constants.CHANNEL_NAME,
    NotificationManager.IMPORTANCE_DEFAULT
);
notificationManager.createNotificationChannel(channel);
```

---

## 10. WorkManager

### Why WorkManager?
WorkManager is the Jetpack solution for deferrable, guaranteed background work. It respects battery optimization (Doze mode) and survives app restarts. Unlike `AlarmManager` or raw `Thread`, WorkManager guarantees execution even if the app is killed.

### NewItemWorker
`NewItemWorker extends Worker` and overrides `doWork()`. This runs on a background thread managed by WorkManager.

```java
public Result doWork() {
    SharedPreferences prefs = getApplicationContext()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    long lastCheck = prefs.getLong(KEY_LAST_CHECK,
        System.currentTimeMillis() - DEFAULT_LOOKBACK_MS);

    // Query Firebase for items newer than lastCheck
    CountDownLatch latch = new CountDownLatch(2);
    // ... Firebase queries with latch.countDown() on completion
    latch.await(20, TimeUnit.SECONDS); // blocks worker thread

    // Fire local notifications for new items
    // Update lastCheck in SharedPreferences
    return Result.success();
}
```

**CountDownLatch** is a Java concurrency tool. Initialized with count 2 (one for lost_items, one for found_items queries). Each Firebase callback calls `latch.countDown()`. `latch.await(20, SECONDS)` blocks the worker thread until both queries complete or 20 seconds elapse — safely synchronizing async Firebase calls in a synchronous Worker context.

### Scheduling
```java
WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "new_item_check",
    ExistingPeriodicWorkPolicy.KEEP, // don't reschedule if already queued
    new PeriodicWorkRequest.Builder(NewItemWorker.class, 15, TimeUnit.MINUTES).build()
);
```
`enqueueUniquePeriodicWork` with `KEEP` policy ensures only one instance of the worker runs — calling it again (e.g., on every app launch) does nothing if the work is already scheduled.

The 15-minute interval is the minimum Android allows for `PeriodicWorkRequest`. The OS may delay execution to batch with other work for battery efficiency.

---

## 11. BroadcastReceiver

### NetworkReceiver
`NetworkReceiver extends BroadcastReceiver` listens for `android.net.conn.CONNECTIVITY_CHANGE` and `android.intent.action.BOOT_COMPLETED`.

```java
@Override
public void onReceive(Context context, Intent intent) {
    if (NetworkUtils.isOnline(context)) {
        // Trigger Firebase sync via ItemRepository
    }
}
```

Registered statically in `AndroidManifest.xml` (not dynamically in an Activity) so it receives broadcasts even when the app is not running. `RECEIVE_BOOT_COMPLETED` allows it to re-register after device reboot.

**Important:** `onReceive()` runs on the main thread and must complete quickly (< 10 seconds). Long operations (Firebase sync) should be offloaded to a Service or WorkManager job.

---

## 12. Background Service — MatchingService

### IntentService / Service
`MatchingService extends Service`. It runs the keyword/category matching algorithm when a new item is posted.

When `ReportViewModel` posts an item, it starts `MatchingService` via an explicit Intent with the new item's data as extras. The service queries Firebase for items of the opposite type, compares categories and title keywords, and sends an FCM notification to the matching item's owner.

Services run on the main thread by default. Long-running operations inside a Service must explicitly use a background thread or `Executors.newSingleThreadExecutor()` to avoid ANR (Application Not Responding) errors.

---

## 13. Google Maps SDK

### Map setup
Maps are embedded as `SupportMapFragment` in XML layouts. The fragment is found via `getSupportFragmentManager().findFragmentById()` and `getMapAsync(callback)` is called to receive the `GoogleMap` instance asynchronously once tiles are loaded.

```java
SupportMapFragment mapFragment =
    (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
mapFragment.getMapAsync(this); // 'this' implements OnMapReadyCallback
```

```java
@Override
public void onMapReady(GoogleMap map) {
    googleMap = map;
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(RVCE, 16));
}
```

### Camera control
`CameraUpdateFactory.newLatLngZoom(latLng, zoom)` creates a camera update. Zoom level 16 shows individual buildings clearly. `moveCamera()` is instant; `animateCamera()` smoothly animates.

### Markers
```java
googleMap.addMarker(new MarkerOptions()
    .position(new LatLng(lat, lng))
    .title("Item Title")
    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
```

In `MapActivity`, custom circular markers are drawn programmatically using `Canvas` and `Paint` on a `Bitmap`, then converted to `BitmapDescriptor` via `BitmapDescriptorFactory.fromBitmap()`.

### InfoWindow
`MapActivity` implements a custom `GoogleMap.InfoWindowAdapter` to inflate a custom `map_info_window.xml` layout for each marker popup, displaying item title, type badge, category, and location.

### Map in ItemDetailActivity
The detail screen embeds a non-interactive map fragment (`getUiSettings().setAllGesturesEnabled(false)`) to show a static pin at the item's location. If coordinates are `0,0` (no location), the map fragment is hidden with `View.GONE`.

### LocationPickerActivity
Implements an interactive pin-drop picker. `setOnMapClickListener` fires on every tap:
```java
googleMap.setOnMapClickListener(latLng -> {
    googleMap.clear();
    googleMap.addMarker(new MarkerOptions().position(latLng));
    pickedLat = latLng.latitude;
    pickedLng = latLng.longitude;
    reverseGeocode(latLng); // runs on background thread
});
```
`Geocoder.getFromLocation()` converts lat/lng to a human-readable address. This runs in a `new Thread()` because `Geocoder` is a blocking network call and must not run on the main thread.

---

## 14. Material Design 3 and Theming

### DayNight Theme
The base theme is `Theme.Material3.DayNight.NoActionBar`. Material3 DayNight automatically selects light or dark color roles based on the system setting. `LostAndFoundApp.onCreate()` calls:
```java
AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
```
This ensures the mode is applied before any Activity is created.

### Resource qualifiers
`res/values/colors.xml` defines light-mode semantic colors (`app_background`, `card_background`, etc.). `res/values-night/colors.xml` overrides the same color names with dark equivalents. Android's resource system automatically selects the correct file based on the system theme.

```
values/colors.xml         → used in light mode
values-night/colors.xml   → used in dark mode
```

All layout files reference `@color/app_background` instead of `#121212` — the OS resolves the correct value at runtime.

### NoActionBar
The theme uses `NoActionBar` so Activities can set up their own `MaterialToolbar` with `setSupportActionBar(toolbar)`. Using a theme with a built-in ActionBar AND calling `setSupportActionBar()` causes a runtime crash (`IllegalStateException`).

### MaterialCardView, MaterialButton, Chip
All interactive UI elements use Material3 components which automatically adapt their color tokens (surface, onSurface, primary, etc.) to the active theme.

### Styles
Chip filters use `@style/Widget.Material3.Chip.Filter`. The Outlined button style `@style/Widget.Material3.Button.OutlinedButton` is used for "Mark as Resolved". Text button `@style/Widget.Material3.Button.TextButton` is used for "Share".

---

## 15. Navigation Patterns

### Navigation Drawer (DrawerLayout + NavigationView)
`DrawerLayout` is the root view in `activity_main.xml`. It wraps the main content and a `NavigationView` (the drawer panel). The drawer slides in from the left.

```java
ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
    this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close);
drawerLayout.addDrawerListener(toggle);
toggle.syncState(); // syncs the hamburger/arrow icon state
```

`NavigationView.setNavigationItemSelectedListener` handles drawer item taps. Returning `false` from this listener prevents the item from being marked as checked/highlighted — important here because all items launch other Activities and the user always returns to the same Feed state.

### Bottom Navigation (BottomNavigationView)
`BottomNavigationView` provides persistent access to three top-level destinations: Feed, Map, My Posts. The selected item is reset to Feed in `onResume()` so that after navigating to MapActivity or MyPostsActivity and pressing Back, the Feed tab appears selected.

### Back Stack
Each call to `startActivity()` pushes a new Activity onto the system back stack. `finish()` in `onSupportNavigateUp()` explicitly pops the current Activity. The Android back button automatically pops the top Activity unless overridden.

---

## 16. Permissions

### Runtime Permissions (API 23+)
Dangerous permissions (location, camera, storage) must be requested at runtime. The app uses `ActivityCompat.requestPermissions()` and handles the result in `onRequestPermissionsResult()`.

### Normal Permissions
`INTERNET`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED` are normal permissions — granted automatically at install time, no runtime request needed.

### POST_NOTIFICATIONS (API 33+)
Android 13 introduced a runtime permission for showing notifications. The app declares and requests `POST_NOTIFICATIONS` before calling `NotificationManager.notify()`.

### READ_MEDIA_IMAGES vs READ_EXTERNAL_STORAGE
`READ_EXTERNAL_STORAGE` works for API ≤ 32. `READ_MEDIA_IMAGES` is the replacement for API ≥ 33. Both are declared in the manifest; the correct one is requested based on `Build.VERSION.SDK_INT` at runtime.

---

## 17. Content Provider — Image Access

When the user selects a photo from the gallery:
```java
Intent intent = new Intent(Intent.ACTION_PICK,
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
startActivityForResult(intent, RC_IMAGE_PICK);
```
The returned URI (e.g., `content://media/external/images/media/42`) is a Content Provider URI. It cannot be used directly as a file path. It is passed to Firebase Storage's `putFile(uri)` which reads the bytes via the Content Resolver.

For camera capture, a `FileProvider` URI is used:
```java
Uri photoUri = FileProvider.getUriForFile(this,
    getPackageName() + ".fileprovider", photoFile);
```
`FileProvider` is a special Content Provider that exposes a private app file to the camera app without granting full storage access.

---

## 18. AppCompatActivity and Toolbar

All Activities extend `AppCompatActivity` instead of `Activity`. This provides:
- `setSupportActionBar(toolbar)` — designates a `MaterialToolbar` as the action bar, giving it title, navigation icon, and options menu support
- `getSupportActionBar().setDisplayHomeAsUpEnabled(true)` — shows the back arrow in the toolbar
- Backward-compatible support for fragments, themes, and Material components on older Android versions

---

## 19. Application Class — LostAndFoundApp

`LostAndFoundApp extends Application`. The `Application` class has one instance per process and its `onCreate()` is called before any Activity. This is the correct place for app-wide initialization:

```java
public class LostAndFoundApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
}
```

Registered in `AndroidManifest.xml` via `android:name=".LostAndFoundApp"`. Without this, the OS instantiates the default `Application` class and the night mode setting would be applied too late (inside an Activity's `onCreate`), causing a flash of the wrong theme.

---

## 20. AndroidManifest.xml

The manifest is the app's declaration file. It registers every component that Android needs to know about:

- **Activities** — with `exported="true"` for the entry point (SplashActivity) and `exported="false"` for all others (cannot be launched by external apps)
- **Services** — `MatchingService`, `FCMService`
- **Receivers** — `NetworkReceiver` with its intent filter for connectivity and boot events
- **Permissions** — all uses-permission declarations
- **Meta-data** — `com.google.android.geo.API_KEY` for the Maps SDK
- **Intent filters** — deep-link filter on `ItemDetailActivity` for `lostandfound://item` URIs
- **Application-level theme** — `android:theme="@style/Theme.LostAndFound"` applied to all Activities unless overridden per-Activity
- **FCM intent filter** on `FCMService` — `com.google.firebase.MESSAGING_EVENT` so Firebase knows which service to call for incoming messages

---

## 21. Gradle Build System

The project uses Gradle with Kotlin DSL (`build.gradle.kts`). A version catalog (`gradle/libs.versions.toml`) centralizes all dependency versions:

```toml
[versions]
workmanager = "2.9.1"

[libraries]
work-runtime = { group = "androidx.work", name = "work-runtime", version.ref = "workmanager" }
```

Dependencies are referenced as `implementation(libs.work.runtime)` in `app/build.gradle.kts`. This avoids version conflicts and makes upgrades a single-line change.

**`google-services` plugin** processes `google-services.json` and injects Firebase configuration into the build automatically.

---

## 22. Executor and Threading Model

Android's main (UI) thread must never be blocked. Long-running operations use:

**`Executor`** — `ItemRepository` uses `Executors.newSingleThreadExecutor()` for all Room write operations. Room enforces that database access must not happen on the main thread (throws `IllegalStateException` if attempted).

```java
executor.execute(() -> {
    itemDao.deleteAll();
    itemDao.insertAll(entities);
});
```

**Background `Thread`** — `LocationPickerActivity` uses `new Thread(() -> { ... }).start()` for `Geocoder` calls, then posts the result back to the main thread with `runOnUiThread()`.

**WorkManager thread** — `doWork()` in `NewItemWorker` already runs on a background thread managed by WorkManager's internal executor. No additional threading is needed there.

---

## 23. Snackbar and User Feedback

`Snackbar` is the Material Design replacement for `Toast`. Unlike a Toast, it is attached to a view, supports an action button, and respects the bottom navigation bar.

```java
Snackbar.make(rootView, "Error loading item", Snackbar.LENGTH_LONG)
    .setAction("Retry", v -> loadItem())
    .show();
```

The `rootView` parameter determines where the Snackbar appears. Using `CoordinatorLayout` as the root allows the Snackbar to automatically dodge the `BottomNavigationView` via `CoordinatorLayout.Behavior`.

---

## 24. CoordinatorLayout and AppBarLayout

`CoordinatorLayout` is a powerful `FrameLayout` subclass that enables coordinated behavior between child views.

`app:layout_behavior="@string/appbar_scrolling_view_behavior"` on a `RecyclerView` inside a `CoordinatorLayout` that has an `AppBarLayout` sibling causes the RecyclerView to automatically offset its top padding to account for the AppBar height. This prevents content from going behind the toolbar.

`MyPostsActivity`, `MapActivity`, and `ItemDetailActivity` all use this pattern:
```xml
<CoordinatorLayout>
    <AppBarLayout>
        <MaterialToolbar />
    </AppBarLayout>
    <RecyclerView app:layout_behavior="@string/appbar_scrolling_view_behavior" />
</CoordinatorLayout>
```

---

## 25. BottomSheetDialog

`BottomSheetDialog` is a Material component that slides a custom view up from the bottom of the screen — commonly used for contextual actions.

```java
BottomSheetDialog sheet = new BottomSheetDialog(this);
View view = getLayoutInflater().inflate(R.layout.dialog_report_options, null);
sheet.setContentView(view);
sheet.show();
```

The layout is inflated manually and set as the content. The dialog handles the drag-to-dismiss gesture automatically.

---

*End of EXPLANATION.md*
