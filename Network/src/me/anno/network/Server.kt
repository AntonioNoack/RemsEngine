package me.anno.network

import me.anno.Engine
import me.anno.Time
import me.anno.io.Streams.readBE32
import me.anno.maths.Maths
import me.anno.network.SocketChannelInputStream.Companion.getInputStream
import me.anno.utils.Color.hex32
import me.anno.utils.Sleep
import me.anno.utils.Threads
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.lists.UnsafeArrayList
import org.apache.logging.log4j.LogManager
import speiger.primitivecollections.IntToObjectHashMap
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import kotlin.random.Random


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

    private val nextRandomId = Random((Maths.random() * 1e16).toLong())
    private val clients = UnsafeArrayList<TCPClient>(1024)
    private val hashedClients = createArrayList<ArrayList<TCPClient>>(512) { ArrayList() }
    private val protocols = IntToObjectHashMap<Protocol>()

    private var tcpSocket: ServerSocketChannel? = null
    private var udpSocket: DatagramChannel? = null
    val run get() = !shutdown && !Engine.shutdown

    fun stop() {
        shutdown = true
    }

    fun register(protocol: Protocol) {
        protocols[protocol.bigEndianMagic] = protocol
    }

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

    fun startTCP(tcpPort: Int) {
        val socket = ServerSocketChannel.open()
        socket.configureBlocking(false)
        socket.bind(InetSocketAddress(tcpPort))

        synchronized(this) {
            tcpSocket?.close()
            tcpSocket = socket
            this.tcpPort = tcpPort
        }
        Threads.runWorkerThread("Server-TCP") {
            runTCP(socket)
        }
    }

    fun startUDP(udpPort: Int, closeTCPIfUDPFails: Boolean) {
        val socket = try {
            val channel = DatagramChannel.open()
            channel.configureBlocking(false)
            channel.bind(InetSocketAddress(udpPort))
            channel
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
        Threads.runWorkerThread("Server-UDP") {
            runUDP(socket)
        }
    }

    /**
     * will throw IOException if TCP/UDP port is already in use
     * */
    fun start(tcpPort: Int, udpPort: Int, closeTCPIfUDPFails: Boolean = true) {
        if (tcpPort in 0..0xffff && protocols.any { _, value ->
                val protocol = value.networkProtocol
                protocol == NetworkProtocol.TCP || protocol == NetworkProtocol.TCP_SSL
            }) {
            startTCP(tcpPort)
        }
        if (udpPort in 0..0xffff && protocols.any { _, value ->
                value.networkProtocol == NetworkProtocol.UDP
            }) {
            startUDP(udpPort, closeTCPIfUDPFails)
        }
    }

    open fun acceptsIP(address: SocketAddress): Boolean {
        // ip blacklist/whitelist
        return true
    }

    open fun createClient(clientSocket: SocketChannel, protocol: Protocol, randomId: Int): TCPClient {
        return TCPClient(clientSocket, protocol, randomId, this)
    }

    private var usedIds = HashSet<Int>()
    private fun createRandomId(): Int {
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

    private fun destroyRandomId(id: Int) {
        synchronized(usedIds) { usedIds.remove(id) }
    }

    open fun onClientConnected(client: TCPClient) {}
    open fun onClientDisconnected(client: TCPClient) {}

    var logRejections = true

    private fun runTCP(socket: ServerSocketChannel) {
        try {
            while (run) {
                val clientSocket = socket.accept()
                if (clientSocket == null) {
                    Sleep.sleepShortly(true)
                    continue
                }

                if (acceptsIP(clientSocket.remoteAddress)) {
                    Threads.runTaskThread("Client ${clientSocket.remoteAddress}") {
                        var tcpClient2: TCPClient? = null
                        try {
                            val input = clientSocket.getInputStream(this, 4)
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
                        } catch (_: EOFException) {
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

    private fun runUDP(channel: DatagramChannel) {

        val array = ByteArray(NetworkProtocol.UDP.limit)
        val buffer = ByteBuffer.wrap(array)

        val input = ResetByteArrayInputStream(array)
        val dis = DataInputStream(input)
        val output = ResetByteArrayOutputStream(array)
        val dos = DataOutputStream(output)
        try {
            while (run) {
                // all malformed packets just are ignored
                buffer.position(0).limit(buffer.capacity())
                val senderAddress = channel.receive(buffer)
                if (senderAddress == null) {
                    Sleep.sleepShortly(true)
                    continue
                }

                val udpLength = buffer.position()
                val udpOffset = 0
                buffer.position(0).limit(udpLength)

                println("Got UDP client: $senderAddress, $udpLength @$udpOffset")

                if (Packet.debugPackets) LOGGER.debug("Got udp packet of size ${udpLength}, offset: $udpOffset")
                if (udpLength < 12) continue // protocol + packet + random id
                if (acceptsIP(senderAddress)) {
                    input.reset()
                    val protocolMagic = dis.readInt()
                    val packetMagic = dis.readInt()
                    val randomId = dis.readInt() // must match an existing TCP connection
                    if (Packet.debugPackets) LOGGER.debug(
                        "protocol: {}, packet: {}, randomId: {}",
                        str32(protocolMagic), str32(packetMagic), hex32(randomId)
                    )
                    val protocol = protocols[protocolMagic] ?: continue
                    val client = findTcpClient(senderAddress, randomId) ?: continue
                    val request = protocol.find(packetMagic) ?: continue
                    if (Packet.debugPackets) LOGGER.debug("Got udp packet from ${client.name}, ${client.randomIdString}")
                    when (request) {
                        is Packet -> synchronized(request) {
                            request.receiveUdp(this, client, dis) { response ->
                                dos.writeInt(response.bigEndianMagic)
                                response.send(this, client, dos)
                                dos.flush()

                                buffer.position(0).limit(output.size)
                                channel.send(buffer, senderAddress)
                            }
                        }
                        is ThreadLocal<*> -> {
                            (request.get() as Packet).receiveUdp(this, client, dis) { response ->
                                dos.writeInt(response.bigEndianMagic)
                                response.send(this, client, dos)
                                dos.flush()

                                buffer.position(0).limit(output.size)
                                channel.send(buffer, senderAddress)
                            }
                        }
                    }
                }
            }
        } catch (e: SocketException) {
            if (run) e.printStackTrace()
        }
    }

    private fun hash(client: TCPClient): Int = client.randomId and (hashedClients.size - 1)
    private fun hash(randomId: Int): Int = randomId and (hashedClients.size - 1)

    private fun addClient(client: TCPClient) {
        val cl = hashedClients[hash(client)]
        synchronized(cl) { cl.add(client) }
        synchronized(clients) { clients.add(client) }
    }

    fun removeClient(client: TCPClient) {
        val cl = hashedClients[hash(client)]
        synchronized(cl) { cl.remove(client) }
        synchronized(clients) { clients.remove(client) }
    }

    private fun findTcpClient(address: SocketAddress, randomId: Int): TCPClient? {
        val clients = hashedClients[hash(randomId)]
        synchronized(clients) {
            for (c in clients) {
                if (c.socket.remoteAddress == address) {
                    return c
                }
            }
        }
        return null
    }

    fun forAllClients(callback: (TCPClient) -> Unit) {
        val clients = clients
        var size = clients.size
        var index = 0
        while (index < size) {
            val client = clients.getSafe(index)
            if (client != null) {
                try {
                    callback(client)
                } catch (_: IOException) {
                    removeClient(client)
                    client.close()
                    index--
                    size--
                }
            }
            index++
        }
    }

    @Suppress("unused")
    fun forAllClientsSync(callback: (TCPClient) -> Unit) {
        val clients = clients
        synchronized(clients) {
            forAllClients(callback)
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