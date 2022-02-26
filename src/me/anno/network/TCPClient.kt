package me.anno.network

import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

class TCPClient(val socket: Socket, var randomId: Int) : Closeable {

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

    fun send(server: Server?, packet: Packet) {
        dos.writeInt(packet.bigEndianMagic)
        synchronized(dos) {
            packet.send(server, this, dos)
        }
    }

    override fun close() {
        dis.close()
        dos.close()
        socket.close()
    }

}