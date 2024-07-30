package net.sf.image4j.codec.ico

import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32
import java.io.InputStream

/**
 * Represents an IconEntry structure, which contains information about an ICO image.
 *
 *
 * Creates an IconEntry structure from the source input
 *
 * @author Ian McDonagh
 * @param input the source input
 */
class IconEntry(input: InputStream) {

    /**
     * The width of the icon image in pixels.
     * 0 specifies a width of 256 pixels.
     */
    var width = input.read()

    /**
     * The height of the icon image in pixels.
     * 0 specifies a height of 256 pixels.
     */
    var height = input.read()

    /**
     * The number of colours, calculated from [sBitCount][.sBitCount].
     * 0 specifies a colour count of &gt;= 256.
     */
    var colorCount = input.read()

    init {
        // Unused. Should always be 0.
        input.read()
    }

    /**
     * Number of planes, which should always be 1.
     */
    var planes = input.readLE16()

    /**
     * Colour depth in bits per pixel.
     */
    var bitCount = input.readLE16()

    /**
     * Size of ICO data, which should be the size of (InfoHeader + AND bitmap + XOR bitmap).
     */
    var sizeInBytes = input.readLE32()

    /**
     * Position in file where the InfoHeader starts.
     */
    var fileOffset = input.readLE32()

    var index = 0

    /**
     * A string representation of this IconEntry structure.
     */
    override fun toString(): String {
        return "width=$width,height=$height,bitCount=$bitCount,colorCount=$colorCount"
    }
}