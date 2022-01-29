package me.anno.network.packets

import me.anno.network.Packet
import me.anno.network.Server
import me.anno.network.TCPClient
import java.io.DataInputStream
import java.io.DataOutputStream

open class POS0Packet(magic: String = "POS0") : Packet(magic) {

    override val size: Int = 8 * 6 + 8 + 8
    override val constantSize: Boolean = true

    var entityUUID = 0L

    var localTimestamp = 0L

    // position
    var x = 0.0
    var y = 0.0
    var z = 0.0

    // velocity
    var vx = 0.0
    var vy = 0.0
    var vz = 0.0

    override fun sendData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        dos.writeLong(entityUUID)
        dos.writeLong(localTimestamp)
        dos.writeDouble(x)
        dos.writeDouble(y)
        dos.writeDouble(z)
        dos.writeDouble(vx)
        dos.writeDouble(vy)
        dos.writeDouble(vz)
    }

    override fun receiveData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        entityUUID = dis.readLong()
        localTimestamp = dis.readLong()
        x = dis.readDouble()
        y = dis.readDouble()
        z = dis.readDouble()
        vx = dis.readDouble()
        vy = dis.readDouble()
        vz = dis.readDouble()
    }

}