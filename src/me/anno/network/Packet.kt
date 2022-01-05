package me.anno.network

import java.io.DataInputStream
import java.io.DataOutputStream

open class Packet {

    /** -1 = unknown, and needs to be buffered */
    open val size: Int get() = -1

    open val networkProtocol = NetworkProtocol.TCP

    open fun onServerSend(dos: DataOutputStream) {
        // todo standard serialization
    }

    open fun onServerReceive(dis: DataInputStream, size: Int) {
        // todo standard deserialization
    }

    open fun onClientSend(dos: DataOutputStream) {
        // todo standard serialization
    }

    open fun onClientReceive(dis: DataInputStream, size: Int) {
        // todo standard deserialization
    }

}