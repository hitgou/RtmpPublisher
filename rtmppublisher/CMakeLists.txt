# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.


add_library( # Sets the name of the library.
        opus-jni
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        src/main/cpp/include/opus.h
        src/main/cpp/include/opus_custom.h
        src/main/cpp/include/opus_defines.h
        src/main/cpp/include/opus_multistream.h
        src/main/cpp/include/opus_types.h
        src/main/cpp/opus-lib.cpp
        )

include_directories(src/main/cpp/librtmp)

add_definitions(-DNO_CRYPTO)

#动态方式加载 third是libxxxx.so的xxxx部分
add_library(opus SHARED IMPORTED)
SET(third_path ${CMAKE_CURRENT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libopus.so)
#设置要连接的so的相对路径，${ANDROID_ABI}表示so文件的ABI类型的路径,这一步引入了动态加入编译的so
set_target_properties(opus PROPERTIES IMPORTED_LOCATION ${third_path})
MESSAGE(STATUS “src third so path= ${third_path}”)

#配置加载native依赖
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/include)

find_library(log-lib log)

target_link_libraries(opus-jni opus ${log-lib})