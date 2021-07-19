/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * blablabla,
 *
 * I am trying to support everything, so I'll be extending it
 */
package me.anno.image.tar;

import me.anno.image.Image;
import me.anno.io.BufferedIO;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Mark Powell
 * @author Joshua Slack - cleaned, commented, added ability to read 16bit true color and color-mapped TGAs.
 * @author Kirill Vainer - ported to jME3
 * @author Antonio Noack - added black & white support; fixed naming (?), tested with crytek sponza; fixed 32 bit color order(?)
 * at least for my test cases, everything was correct, and the same as Gimp
 * @version $Id: TGALoader.java 4131 2009-03-19 20:15:28Z blaine.dev $
 */
public class TGAImage extends Image {

    public int channels;
    public int originalImageType, originalPixelDepth;

    // bgra, even if the implementation calls it rgba
    public byte[] data;

    public TGAImage(byte[] data, int width, int height, int channels) {
        this.data = data;
        this.width = width;
        this.height = height;
        this.channels = channels;
    }

    private static int rgba(int b, int g, int r, int a) {
        return ((r & 255) << 16) | ((g & 255) << 8) | (b & 255) | ((a & 255) << 24);
    }

    @Override
    public BufferedImage createBufferedImage() {
        BufferedImage image = new BufferedImage(width, height, channels > 3 ? 2 : 1);
        DataBuffer buffer = image.getRaster().getDataBuffer();
        for (int y = 0, i = 0, j = 0; y < height; y++) {
            for (int x = 0; x < width; x++, i++, j += channels) {
                int color;
                switch (channels) {
                    case 1:
                        color = 0x10101 * (int) data[j];
                        break;
                    case 3:
                        color = rgba(data[j], data[j + 1], data[j + 2], 255);
                        break;
                    case 4:
                        color = rgba(data[j], data[j + 1], data[j + 2], data[j + 3]);
                        break;
                    default:
                        throw new RuntimeException("" + channels + " channels?");
                }
                buffer.setElem(i, color);
            }
        }
        return image;
    }


    // 0 - no image data in file
    public static final int TYPE_NO_IMAGE = 0;
    // 1 - uncompressed, color-mapped image
    public static final int TYPE_COLORMAPPED = 1;
    // 2 - uncompressed, true-color image
    public static final int TYPE_TRUECOLOR = 2;
    // 3 - uncompressed, black and white image
    public static final int TYPE_BLACKANDWHITE = 3;
    // 9 - run-length encoded, color-mapped image
    public static final int TYPE_COLORMAPPED_RLE = 9;
    // 10 - run-length encoded, true-color image
    public static final int TYPE_TRUECOLOR_RLE = 10;
    // 11 - run-length encoded, black and white image
    public static final int TYPE_BLACKANDWHITE_RLE = 11;

