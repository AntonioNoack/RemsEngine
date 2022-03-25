package me.anno.utils.test.gfx;

import me.anno.image.Image;
import me.anno.image.ImageCPUCache;
import me.anno.image.raw.IntImage;
import me.anno.maths.Maths;
import me.anno.utils.OS;
import me.anno.utils.structures.arrays.FloatArrayList;
import me.anno.utils.structures.arrays.IntArrayList;

import java.io.IOException;
import java.util.BitSet;

public class ImageTracing {

    // test this with real letters
    // to do set the test size for meshes to 120 instead of 20-ish

    public static void main(String[] args) throws IOException {
        Image image = ImageCPUCache.INSTANCE.getImage(OS.INSTANCE.getDocuments().getChild("test-text.png"), false);
        assert image != null;
        int[] pixels = ((IntImage) image).getData();
        int black = 0xff000000;
        for (int i = 0, l = pixels.length; i < l; i++) {
            pixels[i] = pixels[i] & black;
        }
        computeOutline(image.width, image.height, pixels);
    }

    public static void computeOutline(int w, int h, int[] pixels) throws IOException {

        for (int i = 0, l = pixels.length; i < l; i++) {
            pixels[i] = (pixels[i] >>> 24) - 128;
        }

        IntArrayList ops = new IntArrayList(64);
        FloatArrayList data = new FloatArrayList(256, 0f);
        IntArrayList edge = new IntArrayList(64);
        float[] p = new float[2], p2 = new float[2];
        BitSet done = new BitSet(w * h * 2);

        int s = 16;

        IntImage ii = new IntImage(w * s, h * s, false);
        IntImage ij = new IntImage(w, h, false);

        int ctr = 0;

        for (int y = 0, i = 0; y < h; y++) {
            for (int x = 0; x < w; x++, i++) {
                if (isEdge(x, y, w, pixels, true) &&
                        !done.get(x + y * w) && !done.get(x + y * w + 1)) {

                    traceEdge(x, y, w, h, pixels, done, edge);

                    if (edge.size() <= 2) continue;

                    // a full curve was found -> turn into points & lines
                    edgeToPoint(edge.getValue(0), w, pixels, p);
                    moveTo(p[0], p[1], ops, data);
                    for (int k = 1, l = edge.size(); k < l; k++) {
                        edgeToPoint(edge.getValue(k), w, pixels, p);
                        lineTo(p[0], p[1], ops, data);
                    }
                    close(ops);

                    for (int k = 0; k < pixels.length; k++)
                        ii.setRGB(k % w, k / w, 0);

                    for (int k = 0, l = edge.size() - 1; k < l; k++) {
                        edgeToPoint(edge.getValue(k), w, pixels, p);
                        edgeToPoint(edge.getValue(k + 1), w, pixels, p2);
                        for (int m = 0; m < s * 2; m++) {
                            float f = m / (s * 2 - 1f);
                            int px = (int) (Maths.INSTANCE.mix(p[0], p2[0], f) * s);
                            int py = (int) (Maths.INSTANCE.mix(p[1], p2[1], f) * s);
                            ii.setRGB(px, py, -1);
                        }
                    }
                    ii.write(OS.INSTANCE.getDesktop().getChild("it/lines-" + (ctr++) + ".png"));

                }
            }
            i++;
        }

        for (int k = 0; k < w * h; k++)
            ij.setRGB(k % w, k / w, done.get(k * 2) || done.get(k * 2 + 1) ? 0 : -1);
        ij.write(OS.INSTANCE.getDesktop().getChild("it/done.png"));

    }

    private static void edgeToPoint(int edge, int w, int[] pixels, float[] dst) {
        int i = edge >> 1;
        int x = i % w;
        int y = i / w;
        dst[0] = x + 0.5f;
        dst[1] = y + 0.5f;
        if (isVEdge(edge)) {
            dst[0] += cut(pixels[i], pixels[i + 1]);
        } else {
            dst[1] += cut(pixels[i], pixels[i + w]);
        }
    }

    private static void traceEdge(int x0, int y0, int w, int h, int[] pixels, BitSet done, IntArrayList dst) {

        dst.clear();

        int x = x0;
        int y = y0;
        // our direction
        // 0: ->
        // 1: V
        // 2: <-
        // 3: A
        int edge;
        int dir = 1;
        while (true) {

            int i3 = dir * 3, i;
            boolean vEdge = (dir & 1) == 1;
            for (i = 0; i < 3; i++) {
                int x2 = x + dx[i3];
                int y2 = y + dy[i3++];
                if (isEdge(x2, y2, w, pixels, vEdge)) {
                    // go right
                    x = x2;
                    y = y2;
                    dir = (dir + nextDir[i]) & 3;
                    break;
                }
                vEdge = (dir & 1) == 0;
            }
            if (i >= 3) return;

            edge = (x + y * w) * 2 + (dir & 1);
            dst.plusAssign(edge);

            if (done.get(edge)) return;
            done.set(edge, true);

        }
    }

    private static boolean isVEdge(int edge) {
        return (edge & 1) == 1;
    }

    private final static int[] dx = new int[]{
            +1, +0, +0,
            +0, +0, +1,
            -1, -1, -1,
            +0, +1, +0
    };
    private final static int[] dy = new int[]{
            +0, +1, +0,
            +1, +0, +0,
            +0, +0, +1,
            -1, -1, -1
    };
    private final static int[] nextDir = new int[]{0, 1, 3};

    private static boolean isEdge(int x, int y, int w, int[] pixels, boolean vEdge) {
        int i0 = x + y * w, i1;
        if (i0 < 0 || x >= w || i0 >= pixels.length) return false;
        if (vEdge) {
            if (x + 1 >= w) return false;
            i1 = i0 + 1;
        } else {
            i1 = i0 + w;
            if (i1 >= pixels.length) return false;
        }
        return (pixels[i0] > 0) != (pixels[i1] > 0);
    }

    private static float cut(int a, int b) {
        if ((a < 0) == (b < 0)) return 0.5f;
        if (a > b) a++;
        else b++;
        return a / (float) (a - b);
    }

    private static void close(IntArrayList ops) {
        System.out.println("close");
    }

    private static void moveTo(float x, float y, IntArrayList ops, FloatArrayList data) {
        System.out.println("move to " + x + ", " + y);
    }

    private static void lineTo(float x, float y, IntArrayList ops, FloatArrayList data) {
        System.out.println("line to " + x + ", " + y);
    }

}
