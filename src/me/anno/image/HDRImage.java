package me.anno.image;

import me.anno.gpu.texture.Texture2D;
import me.anno.io.files.FileReference;

import java.io.*;

// src/author: https://github.com/aicp7/HDR_file_readin
// modified for our needs
// This class is used to convert a HDR format image
// into a three-dimensional float array representing the RGB channels of the original image.
public class HDRImage extends Image {

    private float[] pixels;

    public HDRImage(int width, int height, int numChannels) {
        super(numChannels, numChannels > 3);
        this.width = width;
        this.height = height;
        this.pixels = new float[width * height * numChannels];
    }

    public HDRImage(InputStream input) throws IOException {
        super(3, false);
        try (InputStream in = optimizeStream(input)) {
            read(in);
        }
    }

    public HDRImage(FileReference file) throws IOException {
        super(3, false);
        try (InputStream in = optimizeStream(file.inputStream())) {
            read(in);
        }
    }

    public HDRImage(File file) throws IOException {
        super(3, false);
        try (InputStream in = optimizeStream(new FileInputStream(file))) {
            read(in);
        }
    }

    public InputStream optimizeStream(InputStream input) {
        return input instanceof BufferedInputStream ||
                input instanceof ByteArrayInputStream ? input : new BufferedInputStream(input);
    }

    @Override
    public boolean hasAlphaChannel() {
        return false;
    }

    private static int rgb(int r, int g, int b) {
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public int getRGB(int index) {
        int i0 = index * 3;
        float delta = typicalBrightness;
        float r = pixels[i0] * delta;
        float g = pixels[i0 + 1] * delta;
        float b = pixels[i0 + 2] * delta;
        // reinhard tonemapping
        r = r / (r + 1f) * 255f;
        g = g / (g + 1f) * 255f;
        b = b / (b + 1f) * 255f;
        return rgb((int) r, (int) g, (int) b);
    }

    // with the reinhard tonemapping, the average brightness of pixels is expected to be
    // more than just 1, and more like 5
    public static float typicalBrightness = 5f;

    @Override
    public void createTexture(Texture2D texture, boolean checkRedundancy) {
        texture.createRGB(pixels, checkRedundancy);
    }

    // Construction method if the input is a InputStream.
    // Parse the HDR file by its format. HDR format encode can be seen in Radiance HDR(.pic,.hdr) file format
    private void read(InputStream in) throws IOException {
        // Parse HDR file's header line
        // readLine(InputStream in) method will be introduced later.

        // The first line of the HDR file. If it is a HDR file, the first line should be "#?RADIANCE"
        // If not, we will throw a IllegalArgumentException.
        String isHDR = readLine(in);
        if (!isHDR.equals("#?RADIANCE")) throw new IllegalArgumentException("Unrecognized format: " + isHDR);

        // Besides the first line, there are serval lines describe the different information of this HDR file.
        // Maybe it will have the exposure time, format(Must be either"32-bit_rle_rgbe" or "32-bit_rle_xyze")
        // Also the owner's information, the software's version, etc.

        // The above information is not so important for us.
        // The only important information for us is the Resolution which shows the size of the HDR image
        // The resolution information's format is fixed. Usually, it will be -Y 1024 +X 2048 something like this.
        String inform = readLine(in);
        while (!inform.equals("")) {
            inform = readLine(in);
        }

        inform = readLine(in);
        String[] tokens = inform.split(" ", 4);
        if (tokens[0].charAt(1) == 'Y') {
            width = Integer.parseInt(tokens[3]);
            height = Integer.parseInt(tokens[1]);
        } else {
            width = Integer.parseInt(tokens[1]);
            height = Integer.parseInt(tokens[3]);
        }

        if (width <= 0) throw new IllegalArgumentException("HDR Width must be positive");
        if (height <= 0) throw new IllegalArgumentException("HDR Height must be positive");

        // In the above, the basic information has been collected. Now, we will deal with the pixel data.
        // According to the HDR format document, each pixel is stored as 4 bytes, one bytes mantissa for each r,g,b and a shared one byte exponent.
        // The pixel data may be stored uncompressed or using a straightforward run length encoding scheme.

        DataInput din = new DataInputStream(in);

        pixels = new float[height * width * 3];

        // optimized from the original; it does not need to be full image size; one row is enough
        // besides it only needs 8 bits of space per component, not 32
        // effectively this halves the required RAM for this program part
        byte[] lineBuffer = new byte[width * 4];
        int index = 0;

        // We read the information row by row. In each row, the first four bytes store the column number information.
        // The first and second bytes store "2". And the third byte stores the higher 8 bits of col num, the fourth byte stores the lower 8 bits of col num.
        // After these four bytes, these are the real pixel data.
        for (int y = 0; y < height; y++) {
            // The following code patch is checking whether the hdr file is compressed by run length encode(RLE).
            // For every line of the data part, the first and second byte should be 2(DEC).
            // The third*2^8+the fourth should equals to the width. They combined the width information.
            // For every line, we need check this kind of informatioin. And the starting four nums of every line is the same
            int a = din.readUnsignedByte();
            int b = din.readUnsignedByte();
            int c = din.readUnsignedByte();
            int d = din.readUnsignedByte();
            if (a != 2 || b != 2)
                throw new IllegalArgumentException("Only HDRs with run length encoding are supported.");
            if (((c << 8) + d) != width)
                throw new IllegalArgumentException("Width-Checksum is incorrect. Is this file a true HDR?");

            // This inner loop is for the four channels. The way they compressed the data is in this way:
            // Firstly, they compressed a row.
            // Inside that row, they firstly compressed the red channel information. If there are duplicate data, they will use RLE to compress.
            // First data shows the numbers of duplicates(which should minus 128), and the following data is the duplicate one.
            // If there is no duplicate, they will store the information in order.
            // And the first data is the number of how many induplicate items, and the following data stream is their associated data.
            for (int channel = 0; channel < 4; channel++) { // This loop controls the four channel. R,G,B and Exp.
                int x4 = channel;
                int w4 = width * 4 + channel;
                while (x4 < w4) {// alternative for x
                    int sequenceLength = din.readUnsignedByte();
                    if (sequenceLength > 128) {// copy-paste data; always the same
                        sequenceLength -= 128;
                        byte value = (byte) din.readUnsignedByte();
                        while (sequenceLength-- > 0) {
                            lineBuffer[x4] = value;
                            x4 += 4;
                        }
                    } else {// unique data for sequence length positions
                        while (sequenceLength-- > 0) {
                            lineBuffer[x4] = (byte) din.readUnsignedByte();
                            x4 += 4;
                        }
                    }
                }
            }

            for (int x = 0; x < width; x++) {
                int i2 = x * 4;
                int exp = lineBuffer[i2 + 3] & 255;
                if (exp == 0) {
                    index += 3;// 0 is default
                } else {
                    float exponent = (float) Math.pow(2, exp - 128 - 8);
                    pixels[index++] = (lineBuffer[i2] & 255) * exponent;
                    pixels[index++] = (lineBuffer[i2 + 1] & 255) * exponent;
                    pixels[index++] = (lineBuffer[i2 + 2] & 255) * exponent;
                }
            }
        }
    }

    private String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        for (int i = 0; ; i++) {
            int b = in.read();
            if (b == '\n' || b == -1) {
                break;
            } else if (i == 500) {// 100 seems short and unsure ;)
                throw new IllegalArgumentException("Line too long");
            } else if (b != '\r') {
                bout.write(b);
            }
        }
        return bout.toString();
    }

}