    /**
     * <code>loadImage</code> is a manual image loader which is entirely
     * independent of AWT. OUT: RGB888 or RGBA8888 Image object
     *
     * @param in   InputStream of an uncompressed 24b RGB or 32b RGBA TGA
     * @param flip Flip the image vertically
     * @return <code>Image</code> object that contains the
     * image, either as a R8, a RGB888 or RGBA8888
     * @throws java.io.IOException if an I/O error occurs
     */
    public static TGAImage read(InputStream in, boolean flip) throws IOException {
        boolean flipH = false;

        // open a stream to the file
        DataInputStream dis = new DataInputStream(BufferedIO.INSTANCE.useBuffered(in));

        // ---------- Start Reading the TGA header ---------- //
        // length of the image id (1 byte)
        int idLength = dis.readUnsignedByte();

        // Type of color map (if any) included with the image
        // 0 - no color map data is included
        // 1 - a color map is included
        int colorMapType = dis.readUnsignedByte();

        // Type of image being read:
        final int imageType = dis.readUnsignedByte();

        // Read Color Map Specification (5 bytes)
        // Index of first color map entry (if we want to use it, uncomment and remove extra read.)
        // short cMapStart = flipEndian(dis.readShort());
        dis.readShort();
        // number of entries in the color map
        short cMapLength = flipEndian(dis.readShort());
        // number of bits per color map entry
        int cMapDepth = dis.readUnsignedByte();

        // Read Image Specification (10 bytes)
        // horizontal coordinate of lower left corner of image. (if we want to use it, uncomment and remove extra read.)
        // int xOffset = flipEndian(dis.readShort());
        dis.readShort();
        // vertical coordinate of lower left corner of image. (if we want to use it, uncomment and remove extra read.)
        // int yOffset = flipEndian(dis.readShort());
        dis.readShort();
        // width of image - in pixels
        int width = flipEndian(dis.readShort());
        // height of image - in pixels
        int height = flipEndian(dis.readShort());
        // bits per pixel in image.
        int pixelDepth = dis.readUnsignedByte();
        int imageDescriptor = dis.readUnsignedByte();
        if ((imageDescriptor & 32) != 0) // bit 5 : if 1, flip top/bottom ordering
        {
            flip = !flip;
        }
        if ((imageDescriptor & 16) != 0) // bit 4 : if 1, flip left/right ordering
        {
            flipH = !flipH;
        }

        // ---------- Done Reading the TGA header ---------- //

        // Skip image ID
        if (idLength > 0) {
            dis.skipBytes(idLength);
        }

        int[] cMapEntries = null;
        if (colorMapType != 0) {

            // read the color map
            int bytesInColorMap = (cMapDepth * cMapLength) >> 3;
            int bitsPerColor = Math.min(cMapDepth / 3, 8);

            byte[] cMapData = new byte[bytesInColorMap];
            dis.readFully(cMapData, 0, bytesInColorMap);

            ColorMapEntry entry = new ColorMapEntry();
            // Only go to the trouble of constructing the color map
            // table if this is declared a color mapped image.
            if (imageType == TYPE_COLORMAPPED || imageType == TYPE_COLORMAPPED_RLE) {
                cMapEntries = new int[cMapLength];
                int alphaSize = cMapDepth - (3 * bitsPerColor);
                float scalar = 255f / ((1 << bitsPerColor) - 1);
                float alphaScalar = 255f / ((1 << alphaSize) - 1);
                int r, g, b, a = 255;
                for (int i = 0; i < cMapLength; i++) {
                    int offset = cMapDepth * i;
                    b = (int) (getBitsAsByte(cMapData, offset, bitsPerColor) * scalar);
                    g = (int) (getBitsAsByte(cMapData, offset + bitsPerColor, bitsPerColor) * scalar);
                    r = (int) (getBitsAsByte(cMapData, offset + (2 * bitsPerColor), bitsPerColor) * scalar);
                    if (alphaSize > 0) {
                        a = (int) (getBitsAsByte(cMapData, offset + (3 * bitsPerColor), alphaSize) * alphaScalar);
                    }
                    cMapEntries[i] = abgr(r, g, b, a);
                }
            }
        }


        // Allocate image data array
        int format;
        final int dl = pixelDepth == 32 ? 4 : imageType == TYPE_BLACKANDWHITE || imageType == TYPE_BLACKANDWHITE_RLE ? 1 : 3;
        final byte[] rawData = new byte[width * height * dl];

        switch (imageType) {
            case TYPE_TRUECOLOR:
                format = readTrueColor(pixelDepth, width, height, flip, rawData, dl, dis);
                break;
            case TYPE_TRUECOLOR_RLE:
                format = readTrueColorRLE(pixelDepth, width, height, flip, rawData, dl, dis);
                break;
            case TYPE_COLORMAPPED:
                format = readColorMapped(pixelDepth, width, height, flip, rawData, dl, dis, cMapEntries);
                break;
            case TYPE_NO_IMAGE:
                throw new IOException("No image is not supported");
            case TYPE_BLACKANDWHITE:
                format = readGrayscale(pixelDepth, width, height, flip, rawData, dl, dis);
                break;
            // throw new IOException("Black & White is not supported");
            case TYPE_COLORMAPPED_RLE:
                throw new IOException("Colormapped RLE is not supported");
            case TYPE_BLACKANDWHITE_RLE:
                throw new IOException("Black & White RLE is not supported");
            default:
                throw new IOException("Unknown TGA type " + imageType);
        }

        in.close();

        // Create the Image object
        TGAImage image = new TGAImage(rawData, width, height, format);
        image.originalImageType = imageType;
        image.originalPixelDepth = pixelDepth;
        return image;

    }

