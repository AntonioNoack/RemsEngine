package me.anno.image.tga;

import org.lwjgl.system.MemoryUtil;

import java.io.*;
import java.nio.IntBuffer;

// from https://stackoverflow.com/a/5134660/4979303, matched to Rem's Studio
// http://paulbourke.net/dataformats/tga/
// little endian multi-byte integers: "low-order byte,high-order byte"
//          00,04 -> 04,00 -> 1024

public class TargaImage {

    public boolean hasAlpha;
    public int width, height;
    public IntBuffer pixels;

    public TargaImage(int w, int h, boolean hasAlpha, IntBuffer pixels) {
        this.width = w;
        this.height = h;
        this.hasAlpha = hasAlpha;
        this.pixels = pixels;
    }

    public static TargaImage getImage(String fileName) throws IOException {
        File f = new File(fileName);
        return decode(new FileInputStream(f));
    }

    public static TargaImage decode(InputStream stream) throws IOException {

        // Reading header bytes
        // buf[2] = image type code 0x02 = uncompressed BGR or BGRA
        // buf[12]+[13] = width
        // buf[14]+[15] = height
        // buf[16] = image pixel size 0x20=32bit, 0x18=24bit
        // buf[17] = Image Descriptor Byte=0x28 (00101000)=32bit/origin upperleft/non-interleaved

        for (int i = 0; i < 2; i++) {
            if (stream.read() < 0) throw new EOFException();
        }

        int encoding = stream.read();

        for (int i = 0; i < 9; i++) {
            if (stream.read() < 0) throw new EOFException();
        }

        int width = stream.read() + (stream.read() << 8);   // 00,04=1024
        int height = stream.read() + (stream.read() << 8);  // 40,02=576

        int pixelSize = stream.read();
        if (stream.read() < 0) throw new EOFException();

        int pixelCount = width * height;
        IntBuffer pixels = MemoryUtil.memAllocInt(pixelCount);
        int idx = 0;

        boolean hasAlpha = false;
        if (encoding == 0x02 && pixelSize == 0x20) { // uncompressed BGRA
            hasAlpha = true;
            while (pixelCount > 0) {
                int b = stream.read();
                int g = stream.read();
                int r = stream.read();
                int a = stream.read();
                int v = (a << 24) | (r << 16) | (g << 8) | b;
                pixels.put(v);
                pixelCount--;
            }
        } else if (encoding == 0x02 && pixelSize == 0x18) {  // uncompressed BGR
            while (pixelCount > 0) {
                int b = stream.read();
                int g = stream.read();
                int r = stream.read();
                int v = 0xff000000 | (r << 16) | (g << 8) | b;
                pixels.put(v);
                pixelCount--;
            }
        } else {
            // RLE compressed
            while (pixelCount > 0) {
                int nb = stream.read(); // num of pixels
                if ((nb & 0x80) == 0) { // 0x80=dec 128, bits 10000000
                    for (int i = 0; i <= nb; i++) {
                        int b = stream.read();
                        int g = stream.read();
                        int r = stream.read();
                        pixels.put(0xff000000 | (r << 16) | (g << 8) | b);
                    }
                } else {
                    nb &= 0x7f;
                    int b = stream.read();
                    int g = stream.read();
                    int r = stream.read();
                    int v = 0xff000000 | (r << 16) | (g << 8) | b;
                    for (int i = 0; i <= nb; i++){
                        pixels.put(v);
                    }
                }
                pixelCount -= nb + 1;
            }
        }

        pixels.position(0);

        return new TargaImage(width, height, hasAlpha, pixels);

    }
}