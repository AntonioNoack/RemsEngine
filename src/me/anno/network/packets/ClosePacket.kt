package me.anno.network.packets

import me.anno.io.base.BaseWriter
import me.anno.network.Packet
import me.anno.network.Server
import me.anno.network.TCPClient
import org.apache.logging.log4j.LogManager
import java.io.DataInputStream
import java.io.DataOutputStream

@Suppress("unused")
open class ClosePacket(var reason: String, magic: String) : Packet(magic) {

    constructor() : this("")

    constructor(reason: String) : this(reason, "CLOS")

    constructor(reason: String, bigEndianMagic: Int) : this(reason) {
        this.bigEndianMagic = bigEndianMagic
    }

    override val size: Int = if (reason.any { it.code !in 0 until 128 }) -1 else reason.length

    override fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        dos.writeUTF(reason)
    }

    override fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        reason = dis.readUTF()
        // should be shown in the UI
        client.closingReason = reason
        client.close()
        LOGGER.warn("Connection was closed, because '$reason'")
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("reason", reason)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "reason" -> reason = value as? String ?: return
            else -> super.setProperty(name, value)
        }
    }

    override val className: String get() = "ClosePacket"

    companion object {
        private val LOGGER = LogManager.getLogger(ClosePacket::class)
    }
}