# Whisper, for transcribing voice notes on the phone: whisper.cpp built as a static library inside
# one small shared one, refuge_whisper, with the few calls refuge makes (see refuge_whisper.c).
cmake_minimum_required(VERSION 3.22)
project(refuge_whisper C CXX)

set(CMAKE_CXX_STANDARD 17)

# Only the library: no examples, tests or server; built for phones in general, not this machine.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

# 64-bit phones only (every phone of recent years): elsewhere there's no library, and refuge says
# that phone can't transcribe (Whisper.available).
if(NOT ANDROID_ABI STREQUAL "arm64-v8a")
    return()
endif()

add_subdirectory(whisper.cpp)

add_library(refuge_whisper SHARED refuge_whisper.c)
target_link_libraries(refuge_whisper whisper log)
# Small: only what's used, and nothing of whisper's own exported.
target_link_options(refuge_whisper PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)
