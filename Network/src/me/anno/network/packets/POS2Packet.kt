package me.anno.network.packets

import me.anno.io.base.BaseWriter
import me.anno.network.Server
import me.anno.network.TCPClient
import java.io.DataInputStream
import java.io.DataOutputStream

open class POS2Packet : POS1Packet {

    constructor() : super("POS2")
    constructor(magic: String) : super(magic)
    constructor(magic: Int) : super(magic)

    override val size: Int = super.size + 4 * 4
    override val constantSize: Boolean = true

    // second rotation, e.g., for body
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

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "sx" -> sx = value as? Float ?: return
            "sy" -> sy = value as? Float ?: return
            "sz" -> sz = value as? Float ?: return
            "sw" -> sw = value as? Float ?: return
            else -> super.setProperty(name, value)
        }
    }
}