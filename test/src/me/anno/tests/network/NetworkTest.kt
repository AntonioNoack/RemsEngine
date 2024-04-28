package me.anno.tests.network

import me.anno.Engine
import me.anno.network.NetworkProtocol
import me.anno.network.Protocol
import me.anno.network.Server
import me.anno.network.TCPClient
import me.anno.network.UDPClient
import me.anno.network.packets.PingPacket
import me.anno.utils.Sleep
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

fun main() {

    // first specify the protocol
    val tcpProtocol = Protocol("TCP ", NetworkProtocol.TCP)
    val udpProtocol = Protocol("UDP ", NetworkProtocol.UDP)

    // then register packets
    tcpProtocol.register(object : PingPacket() {
        override fun onReceive(server: Server?, client: TCPClient) {
            super.onReceive(server, client)
            if (server == null) { // a few extra packets may be received, because Ping packets are also used as keep-alive
                println("client ${client.name} got TCP answer")
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
            Thread.sleep(100)
            // when the connection is established
            val udpClient = UDPClient(address, server.udpPort)
            udpClient.send(null, tcpClient, udpProtocol, PingPacket())
            // receive the answer
            try {
                udpClient.receive(null, tcpClient, PingPacket())
                println("client $name got UDP answer")
            } catch (e: SocketTimeoutException) {
                println("client $name got no UDP answer")
            }
            udpClient.close()
        }
        tcpClient.sendTCP(PingPacket())
        return tcpClient
    }

    createClient("A")
    createClient("B")
    createClient("C")

    // 500ms
    for (i in 0 until 500)
        Sleep.sleepABit(false)

    println("closing server")

    server.close()

    Engine.requestShutdown()
}