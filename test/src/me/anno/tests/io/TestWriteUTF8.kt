package me.anno.tests.io

import me.anno.io.Writer
import me.anno.utils.Color.hex8
import me.anno.utils.Color.toHexString
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Strings.joinChars
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import kotlin.streams.toList

class TestWriteUTF8 {

    class WriterImpl : Writer {
        val stream = ByteArrayOutputStream()
        override var position: Int = 0
        override fun writeLE8(v: Int) {
            stream.write(v)
            position++
        }

        override fun asByteBuffer(): ByteBuffer = throw NotImplementedError()
        override fun asStream(): OutputStream = stream
    }

    @Test
    fun writeUTF8() {
        var ctr = 1
        while (true) {
            val writer = WriterImpl()
            val baseline = ctr + 1
            if(baseline >= 0x20000) break
            writer.writeUTFChar(baseline)
            val asBytes = writer.stream.toByteArray()
            val asString = asBytes.decodeToString()
            val decoded = asString.codePoints().toList()
            assertEquals(1, decoded.size)
            assertEquals(baseline, decoded[0]) {
                "Expected ${
                    listOf(baseline).joinChars().toString()
                        .encodeToByteArray().map { hex8(it.toInt()) }
                }, got ${asBytes.map { hex8(it.toInt()) }} for 0x${baseline.toHexString()}"
            }
            ctr = ctr.shl(1)
        }
    }
}