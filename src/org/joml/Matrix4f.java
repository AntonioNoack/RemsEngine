package org.joml;

import java.nio.FloatBuffer;
import java.text.NumberFormat;

@SuppressWarnings("unused")
public class Matrix4f {

    public static final int PROPERTY_IDENTITY = 4;

    public float m00;
    public float m01;
    public float m02;
    public float m03;
    public float m10;
    public float m11;
    public float m12;
    public float m13;
    public float m20;
    public float m21;
    public float m22;
    public float m23;
    public float m30;
    public float m31;
    public float m32;
    public float m33;
    int properties;

    public Matrix4f() {
        this._m00(1f)._m11(1f)._m22(1f)._m33(1f)._properties(30);
    }

    public Matrix4f(Matrix3f mat) {
        this.set(mat);
    }

    public Matrix4f(Matrix4f mat) {
        this.set(mat);
    }

    public Matrix4f(Matrix4x3f mat) {
        this.set(mat);
    }

    public Matrix4f(Matrix4d mat) {
        this.set(mat);
    }

    public Matrix4f(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23, float m30, float m31, float m32, float m33) {
        this._m00(m00)._m01(m01)._m02(m02)._m03(m03)._m10(m10)._m11(m11)._m12(m12)._m13(m13)._m20(m20)._m21(m21)._m22(m22)._m23(m23)._m30(m30)._m31(m31)._m32(m32)._m33(m33).determineProperties();
    }

    public Matrix4f(Vector4f col0, Vector4f col1, Vector4f col2, Vector4f col3) {
        this.set(col0, col1, col2, col3);
    }

    Matrix4f _properties(int properties) {
        this.properties = properties;
        return this;
    }

    public Matrix4f assume(int properties) {
        this._properties(properties);
        return this;
    }