    private static int readColorMapped(int pixelDepth, int width, int height, boolean flip, byte[] rawData, int dl, DataInputStream dis, int[] cMapEntries) throws IOException {

        int rawDataIndex = 0;
        int bytesPerIndex = pixelDepth / 8;

        if (bytesPerIndex == 1) {
            for (int i = 0; i <= (height - 1); i++) {
                if (!flip) {
                    rawDataIndex = (height - 1 - i) * width * dl;
                }
                for (int j = 0; j < width; j++) {

                    int index = dis.readUnsignedByte();
                    if (index >= cMapEntries.length) {
                        throw new IOException("TGA: Invalid color map entry referenced: " + index);
                    }

                    int entry = cMapEntries[index];
                    rawData[rawDataIndex++] = (byte) (entry >> 16);
                    rawData[rawDataIndex++] = (byte) (entry >> 8);
                    rawData[rawDataIndex++] = (byte) entry;
                    if (dl == 4) {
                        rawData[rawDataIndex++] = (byte) (entry >>> 24);
                    }

                }
            }
        } else if (bytesPerIndex == 2) {
            for (int i = 0; i <= (height - 1); i++) {
                if (!flip) {
                    rawDataIndex = (height - 1 - i) * width * dl;
                }
                for (int j = 0; j < width; j++) {

                    int index = flipEndian(dis.readShort());
                    if (index >= cMapEntries.length || index < 0) {
                        throw new IOException("TGA: Invalid color map entry referenced: " + index);
                    }

                    int entry = cMapEntries[index];
                    rawData[rawDataIndex++] = (byte) (entry >> 16);
                    rawData[rawDataIndex++] = (byte) (entry >> 8);
                    rawData[rawDataIndex++] = (byte) entry;
                    if (dl == 4) {
                        rawData[rawDataIndex++] = (byte) (entry >> 24);
                    }

                }
            }
        } else {
            throw new IOException("TGA: unknown colormap indexing size used: " + bytesPerIndex);
        }

        return dl == 4 ? 4 : 3;

    }

    private static int readTrueColor(int pixelDepth, int width, int height, boolean flip, byte[] rawData, int dl, DataInputStream dis) throws IOException {

        int rawDataIndex = 0;

        byte b, g, r, a;

        // Faster than doing a 16-or-24-or-32 check on each individual pixel,
        // just make a separate loop for each.
        if (pixelDepth == 16) {
            byte[] data = new byte[2];
            float scalar = 255f / 31f;
            for (int i = 0; i <= (height - 1); i++) {
                if (!flip) {
                    rawDataIndex = (height - 1 - i) * width * dl;
                }
                for (int j = 0; j < width; j++) {
                    data[1] = dis.readByte();
                    data[0] = dis.readByte();
                    rawData[rawDataIndex++] = (byte) (int) (getBitsAsByte(data, 1, 5) * scalar);
                    rawData[rawDataIndex++] = (byte) (int) (getBitsAsByte(data, 6, 5) * scalar);
                    rawData[rawDataIndex++] = (byte) (int) (getBitsAsByte(data, 11, 5) * scalar);
                    if (dl == 4) {
                        // create an alpha channel
                        a = getBitsAsByte(data, 0, 1);
                        if (a == 1) {
                            a = (byte) 255;
                        }
                        rawData[rawDataIndex++] = a;
                    }
                }
            }

            return dl == 4 ? 4 : 3;
        } else if (pixelDepth == 24) {
            for (int y = 0; y < height; y++) {
                if (!flip) {
                    rawDataIndex = (height - 1 - y) * width * dl;
                } else {
                    rawDataIndex = y * width * dl;
                }

                dis.readFully(rawData, rawDataIndex, width * dl);
//                    for (int x = 0; x < width; x++) {
                //read scanline
//                        blue = dis.readByte();
//                        green = dis.readByte();
//                        red = dis.readByte();
//                        rawData[rawDataIndex++] = red;
//                        rawData[rawDataIndex++] = green;
//                        rawData[rawDataIndex++] = blue;
//                    }
            }
            return 3;
        } else if (pixelDepth == 32) {
            for (int i = 0; i <= (height - 1); i++) {
                if (!flip) {
                    rawDataIndex = (height - 1 - i) * width * dl;
                }

                for (int j = 0; j < width; j++) {
                    b = dis.readByte();
                    g = dis.readByte();
                    r = dis.readByte();
                    a = dis.readByte();
                    rawData[rawDataIndex++] = b;
                    rawData[rawDataIndex++] = g;
                    rawData[rawDataIndex++] = r;
                    rawData[rawDataIndex++] = a;
                }
            }
            return 4;
        } else {
            throw new IOException("Unsupported TGA true color depth: " + pixelDepth);
        }

    }

