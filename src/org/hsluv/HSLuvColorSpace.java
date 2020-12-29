package org.hsluv;

import org.joml.Vector2d;

public class HSLuvColorSpace {

    private static final double[] xyz2rgb = new double[]{
            3.240969941904521, -1.537383177570093, -0.498610760293,
            -0.96924363628087, 1.87596750150772, 0.041555057407175,
            0.055630079696993, -0.20397695888897, 1.056971514242878,
    };

    private static final double[] rgb2xyz = new double[]{
            0.41239079926595, 0.35758433938387, 0.18048078840183,
            0.21263900587151, 0.71516867876775, 0.072192315360733,
            0.019330818715591, 0.11919477979462, 0.95053215224966,
    };

    private static final double refY = 1.0;

    private static final double refU = 0.19783000664283;
    private static final double refV = 0.46831999493879;

    private static final double kappa = 903.2962962;
    private static final double epsilon = 0.0088564516;

    private static Vector2d[] getBounds(double L, int limit) {

        Vector2d[] result = new Vector2d[limit];

        double sub1 = Math.pow(L + 16.0, 3.0) / 1560896.0;
        double sub2 = sub1 > epsilon ? sub1 : L / kappa;

        for (int c = 0, index = 0; c < 3; c++) {

            double m1 = xyz2rgb[c * 3];
            double m2 = xyz2rgb[c * 3 + 1];
            double m3 = xyz2rgb[c * 3 + 2];

            for (int t = 0; t < 2; t++) {
                double top1 = (284517 * m1 - 94839 * m3) * sub2;
                double top2 = (838422 * m3 + 769860 * m2 + 731718 * m1) * L * sub2 - 769860 * t * L;
                double bottom = (632260 * m3 - 126452 * m2) * sub2 + 126452 * t;
                result[index++] = new Vector2d(top1 / bottom, top2 / bottom);
                if (index >= limit) return result;
            }
        }

        return result;

    }

    private static double intersectLineLine(Vector2d lineA, Vector2d lineB) {
        return (lineA.y - lineB.y) / (lineB.x - lineA.x);
    }

    private static double distanceFromPole(Vector2d point) {
        return point.length();
    }

    private static double lengthOfRayUntilIntersect(double theta, Vector2d line) {
        return line.y / (Math.sin(theta) - line.x * Math.cos(theta));
    }

    private static double maxSafeChromaForL(double L) {
        Vector2d[] bounds = getBounds(L, 2);
        double min = Double.MAX_VALUE;

        for (int i = 0; i < 2; i++) {// why only the first two bounds? because they must belong to L

            Vector2d line0 = bounds[i];
            double m1 = line0.x;
            double b1 = line0.y;

            Vector2d line = new Vector2d(m1, b1);

            double x = intersectLineLine(line, new Vector2d(-1.0 / m1, 0.0));
            double length = distanceFromPole(new Vector2d(x, b1 + x * m1));

            min = Math.min(min, length);

        }

        return min;
    }

    private static double maxChromaForLH(double L, double H) {
        double hRadians = H / 360 * Math.PI * 2;

        Vector2d[] bounds = getBounds(L,6);
        double min = Double.MAX_VALUE;

        for (Vector2d bound : bounds) {
            double length = lengthOfRayUntilIntersect(hRadians, bound);
            if (length >= 0.0) {
                min = Math.min(min, length);
            }
        }

        return min;
    }

    private static double dotProduct(double[] a, int aOffset, double[] b) {
        double sum = 0;
        for (int i = 0; i < b.length; ++i) {
            sum += a[i + aOffset] * b[i];
        }
        return sum;
    }

    private static double fromLinear(double c) {
        if (c <= 0.0031308) {
            return 12.92 * c;
        } else {
            return 1.055 * Math.pow(c, 1 / 2.4) - 0.055;
        }
    }

    private static double toLinear(double c) {
        if (c > 0.04045) {
            return Math.pow((c + 0.055) / (1 + 0.055), 2.4);
        } else {
            return c / 12.92;
        }
    }

    /*private static int[] rgbPrepare(double[] tuple) {

        int[] results = new int[tuple.length];

        for (int i = 0; i < tuple.length; ++i) {
            double chan = tuple[i];
            double rounded = round(chan, 3);

            if (rounded < -0.0001 || rounded > 1.0001) {
                throw new IllegalArgumentException("Illegal rgb value: " + rounded);
            }

            results[i] = (int) Math.round(rounded * 255);
        }

        return results;
    }*/

    public static double[] xyzToRgb(double[] tuple) {
        return new double[]{
                fromLinear(dotProduct(xyz2rgb, 0, tuple)),
                fromLinear(dotProduct(xyz2rgb, 3, tuple)),
                fromLinear(dotProduct(xyz2rgb, 6, tuple)),
        };
    }

    public static double[] rgbToXyz(double[] tuple) {
        double[] linearRGB = new double[]{
                toLinear(tuple[0]),
                toLinear(tuple[1]),
                toLinear(tuple[2]),
        };
        return new double[]{
                dotProduct(rgb2xyz, 0, linearRGB),
                dotProduct(rgb2xyz, 3, linearRGB),
                dotProduct(rgb2xyz, 6, linearRGB),
        };
    }

