package me.anno.network.packets

import me.anno.io.base.BaseWriter
import me.anno.network.Server
import me.anno.network.TCPClient
import java.io.DataInputStream
import java.io.DataOutputStream

class POS2Packet : POS1Packet {

    constructor() : super("POS2")
    constructor(magic: String) : super(magic)
    constructor(magic: Int) : super(magic)

    override val size: Int = super.size + 4 * 4
    override val constantSize: Boolean = true

    // second rotation, e.g. for body
    var sx = 0f
    var sy = 0f
    var sz = 0f
    var sw = 0f

    override fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        super.writeData(server, client, dos)
        dos.writeFloat(sx)
        dos.writeFloat(sy)
        dos.writeFloat(sz)
        dos.writeFloat(sw)
    }

    override fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        super.readData(server, client, dis, size)
        sx = dis.readFloat()
        sy = dis.readFloat()
        sz = dis.readFloat()
        sw = dis.readFloat()
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("sx", sx)
        writer.writeFloat("sy", sy)
        writer.writeFloat("sz", sz)
        writer.writeFloat("sw", sw)
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "sx" -> sx = value
            "sy" -> sy = value
            "sz" -> sz = value
            "sw" -> sw = value
            else -> super.readFloat(name, value)
        }
    }

    override val className: String get() = "POS2Packet"

}