# Campus Lost & Found — App Overview

A campus Android app where students report lost or found items, browse a live feed, see locations on a map, and get push notifications on potential matches.

---

## Screens (Activities)

| Screen | Purpose |
|---|---|
| SplashActivity | Entry point — checks login state, routes to Login or Main after 1.5s |
| LoginActivity | Google Sign-In with college domain restriction |
| MainActivity | Feed of all items — toolbar, drawer, bottom nav, chips, search |
| ReportLostActivity | Form to submit a lost item (photo, location, category) |
| ReportFoundActivity | Form to submit a found item |
| ItemDetailActivity | Full view of one item, mark resolved / delete |
| MapActivity | All item pins on Google Maps with custom info windows |
| LocationPickerActivity | Drop a pin to set GPS coordinates while reporting |
| MyPostsActivity | Current user's own posts only |

---

## Android Concepts Applied

### Activity & Lifecycle
All screens are AppCompatActivity subclasses. The OS calls onCreate to set up the screen, onResume to refresh state (bottom nav reselection in MainActivity), and finish() to close a screen and return to the previous one. SplashActivity calls finish() after routing so it cannot be navigated back to.

### AndroidManifest.xml
Every Activity, the FCM Service, and the BroadcastReceiver are declared here. Without a declaration, Android refuses to start the component. The splash is marked as the launcher entry point via an intent-filter. Internet, network state, and notification permissions are declared here too.

### Intent & Navigation
Activities start other Activities using Intent. Data is passed between screens via putExtra (item ID, item type) and read back with getStringExtra. On logout, the Intent is given flags FLAG_ACTIVITY_NEW_TASK and FLAG_ACTIVITY_CLEAR_TASK so the entire back stack is cleared and the user cannot press Back to return to the feed.

### onActivityResult
LoginActivity uses startActivityForResult to launch the Google Sign-In flow and receives the result in onActivityResult, where it extracts the GoogleSignInAccount from the returned Intent.

### Handler & Looper
SplashActivity uses a Handler with the main Looper and postDelayed to delay the routing decision by 1.5 seconds — a timed action on the main thread without blocking the UI.

### Fragment
MapActivity uses SupportMapFragment, which is an Android Fragment — a reusable UI component embedded inside an Activity. The map is not an Activity itself; it lives inside a Fragment hosted by MapActivity.

### MVVM Architecture
The app separates concerns into three layers:
- Model — data classes (Item, User, ItemEntity) and data sources (Firebase, Room)
- ViewModel — FeedViewModel and ReportViewModel hold UI state and survive screen rotations. They extend AndroidViewModel to access the Application context safely.
- View — Activities only observe data and update views; they contain no business logic.

### ViewModel & ViewModelProvider
ViewModels are created via ViewModelProvider. The OS keeps the same ViewModel instance alive across configuration changes (e.g. screen rotation), so the feed does not reload unnecessarily.

### LiveData
Room DAOs return LiveData containing lists of items. FeedViewModel exposes this to the Activity via observe(). When the database changes, the UI updates automatically — no manual polling. A MutableLiveData string is used for sync status (syncing / success / error).

### Repository Pattern
ItemRepository and UserRepository are the single source of truth for data. Activities and ViewModels never touch Firebase or Room directly — they go through the repository. This keeps the ViewModel testable and the data layer swappable.

### Room Database (SQLite)
AppDatabase is a Room database with one table: items. It is a singleton (double-checked locking in getInstance). ItemEntity is the table schema. ItemDao defines queries: getAllItems, getItemsByType, getItemsByUser, insertAll, deleteAll, updateStatus. Room runs all DAO operations on a background thread and delivers results via LiveData.

### Executor (Background Threading)
ItemRepository creates a single-thread Executor to run all Room write operations off the main thread. Firebase callbacks arrive on the main thread; the Executor moves the insert and delete work to a worker thread before touching the database.

### Firebase Realtime Database
All item posts are stored in Firebase under two nodes: lost_items and found_items. push() generates a unique key for each new item. addListenerForSingleValueEvent fetches data once. setValue and removeValue write and delete data. ValueEventListener handles both success (onDataChange) and failure (onCancelled).

### Firebase Storage
Photos are uploaded to Firebase Storage via putFile with a Uri. After upload, getDownloadUrl returns a public HTTPS URL, which is then saved as part of the item in the Realtime Database.

### Firebase Authentication
Login is Google-only. GoogleSignInOptions requests the ID token and email. After the user picks their Google account, the token is exchanged for a Firebase credential via GoogleAuthProvider. signInWithCredential authenticates with Firebase. An AuthStateListener in MainActivity waits for a valid user before syncing data.

### Firebase Cloud Messaging (FCM)
FCMService extends FirebaseMessagingService. onNewToken is called when the device gets a new FCM registration token — it is saved to Firebase under the user's profile. onMessageReceived handles incoming push notifications and builds a local notification. MainActivity subscribes to the new_items topic so all users receive broadcast notifications.

