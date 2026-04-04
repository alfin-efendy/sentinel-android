# 📱 Android App Monitor & Auto Relaunch System

## 🧠 Overview
This project is an Android native application designed to:

- Monitor a target application (foreground detection)
- Automatically relaunch the app if it is closed
- Support deep links for app launching

Target use cases:
- Game persistence (e.g., Roblox)
- Automation / recovery tools
- Internal tools (not for Play Store distribution)

---

## 🧩 Core Features

1. Real-time App Monitoring
2. Auto Relaunch (Intent / Deep Link)
3. Foreground Background Service
4. Configurable Target App

---

## ⚙️ Architecture

MainActivity (UI Config)
    ↓
DataStore / SharedPreferences
    ↓
ForegroundService
    ↓
AccessibilityService (App Monitor)
    ↓
App Relaunch Engine

---

## 📦 Modules

### 1. UI Layer
- Select target app (package name)
- Input deep link (optional)
- Start / Stop monitoring

### 2. Monitoring Layer
- AccessibilityService
- Detect current foreground app
- Compare with target app

### 3. Execution Layer
- Relaunch app
- Handle deep link vs launcher intent

### 4. System Layer
- Foreground service (keep app alive)
- Permission handling

---

## 🔐 Required Permissions

- SYSTEM_ALERT_WINDOW
- FOREGROUND_SERVICE
- BIND_ACCESSIBILITY_SERVICE
- PACKAGE_USAGE_STATS (optional fallback)

---

## 🔁 Monitoring Logic

IF currentApp != targetApp:
    trigger relaunch()

Debounce:
- Prevent infinite loop (minimum delay: 2 seconds)

---

## 🚀 Relaunch Strategy

Priority:
1. Deep link (if provided)
2. Launch intent (via PackageManager)

---

## 🧠 Edge Case Handling

- Infinite loop prevention
- App loading delay
- User manual navigation
- System kill recovery

---

## ⚠️ Constraints

- Not intended for Play Store distribution
- Accessibility usage must be controlled

---

## 📊 Future Enhancements

- Multi-app monitoring
- Smart detection (crash vs user exit)
- Usage analytics
- Remote configuration

---

## 🧪 Testing Strategy

- Manual testing with multiple apps
- Stress test relaunch loop
- Background kill simulation

---

## 🧰 Tech Stack

- Kotlin
- Android SDK
- Accessibility Service API
- WindowManager
- Foreground Service

---

## 🧭 Naming Convention

- Service: *Service suffix
- Manager: *Manager
- Config: *Config
- Handler: *Handler

---

## 🧱 Example Target App

- com.roblox.client

---

## 🧠 Notes for AI Agents

- Prioritize stability over aggressiveness
- Always debounce relaunch logic
- Avoid infinite loops
- Separate monitoring and execution logic
