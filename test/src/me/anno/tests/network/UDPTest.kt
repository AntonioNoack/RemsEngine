package me.anno.tests.network

import me.anno.network.NetworkProtocol
import me.anno.network.Packet
import me.anno.network.Protocol
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.packets.PingPacket
import me.anno.tests.network.NetworkTests.nextPort
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import kotlin.concurrent.thread

// https://stackoverflow.com/questions/1098897/what-is-the-largest-safe-udp-packet-size-on-the-internet
// val universalUDPPacketSizeLimit = 512

val limit = 256

fun startDatagramClientJavaNet(port: Int, message: String) {
    val socket = DatagramSocket()
    val address = InetAddress.getByName("localhost")
    val buffer = message.toByteArray()
    val packet = DatagramPacket(buffer, buffer.size, address, port)
    socket.send(packet)
    socket.receive(packet)
    val received = String(packet.data, packet.offset, packet.length)
    println("client/1 received $received")
    socket.close()
    assertEquals(message, received)
}

fun startDatagramServerJavaNet(port: Int) {
    val socket = DatagramSocket(port)
    val buffer = ByteArray(limit)
    val packet = DatagramPacket(buffer, buffer.size)
    while (true) {
        socket.receive(packet)
        val received = String(packet.data, packet.offset, packet.length)
        println("server/1 received ${received.length} bytes")
        socket.send(packet)
        if (received == "end") break
    }
    socket.close()
}

fun startDatagramClientJavaNIO(port: Int, message: String) {
    val socket = DatagramChannel.open()
    val target = InetSocketAddress(InetAddress.getByName("localhost"), port)
    // socket.bind(target)

    val buffer = message.toByteArray()
    val packet = ByteBuffer.wrap(buffer)
    socket.send(packet, target)

    val buffer2 = ByteArray(buffer.size)
    val packet2 = ByteBuffer.wrap(buffer2)
    val remote = assertNotNull(socket.receive(packet2))

    val received = String(buffer2, 0, packet2.position())
    println("client/2 received '$received' from $remote")
    socket.close()
    assertEquals(message, received)
}

fun startDatagramServerJavaNIO(port: Int) {
    val socket = DatagramChannel.open()
    socket.bind(InetSocketAddress(port))

    val array = ByteArray(limit)
    val buffer = ByteBuffer.wrap(array)
    while (true) {
        buffer.position(0).limit(buffer.capacity())

        val target = assertNotNull(socket.receive(buffer))
        val received = String(array, 0, buffer.position())
        println("server/2 received ${received.length} bytes, '$received'")
        buffer.flip()
        socket.send(buffer, target)
        if (received == "end") break
    }
    socket.close()
}

fun datagramTest() {
    val port = nextPort()
    thread { startDatagramServerJavaNet(port) }
    startDatagramClientJavaNet(port, "hi, I am a client")
    startDatagramClientJavaNet(port, "cool world")
    startDatagramClientJavaNet(port, "end")
}

fun datagramTestV2() {
    val port = nextPort()
    thread { startDatagramServerJavaNIO(port) }
    startDatagramClientJavaNIO(port, "hi, I am a client")
    startDatagramClientJavaNIO(port, "cool world")
    startDatagramClientJavaNIO(port, "end")
}

fun architectureUDPTest() {
    // udp test using our server-client architecture
    val tcpPort = nextPort()
    val udpPort = nextPort()
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
        // then send a UDP message
        val client = TCPClient(InetSocketAddress("localhost", tcpPort), tcpProtocol, name)
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
    Packet.debugPackets = true
    datagramTest()
    datagramTestV2()
    architectureUDPTest()
}