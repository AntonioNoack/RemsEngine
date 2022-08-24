package org.joml;

import java.text.NumberFormat;

@SuppressWarnings("unused")
public class Matrix3d {

    public double m00;
    public double m01;
    public double m02;
    public double m10;
    public double m11;
    public double m12;
    public double m20;
    public double m21;
    public double m22;

    public Matrix3d() {
        this.m00 = 1.0;
        this.m11 = 1.0;
        this.m22 = 1.0;
    }

    public Matrix3d(Matrix2d mat) {
        this.set(mat);
    }

    public Matrix3d(Matrix2f mat) {
        this.set(mat);
    }

    public Matrix3d(Matrix3d mat) {
        this.set(mat);
    }

    public Matrix3d(Matrix3f mat) {
        this.set(mat);
    }

    public Matrix3d(Matrix4f mat) {
        this.set(mat);
    }

    public Matrix3d(Matrix4d mat) {
        this.set(mat);
    }

    public Matrix3d(double m00, double m01, double m02, double m10, double m11, double m12, double m20, double m21, double m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
    }

    public Matrix3d(Vector3d col0, Vector3d col1, Vector3d col2) {
        this.set(col0, col1, col2);
    }

    public double m00() {
        return this.m00;
    }

    public double m01() {
        return this.m01;
    }

    public double m02() {
        return this.m02;
    }

    public double m10() {
        return this.m10;
    }

    public double m11() {
        return this.m11;
    }

    public double m12() {
        return this.m12;
    }

    public double m20() {
        return this.m20;
    }

    public double m21() {
        return this.m21;
    }

    public double m22() {
        return this.m22;
    }

    public Matrix3d m00(double m00) {
        this.m00 = m00;
        return this;
    }

    public Matrix3d m01(double m01) {
        this.m01 = m01;
        return this;
    }

    public Matrix3d m02(double m02) {
        this.m02 = m02;
        return this;
    }

    public Matrix3d m10(double m10) {
        this.m10 = m10;
        return this;
    }

    public Matrix3d m11(double m11) {
        this.m11 = m11;
        return this;
    }

    public Matrix3d m12(double m12) {
        this.m12 = m12;
        return this;
    }

    public Matrix3d m20(double m20) {
        this.m20 = m20;
        return this;
    }

    public Matrix3d m21(double m21) {
        this.m21 = m21;
        return this;
    }

    public Matrix3d m22(double m22) {
        this.m22 = m22;
        return this;
    }

    Matrix3d _m00(double m00) {
        this.m00 = m00;
        return this;
    }

    Matrix3d _m01(double m01) {
        this.m01 = m01;
        return this;
    }

    Matrix3d _m02(double m02) {
        this.m02 = m02;
        return this;
    }

    Matrix3d _m10(double m10) {
        this.m10 = m10;
        return this;
    }

    Matrix3d _m11(double m11) {
        this.m11 = m11;
        return this;
    }

    Matrix3d _m12(double m12) {
        this.m12 = m12;
        return this;
    }

    Matrix3d _m20(double m20) {
        this.m20 = m20;
        return this;
    }

    Matrix3d _m21(double m21) {
        this.m21 = m21;
        return this;
    }

    Matrix3d _m22(double m22) {
        this.m22 = m22;
        return this;
    }

    public Matrix3d set(Matrix3d m) {
        this.m00 = m.m00;
        this.m01 = m.m01;
        this.m02 = m.m02;
        this.m10 = m.m10;
        this.m11 = m.m11;
        this.m12 = m.m12;
        this.m20 = m.m20;
        this.m21 = m.m21;
        this.m22 = m.m22;
        return this;
    }

    public Matrix3d setTransposed(Matrix3d m) {
        double nm10 = m.m01;
        double nm12 = m.m21;
        double nm20 = m.m02;
        double nm21 = m.m12;
        return this._m00(m.m00)._m01(m.m10)._m02(m.m20)._m10(nm10)._m11(m.m11)._m12(nm12)._m20(nm20)._m21(nm21)._m22(m.m22);
    }

    public Matrix3d set(Matrix3f m) {
        this.m00 = m.m00;
        this.m01 = m.m01;
        this.m02 = m.m02;
        this.m10 = m.m10;
        this.m11 = m.m11;
        this.m12 = m.m12;
        this.m20 = m.m20;
        this.m21 = m.m21;
        this.m22 = m.m22;
        return this;
    }

    public Matrix3d setTransposed(Matrix3f m) {
        float nm10 = m.m01;
        float nm12 = m.m21;
        float nm20 = m.m02;
        float nm21 = m.m12;
        return this._m00(m.m00)._m01(m.m10)._m02(m.m20)._m10(nm10)._m11(m.m11)._m12(nm12)._m20(nm20)._m21(nm21)._m22(m.m22);
    }

    public Matrix3d set(Matrix4x3d m) {
        this.m00 = m.m00;
        this.m01 = m.m01;
        this.m02 = m.m02;
        this.m10 = m.m10;
        this.m11 = m.m11;
        this.m12 = m.m12;
        this.m20 = m.m20;
        this.m21 = m.m21;
        this.m22 = m.m22;
        return this;
    }

    public Matrix3d set(Matrix4f mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m02 = mat.m02;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
        this.m12 = mat.m12;
        this.m20 = mat.m20;
        this.m21 = mat.m21;
        this.m22 = mat.m22;
        return this;
    }

    public Matrix3d set(Matrix4d mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m02 = mat.m02;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
        this.m12 = mat.m12;
        this.m20 = mat.m20;
        this.m21 = mat.m21;
        this.m22 = mat.m22;
        return this;
    }

    public Matrix3d set(Matrix2f mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m02 = 0.0;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = 1.0;
        return this;
    }

    public Matrix3d set(Matrix2d mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m02 = 0.0;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = 1.0;
        return this;
    }

    public Matrix3d set(AxisAngle4f axisAngle) {
        double x = axisAngle.x;
        double y = axisAngle.y;
        double z = axisAngle.z;
        double angle = axisAngle.angle;
        double invLength = Math.invsqrt(x * x + y * y + z * z);
        x *= invLength;
        y *= invLength;
        z *= invLength;
        double s = Math.sin(angle);
        double c = Math.cosFromSin(s, angle);
        double omc = 1.0 - c;
        this.m00 = c + x * x * omc;
        this.m11 = c + y * y * omc;
        this.m22 = c + z * z * omc;
        double tmp1 = x * y * omc;
        double tmp2 = z * s;
        this.m10 = tmp1 - tmp2;
        this.m01 = tmp1 + tmp2;
        tmp1 = x * z * omc;
        tmp2 = y * s;
        this.m20 = tmp1 + tmp2;
        this.m02 = tmp1 - tmp2;
        tmp1 = y * z * omc;
        tmp2 = x * s;
        this.m21 = tmp1 - tmp2;
        this.m12 = tmp1 + tmp2;
        return this;
    }

    public Matrix3d set(AxisAngle4d axisAngle) {
        double x = axisAngle.x;
        double y = axisAngle.y;
        double z = axisAngle.z;
        double angle = axisAngle.angle;
        double invLength = Math.invsqrt(x * x + y * y + z * z);
        x *= invLength;
        y *= invLength;
        z *= invLength;
        double s = Math.sin(angle);
        double c = Math.cosFromSin(s, angle);
        double omc = 1.0 - c;
        this.m00 = c + x * x * omc;
        this.m11 = c + y * y * omc;
        this.m22 = c + z * z * omc;
        double tmp1 = x * y * omc;
        double tmp2 = z * s;
        this.m10 = tmp1 - tmp2;
        this.m01 = tmp1 + tmp2;
        tmp1 = x * z * omc;
        tmp2 = y * s;
        this.m20 = tmp1 + tmp2;
        this.m02 = tmp1 - tmp2;
        tmp1 = y * z * omc;
        tmp2 = x * s;
        this.m21 = tmp1 - tmp2;
        this.m12 = tmp1 + tmp2;
        return this;
    }

    public Matrix3d set(Quaternionf q) {
        return this.rotation(q);
    }

    public Matrix3d set(Quaterniond q) {
        return this.rotation(q);
    }

    public Matrix3d mul(Matrix3d right) {
        return this.mul(right, this);
    }

    public Matrix3d mul(Matrix3d right, Matrix3d dest) {
        double nm00 = Math.fma(this.m00, right.m00, Math.fma(this.m10, right.m01, this.m20 * right.m02));
        double nm01 = Math.fma(this.m01, right.m00, Math.fma(this.m11, right.m01, this.m21 * right.m02));
        double nm02 = Math.fma(this.m02, right.m00, Math.fma(this.m12, right.m01, this.m22 * right.m02));
        double nm10 = Math.fma(this.m00, right.m10, Math.fma(this.m10, right.m11, this.m20 * right.m12));
        double nm11 = Math.fma(this.m01, right.m10, Math.fma(this.m11, right.m11, this.m21 * right.m12));
        double nm12 = Math.fma(this.m02, right.m10, Math.fma(this.m12, right.m11, this.m22 * right.m12));
        double nm20 = Math.fma(this.m00, right.m20, Math.fma(this.m10, right.m21, this.m20 * right.m22));
        double nm21 = Math.fma(this.m01, right.m20, Math.fma(this.m11, right.m21, this.m21 * right.m22));
        double nm22 = Math.fma(this.m02, right.m20, Math.fma(this.m12, right.m21, this.m22 * right.m22));
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        return dest;
    }

    public Matrix3d mulLocal(Matrix3d left) {
        return this.mulLocal(left, this);
    }

    public Matrix3d mulLocal(Matrix3d left, Matrix3d dest) {
        double nm00 = left.m00 * this.m00 + left.m10 * this.m01 + left.m20 * this.m02;
        double nm01 = left.m01 * this.m00 + left.m11 * this.m01 + left.m21 * this.m02;
        double nm02 = left.m02 * this.m00 + left.m12 * this.m01 + left.m22 * this.m02;
        double nm10 = left.m00 * this.m10 + left.m10 * this.m11 + left.m20 * this.m12;
        double nm11 = left.m01 * this.m10 + left.m11 * this.m11 + left.m21 * this.m12;
        double nm12 = left.m02 * this.m10 + left.m12 * this.m11 + left.m22 * this.m12;
        double nm20 = left.m00 * this.m20 + left.m10 * this.m21 + left.m20 * this.m22;
        double nm21 = left.m01 * this.m20 + left.m11 * this.m21 + left.m21 * this.m22;
        double nm22 = left.m02 * this.m20 + left.m12 * this.m21 + left.m22 * this.m22;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        return dest;
    }

