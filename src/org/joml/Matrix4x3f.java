package org.joml;

import java.nio.FloatBuffer;
import java.text.NumberFormat;

@SuppressWarnings("unused")
public class Matrix4x3f {

    public static final int PROPERTY_IDENTITY = 4;

    public float m00;
    public float m01;
    public float m02;
    public float m10;
    public float m11;
    public float m12;
    public float m20;
    public float m21;
    public float m22;
    public float m30;
    public float m31;
    public float m32;
    int properties;

    public Matrix4x3f() {
        this.m00 = 1f;
        this.m11 = 1f;
        this.m22 = 1f;
        this.properties = 28;
    }

    public Matrix4x3f(Matrix3f mat) {
        this.set(mat);
    }

    public Matrix4x3f(Matrix4x3f mat) {
        this.set(mat);
    }

    public Matrix4x3f(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22, float m30, float m31, float m32) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.determineProperties();
    }

    public Matrix4x3f(Vector3f col0, Vector3f col1, Vector3f col2, Vector3f col3) {
        this.set(col0, col1, col2, col3).determineProperties();
    }

    public Matrix4x3f assume(int properties) {
        this.properties = properties;
        return this;
    }

    public Matrix4x3f determineProperties() {
        int properties = 0;
        if (this.m00 == 1f && this.m01 == 0f && this.m02 == 0f && this.m10 == 0f && this.m11 == 1f && this.m12 == 0f && this.m20 == 0f && this.m21 == 0f && this.m22 == 1f) {
            properties |= 24;
            if (this.m30 == 0f && this.m31 == 0f && this.m32 == 0f) {
                properties |= 4;
            }
        }

        this.properties = properties;
        return this;
    }

    public int properties() {
        return this.properties;
    }

    public float m00() {
        return this.m00;
    }

    public float m01() {
        return this.m01;
    }

    public float m02() {
        return this.m02;
    }

    public float m10() {
        return this.m10;
    }

    public float m11() {
        return this.m11;
    }

    public float m12() {
        return this.m12;
    }

    public float m20() {
        return this.m20;
    }

    public float m21() {
        return this.m21;
    }

    public float m22() {
        return this.m22;
    }

    public float m30() {
        return this.m30;
    }

    public float m31() {
        return this.m31;
    }

    public float m32() {
        return this.m32;
    }

    public Matrix4x3f m00(float m00) {
        this.m00 = m00;
        this.properties &= -17;
        if (m00 != 1f) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3f m01(float m01) {
        this.m01 = m01;
        this.properties &= -17;
        if (m01 != 0f) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3f m02(float m02) {
        this.m02 = m02;
        this.properties &= -17;
        if (m02 != 0f) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3f m10(float m10) {
        this.m10 = m10;
        this.properties &= -17;
        if (m10 != 0f) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3f m11(float m11) {
        this.m11 = m11;
        this.properties &= -17;
        if (m11 != 1f) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3f m12(float m12) {
        this.m12 = m12;
        this.properties &= -17;
        if (m12 != 0f) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3f m20(float m20) {
        this.m20 = m20;
        this.properties &= -17;
        if (m20 != 0f) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3f m21(float m21) {
        this.m21 = m21;
        this.properties &= -17;
        if (m21 != 0f) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3f m22(float m22) {
        this.m22 = m22;
        this.properties &= -17;
        if (m22 != 1f) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3f m30(float m30) {
        this.m30 = m30;
        if (m30 != 0f) {
            this.properties &= -5;
        }

        return this;
    }

    public Matrix4x3f m31(float m31) {
        this.m31 = m31;
        if (m31 != 0f) {
            this.properties &= -5;
        }

        return this;
    }

    public Matrix4x3f m32(float m32) {
        this.m32 = m32;
        if (m32 != 0f) {
            this.properties &= -5;
        }

        return this;
    }

    Matrix4x3f _properties(int properties) {
        this.properties = properties;
        return this;
    }

    Matrix4x3f _m00(float m00) {
        this.m00 = m00;
        return this;
    }

    Matrix4x3f _m01(float m01) {
        this.m01 = m01;
        return this;
    }

    Matrix4x3f _m02(float m02) {
        this.m02 = m02;
        return this;
    }

    Matrix4x3f _m10(float m10) {
        this.m10 = m10;
        return this;
    }

    Matrix4x3f _m11(float m11) {
        this.m11 = m11;
        return this;
    }

    Matrix4x3f _m12(float m12) {
        this.m12 = m12;
        return this;
    }

    Matrix4x3f _m20(float m20) {
        this.m20 = m20;
        return this;
    }

    Matrix4x3f _m21(float m21) {
        this.m21 = m21;
        return this;
    }

    Matrix4x3f _m22(float m22) {
        this.m22 = m22;
        return this;
    }

    Matrix4x3f _m30(float m30) {
        this.m30 = m30;
        return this;
    }

    Matrix4x3f _m31(float m31) {
        this.m31 = m31;
        return this;
    }

    Matrix4x3f _m32(float m32) {
        this.m32 = m32;
        return this;
    }

    public Matrix4x3f identity() {
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
            this.properties = 28;
        }
        return this;
    }

    public Matrix4x3f set(Matrix4x3f m) {
        this.m00 = m.m00;
        this.m01 = m.m01;
        this.m02 = m.m02;
        this.m10 = m.m10;
        this.m11 = m.m11;
        this.m12 = m.m12;
        this.m20 = m.m20;
        this.m21 = m.m21;
        this.m22 = m.m22;
        this.m30 = m.m30;
        this.m31 = m.m31;
        this.m32 = m.m32;
        this.properties = m.properties();
        return this;
    }

    public Matrix4x3f set(Matrix4f m) {
        this.m00 = m.m00;
        this.m01 = m.m01;
        this.m02 = m.m02;
        this.m10 = m.m10;
        this.m11 = m.m11;
        this.m12 = m.m12;
        this.m20 = m.m20;
        this.m21 = m.m21;
        this.m22 = m.m22;
        this.m30 = m.m30;
        this.m31 = m.m31;
        this.m32 = m.m32;
        this.properties = m.properties() & 28;
        return this;
    }

    public Matrix4f get(Matrix4f dest) {
        return dest.set4x3(this);
    }

    public Matrix4d get(Matrix4d dest) {
        return dest.set4x3(this);
    }

    public Matrix4x3f set(Matrix3f mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m02 = mat.m02;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
        this.m12 = mat.m12;
        this.m20 = mat.m20;
        this.m21 = mat.m21;
        this.m22 = mat.m22;
        this.m30 = 0f;
        this.m31 = 0f;
        this.m32 = 0f;
        return this.determineProperties();
    }

    public Matrix4x3f set(AxisAngle4f axisAngle) {
        float x = axisAngle.x;
        float y = axisAngle.y;
        float z = axisAngle.z;
        float angle = axisAngle.angle;
        float n = Math.sqrt(x * x + y * y + z * z);
        n = 1f / n;
        x *= n;
        y *= n;
        z *= n;
        float s = Math.sin(angle);
        float c = Math.cosFromSin(s, angle);
        float omc = 1f - c;
        this.m00 = c + x * x * omc;
        this.m11 = c + y * y * omc;
        this.m22 = c + z * z * omc;
        float tmp1 = x * y * omc;
        float tmp2 = z * s;
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
        this.m30 = 0f;
        this.m31 = 0f;
        this.m32 = 0f;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f set(AxisAngle4d axisAngle) {
        double x = axisAngle.x;
        double y = axisAngle.y;
        double z = axisAngle.z;
        double angle = axisAngle.angle;
        double n = Math.sqrt(x * x + y * y + z * z);
        n = 1.0 / n;
        x *= n;
        y *= n;
        z *= n;
        double s = Math.sin(angle);
        double c = Math.cosFromSin(s, angle);
        double omc = 1.0 - c;
        this.m00 = (float) (c + x * x * omc);
        this.m11 = (float) (c + y * y * omc);
        this.m22 = (float) (c + z * z * omc);
        double tmp1 = x * y * omc;
        double tmp2 = z * s;
        this.m10 = (float) (tmp1 - tmp2);
        this.m01 = (float) (tmp1 + tmp2);
        tmp1 = x * z * omc;
        tmp2 = y * s;
        this.m20 = (float) (tmp1 + tmp2);
        this.m02 = (float) (tmp1 - tmp2);
        tmp1 = y * z * omc;
        tmp2 = x * s;
        this.m21 = (float) (tmp1 - tmp2);
        this.m12 = (float) (tmp1 + tmp2);
        this.m30 = 0f;
        this.m31 = 0f;
        this.m32 = 0f;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f set(Quaternionf q) {
        return this.rotation(q);
    }

    public Matrix4x3f set(Quaterniond q) {
        double w2 = q.w * q.w;
        double x2 = q.x * q.x;
        double y2 = q.y * q.y;
        double z2 = q.z * q.z;
        double zw = q.z * q.w;
        double xy = q.x * q.y;
        double xz = q.x * q.z;
        double yw = q.y * q.w;
        double yz = q.y * q.z;
        double xw = q.x * q.w;
        this.m00 = (float) (w2 + x2 - z2 - y2);
        this.m01 = (float) (xy + zw + zw + xy);
        this.m02 = (float) (xz - yw + xz - yw);
        this.m10 = (float) (-zw + xy - zw + xy);
        this.m11 = (float) (y2 - z2 + w2 - x2);
        this.m12 = (float) (yz + yz + xw + xw);
        this.m20 = (float) (yw + xz + xz + yw);
        this.m21 = (float) (yz + yz - xw - xw);
        this.m22 = (float) (z2 - y2 - x2 + w2);
        this.properties = 16;
        return this;
    }

    public Matrix4x3f set(Vector3f col0, Vector3f col1, Vector3f col2, Vector3f col3) {
        this.m00 = col0.x;
        this.m01 = col0.y;
        this.m02 = col0.z;
        this.m10 = col1.x;
        this.m11 = col1.y;
        this.m12 = col1.z;
        this.m20 = col2.x;
        this.m21 = col2.y;
        this.m22 = col2.z;
        this.m30 = col3.x;
        this.m31 = col3.y;
        this.m32 = col3.z;
        return this.determineProperties();
    }

    public Matrix4x3f set3x3(Matrix4x3f mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m02 = mat.m02;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
        this.m12 = mat.m12;
        this.m20 = mat.m20;
        this.m21 = mat.m21;
        this.m22 = mat.m22;
        this.properties &= mat.properties();
        return this;
    }

    public Matrix4x3f mul(Matrix4x3f right) {
        return this.mul(right, this);
    }

    public Matrix4x3f mul(Matrix4x3f right, Matrix4x3f dest) {
        if ((this.properties & 4) != 0) {
            return dest.set(right);
        } else if ((right.properties() & 4) != 0) {
            return dest.set(this);
        } else {
            return (this.properties & 8) != 0 ? this.mulTranslation(right, dest) : this.mulGeneric(right, dest);
        }
    }

    private Matrix4x3f mulGeneric(Matrix4x3f right, Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        float m20 = this.m20;
        float m21 = this.m21;
        float m22 = this.m22;
        float rm00 = right.m00;
        float rm01 = right.m01;
        float rm02 = right.m02;
        float rm10 = right.m10;
        float rm11 = right.m11;
        float rm12 = right.m12;
        float rm20 = right.m20;
        float rm21 = right.m21;
        float rm22 = right.m22;
        float rm30 = right.m30;
        float rm31 = right.m31;
        float rm32 = right.m32;
        return dest._m00(Math.fma(m00, rm00, Math.fma(m10, rm01, m20 * rm02)))._m01(Math.fma(m01, rm00, Math.fma(m11, rm01, m21 * rm02)))._m02(Math.fma(m02, rm00, Math.fma(m12, rm01, m22 * rm02)))._m10(Math.fma(m00, rm10, Math.fma(m10, rm11, m20 * rm12)))._m11(Math.fma(m01, rm10, Math.fma(m11, rm11, m21 * rm12)))._m12(Math.fma(m02, rm10, Math.fma(m12, rm11, m22 * rm12)))._m20(Math.fma(m00, rm20, Math.fma(m10, rm21, m20 * rm22)))._m21(Math.fma(m01, rm20, Math.fma(m11, rm21, m21 * rm22)))._m22(Math.fma(m02, rm20, Math.fma(m12, rm21, m22 * rm22)))._m30(Math.fma(m00, rm30, Math.fma(m10, rm31, Math.fma(m20, rm32, this.m30))))._m31(Math.fma(m01, rm30, Math.fma(m11, rm31, Math.fma(m21, rm32, this.m31))))._m32(Math.fma(m02, rm30, Math.fma(m12, rm31, Math.fma(m22, rm32, this.m32))))._properties(this.properties & right.properties() & 16);
    }

    public Matrix4x3f mulTranslation(Matrix4x3f right, Matrix4x3f dest) {
        return dest._m00(right.m00)._m01(right.m01)._m02(right.m02)._m10(right.m10)._m11(right.m11)._m12(right.m12)._m20(right.m20)._m21(right.m21)._m22(right.m22)._m30(right.m30 + this.m30)._m31(right.m31 + this.m31)._m32(right.m32 + this.m32)._properties(right.properties() & 16);
    }

    public Matrix4x3f mulOrtho(Matrix4x3f view) {
        return this.mulOrtho(view, this);
    }

    public Matrix4x3f mulOrtho(Matrix4x3f view, Matrix4x3f dest) {
        float nm00 = this.m00 * view.m00;
        float nm01 = this.m11 * view.m01;
        float nm02 = this.m22 * view.m02;
        float nm10 = this.m00 * view.m10;
        float nm11 = this.m11 * view.m11;
        float nm12 = this.m22 * view.m12;
        float nm20 = this.m00 * view.m20;
        float nm21 = this.m11 * view.m21;
        float nm22 = this.m22 * view.m22;
        float nm30 = this.m00 * view.m30 + this.m30;
        float nm31 = this.m11 * view.m31 + this.m31;
        float nm32 = this.m22 * view.m32 + this.m32;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        dest.m30 = nm30;
        dest.m31 = nm31;
        dest.m32 = nm32;
        dest.properties = this.properties & view.properties() & 16;
        return dest;
    }

    public Matrix4x3f mul3x3(float rm00, float rm01, float rm02, float rm10, float rm11, float rm12, float rm20, float rm21, float rm22) {
        return this.mul3x3(rm00, rm01, rm02, rm10, rm11, rm12, rm20, rm21, rm22, this);
    }

    public Matrix4x3f mul3x3(float rm00, float rm01, float rm02, float rm10, float rm11, float rm12, float rm20, float rm21, float rm22, Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        float m20 = this.m20;
        float m21 = this.m21;
        float m22 = this.m22;
        return dest._m00(Math.fma(m00, rm00, Math.fma(m10, rm01, m20 * rm02)))._m01(Math.fma(m01, rm00, Math.fma(m11, rm01, m21 * rm02)))._m02(Math.fma(m02, rm00, Math.fma(m12, rm01, m22 * rm02)))._m10(Math.fma(m00, rm10, Math.fma(m10, rm11, m20 * rm12)))._m11(Math.fma(m01, rm10, Math.fma(m11, rm11, m21 * rm12)))._m12(Math.fma(m02, rm10, Math.fma(m12, rm11, m22 * rm12)))._m20(Math.fma(m00, rm20, Math.fma(m10, rm21, m20 * rm22)))._m21(Math.fma(m01, rm20, Math.fma(m11, rm21, m21 * rm22)))._m22(Math.fma(m02, rm20, Math.fma(m12, rm21, m22 * rm22)))._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(0);
    }

    public Matrix4x3f fma(Matrix4x3f other, float otherFactor) {
        return this.fma(other, otherFactor, this);
    }

    public Matrix4x3f fma(Matrix4x3f other, float otherFactor, Matrix4x3f dest) {
        dest._m00(Math.fma(other.m00, otherFactor, this.m00))._m01(Math.fma(other.m01, otherFactor, this.m01))._m02(Math.fma(other.m02, otherFactor, this.m02))._m10(Math.fma(other.m10, otherFactor, this.m10))._m11(Math.fma(other.m11, otherFactor, this.m11))._m12(Math.fma(other.m12, otherFactor, this.m12))._m20(Math.fma(other.m20, otherFactor, this.m20))._m21(Math.fma(other.m21, otherFactor, this.m21))._m22(Math.fma(other.m22, otherFactor, this.m22))._m30(Math.fma(other.m30, otherFactor, this.m30))._m31(Math.fma(other.m31, otherFactor, this.m31))._m32(Math.fma(other.m32, otherFactor, this.m32))._properties(0);
        return dest;
    }

    public Matrix4x3f add(Matrix4x3f other) {
        return this.add(other, this);
    }

    public Matrix4x3f add(Matrix4x3f other, Matrix4x3f dest) {
        dest.m00 = this.m00 + other.m00;
        dest.m01 = this.m01 + other.m01;
        dest.m02 = this.m02 + other.m02;
        dest.m10 = this.m10 + other.m10;
        dest.m11 = this.m11 + other.m11;
        dest.m12 = this.m12 + other.m12;
        dest.m20 = this.m20 + other.m20;
        dest.m21 = this.m21 + other.m21;
        dest.m22 = this.m22 + other.m22;
        dest.m30 = this.m30 + other.m30;
        dest.m31 = this.m31 + other.m31;
        dest.m32 = this.m32 + other.m32;
        dest.properties = 0;
        return dest;
    }

    public Matrix4x3f sub(Matrix4x3f subtrahend) {
        return this.sub(subtrahend, this);
    }

    public Matrix4x3f sub(Matrix4x3f subtrahend, Matrix4x3f dest) {
        dest.m00 = this.m00 - subtrahend.m00;
        dest.m01 = this.m01 - subtrahend.m01;
        dest.m02 = this.m02 - subtrahend.m02;
        dest.m10 = this.m10 - subtrahend.m10;
        dest.m11 = this.m11 - subtrahend.m11;
        dest.m12 = this.m12 - subtrahend.m12;
        dest.m20 = this.m20 - subtrahend.m20;
        dest.m21 = this.m21 - subtrahend.m21;
        dest.m22 = this.m22 - subtrahend.m22;
        dest.m30 = this.m30 - subtrahend.m30;
        dest.m31 = this.m31 - subtrahend.m31;
        dest.m32 = this.m32 - subtrahend.m32;
        dest.properties = 0;
        return dest;
    }

    public Matrix4x3f mulComponentWise(Matrix4x3f other) {
        return this.mulComponentWise(other, this);
    }

    public Matrix4x3f mulComponentWise(Matrix4x3f other, Matrix4x3f dest) {
        dest.m00 = this.m00 * other.m00;
        dest.m01 = this.m01 * other.m01;
        dest.m02 = this.m02 * other.m02;
        dest.m10 = this.m10 * other.m10;
        dest.m11 = this.m11 * other.m11;
        dest.m12 = this.m12 * other.m12;
        dest.m20 = this.m20 * other.m20;
        dest.m21 = this.m21 * other.m21;
        dest.m22 = this.m22 * other.m22;
        dest.m30 = this.m30 * other.m30;
        dest.m31 = this.m31 * other.m31;
        dest.m32 = this.m32 * other.m32;
        dest.properties = 0;
        return dest;
    }

    public Matrix4x3f set(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22, float m30, float m31, float m32) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        return this.determineProperties();
    }

    public Matrix4x3f set(float[] m, int off) {
        MemUtil.INSTANCE.copy(m, off, this);
        return this.determineProperties();
    }

    public Matrix4x3f set(float[] m) {
        return this.set(m, 0);
    }

    public float determinant() {
        return (this.m00 * this.m11 - this.m01 * this.m10) * this.m22 + (this.m02 * this.m10 - this.m00 * this.m12) * this.m21 + (this.m01 * this.m12 - this.m02 * this.m11) * this.m20;
    }

    public Matrix4x3f invert(Matrix4x3f dest) {
        if ((this.properties & 4) != 0) {
            return dest.identity();
        } else {
            return (this.properties & 16) != 0 ? this.invertOrthonormal(dest) : this.invertGeneric(dest);
        }
    }

    private Matrix4x3f invertGeneric(Matrix4x3f dest) {
        float m11m00 = this.m00 * this.m11;
        float m10m01 = this.m01 * this.m10;
        float m10m02 = this.m02 * this.m10;
        float m12m00 = this.m00 * this.m12;
        float m12m01 = this.m01 * this.m12;
        float m11m02 = this.m02 * this.m11;
        float s = 1f / ((m11m00 - m10m01) * this.m22 + (m10m02 - m12m00) * this.m21 + (m12m01 - m11m02) * this.m20);
        float m10m22 = this.m10 * this.m22;
        float m10m21 = this.m10 * this.m21;
        float m11m22 = this.m11 * this.m22;
        float m11m20 = this.m11 * this.m20;
        float m12m21 = this.m12 * this.m21;
        float m12m20 = this.m12 * this.m20;
        float m20m02 = this.m20 * this.m02;
        float m20m01 = this.m20 * this.m01;
        float m21m02 = this.m21 * this.m02;
        float m21m00 = this.m21 * this.m00;
        float m22m01 = this.m22 * this.m01;
        float m22m00 = this.m22 * this.m00;
        float nm00 = (m11m22 - m12m21) * s;
        float nm01 = (m21m02 - m22m01) * s;
        float nm02 = (m12m01 - m11m02) * s;
        float nm10 = (m12m20 - m10m22) * s;
        float nm11 = (m22m00 - m20m02) * s;
        float nm12 = (m10m02 - m12m00) * s;
        float nm20 = (m10m21 - m11m20) * s;
        float nm21 = (m20m01 - m21m00) * s;
        float nm22 = (m11m00 - m10m01) * s;
        float nm30 = (m10m22 * this.m31 - m10m21 * this.m32 + m11m20 * this.m32 - m11m22 * this.m30 + m12m21 * this.m30 - m12m20 * this.m31) * s;
        float nm31 = (m20m02 * this.m31 - m20m01 * this.m32 + m21m00 * this.m32 - m21m02 * this.m30 + m22m01 * this.m30 - m22m00 * this.m31) * s;
        float nm32 = (m11m02 * this.m30 - m12m01 * this.m30 + m12m00 * this.m31 - m10m02 * this.m31 + m10m01 * this.m32 - m11m00 * this.m32) * s;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        dest.m30 = nm30;
        dest.m31 = nm31;
        dest.m32 = nm32;
        dest.properties = 0;
        return dest;
    }

    private Matrix4x3f invertOrthonormal(Matrix4x3f dest) {
        float nm30 = -(this.m00 * this.m30 + this.m01 * this.m31 + this.m02 * this.m32);
        float nm31 = -(this.m10 * this.m30 + this.m11 * this.m31 + this.m12 * this.m32);
        float nm32 = -(this.m20 * this.m30 + this.m21 * this.m31 + this.m22 * this.m32);
        float m01 = this.m01;
        float m02 = this.m02;
        float m12 = this.m12;
        dest.m00 = this.m00;
        dest.m01 = this.m10;
        dest.m02 = this.m20;
        dest.m10 = m01;
        dest.m11 = this.m11;
        dest.m12 = this.m21;
        dest.m20 = m02;
        dest.m21 = m12;
        dest.m22 = this.m22;
        dest.m30 = nm30;
        dest.m31 = nm31;
        dest.m32 = nm32;
        dest.properties = 16;
        return dest;
    }

    public Matrix4f invert(Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.identity();
        } else {
            return (this.properties & 16) != 0 ? this.invertOrthonormal(dest) : this.invertGeneric(dest);
        }
    }

    private Matrix4f invertGeneric(Matrix4f dest) {
        float m11m00 = this.m00 * this.m11;
        float m10m01 = this.m01 * this.m10;
        float m10m02 = this.m02 * this.m10;
        float m12m00 = this.m00 * this.m12;
        float m12m01 = this.m01 * this.m12;
        float m11m02 = this.m02 * this.m11;
        float s = 1f / ((m11m00 - m10m01) * this.m22 + (m10m02 - m12m00) * this.m21 + (m12m01 - m11m02) * this.m20);
        float m10m22 = this.m10 * this.m22;
        float m10m21 = this.m10 * this.m21;
        float m11m22 = this.m11 * this.m22;
        float m11m20 = this.m11 * this.m20;
        float m12m21 = this.m12 * this.m21;
        float m12m20 = this.m12 * this.m20;
        float m20m02 = this.m20 * this.m02;
        float m20m01 = this.m20 * this.m01;
        float m21m02 = this.m21 * this.m02;
        float m21m00 = this.m21 * this.m00;
        float m22m01 = this.m22 * this.m01;
        float m22m00 = this.m22 * this.m00;
        float nm00 = (m11m22 - m12m21) * s;
        float nm01 = (m21m02 - m22m01) * s;
        float nm02 = (m12m01 - m11m02) * s;
        float nm10 = (m12m20 - m10m22) * s;
        float nm11 = (m22m00 - m20m02) * s;
        float nm12 = (m10m02 - m12m00) * s;
        float nm20 = (m10m21 - m11m20) * s;
        float nm21 = (m20m01 - m21m00) * s;
        float nm22 = (m11m00 - m10m01) * s;
        float nm30 = (m10m22 * this.m31 - m10m21 * this.m32 + m11m20 * this.m32 - m11m22 * this.m30 + m12m21 * this.m30 - m12m20 * this.m31) * s;
        float nm31 = (m20m02 * this.m31 - m20m01 * this.m32 + m21m00 * this.m32 - m21m02 * this.m30 + m22m01 * this.m30 - m22m00 * this.m31) * s;
        float nm32 = (m11m02 * this.m30 - m12m01 * this.m30 + m12m00 * this.m31 - m10m02 * this.m31 + m10m01 * this.m32 - m11m00 * this.m32) * s;
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(0f)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)._m20(nm20)._m21(nm21)._m22(nm22)._m23(0f)._m30(nm30)._m31(nm31)._m32(nm32)._m33(1f)._properties(0);
    }

    private Matrix4f invertOrthonormal(Matrix4f dest) {
        float nm30 = -(this.m00 * this.m30 + this.m01 * this.m31 + this.m02 * this.m32);
        float nm31 = -(this.m10 * this.m30 + this.m11 * this.m31 + this.m12 * this.m32);
        float nm32 = -(this.m20 * this.m30 + this.m21 * this.m31 + this.m22 * this.m32);
        float m01 = this.m01;
        float m02 = this.m02;
        float m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m10)._m02(this.m20)._m03(0f)._m10(m01)._m11(this.m11)._m12(this.m21)._m13(0f)._m20(m02)._m21(m12)._m22(this.m22)._m23(0f)._m30(nm30)._m31(nm31)._m32(nm32)._m33(1f)._properties(16);
    }

    public Matrix4x3f invert() {
        return this.invert(this);
    }

    public Matrix4x3f invertOrtho(Matrix4x3f dest) {
        float invM00 = 1f / this.m00;
        float invM11 = 1f / this.m11;
        float invM22 = 1f / this.m22;
        dest.set(invM00, 0f, 0f, 0f, invM11, 0f, 0f, 0f, invM22, -this.m30 * invM00, -this.m31 * invM11, -this.m32 * invM22);
        dest.properties = 0;
        return dest;
    }

    public Matrix4x3f invertOrtho() {
        return this.invertOrtho(this);
    }

    public Matrix4x3f transpose3x3() {
        return this.transpose3x3(this);
    }

    public Matrix4x3f transpose3x3(Matrix4x3f dest) {
        float nm00 = this.m00;
        float nm01 = this.m10;
        float nm02 = this.m20;
        float nm10 = this.m01;
        float nm11 = this.m11;
        float nm12 = this.m21;
        float nm20 = this.m02;
        float nm21 = this.m12;
        float nm22 = this.m22;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        dest.properties = this.properties;
        return dest;
    }

    public Matrix3f transpose3x3(Matrix3f dest) {
        dest.m00(this.m00);
        dest.m01(this.m10);
        dest.m02(this.m20);
        dest.m10(this.m01);
        dest.m11(this.m11);
        dest.m12(this.m21);
        dest.m20(this.m02);
        dest.m21(this.m12);
        dest.m22(this.m22);
        return dest;
    }

    public Matrix4x3f translation(float x, float y, float z) {
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        this.m30 = x;
        this.m31 = y;
        this.m32 = z;
        this.properties = 24;
        return this;
    }

    public Matrix4x3f translation(Vector3f offset) {
        return this.translation(offset.x, offset.y, offset.z);
    }

    public Matrix4x3f setTranslation(float x, float y, float z) {
        this.m30 = x;
        this.m31 = y;
        this.m32 = z;
        this.properties &= -5;
        return this;
    }

    public Matrix4x3f setTranslation(Vector3f xyz) {
        return this.setTranslation(xyz.x, xyz.y, xyz.z);
    }

    public Vector3f getTranslation(Vector3f dest) {
        dest.x = this.m30;
        dest.y = this.m31;
        dest.z = this.m32;
        return dest;
    }

    public Vector3f getScale(Vector3f dest) {
        dest.x = Math.sqrt(this.m00 * this.m00 + this.m01 * this.m01 + this.m02 * this.m02);
        dest.y = Math.sqrt(this.m10 * this.m10 + this.m11 * this.m11 + this.m12 * this.m12);
        dest.z = Math.sqrt(this.m20 * this.m20 + this.m21 * this.m21 + this.m22 * this.m22);
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
        return Runtime.format(this.m00, formatter) + " " + Runtime.format(this.m10, formatter) + " " + Runtime.format(this.m20, formatter) + " " + Runtime.format(this.m30, formatter) + "\n" + Runtime.format(this.m01, formatter) + " " + Runtime.format(this.m11, formatter) + " " + Runtime.format(this.m21, formatter) + " " + Runtime.format(this.m31, formatter) + "\n" + Runtime.format(this.m02, formatter) + " " + Runtime.format(this.m12, formatter) + " " + Runtime.format(this.m22, formatter) + " " + Runtime.format(this.m32, formatter) + "\n";
    }

    public Matrix4x3f get(Matrix4x3f dest) {
        return dest.set(this);
    }

    public Matrix4x3d get(Matrix4x3d dest) {
        return dest.set(this);
    }

    public AxisAngle4f getRotation(AxisAngle4f dest) {
        return dest.set(this);
    }

    public AxisAngle4d getRotation(AxisAngle4d dest) {
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

    public float[] get(float[] arr, int offset) {
        MemUtil.INSTANCE.copy(this, arr, offset);
        return arr;
    }

    public float[] get(float[] arr) {
        return this.get(arr, 0);
    }

    public FloatBuffer putInto(FloatBuffer arr) {
        arr.put(m00).put(m01).put(m02);
        arr.put(m10).put(m11).put(m12);
        arr.put(m20).put(m21).put(m22);
        arr.put(m30).put(m31).put(m32);
        return arr;
    }

    public float[] get4x4(float[] arr, int offset) {
        MemUtil.INSTANCE.copy4x4(this, arr, offset);
        return arr;
    }

    public float[] get4x4(float[] arr) {
        return this.get4x4(arr, 0);
    }

    public float[] getTransposed(float[] arr, int offset) {
        arr[offset] = this.m00;
        arr[offset + 1] = this.m10;
        arr[offset + 2] = this.m20;
        arr[offset + 3] = this.m30;
        arr[offset + 4] = this.m01;
        arr[offset + 5] = this.m11;
        arr[offset + 6] = this.m21;
        arr[offset + 7] = this.m31;
        arr[offset + 8] = this.m02;
        arr[offset + 9] = this.m12;
        arr[offset + 10] = this.m22;
        arr[offset + 11] = this.m32;
        return arr;
    }

    public float[] getTransposed(float[] arr) {
        return this.getTransposed(arr, 0);
    }

    public Matrix4x3f zero() {
        MemUtil.INSTANCE.zero(this);
        this.properties = 0;
        return this;
    }

    public Matrix4x3f scaling(float factor) {
        return this.scaling(factor, factor, factor);
    }

    public Matrix4x3f scaling(float x, float y, float z) {
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        this.m00 = x;
        this.m11 = y;
        this.m22 = z;
        boolean one = Math.absEqualsOne(x) && Math.absEqualsOne(y) && Math.absEqualsOne(z);
        this.properties = one ? 16 : 0;
        return this;
    }

    public Matrix4x3f scaling(Vector3f xyz) {
        return this.scaling(xyz.x, xyz.y, xyz.z);
    }

    public Matrix4x3f rotation(float angle, Vector3f axis) {
        return this.rotation(angle, axis.x, axis.y, axis.z);
    }

    public Matrix4x3f rotation(AxisAngle4f axisAngle) {
        return this.rotation(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Matrix4x3f rotation(float angle, float x, float y, float z) {
        if (y == 0f && z == 0f && Math.absEqualsOne(x)) {
            return this.rotationX(x * angle);
        } else if (x == 0f && z == 0f && Math.absEqualsOne(y)) {
            return this.rotationY(y * angle);
        } else {
            return x == 0f && y == 0f && Math.absEqualsOne(z) ? this.rotationZ(z * angle) : this.rotationInternal(angle, x, y, z);
        }
    }

    private Matrix4x3f rotationInternal(float angle, float x, float y, float z) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        float C = 1f - cos;
        float xy = x * y;
        float xz = x * z;
        float yz = y * z;
        this.m00 = cos + x * x * C;
        this.m01 = xy * C + z * sin;
        this.m02 = xz * C - y * sin;
        this.m10 = xy * C - z * sin;
        this.m11 = cos + y * y * C;
        this.m12 = yz * C + x * sin;
        this.m20 = xz * C + y * sin;
        this.m21 = yz * C - x * sin;
        this.m22 = cos + z * z * C;
        this.m30 = 0f;
        this.m31 = 0f;
        this.m32 = 0f;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f rotationX(float ang) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        this.m00 = 1f;
        this.m01 = 0f;
        this.m02 = 0f;
        this.m10 = 0f;
        this.m11 = cos;
        this.m12 = sin;
        this.m20 = 0f;
        this.m21 = -sin;
        this.m22 = cos;
        this.m30 = 0f;
        this.m31 = 0f;
        this.m32 = 0f;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f rotationY(float ang) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        this.m00 = cos;
        this.m01 = 0f;
        this.m02 = -sin;
        this.m10 = 0f;
        this.m11 = 1f;
        this.m12 = 0f;
        this.m20 = sin;
        this.m21 = 0f;
        this.m22 = cos;
        this.m30 = 0f;
        this.m31 = 0f;
        this.m32 = 0f;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f rotationZ(float ang) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        this.m00 = cos;
        this.m01 = sin;
        this.m02 = 0f;
        this.m10 = -sin;
        this.m11 = cos;
        this.m12 = 0f;
        this.m20 = 0f;
        this.m21 = 0f;
        this.m22 = 1f;
        this.m30 = 0f;
        this.m31 = 0f;
        this.m32 = 0f;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f rotationXYZ(float angleX, float angleY, float angleZ) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float m_sinX = -sinX;
        float m_sinY = -sinY;
        float m_sinZ = -sinZ;
        float nm01 = m_sinX * m_sinY;
        float nm02 = cosX * m_sinY;
        this.m20 = sinY;
        this.m21 = m_sinX * cosY;
        this.m22 = cosX * cosY;
        this.m00 = cosY * cosZ;
        this.m01 = nm01 * cosZ + cosX * sinZ;
        this.m02 = nm02 * cosZ + sinX * sinZ;
        this.m10 = cosY * m_sinZ;
        this.m11 = nm01 * m_sinZ + cosX * cosZ;
        this.m12 = nm02 * m_sinZ + sinX * cosZ;
        this.m30 = 0f;
        this.m31 = 0f;
        this.m32 = 0f;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f rotationZYX(float angleZ, float angleY, float angleX) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float m_sinZ = -sinZ;
        float m_sinY = -sinY;
        float m_sinX = -sinX;
        float nm20 = cosZ * sinY;
        float nm21 = sinZ * sinY;
        this.m00 = cosZ * cosY;
        this.m01 = sinZ * cosY;
        this.m02 = m_sinY;
        this.m10 = m_sinZ * cosX + nm20 * sinX;
        this.m11 = cosZ * cosX + nm21 * sinX;
        this.m12 = cosY * sinX;
        this.m20 = m_sinZ * m_sinX + nm20 * cosX;
        this.m21 = cosZ * m_sinX + nm21 * cosX;
        this.m22 = cosY * cosX;
        this.m30 = 0f;
        this.m31 = 0f;
        this.m32 = 0f;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f rotationYXZ(float angleY, float angleX, float angleZ) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float m_sinY = -sinY;
        float m_sinX = -sinX;
        float m_sinZ = -sinZ;
        float nm10 = sinY * sinX;
        float nm12 = cosY * sinX;
        this.m20 = sinY * cosX;
        this.m21 = m_sinX;
        this.m22 = cosY * cosX;
        this.m00 = cosY * cosZ + nm10 * sinZ;
        this.m01 = cosX * sinZ;
        this.m02 = m_sinY * cosZ + nm12 * sinZ;
        this.m10 = cosY * m_sinZ + nm10 * cosZ;
        this.m11 = cosX * cosZ;
        this.m12 = m_sinY * m_sinZ + nm12 * cosZ;
        this.m30 = 0f;
        this.m31 = 0f;
        this.m32 = 0f;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f setRotationXYZ(float angleX, float angleY, float angleZ) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float m_sinX = -sinX;
        float m_sinY = -sinY;
        float m_sinZ = -sinZ;
        float nm01 = m_sinX * m_sinY;
        float nm02 = cosX * m_sinY;
        this.m20 = sinY;
        this.m21 = m_sinX * cosY;
        this.m22 = cosX * cosY;
        this.m00 = cosY * cosZ;
        this.m01 = nm01 * cosZ + cosX * sinZ;
        this.m02 = nm02 * cosZ + sinX * sinZ;
        this.m10 = cosY * m_sinZ;
        this.m11 = nm01 * m_sinZ + cosX * cosZ;
        this.m12 = nm02 * m_sinZ + sinX * cosZ;
        this.properties &= -13;
        return this;
    }

    public Matrix4x3f setRotationZYX(float angleZ, float angleY, float angleX) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float m_sinZ = -sinZ;
        float m_sinY = -sinY;
        float m_sinX = -sinX;
        float nm20 = cosZ * sinY;
        float nm21 = sinZ * sinY;
        this.m00 = cosZ * cosY;
        this.m01 = sinZ * cosY;
        this.m02 = m_sinY;
        this.m10 = m_sinZ * cosX + nm20 * sinX;
        this.m11 = cosZ * cosX + nm21 * sinX;
        this.m12 = cosY * sinX;
        this.m20 = m_sinZ * m_sinX + nm20 * cosX;
        this.m21 = cosZ * m_sinX + nm21 * cosX;
        this.m22 = cosY * cosX;
        this.properties &= -13;
        return this;
    }

    public Matrix4x3f setRotationYXZ(float angleY, float angleX, float angleZ) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float m_sinY = -sinY;
        float m_sinX = -sinX;
        float m_sinZ = -sinZ;
        float nm10 = sinY * sinX;
        float nm12 = cosY * sinX;
        this.m20 = sinY * cosX;
        this.m21 = m_sinX;
        this.m22 = cosY * cosX;
        this.m00 = cosY * cosZ + nm10 * sinZ;
        this.m01 = cosX * sinZ;
        this.m02 = m_sinY * cosZ + nm12 * sinZ;
        this.m10 = cosY * m_sinZ + nm10 * cosZ;
        this.m11 = cosX * cosZ;
        this.m12 = m_sinY * m_sinZ + nm12 * cosZ;
        this.properties &= -13;
        return this;
    }

    public Matrix4x3f rotation(Quaternionf quat) {
        float w2 = quat.w * quat.w;
        float x2 = quat.x * quat.x;
        float y2 = quat.y * quat.y;
        float z2 = quat.z * quat.z;
        float zw = quat.z * quat.w;
        float dzw = zw + zw;
        float xy = quat.x * quat.y;
        float dxy = xy + xy;
        float xz = quat.x * quat.z;
        float dxz = xz + xz;
        float yw = quat.y * quat.w;
        float dyw = yw + yw;
        float yz = quat.y * quat.z;
        float dyz = yz + yz;
        float xw = quat.x * quat.w;
        float dxw = xw + xw;
        this._m00(w2 + x2 - z2 - y2);
        this._m01(dxy + dzw);
        this._m02(dxz - dyw);
        this._m10(dxy - dzw);
        this._m11(y2 - z2 + w2 - x2);
        this._m12(dyz + dxw);
        this._m20(dyw + dxz);
        this._m21(dyz - dxw);
        this._m22(z2 - y2 - x2 + w2);
        this._m30(0f);
        this._m31(0f);
        this._m32(0f);
        this.properties = 16;
        return this;
    }

    public Matrix4x3f translationRotateScale(float tx, float ty, float tz, float qx, float qy, float qz, float qw, float sx, float sy, float sz) {
        float dqx = qx + qx;
        float dqy = qy + qy;
        float dqz = qz + qz;
        float q00 = dqx * qx;
        float q11 = dqy * qy;
        float q22 = dqz * qz;
        float q01 = dqx * qy;
        float q02 = dqx * qz;
        float q03 = dqx * qw;
        float q12 = dqy * qz;
        float q13 = dqy * qw;
        float q23 = dqz * qw;
        this.m00 = sx - (q11 + q22) * sx;
        this.m01 = (q01 + q23) * sx;
        this.m02 = (q02 - q13) * sx;
        this.m10 = (q01 - q23) * sy;
        this.m11 = sy - (q22 + q00) * sy;
        this.m12 = (q12 + q03) * sy;
        this.m20 = (q02 + q13) * sz;
        this.m21 = (q12 - q03) * sz;
        this.m22 = sz - (q11 + q00) * sz;
        this.m30 = tx;
        this.m31 = ty;
        this.m32 = tz;
        this.properties = 0;
        return this;
    }

    public Matrix4x3f translationRotateScale(Vector3f translation, Quaternionf quat, Vector3f scale) {
        return this.translationRotateScale(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale.x, scale.y, scale.z);
    }

    public Matrix4x3f translationRotateScaleMul(float tx, float ty, float tz, float qx, float qy, float qz, float qw, float sx, float sy, float sz, Matrix4x3f m) {
        float dqx = qx + qx;
        float dqy = qy + qy;
        float dqz = qz + qz;
        float q00 = dqx * qx;
        float q11 = dqy * qy;
        float q22 = dqz * qz;
        float q01 = dqx * qy;
        float q02 = dqx * qz;
        float q03 = dqx * qw;
        float q12 = dqy * qz;
        float q13 = dqy * qw;
        float q23 = dqz * qw;
        float nm00 = sx - (q11 + q22) * sx;
        float nm01 = (q01 + q23) * sx;
        float nm02 = (q02 - q13) * sx;
        float nm10 = (q01 - q23) * sy;
        float nm11 = sy - (q22 + q00) * sy;
        float nm12 = (q12 + q03) * sy;
        float nm20 = (q02 + q13) * sz;
        float nm21 = (q12 - q03) * sz;
        float nm22 = sz - (q11 + q00) * sz;
        float m00 = nm00 * m.m00 + nm10 * m.m01 + nm20 * m.m02;
        float m01 = nm01 * m.m00 + nm11 * m.m01 + nm21 * m.m02;
        this.m02 = nm02 * m.m00 + nm12 * m.m01 + nm22 * m.m02;
        this.m00 = m00;
        this.m01 = m01;
        float m10 = nm00 * m.m10 + nm10 * m.m11 + nm20 * m.m12;
        float m11 = nm01 * m.m10 + nm11 * m.m11 + nm21 * m.m12;
        this.m12 = nm02 * m.m10 + nm12 * m.m11 + nm22 * m.m12;
        this.m10 = m10;
        this.m11 = m11;
        float m20 = nm00 * m.m20 + nm10 * m.m21 + nm20 * m.m22;
        float m21 = nm01 * m.m20 + nm11 * m.m21 + nm21 * m.m22;
        this.m22 = nm02 * m.m20 + nm12 * m.m21 + nm22 * m.m22;
        this.m20 = m20;
        this.m21 = m21;
        float m30 = nm00 * m.m30 + nm10 * m.m31 + nm20 * m.m32 + tx;
        float m31 = nm01 * m.m30 + nm11 * m.m31 + nm21 * m.m32 + ty;
        this.m32 = nm02 * m.m30 + nm12 * m.m31 + nm22 * m.m32 + tz;
        this.m30 = m30;
        this.m31 = m31;
        this.properties = 0;
        return this;
    }

    public Matrix4x3f translationRotateScaleMul(Vector3f translation, Quaternionf quat, Vector3f scale, Matrix4x3f m) {
        return this.translationRotateScaleMul(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale.x, scale.y, scale.z, m);
    }

    public Matrix4x3f translationRotate(float tx, float ty, float tz, Quaternionf quat) {
        float dqx = quat.x + quat.x;
        float dqy = quat.y + quat.y;
        float dqz = quat.z + quat.z;
        float q00 = dqx * quat.x;
        float q11 = dqy * quat.y;
        float q22 = dqz * quat.z;
        float q01 = dqx * quat.y;
        float q02 = dqx * quat.z;
        float q03 = dqx * quat.w;
        float q12 = dqy * quat.z;
        float q13 = dqy * quat.w;
        float q23 = dqz * quat.w;
        this.m00 = 1f - (q11 + q22);
        this.m01 = q01 + q23;
        this.m02 = q02 - q13;
        this.m10 = q01 - q23;
        this.m11 = 1f - (q22 + q00);
        this.m12 = q12 + q03;
        this.m20 = q02 + q13;
        this.m21 = q12 - q03;
        this.m22 = 1f - (q11 + q00);
        this.m30 = tx;
        this.m31 = ty;
        this.m32 = tz;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f translationRotate(float tx, float ty, float tz, float qx, float qy, float qz, float qw) {
        float w2 = qw * qw;
        float x2 = qx * qx;
        float y2 = qy * qy;
        float z2 = qz * qz;
        float zw = qz * qw;
        float xy = qx * qy;
        float xz = qx * qz;
        float yw = qy * qw;
        float yz = qy * qz;
        float xw = qx * qw;
        this.m00 = w2 + x2 - z2 - y2;
        this.m01 = xy + zw + zw + xy;
        this.m02 = xz - yw + xz - yw;
        this.m10 = -zw + xy - zw + xy;
        this.m11 = y2 - z2 + w2 - x2;
        this.m12 = yz + yz + xw + xw;
        this.m20 = yw + xz + xz + yw;
        this.m21 = yz + yz - xw - xw;
        this.m22 = z2 - y2 - x2 + w2;
        this.m30 = tx;
        this.m31 = ty;
        this.m32 = tz;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f translationRotate(Vector3f translation, Quaternionf quat) {
        return this.translationRotate(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w);
    }

    public Matrix4x3f translationRotateMul(float tx, float ty, float tz, Quaternionf quat, Matrix4x3f mat) {
        return this.translationRotateMul(tx, ty, tz, quat.x, quat.y, quat.z, quat.w, mat);
    }

    public Matrix4x3f translationRotateMul(float tx, float ty, float tz, float qx, float qy, float qz, float qw, Matrix4x3f mat) {
        float w2 = qw * qw;
        float x2 = qx * qx;
        float y2 = qy * qy;
        float z2 = qz * qz;
        float zw = qz * qw;
        float xy = qx * qy;
        float xz = qx * qz;
        float yw = qy * qw;
        float yz = qy * qz;
        float xw = qx * qw;
        float nm00 = w2 + x2 - z2 - y2;
        float nm01 = xy + zw + zw + xy;
        float nm02 = xz - yw + xz - yw;
        float nm10 = -zw + xy - zw + xy;
        float nm11 = y2 - z2 + w2 - x2;
        float nm12 = yz + yz + xw + xw;
        float nm20 = yw + xz + xz + yw;
        float nm21 = yz + yz - xw - xw;
        float nm22 = z2 - y2 - x2 + w2;
        this.m00 = nm00 * mat.m00 + nm10 * mat.m01 + nm20 * mat.m02;
        this.m01 = nm01 * mat.m00 + nm11 * mat.m01 + nm21 * mat.m02;
        this.m02 = nm02 * mat.m00 + nm12 * mat.m01 + nm22 * mat.m02;
        this.m10 = nm00 * mat.m10 + nm10 * mat.m11 + nm20 * mat.m12;
        this.m11 = nm01 * mat.m10 + nm11 * mat.m11 + nm21 * mat.m12;
        this.m12 = nm02 * mat.m10 + nm12 * mat.m11 + nm22 * mat.m12;
        this.m20 = nm00 * mat.m20 + nm10 * mat.m21 + nm20 * mat.m22;
        this.m21 = nm01 * mat.m20 + nm11 * mat.m21 + nm21 * mat.m22;
        this.m22 = nm02 * mat.m20 + nm12 * mat.m21 + nm22 * mat.m22;
        this.m30 = nm00 * mat.m30 + nm10 * mat.m31 + nm20 * mat.m32 + tx;
        this.m31 = nm01 * mat.m30 + nm11 * mat.m31 + nm21 * mat.m32 + ty;
        this.m32 = nm02 * mat.m30 + nm12 * mat.m31 + nm22 * mat.m32 + tz;
        this.properties = 0;
        return this;
    }

    public Matrix4x3f translationRotateInvert(float tx, float ty, float tz, float qx, float qy, float qz, float qw) {
        float nqx = -qx;
        float nqy = -qy;
        float nqz = -qz;
        float dqx = nqx + nqx;
        float dqy = nqy + nqy;
        float dqz = nqz + nqz;
        float q00 = dqx * nqx;
        float q11 = dqy * nqy;
        float q22 = dqz * nqz;
        float q01 = dqx * nqy;
        float q02 = dqx * nqz;
        float q03 = dqx * qw;
        float q12 = dqy * nqz;
        float q13 = dqy * qw;
        float q23 = dqz * qw;
        return this._m00(1f - q11 - q22)._m01(q01 + q23)._m02(q02 - q13)._m10(q01 - q23)._m11(1f - q22 - q00)._m12(q12 + q03)._m20(q02 + q13)._m21(q12 - q03)._m22(1f - q11 - q00)._m30(-this.m00 * tx - this.m10 * ty - this.m20 * tz)._m31(-this.m01 * tx - this.m11 * ty - this.m21 * tz)._m32(-this.m02 * tx - this.m12 * ty - this.m22 * tz)._properties(16);
    }

    public Matrix4x3f translationRotateInvert(Vector3f translation, Quaternionf quat) {
        return this.translationRotateInvert(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w);
    }

    public Matrix4x3f set3x3(Matrix3f mat) {
        this.set3x3Matrix3f(mat);
        this.properties = 0;
        return this;
    }

    private void set3x3Matrix3f(Matrix3f mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m02 = mat.m02;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
        this.m12 = mat.m12;
        this.m20 = mat.m20;
        this.m21 = mat.m21;
        this.m22 = mat.m22;
    }

    public Vector4f transform(Vector4f v) {
        return v.mul(this);
    }

    public Vector4f transform(Vector4f v, Vector4f dest) {
        return v.mul(this, dest);
    }

    public Vector3f transformPosition(Vector3f v) {
        v.set(this.m00 * v.x + this.m10 * v.y + this.m20 * v.z + this.m30, this.m01 * v.x + this.m11 * v.y + this.m21 * v.z + this.m31, this.m02 * v.x + this.m12 * v.y + this.m22 * v.z + this.m32);
        return v;
    }

    public Vector3f transformPosition(Vector3f v, Vector3f dest) {
        dest.set(this.m00 * v.x + this.m10 * v.y + this.m20 * v.z + this.m30, this.m01 * v.x + this.m11 * v.y + this.m21 * v.z + this.m31, this.m02 * v.x + this.m12 * v.y + this.m22 * v.z + this.m32);
        return dest;
    }

    public Vector3f transformDirection(Vector3f v) {
        v.set(this.m00 * v.x + this.m10 * v.y + this.m20 * v.z, this.m01 * v.x + this.m11 * v.y + this.m21 * v.z, this.m02 * v.x + this.m12 * v.y + this.m22 * v.z);
        return v;
    }

    public Vector3f transformDirection(Vector3f v, Vector3f dest) {
        dest.set(this.m00 * v.x + this.m10 * v.y + this.m20 * v.z, this.m01 * v.x + this.m11 * v.y + this.m21 * v.z, this.m02 * v.x + this.m12 * v.y + this.m22 * v.z);
        return dest;
    }

    public Matrix4x3f scale(Vector3f xyz, Matrix4x3f dest) {
        return this.scale(xyz.x, xyz.y, xyz.z, dest);
    }

    public Matrix4x3f scale(Vector3f xyz) {
        return this.scale(xyz.x, xyz.y, xyz.z, this);
    }

    public Matrix4x3f scale(float xyz, Matrix4x3f dest) {
        return this.scale(xyz, xyz, xyz, dest);
    }

    public Matrix4x3f scale(float xyz) {
        return this.scale(xyz, xyz, xyz);
    }

    public Matrix4x3f scaleXY(float x, float y, Matrix4x3f dest) {
        return this.scale(x, y, 1f, dest);
    }

    public Matrix4x3f scaleXY(float x, float y) {
        return this.scale(x, y, 1f);
    }

    public Matrix4x3f scale(float x, float y, float z, Matrix4x3f dest) {
        return (this.properties & 4) != 0 ? dest.scaling(x, y, z) : this.scaleGeneric(x, y, z, dest);
    }

    private Matrix4x3f scaleGeneric(float x, float y, float z, Matrix4x3f dest) {
        dest.m00 = this.m00 * x;
        dest.m01 = this.m01 * x;
        dest.m02 = this.m02 * x;
        dest.m10 = this.m10 * y;
        dest.m11 = this.m11 * y;
        dest.m12 = this.m12 * y;
        dest.m20 = this.m20 * z;
        dest.m21 = this.m21 * z;
        dest.m22 = this.m22 * z;
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -29;
        return dest;
    }

    public Matrix4x3f scale(float x, float y, float z) {
        return this.scale(x, y, z, this);
    }

    public Matrix4x3f scaleLocal(float x, float y, float z, Matrix4x3f dest) {
        if ((this.properties & 4) != 0) {
            return dest.scaling(x, y, z);
        } else {
            float nm00 = x * this.m00;
            float nm01 = y * this.m01;
            float nm02 = z * this.m02;
            float nm10 = x * this.m10;
            float nm11 = y * this.m11;
            float nm12 = z * this.m12;
            float nm20 = x * this.m20;
            float nm21 = y * this.m21;
            float nm22 = z * this.m22;
            float nm30 = x * this.m30;
            float nm31 = y * this.m31;
            float nm32 = z * this.m32;
            dest.m00 = nm00;
            dest.m01 = nm01;
            dest.m02 = nm02;
            dest.m10 = nm10;
            dest.m11 = nm11;
            dest.m12 = nm12;
            dest.m20 = nm20;
            dest.m21 = nm21;
            dest.m22 = nm22;
            dest.m30 = nm30;
            dest.m31 = nm31;
            dest.m32 = nm32;
            dest.properties = this.properties & -29;
            return dest;
        }
    }

    public Matrix4x3f scaleAround(float sx, float sy, float sz, float ox, float oy, float oz, Matrix4x3f dest) {
        float nm30 = this.m00 * ox + this.m10 * oy + this.m20 * oz + this.m30;
        float nm31 = this.m01 * ox + this.m11 * oy + this.m21 * oz + this.m31;
        float nm32 = this.m02 * ox + this.m12 * oy + this.m22 * oz + this.m32;
        boolean one = Math.absEqualsOne(sx) && Math.absEqualsOne(sy) && Math.absEqualsOne(sz);
        return dest._m00(this.m00 * sx)._m01(this.m01 * sx)._m02(this.m02 * sx)._m10(this.m10 * sy)._m11(this.m11 * sy)._m12(this.m12 * sy)._m20(this.m20 * sz)._m21(this.m21 * sz)._m22(this.m22 * sz)._m30(-dest.m00 * ox - dest.m10 * oy - dest.m20 * oz + nm30)._m31(-dest.m01 * ox - dest.m11 * oy - dest.m21 * oz + nm31)._m32(-dest.m02 * ox - dest.m12 * oy - dest.m22 * oz + nm32)._properties(this.properties & ~(12 | (one ? 0 : 16)));
    }

    public Matrix4x3f scaleAround(float sx, float sy, float sz, float ox, float oy, float oz) {
        return this.scaleAround(sx, sy, sz, ox, oy, oz, this);
    }

    public Matrix4x3f scaleAround(float factor, float ox, float oy, float oz) {
        return this.scaleAround(factor, factor, factor, ox, oy, oz, this);
    }

    public Matrix4x3f scaleAround(float factor, float ox, float oy, float oz, Matrix4x3f dest) {
        return this.scaleAround(factor, factor, factor, ox, oy, oz, dest);
    }

    public Matrix4x3f scaleLocal(float x, float y, float z) {
        return this.scaleLocal(x, y, z, this);
    }

    public Matrix4x3f rotateX(float ang, Matrix4x3f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationX(ang);
        } else if ((this.properties & 8) != 0) {
            float x = this.m30;
            float y = this.m31;
            float z = this.m32;
            return dest.rotationX(ang).setTranslation(x, y, z);
        } else {
            return this.rotateXInternal(ang, dest);
        }
    }

    private Matrix4x3f rotateXInternal(float ang, Matrix4x3f dest) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        float rm21 = -sin;
        float nm10 = this.m10 * cos + this.m20 * sin;
        float nm11 = this.m11 * cos + this.m21 * sin;
        float nm12 = this.m12 * cos + this.m22 * sin;
        dest.m20 = this.m10 * rm21 + this.m20 * cos;
        dest.m21 = this.m11 * rm21 + this.m21 * cos;
        dest.m22 = this.m12 * rm21 + this.m22 * cos;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m00 = this.m00;
        dest.m01 = this.m01;
        dest.m02 = this.m02;
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f rotateX(float ang) {
        return this.rotateX(ang, this);
    }

    public Matrix4x3f rotateY(float ang, Matrix4x3f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationY(ang);
        } else if ((this.properties & 8) != 0) {
            float x = this.m30;
            float y = this.m31;
            float z = this.m32;
            return dest.rotationY(ang).setTranslation(x, y, z);
        } else {
            return this.rotateYInternal(ang, dest);
        }
    }

    private Matrix4x3f rotateYInternal(float ang, Matrix4x3f dest) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        float rm02 = -sin;
        float nm00 = this.m00 * cos + this.m20 * rm02;
        float nm01 = this.m01 * cos + this.m21 * rm02;
        float nm02 = this.m02 * cos + this.m22 * rm02;
        dest.m20 = this.m00 * sin + this.m20 * cos;
        dest.m21 = this.m01 * sin + this.m21 * cos;
        dest.m22 = this.m02 * sin + this.m22 * cos;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = this.m10;
        dest.m11 = this.m11;
        dest.m12 = this.m12;
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f rotateY(float ang) {
        return this.rotateY(ang, this);
    }

    public Matrix4x3f rotateZ(float ang, Matrix4x3f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationZ(ang);
        } else if ((this.properties & 8) != 0) {
            float x = this.m30;
            float y = this.m31;
            float z = this.m32;
            return dest.rotationZ(ang).setTranslation(x, y, z);
        } else {
            return this.rotateZInternal(ang, dest);
        }
    }

    private Matrix4x3f rotateZInternal(float ang, Matrix4x3f dest) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        float rm10 = -sin;
        float nm00 = this.m00 * cos + this.m10 * sin;
        float nm01 = this.m01 * cos + this.m11 * sin;
        float nm02 = this.m02 * cos + this.m12 * sin;
        dest.m10 = this.m00 * rm10 + this.m10 * cos;
        dest.m11 = this.m01 * rm10 + this.m11 * cos;
        dest.m12 = this.m02 * rm10 + this.m12 * cos;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m20 = this.m20;
        dest.m21 = this.m21;
        dest.m22 = this.m22;
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f rotateZ(float ang) {
        return this.rotateZ(ang, this);
    }

    public Matrix4x3f rotateXYZ(Vector3f angles) {
        return this.rotateXYZ(angles.x, angles.y, angles.z);
    }

    public Matrix4x3f rotateXYZ(float angleX, float angleY, float angleZ) {
        return this.rotateXYZ(angleX, angleY, angleZ, this);
    }

    public Matrix4x3f rotateXYZ(float angleX, float angleY, float angleZ, Matrix4x3f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationXYZ(angleX, angleY, angleZ);
        } else if ((this.properties & 8) != 0) {
            float tx = this.m30;
            float ty = this.m31;
            float tz = this.m32;
            return dest.rotationXYZ(angleX, angleY, angleZ).setTranslation(tx, ty, tz);
        } else {
            return this.rotateXYZInternal(angleX, angleY, angleZ, dest);
        }
    }

    private Matrix4x3f rotateXYZInternal(float angleX, float angleY, float angleZ, Matrix4x3f dest) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float m_sinX = -sinX;
        float m_sinY = -sinY;
        float m_sinZ = -sinZ;
        float nm10 = this.m10 * cosX + this.m20 * sinX;
        float nm11 = this.m11 * cosX + this.m21 * sinX;
        float nm12 = this.m12 * cosX + this.m22 * sinX;
        float nm20 = this.m10 * m_sinX + this.m20 * cosX;
        float nm21 = this.m11 * m_sinX + this.m21 * cosX;
        float nm22 = this.m12 * m_sinX + this.m22 * cosX;
        float nm00 = this.m00 * cosY + nm20 * m_sinY;
        float nm01 = this.m01 * cosY + nm21 * m_sinY;
        float nm02 = this.m02 * cosY + nm22 * m_sinY;
        dest.m20 = this.m00 * sinY + nm20 * cosY;
        dest.m21 = this.m01 * sinY + nm21 * cosY;
        dest.m22 = this.m02 * sinY + nm22 * cosY;
        dest.m00 = nm00 * cosZ + nm10 * sinZ;
        dest.m01 = nm01 * cosZ + nm11 * sinZ;
        dest.m02 = nm02 * cosZ + nm12 * sinZ;
        dest.m10 = nm00 * m_sinZ + nm10 * cosZ;
        dest.m11 = nm01 * m_sinZ + nm11 * cosZ;
        dest.m12 = nm02 * m_sinZ + nm12 * cosZ;
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f rotateZYX(Vector3f angles) {
        return this.rotateZYX(angles.z, angles.y, angles.x);
    }

    public Matrix4x3f rotateZYX(float angleZ, float angleY, float angleX) {
        return this.rotateZYX(angleZ, angleY, angleX, this);
    }

    public Matrix4x3f rotateZYX(float angleZ, float angleY, float angleX, Matrix4x3f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationZYX(angleZ, angleY, angleX);
        } else if ((this.properties & 8) != 0) {
            float tx = this.m30;
            float ty = this.m31;
            float tz = this.m32;
            return dest.rotationZYX(angleZ, angleY, angleX).setTranslation(tx, ty, tz);
        } else {
            return this.rotateZYXInternal(angleZ, angleY, angleX, dest);
        }
    }

    private Matrix4x3f rotateZYXInternal(float angleZ, float angleY, float angleX, Matrix4x3f dest) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float m_sinZ = -sinZ;
        float m_sinY = -sinY;
        float m_sinX = -sinX;
        float nm00 = this.m00 * cosZ + this.m10 * sinZ;
        float nm01 = this.m01 * cosZ + this.m11 * sinZ;
        float nm02 = this.m02 * cosZ + this.m12 * sinZ;
        float nm10 = this.m00 * m_sinZ + this.m10 * cosZ;
        float nm11 = this.m01 * m_sinZ + this.m11 * cosZ;
        float nm12 = this.m02 * m_sinZ + this.m12 * cosZ;
        float nm20 = nm00 * sinY + this.m20 * cosY;
        float nm21 = nm01 * sinY + this.m21 * cosY;
        float nm22 = nm02 * sinY + this.m22 * cosY;
        dest.m00 = nm00 * cosY + this.m20 * m_sinY;
        dest.m01 = nm01 * cosY + this.m21 * m_sinY;
        dest.m02 = nm02 * cosY + this.m22 * m_sinY;
        dest.m10 = nm10 * cosX + nm20 * sinX;
        dest.m11 = nm11 * cosX + nm21 * sinX;
        dest.m12 = nm12 * cosX + nm22 * sinX;
        dest.m20 = nm10 * m_sinX + nm20 * cosX;
        dest.m21 = nm11 * m_sinX + nm21 * cosX;
        dest.m22 = nm12 * m_sinX + nm22 * cosX;
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f rotateYXZ(Vector3f angles) {
        return this.rotateYXZ(angles.y, angles.x, angles.z);
    }

    public Matrix4x3f rotateYXZ(float angleY, float angleX, float angleZ) {
        return this.rotateYXZ(angleY, angleX, angleZ, this);
    }

    public Matrix4x3f rotateYXZ(float angleY, float angleX, float angleZ, Matrix4x3f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationYXZ(angleY, angleX, angleZ);
        } else if ((this.properties & 8) != 0) {
            float tx = this.m30;
            float ty = this.m31;
            float tz = this.m32;
            return dest.rotationYXZ(angleY, angleX, angleZ).setTranslation(tx, ty, tz);
        } else {
            return this.rotateYXZInternal(angleY, angleX, angleZ, dest);
        }
    }

    private Matrix4x3f rotateYXZInternal(float angleY, float angleX, float angleZ, Matrix4x3f dest) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float m_sinY = -sinY;
        float m_sinX = -sinX;
        float m_sinZ = -sinZ;
        float nm20 = this.m00 * sinY + this.m20 * cosY;
        float nm21 = this.m01 * sinY + this.m21 * cosY;
        float nm22 = this.m02 * sinY + this.m22 * cosY;
        float nm00 = this.m00 * cosY + this.m20 * m_sinY;
        float nm01 = this.m01 * cosY + this.m21 * m_sinY;
        float nm02 = this.m02 * cosY + this.m22 * m_sinY;
        float nm10 = this.m10 * cosX + nm20 * sinX;
        float nm11 = this.m11 * cosX + nm21 * sinX;
        float nm12 = this.m12 * cosX + nm22 * sinX;
        dest.m20 = this.m10 * m_sinX + nm20 * cosX;
        dest.m21 = this.m11 * m_sinX + nm21 * cosX;
        dest.m22 = this.m12 * m_sinX + nm22 * cosX;
        dest.m00 = nm00 * cosZ + nm10 * sinZ;
        dest.m01 = nm01 * cosZ + nm11 * sinZ;
        dest.m02 = nm02 * cosZ + nm12 * sinZ;
        dest.m10 = nm00 * m_sinZ + nm10 * cosZ;
        dest.m11 = nm01 * m_sinZ + nm11 * cosZ;
        dest.m12 = nm02 * m_sinZ + nm12 * cosZ;
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f rotate(float ang, float x, float y, float z, Matrix4x3f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotation(ang, x, y, z);
        } else {
            return (this.properties & 8) != 0 ? this.rotateTranslation(ang, x, y, z, dest) : this.rotateGeneric(ang, x, y, z, dest);
        }
    }

    private Matrix4x3f rotateGeneric(float ang, float x, float y, float z, Matrix4x3f dest) {
        if (y == 0f && z == 0f && Math.absEqualsOne(x)) {
            return this.rotateX(x * ang, dest);
        } else if (x == 0f && z == 0f && Math.absEqualsOne(y)) {
            return this.rotateY(y * ang, dest);
        } else {
            return x == 0f && y == 0f && Math.absEqualsOne(z) ? this.rotateZ(z * ang, dest) : this.rotateGenericInternal(ang, x, y, z, dest);
        }
    }

    private Matrix4x3f rotateGenericInternal(float ang, float x, float y, float z, Matrix4x3f dest) {
        float s = Math.sin(ang);
        float c = Math.cosFromSin(s, ang);
        float C = 1f - c;
        float xx = x * x;
        float xy = x * y;
        float xz = x * z;
        float yy = y * y;
        float yz = y * z;
        float zz = z * z;
        float rm00 = xx * C + c;
        float rm01 = xy * C + z * s;
        float rm02 = xz * C - y * s;
        float rm10 = xy * C - z * s;
        float rm11 = yy * C + c;
        float rm12 = yz * C + x * s;
        float rm20 = xz * C + y * s;
        float rm21 = yz * C - x * s;
        float rm22 = zz * C + c;
        float nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        float nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        float nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        float nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        float nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        float nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        dest.m20 = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22;
        dest.m21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22;
        dest.m22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f rotate(float ang, float x, float y, float z) {
        return this.rotate(ang, x, y, z, this);
    }

    public Matrix4x3f rotateTranslation(float ang, float x, float y, float z, Matrix4x3f dest) {
        float tx = this.m30;
        float ty = this.m31;
        float tz = this.m32;
        if (y == 0f && z == 0f && Math.absEqualsOne(x)) {
            return dest.rotationX(x * ang).setTranslation(tx, ty, tz);
        } else if (x == 0f && z == 0f && Math.absEqualsOne(y)) {
            return dest.rotationY(y * ang).setTranslation(tx, ty, tz);
        } else {
            return x == 0f && y == 0f && Math.absEqualsOne(z) ? dest.rotationZ(z * ang).setTranslation(tx, ty, tz) : this.rotateTranslationInternal(ang, x, y, z, dest);
        }
    }

    private Matrix4x3f rotateTranslationInternal(float ang, float x, float y, float z, Matrix4x3f dest) {
        float s = Math.sin(ang);
        float c = Math.cosFromSin(s, ang);
        float C = 1f - c;
        float xx = x * x;
        float xy = x * y;
        float xz = x * z;
        float yy = y * y;
        float yz = y * z;
        float zz = z * z;
        float rm00 = xx * C + c;
        float rm01 = xy * C + z * s;
        float rm02 = xz * C - y * s;
        float rm10 = xy * C - z * s;
        float rm11 = yy * C + c;
        float rm12 = yz * C + x * s;
        float rm20 = xz * C + y * s;
        float rm21 = yz * C - x * s;
        float rm22 = zz * C + c;
        dest.m20 = rm20;
        dest.m21 = rm21;
        dest.m22 = rm22;
        dest.m00 = rm00;
        dest.m01 = rm01;
        dest.m02 = rm02;
        dest.m10 = rm10;
        dest.m11 = rm11;
        dest.m12 = rm12;
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f rotateAround(Quaternionf quat, float ox, float oy, float oz) {
        return this.rotateAround(quat, ox, oy, oz, this);
    }

    private Matrix4x3f rotateAroundAffine(Quaternionf quat, float ox, float oy, float oz, Matrix4x3f dest) {
        float w2 = quat.w * quat.w;
        float x2 = quat.x * quat.x;
        float y2 = quat.y * quat.y;
        float z2 = quat.z * quat.z;
        float zw = quat.z * quat.w;
        float dzw = zw + zw;
        float xy = quat.x * quat.y;
        float dxy = xy + xy;
        float xz = quat.x * quat.z;
        float dxz = xz + xz;
        float yw = quat.y * quat.w;
        float dyw = yw + yw;
        float yz = quat.y * quat.z;
        float dyz = yz + yz;
        float xw = quat.x * quat.w;
        float dxw = xw + xw;
        float rm00 = w2 + x2 - z2 - y2;
        float rm01 = dxy + dzw;
        float rm02 = dxz - dyw;
        float rm10 = dxy - dzw;
        float rm11 = y2 - z2 + w2 - x2;
        float rm12 = dyz + dxw;
        float rm20 = dyw + dxz;
        float rm21 = dyz - dxw;
        float rm22 = z2 - y2 - x2 + w2;
        float tm30 = this.m00 * ox + this.m10 * oy + this.m20 * oz + this.m30;
        float tm31 = this.m01 * ox + this.m11 * oy + this.m21 * oz + this.m31;
        float tm32 = this.m02 * ox + this.m12 * oy + this.m22 * oz + this.m32;
        float nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        float nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        float nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        float nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        float nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        float nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m10(nm10)._m11(nm11)._m12(nm12)._m30(-nm00 * ox - nm10 * oy - this.m20 * oz + tm30)._m31(-nm01 * ox - nm11 * oy - this.m21 * oz + tm31)._m32(-nm02 * ox - nm12 * oy - this.m22 * oz + tm32)._properties(this.properties & -13);
        return dest;
    }

    public Matrix4x3f rotateAround(Quaternionf quat, float ox, float oy, float oz, Matrix4x3f dest) {
        return (this.properties & 4) != 0 ? this.rotationAround(quat, ox, oy, oz) : this.rotateAroundAffine(quat, ox, oy, oz, dest);
    }

    public Matrix4x3f rotationAround(Quaternionf quat, float ox, float oy, float oz) {
        float w2 = quat.w * quat.w;
        float x2 = quat.x * quat.x;
        float y2 = quat.y * quat.y;
        float z2 = quat.z * quat.z;
        float zw = quat.z * quat.w;
        float dzw = zw + zw;
        float xy = quat.x * quat.y;
        float dxy = xy + xy;
        float xz = quat.x * quat.z;
        float dxz = xz + xz;
        float yw = quat.y * quat.w;
        float dyw = yw + yw;
        float yz = quat.y * quat.z;
        float dyz = yz + yz;
        float xw = quat.x * quat.w;
        float dxw = xw + xw;
        this._m20(dyw + dxz);
        this._m21(dyz - dxw);
        this._m22(z2 - y2 - x2 + w2);
        this._m00(w2 + x2 - z2 - y2);
        this._m01(dxy + dzw);
        this._m02(dxz - dyw);
        this._m10(dxy - dzw);
        this._m11(y2 - z2 + w2 - x2);
        this._m12(dyz + dxw);
        this._m30(-this.m00 * ox - this.m10 * oy - this.m20 * oz + ox);
        this._m31(-this.m01 * ox - this.m11 * oy - this.m21 * oz + oy);
        this._m32(-this.m02 * ox - this.m12 * oy - this.m22 * oz + oz);
        this.properties = 16;
        return this;
    }

    public Matrix4x3f rotateLocal(float ang, float x, float y, float z, Matrix4x3f dest) {
        if (y == 0f && z == 0f && Math.absEqualsOne(x)) {
            return this.rotateLocalX(x * ang, dest);
        } else if (x == 0f && z == 0f && Math.absEqualsOne(y)) {
            return this.rotateLocalY(y * ang, dest);
        } else {
            return x == 0f && y == 0f && Math.absEqualsOne(z) ? this.rotateLocalZ(z * ang, dest) : this.rotateLocalInternal(ang, x, y, z, dest);
        }
    }

    private Matrix4x3f rotateLocalInternal(float ang, float x, float y, float z, Matrix4x3f dest) {
        float s = Math.sin(ang);
        float c = Math.cosFromSin(s, ang);
        float C = 1f - c;
        float xx = x * x;
        float xy = x * y;
        float xz = x * z;
        float yy = y * y;
        float yz = y * z;
        float zz = z * z;
        float lm00 = xx * C + c;
        float lm01 = xy * C + z * s;
        float lm02 = xz * C - y * s;
        float lm10 = xy * C - z * s;
        float lm11 = yy * C + c;
        float lm12 = yz * C + x * s;
        float lm20 = xz * C + y * s;
        float lm21 = yz * C - x * s;
        float lm22 = zz * C + c;
        float nm00 = lm00 * this.m00 + lm10 * this.m01 + lm20 * this.m02;
        float nm01 = lm01 * this.m00 + lm11 * this.m01 + lm21 * this.m02;
        float nm02 = lm02 * this.m00 + lm12 * this.m01 + lm22 * this.m02;
        float nm10 = lm00 * this.m10 + lm10 * this.m11 + lm20 * this.m12;
        float nm11 = lm01 * this.m10 + lm11 * this.m11 + lm21 * this.m12;
        float nm12 = lm02 * this.m10 + lm12 * this.m11 + lm22 * this.m12;
        float nm20 = lm00 * this.m20 + lm10 * this.m21 + lm20 * this.m22;
        float nm21 = lm01 * this.m20 + lm11 * this.m21 + lm21 * this.m22;
        float nm22 = lm02 * this.m20 + lm12 * this.m21 + lm22 * this.m22;
        float nm30 = lm00 * this.m30 + lm10 * this.m31 + lm20 * this.m32;
        float nm31 = lm01 * this.m30 + lm11 * this.m31 + lm21 * this.m32;
        float nm32 = lm02 * this.m30 + lm12 * this.m31 + lm22 * this.m32;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        dest.m30 = nm30;
        dest.m31 = nm31;
        dest.m32 = nm32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f rotateLocal(float ang, float x, float y, float z) {
        return this.rotateLocal(ang, x, y, z, this);
    }

    public Matrix4x3f rotateLocalX(float ang, Matrix4x3f dest) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        float nm01 = cos * this.m01 - sin * this.m02;
        float nm02 = sin * this.m01 + cos * this.m02;
        float nm11 = cos * this.m11 - sin * this.m12;
        float nm12 = sin * this.m11 + cos * this.m12;
        float nm21 = cos * this.m21 - sin * this.m22;
        float nm22 = sin * this.m21 + cos * this.m22;
        float nm31 = cos * this.m31 - sin * this.m32;
        float nm32 = sin * this.m31 + cos * this.m32;
        dest._m00(this.m00)._m01(nm01)._m02(nm02)._m10(this.m10)._m11(nm11)._m12(nm12)._m20(this.m20)._m21(nm21)._m22(nm22)._m30(this.m30)._m31(nm31)._m32(nm32)._properties(this.properties & -13);
        return dest;
    }

    public Matrix4x3f rotateLocalX(float ang) {
        return this.rotateLocalX(ang, this);
    }

    public Matrix4x3f rotateLocalY(float ang, Matrix4x3f dest) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        float nm00 = cos * this.m00 + sin * this.m02;
        float nm02 = -sin * this.m00 + cos * this.m02;
        float nm10 = cos * this.m10 + sin * this.m12;
        float nm12 = -sin * this.m10 + cos * this.m12;
        float nm20 = cos * this.m20 + sin * this.m22;
        float nm22 = -sin * this.m20 + cos * this.m22;
        float nm30 = cos * this.m30 + sin * this.m32;
        float nm32 = -sin * this.m30 + cos * this.m32;
        dest._m00(nm00)._m01(this.m01)._m02(nm02)._m10(nm10)._m11(this.m11)._m12(nm12)._m20(nm20)._m21(this.m21)._m22(nm22)._m30(nm30)._m31(this.m31)._m32(nm32)._properties(this.properties & -13);
        return dest;
    }

    public Matrix4x3f rotateLocalY(float ang) {
        return this.rotateLocalY(ang, this);
    }

    public Matrix4x3f rotateLocalZ(float ang, Matrix4x3f dest) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        float nm00 = cos * this.m00 - sin * this.m01;
        float nm01 = sin * this.m00 + cos * this.m01;
        float nm10 = cos * this.m10 - sin * this.m11;
        float nm11 = sin * this.m10 + cos * this.m11;
        float nm20 = cos * this.m20 - sin * this.m21;
        float nm21 = sin * this.m20 + cos * this.m21;
        float nm30 = cos * this.m30 - sin * this.m31;
        float nm31 = sin * this.m30 + cos * this.m31;
        dest._m00(nm00)._m01(nm01)._m02(this.m02)._m10(nm10)._m11(nm11)._m12(this.m12)._m20(nm20)._m21(nm21)._m22(this.m22)._m30(nm30)._m31(nm31)._m32(this.m32)._properties(this.properties & -13);
        return dest;
    }

    public Matrix4x3f rotateLocalZ(float ang) {
        return this.rotateLocalZ(ang, this);
    }

    public Matrix4x3f translate(Vector3f offset) {
        return this.translate(offset.x, offset.y, offset.z);
    }

    public Matrix4x3f translate(Vector3f offset, Matrix4x3f dest) {
        return this.translate(offset.x, offset.y, offset.z, dest);
    }

    public Matrix4x3f translate(float x, float y, float z, Matrix4x3f dest) {
        return (this.properties & 4) != 0 ? dest.translation(x, y, z) : this.translateGeneric(x, y, z, dest);
    }

    private Matrix4x3f translateGeneric(float x, float y, float z, Matrix4x3f dest) {
        MemUtil.INSTANCE.copy(this, dest);
        dest.m30 = this.m00 * x + this.m10 * y + this.m20 * z + this.m30;
        dest.m31 = this.m01 * x + this.m11 * y + this.m21 * z + this.m31;
        dest.m32 = this.m02 * x + this.m12 * y + this.m22 * z + this.m32;
        dest.properties = this.properties & -5;
        return dest;
    }

    public Matrix4x3f translate(float x, float y, float z) {
        if ((this.properties & 4) != 0) {
            return this.translation(x, y, z);
        } else {
            this.m30 += this.m00 * x + this.m10 * y + this.m20 * z;
            this.m31 += this.m01 * x + this.m11 * y + this.m21 * z;
            this.m32 += this.m02 * x + this.m12 * y + this.m22 * z;
            this.properties &= -5;
            return this;
        }
    }

    public Matrix4x3f translateLocal(Vector3f offset) {
        return this.translateLocal(offset.x, offset.y, offset.z);
    }

    public Matrix4x3f translateLocal(Vector3f offset, Matrix4x3f dest) {
        return this.translateLocal(offset.x, offset.y, offset.z, dest);
    }

    public Matrix4x3f translateLocal(float x, float y, float z, Matrix4x3f dest) {
        dest.m00 = this.m00;
        dest.m01 = this.m01;
        dest.m02 = this.m02;
        dest.m10 = this.m10;
        dest.m11 = this.m11;
        dest.m12 = this.m12;
        dest.m20 = this.m20;
        dest.m21 = this.m21;
        dest.m22 = this.m22;
        dest.m30 = this.m30 + x;
        dest.m31 = this.m31 + y;
        dest.m32 = this.m32 + z;
        dest.properties = this.properties & -5;
        return dest;
    }

    public Matrix4x3f translateLocal(float x, float y, float z) {
        return this.translateLocal(x, y, z, this);
    }

    public Matrix4x3f ortho(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne, Matrix4x3f dest) {
        float rm00 = 2f / (right - left);
        float rm11 = 2f / (top - bottom);
        float rm22 = (zZeroToOne ? 1f : 2f) / (zNear - zFar);
        float rm30 = (left + right) / (left - right);
        float rm31 = (top + bottom) / (bottom - top);
        float rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        dest.m30 = this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30;
        dest.m31 = this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31;
        dest.m32 = this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32;
        dest.m00 = this.m00 * rm00;
        dest.m01 = this.m01 * rm00;
        dest.m02 = this.m02 * rm00;
        dest.m10 = this.m10 * rm11;
        dest.m11 = this.m11 * rm11;
        dest.m12 = this.m12 * rm11;
        dest.m20 = this.m20 * rm22;
        dest.m21 = this.m21 * rm22;
        dest.m22 = this.m22 * rm22;
        dest.properties = this.properties & -29;
        return dest;
    }

    public Matrix4x3f ortho(float left, float right, float bottom, float top, float zNear, float zFar, Matrix4x3f dest) {
        return this.ortho(left, right, bottom, top, zNear, zFar, false, dest);
    }

    public Matrix4x3f ortho(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne) {
        return this.ortho(left, right, bottom, top, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4x3f ortho(float left, float right, float bottom, float top, float zNear, float zFar) {
        return this.ortho(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4x3f orthoLH(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne, Matrix4x3f dest) {
        float rm00 = 2f / (right - left);
        float rm11 = 2f / (top - bottom);
        float rm22 = (zZeroToOne ? 1f : 2f) / (zFar - zNear);
        float rm30 = (left + right) / (left - right);
        float rm31 = (top + bottom) / (bottom - top);
        float rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        dest.m30 = this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30;
        dest.m31 = this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31;
        dest.m32 = this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32;
        dest.m00 = this.m00 * rm00;
        dest.m01 = this.m01 * rm00;
        dest.m02 = this.m02 * rm00;
        dest.m10 = this.m10 * rm11;
        dest.m11 = this.m11 * rm11;
        dest.m12 = this.m12 * rm11;
        dest.m20 = this.m20 * rm22;
        dest.m21 = this.m21 * rm22;
        dest.m22 = this.m22 * rm22;
        dest.properties = this.properties & -29;
        return dest;
    }

    public Matrix4x3f orthoLH(float left, float right, float bottom, float top, float zNear, float zFar, Matrix4x3f dest) {
        return this.orthoLH(left, right, bottom, top, zNear, zFar, false, dest);
    }

    public Matrix4x3f orthoLH(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne) {
        return this.orthoLH(left, right, bottom, top, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4x3f orthoLH(float left, float right, float bottom, float top, float zNear, float zFar) {
        return this.orthoLH(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4x3f setOrtho(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne) {
        MemUtil.INSTANCE.identity(this);
        this.m00 = 2f / (right - left);
        this.m11 = 2f / (top - bottom);
        this.m22 = (zZeroToOne ? 1f : 2f) / (zNear - zFar);
        this.m30 = (right + left) / (left - right);
        this.m31 = (top + bottom) / (bottom - top);
        this.m32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        this.properties = 0;
        return this;
    }

    public Matrix4x3f setOrtho(float left, float right, float bottom, float top, float zNear, float zFar) {
        return this.setOrtho(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4x3f setOrthoLH(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne) {
        MemUtil.INSTANCE.identity(this);
        this.m00 = 2f / (right - left);
        this.m11 = 2f / (top - bottom);
        this.m22 = (zZeroToOne ? 1f : 2f) / (zFar - zNear);
        this.m30 = (right + left) / (left - right);
        this.m31 = (top + bottom) / (bottom - top);
        this.m32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        this.properties = 0;
        return this;
    }

    public Matrix4x3f setOrthoLH(float left, float right, float bottom, float top, float zNear, float zFar) {
        return this.setOrthoLH(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4x3f orthoSymmetric(float width, float height, float zNear, float zFar, boolean zZeroToOne, Matrix4x3f dest) {
        float rm00 = 2f / width;
        float rm11 = 2f / height;
        float rm22 = (zZeroToOne ? 1f : 2f) / (zNear - zFar);
        float rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        dest.m30 = this.m20 * rm32 + this.m30;
        dest.m31 = this.m21 * rm32 + this.m31;
        dest.m32 = this.m22 * rm32 + this.m32;
        dest.m00 = this.m00 * rm00;
        dest.m01 = this.m01 * rm00;
        dest.m02 = this.m02 * rm00;
        dest.m10 = this.m10 * rm11;
        dest.m11 = this.m11 * rm11;
        dest.m12 = this.m12 * rm11;
        dest.m20 = this.m20 * rm22;
        dest.m21 = this.m21 * rm22;
        dest.m22 = this.m22 * rm22;
        dest.properties = this.properties & -29;
        return dest;
    }

    public Matrix4x3f orthoSymmetric(float width, float height, float zNear, float zFar, Matrix4x3f dest) {
        return this.orthoSymmetric(width, height, zNear, zFar, false, dest);
    }

    public Matrix4x3f orthoSymmetric(float width, float height, float zNear, float zFar, boolean zZeroToOne) {
        return this.orthoSymmetric(width, height, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4x3f orthoSymmetric(float width, float height, float zNear, float zFar) {
        return this.orthoSymmetric(width, height, zNear, zFar, false, this);
    }

    public Matrix4x3f orthoSymmetricLH(float width, float height, float zNear, float zFar, boolean zZeroToOne, Matrix4x3f dest) {
        float rm00 = 2f / width;
        float rm11 = 2f / height;
        float rm22 = (zZeroToOne ? 1f : 2f) / (zFar - zNear);
        float rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        dest.m30 = this.m20 * rm32 + this.m30;
        dest.m31 = this.m21 * rm32 + this.m31;
        dest.m32 = this.m22 * rm32 + this.m32;
        dest.m00 = this.m00 * rm00;
        dest.m01 = this.m01 * rm00;
        dest.m02 = this.m02 * rm00;
        dest.m10 = this.m10 * rm11;
        dest.m11 = this.m11 * rm11;
        dest.m12 = this.m12 * rm11;
        dest.m20 = this.m20 * rm22;
        dest.m21 = this.m21 * rm22;
        dest.m22 = this.m22 * rm22;
        dest.properties = this.properties & -29;
        return dest;
    }

    public Matrix4x3f orthoSymmetricLH(float width, float height, float zNear, float zFar, Matrix4x3f dest) {
        return this.orthoSymmetricLH(width, height, zNear, zFar, false, dest);
    }

    public Matrix4x3f orthoSymmetricLH(float width, float height, float zNear, float zFar, boolean zZeroToOne) {
        return this.orthoSymmetricLH(width, height, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4x3f orthoSymmetricLH(float width, float height, float zNear, float zFar) {
        return this.orthoSymmetricLH(width, height, zNear, zFar, false, this);
    }

    public Matrix4x3f setOrthoSymmetric(float width, float height, float zNear, float zFar, boolean zZeroToOne) {
        MemUtil.INSTANCE.identity(this);
        this.m00 = 2f / width;
        this.m11 = 2f / height;
        this.m22 = (zZeroToOne ? 1f : 2f) / (zNear - zFar);
        this.m32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        this.properties = 0;
        return this;
    }

    public Matrix4x3f setOrthoSymmetric(float width, float height, float zNear, float zFar) {
        return this.setOrthoSymmetric(width, height, zNear, zFar, false);
    }

    public Matrix4x3f setOrthoSymmetricLH(float width, float height, float zNear, float zFar, boolean zZeroToOne) {
        MemUtil.INSTANCE.identity(this);
        this.m00 = 2f / width;
        this.m11 = 2f / height;
        this.m22 = (zZeroToOne ? 1f : 2f) / (zFar - zNear);
        this.m32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        this.properties = 0;
        return this;
    }

    public Matrix4x3f setOrthoSymmetricLH(float width, float height, float zNear, float zFar) {
        return this.setOrthoSymmetricLH(width, height, zNear, zFar, false);
    }

    public Matrix4x3f ortho2D(float left, float right, float bottom, float top, Matrix4x3f dest) {
        float rm00 = 2f / (right - left);
        float rm11 = 2f / (top - bottom);
        float rm30 = -(right + left) / (right - left);
        float rm31 = -(top + bottom) / (top - bottom);
        dest.m30 = this.m00 * rm30 + this.m10 * rm31 + this.m30;
        dest.m31 = this.m01 * rm30 + this.m11 * rm31 + this.m31;
        dest.m32 = this.m02 * rm30 + this.m12 * rm31 + this.m32;
        dest.m00 = this.m00 * rm00;
        dest.m01 = this.m01 * rm00;
        dest.m02 = this.m02 * rm00;
        dest.m10 = this.m10 * rm11;
        dest.m11 = this.m11 * rm11;
        dest.m12 = this.m12 * rm11;
        dest.m20 = -this.m20;
        dest.m21 = -this.m21;
        dest.m22 = -this.m22;
        dest.properties = this.properties & -29;
        return dest;
    }

    public Matrix4x3f ortho2D(float left, float right, float bottom, float top) {
        return this.ortho2D(left, right, bottom, top, this);
    }

    public Matrix4x3f ortho2DLH(float left, float right, float bottom, float top, Matrix4x3f dest) {
        float rm00 = 2f / (right - left);
        float rm11 = 2f / (top - bottom);
        float rm30 = -(right + left) / (right - left);
        float rm31 = -(top + bottom) / (top - bottom);
        dest.m30 = this.m00 * rm30 + this.m10 * rm31 + this.m30;
        dest.m31 = this.m01 * rm30 + this.m11 * rm31 + this.m31;
        dest.m32 = this.m02 * rm30 + this.m12 * rm31 + this.m32;
        dest.m00 = this.m00 * rm00;
        dest.m01 = this.m01 * rm00;
        dest.m02 = this.m02 * rm00;
        dest.m10 = this.m10 * rm11;
        dest.m11 = this.m11 * rm11;
        dest.m12 = this.m12 * rm11;
        dest.m20 = this.m20;
        dest.m21 = this.m21;
        dest.m22 = this.m22;
        dest.properties = this.properties & -29;
        return dest;
    }

    public Matrix4x3f ortho2DLH(float left, float right, float bottom, float top) {
        return this.ortho2DLH(left, right, bottom, top, this);
    }

    public Matrix4x3f setOrtho2D(float left, float right, float bottom, float top) {
        MemUtil.INSTANCE.identity(this);
        this.m00 = 2f / (right - left);
        this.m11 = 2f / (top - bottom);
        this.m22 = -1f;
        this.m30 = -(right + left) / (right - left);
        this.m31 = -(top + bottom) / (top - bottom);
        this.properties = 0;
        return this;
    }

    public Matrix4x3f setOrtho2DLH(float left, float right, float bottom, float top) {
        MemUtil.INSTANCE.identity(this);
        this.m00 = 2f / (right - left);
        this.m11 = 2f / (top - bottom);
        this.m22 = 1f;
        this.m30 = -(right + left) / (right - left);
        this.m31 = -(top + bottom) / (top - bottom);
        this.properties = 0;
        return this;
    }

    public Matrix4x3f lookAlong(Vector3f dir, Vector3f up) {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this);
    }

    public Matrix4x3f lookAlong(Vector3f dir, Vector3f up, Matrix4x3f dest) {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dest);
    }

    public Matrix4x3f lookAlong(float dirX, float dirY, float dirZ, float upX, float upY, float upZ, Matrix4x3f dest) {
        if ((this.properties & 4) != 0) {
            return this.setLookAlong(dirX, dirY, dirZ, upX, upY, upZ);
        } else {
            float invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            dirX *= -invDirLength;
            dirY *= -invDirLength;
            dirZ *= -invDirLength;
            float leftX = upY * dirZ - upZ * dirY;
            float leftY = upZ * dirX - upX * dirZ;
            float leftZ = upX * dirY - upY * dirX;
            float invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
            leftX *= invLeftLength;
            leftY *= invLeftLength;
            leftZ *= invLeftLength;
            float upnX = dirY * leftZ - dirZ * leftY;
            float upnY = dirZ * leftX - dirX * leftZ;
            float upnZ = dirX * leftY - dirY * leftX;
            float nm00 = this.m00 * leftX + this.m10 * upnX + this.m20 * dirX;
            float nm01 = this.m01 * leftX + this.m11 * upnX + this.m21 * dirX;
            float nm02 = this.m02 * leftX + this.m12 * upnX + this.m22 * dirX;
            float nm10 = this.m00 * leftY + this.m10 * upnY + this.m20 * dirY;
            float nm11 = this.m01 * leftY + this.m11 * upnY + this.m21 * dirY;
            float nm12 = this.m02 * leftY + this.m12 * upnY + this.m22 * dirY;
            dest.m20 = this.m00 * leftZ + this.m10 * upnZ + this.m20 * dirZ;
            dest.m21 = this.m01 * leftZ + this.m11 * upnZ + this.m21 * dirZ;
            dest.m22 = this.m02 * leftZ + this.m12 * upnZ + this.m22 * dirZ;
            dest.m00 = nm00;
            dest.m01 = nm01;
            dest.m02 = nm02;
            dest.m10 = nm10;
            dest.m11 = nm11;
            dest.m12 = nm12;
            dest.m30 = this.m30;
            dest.m31 = this.m31;
            dest.m32 = this.m32;
            dest.properties = this.properties & -13;
            return dest;
        }
    }

    public Matrix4x3f lookAlong(float dirX, float dirY, float dirZ, float upX, float upY, float upZ) {
        return this.lookAlong(dirX, dirY, dirZ, upX, upY, upZ, this);
    }

    public Matrix4x3f setLookAlong(Vector3f dir, Vector3f up) {
        return this.setLookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    public Matrix4x3f setLookAlong(float dirX, float dirY, float dirZ, float upX, float upY, float upZ) {
        float invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= -invDirLength;
        dirY *= -invDirLength;
        dirZ *= -invDirLength;
        float leftX = upY * dirZ - upZ * dirY;
        float leftY = upZ * dirX - upX * dirZ;
        float leftZ = upX * dirY - upY * dirX;
        float invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLength;
        leftY *= invLeftLength;
        leftZ *= invLeftLength;
        float upnX = dirY * leftZ - dirZ * leftY;
        float upnY = dirZ * leftX - dirX * leftZ;
        float upnZ = dirX * leftY - dirY * leftX;
        this.m00 = leftX;
        this.m01 = upnX;
        this.m02 = dirX;
        this.m10 = leftY;
        this.m11 = upnY;
        this.m12 = dirY;
        this.m20 = leftZ;
        this.m21 = upnZ;
        this.m22 = dirZ;
        this.m30 = 0f;
        this.m31 = 0f;
        this.m32 = 0f;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f setLookAt(Vector3f eye, Vector3f center, Vector3f up) {
        return this.setLookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z);
    }

    public Matrix4x3f setLookAt(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        float dirX = eyeX - centerX;
        float dirY = eyeY - centerY;
        float dirZ = eyeZ - centerZ;
        float invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= invDirLength;
        dirY *= invDirLength;
        dirZ *= invDirLength;
        float leftX = upY * dirZ - upZ * dirY;
        float leftY = upZ * dirX - upX * dirZ;
        float leftZ = upX * dirY - upY * dirX;
        float invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLength;
        leftY *= invLeftLength;
        leftZ *= invLeftLength;
        float upnX = dirY * leftZ - dirZ * leftY;
        float upnY = dirZ * leftX - dirX * leftZ;
        float upnZ = dirX * leftY - dirY * leftX;
        this.m00 = leftX;
        this.m01 = upnX;
        this.m02 = dirX;
        this.m10 = leftY;
        this.m11 = upnY;
        this.m12 = dirY;
        this.m20 = leftZ;
        this.m21 = upnZ;
        this.m22 = dirZ;
        this.m30 = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ);
        this.m31 = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ);
        this.m32 = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ);
        this.properties = 16;
        return this;
    }

    public Matrix4x3f lookAt(Vector3f eye, Vector3f center, Vector3f up, Matrix4x3f dest) {
        return this.lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dest);
    }

    public Matrix4x3f lookAt(Vector3f eye, Vector3f center, Vector3f up) {
        return this.lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, this);
    }

    public Matrix4x3f lookAt(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ, Matrix4x3f dest) {
        return (this.properties & 4) != 0 ? dest.setLookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ) : this.lookAtGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dest);
    }

    private Matrix4x3f lookAtGeneric(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ, Matrix4x3f dest) {
        float dirX = eyeX - centerX;
        float dirY = eyeY - centerY;
        float dirZ = eyeZ - centerZ;
        float invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= invDirLength;
        dirY *= invDirLength;
        dirZ *= invDirLength;
        float leftX = upY * dirZ - upZ * dirY;
        float leftY = upZ * dirX - upX * dirZ;
        float leftZ = upX * dirY - upY * dirX;
        float invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLength;
        leftY *= invLeftLength;
        leftZ *= invLeftLength;
        float upnX = dirY * leftZ - dirZ * leftY;
        float upnY = dirZ * leftX - dirX * leftZ;
        float upnZ = dirX * leftY - dirY * leftX;
        float rm30 = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ);
        float rm31 = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ);
        float rm32 = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ);
        dest.m30 = this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30;
        dest.m31 = this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31;
        dest.m32 = this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32;
        float nm00 = this.m00 * leftX + this.m10 * upnX + this.m20 * dirX;
        float nm01 = this.m01 * leftX + this.m11 * upnX + this.m21 * dirX;
        float nm02 = this.m02 * leftX + this.m12 * upnX + this.m22 * dirX;
        float nm10 = this.m00 * leftY + this.m10 * upnY + this.m20 * dirY;
        float nm11 = this.m01 * leftY + this.m11 * upnY + this.m21 * dirY;
        float nm12 = this.m02 * leftY + this.m12 * upnY + this.m22 * dirY;
        dest.m20 = this.m00 * leftZ + this.m10 * upnZ + this.m20 * dirZ;
        dest.m21 = this.m01 * leftZ + this.m11 * upnZ + this.m21 * dirZ;
        dest.m22 = this.m02 * leftZ + this.m12 * upnZ + this.m22 * dirZ;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f lookAt(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        return this.lookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, this);
    }

    public Matrix4x3f setLookAtLH(Vector3f eye, Vector3f center, Vector3f up) {
        return this.setLookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z);
    }

    public Matrix4x3f setLookAtLH(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        float dirX = centerX - eyeX;
        float dirY = centerY - eyeY;
        float dirZ = centerZ - eyeZ;
        float invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= invDirLength;
        dirY *= invDirLength;
        dirZ *= invDirLength;
        float leftX = upY * dirZ - upZ * dirY;
        float leftY = upZ * dirX - upX * dirZ;
        float leftZ = upX * dirY - upY * dirX;
        float invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLength;
        leftY *= invLeftLength;
        leftZ *= invLeftLength;
        float upnX = dirY * leftZ - dirZ * leftY;
        float upnY = dirZ * leftX - dirX * leftZ;
        float upnZ = dirX * leftY - dirY * leftX;
        this.m00 = leftX;
        this.m01 = upnX;
        this.m02 = dirX;
        this.m10 = leftY;
        this.m11 = upnY;
        this.m12 = dirY;
        this.m20 = leftZ;
        this.m21 = upnZ;
        this.m22 = dirZ;
        this.m30 = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ);
        this.m31 = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ);
        this.m32 = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ);
        this.properties = 16;
        return this;
    }

    public Matrix4x3f lookAtLH(Vector3f eye, Vector3f center, Vector3f up, Matrix4x3f dest) {
        return this.lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dest);
    }

    public Matrix4x3f lookAtLH(Vector3f eye, Vector3f center, Vector3f up) {
        return this.lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, this);
    }

    public Matrix4x3f lookAtLH(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ, Matrix4x3f dest) {
        return (this.properties & 4) != 0 ? dest.setLookAtLH(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ) : this.lookAtLHGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dest);
    }

    private Matrix4x3f lookAtLHGeneric(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ, Matrix4x3f dest) {
        float dirX = centerX - eyeX;
        float dirY = centerY - eyeY;
        float dirZ = centerZ - eyeZ;
        float invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= invDirLength;
        dirY *= invDirLength;
        dirZ *= invDirLength;
        float leftX = upY * dirZ - upZ * dirY;
        float leftY = upZ * dirX - upX * dirZ;
        float leftZ = upX * dirY - upY * dirX;
        float invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLength;
        leftY *= invLeftLength;
        leftZ *= invLeftLength;
        float upnX = dirY * leftZ - dirZ * leftY;
        float upnY = dirZ * leftX - dirX * leftZ;
        float upnZ = dirX * leftY - dirY * leftX;
        float rm30 = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ);
        float rm31 = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ);
        float rm32 = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ);
        dest.m30 = this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30;
        dest.m31 = this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31;
        dest.m32 = this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32;
        float nm00 = this.m00 * leftX + this.m10 * upnX + this.m20 * dirX;
        float nm01 = this.m01 * leftX + this.m11 * upnX + this.m21 * dirX;
        float nm02 = this.m02 * leftX + this.m12 * upnX + this.m22 * dirX;
        float nm10 = this.m00 * leftY + this.m10 * upnY + this.m20 * dirY;
        float nm11 = this.m01 * leftY + this.m11 * upnY + this.m21 * dirY;
        float nm12 = this.m02 * leftY + this.m12 * upnY + this.m22 * dirY;
        dest.m20 = this.m00 * leftZ + this.m10 * upnZ + this.m20 * dirZ;
        dest.m21 = this.m01 * leftZ + this.m11 * upnZ + this.m21 * dirZ;
        dest.m22 = this.m02 * leftZ + this.m12 * upnZ + this.m22 * dirZ;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f lookAtLH(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        return this.lookAtLH(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, this);
    }

    public Matrix4x3f rotate(Quaternionf quat, Matrix4x3f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotation(quat);
        } else {
            return (this.properties & 8) != 0 ? this.rotateTranslation(quat, dest) : this.rotateGeneric(quat, dest);
        }
    }

    private Matrix4x3f rotateGeneric(Quaternionf quat, Matrix4x3f dest) {
        float w2 = quat.w * quat.w;
        float x2 = quat.x * quat.x;
        float y2 = quat.y * quat.y;
        float z2 = quat.z * quat.z;
        float zw = quat.z * quat.w;
        float dzw = zw + zw;
        float xy = quat.x * quat.y;
        float dxy = xy + xy;
        float xz = quat.x * quat.z;
        float dxz = xz + xz;
        float yw = quat.y * quat.w;
        float dyw = yw + yw;
        float yz = quat.y * quat.z;
        float dyz = yz + yz;
        float xw = quat.x * quat.w;
        float dxw = xw + xw;
        float rm00 = w2 + x2 - z2 - y2;
        float rm01 = dxy + dzw;
        float rm02 = dxz - dyw;
        float rm10 = dxy - dzw;
        float rm11 = y2 - z2 + w2 - x2;
        float rm12 = dyz + dxw;
        float rm20 = dyw + dxz;
        float rm21 = dyz - dxw;
        float rm22 = z2 - y2 - x2 + w2;
        float nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        float nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        float nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        float nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        float nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        float nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        dest.m20 = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22;
        dest.m21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22;
        dest.m22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f rotate(Quaternionf quat) {
        return this.rotate(quat, this);
    }

    public Matrix4x3f rotateTranslation(Quaternionf quat, Matrix4x3f dest) {
        float w2 = quat.w * quat.w;
        float x2 = quat.x * quat.x;
        float y2 = quat.y * quat.y;
        float z2 = quat.z * quat.z;
        float zw = quat.z * quat.w;
        float dzw = zw + zw;
        float xy = quat.x * quat.y;
        float dxy = xy + xy;
        float xz = quat.x * quat.z;
        float dxz = xz + xz;
        float yw = quat.y * quat.w;
        float dyw = yw + yw;
        float yz = quat.y * quat.z;
        float dyz = yz + yz;
        float xw = quat.x * quat.w;
        float dxw = xw + xw;
        float rm00 = w2 + x2 - z2 - y2;
        float rm01 = dxy + dzw;
        float rm02 = dxz - dyw;
        float rm10 = dxy - dzw;
        float rm11 = y2 - z2 + w2 - x2;
        float rm12 = dyz + dxw;
        float rm20 = dyw + dxz;
        float rm21 = dyz - dxw;
        float rm22 = z2 - y2 - x2 + w2;
        dest.m20 = rm20;
        dest.m21 = rm21;
        dest.m22 = rm22;
        dest.m00 = rm00;
        dest.m01 = rm01;
        dest.m02 = rm02;
        dest.m10 = rm10;
        dest.m11 = rm11;
        dest.m12 = rm12;
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f rotateLocal(Quaternionf quat, Matrix4x3f dest) {
        float w2 = quat.w * quat.w;
        float x2 = quat.x * quat.x;
        float y2 = quat.y * quat.y;
        float z2 = quat.z * quat.z;
        float zw = quat.z * quat.w;
        float xy = quat.x * quat.y;
        float xz = quat.x * quat.z;
        float yw = quat.y * quat.w;
        float yz = quat.y * quat.z;
        float xw = quat.x * quat.w;
        float lm00 = w2 + x2 - z2 - y2;
        float lm01 = xy + zw + zw + xy;
        float lm02 = xz - yw + xz - yw;
        float lm10 = -zw + xy - zw + xy;
        float lm11 = y2 - z2 + w2 - x2;
        float lm12 = yz + yz + xw + xw;
        float lm20 = yw + xz + xz + yw;
        float lm21 = yz + yz - xw - xw;
        float lm22 = z2 - y2 - x2 + w2;
        float nm00 = lm00 * this.m00 + lm10 * this.m01 + lm20 * this.m02;
        float nm01 = lm01 * this.m00 + lm11 * this.m01 + lm21 * this.m02;
        float nm02 = lm02 * this.m00 + lm12 * this.m01 + lm22 * this.m02;
        float nm10 = lm00 * this.m10 + lm10 * this.m11 + lm20 * this.m12;
        float nm11 = lm01 * this.m10 + lm11 * this.m11 + lm21 * this.m12;
        float nm12 = lm02 * this.m10 + lm12 * this.m11 + lm22 * this.m12;
        float nm20 = lm00 * this.m20 + lm10 * this.m21 + lm20 * this.m22;
        float nm21 = lm01 * this.m20 + lm11 * this.m21 + lm21 * this.m22;
        float nm22 = lm02 * this.m20 + lm12 * this.m21 + lm22 * this.m22;
        float nm30 = lm00 * this.m30 + lm10 * this.m31 + lm20 * this.m32;
        float nm31 = lm01 * this.m30 + lm11 * this.m31 + lm21 * this.m32;
        float nm32 = lm02 * this.m30 + lm12 * this.m31 + lm22 * this.m32;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        dest.m30 = nm30;
        dest.m31 = nm31;
        dest.m32 = nm32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f rotateLocal(Quaternionf quat) {
        return this.rotateLocal(quat, this);
    }

    public Matrix4x3f rotate(AxisAngle4f axisAngle) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Matrix4x3f rotate(AxisAngle4f axisAngle, Matrix4x3f dest) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dest);
    }

    public Matrix4x3f rotate(float angle, Vector3f axis) {
        return this.rotate(angle, axis.x, axis.y, axis.z);
    }

    public Matrix4x3f rotate(float angle, Vector3f axis, Matrix4x3f dest) {
        return this.rotate(angle, axis.x, axis.y, axis.z, dest);
    }

    public Matrix4x3f reflect(float a, float b, float c, float d, Matrix4x3f dest) {
        if ((this.properties & 4) != 0) {
            return dest.reflection(a, b, c, d);
        } else {
            float da = a + a;
            float db = b + b;
            float dc = c + c;
            float dd = d + d;
            float rm00 = 1f - da * a;
            float rm01 = -da * b;
            float rm02 = -da * c;
            float rm10 = -db * a;
            float rm11 = 1f - db * b;
            float rm12 = -db * c;
            float rm20 = -dc * a;
            float rm21 = -dc * b;
            float rm22 = 1f - dc * c;
            float rm30 = -dd * a;
            float rm31 = -dd * b;
            float rm32 = -dd * c;
            dest.m30 = this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30;
            dest.m31 = this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31;
            dest.m32 = this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32;
            float nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
            float nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
            float nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
            float nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
            float nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
            float nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
            dest.m20 = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22;
            dest.m21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22;
            dest.m22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22;
            dest.m00 = nm00;
            dest.m01 = nm01;
            dest.m02 = nm02;
            dest.m10 = nm10;
            dest.m11 = nm11;
            dest.m12 = nm12;
            dest.properties = this.properties & -13;
            return dest;
        }
    }

    public Matrix4x3f reflect(float a, float b, float c, float d) {
        return this.reflect(a, b, c, d, this);
    }

    public Matrix4x3f reflect(float nx, float ny, float nz, float px, float py, float pz) {
        return this.reflect(nx, ny, nz, px, py, pz, this);
    }

    public Matrix4x3f reflect(float nx, float ny, float nz, float px, float py, float pz, Matrix4x3f dest) {
        float invLength = Math.invsqrt(nx * nx + ny * ny + nz * nz);
        float nnx = nx * invLength;
        float nny = ny * invLength;
        float nnz = nz * invLength;
        return this.reflect(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz, dest);
    }

    public Matrix4x3f reflect(Vector3f normal, Vector3f point) {
        return this.reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z);
    }

    public Matrix4x3f reflect(Quaternionf orientation, Vector3f point) {
        return this.reflect(orientation, point, this);
    }

    public Matrix4x3f reflect(Quaternionf orientation, Vector3f point, Matrix4x3f dest) {
        double num1 = orientation.x + orientation.x;
        double num2 = orientation.y + orientation.y;
        double num3 = orientation.z + orientation.z;
        float normalX = (float) ((double) orientation.x * num3 + (double) orientation.w * num2);
        float normalY = (float) ((double) orientation.y * num3 - (double) orientation.w * num1);
        float normalZ = (float) (1.0 - ((double) orientation.x * num1 + (double) orientation.y * num2));
        return this.reflect(normalX, normalY, normalZ, point.x, point.y, point.z, dest);
    }

    public Matrix4x3f reflect(Vector3f normal, Vector3f point, Matrix4x3f dest) {
        return this.reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z, dest);
    }

    public Matrix4x3f reflection(float a, float b, float c, float d) {
        float da = a + a;
        float db = b + b;
        float dc = c + c;
        float dd = d + d;
        this.m00 = 1f - da * a;
        this.m01 = -da * b;
        this.m02 = -da * c;
        this.m10 = -db * a;
        this.m11 = 1f - db * b;
        this.m12 = -db * c;
        this.m20 = -dc * a;
        this.m21 = -dc * b;
        this.m22 = 1f - dc * c;
        this.m30 = -dd * a;
        this.m31 = -dd * b;
        this.m32 = -dd * c;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f reflection(float nx, float ny, float nz, float px, float py, float pz) {
        float invLength = Math.invsqrt(nx * nx + ny * ny + nz * nz);
        float nnx = nx * invLength;
        float nny = ny * invLength;
        float nnz = nz * invLength;
        return this.reflection(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz);
    }

    public Matrix4x3f reflection(Vector3f normal, Vector3f point) {
        return this.reflection(normal.x, normal.y, normal.z, point.x, point.y, point.z);
    }

    public Matrix4x3f reflection(Quaternionf orientation, Vector3f point) {
        double num1 = orientation.x + orientation.x;
        double num2 = orientation.y + orientation.y;
        double num3 = orientation.z + orientation.z;
        float normalX = (float) ((double) orientation.x * num3 + (double) orientation.w * num2);
        float normalY = (float) ((double) orientation.y * num3 - (double) orientation.w * num1);
        float normalZ = (float) (1.0 - ((double) orientation.x * num1 + (double) orientation.y * num2));
        return this.reflection(normalX, normalY, normalZ, point.x, point.y, point.z);
    }

    public Vector4f getRow(int row, Vector4f dest) throws IndexOutOfBoundsException {
        switch (row) {
            case 0:
                dest.x = this.m00;
                dest.y = this.m10;
                dest.z = this.m20;
                dest.w = this.m30;
                break;
            case 1:
                dest.x = this.m01;
                dest.y = this.m11;
                dest.z = this.m21;
                dest.w = this.m31;
                break;
            case 2:
                dest.x = this.m02;
                dest.y = this.m12;
                dest.z = this.m22;
                dest.w = this.m32;
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

        return dest;
    }

    public Matrix4x3f setRow(int row, Vector4f src) throws IndexOutOfBoundsException {
        switch (row) {
            case 0:
                this.m00 = src.x;
                this.m10 = src.y;
                this.m20 = src.z;
                this.m30 = src.w;
                break;
            case 1:
                this.m01 = src.x;
                this.m11 = src.y;
                this.m21 = src.z;
                this.m31 = src.w;
                break;
            case 2:
                this.m02 = src.x;
                this.m12 = src.y;
                this.m22 = src.z;
                this.m32 = src.w;
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

        this.properties = 0;
        return this;
    }

    public Vector3f getColumn(int column, Vector3f dest) throws IndexOutOfBoundsException {
        switch (column) {
            case 0:
                dest.x = this.m00;
                dest.y = this.m01;
                dest.z = this.m02;
                break;
            case 1:
                dest.x = this.m10;
                dest.y = this.m11;
                dest.z = this.m12;
                break;
            case 2:
                dest.x = this.m20;
                dest.y = this.m21;
                dest.z = this.m22;
                break;
            case 3:
                dest.x = this.m30;
                dest.y = this.m31;
                dest.z = this.m32;
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

        return dest;
    }

    public Matrix4x3f setColumn(int column, Vector3f src) throws IndexOutOfBoundsException {
        switch (column) {
            case 0:
                this.m00 = src.x;
                this.m01 = src.y;
                this.m02 = src.z;
                break;
            case 1:
                this.m10 = src.x;
                this.m11 = src.y;
                this.m12 = src.z;
                break;
            case 2:
                this.m20 = src.x;
                this.m21 = src.y;
                this.m22 = src.z;
                break;
            case 3:
                this.m30 = src.x;
                this.m31 = src.y;
                this.m32 = src.z;
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

        this.properties = 0;
        return this;
    }

    public Matrix4x3f normal() {
        return this.normal(this);
    }

    public Matrix4x3f normal(Matrix4x3f dest) {
        if ((this.properties & 4) != 0) {
            return dest.identity();
        } else {
            return (this.properties & 16) != 0 ? this.normalOrthonormal(dest) : this.normalGeneric(dest);
        }
    }

    private Matrix4x3f normalOrthonormal(Matrix4x3f dest) {
        if (dest != this) {
            dest.set(this);
        }

        return dest._properties(16);
    }

    private Matrix4x3f normalGeneric(Matrix4x3f dest) {
        float m00m11 = this.m00 * this.m11;
        float m01m10 = this.m01 * this.m10;
        float m02m10 = this.m02 * this.m10;
        float m00m12 = this.m00 * this.m12;
        float m01m12 = this.m01 * this.m12;
        float m02m11 = this.m02 * this.m11;
        float det = (m00m11 - m01m10) * this.m22 + (m02m10 - m00m12) * this.m21 + (m01m12 - m02m11) * this.m20;
        float s = 1f / det;
        float nm00 = (this.m11 * this.m22 - this.m21 * this.m12) * s;
        float nm01 = (this.m20 * this.m12 - this.m10 * this.m22) * s;
        float nm02 = (this.m10 * this.m21 - this.m20 * this.m11) * s;
        float nm10 = (this.m21 * this.m02 - this.m01 * this.m22) * s;
        float nm11 = (this.m00 * this.m22 - this.m20 * this.m02) * s;
        float nm12 = (this.m20 * this.m01 - this.m00 * this.m21) * s;
        float nm20 = (m01m12 - m02m11) * s;
        float nm21 = (m02m10 - m00m12) * s;
        float nm22 = (m00m11 - m01m10) * s;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        dest.m30 = 0f;
        dest.m31 = 0f;
        dest.m32 = 0f;
        dest.properties = this.properties & -9;
        return dest;
    }

    public Matrix3f normal(Matrix3f dest) {
        return (this.properties & 16) != 0 ? this.normalOrthonormal(dest) : this.normalGeneric(dest);
    }

    private Matrix3f normalOrthonormal(Matrix3f dest) {
        return dest.set(this);
    }

    private Matrix3f normalGeneric(Matrix3f dest) {
        float m00m11 = this.m00 * this.m11;
        float m01m10 = this.m01 * this.m10;
        float m02m10 = this.m02 * this.m10;
        float m00m12 = this.m00 * this.m12;
        float m01m12 = this.m01 * this.m12;
        float m02m11 = this.m02 * this.m11;
        float det = (m00m11 - m01m10) * this.m22 + (m02m10 - m00m12) * this.m21 + (m01m12 - m02m11) * this.m20;
        float s = 1f / det;
        dest.m00((this.m11 * this.m22 - this.m21 * this.m12) * s);
        dest.m01((this.m20 * this.m12 - this.m10 * this.m22) * s);
        dest.m02((this.m10 * this.m21 - this.m20 * this.m11) * s);
        dest.m10((this.m21 * this.m02 - this.m01 * this.m22) * s);
        dest.m11((this.m00 * this.m22 - this.m20 * this.m02) * s);
        dest.m12((this.m20 * this.m01 - this.m00 * this.m21) * s);
        dest.m20((m01m12 - m02m11) * s);
        dest.m21((m02m10 - m00m12) * s);
        dest.m22((m00m11 - m01m10) * s);
        return dest;
    }

    public Matrix4x3f cofactor3x3() {
        return this.cofactor3x3(this);
    }

    public Matrix3f cofactor3x3(Matrix3f dest) {
        dest.m00 = this.m11 * this.m22 - this.m21 * this.m12;
        dest.m01 = this.m20 * this.m12 - this.m10 * this.m22;
        dest.m02 = this.m10 * this.m21 - this.m20 * this.m11;
        dest.m10 = this.m21 * this.m02 - this.m01 * this.m22;
        dest.m11 = this.m00 * this.m22 - this.m20 * this.m02;
        dest.m12 = this.m20 * this.m01 - this.m00 * this.m21;
        dest.m20 = this.m01 * this.m12 - this.m02 * this.m11;
        dest.m21 = this.m02 * this.m10 - this.m00 * this.m12;
        dest.m22 = this.m00 * this.m11 - this.m01 * this.m10;
        return dest;
    }

    public Matrix4x3f cofactor3x3(Matrix4x3f dest) {
        float nm00 = this.m11 * this.m22 - this.m21 * this.m12;
        float nm01 = this.m20 * this.m12 - this.m10 * this.m22;
        float nm02 = this.m10 * this.m21 - this.m20 * this.m11;
        float nm10 = this.m21 * this.m02 - this.m01 * this.m22;
        float nm11 = this.m00 * this.m22 - this.m20 * this.m02;
        float nm12 = this.m20 * this.m01 - this.m00 * this.m21;
        float nm20 = this.m01 * this.m12 - this.m11 * this.m02;
        float nm21 = this.m02 * this.m10 - this.m12 * this.m00;
        float nm22 = this.m00 * this.m11 - this.m10 * this.m01;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        dest.m30 = 0f;
        dest.m31 = 0f;
        dest.m32 = 0f;
        dest.properties = this.properties & -9;
        return dest;
    }

    public Matrix4x3f normalize3x3() {
        return this.normalize3x3(this);
    }

    public Matrix4x3f normalize3x3(Matrix4x3f dest) {
        float invXlen = Math.invsqrt(this.m00 * this.m00 + this.m01 * this.m01 + this.m02 * this.m02);
        float invYlen = Math.invsqrt(this.m10 * this.m10 + this.m11 * this.m11 + this.m12 * this.m12);
        float invZlen = Math.invsqrt(this.m20 * this.m20 + this.m21 * this.m21 + this.m22 * this.m22);
        dest.m00 = this.m00 * invXlen;
        dest.m01 = this.m01 * invXlen;
        dest.m02 = this.m02 * invXlen;
        dest.m10 = this.m10 * invYlen;
        dest.m11 = this.m11 * invYlen;
        dest.m12 = this.m12 * invYlen;
        dest.m20 = this.m20 * invZlen;
        dest.m21 = this.m21 * invZlen;
        dest.m22 = this.m22 * invZlen;
        dest.properties = this.properties;
        return dest;
    }

    public Matrix3f normalize3x3(Matrix3f dest) {
        float invXlen = Math.invsqrt(this.m00 * this.m00 + this.m01 * this.m01 + this.m02 * this.m02);
        float invYlen = Math.invsqrt(this.m10 * this.m10 + this.m11 * this.m11 + this.m12 * this.m12);
        float invZlen = Math.invsqrt(this.m20 * this.m20 + this.m21 * this.m21 + this.m22 * this.m22);
        dest.m00(this.m00 * invXlen);
        dest.m01(this.m01 * invXlen);
        dest.m02(this.m02 * invXlen);
        dest.m10(this.m10 * invYlen);
        dest.m11(this.m11 * invYlen);
        dest.m12(this.m12 * invYlen);
        dest.m20(this.m20 * invZlen);
        dest.m21(this.m21 * invZlen);
        dest.m22(this.m22 * invZlen);
        return dest;
    }

    public Vector4f frustumPlane(int which, Vector4f dest) {
        switch (which) {
            case 0:
                dest.set(this.m00, this.m10, this.m20, 1f + this.m30).normalize();
                break;
            case 1:
                dest.set(-this.m00, -this.m10, -this.m20, 1f - this.m30).normalize();
                break;
            case 2:
                dest.set(this.m01, this.m11, this.m21, 1f + this.m31).normalize();
                break;
            case 3:
                dest.set(-this.m01, -this.m11, -this.m21, 1f - this.m31).normalize();
                break;
            case 4:
                dest.set(this.m02, this.m12, this.m22, 1f + this.m32).normalize();
                break;
            case 5:
                dest.set(-this.m02, -this.m12, -this.m22, 1f - this.m32).normalize();
                break;
            default:
                throw new IllegalArgumentException("which");
        }

        return dest;
    }

    public Vector3f positiveZ(Vector3f dir) {
        dir.x = this.m10 * this.m21 - this.m11 * this.m20;
        dir.y = this.m20 * this.m01 - this.m21 * this.m00;
        dir.z = this.m00 * this.m11 - this.m01 * this.m10;
        return dir.normalize(dir);
    }

    public Vector3f normalizedPositiveZ(Vector3f dir) {
        dir.x = this.m02;
        dir.y = this.m12;
        dir.z = this.m22;
        return dir;
    }

    public Vector3f positiveX(Vector3f dir) {
        dir.x = this.m11 * this.m22 - this.m12 * this.m21;
        dir.y = this.m02 * this.m21 - this.m01 * this.m22;
        dir.z = this.m01 * this.m12 - this.m02 * this.m11;
        return dir.normalize(dir);
    }

    public Vector3f normalizedPositiveX(Vector3f dir) {
        dir.x = this.m00;
        dir.y = this.m10;
        dir.z = this.m20;
        return dir;
    }

    public Vector3f positiveY(Vector3f dir) {
        dir.x = this.m12 * this.m20 - this.m10 * this.m22;
        dir.y = this.m00 * this.m22 - this.m02 * this.m20;
        dir.z = this.m02 * this.m10 - this.m00 * this.m12;
        return dir.normalize(dir);
    }

    public Vector3f normalizedPositiveY(Vector3f dir) {
        dir.x = this.m01;
        dir.y = this.m11;
        dir.z = this.m21;
        return dir;
    }

    public Vector3f origin(Vector3f origin) {
        float a = this.m00 * this.m11 - this.m01 * this.m10;
        float b = this.m00 * this.m12 - this.m02 * this.m10;
        float d = this.m01 * this.m12 - this.m02 * this.m11;
        float g = this.m20 * this.m31 - this.m21 * this.m30;
        float h = this.m20 * this.m32 - this.m22 * this.m30;
        float j = this.m21 * this.m32 - this.m22 * this.m31;
        origin.x = -this.m10 * j + this.m11 * h - this.m12 * g;
        origin.y = this.m00 * j - this.m01 * h + this.m02 * g;
        origin.z = -this.m30 * d + this.m31 * b - this.m32 * a;
        return origin;
    }

    public Matrix4x3f shadow(Vector4f light, float a, float b, float c, float d) {
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, this);
    }

    public Matrix4x3f shadow(Vector4f light, float a, float b, float c, float d, Matrix4x3f dest) {
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, dest);
    }

    public Matrix4x3f shadow(float lightX, float lightY, float lightZ, float lightW, float a, float b, float c, float d) {
        return this.shadow(lightX, lightY, lightZ, lightW, a, b, c, d, this);
    }

    public Matrix4x3f shadow(float lightX, float lightY, float lightZ, float lightW, float a, float b, float c, float d, Matrix4x3f dest) {
        float invPlaneLen = Math.invsqrt(a * a + b * b + c * c);
        float an = a * invPlaneLen;
        float bn = b * invPlaneLen;
        float cn = c * invPlaneLen;
        float dn = d * invPlaneLen;
        float dot = an * lightX + bn * lightY + cn * lightZ + dn * lightW;
        float rm00 = dot - an * lightX;
        float rm01 = -an * lightY;
        float rm02 = -an * lightZ;
        float rm03 = -an * lightW;
        float rm10 = -bn * lightX;
        float rm11 = dot - bn * lightY;
        float rm12 = -bn * lightZ;
        float rm13 = -bn * lightW;
        float rm20 = -cn * lightX;
        float rm21 = -cn * lightY;
        float rm22 = dot - cn * lightZ;
        float rm23 = -cn * lightW;
        float rm30 = -dn * lightX;
        float rm31 = -dn * lightY;
        float rm32 = -dn * lightZ;
        float rm33 = dot - dn * lightW;
        float nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02 + this.m30 * rm03;
        float nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02 + this.m31 * rm03;
        float nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02 + this.m32 * rm03;
        float nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12 + this.m30 * rm13;
        float nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12 + this.m31 * rm13;
        float nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12 + this.m32 * rm13;
        float nm20 = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22 + this.m30 * rm23;
        float nm21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22 + this.m31 * rm23;
        float nm22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22 + this.m32 * rm23;
        dest.m30 = this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30 * rm33;
        dest.m31 = this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31 * rm33;
        dest.m32 = this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32 * rm33;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        dest.properties = this.properties & -29;
        return dest;
    }

    public Matrix4x3f shadow(Vector4f light, Matrix4x3f planeTransform, Matrix4x3f dest) {
        float a = planeTransform.m10;
        float b = planeTransform.m11;
        float c = planeTransform.m12;
        float d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32;
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, dest);
    }

    public Matrix4x3f shadow(Vector4f light, Matrix4x3f planeTransform) {
        return this.shadow(light, planeTransform, this);
    }

    public Matrix4x3f shadow(float lightX, float lightY, float lightZ, float lightW, Matrix4x3f planeTransform, Matrix4x3f dest) {
        float a = planeTransform.m10;
        float b = planeTransform.m11;
        float c = planeTransform.m12;
        float d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32;
        return this.shadow(lightX, lightY, lightZ, lightW, a, b, c, d, dest);
    }

    public Matrix4x3f shadow(float lightX, float lightY, float lightZ, float lightW, Matrix4x3f planeTransform) {
        return this.shadow(lightX, lightY, lightZ, lightW, planeTransform, this);
    }

    public Matrix4x3f billboardCylindrical(Vector3f objPos, Vector3f targetPos, Vector3f up) {
        float dirX = targetPos.x - objPos.x;
        float dirY = targetPos.y - objPos.y;
        float dirZ = targetPos.z - objPos.z;
        float leftX = up.y * dirZ - up.z * dirY;
        float leftY = up.z * dirX - up.x * dirZ;
        float leftZ = up.x * dirY - up.y * dirX;
        float invLeftLen = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLen;
        leftY *= invLeftLen;
        leftZ *= invLeftLen;
        dirX = leftY * up.z - leftZ * up.y;
        dirY = leftZ * up.x - leftX * up.z;
        dirZ = leftX * up.y - leftY * up.x;
        float invDirLen = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= invDirLen;
        dirY *= invDirLen;
        dirZ *= invDirLen;
        this.m00 = leftX;
        this.m01 = leftY;
        this.m02 = leftZ;
        this.m10 = up.x;
        this.m11 = up.y;
        this.m12 = up.z;
        this.m20 = dirX;
        this.m21 = dirY;
        this.m22 = dirZ;
        this.m30 = objPos.x;
        this.m31 = objPos.y;
        this.m32 = objPos.z;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f billboardSpherical(Vector3f objPos, Vector3f targetPos, Vector3f up) {
        float dirX = targetPos.x - objPos.x;
        float dirY = targetPos.y - objPos.y;
        float dirZ = targetPos.z - objPos.z;
        float invDirLen = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= invDirLen;
        dirY *= invDirLen;
        dirZ *= invDirLen;
        float leftX = up.y * dirZ - up.z * dirY;
        float leftY = up.z * dirX - up.x * dirZ;
        float leftZ = up.x * dirY - up.y * dirX;
        float invLeftLen = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLen;
        leftY *= invLeftLen;
        leftZ *= invLeftLen;
        float upX = dirY * leftZ - dirZ * leftY;
        float upY = dirZ * leftX - dirX * leftZ;
        float upZ = dirX * leftY - dirY * leftX;
        this.m00 = leftX;
        this.m01 = leftY;
        this.m02 = leftZ;
        this.m10 = upX;
        this.m11 = upY;
        this.m12 = upZ;
        this.m20 = dirX;
        this.m21 = dirY;
        this.m22 = dirZ;
        this.m30 = objPos.x;
        this.m31 = objPos.y;
        this.m32 = objPos.z;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f billboardSpherical(Vector3f objPos, Vector3f targetPos) {
        float toDirX = targetPos.x - objPos.x;
        float toDirY = targetPos.y - objPos.y;
        float toDirZ = targetPos.z - objPos.z;
        float x = -toDirY;
        float w = Math.sqrt(toDirX * toDirX + toDirY * toDirY + toDirZ * toDirZ) + toDirZ;
        float invNorm = Math.invsqrt(x * x + toDirX * toDirX + w * w);
        x *= invNorm;
        float y = toDirX * invNorm;
        w *= invNorm;
        float q00 = (x + x) * x;
        float q11 = (y + y) * y;
        float q01 = (x + x) * y;
        float q03 = (x + x) * w;
        float q13 = (y + y) * w;
        this.m00 = 1f - q11;
        this.m01 = q01;
        this.m02 = -q13;
        this.m10 = q01;
        this.m11 = 1f - q00;
        this.m12 = q03;
        this.m20 = q13;
        this.m21 = -q03;
        this.m22 = 1f - q11 - q00;
        this.m30 = objPos.x;
        this.m31 = objPos.y;
        this.m32 = objPos.z;
        this.properties = 16;
        return this;
    }

    public int hashCode() {
        int result = 1;
        result = 31 * result + Float.floatToIntBits(this.m00);
        result = 31 * result + Float.floatToIntBits(this.m01);
        result = 31 * result + Float.floatToIntBits(this.m02);
        result = 31 * result + Float.floatToIntBits(this.m10);
        result = 31 * result + Float.floatToIntBits(this.m11);
        result = 31 * result + Float.floatToIntBits(this.m12);
        result = 31 * result + Float.floatToIntBits(this.m20);
        result = 31 * result + Float.floatToIntBits(this.m21);
        result = 31 * result + Float.floatToIntBits(this.m22);
        result = 31 * result + Float.floatToIntBits(this.m30);
        result = 31 * result + Float.floatToIntBits(this.m31);
        result = 31 * result + Float.floatToIntBits(this.m32);
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof Matrix4x3f)) {
            return false;
        } else {
            Matrix4x3f other = (Matrix4x3f) obj;
            if (Float.floatToIntBits(this.m00) != Float.floatToIntBits(other.m00)) {
                return false;
            } else if (Float.floatToIntBits(this.m01) != Float.floatToIntBits(other.m01)) {
                return false;
            } else if (Float.floatToIntBits(this.m02) != Float.floatToIntBits(other.m02)) {
                return false;
            } else if (Float.floatToIntBits(this.m10) != Float.floatToIntBits(other.m10)) {
                return false;
            } else if (Float.floatToIntBits(this.m11) != Float.floatToIntBits(other.m11)) {
                return false;
            } else if (Float.floatToIntBits(this.m12) != Float.floatToIntBits(other.m12)) {
                return false;
            } else if (Float.floatToIntBits(this.m20) != Float.floatToIntBits(other.m20)) {
                return false;
            } else if (Float.floatToIntBits(this.m21) != Float.floatToIntBits(other.m21)) {
                return false;
            } else if (Float.floatToIntBits(this.m22) != Float.floatToIntBits(other.m22)) {
                return false;
            } else if (Float.floatToIntBits(this.m30) != Float.floatToIntBits(other.m30)) {
                return false;
            } else if (Float.floatToIntBits(this.m31) != Float.floatToIntBits(other.m31)) {
                return false;
            } else {
                return Float.floatToIntBits(this.m32) == Float.floatToIntBits(other.m32);
            }
        }
    }

    public boolean equals(Matrix4x3f m, float delta) {
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
        } else if (!Runtime.equals(this.m22, m.m22, delta)) {
            return false;
        } else if (!Runtime.equals(this.m30, m.m30, delta)) {
            return false;
        } else if (!Runtime.equals(this.m31, m.m31, delta)) {
            return false;
        } else {
            return Runtime.equals(this.m32, m.m32, delta);
        }
    }

    public Matrix4x3f pick(float x, float y, float width, float height, int[] viewport, Matrix4x3f dest) {
        float sx = (float) viewport[2] / width;
        float sy = (float) viewport[3] / height;
        float tx = ((float) viewport[2] + 2f * ((float) viewport[0] - x)) / width;
        float ty = ((float) viewport[3] + 2f * ((float) viewport[1] - y)) / height;
        dest.m30 = this.m00 * tx + this.m10 * ty + this.m30;
        dest.m31 = this.m01 * tx + this.m11 * ty + this.m31;
        dest.m32 = this.m02 * tx + this.m12 * ty + this.m32;
        dest.m00 = this.m00 * sx;
        dest.m01 = this.m01 * sx;
        dest.m02 = this.m02 * sx;
        dest.m10 = this.m10 * sy;
        dest.m11 = this.m11 * sy;
        dest.m12 = this.m12 * sy;
        dest.properties = 0;
        return dest;
    }

    public Matrix4x3f pick(float x, float y, float width, float height, int[] viewport) {
        return this.pick(x, y, width, height, viewport, this);
    }

    public Matrix4x3f swap(Matrix4x3f other) {
        MemUtil.INSTANCE.swap(this, other);
        int props = this.properties;
        this.properties = other.properties;
        other.properties = props;
        return this;
    }

    public Matrix4x3f arcball(float radius, float centerX, float centerY, float centerZ, float angleX, float angleY, Matrix4x3f dest) {
        float m30 = this.m20 * -radius + this.m30;
        float m31 = this.m21 * -radius + this.m31;
        float m32 = this.m22 * -radius + this.m32;
        float sin = Math.sin(angleX);
        float cos = Math.cosFromSin(sin, angleX);
        float nm10 = this.m10 * cos + this.m20 * sin;
        float nm11 = this.m11 * cos + this.m21 * sin;
        float nm12 = this.m12 * cos + this.m22 * sin;
        float m20 = this.m20 * cos - this.m10 * sin;
        float m21 = this.m21 * cos - this.m11 * sin;
        float m22 = this.m22 * cos - this.m12 * sin;
        sin = Math.sin(angleY);
        cos = Math.cosFromSin(sin, angleY);
        float nm00 = this.m00 * cos - m20 * sin;
        float nm01 = this.m01 * cos - m21 * sin;
        float nm02 = this.m02 * cos - m22 * sin;
        float nm20 = this.m00 * sin + m20 * cos;
        float nm21 = this.m01 * sin + m21 * cos;
        float nm22 = this.m02 * sin + m22 * cos;
        dest.m30 = -nm00 * centerX - nm10 * centerY - nm20 * centerZ + m30;
        dest.m31 = -nm01 * centerX - nm11 * centerY - nm21 * centerZ + m31;
        dest.m32 = -nm02 * centerX - nm12 * centerY - nm22 * centerZ + m32;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f arcball(float radius, Vector3f center, float angleX, float angleY, Matrix4x3f dest) {
        return this.arcball(radius, center.x, center.y, center.z, angleX, angleY, dest);
    }

    public Matrix4x3f arcball(float radius, float centerX, float centerY, float centerZ, float angleX, float angleY) {
        return this.arcball(radius, centerX, centerY, centerZ, angleX, angleY, this);
    }

    public Matrix4x3f arcball(float radius, Vector3f center, float angleX, float angleY) {
        return this.arcball(radius, center.x, center.y, center.z, angleX, angleY, this);
    }

    public Matrix4x3f transformAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Vector3f outMin, Vector3f outMax) {
        float xax = this.m00 * minX;
        float xay = this.m01 * minX;
        float xaz = this.m02 * minX;
        float xbx = this.m00 * maxX;
        float xby = this.m01 * maxX;
        float xbz = this.m02 * maxX;
        float yax = this.m10 * minY;
        float yay = this.m11 * minY;
        float yaz = this.m12 * minY;
        float ybx = this.m10 * maxY;
        float yby = this.m11 * maxY;
        float ybz = this.m12 * maxY;
        float zax = this.m20 * minZ;
        float zay = this.m21 * minZ;
        float zaz = this.m22 * minZ;
        float zbx = this.m20 * maxZ;
        float zby = this.m21 * maxZ;
        float zbz = this.m22 * maxZ;
        float xminx;
        float xmaxx;
        if (xax < xbx) {
            xminx = xax;
            xmaxx = xbx;
        } else {
            xminx = xbx;
            xmaxx = xax;
        }

        float xminy;
        float xmaxy;
        if (xay < xby) {
            xminy = xay;
            xmaxy = xby;
        } else {
            xminy = xby;
            xmaxy = xay;
        }

        float xminz;
        float xmaxz;
        if (xaz < xbz) {
            xminz = xaz;
            xmaxz = xbz;
        } else {
            xminz = xbz;
            xmaxz = xaz;
        }

        float yminx;
        float ymaxx;
        if (yax < ybx) {
            yminx = yax;
            ymaxx = ybx;
        } else {
            yminx = ybx;
            ymaxx = yax;
        }

        float yminy;
        float ymaxy;
        if (yay < yby) {
            yminy = yay;
            ymaxy = yby;
        } else {
            yminy = yby;
            ymaxy = yay;
        }

        float yminz;
        float ymaxz;
        if (yaz < ybz) {
            yminz = yaz;
            ymaxz = ybz;
        } else {
            yminz = ybz;
            ymaxz = yaz;
        }

        float zminx;
        float zmaxx;
        if (zax < zbx) {
            zminx = zax;
            zmaxx = zbx;
        } else {
            zminx = zbx;
            zmaxx = zax;
        }

        float zminy;
        float zmaxy;
        if (zay < zby) {
            zminy = zay;
            zmaxy = zby;
        } else {
            zminy = zby;
            zmaxy = zay;
        }

        float zminz;
        float zmaxz;
        if (zaz < zbz) {
            zminz = zaz;
            zmaxz = zbz;
        } else {
            zminz = zbz;
            zmaxz = zaz;
        }

        outMin.x = xminx + yminx + zminx + this.m30;
        outMin.y = xminy + yminy + zminy + this.m31;
        outMin.z = xminz + yminz + zminz + this.m32;
        outMax.x = xmaxx + ymaxx + zmaxx + this.m30;
        outMax.y = xmaxy + ymaxy + zmaxy + this.m31;
        outMax.z = xmaxz + ymaxz + zmaxz + this.m32;
        return this;
    }

    public Matrix4x3f transformAab(Vector3f min, Vector3f max, Vector3f outMin, Vector3f outMax) {
        return this.transformAab(min.x, min.y, min.z, max.x, max.y, max.z, outMin, outMax);
    }

    public Matrix4x3f lerp(Matrix4x3f other, float t) {
        return this.lerp(other, t, this);
    }

    public Matrix4x3f lerp(Matrix4x3f other, float t, Matrix4x3f dest) {
        dest.m00 = Math.fma(other.m00 - this.m00, t, this.m00);
        dest.m01 = Math.fma(other.m01 - this.m01, t, this.m01);
        dest.m02 = Math.fma(other.m02 - this.m02, t, this.m02);
        dest.m10 = Math.fma(other.m10 - this.m10, t, this.m10);
        dest.m11 = Math.fma(other.m11 - this.m11, t, this.m11);
        dest.m12 = Math.fma(other.m12 - this.m12, t, this.m12);
        dest.m20 = Math.fma(other.m20 - this.m20, t, this.m20);
        dest.m21 = Math.fma(other.m21 - this.m21, t, this.m21);
        dest.m22 = Math.fma(other.m22 - this.m22, t, this.m22);
        dest.m30 = Math.fma(other.m30 - this.m30, t, this.m30);
        dest.m31 = Math.fma(other.m31 - this.m31, t, this.m31);
        dest.m32 = Math.fma(other.m32 - this.m32, t, this.m32);
        dest.properties = this.properties & other.properties();
        return dest;
    }

    public Matrix4x3f rotateTowards(Vector3f dir, Vector3f up, Matrix4x3f dest) {
        return this.rotateTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z, dest);
    }

    public Matrix4x3f rotateTowards(Vector3f dir, Vector3f up) {
        return this.rotateTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z, this);
    }

    public Matrix4x3f rotateTowards(float dirX, float dirY, float dirZ, float upX, float upY, float upZ) {
        return this.rotateTowards(dirX, dirY, dirZ, upX, upY, upZ, this);
    }

    public Matrix4x3f rotateTowards(float dirX, float dirY, float dirZ, float upX, float upY, float upZ, Matrix4x3f dest) {
        float invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        float ndirX = dirX * invDirLength;
        float ndirY = dirY * invDirLength;
        float ndirZ = dirZ * invDirLength;
        float leftX = upY * ndirZ - upZ * ndirY;
        float leftY = upZ * ndirX - upX * ndirZ;
        float leftZ = upX * ndirY - upY * ndirX;
        float invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLength;
        leftY *= invLeftLength;
        leftZ *= invLeftLength;
        float upnX = ndirY * leftZ - ndirZ * leftY;
        float upnY = ndirZ * leftX - ndirX * leftZ;
        float upnZ = ndirX * leftY - ndirY * leftX;
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        float nm00 = this.m00 * leftX + this.m10 * leftY + this.m20 * leftZ;
        float nm01 = this.m01 * leftX + this.m11 * leftY + this.m21 * leftZ;
        float nm02 = this.m02 * leftX + this.m12 * leftY + this.m22 * leftZ;
        float nm10 = this.m00 * upnX + this.m10 * upnY + this.m20 * upnZ;
        float nm11 = this.m01 * upnX + this.m11 * upnY + this.m21 * upnZ;
        float nm12 = this.m02 * upnX + this.m12 * upnY + this.m22 * upnZ;
        dest.m20 = this.m00 * ndirX + this.m10 * ndirY + this.m20 * ndirZ;
        dest.m21 = this.m01 * ndirX + this.m11 * ndirY + this.m21 * ndirZ;
        dest.m22 = this.m02 * ndirX + this.m12 * ndirY + this.m22 * ndirZ;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f rotationTowards(Vector3f dir, Vector3f up) {
        return this.rotationTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    public Matrix4x3f rotationTowards(float dirX, float dirY, float dirZ, float upX, float upY, float upZ) {
        float invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        float ndirX = dirX * invDirLength;
        float ndirY = dirY * invDirLength;
        float ndirZ = dirZ * invDirLength;
        float leftX = upY * ndirZ - upZ * ndirY;
        float leftY = upZ * ndirX - upX * ndirZ;
        float leftZ = upX * ndirY - upY * ndirX;
        float invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLength;
        leftY *= invLeftLength;
        leftZ *= invLeftLength;
        float upnX = ndirY * leftZ - ndirZ * leftY;
        float upnY = ndirZ * leftX - ndirX * leftZ;
        float upnZ = ndirX * leftY - ndirY * leftX;
        this.m00 = leftX;
        this.m01 = leftY;
        this.m02 = leftZ;
        this.m10 = upnX;
        this.m11 = upnY;
        this.m12 = upnZ;
        this.m20 = ndirX;
        this.m21 = ndirY;
        this.m22 = ndirZ;
        this.m30 = 0f;
        this.m31 = 0f;
        this.m32 = 0f;
        this.properties = 16;
        return this;
    }

    public Matrix4x3f translationRotateTowards(Vector3f pos, Vector3f dir, Vector3f up) {
        return this.translationRotateTowards(pos.x, pos.y, pos.z, dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    public Matrix4x3f translationRotateTowards(float posX, float posY, float posZ, float dirX, float dirY, float dirZ, float upX, float upY, float upZ) {
        float invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        float ndirX = dirX * invDirLength;
        float ndirY = dirY * invDirLength;
        float ndirZ = dirZ * invDirLength;
        float leftX = upY * ndirZ - upZ * ndirY;
        float leftY = upZ * ndirX - upX * ndirZ;
        float leftZ = upX * ndirY - upY * ndirX;
        float invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLength;
        leftY *= invLeftLength;
        leftZ *= invLeftLength;
        float upnX = ndirY * leftZ - ndirZ * leftY;
        float upnY = ndirZ * leftX - ndirX * leftZ;
        float upnZ = ndirX * leftY - ndirY * leftX;
        this.m00 = leftX;
        this.m01 = leftY;
        this.m02 = leftZ;
        this.m10 = upnX;
        this.m11 = upnY;
        this.m12 = upnZ;
        this.m20 = ndirX;
        this.m21 = ndirY;
        this.m22 = ndirZ;
        this.m30 = posX;
        this.m31 = posY;
        this.m32 = posZ;
        this.properties = 16;
        return this;
    }

    public Vector3f getEulerAnglesZYX(Vector3f dest) {
        dest.x = Math.atan2(this.m12, this.m22);
        dest.y = Math.atan2(-this.m02, Math.sqrt(1f - this.m02 * this.m02));
        dest.z = Math.atan2(this.m01, this.m00);
        return dest;
    }

    public Vector3f getEulerAnglesXYZ(Vector3f dest) {
        dest.x = Math.atan2(-this.m21, this.m22);
        dest.y = Math.atan2(this.m20, Math.sqrt(1f - this.m20 * this.m20));
        dest.z = Math.atan2(-this.m10, this.m00);
        return dest;
    }

    public Matrix4x3f obliqueZ(float a, float b) {
        this.m20 += this.m00 * a + this.m10 * b;
        this.m21 += this.m01 * a + this.m11 * b;
        this.m22 += this.m02 * a + this.m12 * b;
        this.properties = 0;
        return this;
    }

    public Matrix4x3f obliqueZ(float a, float b, Matrix4x3f dest) {
        dest.m00 = this.m00;
        dest.m01 = this.m01;
        dest.m02 = this.m02;
        dest.m10 = this.m10;
        dest.m11 = this.m11;
        dest.m12 = this.m12;
        dest.m20 = this.m00 * a + this.m10 * b + this.m20;
        dest.m21 = this.m01 * a + this.m11 * b + this.m21;
        dest.m22 = this.m02 * a + this.m12 * b + this.m22;
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = 0;
        return dest;
    }

    public Matrix4x3f withLookAtUp(Vector3f up) {
        return this.withLookAtUp(up.x, up.y, up.z, this);
    }

    public Matrix4x3f withLookAtUp(Vector3f up, Matrix4x3f dest) {
        return this.withLookAtUp(up.x, up.y, up.z);
    }

    public Matrix4x3f withLookAtUp(float upX, float upY, float upZ) {
        return this.withLookAtUp(upX, upY, upZ, this);
    }

    public Matrix4x3f withLookAtUp(float upX, float upY, float upZ, Matrix4x3f dest) {
        float y = (upY * this.m21 - upZ * this.m11) * this.m02 + (upZ * this.m01 - upX * this.m21) * this.m12 + (upX * this.m11 - upY * this.m01) * this.m22;
        float x = upX * this.m01 + upY * this.m11 + upZ * this.m21;
        if ((this.properties & 16) == 0) {
            x *= Math.sqrt(this.m01 * this.m01 + this.m11 * this.m11 + this.m21 * this.m21);
        }

        float invsqrt = Math.invsqrt(y * y + x * x);
        float c = x * invsqrt;
        float s = y * invsqrt;
        float nm00 = c * this.m00 - s * this.m01;
        float nm10 = c * this.m10 - s * this.m11;
        float nm20 = c * this.m20 - s * this.m21;
        float nm31 = s * this.m30 + c * this.m31;
        float nm01 = s * this.m00 + c * this.m01;
        float nm11 = s * this.m10 + c * this.m11;
        float nm21 = s * this.m20 + c * this.m21;
        float nm30 = c * this.m30 - s * this.m31;
        dest._m00(nm00)._m10(nm10)._m20(nm20)._m30(nm30)._m01(nm01)._m11(nm11)._m21(nm21)._m31(nm31);
        if (dest != this) {
            dest._m02(this.m02)._m12(this.m12)._m22(this.m22)._m32(this.m32);
        }

        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3f mapXZY() {
        return this.mapXZY(this);
    }

    public Matrix4x3f mapXZY(Matrix4x3f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapXZnY() {
        return this.mapXZnY(this);
    }

    public Matrix4x3f mapXZnY(Matrix4x3f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapXnYnZ() {
        return this.mapXnYnZ(this);
    }

    public Matrix4x3f mapXnYnZ(Matrix4x3f dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapXnZY() {
        return this.mapXnZY(this);
    }

    public Matrix4x3f mapXnZY(Matrix4x3f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapXnZnY() {
        return this.mapXnZnY(this);
    }

    public Matrix4x3f mapXnZnY(Matrix4x3f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapYXZ() {
        return this.mapYXZ(this);
    }

    public Matrix4x3f mapYXZ(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(m00)._m11(m01)._m12(m02)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapYXnZ() {
        return this.mapYXnZ(this);
    }

    public Matrix4x3f mapYXnZ(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(m00)._m11(m01)._m12(m02)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapYZX() {
        return this.mapYZX(this);
    }

    public Matrix4x3f mapYZX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapYZnX() {
        return this.mapYZnX(this);
    }

    public Matrix4x3f mapYZnX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapYnXZ() {
        return this.mapYnXZ(this);
    }

    public Matrix4x3f mapYnXZ(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapYnXnZ() {
        return this.mapYnXnZ(this);
    }

    public Matrix4x3f mapYnXnZ(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapYnZX() {
        return this.mapYnZX(this);
    }

    public Matrix4x3f mapYnZX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapYnZnX() {
        return this.mapYnZnX(this);
    }

    public Matrix4x3f mapYnZnX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapZXY() {
        return this.mapZXY(this);
    }

    public Matrix4x3f mapZXY(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(m00)._m11(m01)._m12(m02)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapZXnY() {
        return this.mapZXnY(this);
    }

    public Matrix4x3f mapZXnY(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(m00)._m11(m01)._m12(m02)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapZYX() {
        return this.mapZYX(this);
    }

    public Matrix4x3f mapZYX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapZYnX() {
        return this.mapZYnX(this);
    }

    public Matrix4x3f mapZYnX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapZnXY() {
        return this.mapZnXY(this);
    }

    public Matrix4x3f mapZnXY(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapZnXnY() {
        return this.mapZnXnY(this);
    }

    public Matrix4x3f mapZnXnY(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapZnYX() {
        return this.mapZnYX(this);
    }

    public Matrix4x3f mapZnYX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapZnYnX() {
        return this.mapZnYnX(this);
    }

    public Matrix4x3f mapZnYnX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnXYnZ() {
        return this.mapnXYnZ(this);
    }

    public Matrix4x3f mapnXYnZ(Matrix4x3f dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnXZY() {
        return this.mapnXZY(this);
    }

    public Matrix4x3f mapnXZY(Matrix4x3f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnXZnY() {
        return this.mapnXZnY(this);
    }

    public Matrix4x3f mapnXZnY(Matrix4x3f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnXnYZ() {
        return this.mapnXnYZ(this);
    }

    public Matrix4x3f mapnXnYZ(Matrix4x3f dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnXnYnZ() {
        return this.mapnXnYnZ(this);
    }

    public Matrix4x3f mapnXnYnZ(Matrix4x3f dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnXnZY() {
        return this.mapnXnZY(this);
    }

    public Matrix4x3f mapnXnZY(Matrix4x3f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnXnZnY() {
        return this.mapnXnZnY(this);
    }

    public Matrix4x3f mapnXnZnY(Matrix4x3f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnYXZ() {
        return this.mapnYXZ(this);
    }

    public Matrix4x3f mapnYXZ(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(m00)._m11(m01)._m12(m02)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnYXnZ() {
        return this.mapnYXnZ(this);
    }

    public Matrix4x3f mapnYXnZ(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(m00)._m11(m01)._m12(m02)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnYZX() {
        return this.mapnYZX(this);
    }

    public Matrix4x3f mapnYZX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnYZnX() {
        return this.mapnYZnX(this);
    }

    public Matrix4x3f mapnYZnX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnYnXZ() {
        return this.mapnYnXZ(this);
    }

    public Matrix4x3f mapnYnXZ(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnYnXnZ() {
        return this.mapnYnXnZ(this);
    }

    public Matrix4x3f mapnYnXnZ(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnYnZX() {
        return this.mapnYnZX(this);
    }

    public Matrix4x3f mapnYnZX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnYnZnX() {
        return this.mapnYnZnX(this);
    }

    public Matrix4x3f mapnYnZnX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnZXY() {
        return this.mapnZXY(this);
    }

    public Matrix4x3f mapnZXY(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(m00)._m11(m01)._m12(m02)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnZXnY() {
        return this.mapnZXnY(this);
    }

    public Matrix4x3f mapnZXnY(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(m00)._m11(m01)._m12(m02)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnZYX() {
        return this.mapnZYX(this);
    }

    public Matrix4x3f mapnZYX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnZYnX() {
        return this.mapnZYnX(this);
    }

    public Matrix4x3f mapnZYnX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnZnXY() {
        return this.mapnZnXY(this);
    }

    public Matrix4x3f mapnZnXY(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnZnXnY() {
        return this.mapnZnXnY(this);
    }

    public Matrix4x3f mapnZnXnY(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnZnYX() {
        return this.mapnZnYX(this);
    }

    public Matrix4x3f mapnZnYX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f mapnZnYnX() {
        return this.mapnZnYnX(this);
    }

    public Matrix4x3f mapnZnYnX(Matrix4x3f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f negateX() {
        return this._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._properties(this.properties & 16);
    }

    public Matrix4x3f negateX(Matrix4x3f dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f negateY() {
        return this._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._properties(this.properties & 16);
    }

    public Matrix4x3f negateY(Matrix4x3f dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3f negateZ() {
        return this._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._properties(this.properties & 16);
    }

    public Matrix4x3f negateZ(Matrix4x3f dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public boolean isFinite() {
        return Math.isFinite(this.m00) && Math.isFinite(this.m01) && Math.isFinite(this.m02) && Math.isFinite(this.m10) && Math.isFinite(this.m11) && Math.isFinite(this.m12) && Math.isFinite(this.m20) && Math.isFinite(this.m21) && Math.isFinite(this.m22) && Math.isFinite(this.m30) && Math.isFinite(this.m31) && Math.isFinite(this.m32);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
