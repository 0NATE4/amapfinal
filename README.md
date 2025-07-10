# Android Amap POI Search App

A modern Android application built with the Amap (AutoNavi) SDK for location-based POI (Point of Interest) searching and map visualization.

## Features

- **Location-based POI Search**: Search for nearby points of interest using keywords
- **Interactive Map**: Smooth map interaction with custom markers and info windows
- **Permission Management**: Handles location permissions gracefully
- **Privacy Compliance**: Includes privacy policy compliance for Chinese regulations
- **Modern UI**: Clean, responsive interface with search bar and results list
- **Distance Calculation**: Shows distance from user location to each POI
- **Comprehensive Testing**: Unit and integration tests for core components

## Architecture

The app follows **MVVM (Model-View-ViewModel)** architecture with separation of concerns:

```
app/src/main/java/com/example/amap/
├── MainActivity.kt                    # Main activity (UI coordinator - 336 lines)
├── core/
│   └── Constants.kt                   # App-wide configuration constants
├── data/
│   └── model/
│       └── POIDisplayItem.kt          # Data models for POI display
├── map/
│   ├── MapViewModel.kt                # Map state management (LiveData, permissions)
│   └── MapController.kt               # Map operations (markers, camera, lifecycle)
├── search/
│   ├── POISearchManager.kt            # POI search logic (Amap API integration)
│   └── SearchResultsProcessor.kt      # Search result data processing
├── ui/
│   ├── POIResultsAdapter.kt           # RecyclerView adapter for search results
│   └── SearchUIHandler.kt             # UI interaction handling
└── util/
    └── AmapPrivacy.kt                 # Privacy compliance utilities
```

## Key Components

### Core Classes

- **MainActivity**: Main UI coordinator (336 lines) - handles initialization, permissions, and component orchestration
- **MapViewModel**: Manages map state, location permissions, and search results using LiveData
- **MapController**: Handles all map operations including camera control, markers, and lifecycle
- **POISearchManager**: Encapsulates Amap POI search API interactions with callback handling
- **SearchResultsProcessor**: Processes and transforms search results for UI display

### UI Components

- **POIResultsAdapter**: RecyclerView adapter for displaying search results with click handling
- **SearchUIHandler**: Manages search input, keyboard interactions, and UI state
- **Loading Overlay**: Black overlay during app initialization to prevent default Beijing location flash

### Data Models

- **POIDisplayItem**: Simple data class containing title, address, distance, and original PoiItem reference

## UI Resources & Layout Structure

The app uses a modern Material Design interface defined in the `res` directory:

### Layout Files

#### `activity_main.xml` - Main Screen Layout
```xml
ConstraintLayout (full screen)
├── MapView (full screen background)
├── SearchContainer (CardView - floating search bar)
│   └── LinearLayout
│       ├── SearchIcon (magnifying glass)
│       ├── SearchEditText (search input)
│       └── ClearButton (X button, initially hidden)
├── ResultsContainer (CardView - floating results)
│   └── RecyclerView (POI search results)
├── LoadingOverlay (black full-screen overlay)
└── MyLocationButton (FAB - bottom right)
```

#### `item_poi_result.xml` - Search Result Item
```xml
LinearLayout (clickable item)
├── LinearLayout (horizontal)
│   ├── LinearLayout (vertical - main content)
│   │   ├── TextView (POI title - bold)
│   │   └── TextView (POI address - subtitle)
│   └── TextView (distance - right aligned)
└── View (subtle divider line)
```

### Visual Design

- **Floating Cards**: Search bar and results use elevated CardViews with rounded corners
- **Material Design**: Uses Material3 theme with NoActionBar
- **Modern Typography**: Sans-serif fonts with varying weights
- **Subtle Shadows**: CardView elevations (12dp search, 10dp results) 
- **Rounded Corners**: 28dp radius search bar, 20dp radius results
- **Clean Spacing**: Consistent margins (16dp) and padding throughout

### Color Scheme

```xml
<!-- UI Colors -->
search_background: #FAFAFA    (light gray search bar)
search_text: #212121          (dark gray text)
search_hint: #9E9E9E          (medium gray hints)
search_icon: #757575          (medium gray icons)
search_border: #E0E0E0        (light gray borders)
results_background: #FFFFFF   (white results background)
```

### Drawables & Icons

- **search_bar_background.xml**: Rounded rectangle shape with border
- **Built-in Android icons**: Search, clear, location icons from system
- **App icons**: Standard launcher icons in all density folders (mdpi to xxxhdpi)

### Theme Configuration

- **Base Theme**: `Theme.Material3.DayNight.NoActionBar`
- **No Action Bar**: Full-screen map with floating UI elements
- **Dark Mode Support**: `values-night/themes.xml` for dark theme variants

### Responsive Design Features

