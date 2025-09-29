package me.anno.io.binary

import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.arrays.ByteArrayList

/**
 * Utility functions for using ByteArrays like streams.
 * This gives the advantage of arbitrary skipping and hopefully low overhead.
 * */
object ByteArrayIO {

    private fun ByteArray.readUnsafe(index: Int): Int {
        return this[index].toInt().and(255)
    }

    fun ByteArray.read(index: Int, default: Int = 0): Int {
        if (index < 0 || index + 1 > size) return default
        return this[index].toInt().and(255)
    }

    fun ByteArray.readBE16(index: Int, default: Int = 0): Int {
        if (index < 0 || index + 2 > size) return default
        return readUnsafe(index).shl(8) +
                readUnsafe(index + 1)
    }

    fun ByteArray.readBE32(index: Int, default: Int = 0): Int {
        if (index < 0 || index + 4 > size) return default
        return readUnsafe(index).shl(24) +
                readUnsafe(index + 1).shl(16) +
                readUnsafe(index + 2).shl(8) +
                readUnsafe(index + 3)
    }

    fun ByteArray.readBE64(index: Int, default: Long = 0): Long {
        if (index < 0 || index + 8 > size) return default
        return readBE32(index).toLong().shl(32) +
                readBE32(index + 4).toLong().and(0xffffffffL)
    }

    fun ByteArray.readLE16(index: Int, default: Int = 0): Int {
        if (index < 0 || index + 2 > size) return default
        return readUnsafe(index) +
                readUnsafe(index + 1).shl(8)
    }

    fun ByteArray.readLE32(index: Int, default: Int = 0): Int {
        if (index < 0 || index + 4 > size) return default
        return readUnsafe(index) +
                readUnsafe(index + 1).shl(8) +
                readUnsafe(index + 2).shl(16) +
                readUnsafe(index + 3).shl(24)
    }

    fun ByteArray.readLE64(index: Int, default: Long = 0): Long {
        if (index < 0 || index + 8 > size) return default
        return readLE32(index).toLong().and(0xffffffffL) +
                readLE32(index + 4).toLong().shl(32)
    }

    fun ByteArray.readBE32F(index: Int, default: Float = 0f): Float {
        if (index < 0 || index + 4 > size) return default
        return Float.fromBits(readBE32(index))
    }

    fun ByteArray.readBE64F(index: Int, default: Double = 0.0): Double {
        if (index < 0 || index + 8 > size) return default
        return Double.fromBits(readBE64(index))
    }

    fun ByteArray.readLE32F(index: Int, default: Float = 0f): Float {
        if (index < 0 || index + 4 > size) return default
        return Float.fromBits(readLE32(index))
    }

    fun ByteArray.readLE64F(index: Int, default: Double = 0.0): Double {
        if (index < 0 || index + 8 > size) return default
        return Double.fromBits(readLE64(index))
    }

    fun ByteArray.writeBE16(index: Int, value: Int) {
        set(index, (value shr 8).toByte())
        set(index + 1, (value).toByte())
    }

    fun ByteArray.writeBE32(index: Int, value: Int) {
        set(index, (value shr 24).toByte())
        set(index + 1, (value shr 16).toByte())
        set(index + 2, (value shr 8).toByte())
        set(index + 3, (value).toByte())
    }

    fun ByteArray.writeBE64(index: Int, value: Long) {
        set(index, (value shr 56).toByte())
        set(index + 1, (value shr 48).toByte())
        set(index + 2, (value shr 40).toByte())
        set(index + 3, (value shr 32).toByte())
        set(index + 4, (value shr 24).toByte())
        set(index + 5, (value shr 16).toByte())
        set(index + 6, (value shr 8).toByte())
        set(index + 7, (value).toByte())
    }

    fun ByteArray.writeLE16(index: Int, value: Int) {
        set(index, (value).toByte())
        set(index + 1, (value shr 8).toByte())
    }

    fun ByteArray.writeLE32(index: Int, value: Int) {
        set(index, (value).toByte())
        set(index + 1, (value shr 8).toByte())
        set(index + 2, (value shr 16).toByte())
        set(index + 3, (value shr 24).toByte())
    }

