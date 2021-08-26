/*
 * Decodes a BMP image from an <tt>InputStream</tt> to a <tt>BufferedImage</tt>
 *
 * @author Ian McDonagh
 */

package net.sf.image4j.codec.bmp;

import me.anno.image.Image;
import me.anno.image.raw.IntImage;
import net.sf.image4j.io.CountingInputStream;
import net.sf.image4j.io.LittleEndianInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Decodes images in BMP format.
 *
 * @author Ian McDonagh
 */
public class BMPDecoder {

    private final Image img;

    /**
     * Creates a new instance of BMPDecoder and reads the BMP data from the source.
     *
     * @param in the source <tt>InputStream</tt> from which to read the BMP data
     * @throws java.io.IOException if an error occurs
     */
    public BMPDecoder(InputStream in) throws IOException {
        LittleEndianInputStream lis = new LittleEndianInputStream(new CountingInputStream(in));

        /* header [14] */

        //signature "BM" [2]
        byte[] signatureBytes = new byte[2];
        lis.read(signatureBytes);
        String signature = new String(signatureBytes, StandardCharsets.UTF_8);

        if (!signature.equals("BM")) {
            throw new IOException("Invalid signature '" + signature + "' for BMP format");
        }

        // file size [4]
        lis.readIntLE();

        // reserved = 0 [4]
        lis.readIntLE();

        //DataOffset [4] file offset to raster data
        lis.readIntLE();

        /* info header [40] */

        InfoHeader infoHeader = readInfoHeader(lis);

        /* Color table and Raster data */

        img = read(infoHeader, lis);
    }

    /**
     * Retrieves a bit from the lowest order byte of the given integer.
     *
     * @param bits  the source integer, treated as an unsigned byte
     * @param index the index of the bit to retrieve, which must be in the range <tt>0..7</tt>.
     * @return the bit at the specified index, which will be either <tt>0</tt> or <tt>1</tt>.
     */
    private static int getBit(int bits, int index) {
        return (bits >> (7 - index)) & 1;
    }

    /**
     * Retrieves a nibble (4 bits) from the lowest order byte of the given integer.
     *
     * @param nibbles the source integer, treated as an unsigned byte
     * @param index   the index of the nibble to retrieve, which must be in the range <tt>0..1</tt>.
     * @return the nibble at the specified index, as an unsigned byte.
     */
    private static int getNibble(int nibbles, int index) {
        return (nibbles >> (4 * (1 - index))) & 0xF;
    }

    /**
     * The decoded image read from the source input.
     *
     * @return the <tt>BufferedImage</tt> representing the BMP image.
     */
    public Image getImage() {
        return img;
    }

    /**
     * Reads the BMP info header structure from the given <tt>InputStream</tt>.
     *
     * @param lis the <tt>InputStream</tt> to read
     * @return the <tt>InfoHeader</tt> structure
     * @throws java.io.IOException if an error occurred
     */
    public static InfoHeader readInfoHeader(net.sf.image4j.io.LittleEndianInputStream lis) throws IOException {
        return new InfoHeader(lis);
    }

    /**
     * @since 0.6
     */
    public static InfoHeader readInfoHeader(net.sf.image4j.io.LittleEndianInputStream lis, int infoSize) throws IOException {
        return new InfoHeader(lis, infoSize);
    }

    /**
     * Reads the BMP data from the given <tt>InputStream</tt> using the information
     * contained in the <tt>InfoHeader</tt>.
     *
     * @param lis        the source input
     * @param infoHeader an <tt>InfoHeader</tt> that was read by a call to
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}.
     * @return the decoded image read from the source input
     * @throws java.io.IOException if an error occurs
     */
    public static IntImage read(InfoHeader infoHeader, net.sf.image4j.io.LittleEndianInputStream lis) throws IOException {

        /* Color table (palette) */
        int[] colorTable = null;

        // color table is only present for 1, 4 or 8 bit (indexed) images
        if (infoHeader.sBitCount <= 8) {
            colorTable = readColorTable(infoHeader, lis);
        }

        return read(infoHeader, lis, colorTable);

    }

