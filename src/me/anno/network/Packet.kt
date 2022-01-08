package me.anno.network

import me.anno.network.Protocol.Companion.convertMagic
import me.anno.utils.input.Input.readNBytes2
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

open class Packet(val bigEndianMagic: Int) {

    constructor(bigEndianMagic: String) : this(convertMagic(bigEndianMagic))

    /** < 0 = unknown, and needs to be buffered */
    open val size = -1

    /** whether this packet will always have the same size; no matter the version, edition, or sth else */
    open val constantSize = false

    open fun send(server: Server?, client: TCPClient, dos: DataOutputStream) {
        // standard serialization
        val size = size
        if (size < 0) {
            if (constantSize) throw IllegalStateException("Size must be known, if it is constant")
            val bos = ByteArrayOutputStream()
            val dos2 = DataOutputStream(bos)
            sendData(server, client, dos2)
            dos2.close()
            bos.close()
            dos.writeInt(bos.size())
            bos.writeTo(dos)
        } else {
            // size is known
            if (!constantSize) dos.writeInt(size)
            sendData(server, client, dos)
        }
    }

    open fun sendData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        // can send the data out
    }

    open fun receive(server: Server?, client: TCPClient, dis: DataInputStream) {
        if (constantSize) {
            if (size < 0) throw IllegalStateException("Size must be non-negative")
            // we trust the packet reading function
            receiveData(server, client, dis, size)
        } else {
            // we don't trust the packet reading function
            val size = dis.readInt()
            if (size < 0) throw IOException("Size must be >= 0")
            if (size > client.maxPacketSize) throw IOException("Packet is too large")
            val buffer = client.buffer
            buffer.reset()
            buffer.ensureCapacity(size)
            dis.readNBytes2(size, buffer.buffer, true)
            receiveData(server, client, client.bufferDis, size)
        }
    }

    open fun udpReceive(
        server: Server?,
        client: TCPClient,
        dis: DataInputStream,
        sendResponse: (packet: Packet) -> Unit
    ) {
        var size = size
        if (!constantSize) {
            size = dis.readInt()
        }
        if (size < 0) throw IOException("Size must be >= 0")
        dis.mark(size) // size = is the read limit
        receiveData(server, client, dis, size)
        dis.reset()
        dis.skipBytes(size)
    }

    open fun receiveData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        // can read the data in
    }

}