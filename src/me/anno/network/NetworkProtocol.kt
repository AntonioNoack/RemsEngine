package me.anno.network

enum class NetworkProtocol(val limit: Int) {
    TCP(Int.MAX_VALUE),
    UDP(512)
}