    public Matrix3d mul(Matrix3f right) {
        return this.mul(right, this);
    }

    public Matrix3d mul(Matrix3f right, Matrix3d dest) {
        double nm00 = Math.fma(this.m00, right.m00, Math.fma(this.m10, right.m01, this.m20 * (double) right.m02));
        double nm01 = Math.fma(this.m01, right.m00, Math.fma(this.m11, right.m01, this.m21 * (double) right.m02));
        double nm02 = Math.fma(this.m02, right.m00, Math.fma(this.m12, right.m01, this.m22 * (double) right.m02));
        double nm10 = Math.fma(this.m00, right.m10, Math.fma(this.m10, right.m11, this.m20 * (double) right.m12));
        double nm11 = Math.fma(this.m01, right.m10, Math.fma(this.m11, right.m11, this.m21 * (double) right.m12));
        double nm12 = Math.fma(this.m02, right.m10, Math.fma(this.m12, right.m11, this.m22 * (double) right.m12));
        double nm20 = Math.fma(this.m00, right.m20, Math.fma(this.m10, right.m21, this.m20 * (double) right.m22));
        double nm21 = Math.fma(this.m01, right.m20, Math.fma(this.m11, right.m21, this.m21 * (double) right.m22));
        double nm22 = Math.fma(this.m02, right.m20, Math.fma(this.m12, right.m21, this.m22 * (double) right.m22));
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        return dest;
    }

    public Matrix3d set(double m00, double m01, double m02, double m10, double m11, double m12, double m20, double m21, double m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        return this;
    }

    public Matrix3d set(double[] m) {
        this.m00 = m[0];
        this.m01 = m[1];
        this.m02 = m[2];
        this.m10 = m[3];
        this.m11 = m[4];
        this.m12 = m[5];
        this.m20 = m[6];
        this.m21 = m[7];
        this.m22 = m[8];
        return this;
    }

    public Matrix3d set(float[] m) {
        this.m00 = m[0];
        this.m01 = m[1];
        this.m02 = m[2];
        this.m10 = m[3];
        this.m11 = m[4];
        this.m12 = m[5];
        this.m20 = m[6];
        this.m21 = m[7];
        this.m22 = m[8];
        return this;
    }

    public double determinant() {
        return (this.m00 * this.m11 - this.m01 * this.m10) * this.m22 + (this.m02 * this.m10 - this.m00 * this.m12) * this.m21 + (this.m01 * this.m12 - this.m02 * this.m11) * this.m20;
    }

    public Matrix3d invert() {
        return this.invert(this);
    }

    public Matrix3d invert(Matrix3d dest) {
        double a = Math.fma(this.m00, this.m11, -this.m01 * this.m10);
        double b = Math.fma(this.m02, this.m10, -this.m00 * this.m12);
        double c = Math.fma(this.m01, this.m12, -this.m02 * this.m11);
        double d = Math.fma(a, this.m22, Math.fma(b, this.m21, c * this.m20));
        double s = 1.0 / d;
        double nm00 = Math.fma(this.m11, this.m22, -this.m21 * this.m12) * s;
        double nm01 = Math.fma(this.m21, this.m02, -this.m01 * this.m22) * s;
        double nm02 = c * s;
        double nm10 = Math.fma(this.m20, this.m12, -this.m10 * this.m22) * s;
        double nm11 = Math.fma(this.m00, this.m22, -this.m20 * this.m02) * s;
        double nm12 = b * s;
        double nm20 = Math.fma(this.m10, this.m21, -this.m20 * this.m11) * s;
        double nm21 = Math.fma(this.m20, this.m01, -this.m00 * this.m21) * s;
        double nm22 = a * s;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        return dest;
    }

    public Matrix3d transpose() {
        return this.transpose(this);
    }

    public Matrix3d transpose(Matrix3d dest) {
        dest.set(this.m00, this.m10, this.m20, this.m01, this.m11, this.m21, this.m02, this.m12, this.m22);
        return dest;
    }

    public String toString() {
        String str = this.toString(Options.NUMBER_FORMAT);
        StringBuilder res = new StringBuilder();
        int eIndex = Integer.MIN_VALUE;

        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c == 'E') {
                eIndex = i;
            } else {
                if (c == ' ' && eIndex == i - 1) {
                    res.append('+');
                    continue;
                }

                if (Character.isDigit(c) && eIndex == i - 1) {
                    res.append('+');
                }
            }

