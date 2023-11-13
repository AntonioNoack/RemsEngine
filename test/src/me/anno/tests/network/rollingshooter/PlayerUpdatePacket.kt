package me.anno.tests.network.rollingshooter

import me.anno.io.DataStreamUtils.readQuat
import me.anno.io.DataStreamUtils.readVec3
import me.anno.io.DataStreamUtils.writeQuat
import me.anno.io.DataStreamUtils.writeVec3
import me.anno.network.Packet
import me.anno.network.Server
import me.anno.network.TCPClient
import org.joml.Quaternionf
import org.joml.Vector3f
import java.io.DataInputStream
import java.io.DataOutputStream

class PlayerUpdatePacket(val callback: (PlayerUpdatePacket) -> Unit) : Packet("Stat") {

    var name: String = ""

    val position = Vector3f()
    val linearVelocity = Vector3f()
    val rotation = Quaternionf()
    val angularVelocity = Vector3f()

    var color = 0

    override fun writeData(server: Server?, client: TCPClient, dos: DataOutputStream) {
        super.writeData(server, client, dos)
        dos.writeUTF(name)
        dos.writeVec3(position)
        dos.writeVec3(linearVelocity)
        dos.writeQuat(rotation)
        dos.writeVec3(angularVelocity)
        dos.writeInt(color)
    }

    override fun readData(server: Server?, client: TCPClient, dis: DataInputStream, size: Int) {
        super.readData(server, client, dis, size)
        name = dis.readUTF()
        dis.readVec3(position)
        dis.readVec3(linearVelocity)
        dis.readQuat(rotation)
        dis.readVec3(angularVelocity)
        color = dis.readInt()
    }

    override fun onReceive(server: Server?, client: TCPClient) {
        if (server != null) {
            name = client.name
            server.broadcastExcept(this, client)
        } else callback(this)
    }
}
