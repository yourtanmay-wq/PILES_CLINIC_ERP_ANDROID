# Clinica - Clinic Management & Patient Enquiry System

A comprehensive, fully functional Android application for managing clinic staff, tracking patient registrations, recording enquiries, and handling follow-ups. Built with modern Android standards using Kotlin, Jetpack Compose, and Material Design 3.

## Features

- **Authentication & Role-Based Access**: Log in as MASTER ADMIN, STAFF, etc., with session persistence and branch-specific default staff displays.
- **Enquiry Form Module**: Create patient enquiries with 10-digit auto-formatting telephone numbers (`+91`), field validations, and automatic duplicate mobile checks across registered patients and existing enquiries.
- **Enquiry Follow-Up List**: View, update status, record follow-up call outcomes, or reject enquiries with structured reasons.
- **Clinic Dashboard**: High-level telemetry of pending enquiries, registrations, and status metrics.
- **Room Database Integration**: Fully local database persistence keeping all patient and enquiry records secure offline.

## Project Structure

```text
├── app/
│   ├── build.gradle.kts                      # Module-level build configuration
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml           # App Manifest (permissions, activities)
│           ├── java/com/example/             # Kotlin package source directory
│           │   ├── MainActivity.kt           # Main Application entry point
│           │   ├── data/                     # Room Entities, DAOs, and Repositories
│           │   ├── ui/                       # Jetpack Compose screens, ViewModels, and Theme
│           │   └── util/                     # Utilities (Date formatters, etc.)
│           └── res/                          # Android resources (Strings, drawables, etc.)
├── gradle/
│   └── libs.versions.toml                    # Centralized Version Catalog
├── build.gradle.kts                          # Project-level build configuration
├── settings.gradle.kts                       # Project settings
├── gradle.properties                         # Gradle JVM configurations
└── README.md                                 # Project documentation
```

## Prerequisites

To open, build, and run this project, make sure you have:
- **Android Studio** Ladybug (2024.2.1) or newer.
- **JDK 17** or higher configured in Android Studio.
- **Android SDK** with compileSdk/targetSdk 35 installed.

## Build & Run Instructions

Follow these simple steps to import and run the project locally in Android Studio:

### 1. Extract the Project ZIP
Unzip the downloaded `project.zip` file to your local workspace folder.

### 2. Import into Android Studio
1. Open **Android Studio**.
2. Select **File > Open** or choose **Import Project**.
3. Navigate to the extracted project directory and select the root directory (containing `settings.gradle.kts`).
4. Click **OK** and wait for Android Studio to sync Gradle dependencies.

### 3. Build & Run
1. Connect a physical Android device with USB Debugging enabled, or start an Android Virtual Device (Emulator).
2. Click the **Run** button (green play icon in the top toolbar) or press `Shift + F10` to compile and install the application on your device.

## Technologies Used

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Persistence**: Room Database (SQLite)
- **Dependency Management**: Gradle Version Catalog (`libs.versions.toml`)
