package net.sf.image4j.codec.ico

import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32
import net.sf.image4j.io.CountingInputStream

/**
 * Represents an <tt>IconEntry</tt> structure, which contains information about an ICO image.
 *
 *
 * Creates an <tt>IconEntry</tt> structure from the source input
 *
 * @author Ian McDonagh
 * @param input the source input
 */
class IconEntry(input: CountingInputStream) {

    /**
     * The width of the icon image in pixels.
     * <tt>0</tt> specifies a width of 256 pixels.
     */
    var width = input.read()

    /**
     * The height of the icon image in pixels.
     * <tt>0</tt> specifies a height of 256 pixels.
     */
    var height = input.read()

    /**
     * The number of colours, calculated from [sBitCount][.sBitCount].
     * <tt>0</tt> specifies a colour count of &gt;= 256.
     */
    var colorCount = input.read()

    // Unused. Should always be <tt>0</tt>.
    // var bReserved: Byte = input.readByte()
    init {
        input.read()
    }

    /**
     * Number of planes, which should always be <tt>1</tt>.
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

    /**
     * A string representation of this <tt>IconEntry</tt> structure.
     */
    override fun toString(): String {
        return "width=$width,height=$height,bitCount=$bitCount,colorCount=$colorCount"
    }

    // Width 	    1 byte 	Cursor Width (16, 32, 64, 0 = 256)
    // Height 	    1 byte 	Cursor Height (16, 32, 64, 0 = 256 , most commonly = Width)
    // ColorCount 	1 byte 	Number of Colors (2,16, 0=256)
    // Reserved 	1 byte 	=0
    // Planes 	    2 byte 	=1
    // BitCount 	2 byte 	bits per pixel (1, 4, 8)
    // SizeInBytes 	4 byte 	Size of (InfoHeader + AND-bitmap + XOR-bitmap)
    // FileOffset 	4 byte 	FilePos, where InfoHeader starts

}