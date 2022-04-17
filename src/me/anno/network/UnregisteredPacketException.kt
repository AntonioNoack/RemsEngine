package me.anno.network

class UnregisteredPacketException(val packet: Packet) : IllegalArgumentException("Packet ${Server.str32(packet.bigEndianMagic)} was not registered!")