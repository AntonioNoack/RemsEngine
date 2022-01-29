package me.anno.network

enum class NetworkProtocol(val limit: Int) {
    TCP(Int.MAX_VALUE - 1024), // 1024 is a little space for headers
    UDP(512)
}