### WorkManager
NewItemWorker is a Worker that runs every 15 minutes in the background, even when the app is closed. It queries Firebase for items newer than the last check timestamp, and fires a local notification for each new item found. enqueueUniquePeriodicWork with KEEP policy ensures only one periodic task runs at a time. A CountDownLatch blocks the worker until both async Firebase queries complete before returning success.

### BroadcastReceiver
NetworkReceiver extends BroadcastReceiver and listens for the CONNECTIVITY_ACTION system broadcast. When connectivity is restored, it sends a local broadcast so MainActivity can trigger a Firebase sync. BroadcastReceivers are stateless — they only live for the duration of onReceive.

### Service
MatchingService extends Service and runs as an unbound, started service (onBind returns null). It is started via an Intent, fetches a newly posted item from Firebase, compares it against opposite-type items (lost vs found) using keyword matching, and then stops itself with stopSelf(). It returns START_NOT_STICKY so the OS does not restart it if killed.

### Notifications
Both FCMService and NewItemWorker build notifications using NotificationCompat.Builder. A NotificationChannel is created at runtime (required on Android 8 and above). A PendingIntent is attached so tapping the notification opens the relevant item detail screen. FLAG_IMMUTABLE is required on Android 12 and above.

### SharedPreferences
SessionManager wraps SharedPreferences to persist the current user's UID, name, email, and photo URL between app launches. The app checks isLoggedIn in SplashActivity to decide where to route on startup. clearSession is called on logout.

### Permissions (Runtime)
ReportLostActivity and ReportFoundActivity request READ_EXTERNAL_STORAGE (or READ_MEDIA_IMAGES on Android 13+) at runtime before opening the photo picker. The result is handled in onRequestPermissionsResult.

### Content Provider / MediaStore
Photo selection opens the gallery by firing an Intent targeting the MediaStore images URI. The user-selected image URI is returned in onActivityResult and passed to Firebase Storage for upload.

### Google Maps SDK
MapActivity implements OnMapReadyCallback. Once the map is ready, addMarker with MarkerOptions places pins. A custom InfoWindowAdapter inflates a layout to show item details in the tap popup. setOnInfoWindowClickListener navigates to ItemDetailActivity when the popup is tapped. CameraUpdateFactory pans and zooms to the campus location on load.

### Custom Bitmap Drawing (Canvas / Paint)
MapActivity draws its own map marker icons using Bitmap, Canvas, and Paint — a filled colored circle with a white border and a letter label — converted to a BitmapDescriptor for use with Google Maps markers.

### RecyclerView, Adapter, ViewHolder
ItemAdapter extends RecyclerView.Adapter. Each row is represented by an ItemViewHolder which holds references to the views in the row layout. onCreateViewHolder inflates the row layout; onBindViewHolder fills it with data. LinearLayoutManager arranges items in a vertical list.

### DiffUtil
ItemAdapter uses DiffUtil.calculateDiff to compare the old and new list before updating. Only changed rows are redrawn, making list updates smooth and efficient instead of redrawing everything at once.

### Material Design 3 Components
The app uses the full Material 3 component set:
- MaterialToolbar — top app bar with title and nav icon
- BottomNavigationView — Feed / Map / My Posts tabs
- FloatingActionButton — triggers the report options sheet
- BottomSheetDialog — modal bottom sheet for choosing Report Lost or Found
- MaterialCardView — card layout for each feed item
- ChipGroup and Chip — filter bar (All / Lost / Found / Mine)
- Snackbar — sync error and action feedback
- TextInputLayout and TextInputEditText — form fields with floating labels
- NavigationView — side drawer with user profile header
- MaterialButton — Google Sign-In button

### DrawerLayout & NavigationView
MainActivity uses a DrawerLayout wrapping the content. An ActionBarDrawerToggle connects the toolbar hamburger icon to open and close the drawer. The drawer header shows the user's name, email, and profile photo loaded via Glide.

### SearchView
MainActivity includes a SearchView. OnQueryTextListener fires on every keystroke and on submit, filtering the cached item list client-side against title, category, and description fields.

### Glide
Glide loads item photos from Firebase Storage URLs into ImageView with a placeholder while loading. In the drawer header, circleCrop is applied to display the user's Google profile photo as a circle.

### Application Class
LostAndFoundApp extends Application and is declared in the manifest. Its onCreate runs once before any Activity and sets the default night mode to follow the system, enabling automatic dark mode.

### Singleton Pattern
AppDatabase uses double-checked locking with a volatile field to ensure only one database instance is ever created across the entire app lifetime.

---

## Data Flow Summary

Report submitted → ReportViewModel → ItemRepository uploads photo to Firebase Storage → item written to Firebase Realtime DB → cached locally in Room

Feed loads → FeedViewModel reads from Room LiveData → Activity observes and renders list → syncFromFirebase pulls latest from Firebase and refreshes Room

New item posted → NewItemWorker runs in background every 15 min → queries Firebase for recent items → fires local notification for each new entry

FCM push received → FCMService onMessageReceived → builds and shows notification → tapping opens ItemDetailActivity

Network restored → NetworkReceiver onReceive → local broadcast → MainActivity triggers syncFromFirebase
