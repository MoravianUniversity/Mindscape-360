cmake_minimum_required(VERSION 3.4.1)

project("cardboard.kt" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED True)

# Standard Android dependencies (don't seem to be necessary)
#find_library(ANDROID_LIB android)
#find_library(GLESv2_LIB GLESv2)
#find_library(GLESv3_LIB GLESv3)
#find_library(LOG_LIB log)

# Path to the Cardboard SDK.
set(cardboard_sdk_dir ../../../cardboard/sdk)

# Include the Cardboard SDK CMakeLists.txt.
set(CARDBOARDSDK_UNITY_PLUGIN OFF)
set(CARDBOARDSDK_RENDERING_VULKAN OFF)
add_subdirectory(${cardboard_sdk_dir} ${CMAKE_CURRENT_BINARY_DIR}/cardboard_sdk EXCLUDE_FROM_ALL)
set_property(  # Since we turn off the Unity and Vulkan options, we need to have it ignore some warnings.
        TARGET GfxPluginCardboard
        APPEND_STRING
        PROPERTY
        LINK_FLAGS
        " -Wl,--undefined-version"
)

# Add the Cardboard sources.
add_library(cardboard SHARED
        cpp/cardboard.cpp
        cpp/distortion_renderer.cpp
        cpp/lens_distortion.cpp
        cpp/head_tracker.cpp
        cpp/qr_code.cpp
)
target_include_directories(cardboard PRIVATE "${cardboard_sdk_dir}/include")

# Link the Cardboard library with the GfxPluginCardboard target.
target_link_libraries(
        cardboard
#        ${ANDROID_LIB}
#        ${GLESv2_LIB}
#        ${GLESv3_LIB}
#        ${LOG_LIB}
        GfxPluginCardboard
)
