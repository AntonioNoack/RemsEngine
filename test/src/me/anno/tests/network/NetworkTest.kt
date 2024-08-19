package me.anno.tests.network

import me.anno.Engine
import me.anno.network.NetworkProtocol
import me.anno.network.Protocol
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.UDPClient
import me.anno.network.packets.PingPacket
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.concurrent.thread

class NetworkTest {
    @Test
    fun runTest() {

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
        server.start(4123, 4124)

        fun createClient(name: String): TCPClient {
            val address = InetAddress.getByName("localhost")
            val tcpSocket = TCPClient.createSocket(address, server.tcpPort, tcpProtocol)
            val tcpClient = TCPClient(tcpSocket, tcpProtocol, name)
            tcpClient.startClientSideAsync()
            thread(name = "$name.udp") {
                Thread.sleep(10)
                // when the connection is established
                val udpClient = UDPClient(address, server.udpPort)
                udpClient.send(null, tcpClient, udpProtocol, PingPacket())
                // receive the answer
                try {
                    udpClient.receive(null, tcpClient, PingPacket())
                    udpReceived.add(name)
                } catch (e: SocketTimeoutException) {
                    assertTrue(false, "client $name got no UDP answer")
                }
                udpClient.close()
            }
            tcpClient.sendTCP(PingPacket())
            return tcpClient
        }

        createClient("A")
        createClient("B")
        createClient("C")

        Thread.sleep(30)

        server.close()
        Engine.requestShutdown()

        assertEquals(setOf("A", "B", "C"), tcpReceived)
        assertEquals(setOf("A", "B", "C"), udpReceived)
    }
}