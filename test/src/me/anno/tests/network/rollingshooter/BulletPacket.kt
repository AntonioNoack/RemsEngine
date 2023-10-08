package me.anno.tests.network.rollingshooter

import me.anno.io.Writing.readVec3
import me.anno.io.Writing.writeVec3
import me.anno.network.Packet
import me.anno.network.Server
import me.anno.network.TCPClient
import org.joml.Vector3f
import java.io.DataInputStream
import java.io.DataOutputStream

class BulletPacket(val callback: (BulletPacket) -> Unit) : Packet("Shot") {

    val pos = Vector3f()
    val dir = Vector3f()

    var distance = 0f

    override fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        dos.writeVec3(pos)
        dos.writeVec3(dir)
        dos.writeFloat(distance)
    }

    override fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        dis.readVec3(pos)
        dis.readVec3(dir)
        distance = dis.readFloat()
    }

    override fun onReceive(server: Server?, client: TCPClient) {
        if (server != null) server.broadcastExcept(this, client)
        else callback(this)
    }
}