            res.append(c);
        }

        return res.toString();
    }

    public String toString(NumberFormat formatter) {
        return Runtime.format(this.m00, formatter) + " " + Runtime.format(this.m10, formatter) + " " + Runtime.format(this.m20, formatter) + "\n" + Runtime.format(this.m01, formatter) + " " + Runtime.format(this.m11, formatter) + " " + Runtime.format(this.m21, formatter) + "\n" + Runtime.format(this.m02, formatter) + " " + Runtime.format(this.m12, formatter) + " " + Runtime.format(this.m22, formatter) + "\n";
    }

    public Matrix3d get(Matrix3d dest) {
        return dest.set(this);
    }

    public AxisAngle4f getRotation(AxisAngle4f dest) {
        return dest.set(this);
    }

    public Quaternionf getUnnormalizedRotation(Quaternionf dest) {
        return dest.setFromUnnormalized(this);
    }

    public Quaternionf getNormalizedRotation(Quaternionf dest) {
        return dest.setFromNormalized(this);
    }

    public Quaterniond getUnnormalizedRotation(Quaterniond dest) {
        return dest.setFromUnnormalized(this);
    }

    public Quaterniond getNormalizedRotation(Quaterniond dest) {
        return dest.setFromNormalized(this);
    }

    public double[] get(double[] arr, int offset) {
        arr[offset] = this.m00;
        arr[offset + 1] = this.m01;
        arr[offset + 2] = this.m02;
        arr[offset + 3] = this.m10;
        arr[offset + 4] = this.m11;
        arr[offset + 5] = this.m12;
        arr[offset + 6] = this.m20;
        arr[offset + 7] = this.m21;
        arr[offset + 8] = this.m22;
        return arr;
    }

    public double[] get(double[] arr) {
        return this.get(arr, 0);
    }

    public float[] get(float[] arr, int offset) {
        arr[offset] = (float) this.m00;
        arr[offset + 1] = (float) this.m01;
        arr[offset + 2] = (float) this.m02;
        arr[offset + 3] = (float) this.m10;
        arr[offset + 4] = (float) this.m11;
        arr[offset + 5] = (float) this.m12;
        arr[offset + 6] = (float) this.m20;
        arr[offset + 7] = (float) this.m21;
        arr[offset + 8] = (float) this.m22;
        return arr;
    }

    public float[] get(float[] arr) {
        return this.get(arr, 0);
    }

    public Matrix3d set(Vector3d col0, Vector3d col1, Vector3d col2) {
        this.m00 = col0.x;
        this.m01 = col0.y;
        this.m02 = col0.z;
        this.m10 = col1.x;
        this.m11 = col1.y;
        this.m12 = col1.z;
        this.m20 = col2.x;
        this.m21 = col2.y;
        this.m22 = col2.z;
        return this;
    }

    public Matrix3d zero() {
        this.m00 = 0.0;
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m10 = 0.0;
        this.m11 = 0.0;
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = 0.0;
        return this;
    }

    public Matrix3d identity() {
        this.m00 = 1.0;
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m10 = 0.0;
        this.m11 = 1.0;
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = 1.0;
        return this;
    }

    public Matrix3d scaling(double factor) {
        this.m00 = factor;
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m10 = 0.0;
        this.m11 = factor;
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = factor;
        return this;
    }

    public Matrix3d scaling(double x, double y, double z) {
        this.m00 = x;
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m10 = 0.0;
        this.m11 = y;
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = z;
        return this;
    }

    public Matrix3d scaling(Vector3d xyz) {
        this.m00 = xyz.x;
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m10 = 0.0;
        this.m11 = xyz.y;
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = xyz.z;
        return this;
    }

    public Matrix3d scale(Vector3d xyz, Matrix3d dest) {
        return this.scale(xyz.x, xyz.y, xyz.z, dest);
    }

    public Matrix3d scale(Vector3d xyz) {
        return this.scale(xyz.x, xyz.y, xyz.z, this);
    }

    public Matrix3d scale(double x, double y, double z, Matrix3d dest) {
        dest.m00 = this.m00 * x;
        dest.m01 = this.m01 * x;
        dest.m02 = this.m02 * x;
        dest.m10 = this.m10 * y;
        dest.m11 = this.m11 * y;
        dest.m12 = this.m12 * y;
        dest.m20 = this.m20 * z;
        dest.m21 = this.m21 * z;
        dest.m22 = this.m22 * z;
        return dest;
    }

    public Matrix3d scale(double x, double y, double z) {
        return this.scale(x, y, z, this);
    }

    public Matrix3d scale(double xyz, Matrix3d dest) {
        return this.scale(xyz, xyz, xyz, dest);
    }

    public Matrix3d scale(double xyz) {
        return this.scale(xyz, xyz, xyz);
    }

    public Matrix3d scaleLocal(double x, double y, double z, Matrix3d dest) {
        double nm00 = x * this.m00;
        double nm01 = y * this.m01;
        double nm02 = z * this.m02;
        double nm10 = x * this.m10;
        double nm11 = y * this.m11;
        double nm12 = z * this.m12;
        double nm20 = x * this.m20;
        double nm21 = y * this.m21;
        double nm22 = z * this.m22;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        return dest;
    }

    public Matrix3d scaleLocal(double x, double y, double z) {
        return this.scaleLocal(x, y, z, this);
    }

    public Matrix3d rotation(double angle, Vector3d axis) {
        return this.rotation(angle, axis.x, axis.y, axis.z);
    }

    public Matrix3d rotation(double angle, Vector3f axis) {
        return this.rotation(angle, axis.x, axis.y, axis.z);
    }

    public Matrix3d rotation(AxisAngle4f axisAngle) {
        return this.rotation(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Matrix3d rotation(AxisAngle4d axisAngle) {
        return this.rotation(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Matrix3d rotation(double angle, double x, double y, double z) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double C = 1.0 - cos;
        double xy = x * y;
        double xz = x * z;
        double yz = y * z;
        this.m00 = cos + x * x * C;
        this.m10 = xy * C - z * sin;
        this.m20 = xz * C + y * sin;
        this.m01 = xy * C + z * sin;
        this.m11 = cos + y * y * C;
        this.m21 = yz * C - x * sin;
        this.m02 = xz * C - y * sin;
        this.m12 = yz * C + x * sin;
        this.m22 = cos + z * z * C;
        return this;
    }

    public Matrix3d rotationX(double ang) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        this.m00 = 1.0;
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m10 = 0.0;
        this.m11 = cos;
        this.m12 = sin;
        this.m20 = 0.0;
        this.m21 = -sin;
        this.m22 = cos;
        return this;
    }

    public Matrix3d rotationY(double ang) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        this.m00 = cos;
        this.m01 = 0.0;
        this.m02 = -sin;
        this.m10 = 0.0;
        this.m11 = 1.0;
        this.m12 = 0.0;
        this.m20 = sin;
        this.m21 = 0.0;
        this.m22 = cos;
        return this;
    }

    public Matrix3d rotationZ(double ang) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        this.m00 = cos;
        this.m01 = sin;
        this.m02 = 0.0;
        this.m10 = -sin;
        this.m11 = cos;
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = 1.0;
        return this;
    }

    public Matrix3d rotationXYZ(double angleX, double angleY, double angleZ) {
        double sinX = Math.sin(angleX);
        double cosX = Math.cosFromSin(sinX, angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cosFromSin(sinY, angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cosFromSin(sinZ, angleZ);
        double m_sinX = -sinX;
        double m_sinY = -sinY;
        double m_sinZ = -sinZ;
        double nm01 = m_sinX * m_sinY;
        double nm02 = cosX * m_sinY;
        this.m20 = sinY;
        this.m21 = m_sinX * cosY;
        this.m22 = cosX * cosY;
        this.m00 = cosY * cosZ;
        this.m01 = nm01 * cosZ + cosX * sinZ;
        this.m02 = nm02 * cosZ + sinX * sinZ;
        this.m10 = cosY * m_sinZ;
        this.m11 = nm01 * m_sinZ + cosX * cosZ;
        this.m12 = nm02 * m_sinZ + sinX * cosZ;
        return this;
    }

    public Matrix3d rotationZYX(double angleZ, double angleY, double angleX) {
        double sinX = Math.sin(angleX);
        double cosX = Math.cosFromSin(sinX, angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cosFromSin(sinY, angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cosFromSin(sinZ, angleZ);
        double m_sinZ = -sinZ;
        double m_sinY = -sinY;
        double m_sinX = -sinX;
        double nm20 = cosZ * sinY;
        double nm21 = sinZ * sinY;
        this.m00 = cosZ * cosY;
        this.m01 = sinZ * cosY;
        this.m02 = m_sinY;
        this.m10 = m_sinZ * cosX + nm20 * sinX;
        this.m11 = cosZ * cosX + nm21 * sinX;
        this.m12 = cosY * sinX;
        this.m20 = m_sinZ * m_sinX + nm20 * cosX;
        this.m21 = cosZ * m_sinX + nm21 * cosX;
        this.m22 = cosY * cosX;
        return this;
    }

    public Matrix3d rotationYXZ(double angleY, double angleX, double angleZ) {
        double sinX = Math.sin(angleX);
        double cosX = Math.cosFromSin(sinX, angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cosFromSin(sinY, angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cosFromSin(sinZ, angleZ);
        double m_sinY = -sinY;
        double m_sinX = -sinX;
        double m_sinZ = -sinZ;
        double nm10 = sinY * sinX;
        double nm12 = cosY * sinX;
        this.m20 = sinY * cosX;
        this.m21 = m_sinX;
        this.m22 = cosY * cosX;
        this.m00 = cosY * cosZ + nm10 * sinZ;
        this.m01 = cosX * sinZ;
        this.m02 = m_sinY * cosZ + nm12 * sinZ;
        this.m10 = cosY * m_sinZ + nm10 * cosZ;
        this.m11 = cosX * cosZ;
        this.m12 = m_sinY * m_sinZ + nm12 * cosZ;
        return this;
    }

    public Matrix3d rotation(Quaterniond quat) {
        double w2 = quat.w * quat.w;
        double x2 = quat.x * quat.x;
        double y2 = quat.y * quat.y;
        double z2 = quat.z * quat.z;
        double zw = quat.z * quat.w;
        double dzw = zw + zw;
        double xy = quat.x * quat.y;
        double dxy = xy + xy;
        double xz = quat.x * quat.z;
        double dxz = xz + xz;
        double yw = quat.y * quat.w;
        double dyw = yw + yw;
        double yz = quat.y * quat.z;
        double dyz = yz + yz;
        double xw = quat.x * quat.w;
        double dxw = xw + xw;
        this.m00 = w2 + x2 - z2 - y2;
        this.m01 = dxy + dzw;
        this.m02 = dxz - dyw;
        this.m10 = -dzw + dxy;
        this.m11 = y2 - z2 + w2 - x2;
        this.m12 = dyz + dxw;
        this.m20 = dyw + dxz;
        this.m21 = dyz - dxw;
        this.m22 = z2 - y2 - x2 + w2;
        return this;
    }

    public Matrix3d rotation(Quaternionf quat) {
        double w2 = quat.w * quat.w;
        double x2 = quat.x * quat.x;
        double y2 = quat.y * quat.y;
        double z2 = quat.z * quat.z;
        double zw = quat.z * quat.w;
        double dzw = zw + zw;
        double xy = quat.x * quat.y;
        double dxy = xy + xy;
        double xz = quat.x * quat.z;
        double dxz = xz + xz;
        double yw = quat.y * quat.w;
        double dyw = yw + yw;
        double yz = quat.y * quat.z;
        double dyz = yz + yz;
        double xw = quat.x * quat.w;
        double dxw = xw + xw;
        this.m00 = w2 + x2 - z2 - y2;
        this.m01 = dxy + dzw;
        this.m02 = dxz - dyw;
        this.m10 = -dzw + dxy;
        this.m11 = y2 - z2 + w2 - x2;
        this.m12 = dyz + dxw;
        this.m20 = dyw + dxz;
        this.m21 = dyz - dxw;
        this.m22 = z2 - y2 - x2 + w2;
        return this;
    }

    public Vector3d transform(Vector3d v) {
        return v.mul(this);
    }

    public Vector3d transform(Vector3d v, Vector3d dest) {
        v.mul(this, dest);
        return dest;
    }

    public Vector3f transform(Vector3f v) {
        return v.mul(this);
    }

    public Vector3f transform(Vector3f v, Vector3f dest) {
        return v.mul(this, dest);
    }

    public Vector3d transform(double x, double y, double z, Vector3d dest) {
        return dest.set(Math.fma(this.m00, x, Math.fma(this.m10, y, this.m20 * z)), Math.fma(this.m01, x, Math.fma(this.m11, y, this.m21 * z)), Math.fma(this.m02, x, Math.fma(this.m12, y, this.m22 * z)));
    }

    public Vector3d transformTranspose(Vector3d v) {
        return v.mulTranspose(this);
    }

    public Vector3d transformTranspose(Vector3d v, Vector3d dest) {
        return v.mulTranspose(this, dest);
    }

    public Vector3d transformTranspose(double x, double y, double z, Vector3d dest) {
        return dest.set(Math.fma(this.m00, x, Math.fma(this.m01, y, this.m02 * z)), Math.fma(this.m10, x, Math.fma(this.m11, y, this.m12 * z)), Math.fma(this.m20, x, Math.fma(this.m21, y, this.m22 * z)));
    }

    public Matrix3d rotateX(double ang, Matrix3d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        double rm21 = -sin;
        double nm10 = this.m10 * cos + this.m20 * sin;
        double nm11 = this.m11 * cos + this.m21 * sin;
        double nm12 = this.m12 * cos + this.m22 * sin;
        dest.m20 = this.m10 * rm21 + this.m20 * cos;
        dest.m21 = this.m11 * rm21 + this.m21 * cos;
        dest.m22 = this.m12 * rm21 + this.m22 * cos;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m00 = this.m00;
        dest.m01 = this.m01;
        dest.m02 = this.m02;
        return dest;
    }

    public Matrix3d rotateX(double ang) {
        return this.rotateX(ang, this);
    }

    public Matrix3d rotateY(double ang, Matrix3d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        double rm02 = -sin;
        double nm00 = this.m00 * cos + this.m20 * rm02;
        double nm01 = this.m01 * cos + this.m21 * rm02;
        double nm02 = this.m02 * cos + this.m22 * rm02;
        dest.m20 = this.m00 * sin + this.m20 * cos;
        dest.m21 = this.m01 * sin + this.m21 * cos;
        dest.m22 = this.m02 * sin + this.m22 * cos;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = this.m10;
        dest.m11 = this.m11;
        dest.m12 = this.m12;
        return dest;
    }

    public Matrix3d rotateY(double ang) {
        return this.rotateY(ang, this);
    }

    public Matrix3d rotateZ(double ang, Matrix3d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        double rm10 = -sin;
        double nm00 = this.m00 * cos + this.m10 * sin;
        double nm01 = this.m01 * cos + this.m11 * sin;
        double nm02 = this.m02 * cos + this.m12 * sin;
        dest.m10 = this.m00 * rm10 + this.m10 * cos;
        dest.m11 = this.m01 * rm10 + this.m11 * cos;
        dest.m12 = this.m02 * rm10 + this.m12 * cos;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m20 = this.m20;
        dest.m21 = this.m21;
        dest.m22 = this.m22;
        return dest;
    }

    public Matrix3d rotateZ(double ang) {
        return this.rotateZ(ang, this);
    }

    public Matrix3d rotateXYZ(double angleX, double angleY, double angleZ) {
        return this.rotateXYZ(angleX, angleY, angleZ, this);
    }

    public Matrix3d rotateXYZ(double angleX, double angleY, double angleZ, Matrix3d dest) {
        double sinX = Math.sin(angleX);
        double cosX = Math.cosFromSin(sinX, angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cosFromSin(sinY, angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cosFromSin(sinZ, angleZ);
        double m_sinX = -sinX;
        double m_sinY = -sinY;
        double m_sinZ = -sinZ;
        double nm10 = this.m10 * cosX + this.m20 * sinX;
        double nm11 = this.m11 * cosX + this.m21 * sinX;
        double nm12 = this.m12 * cosX + this.m22 * sinX;
        double nm20 = this.m10 * m_sinX + this.m20 * cosX;
        double nm21 = this.m11 * m_sinX + this.m21 * cosX;
        double nm22 = this.m12 * m_sinX + this.m22 * cosX;
        double nm00 = this.m00 * cosY + nm20 * m_sinY;
        double nm01 = this.m01 * cosY + nm21 * m_sinY;
        double nm02 = this.m02 * cosY + nm22 * m_sinY;
        dest.m20 = this.m00 * sinY + nm20 * cosY;
        dest.m21 = this.m01 * sinY + nm21 * cosY;
        dest.m22 = this.m02 * sinY + nm22 * cosY;
        dest.m00 = nm00 * cosZ + nm10 * sinZ;
        dest.m01 = nm01 * cosZ + nm11 * sinZ;
        dest.m02 = nm02 * cosZ + nm12 * sinZ;
        dest.m10 = nm00 * m_sinZ + nm10 * cosZ;
        dest.m11 = nm01 * m_sinZ + nm11 * cosZ;
        dest.m12 = nm02 * m_sinZ + nm12 * cosZ;
        return dest;
    }

    public Matrix3d rotateZYX(double angleZ, double angleY, double angleX) {
        return this.rotateZYX(angleZ, angleY, angleX, this);
    }

    public Matrix3d rotateZYX(double angleZ, double angleY, double angleX, Matrix3d dest) {
        double sinX = Math.sin(angleX);
        double cosX = Math.cosFromSin(sinX, angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cosFromSin(sinY, angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cosFromSin(sinZ, angleZ);
        double m_sinZ = -sinZ;
        double m_sinY = -sinY;
        double m_sinX = -sinX;
        double nm00 = this.m00 * cosZ + this.m10 * sinZ;
        double nm01 = this.m01 * cosZ + this.m11 * sinZ;
        double nm02 = this.m02 * cosZ + this.m12 * sinZ;
        double nm10 = this.m00 * m_sinZ + this.m10 * cosZ;
        double nm11 = this.m01 * m_sinZ + this.m11 * cosZ;
        double nm12 = this.m02 * m_sinZ + this.m12 * cosZ;
        double nm20 = nm00 * sinY + this.m20 * cosY;
        double nm21 = nm01 * sinY + this.m21 * cosY;
        double nm22 = nm02 * sinY + this.m22 * cosY;
        dest.m00 = nm00 * cosY + this.m20 * m_sinY;
        dest.m01 = nm01 * cosY + this.m21 * m_sinY;
        dest.m02 = nm02 * cosY + this.m22 * m_sinY;
        dest.m10 = nm10 * cosX + nm20 * sinX;
        dest.m11 = nm11 * cosX + nm21 * sinX;
        dest.m12 = nm12 * cosX + nm22 * sinX;
        dest.m20 = nm10 * m_sinX + nm20 * cosX;
        dest.m21 = nm11 * m_sinX + nm21 * cosX;
        dest.m22 = nm12 * m_sinX + nm22 * cosX;
        return dest;
    }

    public Matrix3d rotateYXZ(Vector3d angles) {
        return this.rotateYXZ(angles.y, angles.x, angles.z);
    }

    public Matrix3d rotateYXZ(double angleY, double angleX, double angleZ) {
        return this.rotateYXZ(angleY, angleX, angleZ, this);
    }

    public Matrix3d rotateYXZ(double angleY, double angleX, double angleZ, Matrix3d dest) {
        double sinX = Math.sin(angleX);
        double cosX = Math.cosFromSin(sinX, angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cosFromSin(sinY, angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cosFromSin(sinZ, angleZ);
        double m_sinY = -sinY;
        double m_sinX = -sinX;
        double m_sinZ = -sinZ;
        double nm20 = this.m00 * sinY + this.m20 * cosY;
        double nm21 = this.m01 * sinY + this.m21 * cosY;
        double nm22 = this.m02 * sinY + this.m22 * cosY;
        double nm00 = this.m00 * cosY + this.m20 * m_sinY;
        double nm01 = this.m01 * cosY + this.m21 * m_sinY;
        double nm02 = this.m02 * cosY + this.m22 * m_sinY;
        double nm10 = this.m10 * cosX + nm20 * sinX;
        double nm11 = this.m11 * cosX + nm21 * sinX;
        double nm12 = this.m12 * cosX + nm22 * sinX;
        dest.m20 = this.m10 * m_sinX + nm20 * cosX;
        dest.m21 = this.m11 * m_sinX + nm21 * cosX;
        dest.m22 = this.m12 * m_sinX + nm22 * cosX;
        dest.m00 = nm00 * cosZ + nm10 * sinZ;
        dest.m01 = nm01 * cosZ + nm11 * sinZ;
        dest.m02 = nm02 * cosZ + nm12 * sinZ;
        dest.m10 = nm00 * m_sinZ + nm10 * cosZ;
        dest.m11 = nm01 * m_sinZ + nm11 * cosZ;
        dest.m12 = nm02 * m_sinZ + nm12 * cosZ;
        return dest;
    }

    public Matrix3d rotate(double ang, double x, double y, double z) {
        return this.rotate(ang, x, y, z, this);
    }

    public Matrix3d rotate(double ang, double x, double y, double z, Matrix3d dest) {
        double s = Math.sin(ang);
        double c = Math.cosFromSin(s, ang);
        double C = 1.0 - c;
        double xx = x * x;
        double xy = x * y;
        double xz = x * z;
        double yy = y * y;
        double yz = y * z;
        double zz = z * z;
        double rm00 = xx * C + c;
        double rm01 = xy * C + z * s;
        double rm02 = xz * C - y * s;
        double rm10 = xy * C - z * s;
        double rm11 = yy * C + c;
        double rm12 = yz * C + x * s;
        double rm20 = xz * C + y * s;
        double rm21 = yz * C - x * s;
        double rm22 = zz * C + c;
        double nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        double nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        double nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        dest.m20 = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22;
        dest.m21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22;
        dest.m22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        return dest;
    }

    public Matrix3d rotateLocal(double ang, double x, double y, double z, Matrix3d dest) {
        double s = Math.sin(ang);
        double c = Math.cosFromSin(s, ang);
        double C = 1.0 - c;
        double xx = x * x;
        double xy = x * y;
        double xz = x * z;
        double yy = y * y;
        double yz = y * z;
        double zz = z * z;
        double lm00 = xx * C + c;
        double lm01 = xy * C + z * s;
        double lm02 = xz * C - y * s;
        double lm10 = xy * C - z * s;
        double lm11 = yy * C + c;
        double lm12 = yz * C + x * s;
        double lm20 = xz * C + y * s;
        double lm21 = yz * C - x * s;
        double lm22 = zz * C + c;
        double nm00 = lm00 * this.m00 + lm10 * this.m01 + lm20 * this.m02;
        double nm01 = lm01 * this.m00 + lm11 * this.m01 + lm21 * this.m02;
        double nm02 = lm02 * this.m00 + lm12 * this.m01 + lm22 * this.m02;
        double nm10 = lm00 * this.m10 + lm10 * this.m11 + lm20 * this.m12;
        double nm11 = lm01 * this.m10 + lm11 * this.m11 + lm21 * this.m12;
        double nm12 = lm02 * this.m10 + lm12 * this.m11 + lm22 * this.m12;
        double nm20 = lm00 * this.m20 + lm10 * this.m21 + lm20 * this.m22;
        double nm21 = lm01 * this.m20 + lm11 * this.m21 + lm21 * this.m22;
        double nm22 = lm02 * this.m20 + lm12 * this.m21 + lm22 * this.m22;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        return dest;
    }

    public Matrix3d rotateLocal(double ang, double x, double y, double z) {
        return this.rotateLocal(ang, x, y, z, this);
    }

    public Matrix3d rotateLocalX(double ang, Matrix3d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        double nm01 = cos * this.m01 - sin * this.m02;
        double nm02 = sin * this.m01 + cos * this.m02;
        double nm11 = cos * this.m11 - sin * this.m12;
        double nm12 = sin * this.m11 + cos * this.m12;
        double nm21 = cos * this.m21 - sin * this.m22;
        double nm22 = sin * this.m21 + cos * this.m22;
        dest.m00 = this.m00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = this.m10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = this.m20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        return dest;
    }

    public Matrix3d rotateLocalX(double ang) {
        return this.rotateLocalX(ang, this);
    }

    public Matrix3d rotateLocalY(double ang, Matrix3d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        double nm00 = cos * this.m00 + sin * this.m02;
        double nm02 = -sin * this.m00 + cos * this.m02;
        double nm10 = cos * this.m10 + sin * this.m12;
        double nm12 = -sin * this.m10 + cos * this.m12;
        double nm20 = cos * this.m20 + sin * this.m22;
        double nm22 = -sin * this.m20 + cos * this.m22;
        dest.m00 = nm00;
        dest.m01 = this.m01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = this.m11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = this.m21;
        dest.m22 = nm22;
        return dest;
    }

    public Matrix3d rotateLocalY(double ang) {
        return this.rotateLocalY(ang, this);
    }

    public Matrix3d rotateLocalZ(double ang, Matrix3d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        double nm00 = cos * this.m00 - sin * this.m01;
        double nm01 = sin * this.m00 + cos * this.m01;
        double nm10 = cos * this.m10 - sin * this.m11;
        double nm11 = sin * this.m10 + cos * this.m11;
        double nm20 = cos * this.m20 - sin * this.m21;
        double nm21 = sin * this.m20 + cos * this.m21;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = this.m02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = this.m12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = this.m22;
        return dest;
    }

    public Matrix3d rotateLocalZ(double ang) {
        return this.rotateLocalZ(ang, this);
    }

    public Matrix3d rotateLocal(Quaterniond quat, Matrix3d dest) {
        double w2 = quat.w * quat.w;
        double x2 = quat.x * quat.x;
        double y2 = quat.y * quat.y;
        double z2 = quat.z * quat.z;
        double zw = quat.z * quat.w;
        double dzw = zw + zw;
        double xy = quat.x * quat.y;
        double dxy = xy + xy;
        double xz = quat.x * quat.z;
        double dxz = xz + xz;
        double yw = quat.y * quat.w;
        double dyw = yw + yw;
        double yz = quat.y * quat.z;
        double dyz = yz + yz;
        double xw = quat.x * quat.w;
        double dxw = xw + xw;
        double lm00 = w2 + x2 - z2 - y2;
        double lm01 = dxy + dzw;
        double lm02 = dxz - dyw;
        double lm10 = dxy - dzw;
        double lm11 = y2 - z2 + w2 - x2;
        double lm12 = dyz + dxw;
        double lm20 = dyw + dxz;
        double lm21 = dyz - dxw;
        double lm22 = z2 - y2 - x2 + w2;
        double nm00 = lm00 * this.m00 + lm10 * this.m01 + lm20 * this.m02;
        double nm01 = lm01 * this.m00 + lm11 * this.m01 + lm21 * this.m02;
        double nm02 = lm02 * this.m00 + lm12 * this.m01 + lm22 * this.m02;
        double nm10 = lm00 * this.m10 + lm10 * this.m11 + lm20 * this.m12;
        double nm11 = lm01 * this.m10 + lm11 * this.m11 + lm21 * this.m12;
        double nm12 = lm02 * this.m10 + lm12 * this.m11 + lm22 * this.m12;
        double nm20 = lm00 * this.m20 + lm10 * this.m21 + lm20 * this.m22;
        double nm21 = lm01 * this.m20 + lm11 * this.m21 + lm21 * this.m22;
        double nm22 = lm02 * this.m20 + lm12 * this.m21 + lm22 * this.m22;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        return dest;
    }

    public Matrix3d rotateLocal(Quaterniond quat) {
        return this.rotateLocal(quat, this);
    }

    public Matrix3d rotateLocal(Quaternionf quat, Matrix3d dest) {
        double w2 = quat.w * quat.w;
        double x2 = quat.x * quat.x;
        double y2 = quat.y * quat.y;
        double z2 = quat.z * quat.z;
        double zw = quat.z * quat.w;
        double dzw = zw + zw;
        double xy = quat.x * quat.y;
        double dxy = xy + xy;
        double xz = quat.x * quat.z;
        double dxz = xz + xz;
        double yw = quat.y * quat.w;
        double dyw = yw + yw;
        double yz = quat.y * quat.z;
        double dyz = yz + yz;
        double xw = quat.x * quat.w;
        double dxw = xw + xw;
        double lm00 = w2 + x2 - z2 - y2;
        double lm01 = dxy + dzw;
        double lm02 = dxz - dyw;
        double lm10 = dxy - dzw;
        double lm11 = y2 - z2 + w2 - x2;
        double lm12 = dyz + dxw;
        double lm20 = dyw + dxz;
        double lm21 = dyz - dxw;
        double lm22 = z2 - y2 - x2 + w2;
        double nm00 = lm00 * this.m00 + lm10 * this.m01 + lm20 * this.m02;
        double nm01 = lm01 * this.m00 + lm11 * this.m01 + lm21 * this.m02;
        double nm02 = lm02 * this.m00 + lm12 * this.m01 + lm22 * this.m02;
        double nm10 = lm00 * this.m10 + lm10 * this.m11 + lm20 * this.m12;
        double nm11 = lm01 * this.m10 + lm11 * this.m11 + lm21 * this.m12;
        double nm12 = lm02 * this.m10 + lm12 * this.m11 + lm22 * this.m12;
        double nm20 = lm00 * this.m20 + lm10 * this.m21 + lm20 * this.m22;
        double nm21 = lm01 * this.m20 + lm11 * this.m21 + lm21 * this.m22;
        double nm22 = lm02 * this.m20 + lm12 * this.m21 + lm22 * this.m22;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        return dest;
    }

    public Matrix3d rotateLocal(Quaternionf quat) {
        return this.rotateLocal(quat, this);
    }

    public Matrix3d rotate(Quaterniond quat) {
        return this.rotate(quat, this);
    }

    public Matrix3d rotate(Quaterniond quat, Matrix3d dest) {
        double w2 = quat.w * quat.w;
        double x2 = quat.x * quat.x;
        double y2 = quat.y * quat.y;
        double z2 = quat.z * quat.z;
        double zw = quat.z * quat.w;
        double dzw = zw + zw;
        double xy = quat.x * quat.y;
        double dxy = xy + xy;
        double xz = quat.x * quat.z;
        double dxz = xz + xz;
        double yw = quat.y * quat.w;
        double dyw = yw + yw;
        double yz = quat.y * quat.z;
        double dyz = yz + yz;
        double xw = quat.x * quat.w;
        double dxw = xw + xw;
        double rm00 = w2 + x2 - z2 - y2;
        double rm01 = dxy + dzw;
        double rm02 = dxz - dyw;
        double rm10 = dxy - dzw;
        double rm11 = y2 - z2 + w2 - x2;
        double rm12 = dyz + dxw;
        double rm20 = dyw + dxz;
        double rm21 = dyz - dxw;
        double rm22 = z2 - y2 - x2 + w2;
        double nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        double nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        double nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        dest.m20 = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22;
        dest.m21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22;
        dest.m22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        return dest;
    }

    public Matrix3d rotate(Quaternionf quat) {
        return this.rotate(quat, this);
    }

    public Matrix3d rotate(Quaternionf quat, Matrix3d dest) {
        double w2 = quat.w * quat.w;
        double x2 = quat.x * quat.x;
        double y2 = quat.y * quat.y;
        double z2 = quat.z * quat.z;
        double zw = quat.z * quat.w;
        double dzw = zw + zw;
        double xy = quat.x * quat.y;
        double dxy = xy + xy;
        double xz = quat.x * quat.z;
        double dxz = xz + xz;
        double yw = quat.y * quat.w;
        double dyw = yw + yw;
        double yz = quat.y * quat.z;
        double dyz = yz + yz;
        double xw = quat.x * quat.w;
        double dxw = xw + xw;
        double rm00 = w2 + x2 - z2 - y2;
        double rm01 = dxy + dzw;
        double rm02 = dxz - dyw;
        double rm10 = dxy - dzw;
        double rm11 = y2 - z2 + w2 - x2;
        double rm12 = dyz + dxw;
        double rm20 = dyw + dxz;
        double rm21 = dyz - dxw;
        double rm22 = z2 - y2 - x2 + w2;
        double nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        double nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        double nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        dest.m20 = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22;
        dest.m21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22;
        dest.m22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        return dest;
    }

    public Matrix3d rotate(AxisAngle4f axisAngle) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Matrix3d rotate(AxisAngle4f axisAngle, Matrix3d dest) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dest);
    }

    public Matrix3d rotate(AxisAngle4d axisAngle) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Matrix3d rotate(AxisAngle4d axisAngle, Matrix3d dest) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dest);
    }

    public Matrix3d rotate(double angle, Vector3d axis) {
        return this.rotate(angle, axis.x, axis.y, axis.z);
    }

    public Matrix3d rotate(double angle, Vector3d axis, Matrix3d dest) {
        return this.rotate(angle, axis.x, axis.y, axis.z, dest);
    }

    public Matrix3d rotate(double angle, Vector3f axis) {
        return this.rotate(angle, axis.x, axis.y, axis.z);
    }

    public Matrix3d rotate(double angle, Vector3f axis, Matrix3d dest) {
        return this.rotate(angle, axis.x, axis.y, axis.z, dest);
    }

    public Vector3d getRow(int row, Vector3d dest) throws IndexOutOfBoundsException {
        switch (row) {
            case 0:
                return dest.set(this.m00, this.m10, this.m20);
            case 1:
                return dest.set(this.m01, this.m11, this.m21);
            case 2:
                return dest.set(this.m02, this.m12, this.m22);
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public Matrix3d setRow(int row, Vector3d src) throws IndexOutOfBoundsException {
        return this.setRow(row, src.x, src.y, src.z);
    }

    public Matrix3d setRow(int row, double x, double y, double z) throws IndexOutOfBoundsException {
        switch (row) {
            case 0:
                this.m00 = x;
                this.m10 = y;
                this.m20 = z;
                break;
            case 1:
                this.m01 = x;
                this.m11 = y;
                this.m21 = z;
                break;
            case 2:
                this.m02 = x;
                this.m12 = y;
                this.m22 = z;
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

        return this;
    }

    public Vector3d getColumn(int column, Vector3d dest) throws IndexOutOfBoundsException {
        switch (column) {
            case 0:
                return dest.set(this.m00, this.m01, this.m02);
            case 1:
                return dest.set(this.m10, this.m11, this.m12);
            case 2:
                return dest.set(this.m20, this.m21, this.m22);
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public Matrix3d setColumn(int column, Vector3d src) throws IndexOutOfBoundsException {
        return this.setColumn(column, src.x, src.y, src.z);
    }

    public Matrix3d setColumn(int column, double x, double y, double z) throws IndexOutOfBoundsException {
        switch (column) {
            case 0:
                this.m00 = x;
                this.m01 = y;
                this.m02 = z;
                break;
            case 1:
                this.m10 = x;
                this.m11 = y;
                this.m12 = z;
                break;
            case 2:
                this.m20 = x;
                this.m21 = y;
                this.m22 = z;
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

        return this;
    }

    public double get(int column, int row) {
        return MemUtil.INSTANCE.get(this, column, row);
    }

    public Matrix3d set(int column, int row, double value) {
        return MemUtil.INSTANCE.set(this, column, row, value);
    }

    public double getRowColumn(int row, int column) {
        return MemUtil.INSTANCE.get(this, column, row);
    }

    public Matrix3d setRowColumn(int row, int column, double value) {
        return MemUtil.INSTANCE.set(this, column, row, value);
    }

    public Matrix3d normal() {
        return this.normal(this);
    }

    public Matrix3d normal(Matrix3d dest) {
        double m00m11 = this.m00 * this.m11;
        double m01m10 = this.m01 * this.m10;
        double m02m10 = this.m02 * this.m10;
        double m00m12 = this.m00 * this.m12;
        double m01m12 = this.m01 * this.m12;
        double m02m11 = this.m02 * this.m11;
        double det = (m00m11 - m01m10) * this.m22 + (m02m10 - m00m12) * this.m21 + (m01m12 - m02m11) * this.m20;
        double s = 1.0 / det;
        double nm00 = (this.m11 * this.m22 - this.m21 * this.m12) * s;
        double nm01 = (this.m20 * this.m12 - this.m10 * this.m22) * s;
        double nm02 = (this.m10 * this.m21 - this.m20 * this.m11) * s;
        double nm10 = (this.m21 * this.m02 - this.m01 * this.m22) * s;
        double nm11 = (this.m00 * this.m22 - this.m20 * this.m02) * s;
        double nm12 = (this.m20 * this.m01 - this.m00 * this.m21) * s;
        double nm20 = (m01m12 - m02m11) * s;
        double nm21 = (m02m10 - m00m12) * s;
        double nm22 = (m00m11 - m01m10) * s;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        return dest;
    }

    public Matrix3d cofactor() {
        return this.cofactor(this);
    }

    public Matrix3d cofactor(Matrix3d dest) {
        double nm00 = this.m11 * this.m22 - this.m21 * this.m12;
        double nm01 = this.m20 * this.m12 - this.m10 * this.m22;
        double nm02 = this.m10 * this.m21 - this.m20 * this.m11;
        double nm10 = this.m21 * this.m02 - this.m01 * this.m22;
        double nm11 = this.m00 * this.m22 - this.m20 * this.m02;
        double nm12 = this.m20 * this.m01 - this.m00 * this.m21;
        double nm20 = this.m01 * this.m12 - this.m11 * this.m02;
        double nm21 = this.m02 * this.m10 - this.m12 * this.m00;
        double nm22 = this.m00 * this.m11 - this.m10 * this.m01;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        return dest;
    }

    public Matrix3d lookAlong(Vector3d dir, Vector3d up) {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this);
    }

    public Matrix3d lookAlong(Vector3d dir, Vector3d up, Matrix3d dest) {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dest);
    }

    public Matrix3d lookAlong(double dirX, double dirY, double dirZ, double upX, double upY, double upZ, Matrix3d dest) {
        double invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= -invDirLength;
        dirY *= -invDirLength;
        dirZ *= -invDirLength;
        double leftX = upY * dirZ - upZ * dirY;
        double leftY = upZ * dirX - upX * dirZ;
        double leftZ = upX * dirY - upY * dirX;
        double invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLength;
        leftY *= invLeftLength;
        leftZ *= invLeftLength;
        double upnX = dirY * leftZ - dirZ * leftY;
        double upnY = dirZ * leftX - dirX * leftZ;
        double upnZ = dirX * leftY - dirY * leftX;
        double nm00 = this.m00 * leftX + this.m10 * upnX + this.m20 * dirX;
        double nm01 = this.m01 * leftX + this.m11 * upnX + this.m21 * dirX;
        double nm02 = this.m02 * leftX + this.m12 * upnX + this.m22 * dirX;
        double nm10 = this.m00 * leftY + this.m10 * upnY + this.m20 * dirY;
        double nm11 = this.m01 * leftY + this.m11 * upnY + this.m21 * dirY;
        double nm12 = this.m02 * leftY + this.m12 * upnY + this.m22 * dirY;
        dest.m20 = this.m00 * leftZ + this.m10 * upnZ + this.m20 * dirZ;
        dest.m21 = this.m01 * leftZ + this.m11 * upnZ + this.m21 * dirZ;
        dest.m22 = this.m02 * leftZ + this.m12 * upnZ + this.m22 * dirZ;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        return dest;
    }

    public Matrix3d lookAlong(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
        return this.lookAlong(dirX, dirY, dirZ, upX, upY, upZ, this);
    }

    public Matrix3d setLookAlong(Vector3d dir, Vector3d up) {
        return this.setLookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    public Matrix3d setLookAlong(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
        double invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= -invDirLength;
        dirY *= -invDirLength;
        dirZ *= -invDirLength;
        double leftX = upY * dirZ - upZ * dirY;
        double leftY = upZ * dirX - upX * dirZ;
        double leftZ = upX * dirY - upY * dirX;
        double invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLength;
        leftY *= invLeftLength;
        leftZ *= invLeftLength;
        double upnX = dirY * leftZ - dirZ * leftY;
        double upnY = dirZ * leftX - dirX * leftZ;
        double upnZ = dirX * leftY - dirY * leftX;
        this.m00 = leftX;
        this.m01 = upnX;
        this.m02 = dirX;
        this.m10 = leftY;
        this.m11 = upnY;
        this.m12 = dirY;
        this.m20 = leftZ;
        this.m21 = upnZ;
        this.m22 = dirZ;
        return this;
    }

    public Vector3d getScale(Vector3d dest) {
        dest.x = Math.sqrt(this.m00 * this.m00 + this.m01 * this.m01 + this.m02 * this.m02);
        dest.y = Math.sqrt(this.m10 * this.m10 + this.m11 * this.m11 + this.m12 * this.m12);
        dest.z = Math.sqrt(this.m20 * this.m20 + this.m21 * this.m21 + this.m22 * this.m22);
        return dest;
    }

    public Vector3d positiveZ(Vector3d dir) {
        dir.x = this.m10 * this.m21 - this.m11 * this.m20;
        dir.y = this.m20 * this.m01 - this.m21 * this.m00;
        dir.z = this.m00 * this.m11 - this.m01 * this.m10;
        return dir.normalize(dir);
    }

    public Vector3d normalizedPositiveZ(Vector3d dir) {
        dir.x = this.m02;
        dir.y = this.m12;
        dir.z = this.m22;
        return dir;
    }

    public Vector3d positiveX(Vector3d dir) {
        dir.x = this.m11 * this.m22 - this.m12 * this.m21;
        dir.y = this.m02 * this.m21 - this.m01 * this.m22;
        dir.z = this.m01 * this.m12 - this.m02 * this.m11;
        return dir.normalize(dir);
    }

    public Vector3d normalizedPositiveX(Vector3d dir) {
        dir.x = this.m00;
        dir.y = this.m10;
        dir.z = this.m20;
        return dir;
    }

    public Vector3d positiveY(Vector3d dir) {
        dir.x = this.m12 * this.m20 - this.m10 * this.m22;
        dir.y = this.m00 * this.m22 - this.m02 * this.m20;
        dir.z = this.m02 * this.m10 - this.m00 * this.m12;
        return dir.normalize(dir);
    }

    public Vector3d normalizedPositiveY(Vector3d dir) {
        dir.x = this.m01;
        dir.y = this.m11;
        dir.z = this.m21;
        return dir;
    }

    public int hashCode() {
        int result = 1;
        long temp = Double.doubleToLongBits(this.m00);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m01);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m02);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m10);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m11);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m12);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m20);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m21);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m22);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            Matrix3d other = (Matrix3d) obj;
            if (Double.doubleToLongBits(this.m00) != Double.doubleToLongBits(other.m00)) {
                return false;
            } else if (Double.doubleToLongBits(this.m01) != Double.doubleToLongBits(other.m01)) {
                return false;
            } else if (Double.doubleToLongBits(this.m02) != Double.doubleToLongBits(other.m02)) {
                return false;
            } else if (Double.doubleToLongBits(this.m10) != Double.doubleToLongBits(other.m10)) {
                return false;
            } else if (Double.doubleToLongBits(this.m11) != Double.doubleToLongBits(other.m11)) {
                return false;
            } else if (Double.doubleToLongBits(this.m12) != Double.doubleToLongBits(other.m12)) {
                return false;
            } else if (Double.doubleToLongBits(this.m20) != Double.doubleToLongBits(other.m20)) {
                return false;
            } else if (Double.doubleToLongBits(this.m21) != Double.doubleToLongBits(other.m21)) {
                return false;
            } else {
                return Double.doubleToLongBits(this.m22) == Double.doubleToLongBits(other.m22);
            }
        }
    }

    public boolean equals(Matrix3d m, double delta) {
        if (this == m) {
            return true;
        } else if (m == null) {
            return false;
        } else if (!Runtime.equals(this.m00, m.m00, delta)) {
            return false;
        } else if (!Runtime.equals(this.m01, m.m01, delta)) {
            return false;
        } else if (!Runtime.equals(this.m02, m.m02, delta)) {
            return false;
        } else if (!Runtime.equals(this.m10, m.m10, delta)) {
            return false;
        } else if (!Runtime.equals(this.m11, m.m11, delta)) {
            return false;
        } else if (!Runtime.equals(this.m12, m.m12, delta)) {
            return false;
        } else if (!Runtime.equals(this.m20, m.m20, delta)) {
            return false;
        } else if (!Runtime.equals(this.m21, m.m21, delta)) {
            return false;
        } else {
            return Runtime.equals(this.m22, m.m22, delta);
        }
    }

    public Matrix3d swap(Matrix3d other) {
        double tmp = this.m00;
        this.m00 = other.m00;
        other.m00 = tmp;
        tmp = this.m01;
        this.m01 = other.m01;
        other.m01 = tmp;
        tmp = this.m02;
        this.m02 = other.m02;
        other.m02 = tmp;
        tmp = this.m10;
        this.m10 = other.m10;
        other.m10 = tmp;
        tmp = this.m11;
        this.m11 = other.m11;
        other.m11 = tmp;
        tmp = this.m12;
        this.m12 = other.m12;
        other.m12 = tmp;
        tmp = this.m20;
        this.m20 = other.m20;
        other.m20 = tmp;
        tmp = this.m21;
        this.m21 = other.m21;
        other.m21 = tmp;
        tmp = this.m22;
        this.m22 = other.m22;
        other.m22 = tmp;
        return this;
    }

    public Matrix3d add(Matrix3d other) {
        return this.add(other, this);
    }

    public Matrix3d add(Matrix3d other, Matrix3d dest) {
        dest.m00 = this.m00 + other.m00;
        dest.m01 = this.m01 + other.m01;
        dest.m02 = this.m02 + other.m02;
        dest.m10 = this.m10 + other.m10;
        dest.m11 = this.m11 + other.m11;
        dest.m12 = this.m12 + other.m12;
        dest.m20 = this.m20 + other.m20;
        dest.m21 = this.m21 + other.m21;
        dest.m22 = this.m22 + other.m22;
        return dest;
    }

    public Matrix3d sub(Matrix3d subtrahend) {
        return this.sub(subtrahend, this);
    }

    public Matrix3d sub(Matrix3d subtrahend, Matrix3d dest) {
        dest.m00 = this.m00 - subtrahend.m00;
        dest.m01 = this.m01 - subtrahend.m01;
        dest.m02 = this.m02 - subtrahend.m02;
        dest.m10 = this.m10 - subtrahend.m10;
        dest.m11 = this.m11 - subtrahend.m11;
        dest.m12 = this.m12 - subtrahend.m12;
        dest.m20 = this.m20 - subtrahend.m20;
        dest.m21 = this.m21 - subtrahend.m21;
        dest.m22 = this.m22 - subtrahend.m22;
        return dest;
    }

    public Matrix3d mulComponentWise(Matrix3d other) {
        return this.mulComponentWise(other, this);
    }

    public Matrix3d mulComponentWise(Matrix3d other, Matrix3d dest) {
        dest.m00 = this.m00 * other.m00;
        dest.m01 = this.m01 * other.m01;
        dest.m02 = this.m02 * other.m02;
        dest.m10 = this.m10 * other.m10;
        dest.m11 = this.m11 * other.m11;
        dest.m12 = this.m12 * other.m12;
        dest.m20 = this.m20 * other.m20;
        dest.m21 = this.m21 * other.m21;
        dest.m22 = this.m22 * other.m22;
        return dest;
    }

    public Matrix3d setSkewSymmetric(double a, double b, double c) {
        this.m00 = this.m11 = this.m22 = 0.0;
        this.m01 = -a;
        this.m02 = b;
        this.m10 = a;
        this.m12 = -c;
        this.m20 = -b;
        this.m21 = c;
        return this;
    }

    public Matrix3d lerp(Matrix3d other, double t) {
        return this.lerp(other, t, this);
    }

    public Matrix3d lerp(Matrix3d other, double t, Matrix3d dest) {
        dest.m00 = Math.fma(other.m00 - this.m00, t, this.m00);
        dest.m01 = Math.fma(other.m01 - this.m01, t, this.m01);
        dest.m02 = Math.fma(other.m02 - this.m02, t, this.m02);
        dest.m10 = Math.fma(other.m10 - this.m10, t, this.m10);
        dest.m11 = Math.fma(other.m11 - this.m11, t, this.m11);
        dest.m12 = Math.fma(other.m12 - this.m12, t, this.m12);
        dest.m20 = Math.fma(other.m20 - this.m20, t, this.m20);
        dest.m21 = Math.fma(other.m21 - this.m21, t, this.m21);
        dest.m22 = Math.fma(other.m22 - this.m22, t, this.m22);
        return dest;
    }

    public Matrix3d rotateTowards(Vector3d direction, Vector3d up, Matrix3d dest) {
        return this.rotateTowards(direction.x, direction.y, direction.z, up.x, up.y, up.z, dest);
    }

    public Matrix3d rotateTowards(Vector3d direction, Vector3d up) {
        return this.rotateTowards(direction.x, direction.y, direction.z, up.x, up.y, up.z, this);
    }

    public Matrix3d rotateTowards(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
        return this.rotateTowards(dirX, dirY, dirZ, upX, upY, upZ, this);
    }

    public Matrix3d rotateTowards(double dirX, double dirY, double dirZ, double upX, double upY, double upZ, Matrix3d dest) {
        double invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        double ndirX = dirX * invDirLength;
        double ndirY = dirY * invDirLength;
        double ndirZ = dirZ * invDirLength;
        double leftX = upY * ndirZ - upZ * ndirY;
        double leftY = upZ * ndirX - upX * ndirZ;
        double leftZ = upX * ndirY - upY * ndirX;
        double invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLength;
        leftY *= invLeftLength;
        leftZ *= invLeftLength;
        double upnX = ndirY * leftZ - ndirZ * leftY;
        double upnY = ndirZ * leftX - ndirX * leftZ;
        double upnZ = ndirX * leftY - ndirY * leftX;
        double nm00 = this.m00 * leftX + this.m10 * leftY + this.m20 * leftZ;
        double nm01 = this.m01 * leftX + this.m11 * leftY + this.m21 * leftZ;
        double nm02 = this.m02 * leftX + this.m12 * leftY + this.m22 * leftZ;
        double nm10 = this.m00 * upnX + this.m10 * upnY + this.m20 * upnZ;
        double nm11 = this.m01 * upnX + this.m11 * upnY + this.m21 * upnZ;
        double nm12 = this.m02 * upnX + this.m12 * upnY + this.m22 * upnZ;
        dest.m20 = this.m00 * ndirX + this.m10 * ndirY + this.m20 * ndirZ;
        dest.m21 = this.m01 * ndirX + this.m11 * ndirY + this.m21 * ndirZ;
        dest.m22 = this.m02 * ndirX + this.m12 * ndirY + this.m22 * ndirZ;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        return dest;
    }

    public Matrix3d rotationTowards(Vector3d dir, Vector3d up) {
        return this.rotationTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    public Matrix3d rotationTowards(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
        double invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        double ndirX = dirX * invDirLength;
        double ndirY = dirY * invDirLength;
        double ndirZ = dirZ * invDirLength;
        double leftX = upY * ndirZ - upZ * ndirY;
        double leftY = upZ * ndirX - upX * ndirZ;
        double leftZ = upX * ndirY - upY * ndirX;
        double invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLength;
        leftY *= invLeftLength;
        leftZ *= invLeftLength;
        double upnX = ndirY * leftZ - ndirZ * leftY;
        double upnY = ndirZ * leftX - ndirX * leftZ;
        double upnZ = ndirX * leftY - ndirY * leftX;
        this.m00 = leftX;
        this.m01 = leftY;
        this.m02 = leftZ;
        this.m10 = upnX;
        this.m11 = upnY;
        this.m12 = upnZ;
        this.m20 = ndirX;
        this.m21 = ndirY;
        this.m22 = ndirZ;
        return this;
    }

    public Vector3d getEulerAnglesZYX(Vector3d dest) {
        dest.x = Math.atan2(this.m12, this.m22);
        dest.y = Math.atan2(-this.m02, Math.sqrt(1.0 - this.m02 * this.m02));
        dest.z = Math.atan2(this.m01, this.m00);
        return dest;
    }

    public Vector3d getEulerAnglesXYZ(Vector3d dest) {
        dest.x = Math.atan2(-this.m21, this.m22);
        dest.y = Math.atan2(this.m20, Math.sqrt(1.0 - this.m20 * this.m20));
        dest.z = Math.atan2(-this.m10, this.m00);
        return dest;
    }

    public Matrix3d obliqueZ(double a, double b) {
        this.m20 += this.m00 * a + this.m10 * b;
        this.m21 += this.m01 * a + this.m11 * b;
        this.m22 += this.m02 * a + this.m12 * b;
        return this;
    }

    public Matrix3d obliqueZ(double a, double b, Matrix3d dest) {
        dest.m00 = this.m00;
        dest.m01 = this.m01;
        dest.m02 = this.m02;
        dest.m10 = this.m10;
        dest.m11 = this.m11;
        dest.m12 = this.m12;
        dest.m20 = this.m00 * a + this.m10 * b + this.m20;
        dest.m21 = this.m01 * a + this.m11 * b + this.m21;
        dest.m22 = this.m02 * a + this.m12 * b + this.m22;
        return dest;
    }

    public Matrix3d reflect(double nx, double ny, double nz, Matrix3d dest) {
        double da = nx + nx;
        double db = ny + ny;
        double dc = nz + nz;
        double rm00 = 1.0 - da * nx;
        double rm01 = -da * ny;
        double rm02 = -da * nz;
        double rm10 = -db * nx;
        double rm11 = 1.0 - db * ny;
        double rm12 = -db * nz;
        double rm20 = -dc * nx;
        double rm21 = -dc * ny;
        double rm22 = 1.0 - dc * nz;
        double nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        double nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        double nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        return dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m10(nm10)._m11(nm11)._m12(nm12);
    }

    public Matrix3d reflect(double nx, double ny, double nz) {
        return this.reflect(nx, ny, nz, this);
    }

    public Matrix3d reflect(Vector3d normal) {
        return this.reflect(normal.x, normal.y, normal.z);
    }

    public Matrix3d reflect(Quaterniond orientation) {
        return this.reflect(orientation, this);
    }

    public Matrix3d reflect(Quaterniond orientation, Matrix3d dest) {
        double num1 = orientation.x + orientation.x;
        double num2 = orientation.y + orientation.y;
        double num3 = orientation.z + orientation.z;
        double normalX = orientation.x * num3 + orientation.w * num2;
        double normalY = orientation.y * num3 - orientation.w * num1;
        double normalZ = 1.0 - (orientation.x * num1 + orientation.y * num2);
        return this.reflect(normalX, normalY, normalZ, dest);
    }

    public Matrix3d reflect(Vector3d normal, Matrix3d dest) {
        return this.reflect(normal.x, normal.y, normal.z, dest);
    }

    public Matrix3d reflection(double nx, double ny, double nz) {
        double da = nx + nx;
        double db = ny + ny;
        double dc = nz + nz;
        this._m00(1.0 - da * nx);
        this._m01(-da * ny);
        this._m02(-da * nz);
        this._m10(-db * nx);
        this._m11(1.0 - db * ny);
        this._m12(-db * nz);
        this._m20(-dc * nx);
        this._m21(-dc * ny);
        this._m22(1.0 - dc * nz);
        return this;
    }

    public Matrix3d reflection(Vector3d normal) {
        return this.reflection(normal.x, normal.y, normal.z);
    }

    public Matrix3d reflection(Quaterniond orientation) {
        double num1 = orientation.x + orientation.x;
        double num2 = orientation.y + orientation.y;
        double num3 = orientation.z + orientation.z;
        double normalX = orientation.x * num3 + orientation.w * num2;
        double normalY = orientation.y * num3 - orientation.w * num1;
        double normalZ = 1.0 - (orientation.x * num1 + orientation.y * num2);
        return this.reflection(normalX, normalY, normalZ);
    }

    public boolean isFinite() {
        return Math.isFinite(this.m00) && Math.isFinite(this.m01) && Math.isFinite(this.m02) && Math.isFinite(this.m10) && Math.isFinite(this.m11) && Math.isFinite(this.m12) && Math.isFinite(this.m20) && Math.isFinite(this.m21) && Math.isFinite(this.m22);
    }

    public double quadraticFormProduct(double x, double y, double z) {
        double Axx = this.m00 * x + this.m10 * y + this.m20 * z;
        double Axy = this.m01 * x + this.m11 * y + this.m21 * z;
        double Axz = this.m02 * x + this.m12 * y + this.m22 * z;
        return x * Axx + y * Axy + z * Axz;
    }

    public double quadraticFormProduct(Vector3d v) {
        return this.quadraticFormProduct(v.x, v.y, v.z);
    }

    public double quadraticFormProduct(Vector3f v) {
        return this.quadraticFormProduct(v.x, v.y, v.z);
    }

    public Matrix3d mapXZY() {
        return this.mapXZY(this);
    }

    public Matrix3d mapXZY(Matrix3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(m10)._m21(m11)._m22(m12);
    }

    public Matrix3d mapXZnY() {
        return this.mapXZnY(this);
    }

    public Matrix3d mapXZnY(Matrix3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(-m10)._m21(-m11)._m22(-m12);
    }

    public Matrix3d mapXnYnZ() {
        return this.mapXnYnZ(this);
    }

    public Matrix3d mapXnYnZ(Matrix3d dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22);
    }

    public Matrix3d mapXnZY() {
        return this.mapXnZY(this);
    }

    public Matrix3d mapXnZY(Matrix3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(m10)._m21(m11)._m22(m12);
    }

    public Matrix3d mapXnZnY() {
        return this.mapXnZnY(this);
    }

    public Matrix3d mapXnZnY(Matrix3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(-m10)._m21(-m11)._m22(-m12);
    }

    public Matrix3d mapYXZ() {
        return this.mapYXZ(this);
    }

    public Matrix3d mapYXZ(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(m00)._m11(m01)._m12(m02)._m20(this.m20)._m21(this.m21)._m22(this.m22);
    }

    public Matrix3d mapYXnZ() {
        return this.mapYXnZ(this);
    }

    public Matrix3d mapYXnZ(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(m00)._m11(m01)._m12(m02)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22);
    }

    public Matrix3d mapYZX() {
        return this.mapYZX(this);
    }

    public Matrix3d mapYZX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(m00)._m21(m01)._m22(m02);
    }

    public Matrix3d mapYZnX() {
        return this.mapYZnX(this);
    }

    public Matrix3d mapYZnX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(-m00)._m21(-m01)._m22(-m02);
    }

    public Matrix3d mapYnXZ() {
        return this.mapYnXZ(this);
    }

    public Matrix3d mapYnXZ(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(this.m20)._m21(this.m21)._m22(this.m22);
    }

    public Matrix3d mapYnXnZ() {
        return this.mapYnXnZ(this);
    }

    public Matrix3d mapYnXnZ(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22);
    }

    public Matrix3d mapYnZX() {
        return this.mapYnZX(this);
    }

    public Matrix3d mapYnZX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(m00)._m21(m01)._m22(m02);
    }

    public Matrix3d mapYnZnX() {
        return this.mapYnZnX(this);
    }

    public Matrix3d mapYnZnX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(-m00)._m21(-m01)._m22(-m02);
    }

    public Matrix3d mapZXY() {
        return this.mapZXY(this);
    }

    public Matrix3d mapZXY(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(m00)._m11(m01)._m12(m02)._m20(m10)._m21(m11)._m22(m12);
    }

    public Matrix3d mapZXnY() {
        return this.mapZXnY(this);
    }

    public Matrix3d mapZXnY(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(m00)._m11(m01)._m12(m02)._m20(-m10)._m21(-m11)._m22(-m12);
    }

    public Matrix3d mapZYX() {
        return this.mapZYX(this);
    }

    public Matrix3d mapZYX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(m00)._m21(m01)._m22(m02);
    }

    public Matrix3d mapZYnX() {
        return this.mapZYnX(this);
    }

    public Matrix3d mapZYnX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(-m00)._m21(-m01)._m22(-m02);
    }

    public Matrix3d mapZnXY() {
        return this.mapZnXY(this);
    }

    public Matrix3d mapZnXY(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(m10)._m21(m11)._m22(m12);
    }

    public Matrix3d mapZnXnY() {
        return this.mapZnXnY(this);
    }

    public Matrix3d mapZnXnY(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-m10)._m21(-m11)._m22(-m12);
    }

    public Matrix3d mapZnYX() {
        return this.mapZnYX(this);
    }

    public Matrix3d mapZnYX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(m00)._m21(m01)._m22(m02);
    }

    public Matrix3d mapZnYnX() {
        return this.mapZnYnX(this);
    }

    public Matrix3d mapZnYnX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(-m00)._m21(-m01)._m22(-m02);
    }

    public Matrix3d mapnXYnZ() {
        return this.mapnXYnZ(this);
    }

    public Matrix3d mapnXYnZ(Matrix3d dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22);
    }

    public Matrix3d mapnXZY() {
        return this.mapnXZY(this);
    }

    public Matrix3d mapnXZY(Matrix3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(m10)._m21(m11)._m22(m12);
    }

    public Matrix3d mapnXZnY() {
        return this.mapnXZnY(this);
    }

    public Matrix3d mapnXZnY(Matrix3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(-m10)._m21(-m11)._m22(-m12);
    }

    public Matrix3d mapnXnYZ() {
        return this.mapnXnYZ(this);
    }

    public Matrix3d mapnXnYZ(Matrix3d dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(this.m20)._m21(this.m21)._m22(this.m22);
    }

    public Matrix3d mapnXnYnZ() {
        return this.mapnXnYnZ(this);
    }

    public Matrix3d mapnXnYnZ(Matrix3d dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22);
    }

    public Matrix3d mapnXnZY() {
        return this.mapnXnZY(this);
    }

    public Matrix3d mapnXnZY(Matrix3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(m10)._m21(m11)._m22(m12);
    }

    public Matrix3d mapnXnZnY() {
        return this.mapnXnZnY(this);
    }

    public Matrix3d mapnXnZnY(Matrix3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(-m10)._m21(-m11)._m22(-m12);
    }

    public Matrix3d mapnYXZ() {
        return this.mapnYXZ(this);
    }

    public Matrix3d mapnYXZ(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(m00)._m11(m01)._m12(m02)._m20(this.m20)._m21(this.m21)._m22(this.m22);
    }

    public Matrix3d mapnYXnZ() {
        return this.mapnYXnZ(this);
    }

    public Matrix3d mapnYXnZ(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(m00)._m11(m01)._m12(m02)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22);
    }

    public Matrix3d mapnYZX() {
        return this.mapnYZX(this);
    }

    public Matrix3d mapnYZX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(m00)._m21(m01)._m22(m02);
    }

    public Matrix3d mapnYZnX() {
        return this.mapnYZnX(this);
    }

    public Matrix3d mapnYZnX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(-m00)._m21(-m01)._m22(-m02);
    }

    public Matrix3d mapnYnXZ() {
        return this.mapnYnXZ(this);
    }

    public Matrix3d mapnYnXZ(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(this.m20)._m21(this.m21)._m22(this.m22);
    }

    public Matrix3d mapnYnXnZ() {
        return this.mapnYnXnZ(this);
    }

    public Matrix3d mapnYnXnZ(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22);
    }

    public Matrix3d mapnYnZX() {
        return this.mapnYnZX(this);
    }

    public Matrix3d mapnYnZX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(m00)._m21(m01)._m22(m02);
    }

    public Matrix3d mapnYnZnX() {
        return this.mapnYnZnX(this);
    }

    public Matrix3d mapnYnZnX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(-m00)._m21(-m01)._m22(-m02);
    }

    public Matrix3d mapnZXY() {
        return this.mapnZXY(this);
    }

    public Matrix3d mapnZXY(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(m00)._m11(m01)._m12(m02)._m20(m10)._m21(m11)._m22(m12);
    }

    public Matrix3d mapnZXnY() {
        return this.mapnZXnY(this);
    }

    public Matrix3d mapnZXnY(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(m00)._m11(m01)._m12(m02)._m20(-m10)._m21(-m11)._m22(-m12);
    }

    public Matrix3d mapnZYX() {
        return this.mapnZYX(this);
    }

    public Matrix3d mapnZYX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(m00)._m21(m01)._m22(m02);
    }

    public Matrix3d mapnZYnX() {
        return this.mapnZYnX(this);
    }

    public Matrix3d mapnZYnX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(-m00)._m21(-m01)._m22(-m02);
    }

    public Matrix3d mapnZnXY() {
        return this.mapnZnXY(this);
    }

    public Matrix3d mapnZnXY(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(m10)._m21(m11)._m22(m12);
    }

    public Matrix3d mapnZnXnY() {
        return this.mapnZnXnY(this);
    }

    public Matrix3d mapnZnXnY(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-m10)._m21(-m11)._m22(-m12);
    }

    public Matrix3d mapnZnYX() {
        return this.mapnZnYX(this);
    }

    public Matrix3d mapnZnYX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(m00)._m21(m01)._m22(m02);
    }

    public Matrix3d mapnZnYnX() {
        return this.mapnZnYnX(this);
    }

    public Matrix3d mapnZnYnX(Matrix3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(-m00)._m21(-m01)._m22(-m02);
    }

    public Matrix3d negateX() {
        return this._m00(-this.m00)._m01(-this.m01)._m02(-this.m02);
    }

    public Matrix3d negateX(Matrix3d dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(this.m20)._m21(this.m21)._m22(this.m22);
    }

    public Matrix3d negateY() {
        return this._m10(-this.m10)._m11(-this.m11)._m12(-this.m12);
    }

    public Matrix3d negateY(Matrix3d dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(this.m20)._m21(this.m21)._m22(this.m22);
    }

    public Matrix3d negateZ() {
        return this._m20(-this.m20)._m21(-this.m21)._m22(-this.m22);
    }

    public Matrix3d negateZ(Matrix3d dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
