cmake_minimum_required(VERSION 3.22.1)

project("smartvoicechanger_native" C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(
    smartvoicechanger_native
    SHARED
    native-lib.cpp
    gpu_diagnostic_probe.cpp
    dsp/audio_dsp_engine.cpp
    dsp/kissfft/kiss_fft.c
    dsp/kissfft/kiss_fftr.c
)

target_include_directories(
    smartvoicechanger_native
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/dsp
    ${CMAKE_CURRENT_SOURCE_DIR}/dsp/kissfft
)

find_library(log-lib log)

target_link_libraries(
    smartvoicechanger_native
    ${log-lib}
    dl
)
