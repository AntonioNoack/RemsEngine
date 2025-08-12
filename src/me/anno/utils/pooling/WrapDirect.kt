package me.anno.utils.pooling

import org.apache.logging.log4j.LogManager
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

/**
 * When working with native libraries, we always need direct buffers,
 * but usually we prefer native arrays, so these are utility methods for achieving the same, but with a bit of copy overhead.
 *
 * Call the second return value, when you're done!
 * */
object WrapDirect {

    private val LOGGER = LogManager.getLogger(WrapDirect::class)

    private val canWrapDirectly by lazy {
        val value = ByteBuffer.wrap(ByteArray(1)).isDirect
        LOGGER.info("Supports direct wrapping? $value")
        value
    }

    private fun warnLimitedWrapping() {
        LOGGER.warn("Wrapping only works sometimes???")
    }

    @JvmStatic
    fun ByteArray.wrapDirect(offset: Int, length: Int): Pair<ByteBuffer, () -> Unit> {
        if (canWrapDirectly) {
            val maybe = ByteBuffer.wrap(this, offset, length)
            if (maybe.isDirect) return maybe to {}
            else warnLimitedWrapping()
        }

        val pool = Pools.byteBufferPool
        val tmp = pool[length, false, false]
        tmp.position(0).limit(length)
        tmp.put(this, offset, length).flip()
        return tmp to { pool.returnBuffer(tmp) }
    }

    @JvmStatic
    fun ShortArray.wrapDirect(offset: Int, length: Int): Pair<ShortBuffer, () -> Unit> {
        if (canWrapDirectly) {
            val maybe = ShortBuffer.wrap(this, offset, length)
            if (maybe.isDirect) return maybe to {}
            else warnLimitedWrapping()
        }

        val pool = Pools.byteBufferPool
        val tmp = pool[length shl 1, false, false]
        tmp.position(0).limit(length shl 1)
        val ret = tmp.asShortBuffer()
        ret.put(this, offset, length).flip()
        return ret to { pool.returnBuffer(tmp) }
    }

    @JvmStatic
    fun IntArray.wrapDirect(offset: Int, length: Int): Pair<IntBuffer, () -> Unit> {
        if (canWrapDirectly) {
            val maybe = IntBuffer.wrap(this, offset, length)
            if (maybe.isDirect) return maybe to {}
            else warnLimitedWrapping()
        }

        val pool = Pools.byteBufferPool
        val tmp = pool[length shl 2, false, false]
        tmp.position(0).limit(length shl 2)
        val ret = tmp.asIntBuffer()
        ret.put(this, offset, length).flip()
        return ret to { pool.returnBuffer(tmp) }
    }

    @JvmStatic
    fun FloatArray.wrapDirect(offset: Int, length: Int): Pair<FloatBuffer, () -> Unit> {
        if (canWrapDirectly) {
            val maybe = FloatBuffer.wrap(this, offset, length)
            if (maybe.isDirect) return maybe to {}
            else warnLimitedWrapping()
        }

        val pool = Pools.byteBufferPool
        val tmp = pool[length shl 2, false, false]
        tmp.position(0).limit(length shl 2)
        val ret = tmp.asFloatBuffer()
        ret.put(this, offset, length).flip()
        return ret to { pool.returnBuffer(tmp) }
    }
}