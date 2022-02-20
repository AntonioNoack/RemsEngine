package me.anno.network.test

import me.anno.network.*
import me.anno.network.packets.POS0Packet
import me.anno.network.packets.PingPacket
import me.anno.utils.Sleep
import java.net.*
import kotlin.concurrent.thread

fun main() {

    // first specify the protocol
    val tcpProtocol = Protocol("TCP ", NetworkProtocol.TCP)
    val udpProtocol = Protocol("UDP ", NetworkProtocol.UDP)

    // then register packets
    tcpProtocol.register(PingPacket())
    udpProtocol.register(POS0Packet())

    // then create server & clients
    val server = Server()
    server.register(tcpProtocol)
    server.register(udpProtocol)
    server.start(4123, 4124)

    fun createClient(name: String): TCPClient {
        val address = InetAddress.getByName("localhost")
        val tcpSocket = Socket(address, server.tcpPort)
        val client = TCPClient(tcpSocket, 0)
        thread(name = "$name.tcp") {
            try {
                client.name = name
                val tcpDos = client.dos
                tcpDos.writeInt(tcpProtocol.bigEndianMagic)
                tcpProtocol.clientHandshake(tcpSocket, client)
                tcpProtocol.clientRun(tcpSocket, client) { false }
            } catch (e: SocketException) {
                // connection closed
            }
        }
        thread(name = "$name.udp") {
            Thread.sleep(100)
            // when the connection is established
            val client1 = UDPClient(address, server.udpPort)
            client1.send(null, client, udpProtocol, POS0Packet())
            println("sent udp packet")
            // receive the answer
            try {
                client1.receive(null, client, POS0Packet())
                println("client got answer")
            } catch (e: SocketTimeoutException){
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

}