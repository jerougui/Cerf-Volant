cmake_minimum_required(VERSION 3.22.1)

project("kite_tracker")

# Oboe
set(OBOE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/oboe)
add_subdirectory(${OBOE_DIR} ./oboe)

# OpenCV (assuming we'll include prebuilt binaries via Gradle AAR, but we can also build from source if needed)
# For now, we'll use OpenCV via Java API; native modules rely on Oboe only

# Native audio synth module
add_library(audio_synth SHARED
        audio_synth.cpp
        audio_synth.h)

target_include_directories(audio_synth PRIVATE
        ${OBOE_DIR}/include)

target_link_libraries(audio_synth
        oboe
        log
        android)

# Native tracking utilities (optional: could host custom filters, kinematic math in C++)
add_library(tracking_utils SHARED
        tracking_utils.cpp
        tracking_utils.h)

target_link_libraries(tracking_utils
        log
        android)
