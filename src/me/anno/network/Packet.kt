package me.anno.network

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.network.Protocol.Companion.convertMagic
import me.anno.utils.input.Input.readNBytes2
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

open class Packet(var bigEndianMagic: Int) : Saveable() {

    constructor(bigEndianMagic: String) : this(convertMagic(bigEndianMagic))

    /** < 0 = unknown, and needs to be buffered */
    open val size = -1

    /** whether this packet will always have the same size; no matter the version, edition, or sth else */
    open val constantSize = false

    /** whether this packet may be skipped, if there is lots of traffic or the client is malicious and doesn't read to cause server-side out-of-memory */
    open val canDropPacket = true

    open fun send(server: Server?, client: TCPClient, dos: DataOutputStream) {
        if (debugPackets) LOGGER.info("Sending(TCP) $this ${if (server == null) "${client.randomIdString} -> s" else "s -> ${client.randomIdString}"}")
        // standard serialization
        val size = size
        if (size < 0) {
            if (constantSize) throw IllegalStateException("Size must be known, if it is constant")
            val bos = ByteArrayOutputStream()
            val dos2 = DataOutputStream(bos)
            writeData(server, client, dos2)
            dos2.close()
            bos.close()
            dos.writeInt(bos.size())
            bos.writeTo(dos)
        } else {
            // size is known
            if (!constantSize) dos.writeInt(size)
            writeData(server, client, dos)
        }
    }

    open fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        // can send the data out
    }

    open fun receive(server: Server?, client: TCPClient, dis: DataInputStream) {
        if (constantSize) {
            if (size < 0) throw IllegalStateException("Size must be non-negative")
            // we trust the packet reading function
            readData(server, client, dis, size)
        } else {
            // we don't trust the packet reading function
            val size = dis.readInt()
            if (size < 0) throw IOException("Size must be >= 0")
            if (size > client.maxPacketSize) throw IOException("Packet is too large")
            val buffer = client.buffer
            buffer.reset()
            buffer.ensureCapacity(size)
            dis.readNBytes2(size, buffer.buffer, true)
            readData(server, client, client.bufferDis, size)
        }
        onReceive(server, client)
        if (debugPackets) LOGGER.info("Received(TCP) $this ${if (server == null) "s -> ${client.randomIdString}" else "${client.randomIdString} -> s"}")
    }

    open fun onReceive(server: Server?, client: TCPClient) {

    }

    open fun onReceiveUDP(server: Server?, client: TCPClient, sendResponse: (packet: Packet) -> Unit) {
        onReceive(server, client)
    }

    open fun receiveUdp(
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
        readData(server, client, dis, size)
        onReceiveUDP(server, client, sendResponse)
        dis.reset()
        dis.skipBytes(size)
        if (debugPackets) LOGGER.info("Received(UDP) $this ${if (server == null) "s -> ${client.randomIdString}" else "${client.randomIdString} -> s"}")
    }

    open fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        // can read the data in
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("magic", bigEndianMagic)
    }

    override fun readInt(name: String, value: Int) {
        if (name == "magic") bigEndianMagic = value
        else super.readInt(name, value)
    }

    override val className: String = "Packet"

    companion object {
        private val LOGGER = LogManager.getLogger(Packet::class)

        /**
         * to debug, which packets are sent, set this value to true
         * */
        var debugPackets = false
    }

}