# Graph Report - composeApp  (2026-09-07)

## Corpus Check
- Large corpus: 530 files · ~192,215 words. Semantic extraction will be expensive (many Claude tokens). Consider running on a subfolder.

## Summary
- 2497 nodes · 5536 edges · 198 communities (132 shown, 33 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 259 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Post Date Formatting
- Runtime Permissions
- Android Room Database Setup
- Memories Repository
- Events Repository
- Department DAO
- User & Member Entities
- Lending Item DAO
- Lendings List UI
- Error Handling ViewModels
- Background Sync Jobs
- Dialog Context & Delete Dialog
- Misc Icons & Event Item
- Entity Visibility Rules
- Lending Exceptions
- Logout Dialog & Icons
- Inventory Item Types Remote Repo
- Members Repository
- Brand Icons & Department Card
- Barcode Scanning & Camera Permission
- Contact Icons & Back Button
- Database Integrity Verifier
- Entity Sync & Caching
- Referenced-Entity Extensions
- Koin DI & Dispatcher Provider
- List View Toolbar & Editor Context
- File Path Resolution
- Navigation Destinations
- Android Platform Module (NFC/Calendar)
- Calendar Sync (expect/actual)
- Database Integrity Verifier Tests
- Android In-App Updates
- NFC Read/Write
- Inventory Item DAO
- App Root Composable
- Inventory Item Types Repository
- Departments Management ViewModel
- Activity Memory Editor Form
- Lendings Page Icons
- Network Error Handling
- Received Item DAO
- Post DAO
- Users List UI
- JVM Drag & Drop / QR
- User Profile Data
- WorkManager Background Job Worker
- Background Job Coordinator (Workers impl)
- App Base Lifecycle
- Memory DAO
- Settings Dialogs & SSE Config
- Lending Page Layout Helpers
- Rich Text Editor & Item Details
- Auth Backend
- Lending DAO
- Shared-Element Transitions
- Members Remote Repo & Symmetric Base
- Create Inventory Item Dialog
- Department Role Editor Dialog
- Content Type Icons
- Push Notification Listener
- NFC Tag Utilities
- Inventory Cross-Reference Tests
- FEMECV Account Card
- Sentry Logging
- Inventory Item Type DAO
- File Logger
- Events List UI
- Adaptive Layout (Window Size)
- Android Content Provider
- Cookie Storage
- JVM In-App Updates (GitHub releases)
- Android Open/Share File
- User DAO
- Inventory Items Repository
- Departments Remote Repository
- NFC Intent Handler Activity
- Date Picker Form Field
- Background Job Coordinator (Android)
- Departments Repository
- Posts Repository
- Memory Detail Dialog
- Insurance Dialog
- Inventory Management ViewModel
- Cross-Platform File System
- Add Insurance Dialog & File Picker
- Posts Remote Repository
- Main Activity (Android)
- Manufacturer Item Details
- Users Repository
- Inventory Items Remote Repository
- iOS Share Logic
- Edit Inventory Item & Lending History Dialogs
- Memories Page UI
- Profile Page ViewModel
- Entity-With-Relations Mappers
- HTTP Client Setup
- Inventory Item Type Dialogs
- Activity Memory Editor ViewModel
- Events Management ViewModel
- Posts Management ViewModel
- JVM Clipboard
- Background Job State
- Loading & Error Screens
- Dropdown Icon Button & Choice Flow
- File Provider ViewModel
- Local Notifications
- Background Job Coordinator (common)
- Navigator
- Settings Row UI
- Logger Abstraction (expect/actual)
- Save File Logic
- ViewModel Coroutine Utils
- Android Drag & Drop / QR
- Observable Background Job (Android)
- Drag & Drop / QR (common)
- Platform Initializer
- Lending Page & ViewModel
- Sort Options
- Navigation Page Enum
- Main ViewModel
- Users Management ViewModel
- Observable Background Job (Workers impl)
- iOS Drag & Drop / QR
- Event-User Cross-Ref DAO
- Memory-Member Cross-Ref DAO
- Log Level Enum
- Grammatical Gender Enum
- Settings Options Row
- Lendings Page ViewModel
- iOS In-App Updates
- Android Printer
- Clipboard (common)
- Printer (common)
- Past-Only Selectable Dates
- Observable Unique Background Job (Workers impl)
- JVM Printer
- Android Clipboard
- Android Load Logic
- Observable Background Jobs (Android)
- Observable Unique Background Job (Android)
- Database Koin Module
- Observable Background Jobs (common)
- Observable Unique Background Job (common)
- Date Range Selectable Dates
- Observable Background Jobs (Workers impl)
- iOS Clipboard
- iOS Load Logic
- Android HTTP Client Engine
- Android Progress Conversion
- Android Window Size Class
- Users Nav Visibility
- Hover Modifier (common)
- HTTP Date Formatter Tests
- iOS Main View Controller
- iOS HTTP Client Engine
- iOS Window Size Class
- JVM HTTP Client Engine
- Hover Modifier (JVM)
- JVM Window Size Class
- File System Tests
- Hover Modifier (Phones)
- Disabled Annotated Koin Modules
- Observe Unique Background Job (interface)
- JVM SSE Configuration
- Phones SSE Configuration

## God Nodes (most connected - your core abstractions)
1. `ProgressNotifier` - 91 edges
2. `launch()` - 62 edges
3. `MaterialSymbols` - 47 edges
4. `LoadingBox()` - 38 edges
5. `TestDatabaseIntegrityVerifier` - 37 edges
6. `DispatcherProvider` - 33 edges
7. `LendingsRepository` - 30 edges
8. `AsyncByteImage()` - 30 edges
9. `DepartmentsRepository` - 29 edges
10. `AppDatabase` - 28 edges

## Surprising Connections (you probably didn't know these)
- `platformModule()` --calls--> `PlatformCalendarSync`  [INFERRED]
  src/androidMain/kotlin/org/centrexcursionistalcoi/app/di/ManualModules.android.kt → src/androidMain/kotlin/org/centrexcursionistalcoi/app/platform/PlatformCalendarSync.android.kt
- `platformModule()` --calls--> `PlatformDragAndDrop`  [INFERRED]
  src/androidMain/kotlin/org/centrexcursionistalcoi/app/di/ManualModules.android.kt → src/androidMain/kotlin/org/centrexcursionistalcoi/app/platform/PlatformDragAndDrop.android.kt
- `platformModule()` --calls--> `PlatformOpenFileLogic`  [INFERRED]
  src/androidMain/kotlin/org/centrexcursionistalcoi/app/di/ManualModules.android.kt → src/androidMain/kotlin/org/centrexcursionistalcoi/app/platform/PlatformOpenFileLogic.android.kt
- `platformModule()` --calls--> `PlatformShareLogic`  [INFERRED]
  src/androidMain/kotlin/org/centrexcursionistalcoi/app/di/ManualModules.android.kt → src/androidMain/kotlin/org/centrexcursionistalcoi/app/platform/PlatformShareLogic.android.kt
- `platformModule()` --calls--> `BackgroundJobCoordinator`  [INFERRED]
  src/androidMain/kotlin/org/centrexcursionistalcoi/app/di/ManualModules.android.kt → src/androidMain/kotlin/org/centrexcursionistalcoi/app/sync/BackgroundJobCoordinator.android.kt

## Import Cycles
- None detected.

## Communities (198 total, 33 thin omitted)

### Community 0 - "Post Date Formatting"
Cohesion: 0.06
Nodes (34): CardColors, ServerInfo, localizedDateRange(), localizedDate(), HttpDateFormatter, DateTimeFormat, Server, FutureSelectableDates (+26 more)

### Community 1 - "Runtime Permissions"
Cohesion: 0.06
Nodes (38): LibPermission, LT, Camera, Location, Notification, Permission, RecordAudio, T (+30 more)

### Community 2 - "Android Room Database Setup"
Cohesion: 0.06
Nodes (24): RoomDatabaseConstructor, getDatabaseBuilder(), Context, RoomDatabase, Module, platformDatabaseModule(), Flow, MemberDao (+16 more)

### Community 3 - "Memories Repository"
Cohesion: 0.08
Nodes (31): MemoryMemberCrossRef, Flow, Memory, ReferencedMemory, Uuid, MemoriesRepository, Department, Member (+23 more)

### Community 4 - "Events Repository"
Cohesion: 0.08
Nodes (18): EventUserCrossRef, EventsRepository, Event, Flow, ReferencedEvent, Uuid, EventsRemoteRepository, Event (+10 more)

### Community 5 - "Department DAO"
Cohesion: 0.09
Nodes (14): DepartmentDao, Flow, Uuid, EventDao, Flow, org, Uuid, DepartmentEntity (+6 more)

### Community 6 - "User & Member Entities"
Cohesion: 0.08
Nodes (9): LendingUser, DepartmentMemberInfo, FileWithContext, Member, Sports, UInt, UserInsurance, Uuid (+1 more)

### Community 7 - "Lending Item DAO"
Cohesion: 0.15
Nodes (10): Flow, Uuid, LendingItemDao, LendingItemEntity, Flow, Lending, ReferencedLending, ReferencedMemory (+2 more)

### Community 8 - "Lendings List UI"
Cohesion: 0.14
Nodes (31): CalendarDay, Density, ID, Size, LendingItem(), BoundType, CONTINUATION, END (+23 more)

### Community 9 - "Error Handling ViewModels"
Cohesion: 0.12
Nodes (7): ErrorViewModel, ViewModel, ByteArray, Job, Uuid, LendingManagementViewModel, LoginViewModel

### Community 10 - "Background Sync Jobs"
Cohesion: 0.10
Nodes (13): BackgroundJob, KoinComponent, SyncAllDataBackgroundJob, SyncDepartmentBackgroundJob, SyncEntityBackgroundJob, SyncEventBackgroundJob, SyncLendingBackgroundJob, SyncPostBackgroundJob (+5 more)

### Community 11 - "Dialog Context & Delete Dialog"
Cohesion: 0.13
Nodes (21): DialogContext, DialogContextImpl, IDialogContext, ColumnScope, DeleteDialog(), DeleteDialogContext, DeleteDialogContext, T (+13 more)

### Community 12 - "Misc Icons & Event Item"
Cohesion: 0.12
Nodes (16): ContentScale, EventItem(), ProfileResponse, ReferencedEvent, FeedItem(), ImageVector, ReferencedPost, PostItem() (+8 more)

### Community 13 - "Entity Visibility Rules"
Cohesion: 0.16
Nodes (18): Departments, Events, Inventory, Department, EntityType, Memory, ProfileResponse, ReferencedEvent (+10 more)

### Community 14 - "Lending Exceptions"
Cohesion: 0.17
Nodes (13): NoSuchElementException, CannotAllocateEnoughItemsException, IllegalStateException, IllegalStateException, NoValidInsuranceForPeriodException, Department, Lending, Member (+5 more)

### Community 15 - "Logout Dialog & Icons"
Cohesion: 0.13
Nodes (13): PagerState, LogoutConfirmationDialog(), ConditionalBadge(), Department, ProfileResponse, ReferencedLending, SnackbarHostState, Uuid (+5 more)

### Community 16 - "Inventory Item Types Remote Repo"
Cohesion: 0.14
Nodes (14): fileWithContext(), InventoryItemTypesRemoteRepository, Department, InventoryItemType, PlatformFile, ReferencedInventoryItemType, Uuid, Data (+6 more)

### Community 17 - "Members Repository"
Cohesion: 0.14
Nodes (8): Flow, Member, UInt, MembersRepository, Flow, IdType, T, Repository

### Community 18 - "Brand Icons & Department Card"
Cohesion: 0.14
Nodes (17): IconAction, BrandIcons, DepartmentsListCard(), Department, Job, InsurancesListCard(), UserInsurance, NoInsurancesCard() (+9 more)

### Community 19 - "Barcode Scanning & Camera Permission"
Cohesion: 0.12
Nodes (18): Barcode, BarcodeFormat, Painter, Job, launchWithCameraPermission(), ImageDisplay(), InventoryItemInformationDialog(), Modifier (+10 more)

### Community 20 - "Contact Icons & Back Button"
Cohesion: 0.20
Nodes (18): BackButton(), GeneralLendingDetailsExtra(), ColumnScope, Composable, Modifier, ReferencedLending, SnackbarHostState, TextStyle (+10 more)

### Community 21 - "Database Integrity Verifier"
Cohesion: 0.15
Nodes (15): CoroutineContext, DatabaseIntegrityVerifier, await(), copyToProgress(), JobDoneThrowable, Flow, Progress, Throwable (+7 more)

### Community 22 - "Entity Sync & Caching"
Cohesion: 0.18
Nodes (11): KSerializer, LocalEntity, RemoteEntity, RemoteIdType, ResourceNotModifiedException, ifModifiedSince(), HttpClient, T (+3 more)

### Community 23 - "Referenced-Entity Extensions"
Cohesion: 0.13
Nodes (19): ReferencedInventoryItemType, ReferencedMemory, UserData, referenced(), ColumnScope, ImageVector, Modifier, ReferencedLending (+11 more)

### Community 24 - "Koin DI & Dispatcher Provider"
Cohesion: 0.11
Nodes (15): DefaultDispatcherProvider, DispatcherProvider, DispatcherProviderHolder, CoroutineDispatcher, KoinComponent, Module, platformModule(), ViewModel (+7 more)

### Community 25 - "List View Toolbar & Editor Context"
Cohesion: 0.14
Nodes (17): EditorContext, Filter, Color, ColumnScope, Composable, Job, Modifier, RowScope (+9 more)

### Community 26 - "File Path Resolution"
Cohesion: 0.22
Nodes (21): SnapshotStateMap, fetchDocumentFilePath(), fetchFilePath(), fetchImageFilePath(), fetchSubReferencedFilePath(), filePaths(), imageFile(), joinPaths() (+13 more)

### Community 27 - "Navigation Destinations"
Cohesion: 0.12
Nodes (19): NavKey, Admin, Destination, External, ItemTypeDetails, ShoppingList, Url, LendingCreation (+11 more)

### Community 28 - "Android Platform Module (NFC/Calendar)"
Cohesion: 0.13
Nodes (14): PlatformNFC, Module, platformModule(), addCalendarEvent(), ReferencedEvent, PlatformCalendarSync, ContentType, PlatformOpenFileLogic (+6 more)

### Community 29 - "Calendar Sync (expect/actual)"
Cohesion: 0.09
Nodes (11): PlatformCalendarSync, PlatformProvider, PlatformCalendarSync, ContentType, PlatformOpenFileLogic, ByteArray, ContentType, PlatformPrinter (+3 more)

### Community 30 - "Database Integrity Verifier Tests"
Cohesion: 0.19
Nodes (3): DispatcherProvider, TestDatabaseIntegrityVerifier, DispatcherProvider

### Community 31 - "Android In-App Updates"
Cohesion: 0.13
Nodes (10): ActivityResultLauncher, AppUpdateInfo, AppUpdateManager, ComponentActivity, IntentSenderRequest, RuntimeException, Context, Flow (+2 more)

### Community 32 - "NFC Read/Write"
Cohesion: 0.12
Nodes (9): Continuation, Mutex, Tag, PlatformNFC, Uuid, NfcPayload, PlatformNFC, PlatformNFC (+1 more)

### Community 33 - "Inventory Item DAO"
Cohesion: 0.13
Nodes (5): InventoryItemDao, Flow, org, Uuid, InventoryItemEntity

### Community 34 - "App Root Composable"
Cohesion: 0.15
Nodes (13): App(), PushNotification, Url, MainApp(), Flow, PlatformAppUpdates, ErrorDialog(), UpdateAvailableDialog() (+5 more)

### Community 35 - "Inventory Item Types Repository"
Cohesion: 0.18
Nodes (5): InventoryItemTypeEntity, InventoryItemTypesRepository, Flow, ReferencedInventoryItemType, Uuid

### Community 36 - "Departments Management ViewModel"
Cohesion: 0.19
Nodes (11): DepartmentsManagementViewModel, Department, DepartmentMemberInfo, DepartmentRole, PlatformFile, Uuid, ViewModel, ReferencedLending (+3 more)

### Community 37 - "Activity Memory Editor Form"
Cohesion: 0.18
Nodes (17): ImageFileListContainer, AutocompleteFormField(), Modifier, T, ActivityMemoryEditor(), ActivityMemoryEditor_Preview(), Department, Member (+9 more)

### Community 38 - "Lendings Page Icons"
Cohesion: 0.17
Nodes (14): LazyGridState, ProfileResponse, ReferencedInventoryItem, ReferencedInventoryItemType, ReferencedLending, Uuid, WindowSizeClass, LendingItem_Large() (+6 more)

### Community 39 - "Network Error Handling"
Cohesion: 0.13
Nodes (10): PartData, toFormData(), InternetAccessNotAvailable, IllegalStateException, isNoConnectionError(), CharArray, PlatformFile, ProfileResponse (+2 more)

### Community 40 - "Received Item DAO"
Cohesion: 0.17
Nodes (7): ReceivedItem, Flow, Uuid, ReceivedItemDao, InventoryItem, Uuid, ReceivedItemEntity

### Community 41 - "Post DAO"
Cohesion: 0.17
Nodes (5): Flow, Uuid, PostDao, PostEntity, PostWithRelations

### Community 42 - "Users List UI"
Cohesion: 0.15
Nodes (12): Department, Member, ProfileResponse, UserData, UsersListView(), Modifier, ReadOnlyFormField(), ImageVector (+4 more)

### Community 43 - "JVM Drag & Drop / QR"
Cohesion: 0.17
Nodes (10): ContentType, DragAndDropTransferData, QrCodePainter, PlatformDragAndDrop, ByteArrayTransferable, DataFlavor, Transferable, FileTransferable (+2 more)

### Community 44 - "User Profile Data"
Cohesion: 0.14
Nodes (9): getUser(), isStub(), UserData, ProfileResponse, ProfileRepository, Department, ReferencedLending, ViewModel (+1 more)

### Community 45 - "WorkManager Background Job Worker"
Cohesion: 0.19
Nodes (13): CoroutineWorker, Result, BackgroundJobWorker, Default, Download, Composable, Progress, NamedDownload (+5 more)

### Community 46 - "Background Job Coordinator (Workers impl)"
Cohesion: 0.27
Nodes (6): BackgroundJobCoordinator, KoinComponent, MutableStateFlow, ObservableBackgroundJob, Progress, Uuid

### Community 47 - "App Base Lifecycle"
Cohesion: 0.18
Nodes (10): Application, PointerEvent, AppBase, KoinComponent, initKoin(), Module, initializeSentry(), initKoinIos() (+2 more)

### Community 48 - "Memory DAO"
Cohesion: 0.23
Nodes (5): Flow, Uuid, MemoryDao, MemoryEntity, MemoryWithRelations

### Community 49 - "Settings Dialogs & SSE Config"
Cohesion: 0.16
Nodes (10): PlatformSSEConfiguration, RemoveAccountDialog(), SettingsCategory(), ImageVector, SettingsSwitchRow(), Language, SettingsScreen(), SettingsScreen_NoFcmToken_Preview() (+2 more)

### Community 50 - "Lending Page Layout Helpers"
Cohesion: 0.18
Nodes (10): LendingPageOnCreate, ColumnWidthWrapper(), Alignment, Dp, Modifier, DropdownSelector(), Modifier, T (+2 more)

### Community 51 - "Rich Text Editor & Item Details"
Cohesion: 0.19
Nodes (10): RichSpanStyle, RichTextConfig, SpanStyle, PetzlItemDetails, Modifier, RichTextState, RichTextStyleRow(), SpellCheck (+2 more)

### Community 52 - "Auth Backend"
Cohesion: 0.18
Nodes (4): AuthBackend, getHttpClient(), FCMTokenManager, FCMTokenRemote

### Community 53 - "Lending DAO"
Cohesion: 0.22
Nodes (6): Flow, org, Uuid, LendingDao, LendingEntity, LendingWithRelations

### Community 54 - "Shared-Element Transitions"
Cohesion: 0.18
Nodes (11): Modifier, sharedBounds(), Alignment, Dp, Modifier, LazyColumnWidthWrapper(), InventoryItemTypeDetailsScreen(), ReferencedInventoryItemType (+3 more)

### Community 55 - "Members Remote Repo & Symmetric Base"
Cohesion: 0.16
Nodes (8): Member, UInt, MembersRemoteRepository, EntityType, IdType, SymmetricRemoteRepository, UserData, UsersRemoteRepository

### Community 56 - "Create Inventory Item Dialog"
Cohesion: 0.18
Nodes (11): CreateInventoryItemDialog(), ReferencedInventoryItemType, Department, ProfileResponse, ReferencedPost, PostsListView(), DropdownField(), Composable (+3 more)

### Community 57 - "Department Role Editor Dialog"
Cohesion: 0.21
Nodes (12): DepartmentMemberRolesDialog(), description(), displayName(), joinedDisplayNames(), DepartmentRole, DepartmentPendingJoinRequest(), Department, UserData (+4 more)

### Community 58 - "Content Type Icons"
Cohesion: 0.13
Nodes (4): departmentsCountBadge(), lendingsCountBadge(), ViewModel, ManagementPageScreenModel

### Community 59 - "Push Notification Listener"
Cohesion: 0.18
Nodes (8): CoroutineScope, PayloadData, PushListener, KoinComponent, PushNotifierListener, KoinComponent, PlatformLoadLogic, PlatformBackHandler()

### Community 60 - "NFC Tag Utilities"
Cohesion: 0.20
Nodes (9): ByteArray, Intent, Tag, NfcUtils, Throwable, NfcException, NfcTagFormatNotSupportedException, NfcTagIsReadOnlyException (+1 more)

### Community 61 - "Inventory Cross-Reference Tests"
Cohesion: 0.38
Nodes (3): InventoryItemTypeWithRelations, InventoryItemWithRelations, Uuid

### Community 62 - "FEMECV Account Card"
Cohesion: 0.21
Nodes (9): FEMECVAccountCard(), ProfileResponse, Modifier, PasswordFormField(), AuthScreen(), AuthScreen_Form(), AuthScreen_Login(), AuthScreen_Register() (+1 more)

### Community 64 - "Inventory Item Type DAO"
Cohesion: 0.24
Nodes (4): InventoryItemTypeDao, Flow, org, Uuid

### Community 66 - "Events List UI"
Cohesion: 0.22
Nodes (10): EventsListView(), Department, ProfileResponse, ReferencedEvent, FormImagePicker(), ImageFileContainer, Modifier, PlatformFile (+2 more)

### Community 67 - "Adaptive Layout (Window Size)"
Cohesion: 0.24
Nodes (9): LendingsPage(), calculateWindowSizeClass(), WindowSizeClass, AdaptiveTabRow(), ImageVector, Modifier, StringResource, WindowSizeClass (+1 more)

### Community 68 - "Android Content Provider"
Cohesion: 0.25
Nodes (6): ContentProvider, ContentValues, Cursor, ContextProvider, Context, Uri

### Community 69 - "Cookie Storage"
Cohesion: 0.30
Nodes (5): Cookie, CookiesStorage, CookieWithTimestamp, Url, SettingsCookiesStorage

### Community 70 - "JVM In-App Updates (GitHub releases)"
Cohesion: 0.23
Nodes (4): JsonArray, Flow, KoinComponent, PlatformAppUpdates

### Community 71 - "Android Open/Share File"
Cohesion: 0.19
Nodes (8): ContentType, PlatformOpenFileLogic, ContentType, PlatformShareLogic, FilePermissionsUtil, ContentType, Context, Uri

### Community 72 - "User DAO"
Cohesion: 0.24
Nodes (3): Flow, UserDao, UserEntity

### Community 73 - "Inventory Items Repository"
Cohesion: 0.34
Nodes (4): InventoryItemsRepository, Flow, ReferencedInventoryItem, Uuid

### Community 74 - "Departments Remote Repository"
Cohesion: 0.25
Nodes (6): DepartmentsRemoteRepository, ByteArray, Department, DepartmentMemberInfo, DepartmentRole, Uuid

### Community 75 - "NFC Intent Handler Activity"
Cohesion: 0.29
Nodes (7): AppCompatActivity, IntentFilter, NfcAdapter, PendingIntent, Bundle, Intent, NfcIntentHandlerActivity

### Community 76 - "Date Picker Form Field"
Cohesion: 0.26
Nodes (10): ClosedRange, clickInteractionSource(), DatePickerFormField(), DateTimeFormat, Modifier, SelectableDates, DateTimePickerFormField(), DateTimeFormat (+2 more)

### Community 77 - "Background Job Coordinator (Android)"
Cohesion: 0.27
Nodes (7): Operation, BackgroundJobCoordinator, kotlin, ObservableBackgroundJob, ObservableUniqueBackgroundJob, Uuid, WorkManager

### Community 78 - "Departments Repository"
Cohesion: 0.31
Nodes (4): DepartmentsRepository, Department, Flow, Uuid

### Community 79 - "Posts Repository"
Cohesion: 0.35
Nodes (4): Flow, ReferencedPost, Uuid, PostsRepository

### Community 80 - "Memory Detail Dialog"
Cohesion: 0.29
Nodes (10): Modifier, ReferencedMemory, SnackbarHostState, LabelWithTitle(), MemoryDialog(), MemoryDisplay(), CloseButton(), Color (+2 more)

### Community 81 - "Insurance Dialog"
Cohesion: 0.31
Nodes (11): InsuranceDialog(), InsuranceDialog_FEMECV2026_Preview(), InsuranceDialog_Generic_Preview(), InsuranceInfoText(), Progress, StringResource, UserInsurance, Modifier (+3 more)

### Community 82 - "Inventory Management ViewModel"
Cohesion: 0.28
Nodes (7): InventoryManagementViewModel, Department, PlatformFile, ReferencedInventoryItem, ReferencedInventoryItemType, Uuid, ViewModel

### Community 83 - "Cross-Platform File System"
Cohesion: 0.23
Nodes (5): ByteWriteChannel, FileSystem, ByteArray, ByteReadChannel, copyTo()

### Community 84 - "Add Insurance Dialog & File Picker"
Cohesion: 0.29
Nodes (9): FileKitType, AddInsuranceDialog(), CreateInsuranceRequest, FormFilePicker(), Modifier, PlatformFile, ImageVector, Modifier (+1 more)

### Community 85 - "Posts Remote Repository"
Cohesion: 0.41
Nodes (5): Post, PlatformFile, ReferencedPost, Uuid, PostsRemoteRepository

### Community 86 - "Main Activity (Android)"
Cohesion: 0.24
Nodes (5): Bundle, Intent, PushNotification, Url, MainActivity

### Community 87 - "Manufacturer Item Details"
Cohesion: 0.20
Nodes (5): BasePetzlItemDetails, DrawableResource, DrawableResource, ManufacturerItemDetails, PetzlOldItemDetails

### Community 88 - "Users Repository"
Cohesion: 0.27
Nodes (3): Flow, UserData, UsersRepository

### Community 89 - "Inventory Items Remote Repository"
Cohesion: 0.41
Nodes (5): InventoryItemsRemoteRepository, ByteArray, InventoryItem, ReferencedInventoryItem, Uuid

### Community 90 - "iOS Share Logic"
Cohesion: 0.23
Nodes (5): ContentType, UIViewController, PlatformShareLogic, ContentType, PlatformOpenFileLogic

### Community 91 - "Edit Inventory Item & Lending History Dialogs"
Cohesion: 0.26
Nodes (6): EditInventoryItemDialog(), ReferencedInventoryItem, ReferencedLending, LendingHistoryItem(), LendingsHistoryDialog(), MaterialSymbols

### Community 92 - "Memories Page UI"
Cohesion: 0.33
Nodes (8): ReferencedMemory, MemoriesPage(), MemoryCard(), MemoryListContent(), TaggedMemoriesPage(), ActivitiesPage(), ViewModel, MemoriesViewModel

### Community 93 - "Profile Page ViewModel"
Cohesion: 0.24
Nodes (6): CharArray, Deferred, Department, PlatformFile, ViewModel, ProfilePageModel

### Community 94 - "Entity-With-Relations Mappers"
Cohesion: 0.29
Nodes (7): ReferencedInventoryItem, toReferenced(), ReferencedLending, toReferenced(), ReferencedMemory, toReferenced(), MissingCrossReferenceException

### Community 95 - "HTTP Client Setup"
Cohesion: 0.31
Nodes (7): configureLogging(), createHttpClient(), HttpClient, createHttpClientEngine(), HttpClientEngine, Job, SSENotificationsListener

### Community 96 - "Inventory Item Type Dialogs"
Cohesion: 0.25
Nodes (6): CreateInventoryItemTypeDialog(), EditDialog(), EditInventoryItemTypeDialog(), InventoryItemType, AutocompleteMultipleFormField(), Modifier

### Community 97 - "Activity Memory Editor ViewModel"
Cohesion: 0.31
Nodes (10): ActivityMemoryEditorViewModel, Department, Member, PlatformFile, RichTextState, Sports, Uuid, ViewModel (+2 more)

### Community 98 - "Events Management ViewModel"
Cohesion: 0.35
Nodes (7): EventsManagementViewModel, Department, PlatformFile, ReferencedEvent, RichTextState, Uuid, ViewModel

### Community 99 - "Posts Management ViewModel"
Cohesion: 0.35
Nodes (7): Department, PlatformFile, ReferencedPost, RichTextState, Uuid, ViewModel, PostsManagementViewModel

### Community 100 - "JVM Clipboard"
Cohesion: 0.29
Nodes (5): ClipEntry, PlatformClipboard, DataFlavor, Transferable, TextTransferable

### Community 101 - "Background Job State"
Cohesion: 0.20
Nodes (8): toBackgroundJobState(), BackgroundJobState, BLOCKED, CANCELLED, ENQUEUED, FAILED, RUNNING, SUCCEEDED

### Community 102 - "Loading & Error Screens"
Cohesion: 0.31
Nodes (6): PaddingValues, Progress, LoadingBox(), Progress, LoadingScreen(), LogoutScreen()

### Community 103 - "Dropdown Icon Button & Choice Flow"
Cohesion: 0.29
Nodes (7): DropdownIconButton(), ImageVector, Modifier, T, Choice, T, or()

### Community 104 - "File Provider ViewModel"
Cohesion: 0.38
Nodes (9): FileProviderModel, ContentType, FileContainer, MutableStateFlow, Progress, openFile(), saveFile(), shareFile() (+1 more)

### Community 105 - "Local Notifications"
Cohesion: 0.36
Nodes (4): KoinComponent, PushNotification, StringResource, LocalNotifications

### Community 106 - "Background Job Coordinator (common)"
Cohesion: 0.36
Nodes (8): kotlin, ObservableBackgroundJob, ObservableUniqueBackgroundJob, Uuid, observe(), observeUnique(), schedule(), scheduleAsync()

### Community 107 - "Navigator"
Cohesion: 0.32
Nodes (3): KClass, Navigator, rememberNavigator()

### Community 108 - "Settings Row UI"
Cohesion: 0.36
Nodes (5): ListItemColors, Composable, ImageVector, Shape, SettingsRow()

### Community 109 - "Logger Abstraction (expect/actual)"
Cohesion: 0.39
Nodes (4): LogLevelController, Logger, notifierManagerLogger(), JvmLogger

### Community 110 - "Save File Logic"
Cohesion: 0.43
Nodes (6): Source, FileContainer, PlatformFile, Uuid, pickAndSave(), PlatformSaveFileLogic

### Community 111 - "ViewModel Coroutine Utils"
Cohesion: 0.48
Nodes (6): CoroutineStart, SharingStarted, async(), Deferred, T, stateInViewModel()

### Community 112 - "Android Drag & Drop / QR"
Cohesion: 0.52
Nodes (4): ContentType, DragAndDropTransferData, QrCodePainter, PlatformDragAndDrop

### Community 113 - "Observable Background Job (Android)"
Cohesion: 0.48
Nodes (4): Flow, Progress, Uuid, ObservableBackgroundJob

### Community 114 - "Drag & Drop / QR (common)"
Cohesion: 0.52
Nodes (4): ContentType, DragAndDropTransferData, QrCodePainter, PlatformDragAndDrop

### Community 115 - "Platform Initializer"
Cohesion: 0.33
Nodes (3): PlatformLoadLogic, ViewModel, PlatformInitializerViewModel

### Community 116 - "Lending Page & ViewModel"
Cohesion: 0.48
Nodes (5): SnackbarHostState, LendingPage(), LendingsActionBarIcons(), ViewModel, LendingPageModel

### Community 118 - "Navigation Page Enum"
Cohesion: 0.29
Nodes (7): Page, ACTIVITIES, HOME, LENDING, LENDINGS, MANAGEMENT, PROFILE

### Community 119 - "Main ViewModel"
Cohesion: 0.43
Nodes (3): ReferencedLending, ViewModel, MainViewModel

### Community 120 - "Users Management ViewModel"
Cohesion: 0.48
Nodes (4): Department, UserData, ViewModel, UsersManagementViewModel

### Community 121 - "Observable Background Job (Workers impl)"
Cohesion: 0.48
Nodes (4): Flow, KoinComponent, Progress, ObservableBackgroundJob

### Community 122 - "iOS Drag & Drop / QR"
Cohesion: 0.52
Nodes (4): ContentType, DragAndDropTransferData, QrCodePainter, PlatformDragAndDrop

### Community 123 - "Event-User Cross-Ref DAO"
Cohesion: 0.40
Nodes (3): EventUserCrossRefDao, org, Uuid

### Community 124 - "Memory-Member Cross-Ref DAO"
Cohesion: 0.40
Nodes (3): org, Uuid, MemoryMemberCrossRefDao

### Community 125 - "Log Level Enum"
Cohesion: 0.33
Nodes (6): Level, DEBUG, ERROR, INFO, VERBOSE, WARN

### Community 126 - "Grammatical Gender Enum"
Cohesion: 0.33
Nodes (5): GrammaticalGender, FEMININE, MASCULINE, NEUTRAL, NOT_SPECIFIED

### Community 127 - "Settings Options Row"
Cohesion: 0.53
Nodes (5): CloseTextButton(), Composable, ImageVector, T, SettingsOptionsRow()

### Community 128 - "Lendings Page ViewModel"
Cohesion: 0.53
Nodes (3): ReferencedInventoryItemType, ViewModel, LendingsPageModel

### Community 130 - "Android Printer"
Cohesion: 0.50
Nodes (3): ByteArray, ContentType, PlatformPrinter

### Community 131 - "Clipboard (common)"
Cohesion: 0.60
Nodes (3): ClipEntry, PlatformClipboard, setClipEntry()

### Community 132 - "Printer (common)"
Cohesion: 0.50
Nodes (3): ByteArray, ContentType, PlatformPrinter

### Community 134 - "Observable Unique Background Job (Workers impl)"
Cohesion: 0.60
Nodes (3): Flow, KoinComponent, ObservableUniqueBackgroundJob

### Community 135 - "JVM Printer"
Cohesion: 0.50
Nodes (3): ByteArray, ContentType, PlatformPrinter

### Community 140 - "Database Koin Module"
Cohesion: 1.00
Nodes (3): databaseModules(), Module, platformDatabaseModule()

## Knowledge Gaps
- **53 isolated node(s):** `PushScanModule`, `VERBOSE`, `DEBUG`, `INFO`, `WARN` (+48 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 377 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **33 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ProgressNotifier` connect `File Path Resolution` to `Memories Repository`, `Events Repository`, `Error Handling ViewModels`, `Lending Exceptions`, `Database Integrity Verifier`, `Entity Sync & Caching`, `Departments Management ViewModel`, `Network Error Handling`, `WorkManager Background Job Worker`, `Background Job Coordinator (Workers impl)`, `Members Remote Repo & Symmetric Base`, `Create Inventory Item Dialog`, `Department Role Editor Dialog`, `Events List UI`, `Departments Remote Repository`, `Insurance Dialog`, `Cross-Platform File System`, `Posts Remote Repository`, `Inventory Items Remote Repository`, `Activity Memory Editor ViewModel`, `Events Management ViewModel`, `Posts Management ViewModel`, `File Provider ViewModel`, `Save File Logic`?**
  _High betweenness centrality (0.117) - this node is a cross-community bridge._
- **Why does `LoadingBox()` connect `Loading & Error Screens` to `App Root Composable`, `Adaptive Layout (Window Size)`, `Events List UI`, `Lendings Page Icons`, `Users List UI`, `Misc Icons & Event Item`, `Entity Visibility Rules`, `Logout Dialog & Icons`, `Brand Icons & Department Card`, `Barcode Scanning & Camera Permission`, `Lending Page & ViewModel`, `Contact Icons & Back Button`, `Referenced-Entity Extensions`, `Create Inventory Item Dialog`, `Department Role Editor Dialog`, `Content Type Icons`, `Memories Page UI`?**
  _High betweenness centrality (0.058) - this node is a cross-community bridge._
- **Why does `DispatcherProvider` connect `Koin DI & Dispatcher Provider` to `Post Date Formatting`, `Memories Repository`, `Error Handling ViewModels`, `Dialog Context & Delete Dialog`, `Barcode Scanning & Camera Permission`, `Database Integrity Verifier`, `File Path Resolution`, `Departments Management ViewModel`, `Network Error Handling`, `WorkManager Background Job Worker`, `Background Job Coordinator (Workers impl)`, `Auth Backend`, `Lending DAO`, `Push Notification Listener`, `JVM In-App Updates (GitHub releases)`, `Inventory Management ViewModel`, `HTTP Client Setup`, `Activity Memory Editor ViewModel`, `Events Management ViewModel`, `Posts Management ViewModel`, `File Provider ViewModel`, `Local Notifications`, `Platform Initializer`, `Users Management ViewModel`?**
  _High betweenness centrality (0.058) - this node is a cross-community bridge._
- **Are the 29 inferred relationships involving `launch()` (e.g. with `launchWithCameraPermission()` and `copyToProgress()`) actually correct?**
  _`launch()` has 29 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PushScanModule`, `VERBOSE`, `DEBUG` to the rest of the system?**
  _53 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Post Date Formatting` be split into smaller, more focused modules?**
  _Cohesion score 0.05989110707803993 - nodes in this community are weakly interconnected._
- **Should `Runtime Permissions` be split into smaller, more focused modules?**
  _Cohesion score 0.05701754385964912 - nodes in this community are weakly interconnected._