package me.anno.network.packets

import me.anno.io.base.BaseWriter
import me.anno.network.Packet
import me.anno.network.Server
import me.anno.network.TCPClient
import java.io.DataInputStream
import java.io.DataOutputStream

abstract class MSG0Packet : Packet {

    constructor(magic: String = "MSG0") : super(magic)
    constructor(magic: Int) : super(magic)

    var sender = 0L
    var senderName = ""

    var receiver = 0L
    var receiverName = ""

    var message = ""

    override val constantSize: Boolean = false

    override fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        dos.writeLong(sender)
        dos.writeUTF(senderName)
        dos.writeLong(receiver)
        dos.writeUTF(receiverName)
        dos.writeUTF(message)
    }

    override fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        sender = dis.readLong()
        senderName = dis.readUTF()
        receiver = dis.readLong()
        receiverName = dis.readUTF()
        message = dis.readUTF()
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeLong("sender", sender)
        writer.writeString("senderName", senderName)
        writer.writeLong("receiver", receiver)
        writer.writeString("receiverName", receiverName)
        writer.writeString("message", message)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "sender" -> sender = value as? Long ?: return
            "receiver" -> receiver = value as? Long ?: return
            "senderName" -> senderName = value as? String ?: return
            "receiverName" -> receiverName = value as? String ?: return
            "message" -> message = value as? String ?: return
            else -> super.setProperty(name, value)
        }
    }
}