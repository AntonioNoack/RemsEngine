package me.anno.network

// to do support different base protocols? idk...

enum class NetworkProtocol(val limit: Int) {
    /** not fully implemented yet... */
    TCP_SSL(Int.MAX_VALUE - 1024),
    TCP(Int.MAX_VALUE - 1024), // 1024 is a little space for headers
    UDP(512);
}