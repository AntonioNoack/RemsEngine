package me.anno.network.packets

import me.anno.network.Server
import me.anno.network.TCPClient
import java.io.DataInputStream
import java.io.DataOutputStream

class POS2Packet(magic: String = "POS2") : POS1Packet(magic) {

    override val size: Int = super.size + 4 * 4
    override val constantSize: Boolean = true

    // second rotation, e.g. for body
    var sx = 0f
    var sy = 0f
    var sz = 0f
    var sw = 0f

    override fun sendData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        super.sendData(server, client, dos)
        dos.writeFloat(sx)
        dos.writeFloat(sy)
        dos.writeFloat(sz)
        dos.writeFloat(sw)
    }

    override fun receiveData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        super.receiveData(server, client, dis, size)
        sx = dis.readFloat()
        sy = dis.readFloat()
        sz = dis.readFloat()
        sw = dis.readFloat()
    }

}