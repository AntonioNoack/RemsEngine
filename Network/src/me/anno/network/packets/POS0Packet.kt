package me.anno.network.packets

import me.anno.io.base.BaseWriter
import me.anno.network.Packet
import me.anno.network.Server
import me.anno.network.TCPClient
import java.io.DataInputStream
import java.io.DataOutputStream

open class POS0Packet : Packet {

    // to do there could be a ping-pong-pos packet that responds with the positions of the nearest k players for super smooth movements :)

    constructor() : super("POS0")
    constructor(magic: String) : super(magic)
    constructor(magic: Int) : super(magic)

    override val size: Int = 8 * 3 + 4 * 3 + 2 * 8
    override val constantSize: Boolean = true

    var entity = 0L

    var localTime = 0L

    // position
    var px = 0.0
    var py = 0.0
    var pz = 0.0

    // velocity
    var vx = 0f
    var vy = 0f
    var vz = 0f

    override fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        dos.writeLong(entity)
        dos.writeLong(localTime)
        dos.writeDouble(px)
        dos.writeDouble(py)
        dos.writeDouble(pz)
        dos.writeFloat(vx)
        dos.writeFloat(vy)
        dos.writeFloat(vz)
    }

    override fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        entity = dis.readLong()
        localTime = dis.readLong()
        px = dis.readDouble()
        py = dis.readDouble()
        pz = dis.readDouble()
        vx = dis.readFloat()
        vy = dis.readFloat()
        vz = dis.readFloat()
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeLong("entity", entity)
        writer.writeLong("time", localTime)
        writer.writeDouble("px", px)
        writer.writeDouble("py", py)
        writer.writeDouble("pz", pz)
        writer.writeFloat("vx", vx)
        writer.writeFloat("vy", vy)
        writer.writeFloat("vz", vz)
    }

    override fun setProperty(name: String, value: Any?) {
        when(name){
            "px" -> px = value as? Double ?: return
            "py" -> py = value as? Double ?: return
            "pz" -> pz = value as? Double ?: return
            "entity" -> entity = value as? Long ?: return
            "time" -> entity = value as? Long ?: return
            "vx" -> vx = value as? Float ?: return
            "vy" -> vy = value as? Float ?: return
            "vz" -> vz = value as? Float ?: return
            else -> super.setProperty(name, value)
        }
    }
}