cmake_minimum_required(VERSION 3.22.1)
project(driver_loader)

add_library(driver_loader SHARED driver_loader.cpp)

find_library(log-lib log)
target_link_libraries(driver_loader ${log-lib} android dl)

target_compile_options(driver_loader PRIVATE -Wall -O2)
