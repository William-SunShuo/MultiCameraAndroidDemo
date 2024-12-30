cmake_minimum_required(VERSION 3.22.1)
project("multiCamera")
set(CMAKE_CXX_STANDARD 14)

include_directories("${CMAKE_CURRENT_SOURCE_DIR}/include/srt")
include_directories("${CMAKE_CURRENT_SOURCE_DIR}/sdk/src/push")
include_directories("${CMAKE_CURRENT_SOURCE_DIR}/sdk/src/push/android")
include_directories("${CMAKE_CURRENT_SOURCE_DIR}/sdk/src/push/linux")
include_directories("${CMAKE_CURRENT_SOURCE_DIR}/sdk/src/server")
include_directories("${CMAKE_CURRENT_SOURCE_DIR}/sdk/src/server/android")
include_directories("${CMAKE_CURRENT_SOURCE_DIR}/sdk/src/server/linux")

file(GLOB_RECURSE push_src_files ${push_src_files} "${CMAKE_CURRENT_SOURCE_DIR}/sdk/src/push/*.cpp")
file(GLOB_RECURSE server_src_files ${server_src_files} "${CMAKE_CURRENT_SOURCE_DIR}/sdk/src/server/*.cpp")
add_library("push" SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        native-lib.cpp LogcatBuffer.cpp ${push_src_files})
add_library("server" SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        server-lib.cpp LogcatBuffer.cpp ${server_src_files})

add_library("mediacodec" SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        media-codec.cpp)
# 库列表
set(LIBRARY_NAMES srt)
# 设置共享库路径
set(LIBRARY_PATH "${CMAKE_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}")

# 自动导入所有库
foreach(LIBRARY_NAME ${LIBRARY_NAMES})
    add_library(${LIBRARY_NAME} SHARED IMPORTED)
    set_target_properties(${LIBRARY_NAME} PROPERTIES
            IMPORTED_LOCATION "${LIBRARY_PATH}/lib${LIBRARY_NAME}.so"
    )
endforeach()

target_link_libraries("push"
        PRIVATE
        # List libraries link to the target library
        android
        log
        mediandk
        ${LIBRARY_NAMES})

target_link_libraries("server"
        PRIVATE
        # List libraries link to the target library
        android
        log
        mediandk
        ${LIBRARY_NAMES})

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries("mediacodec"
        # List libraries link to the target library
        android
        mediandk#硬解码
        log)