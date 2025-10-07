# 🎨 OpenCV Filter App  
### Real-Time Camera Filters with Kotlin + OpenCV + Native C++ (JNI)

> **OpenCV Filter App** is an advanced Android application that applies **real-time camera filters** using the **Camera2 API** and **OpenCV native library (C++)**.  
> The app delivers instant visual transformations like *Cartoon, Edge Detection, Blur,* and *Grayscale* — all while maintaining high FPS.  
> A polished Material UI, smooth thumbnail animations, and custom toast notifications make it both beautiful and fast.

---

## 🚀 Project Overview

This project bridges **Kotlin (Android)** and **OpenCV (C++)** using the **JNI (Java Native Interface)** to perform pixel-level image processing in native code for maximum performance.

Every frame from the live camera preview is captured, processed through native OpenCV filters, and displayed instantly.  
Users can:
- 🎛️ Select filters via dropdown spinner  
- 🎚️ Adjust intensity using a slider  
- 📸 Capture images with flash and vibration feedback  
- 🖼️ View saved images via animated thumbnails  
- 📂 Access the gallery to view previously captured photos  

---

## 🧩 Key Features

| Category | Description |
|-----------|--------------|
| 🧠 **Real-Time Processing** | OpenCV filters run directly on live frames for smooth and instant results. |
| 🎨 **Filter Modes** | Supports *None*, *Cartoon*, *Edge*, *Blur*, and *Grayscale* filters. |
| ⚡ **Native Acceleration** | Filters are computed using C++ and OpenCV for optimized performance. |
| 📷 **Camera2 API** | Modern camera handling for stable frame streaming. |
| 🪄 **Dynamic UI** | Filter dropdown, intensity control, and animated thumbnails. |
| 💾 **Instant Save** | Captures are stored in `/Pictures/OpenCVFilterApp` and visible in Gallery. |
| 💬 **Stylish Feedback** | Custom animated toast pop-ups for success/failure. |
| 🎚️ **Intensity Control** | Dynamically adjusts edge/blur levels in real time. |
| 🖼️ **Thumbnail Animation** | Smooth upward transition and fade when a new photo is captured. |
| 📂 **In-App Gallery** | Integrated grid and fullscreen image preview. |
| 💜 **Material Theme** | Elegant purple-accent UI with rounded edges and modern typography. |

---

## ⚙️ Tech Stack

| Layer | Technology |
|--------|-------------|
| **Language** | Kotlin (Android), C++ (Native) |
| **Framework** | Camera2 API + OpenCV 4.x |
| **Build System** | Gradle + CMake + NDK |
| **UI Design** | Material Components + ConstraintLayout |
| **Animation** | Android Animator APIs |
| **Storage** | MediaStore + Scoped Storage |

---

## 🧠 Filters Implemented

All filters are processed inside the native layer (`native-lib.cpp`) and called from Kotlin through JNI.

| ID | Filter Name | Description |
|----|--------------|-------------|
| 0 | **None** | Displays original frame without modifications. |
| 1 | **Cartoon** | Blends edge detection with color quantization to give a cartoonish look. |
| 2 | **Edge Detection** | Uses Canny edge detector; adjustable via intensity slider. |
| 3 | **Blur** | Applies Gaussian blur proportional to intensity. |
| 4 | **Grayscale** | Converts frame to black & white image. |

---

### 🧮 Example — Cartoon Filter (C++ Code)

```cpp
case 1: { // Cartoon Filter
    Mat bgr, gray, edges, color;
    cvtColor(src, bgr, COLOR_RGBA2BGR);
    cvtColor(bgr, gray, COLOR_BGR2GRAY);
    medianBlur(gray, gray, 5);
    Laplacian(gray, edges, CV_8U, 5);
    threshold(edges, edges, 100, 255, THRESH_BINARY_INV);
    Mat small; 
    pyrDown(bgr, small); 
    pyrDown(small, small);
    pyrUp(small, small); 
    pyrUp(small, small);
    cvtColor(edges, edges, COLOR_GRAY2BGR);
    bitwise_and(small, edges, color);
    cvtColor(color, dst, COLOR_BGR2RGBA);
}
```

---

## 🧱 Architecture Overview

```
📱 Kotlin (MainActivity)
│
│   ├── Captures frames from Camera2 TextureView
│   ├── Sends bitmap + filter mode + intensity to JNI
│   ├── Displays processed frame in ImageView
│   ├── Handles capture button, animations, toast, and save
│   └── Updates thumbnail preview
│
└── 🧠 Native Layer (C++ - native-lib.cpp)
    ├── Receives input & output bitmaps
    ├── Converts between ARGB ↔ Mat using AndroidBitmap
    ├── Runs OpenCV filter pipeline
    ├── Returns processed frame
    └── Ensures memory safety & efficiency
```

---

## 🖥️ App UI Flow

### 🎛️ Filter Dropdown
- Default shows “🎨 Select Filter”
- Options: None | Cartoon | Edge | Blur | Grayscale
- Auto-hides intensity bar for filters where not needed.

### 🎚️ Intensity Slider
- Updates blur radius or Canny thresholds live.
- Value shown as **Filter Intensity: 0–100%**

### 📸 Capture Button
- Captures current processed frame.
- Triggers flash effect + vibration.
- Saves image via MediaStore to app folder.

### 🖼️ Animated Thumbnail
- Old image smoothly lifts upward and fades out.
- New image fades in from bottom.
- Clicking thumbnail opens image in system gallery.

### 💬 Custom Toast
- Pop-up with “📸 Image Saved Successfully!” message.
- Styled with purple rounded background and drop shadow.
- Animated fade-in from top center.

---

## 🧰 Build & Installation Guide

### 🔧 Prerequisites
- Android Studio **Flamingo or newer**
- Installed **NDK** and **CMake** (via SDK Manager)
- OpenCV SDK (for Android)

### 🪜 Setup Steps

```bash
# Clone repository
git clone https://github.com/GSNAIK-GAUTAMI/OpenCVFilterApp.git
cd OpenCVFilterApp

# Open in Android Studio
# Let Gradle sync and build automatically

# Connect your Android device (enable USB debugging)
# Click ▶ Run
```

---

## 🧑‍💻 Developer

**👨‍💻 Ravi Raj**  
💼 IIIT Dharwad, India  

---

> “Where creativity meets computer vision — turning every frame into art.”
