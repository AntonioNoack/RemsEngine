package me.anno.tests.network

import me.anno.network.NetworkProtocol
import me.anno.network.Protocol
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.UDPClient
import me.anno.network.packets.PingPacket
import me.anno.tests.FlakyTest
import me.anno.tests.LOGGER
import me.anno.tests.network.NetworkTests.nextPort
import me.anno.utils.Sleep
import me.anno.utils.Threads
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertLessThan
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class NetworkTest {

    @Test
    @FlakyTest
    fun runParallelServers() {
        val bad = AtomicInteger(0)
        val threads = (0 until 100).map {
            thread {
                try {
                    runSerialServer(false)
                } catch (e: Exception) {
                    e.printStackTrace()
                    bad.incrementAndGet()
                }
            }
        }
        threads.forEach { it.join() }
        assertLessThan(bad.get(), 25)
        println("Failed servers: ${bad.get()}/${threads.size}")
    }

    @Test
    @FlakyTest
    fun runSerialServer() {
        runSerialServer(false)
    }

    fun runSerialServer(checkUdp: Boolean) {

        val allReachTheEnd = AtomicInteger(0)

        val tcpReceived = ConcurrentSkipListSet<String>()
        val udpReceived = ConcurrentSkipListSet<String>()

        // first specify the protocol
        val tcpProtocol = Protocol("TCP ", NetworkProtocol.TCP)
        val udpProtocol = Protocol("UDP ", NetworkProtocol.UDP)

        // then register packets
        tcpProtocol.register(object : PingPacket() {
            override fun onReceive(server: Server?, client: TCPClient) {
                super.onReceive(server, client)
                if (server == null) {
                    // a few extra packets may be received, because Ping packets are also used as keep-alive
                    tcpReceived.add(client.name)
                }
            }
        })
        udpProtocol.register(PingPacket())

        // then create server & clients
        val server = Server()
        server.register(tcpProtocol)
        server.register(udpProtocol)
        server.start(nextPort(), nextPort())

        val missingUdpAnswers = AtomicInteger(0)

        fun createClient(name: String): TCPClient {
            allReachTheEnd.incrementAndGet()
            val address = InetAddress.getByName("localhost")
            val tcpSocket = TCPClient.createSocket(address, server.tcpPort, tcpProtocol)
            val tcpClient = TCPClient(tcpSocket, tcpProtocol, name)
            tcpClient.startClientSideAsync()
            Threads.start("$name.udp") {
                // when the connection is established
                val udpClient = UDPClient(address, server.udpPort)
                udpClient.send(null, tcpClient, udpProtocol, PingPacket())
                // receive the answer
                try {
                    udpClient.receive(null, tcpClient, PingPacket())
                    udpReceived.add(name)
                } catch (_: SocketTimeoutException) {
                    // assertTrue(false, "client $name got no UDP answer")
                    //  println("client $name got no UDP answer")
                    missingUdpAnswers.incrementAndGet()
                }
                udpClient.close()
                allReachTheEnd.decrementAndGet()
            }
            tcpClient.sendTCP(PingPacket())
            return tcpClient
        }

        createClient("A")
        createClient("B")
        createClient("C")

        var i = 0
        while (tcpReceived.size < 3 && i++ < 200) {
            Thread.sleep(5)
        }
        Thread.sleep(50)

        server.close()

        if (missingUdpAnswers.get() > 0) {
            LOGGER.warn("Missed ${missingUdpAnswers.get()} udp answers")
        }
        assertEquals(setOf("A", "B", "C"), tcpReceived)
        if (checkUdp) assertTrue(udpReceived.isNotEmpty()) // UDP is getting lossy when the network is saturated
        // assertEquals(setOf("A", "B", "C"), udpReceived)
        Sleep.waitUntil(false) { allReachTheEnd.get() == 0 }
    }
}