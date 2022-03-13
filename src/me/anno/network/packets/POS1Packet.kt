package me.anno.network.packets

import me.anno.io.base.BaseWriter
import me.anno.network.Server
import me.anno.network.TCPClient
import java.io.DataInputStream
import java.io.DataOutputStream

open class POS1Packet : POS0Packet {

    constructor() : super("POS1")
    constructor(magic: String) : super(magic)
    constructor(magic: Int) : super(magic)

    override val size: Int = super.size + 4 * 4
    override val constantSize: Boolean = true

    // rotation quaternion, e.g. for head
    var rx = 0f
    var ry = 0f
    var rz = 0f
    var rw = 0f

    override fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        super.writeData(server, client, dos)
        dos.writeFloat(rx)
        dos.writeFloat(ry)
        dos.writeFloat(rz)
        dos.writeFloat(rw)
    }

    override fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        super.readData(server, client, dis, size)
        rx = dis.readFloat()
        ry = dis.readFloat()
        rz = dis.readFloat()
        rw = dis.readFloat()
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("rx", rx)
        writer.writeFloat("ry", ry)
        writer.writeFloat("rz", rz)
        writer.writeFloat("rw", rw)
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "rx" -> rx = value
            "ry" -> ry = value
            "rz" -> rz = value
            "rw" -> rw = value
            else -> super.readFloat(name, value)
        }
    }

    override val className = "POS1Packet"

}