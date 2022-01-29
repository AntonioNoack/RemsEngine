package me.anno.network

import me.anno.Engine
import me.anno.utils.Color.hex32
import org.apache.logging.log4j.LogManager
import java.io.*
import java.net.*
import kotlin.concurrent.thread
import kotlin.random.Random

// todo there are multiple client-server models:
// todo 1 simulating server, many clients
// todo we should support multiple players in a single client
// todo 1 data transfer server, many clients

// done support TCP and UDP packets

// todo there should be lobbies, where players are assigned to other servers, with seamless transfers...

/**
 * This is a server-client architecture for many use-cases.
 * A server can support both TCP as UDP, and multiple protocols, e.g. HTTP and game-related-protocols.
 * This multi-functionality is inspired by my Bukkit plugin Uniport Webserver, which did this for Minecraft servers.
 * @see <a href="https://dev.bukkit.org/projects/uniport-webserver">Uniport WebServer</a>
 * */
open class Server : Closeable {

    var name = ""
    var motd = ""

    var tcpPort = -1
    var udpPort = -1

    var shutdown = false

    val nextRandomId = Random(System.nanoTime() shl 32)

    val clients = HashSet<TCPClient>(1024)

    val clients2 = Array<ArrayList<TCPClient>>(512) { ArrayList() }

    val protocols = HashMap<Int, Protocol>()

    fun register(protocol: Protocol) {
        protocols[protocol.bigEndianMagic] = protocol
    }

    var tcpSocket: ServerSocket? = null
    var udpSocket: DatagramSocket? = null

    override fun close() {
        synchronized(this) {
            shutdown = true
            tcpSocket?.close()
            tcpSocket = null
            udpSocket?.close()
            udpSocket = null
        }
        synchronized(clients) {
            for (client in clients) {
                client.close()
            }
            clients.clear()
        }
        for (list in clients2) {
            synchronized(list) {
                for (c in list) c.close()
                list.clear()
            }
        }
    }

    fun start(tcpPort: Int, udpPort: Int) {
        if (protocols.any { it.value.networkProtocol == NetworkProtocol.TCP }) {
            this.tcpPort = tcpPort
            thread(name = "Server-TCP") {
                runTCP(tcpPort)
            }
        }
        if (protocols.any { it.value.networkProtocol == NetworkProtocol.UDP }) {
            this.udpPort = udpPort
            thread(name = "Server-UDP") {
                runUDP(udpPort)
            }
        }
    }

    open fun acceptsIP(address: InetAddress, port: Int): Boolean {
        // ip blacklist/whitelist
        return true
    }

    val run get() = !shutdown && !Engine.shutdown

    private fun runTCP(port: Int) {
        ServerSocket(port).use { socket ->
            synchronized(this) {
                tcpSocket?.close()
                tcpSocket = socket
            }
            try {
                while (run) {
                    val clientSocket = socket.accept()
                    if (acceptsIP(clientSocket.inetAddress, clientSocket.port)) {
                        val randomId = nextRandomId.nextInt()
                        thread(name = "Client ${clientSocket.inetAddress}:${clientSocket.port}") {
                            var tcpClient2: TCPClient? = null
                            try {
                                val input = clientSocket.getInputStream()
                                val magic = input.readBE32()
                                val protocol = protocols[magic]
                                if (protocol != null) {
                                    val tcpClient = TCPClient(clientSocket, randomId)
                                    if (protocol.serverHandshake(this, tcpClient, magic)) {
                                        addClient(tcpClient)
                                        tcpClient2 = tcpClient
                                        protocol.serverRun(this, tcpClient, magic)
                                    } else clientSocket.close()
                                } else clientSocket.close()
                            } catch (e: IOException) {
                                // unregister client
                                if (run) e.printStackTrace()
                                clientSocket.close()
                            }
                            if (tcpClient2 != null) {
                                removeClient(tcpClient2)
                            }
                        }
                    } else clientSocket.close()
                }
            } catch (e: SocketException) {
                if (run) e.printStackTrace()
            }
        }
    }

    private fun runUDP(port: Int) {
        DatagramSocket(port).use { socket ->
            synchronized(this) {
                udpSocket?.close()
                udpSocket = socket
            }
            val buffer = ByteArray(NetworkProtocol.UDP.limit)
            val udpPacket = DatagramPacket(buffer, buffer.size)
            val input = ResetByteArrayInputStream(buffer)
            val dis = DataInputStream(input)
            val output = ResetByteArrayOutputStream(buffer)
            val dos = DataOutputStream(output)
            try {
                while (run) {
                    // all malformed packets just are ignored
                    socket.receive(udpPacket)
                    LOGGER.debug("got udp packet of size ${udpPacket.length}, offset: ${udpPacket.offset}")
                    if (udpPacket.length < 12) continue // protocol + packet + random id
                    if (acceptsIP(udpPacket.address, udpPacket.port)) {
                        input.reset()
                        val protocolMagic = dis.readInt()
                        val packetMagic = dis.readInt()
                        val randomId = dis.readInt() // must match an existing TCP connection
                        LOGGER.debug(
                            "protocol: ${str32(protocolMagic)}, " +
                                    "packet: ${str32(packetMagic)}, " +
                                    "randomId: ${hex32(randomId)}"
                        )
                        val protocol = protocols[protocolMagic] ?: continue
                        val request = protocol.packets[packetMagic] ?: continue
                        val client = findTcpClient(udpPacket.address, randomId) ?: continue
                        LOGGER.debug("got udp packet from ${client.name}")
                        request.udpReceive(this, client, dis) { response ->
                            dos.writeInt(response.bigEndianMagic)
                            response.send(this, client, dos)
                            dos.flush()
                            socket.send(udpPacket)
                        }
                    }
                }
            } catch (e: SocketException) {
                if (run) e.printStackTrace()
            }
        }
    }

    fun addClient(client: TCPClient) {
        val cl = clients2[client.randomId and (clients2.size - 1)]
        synchronized(cl) { cl.add(client) }
        synchronized(clients) { clients.add(client) }
    }

    fun removeClient(client: TCPClient) {
        val cl = clients2[client.randomId and (clients2.size - 1)]
        synchronized(cl) { cl.remove(client) }
        synchronized(clients) { clients.remove(client) }
    }

    private fun InputStream.readBE32(): Int {
        return read().shl(24) + read().shl(16) + read().shl(8) + read()
    }

    private fun findTcpClient(address: InetAddress, randomId: Int): TCPClient? {
        val clients = clients2[randomId and (clients2.size - 1)]
        synchronized(clients) {
            for (c in clients) {
                if (c.socket.inetAddress == address) {
                    return c
                }
            }
        }
        return null
    }

    fun broadcast(packet: Packet) {
        synchronized(clients) {
            for (client in clients) {
                client.send(this, packet)
            }
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(Server::class)

        fun str32(i: Int): String {
            return String(
                charArrayOf(
                    i.shr(24).and(255).toChar(),
                    i.shr(16).and(255).toChar(),
                    i.shr(8).and(255).toChar(),
                    i.and(255).toChar(),
                )
            )
        }
    }

}