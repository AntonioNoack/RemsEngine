package me.anno.network

import me.anno.Engine
import me.anno.Time
import me.anno.io.Streams.readBE32
import me.anno.maths.Maths
import me.anno.utils.Color.hex32
import me.anno.utils.structures.lists.UnsafeArrayList
import org.apache.logging.log4j.LogManager
import java.io.*
import java.net.*
import javax.net.ssl.SSLServerSocketFactory
import kotlin.concurrent.thread
import kotlin.random.Random

// there are multiple client-server models:
// todo 1 simulating server, many clients
// todo we should support multiple players in a single client
// todo 1 data transfer server, many clients

// done support TCP and UDP packets

// to do there should be lobbies, where players are assigned to other servers, with seamless transfers...
// todo ssl encryption -> you need to generate and register certificates for that to work...
// to do be safe against malicious attacks
// done: e.g. players might block data transfer by not reading

/**
 * This is a server-client architecture for many use-cases.
 * A server can support both TCP as UDP, and multiple protocols, e.g., HTTP and game-related-protocols.
 * This multi-functionality is inspired by my Bukkit plugin Uniport Webserver, which did this for Minecraft servers.
 * [Project Page](https://dev.bukkit.org/projects/uniport-webserver)
 * */
open class Server : Closeable {

    var name = ""
    var motd = ""

    var timeoutMillis = 3000

    var tcpPort = -1
    var udpPort = -1

    var shutdown = false

    val nextRandomId = Random((Maths.random() * 1e16).toLong())

    val clients = UnsafeArrayList<TCPClient>(1024)

