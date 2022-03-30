/*
 * ICODecoder.java
 *
 * Created on May 9, 2006, 9:31 PM
 *
 */

package net.sf.image4j.codec.ico;

import me.anno.image.Image;
import me.anno.image.raw.BIImage;
import me.anno.image.raw.IntImage;
import net.sf.image4j.codec.bmp.BMPDecoder;
import net.sf.image4j.codec.bmp.InfoHeader;
import net.sf.image4j.io.CountingInputStream;
import net.sf.image4j.io.EndianUtils;
import net.sf.image4j.io.LittleEndianInputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decodes images in ICO format.
 *
 * @author Ian McDonagh
 * Modified by Antonio Noack to read our own Image class, not BufferedImage
 */
public class ICODecoder {

    private static final Logger log = Logger.getLogger(ICODecoder.class.getName());

    private static final int PNG_MAGIC = 0x89504E47;
    private static final int PNG_MAGIC_LE = 0x474E5089;
    private static final int PNG_MAGIC2 = 0x0D0A1A0A;
    private static final int PNG_MAGIC2_LE = 0x0A1A0A0D;

    private ICODecoder() {
    }

    /**
     * Reads and decodes ICO data from the given source. The returned list of
     * images is in the order in which they appear in the source ICO data.
     *
     * @param is the source <tt>InputStream</tt> to read
     * @return the list of images decoded from the ICO data
     * @throws java.io.IOException if an error occurs
     */
    public static List<Image> read(InputStream is) throws IOException {
        // long t = System.currentTimeMillis();

        LittleEndianInputStream in = new LittleEndianInputStream(
                new CountingInputStream(is));

        // Reserved 2 byte =0
        in.readShortLE();
        // Type 2 byte =1
        in.readShortLE();
        // Count 2 byte Number of Icons in this file
        short sCount = in.readShortLE();

        // Entries Count * 16 list of icons
        IconEntry[] entries = new IconEntry[sCount];
        for (short s = 0; s < sCount; s++) {
            entries[s] = new IconEntry(in);
        }
        // Seems like we don't need this, but you never know!
        // entries = sortByFileOffset(entries);

        int i = 0;
        // images list of bitmap structures in BMP/PNG format
        List<Image> ret = new ArrayList<>(sCount);

        try {
            for (i = 0; i < sCount; i++) {
                // Make sure we're at the right file offset!
                int fileOffset = in.getCount();
                if (fileOffset != entries[i].iFileOffset) {
                    throw new IOException("Cannot read image #" + i
                            + " starting at unexpected file offset.");
                }
                int info = in.readIntLE();
                log.log(Level.FINE, "Image #" + i + " @ " + in.getCount()
                        + " info = " + EndianUtils.toInfoString(info));
                if (info == 40) {

                    // read XOR bitmap
                    // BMPDecoder bmp = new BMPDecoder(is);
                    InfoHeader infoHeader = BMPDecoder.readInfoHeader(in, info);
                    InfoHeader andHeader = new InfoHeader(infoHeader);
                    andHeader.iHeight = infoHeader.iHeight / 2;
                    InfoHeader xorHeader = new InfoHeader(infoHeader);
                    xorHeader.iHeight = andHeader.iHeight;

                    andHeader.sBitCount = 1;
                    andHeader.iNumColors = 2;

                    // for now, just read all the raster data (xor + and)
                    // and store as separate images

                    IntImage img = BMPDecoder.read(xorHeader, in);
                    // If we want to be sure we've decoded the XOR mask
                    // correctly,
                    // we can write it out as a PNG to a temp file here.
                    // try {
                    // File temp = File.createTempFile("image4j", ".png");
                    // ImageIO.write(xor, "png", temp);
                    // log.info("Wrote xor mask for image #" + i + " to "
                    // + temp.getAbsolutePath());
                    // } catch (Throwable ex) {
                    // }
                    // Or just add it to the output list:
                    // img.add(xor);

                    int[] andColorTable = new int[]{-1, 0};
                    if (infoHeader.sBitCount == 32) {

                        // transparency from alpha
                        // ignore bytes after XOR bitmap
                        int size = entries[i].iSizeInBytes;
                        int infoHeaderSize = infoHeader.iSize;
                        // data size = w * h * 4
                        int dataSize = xorHeader.iWidth * xorHeader.iHeight * 4;
                        int skip = size - infoHeaderSize - dataSize;
                        // int skip2 = entries[i].iFileOffset + size - in.getCount();

                        // ignore AND bitmap since alpha channel stores
                        // transparency

                        if (in.skip(skip, false) < skip && i < sCount - 1) {
                            throw new EOFException("Unexpected end of input");
                        }
                        // If we skipped less bytes than expected, the AND mask
                        // is probably badly formatted.
                        // If we're at the last/only entry in the file, silently
                        // ignore and continue processing...

                        // // read AND bitmap
                        // BufferedImage and = BMPDecoder.read(andHeader, in,
                        // andColorTable);
                        // this.img.add(and);

                    } else {

                        // replace the alpha with the next image
                        IntImage alphaData = BMPDecoder.read(andHeader, in, andColorTable);
                        int[] alphaData2 = alphaData.getData();
                        int[] data0 = img.getData();
                        System.arraycopy(img.getData(), 0, data0, 0, data0.length);
                        for (int j = 0, l = data0.length; j < l; j++) {
                            data0[j] = (data0[j] & 0xffffff) | (alphaData2[j] & 0xff000000);
                        }

                        img.setHasAlphaChannel(true);

                    }

                    // create ICOImage
                    ret.add(img);

                }
                // check for PNG magic header and that image height and width =
                // 0 = 256 -> Vista format
                else if (info == PNG_MAGIC_LE) {

                    int info2 = in.readIntLE();

                    if (info2 != PNG_MAGIC2_LE) {
                        throw new IOException(
                                "Unrecognized icon format for image #" + i);
                    }

                    IconEntry e = entries[i];
                    int size = e.iSizeInBytes - 8;
                    byte[] pngData = new byte[size];
                    in.readFully(pngData);
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    DataOutputStream dout = new DataOutputStream(bout);
                    dout.writeInt(PNG_MAGIC);
                    dout.writeInt(PNG_MAGIC2);
                    dout.write(pngData);
                    byte[] pngData2 = bout.toByteArray();
                    ByteArrayInputStream bin = new ByteArrayInputStream(pngData2);
                    ImageInputStream input = ImageIO.createImageInputStream(bin);
                    ImageReader reader = getPNGImageReader();
                    reader.setInput(input);
                    BufferedImage img = reader.read(0);

                    // create ICOImage
                    ret.add(new BIImage(img));

                } else {
                    throw new IOException(
                            "Unrecognized icon format for image #" + i);
                }
            }
        } catch (IOException ex) {
            throw new IOException("Failed to read image # " + i, ex);
        }

        // long t2 = System.currentTimeMillis();
        // System.out.println("Loaded ICO file in "+(t2 - t)+"ms");

        return ret;
    }

    private static ImageReader getPNGImageReader() {
        ImageReader ret = null;
        Iterator<ImageReader> itr = ImageIO.getImageReadersByFormatName("png");
        if (itr.hasNext()) {
            ret = itr.next();
        }
        return ret;
    }
}