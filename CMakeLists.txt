cmake_minimum_required(VERSION 3.16)

project(parkingmate_blockchain
        VERSION 1.0
        LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Source files
add_executable(blockchain
        src/main.cpp
        src/blockchain.cpp
)

# Include directory (for blockchain.h)
target_include_directories(blockchain
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/src
)