    val hashedClients = Array<ArrayList<TCPClient>>(512) { ArrayList() }

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
            for (index in clients.indices) {
                val client = clients[index]
                client.close()
            }
            clients.clear()
        }
        for (list in hashedClients) {
            synchronized(list) {
                for (c in list) c.close()
                list.clear()
            }
        }
    }

    /**
     * will throw IOException if TCP/UDP port is already in use
     * */
    fun start(tcpPort: Int, udpPort: Int, closeTCPIfUDPFails: Boolean = true) {
        if (tcpPort in 0..0xffff && protocols.any {
                val v = it.value.networkProtocol
                v == NetworkProtocol.TCP || v == NetworkProtocol.TCP_SSL
            }) {
            val socket = if (protocols.any { it.value.networkProtocol == NetworkProtocol.TCP_SSL })
                SSLServerSocketFactory.getDefault().createServerSocket(tcpPort) else ServerSocket(tcpPort)
            socket.soTimeout = timeoutMillis
            synchronized(this) {
                tcpSocket?.close()
                tcpSocket = socket
                this.tcpPort = tcpPort
            }
            thread(name = "Server-TCP") {
                runTCP(socket)
            }
        }
        if (udpPort in 0..0xffff && protocols.any { it.value.networkProtocol == NetworkProtocol.UDP }) {
            val socket = try {
                DatagramSocket(udpPort)
            } catch (e: Exception) {
                if (closeTCPIfUDPFails) {
                    this.tcpSocket?.close()
                    this.tcpSocket = null
                }
                throw e
            }
            synchronized(this) {
                udpSocket?.close()
                udpSocket = socket
                this.udpPort = udpPort
            }
            thread(name = "Server-UDP") {
                runUDP(socket)
            }
        }
    }

    open fun acceptsIP(address: InetAddress, port: Int): Boolean {
        // ip blacklist/whitelist
        return true
    }

    val run get() = !shutdown && !Engine.shutdown

    open fun createClient(clientSocket: Socket, protocol: Protocol, randomId: Int): TCPClient {
        return TCPClient(clientSocket, protocol, randomId)
    }

    var usedIds = HashSet<Int>()
    fun createRandomId(): Int {
        var id = 0
        synchronized(usedIds) {
            while (id == 0 || id in usedIds) {
                // is supposed to be non-deterministic,
                // so a foreign client cannot guess their randomId
                // this would ensure that it cannot send a udp packet in their name, if it is on the same inet address
                id = nextRandomId.nextInt() or Time.nanoTime.toInt()
            }
            usedIds.add(id)
        }
        return id
    }

    fun destroyRandomId(id: Int) {
        synchronized(usedIds) { usedIds.remove(id) }
    }

    open fun onClientConnected(client: TCPClient) {}

    open fun onClientDisconnected(client: TCPClient) {}

    var logRejections = true

    private fun runTCP(socket: ServerSocket) {
        try {
            while (run) {
                val clientSocket = try {
                    socket.accept()
                } catch (e: SocketTimeoutException) {
                    continue
                }
                if (acceptsIP(clientSocket.inetAddress, clientSocket.port)) {
                    thread(name = "Client ${clientSocket.inetAddress}:${clientSocket.port}") {
                        var tcpClient2: TCPClient? = null
                        try {
                            val input = clientSocket.getInputStream()
                            val magic = input.readBE32()
                            val protocol = protocols[magic]
                            if (protocol != null) {
                                val randomId = createRandomId()
                                try {
                                    val tcpClient = createClient(clientSocket, protocol, randomId)
                                    if (protocol.serverHandshake(this, tcpClient, magic)) {
                                        addClient(tcpClient)
                                        tcpClient2 = tcpClient
                                        tcpClient.isRunning = true
                                        onClientConnected(tcpClient)
                                        try {
                                            protocol.serverRun(this, tcpClient, magic)
                                        } finally {
                                            onClientDisconnected(tcpClient)
                                        }
                                    } else {
                                        if (logRejections) LOGGER.info("Handshake rejected")
                                        clientSocket.close()
                                    }
                                } finally {
                                    destroyRandomId(randomId)
                                }
                            } else {
                                LOGGER.info("Protocol ${str32(magic)} is unknown")
                                clientSocket.close()
                            }
                        } catch (e: EOFException) {
                            // unregister client
                            clientSocket.close()
                        } catch (e: IOException) {
                            // unregister client
                            if (run) e.printStackTrace()
                            clientSocket.close()
                        }
                        if (tcpClient2 != null) {
                            tcpClient2.isRunning = false
                            removeClient(tcpClient2)
                        }
                    }
                } else clientSocket.close()
            }
        } catch (e: SocketException) {
            if (run) e.printStackTrace()
        }
    }

    private fun runUDP(socket: DatagramSocket) {
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
                if (Packet.debugPackets) LOGGER.debug("Got udp packet of size ${udpPacket.length}, offset: ${udpPacket.offset}")
                if (udpPacket.length < 12) continue // protocol + packet + random id
                if (acceptsIP(udpPacket.address, udpPacket.port)) {
                    input.reset()
                    val protocolMagic = dis.readInt()
                    val packetMagic = dis.readInt()
                    val randomId = dis.readInt() // must match an existing TCP connection
                    if (Packet.debugPackets) LOGGER.debug(
                        "protocol: {}, packet: {}, randomId: {}",
                        str32(protocolMagic), str32(packetMagic), hex32(randomId)
                    )
                    val protocol = protocols[protocolMagic] ?: continue
                    val client = findTcpClient(udpPacket.address, randomId) ?: continue
                    val request = protocol.find(packetMagic) ?: continue
                    if (Packet.debugPackets) LOGGER.debug("Got udp packet from ${client.name}, ${client.randomIdString}")
                    when (request) {
                        is Packet -> synchronized(request) {
                            request.receiveUdp(this, client, dis) { response ->
                                dos.writeInt(response.bigEndianMagic)
                                response.send(this, client, dos)
                                dos.flush()
                                socket.send(udpPacket)
                            }
                        }
                        is ThreadLocal<*> -> {
                            (request.get() as Packet).receiveUdp(this, client, dis) { response ->
                                dos.writeInt(response.bigEndianMagic)
                                response.send(this, client, dos)
                                dos.flush()
                                socket.send(udpPacket)
                            }
                        }
                    }
                }
            }
        } catch (e: SocketException) {
            if (run) e.printStackTrace()
        }
    }

    fun hash(client: TCPClient): Int = client.randomId and (hashedClients.size - 1)
    fun hash(randomId: Int): Int = randomId and (hashedClients.size - 1)

    fun addClient(client: TCPClient) {
        val cl = hashedClients[hash(client)]
        synchronized(cl) { cl.add(client) }
        synchronized(clients) { clients.add(client) }
    }

    fun removeClient(client: TCPClient) {
        val cl = hashedClients[hash(client)]
        synchronized(cl) { cl.remove(client) }
        synchronized(clients) { clients.remove(client) }
    }

    private fun findTcpClient(address: InetAddress, randomId: Int): TCPClient? {
        val clients = hashedClients[hash(randomId)]
        synchronized(clients) {
            for (c in clients) {
                if (c.socket.inetAddress == address) {
                    return c
                }
            }
        }
        return null
    }

    inline fun forAllClients(run: (TCPClient) -> Unit) {
        val clients = clients
        var size = clients.size
        var index = 0
        while (index < size) {
            val client = clients.getSafe(index)
            if (client != null) {
                try {
                    run(client)
                } catch (e: IOException) {
                    removeClient(client)
                    client.close()
                    index--
                    size--
                }
            }
            index++
        }
    }

    inline fun forAllClientsSync(run: (TCPClient) -> Unit) {
        val clients = clients
        synchronized(clients) {
            forAllClients(run)
        }
    }

    fun broadcast(packet: Packet) {
        forAllClients {
            it.sendTCP(packet)
        }
    }

    fun broadcastExcept(packet: Packet, client: TCPClient) {
        forAllClients {
            if (it !== client) {
                it.sendTCP(packet)
            }
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(Server::class)
        private val allowedChars = BooleanArray(256)

        init {
            val ac = allowedChars
            for (c in 'A'.code..'Z'.code) ac[c] = true
            for (c in 'a'.code..'z'.code) ac[c] = true
            for (c in '0'.code..'9'.code) ac[c] = true
            for (c in ",.-+*/%&/()[]{}") ac[c.code] = true
        }

        private fun isAllowed(i: Int, c: Int): Boolean = allowedChars[i.shr(c).and(255)]

        fun str32(i: Int): String {
            return if (
                isAllowed(i, 24) &&
                isAllowed(i, 16) &&
                isAllowed(i, 8) &&
                isAllowed(i, 0)
            ) {
                charArrayOf(
                    ((i shr 24) and 255).toChar(),
                    ((i shr 16) and 255).toChar(),
                    ((i shr 8) and 255).toChar(),
                    (i and 255).toChar(),
                ).concatToString()
            } else hex32(i)
        }
    }
}