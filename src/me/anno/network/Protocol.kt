package me.anno.network

import me.anno.Engine
import me.anno.network.Server.Companion.str32
import me.anno.network.packets.PingPacket
import me.anno.utils.Color.argb
import me.anno.utils.Color.hex32
import me.anno.utils.Sleep
import java.io.IOException
import java.net.Socket
import kotlin.math.abs

open class Protocol(val bigEndianMagic: Int, val networkProtocol: NetworkProtocol) {

    constructor(bigEndianMagic: String, networkProtocol: NetworkProtocol) :
            this(convertMagic(bigEndianMagic), networkProtocol)

    // todo user groups, and requests to those groups specifically? (e.g. friends)

    val packets = HashMap<Int, Packet>()

    fun register(packet: Packet) {
        packets[packet.bigEndianMagic] = packet
    }

    /**
     * return true, if the handshake was fine
     * return false to exit the connection (or throw an Exception)
     * */
    open fun serverHandshake(server: Server, client: TCPClient, magic: Int): Boolean {
        val dis = client.dis
        val dos = client.dos
        dos.writeInt(client.randomId) // the client needs this ID for UDP messages
        dos.writeUTF(server.name)
        dos.writeUTF(server.motd)
        dos.flush()
        client.name = dis.readUTF()
        client.uuid = dis.readUTF()
        return true
    }

    /**
     * return true, if the handshake was fine
     * return false to exit the connection (or throw an Exception)
     * */
    open fun clientHandshake(socket: Socket, client: TCPClient): Boolean {
        val dis = client.dis
        val dos = client.dos
        client.randomId = dis.readInt()
        client.serverName = dis.readUTF()
        client.serverMotd = dis.readUTF()
        dos.writeUTF(client.name)
        dos.writeUTF(client.uuid)
        dos.flush()
        return true
    }

    open fun serverRun(server: Server, client: TCPClient, magic: Int) {
        defaultRun(server, client) { server.shutdown }
    }

    open fun clientRun(socket: Socket, client: TCPClient, shutdown: () -> Boolean) {
        defaultRun(null, client, shutdown)
    }

    private fun defaultRun(server: Server?, client: TCPClient, shutdown: () -> Boolean) {
        val dis = client.dis
        var lastTime = System.nanoTime()
        while (!Engine.shutdown && !shutdown() && !client.isClosed) {
            if (dis.available() > 3) {
                val packetId = dis.readInt()
                val packet = packets[packetId] ?: throw IOException("Unknown packet ${str32(packetId)}")
                packet.receive(server, client, dis)
            } else {// no packet available for us :/
                val time = System.nanoTime()
                // send a ping to detect whether the server is still alive
                // but don't send it too often
                if (abs(time - lastTime) > 50_000_000) { // 50 ms
                    client.send(server, PingPacket())
                    lastTime = System.nanoTime()
                } else {
                    Sleep.sleepShortly(false)
                }
            }
        }
    }

    companion object {
        fun convertMagic(string: String): Int {
            if (string.length != 4) throw IllegalArgumentException("Magic length must be exactly 4")
            return argb(string[0].code, string[1].code, string[2].code, string[3].code)
        }
    }

}