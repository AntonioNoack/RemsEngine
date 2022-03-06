package me.anno.network

import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.net.SocketException
import kotlin.concurrent.thread

class TCPClient(val socket: Socket, var randomId: Int) : Closeable {

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

    /**
     * client time minus own system time,
     * so how many nano seconds, the clock of the client is ahead
     * */
    var localTimeOffset = 0L

    val isClosed get() = socket.isClosed || !socket.isConnected || !socket.isBound
    var isRunning = false

    fun send(server: Server?, packet: Packet) {
        synchronized(dos) {
            dos.writeInt(packet.bigEndianMagic)
            packet.send(server, this, dos)
        }
    }

    fun start(protocol: Protocol, shutdown: () -> Boolean = { false }) {
        if (synchronized(dos) {
                val socket = socket
                dos.writeInt(protocol.bigEndianMagic)
                protocol.clientHandshake(socket, this)
            }) {
            isRunning = true
            protocol.clientRun(socket, this, shutdown)
        } else {
            close()
            throw SocketException()
        }
    }

    fun startAsync(protocol: Protocol, shutdown: () -> Boolean = { false }) {
        thread(name = "$name.tcp") {
            try {
                start(protocol, shutdown)
            } catch (e: SocketException) {
                // connection closed
            }
        }
    }

    override fun close() {
        dis.close()
        dos.close()
        socket.close()
    }

}