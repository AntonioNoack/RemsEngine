package me.anno.network.test

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

// https://stackoverflow.com/questions/1098897/what-is-the-largest-safe-udp-packet-size-on-the-internet
// val universalUDPPacketSizeLimit = 512

val port = 12345
val limit = 256

fun startClient(message: String) {
    val socket = DatagramSocket()
    val address = InetAddress.getByName("localhost")
    val buffer = message.toByteArray()
    val packet = DatagramPacket(buffer, buffer.size, address, port)
    socket.send(packet)
    socket.receive(packet)
    val received = String(packet.data, packet.offset, packet.length)
    println("client received $received")
    socket.close()
}

fun startServer() {
    val socket = DatagramSocket(port)
    val buffer = ByteArray(limit)
    val packet = DatagramPacket(buffer, buffer.size)
    while (true) {
        socket.receive(packet)
        val received = String(packet.data, packet.offset, packet.length)
        println("server received ${received.length} bytes")
        socket.send(packet)
        if (received == "end") break
    }
    socket.close()
}

fun main() {
    thread { startServer() }
    startClient("hi, I am a client")
    startClient("cool world")
    startClient("end")
}