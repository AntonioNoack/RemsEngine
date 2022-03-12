package me.anno.network

import me.anno.utils.Sleep.waitUntil
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.net.SocketException
import kotlin.concurrent.thread

open class TCPClient(val socket: Socket, var randomId: Int) : Closeable {

    constructor(socket: Socket, name: String) : this(socket, 0) {
        this.name = name
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

    /**
     * client time minus own system time,
     * so how many nano seconds, the clock of the client is ahead
     * */
    var localTimeOffset = 0L

    val isClosed get() = socket.isClosed || !socket.isConnected || !socket.isBound
    var isRunning = false

    fun sendTCP(packet: Packet) {
        sendTCP(null, packet)
    }

    fun sendTCP(server: Server?, packet: Packet) {
        waitUntil(true) { isRunning || isClosed }
        if (isClosed) throw SocketException("Connection has been closed")
        synchronized(dos) {
            dos.writeInt(packet.bigEndianMagic)
            packet.send(server, this, dos)
        }
    }

    fun sendUDP(packet: Packet, protocol: Protocol, receive: Boolean) {
        sendUDP(null, protocol, packet, receive)
    }

    fun sendUDP(server: Server?, protocol: Protocol, packet: Packet, receive: Boolean) {
        if (receive) sendUDP(server, protocol, packet) {}
        else sendUDP(server, protocol, packet, null)
    }

    fun sendUDP(packet: Packet, protocol: Protocol, onReceive: ((Packet) -> Unit)?) {
        sendUDP(null, protocol, packet, onReceive)
    }

    fun sendUDP(server: Server?, protocol: Protocol, packet: Packet, onReceive: ((Packet) -> Unit)?) {
        if (server != null) throw SocketException("You need to send a response to a client directly after their request")
        waitUntil(true) { isRunning || isClosed }
        if (isClosed) throw SocketException("Connection has been closed")
        val sender = UDPSender(protocol, this)
        sender.send(packet, onReceive)
        sender.close()
    }

    fun startClientSide(protocol: Protocol, shutdown: () -> Boolean = { false }) {
        if (synchronized(dos) {
                val socket = socket
                dos.writeInt(protocol.bigEndianMagic)
                protocol.clientHandshake(socket, this)
            }) {
            isRunning = true
            protocol.clientRun(socket, this, shutdown)
        } else {
            close()
            throw SocketException("Handshake failed")
        }
    }

    fun startClientSideAsync(protocol: Protocol, shutdown: () -> Boolean = { false }) {
        thread(name = "$name.tcp") {
            try {
                startClientSide(protocol, shutdown)
            } catch (e: SocketException) {
                // connection closed
                e.printStackTrace()
            }
        }
    }

    override fun close() {
        dis.close()
        dos.close()
        socket.close()
    }

}