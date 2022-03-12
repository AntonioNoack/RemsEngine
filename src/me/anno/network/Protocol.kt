package me.anno.network

import me.anno.Engine
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.network.Server.Companion.str32
import me.anno.network.packets.PingPacket
import me.anno.utils.Color.argb
import me.anno.utils.Sleep
import me.anno.utils.hpc.ThreadLocal2
import java.io.IOException
import java.net.Socket
import kotlin.math.abs

open class Protocol(val bigEndianMagic: Int, val networkProtocol: NetworkProtocol) {

    constructor(bigEndianMagic: String, networkProtocol: NetworkProtocol) :
            this(convertMagic(bigEndianMagic), networkProtocol)

    // todo user groups, and requests to those groups specifically? (e.g. friends)

    private val packets = HashMap<Int, Any>()
    var pingDelayMillis = 500

    /**
     * register a serial packet
     * serial packets will always be read in serial
     *
     * use this for quickly to process, constant size packets
     * */
    fun register(serialPacket: Packet) {
        packets[serialPacket.bigEndianMagic] = serialPacket
    }

    /**
     * register a parallel packet; will create at most one instance per client plus one statically
     * */
    fun register(parallelPacket: () -> Packet) {
        val sample = parallelPacket()
        register(sample.bigEndianMagic, parallelPacket)
    }

    /**
     * register a parallel packet; will create at most one instance per client
     * */
    fun register(bigEndianMagic: Int, parallelPacket: () -> Packet) {
        packets[bigEndianMagic] = ThreadLocal2(parallelPacket)
    }

    /**
     * register a parallel packet; will create at most one instance per client
     * */
    fun register(magic: String, parallelPacket: () -> Packet) {
        register(convertMagic(magic), parallelPacket)
    }

    fun find(id: Int): Any? {
        return packets[id]
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
                when (val packet = find(packetId)) {
                    is Packet -> synchronized(packet) {
                        packet.receive(server, client, dis)
                    }
                    is ThreadLocal<*> -> {
                        (packet.get() as Packet)
                            .receive(server, client, dis)
                    }
                    else -> throw IOException("Unknown packet ${str32(packetId)}")
                }
            } else {// no packet available for us :/
                val time = System.nanoTime()
                // send a ping to detect whether the server is still alive
                // but don't send it too often
                if (pingDelayMillis >= 0 && abs(time - lastTime) >= pingDelayMillis * MILLIS_TO_NANOS) {
                    client.sendTCP(server, PingPacket())
                    lastTime = System.nanoTime()
                } else {
                    Sleep.sleepShortly(false)
                }
            }
        }
    }

    companion object {
        fun convertMagic(string: String): Int {
            return when (string.length) {
                0 -> 0
                1 -> argb(string[0].code, 0, 0, 0)
                2 -> argb(string[0].code, string[1].code, 0, 0)
                3 -> argb(string[0].code, string[1].code, string[2].code, 0)
                4 -> argb(string[0].code, string[1].code, string[2].code, string[3].code)
                else -> throw IllegalArgumentException("Magic length must be <= 4")
            }
        }
    }

}