- **Max Height**: Results container limited to 320dp to prevent full-screen coverage
- **Scroll Support**: RecyclerView with nested scrolling for long result lists  
- **Elevation Layering**: Loading overlay at 100dp elevation covers everything
- **Touch Targets**: Minimum 32dp touch areas for clear button and FAB
- **Edge-to-Edge**: Full-screen map with floating UI elements

## Technical Specifications

- **Target SDK**: 36
- **Minimum SDK**: 24
- **Compile SDK**: 36
- **Language**: Kotlin
- **Architecture**: MVVM with LiveData
- **UI Framework**: Android Views (not Compose)
- **Mapping SDK**: Amap 3D Map SDK with Location and Search
- **Testing**: JUnit 4.13.2, MockK, Espresso
- **Build System**: Gradle with Kotlin DSL

## Setup Instructions

### Prerequisites

1. Android Studio Arctic Fox or later
2. Amap developer account and API key
3. JDK 11 or later

### Installation

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd amap
   ```

2. **IMPORTANT - API Key Setup**: 
   The project currently contains a hardcoded API key in `app/src/main/AndroidManifest.xml`. For security:
   - Replace the existing key with your own API key:
   ```xml
   <meta-data
       android:name="com.amap.api.v2.apikey"
       android:value="YOUR_API_KEY_HERE" />
   ```
   - **Never commit API keys to version control**

3. Build and run:
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

## Dependencies

### Core Dependencies
```kotlin
// Amap SDK
implementation("com.amap.api:3dmap-location-search:latest.integration")

// AndroidX Core
implementation("androidx.activity:activity-ktx:1.9.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
implementation("androidx.core:core-ktx:1.10.1")
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")

// Material Design
implementation("com.google.android.material:material:1.10.0")
```

### Testing Dependencies
```kotlin
// Unit Testing
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("androidx.arch.core:core-testing:2.2.0")
testImplementation("io.mockk:mockk:1.13.8")

// Instrumented Testing
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("androidx.test:rules:1.5.0")
androidTestImplementation("androidx.test:runner:1.5.2")
```

## App Behavior

### Initialization Flow
1. **Privacy Compliance**: Ensures Amap privacy policy compliance
2. **Loading Screen**: Black overlay prevents Beijing default location flash
3. **Permission Check**: Requests location permissions if needed
4. **Map Setup**: Initializes map with world view (0,0) to avoid Beijing default
5. **Location Acquisition**: Waits for user location and centers map
6. **Components Ready**: Initializes MapController, POISearchManager, and UI handlers

### Search Flow
1. User enters search keywords in top search bar
2. POISearchManager queries Amap API for nearby POIs (within 1000m radius)
3. SearchResultsProcessor transforms results to POIDisplayItem models
4. Results displayed in bottom RecyclerView with distances calculated
5. Tapping results centers map on selected POI and shows info window
6. Clear button clears search and hides results

### User Location Features
- **My Location Button**: Centers map on user's current location
- **Distance Calculation**: Shows distance from user to each POI result
- **Nearby Search**: Searches within 1000m radius of user location when available

## Configuration Constants

The app uses centralized configuration in `Constants.kt`:

```kotlin
object Search {
    const val DEFAULT_PAGE_SIZE = 10
    const val DEFAULT_SEARCH_RADIUS = 1000 // meters
}

object Map {
    const val DEFAULT_ZOOM_LEVEL = 14f
    const val POI_FOCUS_ZOOM_LEVEL = 16f
    const val SEARCH_RESULTS_ZOOM_LEVEL = 15f
}
```

## Testing

Run tests with:

```bash
# Unit tests
./gradlew test

# Instrumented tests  
./gradlew connectedAndroidTest
```

### Test Coverage
- **MapViewModelTest**: Permission state management and LiveData
- **POISearchManagerTest**: Search API interactions and callbacks
- **SearchResultsProcessorTest**: Data transformation logic
- **MapControllerTest**: Map operations and marker management
- **MainActivityLogicTest**: Business logic integration
- **UI Tests**: User interaction flows and component integration

## Privacy & Compliance

The app includes privacy compliance for Chinese regulations through `AmapPrivacy.kt`:
- Privacy policy acceptance tracking
- Location permission explanations
- Data usage transparency

## Security Considerations

- **API Key Management**: Remove hardcoded API key from AndroidManifest.xml before committing
- **Location Data**: Proper handling of sensitive location information
- **Permission Handling**: Graceful degradation when permissions denied

## Development Status

This is an actively developed project with:
- Complete core functionality implemented
- MVVM architecture in place
- Comprehensive test coverage
- Production-ready privacy compliance

## Contributing

1. Follow the existing MVVM architecture
2. Add unit tests for new features
3. Ensure privacy compliance for new data usage
4. Never commit API keys or sensitive data
5. Update this README for structural changes

## License

[Add your license information here] 