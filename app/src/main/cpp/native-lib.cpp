#include <jni.h>
#include <android/bitmap.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

using namespace cv;

// Helper: Lock bitmap safely
static inline bool lockBitmap(JNIEnv* env, jobject bmp, AndroidBitmapInfo& info, void** pixels) {
    if (AndroidBitmap_getInfo(env, bmp, &info) != ANDROID_BITMAP_RESULT_SUCCESS) return false;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) return false; // Expect RGBA_8888
    if (AndroidBitmap_lockPixels(env, bmp, pixels) != ANDROID_BITMAP_RESULT_SUCCESS) return false;
    return true;
}

// ✅ Main JNI function with filter intensity parameter
extern "C"
JNIEXPORT void JNICALL
Java_com_example_opencvfilterapp_MainActivity_processFrameJNI(
        JNIEnv* env,
        jobject /*thiz*/,
        jobject bitmapIn,
        jobject bitmapOut,
        jint mode,          // 0 = none, 1 = gray, 2 = edge
        jint intensity) {   // 0–100 slider control

    AndroidBitmapInfo inInfo{}, outInfo{};
    void* inPixels = nullptr;
    void* outPixels = nullptr;

    if (!lockBitmap(env, bitmapIn, inInfo, &inPixels)) return;
    if (!lockBitmap(env, bitmapOut, outInfo, &outPixels)) {
        AndroidBitmap_unlockPixels(env, bitmapIn);
        return;
    }

    // Wrap as OpenCV Mats
    Mat src(inInfo.height, inInfo.width, CV_8UC4, inPixels);
    Mat dst(outInfo.height, outInfo.width, CV_8UC4, outPixels);

    // Ensure same size
    if (src.size() != dst.size()) {
        resize(src, dst, dst.size());
    } else {
        switch (mode) {
            case 0: // NONE
                src.copyTo(dst);
                break;

            case 1: { // GRAYSCALE + brightness scaling
                Mat gray;
                cvtColor(src, gray, COLOR_RGBA2GRAY);
                cvtColor(gray, dst, COLOR_GRAY2RGBA);

                // Adjust brightness/contrast using intensity (0–100)
                double scale = std::max(0.1, intensity / 50.0); // 50 = normal brightness
                dst.convertTo(dst, -1, scale, 0);
                break;
            }

            case 2: { // Edge Detection (Canny)
                Mat gray, edges;
                cvtColor(src, gray, COLOR_RGBA2GRAY);
                GaussianBlur(gray, gray, Size(5, 5), 1.2);

                // Reverse intensity mapping for correct behavior
                double factor = (100.0 - intensity) / 100.0;  // 0 → 1 range reversed
                double lowThresh = 30 + (factor * 80);        // 30–110 range
                double highThresh = 90 + (factor * 120);      // 90–210 range

                Canny(gray, edges, lowThresh, highThresh);

                // White background, dark edges
                Mat inverted;
                bitwise_not(edges, inverted);
                cvtColor(inverted, dst, COLOR_GRAY2RGBA);
                break;
            }

            case 3: { // 🟣 FINAL CARTOON FILTER — Fast + Real Transformation
                try {
                    Mat bgr, gray, edges, color;

                    // 1️⃣ Convert RGBA → BGR
                    cvtColor(src, bgr, COLOR_RGBA2BGR);

                    // 2️⃣ Edge mask
                    cvtColor(bgr, gray, COLOR_BGR2GRAY);
                    medianBlur(gray, gray, 5);
                    Laplacian(gray, edges, CV_8U, 5);
                    threshold(edges, edges, 100, 255, THRESH_BINARY_INV);

                    // 3️⃣ Fast color smoothing (pyramid method)
                    Mat small;
                    pyrDown(bgr, small);
                    pyrDown(small, small);
                    pyrUp(small, small);
                    pyrUp(small, small);

                    // 4️⃣ Color quantization (this gives the "cartoon" color blocks)
                    Mat quantized;
                    small.convertTo(quantized, CV_32F, 1.0 / 255.0);
                    quantized = quantized * 6.0;  // reduce color levels
                    quantized.convertTo(quantized, CV_8U, 255.0 / 6.0);

                    // 5️⃣ Boost saturation slightly for a punchy look
                    Mat hsv;
                    cvtColor(quantized, hsv, COLOR_BGR2HSV);
                    std::vector<Mat> channels;
                    split(hsv, channels);
                    channels[1] = channels[1] * 1.3; // increase saturation
                    merge(channels, hsv);
                    cvtColor(hsv, quantized, COLOR_HSV2BGR);

                    // 6️⃣ Combine smoothed color regions with edge mask
                    cvtColor(edges, edges, COLOR_GRAY2BGR);
                    bitwise_and(quantized, edges, color);

                    // 7️⃣ Convert back to RGBA for Android display
                    cvtColor(color, dst, COLOR_BGR2RGBA);
                }
                catch (...) {
                    src.copyTo(dst);
                }
                break;
            }

            case 4: { // BLUR
                int ksize = std::max(1, (int)(intensity / 10) * 2 + 1);
                GaussianBlur(src, dst, Size(ksize, ksize), 0);
                break;
            }

            default:
                src.copyTo(dst);
        }
    }

    AndroidBitmap_unlockPixels(env, bitmapIn);
    AndroidBitmap_unlockPixels(env, bitmapOut);
}