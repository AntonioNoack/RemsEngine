package net.sf.image4j.codec.ico

import net.sf.image4j.io.LittleEndianInputStream

/**
 * Represents an <tt>IconEntry</tt> structure, which contains information about an ICO image.
 *
 *
 * Creates an <tt>IconEntry</tt> structure from the source input
 *
 * @author Ian McDonagh
 * @param input the source input
 * @throws java.io.IOException if an error occurs
 */
class IconEntry(input: LittleEndianInputStream) {

    /**
     * The width of the icon image in pixels.
     * <tt>0</tt> specifies a width of 256 pixels.
     */
    var bWidth = input.readUnsignedByte()

    /**
     * The height of the icon image in pixels.
     * <tt>0</tt> specifies a height of 256 pixels.
     */
    var bHeight = input.readUnsignedByte()

    /**
     * The number of colours, calculated from [sBitCount][.sBitCount].
     * <tt>0</tt> specifies a colour count of &gt;= 256.
     */
    var bColorCount = input.readUnsignedByte()

    // Unused. Should always be <tt>0</tt>.
    // var bReserved: Byte = input.readByte()
    init {
        input.readByte()
    }

    /**
     * Number of planes, which should always be <tt>1</tt>.
     */
    var sPlanes = input.readShortLE()

    /**
     * Colour depth in bits per pixel.
     */
    var sBitCount = input.readShortLE()

    /**
     * Size of ICO data, which should be the size of (InfoHeader + AND bitmap + XOR bitmap).
     */
    @JvmField
    var iSizeInBytes = input.readIntLE()

    /**
     * Position in file where the InfoHeader starts.
     */
    @JvmField
    var iFileOffset = input.readIntLE()

    /**
     * A string representation of this <tt>IconEntry</tt> structure.
     */
    override fun toString(): String {
        return "width=$bWidth,height=$bHeight,bitCount=$sBitCount,colorCount=$bColorCount"
    }

    //Width 	1 byte 	Cursor Width (16, 32, 64, 0 = 256)
    //Height 	1 byte 	Cursor Height (16, 32, 64, 0 = 256 , most commonly = Width)
    //ColorCount 	1 byte 	Number of Colors (2,16, 0=256)
    //Reserved 	1 byte 	=0
    //Planes 	2 byte 	=1
    //BitCount 	2 byte 	bits per pixel (1, 4, 8)
    //SizeInBytes 	4 byte 	Size of (InfoHeader + ANDbitmap + XORbitmap)
    //FileOffset 	4 byte 	FilePos, where InfoHeader starts

}