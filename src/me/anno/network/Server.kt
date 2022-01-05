package me.anno.network

import me.anno.Engine
import java.io.DataInputStream
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

// todo there are multiple client-server models:
// todo 1 simulating server, many clients
// todo we should support multiple players in a single client
// todo 1 data transfer server, many clients

// todo support TCP and UDP packets

// todo there should be lobbies, where players are assigned to other servers, with seamless transfers...

open class Server {

    var shutdown = false

    val clients = ArrayList<Client>()

    val protocols = HashMap<Int, Protocol>()

    fun start(tcpPort: Int, udpPort: Int) {
        if (protocols.any { it.value.tcpById.isNotEmpty() }) thread(name = "Server-TCP") {
            runTCP(tcpPort)
        }
        if (protocols.any { it.value.udpById.isNotEmpty() }) thread(name = "Server-UDP") {
            runUDP(udpPort)
        }
    }

    open fun acceptsIP(address: InetAddress, port: Int): Boolean {
        // ip blacklist/whitelist
        return true
    }

    // todo handle authentication
    // todo additionally handle web server?
    // todo magic <3

    val run get() = !shutdown && !Engine.shutdown

    private fun runTCP(port: Int) {
        ServerSocket(port).use { socket ->
            while (run) {
                val client = socket.accept()
                if (acceptsIP(client.inetAddress, client.port)) {
                    thread {
                        val input = client.getInputStream()
                        val magic = input.readBE32()
                        val protocol = protocols[magic]
                        if (protocol != null) {
                            val client2 = Client(client.inetAddress, client.port, 0)
                            if (protocol.serverHandshake(client2)) {
                                protocol.serverRun(client2)
                            } else client.close()
                        } else client.close()
                    }
                } else client.close()
            }
        }
    }

    private fun InputStream.readBE32(): Int {
        return read().shl(24) + read().shl(16) + read().shl(8) + read()
    }

    private fun runUDP(port: Int) {
        DatagramSocket(port).use { socket ->
            val buffer = ByteArray(NetworkProtocol.UDP.limit)
            val packet = DatagramPacket(buffer, buffer.size)
            val input = ResetByteArrayInputStream(buffer)
            val dis = DataInputStream(input)
            while (run) {
                socket.receive(packet)
                if (acceptsIP(packet.address, packet.port)) {
                    input.reset()
                    val magic = dis.readInt()
                    val protocol = protocols[magic]
                    if (protocol != null) {
                        // todo identify client by his ip address

                    } // else no response (?)
                }
            }
        }
    }

}