    private static int readGrayscale(int pixelDepth, int width, int height, boolean flip, byte[] rawData, int dl, DataInputStream dis) throws IOException {

        int rawDataIndex = 0;

        byte v;

        for (int i = 0; i <= (height - 1); i++) {
            if (!flip) {
                rawDataIndex = (height - 1 - i) * width * dl;
            }

            for (int j = 0; j < width; j++) {
                v = dis.readByte();
                rawData[rawDataIndex++] = v;
                if (pixelDepth >= 16) rawData[rawDataIndex++] = v;
                if (pixelDepth >= 24) rawData[rawDataIndex++] = v;
                if (pixelDepth >= 32) rawData[rawDataIndex++] = (byte) 255;
            }
        }

        return pixelDepth / 8;

    }

    private static int readTrueColorRLE(int pixelDepth, int width, int height, boolean flip, byte[] rawData, int dl, DataInputStream dis) throws IOException {

        int format;
        int rawDataIndex = 0;

        byte b, g, r, a;


        // Faster than doing a 16-or-24-or-32 check on each individual pixel,
        // just make a separate loop for each.
        if (pixelDepth == 32) {
            for (int i = 0; i <= (height - 1); ++i) {
                if (!flip) {
                    rawDataIndex = (height - 1 - i) * width * dl;
                }

                for (int j = 0; j < width; ++j) {
                    // Get the number of pixels the next chunk covers (either packed or unpacked)
                    int count = dis.readByte();
                    if ((count & 0x80) != 0) {
                        // Its an RLE packed block - use the following 1 pixel for the next <count> pixels
                        count &= 0x07f;
                        j += count;
                        b = dis.readByte();
                        g = dis.readByte();
                        r = dis.readByte();
                        a = dis.readByte();
                        while (count-- >= 0) {
                            rawData[rawDataIndex++] = b;
                            rawData[rawDataIndex++] = g;
                            rawData[rawDataIndex++] = r;
                            rawData[rawDataIndex++] = a;
                        }
                    } else {
                        // It's not RLE packed, but the next <count> pixels are raw.
                        j += count;
                        while (count-- >= 0) {
                            b = dis.readByte();
                            g = dis.readByte();
                            r = dis.readByte();
                            a = dis.readByte();
                            rawData[rawDataIndex++] = b;
                            rawData[rawDataIndex++] = g;
                            rawData[rawDataIndex++] = r;
                            rawData[rawDataIndex++] = a;
                        }
                    }
                }
            }
            format = 4;
        } else if (pixelDepth == 24) {
            for (int i = 0; i <= (height - 1); i++) {
                if (!flip) {
                    rawDataIndex = (height - 1 - i) * width * dl;
                }
                for (int j = 0; j < width; ++j) {
                    // Get the number of pixels the next chunk covers (either packed or unpacked)
                    int count = dis.readByte();
                    if ((count & 0x80) != 0) {
                        // Its an RLE packed block - use the following 1 pixel for the next <count> pixels
                        count &= 0x07f;
                        j += count;
                        r = dis.readByte();
                        g = dis.readByte();
                        b = dis.readByte();
                        while (count-- >= 0) {
                            rawData[rawDataIndex++] = b;
                            rawData[rawDataIndex++] = g;
                            rawData[rawDataIndex++] = r;
                        }
                    } else {
                        // It's not RLE packed, but the next <count> pixels are raw.
                        j += count;
                        while (count-- >= 0) {
                            r = dis.readByte();
                            g = dis.readByte();
                            b = dis.readByte();
                            rawData[rawDataIndex++] = b;
                            rawData[rawDataIndex++] = g;
                            rawData[rawDataIndex++] = r;
                        }
                    }
                }
            }
            format = 3;
        } else if (pixelDepth == 16) {
            byte[] data = new byte[2];
            float scalar = 255f / 31f;
            for (int i = 0; i <= (height - 1); i++) {
                if (!flip) {
                    rawDataIndex = (height - 1 - i) * width * dl;
                }
                for (int j = 0; j < width; j++) {
                    // Get the number of pixels the next chunk covers (either packed or unpacked)
                    int count = dis.readByte();
                    if ((count & 0x80) != 0) {
                        // Its an RLE packed block - use the following 1 pixel for the next <count> pixels
                        count &= 0x07f;
                        j += count;
                        data[1] = dis.readByte();
                        data[0] = dis.readByte();
                        r = (byte) (int) (getBitsAsByte(data, 1, 5) * scalar);
                        g = (byte) (int) (getBitsAsByte(data, 6, 5) * scalar);
                        b = (byte) (int) (getBitsAsByte(data, 11, 5) * scalar);
                        while (count-- >= 0) {
                            rawData[rawDataIndex++] = b;
                            rawData[rawDataIndex++] = g;
                            rawData[rawDataIndex++] = r;
                        }
                    } else {
                        // It's not RLE packed, but the next <count> pixels are raw.
                        j += count;
                        while (count-- >= 0) {
                            data[1] = dis.readByte();
                            data[0] = dis.readByte();
                            r = (byte) (int) (getBitsAsByte(data, 1, 5) * scalar);
                            g = (byte) (int) (getBitsAsByte(data, 6, 5) * scalar);
                            b = (byte) (int) (getBitsAsByte(data, 11, 5) * scalar);
                            rawData[rawDataIndex++] = b;
                            rawData[rawDataIndex++] = g;
                            rawData[rawDataIndex++] = r;
                        }
                    }
                }
            }
            format = 3;
        } else {
            throw new IOException("Unsupported TGA true color depth: " + pixelDepth);
        }

        return format;

    }

