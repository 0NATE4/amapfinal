# Android Amap POI Search App

A modern Android application built with the Amap (AutoNavi) SDK for location-based POI (Point of Interest) searching and map visualization.

## Features

- **Location-based POI Search**: Search for nearby points of interest using keywords
- **Interactive Map**: Smooth map interaction with custom markers
- **Permission Management**: Handles location permissions gracefully
- **Privacy Compliance**: Includes privacy policy compliance for Chinese regulations
- **Modern UI**: Clean, responsive interface with smooth loading transitions
- **Comprehensive Testing**: Unit and integration tests for all components

## Architecture

The app follows **MVVM (Model-View-ViewModel)** architecture with clean separation of concerns:

```
app/src/main/java/com/example/amap/
├── MainActivity.kt                    # Main activity (UI coordinator)
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

- **MainActivity**: Main UI coordinator (~100 lines, refactored from 338 lines)
- **MapViewModel**: Manages map state, location permissions, and search results using LiveData
- **MapController**: Handles all map operations including camera control, markers, and lifecycle
- **POISearchManager**: Encapsulates Amap POI search API interactions
- **SearchResultsProcessor**: Processes and transforms search results for UI display

### UI Components

- **POIResultsAdapter**: RecyclerView adapter for displaying search results
- **SearchUIHandler**: Manages search input, keyboard interactions, and UI state
- **Custom Loading**: Black overlay during app initialization to prevent Beijing flash

### Data Models

- **POIDisplayItem**: Unified data model for POI display in RecyclerView

## Technical Specifications

- **Target SDK**: 36
- **Minimum SDK**: 24
- **Language**: Kotlin
- **Architecture**: MVVM with LiveData
- **UI Framework**: Android Views (not Compose)
- **Mapping SDK**: Amap 3D Map SDK
- **Testing**: JUnit 5, MockK, Espresso
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

2. Add your Amap API key to `app/src/main/AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.amap.api.v2.apikey"
       android:value="YOUR_API_KEY_HERE" />
   ```

3. Build and run:
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

## Dependencies

### Core Dependencies
- Amap 3D Map SDK with Location and Search modules
- AndroidX Core, AppCompat, Fragment, Lifecycle
- Material Design Components
- ConstraintLayout, RecyclerView

### Testing Dependencies
- JUnit 5 (Jupiter)
- MockK for Kotlin mocking
- Kotlinx Coroutines Test
- Espresso for UI testing
- AndroidX Test Core and Rules

## App Behavior

### Initialization Flow
1. **Black Loading Screen**: Prevents Beijing default location flash
2. **Permission Check**: Requests location permissions if needed
3. **Map Setup**: Initializes map with world view
4. **Location Acquisition**: Gets user location and centers map
5. **Search Ready**: UI becomes interactive for POI searches

### Search Flow
1. User enters search keywords in top search bar
2. POISearchManager queries Amap API for nearby POIs
3. SearchResultsProcessor transforms results to POIDisplayItem models
4. Results displayed in bottom RecyclerView with distances
5. Tapping results centers map on selected POI

## Testing

The project includes comprehensive testing:

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

### Test Coverage
- **MapViewModelTest**: Permission state management
- **POISearchManagerTest**: Search API interactions
- **SearchResultsProcessorTest**: Data transformation logic
- **MapControllerTest**: Map operations and lifecycle
- **MainActivityLogicTest**: Business logic integration
- **UI Integration Tests**: End-to-end user flows

## Privacy & Compliance

The app includes privacy compliance for Chinese regulations:
- Privacy policy acceptance tracking
- Location permission explanations
- Data usage transparency

## Development Notes

### Recent Refactoring
- Broke down 338-line MainActivity into focused components
- Implemented proper MVVM separation
- Added comprehensive test suite
- Fixed map loading UX issues

### Code Quality
- Clean architecture principles
- Single Responsibility Principle
- Dependency injection patterns
- Comprehensive error handling
- Proper lifecycle management

## Contributing

1. Follow the existing MVVM architecture
2. Add unit tests for new features
3. Update this README for structural changes
4. Ensure privacy compliance for new data usage

## License

[Add your license information here] 