    public Matrix4f determineProperties() {
        int properties = 0;
        if (this.m03 == 0f && this.m13 == 0f) {
            if (this.m23 == 0f && this.m33 == 1f) {
                properties |= 2;
                if (this.m00 == 1f && this.m01 == 0f && this.m02 == 0f && this.m10 == 0f && this.m11 == 1f && this.m12 == 0f && this.m20 == 0f && this.m21 == 0f && this.m22 == 1f) {
                    properties |= 24;
                    if (this.m30 == 0f && this.m31 == 0f && this.m32 == 0f) {
                        properties |= 4;
                    }
                }
            } else if (this.m01 == 0f && this.m02 == 0f && this.m10 == 0f && this.m12 == 0f && this.m20 == 0f && this.m21 == 0f && this.m30 == 0f && this.m31 == 0f && this.m33 == 0f) {
                properties |= 1;
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

    public float m03() {
        return this.m03;
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

    public float m13() {
        return this.m13;
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

    public float m23() {
        return this.m23;
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

    public float m33() {
        return this.m33;
    }

    public Matrix4f m00(float m00) {
        this.m00 = m00;
        this.properties &= -17;
        if (m00 != 1f) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4f m01(float m01) {
        this.m01 = m01;
        this.properties &= -17;
        if (m01 != 0f) {
            this.properties &= -14;
        }

        return this;
    }

    public Matrix4f m02(float m02) {
        this.m02 = m02;
        this.properties &= -17;
        if (m02 != 0f) {
            this.properties &= -14;
        }

        return this;
    }

    public Matrix4f m03(float m03) {
        this.m03 = m03;
        if (m03 != 0f) {
            this.properties = 0;
        }

        return this;
    }

    public Matrix4f m10(float m10) {
        this.m10 = m10;
        this.properties &= -17;
        if (m10 != 0f) {
            this.properties &= -14;
        }

        return this;
    }

    public Matrix4f m11(float m11) {
        this.m11 = m11;
        this.properties &= -17;
        if (m11 != 1f) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4f m12(float m12) {
        this.m12 = m12;
        this.properties &= -17;
        if (m12 != 0f) {
            this.properties &= -14;
        }

        return this;
    }

    public Matrix4f m13(float m13) {
        this.m13 = m13;
        if (m13 != 0f) {
            this.properties = 0;
        }

        return this;
    }

    public Matrix4f m20(float m20) {
        this.m20 = m20;
        this.properties &= -17;
        if (m20 != 0f) {
            this.properties &= -14;
        }

        return this;
    }

    public Matrix4f m21(float m21) {
        this.m21 = m21;
        this.properties &= -17;
        if (m21 != 0f) {
            this.properties &= -14;
        }

        return this;
    }

    public Matrix4f m22(float m22) {
        this.m22 = m22;
        this.properties &= -17;
        if (m22 != 1f) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4f m23(float m23) {
        this.m23 = m23;
        if (m23 != 0f) {
            this.properties &= -31;
        }

        return this;
    }

    public Matrix4f m30(float m30) {
        this.m30 = m30;
        if (m30 != 0f) {
            this.properties &= -6;
        }

        return this;
    }

    public Matrix4f m31(float m31) {
        this.m31 = m31;
        if (m31 != 0f) {
            this.properties &= -6;
        }

        return this;
    }

    public Matrix4f m32(float m32) {
        this.m32 = m32;
        if (m32 != 0f) {
            this.properties &= -6;
        }

        return this;
    }

    public Matrix4f m33(float m33) {
        this.m33 = m33;
        if (m33 != 0f) {
            this.properties &= -2;
        }

        if (m33 != 1f) {
            this.properties &= -31;
        }

        return this;
    }

    Matrix4f _m00(float m00) {
        this.m00 = m00;
        return this;
    }

    Matrix4f _m01(float m01) {
        this.m01 = m01;
        return this;
    }

    Matrix4f _m02(float m02) {
        this.m02 = m02;
        return this;
    }

    Matrix4f _m03(float m03) {
        this.m03 = m03;
        return this;
    }

    Matrix4f _m10(float m10) {
        this.m10 = m10;
        return this;
    }

    Matrix4f _m11(float m11) {
        this.m11 = m11;
        return this;
    }

    Matrix4f _m12(float m12) {
        this.m12 = m12;
        return this;
    }

    Matrix4f _m13(float m13) {
        this.m13 = m13;
        return this;
    }

    Matrix4f _m20(float m20) {
        this.m20 = m20;
        return this;
    }

    Matrix4f _m21(float m21) {
        this.m21 = m21;
        return this;
    }

    Matrix4f _m22(float m22) {
        this.m22 = m22;
        return this;
    }

    Matrix4f _m23(float m23) {
        this.m23 = m23;
        return this;
    }

    Matrix4f _m30(float m30) {
        this.m30 = m30;
        return this;
    }

    Matrix4f _m31(float m31) {
        this.m31 = m31;
        return this;
    }

    Matrix4f _m32(float m32) {
        this.m32 = m32;
        return this;
    }

    Matrix4f _m33(float m33) {
        this.m33 = m33;
        return this;
    }

    public Matrix4f identity() {
        return (this.properties & 4) != 0 ? this : this._m00(1f)._m01(0f)._m02(0f)._m03(0f)._m10(0f)._m11(1f)._m12(0f)._m13(0f)._m20(0f)._m21(0f)._m22(1f)._m23(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)._properties(30);
    }

    public FloatBuffer putInto(FloatBuffer arr) {
        arr.put(m00).put(m01).put(m02).put(m03);
        arr.put(m10).put(m11).put(m12).put(m13);
        arr.put(m20).put(m21).put(m22).put(m23);
        arr.put(m30).put(m31).put(m32).put(m33);
        return arr;
    }

    public Matrix4f set(Matrix4f m) {
        return this._m00(m.m00)._m01(m.m01)._m02(m.m02)._m03(m.m03)._m10(m.m10)._m11(m.m11)._m12(m.m12)._m13(m.m13)._m20(m.m20)._m21(m.m21)._m22(m.m22)._m23(m.m23)._m30(m.m30)._m31(m.m31)._m32(m.m32)._m33(m.m33)._properties(m.properties());
    }

    public Matrix4f setTransposed(Matrix4f m) {
        return (m.properties() & 4) != 0 ? this.identity() : this.setTransposedInternal(m);
    }

    private Matrix4f setTransposedInternal(Matrix4f m) {
        float nm10 = m.m01;
        float nm12 = m.m21;
        float nm13 = m.m31;
        float nm20 = m.m02;
        float nm21 = m.m12;
        float nm30 = m.m03;
        float nm31 = m.m13;
        float nm32 = m.m23;
        return this._m00(m.m00)._m01(m.m10)._m02(m.m20)._m03(m.m30)._m10(nm10)._m11(m.m11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(m.m22)._m23(m.m32)._m30(nm30)._m31(nm31)._m32(nm32)._m33(m.m33)._properties(m.properties() & 4);
    }

    public Matrix4f set(Matrix4x3f m) {
        return this._m00(m.m00)._m01(m.m01)._m02(m.m02)._m03(0f)._m10(m.m10)._m11(m.m11)._m12(m.m12)._m13(0f)._m20(m.m20)._m21(m.m21)._m22(m.m22)._m23(0f)._m30(m.m30)._m31(m.m31)._m32(m.m32)._m33(1f)._properties(m.properties() | 2);
    }

    public Matrix4f set(Matrix4d m) {
        return this._m00((float)m.m00)._m01((float)m.m01)._m02((float)m.m02)._m03((float)m.m03)._m10((float)m.m10)._m11((float)m.m11)._m12((float)m.m12)._m13((float)m.m13)._m20((float)m.m20)._m21((float)m.m21)._m22((float)m.m22)._m23((float)m.m23)._m30((float)m.m30)._m31((float)m.m31)._m32((float)m.m32)._m33((float)m.m33)._properties(m.properties());
    }

    public Matrix4f set(Matrix3f mat) {
        return this._m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m03(0f)._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)._m13(0f)._m20(mat.m20)._m21(mat.m21)._m22(mat.m22)._m23(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)._properties(2);
    }

    public Matrix4f set(AxisAngle4f axisAngle) {
        float x = axisAngle.x;
        float y = axisAngle.y;
        float z = axisAngle.z;
        float angle = axisAngle.angle;
        double n = (double)Math.sqrt(x * x + y * y + z * z);
        n = 1.0 / n;
        x = (float)((double)x * n);
        y = (float)((double)y * n);
        z = (float)((double)z * n);
        float s = Math.sin(angle);
        float c = Math.cosFromSin(s, angle);
        float omc = 1f - c;
        this._m00(c + x * x * omc)._m11(c + y * y * omc)._m22(c + z * z * omc);
        float tmp1 = x * y * omc;
        float tmp2 = z * s;
        this._m10(tmp1 - tmp2)._m01(tmp1 + tmp2);
        tmp1 = x * z * omc;
        tmp2 = y * s;
        this._m20(tmp1 + tmp2)._m02(tmp1 - tmp2);
        tmp1 = y * z * omc;
        tmp2 = x * s;
        return this._m21(tmp1 - tmp2)._m12(tmp1 + tmp2)._m03(0f)._m13(0f)._m23(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)._properties(18);
    }

    public Matrix4f set(AxisAngle4d axisAngle) {
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
        this._m00((float)(c + x * x * omc))._m11((float)(c + y * y * omc))._m22((float)(c + z * z * omc));
        double tmp1 = x * y * omc;
        double tmp2 = z * s;
        this._m10((float)(tmp1 - tmp2))._m01((float)(tmp1 + tmp2));
        tmp1 = x * z * omc;
        tmp2 = y * s;
        this._m20((float)(tmp1 + tmp2))._m02((float)(tmp1 - tmp2));
        tmp1 = y * z * omc;
        tmp2 = x * s;
        return this._m21((float)(tmp1 - tmp2))._m12((float)(tmp1 + tmp2))._m03(0f)._m13(0f)._m23(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)._properties(18);
    }

    public Matrix4f set(Quaternionf q) {
        return this.rotation(q);
    }

    public Matrix4f set(Quaterniond q) {
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
        return this._m00((float)(w2 + x2 - z2 - y2))._m01((float)(xy + zw + zw + xy))._m02((float)(xz - yw + xz - yw))._m03(0f)._m10((float)(-zw + xy - zw + xy))._m11((float)(y2 - z2 + w2 - x2))._m12((float)(yz + yz + xw + xw))._m13(0f)._m20((float)(yw + xz + xz + yw))._m21((float)(yz + yz - xw - xw))._m22((float)(z2 - y2 - x2 + w2))._m30(0f)._m31(0f)._m32(0f)._m33(1f)._properties(18);
    }

    public Matrix4f set3x3(Matrix4f mat) {
        MemUtil.INSTANCE.copy3x3(mat, this);
        return this._properties(this.properties & mat.properties & -2);
    }

    public Matrix4f set4x3(Matrix4x3f mat) {
        return this._m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)._m20(mat.m20)._m21(mat.m21)._m22(mat.m22)._m30(mat.m30)._m31(mat.m31)._m32(mat.m32)._properties(this.properties & mat.properties() & -2);
    }

    public Matrix4f set4x3(Matrix4f mat) {
        MemUtil.INSTANCE.copy4x3(mat, this);
        return this._properties(this.properties & mat.properties & -2);
    }

    public Matrix4f mul(Matrix4f right) {
        return this.mul(right, this);
    }

    public Matrix4f mul(Matrix4f right, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.set(right);
        } else if ((right.properties() & 4) != 0) {
            return dest.set((Matrix4f)this);
        } else if ((this.properties & 8) != 0 && (right.properties() & 2) != 0) {
            return this.mulTranslationAffine(right, dest);
        } else if ((this.properties & 2) != 0 && (right.properties() & 2) != 0) {
            return this.mulAffine(right, dest);
        } else if ((this.properties & 1) != 0 && (right.properties() & 2) != 0) {
            return this.mulPerspectiveAffine(right, dest);
        } else {
            return (right.properties() & 2) != 0 ? this.mulAffineR(right, dest) : this.mul0(right, dest);
        }
    }

    public Matrix4f mul0(Matrix4f right) {
        return this.mul0(right, this);
    }

    public Matrix4f mul0(Matrix4f right, Matrix4f dest) {
        float nm00 = Math.fma(this.m00, right.m00, Math.fma(this.m10, right.m01, Math.fma(this.m20, right.m02, this.m30 * right.m03)));
        float nm01 = Math.fma(this.m01, right.m00, Math.fma(this.m11, right.m01, Math.fma(this.m21, right.m02, this.m31 * right.m03)));
        float nm02 = Math.fma(this.m02, right.m00, Math.fma(this.m12, right.m01, Math.fma(this.m22, right.m02, this.m32 * right.m03)));
        float nm03 = Math.fma(this.m03, right.m00, Math.fma(this.m13, right.m01, Math.fma(this.m23, right.m02, this.m33 * right.m03)));
        float nm10 = Math.fma(this.m00, right.m10, Math.fma(this.m10, right.m11, Math.fma(this.m20, right.m12, this.m30 * right.m13)));
        float nm11 = Math.fma(this.m01, right.m10, Math.fma(this.m11, right.m11, Math.fma(this.m21, right.m12, this.m31 * right.m13)));
        float nm12 = Math.fma(this.m02, right.m10, Math.fma(this.m12, right.m11, Math.fma(this.m22, right.m12, this.m32 * right.m13)));
        float nm13 = Math.fma(this.m03, right.m10, Math.fma(this.m13, right.m11, Math.fma(this.m23, right.m12, this.m33 * right.m13)));
        float nm20 = Math.fma(this.m00, right.m20, Math.fma(this.m10, right.m21, Math.fma(this.m20, right.m22, this.m30 * right.m23)));
        float nm21 = Math.fma(this.m01, right.m20, Math.fma(this.m11, right.m21, Math.fma(this.m21, right.m22, this.m31 * right.m23)));
        float nm22 = Math.fma(this.m02, right.m20, Math.fma(this.m12, right.m21, Math.fma(this.m22, right.m22, this.m32 * right.m23)));
        float nm23 = Math.fma(this.m03, right.m20, Math.fma(this.m13, right.m21, Math.fma(this.m23, right.m22, this.m33 * right.m23)));
        float nm30 = Math.fma(this.m00, right.m30, Math.fma(this.m10, right.m31, Math.fma(this.m20, right.m32, this.m30 * right.m33)));
        float nm31 = Math.fma(this.m01, right.m30, Math.fma(this.m11, right.m31, Math.fma(this.m21, right.m32, this.m31 * right.m33)));
        float nm32 = Math.fma(this.m02, right.m30, Math.fma(this.m12, right.m31, Math.fma(this.m22, right.m32, this.m32 * right.m33)));
        float nm33 = Math.fma(this.m03, right.m30, Math.fma(this.m13, right.m31, Math.fma(this.m23, right.m32, this.m33 * right.m33)));
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
    }

    public Matrix4f mul(float r00, float r01, float r02, float r03, float r10, float r11, float r12, float r13, float r20, float r21, float r22, float r23, float r30, float r31, float r32, float r33) {
        return this.mul(r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23, r30, r31, r32, r33, this);
    }

    public Matrix4f mul(float r00, float r01, float r02, float r03, float r10, float r11, float r12, float r13, float r20, float r21, float r22, float r23, float r30, float r31, float r32, float r33, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.set(r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23, r30, r31, r32, r33);
        } else {
            return (this.properties & 2) != 0 ? this.mulAffineL(r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23, r30, r31, r32, r33, dest) : this.mulGeneric(r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23, r30, r31, r32, r33, dest);
        }
    }

    private Matrix4f mulAffineL(float r00, float r01, float r02, float r03, float r10, float r11, float r12, float r13, float r20, float r21, float r22, float r23, float r30, float r31, float r32, float r33, Matrix4f dest) {
        float nm00 = Math.fma(this.m00, r00, Math.fma(this.m10, r01, Math.fma(this.m20, r02, this.m30 * r03)));
        float nm01 = Math.fma(this.m01, r00, Math.fma(this.m11, r01, Math.fma(this.m21, r02, this.m31 * r03)));
        float nm02 = Math.fma(this.m02, r00, Math.fma(this.m12, r01, Math.fma(this.m22, r02, this.m32 * r03)));
        float nm10 = Math.fma(this.m00, r10, Math.fma(this.m10, r11, Math.fma(this.m20, r12, this.m30 * r13)));
        float nm11 = Math.fma(this.m01, r10, Math.fma(this.m11, r11, Math.fma(this.m21, r12, this.m31 * r13)));
        float nm12 = Math.fma(this.m02, r10, Math.fma(this.m12, r11, Math.fma(this.m22, r12, this.m32 * r13)));
        float nm20 = Math.fma(this.m00, r20, Math.fma(this.m10, r21, Math.fma(this.m20, r22, this.m30 * r23)));
        float nm21 = Math.fma(this.m01, r20, Math.fma(this.m11, r21, Math.fma(this.m21, r22, this.m31 * r23)));
        float nm22 = Math.fma(this.m02, r20, Math.fma(this.m12, r21, Math.fma(this.m22, r22, this.m32 * r23)));
        float nm30 = Math.fma(this.m00, r30, Math.fma(this.m10, r31, Math.fma(this.m20, r32, this.m30 * r33)));
        float nm31 = Math.fma(this.m01, r30, Math.fma(this.m11, r31, Math.fma(this.m21, r32, this.m31 * r33)));
        float nm32 = Math.fma(this.m02, r30, Math.fma(this.m12, r31, Math.fma(this.m22, r32, this.m32 * r33)));
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(r03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(r13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(r23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(r33)._properties(2);
    }

    private Matrix4f mulGeneric(float r00, float r01, float r02, float r03, float r10, float r11, float r12, float r13, float r20, float r21, float r22, float r23, float r30, float r31, float r32, float r33, Matrix4f dest) {
        float nm00 = Math.fma(this.m00, r00, Math.fma(this.m10, r01, Math.fma(this.m20, r02, this.m30 * r03)));
        float nm01 = Math.fma(this.m01, r00, Math.fma(this.m11, r01, Math.fma(this.m21, r02, this.m31 * r03)));
        float nm02 = Math.fma(this.m02, r00, Math.fma(this.m12, r01, Math.fma(this.m22, r02, this.m32 * r03)));
        float nm03 = Math.fma(this.m03, r00, Math.fma(this.m13, r01, Math.fma(this.m23, r02, this.m33 * r03)));
        float nm10 = Math.fma(this.m00, r10, Math.fma(this.m10, r11, Math.fma(this.m20, r12, this.m30 * r13)));
        float nm11 = Math.fma(this.m01, r10, Math.fma(this.m11, r11, Math.fma(this.m21, r12, this.m31 * r13)));
        float nm12 = Math.fma(this.m02, r10, Math.fma(this.m12, r11, Math.fma(this.m22, r12, this.m32 * r13)));
        float nm13 = Math.fma(this.m03, r10, Math.fma(this.m13, r11, Math.fma(this.m23, r12, this.m33 * r13)));
        float nm20 = Math.fma(this.m00, r20, Math.fma(this.m10, r21, Math.fma(this.m20, r22, this.m30 * r23)));
        float nm21 = Math.fma(this.m01, r20, Math.fma(this.m11, r21, Math.fma(this.m21, r22, this.m31 * r23)));
        float nm22 = Math.fma(this.m02, r20, Math.fma(this.m12, r21, Math.fma(this.m22, r22, this.m32 * r23)));
        float nm23 = Math.fma(this.m03, r20, Math.fma(this.m13, r21, Math.fma(this.m23, r22, this.m33 * r23)));
        float nm30 = Math.fma(this.m00, r30, Math.fma(this.m10, r31, Math.fma(this.m20, r32, this.m30 * r33)));
        float nm31 = Math.fma(this.m01, r30, Math.fma(this.m11, r31, Math.fma(this.m21, r32, this.m31 * r33)));
        float nm32 = Math.fma(this.m02, r30, Math.fma(this.m12, r31, Math.fma(this.m22, r32, this.m32 * r33)));
        float nm33 = Math.fma(this.m03, r30, Math.fma(this.m13, r31, Math.fma(this.m23, r32, this.m33 * r33)));
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
    }

    public Matrix4f mul3x3(float r00, float r01, float r02, float r10, float r11, float r12, float r20, float r21, float r22) {
        return this.mul3x3(r00, r01, r02, r10, r11, r12, r20, r21, r22, this);
    }

    public Matrix4f mul3x3(float r00, float r01, float r02, float r10, float r11, float r12, float r20, float r21, float r22, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.set(r00, r01, r02, 0f, r10, r11, r12, 0f, r20, r21, r22, 0f, 0f, 0f, 0f, 1f) : this.mulGeneric3x3(r00, r01, r02, r10, r11, r12, r20, r21, r22, dest);
    }

    private Matrix4f mulGeneric3x3(float r00, float r01, float r02, float r10, float r11, float r12, float r20, float r21, float r22, Matrix4f dest) {
        float nm00 = Math.fma(this.m00, r00, Math.fma(this.m10, r01, this.m20 * r02));
        float nm01 = Math.fma(this.m01, r00, Math.fma(this.m11, r01, this.m21 * r02));
        float nm02 = Math.fma(this.m02, r00, Math.fma(this.m12, r01, this.m22 * r02));
        float nm03 = Math.fma(this.m03, r00, Math.fma(this.m13, r01, this.m23 * r02));
        float nm10 = Math.fma(this.m00, r10, Math.fma(this.m10, r11, this.m20 * r12));
        float nm11 = Math.fma(this.m01, r10, Math.fma(this.m11, r11, this.m21 * r12));
        float nm12 = Math.fma(this.m02, r10, Math.fma(this.m12, r11, this.m22 * r12));
        float nm13 = Math.fma(this.m03, r10, Math.fma(this.m13, r11, this.m23 * r12));
        float nm20 = Math.fma(this.m00, r20, Math.fma(this.m10, r21, this.m20 * r22));
        float nm21 = Math.fma(this.m01, r20, Math.fma(this.m11, r21, this.m21 * r22));
        float nm22 = Math.fma(this.m02, r20, Math.fma(this.m12, r21, this.m22 * r22));
        float nm23 = Math.fma(this.m03, r20, Math.fma(this.m13, r21, this.m23 * r22));
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 2);
    }

    public Matrix4f mulLocal(Matrix4f left) {
        return this.mulLocal(left, this);
    }

    public Matrix4f mulLocal(Matrix4f left, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.set(left);
        } else if ((left.properties() & 4) != 0) {
            return dest.set((Matrix4f)this);
        } else {
            return (this.properties & 2) != 0 && (left.properties() & 2) != 0 ? this.mulLocalAffine(left, dest) : this.mulLocalGeneric(left, dest);
        }
    }

    private Matrix4f mulLocalGeneric(Matrix4f left, Matrix4f dest) {
        float nm00 = Math.fma(left.m00, this.m00, Math.fma(left.m10, this.m01, Math.fma(left.m20, this.m02, left.m30 * this.m03)));
        float nm01 = Math.fma(left.m01, this.m00, Math.fma(left.m11, this.m01, Math.fma(left.m21, this.m02, left.m31 * this.m03)));
        float nm02 = Math.fma(left.m02, this.m00, Math.fma(left.m12, this.m01, Math.fma(left.m22, this.m02, left.m32 * this.m03)));
        float nm03 = Math.fma(left.m03, this.m00, Math.fma(left.m13, this.m01, Math.fma(left.m23, this.m02, left.m33 * this.m03)));
        float nm10 = Math.fma(left.m00, this.m10, Math.fma(left.m10, this.m11, Math.fma(left.m20, this.m12, left.m30 * this.m13)));
        float nm11 = Math.fma(left.m01, this.m10, Math.fma(left.m11, this.m11, Math.fma(left.m21, this.m12, left.m31 * this.m13)));
        float nm12 = Math.fma(left.m02, this.m10, Math.fma(left.m12, this.m11, Math.fma(left.m22, this.m12, left.m32 * this.m13)));
        float nm13 = Math.fma(left.m03, this.m10, Math.fma(left.m13, this.m11, Math.fma(left.m23, this.m12, left.m33 * this.m13)));
        float nm20 = Math.fma(left.m00, this.m20, Math.fma(left.m10, this.m21, Math.fma(left.m20, this.m22, left.m30 * this.m23)));
        float nm21 = Math.fma(left.m01, this.m20, Math.fma(left.m11, this.m21, Math.fma(left.m21, this.m22, left.m31 * this.m23)));
        float nm22 = Math.fma(left.m02, this.m20, Math.fma(left.m12, this.m21, Math.fma(left.m22, this.m22, left.m32 * this.m23)));
        float nm23 = Math.fma(left.m03, this.m20, Math.fma(left.m13, this.m21, Math.fma(left.m23, this.m22, left.m33 * this.m23)));
        float nm30 = Math.fma(left.m00, this.m30, Math.fma(left.m10, this.m31, Math.fma(left.m20, this.m32, left.m30 * this.m33)));
        float nm31 = Math.fma(left.m01, this.m30, Math.fma(left.m11, this.m31, Math.fma(left.m21, this.m32, left.m31 * this.m33)));
        float nm32 = Math.fma(left.m02, this.m30, Math.fma(left.m12, this.m31, Math.fma(left.m22, this.m32, left.m32 * this.m33)));
        float nm33 = Math.fma(left.m03, this.m30, Math.fma(left.m13, this.m31, Math.fma(left.m23, this.m32, left.m33 * this.m33)));
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
    }

    public Matrix4f mulLocalAffine(Matrix4f left) {
        return this.mulLocalAffine(left, this);
    }

    public Matrix4f mulLocalAffine(Matrix4f left, Matrix4f dest) {
        float nm00 = left.m00 * this.m00 + left.m10 * this.m01 + left.m20 * this.m02;
        float nm01 = left.m01 * this.m00 + left.m11 * this.m01 + left.m21 * this.m02;
        float nm02 = left.m02 * this.m00 + left.m12 * this.m01 + left.m22 * this.m02;
        float nm03 = left.m03;
        float nm10 = left.m00 * this.m10 + left.m10 * this.m11 + left.m20 * this.m12;
        float nm11 = left.m01 * this.m10 + left.m11 * this.m11 + left.m21 * this.m12;
        float nm12 = left.m02 * this.m10 + left.m12 * this.m11 + left.m22 * this.m12;
        float nm13 = left.m13;
        float nm20 = left.m00 * this.m20 + left.m10 * this.m21 + left.m20 * this.m22;
        float nm21 = left.m01 * this.m20 + left.m11 * this.m21 + left.m21 * this.m22;
        float nm22 = left.m02 * this.m20 + left.m12 * this.m21 + left.m22 * this.m22;
        float nm23 = left.m23;
        float nm30 = left.m00 * this.m30 + left.m10 * this.m31 + left.m20 * this.m32 + left.m30;
        float nm31 = left.m01 * this.m30 + left.m11 * this.m31 + left.m21 * this.m32 + left.m31;
        float nm32 = left.m02 * this.m30 + left.m12 * this.m31 + left.m22 * this.m32 + left.m32;
        float nm33 = left.m33;
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(2 | this.properties() & left.properties() & 16);
    }

    public Matrix4f mul(Matrix4x3f right) {
        return this.mul(right, this);
    }

    public Matrix4f mul(Matrix4x3f right, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.set(right);
        } else if ((right.properties() & 4) != 0) {
            return dest.set((Matrix4f)this);
        } else if ((this.properties & 8) != 0) {
            return this.mulTranslation(right, dest);
        } else if ((this.properties & 2) != 0) {
            return this.mulAffine(right, dest);
        } else {
            return (this.properties & 1) != 0 ? this.mulPerspectiveAffine(right, dest) : this.mulGeneric(right, dest);
        }
    }

    private Matrix4f mulTranslation(Matrix4x3f right, Matrix4f dest) {
        return dest._m00(right.m00)._m01(right.m01)._m02(right.m02)._m03(this.m03)._m10(right.m10)._m11(right.m11)._m12(right.m12)._m13(this.m13)._m20(right.m20)._m21(right.m21)._m22(right.m22)._m23(this.m23)._m30(right.m30 + this.m30)._m31(right.m31 + this.m31)._m32(right.m32 + this.m32)._m33(this.m33)._properties(2 | right.properties() & 16);
    }

    private Matrix4f mulAffine(Matrix4x3f right, Matrix4f dest) {
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
        return dest._m00(Math.fma(m00, rm00, Math.fma(m10, rm01, m20 * rm02)))._m01(Math.fma(m01, rm00, Math.fma(m11, rm01, m21 * rm02)))._m02(Math.fma(m02, rm00, Math.fma(m12, rm01, m22 * rm02)))._m03(this.m03)._m10(Math.fma(m00, rm10, Math.fma(m10, rm11, m20 * rm12)))._m11(Math.fma(m01, rm10, Math.fma(m11, rm11, m21 * rm12)))._m12(Math.fma(m02, rm10, Math.fma(m12, rm11, m22 * rm12)))._m13(this.m13)._m20(Math.fma(m00, rm20, Math.fma(m10, rm21, m20 * rm22)))._m21(Math.fma(m01, rm20, Math.fma(m11, rm21, m21 * rm22)))._m22(Math.fma(m02, rm20, Math.fma(m12, rm21, m22 * rm22)))._m23(this.m23)._m30(Math.fma(m00, rm30, Math.fma(m10, rm31, Math.fma(m20, rm32, this.m30))))._m31(Math.fma(m01, rm30, Math.fma(m11, rm31, Math.fma(m21, rm32, this.m31))))._m32(Math.fma(m02, rm30, Math.fma(m12, rm31, Math.fma(m22, rm32, this.m32))))._m33(this.m33)._properties(2 | this.properties & right.properties() & 16);
    }

    private Matrix4f mulGeneric(Matrix4x3f right, Matrix4f dest) {
        float nm00 = Math.fma(this.m00, right.m00, Math.fma(this.m10, right.m01, this.m20 * right.m02));
        float nm01 = Math.fma(this.m01, right.m00, Math.fma(this.m11, right.m01, this.m21 * right.m02));
        float nm02 = Math.fma(this.m02, right.m00, Math.fma(this.m12, right.m01, this.m22 * right.m02));
        float nm03 = Math.fma(this.m03, right.m00, Math.fma(this.m13, right.m01, this.m23 * right.m02));
        float nm10 = Math.fma(this.m00, right.m10, Math.fma(this.m10, right.m11, this.m20 * right.m12));
        float nm11 = Math.fma(this.m01, right.m10, Math.fma(this.m11, right.m11, this.m21 * right.m12));
        float nm12 = Math.fma(this.m02, right.m10, Math.fma(this.m12, right.m11, this.m22 * right.m12));
        float nm13 = Math.fma(this.m03, right.m10, Math.fma(this.m13, right.m11, this.m23 * right.m12));
        float nm20 = Math.fma(this.m00, right.m20, Math.fma(this.m10, right.m21, this.m20 * right.m22));
        float nm21 = Math.fma(this.m01, right.m20, Math.fma(this.m11, right.m21, this.m21 * right.m22));
        float nm22 = Math.fma(this.m02, right.m20, Math.fma(this.m12, right.m21, this.m22 * right.m22));
        float nm23 = Math.fma(this.m03, right.m20, Math.fma(this.m13, right.m21, this.m23 * right.m22));
        float nm30 = Math.fma(this.m00, right.m30, Math.fma(this.m10, right.m31, Math.fma(this.m20, right.m32, this.m30)));
        float nm31 = Math.fma(this.m01, right.m30, Math.fma(this.m11, right.m31, Math.fma(this.m21, right.m32, this.m31)));
        float nm32 = Math.fma(this.m02, right.m30, Math.fma(this.m12, right.m31, Math.fma(this.m22, right.m32, this.m32)));
        float nm33 = Math.fma(this.m03, right.m30, Math.fma(this.m13, right.m31, Math.fma(this.m23, right.m32, this.m33)));
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(this.properties & -30);
    }

    public Matrix4f mul(Matrix3x2f right) {
        return this.mul(right, this);
    }

    public Matrix4f mul(Matrix3x2f right, Matrix4f dest) {
        float nm00 = this.m00 * right.m00 + this.m10 * right.m01;
        float nm01 = this.m01 * right.m00 + this.m11 * right.m01;
        float nm02 = this.m02 * right.m00 + this.m12 * right.m01;
        float nm03 = this.m03 * right.m00 + this.m13 * right.m01;
        float nm10 = this.m00 * right.m10 + this.m10 * right.m11;
        float nm11 = this.m01 * right.m10 + this.m11 * right.m11;
        float nm12 = this.m02 * right.m10 + this.m12 * right.m11;
        float nm13 = this.m03 * right.m10 + this.m13 * right.m11;
        float nm30 = this.m00 * right.m20 + this.m10 * right.m21 + this.m30;
        float nm31 = this.m01 * right.m20 + this.m11 * right.m21 + this.m31;
        float nm32 = this.m02 * right.m20 + this.m12 * right.m21 + this.m32;
        float nm33 = this.m03 * right.m20 + this.m13 * right.m21 + this.m33;
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(this.properties & -30);
    }

    public Matrix4f mulPerspectiveAffine(Matrix4f view) {
        return this.mulPerspectiveAffine(view, this);
    }

    public Matrix4f mulPerspectiveAffine(Matrix4f view, Matrix4f dest) {
        float nm00 = this.m00 * view.m00;
        float nm01 = this.m11 * view.m01;
        float nm02 = this.m22 * view.m02;
        float nm03 = this.m23 * view.m02;
        float nm10 = this.m00 * view.m10;
        float nm11 = this.m11 * view.m11;
        float nm12 = this.m22 * view.m12;
        float nm13 = this.m23 * view.m12;
        float nm20 = this.m00 * view.m20;
        float nm21 = this.m11 * view.m21;
        float nm22 = this.m22 * view.m22;
        float nm23 = this.m23 * view.m22;
        float nm30 = this.m00 * view.m30;
        float nm31 = this.m11 * view.m31;
        float nm32 = this.m22 * view.m32 + this.m32;
        float nm33 = this.m23 * view.m32;
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
    }

    public Matrix4f mulPerspectiveAffine(Matrix4x3f view) {
        return this.mulPerspectiveAffine(view, this);
    }

    public Matrix4f mulPerspectiveAffine(Matrix4x3f view, Matrix4f dest) {
        float lm00 = this.m00;
        float lm11 = this.m11;
        float lm22 = this.m22;
        float lm23 = this.m23;
        return dest._m00(lm00 * view.m00)._m01(lm11 * view.m01)._m02(lm22 * view.m02)._m03(lm23 * view.m02)._m10(lm00 * view.m10)._m11(lm11 * view.m11)._m12(lm22 * view.m12)._m13(lm23 * view.m12)._m20(lm00 * view.m20)._m21(lm11 * view.m21)._m22(lm22 * view.m22)._m23(lm23 * view.m22)._m30(lm00 * view.m30)._m31(lm11 * view.m31)._m32(lm22 * view.m32 + this.m32)._m33(lm23 * view.m32)._properties(0);
    }

    public Matrix4f mulAffineR(Matrix4f right) {
        return this.mulAffineR(right, this);
    }

    public Matrix4f mulAffineR(Matrix4f right, Matrix4f dest) {
        float nm00 = Math.fma(this.m00, right.m00, Math.fma(this.m10, right.m01, this.m20 * right.m02));
        float nm01 = Math.fma(this.m01, right.m00, Math.fma(this.m11, right.m01, this.m21 * right.m02));
        float nm02 = Math.fma(this.m02, right.m00, Math.fma(this.m12, right.m01, this.m22 * right.m02));
        float nm03 = Math.fma(this.m03, right.m00, Math.fma(this.m13, right.m01, this.m23 * right.m02));
        float nm10 = Math.fma(this.m00, right.m10, Math.fma(this.m10, right.m11, this.m20 * right.m12));
        float nm11 = Math.fma(this.m01, right.m10, Math.fma(this.m11, right.m11, this.m21 * right.m12));
        float nm12 = Math.fma(this.m02, right.m10, Math.fma(this.m12, right.m11, this.m22 * right.m12));
        float nm13 = Math.fma(this.m03, right.m10, Math.fma(this.m13, right.m11, this.m23 * right.m12));
        float nm20 = Math.fma(this.m00, right.m20, Math.fma(this.m10, right.m21, this.m20 * right.m22));
        float nm21 = Math.fma(this.m01, right.m20, Math.fma(this.m11, right.m21, this.m21 * right.m22));
        float nm22 = Math.fma(this.m02, right.m20, Math.fma(this.m12, right.m21, this.m22 * right.m22));
        float nm23 = Math.fma(this.m03, right.m20, Math.fma(this.m13, right.m21, this.m23 * right.m22));
        float nm30 = Math.fma(this.m00, right.m30, Math.fma(this.m10, right.m31, Math.fma(this.m20, right.m32, this.m30)));
        float nm31 = Math.fma(this.m01, right.m30, Math.fma(this.m11, right.m31, Math.fma(this.m21, right.m32, this.m31)));
        float nm32 = Math.fma(this.m02, right.m30, Math.fma(this.m12, right.m31, Math.fma(this.m22, right.m32, this.m32)));
        float nm33 = Math.fma(this.m03, right.m30, Math.fma(this.m13, right.m31, Math.fma(this.m23, right.m32, this.m33)));
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(this.properties & -30);
    }

    public Matrix4f mulAffine(Matrix4f right) {
        return this.mulAffine(right, this);
    }

    public Matrix4f mulAffine(Matrix4f right, Matrix4f dest) {
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
        return dest._m00(Math.fma(m00, rm00, Math.fma(m10, rm01, m20 * rm02)))._m01(Math.fma(m01, rm00, Math.fma(m11, rm01, m21 * rm02)))._m02(Math.fma(m02, rm00, Math.fma(m12, rm01, m22 * rm02)))._m03(this.m03)._m10(Math.fma(m00, rm10, Math.fma(m10, rm11, m20 * rm12)))._m11(Math.fma(m01, rm10, Math.fma(m11, rm11, m21 * rm12)))._m12(Math.fma(m02, rm10, Math.fma(m12, rm11, m22 * rm12)))._m13(this.m13)._m20(Math.fma(m00, rm20, Math.fma(m10, rm21, m20 * rm22)))._m21(Math.fma(m01, rm20, Math.fma(m11, rm21, m21 * rm22)))._m22(Math.fma(m02, rm20, Math.fma(m12, rm21, m22 * rm22)))._m23(this.m23)._m30(Math.fma(m00, rm30, Math.fma(m10, rm31, Math.fma(m20, rm32, this.m30))))._m31(Math.fma(m01, rm30, Math.fma(m11, rm31, Math.fma(m21, rm32, this.m31))))._m32(Math.fma(m02, rm30, Math.fma(m12, rm31, Math.fma(m22, rm32, this.m32))))._m33(this.m33)._properties(2 | this.properties & right.properties() & 16);
    }

    public Matrix4f mulTranslationAffine(Matrix4f right, Matrix4f dest) {
        return dest._m00(right.m00)._m01(right.m01)._m02(right.m02)._m03(this.m03)._m10(right.m10)._m11(right.m11)._m12(right.m12)._m13(this.m13)._m20(right.m20)._m21(right.m21)._m22(right.m22)._m23(this.m23)._m30(right.m30 + this.m30)._m31(right.m31 + this.m31)._m32(right.m32 + this.m32)._m33(this.m33)._properties(2 | right.properties() & 16);
    }

    public Matrix4f mulOrthoAffine(Matrix4f view) {
        return this.mulOrthoAffine(view, this);
    }

    public Matrix4f mulOrthoAffine(Matrix4f view, Matrix4f dest) {
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
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(0f)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)._m20(nm20)._m21(nm21)._m22(nm22)._m23(0f)._m30(nm30)._m31(nm31)._m32(nm32)._m33(1f)._properties(2);
    }

    public Matrix4f fma4x3(Matrix4f other, float otherFactor) {
        return this.fma4x3(other, otherFactor, this);
    }

    public Matrix4f fma4x3(Matrix4f other, float otherFactor, Matrix4f dest) {
        dest._m00(Math.fma(other.m00, otherFactor, this.m00))._m01(Math.fma(other.m01, otherFactor, this.m01))._m02(Math.fma(other.m02, otherFactor, this.m02))._m03(this.m03)._m10(Math.fma(other.m10, otherFactor, this.m10))._m11(Math.fma(other.m11, otherFactor, this.m11))._m12(Math.fma(other.m12, otherFactor, this.m12))._m13(this.m13)._m20(Math.fma(other.m20, otherFactor, this.m20))._m21(Math.fma(other.m21, otherFactor, this.m21))._m22(Math.fma(other.m22, otherFactor, this.m22))._m23(this.m23)._m30(Math.fma(other.m30, otherFactor, this.m30))._m31(Math.fma(other.m31, otherFactor, this.m31))._m32(Math.fma(other.m32, otherFactor, this.m32))._m33(this.m33)._properties(0);
        return dest;
    }

    public Matrix4f add(Matrix4f other) {
        return this.add(other, this);
    }

    public Matrix4f add(Matrix4f other, Matrix4f dest) {
        dest._m00(this.m00 + other.m00)._m01(this.m01 + other.m01)._m02(this.m02 + other.m02)._m03(this.m03 + other.m03)._m10(this.m10 + other.m10)._m11(this.m11 + other.m11)._m12(this.m12 + other.m12)._m13(this.m13 + other.m13)._m20(this.m20 + other.m20)._m21(this.m21 + other.m21)._m22(this.m22 + other.m22)._m23(this.m23 + other.m23)._m30(this.m30 + other.m30)._m31(this.m31 + other.m31)._m32(this.m32 + other.m32)._m33(this.m33 + other.m33)._properties(0);
        return dest;
    }

    public Matrix4f sub(Matrix4f subtrahend) {
        return this.sub(subtrahend, this);
    }

    public Matrix4f sub(Matrix4f subtrahend, Matrix4f dest) {
        dest._m00(this.m00 - subtrahend.m00)._m01(this.m01 - subtrahend.m01)._m02(this.m02 - subtrahend.m02)._m03(this.m03 - subtrahend.m03)._m10(this.m10 - subtrahend.m10)._m11(this.m11 - subtrahend.m11)._m12(this.m12 - subtrahend.m12)._m13(this.m13 - subtrahend.m13)._m20(this.m20 - subtrahend.m20)._m21(this.m21 - subtrahend.m21)._m22(this.m22 - subtrahend.m22)._m23(this.m23 - subtrahend.m23)._m30(this.m30 - subtrahend.m30)._m31(this.m31 - subtrahend.m31)._m32(this.m32 - subtrahend.m32)._m33(this.m33 - subtrahend.m33)._properties(0);
        return dest;
    }

    public Matrix4f mulComponentWise(Matrix4f other) {
        return this.mulComponentWise(other, this);
    }

    public Matrix4f mulComponentWise(Matrix4f other, Matrix4f dest) {
        dest._m00(this.m00 * other.m00)._m01(this.m01 * other.m01)._m02(this.m02 * other.m02)._m03(this.m03 * other.m03)._m10(this.m10 * other.m10)._m11(this.m11 * other.m11)._m12(this.m12 * other.m12)._m13(this.m13 * other.m13)._m20(this.m20 * other.m20)._m21(this.m21 * other.m21)._m22(this.m22 * other.m22)._m23(this.m23 * other.m23)._m30(this.m30 * other.m30)._m31(this.m31 * other.m31)._m32(this.m32 * other.m32)._m33(this.m33 * other.m33)._properties(0);
        return dest;
    }

    public Matrix4f add4x3(Matrix4f other) {
        return this.add4x3(other, this);
    }

    public Matrix4f add4x3(Matrix4f other, Matrix4f dest) {
        dest._m00(this.m00 + other.m00)._m01(this.m01 + other.m01)._m02(this.m02 + other.m02)._m03(this.m03)._m10(this.m10 + other.m10)._m11(this.m11 + other.m11)._m12(this.m12 + other.m12)._m13(this.m13)._m20(this.m20 + other.m20)._m21(this.m21 + other.m21)._m22(this.m22 + other.m22)._m23(this.m23)._m30(this.m30 + other.m30)._m31(this.m31 + other.m31)._m32(this.m32 + other.m32)._m33(this.m33)._properties(0);
        return dest;
    }

    public Matrix4f sub4x3(Matrix4f subtrahend) {
        return this.sub4x3(subtrahend, this);
    }

    public Matrix4f sub4x3(Matrix4f subtrahend, Matrix4f dest) {
        dest._m00(this.m00 - subtrahend.m00)._m01(this.m01 - subtrahend.m01)._m02(this.m02 - subtrahend.m02)._m03(this.m03)._m10(this.m10 - subtrahend.m10)._m11(this.m11 - subtrahend.m11)._m12(this.m12 - subtrahend.m12)._m13(this.m13)._m20(this.m20 - subtrahend.m20)._m21(this.m21 - subtrahend.m21)._m22(this.m22 - subtrahend.m22)._m23(this.m23)._m30(this.m30 - subtrahend.m30)._m31(this.m31 - subtrahend.m31)._m32(this.m32 - subtrahend.m32)._m33(this.m33)._properties(0);
        return dest;
    }

    public Matrix4f mul4x3ComponentWise(Matrix4f other) {
        return this.mul4x3ComponentWise(other, this);
    }

    public Matrix4f mul4x3ComponentWise(Matrix4f other, Matrix4f dest) {
        dest._m00(this.m00 * other.m00)._m01(this.m01 * other.m01)._m02(this.m02 * other.m02)._m03(this.m03)._m10(this.m10 * other.m10)._m11(this.m11 * other.m11)._m12(this.m12 * other.m12)._m13(this.m13)._m20(this.m20 * other.m20)._m21(this.m21 * other.m21)._m22(this.m22 * other.m22)._m23(this.m23)._m30(this.m30 * other.m30)._m31(this.m31 * other.m31)._m32(this.m32 * other.m32)._m33(this.m33)._properties(0);
        return dest;
    }

    public Matrix4f set(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23, float m30, float m31, float m32, float m33) {
        return this._m00(m00)._m10(m10)._m20(m20)._m30(m30)._m01(m01)._m11(m11)._m21(m21)._m31(m31)._m02(m02)._m12(m12)._m22(m22)._m32(m32)._m03(m03)._m13(m13)._m23(m23)._m33(m33).determineProperties();
    }

    public Matrix4f set(float[] m, int off) {
        MemUtil.INSTANCE.copy(m, off, this);
        return this.determineProperties();
    }

    public Matrix4f set(float[] m) {
        return this.set(m, 0);
    }

    public Matrix4f setTransposed(float[] m, int off) {
        MemUtil.INSTANCE.copyTransposed(m, off, this);
        return this.determineProperties();
    }

    public Matrix4f setTransposed(float[] m) {
        return this.setTransposed(m, 0);
    }

    public Matrix4f set(Vector4f col0, Vector4f col1, Vector4f col2, Vector4f col3) {
        return this._m00(col0.x)._m01(col0.y)._m02(col0.z)._m03(col0.w)._m10(col1.x)._m11(col1.y)._m12(col1.z)._m13(col1.w)._m20(col2.x)._m21(col2.y)._m22(col2.z)._m23(col2.w)._m30(col3.x)._m31(col3.y)._m32(col3.z)._m33(col3.w).determineProperties();
    }

    public float determinant() {
        return (this.properties & 2) != 0 ? this.determinantAffine() : (this.m00 * this.m11 - this.m01 * this.m10) * (this.m22 * this.m33 - this.m23 * this.m32) + (this.m02 * this.m10 - this.m00 * this.m12) * (this.m21 * this.m33 - this.m23 * this.m31) + (this.m00 * this.m13 - this.m03 * this.m10) * (this.m21 * this.m32 - this.m22 * this.m31) + (this.m01 * this.m12 - this.m02 * this.m11) * (this.m20 * this.m33 - this.m23 * this.m30) + (this.m03 * this.m11 - this.m01 * this.m13) * (this.m20 * this.m32 - this.m22 * this.m30) + (this.m02 * this.m13 - this.m03 * this.m12) * (this.m20 * this.m31 - this.m21 * this.m30);
    }

    public float determinant3x3() {
        return (this.m00 * this.m11 - this.m01 * this.m10) * this.m22 + (this.m02 * this.m10 - this.m00 * this.m12) * this.m21 + (this.m01 * this.m12 - this.m02 * this.m11) * this.m20;
    }

    public float determinantAffine() {
        return (this.m00 * this.m11 - this.m01 * this.m10) * this.m22 + (this.m02 * this.m10 - this.m00 * this.m12) * this.m21 + (this.m01 * this.m12 - this.m02 * this.m11) * this.m20;
    }

    public Matrix4f invert(Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.identity();
        } else if ((this.properties & 8) != 0) {
            return this.invertTranslation(dest);
        } else if ((this.properties & 16) != 0) {
            return this.invertOrthonormal(dest);
        } else if ((this.properties & 2) != 0) {
            return this.invertAffine(dest);
        } else {
            return (this.properties & 1) != 0 ? this.invertPerspective(dest) : this.invertGeneric(dest);
        }
    }

    private Matrix4f invertTranslation(Matrix4f dest) {
        if (dest != this) {
            dest.set((Matrix4f)this);
        }

        return dest._m30(-this.m30)._m31(-this.m31)._m32(-this.m32)._properties(26);
    }

    private Matrix4f invertOrthonormal(Matrix4f dest) {
        float nm30 = -(this.m00 * this.m30 + this.m01 * this.m31 + this.m02 * this.m32);
        float nm31 = -(this.m10 * this.m30 + this.m11 * this.m31 + this.m12 * this.m32);
        float nm32 = -(this.m20 * this.m30 + this.m21 * this.m31 + this.m22 * this.m32);
        float m01 = this.m01;
        float m02 = this.m02;
        float m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m10)._m02(this.m20)._m03(0f)._m10(m01)._m11(this.m11)._m12(this.m21)._m13(0f)._m20(m02)._m21(m12)._m22(this.m22)._m23(0f)._m30(nm30)._m31(nm31)._m32(nm32)._m33(1f)._properties(18);
    }

    private Matrix4f invertGeneric(Matrix4f dest) {
        return this != dest ? this.invertGenericNonThis(dest) : this.invertGenericThis(dest);
    }

    private Matrix4f invertGenericNonThis(Matrix4f dest) {
        float a = this.m00 * this.m11 - this.m01 * this.m10;
        float b = this.m00 * this.m12 - this.m02 * this.m10;
        float c = this.m00 * this.m13 - this.m03 * this.m10;
        float d = this.m01 * this.m12 - this.m02 * this.m11;
        float e = this.m01 * this.m13 - this.m03 * this.m11;
        float f = this.m02 * this.m13 - this.m03 * this.m12;
        float g = this.m20 * this.m31 - this.m21 * this.m30;
        float h = this.m20 * this.m32 - this.m22 * this.m30;
        float i = this.m20 * this.m33 - this.m23 * this.m30;
        float j = this.m21 * this.m32 - this.m22 * this.m31;
        float k = this.m21 * this.m33 - this.m23 * this.m31;
        float l = this.m22 * this.m33 - this.m23 * this.m32;
        float det = a * l - b * k + c * j + d * i - e * h + f * g;
        det = 1f / det;
        return dest._m00(Math.fma(this.m11, l, Math.fma(-this.m12, k, this.m13 * j)) * det)._m01(Math.fma(-this.m01, l, Math.fma(this.m02, k, -this.m03 * j)) * det)._m02(Math.fma(this.m31, f, Math.fma(-this.m32, e, this.m33 * d)) * det)._m03(Math.fma(-this.m21, f, Math.fma(this.m22, e, -this.m23 * d)) * det)._m10(Math.fma(-this.m10, l, Math.fma(this.m12, i, -this.m13 * h)) * det)._m11(Math.fma(this.m00, l, Math.fma(-this.m02, i, this.m03 * h)) * det)._m12(Math.fma(-this.m30, f, Math.fma(this.m32, c, -this.m33 * b)) * det)._m13(Math.fma(this.m20, f, Math.fma(-this.m22, c, this.m23 * b)) * det)._m20(Math.fma(this.m10, k, Math.fma(-this.m11, i, this.m13 * g)) * det)._m21(Math.fma(-this.m00, k, Math.fma(this.m01, i, -this.m03 * g)) * det)._m22(Math.fma(this.m30, e, Math.fma(-this.m31, c, this.m33 * a)) * det)._m23(Math.fma(-this.m20, e, Math.fma(this.m21, c, -this.m23 * a)) * det)._m30(Math.fma(-this.m10, j, Math.fma(this.m11, h, -this.m12 * g)) * det)._m31(Math.fma(this.m00, j, Math.fma(-this.m01, h, this.m02 * g)) * det)._m32(Math.fma(-this.m30, d, Math.fma(this.m31, b, -this.m32 * a)) * det)._m33(Math.fma(this.m20, d, Math.fma(-this.m21, b, this.m22 * a)) * det)._properties(0);
    }

    private Matrix4f invertGenericThis(Matrix4f dest) {
        float a = this.m00 * this.m11 - this.m01 * this.m10;
        float b = this.m00 * this.m12 - this.m02 * this.m10;
        float c = this.m00 * this.m13 - this.m03 * this.m10;
        float d = this.m01 * this.m12 - this.m02 * this.m11;
        float e = this.m01 * this.m13 - this.m03 * this.m11;
        float f = this.m02 * this.m13 - this.m03 * this.m12;
        float g = this.m20 * this.m31 - this.m21 * this.m30;
        float h = this.m20 * this.m32 - this.m22 * this.m30;
        float i = this.m20 * this.m33 - this.m23 * this.m30;
        float j = this.m21 * this.m32 - this.m22 * this.m31;
        float k = this.m21 * this.m33 - this.m23 * this.m31;
        float l = this.m22 * this.m33 - this.m23 * this.m32;
        float det = a * l - b * k + c * j + d * i - e * h + f * g;
        det = 1f / det;
        float nm00 = Math.fma(this.m11, l, Math.fma(-this.m12, k, this.m13 * j)) * det;
        float nm01 = Math.fma(-this.m01, l, Math.fma(this.m02, k, -this.m03 * j)) * det;
        float nm02 = Math.fma(this.m31, f, Math.fma(-this.m32, e, this.m33 * d)) * det;
        float nm03 = Math.fma(-this.m21, f, Math.fma(this.m22, e, -this.m23 * d)) * det;
        float nm10 = Math.fma(-this.m10, l, Math.fma(this.m12, i, -this.m13 * h)) * det;
        float nm11 = Math.fma(this.m00, l, Math.fma(-this.m02, i, this.m03 * h)) * det;
        float nm12 = Math.fma(-this.m30, f, Math.fma(this.m32, c, -this.m33 * b)) * det;
        float nm13 = Math.fma(this.m20, f, Math.fma(-this.m22, c, this.m23 * b)) * det;
        float nm20 = Math.fma(this.m10, k, Math.fma(-this.m11, i, this.m13 * g)) * det;
        float nm21 = Math.fma(-this.m00, k, Math.fma(this.m01, i, -this.m03 * g)) * det;
        float nm22 = Math.fma(this.m30, e, Math.fma(-this.m31, c, this.m33 * a)) * det;
        float nm23 = Math.fma(-this.m20, e, Math.fma(this.m21, c, -this.m23 * a)) * det;
        float nm30 = Math.fma(-this.m10, j, Math.fma(this.m11, h, -this.m12 * g)) * det;
        float nm31 = Math.fma(this.m00, j, Math.fma(-this.m01, h, this.m02 * g)) * det;
        float nm32 = Math.fma(-this.m30, d, Math.fma(this.m31, b, -this.m32 * a)) * det;
        float nm33 = Math.fma(this.m20, d, Math.fma(-this.m21, b, this.m22 * a)) * det;
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
    }

    public Matrix4f invert() {
        return this.invert(this);
    }

    public Matrix4f invertPerspective(Matrix4f dest) {
        float a = 1f / (this.m00 * this.m11);
        float l = -1f / (this.m23 * this.m32);
        return dest.set(this.m11 * a, 0f, 0f, 0f, 0f, this.m00 * a, 0f, 0f, 0f, 0f, 0f, -this.m23 * l, 0f, 0f, -this.m32 * l, this.m22 * l)._properties(0);
    }

    public Matrix4f invertPerspective() {
        return this.invertPerspective(this);
    }

    public Matrix4f invertFrustum(Matrix4f dest) {
        float invM00 = 1f / this.m00;
        float invM11 = 1f / this.m11;
        float invM23 = 1f / this.m23;
        float invM32 = 1f / this.m32;
        return dest.set(invM00, 0f, 0f, 0f, 0f, invM11, 0f, 0f, 0f, 0f, 0f, invM32, -this.m20 * invM00 * invM23, -this.m21 * invM11 * invM23, invM23, -this.m22 * invM23 * invM32);
    }

    public Matrix4f invertFrustum() {
        return this.invertFrustum(this);
    }

    public Matrix4f invertOrtho(Matrix4f dest) {
        float invM00 = 1f / this.m00;
        float invM11 = 1f / this.m11;
        float invM22 = 1f / this.m22;
        return dest.set(invM00, 0f, 0f, 0f, 0f, invM11, 0f, 0f, 0f, 0f, invM22, 0f, -this.m30 * invM00, -this.m31 * invM11, -this.m32 * invM22, 1f)._properties(2 | this.properties & 16);
    }

    public Matrix4f invertOrtho() {
        return this.invertOrtho(this);
    }

    public Matrix4f invertPerspectiveView(Matrix4f view, Matrix4f dest) {
        float a = 1f / (this.m00 * this.m11);
        float l = -1f / (this.m23 * this.m32);
        float pm00 = this.m11 * a;
        float pm11 = this.m00 * a;
        float pm23 = -this.m23 * l;
        float pm32 = -this.m32 * l;
        float pm33 = this.m22 * l;
        float vm30 = -view.m00 * view.m30 - view.m01 * view.m31 - view.m02 * view.m32;
        float vm31 = -view.m10 * view.m30 - view.m11 * view.m31 - view.m12 * view.m32;
        float vm32 = -view.m20 * view.m30 - view.m21 * view.m31 - view.m22 * view.m32;
        float nm10 = view.m01 * pm11;
        float nm30 = view.m02 * pm32 + vm30 * pm33;
        float nm31 = view.m12 * pm32 + vm31 * pm33;
        float nm32 = view.m22 * pm32 + vm32 * pm33;
        return dest._m00(view.m00 * pm00)._m01(view.m10 * pm00)._m02(view.m20 * pm00)._m03(0f)._m10(nm10)._m11(view.m11 * pm11)._m12(view.m21 * pm11)._m13(0f)._m20(vm30 * pm23)._m21(vm31 * pm23)._m22(vm32 * pm23)._m23(pm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(pm33)._properties(0);
    }

    public Matrix4f invertPerspectiveView(Matrix4x3f view, Matrix4f dest) {
        float a = 1f / (this.m00 * this.m11);
        float l = -1f / (this.m23 * this.m32);
        float pm00 = this.m11 * a;
        float pm11 = this.m00 * a;
        float pm23 = -this.m23 * l;
        float pm32 = -this.m32 * l;
        float pm33 = this.m22 * l;
        float vm30 = -view.m00 * view.m30 - view.m01 * view.m31 - view.m02 * view.m32;
        float vm31 = -view.m10 * view.m30 - view.m11 * view.m31 - view.m12 * view.m32;
        float vm32 = -view.m20 * view.m30 - view.m21 * view.m31 - view.m22 * view.m32;
        return dest._m00(view.m00 * pm00)._m01(view.m10 * pm00)._m02(view.m20 * pm00)._m03(0f)._m10(view.m01 * pm11)._m11(view.m11 * pm11)._m12(view.m21 * pm11)._m13(0f)._m20(vm30 * pm23)._m21(vm31 * pm23)._m22(vm32 * pm23)._m23(pm23)._m30(view.m02 * pm32 + vm30 * pm33)._m31(view.m12 * pm32 + vm31 * pm33)._m32(view.m22 * pm32 + vm32 * pm33)._m33(pm33)._properties(0);
    }

    public Matrix4f invertAffine(Matrix4f dest) {
        float m11m00 = this.m00 * this.m11;
        float m10m01 = this.m01 * this.m10;
        float m10m02 = this.m02 * this.m10;
        float m12m00 = this.m00 * this.m12;
        float m12m01 = this.m01 * this.m12;
        float m11m02 = this.m02 * this.m11;
        float det = (m11m00 - m10m01) * this.m22 + (m10m02 - m12m00) * this.m21 + (m12m01 - m11m02) * this.m20;
        float s = 1f / det;
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
        float nm31 = (m20m02 * this.m31 - m20m01 * this.m32 + m21m00 * this.m32 - m21m02 * this.m30 + m22m01 * this.m30 - m22m00 * this.m31) * s;
        float nm32 = (m11m02 * this.m30 - m12m01 * this.m30 + m12m00 * this.m31 - m10m02 * this.m31 + m10m01 * this.m32 - m11m00 * this.m32) * s;
        return dest._m00((m11m22 - m12m21) * s)._m01((m21m02 - m22m01) * s)._m02((m12m01 - m11m02) * s)._m03(0f)._m10((m12m20 - m10m22) * s)._m11((m22m00 - m20m02) * s)._m12((m10m02 - m12m00) * s)._m13(0f)._m20((m10m21 - m11m20) * s)._m21((m20m01 - m21m00) * s)._m22((m11m00 - m10m01) * s)._m23(0f)._m30((m10m22 * this.m31 - m10m21 * this.m32 + m11m20 * this.m32 - m11m22 * this.m30 + m12m21 * this.m30 - m12m20 * this.m31) * s)._m31(nm31)._m32(nm32)._m33(1f)._properties(2);
    }

    public Matrix4f invertAffine() {
        return this.invertAffine(this);
    }

    public Matrix4f transpose(Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.identity();
        } else {
            return this != dest ? this.transposeNonThisGeneric(dest) : this.transposeThisGeneric(dest);
        }
    }

    private Matrix4f transposeNonThisGeneric(Matrix4f dest) {
        return dest._m00(this.m00)._m01(this.m10)._m02(this.m20)._m03(this.m30)._m10(this.m01)._m11(this.m11)._m12(this.m21)._m13(this.m31)._m20(this.m02)._m21(this.m12)._m22(this.m22)._m23(this.m32)._m30(this.m03)._m31(this.m13)._m32(this.m23)._m33(this.m33)._properties(0);
    }

    private Matrix4f transposeThisGeneric(Matrix4f dest) {
        float nm10 = this.m01;
        float nm20 = this.m02;
        float nm21 = this.m12;
        float nm30 = this.m03;
        float nm31 = this.m13;
        float nm32 = this.m23;
        return dest._m01(this.m10)._m02(this.m20)._m03(this.m30)._m10(nm10)._m12(this.m21)._m13(this.m31)._m20(nm20)._m21(nm21)._m23(this.m32)._m30(nm30)._m31(nm31)._m32(nm32)._properties(0);
    }

    public Matrix4f transpose3x3() {
        return this.transpose3x3(this);
    }

    public Matrix4f transpose3x3(Matrix4f dest) {
        float nm10 = this.m01;
        float nm20 = this.m02;
        float nm21 = this.m12;
        return dest._m00(this.m00)._m01(this.m10)._m02(this.m20)._m10(nm10)._m11(this.m11)._m12(this.m21)._m20(nm20)._m21(nm21)._m22(this.m22)._properties(this.properties & 30);
    }

    public Matrix3f transpose3x3(Matrix3f dest) {
        return dest._m00(this.m00)._m01(this.m10)._m02(this.m20)._m10(this.m01)._m11(this.m11)._m12(this.m21)._m20(this.m02)._m21(this.m12)._m22(this.m22);
    }

    public Matrix4f transpose() {
        return this.transpose(this);
    }

    public Matrix4f translation(float x, float y, float z) {
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        return this._m30(x)._m31(y)._m32(z)._properties(26);
    }

    public Matrix4f translation(Vector3f offset) {
        return this.translation(offset.x, offset.y, offset.z);
    }

    public Matrix4f setTranslation(float x, float y, float z) {
        return this._m30(x)._m31(y)._m32(z)._properties(this.properties & -6);
    }

    public Matrix4f setTranslation(Vector3f xyz) {
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
        StringBuffer res = new StringBuffer();
        int eIndex = Integer.MIN_VALUE;

        for(int i = 0; i < str.length(); ++i) {
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
        return Runtime.format((double)this.m00, formatter) + " " + Runtime.format((double)this.m10, formatter) + " " + Runtime.format((double)this.m20, formatter) + " " + Runtime.format((double)this.m30, formatter) + "\n" + Runtime.format((double)this.m01, formatter) + " " + Runtime.format((double)this.m11, formatter) + " " + Runtime.format((double)this.m21, formatter) + " " + Runtime.format((double)this.m31, formatter) + "\n" + Runtime.format((double)this.m02, formatter) + " " + Runtime.format((double)this.m12, formatter) + " " + Runtime.format((double)this.m22, formatter) + " " + Runtime.format((double)this.m32, formatter) + "\n" + Runtime.format((double)this.m03, formatter) + " " + Runtime.format((double)this.m13, formatter) + " " + Runtime.format((double)this.m23, formatter) + " " + Runtime.format((double)this.m33, formatter) + "\n";
    }

    public Matrix4f get(Matrix4f dest) {
        return dest.set(this);
    }

    public Matrix4x3f get4x3(Matrix4x3f dest) {
        return dest.set(this);
    }

    public Matrix4d get(Matrix4d dest) {
        return dest.set(this);
    }

    public Matrix3f get3x3(Matrix3f dest) {
        return dest.set(this);
    }

    public Matrix3d get3x3(Matrix3d dest) {
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
        MemUtil.INSTANCE.copy(this, arr, 0);
        return arr;
    }

    public Matrix4f zero() {
        MemUtil.INSTANCE.zero(this);
        return this._properties(0);
    }

    public Matrix4f scaling(float factor) {
        return this.scaling(factor, factor, factor);
    }

    public Matrix4f scaling(float x, float y, float z) {
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        boolean one = Math.absEqualsOne(x) && Math.absEqualsOne(y) && Math.absEqualsOne(z);
        return this._m00(x)._m11(y)._m22(z)._properties(2 | (one ? 16 : 0));
    }

    public Matrix4f scaling(Vector3f xyz) {
        return this.scaling(xyz.x, xyz.y, xyz.z);
    }

    public Matrix4f rotation(float angle, Vector3f axis) {
        return this.rotation(angle, axis.x, axis.y, axis.z);
    }

    public Matrix4f rotation(AxisAngle4f axisAngle) {
        return this.rotation(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Matrix4f rotation(float angle, float x, float y, float z) {
        if (y == 0f && z == 0f && Math.absEqualsOne(x)) {
            return this.rotationX(x * angle);
        } else if (x == 0f && z == 0f && Math.absEqualsOne(y)) {
            return this.rotationY(y * angle);
        } else {
            return x == 0f && y == 0f && Math.absEqualsOne(z) ? this.rotationZ(z * angle) : this.rotationInternal(angle, x, y, z);
        }
    }

    private Matrix4f rotationInternal(float angle, float x, float y, float z) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        float C = 1f - cos;
        float xy = x * y;
        float xz = x * z;
        float yz = y * z;
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        return this._m00(cos + x * x * C)._m10(xy * C - z * sin)._m20(xz * C + y * sin)._m01(xy * C + z * sin)._m11(cos + y * y * C)._m21(yz * C - x * sin)._m02(xz * C - y * sin)._m12(yz * C + x * sin)._m22(cos + z * z * C)._properties(18);
    }

    public Matrix4f rotationX(float ang) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        this._m11(cos)._m12(sin)._m21(-sin)._m22(cos)._properties(18);
        return this;
    }

    public Matrix4f rotationY(float ang) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        this._m00(cos)._m02(-sin)._m20(sin)._m22(cos)._properties(18);
        return this;
    }

    public Matrix4f rotationZ(float ang) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        return this._m00(cos)._m01(sin)._m10(-sin)._m11(cos)._properties(18);
    }

    public Matrix4f rotationTowardsXY(float dirX, float dirY) {
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        return this._m00(dirY)._m01(dirX)._m10(-dirX)._m11(dirY)._properties(18);
    }

    public Matrix4f rotationXYZ(float angleX, float angleY, float angleZ) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        float nm01 = -sinX * -sinY;
        float nm02 = cosX * -sinY;
        return this._m20(sinY)._m21(-sinX * cosY)._m22(cosX * cosY)._m00(cosY * cosZ)._m01(nm01 * cosZ + cosX * sinZ)._m02(nm02 * cosZ + sinX * sinZ)._m10(cosY * -sinZ)._m11(nm01 * -sinZ + cosX * cosZ)._m12(nm02 * -sinZ + sinX * cosZ)._properties(18);
    }

    public Matrix4f rotationZYX(float angleZ, float angleY, float angleX) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float nm20 = cosZ * sinY;
        float nm21 = sinZ * sinY;
        return this._m00(cosZ * cosY)._m01(sinZ * cosY)._m02(-sinY)._m03(0f)._m10(-sinZ * cosX + nm20 * sinX)._m11(cosZ * cosX + nm21 * sinX)._m12(cosY * sinX)._m13(0f)._m20(-sinZ * -sinX + nm20 * cosX)._m21(cosZ * -sinX + nm21 * cosX)._m22(cosY * cosX)._m23(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)._properties(18);
    }

