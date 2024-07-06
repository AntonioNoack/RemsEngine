package me.anno.maths

object Packing {

    fun pack16(high8: Int, low8: Int): Int {
        return high8.and(0xff).shl(8) or low8.and(0xff)
    }

    fun pack32(high16: Int, low16: Int): Int {
        return high16.shl(16) or low16.and(0xffff)
    }

    fun pack64(high32: Int, low32: Int): Long {
        return high32.toLong().shl(32) or low32.toLong().and(0xffffffffL)
    }

    fun unpackLowFrom16(pack16: Int, signed: Boolean): Int {
        return if (signed) {
            pack16.shl(24).shr(8)
        } else {
            pack16.and(0xff)
        }
    }

    fun unpackHighFrom16(pack16: Int, signed: Boolean): Int {
        return if (signed) {
            pack16.shl(16).shr(24)
        } else {
            pack16.shr(8).and(0xff)
        }
    }

    fun unpackLowFrom32(pack32: Int, signed: Boolean): Int {
        return if (signed) {
            pack32.shl(16).shr(16)
        } else {
            pack32.and(0xffff)
        }
    }

    fun unpackHighFrom32(pack32: Int, signed: Boolean): Int {
        val base = pack32.shr(16)
        return if (signed) {
            base
        } else {
            base.and(0xffff)
        }
    }

    fun unpackLowFrom64(pack64: Long): Int {
        return pack64.toInt()
    }

    fun unpackHighFrom64(pack32: Long): Int {
        return pack32.shr(32).toInt()
    }

    fun unpackLowFrom64(pack64: Long, signed: Boolean): Long {
        return if (signed) {
            pack64.shl(32).shr(32)
        } else {
            pack64.and(0xffffffffL)
        }
    }

    fun unpackHighFrom64(pack32: Long, signed: Boolean): Long {
        val base = pack32.shr(32)
        return if (signed) {
            base
        } else {
            base.and(0xffffffffL)
        }
    }
}