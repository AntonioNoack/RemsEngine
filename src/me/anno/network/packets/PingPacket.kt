package me.anno.network.packets

import me.anno.network.Packet

class PingPacket(magic: String = "PING") : Packet(magic) {
    override val size: Int = 0
    override val constantSize: Boolean = true
}