    private static double yToL(double Y) {
        if (Y <= epsilon) {
            return (Y / refY) * kappa;
        } else {
            return 116.0 * Math.pow(Y / refY, 1.0 / 3.0) - 16.0;
        }
    }

    private static double lToY(double L) {
        if (L <= 8.0) {
            return refY * L / kappa;
        } else {
            return refY * Math.pow((L + 16.0) / 116.0, 3.0);
        }
    }

    public static double[] xyzToLuv(double[] tuple) {

        double X = tuple[0];
        double Y = tuple[1];
        double Z = tuple[2];

        double varU = (4.0 * X) / (X + (15.0 * Y) + (3.0 * Z));
        double varV = (9.0 * Y) / (X + (15.0 * Y) + (3.0 * Z));

        double L = yToL(Y);

        if (L == 0) {
            return new double[]{0, 0, 0};
        }

        double U = 13.0 * L * (varU - refU);
        double V = 13.0 * L * (varV - refV);

        return new double[]{L, U, V};

    }

    public static double[] luvToXyz(double[] tuple) {
        double L = tuple[0];
        double U = tuple[1];
        double V = tuple[2];

        if (L == 0) {
            return new double[]{0, 0, 0};
        }

        double varU = U / (13.0 * L) + refU;
        double varV = V / (13.0 * L) + refV;

        double Y = lToY(L);
        double X = 0 - (9.0 * Y * varU) / ((varU - 4.0) * varV - varU * varV);
        double Z = (9.0 * Y - (15.0 * varV * Y) - (varV * X)) / (3.0 * varV);

        return new double[]{X, Y, Z};
    }

    public static double[] luvToLch(double[] tuple) {

        double L = tuple[0];
        double U = tuple[1];
        double V = tuple[2];

        double C = Math.sqrt(U * U + V * V);
        double H;

        if (C < 0.00000001) {

            H = 0;

        } else {

            H = StrictMath.toDegrees(Math.atan2(V, U));

            if (H < 0) {
                H += 360;
            }

        }

        return new double[]{L, C, H};

    }

    public static double[] lchToLuv(double[] tuple) {

        double L = tuple[0];
        double C = tuple[1];
        double H = tuple[2];

        double hRadians = StrictMath.toRadians(H);
        double U = Math.cos(hRadians) * C;
        double V = Math.sin(hRadians) * C;

        return new double[]{L, U, V};

    }

    public static double[] hsluvToLch(double[] tuple) {

        double H = tuple[0];
        double S = tuple[1];
        double L = tuple[2];

        if (L > 99.9999999) {
            return new double[]{100, 0, H};
        }

        if (L < 0.00000001) {
            return new double[]{0, 0, H};
        }

        double max = maxChromaForLH(L, H);
        double C = max * 0.01 * S;

        return new double[]{L, C, H};

    }

    public static double[] lchToHsluv(double[] tuple) {
        double L = tuple[0];
        double C = tuple[1];
        double H = tuple[2];

        if (L > 99.9999999) {
            return new double[]{H, 0, 100};
        }

        if (L < 0.00000001) {
            return new double[]{H, 0, 0};
        }

        double max = maxChromaForLH(L, H);
        double S = C / max * 100;

        return new double[]{H, S, L};
    }

    public static double[] hpluvToLch(double[] tuple) {
        double H = tuple[0];
        double S = tuple[1];
        double L = tuple[2];

        if (L > 99.9999999) {
            return new double[]{100, 0, H};
        }

        if (L < 0.00000001) {
            return new double[]{0, 0, H};
        }

        double max = maxSafeChromaForL(L);
        double C = max / 100 * S;

        return new double[]{L, C, H};
    }

    public static double[] lchToHpluv(double[] tuple) {
        double L = tuple[0];
        double C = tuple[1];
        double H = tuple[2];

        if (L > 99.9999999) {
            return new double[]{H, 0, 100};
        }

        if (L < 0.00000001) {
            return new double[]{H, 0, 0};
        }

        double max = maxSafeChromaForL(L);
        double S = C / max * 100;

        return new double[]{H, S, L};
    }

    public static double[] lchToRgb(double[] tuple) {
        return xyzToRgb(luvToXyz(lchToLuv(tuple)));
    }

    public static double[] rgbToLch(double[] tuple) {
        return luvToLch(xyzToLuv(rgbToXyz(tuple)));
    }

    public static double[] hsluvToRgb(double[] tuple) {
        return lchToRgb(hsluvToLch(tuple));
    }

    public static double[] rgbToHsluv(double[] tuple) {
        return lchToHsluv(rgbToLch(tuple));
    }

    public static double[] hpluvToRgb(double[] tuple) {
        return lchToRgb(hpluvToLch(tuple));
    }

    public static double[] rgbToHpluv(double[] tuple) {
        return lchToHpluv(rgbToLch(tuple));
    }

}