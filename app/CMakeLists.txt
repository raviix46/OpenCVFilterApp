cmake_minimum_required(VERSION 3.10.2)
project(OpenCVFilterApp)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17 -fexceptions -frtti")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -stdlib=libc++")

add_library(opencvfilterapp SHARED src/main/cpp/native-lib.cpp)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)
find_library(android-lib android)

set(OpenCV_DIR /Users/ravii../OpenCV-android-sdk/sdk/native/jni)
include_directories(${OpenCV_DIR}/include)

add_library(opencv_java4 SHARED IMPORTED)
set_target_properties(opencv_java4 PROPERTIES
        IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libopencv_java4.so)

target_link_libraries(opencvfilterapp
        ${log-lib}
        ${jnigraphics-lib}
        ${android-lib}
        opencv_java4
        c++_shared
)