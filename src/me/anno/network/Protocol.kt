package me.anno.network

import me.anno.Engine
import java.net.Socket

// todo support different base protocols? idk...

open class Protocol(val bigEndianMagic: Int) {

    // todo user groups, and requests to those groups specifically? (e.g. friends)

    val udpById = HashMap<Int, Packet>()
    val tcpById = HashMap<Int, Packet>()

    var shutdown = false

    fun registerPacket(id: Int, packet: Packet) {

    }

    /**
     * return true, if the handshake was fine
     * return false to exit the connection
     * */
    open fun serverHandshake(client: Client): Boolean {
        return true
    }

    open fun clientHandshake(socket: Socket) {

    }

    open fun serverRun(client: Client) {
        while (!Engine.shutdown && !shutdown) {

        }
    }

    open fun clientRun(socket: Socket) {
        while (!Engine.shutdown && !shutdown) {

        }
    }

}