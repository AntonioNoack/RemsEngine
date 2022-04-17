package me.anno.network.test

import me.anno.Engine
import me.anno.network.*
import me.anno.network.packets.POS0Packet
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
    tcpProtocol.register(PingPacket())
    udpProtocol.register(PingPacket())
    tcpProtocol.register(POS0Packet())
    udpProtocol.register(POS0Packet())

    // then create server & clients
    val server = Server()
    server.register(tcpProtocol)
    server.register(udpProtocol)
    server.start(4123, 4124)

    fun createClient(name: String): TCPClient {
        val address = InetAddress.getByName("localhost")
        val tcpSocket = TCPClient.createSocket(address, server.tcpPort, tcpProtocol)
        val client = TCPClient(tcpSocket, tcpProtocol, name)
        client.startClientSideAsync()
        thread(name = "$name.udp") {
            Thread.sleep(100)
            // when the connection is established
            val client1 = UDPClient(address, server.udpPort)
            client1.send(null, client, udpProtocol, PingPacket())
            println("sent udp packet")
            // receive the answer
            try {
                client1.receive(null, client, PingPacket())
                println("client got answer")
            } catch (e: SocketTimeoutException) {
                println("client $name got no answer")
            }
            client1.close()
        }
        return client
    }

    createClient("A")
    createClient("B")
    createClient("C")

    // 500ms
    for (i in 0 until 50)
        Sleep.sleepABit10(false)

    println("closing server")

    server.close()

    Engine.requestShutdown()

}