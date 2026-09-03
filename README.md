# Learnify

**Learnify** is a modern Android e-learning application designed to provide users with an organized and engaging way to discover courses, access lessons, watch educational videos, and track their learning progress.

The application is built using **Kotlin and Jetpack Compose**, with **Firebase** used for backend services and data management.

## Features

* 🔐 User Authentication
* 🏠 Home Screen
* 📚 Featured Courses
* 🗂️ Course Categories
* 🔥 Popular Courses
* 🔎 Course Search
* 📖 Course Details
* 🎥 Video Lessons
* ✅ Lesson Completion Tracking
* 📊 Learning Progress
* 👤 User Dashboard
* 📱 Modern Android UI
* 🧭 Multi-screen Navigation
* ☁️ Firebase Integration

## Screens

### Home

The home screen provides users with quick access to featured courses, categories, popular courses, and course discovery.

### Course Details

Users can view information about a selected course and access its available learning content.

### Lessons & Videos

Users can watch course lessons and mark lessons as completed while progressing through a course.

### My Courses

Provides users with access to courses they are currently learning.

### Search

Allows users to search and discover courses.

### Dashboard

Provides users with a personal overview of their learning activity and progress.

## Technology Stack

| Technology      | Purpose                      |
| --------------- | ---------------------------- |
| Kotlin          | Primary programming language |
| Jetpack Compose | Android UI development       |
| Firebase        | Backend and cloud services   |
| Android Studio  | Development environment      |
| Git & GitHub    | Version control              |

## Architecture & Development

Learnify follows modern Android development practices with a focus on:

* Reusable Jetpack Compose UI components
* Screen-based navigation
* Firebase integration
* Modular application structure
* Responsive layouts
* Maintainable Kotlin code
* User-focused interface design

## Application Flow

```text
Login / Register
       ↓
     Home
       ↓
 ┌─────┼─────────┐
 ↓     ↓         ↓
Courses Search  Dashboard
 ↓
Course Details
 ↓
Lessons
 ↓
Video Lesson
 ↓
Track Progress
```

## Project Structure

```text
Learnify
├── app
│   └── src
│       └── main
│           ├── java
│           │   └── com.example.learnify
│           │       ├── ui
│           │       ├── screens
│           │       ├── navigation
│           │       ├── data
│           │       └── MainActivity.kt
│           └── res
└── README.md
```

> The exact package and folder structure may vary depending on the current project implementation.

## Getting Started

### Prerequisites

* Android Studio
* Android SDK
* JDK
* Kotlin
* A Firebase project

### Installation

1. Clone the repository:

```bash
git clone https://github.com/Atifkhan5/learnify.git
```

2. Open the project in Android Studio.

3. Configure your Firebase project and add the required `google-services.json` file.

4. Sync the Gradle project.

5. Build and run the application on an Android device or emulator.

## Firebase

Learnify uses Firebase to support backend functionality such as:

* User authentication
* Cloud data storage
* User information
* Course-related data
* Learning progress

Firebase configuration files and credentials should **not** be committed to a public repository.

## Future Improvements

Potential future improvements include:

* Course enrollment system
* Instructor accounts
* Course creation and management
* Downloadable lessons
* Course certificates
* Notifications
* Advanced learning analytics
* Improved recommendation system
* Offline learning support

## Developer

**Atif Khan**

Computer Science Student
Software & Mobile Application Developer

GitHub: https://github.com/Atifkhan5

## License

This project is developed for educational and portfolio purposes.
