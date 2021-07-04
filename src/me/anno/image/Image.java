package me.anno.image;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

public abstract class Image {

    public int width, height;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public abstract BufferedImage createBufferedImage();

    public BufferedImage createBufferedImage(int w, int h) {
        BufferedImage src = createBufferedImage();
        final int width = this.width;
        final int height = this.height;
        if (w == width && h == height) return src;
        BufferedImage img = new BufferedImage(w, h, src.getColorModel().hasAlpha() ? 2 : 1);
        DataBuffer srcBuffer = src.getRaster().getDataBuffer();
        DataBuffer buffer = img.getRaster().getDataBuffer();
        for (int y = 0, index = 0; y < h; y++) {
            int iy = (y * height) / h;
            int iyw = iy * width;
            for (int x = 0; x < w; x++, index++) {
                int ix = (x * width) / w;
                int i0 = ix + iyw;
                buffer.setElem(index, srcBuffer.getElem(i0));
            }
        }
        return img;
    }

}