    /**
     * Reads the BMP data from the given <tt>InputStream</tt> using the information
     * contained in the <tt>InfoHeader</tt>.
     *
     * @param colorTable <tt>ColorEntry</tt> array containing palette
     * @param infoHeader an <tt>InfoHeader</tt> that was read by a call to
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}.
     * @param lis        the source input
     * @return the decoded image read from the source input
     * @throws java.io.IOException if any error occurs
     */
    public static IntImage read(InfoHeader infoHeader, LittleEndianInputStream lis, int[] colorTable) throws IOException {
        if (infoHeader.sBitCount == 1 && infoHeader.iCompression == BMPConstants.BI_RGB) {// 1-bit (monochrome) uncompressed
            return read1(infoHeader, lis, colorTable);
        } else if (infoHeader.sBitCount == 4 && infoHeader.iCompression == BMPConstants.BI_RGB) {// 4-bit uncompressed
            return read4(infoHeader, lis, colorTable);
        } else if (infoHeader.sBitCount == 8 && infoHeader.iCompression == BMPConstants.BI_RGB) { // 8-bit uncompressed
            return read8(infoHeader, lis, colorTable);
        } else if (infoHeader.sBitCount == 24 && infoHeader.iCompression == BMPConstants.BI_RGB) {// 24-bit uncompressed
            return read24(infoHeader, lis);
        } else if (infoHeader.sBitCount == 32 && infoHeader.iCompression == BMPConstants.BI_RGB) {// 32bit uncompressed
            return read32(infoHeader, lis);
        } else {
            throw new IOException("Unrecognized bitmap format: bit count=" + infoHeader.sBitCount + ", compression=" +
                    infoHeader.iCompression);
        }
    }

    /**
     * Reads the <tt>ColorEntry</tt> table from the given <tt>InputStream</tt> using
     * the information contained in the given <tt>infoHeader</tt>.
     *
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}
     * @param lis        the <tt>InputStream</tt> to read
     * @return the decoded image read from the source input
     * @throws java.io.IOException if an error occurs
     */
    public static int[] readColorTable(InfoHeader infoHeader, LittleEndianInputStream lis) throws IOException {
        int[] colorTable = new int[infoHeader.iNumColors];
        for (int i = 0; i < infoHeader.iNumColors; i++) {
            colorTable[i] = lis.readIntLE();
        }
        return colorTable;
    }

    /**
     * Reads 1-bit uncompressed bitmap raster data, which may be monochrome depending on the
     * palette entries in <tt>colorTable</tt>.
     *
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}
     * @param lis        the source input
     * @param colors     int[] specifying the palette, which
     *                   must not be <tt>null</tt>.
     * @return the decoded image read from the source input
     * @throws java.io.IOException if an error occurs
     */
    public static IntImage read1(InfoHeader infoHeader, LittleEndianInputStream lis, int[] colors) throws IOException {
        //1 bit per pixel or 8 pixels per byte
        //each pixel specifies the palette index

        // padding
        int bitsPerLine = infoHeader.iWidth;
        if (bitsPerLine % 32 != 0) {
            bitsPerLine = (bitsPerLine / 32 + 1) * 32;
        }

        int bytesPerLine = bitsPerLine / 8;

        int[] line = new int[bytesPerLine];
        int[] data = new int[infoHeader.iWidth * infoHeader.iHeight];
        for (int y = infoHeader.iHeight - 1; y >= 0; y--) {
            for (int i = 0; i < bytesPerLine; i++) {
                line[i] = lis.readUnsignedByte();
            }

            int ctr = y * infoHeader.iWidth;
            for (int x = 0; x < infoHeader.iWidth; x++) {
                int i = x >> 3;
                int v = line[i];
                int b = x & 7;
                int index = getBit(v, b);
                // img.setRGB(x, y, rgb);
                // set the sample (colour index) for the pixel
                data[ctr++] = colors[index];
            }
        }

        return new IntImage(infoHeader.iWidth, infoHeader.iHeight, data, false);
    }

    /**
     * Reads 4-bit uncompressed bitmap raster data, which is interpreted based on the colours
     * specified in the palette.
     *
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}
     * @param lis        the source input
     * @param colors     int[] specifying the palette, which
     *                   must not be <tt>null</tt>.
     * @return the decoded image read from the source input
     * @throws java.io.IOException if an error occurs
     */
    public static IntImage read4(InfoHeader infoHeader, LittleEndianInputStream lis, int[] colors) throws IOException {

        // 2 pixels per byte or 4 bits per pixel.
        // Colour for each pixel specified by the color index in the palette.

        //padding
        int bitsPerLine = infoHeader.iWidth * 4;
        if (bitsPerLine % 32 != 0) {
            bitsPerLine = (bitsPerLine / 32 + 1) * 32;
        }
        int bytesPerLine = bitsPerLine / 8;

        int[] line = new int[bytesPerLine];
        int[] data = new int[infoHeader.iWidth * infoHeader.iHeight];
        for (int y = infoHeader.iHeight - 1; y >= 0; y--) {

            // scan line
            for (int i = 0; i < bytesPerLine; i++) {
                int b = lis.readUnsignedByte();
                line[i] = b;
            }

            // get pixels
            int ctr = y * infoHeader.iWidth;
            for (int x = 0; x < infoHeader.iWidth; x++) {
                //get byte index for line
                int b = x >> 1; // 2 pixels per byte
                int i = x & 1;
                int n = line[b];
                int index = getNibble(n, i);
                data[ctr++] = colors[index];
            }
        }

        return new IntImage(infoHeader.iWidth, infoHeader.iHeight, data, false);
    }

