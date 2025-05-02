package me.anno.network

import me.anno.Time
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.Sleep.waitUntilOrThrow
import org.apache.logging.log4j.LogManager
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLSocketFactory
import kotlin.concurrent.thread

open class TCPClient(val socket: Socket, val protocol: Protocol, var randomId: Int) : Closeable {

    companion object {
        private val LOGGER = LogManager.getLogger(TCPClient::class)
        fun createSocket(address: InetAddress, port: Int, protocol: Protocol): Socket {
            return if (protocol.networkProtocol == NetworkProtocol.TCP_SSL) {
                SSLSocketFactory.getDefault().createSocket(address, port)
            } else {
                Socket(address, port)
            }
        }
    }

    constructor(address: InetAddress, port: Int, protocol: Protocol, name: String, uuid: String = name) :
            this(createSocket(address, port, protocol), protocol, 0) {
        this.name = name
        this.uuid = uuid
    }

    constructor(socket: Socket, protocol: Protocol, name: String, uuid: String = name) :
            this(socket, protocol, 0) {
        this.name = name
        this.uuid = uuid
    }

    var name = ""
    var uuid = ""
    var version: Int = 0

    var serverName = ""
    var serverMotd = ""

    var closingReason = ""

    val dis = DataInputStream(socket.getInputStream())
    val dos = DataOutputStream(socket.getOutputStream())

    val buffer = ResetByteArrayInputStream(32)
    val bufferDis = DataInputStream(buffer)

    var maxPacketSize = 1 shl 20

    var udpPort = -1
    var isClosed2 = false

    val writingQueue = LinkedBlockingQueue<Packet>()

    /**
     * client time minus own system time,
     * so how many nanoseconds, the clock of the client is ahead
     * */
    var localTimeOffset = 0L

    val isClosed get() = socket.isClosed || !socket.isConnected || !socket.isBound || isClosed2
    val randomIdString get() = randomId.toUInt().toString(16)
    var isRunning = false

    fun ensureConnection(timeoutMillis: Long) {
        waitUntilOrThrow(true, timeoutMillis * MILLIS_TO_NANOS, this) { isRunning || isClosed }
        if (isClosed) throw SocketException("Connection has been closed")
    }

    var packetLimit = 128

    fun sendTCP(packet: Packet, maxWaitTimeMillis: Long = 0L) {
        if (protocol.find(packet.bigEndianMagic) == null) throw UnregisteredPacketException(packet)
        if (!isClosed) {
            when {
                writingQueue.size < packetLimit -> {
                    writingQueue.add(packet)
                }
                !packet.canDropPacket -> {
                    // find slot in packets that we can take
                    if (writingQueue.removeAll { it.canDropPacket }) {
                        writingQueue.add(packet)
                    } else {
                        val t0 = Time.nanoTime
                        val tMax = t0 + maxWaitTimeMillis * MILLIS_TO_NANOS
                        while (true) {
                            if (writingQueue.size < packetLimit) {
                                writingQueue.add(packet)
                                return
                            }
                            if (Time.nanoTime > tMax) {
                                // all packets are important... this is awkward
                                // should not happen with a well-designed protocol,
                                // or only if the client is malicious and not reading packets
                                close()
                                throw SocketException("There were too many waiting, important packets")
                            } else Thread.sleep(1)
                        }
                    }
                }
                else -> LOGGER.info("Dropped packet: $packet")
            }
        }
    }

    fun workPacketTasks(server: Server?) {
        try {
            ensureConnection(0)
            var hasData = false
            while (!isClosed) {
                val packet = writingQueue.poll()
                if (packet == null) {
                    // flush automatically :)
                    if (hasData) {
                        synchronized(dos) { dos.flush() }
                        hasData = false
                    }
                    Thread.sleep(5)
                } else {
                    synchronized(dos) {
                        dos.writeInt(packet.bigEndianMagic)
                        packet.send(server, this, dos)
                        hasData = true
                    }
                }
            }
        } catch (e: SocketException) {
            if (debug) e.printStackTrace()
        } catch (e: IOException) {
            if (debug) e.printStackTrace()
        } finally {
            close()
        }
    }

    var debug = false

    fun flush() {
        try {
            ensureConnection(1)
        } catch (_: TimeoutException) {
        }
        synchronized(dos) {
            dos.flush()
        }
    }

    @Suppress("unused")
    fun sendUDP(packet: Packet, protocol: Protocol, receiveAnswer: Boolean): Packet? {
        val server = null
        var answer: Packet? = null
        if (receiveAnswer) {
            sendUDP(server, protocol, packet) {
                answer = it
            }
        } else sendUDP(server, protocol, packet, null)
        return answer
    }

    fun sendUDP(packet: Packet, protocol: Protocol, onReceive: ((Packet) -> Unit)?) {
        sendUDP(null, protocol, packet, onReceive)
    }

    fun sendUDP(server: Server?, protocol: Protocol, packet: Packet, onReceive: ((Packet) -> Unit)?): Boolean {
        if (server != null) throw SocketException("You need to send a response to a client directly after their request")
        return try {
            ensureConnection(1) // udp is lossy, so it shouldn't hurt too much xD
            val sender = UDPSender(protocol, this)
            sender.send(packet, onReceive)
            sender.close()
            true
        } catch (_: TimeoutException) {
            false
        }
    }

    fun startClientSide(shutdown: () -> Boolean = { false }) {
        if (synchronized(dos) {
                val socket = socket
                dos.writeInt(protocol.bigEndianMagic)
                protocol.clientHandshake(socket, this)
            }) {
            isRunning = true
            protocol.clientRun(this, shutdown)
        } else {
            close()
            throw SocketException("Handshake failed")
        }
    }

    fun startClientSideAsync(shutdown: () -> Boolean = { false }) {
        thread(name = "$name.tcp") {
            try {
                startClientSide(shutdown)
            } catch (e: SocketException) {
                // connection closed
                e.printStackTrace()
            } finally {
                close()
            }
        }
    }

    override fun close() {
        isClosed2 = true
        isRunning = false
        socket.close()
        dis.close()
        dos.close()
    }
}