    public Matrix4f rotationYXZ(float angleY, float angleX, float angleZ) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float nm10 = sinY * sinX;
        float nm12 = cosY * sinX;
        return this._m20(sinY * cosX)._m21(-sinX)._m22(cosY * cosX)._m23(0f)._m00(cosY * cosZ + nm10 * sinZ)._m01(cosX * sinZ)._m02(-sinY * cosZ + nm12 * sinZ)._m03(0f)._m10(cosY * -sinZ + nm10 * cosZ)._m11(cosX * cosZ)._m12(-sinY * -sinZ + nm12 * cosZ)._m13(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)._properties(18);
    }

    public Matrix4f setRotationXYZ(float angleX, float angleY, float angleZ) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float nm01 = -sinX * -sinY;
        float nm02 = cosX * -sinY;
        return this._m20(sinY)._m21(-sinX * cosY)._m22(cosX * cosY)._m00(cosY * cosZ)._m01(nm01 * cosZ + cosX * sinZ)._m02(nm02 * cosZ + sinX * sinZ)._m10(cosY * -sinZ)._m11(nm01 * -sinZ + cosX * cosZ)._m12(nm02 * -sinZ + sinX * cosZ)._properties(this.properties & -14);
    }

    public Matrix4f setRotationZYX(float angleZ, float angleY, float angleX) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float nm20 = cosZ * sinY;
        float nm21 = sinZ * sinY;
        return this._m00(cosZ * cosY)._m01(sinZ * cosY)._m02(-sinY)._m10(-sinZ * cosX + nm20 * sinX)._m11(cosZ * cosX + nm21 * sinX)._m12(cosY * sinX)._m20(-sinZ * -sinX + nm20 * cosX)._m21(cosZ * -sinX + nm21 * cosX)._m22(cosY * cosX)._properties(this.properties & -14);
    }

    public Matrix4f setRotationYXZ(float angleY, float angleX, float angleZ) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float nm10 = sinY * sinX;
        float nm12 = cosY * sinX;
        return this._m20(sinY * cosX)._m21(-sinX)._m22(cosY * cosX)._m00(cosY * cosZ + nm10 * sinZ)._m01(cosX * sinZ)._m02(-sinY * cosZ + nm12 * sinZ)._m10(cosY * -sinZ + nm10 * cosZ)._m11(cosX * cosZ)._m12(-sinY * -sinZ + nm12 * cosZ)._properties(this.properties & -14);
    }

    public Matrix4f rotation(Quaternionf quat) {
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
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        return this._m00(w2 + x2 - z2 - y2)._m01(dxy + dzw)._m02(dxz - dyw)._m10(-dzw + dxy)._m11(y2 - z2 + w2 - x2)._m12(dyz + dxw)._m20(dyw + dxz)._m21(dyz - dxw)._m22(z2 - y2 - x2 + w2)._properties(18);
    }

    public Matrix4f translationRotateScale(float tx, float ty, float tz, float qx, float qy, float qz, float qw, float sx, float sy, float sz) {
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
        boolean one = Math.absEqualsOne(sx) && Math.absEqualsOne(sy) && Math.absEqualsOne(sz);
        return this._m00(sx - (q11 + q22) * sx)._m01((q01 + q23) * sx)._m02((q02 - q13) * sx)._m03(0f)._m10((q01 - q23) * sy)._m11(sy - (q22 + q00) * sy)._m12((q12 + q03) * sy)._m13(0f)._m20((q02 + q13) * sz)._m21((q12 - q03) * sz)._m22(sz - (q11 + q00) * sz)._m23(0f)._m30(tx)._m31(ty)._m32(tz)._m33(1f)._properties(2 | (one ? 16 : 0));
    }

    public Matrix4f translationRotateScale(Vector3f translation, Quaternionf quat, Vector3f scale) {
        return this.translationRotateScale(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale.x, scale.y, scale.z);
    }

    public Matrix4f translationRotateScale(float tx, float ty, float tz, float qx, float qy, float qz, float qw, float scale) {
        return this.translationRotateScale(tx, ty, tz, qx, qy, qz, qw, scale, scale, scale);
    }

    public Matrix4f translationRotateScale(Vector3f translation, Quaternionf quat, float scale) {
        return this.translationRotateScale(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale, scale, scale);
    }

    public Matrix4f translationRotateScaleInvert(float tx, float ty, float tz, float qx, float qy, float qz, float qw, float sx, float sy, float sz) {
        boolean one = Math.absEqualsOne(sx) && Math.absEqualsOne(sy) && Math.absEqualsOne(sz);
        if (one) {
            return this.translationRotateInvert(tx, ty, tz, qx, qy, qz, qw);
        } else {
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
            float isx = 1f / sx;
            float isy = 1f / sy;
            float isz = 1f / sz;
            return this._m00(isx * (1f - q11 - q22))._m01(isy * (q01 + q23))._m02(isz * (q02 - q13))._m03(0f)._m10(isx * (q01 - q23))._m11(isy * (1f - q22 - q00))._m12(isz * (q12 + q03))._m13(0f)._m20(isx * (q02 + q13))._m21(isy * (q12 - q03))._m22(isz * (1f - q11 - q00))._m23(0f)._m30(-this.m00 * tx - this.m10 * ty - this.m20 * tz)._m31(-this.m01 * tx - this.m11 * ty - this.m21 * tz)._m32(-this.m02 * tx - this.m12 * ty - this.m22 * tz)._m33(1f)._properties(2);
        }
    }

    public Matrix4f translationRotateScaleInvert(Vector3f translation, Quaternionf quat, Vector3f scale) {
        return this.translationRotateScaleInvert(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale.x, scale.y, scale.z);
    }

    public Matrix4f translationRotateScaleInvert(Vector3f translation, Quaternionf quat, float scale) {
        return this.translationRotateScaleInvert(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale, scale, scale);
    }

    public Matrix4f translationRotateScaleMulAffine(float tx, float ty, float tz, float qx, float qy, float qz, float qw, float sx, float sy, float sz, Matrix4f m) {
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
        float m00 = nm00 * m.m00 + nm10 * m.m01 + nm20 * m.m02;
        float m01 = nm01 * m.m00 + nm11 * m.m01 + nm21 * m.m02;
        this._m02(nm02 * m.m00 + nm12 * m.m01 + nm22 * m.m02)._m00(m00)._m01(m01)._m03(0f);
        float m10 = nm00 * m.m10 + nm10 * m.m11 + nm20 * m.m12;
        float m11 = nm01 * m.m10 + nm11 * m.m11 + nm21 * m.m12;
        this._m12(nm02 * m.m10 + nm12 * m.m11 + nm22 * m.m12)._m10(m10)._m11(m11)._m13(0f);
        float m20 = nm00 * m.m20 + nm10 * m.m21 + nm20 * m.m22;
        float m21 = nm01 * m.m20 + nm11 * m.m21 + nm21 * m.m22;
        this._m22(nm02 * m.m20 + nm12 * m.m21 + nm22 * m.m22)._m20(m20)._m21(m21)._m23(0f);
        float m30 = nm00 * m.m30 + nm10 * m.m31 + nm20 * m.m32 + tx;
        float m31 = nm01 * m.m30 + nm11 * m.m31 + nm21 * m.m32 + ty;
        this._m32(nm02 * m.m30 + nm12 * m.m31 + nm22 * m.m32 + tz)._m30(m30)._m31(m31)._m33(1f);
        boolean one = Math.absEqualsOne(sx) && Math.absEqualsOne(sy) && Math.absEqualsOne(sz);
        return this._properties(2 | (one && (m.properties & 16) != 0 ? 16 : 0));
    }

    public Matrix4f translationRotateScaleMulAffine(Vector3f translation, Quaternionf quat, Vector3f scale, Matrix4f m) {
        return this.translationRotateScaleMulAffine(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale.x, scale.y, scale.z, m);
    }

    public Matrix4f translationRotate(float tx, float ty, float tz, float qx, float qy, float qz, float qw) {
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
        return this._m00(w2 + x2 - z2 - y2)._m01(xy + zw + zw + xy)._m02(xz - yw + xz - yw)._m10(-zw + xy - zw + xy)._m11(y2 - z2 + w2 - x2)._m12(yz + yz + xw + xw)._m20(yw + xz + xz + yw)._m21(yz + yz - xw - xw)._m22(z2 - y2 - x2 + w2)._m30(tx)._m31(ty)._m32(tz)._m33(1f)._properties(18);
    }

    public Matrix4f translationRotate(float tx, float ty, float tz, Quaternionf quat) {
        return this.translationRotate(tx, ty, tz, quat.x, quat.y, quat.z, quat.w);
    }

    public Matrix4f translationRotate(Vector3f translation, Quaternionf quat) {
        return this.translationRotate(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w);
    }

    public Matrix4f translationRotateInvert(float tx, float ty, float tz, float qx, float qy, float qz, float qw) {
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
        return this._m00(1f - q11 - q22)._m01(q01 + q23)._m02(q02 - q13)._m03(0f)._m10(q01 - q23)._m11(1f - q22 - q00)._m12(q12 + q03)._m13(0f)._m20(q02 + q13)._m21(q12 - q03)._m22(1f - q11 - q00)._m23(0f)._m30(-this.m00 * tx - this.m10 * ty - this.m20 * tz)._m31(-this.m01 * tx - this.m11 * ty - this.m21 * tz)._m32(-this.m02 * tx - this.m12 * ty - this.m22 * tz)._m33(1f)._properties(18);
    }

    public Matrix4f translationRotateInvert(Vector3f translation, Quaternionf quat) {
        return this.translationRotateInvert(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w);
    }

    public Matrix4f set3x3(Matrix3f mat) {
        return this.set3x3Matrix3f(mat)._properties(this.properties & -30);
    }

    private Matrix4f set3x3Matrix3f(Matrix3f mat) {
        return this._m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)._m20(mat.m20)._m21(mat.m21)._m22(mat.m22);
    }

    public Vector4f transform(Vector4f v) {
        return v.mul(this);
    }

    public Vector4f transform(Vector4f v, Vector4f dest) {
        return v.mul(this, dest);
    }

    public Vector4f transform(float x, float y, float z, float w, Vector4f dest) {
        return dest.set(x, y, z, w).mul(this);
    }

    public Vector4f transformTranspose(Vector4f v) {
        return v.mulTranspose(this);
    }

    public Vector4f transformTranspose(Vector4f v, Vector4f dest) {
        return v.mulTranspose(this, dest);
    }

    public Vector4f transformTranspose(float x, float y, float z, float w, Vector4f dest) {
        return dest.set(x, y, z, w).mulTranspose(this);
    }

    public Vector4f transformProject(Vector4f v) {
        return v.mulProject(this);
    }

    public Vector4f transformProject(Vector4f v, Vector4f dest) {
        return v.mulProject(this, dest);
    }

    public Vector4f transformProject(float x, float y, float z, float w, Vector4f dest) {
        return dest.set(x, y, z, w).mulProject(this);
    }

    public Vector3f transformProject(Vector4f v, Vector3f dest) {
        return v.mulProject(this, dest);
    }

    public Vector3f transformProject(float x, float y, float z, float w, Vector3f dest) {
        return dest.set(x, y, z).mulProject(this, w, dest);
    }

    public Vector3f transformProject(Vector3f v) {
        return v.mulProject(this);
    }

    public Vector3f transformProject(Vector3f v, Vector3f dest) {
        return v.mulProject(this, dest);
    }

    public Vector3f transformProject(float x, float y, float z, Vector3f dest) {
        return dest.set(x, y, z).mulProject(this);
    }

    public Vector3f transformPosition(Vector3f v) {
        return v.mulPosition(this);
    }

    public Vector3f transformPosition(Vector3f v, Vector3f dest) {
        return this.transformPosition(v.x, v.y, v.z, dest);
    }

    public Vector3f transformPosition(float x, float y, float z, Vector3f dest) {
        return dest.set(x, y, z).mulPosition(this);
    }

    public Vector3f transformDirection(Vector3f v) {
        return this.transformDirection(v.x, v.y, v.z, v);
    }

    public Vector3f transformDirection(Vector3f v, Vector3f dest) {
        return this.transformDirection(v.x, v.y, v.z, dest);
    }

    public Vector3f transformDirection(float x, float y, float z, Vector3f dest) {
        return dest.set(x, y, z).mulDirection(this);
    }

    public Vector4f transformAffine(Vector4f v) {
        return v.mulAffine(this, v);
    }

    public Vector4f transformAffine(Vector4f v, Vector4f dest) {
        return this.transformAffine(v.x, v.y, v.z, v.w, dest);
    }

    public Vector4f transformAffine(float x, float y, float z, float w, Vector4f dest) {
        return dest.set(x, y, z, w).mulAffine(this, dest);
    }

    public Matrix4f scale(Vector3f xyz, Matrix4f dest) {
        return this.scale(xyz.x, xyz.y, xyz.z, dest);
    }

    public Matrix4f scale(Vector3f xyz) {
        return this.scale(xyz.x, xyz.y, xyz.z, this);
    }

    public Matrix4f scale(float xyz, Matrix4f dest) {
        return this.scale(xyz, xyz, xyz, dest);
    }

    public Matrix4f scale(float xyz) {
        return this.scale(xyz, xyz, xyz);
    }

    public Matrix4f scaleXY(float x, float y, Matrix4f dest) {
        return this.scale(x, y, 1f, dest);
    }

    public Matrix4f scaleXY(float x, float y) {
        return this.scale(x, y, 1f);
    }

    public Matrix4f scale(float x, float y, float z, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.scaling(x, y, z) : this.scaleGeneric(x, y, z, dest);
    }

    private Matrix4f scaleGeneric(float x, float y, float z, Matrix4f dest) {
        boolean one = Math.absEqualsOne(x) && Math.absEqualsOne(y) && Math.absEqualsOne(z);
        return dest._m00(this.m00 * x)._m01(this.m01 * x)._m02(this.m02 * x)._m03(this.m03 * x)._m10(this.m10 * y)._m11(this.m11 * y)._m12(this.m12 * y)._m13(this.m13 * y)._m20(this.m20 * z)._m21(this.m21 * z)._m22(this.m22 * z)._m23(this.m23 * z)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & ~(13 | (one ? 0 : 16)));
    }

    public Matrix4f scale(float x, float y, float z) {
        return this.scale(x, y, z, this);
    }

    public Matrix4f scaleAround(float sx, float sy, float sz, float ox, float oy, float oz, Matrix4f dest) {
        float nm30 = this.m00 * ox + this.m10 * oy + this.m20 * oz + this.m30;
        float nm31 = this.m01 * ox + this.m11 * oy + this.m21 * oz + this.m31;
        float nm32 = this.m02 * ox + this.m12 * oy + this.m22 * oz + this.m32;
        float nm33 = this.m03 * ox + this.m13 * oy + this.m23 * oz + this.m33;
        boolean one = Math.absEqualsOne(sx) && Math.absEqualsOne(sy) && Math.absEqualsOne(sz);
        return dest._m00(this.m00 * sx)._m01(this.m01 * sx)._m02(this.m02 * sx)._m03(this.m03 * sx)._m10(this.m10 * sy)._m11(this.m11 * sy)._m12(this.m12 * sy)._m13(this.m13 * sy)._m20(this.m20 * sz)._m21(this.m21 * sz)._m22(this.m22 * sz)._m23(this.m23 * sz)._m30(-dest.m00 * ox - dest.m10 * oy - dest.m20 * oz + nm30)._m31(-dest.m01 * ox - dest.m11 * oy - dest.m21 * oz + nm31)._m32(-dest.m02 * ox - dest.m12 * oy - dest.m22 * oz + nm32)._m33(-dest.m03 * ox - dest.m13 * oy - dest.m23 * oz + nm33)._properties(this.properties & ~(13 | (one ? 0 : 16)));
    }

    public Matrix4f scaleAround(float sx, float sy, float sz, float ox, float oy, float oz) {
        return this.scaleAround(sx, sy, sz, ox, oy, oz, this);
    }

    public Matrix4f scaleAround(float factor, float ox, float oy, float oz) {
        return this.scaleAround(factor, factor, factor, ox, oy, oz, this);
    }

    public Matrix4f scaleAround(float factor, float ox, float oy, float oz, Matrix4f dest) {
        return this.scaleAround(factor, factor, factor, ox, oy, oz, dest);
    }

    public Matrix4f scaleLocal(float x, float y, float z, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.scaling(x, y, z) : this.scaleLocalGeneric(x, y, z, dest);
    }

    private Matrix4f scaleLocalGeneric(float x, float y, float z, Matrix4f dest) {
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
        boolean one = Math.absEqualsOne(x) && Math.absEqualsOne(y) && Math.absEqualsOne(z);
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(this.m03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(this.m13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(this.m23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(this.m33)._properties(this.properties & ~(13 | (one ? 0 : 16)));
    }

    public Matrix4f scaleLocal(float xyz, Matrix4f dest) {
        return this.scaleLocal(xyz, xyz, xyz, dest);
    }

    public Matrix4f scaleLocal(float xyz) {
        return this.scaleLocal(xyz, this);
    }

    public Matrix4f scaleLocal(float x, float y, float z) {
        return this.scaleLocal(x, y, z, this);
    }

    public Matrix4f scaleAroundLocal(float sx, float sy, float sz, float ox, float oy, float oz, Matrix4f dest) {
        boolean one = Math.absEqualsOne(sx) && Math.absEqualsOne(sy) && Math.absEqualsOne(sz);
        return dest._m00(sx * (this.m00 - ox * this.m03) + ox * this.m03)._m01(sy * (this.m01 - oy * this.m03) + oy * this.m03)._m02(sz * (this.m02 - oz * this.m03) + oz * this.m03)._m03(this.m03)._m10(sx * (this.m10 - ox * this.m13) + ox * this.m13)._m11(sy * (this.m11 - oy * this.m13) + oy * this.m13)._m12(sz * (this.m12 - oz * this.m13) + oz * this.m13)._m13(this.m13)._m20(sx * (this.m20 - ox * this.m23) + ox * this.m23)._m21(sy * (this.m21 - oy * this.m23) + oy * this.m23)._m22(sz * (this.m22 - oz * this.m23) + oz * this.m23)._m23(this.m23)._m30(sx * (this.m30 - ox * this.m33) + ox * this.m33)._m31(sy * (this.m31 - oy * this.m33) + oy * this.m33)._m32(sz * (this.m32 - oz * this.m33) + oz * this.m33)._m33(this.m33)._properties(this.properties & ~(13 | (one ? 0 : 16)));
    }

    public Matrix4f scaleAroundLocal(float sx, float sy, float sz, float ox, float oy, float oz) {
        return this.scaleAroundLocal(sx, sy, sz, ox, oy, oz, this);
    }

    public Matrix4f scaleAroundLocal(float factor, float ox, float oy, float oz) {
        return this.scaleAroundLocal(factor, factor, factor, ox, oy, oz, this);
    }

    public Matrix4f scaleAroundLocal(float factor, float ox, float oy, float oz, Matrix4f dest) {
        return this.scaleAroundLocal(factor, factor, factor, ox, oy, oz, dest);
    }

    public Matrix4f rotateX(float ang, Matrix4f dest) {
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

    private Matrix4f rotateXInternal(float ang, Matrix4f dest) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        float lm10 = this.m10;
        float lm11 = this.m11;
        float lm12 = this.m12;
        float lm13 = this.m13;
        float lm20 = this.m20;
        float lm21 = this.m21;
        float lm22 = this.m22;
        float lm23 = this.m23;
        return dest._m20(Math.fma(lm10, -sin, lm20 * cos))._m21(Math.fma(lm11, -sin, lm21 * cos))._m22(Math.fma(lm12, -sin, lm22 * cos))._m23(Math.fma(lm13, -sin, lm23 * cos))._m10(Math.fma(lm10, cos, lm20 * sin))._m11(Math.fma(lm11, cos, lm21 * sin))._m12(Math.fma(lm12, cos, lm22 * sin))._m13(Math.fma(lm13, cos, lm23 * sin))._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotateX(float ang) {
        return this.rotateX(ang, this);
    }

    public Matrix4f rotateY(float ang, Matrix4f dest) {
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

    private Matrix4f rotateYInternal(float ang, Matrix4f dest) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        float nm00 = Math.fma(this.m00, cos, this.m20 * -sin);
        float nm01 = Math.fma(this.m01, cos, this.m21 * -sin);
        float nm02 = Math.fma(this.m02, cos, this.m22 * -sin);
        float nm03 = Math.fma(this.m03, cos, this.m23 * -sin);
        return dest._m20(Math.fma(this.m00, sin, this.m20 * cos))._m21(Math.fma(this.m01, sin, this.m21 * cos))._m22(Math.fma(this.m02, sin, this.m22 * cos))._m23(Math.fma(this.m03, sin, this.m23 * cos))._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotateY(float ang) {
        return this.rotateY(ang, this);
    }

    public Matrix4f rotateZ(float ang, Matrix4f dest) {
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

    private Matrix4f rotateZInternal(float ang, Matrix4f dest) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        return this.rotateTowardsXY(sin, cos, dest);
    }

    public Matrix4f rotateZ(float ang) {
        return this.rotateZ(ang, this);
    }

    public Matrix4f rotateTowardsXY(float dirX, float dirY) {
        return this.rotateTowardsXY(dirX, dirY, this);
    }

    public Matrix4f rotateTowardsXY(float dirX, float dirY, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationTowardsXY(dirX, dirY);
        } else {
            float nm00 = Math.fma(this.m00, dirY, this.m10 * dirX);
            float nm01 = Math.fma(this.m01, dirY, this.m11 * dirX);
            float nm02 = Math.fma(this.m02, dirY, this.m12 * dirX);
            float nm03 = Math.fma(this.m03, dirY, this.m13 * dirX);
            return dest._m10(Math.fma(this.m00, -dirX, this.m10 * dirY))._m11(Math.fma(this.m01, -dirX, this.m11 * dirY))._m12(Math.fma(this.m02, -dirX, this.m12 * dirY))._m13(Math.fma(this.m03, -dirX, this.m13 * dirY))._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        }
    }

    public Matrix4f rotateXYZ(Vector3f angles) {
        return this.rotateXYZ(angles.x, angles.y, angles.z);
    }

    public Matrix4f rotateXYZ(float angleX, float angleY, float angleZ) {
        return this.rotateXYZ(angleX, angleY, angleZ, this);
    }

    public Matrix4f rotateXYZ(float angleX, float angleY, float angleZ, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationXYZ(angleX, angleY, angleZ);
        } else if ((this.properties & 8) != 0) {
            float tx = this.m30;
            float ty = this.m31;
            float tz = this.m32;
            return dest.rotationXYZ(angleX, angleY, angleZ).setTranslation(tx, ty, tz);
        } else {
            return (this.properties & 2) != 0 ? dest.rotateAffineXYZ(angleX, angleY, angleZ) : this.rotateXYZInternal(angleX, angleY, angleZ, dest);
        }
    }

    private Matrix4f rotateXYZInternal(float angleX, float angleY, float angleZ, Matrix4f dest) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float m_sinX = -sinX;
        float m_sinY = -sinY;
        float m_sinZ = -sinZ;
        float nm10 = Math.fma(this.m10, cosX, this.m20 * sinX);
        float nm11 = Math.fma(this.m11, cosX, this.m21 * sinX);
        float nm12 = Math.fma(this.m12, cosX, this.m22 * sinX);
        float nm13 = Math.fma(this.m13, cosX, this.m23 * sinX);
        float nm20 = Math.fma(this.m10, m_sinX, this.m20 * cosX);
        float nm21 = Math.fma(this.m11, m_sinX, this.m21 * cosX);
        float nm22 = Math.fma(this.m12, m_sinX, this.m22 * cosX);
        float nm23 = Math.fma(this.m13, m_sinX, this.m23 * cosX);
        float nm00 = Math.fma(this.m00, cosY, nm20 * m_sinY);
        float nm01 = Math.fma(this.m01, cosY, nm21 * m_sinY);
        float nm02 = Math.fma(this.m02, cosY, nm22 * m_sinY);
        float nm03 = Math.fma(this.m03, cosY, nm23 * m_sinY);
        return dest._m20(Math.fma(this.m00, sinY, nm20 * cosY))._m21(Math.fma(this.m01, sinY, nm21 * cosY))._m22(Math.fma(this.m02, sinY, nm22 * cosY))._m23(Math.fma(this.m03, sinY, nm23 * cosY))._m00(Math.fma(nm00, cosZ, nm10 * sinZ))._m01(Math.fma(nm01, cosZ, nm11 * sinZ))._m02(Math.fma(nm02, cosZ, nm12 * sinZ))._m03(Math.fma(nm03, cosZ, nm13 * sinZ))._m10(Math.fma(nm00, m_sinZ, nm10 * cosZ))._m11(Math.fma(nm01, m_sinZ, nm11 * cosZ))._m12(Math.fma(nm02, m_sinZ, nm12 * cosZ))._m13(Math.fma(nm03, m_sinZ, nm13 * cosZ))._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotateAffineXYZ(float angleX, float angleY, float angleZ) {
        return this.rotateAffineXYZ(angleX, angleY, angleZ, this);
    }

    public Matrix4f rotateAffineXYZ(float angleX, float angleY, float angleZ, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationXYZ(angleX, angleY, angleZ);
        } else if ((this.properties & 8) != 0) {
            float tx = this.m30;
            float ty = this.m31;
            float tz = this.m32;
            return dest.rotationXYZ(angleX, angleY, angleZ).setTranslation(tx, ty, tz);
        } else {
            return this.rotateAffineXYZInternal(angleX, angleY, angleZ, dest);
        }
    }

    private Matrix4f rotateAffineXYZInternal(float angleX, float angleY, float angleZ, Matrix4f dest) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float m_sinX = -sinX;
        float m_sinY = -sinY;
        float m_sinZ = -sinZ;
        float nm10 = Math.fma(this.m10, cosX, this.m20 * sinX);
        float nm11 = Math.fma(this.m11, cosX, this.m21 * sinX);
        float nm12 = Math.fma(this.m12, cosX, this.m22 * sinX);
        float nm20 = Math.fma(this.m10, m_sinX, this.m20 * cosX);
        float nm21 = Math.fma(this.m11, m_sinX, this.m21 * cosX);
        float nm22 = Math.fma(this.m12, m_sinX, this.m22 * cosX);
        float nm00 = Math.fma(this.m00, cosY, nm20 * m_sinY);
        float nm01 = Math.fma(this.m01, cosY, nm21 * m_sinY);
        float nm02 = Math.fma(this.m02, cosY, nm22 * m_sinY);
        return dest._m20(Math.fma(this.m00, sinY, nm20 * cosY))._m21(Math.fma(this.m01, sinY, nm21 * cosY))._m22(Math.fma(this.m02, sinY, nm22 * cosY))._m23(0f)._m00(Math.fma(nm00, cosZ, nm10 * sinZ))._m01(Math.fma(nm01, cosZ, nm11 * sinZ))._m02(Math.fma(nm02, cosZ, nm12 * sinZ))._m03(0f)._m10(Math.fma(nm00, m_sinZ, nm10 * cosZ))._m11(Math.fma(nm01, m_sinZ, nm11 * cosZ))._m12(Math.fma(nm02, m_sinZ, nm12 * cosZ))._m13(0f)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotateZYX(Vector3f angles) {
        return this.rotateZYX(angles.z, angles.y, angles.x);
    }

    public Matrix4f rotateZYX(float angleZ, float angleY, float angleX) {
        return this.rotateZYX(angleZ, angleY, angleX, this);
    }

    public Matrix4f rotateZYX(float angleZ, float angleY, float angleX, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationZYX(angleZ, angleY, angleX);
        } else if ((this.properties & 8) != 0) {
            float tx = this.m30;
            float ty = this.m31;
            float tz = this.m32;
            return dest.rotationZYX(angleZ, angleY, angleX).setTranslation(tx, ty, tz);
        } else {
            return (this.properties & 2) != 0 ? dest.rotateAffineZYX(angleZ, angleY, angleX) : this.rotateZYXInternal(angleZ, angleY, angleX, dest);
        }
    }

    private Matrix4f rotateZYXInternal(float angleZ, float angleY, float angleX, Matrix4f dest) {
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
        float nm03 = this.m03 * cosZ + this.m13 * sinZ;
        float nm10 = this.m00 * m_sinZ + this.m10 * cosZ;
        float nm11 = this.m01 * m_sinZ + this.m11 * cosZ;
        float nm12 = this.m02 * m_sinZ + this.m12 * cosZ;
        float nm13 = this.m03 * m_sinZ + this.m13 * cosZ;
        float nm20 = nm00 * sinY + this.m20 * cosY;
        float nm21 = nm01 * sinY + this.m21 * cosY;
        float nm22 = nm02 * sinY + this.m22 * cosY;
        float nm23 = nm03 * sinY + this.m23 * cosY;
        return dest._m00(nm00 * cosY + this.m20 * m_sinY)._m01(nm01 * cosY + this.m21 * m_sinY)._m02(nm02 * cosY + this.m22 * m_sinY)._m03(nm03 * cosY + this.m23 * m_sinY)._m10(nm10 * cosX + nm20 * sinX)._m11(nm11 * cosX + nm21 * sinX)._m12(nm12 * cosX + nm22 * sinX)._m13(nm13 * cosX + nm23 * sinX)._m20(nm10 * m_sinX + nm20 * cosX)._m21(nm11 * m_sinX + nm21 * cosX)._m22(nm12 * m_sinX + nm22 * cosX)._m23(nm13 * m_sinX + nm23 * cosX)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotateAffineZYX(float angleZ, float angleY, float angleX) {
        return this.rotateAffineZYX(angleZ, angleY, angleX, this);
    }

    public Matrix4f rotateAffineZYX(float angleZ, float angleY, float angleX, Matrix4f dest) {
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
        return dest._m00(nm00 * cosY + this.m20 * m_sinY)._m01(nm01 * cosY + this.m21 * m_sinY)._m02(nm02 * cosY + this.m22 * m_sinY)._m03(0f)._m10(nm10 * cosX + nm20 * sinX)._m11(nm11 * cosX + nm21 * sinX)._m12(nm12 * cosX + nm22 * sinX)._m13(0f)._m20(nm10 * m_sinX + nm20 * cosX)._m21(nm11 * m_sinX + nm21 * cosX)._m22(nm12 * m_sinX + nm22 * cosX)._m23(0f)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotateYXZ(Vector3f angles) {
        return this.rotateYXZ(angles.y, angles.x, angles.z);
    }

    public Matrix4f rotateYXZ(float angleY, float angleX, float angleZ) {
        return this.rotateYXZ(angleY, angleX, angleZ, this);
    }

    public Matrix4f rotateYXZ(float angleY, float angleX, float angleZ, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationYXZ(angleY, angleX, angleZ);
        } else if ((this.properties & 8) != 0) {
            float tx = this.m30;
            float ty = this.m31;
            float tz = this.m32;
            return dest.rotationYXZ(angleY, angleX, angleZ).setTranslation(tx, ty, tz);
        } else {
            return (this.properties & 2) != 0 ? dest.rotateAffineYXZ(angleY, angleX, angleZ) : this.rotateYXZInternal(angleY, angleX, angleZ, dest);
        }
    }

    private Matrix4f rotateYXZInternal(float angleY, float angleX, float angleZ, Matrix4f dest) {
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
        float nm23 = this.m03 * sinY + this.m23 * cosY;
        float nm00 = this.m00 * cosY + this.m20 * m_sinY;
        float nm01 = this.m01 * cosY + this.m21 * m_sinY;
        float nm02 = this.m02 * cosY + this.m22 * m_sinY;
        float nm03 = this.m03 * cosY + this.m23 * m_sinY;
        float nm10 = this.m10 * cosX + nm20 * sinX;
        float nm11 = this.m11 * cosX + nm21 * sinX;
        float nm12 = this.m12 * cosX + nm22 * sinX;
        float nm13 = this.m13 * cosX + nm23 * sinX;
        return dest._m20(this.m10 * m_sinX + nm20 * cosX)._m21(this.m11 * m_sinX + nm21 * cosX)._m22(this.m12 * m_sinX + nm22 * cosX)._m23(this.m13 * m_sinX + nm23 * cosX)._m00(nm00 * cosZ + nm10 * sinZ)._m01(nm01 * cosZ + nm11 * sinZ)._m02(nm02 * cosZ + nm12 * sinZ)._m03(nm03 * cosZ + nm13 * sinZ)._m10(nm00 * m_sinZ + nm10 * cosZ)._m11(nm01 * m_sinZ + nm11 * cosZ)._m12(nm02 * m_sinZ + nm12 * cosZ)._m13(nm03 * m_sinZ + nm13 * cosZ)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotateAffineYXZ(float angleY, float angleX, float angleZ) {
        return this.rotateAffineYXZ(angleY, angleX, angleZ, this);
    }

    public Matrix4f rotateAffineYXZ(float angleY, float angleX, float angleZ, Matrix4f dest) {
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
        return dest._m20(this.m10 * m_sinX + nm20 * cosX)._m21(this.m11 * m_sinX + nm21 * cosX)._m22(this.m12 * m_sinX + nm22 * cosX)._m23(0f)._m00(nm00 * cosZ + nm10 * sinZ)._m01(nm01 * cosZ + nm11 * sinZ)._m02(nm02 * cosZ + nm12 * sinZ)._m03(0f)._m10(nm00 * m_sinZ + nm10 * cosZ)._m11(nm01 * m_sinZ + nm11 * cosZ)._m12(nm02 * m_sinZ + nm12 * cosZ)._m13(0f)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotate(float ang, float x, float y, float z, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotation(ang, x, y, z);
        } else if ((this.properties & 8) != 0) {
            return this.rotateTranslation(ang, x, y, z, dest);
        } else {
            return (this.properties & 2) != 0 ? this.rotateAffine(ang, x, y, z, dest) : this.rotateGeneric(ang, x, y, z, dest);
        }
    }

    private Matrix4f rotateGeneric(float ang, float x, float y, float z, Matrix4f dest) {
        if (y == 0f && z == 0f && Math.absEqualsOne(x)) {
            return this.rotateX(x * ang, dest);
        } else if (x == 0f && z == 0f && Math.absEqualsOne(y)) {
            return this.rotateY(y * ang, dest);
        } else {
            return x == 0f && y == 0f && Math.absEqualsOne(z) ? this.rotateZ(z * ang, dest) : this.rotateGenericInternal(ang, x, y, z, dest);
        }
    }

    private Matrix4f rotateGenericInternal(float ang, float x, float y, float z, Matrix4f dest) {
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
        float nm03 = this.m03 * rm00 + this.m13 * rm01 + this.m23 * rm02;
        float nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        float nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        float nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        float nm13 = this.m03 * rm10 + this.m13 * rm11 + this.m23 * rm12;
        return dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotate(float ang, float x, float y, float z) {
        return this.rotate(ang, x, y, z, this);
    }

    public Matrix4f rotateTranslation(float ang, float x, float y, float z, Matrix4f dest) {
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

    private Matrix4f rotateTranslationInternal(float ang, float x, float y, float z, Matrix4f dest) {
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
        return dest._m20(rm20)._m21(rm21)._m22(rm22)._m23(0f)._m00(rm00)._m01(rm01)._m02(rm02)._m03(0f)._m10(rm10)._m11(rm11)._m12(rm12)._m13(0f)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(1f)._properties(this.properties & -14);
    }

    public Matrix4f rotateAffine(float ang, float x, float y, float z, Matrix4f dest) {
        if (y == 0f && z == 0f && Math.absEqualsOne(x)) {
            return this.rotateX(x * ang, dest);
        } else if (x == 0f && z == 0f && Math.absEqualsOne(y)) {
            return this.rotateY(y * ang, dest);
        } else {
            return x == 0f && y == 0f && Math.absEqualsOne(z) ? this.rotateZ(z * ang, dest) : this.rotateAffineInternal(ang, x, y, z, dest);
        }
    }

    private Matrix4f rotateAffineInternal(float ang, float x, float y, float z, Matrix4f dest) {
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
        return dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(0f)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0f)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(1f)._properties(this.properties & -14);
    }

    public Matrix4f rotateAffine(float ang, float x, float y, float z) {
        return this.rotateAffine(ang, x, y, z, this);
    }

    public Matrix4f rotateLocal(float ang, float x, float y, float z, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.rotation(ang, x, y, z) : this.rotateLocalGeneric(ang, x, y, z, dest);
    }

    private Matrix4f rotateLocalGeneric(float ang, float x, float y, float z, Matrix4f dest) {
        if (y == 0f && z == 0f && Math.absEqualsOne(x)) {
            return this.rotateLocalX(x * ang, dest);
        } else if (x == 0f && z == 0f && Math.absEqualsOne(y)) {
            return this.rotateLocalY(y * ang, dest);
        } else {
            return x == 0f && y == 0f && Math.absEqualsOne(z) ? this.rotateLocalZ(z * ang, dest) : this.rotateLocalGenericInternal(ang, x, y, z, dest);
        }
    }

    private Matrix4f rotateLocalGenericInternal(float ang, float x, float y, float z, Matrix4f dest) {
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
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(this.m03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(this.m13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(this.m23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotateLocal(float ang, float x, float y, float z) {
        return this.rotateLocal(ang, x, y, z, this);
    }

    public Matrix4f rotateLocalX(float ang, Matrix4f dest) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        float nm02 = sin * this.m01 + cos * this.m02;
        float nm12 = sin * this.m11 + cos * this.m12;
        float nm22 = sin * this.m21 + cos * this.m22;
        float nm32 = sin * this.m31 + cos * this.m32;
        return dest._m00(this.m00)._m01(cos * this.m01 - sin * this.m02)._m02(nm02)._m03(this.m03)._m10(this.m10)._m11(cos * this.m11 - sin * this.m12)._m12(nm12)._m13(this.m13)._m20(this.m20)._m21(cos * this.m21 - sin * this.m22)._m22(nm22)._m23(this.m23)._m30(this.m30)._m31(cos * this.m31 - sin * this.m32)._m32(nm32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotateLocalX(float ang) {
        return this.rotateLocalX(ang, this);
    }

    public Matrix4f rotateLocalY(float ang, Matrix4f dest) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        float nm02 = -sin * this.m00 + cos * this.m02;
        float nm12 = -sin * this.m10 + cos * this.m12;
        float nm22 = -sin * this.m20 + cos * this.m22;
        float nm32 = -sin * this.m30 + cos * this.m32;
        return dest._m00(cos * this.m00 + sin * this.m02)._m01(this.m01)._m02(nm02)._m03(this.m03)._m10(cos * this.m10 + sin * this.m12)._m11(this.m11)._m12(nm12)._m13(this.m13)._m20(cos * this.m20 + sin * this.m22)._m21(this.m21)._m22(nm22)._m23(this.m23)._m30(cos * this.m30 + sin * this.m32)._m31(this.m31)._m32(nm32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotateLocalY(float ang) {
        return this.rotateLocalY(ang, this);
    }

    public Matrix4f rotateLocalZ(float ang, Matrix4f dest) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        float nm01 = sin * this.m00 + cos * this.m01;
        float nm11 = sin * this.m10 + cos * this.m11;
        float nm21 = sin * this.m20 + cos * this.m21;
        float nm31 = sin * this.m30 + cos * this.m31;
        return dest._m00(cos * this.m00 - sin * this.m01)._m01(nm01)._m02(this.m02)._m03(this.m03)._m10(cos * this.m10 - sin * this.m11)._m11(nm11)._m12(this.m12)._m13(this.m13)._m20(cos * this.m20 - sin * this.m21)._m21(nm21)._m22(this.m22)._m23(this.m23)._m30(cos * this.m30 - sin * this.m31)._m31(nm31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotateLocalZ(float ang) {
        return this.rotateLocalZ(ang, this);
    }

    public Matrix4f translate(Vector3f offset) {
        return this.translate(offset.x, offset.y, offset.z);
    }

    public Matrix4f translate(Vector3f offset, Matrix4f dest) {
        return this.translate(offset.x, offset.y, offset.z, dest);
    }

    public Matrix4f translate(float x, float y, float z, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.translation(x, y, z) : this.translateGeneric(x, y, z, dest);
    }

    private Matrix4f translateGeneric(float x, float y, float z, Matrix4f dest) {
        MemUtil.INSTANCE.copy(this, dest);
        return dest._m30(Math.fma(this.m00, x, Math.fma(this.m10, y, Math.fma(this.m20, z, this.m30))))._m31(Math.fma(this.m01, x, Math.fma(this.m11, y, Math.fma(this.m21, z, this.m31))))._m32(Math.fma(this.m02, x, Math.fma(this.m12, y, Math.fma(this.m22, z, this.m32))))._m33(Math.fma(this.m03, x, Math.fma(this.m13, y, Math.fma(this.m23, z, this.m33))))._properties(this.properties & -6);
    }

    public Matrix4f translate(float x, float y, float z) {
        return (this.properties & 4) != 0 ? this.translation(x, y, z) : this.translateGeneric(x, y, z);
    }

    private Matrix4f translateGeneric(float x, float y, float z) {
        return this._m30(Math.fma(this.m00, x, Math.fma(this.m10, y, Math.fma(this.m20, z, this.m30))))._m31(Math.fma(this.m01, x, Math.fma(this.m11, y, Math.fma(this.m21, z, this.m31))))._m32(Math.fma(this.m02, x, Math.fma(this.m12, y, Math.fma(this.m22, z, this.m32))))._m33(Math.fma(this.m03, x, Math.fma(this.m13, y, Math.fma(this.m23, z, this.m33))))._properties(this.properties & -6);
    }

    public Matrix4f translateLocal(Vector3f offset) {
        return this.translateLocal(offset.x, offset.y, offset.z);
    }

    public Matrix4f translateLocal(Vector3f offset, Matrix4f dest) {
        return this.translateLocal(offset.x, offset.y, offset.z, dest);
    }

    public Matrix4f translateLocal(float x, float y, float z, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.translation(x, y, z) : this.translateLocalGeneric(x, y, z, dest);
    }

    private Matrix4f translateLocalGeneric(float x, float y, float z, Matrix4f dest) {
        float nm00 = this.m00 + x * this.m03;
        float nm01 = this.m01 + y * this.m03;
        float nm02 = this.m02 + z * this.m03;
        float nm10 = this.m10 + x * this.m13;
        float nm11 = this.m11 + y * this.m13;
        float nm12 = this.m12 + z * this.m13;
        float nm20 = this.m20 + x * this.m23;
        float nm21 = this.m21 + y * this.m23;
        float nm22 = this.m22 + z * this.m23;
        float nm30 = this.m30 + x * this.m33;
        float nm31 = this.m31 + y * this.m33;
        float nm32 = this.m32 + z * this.m33;
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(this.m03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(this.m13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(this.m23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(this.m33)._properties(this.properties & -6);
    }

    public Matrix4f translateLocal(float x, float y, float z) {
        return this.translateLocal(x, y, z, this);
    }

    public Matrix4f ortho(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.setOrtho(left, right, bottom, top, zNear, zFar, zZeroToOne) : this.orthoGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4f orthoGeneric(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        float rm00 = 2f / (right - left);
        float rm11 = 2f / (top - bottom);
        float rm22 = (zZeroToOne ? 1f : 2f) / (zNear - zFar);
        float rm30 = (left + right) / (left - right);
        float rm31 = (top + bottom) / (bottom - top);
        float rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m23 * rm32 + this.m33)._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m20(this.m20 * rm22)._m21(this.m21 * rm22)._m22(this.m22 * rm22)._m23(this.m23 * rm22)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4f ortho(float left, float right, float bottom, float top, float zNear, float zFar, Matrix4f dest) {
        return this.ortho(left, right, bottom, top, zNear, zFar, false, dest);
    }

    public Matrix4f ortho(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne) {
        return this.ortho(left, right, bottom, top, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4f ortho(float left, float right, float bottom, float top, float zNear, float zFar) {
        return this.ortho(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4f orthoLH(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.setOrthoLH(left, right, bottom, top, zNear, zFar, zZeroToOne) : this.orthoLHGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4f orthoLHGeneric(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        float rm00 = 2f / (right - left);
        float rm11 = 2f / (top - bottom);
        float rm22 = (zZeroToOne ? 1f : 2f) / (zFar - zNear);
        float rm30 = (left + right) / (left - right);
        float rm31 = (top + bottom) / (bottom - top);
        float rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m23 * rm32 + this.m33)._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m20(this.m20 * rm22)._m21(this.m21 * rm22)._m22(this.m22 * rm22)._m23(this.m23 * rm22)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4f orthoLH(float left, float right, float bottom, float top, float zNear, float zFar, Matrix4f dest) {
        return this.orthoLH(left, right, bottom, top, zNear, zFar, false, dest);
    }

    public Matrix4f orthoLH(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne) {
        return this.orthoLH(left, right, bottom, top, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4f orthoLH(float left, float right, float bottom, float top, float zNear, float zFar) {
        return this.orthoLH(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4f setOrtho(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne) {
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        this._m00(2f / (right - left))._m11(2f / (top - bottom))._m22((zZeroToOne ? 1f : 2f) / (zNear - zFar))._m30((right + left) / (left - right))._m31((top + bottom) / (bottom - top))._m32((zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar))._properties(2);
        return this;
    }

    public Matrix4f setOrtho(float left, float right, float bottom, float top, float zNear, float zFar) {
        return this.setOrtho(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4f setOrthoLH(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne) {
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        this._m00(2f / (right - left))._m11(2f / (top - bottom))._m22((zZeroToOne ? 1f : 2f) / (zFar - zNear))._m30((right + left) / (left - right))._m31((top + bottom) / (bottom - top))._m32((zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar))._properties(2);
        return this;
    }

    public Matrix4f setOrthoLH(float left, float right, float bottom, float top, float zNear, float zFar) {
        return this.setOrthoLH(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4f orthoSymmetric(float width, float height, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.setOrthoSymmetric(width, height, zNear, zFar, zZeroToOne) : this.orthoSymmetricGeneric(width, height, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4f orthoSymmetricGeneric(float width, float height, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        float rm00 = 2f / width;
        float rm11 = 2f / height;
        float rm22 = (zZeroToOne ? 1f : 2f) / (zNear - zFar);
        float rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        dest._m30(this.m20 * rm32 + this.m30)._m31(this.m21 * rm32 + this.m31)._m32(this.m22 * rm32 + this.m32)._m33(this.m23 * rm32 + this.m33)._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m20(this.m20 * rm22)._m21(this.m21 * rm22)._m22(this.m22 * rm22)._m23(this.m23 * rm22)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4f orthoSymmetric(float width, float height, float zNear, float zFar, Matrix4f dest) {
        return this.orthoSymmetric(width, height, zNear, zFar, false, dest);
    }

    public Matrix4f orthoSymmetric(float width, float height, float zNear, float zFar, boolean zZeroToOne) {
        return this.orthoSymmetric(width, height, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4f orthoSymmetric(float width, float height, float zNear, float zFar) {
        return this.orthoSymmetric(width, height, zNear, zFar, false, this);
    }

    public Matrix4f orthoSymmetricLH(float width, float height, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.setOrthoSymmetricLH(width, height, zNear, zFar, zZeroToOne) : this.orthoSymmetricLHGeneric(width, height, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4f orthoSymmetricLHGeneric(float width, float height, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        float rm00 = 2f / width;
        float rm11 = 2f / height;
        float rm22 = (zZeroToOne ? 1f : 2f) / (zFar - zNear);
        float rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        dest._m30(this.m20 * rm32 + this.m30)._m31(this.m21 * rm32 + this.m31)._m32(this.m22 * rm32 + this.m32)._m33(this.m23 * rm32 + this.m33)._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m20(this.m20 * rm22)._m21(this.m21 * rm22)._m22(this.m22 * rm22)._m23(this.m23 * rm22)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4f orthoSymmetricLH(float width, float height, float zNear, float zFar, Matrix4f dest) {
        return this.orthoSymmetricLH(width, height, zNear, zFar, false, dest);
    }

    public Matrix4f orthoSymmetricLH(float width, float height, float zNear, float zFar, boolean zZeroToOne) {
        return this.orthoSymmetricLH(width, height, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4f orthoSymmetricLH(float width, float height, float zNear, float zFar) {
        return this.orthoSymmetricLH(width, height, zNear, zFar, false, this);
    }

    public Matrix4f setOrthoSymmetric(float width, float height, float zNear, float zFar, boolean zZeroToOne) {
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        this._m00(2f / width)._m11(2f / height)._m22((zZeroToOne ? 1f : 2f) / (zNear - zFar))._m32((zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar))._properties(2);
        return this;
    }

    public Matrix4f setOrthoSymmetric(float width, float height, float zNear, float zFar) {
        return this.setOrthoSymmetric(width, height, zNear, zFar, false);
    }

    public Matrix4f setOrthoSymmetricLH(float width, float height, float zNear, float zFar, boolean zZeroToOne) {
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        this._m00(2f / width)._m11(2f / height)._m22((zZeroToOne ? 1f : 2f) / (zFar - zNear))._m32((zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar))._properties(2);
        return this;
    }

    public Matrix4f setOrthoSymmetricLH(float width, float height, float zNear, float zFar) {
        return this.setOrthoSymmetricLH(width, height, zNear, zFar, false);
    }

    public Matrix4f ortho2D(float left, float right, float bottom, float top, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.setOrtho2D(left, right, bottom, top) : this.ortho2DGeneric(left, right, bottom, top, dest);
    }

    private Matrix4f ortho2DGeneric(float left, float right, float bottom, float top, Matrix4f dest) {
        float rm00 = 2f / (right - left);
        float rm11 = 2f / (top - bottom);
        float rm30 = (right + left) / (left - right);
        float rm31 = (top + bottom) / (bottom - top);
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m32)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m33)._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(-this.m23)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4f ortho2D(float left, float right, float bottom, float top) {
        return this.ortho2D(left, right, bottom, top, this);
    }

    public Matrix4f ortho2DLH(float left, float right, float bottom, float top, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.setOrtho2DLH(left, right, bottom, top) : this.ortho2DLHGeneric(left, right, bottom, top, dest);
    }

    private Matrix4f ortho2DLHGeneric(float left, float right, float bottom, float top, Matrix4f dest) {
        float rm00 = 2f / (right - left);
        float rm11 = 2f / (top - bottom);
        float rm30 = (right + left) / (left - right);
        float rm31 = (top + bottom) / (bottom - top);
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m32)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m33)._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4f ortho2DLH(float left, float right, float bottom, float top) {
        return this.ortho2DLH(left, right, bottom, top, this);
    }

    public Matrix4f setOrtho2D(float left, float right, float bottom, float top) {
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        this._m00(2f / (right - left))._m11(2f / (top - bottom))._m22(-1f)._m30((right + left) / (left - right))._m31((top + bottom) / (bottom - top))._properties(2);
        return this;
    }

    public Matrix4f setOrtho2DLH(float left, float right, float bottom, float top) {
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        this._m00(2f / (right - left))._m11(2f / (top - bottom))._m30((right + left) / (left - right))._m31((top + bottom) / (bottom - top))._properties(2);
        return this;
    }

    public Matrix4f lookAlong(Vector3f dir, Vector3f up) {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this);
    }

    public Matrix4f lookAlong(Vector3f dir, Vector3f up, Matrix4f dest) {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dest);
    }

    public Matrix4f lookAlong(float dirX, float dirY, float dirZ, float upX, float upY, float upZ, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.setLookAlong(dirX, dirY, dirZ, upX, upY, upZ) : this.lookAlongGeneric(dirX, dirY, dirZ, upX, upY, upZ, dest);
    }

    private Matrix4f lookAlongGeneric(float dirX, float dirY, float dirZ, float upX, float upY, float upZ, Matrix4f dest) {
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
        float nm03 = this.m03 * leftX + this.m13 * upnX + this.m23 * dirX;
        float nm10 = this.m00 * leftY + this.m10 * upnY + this.m20 * dirY;
        float nm11 = this.m01 * leftY + this.m11 * upnY + this.m21 * dirY;
        float nm12 = this.m02 * leftY + this.m12 * upnY + this.m22 * dirY;
        float nm13 = this.m03 * leftY + this.m13 * upnY + this.m23 * dirY;
        return dest._m20(this.m00 * leftZ + this.m10 * upnZ + this.m20 * dirZ)._m21(this.m01 * leftZ + this.m11 * upnZ + this.m21 * dirZ)._m22(this.m02 * leftZ + this.m12 * upnZ + this.m22 * dirZ)._m23(this.m03 * leftZ + this.m13 * upnZ + this.m23 * dirZ)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f lookAlong(float dirX, float dirY, float dirZ, float upX, float upY, float upZ) {
        return this.lookAlong(dirX, dirY, dirZ, upX, upY, upZ, this);
    }

    public Matrix4f setLookAlong(Vector3f dir, Vector3f up) {
        return this.setLookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    public Matrix4f setLookAlong(float dirX, float dirY, float dirZ, float upX, float upY, float upZ) {
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
        this._m00(leftX)._m01(dirY * leftZ - dirZ * leftY)._m02(dirX)._m03(0f)._m10(leftY)._m11(dirZ * leftX - dirX * leftZ)._m12(dirY)._m13(0f)._m20(leftZ)._m21(dirX * leftY - dirY * leftX)._m22(dirZ)._m23(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)._properties(18);
        return this;
    }

    public Matrix4f setLookAt(Vector3f eye, Vector3f center, Vector3f up) {
        return this.setLookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z);
    }

    public Matrix4f setLookAt(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
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
        return this._m00(leftX)._m01(upnX)._m02(dirX)._m03(0f)._m10(leftY)._m11(upnY)._m12(dirY)._m13(0f)._m20(leftZ)._m21(upnZ)._m22(dirZ)._m23(0f)._m30(-(leftX * eyeX + leftY * eyeY + leftZ * eyeZ))._m31(-(upnX * eyeX + upnY * eyeY + upnZ * eyeZ))._m32(-(dirX * eyeX + dirY * eyeY + dirZ * eyeZ))._m33(1f)._properties(18);
    }

    public Matrix4f lookAt(Vector3f eye, Vector3f center, Vector3f up, Matrix4f dest) {
        return this.lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dest);
    }

    public Matrix4f lookAt(Vector3f eye, Vector3f center, Vector3f up) {
        return this.lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, this);
    }

    public Matrix4f lookAt(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.setLookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        } else {
            return (this.properties & 1) != 0 ? this.lookAtPerspective(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dest) : this.lookAtGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dest);
        }
    }

    private Matrix4f lookAtGeneric(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ, Matrix4f dest) {
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
        float nm00 = this.m00 * leftX + this.m10 * upnX + this.m20 * dirX;
        float nm01 = this.m01 * leftX + this.m11 * upnX + this.m21 * dirX;
        float nm02 = this.m02 * leftX + this.m12 * upnX + this.m22 * dirX;
        float nm03 = this.m03 * leftX + this.m13 * upnX + this.m23 * dirX;
        float nm10 = this.m00 * leftY + this.m10 * upnY + this.m20 * dirY;
        float nm11 = this.m01 * leftY + this.m11 * upnY + this.m21 * dirY;
        float nm12 = this.m02 * leftY + this.m12 * upnY + this.m22 * dirY;
        float nm13 = this.m03 * leftY + this.m13 * upnY + this.m23 * dirY;
        return dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m23 * rm32 + this.m33)._m20(this.m00 * leftZ + this.m10 * upnZ + this.m20 * dirZ)._m21(this.m01 * leftZ + this.m11 * upnZ + this.m21 * dirZ)._m22(this.m02 * leftZ + this.m12 * upnZ + this.m22 * dirZ)._m23(this.m03 * leftZ + this.m13 * upnZ + this.m23 * dirZ)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._properties(this.properties & -14);
    }

    public Matrix4f lookAtPerspective(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ, Matrix4f dest) {
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
        float nm10 = this.m00 * leftY;
        float nm20 = this.m00 * leftZ;
        float nm21 = this.m11 * upnZ;
        float nm30 = this.m00 * rm30;
        float nm31 = this.m11 * rm31;
        float nm32 = this.m22 * rm32 + this.m32;
        float nm33 = this.m23 * rm32;
        return dest._m00(this.m00 * leftX)._m01(this.m11 * upnX)._m02(this.m22 * dirX)._m03(this.m23 * dirX)._m10(nm10)._m11(this.m11 * upnY)._m12(this.m22 * dirY)._m13(this.m23 * dirY)._m20(nm20)._m21(nm21)._m22(this.m22 * dirZ)._m23(this.m23 * dirZ)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
    }

    public Matrix4f lookAt(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        return this.lookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, this);
    }

    public Matrix4f setLookAtLH(Vector3f eye, Vector3f center, Vector3f up) {
        return this.setLookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z);
    }

    public Matrix4f setLookAtLH(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
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
        this._m00(leftX)._m01(upnX)._m02(dirX)._m03(0f)._m10(leftY)._m11(upnY)._m12(dirY)._m13(0f)._m20(leftZ)._m21(upnZ)._m22(dirZ)._m23(0f)._m30(-(leftX * eyeX + leftY * eyeY + leftZ * eyeZ))._m31(-(upnX * eyeX + upnY * eyeY + upnZ * eyeZ))._m32(-(dirX * eyeX + dirY * eyeY + dirZ * eyeZ))._m33(1f)._properties(18);
        return this;
    }

    public Matrix4f lookAtLH(Vector3f eye, Vector3f center, Vector3f up, Matrix4f dest) {
        return this.lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dest);
    }

    public Matrix4f lookAtLH(Vector3f eye, Vector3f center, Vector3f up) {
        return this.lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, this);
    }

    public Matrix4f lookAtLH(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.setLookAtLH(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        } else {
            return (this.properties & 1) != 0 ? this.lookAtPerspectiveLH(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dest) : this.lookAtLHGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dest);
        }
    }

    private Matrix4f lookAtLHGeneric(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ, Matrix4f dest) {
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
        float nm00 = this.m00 * leftX + this.m10 * upnX + this.m20 * dirX;
        float nm01 = this.m01 * leftX + this.m11 * upnX + this.m21 * dirX;
        float nm02 = this.m02 * leftX + this.m12 * upnX + this.m22 * dirX;
        float nm03 = this.m03 * leftX + this.m13 * upnX + this.m23 * dirX;
        float nm10 = this.m00 * leftY + this.m10 * upnY + this.m20 * dirY;
        float nm11 = this.m01 * leftY + this.m11 * upnY + this.m21 * dirY;
        float nm12 = this.m02 * leftY + this.m12 * upnY + this.m22 * dirY;
        float nm13 = this.m03 * leftY + this.m13 * upnY + this.m23 * dirY;
        return dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m23 * rm32 + this.m33)._m20(this.m00 * leftZ + this.m10 * upnZ + this.m20 * dirZ)._m21(this.m01 * leftZ + this.m11 * upnZ + this.m21 * dirZ)._m22(this.m02 * leftZ + this.m12 * upnZ + this.m22 * dirZ)._m23(this.m03 * leftZ + this.m13 * upnZ + this.m23 * dirZ)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._properties(this.properties & -14);
    }

    public Matrix4f lookAtLH(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        return this.lookAtLH(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, this);
    }

    public Matrix4f lookAtPerspectiveLH(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ, Matrix4f dest) {
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
        float nm00 = this.m00 * leftX;
        float nm01 = this.m11 * upnX;
        float nm02 = this.m22 * dirX;
        float nm03 = this.m23 * dirX;
        float nm10 = this.m00 * leftY;
        float nm11 = this.m11 * upnY;
        float nm12 = this.m22 * dirY;
        float nm13 = this.m23 * dirY;
        float nm20 = this.m00 * leftZ;
        float nm21 = this.m11 * upnZ;
        float nm22 = this.m22 * dirZ;
        float nm23 = this.m23 * dirZ;
        float nm30 = this.m00 * rm30;
        float nm31 = this.m11 * rm31;
        float nm32 = this.m22 * rm32 + this.m32;
        float nm33 = this.m23 * rm32;
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
    }

    public Matrix4f tile(int x, int y, int w, int h) {
        return this.tile(x, y, w, h, this);
    }

    public Matrix4f tile(int x, int y, int w, int h, Matrix4f dest) {
        float tx = (float)(w - 1 - (x << 1));
        float ty = (float)(h - 1 - (y << 1));
        return dest._m30(Math.fma(this.m00, tx, Math.fma(this.m10, ty, this.m30)))._m31(Math.fma(this.m01, tx, Math.fma(this.m11, ty, this.m31)))._m32(Math.fma(this.m02, tx, Math.fma(this.m12, ty, this.m32)))._m33(Math.fma(this.m03, tx, Math.fma(this.m13, ty, this.m33)))._m00(this.m00 * (float)w)._m01(this.m01 * (float)w)._m02(this.m02 * (float)w)._m03(this.m03 * (float)w)._m10(this.m10 * (float)h)._m11(this.m11 * (float)h)._m12(this.m12 * (float)h)._m13(this.m13 * (float)h)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._properties(this.properties & -30);
    }

    public Matrix4f perspective(float fovy, float aspect, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.setPerspective(fovy, aspect, zNear, zFar, zZeroToOne) : this.perspectiveGeneric(fovy, aspect, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4f perspectiveGeneric(float fovy, float aspect, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        float h = Math.tan(fovy * 0.5F);
        float rm00 = 1f / (h * aspect);
        float rm11 = 1f / h;
        boolean farInf = zFar > 0f && Float.isInfinite(zFar);
        boolean nearInf = zNear > 0f && Float.isInfinite(zNear);
        float rm22;
        float rm32;
        float e;
        if (farInf) {
            e = 1.0E-6F;
            rm22 = e - 1f;
            rm32 = (e - (zZeroToOne ? 1f : 2f)) * zNear;
        } else if (nearInf) {
            e = 1.0E-6F;
            rm22 = (zZeroToOne ? 0f : 1f) - e;
            rm32 = ((zZeroToOne ? 1f : 2f) - e) * zFar;
        } else {
            rm22 = (zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar);
            rm32 = (zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar);
        }

        e = this.m20 * rm22 - this.m30;
        float nm21 = this.m21 * rm22 - this.m31;
        float nm22 = this.m22 * rm22 - this.m32;
        float nm23 = this.m23 * rm22 - this.m33;
        dest._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m30(this.m20 * rm32)._m31(this.m21 * rm32)._m32(this.m22 * rm32)._m33(this.m23 * rm32)._m20(e)._m21(nm21)._m22(nm22)._m23(nm23)._properties(this.properties & -31);
        return dest;
    }

    public Matrix4f perspective(float fovy, float aspect, float zNear, float zFar, Matrix4f dest) {
        return this.perspective(fovy, aspect, zNear, zFar, false, dest);
    }

    public Matrix4f perspective(float fovy, float aspect, float zNear, float zFar, boolean zZeroToOne) {
        return this.perspective(fovy, aspect, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4f perspective(float fovy, float aspect, float zNear, float zFar) {
        return this.perspective(fovy, aspect, zNear, zFar, this);
    }

    public Matrix4f perspectiveRect(float width, float height, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.setPerspectiveRect(width, height, zNear, zFar, zZeroToOne) : this.perspectiveRectGeneric(width, height, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4f perspectiveRectGeneric(float width, float height, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        float rm00 = (zNear + zNear) / width;
        float rm11 = (zNear + zNear) / height;
        boolean farInf = zFar > 0f && Float.isInfinite(zFar);
        boolean nearInf = zNear > 0f && Float.isInfinite(zNear);
        float rm22;
        float rm32;
        float e;
        if (farInf) {
            e = 1.0E-6F;
            rm22 = e - 1f;
            rm32 = (e - (zZeroToOne ? 1f : 2f)) * zNear;
        } else if (nearInf) {
            e = 1.0E-6F;
            rm22 = (zZeroToOne ? 0f : 1f) - e;
            rm32 = ((zZeroToOne ? 1f : 2f) - e) * zFar;
        } else {
            rm22 = (zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar);
            rm32 = (zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar);
        }

        e = this.m20 * rm22 - this.m30;
        float nm21 = this.m21 * rm22 - this.m31;
        float nm22 = this.m22 * rm22 - this.m32;
        float nm23 = this.m23 * rm22 - this.m33;
        dest._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m30(this.m20 * rm32)._m31(this.m21 * rm32)._m32(this.m22 * rm32)._m33(this.m23 * rm32)._m20(e)._m21(nm21)._m22(nm22)._m23(nm23)._properties(this.properties & -31);
        return dest;
    }

    public Matrix4f perspectiveRect(float width, float height, float zNear, float zFar, Matrix4f dest) {
        return this.perspectiveRect(width, height, zNear, zFar, false, dest);
    }

    public Matrix4f perspectiveRect(float width, float height, float zNear, float zFar, boolean zZeroToOne) {
        return this.perspectiveRect(width, height, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4f perspectiveRect(float width, float height, float zNear, float zFar) {
        return this.perspectiveRect(width, height, zNear, zFar, this);
    }

    public Matrix4f perspectiveOffCenter(float fovy, float offAngleX, float offAngleY, float aspect, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.setPerspectiveOffCenter(fovy, offAngleX, offAngleY, aspect, zNear, zFar, zZeroToOne) : this.perspectiveOffCenterGeneric(fovy, offAngleX, offAngleY, aspect, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4f perspectiveOffCenterGeneric(float fovy, float offAngleX, float offAngleY, float aspect, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        float h = Math.tan(fovy * 0.5F);
        float xScale = 1f / (h * aspect);
        float yScale = 1f / h;
        float offX = Math.tan(offAngleX);
        float offY = Math.tan(offAngleY);
        float rm20 = offX * xScale;
        float rm21 = offY * yScale;
        boolean farInf = zFar > 0f && Float.isInfinite(zFar);
        boolean nearInf = zNear > 0f && Float.isInfinite(zNear);
        float rm22;
        float rm32;
        float e;
        if (farInf) {
            e = 1.0E-6F;
            rm22 = e - 1f;
            rm32 = (e - (zZeroToOne ? 1f : 2f)) * zNear;
        } else if (nearInf) {
            e = 1.0E-6F;
            rm22 = (zZeroToOne ? 0f : 1f) - e;
            rm32 = ((zZeroToOne ? 1f : 2f) - e) * zFar;
        } else {
            rm22 = (zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar);
            rm32 = (zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar);
        }

        e = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22 - this.m30;
        float nm21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22 - this.m31;
        float nm22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22 - this.m32;
        float nm23 = this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22 - this.m33;
        dest._m00(this.m00 * xScale)._m01(this.m01 * xScale)._m02(this.m02 * xScale)._m03(this.m03 * xScale)._m10(this.m10 * yScale)._m11(this.m11 * yScale)._m12(this.m12 * yScale)._m13(this.m13 * yScale)._m30(this.m20 * rm32)._m31(this.m21 * rm32)._m32(this.m22 * rm32)._m33(this.m23 * rm32)._m20(e)._m21(nm21)._m22(nm22)._m23(nm23)._properties(this.properties & ~(30 | (rm20 == 0f && rm21 == 0f ? 0 : 1)));
        return dest;
    }

    public Matrix4f perspectiveOffCenter(float fovy, float offAngleX, float offAngleY, float aspect, float zNear, float zFar, Matrix4f dest) {
        return this.perspectiveOffCenter(fovy, offAngleX, offAngleY, aspect, zNear, zFar, false, dest);
    }

    public Matrix4f perspectiveOffCenter(float fovy, float offAngleX, float offAngleY, float aspect, float zNear, float zFar, boolean zZeroToOne) {
        return this.perspectiveOffCenter(fovy, offAngleX, offAngleY, aspect, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4f perspectiveOffCenter(float fovy, float offAngleX, float offAngleY, float aspect, float zNear, float zFar) {
        return this.perspectiveOffCenter(fovy, offAngleX, offAngleY, aspect, zNear, zFar, this);
    }

    public Matrix4f perspectiveOffCenterFov(float angleLeft, float angleRight, float angleDown, float angleUp, float zNear, float zFar, boolean zZeroToOne) {
        return this.perspectiveOffCenterFov(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4f perspectiveOffCenterFov(float angleLeft, float angleRight, float angleDown, float angleUp, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        return this.frustum(Math.tan(angleLeft) * zNear, Math.tan(angleRight) * zNear, Math.tan(angleDown) * zNear, Math.tan(angleUp) * zNear, zNear, zFar, zZeroToOne, dest);
    }

    public Matrix4f perspectiveOffCenterFov(float angleLeft, float angleRight, float angleDown, float angleUp, float zNear, float zFar) {
        return this.perspectiveOffCenterFov(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, this);
    }

    public Matrix4f perspectiveOffCenterFov(float angleLeft, float angleRight, float angleDown, float angleUp, float zNear, float zFar, Matrix4f dest) {
        return this.frustum(Math.tan(angleLeft) * zNear, Math.tan(angleRight) * zNear, Math.tan(angleDown) * zNear, Math.tan(angleUp) * zNear, zNear, zFar, dest);
    }

    public Matrix4f perspectiveOffCenterFovLH(float angleLeft, float angleRight, float angleDown, float angleUp, float zNear, float zFar, boolean zZeroToOne) {
        return this.perspectiveOffCenterFovLH(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4f perspectiveOffCenterFovLH(float angleLeft, float angleRight, float angleDown, float angleUp, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        return this.frustumLH(Math.tan(angleLeft) * zNear, Math.tan(angleRight) * zNear, Math.tan(angleDown) * zNear, Math.tan(angleUp) * zNear, zNear, zFar, zZeroToOne, dest);
    }

    public Matrix4f perspectiveOffCenterFovLH(float angleLeft, float angleRight, float angleDown, float angleUp, float zNear, float zFar) {
        return this.perspectiveOffCenterFovLH(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, this);
    }

    public Matrix4f perspectiveOffCenterFovLH(float angleLeft, float angleRight, float angleDown, float angleUp, float zNear, float zFar, Matrix4f dest) {
        return this.frustumLH(Math.tan(angleLeft) * zNear, Math.tan(angleRight) * zNear, Math.tan(angleDown) * zNear, Math.tan(angleUp) * zNear, zNear, zFar, dest);
    }

    public Matrix4f setPerspective(float fovy, float aspect, float zNear, float zFar, boolean zZeroToOne) {
        MemUtil.INSTANCE.zero(this);
        float h = Math.tan(fovy * 0.5F);
        this._m00(1f / (h * aspect))._m11(1f / h);
        boolean farInf = zFar > 0f && Float.isInfinite(zFar);
        boolean nearInf = zNear > 0f && Float.isInfinite(zNear);
        float e;
        if (farInf) {
            e = 1.0E-6F;
            this._m22(e - 1f)._m32((e - (zZeroToOne ? 1f : 2f)) * zNear);
        } else if (nearInf) {
            e = 1.0E-6F;
            this._m22((zZeroToOne ? 0f : 1f) - e)._m32(((zZeroToOne ? 1f : 2f) - e) * zFar);
        } else {
            this._m22((zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar))._m32((zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar));
        }

        return this._m23(-1f)._properties(1);
    }

    public Matrix4f setPerspective(float fovy, float aspect, float zNear, float zFar) {
        return this.setPerspective(fovy, aspect, zNear, zFar, false);
    }

    public Matrix4f setPerspectiveRect(float width, float height, float zNear, float zFar, boolean zZeroToOne) {
        MemUtil.INSTANCE.zero(this);
        this._m00((zNear + zNear) / width)._m11((zNear + zNear) / height);
        boolean farInf = zFar > 0f && Float.isInfinite(zFar);
        boolean nearInf = zNear > 0f && Float.isInfinite(zNear);
        float e;
        if (farInf) {
            e = 1.0E-6F;
            this._m22(e - 1f)._m32((e - (zZeroToOne ? 1f : 2f)) * zNear);
        } else if (nearInf) {
            e = 1.0E-6F;
            this._m22((zZeroToOne ? 0f : 1f) - e)._m32(((zZeroToOne ? 1f : 2f) - e) * zFar);
        } else {
            this._m22((zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar))._m32((zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar));
        }

        this._m23(-1f)._properties(1);
        return this;
    }

    public Matrix4f setPerspectiveRect(float width, float height, float zNear, float zFar) {
        return this.setPerspectiveRect(width, height, zNear, zFar, false);
    }

    public Matrix4f setPerspectiveOffCenter(float fovy, float offAngleX, float offAngleY, float aspect, float zNear, float zFar) {
        return this.setPerspectiveOffCenter(fovy, offAngleX, offAngleY, aspect, zNear, zFar, false);
    }

    public Matrix4f setPerspectiveOffCenter(float fovy, float offAngleX, float offAngleY, float aspect, float zNear, float zFar, boolean zZeroToOne) {
        MemUtil.INSTANCE.zero(this);
        float h = Math.tan(fovy * 0.5F);
        float xScale = 1f / (h * aspect);
        float yScale = 1f / h;
        float offX = Math.tan(offAngleX);
        float offY = Math.tan(offAngleY);
        this._m00(xScale)._m11(yScale)._m20(offX * xScale)._m21(offY * yScale);
        boolean farInf = zFar > 0f && Float.isInfinite(zFar);
        boolean nearInf = zNear > 0f && Float.isInfinite(zNear);
        float e;
        if (farInf) {
            e = 1.0E-6F;
            this._m22(e - 1f)._m32((e - (zZeroToOne ? 1f : 2f)) * zNear);
        } else if (nearInf) {
            e = 1.0E-6F;
            this._m22((zZeroToOne ? 0f : 1f) - e)._m32(((zZeroToOne ? 1f : 2f) - e) * zFar);
        } else {
            this._m22((zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar))._m32((zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar));
        }

        this._m23(-1f)._properties(offAngleX == 0f && offAngleY == 0f ? 1 : 0);
        return this;
    }

    public Matrix4f setPerspectiveOffCenterFov(float angleLeft, float angleRight, float angleDown, float angleUp, float zNear, float zFar) {
        return this.setPerspectiveOffCenterFov(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, false);
    }

    public Matrix4f setPerspectiveOffCenterFov(float angleLeft, float angleRight, float angleDown, float angleUp, float zNear, float zFar, boolean zZeroToOne) {
        return this.setFrustum(Math.tan(angleLeft) * zNear, Math.tan(angleRight) * zNear, Math.tan(angleDown) * zNear, Math.tan(angleUp) * zNear, zNear, zFar, zZeroToOne);
    }

    public Matrix4f setPerspectiveOffCenterFovLH(float angleLeft, float angleRight, float angleDown, float angleUp, float zNear, float zFar) {
        return this.setPerspectiveOffCenterFovLH(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, false);
    }

    public Matrix4f setPerspectiveOffCenterFovLH(float angleLeft, float angleRight, float angleDown, float angleUp, float zNear, float zFar, boolean zZeroToOne) {
        return this.setFrustumLH(Math.tan(angleLeft) * zNear, Math.tan(angleRight) * zNear, Math.tan(angleDown) * zNear, Math.tan(angleUp) * zNear, zNear, zFar, zZeroToOne);
    }

    public Matrix4f perspectiveLH(float fovy, float aspect, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.setPerspectiveLH(fovy, aspect, zNear, zFar, zZeroToOne) : this.perspectiveLHGeneric(fovy, aspect, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4f perspectiveLHGeneric(float fovy, float aspect, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        float h = Math.tan(fovy * 0.5F);
        float rm00 = 1f / (h * aspect);
        float rm11 = 1f / h;
        boolean farInf = zFar > 0f && Float.isInfinite(zFar);
        boolean nearInf = zNear > 0f && Float.isInfinite(zNear);
        float rm22;
        float rm32;
        float e;
        if (farInf) {
            e = 1.0E-6F;
            rm22 = 1f - e;
            rm32 = (e - (zZeroToOne ? 1f : 2f)) * zNear;
        } else if (nearInf) {
            e = 1.0E-6F;
            rm22 = (zZeroToOne ? 0f : 1f) - e;
            rm32 = ((zZeroToOne ? 1f : 2f) - e) * zFar;
        } else {
            rm22 = (zZeroToOne ? zFar : zFar + zNear) / (zFar - zNear);
            rm32 = (zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar);
        }

        e = this.m20 * rm22 + this.m30;
        float nm21 = this.m21 * rm22 + this.m31;
        float nm22 = this.m22 * rm22 + this.m32;
        float nm23 = this.m23 * rm22 + this.m33;
        dest._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m30(this.m20 * rm32)._m31(this.m21 * rm32)._m32(this.m22 * rm32)._m33(this.m23 * rm32)._m20(e)._m21(nm21)._m22(nm22)._m23(nm23)._properties(this.properties & -31);
        return dest;
    }

    public Matrix4f perspectiveLH(float fovy, float aspect, float zNear, float zFar, boolean zZeroToOne) {
        return this.perspectiveLH(fovy, aspect, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4f perspectiveLH(float fovy, float aspect, float zNear, float zFar, Matrix4f dest) {
        return this.perspectiveLH(fovy, aspect, zNear, zFar, false, dest);
    }

    public Matrix4f perspectiveLH(float fovy, float aspect, float zNear, float zFar) {
        return this.perspectiveLH(fovy, aspect, zNear, zFar, this);
    }

    public Matrix4f setPerspectiveLH(float fovy, float aspect, float zNear, float zFar, boolean zZeroToOne) {
        MemUtil.INSTANCE.zero(this);
        float h = Math.tan(fovy * 0.5F);
        this._m00(1f / (h * aspect))._m11(1f / h);
        boolean farInf = zFar > 0f && Float.isInfinite(zFar);
        boolean nearInf = zNear > 0f && Float.isInfinite(zNear);
        float e;
        if (farInf) {
            e = 1.0E-6F;
            this._m22(1f - e)._m32((e - (zZeroToOne ? 1f : 2f)) * zNear);
        } else if (nearInf) {
            e = 1.0E-6F;
            this._m22((zZeroToOne ? 0f : 1f) - e)._m32(((zZeroToOne ? 1f : 2f) - e) * zFar);
        } else {
            this._m22((zZeroToOne ? zFar : zFar + zNear) / (zFar - zNear))._m32((zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar));
        }

        this._m23(1f)._properties(1);
        return this;
    }

    public Matrix4f setPerspectiveLH(float fovy, float aspect, float zNear, float zFar) {
        return this.setPerspectiveLH(fovy, aspect, zNear, zFar, false);
    }

    public Matrix4f frustum(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.setFrustum(left, right, bottom, top, zNear, zFar, zZeroToOne) : this.frustumGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4f frustumGeneric(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        float rm00 = (zNear + zNear) / (right - left);
        float rm11 = (zNear + zNear) / (top - bottom);
        float rm20 = (right + left) / (right - left);
        float rm21 = (top + bottom) / (top - bottom);
        boolean farInf = zFar > 0f && Float.isInfinite(zFar);
        boolean nearInf = zNear > 0f && Float.isInfinite(zNear);
        float rm22;
        float rm32;
        float e;
        if (farInf) {
            e = 1.0E-6F;
            rm22 = e - 1f;
            rm32 = (e - (zZeroToOne ? 1f : 2f)) * zNear;
        } else if (nearInf) {
            e = 1.0E-6F;
            rm22 = (zZeroToOne ? 0f : 1f) - e;
            rm32 = ((zZeroToOne ? 1f : 2f) - e) * zFar;
        } else {
            rm22 = (zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar);
            rm32 = (zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar);
        }

        e = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22 - this.m30;
        float nm21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22 - this.m31;
        float nm22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22 - this.m32;
        float nm23 = this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22 - this.m33;
        dest._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m30(this.m20 * rm32)._m31(this.m21 * rm32)._m32(this.m22 * rm32)._m33(this.m23 * rm32)._m20(e)._m21(nm21)._m22(nm22)._m23(nm23)._properties(0);
        return dest;
    }

    public Matrix4f frustum(float left, float right, float bottom, float top, float zNear, float zFar, Matrix4f dest) {
        return this.frustum(left, right, bottom, top, zNear, zFar, false, dest);
    }

    public Matrix4f frustum(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne) {
        return this.frustum(left, right, bottom, top, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4f frustum(float left, float right, float bottom, float top, float zNear, float zFar) {
        return this.frustum(left, right, bottom, top, zNear, zFar, this);
    }

    public Matrix4f setFrustum(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne) {
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        this._m00((zNear + zNear) / (right - left))._m11((zNear + zNear) / (top - bottom))._m20((right + left) / (right - left))._m21((top + bottom) / (top - bottom));
        boolean farInf = zFar > 0f && Float.isInfinite(zFar);
        boolean nearInf = zNear > 0f && Float.isInfinite(zNear);
        float e;
        if (farInf) {
            e = 1.0E-6F;
            this._m22(e - 1f)._m32((e - (zZeroToOne ? 1f : 2f)) * zNear);
        } else if (nearInf) {
            e = 1.0E-6F;
            this._m22((zZeroToOne ? 0f : 1f) - e)._m32(((zZeroToOne ? 1f : 2f) - e) * zFar);
        } else {
            this._m22((zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar))._m32((zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar));
        }

        this._m23(-1f)._m33(0f)._properties(this.m20 == 0f && this.m21 == 0f ? 1 : 0);
        return this;
    }

    public Matrix4f setFrustum(float left, float right, float bottom, float top, float zNear, float zFar) {
        return this.setFrustum(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4f frustumLH(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        return (this.properties & 4) != 0 ? dest.setFrustumLH(left, right, bottom, top, zNear, zFar, zZeroToOne) : this.frustumLHGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4f frustumLHGeneric(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne, Matrix4f dest) {
        float rm00 = (zNear + zNear) / (right - left);
        float rm11 = (zNear + zNear) / (top - bottom);
        float rm20 = (right + left) / (right - left);
        float rm21 = (top + bottom) / (top - bottom);
        boolean farInf = zFar > 0f && Float.isInfinite(zFar);
        boolean nearInf = zNear > 0f && Float.isInfinite(zNear);
        float rm22;
        float rm32;
        float e;
        if (farInf) {
            e = 1.0E-6F;
            rm22 = 1f - e;
            rm32 = (e - (zZeroToOne ? 1f : 2f)) * zNear;
        } else if (nearInf) {
            e = 1.0E-6F;
            rm22 = (zZeroToOne ? 0f : 1f) - e;
            rm32 = ((zZeroToOne ? 1f : 2f) - e) * zFar;
        } else {
            rm22 = (zZeroToOne ? zFar : zFar + zNear) / (zFar - zNear);
            rm32 = (zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar);
        }

        e = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22 + this.m30;
        float nm21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22 + this.m31;
        float nm22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22 + this.m32;
        float nm23 = this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22 + this.m33;
        dest._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m30(this.m20 * rm32)._m31(this.m21 * rm32)._m32(this.m22 * rm32)._m33(this.m23 * rm32)._m20(e)._m21(nm21)._m22(nm22)._m23(nm23)._properties(0);
        return dest;
    }

    public Matrix4f frustumLH(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne) {
        return this.frustumLH(left, right, bottom, top, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4f frustumLH(float left, float right, float bottom, float top, float zNear, float zFar, Matrix4f dest) {
        return this.frustumLH(left, right, bottom, top, zNear, zFar, false, dest);
    }

    public Matrix4f frustumLH(float left, float right, float bottom, float top, float zNear, float zFar) {
        return this.frustumLH(left, right, bottom, top, zNear, zFar, this);
    }

    public Matrix4f setFrustumLH(float left, float right, float bottom, float top, float zNear, float zFar, boolean zZeroToOne) {
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        this._m00((zNear + zNear) / (right - left))._m11((zNear + zNear) / (top - bottom))._m20((right + left) / (right - left))._m21((top + bottom) / (top - bottom));
        boolean farInf = zFar > 0f && Float.isInfinite(zFar);
        boolean nearInf = zNear > 0f && Float.isInfinite(zNear);
        float e;
        if (farInf) {
            e = 1.0E-6F;
            this._m22(1f - e)._m32((e - (zZeroToOne ? 1f : 2f)) * zNear);
        } else if (nearInf) {
            e = 1.0E-6F;
            this._m22((zZeroToOne ? 0f : 1f) - e)._m32(((zZeroToOne ? 1f : 2f) - e) * zFar);
        } else {
            this._m22((zZeroToOne ? zFar : zFar + zNear) / (zFar - zNear))._m32((zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar));
        }

        return this._m23(1f)._m33(0f)._properties(0);
    }

    public Matrix4f setFrustumLH(float left, float right, float bottom, float top, float zNear, float zFar) {
        return this.setFrustumLH(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4f setFromIntrinsic(float alphaX, float alphaY, float gamma, float u0, float v0, int imgWidth, int imgHeight, float near, float far) {
        float l00 = 2f / (float)imgWidth;
        float l11 = 2f / (float)imgHeight;
        float l22 = 2f / (near - far);
        return this._m00(l00 * alphaX)._m01(0f)._m02(0f)._m03(0f)._m10(l00 * gamma)._m11(l11 * alphaY)._m12(0f)._m13(0f)._m20(l00 * u0 - 1f)._m21(l11 * v0 - 1f)._m22(l22 * -(near + far) + (far + near) / (near - far))._m23(-1f)._m30(0f)._m31(0f)._m32(l22 * -near * far)._m33(0f)._properties(1);
    }

    public Matrix4f rotate(Quaternionf quat, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotation(quat);
        } else if ((this.properties & 8) != 0) {
            return this.rotateTranslation(quat, dest);
        } else {
            return (this.properties & 2) != 0 ? this.rotateAffine(quat, dest) : this.rotateGeneric(quat, dest);
        }
    }

    private Matrix4f rotateGeneric(Quaternionf quat, Matrix4f dest) {
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
        float rm10 = -dzw + dxy;
        float rm11 = y2 - z2 + w2 - x2;
        float rm12 = dyz + dxw;
        float rm20 = dyw + dxz;
        float rm21 = dyz - dxw;
        float rm22 = z2 - y2 - x2 + w2;
        float nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        float nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        float nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        float nm03 = this.m03 * rm00 + this.m13 * rm01 + this.m23 * rm02;
        float nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        float nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        float nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        float nm13 = this.m03 * rm10 + this.m13 * rm11 + this.m23 * rm12;
        return dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotate(Quaternionf quat) {
        return this.rotate(quat, this);
    }

    public Matrix4f rotateAffine(Quaternionf quat, Matrix4f dest) {
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
        float rm10 = -dzw + dxy;
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
        return dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(0f)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0f)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotateAffine(Quaternionf quat) {
        return this.rotateAffine(quat, this);
    }

    public Matrix4f rotateTranslation(Quaternionf quat, Matrix4f dest) {
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
        float rm10 = -dzw + dxy;
        float rm11 = y2 - z2 + w2 - x2;
        float rm12 = dyz + dxw;
        float rm20 = dyw + dxz;
        float rm21 = dyz - dxw;
        float rm22 = z2 - y2 - x2 + w2;
        return dest._m20(rm20)._m21(rm21)._m22(rm22)._m23(0f)._m00(rm00)._m01(rm01)._m02(rm02)._m03(0f)._m10(rm10)._m11(rm11)._m12(rm12)._m13(0f)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotateAround(Quaternionf quat, float ox, float oy, float oz) {
        return this.rotateAround(quat, ox, oy, oz, this);
    }

    public Matrix4f rotateAroundAffine(Quaternionf quat, float ox, float oy, float oz, Matrix4f dest) {
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
        float rm10 = -dzw + dxy;
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
        dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(0f)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0f)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)._m30(-nm00 * ox - nm10 * oy - this.m20 * oz + tm30)._m31(-nm01 * ox - nm11 * oy - this.m21 * oz + tm31)._m32(-nm02 * ox - nm12 * oy - this.m22 * oz + tm32)._m33(1f)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4f rotateAround(Quaternionf quat, float ox, float oy, float oz, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return this.rotationAround(quat, ox, oy, oz);
        } else {
            return (this.properties & 2) != 0 ? this.rotateAroundAffine(quat, ox, oy, oz, dest) : this.rotateAroundGeneric(quat, ox, oy, oz, dest);
        }
    }

    private Matrix4f rotateAroundGeneric(Quaternionf quat, float ox, float oy, float oz, Matrix4f dest) {
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
        float rm10 = -dzw + dxy;
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
        float nm03 = this.m03 * rm00 + this.m13 * rm01 + this.m23 * rm02;
        float nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        float nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        float nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        float nm13 = this.m03 * rm10 + this.m13 * rm11 + this.m23 * rm12;
        dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m30(-nm00 * ox - nm10 * oy - this.m20 * oz + tm30)._m31(-nm01 * ox - nm11 * oy - this.m21 * oz + tm31)._m32(-nm02 * ox - nm12 * oy - this.m22 * oz + tm32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4f rotationAround(Quaternionf quat, float ox, float oy, float oz) {
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
        this._m20(dyw + dxz)._m21(dyz - dxw)._m22(z2 - y2 - x2 + w2)._m23(0f)._m00(w2 + x2 - z2 - y2)._m01(dxy + dzw)._m02(dxz - dyw)._m03(0f)._m10(-dzw + dxy)._m11(y2 - z2 + w2 - x2)._m12(dyz + dxw)._m13(0f)._m30(-this.m00 * ox - this.m10 * oy - this.m20 * oz + ox)._m31(-this.m01 * ox - this.m11 * oy - this.m21 * oz + oy)._m32(-this.m02 * ox - this.m12 * oy - this.m22 * oz + oz)._m33(1f)._properties(18);
        return this;
    }

    public Matrix4f rotateLocal(Quaternionf quat, Matrix4f dest) {
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
        float lm00 = w2 + x2 - z2 - y2;
        float lm01 = dxy + dzw;
        float lm02 = dxz - dyw;
        float lm10 = -dzw + dxy;
        float lm11 = y2 - z2 + w2 - x2;
        float lm12 = dyz + dxw;
        float lm20 = dyw + dxz;
        float lm21 = dyz - dxw;
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
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(this.m03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(this.m13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(this.m23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(this.m33)._properties(this.properties & -14);
    }

    public Matrix4f rotateLocal(Quaternionf quat) {
        return this.rotateLocal(quat, this);
    }

    public Matrix4f rotateAroundLocal(Quaternionf quat, float ox, float oy, float oz, Matrix4f dest) {
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
        float tm00 = this.m00 - ox * this.m03;
        float tm01 = this.m01 - oy * this.m03;
        float tm02 = this.m02 - oz * this.m03;
        float tm10 = this.m10 - ox * this.m13;
        float tm11 = this.m11 - oy * this.m13;
        float tm12 = this.m12 - oz * this.m13;
        float tm20 = this.m20 - ox * this.m23;
        float tm21 = this.m21 - oy * this.m23;
        float tm22 = this.m22 - oz * this.m23;
        float tm30 = this.m30 - ox * this.m33;
        float tm31 = this.m31 - oy * this.m33;
        float tm32 = this.m32 - oz * this.m33;
        dest._m00(lm00 * tm00 + lm10 * tm01 + lm20 * tm02 + ox * this.m03)._m01(lm01 * tm00 + lm11 * tm01 + lm21 * tm02 + oy * this.m03)._m02(lm02 * tm00 + lm12 * tm01 + lm22 * tm02 + oz * this.m03)._m03(this.m03)._m10(lm00 * tm10 + lm10 * tm11 + lm20 * tm12 + ox * this.m13)._m11(lm01 * tm10 + lm11 * tm11 + lm21 * tm12 + oy * this.m13)._m12(lm02 * tm10 + lm12 * tm11 + lm22 * tm12 + oz * this.m13)._m13(this.m13)._m20(lm00 * tm20 + lm10 * tm21 + lm20 * tm22 + ox * this.m23)._m21(lm01 * tm20 + lm11 * tm21 + lm21 * tm22 + oy * this.m23)._m22(lm02 * tm20 + lm12 * tm21 + lm22 * tm22 + oz * this.m23)._m23(this.m23)._m30(lm00 * tm30 + lm10 * tm31 + lm20 * tm32 + ox * this.m33)._m31(lm01 * tm30 + lm11 * tm31 + lm21 * tm32 + oy * this.m33)._m32(lm02 * tm30 + lm12 * tm31 + lm22 * tm32 + oz * this.m33)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4f rotateAroundLocal(Quaternionf quat, float ox, float oy, float oz) {
        return this.rotateAroundLocal(quat, ox, oy, oz, this);
    }

    public Matrix4f rotate(AxisAngle4f axisAngle) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Matrix4f rotate(AxisAngle4f axisAngle, Matrix4f dest) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dest);
    }

    public Matrix4f rotate(float angle, Vector3f axis) {
        return this.rotate(angle, axis.x, axis.y, axis.z);
    }

    public Matrix4f rotate(float angle, Vector3f axis, Matrix4f dest) {
        return this.rotate(angle, axis.x, axis.y, axis.z, dest);
    }

    public Vector4f unproject(float winX, float winY, float winZ, int[] viewport, Vector4f dest) {
        float a = this.m00 * this.m11 - this.m01 * this.m10;
        float b = this.m00 * this.m12 - this.m02 * this.m10;
        float c = this.m00 * this.m13 - this.m03 * this.m10;
        float d = this.m01 * this.m12 - this.m02 * this.m11;
        float e = this.m01 * this.m13 - this.m03 * this.m11;
        float f = this.m02 * this.m13 - this.m03 * this.m12;
        float g = this.m20 * this.m31 - this.m21 * this.m30;
        float h = this.m20 * this.m32 - this.m22 * this.m30;
        float i = this.m20 * this.m33 - this.m23 * this.m30;
        float j = this.m21 * this.m32 - this.m22 * this.m31;
        float k = this.m21 * this.m33 - this.m23 * this.m31;
        float l = this.m22 * this.m33 - this.m23 * this.m32;
        float det = a * l - b * k + c * j + d * i - e * h + f * g;
        det = 1f / det;
        float im00 = (this.m11 * l - this.m12 * k + this.m13 * j) * det;
        float im01 = (-this.m01 * l + this.m02 * k - this.m03 * j) * det;
        float im02 = (this.m31 * f - this.m32 * e + this.m33 * d) * det;
        float im03 = (-this.m21 * f + this.m22 * e - this.m23 * d) * det;
        float im10 = (-this.m10 * l + this.m12 * i - this.m13 * h) * det;
        float im11 = (this.m00 * l - this.m02 * i + this.m03 * h) * det;
        float im12 = (-this.m30 * f + this.m32 * c - this.m33 * b) * det;
        float im13 = (this.m20 * f - this.m22 * c + this.m23 * b) * det;
        float im20 = (this.m10 * k - this.m11 * i + this.m13 * g) * det;
        float im21 = (-this.m00 * k + this.m01 * i - this.m03 * g) * det;
        float im22 = (this.m30 * e - this.m31 * c + this.m33 * a) * det;
        float im23 = (-this.m20 * e + this.m21 * c - this.m23 * a) * det;
        float im30 = (-this.m10 * j + this.m11 * h - this.m12 * g) * det;
        float im31 = (this.m00 * j - this.m01 * h + this.m02 * g) * det;
        float im32 = (-this.m30 * d + this.m31 * b - this.m32 * a) * det;
        float im33 = (this.m20 * d - this.m21 * b + this.m22 * a) * det;
        float ndcX = (winX - (float)viewport[0]) / (float)viewport[2] * 2f - 1f;
        float ndcY = (winY - (float)viewport[1]) / (float)viewport[3] * 2f - 1f;
        float ndcZ = winZ + winZ - 1f;
        float invW = 1f / (im03 * ndcX + im13 * ndcY + im23 * ndcZ + im33);
        return dest.set((im00 * ndcX + im10 * ndcY + im20 * ndcZ + im30) * invW, (im01 * ndcX + im11 * ndcY + im21 * ndcZ + im31) * invW, (im02 * ndcX + im12 * ndcY + im22 * ndcZ + im32) * invW, 1f);
    }

    public Vector3f unproject(float winX, float winY, float winZ, int[] viewport, Vector3f dest) {
        float a = this.m00 * this.m11 - this.m01 * this.m10;
        float b = this.m00 * this.m12 - this.m02 * this.m10;
        float c = this.m00 * this.m13 - this.m03 * this.m10;
        float d = this.m01 * this.m12 - this.m02 * this.m11;
        float e = this.m01 * this.m13 - this.m03 * this.m11;
        float f = this.m02 * this.m13 - this.m03 * this.m12;
        float g = this.m20 * this.m31 - this.m21 * this.m30;
        float h = this.m20 * this.m32 - this.m22 * this.m30;
        float i = this.m20 * this.m33 - this.m23 * this.m30;
        float j = this.m21 * this.m32 - this.m22 * this.m31;
        float k = this.m21 * this.m33 - this.m23 * this.m31;
        float l = this.m22 * this.m33 - this.m23 * this.m32;
        float det = a * l - b * k + c * j + d * i - e * h + f * g;
        det = 1f / det;
        float im00 = (this.m11 * l - this.m12 * k + this.m13 * j) * det;
        float im01 = (-this.m01 * l + this.m02 * k - this.m03 * j) * det;
        float im02 = (this.m31 * f - this.m32 * e + this.m33 * d) * det;
        float im03 = (-this.m21 * f + this.m22 * e - this.m23 * d) * det;
        float im10 = (-this.m10 * l + this.m12 * i - this.m13 * h) * det;
        float im11 = (this.m00 * l - this.m02 * i + this.m03 * h) * det;
        float im12 = (-this.m30 * f + this.m32 * c - this.m33 * b) * det;
        float im13 = (this.m20 * f - this.m22 * c + this.m23 * b) * det;
        float im20 = (this.m10 * k - this.m11 * i + this.m13 * g) * det;
        float im21 = (-this.m00 * k + this.m01 * i - this.m03 * g) * det;
        float im22 = (this.m30 * e - this.m31 * c + this.m33 * a) * det;
        float im23 = (-this.m20 * e + this.m21 * c - this.m23 * a) * det;
        float im30 = (-this.m10 * j + this.m11 * h - this.m12 * g) * det;
        float im31 = (this.m00 * j - this.m01 * h + this.m02 * g) * det;
        float im32 = (-this.m30 * d + this.m31 * b - this.m32 * a) * det;
        float im33 = (this.m20 * d - this.m21 * b + this.m22 * a) * det;
        float ndcX = (winX - (float)viewport[0]) / (float)viewport[2] * 2f - 1f;
        float ndcY = (winY - (float)viewport[1]) / (float)viewport[3] * 2f - 1f;
        float ndcZ = winZ + winZ - 1f;
        float invW = 1f / (im03 * ndcX + im13 * ndcY + im23 * ndcZ + im33);
        return dest.set((im00 * ndcX + im10 * ndcY + im20 * ndcZ + im30) * invW, (im01 * ndcX + im11 * ndcY + im21 * ndcZ + im31) * invW, (im02 * ndcX + im12 * ndcY + im22 * ndcZ + im32) * invW);
    }

    public Vector4f unproject(Vector3f winCoords, int[] viewport, Vector4f dest) {
        return this.unproject(winCoords.x, winCoords.y, winCoords.z, viewport, dest);
    }

    public Vector3f unproject(Vector3f winCoords, int[] viewport, Vector3f dest) {
        return this.unproject(winCoords.x, winCoords.y, winCoords.z, viewport, dest);
    }

    public Matrix4f unprojectRay(float winX, float winY, int[] viewport, Vector3f originDest, Vector3f dirDest) {
        float a = this.m00 * this.m11 - this.m01 * this.m10;
        float b = this.m00 * this.m12 - this.m02 * this.m10;
        float c = this.m00 * this.m13 - this.m03 * this.m10;
        float d = this.m01 * this.m12 - this.m02 * this.m11;
        float e = this.m01 * this.m13 - this.m03 * this.m11;
        float f = this.m02 * this.m13 - this.m03 * this.m12;
        float g = this.m20 * this.m31 - this.m21 * this.m30;
        float h = this.m20 * this.m32 - this.m22 * this.m30;
        float i = this.m20 * this.m33 - this.m23 * this.m30;
        float j = this.m21 * this.m32 - this.m22 * this.m31;
        float k = this.m21 * this.m33 - this.m23 * this.m31;
        float l = this.m22 * this.m33 - this.m23 * this.m32;
        float det = a * l - b * k + c * j + d * i - e * h + f * g;
        det = 1f / det;
        float im00 = (this.m11 * l - this.m12 * k + this.m13 * j) * det;
        float im01 = (-this.m01 * l + this.m02 * k - this.m03 * j) * det;
        float im02 = (this.m31 * f - this.m32 * e + this.m33 * d) * det;
        float im03 = (-this.m21 * f + this.m22 * e - this.m23 * d) * det;
        float im10 = (-this.m10 * l + this.m12 * i - this.m13 * h) * det;
        float im11 = (this.m00 * l - this.m02 * i + this.m03 * h) * det;
        float im12 = (-this.m30 * f + this.m32 * c - this.m33 * b) * det;
        float im13 = (this.m20 * f - this.m22 * c + this.m23 * b) * det;
        float im20 = (this.m10 * k - this.m11 * i + this.m13 * g) * det;
        float im21 = (-this.m00 * k + this.m01 * i - this.m03 * g) * det;
        float im22 = (this.m30 * e - this.m31 * c + this.m33 * a) * det;
        float im23 = (-this.m20 * e + this.m21 * c - this.m23 * a) * det;
        float im30 = (-this.m10 * j + this.m11 * h - this.m12 * g) * det;
        float im31 = (this.m00 * j - this.m01 * h + this.m02 * g) * det;
        float im32 = (-this.m30 * d + this.m31 * b - this.m32 * a) * det;
        float im33 = (this.m20 * d - this.m21 * b + this.m22 * a) * det;
        float ndcX = (winX - (float)viewport[0]) / (float)viewport[2] * 2f - 1f;
        float ndcY = (winY - (float)viewport[1]) / (float)viewport[3] * 2f - 1f;
        float px = im00 * ndcX + im10 * ndcY + im30;
        float py = im01 * ndcX + im11 * ndcY + im31;
        float pz = im02 * ndcX + im12 * ndcY + im32;
        float invNearW = 1f / (im03 * ndcX + im13 * ndcY - im23 + im33);
        float nearX = (px - im20) * invNearW;
        float nearY = (py - im21) * invNearW;
        float nearZ = (pz - im22) * invNearW;
        float invW0 = 1f / (im03 * ndcX + im13 * ndcY + im33);
        float x0 = px * invW0;
        float y0 = py * invW0;
        float z0 = pz * invW0;
        originDest.x = nearX;
        originDest.y = nearY;
        originDest.z = nearZ;
        dirDest.x = x0 - nearX;
        dirDest.y = y0 - nearY;
        dirDest.z = z0 - nearZ;
        return this;
    }

    public Matrix4f unprojectRay(Vector2f winCoords, int[] viewport, Vector3f originDest, Vector3f dirDest) {
        return this.unprojectRay(winCoords.x, winCoords.y, viewport, originDest, dirDest);
    }

    public Vector4f unprojectInv(Vector3f winCoords, int[] viewport, Vector4f dest) {
        return this.unprojectInv(winCoords.x, winCoords.y, winCoords.z, viewport, dest);
    }

    public Vector4f unprojectInv(float winX, float winY, float winZ, int[] viewport, Vector4f dest) {
        float ndcX = (winX - (float)viewport[0]) / (float)viewport[2] * 2f - 1f;
        float ndcY = (winY - (float)viewport[1]) / (float)viewport[3] * 2f - 1f;
        float ndcZ = winZ + winZ - 1f;
        float invW = 1f / (this.m03 * ndcX + this.m13 * ndcY + this.m23 * ndcZ + this.m33);
        return dest.set((this.m00 * ndcX + this.m10 * ndcY + this.m20 * ndcZ + this.m30) * invW, (this.m01 * ndcX + this.m11 * ndcY + this.m21 * ndcZ + this.m31) * invW, (this.m02 * ndcX + this.m12 * ndcY + this.m22 * ndcZ + this.m32) * invW, 1f);
    }

    public Matrix4f unprojectInvRay(Vector2f winCoords, int[] viewport, Vector3f originDest, Vector3f dirDest) {
        return this.unprojectInvRay(winCoords.x, winCoords.y, viewport, originDest, dirDest);
    }

    public Matrix4f unprojectInvRay(float winX, float winY, int[] viewport, Vector3f originDest, Vector3f dirDest) {
        float ndcX = (winX - (float)viewport[0]) / (float)viewport[2] * 2f - 1f;
        float ndcY = (winY - (float)viewport[1]) / (float)viewport[3] * 2f - 1f;
        float px = this.m00 * ndcX + this.m10 * ndcY + this.m30;
        float py = this.m01 * ndcX + this.m11 * ndcY + this.m31;
        float pz = this.m02 * ndcX + this.m12 * ndcY + this.m32;
        float invNearW = 1f / (this.m03 * ndcX + this.m13 * ndcY - this.m23 + this.m33);
        float nearX = (px - this.m20) * invNearW;
        float nearY = (py - this.m21) * invNearW;
        float nearZ = (pz - this.m22) * invNearW;
        float invW0 = 1f / (this.m03 * ndcX + this.m13 * ndcY + this.m33);
        float x0 = px * invW0;
        float y0 = py * invW0;
        float z0 = pz * invW0;
        originDest.x = nearX;
        originDest.y = nearY;
        originDest.z = nearZ;
        dirDest.x = x0 - nearX;
        dirDest.y = y0 - nearY;
        dirDest.z = z0 - nearZ;
        return this;
    }

    public Vector3f unprojectInv(Vector3f winCoords, int[] viewport, Vector3f dest) {
        return this.unprojectInv(winCoords.x, winCoords.y, winCoords.z, viewport, dest);
    }

    public Vector3f unprojectInv(float winX, float winY, float winZ, int[] viewport, Vector3f dest) {
        float ndcX = (winX - (float)viewport[0]) / (float)viewport[2] * 2f - 1f;
        float ndcY = (winY - (float)viewport[1]) / (float)viewport[3] * 2f - 1f;
        float ndcZ = winZ + winZ - 1f;
        float invW = 1f / (this.m03 * ndcX + this.m13 * ndcY + this.m23 * ndcZ + this.m33);
        return dest.set((this.m00 * ndcX + this.m10 * ndcY + this.m20 * ndcZ + this.m30) * invW, (this.m01 * ndcX + this.m11 * ndcY + this.m21 * ndcZ + this.m31) * invW, (this.m02 * ndcX + this.m12 * ndcY + this.m22 * ndcZ + this.m32) * invW);
    }

    public Vector4f project(float x, float y, float z, int[] viewport, Vector4f winCoordsDest) {
        float invW = 1f / Math.fma(this.m03, x, Math.fma(this.m13, y, Math.fma(this.m23, z, this.m33)));
        float nx = Math.fma(this.m00, x, Math.fma(this.m10, y, Math.fma(this.m20, z, this.m30))) * invW;
        float ny = Math.fma(this.m01, x, Math.fma(this.m11, y, Math.fma(this.m21, z, this.m31))) * invW;
        float nz = Math.fma(this.m02, x, Math.fma(this.m12, y, Math.fma(this.m22, z, this.m32))) * invW;
        return winCoordsDest.set(Math.fma(Math.fma(nx, 0.5F, 0.5F), (float)viewport[2], (float)viewport[0]), Math.fma(Math.fma(ny, 0.5F, 0.5F), (float)viewport[3], (float)viewport[1]), Math.fma(0.5F, nz, 0.5F), 1f);
    }

    public Vector3f project(float x, float y, float z, int[] viewport, Vector3f winCoordsDest) {
        float invW = 1f / Math.fma(this.m03, x, Math.fma(this.m13, y, Math.fma(this.m23, z, this.m33)));
        float nx = Math.fma(this.m00, x, Math.fma(this.m10, y, Math.fma(this.m20, z, this.m30))) * invW;
        float ny = Math.fma(this.m01, x, Math.fma(this.m11, y, Math.fma(this.m21, z, this.m31))) * invW;
        float nz = Math.fma(this.m02, x, Math.fma(this.m12, y, Math.fma(this.m22, z, this.m32))) * invW;
        winCoordsDest.x = Math.fma(Math.fma(nx, 0.5F, 0.5F), (float)viewport[2], (float)viewport[0]);
        winCoordsDest.y = Math.fma(Math.fma(ny, 0.5F, 0.5F), (float)viewport[3], (float)viewport[1]);
        winCoordsDest.z = Math.fma(0.5F, nz, 0.5F);
        return winCoordsDest;
    }

    public Vector4f project(Vector3f position, int[] viewport, Vector4f winCoordsDest) {
        return this.project(position.x, position.y, position.z, viewport, winCoordsDest);
    }

    public Vector3f project(Vector3f position, int[] viewport, Vector3f winCoordsDest) {
        return this.project(position.x, position.y, position.z, viewport, winCoordsDest);
    }

    public Matrix4f reflect(float a, float b, float c, float d, Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.reflection(a, b, c, d);
        } else {
            return (this.properties & 2) != 0 ? this.reflectAffine(a, b, c, d, dest) : this.reflectGeneric(a, b, c, d, dest);
        }
    }

    private Matrix4f reflectAffine(float a, float b, float c, float d, Matrix4f dest) {
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
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32)._m33(this.m33);
        float nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        float nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        float nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        float nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        float nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        float nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(0f)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0f)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)._properties(this.properties & -14);
        return dest;
    }

    private Matrix4f reflectGeneric(float a, float b, float c, float d, Matrix4f dest) {
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
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m23 * rm32 + this.m33);
        float nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        float nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        float nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        float nm03 = this.m03 * rm00 + this.m13 * rm01 + this.m23 * rm02;
        float nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        float nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        float nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        float nm13 = this.m03 * rm10 + this.m13 * rm11 + this.m23 * rm12;
        dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4f reflect(float a, float b, float c, float d) {
        return this.reflect(a, b, c, d, this);
    }

    public Matrix4f reflect(float nx, float ny, float nz, float px, float py, float pz) {
        return this.reflect(nx, ny, nz, px, py, pz, this);
    }

    public Matrix4f reflect(float nx, float ny, float nz, float px, float py, float pz, Matrix4f dest) {
        float invLength = Math.invsqrt(nx * nx + ny * ny + nz * nz);
        float nnx = nx * invLength;
        float nny = ny * invLength;
        float nnz = nz * invLength;
        return this.reflect(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz, dest);
    }

    public Matrix4f reflect(Vector3f normal, Vector3f point) {
        return this.reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z);
    }

    public Matrix4f reflect(Quaternionf orientation, Vector3f point) {
        return this.reflect(orientation, point, this);
    }

    public Matrix4f reflect(Quaternionf orientation, Vector3f point, Matrix4f dest) {
        double num1 = (double)(orientation.x + orientation.x);
        double num2 = (double)(orientation.y + orientation.y);
        double num3 = (double)(orientation.z + orientation.z);
        float normalX = (float)((double)orientation.x * num3 + (double)orientation.w * num2);
        float normalY = (float)((double)orientation.y * num3 - (double)orientation.w * num1);
        float normalZ = (float)(1.0 - ((double)orientation.x * num1 + (double)orientation.y * num2));
        return this.reflect(normalX, normalY, normalZ, point.x, point.y, point.z, dest);
    }

    public Matrix4f reflect(Vector3f normal, Vector3f point, Matrix4f dest) {
        return this.reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z, dest);
    }

    public Matrix4f reflection(float a, float b, float c, float d) {
        float da = a + a;
        float db = b + b;
        float dc = c + c;
        float dd = d + d;
        this._m00(1f - da * a)._m01(-da * b)._m02(-da * c)._m03(0f)._m10(-db * a)._m11(1f - db * b)._m12(-db * c)._m13(0f)._m20(-dc * a)._m21(-dc * b)._m22(1f - dc * c)._m23(0f)._m30(-dd * a)._m31(-dd * b)._m32(-dd * c)._m33(1f)._properties(18);
        return this;
    }

    public Matrix4f reflection(float nx, float ny, float nz, float px, float py, float pz) {
        float invLength = Math.invsqrt(nx * nx + ny * ny + nz * nz);
        float nnx = nx * invLength;
        float nny = ny * invLength;
        float nnz = nz * invLength;
        return this.reflection(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz);
    }

    public Matrix4f reflection(Vector3f normal, Vector3f point) {
        return this.reflection(normal.x, normal.y, normal.z, point.x, point.y, point.z);
    }

    public Matrix4f reflection(Quaternionf orientation, Vector3f point) {
        double num1 = (double)(orientation.x + orientation.x);
        double num2 = (double)(orientation.y + orientation.y);
        double num3 = (double)(orientation.z + orientation.z);
        float normalX = (float)((double)orientation.x * num3 + (double)orientation.w * num2);
        float normalY = (float)((double)orientation.y * num3 - (double)orientation.w * num1);
        float normalZ = (float)(1.0 - ((double)orientation.x * num1 + (double)orientation.y * num2));
        return this.reflection(normalX, normalY, normalZ, point.x, point.y, point.z);
    }

    public Vector4f getRow(int row, Vector4f dest) throws IndexOutOfBoundsException {
        switch (row) {
            case 0:
                return dest.set(this.m00, this.m10, this.m20, this.m30);
            case 1:
                return dest.set(this.m01, this.m11, this.m21, this.m31);
            case 2:
                return dest.set(this.m02, this.m12, this.m22, this.m32);
            case 3:
                return dest.set(this.m03, this.m13, this.m23, this.m33);
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public Vector3f getRow(int row, Vector3f dest) throws IndexOutOfBoundsException {
        switch (row) {
            case 0:
                return dest.set(this.m00, this.m10, this.m20);
            case 1:
                return dest.set(this.m01, this.m11, this.m21);
            case 2:
                return dest.set(this.m02, this.m12, this.m22);
            case 3:
                return dest.set(this.m03, this.m13, this.m23);
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public Matrix4f setRow(int row, Vector4f src) throws IndexOutOfBoundsException {
        switch (row) {
            case 0:
                return this._m00(src.x)._m10(src.y)._m20(src.z)._m30(src.w)._properties(0);
            case 1:
                return this._m01(src.x)._m11(src.y)._m21(src.z)._m31(src.w)._properties(0);
            case 2:
                return this._m02(src.x)._m12(src.y)._m22(src.z)._m32(src.w)._properties(0);
            case 3:
                return this._m03(src.x)._m13(src.y)._m23(src.z)._m33(src.w)._properties(0);
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public Vector4f getColumn(int column, Vector4f dest) throws IndexOutOfBoundsException {
        return MemUtil.INSTANCE.getColumn(this, column, dest);
    }

    public Vector3f getColumn(int column, Vector3f dest) throws IndexOutOfBoundsException {
        switch (column) {
            case 0:
                return dest.set(this.m00, this.m01, this.m02);
            case 1:
                return dest.set(this.m10, this.m11, this.m12);
            case 2:
                return dest.set(this.m20, this.m21, this.m22);
            case 3:
                return dest.set(this.m30, this.m31, this.m32);
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public Matrix4f setColumn(int column, Vector4f src) throws IndexOutOfBoundsException {
        return src instanceof Vector4f ? MemUtil.INSTANCE.setColumn((Vector4f)src, column, this)._properties(0) : MemUtil.INSTANCE.setColumn(src, column, this)._properties(0);
    }

    public float get(int column, int row) {
        return MemUtil.INSTANCE.get(this, column, row);
    }

    public Matrix4f set(int column, int row, float value) {
        return MemUtil.INSTANCE.set(this, column, row, value);
    }

    public float getRowColumn(int row, int column) {
        return MemUtil.INSTANCE.get(this, column, row);
    }

    public Matrix4f setRowColumn(int row, int column, float value) {
        return MemUtil.INSTANCE.set(this, column, row, value);
    }

    public Matrix4f normal() {
        return this.normal(this);
    }

    public Matrix4f normal(Matrix4f dest) {
        if ((this.properties & 4) != 0) {
            return dest.identity();
        } else {
            return (this.properties & 16) != 0 ? this.normalOrthonormal(dest) : this.normalGeneric(dest);
        }
    }

    private Matrix4f normalOrthonormal(Matrix4f dest) {
        if (dest != this) {
            dest.set((Matrix4f)this);
        }

        return dest._properties(18);
    }

    private Matrix4f normalGeneric(Matrix4f dest) {
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
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(0f)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)._m20(nm20)._m21(nm21)._m22(nm22)._m23(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)._properties((this.properties | 2) & -10);
    }

    public Matrix3f normal(Matrix3f dest) {
        return (this.properties & 16) != 0 ? this.normalOrthonormal(dest) : this.normalGeneric(dest);
    }

    private Matrix3f normalOrthonormal(Matrix3f dest) {
        dest.set(this);
        return dest;
    }

    private Matrix3f normalGeneric(Matrix3f dest) {
        float det = (this.m00 * this.m11 - this.m01 * this.m10) * this.m22 + (this.m02 * this.m10 - this.m00 * this.m12) * this.m21 + (this.m01 * this.m12 - this.m02 * this.m11) * this.m20;
        float s = 1f / det;
        return dest._m00((this.m11 * this.m22 - this.m21 * this.m12) * s)._m01((this.m20 * this.m12 - this.m10 * this.m22) * s)._m02((this.m10 * this.m21 - this.m20 * this.m11) * s)._m10((this.m21 * this.m02 - this.m01 * this.m22) * s)._m11((this.m00 * this.m22 - this.m20 * this.m02) * s)._m12((this.m20 * this.m01 - this.m00 * this.m21) * s)._m20((this.m01 * this.m12 - this.m02 * this.m11) * s)._m21((this.m02 * this.m10 - this.m00 * this.m12) * s)._m22((this.m00 * this.m11 - this.m01 * this.m10) * s);
    }

    public Matrix4f cofactor3x3() {
        return this.cofactor3x3(this);
    }

    public Matrix3f cofactor3x3(Matrix3f dest) {
        return dest._m00(this.m11 * this.m22 - this.m21 * this.m12)._m01(this.m20 * this.m12 - this.m10 * this.m22)._m02(this.m10 * this.m21 - this.m20 * this.m11)._m10(this.m21 * this.m02 - this.m01 * this.m22)._m11(this.m00 * this.m22 - this.m20 * this.m02)._m12(this.m20 * this.m01 - this.m00 * this.m21)._m20(this.m01 * this.m12 - this.m02 * this.m11)._m21(this.m02 * this.m10 - this.m00 * this.m12)._m22(this.m00 * this.m11 - this.m01 * this.m10);
    }

    public Matrix4f cofactor3x3(Matrix4f dest) {
        float nm10 = this.m21 * this.m02 - this.m01 * this.m22;
        float nm11 = this.m00 * this.m22 - this.m20 * this.m02;
        float nm12 = this.m20 * this.m01 - this.m00 * this.m21;
        float nm20 = this.m01 * this.m12 - this.m11 * this.m02;
        float nm21 = this.m02 * this.m10 - this.m12 * this.m00;
        float nm22 = this.m00 * this.m11 - this.m10 * this.m01;
        return dest._m00(this.m11 * this.m22 - this.m21 * this.m12)._m01(this.m20 * this.m12 - this.m10 * this.m22)._m02(this.m10 * this.m21 - this.m20 * this.m11)._m03(0f)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)._m20(nm20)._m21(nm21)._m22(nm22)._m23(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)._properties((this.properties | 2) & -10);
    }

    public Matrix4f normalize3x3() {
        return this.normalize3x3(this);
    }

    public Matrix4f normalize3x3(Matrix4f dest) {
        float invXlen = Math.invsqrt(this.m00 * this.m00 + this.m01 * this.m01 + this.m02 * this.m02);
        float invYlen = Math.invsqrt(this.m10 * this.m10 + this.m11 * this.m11 + this.m12 * this.m12);
        float invZlen = Math.invsqrt(this.m20 * this.m20 + this.m21 * this.m21 + this.m22 * this.m22);
        return dest._m00(this.m00 * invXlen)._m01(this.m01 * invXlen)._m02(this.m02 * invXlen)._m10(this.m10 * invYlen)._m11(this.m11 * invYlen)._m12(this.m12 * invYlen)._m20(this.m20 * invZlen)._m21(this.m21 * invZlen)._m22(this.m22 * invZlen)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties);
    }

    public Matrix3f normalize3x3(Matrix3f dest) {
        float invXlen = Math.invsqrt(this.m00 * this.m00 + this.m01 * this.m01 + this.m02 * this.m02);
        float invYlen = Math.invsqrt(this.m10 * this.m10 + this.m11 * this.m11 + this.m12 * this.m12);
        float invZlen = Math.invsqrt(this.m20 * this.m20 + this.m21 * this.m21 + this.m22 * this.m22);
        return dest._m00(this.m00 * invXlen)._m01(this.m01 * invXlen)._m02(this.m02 * invXlen)._m10(this.m10 * invYlen)._m11(this.m11 * invYlen)._m12(this.m12 * invYlen)._m20(this.m20 * invZlen)._m21(this.m21 * invZlen)._m22(this.m22 * invZlen);
    }

    public Vector4f frustumPlane(int plane, Vector4f dest) {
        switch (plane) {
            case 0:
                dest.set(this.m03 + this.m00, this.m13 + this.m10, this.m23 + this.m20, this.m33 + this.m30).normalize3();
                break;
            case 1:
                dest.set(this.m03 - this.m00, this.m13 - this.m10, this.m23 - this.m20, this.m33 - this.m30).normalize3();
                break;
            case 2:
                dest.set(this.m03 + this.m01, this.m13 + this.m11, this.m23 + this.m21, this.m33 + this.m31).normalize3();
                break;
            case 3:
                dest.set(this.m03 - this.m01, this.m13 - this.m11, this.m23 - this.m21, this.m33 - this.m31).normalize3();
                break;
            case 4:
                dest.set(this.m03 + this.m02, this.m13 + this.m12, this.m23 + this.m22, this.m33 + this.m32).normalize3();
                break;
            case 5:
                dest.set(this.m03 - this.m02, this.m13 - this.m12, this.m23 - this.m22, this.m33 - this.m32).normalize3();
                break;
            default:
                throw new IllegalArgumentException("dest");
        }

        return dest;
    }

    public Vector3f frustumCorner(int corner, Vector3f point) {
        float d1;
        float d2;
        float d3;
        float n1x;
        float n1y;
        float n1z;
        float n2x;
        float n2y;
        float n2z;
        float n3x;
        float n3y;
        float n3z;
        switch (corner) {
            case 0:
                n1x = this.m03 + this.m00;
                n1y = this.m13 + this.m10;
                n1z = this.m23 + this.m20;
                d1 = this.m33 + this.m30;
                n2x = this.m03 + this.m01;
                n2y = this.m13 + this.m11;
                n2z = this.m23 + this.m21;
                d2 = this.m33 + this.m31;
                n3x = this.m03 + this.m02;
                n3y = this.m13 + this.m12;
                n3z = this.m23 + this.m22;
                d3 = this.m33 + this.m32;
                break;
            case 1:
                n1x = this.m03 - this.m00;
                n1y = this.m13 - this.m10;
                n1z = this.m23 - this.m20;
                d1 = this.m33 - this.m30;
                n2x = this.m03 + this.m01;
                n2y = this.m13 + this.m11;
                n2z = this.m23 + this.m21;
                d2 = this.m33 + this.m31;
                n3x = this.m03 + this.m02;
                n3y = this.m13 + this.m12;
                n3z = this.m23 + this.m22;
                d3 = this.m33 + this.m32;
                break;
            case 2:
                n1x = this.m03 - this.m00;
                n1y = this.m13 - this.m10;
                n1z = this.m23 - this.m20;
                d1 = this.m33 - this.m30;
                n2x = this.m03 - this.m01;
                n2y = this.m13 - this.m11;
                n2z = this.m23 - this.m21;
                d2 = this.m33 - this.m31;
                n3x = this.m03 + this.m02;
                n3y = this.m13 + this.m12;
                n3z = this.m23 + this.m22;
                d3 = this.m33 + this.m32;
                break;
            case 3:
                n1x = this.m03 + this.m00;
                n1y = this.m13 + this.m10;
                n1z = this.m23 + this.m20;
                d1 = this.m33 + this.m30;
                n2x = this.m03 - this.m01;
                n2y = this.m13 - this.m11;
                n2z = this.m23 - this.m21;
                d2 = this.m33 - this.m31;
                n3x = this.m03 + this.m02;
                n3y = this.m13 + this.m12;
                n3z = this.m23 + this.m22;
                d3 = this.m33 + this.m32;
                break;
            case 4:
                n1x = this.m03 - this.m00;
                n1y = this.m13 - this.m10;
                n1z = this.m23 - this.m20;
                d1 = this.m33 - this.m30;
                n2x = this.m03 + this.m01;
                n2y = this.m13 + this.m11;
                n2z = this.m23 + this.m21;
                d2 = this.m33 + this.m31;
                n3x = this.m03 - this.m02;
                n3y = this.m13 - this.m12;
                n3z = this.m23 - this.m22;
                d3 = this.m33 - this.m32;
                break;
            case 5:
                n1x = this.m03 + this.m00;
                n1y = this.m13 + this.m10;
                n1z = this.m23 + this.m20;
                d1 = this.m33 + this.m30;
                n2x = this.m03 + this.m01;
                n2y = this.m13 + this.m11;
                n2z = this.m23 + this.m21;
                d2 = this.m33 + this.m31;
                n3x = this.m03 - this.m02;
                n3y = this.m13 - this.m12;
                n3z = this.m23 - this.m22;
                d3 = this.m33 - this.m32;
                break;
            case 6:
                n1x = this.m03 + this.m00;
                n1y = this.m13 + this.m10;
                n1z = this.m23 + this.m20;
                d1 = this.m33 + this.m30;
                n2x = this.m03 - this.m01;
                n2y = this.m13 - this.m11;
                n2z = this.m23 - this.m21;
                d2 = this.m33 - this.m31;
                n3x = this.m03 - this.m02;
                n3y = this.m13 - this.m12;
                n3z = this.m23 - this.m22;
                d3 = this.m33 - this.m32;
                break;
            case 7:
                n1x = this.m03 - this.m00;
                n1y = this.m13 - this.m10;
                n1z = this.m23 - this.m20;
                d1 = this.m33 - this.m30;
                n2x = this.m03 - this.m01;
                n2y = this.m13 - this.m11;
                n2z = this.m23 - this.m21;
                d2 = this.m33 - this.m31;
                n3x = this.m03 - this.m02;
                n3y = this.m13 - this.m12;
                n3z = this.m23 - this.m22;
                d3 = this.m33 - this.m32;
                break;
            default:
                throw new IllegalArgumentException("corner");
        }

        float c23x = n2y * n3z - n2z * n3y;
        float c23y = n2z * n3x - n2x * n3z;
        float c23z = n2x * n3y - n2y * n3x;
        float c31x = n3y * n1z - n3z * n1y;
        float c31y = n3z * n1x - n3x * n1z;
        float c31z = n3x * n1y - n3y * n1x;
        float c12x = n1y * n2z - n1z * n2y;
        float c12y = n1z * n2x - n1x * n2z;
        float c12z = n1x * n2y - n1y * n2x;
        float invDot = 1f / (n1x * c23x + n1y * c23y + n1z * c23z);
        point.x = (-c23x * d1 - c31x * d2 - c12x * d3) * invDot;
        point.y = (-c23y * d1 - c31y * d2 - c12y * d3) * invDot;
        point.z = (-c23z * d1 - c31z * d2 - c12z * d3) * invDot;
        return point;
    }

    public Vector3f perspectiveOrigin(Vector3f origin) {
        float n1x = this.m03 + this.m00;
        float n1y = this.m13 + this.m10;
        float n1z = this.m23 + this.m20;
        float d1 = this.m33 + this.m30;
        float n2x = this.m03 - this.m00;
        float n2y = this.m13 - this.m10;
        float n2z = this.m23 - this.m20;
        float d2 = this.m33 - this.m30;
        float n3x = this.m03 - this.m01;
        float n3y = this.m13 - this.m11;
        float n3z = this.m23 - this.m21;
        float d3 = this.m33 - this.m31;
        float c23x = n2y * n3z - n2z * n3y;
        float c23y = n2z * n3x - n2x * n3z;
        float c23z = n2x * n3y - n2y * n3x;
        float c31x = n3y * n1z - n3z * n1y;
        float c31y = n3z * n1x - n3x * n1z;
        float c31z = n3x * n1y - n3y * n1x;
        float c12x = n1y * n2z - n1z * n2y;
        float c12y = n1z * n2x - n1x * n2z;
        float c12z = n1x * n2y - n1y * n2x;
        float invDot = 1f / (n1x * c23x + n1y * c23y + n1z * c23z);
        origin.x = (-c23x * d1 - c31x * d2 - c12x * d3) * invDot;
        origin.y = (-c23y * d1 - c31y * d2 - c12y * d3) * invDot;
        origin.z = (-c23z * d1 - c31z * d2 - c12z * d3) * invDot;
        return origin;
    }

    public Vector3f perspectiveInvOrigin(Vector3f dest) {
        float invW = 1f / this.m23;
        dest.x = this.m20 * invW;
        dest.y = this.m21 * invW;
        dest.z = this.m22 * invW;
        return dest;
    }

    public float perspectiveFov() {
        float n1x = this.m03 + this.m01;
        float n1y = this.m13 + this.m11;
        float n1z = this.m23 + this.m21;
        float n2x = this.m01 - this.m03;
        float n2y = this.m11 - this.m13;
        float n2z = this.m21 - this.m23;
        float n1len = Math.sqrt(n1x * n1x + n1y * n1y + n1z * n1z);
        float n2len = Math.sqrt(n2x * n2x + n2y * n2y + n2z * n2z);
        return Math.acos((n1x * n2x + n1y * n2y + n1z * n2z) / (n1len * n2len));
    }

    public float perspectiveNear() {
        return this.m32 / (this.m23 + this.m22);
    }

    public float perspectiveFar() {
        return this.m32 / (this.m22 - this.m23);
    }

    public Vector3f frustumRayDir(float x, float y, Vector3f dir) {
        float a = this.m10 * this.m23;
        float b = this.m13 * this.m21;
        float c = this.m10 * this.m21;
        float d = this.m11 * this.m23;
        float e = this.m13 * this.m20;
        float f = this.m11 * this.m20;
        float g = this.m03 * this.m20;
        float h = this.m01 * this.m23;
        float i = this.m01 * this.m20;
        float j = this.m03 * this.m21;
        float k = this.m00 * this.m23;
        float l = this.m00 * this.m21;
        float m = this.m00 * this.m13;
        float n = this.m03 * this.m11;
        float o = this.m00 * this.m11;
        float p = this.m01 * this.m13;
        float q = this.m03 * this.m10;
        float r = this.m01 * this.m10;
        float m1x = (d + e + f - a - b - c) * (1f - y) + (a - b - c + d - e + f) * y;
        float m1y = (j + k + l - g - h - i) * (1f - y) + (g - h - i + j - k + l) * y;
        float m1z = (p + q + r - m - n - o) * (1f - y) + (m - n - o + p - q + r) * y;
        float m2x = (b - c - d + e + f - a) * (1f - y) + (a + b - c - d - e + f) * y;
        float m2y = (h - i - j + k + l - g) * (1f - y) + (g + h - i - j - k + l) * y;
        float m2z = (n - o - p + q + r - m) * (1f - y) + (m + n - o - p - q + r) * y;
        dir.x = m1x + (m2x - m1x) * x;
        dir.y = m1y + (m2y - m1y) * x;
        dir.z = m1z + (m2z - m1z) * x;
        return dir.normalize(dir);
    }

    public Vector3f positiveZ(Vector3f dir) {
        return (this.properties & 16) != 0 ? this.normalizedPositiveZ(dir) : this.positiveZGeneric(dir);
    }

    private Vector3f positiveZGeneric(Vector3f dir) {
        return dir.set(this.m10 * this.m21 - this.m11 * this.m20, this.m20 * this.m01 - this.m21 * this.m00, this.m00 * this.m11 - this.m01 * this.m10).normalize();
    }

    public Vector3f normalizedPositiveZ(Vector3f dir) {
        return dir.set(this.m02, this.m12, this.m22);
    }

    public Vector3f positiveX(Vector3f dir) {
        return (this.properties & 16) != 0 ? this.normalizedPositiveX(dir) : this.positiveXGeneric(dir);
    }

    private Vector3f positiveXGeneric(Vector3f dir) {
        return dir.set(this.m11 * this.m22 - this.m12 * this.m21, this.m02 * this.m21 - this.m01 * this.m22, this.m01 * this.m12 - this.m02 * this.m11).normalize();
    }

    public Vector3f normalizedPositiveX(Vector3f dir) {
        return dir.set(this.m00, this.m10, this.m20);
    }

    public Vector3f positiveY(Vector3f dir) {
        return (this.properties & 16) != 0 ? this.normalizedPositiveY(dir) : this.positiveYGeneric(dir);
    }

    private Vector3f positiveYGeneric(Vector3f dir) {
        return dir.set(this.m12 * this.m20 - this.m10 * this.m22, this.m00 * this.m22 - this.m02 * this.m20, this.m02 * this.m10 - this.m00 * this.m12).normalize();
    }

    public Vector3f normalizedPositiveY(Vector3f dir) {
        return dir.set(this.m01, this.m11, this.m21);
    }

    public Vector3f originAffine(Vector3f origin) {
        float a = this.m00 * this.m11 - this.m01 * this.m10;
        float b = this.m00 * this.m12 - this.m02 * this.m10;
        float d = this.m01 * this.m12 - this.m02 * this.m11;
        float g = this.m20 * this.m31 - this.m21 * this.m30;
        float h = this.m20 * this.m32 - this.m22 * this.m30;
        float j = this.m21 * this.m32 - this.m22 * this.m31;
        return origin.set(-this.m10 * j + this.m11 * h - this.m12 * g, this.m00 * j - this.m01 * h + this.m02 * g, -this.m30 * d + this.m31 * b - this.m32 * a);
    }

    public Vector3f origin(Vector3f dest) {
        return (this.properties & 2) != 0 ? this.originAffine(dest) : this.originGeneric(dest);
    }

    private Vector3f originGeneric(Vector3f dest) {
        float a = this.m00 * this.m11 - this.m01 * this.m10;
        float b = this.m00 * this.m12 - this.m02 * this.m10;
        float c = this.m00 * this.m13 - this.m03 * this.m10;
        float d = this.m01 * this.m12 - this.m02 * this.m11;
        float e = this.m01 * this.m13 - this.m03 * this.m11;
        float f = this.m02 * this.m13 - this.m03 * this.m12;
        float g = this.m20 * this.m31 - this.m21 * this.m30;
        float h = this.m20 * this.m32 - this.m22 * this.m30;
        float i = this.m20 * this.m33 - this.m23 * this.m30;
        float j = this.m21 * this.m32 - this.m22 * this.m31;
        float k = this.m21 * this.m33 - this.m23 * this.m31;
        float l = this.m22 * this.m33 - this.m23 * this.m32;
        float det = a * l - b * k + c * j + d * i - e * h + f * g;
        float invDet = 1f / det;
        float nm30 = (-this.m10 * j + this.m11 * h - this.m12 * g) * invDet;
        float nm31 = (this.m00 * j - this.m01 * h + this.m02 * g) * invDet;
        float nm32 = (-this.m30 * d + this.m31 * b - this.m32 * a) * invDet;
        float nm33 = det / (this.m20 * d - this.m21 * b + this.m22 * a);
        return dest.set(nm30 * nm33, nm31 * nm33, nm32 * nm33);
    }

    public Matrix4f shadow(Vector4f light, float a, float b, float c, float d) {
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, this);
    }

    public Matrix4f shadow(Vector4f light, float a, float b, float c, float d, Matrix4f dest) {
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, dest);
    }

    public Matrix4f shadow(float lightX, float lightY, float lightZ, float lightW, float a, float b, float c, float d) {
        return this.shadow(lightX, lightY, lightZ, lightW, a, b, c, d, this);
    }

    public Matrix4f shadow(float lightX, float lightY, float lightZ, float lightW, float a, float b, float c, float d, Matrix4f dest) {
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
        float nm03 = this.m03 * rm00 + this.m13 * rm01 + this.m23 * rm02 + this.m33 * rm03;
        float nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12 + this.m30 * rm13;
        float nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12 + this.m31 * rm13;
        float nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12 + this.m32 * rm13;
        float nm13 = this.m03 * rm10 + this.m13 * rm11 + this.m23 * rm12 + this.m33 * rm13;
        float nm20 = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22 + this.m30 * rm23;
        float nm21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22 + this.m31 * rm23;
        float nm22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22 + this.m32 * rm23;
        float nm23 = this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22 + this.m33 * rm23;
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30 * rm33)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31 * rm33)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32 * rm33)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m23 * rm32 + this.m33 * rm33)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4f shadow(Vector4f light, Matrix4f planeTransform, Matrix4f dest) {
        float a = planeTransform.m10;
        float b = planeTransform.m11;
        float c = planeTransform.m12;
        float d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32;
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, dest);
    }

    public Matrix4f shadow(Vector4f light, Matrix4f planeTransform) {
        return this.shadow(light, planeTransform, this);
    }

    public Matrix4f shadow(float lightX, float lightY, float lightZ, float lightW, Matrix4f planeTransform, Matrix4f dest) {
        float a = planeTransform.m10;
        float b = planeTransform.m11;
        float c = planeTransform.m12;
        float d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32;
        return this.shadow(lightX, lightY, lightZ, lightW, a, b, c, d, dest);
    }

    public Matrix4f shadow(float lightX, float lightY, float lightZ, float lightW, Matrix4f planeTransform) {
        return this.shadow(lightX, lightY, lightZ, lightW, planeTransform, this);
    }

    public Matrix4f billboardCylindrical(Vector3f objPos, Vector3f targetPos, Vector3f up) {
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
        this._m00(leftX)._m01(leftY)._m02(leftZ)._m03(0f)._m10(up.x)._m11(up.y)._m12(up.z)._m13(0f)._m20(dirX)._m21(dirY)._m22(dirZ)._m23(0f)._m30(objPos.x)._m31(objPos.y)._m32(objPos.z)._m33(1f)._properties(18);
        return this;
    }

    public Matrix4f billboardSpherical(Vector3f objPos, Vector3f targetPos, Vector3f up) {
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
        this._m00(leftX)._m01(leftY)._m02(leftZ)._m03(0f)._m10(upX)._m11(upY)._m12(upZ)._m13(0f)._m20(dirX)._m21(dirY)._m22(dirZ)._m23(0f)._m30(objPos.x)._m31(objPos.y)._m32(objPos.z)._m33(1f)._properties(18);
        return this;
    }

    public Matrix4f billboardSpherical(Vector3f objPos, Vector3f targetPos) {
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
        this._m00(1f - q11)._m01(q01)._m02(-q13)._m03(0f)._m10(q01)._m11(1f - q00)._m12(q03)._m13(0f)._m20(q13)._m21(-q03)._m22(1f - q11 - q00)._m23(0f)._m30(objPos.x)._m31(objPos.y)._m32(objPos.z)._m33(1f)._properties(18);
        return this;
    }

    public int hashCode() {
        int result = 1;
        result = 31 * result + Float.floatToIntBits(this.m00);
        result = 31 * result + Float.floatToIntBits(this.m01);
        result = 31 * result + Float.floatToIntBits(this.m02);
        result = 31 * result + Float.floatToIntBits(this.m03);
        result = 31 * result + Float.floatToIntBits(this.m10);
        result = 31 * result + Float.floatToIntBits(this.m11);
        result = 31 * result + Float.floatToIntBits(this.m12);
        result = 31 * result + Float.floatToIntBits(this.m13);
        result = 31 * result + Float.floatToIntBits(this.m20);
        result = 31 * result + Float.floatToIntBits(this.m21);
        result = 31 * result + Float.floatToIntBits(this.m22);
        result = 31 * result + Float.floatToIntBits(this.m23);
        result = 31 * result + Float.floatToIntBits(this.m30);
        result = 31 * result + Float.floatToIntBits(this.m31);
        result = 31 * result + Float.floatToIntBits(this.m32);
        result = 31 * result + Float.floatToIntBits(this.m33);
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof Matrix4f)) {
            return false;
        } else {
            Matrix4f other = (Matrix4f)obj;
            if (Float.floatToIntBits(this.m00) != Float.floatToIntBits(other.m00)) {
                return false;
            } else if (Float.floatToIntBits(this.m01) != Float.floatToIntBits(other.m01)) {
                return false;
            } else if (Float.floatToIntBits(this.m02) != Float.floatToIntBits(other.m02)) {
                return false;
            } else if (Float.floatToIntBits(this.m03) != Float.floatToIntBits(other.m03)) {
                return false;
            } else if (Float.floatToIntBits(this.m10) != Float.floatToIntBits(other.m10)) {
                return false;
            } else if (Float.floatToIntBits(this.m11) != Float.floatToIntBits(other.m11)) {
                return false;
            } else if (Float.floatToIntBits(this.m12) != Float.floatToIntBits(other.m12)) {
                return false;
            } else if (Float.floatToIntBits(this.m13) != Float.floatToIntBits(other.m13)) {
                return false;
            } else if (Float.floatToIntBits(this.m20) != Float.floatToIntBits(other.m20)) {
                return false;
            } else if (Float.floatToIntBits(this.m21) != Float.floatToIntBits(other.m21)) {
                return false;
            } else if (Float.floatToIntBits(this.m22) != Float.floatToIntBits(other.m22)) {
                return false;
            } else if (Float.floatToIntBits(this.m23) != Float.floatToIntBits(other.m23)) {
                return false;
            } else if (Float.floatToIntBits(this.m30) != Float.floatToIntBits(other.m30)) {
                return false;
            } else if (Float.floatToIntBits(this.m31) != Float.floatToIntBits(other.m31)) {
                return false;
            } else if (Float.floatToIntBits(this.m32) != Float.floatToIntBits(other.m32)) {
                return false;
            } else {
                return Float.floatToIntBits(this.m33) == Float.floatToIntBits(other.m33);
            }
        }
    }

    public boolean equals(Matrix4f m, float delta) {
        if (this == m) {
            return true;
        } else if (m == null) {
            return false;
        } else if (!(m instanceof Matrix4f)) {
            return false;
        } else if (!Runtime.equals(this.m00, m.m00, delta)) {
            return false;
        } else if (!Runtime.equals(this.m01, m.m01, delta)) {
            return false;
        } else if (!Runtime.equals(this.m02, m.m02, delta)) {
            return false;
        } else if (!Runtime.equals(this.m03, m.m03, delta)) {
            return false;
        } else if (!Runtime.equals(this.m10, m.m10, delta)) {
            return false;
        } else if (!Runtime.equals(this.m11, m.m11, delta)) {
            return false;
        } else if (!Runtime.equals(this.m12, m.m12, delta)) {
            return false;
        } else if (!Runtime.equals(this.m13, m.m13, delta)) {
            return false;
        } else if (!Runtime.equals(this.m20, m.m20, delta)) {
            return false;
        } else if (!Runtime.equals(this.m21, m.m21, delta)) {
            return false;
        } else if (!Runtime.equals(this.m22, m.m22, delta)) {
            return false;
        } else if (!Runtime.equals(this.m23, m.m23, delta)) {
            return false;
        } else if (!Runtime.equals(this.m30, m.m30, delta)) {
            return false;
        } else if (!Runtime.equals(this.m31, m.m31, delta)) {
            return false;
        } else if (!Runtime.equals(this.m32, m.m32, delta)) {
            return false;
        } else {
            return Runtime.equals(this.m33, m.m33, delta);
        }
    }

    public Matrix4f pick(float x, float y, float width, float height, int[] viewport, Matrix4f dest) {
        float sx = (float)viewport[2] / width;
        float sy = (float)viewport[3] / height;
        float tx = ((float)viewport[2] + 2f * ((float)viewport[0] - x)) / width;
        float ty = ((float)viewport[3] + 2f * ((float)viewport[1] - y)) / height;
        dest._m30(this.m00 * tx + this.m10 * ty + this.m30)._m31(this.m01 * tx + this.m11 * ty + this.m31)._m32(this.m02 * tx + this.m12 * ty + this.m32)._m33(this.m03 * tx + this.m13 * ty + this.m33)._m00(this.m00 * sx)._m01(this.m01 * sx)._m02(this.m02 * sx)._m03(this.m03 * sx)._m10(this.m10 * sy)._m11(this.m11 * sy)._m12(this.m12 * sy)._m13(this.m13 * sy)._properties(0);
        return dest;
    }

    public Matrix4f pick(float x, float y, float width, float height, int[] viewport) {
        return this.pick(x, y, width, height, viewport, this);
    }

    public boolean isAffine() {
        return this.m03 == 0f && this.m13 == 0f && this.m23 == 0f && this.m33 == 1f;
    }

    public Matrix4f swap(Matrix4f other) {
        MemUtil.INSTANCE.swap(this, other);
        int props = this.properties;
        this.properties = other.properties();
        other.properties = props;
        return this;
    }

    public Matrix4f arcball(float radius, float centerX, float centerY, float centerZ, float angleX, float angleY, Matrix4f dest) {
        float m30 = this.m20 * -radius + this.m30;
        float m31 = this.m21 * -radius + this.m31;
        float m32 = this.m22 * -radius + this.m32;
        float m33 = this.m23 * -radius + this.m33;
        float sin = Math.sin(angleX);
        float cos = Math.cosFromSin(sin, angleX);
        float nm10 = this.m10 * cos + this.m20 * sin;
        float nm11 = this.m11 * cos + this.m21 * sin;
        float nm12 = this.m12 * cos + this.m22 * sin;
        float nm13 = this.m13 * cos + this.m23 * sin;
        float m20 = this.m20 * cos - this.m10 * sin;
        float m21 = this.m21 * cos - this.m11 * sin;
        float m22 = this.m22 * cos - this.m12 * sin;
        float m23 = this.m23 * cos - this.m13 * sin;
        sin = Math.sin(angleY);
        cos = Math.cosFromSin(sin, angleY);
        float nm00 = this.m00 * cos - m20 * sin;
        float nm01 = this.m01 * cos - m21 * sin;
        float nm02 = this.m02 * cos - m22 * sin;
        float nm03 = this.m03 * cos - m23 * sin;
        float nm20 = this.m00 * sin + m20 * cos;
        float nm21 = this.m01 * sin + m21 * cos;
        float nm22 = this.m02 * sin + m22 * cos;
        float nm23 = this.m03 * sin + m23 * cos;
        dest._m30(-nm00 * centerX - nm10 * centerY - nm20 * centerZ + m30)._m31(-nm01 * centerX - nm11 * centerY - nm21 * centerZ + m31)._m32(-nm02 * centerX - nm12 * centerY - nm22 * centerZ + m32)._m33(-nm03 * centerX - nm13 * centerY - nm23 * centerZ + m33)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4f arcball(float radius, Vector3f center, float angleX, float angleY, Matrix4f dest) {
        return this.arcball(radius, center.x, center.y, center.z, angleX, angleY, dest);
    }

    public Matrix4f arcball(float radius, float centerX, float centerY, float centerZ, float angleX, float angleY) {
        return this.arcball(radius, centerX, centerY, centerZ, angleX, angleY, this);
    }

    public Matrix4f arcball(float radius, Vector3f center, float angleX, float angleY) {
        return this.arcball(radius, center.x, center.y, center.z, angleX, angleY, this);
    }

    public Matrix4f frustumAabb(Vector3f min, Vector3f max) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for(int t = 0; t < 8; ++t) {
            float x = (float)((t & 1) << 1) - 1f;
            float y = (float)((t >>> 1 & 1) << 1) - 1f;
            float z = (float)((t >>> 2 & 1) << 1) - 1f;
            float invW = 1f / (this.m03 * x + this.m13 * y + this.m23 * z + this.m33);
            float nx = (this.m00 * x + this.m10 * y + this.m20 * z + this.m30) * invW;
            float ny = (this.m01 * x + this.m11 * y + this.m21 * z + this.m31) * invW;
            float nz = (this.m02 * x + this.m12 * y + this.m22 * z + this.m32) * invW;
            minX = minX < nx ? minX : nx;
            minY = minY < ny ? minY : ny;
            minZ = minZ < nz ? minZ : nz;
            maxX = maxX > nx ? maxX : nx;
            maxY = maxY > ny ? maxY : ny;
            maxZ = maxZ > nz ? maxZ : nz;
        }

        min.x = minX;
        min.y = minY;
        min.z = minZ;
        max.x = maxX;
        max.y = maxY;
        max.z = maxZ;
        return this;
    }

    public Matrix4f projectedGridRange(Matrix4f projector, float sLower, float sUpper, Matrix4f dest) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        boolean intersection = false;

        for(int t = 0; t < 12; ++t) {
            float c0X;
            float c0Y;
            float c0Z;
            float c1X;
            float c1Y;
            float c1Z;
            if (t < 4) {
                c0X = -1f;
                c1X = 1f;
                c0Y = c1Y = (float)((t & 1) << 1) - 1f;
                c0Z = c1Z = (float)((t >>> 1 & 1) << 1) - 1f;
            } else if (t < 8) {
                c0Y = -1f;
                c1Y = 1f;
                c0X = c1X = (float)((t & 1) << 1) - 1f;
                c0Z = c1Z = (float)((t >>> 1 & 1) << 1) - 1f;
            } else {
                c0Z = -1f;
                c1Z = 1f;
                c0X = c1X = (float)((t & 1) << 1) - 1f;
                c0Y = c1Y = (float)((t >>> 1 & 1) << 1) - 1f;
            }

            float invW = 1f / (this.m03 * c0X + this.m13 * c0Y + this.m23 * c0Z + this.m33);
            float p0x = (this.m00 * c0X + this.m10 * c0Y + this.m20 * c0Z + this.m30) * invW;
            float p0y = (this.m01 * c0X + this.m11 * c0Y + this.m21 * c0Z + this.m31) * invW;
            float p0z = (this.m02 * c0X + this.m12 * c0Y + this.m22 * c0Z + this.m32) * invW;
            invW = 1f / (this.m03 * c1X + this.m13 * c1Y + this.m23 * c1Z + this.m33);
            float p1x = (this.m00 * c1X + this.m10 * c1Y + this.m20 * c1Z + this.m30) * invW;
            float p1y = (this.m01 * c1X + this.m11 * c1Y + this.m21 * c1Z + this.m31) * invW;
            float p1z = (this.m02 * c1X + this.m12 * c1Y + this.m22 * c1Z + this.m32) * invW;
            float dirX = p1x - p0x;
            float dirY = p1y - p0y;
            float dirZ = p1z - p0z;
            float invDenom = 1f / dirY;

            for(int s = 0; s < 2; ++s) {
                float isectT = -(p0y + (s == 0 ? sLower : sUpper)) * invDenom;
                if (isectT >= 0f && isectT <= 1f) {
                    intersection = true;
                    float ix = p0x + isectT * dirX;
                    float iz = p0z + isectT * dirZ;
                    invW = 1f / (projector.m03 * ix + projector.m23 * iz + projector.m33);
                    float px = (projector.m00 * ix + projector.m20 * iz + projector.m30) * invW;
                    float py = (projector.m01 * ix + projector.m21 * iz + projector.m31) * invW;
                    minX = minX < px ? minX : px;
                    minY = minY < py ? minY : py;
                    maxX = maxX > px ? maxX : px;
                    maxY = maxY > py ? maxY : py;
                }
            }
        }

        if (!intersection) {
            return null;
        } else {
            dest.set(maxX - minX, 0f, 0f, 0f, 0f, maxY - minY, 0f, 0f, 0f, 0f, 1f, 0f, minX, minY, 0f, 1f);
            dest._properties(2);
            return dest;
        }
    }

    public Matrix4f perspectiveFrustumSlice(float near, float far, Matrix4f dest) {
        float invOldNear = (this.m23 + this.m22) / this.m32;
        float invNearFar = 1f / (near - far);
        dest._m00(this.m00 * invOldNear * near)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(this.m10)._m11(this.m11 * invOldNear * near)._m12(this.m12)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22((far + near) * invNearFar)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32((far + far) * near * invNearFar)._m33(this.m33)._properties(this.properties & -29);
        return dest;
    }

    public Matrix4f orthoCrop(Matrix4f view, Matrix4f dest) {
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for(int t = 0; t < 8; ++t) {
            float x = (float)((t & 1) << 1) - 1f;
            float y = (float)((t >>> 1 & 1) << 1) - 1f;
            float z = (float)((t >>> 2 & 1) << 1) - 1f;
            float invW = 1f / (this.m03 * x + this.m13 * y + this.m23 * z + this.m33);
            float wx = (this.m00 * x + this.m10 * y + this.m20 * z + this.m30) * invW;
            float wy = (this.m01 * x + this.m11 * y + this.m21 * z + this.m31) * invW;
            float wz = (this.m02 * x + this.m12 * y + this.m22 * z + this.m32) * invW;
            invW = 1f / (view.m03 * wx + view.m13 * wy + view.m23 * wz + view.m33);
            float vx = view.m00 * wx + view.m10 * wy + view.m20 * wz + view.m30;
            float vy = view.m01 * wx + view.m11 * wy + view.m21 * wz + view.m31;
            float vz = (view.m02 * wx + view.m12 * wy + view.m22 * wz + view.m32) * invW;
            minX = minX < vx ? minX : vx;
            maxX = maxX > vx ? maxX : vx;
            minY = minY < vy ? minY : vy;
            maxY = maxY > vy ? maxY : vy;
            minZ = minZ < vz ? minZ : vz;
            maxZ = maxZ > vz ? maxZ : vz;
        }

        return dest.setOrtho(minX, maxX, minY, maxY, -maxZ, -minZ);
    }

    public Matrix4f trapezoidCrop(float p0x, float p0y, float p1x, float p1y, float p2x, float p2y, float p3x, float p3y) {
        float aX = p1y - p0y;
        float aY = p0x - p1x;
        float nm10 = -aX;
        float nm30 = aX * p0y - aY * p0x;
        float nm31 = -(aX * p0x + aY * p0y);
        float c3x = aY * p3x + nm10 * p3y + nm30;
        float c3y = aX * p3x + aY * p3y + nm31;
        float s = -c3x / c3y;
        float nm00 = aY + s * aX;
        nm10 += s * aY;
        nm30 += s * nm31;
        float d1x = nm00 * p1x + nm10 * p1y + nm30;
        float d2x = nm00 * p2x + nm10 * p2y + nm30;
        float d = d1x * c3y / (d2x - d1x);
        nm31 += d;
        float sx = 2f / d2x;
        float sy = 1f / (c3y + d);
        float u = (sy + sy) * d / (1f - sy * d);
        float m03 = aX * sy;
        float m13 = aY * sy;
        float m33 = nm31 * sy;
        float nm01 = (u + 1f) * m03;
        float nm11 = (u + 1f) * m13;
        nm31 = (u + 1f) * m33 - u;
        nm00 = sx * nm00 - m03;
        nm10 = sx * nm10 - m13;
        nm30 = sx * nm30 - m33;
        this.set(nm00, nm01, 0f, m03, nm10, nm11, 0f, m13, 0f, 0f, 1f, 0f, nm30, nm31, 0f, m33);
        this._properties(0);
        return this;
    }

    public Matrix4f transformAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Vector3f outMin, Vector3f outMax) {
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

    public Matrix4f transformAab(Vector3f min, Vector3f max, Vector3f outMin, Vector3f outMax) {
        return this.transformAab(min.x, min.y, min.z, max.x, max.y, max.z, outMin, outMax);
    }

    public Matrix4f lerp(Matrix4f other, float t) {
        return this.lerp(other, t, this);
    }

    public Matrix4f lerp(Matrix4f other, float t, Matrix4f dest) {
        dest._m00(Math.fma(other.m00 - this.m00, t, this.m00))._m01(Math.fma(other.m01 - this.m01, t, this.m01))._m02(Math.fma(other.m02 - this.m02, t, this.m02))._m03(Math.fma(other.m03 - this.m03, t, this.m03))._m10(Math.fma(other.m10 - this.m10, t, this.m10))._m11(Math.fma(other.m11 - this.m11, t, this.m11))._m12(Math.fma(other.m12 - this.m12, t, this.m12))._m13(Math.fma(other.m13 - this.m13, t, this.m13))._m20(Math.fma(other.m20 - this.m20, t, this.m20))._m21(Math.fma(other.m21 - this.m21, t, this.m21))._m22(Math.fma(other.m22 - this.m22, t, this.m22))._m23(Math.fma(other.m23 - this.m23, t, this.m23))._m30(Math.fma(other.m30 - this.m30, t, this.m30))._m31(Math.fma(other.m31 - this.m31, t, this.m31))._m32(Math.fma(other.m32 - this.m32, t, this.m32))._m33(Math.fma(other.m33 - this.m33, t, this.m33))._properties(this.properties & other.properties());
        return dest;
    }

    public Matrix4f rotateTowards(Vector3f dir, Vector3f up, Matrix4f dest) {
        return this.rotateTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z, dest);
    }

    public Matrix4f rotateTowards(Vector3f dir, Vector3f up) {
        return this.rotateTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z, this);
    }

    public Matrix4f rotateTowards(float dirX, float dirY, float dirZ, float upX, float upY, float upZ) {
        return this.rotateTowards(dirX, dirY, dirZ, upX, upY, upZ, this);
    }

    public Matrix4f rotateTowards(float dirX, float dirY, float dirZ, float upX, float upY, float upZ, Matrix4f dest) {
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
        dest._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33);
        float nm00 = this.m00 * leftX + this.m10 * leftY + this.m20 * leftZ;
        float nm01 = this.m01 * leftX + this.m11 * leftY + this.m21 * leftZ;
        float nm02 = this.m02 * leftX + this.m12 * leftY + this.m22 * leftZ;
        float nm03 = this.m03 * leftX + this.m13 * leftY + this.m23 * leftZ;
        float nm10 = this.m00 * upnX + this.m10 * upnY + this.m20 * upnZ;
        float nm11 = this.m01 * upnX + this.m11 * upnY + this.m21 * upnZ;
        float nm12 = this.m02 * upnX + this.m12 * upnY + this.m22 * upnZ;
        float nm13 = this.m03 * upnX + this.m13 * upnY + this.m23 * upnZ;
        dest._m20(this.m00 * ndirX + this.m10 * ndirY + this.m20 * ndirZ)._m21(this.m01 * ndirX + this.m11 * ndirY + this.m21 * ndirZ)._m22(this.m02 * ndirX + this.m12 * ndirY + this.m22 * ndirZ)._m23(this.m03 * ndirX + this.m13 * ndirY + this.m23 * ndirZ)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4f rotationTowards(Vector3f dir, Vector3f up) {
        return this.rotationTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    public Matrix4f rotationTowards(float dirX, float dirY, float dirZ, float upX, float upY, float upZ) {
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
        if ((this.properties & 4) == 0) {
            MemUtil.INSTANCE.identity(this);
        }

        this._m00(leftX)._m01(leftY)._m02(leftZ)._m10(upnX)._m11(upnY)._m12(upnZ)._m20(ndirX)._m21(ndirY)._m22(ndirZ)._properties(18);
        return this;
    }

    public Matrix4f translationRotateTowards(Vector3f pos, Vector3f dir, Vector3f up) {
        return this.translationRotateTowards(pos.x, pos.y, pos.z, dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    public Matrix4f translationRotateTowards(float posX, float posY, float posZ, float dirX, float dirY, float dirZ, float upX, float upY, float upZ) {
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
        this._m00(leftX)._m01(leftY)._m02(leftZ)._m03(0f)._m10(upnX)._m11(upnY)._m12(upnZ)._m13(0f)._m20(ndirX)._m21(ndirY)._m22(ndirZ)._m23(0f)._m30(posX)._m31(posY)._m32(posZ)._m33(1f)._properties(18);
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

    public Matrix4f affineSpan(Vector3f corner, Vector3f xDir, Vector3f yDir, Vector3f zDir) {
        float a = this.m10 * this.m22;
        float b = this.m10 * this.m21;
        float c = this.m10 * this.m02;
        float d = this.m10 * this.m01;
        float e = this.m11 * this.m22;
        float f = this.m11 * this.m20;
        float g = this.m11 * this.m02;
        float h = this.m11 * this.m00;
        float i = this.m12 * this.m21;
        float j = this.m12 * this.m20;
        float k = this.m12 * this.m01;
        float l = this.m12 * this.m00;
        float m = this.m20 * this.m02;
        float n = this.m20 * this.m01;
        float o = this.m21 * this.m02;
        float p = this.m21 * this.m00;
        float q = this.m22 * this.m01;
        float r = this.m22 * this.m00;
        float s = 1f / (this.m00 * this.m11 - this.m01 * this.m10) * this.m22 + (this.m02 * this.m10 - this.m00 * this.m12) * this.m21 + (this.m01 * this.m12 - this.m02 * this.m11) * this.m20;
        float nm00 = (e - i) * s;
        float nm01 = (o - q) * s;
        float nm02 = (k - g) * s;
        float nm10 = (j - a) * s;
        float nm11 = (r - m) * s;
        float nm12 = (c - l) * s;
        float nm20 = (b - f) * s;
        float nm21 = (n - p) * s;
        float nm22 = (h - d) * s;
        corner.x = -nm00 - nm10 - nm20 + (a * this.m31 - b * this.m32 + f * this.m32 - e * this.m30 + i * this.m30 - j * this.m31) * s;
        corner.y = -nm01 - nm11 - nm21 + (m * this.m31 - n * this.m32 + p * this.m32 - o * this.m30 + q * this.m30 - r * this.m31) * s;
        corner.z = -nm02 - nm12 - nm22 + (g * this.m30 - k * this.m30 + l * this.m31 - c * this.m31 + d * this.m32 - h * this.m32) * s;
        xDir.x = 2f * nm00;
        xDir.y = 2f * nm01;
        xDir.z = 2f * nm02;
        yDir.x = 2f * nm10;
        yDir.y = 2f * nm11;
        yDir.z = 2f * nm12;
        zDir.x = 2f * nm20;
        zDir.y = 2f * nm21;
        zDir.z = 2f * nm22;
        return this;
    }

    public boolean testPoint(float x, float y, float z) {
        float nxX = this.m03 + this.m00;
        float nxY = this.m13 + this.m10;
        float nxZ = this.m23 + this.m20;
        float nxW = this.m33 + this.m30;
        float pxX = this.m03 - this.m00;
        float pxY = this.m13 - this.m10;
        float pxZ = this.m23 - this.m20;
        float pxW = this.m33 - this.m30;
        float nyX = this.m03 + this.m01;
        float nyY = this.m13 + this.m11;
        float nyZ = this.m23 + this.m21;
        float nyW = this.m33 + this.m31;
        float pyX = this.m03 - this.m01;
        float pyY = this.m13 - this.m11;
        float pyZ = this.m23 - this.m21;
        float pyW = this.m33 - this.m31;
        float nzX = this.m03 + this.m02;
        float nzY = this.m13 + this.m12;
        float nzZ = this.m23 + this.m22;
        float nzW = this.m33 + this.m32;
        float pzX = this.m03 - this.m02;
        float pzY = this.m13 - this.m12;
        float pzZ = this.m23 - this.m22;
        float pzW = this.m33 - this.m32;
        return nxX * x + nxY * y + nxZ * z + nxW >= 0f && pxX * x + pxY * y + pxZ * z + pxW >= 0f && nyX * x + nyY * y + nyZ * z + nyW >= 0f && pyX * x + pyY * y + pyZ * z + pyW >= 0f && nzX * x + nzY * y + nzZ * z + nzW >= 0f && pzX * x + pzY * y + pzZ * z + pzW >= 0f;
    }

    public boolean testSphere(float x, float y, float z, float r) {
        float nxX = this.m03 + this.m00;
        float nxY = this.m13 + this.m10;
        float nxZ = this.m23 + this.m20;
        float nxW = this.m33 + this.m30;
        float invl = Math.invsqrt(nxX * nxX + nxY * nxY + nxZ * nxZ);
        nxX *= invl;
        nxY *= invl;
        nxZ *= invl;
        nxW *= invl;
        float pxX = this.m03 - this.m00;
        float pxY = this.m13 - this.m10;
        float pxZ = this.m23 - this.m20;
        float pxW = this.m33 - this.m30;
        invl = Math.invsqrt(pxX * pxX + pxY * pxY + pxZ * pxZ);
        pxX *= invl;
        pxY *= invl;
        pxZ *= invl;
        pxW *= invl;
        float nyX = this.m03 + this.m01;
        float nyY = this.m13 + this.m11;
        float nyZ = this.m23 + this.m21;
        float nyW = this.m33 + this.m31;
        invl = Math.invsqrt(nyX * nyX + nyY * nyY + nyZ * nyZ);
        nyX *= invl;
        nyY *= invl;
        nyZ *= invl;
        nyW *= invl;
        float pyX = this.m03 - this.m01;
        float pyY = this.m13 - this.m11;
        float pyZ = this.m23 - this.m21;
        float pyW = this.m33 - this.m31;
        invl = Math.invsqrt(pyX * pyX + pyY * pyY + pyZ * pyZ);
        pyX *= invl;
        pyY *= invl;
        pyZ *= invl;
        pyW *= invl;
        float nzX = this.m03 + this.m02;
        float nzY = this.m13 + this.m12;
        float nzZ = this.m23 + this.m22;
        float nzW = this.m33 + this.m32;
        invl = Math.invsqrt(nzX * nzX + nzY * nzY + nzZ * nzZ);
        nzX *= invl;
        nzY *= invl;
        nzZ *= invl;
        nzW *= invl;
        float pzX = this.m03 - this.m02;
        float pzY = this.m13 - this.m12;
        float pzZ = this.m23 - this.m22;
        float pzW = this.m33 - this.m32;
        invl = Math.invsqrt(pzX * pzX + pzY * pzY + pzZ * pzZ);
        pzX *= invl;
        pzY *= invl;
        pzZ *= invl;
        pzW *= invl;
        return nxX * x + nxY * y + nxZ * z + nxW >= -r && pxX * x + pxY * y + pxZ * z + pxW >= -r && nyX * x + nyY * y + nyZ * z + nyW >= -r && pyX * x + pyY * y + pyZ * z + pyW >= -r && nzX * x + nzY * y + nzZ * z + nzW >= -r && pzX * x + pzY * y + pzZ * z + pzW >= -r;
    }

    public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        float nxX = this.m03 + this.m00;
        float nxY = this.m13 + this.m10;
        float nxZ = this.m23 + this.m20;
        float nxW = this.m33 + this.m30;
        float pxX = this.m03 - this.m00;
        float pxY = this.m13 - this.m10;
        float pxZ = this.m23 - this.m20;
        float pxW = this.m33 - this.m30;
        float nyX = this.m03 + this.m01;
        float nyY = this.m13 + this.m11;
        float nyZ = this.m23 + this.m21;
        float nyW = this.m33 + this.m31;
        float pyX = this.m03 - this.m01;
        float pyY = this.m13 - this.m11;
        float pyZ = this.m23 - this.m21;
        float pyW = this.m33 - this.m31;
        float nzX = this.m03 + this.m02;
        float nzY = this.m13 + this.m12;
        float nzZ = this.m23 + this.m22;
        float nzW = this.m33 + this.m32;
        float pzX = this.m03 - this.m02;
        float pzY = this.m13 - this.m12;
        float pzZ = this.m23 - this.m22;
        float pzW = this.m33 - this.m32;
        return nxX * (nxX < 0f ? minX : maxX) + nxY * (nxY < 0f ? minY : maxY) + nxZ * (nxZ < 0f ? minZ : maxZ) >= -nxW && pxX * (pxX < 0f ? minX : maxX) + pxY * (pxY < 0f ? minY : maxY) + pxZ * (pxZ < 0f ? minZ : maxZ) >= -pxW && nyX * (nyX < 0f ? minX : maxX) + nyY * (nyY < 0f ? minY : maxY) + nyZ * (nyZ < 0f ? minZ : maxZ) >= -nyW && pyX * (pyX < 0f ? minX : maxX) + pyY * (pyY < 0f ? minY : maxY) + pyZ * (pyZ < 0f ? minZ : maxZ) >= -pyW && nzX * (nzX < 0f ? minX : maxX) + nzY * (nzY < 0f ? minY : maxY) + nzZ * (nzZ < 0f ? minZ : maxZ) >= -nzW && pzX * (pzX < 0f ? minX : maxX) + pzY * (pzY < 0f ? minY : maxY) + pzZ * (pzZ < 0f ? minZ : maxZ) >= -pzW;
    }

    public Matrix4f obliqueZ(float a, float b) {
        return this._m20(this.m00 * a + this.m10 * b + this.m20)._m21(this.m01 * a + this.m11 * b + this.m21)._m22(this.m02 * a + this.m12 * b + this.m22)._properties(this.properties & 2);
    }

    public Matrix4f obliqueZ(float a, float b, Matrix4f dest) {
        dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(this.m00 * a + this.m10 * b + this.m20)._m21(this.m01 * a + this.m11 * b + this.m21)._m22(this.m02 * a + this.m12 * b + this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 2);
        return dest;
    }

    public static void projViewFromRectangle(Vector3f eye, Vector3f p, Vector3f x, Vector3f y, float nearFarDist, boolean zeroToOne, Matrix4f projDest, Matrix4f viewDest) {
        float zx = y.y * x.z - y.z * x.y;
        float zy = y.z * x.x - y.x * x.z;
        float zz = y.x * x.y - y.y * x.x;
        float zd = zx * (p.x - eye.x) + zy * (p.y - eye.y) + zz * (p.z - eye.z);
        float zs = zd >= 0f ? 1f : -1f;
        zx *= zs;
        zy *= zs;
        zz *= zs;
        zd *= zs;
        viewDest.setLookAt(eye.x, eye.y, eye.z, eye.x + zx, eye.y + zy, eye.z + zz, y.x, y.y, y.z);
        float px = viewDest.m00 * p.x + viewDest.m10 * p.y + viewDest.m20 * p.z + viewDest.m30;
        float py = viewDest.m01 * p.x + viewDest.m11 * p.y + viewDest.m21 * p.z + viewDest.m31;
        float tx = viewDest.m00 * x.x + viewDest.m10 * x.y + viewDest.m20 * x.z;
        float ty = viewDest.m01 * y.x + viewDest.m11 * y.y + viewDest.m21 * y.z;
        float len = Math.sqrt(zx * zx + zy * zy + zz * zz);
        float near = zd / len;
        float far;
        if (Float.isInfinite(nearFarDist) && nearFarDist < 0f) {
            far = near;
            near = Float.POSITIVE_INFINITY;
        } else if (Float.isInfinite(nearFarDist) && nearFarDist > 0f) {
            far = Float.POSITIVE_INFINITY;
        } else if (nearFarDist < 0f) {
            far = near;
            near += nearFarDist;
        } else {
            far = near + nearFarDist;
        }

        projDest.setFrustum(px, px + tx, py, py + ty, near, far, zeroToOne);
    }

    public Matrix4f withLookAtUp(Vector3f up) {
        return this.withLookAtUp(up.x, up.y, up.z, this);
    }

    public Matrix4f withLookAtUp(Vector3f up, Matrix4f dest) {
        return this.withLookAtUp(up.x, up.y, up.z);
    }

    public Matrix4f withLookAtUp(float upX, float upY, float upZ) {
        return this.withLookAtUp(upX, upY, upZ, this);
    }

    public Matrix4f withLookAtUp(float upX, float upY, float upZ, Matrix4f dest) {
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
            dest._m02(this.m02)._m12(this.m12)._m22(this.m22)._m32(this.m32)._m03(this.m03)._m13(this.m13)._m23(this.m23)._m33(this.m33);
        }

        dest._properties(this.properties & -14);
        return dest;
    }

    public Matrix4f mapXZY() {
        return this.mapXZY(this);
    }

    public Matrix4f mapXZY(Matrix4f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapXZnY() {
        return this.mapXZnY(this);
    }

    public Matrix4f mapXZnY(Matrix4f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapXnYnZ() {
        return this.mapXnYnZ(this);
    }

    public Matrix4f mapXnYnZ(Matrix4f dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapXnZY() {
        return this.mapXnZY(this);
    }

    public Matrix4f mapXnZY(Matrix4f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapXnZnY() {
        return this.mapXnZnY(this);
    }

    public Matrix4f mapXnZnY(Matrix4f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapYXZ() {
        return this.mapYXZ(this);
    }

    public Matrix4f mapYXZ(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapYXnZ() {
        return this.mapYXnZ(this);
    }

    public Matrix4f mapYXnZ(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapYZX() {
        return this.mapYZX(this);
    }

    public Matrix4f mapYZX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapYZnX() {
        return this.mapYZnX(this);
    }

    public Matrix4f mapYZnX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapYnXZ() {
        return this.mapYnXZ(this);
    }

    public Matrix4f mapYnXZ(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapYnXnZ() {
        return this.mapYnXnZ(this);
    }

    public Matrix4f mapYnXnZ(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapYnZX() {
        return this.mapYnZX(this);
    }

    public Matrix4f mapYnZX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapYnZnX() {
        return this.mapYnZnX(this);
    }

    public Matrix4f mapYnZnX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapZXY() {
        return this.mapZXY(this);
    }

    public Matrix4f mapZXY(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapZXnY() {
        return this.mapZXnY(this);
    }

    public Matrix4f mapZXnY(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapZYX() {
        return this.mapZYX(this);
    }

    public Matrix4f mapZYX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapZYnX() {
        return this.mapZYnX(this);
    }

    public Matrix4f mapZYnX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapZnXY() {
        return this.mapZnXY(this);
    }

    public Matrix4f mapZnXY(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapZnXnY() {
        return this.mapZnXnY(this);
    }

    public Matrix4f mapZnXnY(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapZnYX() {
        return this.mapZnYX(this);
    }

    public Matrix4f mapZnYX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapZnYnX() {
        return this.mapZnYnX(this);
    }

    public Matrix4f mapZnYnX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnXYnZ() {
        return this.mapnXYnZ(this);
    }

    public Matrix4f mapnXYnZ(Matrix4f dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnXZY() {
        return this.mapnXZY(this);
    }

    public Matrix4f mapnXZY(Matrix4f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnXZnY() {
        return this.mapnXZnY(this);
    }

    public Matrix4f mapnXZnY(Matrix4f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnXnYZ() {
        return this.mapnXnYZ(this);
    }

    public Matrix4f mapnXnYZ(Matrix4f dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnXnYnZ() {
        return this.mapnXnYnZ(this);
    }

    public Matrix4f mapnXnYnZ(Matrix4f dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnXnZY() {
        return this.mapnXnZY(this);
    }

    public Matrix4f mapnXnZY(Matrix4f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnXnZnY() {
        return this.mapnXnZnY(this);
    }

    public Matrix4f mapnXnZnY(Matrix4f dest) {
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnYXZ() {
        return this.mapnYXZ(this);
    }

    public Matrix4f mapnYXZ(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnYXnZ() {
        return this.mapnYXnZ(this);
    }

    public Matrix4f mapnYXnZ(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnYZX() {
        return this.mapnYZX(this);
    }

    public Matrix4f mapnYZX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnYZnX() {
        return this.mapnYZnX(this);
    }

    public Matrix4f mapnYZnX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnYnXZ() {
        return this.mapnYnXZ(this);
    }

    public Matrix4f mapnYnXZ(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnYnXnZ() {
        return this.mapnYnXnZ(this);
    }

    public Matrix4f mapnYnXnZ(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnYnZX() {
        return this.mapnYnZX(this);
    }

    public Matrix4f mapnYnZX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnYnZnX() {
        return this.mapnYnZnX(this);
    }

    public Matrix4f mapnYnZnX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnZXY() {
        return this.mapnZXY(this);
    }

    public Matrix4f mapnZXY(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnZXnY() {
        return this.mapnZXnY(this);
    }

    public Matrix4f mapnZXnY(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnZYX() {
        return this.mapnZYX(this);
    }

    public Matrix4f mapnZYX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnZYnX() {
        return this.mapnZYnX(this);
    }

    public Matrix4f mapnZYnX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnZnXY() {
        return this.mapnZnXY(this);
    }

    public Matrix4f mapnZnXY(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnZnXnY() {
        return this.mapnZnXnY(this);
    }

    public Matrix4f mapnZnXnY(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        float m10 = this.m10;
        float m11 = this.m11;
        float m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnZnYX() {
        return this.mapnZnYX(this);
    }

    public Matrix4f mapnZnYX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f mapnZnYnX() {
        return this.mapnZnYnX(this);
    }

    public Matrix4f mapnZnYnX(Matrix4f dest) {
        float m00 = this.m00;
        float m01 = this.m01;
        float m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f negateX() {
        return this._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._properties(this.properties & 18);
    }

    public Matrix4f negateX(Matrix4f dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f negateY() {
        return this._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._properties(this.properties & 18);
    }

    public Matrix4f negateY(Matrix4f dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4f negateZ() {
        return this._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._properties(this.properties & 18);
    }

    public Matrix4f negateZ(Matrix4f dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public boolean isFinite() {
        return Math.isFinite(this.m00) && Math.isFinite(this.m01) && Math.isFinite(this.m02) && Math.isFinite(this.m03) && Math.isFinite(this.m10) && Math.isFinite(this.m11) && Math.isFinite(this.m12) && Math.isFinite(this.m13) && Math.isFinite(this.m20) && Math.isFinite(this.m21) && Math.isFinite(this.m22) && Math.isFinite(this.m23) && Math.isFinite(this.m30) && Math.isFinite(this.m31) && Math.isFinite(this.m32) && Math.isFinite(this.m33);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
