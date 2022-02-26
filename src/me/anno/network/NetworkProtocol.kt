package me.anno.network

// todo support different base protocols? idk...

enum class NetworkProtocol(val limit: Int) {
    TCP(Int.MAX_VALUE - 1024), // 1024 is a little space for headers
    UDP(512);
}