package me.anno.image;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


// This class is used to convert a HDR format image into a three-dimensional float array representing the
// RGB channels of the original image.
public class HDRImage {

    private int width;
    private int height;

    private float[] pixels;

    // This two-dimensional float array is storing the luminance information of the pixel.
    // We use the YUV format to calculate the luminance by lum=0.299*R+0.587*G+0.114*B
    //private float[][] lum;

    //private float lummean, lummax, lummin;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float[] getPixelArray() {
        return pixels;
    }

    public HDRImage(File file) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file));) {
            read(in);
        }
    }

    //Construction method if the input is a InputStream.
    //Parse the HDR file by its format. HDR format encode can be seen in Radiance HDR(.pic,.hdr) file format
    private void read(InputStream in) throws IOException {
        //Parse HDR file's header line
        //readLine(InputStream in) method will be introduced later.

        //The first line of the HDR file. If it is a HDR file, the first line should be "#?RADIANCE"
        //If not, we will throw a IllegalArgumentException.
        String isHDR = readLine(in);
        if (!isHDR.equals("#?RADIANCE")) throw new IllegalArgumentException("Unrecognized format: " + isHDR);

        // Besides the first line, there are serval lines describe the different information of this HDR file.
        // Maybe it will have the exposure time, format(Must be either"32-bit_rle_rgbe" or "32-bit_rle_xyze")
        // Also the owner's information, the software's version, etc.

        //The above information is not so important for us.
        //The only important information for us is the Resolution which shows the size of the HDR image
        //The resolution information's format is fixed. Usually, it will be -Y 1024 +X 2048 something like this.
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

        System.out.println(width+" x "+height);
        if (width <= 0) throw new IllegalArgumentException("Width must be positive");
        if (height <= 0) throw new IllegalArgumentException("Height must be positive");

        // In the above, the basic information has been collected. Now, we will deal with the pixel data.
        // According to the HDR format document, each pixel is stored as 4 bytes, one bytes mantissa for each r,g,b and a shared one byte exponent.
        // The pixel data may be stored uncompressed or using a straightforward run length encoding scheme.

        DataInput din = new DataInputStream(in);
        int[][][] buffers = new int[height][width][4];

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
                throw new IllegalArgumentException("This hdr file is not made by RLE run length encoded ");
            if (((c << 8) + d) != width)
                throw new IllegalArgumentException("Wrong width information");

            // This inner loop is for the four channels. The way they compressed the data is in this way:
            // Firstly, they compressed a row.
            // Inside that row, they firstly compressed the red channel information. If there are duplicate data, they will use RLE to compress.
            // First data shows the numbers of duplicates(which should minus 128), and the following data is the duplicate one.
            // If there is no duplicate, they will store the information in order.
            // And the first data is the number of how many induplicate items, and the following data stream is their associated data.
            for (int rgbe = 0; rgbe < 4; rgbe++) { // This loop controls the four channel. R,G,B and Exp.
                for (int x = 0; x < width; ) {// This w controls the Wth col to readin.
                    int num = din.readUnsignedByte();
                    if (num > 128) {// This means the following one data is duplicate item.
                        int value = din.readUnsignedByte();
                        num -= 128;
                        while (num > 0) {
                            buffers[y][x++][rgbe] = value;
                            num--;
                        }
                    } else {// no duplicate case
                        while (num > 0) {
                            buffers[y][x++][rgbe] = din.readUnsignedByte();
                            num--;
                        }
                    }
                }
            }

        }

        // The above for loop is used to generated the four channel of each pixel. RGBE. The next patch of codes are used to generate float pixel values of each three channel by using the transition expression.
        /* The transition relationship between rgbe and HDR FP32(RGB) is as follows:
         * 1. From rgbe to FP 32 (RGB)   this relationship is used to input and decode the HDR file.
         * if(e==0) R=G=B=0.0;
         * else R=r*2^(e-128-8);
         *      G=g*2^(e-128-8);
         *      B=b*2^(e-128-8);
         *
         * 2. From FP32(RGB) to rgbe   This relationship is used to output and encode the HDR file.
         * v=max(R,G,B);
         * if(v<1e-32),r=g=b=0;
         * else  we present v as v=m*2^n(0.5<=m<=1)
         *       r=R*m*256.0/v;
         *       g=G*m*256.0/v;
         *       b=B*m*256.0/v;
         *       e=n+128;
         *
         *
         * pixels[][][] stores the FP32(RGB) information. pixels[i][j][0] stores the R channel.
         *
         * By the way, we need generate the luminance of each pixel. By using the expressing:
         * Y=0.299*R+0.587*G+0.114*B;
         */
        pixels = new float[height*width*4];
        //lum = new float[height][width];
        float lmax = 0.0F; // This float value is storing the max value of FP32 (RGB) data.
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int exp = buffers[y][x][3];
                if (exp == 0) {
                    pixels[index++] = 0;
                    pixels[index++] = 0;
                    pixels[index++] = 0;
                    // lum[y][x] = 0.0F;
                } else {
                    float exponent = (float) Math.pow(2, exp - 128 - 8);
                    pixels[index++] = buffers[y][x][0] * exponent;
                    pixels[index++] = buffers[y][x][1] * exponent;
                    pixels[index++] = buffers[y][x][2] * exponent;
                    // lum[y][x] = 0.299f * pixels[index-3] + 0.587f * pixels[index-2] + 0.114f * pixels[index-1];
                    /*if (lum[y][x] > lmax) {
                        lmax = lum[y][x];
                    }*/
                }
                pixels[index++] = 1;
            }
        }

        System.out.println("max: "+lmax);

        // The next step is normalize to 1; In the above loop, we already find the max value of the FP32(RGB) data.
        /*lummax = 0.0F;
        lummin = 1.0F;
        float lumsum = 0.0F;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                lum[i][j] /= lmax;
                if (lum[i][j] > lummax) {
                    lummax = lum[i][j];
                }
                if (lum[i][j] < lummin) {
                    lummin = lum[i][j];
                }
                lumsum += lum[i][j];
            }
        }
        lummean = lumsum / (height * width);*/
    }

    private String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        for (int i = 0; ; i++) {
            int b = in.read();
            if (b == '\n' || b == -1) {
                break;
            } else if (i == 100) {
                throw new IllegalArgumentException("Line too long");
            } else {
                bout.write(b);
            }
        }
        return new String(bout.toByteArray());
    }

}