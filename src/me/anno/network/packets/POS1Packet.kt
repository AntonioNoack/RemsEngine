package me.anno.network.packets

import me.anno.network.Server
import me.anno.network.TCPClient
import java.io.DataInputStream
import java.io.DataOutputStream

open class POS1Packet(magic: String = "POS1") : POS0Packet(magic) {

    override val size: Int = super.size + 4 * 4
    override val constantSize: Boolean = true

    // rotation quaternion, e.g. for head
    var rx = 0f
    var ry = 0f
    var rz = 0f
    var rw = 0f

    override fun sendData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        super.sendData(server, client, dos)
        dos.writeFloat(rx)
        dos.writeFloat(ry)
        dos.writeFloat(rz)
        dos.writeFloat(rw)
    }

    override fun receiveData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        super.receiveData(server, client, dis, size)
        rx = dis.readFloat()
        ry = dis.readFloat()
        rz = dis.readFloat()
        rw = dis.readFloat()
    }

}