    fun ByteArray.writeLE64(index: Int, value: Long) {
        set(index, (value).toByte())
        set(index + 1, (value shr 8).toByte())
        set(index + 2, (value shr 16).toByte())
        set(index + 3, (value shr 24).toByte())
        set(index + 4, (value shr 32).toByte())
        set(index + 5, (value shr 40).toByte())
        set(index + 6, (value shr 48).toByte())
        set(index + 7, (value shr 56).toByte())
    }

    fun ByteArray.writeBE32(index: Int, value: Float) {
        writeBE32(index, value.toRawBits())
    }

    fun ByteArray.writeBE64(index: Int, value: Double) {
        writeBE64(index, value.toRawBits())
    }

    fun ByteArray.writeLE32(index: Int, value: Float) {
        writeLE32(index, value.toRawBits())
    }

    fun ByteArray.writeLE64(index: Int, value: Double) {
        writeLE64(index, value.toRawBits())
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

    fun ByteArrayList.writeBE32(value: Float) {
        writeBE32(value.toRawBits())
    }

    fun ByteArrayList.writeBE64(value: Double) {
        writeBE64(value.toRawBits())
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
        values.writeLE64(size, value)
        size += 8
    }

    fun ByteArrayList.writeLE32(value: Float) {
        writeLE32(value.toRawBits())
    }

    fun ByteArrayList.writeLE64(value: Double) {
        writeLE64(value.toRawBits())
    }

    fun ByteArrayList.readBE16(index: Int, default: Int = 0): Int {
        return if (index >= 0 && index + 2 <= size) values.readBE16(index, default) else default
    }

    fun ByteArrayList.readBE32(index: Int, default: Int = 0): Int {
        return if (index >= 0 && index + 4 <= size) values.readBE32(index, default) else default
    }

    fun ByteArrayList.readBE32F(index: Int, default: Float = 0f): Float {
        return if (index >= 0 && index + 4 <= size) values.readBE32F(index, default) else default
    }

    fun ByteArrayList.readBE64(index: Int, default: Long = 0L): Long {
        return if (index >= 0 && index + 8 <= size) values.readBE64(index, default) else default
    }

    fun ByteArrayList.readBE64F(index: Int, default: Double = 0.0): Double {
        return if (index >= 0 && index + 8 <= size) values.readBE64F(index, default) else default
    }

    fun ByteArrayList.readLE16(index: Int, default: Int = 0): Int {
        return if (index >= 0 && index + 2 <= size) values.readLE16(index, default) else default
    }

    fun ByteArrayList.readLE32(index: Int, default: Int = 0): Int {
        return if (index >= 0 && index + 4 <= size) values.readLE32(index, default) else default
    }

    fun ByteArrayList.readLE32F(index: Int, default: Float = 0f): Float {
        return if (index >= 0 && index + 4 <= size) values.readLE32F(index, default) else default
    }

    fun ByteArrayList.readLE64(index: Int, default: Long = 0L): Long {
        return if (index >= 0 && index + 8 <= size) values.readLE64(index, default) else default
    }

    fun ByteArrayList.readLE64F(index: Int, default: Double = 0.0): Double {
        return if (index >= 0 && index + 8 <= size) values.readLE64F(index, default) else default
    }

    fun leMagic(c0: Char, c1: Char, c2: Char, c3: Char): Int {
        return (c3.code shl 24) or (c2.code shl 16) or (c1.code shl 8) or c0.code
    }

    fun beMagic(c0: Char, c1: Char, c2: Char, c3: Char): Int {
        return (c0.code shl 24) or (c1.code shl 16) or (c2.code shl 8) or c3.code
    }

    fun leMagic(string: String): Int {
        assertEquals(4, string.length)
        return leMagic(string[0], string[1], string[2], string[3])
    }

    fun beMagic(string: String): Int {
        assertEquals(4, string.length)
        return beMagic(string[0], string[1], string[2], string[3])
    }
}