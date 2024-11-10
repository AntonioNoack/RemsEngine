package net.sf.image4j.codec.ico

import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32
import java.io.InputStream

/**
 * Represents an IconEntry structure, which contains information about an ICO image.
 *
 * Creates an IconEntry structure from the source input
 *
 * @author Ian McDonagh, modified by Antonio Noack
 * @param input the source input
 */
class IconEntry(input: InputStream) {

    private fun readSize(i: Int): Int {
        return if(i == 0) 256 else i
    }

    /**
     * The width of the icon image in pixels.
     * 0 specifies a width of 256 pixels.
     */
    val width = readSize(input.read())

    /**
     * The height of the icon image in pixels.
     * 0 specifies a height of 256 pixels.
     */
    val height = readSize(input.read())

    /**
     * The number of colours, calculated from [sBitCount][.sBitCount].
     * 0 specifies a colour count of &gt;= 256.
     */
    val colorCount = readSize(input.read())

    init {
        // Unused. Should always be 0.
        input.read()
    }

    /**
     * Number of planes, which should always be 1.
     */
    val planes = input.readLE16()

    /**
     * Colour depth in bits per pixel.
     */
    val bitCount = input.readLE16()

    /**
     * Size of ICO data, which should be the size of (InfoHeader + AND bitmap + XOR bitmap).
     */
    val sizeInBytes = input.readLE32()

    /**
     * Position in file where the InfoHeader starts.
     */
    val fileOffset = input.readLE32()

    var index = 0

    /**
     * A string representation of this IconEntry structure.
     */
    override fun toString(): String {
        return "width=$width,height=$height,bitCount=$bitCount,colorCount=$colorCount"
    }
}