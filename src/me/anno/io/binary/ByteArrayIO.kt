package me.anno.io.binary

import me.anno.utils.structures.arrays.ByteArrayList

/**
 * Utility functions for using ByteArrays like streams.
 * This gives the advantage of arbitrary skipping and hopefully low overhead.
 * */
object ByteArrayIO {

    private fun ByteArray.readUnsafe(addr: Int): Int {
        return this[addr].toInt().and(255)
    }

    fun ByteArray.read(addr: Int, default: Int = 0): Int {
        if (addr < 0 || addr + 1 > size) return default
        return this[addr].toInt().and(255)
    }

    fun ByteArray.readBE16(addr: Int, default: Int = 0): Int {
        if (addr < 0 || addr + 2 > size) return default
        return readUnsafe(addr).shl(8) +
                readUnsafe(addr + 1)
    }

    fun ByteArray.readBE32(addr: Int, default: Int = 0): Int {
        if (addr < 0 || addr + 4 > size) return default
        return readUnsafe(addr).shl(24) +
                readUnsafe(addr + 1).shl(16) +
                readUnsafe(addr + 2).shl(8) +
                readUnsafe(addr + 3)
    }

    fun ByteArray.readBE64(addr: Int, default: Long = 0): Long {
        if (addr < 0 || addr + 8 > size) return default
        return readLE32(addr).toLong().shl(32) +
                readLE32(addr + 4).toLong().and(0xffffffffL)
    }

    fun ByteArray.readLE16(addr: Int, default: Int = 0): Int {
        if (addr < 0 || addr + 2 > size) return default
        return readUnsafe(addr) +
                readUnsafe(addr + 1).shl(8)
    }

    fun ByteArray.readLE32(addr: Int, default: Int = 0): Int {
        if (addr < 0 || addr + 4 > size) return default
        return readUnsafe(addr) +
                readUnsafe(addr + 1).shl(8) +
                readUnsafe(addr + 2).shl(16) +
                readUnsafe(addr + 3).shl(24)
    }

    fun ByteArray.readLE64(addr: Int, default: Long = 0): Long {
        if (addr < 0 || addr + 8 > size) return default
        return readLE32(addr).toLong().and(0xffffffffL) +
                readLE32(addr + 4).toLong().shl(32)
    }

    fun ByteArray.readBE32F(addr: Int, default: Float = 0f): Float {
        if (addr < 0 || addr + 4 > size) return default
        return Float.fromBits(readBE32(addr))
    }

    fun ByteArray.readBE64F(addr: Int, default: Double = 0.0): Double {
        if (addr < 0 || addr + 8 > size) return default
        return Double.fromBits(readBE64(addr))
    }

    fun ByteArray.readLE32F(addr: Int, default: Float = 0f): Float {
        if (addr < 0 || addr + 4 > size) return default
        return Float.fromBits(readLE32(addr))
    }

    fun ByteArray.readLE64F(addr: Int, default: Double = 0.0): Double {
        if (addr < 0 || addr + 8 > size) return default
        return Double.fromBits(readLE64(addr))
    }

    fun ByteArray.writeBE16(addr: Int, value: Int) {
        set(addr, (value shr 8).toByte())
        set(addr + 1, (value).toByte())
    }

    fun ByteArray.writeBE32(addr: Int, value: Int) {
        set(addr, (value shr 24).toByte())
        set(addr + 1, (value shr 16).toByte())
        set(addr + 2, (value shr 8).toByte())
        set(addr + 3, (value).toByte())
    }

    fun ByteArray.writeBE64(addr: Int, value: Long) {
        set(addr, (value shr 56).toByte())
        set(addr + 1, (value shr 48).toByte())
        set(addr + 2, (value shr 40).toByte())
        set(addr + 3, (value shr 32).toByte())
        set(addr + 4, (value shr 24).toByte())
        set(addr + 5, (value shr 16).toByte())
        set(addr + 6, (value shr 8).toByte())
        set(addr + 7, (value).toByte())
    }

    fun ByteArray.writeLE16(addr: Int, value: Int) {
        set(addr, (value).toByte())
        set(addr + 1, (value shr 8).toByte())
    }

    fun ByteArray.writeLE32(addr: Int, value: Int) {
        set(addr, (value).toByte())
        set(addr + 1, (value shr 8).toByte())
        set(addr + 2, (value shr 16).toByte())
        set(addr + 3, (value shr 24).toByte())
    }

    fun ByteArray.writeLE64(addr: Int, value: Long) {
        set(addr, (value).toByte())
        set(addr + 1, (value shr 8).toByte())
        set(addr + 2, (value shr 16).toByte())
        set(addr + 3, (value shr 24).toByte())
        set(addr + 4, (value shr 32).toByte())
        set(addr + 5, (value shr 40).toByte())
        set(addr + 6, (value shr 48).toByte())
        set(addr + 7, (value shr 56).toByte())
    }

    fun ByteArray.writeBE32(addr: Int, value: Float) {
        writeBE32(addr, value.toRawBits())
    }

    fun ByteArray.writeBE64(addr: Int, value: Double) {
        writeBE64(addr, value.toRawBits())
    }

    fun ByteArray.writeLE32(addr: Int, value: Float) {
        writeLE32(addr, value.toRawBits())
    }

    fun ByteArray.writeLE64(addr: Int, value: Double) {
        writeLE64(addr, value.toRawBits())
    }

    fun ByteArrayList.writeBE16(value: Int) {
        ensureExtra(2)
        values.writeBE16(size, value)
        size += 2
    }

    fun ByteArrayList.writeBE32(value: Int) {
        ensureExtra(4)
        values.writeBE32(size, value)
        size += 4
    }

    fun ByteArrayList.writeBE64(value: Long) {
        ensureExtra(8)
        values.writeBE64(size, value)
        size += 8
    }

    fun ByteArrayList.writeLE16(value: Int) {
        ensureExtra(2)
        values.writeLE32(size, value)
        size += 2
    }

    fun ByteArrayList.writeLE32(value: Int) {
        ensureExtra(4)
        values.writeLE32(size, value)
        size += 4
    }

    fun ByteArrayList.writeLE64(value: Long) {
        ensureExtra(8)
        values.writeBE64(size, value)
        size += 8
    }

    fun ByteArrayList.writeBE32(value: Float) {
        writeBE32(value.toRawBits())
    }

    fun ByteArrayList.writeBE64(value: Double) {
        writeBE64(value.toRawBits())
    }

    fun ByteArrayList.writeLE32(value: Float) {
        writeLE32(value.toRawBits())
    }

    fun ByteArrayList.writeLE64(value: Double) {
        writeLE64(value.toRawBits())
    }

    fun leMagic(b: Char, g: Char, r: Char, a: Char): Int {
        return (a.code shl 24) or (r.code shl 16) or (g.code shl 8) or b.code
    }

    fun beMagic(a: Char, r: Char, g: Char, b: Char): Int {
        return (a.code shl 24) or (r.code shl 16) or (g.code shl 8) or b.code
    }

}