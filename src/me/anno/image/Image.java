package me.anno.image;

import me.anno.cache.data.ICacheData;
import me.anno.gpu.texture.Texture2D;
import me.anno.image.raw.BIImage;
import me.anno.image.raw.IntImage;
import me.anno.io.files.FileReference;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public abstract class Image implements ICacheData {

    int numChannels;
    public boolean hasAlphaChannel;

    public Image(int width, int height, int numChannels, boolean hasAlphaChannel) {
        this.numChannels = numChannels;
        this.hasAlphaChannel = hasAlphaChannel;
        this.width = width;
        this.height = height;
    }

    public int width, height;

    public int getIndex(int x, int y) {
        x = Math.max(Math.min(x, width - 1), 0);
        y = Math.max(Math.min(y, height - 1), 0);
        return x + y * width;
    }

    public int getNumChannels() {
        return numChannels;
    }

    public void setHasAlphaChannel(boolean alpha) {
        hasAlphaChannel = alpha;
    }

    public boolean hasAlphaChannel() {
        return hasAlphaChannel;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public IntImage createIntImage() {
        int width = getWidth();
        int height = getHeight();
        int[] data = new int[width * height];
        IntImage image = new IntImage(width, height, data, hasAlphaChannel);
        for (int i = 0, size = width * height; i < size; i++) {
            data[i] = getRGB(i);
        }
        return image;
    }

    public BIImage createBImage(int width, int height) {
        return new BIImage(createBufferedImage(width, height));
    }

    public BufferedImage createBufferedImage() {
        int width = getWidth();
        int height = getHeight();
        BufferedImage image = new BufferedImage(width, height, hasAlphaChannel ? 2 : 1);
        DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
        int[] data = dataBuffer.getData();
        for (int i = 0, size = width * height; i < size; i++) {
            data[i] = getRGB(i);
        }
        return image;
    }

    public abstract int getRGB(int index);

    public float getValueAt(float x, float y, int shift) {

        float xf = (float) Math.floor(x);
        float yf = (float) Math.floor(y);

        int xi = (int) xf;
        int yi = (int) yf;

        float fx = x - xf, gx = 1f - fx;
        float fy = y - yf, gy = 1f - fy;

        int c00, c01, c10, c11;
        int width = this.width;
        if (xi >= 0 && yi >= 0 && xi < width - 1 && yi < height - 1) {
            // safe
            int index = xi + yi * width;
            c00 = getRGB(index);
            c01 = getRGB(index + width);
            c10 = getRGB(index + 1);
            c11 = getRGB(index + 1 + width);
        } else {
            // border
            c00 = getSafeRGB(xi, yi);
            c01 = getSafeRGB(xi, yi + 1);
            c10 = getSafeRGB(xi + 1, yi);
            c11 = getSafeRGB(xi + 1, yi + 1);
        }

        c00 = (c00 >> shift) & 255;
        c01 = (c01 >> shift) & 255;
        c10 = (c10 >> shift) & 255;
        c11 = (c11 >> shift) & 255;

        float r0 = c00 * gy + fy * c01;
        float r1 = c10 * gy + fy * c11;
        return r0 * gx + fx * r1;

    }

    public int getRGB(int x, int y) {
        return getRGB(x + y * width);
    }

    public int getSafeRGB(int x, int y) {
        return getRGB(getIndex(x, y));
    }

    public void createTexture(Texture2D texture, boolean checkRedundancy) {
        texture.create(createBufferedImage(), true, true);
    }

    public BufferedImage createBufferedImage(int dstWidth, int dstHeight) {
        final int srcWidth = this.width;
        final int srcHeight = this.height;
        if (dstWidth > srcWidth) {
            dstHeight = dstHeight * srcWidth / dstWidth;
            dstWidth = srcWidth;
        }
        if (dstHeight > srcHeight) {
            dstWidth = dstWidth * srcHeight / dstHeight;
            dstHeight = srcHeight;
        }
        if (dstWidth == srcWidth && dstHeight == srcHeight) {
            return createBufferedImage();
        }
        final BufferedImage img = new BufferedImage(dstWidth, dstHeight, hasAlphaChannel ? 2 : 1);
        final DataBuffer buffer = img.getData().getDataBuffer();
        int srcY0 = 0;
        for (int dstY = 0, dstIndex = 0; dstY < dstHeight; dstY++) {
            final int srcY1 = ((dstY + 1) * srcHeight) / dstHeight;
            final int srcDY = srcY1 - srcY0;
            final int srcIndexY0 = srcY0 * srcWidth;
            for (int dstX = 0; dstX < dstWidth; dstX++, dstIndex++) {
                int srcX0 = (dstX * srcWidth) / dstWidth;
                int srcX1 = ((dstX + 1) * srcWidth) / dstWidth;
                // we could use better interpolation, but it shouldn't really matter
                int r = 0, g = 0, b = 0, a = 0;
                int srcIndexYI = srcIndexY0;
                // use interpolation
                for (int y0 = srcY0; y0 < srcY1; y0++) {
                    final int startIndex = srcX0 + srcIndexYI;
                    final int endIndex = srcX1 + srcIndexYI;
                    for (int i = startIndex; i < endIndex; i++) {
                        final int color = getRGB(i);
                        a += (color >> 24) & 255;
                        r += (color >> 16) & 255;
                        g += (color >> 8) & 255;
                        b += color & 255;
                    }
                    srcIndexYI += srcWidth;
                }
                final int count = (srcX1 - srcX0) * srcDY;
                if (count > 1) {
                    a /= count;
                    r /= count;
                    g /= count;
                    b /= count;
                }
                buffer.setElem(dstIndex, argb(a, r, g, b));
            }
            srcY0 = srcY1;
        }
        // update the image, otherwise the result is broken
        img.setData(Raster.createRaster(img.getRaster().getSampleModel(), buffer, new Point()));
        return img;
    }

    public void write(FileReference dst) throws IOException {
        String format = dst.getLcExtension();
        try (OutputStream out = dst.outputStream()) {
            write(out, format);
        }
    }

    public void write(OutputStream dst, String format) throws IOException {
        BufferedImage image = createBufferedImage();
        ImageIO.write(image, format, dst);
    }

    public static int argb(int a, int r, int g, int b) {
        return (a << 24) + (r << 16) + (g << 8) + b;
    }

    public static void createRGBFrom3StridedData(Texture2D texture, int width, int height, boolean checkRedundancy, byte[] data) {
        // add a padding for alpha, because OpenGL needs it that way
        ByteBuffer buffer = Texture2D.Companion.getBufferPool().get(width * height * 4, false);
        for (int j = 0, k = 0, l = width * height * 3; k < l; ) {
            buffer.put(j++, (byte) 255);// a
            buffer.put(j++, data[k++]);// r
            buffer.put(j++, data[k++]);// g
            buffer.put(j++, data[k++]);// b
        }
        texture.createRGB(buffer, checkRedundancy);
    }

    @Override
    public void destroy() {
    }

}