    private static byte getBitsAsByte(byte[] data, int offset, int length) {
        int offsetBytes = offset / 8;
        int indexBits = offset % 8;
        int rVal = 0;

        // start at data[offsetBytes]...  spill into next byte as needed.
        for (int i = length; --i >= 0; ) {
            byte b = data[offsetBytes];
            int test = indexBits == 7 ? 1 : 2 << (6 - indexBits);
            if ((b & test) != 0) {
                if (i == 0) {
                    rVal++;
                } else {
                    rVal += (2 << i - 1);
                }
            }
            indexBits++;
            if (indexBits == 8) {
                indexBits = 0;
                offsetBytes++;
            }
        }

        return (byte) rVal;
    }

    /**
     * <code>flipEndian</code> is used to flip the endian bit of the header
     * file.
     *
     * @param signedShort the bit to flip.
     * @return the flipped bit.
     */
    private static short flipEndian(short signedShort) {
        int input = signedShort & 0xFFFF;
        return (short) (input << 8 | (input & 0xFF00) >>> 8);
    }

    private static int abgr(int r, int g, int b, int a) {
        return ((b & 255) << 16) | ((g & 255) << 8) | (r & 255) | ((a & 255) << 24);
    }

    static class ColorMapEntry {

        byte b, g, r, a;

        int getABGR() {
            return (((int) b & 255) << 16) | (((int) g & 255) << 8) | ((int) r & 255) | (((int) a & 255) << 24);
        }

        @Override
        public String toString() {
            return "entry: " + b + "," + g + "," + r + "," + a;
        }
    }
}
