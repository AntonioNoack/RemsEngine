package me.anno.tests.network

import me.anno.network.NetworkProtocol
import me.anno.network.Protocol
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.packets.PingPacket
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import kotlin.concurrent.thread

// https://stackoverflow.com/questions/1098897/what-is-the-largest-safe-udp-packet-size-on-the-internet
// val universalUDPPacketSizeLimit = 512

val port = 12345
val limit = 256

fun startDatagramClient(message: String) {
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

fun startDatagramServer() {
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

fun datagramTest() {
    thread { startDatagramServer() }
    startDatagramClient("hi, I am a client")
    startDatagramClient("cool world")
    startDatagramClient("end")
}

fun architectureUDPTest() {
    // udp test using our server-client architecture
    val tcpPort = 4651
    val udpPort = 4541
    val udpProtocol = Protocol("udp", NetworkProtocol.UDP)
    val tcpProtocol = Protocol("tcp", NetworkProtocol.TCP)
    val testServer = Server()
    tcpProtocol.pingDelayMillis = -1
    udpProtocol.register(PingPacket())
    testServer.register(udpProtocol)
    testServer.register(tcpProtocol)
    testServer.start(tcpPort, udpPort)
    fun startClient(name: String) {
        // start tcp client
        // then send a udp message
        val client = TCPClient(Socket("localhost", tcpPort), tcpProtocol, name)
        client.startClientSideAsync()
        client.udpPort = udpPort
        client.sendUDP(PingPacket(), udpProtocol) {
            println("got pong packet, dt: ${client.localTimeOffset / 1e6f} ms")
        }
        client.close()
    }
    Thread.sleep(100)
    startClient("A")
    startClient("B")
    startClient("C")
    Thread.sleep(100)
    testServer.close()
}

fun main() {
    datagramTest()
    architectureUDPTest()
}