    /**
     * Reads 8-bit uncompressed bitmap raster data, which is interpreted based on the colours
     * specified in the palette.
     *
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}
     * @param lis        the source input
     * @param colors int[] specifying the palette, which
     *                   must not be <tt>null</tt>.
     * @return the decoded image read from the source input
     * @throws java.io.IOException if an error occurs
     */
    public static IntImage read8(InfoHeader infoHeader, LittleEndianInputStream lis, int[] colors) throws IOException {

        // 1 byte per pixel
        //  color index 1 (index of color in palette)
        // lines padded to nearest 32bits
        // no alpha

        //padding
        int dataPerLine = infoHeader.iWidth;
        int bytesPerLine = dataPerLine;
        if (bytesPerLine % 4 != 0) {
            bytesPerLine = (bytesPerLine / 4 + 1) * 4;
        }
        int padBytesPerLine = bytesPerLine - dataPerLine;

        int[] data = new int[infoHeader.iWidth * infoHeader.iHeight];
        for (int y = infoHeader.iHeight - 1; y >= 0; y--) {
            int ctr = y * infoHeader.iWidth;
            for (int x = 0; x < infoHeader.iWidth; x++) {
                int b = lis.readUnsignedByte();
                data[ctr++] = colors[b];
            }
            lis.skip(padBytesPerLine);
        }

        return new IntImage(infoHeader.iWidth, infoHeader.iHeight, data, false);
    }

    /**
     * Reads 24-bit uncompressed bitmap raster data.
     *
     * @param lis        the source input
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}
     * @return the decoded image read from the source input
     * @throws java.io.IOException if an error occurs
     */
    public static IntImage read24(InfoHeader infoHeader, LittleEndianInputStream lis) throws IOException {

        // 3 bytes per pixel
        //  blue 1
        //  green 1
        //  red 1
        // lines padded to nearest 32 bits
        // no alpha

        // padding to nearest 32 bits
        int dataPerLine = infoHeader.iWidth * 3;
        int bytesPerLine = dataPerLine;
        if (bytesPerLine % 4 != 0) {
            bytesPerLine = (bytesPerLine / 4 + 1) * 4;
        }
        int padBytesPerLine = bytesPerLine - dataPerLine;

        int[] data = new int[infoHeader.iHeight * infoHeader.iWidth];
        for (int y = infoHeader.iHeight - 1; y >= 0; y--) {
            int ctr = y * infoHeader.iWidth;
            for (int x = 0; x < infoHeader.iWidth; x++) {
                int b = lis.readUnsignedByte();
                int g = lis.readUnsignedByte();
                int r = lis.readUnsignedByte();
                data[ctr++] = (r << 16) | (g << 8) | b;
            }
            lis.skip(padBytesPerLine);
        }

        return new IntImage(infoHeader.iWidth, infoHeader.iHeight, data, false);

    }


    /**
     * Reads 32-bit uncompressed bitmap raster data, with transparency.
     *
     * @param lis        the source input
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}
     * @return the decoded image read from the source input
     * @throws java.io.IOException if an error occurs
     */
    public static IntImage read32(InfoHeader infoHeader, LittleEndianInputStream lis) throws IOException {
        // 4 bytes per pixel
        // blue 1
        // green 1
        // red 1
        // alpha 1
        // No padding since each pixel = 32 bits

        int[] data = new int[infoHeader.iWidth * infoHeader.iHeight];
        for (int y = infoHeader.iHeight - 1; y >= 0; y--) {
            int ctr = y * infoHeader.iWidth;
            for (int x = 0; x < infoHeader.iWidth; x++) {
                data[ctr++] = lis.readIntLE();// bgra
            }
        }

        return new IntImage(infoHeader.iWidth, infoHeader.iHeight, data, true);
    }

    /**
     * Reads and decodes BMP data from the source input.
     *
     * @param in the source input
     * @return the decoded image read from the source file
     * @throws java.io.IOException if an error occurs
     */
    public static Image read(InputStream in) throws IOException {
        BMPDecoder d = new BMPDecoder(in);
        return d.getImage();
    }

}