package org.joml;

import java.text.NumberFormat;

@SuppressWarnings("unused")
public class Matrix4d {

    public double m00;
    public double m01;
    public double m02;
    public double m03;
    public double m10;
    public double m11;
    public double m12;
    public double m13;
    public double m20;
    public double m21;
    public double m22;
    public double m23;
    public double m30;
    public double m31;
    public double m32;
    public double m33;
    public int properties;

    public Matrix4d() {
        this._m00(1.0)._m11(1.0)._m22(1.0)._m33(1.0).properties = 30;
    }

    public Matrix4d(Matrix4d mat) {
        this.set(mat);
    }

    public Matrix4d(Matrix4f mat) {
        this.set(mat);
    }

    public Matrix4d(Matrix4x3d mat) {
        this.set(mat);
    }

    public Matrix4d(Matrix4x3f mat) {
        this.set(mat);
    }

    public Matrix4d(Matrix3d mat) {
        this.set(mat);
    }

    public Matrix4d(double m00, double m01, double m02, double m03, double m10, double m11, double m12, double m13, double m20, double m21, double m22, double m23, double m30, double m31, double m32, double m33) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
        this.determineProperties();
    }

    public Matrix4d(Vector4d col0, Vector4d col1, Vector4d col2, Vector4d col3) {
        this.set(col0, col1, col2, col3);
    }

    public Matrix4d assume(int properties) {
        this.properties = (byte)properties;
        return this;
    }

    public Matrix4d determineProperties() {
        int properties = 0;
        if (this.m03 == 0.0 && this.m13 == 0.0) {
            if (this.m23 == 0.0 && this.m33 == 1.0) {
                properties |= 2;
                if (this.m00 == 1.0 && this.m01 == 0.0 && this.m02 == 0.0 && this.m10 == 0.0 && this.m11 == 1.0 && this.m12 == 0.0 && this.m20 == 0.0 && this.m21 == 0.0 && this.m22 == 1.0) {
                    properties |= 24;
                    if (this.m30 == 0.0 && this.m31 == 0.0 && this.m32 == 0.0) {
                        properties |= 4;
                    }
                }
            } else if (this.m01 == 0.0 && this.m02 == 0.0 && this.m10 == 0.0 && this.m12 == 0.0 && this.m20 == 0.0 && this.m21 == 0.0 && this.m30 == 0.0 && this.m31 == 0.0 && this.m33 == 0.0) {
                properties |= 1;
            }
        }

        this.properties = properties;
        return this;
    }

    public int properties() {
        return this.properties;
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

    public double m03() {
        return this.m03;
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

    public double m13() {
        return this.m13;
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

    public double m23() {
        return this.m23;
    }

    public double m30() {
        return this.m30;
    }

    public double m31() {
        return this.m31;
    }

    public double m32() {
        return this.m32;
    }

    public double m33() {
        return this.m33;
    }

    public Matrix4d m00(double m00) {
        this.m00 = m00;
        this.properties &= -17;
        if (m00 != 1.0) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4d m01(double m01) {
        this.m01 = m01;
        this.properties &= -17;
        if (m01 != 0.0) {
            this.properties &= -14;
        }

        return this;
    }

    public Matrix4d m02(double m02) {
        this.m02 = m02;
        this.properties &= -17;
        if (m02 != 0.0) {
            this.properties &= -14;
        }

        return this;
    }

    public Matrix4d m03(double m03) {
        this.m03 = m03;
        if (m03 != 0.0) {
            this.properties = 0;
        }

        return this;
    }

    public Matrix4d m10(double m10) {
        this.m10 = m10;
        this.properties &= -17;
        if (m10 != 0.0) {
            this.properties &= -14;
        }

        return this;
    }

    public Matrix4d m11(double m11) {
        this.m11 = m11;
        this.properties &= -17;
        if (m11 != 1.0) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4d m12(double m12) {
        this.m12 = m12;
        this.properties &= -17;
        if (m12 != 0.0) {
            this.properties &= -14;
        }

        return this;
    }

    public Matrix4d m13(double m13) {
        this.m13 = m13;
        if (this.m03 != 0.0) {
            this.properties = 0;
        }

        return this;
    }

    public Matrix4d m20(double m20) {
        this.m20 = m20;
        this.properties &= -17;
        if (m20 != 0.0) {
            this.properties &= -14;
        }

        return this;
    }

    public Matrix4d m21(double m21) {
        this.m21 = m21;
        this.properties &= -17;
        if (m21 != 0.0) {
            this.properties &= -14;
        }

        return this;
    }

    public Matrix4d m22(double m22) {
        this.m22 = m22;
        this.properties &= -17;
        if (m22 != 1.0) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4d m23(double m23) {
        this.m23 = m23;
        if (m23 != 0.0) {
            this.properties &= -31;
        }

        return this;
    }

    public Matrix4d m30(double m30) {
        this.m30 = m30;
        if (m30 != 0.0) {
            this.properties &= -6;
        }

        return this;
    }

    public Matrix4d m31(double m31) {
        this.m31 = m31;
        if (m31 != 0.0) {
            this.properties &= -6;
        }

        return this;
    }

    public Matrix4d m32(double m32) {
        this.m32 = m32;
        if (m32 != 0.0) {
            this.properties &= -6;
        }

        return this;
    }

    public Matrix4d m33(double m33) {
        this.m33 = m33;
        if (m33 != 0.0) {
            this.properties &= -2;
        }

        if (m33 != 1.0) {
            this.properties &= -31;
        }

        return this;
    }

    Matrix4d _properties(int properties) {
        this.properties = properties;
        return this;
    }

    Matrix4d _m00(double m00) {
        this.m00 = m00;
        return this;
    }

    Matrix4d _m01(double m01) {
        this.m01 = m01;
        return this;
    }

    Matrix4d _m02(double m02) {
        this.m02 = m02;
        return this;
    }

    Matrix4d _m03(double m03) {
        this.m03 = m03;
        return this;
    }

    Matrix4d _m10(double m10) {
        this.m10 = m10;
        return this;
    }

    Matrix4d _m11(double m11) {
        this.m11 = m11;
        return this;
    }

    Matrix4d _m12(double m12) {
        this.m12 = m12;
        return this;
    }

    Matrix4d _m13(double m13) {
        this.m13 = m13;
        return this;
    }

    Matrix4d _m20(double m20) {
        this.m20 = m20;
        return this;
    }

    Matrix4d _m21(double m21) {
        this.m21 = m21;
        return this;
    }

    Matrix4d _m22(double m22) {
        this.m22 = m22;
        return this;
    }

    Matrix4d _m23(double m23) {
        this.m23 = m23;
        return this;
    }

    Matrix4d _m30(double m30) {
        this.m30 = m30;
        return this;
    }

    Matrix4d _m31(double m31) {
        this.m31 = m31;
        return this;
    }

    Matrix4d _m32(double m32) {
        this.m32 = m32;
        return this;
    }

    Matrix4d _m33(double m33) {
        this.m33 = m33;
        return this;
    }

    public Matrix4d identity() {
        if ((this.properties & 4) == 0) {
            this._identity();
            this.properties = 30;
        }
        return this;
    }

    private void _identity() {
        this._m00(1.0)._m10(0.0)._m20(0.0)._m30(0.0)._m01(0.0)._m11(1.0)._m21(0.0)._m31(0.0)._m02(0.0)._m12(0.0)._m22(1.0)._m32(0.0)._m03(0.0)._m13(0.0)._m23(0.0)._m33(1.0);
    }

    public Matrix4d set(Matrix4d m) {
        return this._m00(m.m00)._m01(m.m01)._m02(m.m02)._m03(m.m03)._m10(m.m10)._m11(m.m11)._m12(m.m12)._m13(m.m13)._m20(m.m20)._m21(m.m21)._m22(m.m22)._m23(m.m23)._m30(m.m30)._m31(m.m31)._m32(m.m32)._m33(m.m33)._properties(m.properties());
    }

    public Matrix4d set(Matrix4f m) {
        return this._m00(m.m00)._m01(m.m01)._m02(m.m02)._m03(m.m03)._m10(m.m10)._m11(m.m11)._m12(m.m12)._m13(m.m13)._m20(m.m20)._m21(m.m21)._m22(m.m22)._m23(m.m23)._m30(m.m30)._m31(m.m31)._m32(m.m32)._m33(m.m33)._properties(m.properties());
    }

    public Matrix4d setTransposed(Matrix4d m) {
        return (m.properties() & 4) != 0 ? this.identity() : this.setTransposedInternal(m);
    }

    private Matrix4d setTransposedInternal(Matrix4d m) {
        double nm10 = m.m01;
        double nm12 = m.m21;
        double nm13 = m.m31;
        double nm20 = m.m02;
        double nm21 = m.m12;
        double nm30 = m.m03;
        double nm31 = m.m13;
        double nm32 = m.m23;
        return this._m00(m.m00)._m01(m.m10)._m02(m.m20)._m03(m.m30)._m10(nm10)._m11(m.m11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(m.m22)._m23(m.m32)._m30(nm30)._m31(nm31)._m32(nm32)._m33(m.m33)._properties(m.properties() & 4);
    }

    public Matrix4d set(Matrix4x3d m) {
        return this._m00(m.m00)._m01(m.m01)._m02(m.m02)._m03(0.0)._m10(m.m10)._m11(m.m11)._m12(m.m12)._m13(0.0)._m20(m.m20)._m21(m.m21)._m22(m.m22)._m23(0.0)._m30(m.m30)._m31(m.m31)._m32(m.m32)._m33(1.0)._properties(m.properties() | 2);
    }

    public Matrix4d set(Matrix4x3f m) {
        return this._m00(m.m00)._m01(m.m01)._m02(m.m02)._m03(0.0)._m10(m.m10)._m11(m.m11)._m12(m.m12)._m13(0.0)._m20(m.m20)._m21(m.m21)._m22(m.m22)._m23(0.0)._m30(m.m30)._m31(m.m31)._m32(m.m32)._m33(1.0)._properties(m.properties() | 2);
    }

    public Matrix4d set(Matrix3d mat) {
        return this._m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m03(0.0)._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)._m13(0.0)._m20(mat.m20)._m21(mat.m21)._m22(mat.m22)._m23(0.0)._m30(0.0)._m31(0.0)._m32(0.0)._m33(1.0)._properties(2);
    }

    public Matrix4d set3x3(Matrix4d mat) {
        return this._m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)._m20(mat.m20)._m21(mat.m21)._m22(mat.m22)._properties(this.properties & mat.properties() & -2);
    }

    public Matrix4d set4x3(Matrix4x3d mat) {
        return this._m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)._m20(mat.m20)._m21(mat.m21)._m22(mat.m22)._m30(mat.m30)._m31(mat.m31)._m32(mat.m32)._properties(this.properties & mat.properties() & -2);
    }

    public Matrix4d set4x3(Matrix4x3f mat) {
        return this._m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)._m20(mat.m20)._m21(mat.m21)._m22(mat.m22)._m30(mat.m30)._m31(mat.m31)._m32(mat.m32)._properties(this.properties & mat.properties() & -2);
    }

    public Matrix4d set4x3(Matrix4d mat) {
        return this._m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)._m20(mat.m20)._m21(mat.m21)._m22(mat.m22)._m30(mat.m30)._m31(mat.m31)._m32(mat.m32)._properties(this.properties & mat.properties() & -2);
    }

    public Matrix4d set(AxisAngle4f axisAngle) {
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
        this._m00(c + x * x * omc)._m11(c + y * y * omc)._m22(c + z * z * omc);
        double tmp1 = x * y * omc;
        double tmp2 = z * s;
        this._m10(tmp1 - tmp2)._m01(tmp1 + tmp2);
        tmp1 = x * z * omc;
        tmp2 = y * s;
        this._m20(tmp1 + tmp2)._m02(tmp1 - tmp2);
        tmp1 = y * z * omc;
        tmp2 = x * s;
        this._m21(tmp1 - tmp2)._m12(tmp1 + tmp2)._m03(0.0)._m13(0.0)._m23(0.0)._m30(0.0)._m31(0.0)._m32(0.0)._m33(1.0).properties = 18;
        return this;
    }

    public Matrix4d set(AxisAngle4d axisAngle) {
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
        this._m00(c + x * x * omc)._m11(c + y * y * omc)._m22(c + z * z * omc);
        double tmp1 = x * y * omc;
        double tmp2 = z * s;
        this._m10(tmp1 - tmp2)._m01(tmp1 + tmp2);
        tmp1 = x * z * omc;
        tmp2 = y * s;
        this._m20(tmp1 + tmp2)._m02(tmp1 - tmp2);
        tmp1 = y * z * omc;
        tmp2 = x * s;
        this._m21(tmp1 - tmp2)._m12(tmp1 + tmp2)._m03(0.0)._m13(0.0)._m23(0.0)._m30(0.0)._m31(0.0)._m32(0.0)._m33(1.0).properties = 18;
        return this;
    }

    public Matrix4d set(Quaternionf q) {
        return this.rotation(q);
    }

    public Matrix4d set(Quaterniond q) {
        return this.rotation(q);
    }

    public Matrix4d mul(Matrix4d right) {
        return this.mul(right, this);
    }

    public Matrix4d mul(Matrix4d right, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.set(right);
        } else if ((right.properties() & 4) != 0) {
            return dest.set(this);
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

    public Matrix4d mul0(Matrix4d right) {
        return this.mul0(right, this);
    }

    public Matrix4d mul0(Matrix4d right, Matrix4d dest) {
        double nm00 = Math.fma(this.m00, right.m00, Math.fma(this.m10, right.m01, Math.fma(this.m20, right.m02, this.m30 * right.m03)));
        double nm01 = Math.fma(this.m01, right.m00, Math.fma(this.m11, right.m01, Math.fma(this.m21, right.m02, this.m31 * right.m03)));
        double nm02 = Math.fma(this.m02, right.m00, Math.fma(this.m12, right.m01, Math.fma(this.m22, right.m02, this.m32 * right.m03)));
        double nm03 = Math.fma(this.m03, right.m00, Math.fma(this.m13, right.m01, Math.fma(this.m23, right.m02, this.m33 * right.m03)));
        double nm10 = Math.fma(this.m00, right.m10, Math.fma(this.m10, right.m11, Math.fma(this.m20, right.m12, this.m30 * right.m13)));
        double nm11 = Math.fma(this.m01, right.m10, Math.fma(this.m11, right.m11, Math.fma(this.m21, right.m12, this.m31 * right.m13)));
        double nm12 = Math.fma(this.m02, right.m10, Math.fma(this.m12, right.m11, Math.fma(this.m22, right.m12, this.m32 * right.m13)));
        double nm13 = Math.fma(this.m03, right.m10, Math.fma(this.m13, right.m11, Math.fma(this.m23, right.m12, this.m33 * right.m13)));
        double nm20 = Math.fma(this.m00, right.m20, Math.fma(this.m10, right.m21, Math.fma(this.m20, right.m22, this.m30 * right.m23)));
        double nm21 = Math.fma(this.m01, right.m20, Math.fma(this.m11, right.m21, Math.fma(this.m21, right.m22, this.m31 * right.m23)));
        double nm22 = Math.fma(this.m02, right.m20, Math.fma(this.m12, right.m21, Math.fma(this.m22, right.m22, this.m32 * right.m23)));
        double nm23 = Math.fma(this.m03, right.m20, Math.fma(this.m13, right.m21, Math.fma(this.m23, right.m22, this.m33 * right.m23)));
        double nm30 = Math.fma(this.m00, right.m30, Math.fma(this.m10, right.m31, Math.fma(this.m20, right.m32, this.m30 * right.m33)));
        double nm31 = Math.fma(this.m01, right.m30, Math.fma(this.m11, right.m31, Math.fma(this.m21, right.m32, this.m31 * right.m33)));
        double nm32 = Math.fma(this.m02, right.m30, Math.fma(this.m12, right.m31, Math.fma(this.m22, right.m32, this.m32 * right.m33)));
        double nm33 = Math.fma(this.m03, right.m30, Math.fma(this.m13, right.m31, Math.fma(this.m23, right.m32, this.m33 * right.m33)));
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
    }

    public Matrix4d mul(double r00, double r01, double r02, double r03, double r10, double r11, double r12, double r13, double r20, double r21, double r22, double r23, double r30, double r31, double r32, double r33) {
        return this.mul(r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23, r30, r31, r32, r33, this);
    }

    public Matrix4d mul(double r00, double r01, double r02, double r03, double r10, double r11, double r12, double r13, double r20, double r21, double r22, double r23, double r30, double r31, double r32, double r33, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.set(r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23, r30, r31, r32, r33);
        } else {
            return (this.properties & 2) != 0 ? this.mulAffineL(r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23, r30, r31, r32, r33, dest) : this.mulGeneric(r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23, r30, r31, r32, r33, dest);
        }
    }

    private Matrix4d mulAffineL(double r00, double r01, double r02, double r03, double r10, double r11, double r12, double r13, double r20, double r21, double r22, double r23, double r30, double r31, double r32, double r33, Matrix4d dest) {
        double nm00 = Math.fma(this.m00, r00, Math.fma(this.m10, r01, Math.fma(this.m20, r02, this.m30 * r03)));
        double nm01 = Math.fma(this.m01, r00, Math.fma(this.m11, r01, Math.fma(this.m21, r02, this.m31 * r03)));
        double nm02 = Math.fma(this.m02, r00, Math.fma(this.m12, r01, Math.fma(this.m22, r02, this.m32 * r03)));
        double nm10 = Math.fma(this.m00, r10, Math.fma(this.m10, r11, Math.fma(this.m20, r12, this.m30 * r13)));
        double nm11 = Math.fma(this.m01, r10, Math.fma(this.m11, r11, Math.fma(this.m21, r12, this.m31 * r13)));
        double nm12 = Math.fma(this.m02, r10, Math.fma(this.m12, r11, Math.fma(this.m22, r12, this.m32 * r13)));
        double nm20 = Math.fma(this.m00, r20, Math.fma(this.m10, r21, Math.fma(this.m20, r22, this.m30 * r23)));
        double nm21 = Math.fma(this.m01, r20, Math.fma(this.m11, r21, Math.fma(this.m21, r22, this.m31 * r23)));
        double nm22 = Math.fma(this.m02, r20, Math.fma(this.m12, r21, Math.fma(this.m22, r22, this.m32 * r23)));
        double nm30 = Math.fma(this.m00, r30, Math.fma(this.m10, r31, Math.fma(this.m20, r32, this.m30 * r33)));
        double nm31 = Math.fma(this.m01, r30, Math.fma(this.m11, r31, Math.fma(this.m21, r32, this.m31 * r33)));
        double nm32 = Math.fma(this.m02, r30, Math.fma(this.m12, r31, Math.fma(this.m22, r32, this.m32 * r33)));
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(r03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(r13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(r23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(r33)._properties(2);
    }

    private Matrix4d mulGeneric(double r00, double r01, double r02, double r03, double r10, double r11, double r12, double r13, double r20, double r21, double r22, double r23, double r30, double r31, double r32, double r33, Matrix4d dest) {
        double nm00 = Math.fma(this.m00, r00, Math.fma(this.m10, r01, Math.fma(this.m20, r02, this.m30 * r03)));
        double nm01 = Math.fma(this.m01, r00, Math.fma(this.m11, r01, Math.fma(this.m21, r02, this.m31 * r03)));
        double nm02 = Math.fma(this.m02, r00, Math.fma(this.m12, r01, Math.fma(this.m22, r02, this.m32 * r03)));
        double nm03 = Math.fma(this.m03, r00, Math.fma(this.m13, r01, Math.fma(this.m23, r02, this.m33 * r03)));
        double nm10 = Math.fma(this.m00, r10, Math.fma(this.m10, r11, Math.fma(this.m20, r12, this.m30 * r13)));
        double nm11 = Math.fma(this.m01, r10, Math.fma(this.m11, r11, Math.fma(this.m21, r12, this.m31 * r13)));
        double nm12 = Math.fma(this.m02, r10, Math.fma(this.m12, r11, Math.fma(this.m22, r12, this.m32 * r13)));
        double nm13 = Math.fma(this.m03, r10, Math.fma(this.m13, r11, Math.fma(this.m23, r12, this.m33 * r13)));
        double nm20 = Math.fma(this.m00, r20, Math.fma(this.m10, r21, Math.fma(this.m20, r22, this.m30 * r23)));
        double nm21 = Math.fma(this.m01, r20, Math.fma(this.m11, r21, Math.fma(this.m21, r22, this.m31 * r23)));
        double nm22 = Math.fma(this.m02, r20, Math.fma(this.m12, r21, Math.fma(this.m22, r22, this.m32 * r23)));
        double nm23 = Math.fma(this.m03, r20, Math.fma(this.m13, r21, Math.fma(this.m23, r22, this.m33 * r23)));
        double nm30 = Math.fma(this.m00, r30, Math.fma(this.m10, r31, Math.fma(this.m20, r32, this.m30 * r33)));
        double nm31 = Math.fma(this.m01, r30, Math.fma(this.m11, r31, Math.fma(this.m21, r32, this.m31 * r33)));
        double nm32 = Math.fma(this.m02, r30, Math.fma(this.m12, r31, Math.fma(this.m22, r32, this.m32 * r33)));
        double nm33 = Math.fma(this.m03, r30, Math.fma(this.m13, r31, Math.fma(this.m23, r32, this.m33 * r33)));
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
    }

    public Matrix4d mul3x3(double r00, double r01, double r02, double r10, double r11, double r12, double r20, double r21, double r22) {
        return this.mul3x3(r00, r01, r02, r10, r11, r12, r20, r21, r22, this);
    }

    public Matrix4d mul3x3(double r00, double r01, double r02, double r10, double r11, double r12, double r20, double r21, double r22, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.set(r00, r01, r02, 0.0, r10, r11, r12, 0.0, r20, r21, r22, 0.0, 0.0, 0.0, 0.0, 1.0) : this.mulGeneric3x3(r00, r01, r02, r10, r11, r12, r20, r21, r22, dest);
    }

    private Matrix4d mulGeneric3x3(double r00, double r01, double r02, double r10, double r11, double r12, double r20, double r21, double r22, Matrix4d dest) {
        double nm00 = Math.fma(this.m00, r00, Math.fma(this.m10, r01, this.m20 * r02));
        double nm01 = Math.fma(this.m01, r00, Math.fma(this.m11, r01, this.m21 * r02));
        double nm02 = Math.fma(this.m02, r00, Math.fma(this.m12, r01, this.m22 * r02));
        double nm03 = Math.fma(this.m03, r00, Math.fma(this.m13, r01, this.m23 * r02));
        double nm10 = Math.fma(this.m00, r10, Math.fma(this.m10, r11, this.m20 * r12));
        double nm11 = Math.fma(this.m01, r10, Math.fma(this.m11, r11, this.m21 * r12));
        double nm12 = Math.fma(this.m02, r10, Math.fma(this.m12, r11, this.m22 * r12));
        double nm13 = Math.fma(this.m03, r10, Math.fma(this.m13, r11, this.m23 * r12));
        double nm20 = Math.fma(this.m00, r20, Math.fma(this.m10, r21, this.m20 * r22));
        double nm21 = Math.fma(this.m01, r20, Math.fma(this.m11, r21, this.m21 * r22));
        double nm22 = Math.fma(this.m02, r20, Math.fma(this.m12, r21, this.m22 * r22));
        double nm23 = Math.fma(this.m03, r20, Math.fma(this.m13, r21, this.m23 * r22));
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 2);
    }

    public Matrix4d mulLocal(Matrix4d left) {
        return this.mulLocal(left, this);
    }

    public Matrix4d mulLocal(Matrix4d left, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.set(left);
        } else if ((left.properties() & 4) != 0) {
            return dest.set(this);
        } else {
            return (this.properties & 2) != 0 && (left.properties() & 2) != 0 ? this.mulLocalAffine(left, dest) : this.mulLocalGeneric(left, dest);
        }
    }

    private Matrix4d mulLocalGeneric(Matrix4d left, Matrix4d dest) {
        double nm00 = Math.fma(left.m00, this.m00, Math.fma(left.m10, this.m01, Math.fma(left.m20, this.m02, left.m30 * this.m03)));
        double nm01 = Math.fma(left.m01, this.m00, Math.fma(left.m11, this.m01, Math.fma(left.m21, this.m02, left.m31 * this.m03)));
        double nm02 = Math.fma(left.m02, this.m00, Math.fma(left.m12, this.m01, Math.fma(left.m22, this.m02, left.m32 * this.m03)));
        double nm03 = Math.fma(left.m03, this.m00, Math.fma(left.m13, this.m01, Math.fma(left.m23, this.m02, left.m33 * this.m03)));
        double nm10 = Math.fma(left.m00, this.m10, Math.fma(left.m10, this.m11, Math.fma(left.m20, this.m12, left.m30 * this.m13)));
        double nm11 = Math.fma(left.m01, this.m10, Math.fma(left.m11, this.m11, Math.fma(left.m21, this.m12, left.m31 * this.m13)));
        double nm12 = Math.fma(left.m02, this.m10, Math.fma(left.m12, this.m11, Math.fma(left.m22, this.m12, left.m32 * this.m13)));
        double nm13 = Math.fma(left.m03, this.m10, Math.fma(left.m13, this.m11, Math.fma(left.m23, this.m12, left.m33 * this.m13)));
        double nm20 = Math.fma(left.m00, this.m20, Math.fma(left.m10, this.m21, Math.fma(left.m20, this.m22, left.m30 * this.m23)));
        double nm21 = Math.fma(left.m01, this.m20, Math.fma(left.m11, this.m21, Math.fma(left.m21, this.m22, left.m31 * this.m23)));
        double nm22 = Math.fma(left.m02, this.m20, Math.fma(left.m12, this.m21, Math.fma(left.m22, this.m22, left.m32 * this.m23)));
        double nm23 = Math.fma(left.m03, this.m20, Math.fma(left.m13, this.m21, Math.fma(left.m23, this.m22, left.m33 * this.m23)));
        double nm30 = Math.fma(left.m00, this.m30, Math.fma(left.m10, this.m31, Math.fma(left.m20, this.m32, left.m30 * this.m33)));
        double nm31 = Math.fma(left.m01, this.m30, Math.fma(left.m11, this.m31, Math.fma(left.m21, this.m32, left.m31 * this.m33)));
        double nm32 = Math.fma(left.m02, this.m30, Math.fma(left.m12, this.m31, Math.fma(left.m22, this.m32, left.m32 * this.m33)));
        double nm33 = Math.fma(left.m03, this.m30, Math.fma(left.m13, this.m31, Math.fma(left.m23, this.m32, left.m33 * this.m33)));
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
    }

    public Matrix4d mulLocalAffine(Matrix4d left) {
        return this.mulLocalAffine(left, this);
    }

    public Matrix4d mulLocalAffine(Matrix4d left, Matrix4d dest) {
        double nm00 = left.m00 * this.m00 + left.m10 * this.m01 + left.m20 * this.m02;
        double nm01 = left.m01 * this.m00 + left.m11 * this.m01 + left.m21 * this.m02;
        double nm02 = left.m02 * this.m00 + left.m12 * this.m01 + left.m22 * this.m02;
        double nm03 = left.m03;
        double nm10 = left.m00 * this.m10 + left.m10 * this.m11 + left.m20 * this.m12;
        double nm11 = left.m01 * this.m10 + left.m11 * this.m11 + left.m21 * this.m12;
        double nm12 = left.m02 * this.m10 + left.m12 * this.m11 + left.m22 * this.m12;
        double nm13 = left.m13;
        double nm20 = left.m00 * this.m20 + left.m10 * this.m21 + left.m20 * this.m22;
        double nm21 = left.m01 * this.m20 + left.m11 * this.m21 + left.m21 * this.m22;
        double nm22 = left.m02 * this.m20 + left.m12 * this.m21 + left.m22 * this.m22;
        double nm23 = left.m23;
        double nm30 = left.m00 * this.m30 + left.m10 * this.m31 + left.m20 * this.m32 + left.m30;
        double nm31 = left.m01 * this.m30 + left.m11 * this.m31 + left.m21 * this.m32 + left.m31;
        double nm32 = left.m02 * this.m30 + left.m12 * this.m31 + left.m22 * this.m32 + left.m32;
        double nm33 = left.m33;
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(2);
        return dest;
    }

    public Matrix4d mul(Matrix4x3d right) {
        return this.mul(right, this);
    }

    public Matrix4d mul(Matrix4x3d right, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.set(right);
        } else if ((right.properties() & 4) != 0) {
            return dest.set(this);
        } else if ((this.properties & 8) != 0) {
            return this.mulTranslation(right, dest);
        } else if ((this.properties & 2) != 0) {
            return this.mulAffine(right, dest);
        } else {
            return (this.properties & 1) != 0 ? this.mulPerspectiveAffine(right, dest) : this.mulGeneric(right, dest);
        }
    }

    private Matrix4d mulTranslation(Matrix4x3d right, Matrix4d dest) {
        return dest._m00(right.m00)._m01(right.m01)._m02(right.m02)._m03(this.m03)._m10(right.m10)._m11(right.m11)._m12(right.m12)._m13(this.m13)._m20(right.m20)._m21(right.m21)._m22(right.m22)._m23(this.m23)._m30(right.m30 + this.m30)._m31(right.m31 + this.m31)._m32(right.m32 + this.m32)._m33(this.m33)._properties(2 | right.properties() & 16);
    }

    private Matrix4d mulAffine(Matrix4x3d right, Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        double m20 = this.m20;
        double m21 = this.m21;
        double m22 = this.m22;
        double rm00 = right.m00;
        double rm01 = right.m01;
        double rm02 = right.m02;
        double rm10 = right.m10;
        double rm11 = right.m11;
        double rm12 = right.m12;
        double rm20 = right.m20;
        double rm21 = right.m21;
        double rm22 = right.m22;
        double rm30 = right.m30;
        double rm31 = right.m31;
        double rm32 = right.m32;
        return dest._m00(Math.fma(m00, rm00, Math.fma(m10, rm01, m20 * rm02)))._m01(Math.fma(m01, rm00, Math.fma(m11, rm01, m21 * rm02)))._m02(Math.fma(m02, rm00, Math.fma(m12, rm01, m22 * rm02)))._m03(this.m03)._m10(Math.fma(m00, rm10, Math.fma(m10, rm11, m20 * rm12)))._m11(Math.fma(m01, rm10, Math.fma(m11, rm11, m21 * rm12)))._m12(Math.fma(m02, rm10, Math.fma(m12, rm11, m22 * rm12)))._m13(this.m13)._m20(Math.fma(m00, rm20, Math.fma(m10, rm21, m20 * rm22)))._m21(Math.fma(m01, rm20, Math.fma(m11, rm21, m21 * rm22)))._m22(Math.fma(m02, rm20, Math.fma(m12, rm21, m22 * rm22)))._m23(this.m23)._m30(Math.fma(m00, rm30, Math.fma(m10, rm31, Math.fma(m20, rm32, this.m30))))._m31(Math.fma(m01, rm30, Math.fma(m11, rm31, Math.fma(m21, rm32, this.m31))))._m32(Math.fma(m02, rm30, Math.fma(m12, rm31, Math.fma(m22, rm32, this.m32))))._m33(this.m33)._properties(2 | this.properties & right.properties() & 16);
    }

    private Matrix4d mulGeneric(Matrix4x3d right, Matrix4d dest) {
        double nm00 = Math.fma(this.m00, right.m00, Math.fma(this.m10, right.m01, this.m20 * right.m02));
        double nm01 = Math.fma(this.m01, right.m00, Math.fma(this.m11, right.m01, this.m21 * right.m02));
        double nm02 = Math.fma(this.m02, right.m00, Math.fma(this.m12, right.m01, this.m22 * right.m02));
        double nm03 = Math.fma(this.m03, right.m00, Math.fma(this.m13, right.m01, this.m23 * right.m02));
        double nm10 = Math.fma(this.m00, right.m10, Math.fma(this.m10, right.m11, this.m20 * right.m12));
        double nm11 = Math.fma(this.m01, right.m10, Math.fma(this.m11, right.m11, this.m21 * right.m12));
        double nm12 = Math.fma(this.m02, right.m10, Math.fma(this.m12, right.m11, this.m22 * right.m12));
        double nm13 = Math.fma(this.m03, right.m10, Math.fma(this.m13, right.m11, this.m23 * right.m12));
        double nm20 = Math.fma(this.m00, right.m20, Math.fma(this.m10, right.m21, this.m20 * right.m22));
        double nm21 = Math.fma(this.m01, right.m20, Math.fma(this.m11, right.m21, this.m21 * right.m22));
        double nm22 = Math.fma(this.m02, right.m20, Math.fma(this.m12, right.m21, this.m22 * right.m22));
        double nm23 = Math.fma(this.m03, right.m20, Math.fma(this.m13, right.m21, this.m23 * right.m22));
        double nm30 = Math.fma(this.m00, right.m30, Math.fma(this.m10, right.m31, Math.fma(this.m20, right.m32, this.m30)));
        double nm31 = Math.fma(this.m01, right.m30, Math.fma(this.m11, right.m31, Math.fma(this.m21, right.m32, this.m31)));
        double nm32 = Math.fma(this.m02, right.m30, Math.fma(this.m12, right.m31, Math.fma(this.m22, right.m32, this.m32)));
        double nm33 = Math.fma(this.m03, right.m30, Math.fma(this.m13, right.m31, Math.fma(this.m23, right.m32, this.m33)));
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4d mulPerspectiveAffine(Matrix4x3d view, Matrix4d dest) {
        double lm00 = this.m00;
        double lm11 = this.m11;
        double lm22 = this.m22;
        double lm23 = this.m23;
        dest._m00(lm00 * view.m00)._m01(lm11 * view.m01)._m02(lm22 * view.m02)._m03(lm23 * view.m02)._m10(lm00 * view.m10)._m11(lm11 * view.m11)._m12(lm22 * view.m12)._m13(lm23 * view.m12)._m20(lm00 * view.m20)._m21(lm11 * view.m21)._m22(lm22 * view.m22)._m23(lm23 * view.m22)._m30(lm00 * view.m30)._m31(lm11 * view.m31)._m32(lm22 * view.m32 + this.m32)._m33(lm23 * view.m32)._properties(0);
        return dest;
    }

    public Matrix4d mul(Matrix4x3f right, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.set(right);
        } else {
            return (right.properties() & 4) != 0 ? dest.set(this) : this.mulGeneric(right, dest);
        }
    }

    private Matrix4d mulGeneric(Matrix4x3f right, Matrix4d dest) {
        double nm00 = Math.fma(this.m00, right.m00, Math.fma(this.m10, right.m01, this.m20 * (double)right.m02));
        double nm01 = Math.fma(this.m01, right.m00, Math.fma(this.m11, right.m01, this.m21 * (double)right.m02));
        double nm02 = Math.fma(this.m02, right.m00, Math.fma(this.m12, right.m01, this.m22 * (double)right.m02));
        double nm03 = Math.fma(this.m03, right.m00, Math.fma(this.m13, right.m01, this.m23 * (double)right.m02));
        double nm10 = Math.fma(this.m00, right.m10, Math.fma(this.m10, right.m11, this.m20 * (double)right.m12));
        double nm11 = Math.fma(this.m01, right.m10, Math.fma(this.m11, right.m11, this.m21 * (double)right.m12));
        double nm12 = Math.fma(this.m02, right.m10, Math.fma(this.m12, right.m11, this.m22 * (double)right.m12));
        double nm13 = Math.fma(this.m03, right.m10, Math.fma(this.m13, right.m11, this.m23 * (double)right.m12));
        double nm20 = Math.fma(this.m00, right.m20, Math.fma(this.m10, right.m21, this.m20 * (double)right.m22));
        double nm21 = Math.fma(this.m01, right.m20, Math.fma(this.m11, right.m21, this.m21 * (double)right.m22));
        double nm22 = Math.fma(this.m02, right.m20, Math.fma(this.m12, right.m21, this.m22 * (double)right.m22));
        double nm23 = Math.fma(this.m03, right.m20, Math.fma(this.m13, right.m21, this.m23 * (double)right.m22));
        double nm30 = Math.fma(this.m00, right.m30, Math.fma(this.m10, right.m31, Math.fma(this.m20, right.m32, this.m30)));
        double nm31 = Math.fma(this.m01, right.m30, Math.fma(this.m11, right.m31, Math.fma(this.m21, right.m32, this.m31)));
        double nm32 = Math.fma(this.m02, right.m30, Math.fma(this.m12, right.m31, Math.fma(this.m22, right.m32, this.m32)));
        double nm33 = Math.fma(this.m03, right.m30, Math.fma(this.m13, right.m31, Math.fma(this.m23, right.m32, this.m33)));
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4d mul(Matrix3x2d right) {
        return this.mul(right, this);
    }

    public Matrix4d mul(Matrix3x2d right, Matrix4d dest) {
        double nm00 = this.m00 * right.m00 + this.m10 * right.m01;
        double nm01 = this.m01 * right.m00 + this.m11 * right.m01;
        double nm02 = this.m02 * right.m00 + this.m12 * right.m01;
        double nm03 = this.m03 * right.m00 + this.m13 * right.m01;
        double nm10 = this.m00 * right.m10 + this.m10 * right.m11;
        double nm11 = this.m01 * right.m10 + this.m11 * right.m11;
        double nm12 = this.m02 * right.m10 + this.m12 * right.m11;
        double nm13 = this.m03 * right.m10 + this.m13 * right.m11;
        double nm30 = this.m00 * right.m20 + this.m10 * right.m21 + this.m30;
        double nm31 = this.m01 * right.m20 + this.m11 * right.m21 + this.m31;
        double nm32 = this.m02 * right.m20 + this.m12 * right.m21 + this.m32;
        double nm33 = this.m03 * right.m20 + this.m13 * right.m21 + this.m33;
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4d mul(Matrix3x2f right) {
        return this.mul(right, this);
    }

    public Matrix4d mul(Matrix3x2f right, Matrix4d dest) {
        double nm00 = this.m00 * right.m00 + this.m10 * right.m01;
        double nm01 = this.m01 * right.m00 + this.m11 * right.m01;
        double nm02 = this.m02 * right.m00 + this.m12 * right.m01;
        double nm03 = this.m03 * right.m00 + this.m13 * right.m01;
        double nm10 = this.m00 * right.m10 + this.m10 * right.m11;
        double nm11 = this.m01 * right.m10 + this.m11 * right.m11;
        double nm12 = this.m02 * right.m10 + this.m12 * right.m11;
        double nm13 = this.m03 * right.m10 + this.m13 * right.m11;
        double nm30 = this.m00 * right.m20 + this.m10 * right.m21 + this.m30;
        double nm31 = this.m01 * right.m20 + this.m11 * right.m21 + this.m31;
        double nm32 = this.m02 * right.m20 + this.m12 * right.m21 + this.m32;
        double nm33 = this.m03 * right.m20 + this.m13 * right.m21 + this.m33;
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4d mul(Matrix4f right) {
        return this.mul(right, this);
    }

    public Matrix4d mul(Matrix4f right, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.set(right);
        } else {
            return (right.properties() & 4) != 0 ? dest.set(this) : this.mulGeneric(right, dest);
        }
    }

    private Matrix4d mulGeneric(Matrix4f right, Matrix4d dest) {
        double nm00 = this.m00 * right.m00 + this.m10 * right.m01 + this.m20 * right.m02 + this.m30 * right.m03;
        double nm01 = this.m01 * right.m00 + this.m11 * right.m01 + this.m21 * right.m02 + this.m31 * right.m03;
        double nm02 = this.m02 * right.m00 + this.m12 * right.m01 + this.m22 * right.m02 + this.m32 * right.m03;
        double nm03 = this.m03 * right.m00 + this.m13 * right.m01 + this.m23 * right.m02 + this.m33 * right.m03;
        double nm10 = this.m00 * right.m10 + this.m10 * right.m11 + this.m20 * right.m12 + this.m30 * right.m13;
        double nm11 = this.m01 * right.m10 + this.m11 * right.m11 + this.m21 * right.m12 + this.m31 * right.m13;
        double nm12 = this.m02 * right.m10 + this.m12 * right.m11 + this.m22 * right.m12 + this.m32 * right.m13;
        double nm13 = this.m03 * right.m10 + this.m13 * right.m11 + this.m23 * right.m12 + this.m33 * right.m13;
        double nm20 = this.m00 * right.m20 + this.m10 * right.m21 + this.m20 * right.m22 + this.m30 * right.m23;
        double nm21 = this.m01 * right.m20 + this.m11 * right.m21 + this.m21 * right.m22 + this.m31 * right.m23;
        double nm22 = this.m02 * right.m20 + this.m12 * right.m21 + this.m22 * right.m22 + this.m32 * right.m23;
        double nm23 = this.m03 * right.m20 + this.m13 * right.m21 + this.m23 * right.m22 + this.m33 * right.m23;
        double nm30 = this.m00 * right.m30 + this.m10 * right.m31 + this.m20 * right.m32 + this.m30 * right.m33;
        double nm31 = this.m01 * right.m30 + this.m11 * right.m31 + this.m21 * right.m32 + this.m31 * right.m33;
        double nm32 = this.m02 * right.m30 + this.m12 * right.m31 + this.m22 * right.m32 + this.m32 * right.m33;
        double nm33 = this.m03 * right.m30 + this.m13 * right.m31 + this.m23 * right.m32 + this.m33 * right.m33;
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
        return dest;
    }

    public Matrix4d mulPerspectiveAffine(Matrix4d view) {
        return this.mulPerspectiveAffine(view, this);
    }

    public Matrix4d mulPerspectiveAffine(Matrix4d view, Matrix4d dest) {
        double nm00 = this.m00 * view.m00;
        double nm01 = this.m11 * view.m01;
        double nm02 = this.m22 * view.m02;
        double nm03 = this.m23 * view.m02;
        double nm10 = this.m00 * view.m10;
        double nm11 = this.m11 * view.m11;
        double nm12 = this.m22 * view.m12;
        double nm13 = this.m23 * view.m12;
        double nm20 = this.m00 * view.m20;
        double nm21 = this.m11 * view.m21;
        double nm22 = this.m22 * view.m22;
        double nm23 = this.m23 * view.m22;
        double nm30 = this.m00 * view.m30;
        double nm31 = this.m11 * view.m31;
        double nm32 = this.m22 * view.m32 + this.m32;
        double nm33 = this.m23 * view.m32;
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
    }

    public Matrix4d mulAffineR(Matrix4d right) {
        return this.mulAffineR(right, this);
    }

    public Matrix4d mulAffineR(Matrix4d right, Matrix4d dest) {
        double nm00 = Math.fma(this.m00, right.m00, Math.fma(this.m10, right.m01, this.m20 * right.m02));
        double nm01 = Math.fma(this.m01, right.m00, Math.fma(this.m11, right.m01, this.m21 * right.m02));
        double nm02 = Math.fma(this.m02, right.m00, Math.fma(this.m12, right.m01, this.m22 * right.m02));
        double nm03 = Math.fma(this.m03, right.m00, Math.fma(this.m13, right.m01, this.m23 * right.m02));
        double nm10 = Math.fma(this.m00, right.m10, Math.fma(this.m10, right.m11, this.m20 * right.m12));
        double nm11 = Math.fma(this.m01, right.m10, Math.fma(this.m11, right.m11, this.m21 * right.m12));
        double nm12 = Math.fma(this.m02, right.m10, Math.fma(this.m12, right.m11, this.m22 * right.m12));
        double nm13 = Math.fma(this.m03, right.m10, Math.fma(this.m13, right.m11, this.m23 * right.m12));
        double nm20 = Math.fma(this.m00, right.m20, Math.fma(this.m10, right.m21, this.m20 * right.m22));
        double nm21 = Math.fma(this.m01, right.m20, Math.fma(this.m11, right.m21, this.m21 * right.m22));
        double nm22 = Math.fma(this.m02, right.m20, Math.fma(this.m12, right.m21, this.m22 * right.m22));
        double nm23 = Math.fma(this.m03, right.m20, Math.fma(this.m13, right.m21, this.m23 * right.m22));
        double nm30 = Math.fma(this.m00, right.m30, Math.fma(this.m10, right.m31, Math.fma(this.m20, right.m32, this.m30)));
        double nm31 = Math.fma(this.m01, right.m30, Math.fma(this.m11, right.m31, Math.fma(this.m21, right.m32, this.m31)));
        double nm32 = Math.fma(this.m02, right.m30, Math.fma(this.m12, right.m31, Math.fma(this.m22, right.m32, this.m32)));
        double nm33 = Math.fma(this.m03, right.m30, Math.fma(this.m13, right.m31, Math.fma(this.m23, right.m32, this.m33)));
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4d mulAffine(Matrix4d right) {
        return this.mulAffine(right, this);
    }

    public Matrix4d mulAffine(Matrix4d right, Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        double m20 = this.m20;
        double m21 = this.m21;
        double m22 = this.m22;
        double rm00 = right.m00;
        double rm01 = right.m01;
        double rm02 = right.m02;
        double rm10 = right.m10;
        double rm11 = right.m11;
        double rm12 = right.m12;
        double rm20 = right.m20;
        double rm21 = right.m21;
        double rm22 = right.m22;
        double rm30 = right.m30;
        double rm31 = right.m31;
        double rm32 = right.m32;
        return dest._m00(Math.fma(m00, rm00, Math.fma(m10, rm01, m20 * rm02)))._m01(Math.fma(m01, rm00, Math.fma(m11, rm01, m21 * rm02)))._m02(Math.fma(m02, rm00, Math.fma(m12, rm01, m22 * rm02)))._m03(this.m03)._m10(Math.fma(m00, rm10, Math.fma(m10, rm11, m20 * rm12)))._m11(Math.fma(m01, rm10, Math.fma(m11, rm11, m21 * rm12)))._m12(Math.fma(m02, rm10, Math.fma(m12, rm11, m22 * rm12)))._m13(this.m13)._m20(Math.fma(m00, rm20, Math.fma(m10, rm21, m20 * rm22)))._m21(Math.fma(m01, rm20, Math.fma(m11, rm21, m21 * rm22)))._m22(Math.fma(m02, rm20, Math.fma(m12, rm21, m22 * rm22)))._m23(this.m23)._m30(Math.fma(m00, rm30, Math.fma(m10, rm31, Math.fma(m20, rm32, this.m30))))._m31(Math.fma(m01, rm30, Math.fma(m11, rm31, Math.fma(m21, rm32, this.m31))))._m32(Math.fma(m02, rm30, Math.fma(m12, rm31, Math.fma(m22, rm32, this.m32))))._m33(this.m33)._properties(2 | this.properties & right.properties() & 16);
    }

    public Matrix4d mulTranslationAffine(Matrix4d right, Matrix4d dest) {
        return dest._m00(right.m00)._m01(right.m01)._m02(right.m02)._m03(this.m03)._m10(right.m10)._m11(right.m11)._m12(right.m12)._m13(this.m13)._m20(right.m20)._m21(right.m21)._m22(right.m22)._m23(this.m23)._m30(right.m30 + this.m30)._m31(right.m31 + this.m31)._m32(right.m32 + this.m32)._m33(this.m33)._properties(2 | right.properties() & 16);
    }

    public Matrix4d mulOrthoAffine(Matrix4d view) {
        return this.mulOrthoAffine(view, this);
    }

    public Matrix4d mulOrthoAffine(Matrix4d view, Matrix4d dest) {
        double nm00 = this.m00 * view.m00;
        double nm01 = this.m11 * view.m01;
        double nm02 = this.m22 * view.m02;
        double nm03 = 0.0;
        double nm10 = this.m00 * view.m10;
        double nm11 = this.m11 * view.m11;
        double nm12 = this.m22 * view.m12;
        double nm13 = 0.0;
        double nm20 = this.m00 * view.m20;
        double nm21 = this.m11 * view.m21;
        double nm22 = this.m22 * view.m22;
        double nm23 = 0.0;
        double nm30 = this.m00 * view.m30 + this.m30;
        double nm31 = this.m11 * view.m31 + this.m31;
        double nm32 = this.m22 * view.m32 + this.m32;
        double nm33 = 1.0;
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(2);
        return dest;
    }

    public Matrix4d fma4x3(Matrix4d other, double otherFactor) {
        return this.fma4x3(other, otherFactor, this);
    }

    public Matrix4d fma4x3(Matrix4d other, double otherFactor, Matrix4d dest) {
        dest._m00(Math.fma(other.m00, otherFactor, this.m00))._m01(Math.fma(other.m01, otherFactor, this.m01))._m02(Math.fma(other.m02, otherFactor, this.m02))._m03(this.m03)._m10(Math.fma(other.m10, otherFactor, this.m10))._m11(Math.fma(other.m11, otherFactor, this.m11))._m12(Math.fma(other.m12, otherFactor, this.m12))._m13(this.m13)._m20(Math.fma(other.m20, otherFactor, this.m20))._m21(Math.fma(other.m21, otherFactor, this.m21))._m22(Math.fma(other.m22, otherFactor, this.m22))._m23(this.m23)._m30(Math.fma(other.m30, otherFactor, this.m30))._m31(Math.fma(other.m31, otherFactor, this.m31))._m32(Math.fma(other.m32, otherFactor, this.m32))._m33(this.m33)._properties(0);
        return dest;
    }

    public Matrix4d add(Matrix4d other) {
        return this.add(other, this);
    }

    public Matrix4d add(Matrix4d other, Matrix4d dest) {
        dest._m00(this.m00 + other.m00)._m01(this.m01 + other.m01)._m02(this.m02 + other.m02)._m03(this.m03 + other.m03)._m10(this.m10 + other.m10)._m11(this.m11 + other.m11)._m12(this.m12 + other.m12)._m13(this.m13 + other.m13)._m20(this.m20 + other.m20)._m21(this.m21 + other.m21)._m22(this.m22 + other.m22)._m23(this.m23 + other.m23)._m30(this.m30 + other.m30)._m31(this.m31 + other.m31)._m32(this.m32 + other.m32)._m33(this.m33 + other.m33)._properties(0);
        return dest;
    }

    public Matrix4d sub(Matrix4d subtrahend) {
        return this.sub(subtrahend, this);
    }

    public Matrix4d sub(Matrix4d subtrahend, Matrix4d dest) {
        dest._m00(this.m00 - subtrahend.m00)._m01(this.m01 - subtrahend.m01)._m02(this.m02 - subtrahend.m02)._m03(this.m03 - subtrahend.m03)._m10(this.m10 - subtrahend.m10)._m11(this.m11 - subtrahend.m11)._m12(this.m12 - subtrahend.m12)._m13(this.m13 - subtrahend.m13)._m20(this.m20 - subtrahend.m20)._m21(this.m21 - subtrahend.m21)._m22(this.m22 - subtrahend.m22)._m23(this.m23 - subtrahend.m23)._m30(this.m30 - subtrahend.m30)._m31(this.m31 - subtrahend.m31)._m32(this.m32 - subtrahend.m32)._m33(this.m33 - subtrahend.m33)._properties(0);
        return dest;
    }

    public Matrix4d mulComponentWise(Matrix4d other) {
        return this.mulComponentWise(other, this);
    }

    public Matrix4d mulComponentWise(Matrix4d other, Matrix4d dest) {
        dest._m00(this.m00 * other.m00)._m01(this.m01 * other.m01)._m02(this.m02 * other.m02)._m03(this.m03 * other.m03)._m10(this.m10 * other.m10)._m11(this.m11 * other.m11)._m12(this.m12 * other.m12)._m13(this.m13 * other.m13)._m20(this.m20 * other.m20)._m21(this.m21 * other.m21)._m22(this.m22 * other.m22)._m23(this.m23 * other.m23)._m30(this.m30 * other.m30)._m31(this.m31 * other.m31)._m32(this.m32 * other.m32)._m33(this.m33 * other.m33)._properties(0);
        return dest;
    }

    public Matrix4d add4x3(Matrix4d other) {
        return this.add4x3(other, this);
    }

    public Matrix4d add4x3(Matrix4d other, Matrix4d dest) {
        dest._m00(this.m00 + other.m00)._m01(this.m01 + other.m01)._m02(this.m02 + other.m02)._m03(this.m03)._m10(this.m10 + other.m10)._m11(this.m11 + other.m11)._m12(this.m12 + other.m12)._m13(this.m13)._m20(this.m20 + other.m20)._m21(this.m21 + other.m21)._m22(this.m22 + other.m22)._m23(this.m23)._m30(this.m30 + other.m30)._m31(this.m31 + other.m31)._m32(this.m32 + other.m32)._m33(this.m33)._properties(0);
        return dest;
    }

    public Matrix4d add4x3(Matrix4f other) {
        return this.add4x3(other, this);
    }

    public Matrix4d add4x3(Matrix4f other, Matrix4d dest) {
        dest._m00(this.m00 + other.m00)._m01(this.m01 + other.m01)._m02(this.m02 + other.m02)._m03(this.m03)._m10(this.m10 + other.m10)._m11(this.m11 + other.m11)._m12(this.m12 + other.m12)._m13(this.m13)._m20(this.m20 + other.m20)._m21(this.m21 + other.m21)._m22(this.m22 + other.m22)._m23(this.m23)._m30(this.m30 + other.m30)._m31(this.m31 + other.m31)._m32(this.m32 + other.m32)._m33(this.m33)._properties(0);
        return dest;
    }

    public Matrix4d sub4x3(Matrix4d subtrahend) {
        return this.sub4x3(subtrahend, this);
    }

    public Matrix4d sub4x3(Matrix4d subtrahend, Matrix4d dest) {
        dest._m00(this.m00 - subtrahend.m00)._m01(this.m01 - subtrahend.m01)._m02(this.m02 - subtrahend.m02)._m03(this.m03)._m10(this.m10 - subtrahend.m10)._m11(this.m11 - subtrahend.m11)._m12(this.m12 - subtrahend.m12)._m13(this.m13)._m20(this.m20 - subtrahend.m20)._m21(this.m21 - subtrahend.m21)._m22(this.m22 - subtrahend.m22)._m23(this.m23)._m30(this.m30 - subtrahend.m30)._m31(this.m31 - subtrahend.m31)._m32(this.m32 - subtrahend.m32)._m33(this.m33)._properties(0);
        return dest;
    }

    public Matrix4d mul4x3ComponentWise(Matrix4d other) {
        return this.mul4x3ComponentWise(other, this);
    }

    public Matrix4d mul4x3ComponentWise(Matrix4d other, Matrix4d dest) {
        dest._m00(this.m00 * other.m00)._m01(this.m01 * other.m01)._m02(this.m02 * other.m02)._m03(this.m03)._m10(this.m10 * other.m10)._m11(this.m11 * other.m11)._m12(this.m12 * other.m12)._m13(this.m13)._m20(this.m20 * other.m20)._m21(this.m21 * other.m21)._m22(this.m22 * other.m22)._m23(this.m23)._m30(this.m30 * other.m30)._m31(this.m31 * other.m31)._m32(this.m32 * other.m32)._m33(this.m33)._properties(0);
        return dest;
    }

    public Matrix4d set(double m00, double m01, double m02, double m03, double m10, double m11, double m12, double m13, double m20, double m21, double m22, double m23, double m30, double m31, double m32, double m33) {
        this.m00 = m00;
        this.m10 = m10;
        this.m20 = m20;
        this.m30 = m30;
        this.m01 = m01;
        this.m11 = m11;
        this.m21 = m21;
        this.m31 = m31;
        this.m02 = m02;
        this.m12 = m12;
        this.m22 = m22;
        this.m32 = m32;
        this.m03 = m03;
        this.m13 = m13;
        this.m23 = m23;
        this.m33 = m33;
        return this.determineProperties();
    }

    public Matrix4d set(double[] m, int off) {
        return this._m00(m[off])._m01(m[off + 1])._m02(m[off + 2])._m03(m[off + 3])._m10(m[off + 4])._m11(m[off + 5])._m12(m[off + 6])._m13(m[off + 7])._m20(m[off + 8])._m21(m[off + 9])._m22(m[off + 10])._m23(m[off + 11])._m30(m[off + 12])._m31(m[off + 13])._m32(m[off + 14])._m33(m[off + 15]).determineProperties();
    }

    public Matrix4d set(double[] m) {
        return this.set(m, 0);
    }

    public Matrix4d set(float[] m, int off) {
        return this._m00(m[off])._m01(m[off + 1])._m02(m[off + 2])._m03(m[off + 3])._m10(m[off + 4])._m11(m[off + 5])._m12(m[off + 6])._m13(m[off + 7])._m20(m[off + 8])._m21(m[off + 9])._m22(m[off + 10])._m23(m[off + 11])._m30(m[off + 12])._m31(m[off + 13])._m32(m[off + 14])._m33(m[off + 15]).determineProperties();
    }

    public Matrix4d set(float[] m) {
        return this.set(m, 0);
    }

    public Matrix4d set(Vector4d col0, Vector4d col1, Vector4d col2, Vector4d col3) {
        return this._m00(col0.x)._m01(col0.y)._m02(col0.z)._m03(col0.w)._m10(col1.x)._m11(col1.y)._m12(col1.z)._m13(col1.w)._m20(col2.x)._m21(col2.y)._m22(col2.z)._m23(col2.w)._m30(col3.x)._m31(col3.y)._m32(col3.z)._m33(col3.w).determineProperties();
    }

    public double determinant() {
        return (this.properties & 2) != 0 ? this.determinantAffine() : (this.m00 * this.m11 - this.m01 * this.m10) * (this.m22 * this.m33 - this.m23 * this.m32) + (this.m02 * this.m10 - this.m00 * this.m12) * (this.m21 * this.m33 - this.m23 * this.m31) + (this.m00 * this.m13 - this.m03 * this.m10) * (this.m21 * this.m32 - this.m22 * this.m31) + (this.m01 * this.m12 - this.m02 * this.m11) * (this.m20 * this.m33 - this.m23 * this.m30) + (this.m03 * this.m11 - this.m01 * this.m13) * (this.m20 * this.m32 - this.m22 * this.m30) + (this.m02 * this.m13 - this.m03 * this.m12) * (this.m20 * this.m31 - this.m21 * this.m30);
    }

    public double determinant3x3() {
        return (this.m00 * this.m11 - this.m01 * this.m10) * this.m22 + (this.m02 * this.m10 - this.m00 * this.m12) * this.m21 + (this.m01 * this.m12 - this.m02 * this.m11) * this.m20;
    }

    public double determinantAffine() {
        return (this.m00 * this.m11 - this.m01 * this.m10) * this.m22 + (this.m02 * this.m10 - this.m00 * this.m12) * this.m21 + (this.m01 * this.m12 - this.m02 * this.m11) * this.m20;
    }

    public Matrix4d invert() {
        return this.invert(this);
    }

    public Matrix4d invert(Matrix4d dest) {
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

    private Matrix4d invertTranslation(Matrix4d dest) {
        if (dest != this) {
            dest.set(this);
        }

        dest._m30(-this.m30)._m31(-this.m31)._m32(-this.m32)._properties(26);
        return dest;
    }

    private Matrix4d invertOrthonormal(Matrix4d dest) {
        double nm30 = -(this.m00 * this.m30 + this.m01 * this.m31 + this.m02 * this.m32);
        double nm31 = -(this.m10 * this.m30 + this.m11 * this.m31 + this.m12 * this.m32);
        double nm32 = -(this.m20 * this.m30 + this.m21 * this.m31 + this.m22 * this.m32);
        double m01 = this.m01;
        double m02 = this.m02;
        double m12 = this.m12;
        dest._m00(this.m00)._m01(this.m10)._m02(this.m20)._m03(0.0)._m10(m01)._m11(this.m11)._m12(this.m21)._m13(0.0)._m20(m02)._m21(m12)._m22(this.m22)._m23(0.0)._m30(nm30)._m31(nm31)._m32(nm32)._m33(1.0)._properties(18);
        return dest;
    }

    private Matrix4d invertGeneric(Matrix4d dest) {
        return this != dest ? this.invertGenericNonThis(dest) : this.invertGenericThis(dest);
    }

    private Matrix4d invertGenericNonThis(Matrix4d dest) {
        double a = this.m00 * this.m11 - this.m01 * this.m10;
        double b = this.m00 * this.m12 - this.m02 * this.m10;
        double c = this.m00 * this.m13 - this.m03 * this.m10;
        double d = this.m01 * this.m12 - this.m02 * this.m11;
        double e = this.m01 * this.m13 - this.m03 * this.m11;
        double f = this.m02 * this.m13 - this.m03 * this.m12;
        double g = this.m20 * this.m31 - this.m21 * this.m30;
        double h = this.m20 * this.m32 - this.m22 * this.m30;
        double i = this.m20 * this.m33 - this.m23 * this.m30;
        double j = this.m21 * this.m32 - this.m22 * this.m31;
        double k = this.m21 * this.m33 - this.m23 * this.m31;
        double l = this.m22 * this.m33 - this.m23 * this.m32;
        double det = a * l - b * k + c * j + d * i - e * h + f * g;
        det = 1.0 / det;
        return dest._m00(Math.fma(this.m11, l, Math.fma(-this.m12, k, this.m13 * j)) * det)._m01(Math.fma(-this.m01, l, Math.fma(this.m02, k, -this.m03 * j)) * det)._m02(Math.fma(this.m31, f, Math.fma(-this.m32, e, this.m33 * d)) * det)._m03(Math.fma(-this.m21, f, Math.fma(this.m22, e, -this.m23 * d)) * det)._m10(Math.fma(-this.m10, l, Math.fma(this.m12, i, -this.m13 * h)) * det)._m11(Math.fma(this.m00, l, Math.fma(-this.m02, i, this.m03 * h)) * det)._m12(Math.fma(-this.m30, f, Math.fma(this.m32, c, -this.m33 * b)) * det)._m13(Math.fma(this.m20, f, Math.fma(-this.m22, c, this.m23 * b)) * det)._m20(Math.fma(this.m10, k, Math.fma(-this.m11, i, this.m13 * g)) * det)._m21(Math.fma(-this.m00, k, Math.fma(this.m01, i, -this.m03 * g)) * det)._m22(Math.fma(this.m30, e, Math.fma(-this.m31, c, this.m33 * a)) * det)._m23(Math.fma(-this.m20, e, Math.fma(this.m21, c, -this.m23 * a)) * det)._m30(Math.fma(-this.m10, j, Math.fma(this.m11, h, -this.m12 * g)) * det)._m31(Math.fma(this.m00, j, Math.fma(-this.m01, h, this.m02 * g)) * det)._m32(Math.fma(-this.m30, d, Math.fma(this.m31, b, -this.m32 * a)) * det)._m33(Math.fma(this.m20, d, Math.fma(-this.m21, b, this.m22 * a)) * det)._properties(0);
    }

    private Matrix4d invertGenericThis(Matrix4d dest) {
        double a = this.m00 * this.m11 - this.m01 * this.m10;
        double b = this.m00 * this.m12 - this.m02 * this.m10;
        double c = this.m00 * this.m13 - this.m03 * this.m10;
        double d = this.m01 * this.m12 - this.m02 * this.m11;
        double e = this.m01 * this.m13 - this.m03 * this.m11;
        double f = this.m02 * this.m13 - this.m03 * this.m12;
        double g = this.m20 * this.m31 - this.m21 * this.m30;
        double h = this.m20 * this.m32 - this.m22 * this.m30;
        double i = this.m20 * this.m33 - this.m23 * this.m30;
        double j = this.m21 * this.m32 - this.m22 * this.m31;
        double k = this.m21 * this.m33 - this.m23 * this.m31;
        double l = this.m22 * this.m33 - this.m23 * this.m32;
        double det = a * l - b * k + c * j + d * i - e * h + f * g;
        det = 1.0 / det;
        double nm00 = Math.fma(this.m11, l, Math.fma(-this.m12, k, this.m13 * j)) * det;
        double nm01 = Math.fma(-this.m01, l, Math.fma(this.m02, k, -this.m03 * j)) * det;
        double nm02 = Math.fma(this.m31, f, Math.fma(-this.m32, e, this.m33 * d)) * det;
        double nm03 = Math.fma(-this.m21, f, Math.fma(this.m22, e, -this.m23 * d)) * det;
        double nm10 = Math.fma(-this.m10, l, Math.fma(this.m12, i, -this.m13 * h)) * det;
        double nm11 = Math.fma(this.m00, l, Math.fma(-this.m02, i, this.m03 * h)) * det;
        double nm12 = Math.fma(-this.m30, f, Math.fma(this.m32, c, -this.m33 * b)) * det;
        double nm13 = Math.fma(this.m20, f, Math.fma(-this.m22, c, this.m23 * b)) * det;
        double nm20 = Math.fma(this.m10, k, Math.fma(-this.m11, i, this.m13 * g)) * det;
        double nm21 = Math.fma(-this.m00, k, Math.fma(this.m01, i, -this.m03 * g)) * det;
        double nm22 = Math.fma(this.m30, e, Math.fma(-this.m31, c, this.m33 * a)) * det;
        double nm23 = Math.fma(-this.m20, e, Math.fma(this.m21, c, -this.m23 * a)) * det;
        double nm30 = Math.fma(-this.m10, j, Math.fma(this.m11, h, -this.m12 * g)) * det;
        double nm31 = Math.fma(this.m00, j, Math.fma(-this.m01, h, this.m02 * g)) * det;
        double nm32 = Math.fma(-this.m30, d, Math.fma(this.m31, b, -this.m32 * a)) * det;
        double nm33 = Math.fma(this.m20, d, Math.fma(-this.m21, b, this.m22 * a)) * det;
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
    }

    public Matrix4d invertPerspective(Matrix4d dest) {
        double a = 1.0 / (this.m00 * this.m11);
        double l = -1.0 / (this.m23 * this.m32);
        dest.set(this.m11 * a, 0.0, 0.0, 0.0, 0.0, this.m00 * a, 0.0, 0.0, 0.0, 0.0, 0.0, -this.m23 * l, 0.0, 0.0, -this.m32 * l, this.m22 * l);
        return dest;
    }

    public Matrix4d invertPerspective() {
        return this.invertPerspective(this);
    }

    public Matrix4d invertFrustum(Matrix4d dest) {
        double invM00 = 1.0 / this.m00;
        double invM11 = 1.0 / this.m11;
        double invM23 = 1.0 / this.m23;
        double invM32 = 1.0 / this.m32;
        dest.set(invM00, 0.0, 0.0, 0.0, 0.0, invM11, 0.0, 0.0, 0.0, 0.0, 0.0, invM32, -this.m20 * invM00 * invM23, -this.m21 * invM11 * invM23, invM23, -this.m22 * invM23 * invM32);
        return dest;
    }

    public Matrix4d invertFrustum() {
        return this.invertFrustum(this);
    }

    public Matrix4d invertOrtho(Matrix4d dest) {
        double invM00 = 1.0 / this.m00;
        double invM11 = 1.0 / this.m11;
        double invM22 = 1.0 / this.m22;
        dest.set(invM00, 0.0, 0.0, 0.0, 0.0, invM11, 0.0, 0.0, 0.0, 0.0, invM22, 0.0, -this.m30 * invM00, -this.m31 * invM11, -this.m32 * invM22, 1.0)._properties(2 | this.properties & 16);
        return dest;
    }

    public Matrix4d invertOrtho() {
        return this.invertOrtho(this);
    }

    public Matrix4d invertPerspectiveView(Matrix4d view, Matrix4d dest) {
        double a = 1.0 / (this.m00 * this.m11);
        double l = -1.0 / (this.m23 * this.m32);
        double pm00 = this.m11 * a;
        double pm11 = this.m00 * a;
        double pm23 = -this.m23 * l;
        double pm32 = -this.m32 * l;
        double pm33 = this.m22 * l;
        double vm30 = -view.m00 * view.m30 - view.m01 * view.m31 - view.m02 * view.m32;
        double vm31 = -view.m10 * view.m30 - view.m11 * view.m31 - view.m12 * view.m32;
        double vm32 = -view.m20 * view.m30 - view.m21 * view.m31 - view.m22 * view.m32;
        double nm10 = view.m01 * pm11;
        double nm30 = view.m02 * pm32 + vm30 * pm33;
        double nm31 = view.m12 * pm32 + vm31 * pm33;
        double nm32 = view.m22 * pm32 + vm32 * pm33;
        return dest._m00(view.m00 * pm00)._m01(view.m10 * pm00)._m02(view.m20 * pm00)._m03(0.0)._m10(nm10)._m11(view.m11 * pm11)._m12(view.m21 * pm11)._m13(0.0)._m20(vm30 * pm23)._m21(vm31 * pm23)._m22(vm32 * pm23)._m23(pm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(pm33)._properties(0);
    }

    public Matrix4d invertPerspectiveView(Matrix4x3d view, Matrix4d dest) {
        double a = 1.0 / (this.m00 * this.m11);
        double l = -1.0 / (this.m23 * this.m32);
        double pm00 = this.m11 * a;
        double pm11 = this.m00 * a;
        double pm23 = -this.m23 * l;
        double pm32 = -this.m32 * l;
        double pm33 = this.m22 * l;
        double vm30 = -view.m00 * view.m30 - view.m01 * view.m31 - view.m02 * view.m32;
        double vm31 = -view.m10 * view.m30 - view.m11 * view.m31 - view.m12 * view.m32;
        double vm32 = -view.m20 * view.m30 - view.m21 * view.m31 - view.m22 * view.m32;
        return dest._m00(view.m00 * pm00)._m01(view.m10 * pm00)._m02(view.m20 * pm00)._m03(0.0)._m10(view.m01 * pm11)._m11(view.m11 * pm11)._m12(view.m21 * pm11)._m13(0.0)._m20(vm30 * pm23)._m21(vm31 * pm23)._m22(vm32 * pm23)._m23(pm23)._m30(view.m02 * pm32 + vm30 * pm33)._m31(view.m12 * pm32 + vm31 * pm33)._m32(view.m22 * pm32 + vm32 * pm33)._m33(pm33)._properties(0);
    }

    public Matrix4d invertAffine(Matrix4d dest) {
        double m11m00 = this.m00 * this.m11;
        double m10m01 = this.m01 * this.m10;
        double m10m02 = this.m02 * this.m10;
        double m12m00 = this.m00 * this.m12;
        double m12m01 = this.m01 * this.m12;
        double m11m02 = this.m02 * this.m11;
        double s = 1.0 / ((m11m00 - m10m01) * this.m22 + (m10m02 - m12m00) * this.m21 + (m12m01 - m11m02) * this.m20);
        double m10m22 = this.m10 * this.m22;
        double m10m21 = this.m10 * this.m21;
        double m11m22 = this.m11 * this.m22;
        double m11m20 = this.m11 * this.m20;
        double m12m21 = this.m12 * this.m21;
        double m12m20 = this.m12 * this.m20;
        double m20m02 = this.m20 * this.m02;
        double m20m01 = this.m20 * this.m01;
        double m21m02 = this.m21 * this.m02;
        double m21m00 = this.m21 * this.m00;
        double m22m01 = this.m22 * this.m01;
        double m22m00 = this.m22 * this.m00;
        double nm00 = (m11m22 - m12m21) * s;
        double nm01 = (m21m02 - m22m01) * s;
        double nm02 = (m12m01 - m11m02) * s;
        double nm10 = (m12m20 - m10m22) * s;
        double nm11 = (m22m00 - m20m02) * s;
        double nm12 = (m10m02 - m12m00) * s;
        double nm20 = (m10m21 - m11m20) * s;
        double nm21 = (m20m01 - m21m00) * s;
        double nm22 = (m11m00 - m10m01) * s;
        double nm30 = (m10m22 * this.m31 - m10m21 * this.m32 + m11m20 * this.m32 - m11m22 * this.m30 + m12m21 * this.m30 - m12m20 * this.m31) * s;
        double nm31 = (m20m02 * this.m31 - m20m01 * this.m32 + m21m00 * this.m32 - m21m02 * this.m30 + m22m01 * this.m30 - m22m00 * this.m31) * s;
        double nm32 = (m11m02 * this.m30 - m12m01 * this.m30 + m12m00 * this.m31 - m10m02 * this.m31 + m10m01 * this.m32 - m11m00 * this.m32) * s;
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(0.0)
                ._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)
                ._m20(nm20)._m21(nm21)._m22(nm22)._m23(0.0)
                ._m30(nm30)._m31(nm31)._m32(nm32)._m33(1.0)
                ._properties(2);
        return dest;
    }

    public Matrix4d invertAffine() {
        return this.invertAffine(this);
    }

    public Matrix4d transpose() {
        return this.transpose(this);
    }

    public Matrix4d transpose(Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.identity();
        } else {
            return this != dest ? this.transposeNonThisGeneric(dest) : this.transposeThisGeneric(dest);
        }
    }

    private Matrix4d transposeNonThisGeneric(Matrix4d dest) {
        return dest._m00(this.m00)._m01(this.m10)._m02(this.m20)._m03(this.m30)
                ._m10(this.m01)._m11(this.m11)._m12(this.m21)._m13(this.m31)
                ._m20(this.m02)._m21(this.m12)._m22(this.m22)._m23(this.m32)
                ._m30(this.m03)._m31(this.m13)._m32(this.m23)._m33(this.m33)
                ._properties(0);
    }

    private Matrix4d transposeThisGeneric(Matrix4d dest) {
        double nm10 = this.m01;
        double nm20 = this.m02;
        double nm21 = this.m12;
        double nm30 = this.m03;
        double nm31 = this.m13;
        double nm32 = this.m23;
        return dest._m01(this.m10)._m02(this.m20)._m03(this.m30)
                ._m10(nm10)._m12(this.m21)._m13(this.m31)
                ._m20(nm20)._m21(nm21)._m23(this.m32)
                ._m30(nm30)._m31(nm31)._m32(nm32)
                ._properties(0);
    }

    public Matrix4d transpose3x3() {
        return this.transpose3x3(this);
    }

    public Matrix4d transpose3x3(Matrix4d dest) {
        double nm10 = this.m01;
        double nm20 = this.m02;
        double nm21 = this.m12;
        return dest._m00(this.m00)._m01(this.m10)._m02(this.m20)
                ._m10(nm10)._m11(this.m11)._m12(this.m21)
                ._m20(nm20)._m21(nm21)._m22(this.m22)
                ._properties(this.properties & 30);
    }

    public Matrix3d transpose3x3(Matrix3d dest) {
        return dest._m00(this.m00)._m01(this.m10)._m02(this.m20)._m10(this.m01)._m11(this.m11)._m12(this.m21)._m20(this.m02)._m21(this.m12)._m22(this.m22);
    }

    public Matrix4d translation(double x, double y, double z) {
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        return this._m30(x)._m31(y)._m32(z)._m33(1.0)._properties(26);
    }

    public Matrix4d translation(Vector3f offset) {
        return this.translation(offset.x, offset.y, offset.z);
    }

    public Matrix4d translation(Vector3d offset) {
        return this.translation(offset.x, offset.y, offset.z);
    }

    public Matrix4d setTranslation(double x, double y, double z) {
        Matrix4d var10000 = this._m30(x)._m31(y)._m32(z);
        var10000.properties &= -6;
        return this;
    }

    public Matrix4d setTranslation(Vector3d xyz) {
        return this.setTranslation(xyz.x, xyz.y, xyz.z);
    }

    public Vector3d getTranslation(Vector3d dest) {
        dest.x = this.m30;
        dest.y = this.m31;
        dest.z = this.m32;
        return dest;
    }

    public Vector3d getScale(Vector3d dest) {
        dest.x = Math.sqrt(this.m00 * this.m00 + this.m01 * this.m01 + this.m02 * this.m02);
        dest.y = Math.sqrt(this.m10 * this.m10 + this.m11 * this.m11 + this.m12 * this.m12);
        dest.z = Math.sqrt(this.m20 * this.m20 + this.m21 * this.m21 + this.m22 * this.m22);
        return dest;
    }

    public String toString() {
        String str = this.toString(Options.NUMBER_FORMAT);
        StringBuilder res = new StringBuilder();
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
        return Runtime.format(this.m00, formatter) + " " + Runtime.format(this.m10, formatter) + " " + Runtime.format(this.m20, formatter) + " " + Runtime.format(this.m30, formatter) + "\n" + Runtime.format(this.m01, formatter) + " " + Runtime.format(this.m11, formatter) + " " + Runtime.format(this.m21, formatter) + " " + Runtime.format(this.m31, formatter) + "\n" + Runtime.format(this.m02, formatter) + " " + Runtime.format(this.m12, formatter) + " " + Runtime.format(this.m22, formatter) + " " + Runtime.format(this.m32, formatter) + "\n" + Runtime.format(this.m03, formatter) + " " + Runtime.format(this.m13, formatter) + " " + Runtime.format(this.m23, formatter) + " " + Runtime.format(this.m33, formatter) + "\n";
    }

    public Matrix4d get(Matrix4d dest) {
        return dest.set(this);
    }

    public Matrix4x3d get4x3(Matrix4x3d dest) {
        return dest.set(this);
    }

    public Matrix3d get3x3(Matrix3d dest) {
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

    public double[] get(double[] dest, int offset) {
        dest[offset] = this.m00;
        dest[offset + 1] = this.m01;
        dest[offset + 2] = this.m02;
        dest[offset + 3] = this.m03;
        dest[offset + 4] = this.m10;
        dest[offset + 5] = this.m11;
        dest[offset + 6] = this.m12;
        dest[offset + 7] = this.m13;
        dest[offset + 8] = this.m20;
        dest[offset + 9] = this.m21;
        dest[offset + 10] = this.m22;
        dest[offset + 11] = this.m23;
        dest[offset + 12] = this.m30;
        dest[offset + 13] = this.m31;
        dest[offset + 14] = this.m32;
        dest[offset + 15] = this.m33;
        return dest;
    }

    public double[] get(double[] dest) {
        return this.get(dest, 0);
    }

    public float[] get(float[] dest, int offset) {
        dest[offset] = (float)this.m00;
        dest[offset + 1] = (float)this.m01;
        dest[offset + 2] = (float)this.m02;
        dest[offset + 3] = (float)this.m03;
        dest[offset + 4] = (float)this.m10;
        dest[offset + 5] = (float)this.m11;
        dest[offset + 6] = (float)this.m12;
        dest[offset + 7] = (float)this.m13;
        dest[offset + 8] = (float)this.m20;
        dest[offset + 9] = (float)this.m21;
        dest[offset + 10] = (float)this.m22;
        dest[offset + 11] = (float)this.m23;
        dest[offset + 12] = (float)this.m30;
        dest[offset + 13] = (float)this.m31;
        dest[offset + 14] = (float)this.m32;
        dest[offset + 15] = (float)this.m33;
        return dest;
    }

    public float[] get(float[] dest) {
        return this.get(dest, 0);
    }

    public Matrix4d zero() {
        return this._m00(0.0)._m01(0.0)._m02(0.0)._m03(0.0)._m10(0.0)._m11(0.0)._m12(0.0)._m13(0.0)._m20(0.0)._m21(0.0)._m22(0.0)._m23(0.0)._m30(0.0)._m31(0.0)._m32(0.0)._m33(0.0)._properties(0);
    }

    public Matrix4d scaling(double factor) {
        return this.scaling(factor, factor, factor);
    }

    public Matrix4d scaling(double x, double y, double z) {
        if ((this.properties & 4) == 0) {
            this.identity();
        }

        boolean one = Math.absEqualsOne(x) && Math.absEqualsOne(y) && Math.absEqualsOne(z);
        this._m00(x)._m11(y)._m22(z).properties = 2 | (one ? 16 : 0);
        return this;
    }

    public Matrix4d scaling(Vector3d xyz) {
        return this.scaling(xyz.x, xyz.y, xyz.z);
    }

    public Matrix4d rotation(double angle, double x, double y, double z) {
        if (y == 0.0 && z == 0.0 && Math.absEqualsOne(x)) {
            return this.rotationX(x * angle);
        } else if (x == 0.0 && z == 0.0 && Math.absEqualsOne(y)) {
            return this.rotationY(y * angle);
        } else {
            return x == 0.0 && y == 0.0 && Math.absEqualsOne(z) ? this.rotationZ(z * angle) : this.rotationInternal(angle, x, y, z);
        }
    }

    private Matrix4d rotationInternal(double angle, double x, double y, double z) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double C = 1.0 - cos;
        double xy = x * y;
        double xz = x * z;
        double yz = y * z;
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this._m00(cos + x * x * C)._m10(xy * C - z * sin)._m20(xz * C + y * sin)._m01(xy * C + z * sin)._m11(cos + y * y * C)._m21(yz * C - x * sin)._m02(xz * C - y * sin)._m12(yz * C + x * sin)._m22(cos + z * z * C).properties = 18;
        return this;
    }

    public Matrix4d rotationX(double ang) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this._m11(cos)._m12(sin)._m21(-sin)._m22(cos).properties = 18;
        return this;
    }

    public Matrix4d rotationY(double ang) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this._m00(cos)._m02(-sin)._m20(sin)._m22(cos).properties = 18;
        return this;
    }

    public Matrix4d rotationZ(double ang) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this._m00(cos)._m01(sin)._m10(-sin)._m11(cos).properties = 18;
        return this;
    }

    public Matrix4d rotationTowardsXY(double dirX, double dirY) {
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this.m00 = dirY;
        this.m01 = dirX;
        this.m10 = -dirX;
        this.m11 = dirY;
        this.properties = 18;
        return this;
    }

    public Matrix4d rotationXYZ(double angleX, double angleY, double angleZ) {
        double sinX = Math.sin(angleX);
        double cosX = Math.cosFromSin(sinX, angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cosFromSin(sinY, angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cosFromSin(sinZ, angleZ);
        double m_sinX = -sinX;
        double m_sinY = -sinY;
        double m_sinZ = -sinZ;
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        double nm01 = m_sinX * m_sinY;
        double nm02 = cosX * m_sinY;
        this._m20(sinY)._m21(m_sinX * cosY)._m22(cosX * cosY)._m00(cosY * cosZ)._m01(nm01 * cosZ + cosX * sinZ)._m02(nm02 * cosZ + sinX * sinZ)._m10(cosY * m_sinZ)._m11(nm01 * m_sinZ + cosX * cosZ)._m12(nm02 * m_sinZ + sinX * cosZ).properties = 18;
        return this;
    }

    public Matrix4d rotationZYX(double angleZ, double angleY, double angleX) {
        double sinX = Math.sin(angleX);
        double cosX = Math.cosFromSin(sinX, angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cosFromSin(sinY, angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cosFromSin(sinZ, angleZ);
        double m_sinZ = -sinZ;
        double m_sinY = -sinY;
        double m_sinX = -sinX;
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        double nm20 = cosZ * sinY;
        double nm21 = sinZ * sinY;
        this._m00(cosZ * cosY)._m01(sinZ * cosY)._m02(m_sinY)._m10(m_sinZ * cosX + nm20 * sinX)._m11(cosZ * cosX + nm21 * sinX)._m12(cosY * sinX)._m20(m_sinZ * m_sinX + nm20 * cosX)._m21(cosZ * m_sinX + nm21 * cosX)._m22(cosY * cosX).properties = 18;
        return this;
    }

    public Matrix4d rotationYXZ(double angleY, double angleX, double angleZ) {
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
        this._m20(sinY * cosX)._m21(m_sinX)._m22(cosY * cosX)._m23(0.0)._m00(cosY * cosZ + nm10 * sinZ)._m01(cosX * sinZ)._m02(m_sinY * cosZ + nm12 * sinZ)._m03(0.0)._m10(cosY * m_sinZ + nm10 * cosZ)._m11(cosX * cosZ)._m12(m_sinY * m_sinZ + nm12 * cosZ)._m13(0.0)._m30(0.0)._m31(0.0)._m32(0.0)._m33(1.0).properties = 18;
        return this;
    }

    public Matrix4d setRotationXYZ(double angleX, double angleY, double angleZ) {
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
        Matrix4d var10000 = this._m20(sinY)._m21(m_sinX * cosY)._m22(cosX * cosY)._m00(cosY * cosZ)._m01(nm01 * cosZ + cosX * sinZ)._m02(nm02 * cosZ + sinX * sinZ)._m10(cosY * m_sinZ)._m11(nm01 * m_sinZ + cosX * cosZ)._m12(nm02 * m_sinZ + sinX * cosZ);
        var10000.properties &= -14;
        return this;
    }

    public Matrix4d setRotationZYX(double angleZ, double angleY, double angleX) {
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
        Matrix4d var10000 = this._m00(cosZ * cosY)._m01(sinZ * cosY)._m02(m_sinY)._m10(m_sinZ * cosX + nm20 * sinX)._m11(cosZ * cosX + nm21 * sinX)._m12(cosY * sinX)._m20(m_sinZ * m_sinX + nm20 * cosX)._m21(cosZ * m_sinX + nm21 * cosX)._m22(cosY * cosX);
        var10000.properties &= -14;
        return this;
    }

    public Matrix4d setRotationYXZ(double angleY, double angleX, double angleZ) {
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
        Matrix4d var10000 = this._m20(sinY * cosX)._m21(m_sinX)._m22(cosY * cosX)._m00(cosY * cosZ + nm10 * sinZ)._m01(cosX * sinZ)._m02(m_sinY * cosZ + nm12 * sinZ)._m10(cosY * m_sinZ + nm10 * cosZ)._m11(cosX * cosZ)._m12(m_sinY * m_sinZ + nm12 * cosZ);
        var10000.properties &= -14;
        return this;
    }

    public Matrix4d rotation(double angle, Vector3d axis) {
        return this.rotation(angle, axis.x, axis.y, axis.z);
    }

    public Matrix4d rotation(double angle, Vector3f axis) {
        return this.rotation(angle, axis.x, axis.y, axis.z);
    }

    public Vector4d transform(Vector4d v) {
        return v.mul(this);
    }

    public Vector4d transform(Vector4d v, Vector4d dest) {
        return v.mul(this, dest);
    }

    public Vector4d transform(double x, double y, double z, double w, Vector4d dest) {
        return dest.set(this.m00 * x + this.m10 * y + this.m20 * z + this.m30 * w, this.m01 * x + this.m11 * y + this.m21 * z + this.m31 * w, this.m02 * x + this.m12 * y + this.m22 * z + this.m32 * w, this.m03 * x + this.m13 * y + this.m23 * z + this.m33 * w);
    }

    public Vector4d transformTranspose(Vector4d v) {
        return v.mulTranspose(this);
    }

    public Vector4d transformTranspose(Vector4d v, Vector4d dest) {
        return v.mulTranspose(this, dest);
    }

    public Vector4d transformTranspose(double x, double y, double z, double w, Vector4d dest) {
        return dest.set(x, y, z, w).mulTranspose(this);
    }

    public Vector4d transformProject(Vector4d v) {
        return v.mulProject(this);
    }

    public Vector4d transformProject(Vector4d v, Vector4d dest) {
        return v.mulProject(this, dest);
    }

    public Vector4d transformProject(double x, double y, double z, double w, Vector4d dest) {
        double invW = 1.0 / (this.m03 * x + this.m13 * y + this.m23 * z + this.m33 * w);
        return dest.set((this.m00 * x + this.m10 * y + this.m20 * z + this.m30 * w) * invW, (this.m01 * x + this.m11 * y + this.m21 * z + this.m31 * w) * invW, (this.m02 * x + this.m12 * y + this.m22 * z + this.m32 * w) * invW, 1.0);
    }

    public Vector3d transformProject(Vector3d v) {
        return v.mulProject(this);
    }

    public Vector3d transformProject(Vector3d v, Vector3d dest) {
        return v.mulProject(this, dest);
    }

    public Vector3d transformProject(double x, double y, double z, Vector3d dest) {
        double invW = 1.0 / (this.m03 * x + this.m13 * y + this.m23 * z + this.m33);
        return dest.set((this.m00 * x + this.m10 * y + this.m20 * z + this.m30) * invW, (this.m01 * x + this.m11 * y + this.m21 * z + this.m31) * invW, (this.m02 * x + this.m12 * y + this.m22 * z + this.m32) * invW);
    }

    public Vector3d transformProject(Vector4d v, Vector3d dest) {
        return v.mulProject(this, dest);
    }

    public Vector3d transformProject(double x, double y, double z, double w, Vector3d dest) {
        dest.x = x;
        dest.y = y;
        dest.z = z;
        return dest.mulProject(this, w, dest);
    }

    public Vector3d transformPosition(Vector3d dest) {
        return dest.set(this.m00 * dest.x + this.m10 * dest.y + this.m20 * dest.z + this.m30, this.m01 * dest.x + this.m11 * dest.y + this.m21 * dest.z + this.m31, this.m02 * dest.x + this.m12 * dest.y + this.m22 * dest.z + this.m32);
    }

    public Vector3d transformPosition(Vector3d v, Vector3d dest) {
        return this.transformPosition(v.x, v.y, v.z, dest);
    }

    public Vector3d transformPosition(double x, double y, double z, Vector3d dest) {
        return dest.set(this.m00 * x + this.m10 * y + this.m20 * z + this.m30, this.m01 * x + this.m11 * y + this.m21 * z + this.m31, this.m02 * x + this.m12 * y + this.m22 * z + this.m32);
    }

    public Vector3d transformDirection(Vector3d dest) {
        return dest.set(this.m00 * dest.x + this.m10 * dest.y + this.m20 * dest.z, this.m01 * dest.x + this.m11 * dest.y + this.m21 * dest.z, this.m02 * dest.x + this.m12 * dest.y + this.m22 * dest.z);
    }

    public Vector3d transformDirection(Vector3d v, Vector3d dest) {
        return dest.set(this.m00 * v.x + this.m10 * v.y + this.m20 * v.z, this.m01 * v.x + this.m11 * v.y + this.m21 * v.z, this.m02 * v.x + this.m12 * v.y + this.m22 * v.z);
    }

    public Vector3d transformDirection(double x, double y, double z, Vector3d dest) {
        return dest.set(this.m00 * x + this.m10 * y + this.m20 * z, this.m01 * x + this.m11 * y + this.m21 * z, this.m02 * x + this.m12 * y + this.m22 * z);
    }

    public Vector3f transformDirection(Vector3f dest) {
        return dest.mulDirection(this);
    }

    public Vector3f transformDirection(Vector3f v, Vector3f dest) {
        return v.mulDirection(this, dest);
    }

    public Vector3f transformDirection(double x, double y, double z, Vector3f dest) {
        float rx = (float)(this.m00 * x + this.m10 * y + this.m20 * z);
        float ry = (float)(this.m01 * x + this.m11 * y + this.m21 * z);
        float rz = (float)(this.m02 * x + this.m12 * y + this.m22 * z);
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector4d transformAffine(Vector4d dest) {
        return dest.mulAffine(this, dest);
    }

    public Vector4d transformAffine(Vector4d v, Vector4d dest) {
        return this.transformAffine(v.x, v.y, v.z, v.w, dest);
    }

    public Vector4d transformAffine(double x, double y, double z, double w, Vector4d dest) {
        double rx = this.m00 * x + this.m10 * y + this.m20 * z + this.m30 * w;
        double ry = this.m01 * x + this.m11 * y + this.m21 * z + this.m31 * w;
        double rz = this.m02 * x + this.m12 * y + this.m22 * z + this.m32 * w;
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        dest.w = w;
        return dest;
    }

    public Matrix4d set3x3(Matrix3d mat) {
        return this._m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)._m20(mat.m20)._m21(mat.m21)._m22(mat.m22)._properties(this.properties & -30);
    }

    public Matrix4d scale(Vector3d xyz, Matrix4d dest) {
        return this.scale(xyz.x, xyz.y, xyz.z, dest);
    }

    public Matrix4d scale(Vector3d xyz) {
        return this.scale(xyz.x, xyz.y, xyz.z, this);
    }

    public Matrix4d scale(double x, double y, double z, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.scaling(x, y, z) : this.scaleGeneric(x, y, z, dest);
    }

    private Matrix4d scaleGeneric(double x, double y, double z, Matrix4d dest) {
        boolean one = Math.absEqualsOne(x) && Math.absEqualsOne(y) && Math.absEqualsOne(z);
        dest._m00(this.m00 * x)._m01(this.m01 * x)._m02(this.m02 * x)._m03(this.m03 * x)._m10(this.m10 * y)._m11(this.m11 * y)._m12(this.m12 * y)._m13(this.m13 * y)._m20(this.m20 * z)._m21(this.m21 * z)._m22(this.m22 * z)._m23(this.m23 * z)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & ~(13 | (one ? 0 : 16)));
        return dest;
    }

    public Matrix4d scale(double x, double y, double z) {
        return this.scale(x, y, z, this);
    }

    public Matrix4d scale(double xyz, Matrix4d dest) {
        return this.scale(xyz, xyz, xyz, dest);
    }

    public Matrix4d scale(double xyz) {
        return this.scale(xyz, xyz, xyz);
    }

    public Matrix4d scaleXY(double x, double y, Matrix4d dest) {
        return this.scale(x, y, 1.0, dest);
    }

    public Matrix4d scaleXY(double x, double y) {
        return this.scale(x, y, 1.0);
    }

    public Matrix4d scaleAround(double sx, double sy, double sz, double ox, double oy, double oz, Matrix4d dest) {
        double nm30 = this.m00 * ox + this.m10 * oy + this.m20 * oz + this.m30;
        double nm31 = this.m01 * ox + this.m11 * oy + this.m21 * oz + this.m31;
        double nm32 = this.m02 * ox + this.m12 * oy + this.m22 * oz + this.m32;
        double nm33 = this.m03 * ox + this.m13 * oy + this.m23 * oz + this.m33;
        boolean one = Math.absEqualsOne(sx) && Math.absEqualsOne(sy) && Math.absEqualsOne(sz);
        return dest._m00(this.m00 * sx)._m01(this.m01 * sx)._m02(this.m02 * sx)._m03(this.m03 * sx)._m10(this.m10 * sy)._m11(this.m11 * sy)._m12(this.m12 * sy)._m13(this.m13 * sy)._m20(this.m20 * sz)._m21(this.m21 * sz)._m22(this.m22 * sz)._m23(this.m23 * sz)._m30(-dest.m00 * ox - dest.m10 * oy - dest.m20 * oz + nm30)._m31(-dest.m01 * ox - dest.m11 * oy - dest.m21 * oz + nm31)._m32(-dest.m02 * ox - dest.m12 * oy - dest.m22 * oz + nm32)._m33(-dest.m03 * ox - dest.m13 * oy - dest.m23 * oz + nm33)._properties(this.properties & ~(13 | (one ? 0 : 16)));
    }

    public Matrix4d scaleAround(double sx, double sy, double sz, double ox, double oy, double oz) {
        return this.scaleAround(sx, sy, sz, ox, oy, oz, this);
    }

    public Matrix4d scaleAround(double factor, double ox, double oy, double oz) {
        return this.scaleAround(factor, factor, factor, ox, oy, oz, this);
    }

    public Matrix4d scaleAround(double factor, double ox, double oy, double oz, Matrix4d dest) {
        return this.scaleAround(factor, factor, factor, ox, oy, oz, dest);
    }

    public Matrix4d scaleLocal(double x, double y, double z, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.scaling(x, y, z) : this.scaleLocalGeneric(x, y, z, dest);
    }

    private Matrix4d scaleLocalGeneric(double x, double y, double z, Matrix4d dest) {
        double nm00 = x * this.m00;
        double nm01 = y * this.m01;
        double nm02 = z * this.m02;
        double nm10 = x * this.m10;
        double nm11 = y * this.m11;
        double nm12 = z * this.m12;
        double nm20 = x * this.m20;
        double nm21 = y * this.m21;
        double nm22 = z * this.m22;
        double nm30 = x * this.m30;
        double nm31 = y * this.m31;
        double nm32 = z * this.m32;
        boolean one = Math.absEqualsOne(x) && Math.absEqualsOne(y) && Math.absEqualsOne(z);
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(this.m03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(this.m13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(this.m23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(this.m33)._properties(this.properties & ~(13 | (one ? 0 : 16)));
        return dest;
    }

    public Matrix4d scaleLocal(double xyz, Matrix4d dest) {
        return this.scaleLocal(xyz, xyz, xyz, dest);
    }

    public Matrix4d scaleLocal(double xyz) {
        return this.scaleLocal(xyz, this);
    }

    public Matrix4d scaleLocal(double x, double y, double z) {
        return this.scaleLocal(x, y, z, this);
    }

    public Matrix4d scaleAroundLocal(double sx, double sy, double sz, double ox, double oy, double oz, Matrix4d dest) {
        boolean one = Math.absEqualsOne(sx) && Math.absEqualsOne(sy) && Math.absEqualsOne(sz);
        dest._m00(sx * (this.m00 - ox * this.m03) + ox * this.m03)._m01(sy * (this.m01 - oy * this.m03) + oy * this.m03)._m02(sz * (this.m02 - oz * this.m03) + oz * this.m03)._m03(this.m03)._m10(sx * (this.m10 - ox * this.m13) + ox * this.m13)._m11(sy * (this.m11 - oy * this.m13) + oy * this.m13)._m12(sz * (this.m12 - oz * this.m13) + oz * this.m13)._m13(this.m13)._m20(sx * (this.m20 - ox * this.m23) + ox * this.m23)._m21(sy * (this.m21 - oy * this.m23) + oy * this.m23)._m22(sz * (this.m22 - oz * this.m23) + oz * this.m23)._m23(this.m23)._m30(sx * (this.m30 - ox * this.m33) + ox * this.m33)._m31(sy * (this.m31 - oy * this.m33) + oy * this.m33)._m32(sz * (this.m32 - oz * this.m33) + oz * this.m33)._m33(this.m33)._properties(this.properties & ~(13 | (one ? 0 : 16)));
        return dest;
    }

    public Matrix4d scaleAroundLocal(double sx, double sy, double sz, double ox, double oy, double oz) {
        return this.scaleAroundLocal(sx, sy, sz, ox, oy, oz, this);
    }

    public Matrix4d scaleAroundLocal(double factor, double ox, double oy, double oz) {
        return this.scaleAroundLocal(factor, factor, factor, ox, oy, oz, this);
    }

    public Matrix4d scaleAroundLocal(double factor, double ox, double oy, double oz, Matrix4d dest) {
        return this.scaleAroundLocal(factor, factor, factor, ox, oy, oz, dest);
    }

    public Matrix4d rotate(double ang, double x, double y, double z, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotation(ang, x, y, z);
        } else if ((this.properties & 8) != 0) {
            return this.rotateTranslation(ang, x, y, z, dest);
        } else {
            return (this.properties & 2) != 0 ? this.rotateAffine(ang, x, y, z, dest) : this.rotateGeneric(ang, x, y, z, dest);
        }
    }

    private Matrix4d rotateGeneric(double ang, double x, double y, double z, Matrix4d dest) {
        if (y == 0.0 && z == 0.0 && Math.absEqualsOne(x)) {
            return this.rotateX(x * ang, dest);
        } else if (x == 0.0 && z == 0.0 && Math.absEqualsOne(y)) {
            return this.rotateY(y * ang, dest);
        } else {
            return x == 0.0 && y == 0.0 && Math.absEqualsOne(z) ? this.rotateZ(z * ang, dest) : this.rotateGenericInternal(ang, x, y, z, dest);
        }
    }

    private Matrix4d rotateGenericInternal(double ang, double x, double y, double z, Matrix4d dest) {
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
        double nm03 = this.m03 * rm00 + this.m13 * rm01 + this.m23 * rm02;
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        double nm13 = this.m03 * rm10 + this.m13 * rm11 + this.m23 * rm12;
        dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotate(double ang, double x, double y, double z) {
        return this.rotate(ang, x, y, z, this);
    }

    public Matrix4d rotateTranslation(double ang, double x, double y, double z, Matrix4d dest) {
        double tx = this.m30;
        double ty = this.m31;
        double tz = this.m32;
        if (y == 0.0 && z == 0.0 && Math.absEqualsOne(x)) {
            return dest.rotationX(x * ang).setTranslation(tx, ty, tz);
        } else if (x == 0.0 && z == 0.0 && Math.absEqualsOne(y)) {
            return dest.rotationY(y * ang).setTranslation(tx, ty, tz);
        } else {
            return x == 0.0 && y == 0.0 && Math.absEqualsOne(z) ? dest.rotationZ(z * ang).setTranslation(tx, ty, tz) : this.rotateTranslationInternal(ang, x, y, z, dest);
        }
    }

    private Matrix4d rotateTranslationInternal(double ang, double x, double y, double z, Matrix4d dest) {
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
        return dest._m20(rm20)._m21(rm21)._m22(rm22)._m23(0.0)._m00(rm00)._m01(rm01)._m02(rm02)._m03(0.0)._m10(rm10)._m11(rm11)._m12(rm12)._m13(0.0)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(1.0)._properties(this.properties & -14);
    }

    public Matrix4d rotateAffine(double ang, double x, double y, double z, Matrix4d dest) {
        if (y == 0.0 && z == 0.0 && Math.absEqualsOne(x)) {
            return this.rotateX(x * ang, dest);
        } else if (x == 0.0 && z == 0.0 && Math.absEqualsOne(y)) {
            return this.rotateY(y * ang, dest);
        } else {
            return x == 0.0 && y == 0.0 && Math.absEqualsOne(z) ? this.rotateZ(z * ang, dest) : this.rotateAffineInternal(ang, x, y, z, dest);
        }
    }

    private Matrix4d rotateAffineInternal(double ang, double x, double y, double z, Matrix4d dest) {
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
        dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(0.0)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0.0)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateAffine(double ang, double x, double y, double z) {
        return this.rotateAffine(ang, x, y, z, this);
    }

    public Matrix4d rotateAround(Quaterniond quat, double ox, double oy, double oz) {
        return this.rotateAround(quat, ox, oy, oz, this);
    }

    public Matrix4d rotateAroundAffine(Quaterniond quat, double ox, double oy, double oz, Matrix4d dest) {
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
        double rm10 = -dzw + dxy;
        double rm11 = y2 - z2 + w2 - x2;
        double rm12 = dyz + dxw;
        double rm20 = dyw + dxz;
        double rm21 = dyz - dxw;
        double rm22 = z2 - y2 - x2 + w2;
        double tm30 = this.m00 * ox + this.m10 * oy + this.m20 * oz + this.m30;
        double tm31 = this.m01 * ox + this.m11 * oy + this.m21 * oz + this.m31;
        double tm32 = this.m02 * ox + this.m12 * oy + this.m22 * oz + this.m32;
        double nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        double nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        double nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(0.0)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0.0)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)._m30(-nm00 * ox - nm10 * oy - this.m20 * oz + tm30)._m31(-nm01 * ox - nm11 * oy - this.m21 * oz + tm31)._m32(-nm02 * ox - nm12 * oy - this.m22 * oz + tm32)._m33(1.0)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateAround(Quaterniond quat, double ox, double oy, double oz, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return this.rotationAround(quat, ox, oy, oz);
        } else {
            return (this.properties & 2) != 0 ? this.rotateAroundAffine(quat, ox, oy, oz, this) : this.rotateAroundGeneric(quat, ox, oy, oz, this);
        }
    }

    private Matrix4d rotateAroundGeneric(Quaterniond quat, double ox, double oy, double oz, Matrix4d dest) {
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
        double rm10 = -dzw + dxy;
        double rm11 = y2 - z2 + w2 - x2;
        double rm12 = dyz + dxw;
        double rm20 = dyw + dxz;
        double rm21 = dyz - dxw;
        double rm22 = z2 - y2 - x2 + w2;
        double tm30 = this.m00 * ox + this.m10 * oy + this.m20 * oz + this.m30;
        double tm31 = this.m01 * ox + this.m11 * oy + this.m21 * oz + this.m31;
        double tm32 = this.m02 * ox + this.m12 * oy + this.m22 * oz + this.m32;
        double nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        double nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        double nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        double nm03 = this.m03 * rm00 + this.m13 * rm01 + this.m23 * rm02;
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        double nm13 = this.m03 * rm10 + this.m13 * rm11 + this.m23 * rm12;
        dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m30(-nm00 * ox - nm10 * oy - this.m20 * oz + tm30)._m31(-nm01 * ox - nm11 * oy - this.m21 * oz + tm31)._m32(-nm02 * ox - nm12 * oy - this.m22 * oz + tm32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotationAround(Quaterniond quat, double ox, double oy, double oz) {
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
        this._m20(dyw + dxz);
        this._m21(dyz - dxw);
        this._m22(z2 - y2 - x2 + w2);
        this._m23(0.0);
        this._m00(w2 + x2 - z2 - y2);
        this._m01(dxy + dzw);
        this._m02(dxz - dyw);
        this._m03(0.0);
        this._m10(-dzw + dxy);
        this._m11(y2 - z2 + w2 - x2);
        this._m12(dyz + dxw);
        this._m13(0.0);
        this._m30(-this.m00 * ox - this.m10 * oy - this.m20 * oz + ox);
        this._m31(-this.m01 * ox - this.m11 * oy - this.m21 * oz + oy);
        this._m32(-this.m02 * ox - this.m12 * oy - this.m22 * oz + oz);
        this._m33(1.0);
        this.properties = 18;
        return this;
    }

    public Matrix4d rotateLocal(double ang, double x, double y, double z, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.rotation(ang, x, y, z) : this.rotateLocalGeneric(ang, x, y, z, dest);
    }

    private Matrix4d rotateLocalGeneric(double ang, double x, double y, double z, Matrix4d dest) {
        if (y == 0.0 && z == 0.0 && Math.absEqualsOne(x)) {
            return this.rotateLocalX(x * ang, dest);
        } else if (x == 0.0 && z == 0.0 && Math.absEqualsOne(y)) {
            return this.rotateLocalY(y * ang, dest);
        } else {
            return x == 0.0 && y == 0.0 && Math.absEqualsOne(z) ? this.rotateLocalZ(z * ang, dest) : this.rotateLocalGenericInternal(ang, x, y, z, dest);
        }
    }

    private Matrix4d rotateLocalGenericInternal(double ang, double x, double y, double z, Matrix4d dest) {
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
        double nm30 = lm00 * this.m30 + lm10 * this.m31 + lm20 * this.m32;
        double nm31 = lm01 * this.m30 + lm11 * this.m31 + lm21 * this.m32;
        double nm32 = lm02 * this.m30 + lm12 * this.m31 + lm22 * this.m32;
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(this.m03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(this.m13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(this.m23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateLocal(double ang, double x, double y, double z) {
        return this.rotateLocal(ang, x, y, z, this);
    }

    public Matrix4d rotateAroundLocal(Quaterniond quat, double ox, double oy, double oz, Matrix4d dest) {
        double w2 = quat.w * quat.w;
        double x2 = quat.x * quat.x;
        double y2 = quat.y * quat.y;
        double z2 = quat.z * quat.z;
        double zw = quat.z * quat.w;
        double xy = quat.x * quat.y;
        double xz = quat.x * quat.z;
        double yw = quat.y * quat.w;
        double yz = quat.y * quat.z;
        double xw = quat.x * quat.w;
        double lm00 = w2 + x2 - z2 - y2;
        double lm01 = xy + zw + zw + xy;
        double lm02 = xz - yw + xz - yw;
        double lm10 = -zw + xy - zw + xy;
        double lm11 = y2 - z2 + w2 - x2;
        double lm12 = yz + yz + xw + xw;
        double lm20 = yw + xz + xz + yw;
        double lm21 = yz + yz - xw - xw;
        double lm22 = z2 - y2 - x2 + w2;
        double tm00 = this.m00 - ox * this.m03;
        double tm01 = this.m01 - oy * this.m03;
        double tm02 = this.m02 - oz * this.m03;
        double tm10 = this.m10 - ox * this.m13;
        double tm11 = this.m11 - oy * this.m13;
        double tm12 = this.m12 - oz * this.m13;
        double tm20 = this.m20 - ox * this.m23;
        double tm21 = this.m21 - oy * this.m23;
        double tm22 = this.m22 - oz * this.m23;
        double tm30 = this.m30 - ox * this.m33;
        double tm31 = this.m31 - oy * this.m33;
        double tm32 = this.m32 - oz * this.m33;
        dest._m00(lm00 * tm00 + lm10 * tm01 + lm20 * tm02 + ox * this.m03)._m01(lm01 * tm00 + lm11 * tm01 + lm21 * tm02 + oy * this.m03)._m02(lm02 * tm00 + lm12 * tm01 + lm22 * tm02 + oz * this.m03)._m03(this.m03)._m10(lm00 * tm10 + lm10 * tm11 + lm20 * tm12 + ox * this.m13)._m11(lm01 * tm10 + lm11 * tm11 + lm21 * tm12 + oy * this.m13)._m12(lm02 * tm10 + lm12 * tm11 + lm22 * tm12 + oz * this.m13)._m13(this.m13)._m20(lm00 * tm20 + lm10 * tm21 + lm20 * tm22 + ox * this.m23)._m21(lm01 * tm20 + lm11 * tm21 + lm21 * tm22 + oy * this.m23)._m22(lm02 * tm20 + lm12 * tm21 + lm22 * tm22 + oz * this.m23)._m23(this.m23)._m30(lm00 * tm30 + lm10 * tm31 + lm20 * tm32 + ox * this.m33)._m31(lm01 * tm30 + lm11 * tm31 + lm21 * tm32 + oy * this.m33)._m32(lm02 * tm30 + lm12 * tm31 + lm22 * tm32 + oz * this.m33)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateAroundLocal(Quaterniond quat, double ox, double oy, double oz) {
        return this.rotateAroundLocal(quat, ox, oy, oz, this);
    }

    public Matrix4d translate(Vector3d offset) {
        return this.translate(offset.x, offset.y, offset.z);
    }

    public Matrix4d translate(Vector3d offset, Matrix4d dest) {
        return this.translate(offset.x, offset.y, offset.z, dest);
    }

    public Matrix4d translate(Vector3f offset) {
        return this.translate(offset.x, offset.y, offset.z);
    }

    public Matrix4d translate(Vector3f offset, Matrix4d dest) {
        return this.translate(offset.x, offset.y, offset.z, dest);
    }

    public Matrix4d translate(double x, double y, double z, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.translation(x, y, z) : this.translateGeneric(x, y, z, dest);
    }

    private Matrix4d translateGeneric(double x, double y, double z, Matrix4d dest) {
        dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(Math.fma(this.m00, x, Math.fma(this.m10, y, Math.fma(this.m20, z, this.m30))))._m31(Math.fma(this.m01, x, Math.fma(this.m11, y, Math.fma(this.m21, z, this.m31))))._m32(Math.fma(this.m02, x, Math.fma(this.m12, y, Math.fma(this.m22, z, this.m32))))._m33(Math.fma(this.m03, x, Math.fma(this.m13, y, Math.fma(this.m23, z, this.m33))))._properties(this.properties & -6);
        return dest;
    }

    public Matrix4d translate(double x, double y, double z) {
        if ((this.properties & 4) != 0) {
            return this.translation(x, y, z);
        } else {
            this._m30(Math.fma(this.m00, x, Math.fma(this.m10, y, Math.fma(this.m20, z, this.m30))));
            this._m31(Math.fma(this.m01, x, Math.fma(this.m11, y, Math.fma(this.m21, z, this.m31))));
            this._m32(Math.fma(this.m02, x, Math.fma(this.m12, y, Math.fma(this.m22, z, this.m32))));
            this._m33(Math.fma(this.m03, x, Math.fma(this.m13, y, Math.fma(this.m23, z, this.m33))));
            this.properties &= -6;
            return this;
        }
    }

    public Matrix4d translateLocal(Vector3f offset) {
        return this.translateLocal(offset.x, offset.y, offset.z);
    }

    public Matrix4d translateLocal(Vector3f offset, Matrix4d dest) {
        return this.translateLocal(offset.x, offset.y, offset.z, dest);
    }

    public Matrix4d translateLocal(Vector3d offset) {
        return this.translateLocal(offset.x, offset.y, offset.z);
    }

    public Matrix4d translateLocal(Vector3d offset, Matrix4d dest) {
        return this.translateLocal(offset.x, offset.y, offset.z, dest);
    }

    public Matrix4d translateLocal(double x, double y, double z, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.translation(x, y, z) : this.translateLocalGeneric(x, y, z, dest);
    }

    private Matrix4d translateLocalGeneric(double x, double y, double z, Matrix4d dest) {
        double nm00 = this.m00 + x * this.m03;
        double nm01 = this.m01 + y * this.m03;
        double nm02 = this.m02 + z * this.m03;
        double nm10 = this.m10 + x * this.m13;
        double nm11 = this.m11 + y * this.m13;
        double nm12 = this.m12 + z * this.m13;
        double nm20 = this.m20 + x * this.m23;
        double nm21 = this.m21 + y * this.m23;
        double nm22 = this.m22 + z * this.m23;
        double nm30 = this.m30 + x * this.m33;
        double nm31 = this.m31 + y * this.m33;
        double nm32 = this.m32 + z * this.m33;
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(this.m03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(this.m13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(this.m23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(this.m33)._properties(this.properties & -6);
    }

    public Matrix4d translateLocal(double x, double y, double z) {
        return this.translateLocal(x, y, z, this);
    }

    public Matrix4d rotateLocalX(double ang, Matrix4d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        double nm02 = sin * this.m01 + cos * this.m02;
        double nm12 = sin * this.m11 + cos * this.m12;
        double nm22 = sin * this.m21 + cos * this.m22;
        double nm32 = sin * this.m31 + cos * this.m32;
        dest._m00(this.m00)._m01(cos * this.m01 - sin * this.m02)._m02(nm02)._m03(this.m03)._m10(this.m10)._m11(cos * this.m11 - sin * this.m12)._m12(nm12)._m13(this.m13)._m20(this.m20)._m21(cos * this.m21 - sin * this.m22)._m22(nm22)._m23(this.m23)._m30(this.m30)._m31(cos * this.m31 - sin * this.m32)._m32(nm32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateLocalX(double ang) {
        return this.rotateLocalX(ang, this);
    }

    public Matrix4d rotateLocalY(double ang, Matrix4d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        double nm02 = -sin * this.m00 + cos * this.m02;
        double nm12 = -sin * this.m10 + cos * this.m12;
        double nm22 = -sin * this.m20 + cos * this.m22;
        double nm32 = -sin * this.m30 + cos * this.m32;
        dest._m00(cos * this.m00 + sin * this.m02)._m01(this.m01)._m02(nm02)._m03(this.m03)._m10(cos * this.m10 + sin * this.m12)._m11(this.m11)._m12(nm12)._m13(this.m13)._m20(cos * this.m20 + sin * this.m22)._m21(this.m21)._m22(nm22)._m23(this.m23)._m30(cos * this.m30 + sin * this.m32)._m31(this.m31)._m32(nm32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateLocalY(double ang) {
        return this.rotateLocalY(ang, this);
    }

    public Matrix4d rotateLocalZ(double ang, Matrix4d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        double nm01 = sin * this.m00 + cos * this.m01;
        double nm11 = sin * this.m10 + cos * this.m11;
        double nm21 = sin * this.m20 + cos * this.m21;
        double nm31 = sin * this.m30 + cos * this.m31;
        dest._m00(cos * this.m00 - sin * this.m01)._m01(nm01)._m02(this.m02)._m03(this.m03)._m10(cos * this.m10 - sin * this.m11)._m11(nm11)._m12(this.m12)._m13(this.m13)._m20(cos * this.m20 - sin * this.m21)._m21(nm21)._m22(this.m22)._m23(this.m23)._m30(cos * this.m30 - sin * this.m31)._m31(nm31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateLocalZ(double ang) {
        return this.rotateLocalZ(ang, this);
    }

    public Matrix4d rotateX(double ang, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationX(ang);
        } else if ((this.properties & 8) != 0) {
            double x = this.m30;
            double y = this.m31;
            double z = this.m32;
            return dest.rotationX(ang).setTranslation(x, y, z);
        } else {
            return this.rotateXInternal(ang, dest);
        }
    }

    private Matrix4d rotateXInternal(double ang, Matrix4d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        double rm21 = -sin;
        double nm10 = this.m10 * cos + this.m20 * sin;
        double nm11 = this.m11 * cos + this.m21 * sin;
        double nm12 = this.m12 * cos + this.m22 * sin;
        double nm13 = this.m13 * cos + this.m23 * sin;
        dest._m20(this.m10 * rm21 + this.m20 * cos)._m21(this.m11 * rm21 + this.m21 * cos)._m22(this.m12 * rm21 + this.m22 * cos)._m23(this.m13 * rm21 + this.m23 * cos)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateX(double ang) {
        return this.rotateX(ang, this);
    }

    public Matrix4d rotateY(double ang, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationY(ang);
        } else if ((this.properties & 8) != 0) {
            double x = this.m30;
            double y = this.m31;
            double z = this.m32;
            return dest.rotationY(ang).setTranslation(x, y, z);
        } else {
            return this.rotateYInternal(ang, dest);
        }
    }

    private Matrix4d rotateYInternal(double ang, Matrix4d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        double rm02 = -sin;
        double nm00 = this.m00 * cos + this.m20 * rm02;
        double nm01 = this.m01 * cos + this.m21 * rm02;
        double nm02 = this.m02 * cos + this.m22 * rm02;
        double nm03 = this.m03 * cos + this.m23 * rm02;
        dest._m20(this.m00 * sin + this.m20 * cos)._m21(this.m01 * sin + this.m21 * cos)._m22(this.m02 * sin + this.m22 * cos)._m23(this.m03 * sin + this.m23 * cos)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateY(double ang) {
        return this.rotateY(ang, this);
    }

    public Matrix4d rotateZ(double ang, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationZ(ang);
        } else if ((this.properties & 8) != 0) {
            double x = this.m30;
            double y = this.m31;
            double z = this.m32;
            return dest.rotationZ(ang).setTranslation(x, y, z);
        } else {
            return this.rotateZInternal(ang, dest);
        }
    }

    private Matrix4d rotateZInternal(double ang, Matrix4d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        return this.rotateTowardsXY(sin, cos, dest);
    }

    public Matrix4d rotateZ(double ang) {
        return this.rotateZ(ang, this);
    }

    public Matrix4d rotateTowardsXY(double dirX, double dirY) {
        return this.rotateTowardsXY(dirX, dirY, this);
    }

    public Matrix4d rotateTowardsXY(double dirX, double dirY, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationTowardsXY(dirX, dirY);
        } else {
            double rm10 = -dirX;
            double nm00 = this.m00 * dirY + this.m10 * dirX;
            double nm01 = this.m01 * dirY + this.m11 * dirX;
            double nm02 = this.m02 * dirY + this.m12 * dirX;
            double nm03 = this.m03 * dirY + this.m13 * dirX;
            dest._m10(this.m00 * rm10 + this.m10 * dirY)._m11(this.m01 * rm10 + this.m11 * dirY)._m12(this.m02 * rm10 + this.m12 * dirY)._m13(this.m03 * rm10 + this.m13 * dirY)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
            return dest;
        }
    }

    public Matrix4d rotateXYZ(Vector3d angles) {
        return this.rotateXYZ(angles.x, angles.y, angles.z);
    }

    public Matrix4d rotateXYZ(double angleX, double angleY, double angleZ) {
        return this.rotateXYZ(angleX, angleY, angleZ, this);
    }

    public Matrix4d rotateXYZ(double angleX, double angleY, double angleZ, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationXYZ(angleX, angleY, angleZ);
        } else if ((this.properties & 8) != 0) {
            double tx = this.m30;
            double ty = this.m31;
            double tz = this.m32;
            return dest.rotationXYZ(angleX, angleY, angleZ).setTranslation(tx, ty, tz);
        } else {
            return (this.properties & 2) != 0 ? dest.rotateAffineXYZ(angleX, angleY, angleZ) : this.rotateXYZInternal(angleX, angleY, angleZ, dest);
        }
    }

    private Matrix4d rotateXYZInternal(double angleX, double angleY, double angleZ, Matrix4d dest) {
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
        double nm13 = this.m13 * cosX + this.m23 * sinX;
        double nm20 = this.m10 * m_sinX + this.m20 * cosX;
        double nm21 = this.m11 * m_sinX + this.m21 * cosX;
        double nm22 = this.m12 * m_sinX + this.m22 * cosX;
        double nm23 = this.m13 * m_sinX + this.m23 * cosX;
        double nm00 = this.m00 * cosY + nm20 * m_sinY;
        double nm01 = this.m01 * cosY + nm21 * m_sinY;
        double nm02 = this.m02 * cosY + nm22 * m_sinY;
        double nm03 = this.m03 * cosY + nm23 * m_sinY;
        dest._m20(this.m00 * sinY + nm20 * cosY)._m21(this.m01 * sinY + nm21 * cosY)._m22(this.m02 * sinY + nm22 * cosY)._m23(this.m03 * sinY + nm23 * cosY)._m00(nm00 * cosZ + nm10 * sinZ)._m01(nm01 * cosZ + nm11 * sinZ)._m02(nm02 * cosZ + nm12 * sinZ)._m03(nm03 * cosZ + nm13 * sinZ)._m10(nm00 * m_sinZ + nm10 * cosZ)._m11(nm01 * m_sinZ + nm11 * cosZ)._m12(nm02 * m_sinZ + nm12 * cosZ)._m13(nm03 * m_sinZ + nm13 * cosZ)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateAffineXYZ(double angleX, double angleY, double angleZ) {
        return this.rotateAffineXYZ(angleX, angleY, angleZ, this);
    }

    public Matrix4d rotateAffineXYZ(double angleX, double angleY, double angleZ, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationXYZ(angleX, angleY, angleZ);
        } else if ((this.properties & 8) != 0) {
            double tx = this.m30;
            double ty = this.m31;
            double tz = this.m32;
            return dest.rotationXYZ(angleX, angleY, angleZ).setTranslation(tx, ty, tz);
        } else {
            return this.rotateAffineXYZInternal(angleX, angleY, angleZ, dest);
        }
    }

    private Matrix4d rotateAffineXYZInternal(double angleX, double angleY, double angleZ, Matrix4d dest) {
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
        dest._m20(this.m00 * sinY + nm20 * cosY)._m21(this.m01 * sinY + nm21 * cosY)._m22(this.m02 * sinY + nm22 * cosY)._m23(0.0)._m00(nm00 * cosZ + nm10 * sinZ)._m01(nm01 * cosZ + nm11 * sinZ)._m02(nm02 * cosZ + nm12 * sinZ)._m03(0.0)._m10(nm00 * m_sinZ + nm10 * cosZ)._m11(nm01 * m_sinZ + nm11 * cosZ)._m12(nm02 * m_sinZ + nm12 * cosZ)._m13(0.0)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateZYX(Vector3d angles) {
        return this.rotateZYX(angles.z, angles.y, angles.x);
    }

    public Matrix4d rotateZYX(double angleZ, double angleY, double angleX) {
        return this.rotateZYX(angleZ, angleY, angleX, this);
    }

    public Matrix4d rotateZYX(double angleZ, double angleY, double angleX, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationZYX(angleZ, angleY, angleX);
        } else if ((this.properties & 8) != 0) {
            double tx = this.m30;
            double ty = this.m31;
            double tz = this.m32;
            return dest.rotationZYX(angleZ, angleY, angleX).setTranslation(tx, ty, tz);
        } else {
            return (this.properties & 2) != 0 ? dest.rotateAffineZYX(angleZ, angleY, angleX) : this.rotateZYXInternal(angleZ, angleY, angleX, dest);
        }
    }

    private Matrix4d rotateZYXInternal(double angleZ, double angleY, double angleX, Matrix4d dest) {
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
        double nm03 = this.m03 * cosZ + this.m13 * sinZ;
        double nm10 = this.m00 * m_sinZ + this.m10 * cosZ;
        double nm11 = this.m01 * m_sinZ + this.m11 * cosZ;
        double nm12 = this.m02 * m_sinZ + this.m12 * cosZ;
        double nm13 = this.m03 * m_sinZ + this.m13 * cosZ;
        double nm20 = nm00 * sinY + this.m20 * cosY;
        double nm21 = nm01 * sinY + this.m21 * cosY;
        double nm22 = nm02 * sinY + this.m22 * cosY;
        double nm23 = nm03 * sinY + this.m23 * cosY;
        dest._m00(nm00 * cosY + this.m20 * m_sinY)._m01(nm01 * cosY + this.m21 * m_sinY)._m02(nm02 * cosY + this.m22 * m_sinY)._m03(nm03 * cosY + this.m23 * m_sinY)._m10(nm10 * cosX + nm20 * sinX)._m11(nm11 * cosX + nm21 * sinX)._m12(nm12 * cosX + nm22 * sinX)._m13(nm13 * cosX + nm23 * sinX)._m20(nm10 * m_sinX + nm20 * cosX)._m21(nm11 * m_sinX + nm21 * cosX)._m22(nm12 * m_sinX + nm22 * cosX)._m23(nm13 * m_sinX + nm23 * cosX)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateAffineZYX(double angleZ, double angleY, double angleX) {
        return this.rotateAffineZYX(angleZ, angleY, angleX, this);
    }

    public Matrix4d rotateAffineZYX(double angleZ, double angleY, double angleX, Matrix4d dest) {
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
        dest._m00(nm00 * cosY + this.m20 * m_sinY)._m01(nm01 * cosY + this.m21 * m_sinY)._m02(nm02 * cosY + this.m22 * m_sinY)._m03(0.0)._m10(nm10 * cosX + nm20 * sinX)._m11(nm11 * cosX + nm21 * sinX)._m12(nm12 * cosX + nm22 * sinX)._m13(0.0)._m20(nm10 * m_sinX + nm20 * cosX)._m21(nm11 * m_sinX + nm21 * cosX)._m22(nm12 * m_sinX + nm22 * cosX)._m23(0.0)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateYXZ(Vector3d angles) {
        return this.rotateYXZ(angles.y, angles.x, angles.z);
    }

    public Matrix4d rotateYXZ(double angleY, double angleX, double angleZ) {
        return this.rotateYXZ(angleY, angleX, angleZ, this);
    }

    public Matrix4d rotateYXZ(double angleY, double angleX, double angleZ, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationYXZ(angleY, angleX, angleZ);
        } else if ((this.properties & 8) != 0) {
            double tx = this.m30;
            double ty = this.m31;
            double tz = this.m32;
            return dest.rotationYXZ(angleY, angleX, angleZ).setTranslation(tx, ty, tz);
        } else {
            return (this.properties & 2) != 0 ? dest.rotateAffineYXZ(angleY, angleX, angleZ) : this.rotateYXZInternal(angleY, angleX, angleZ, dest);
        }
    }

    private Matrix4d rotateYXZInternal(double angleY, double angleX, double angleZ, Matrix4d dest) {
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
        double nm23 = this.m03 * sinY + this.m23 * cosY;
        double nm00 = this.m00 * cosY + this.m20 * m_sinY;
        double nm01 = this.m01 * cosY + this.m21 * m_sinY;
        double nm02 = this.m02 * cosY + this.m22 * m_sinY;
        double nm03 = this.m03 * cosY + this.m23 * m_sinY;
        double nm10 = this.m10 * cosX + nm20 * sinX;
        double nm11 = this.m11 * cosX + nm21 * sinX;
        double nm12 = this.m12 * cosX + nm22 * sinX;
        double nm13 = this.m13 * cosX + nm23 * sinX;
        dest._m20(this.m10 * m_sinX + nm20 * cosX)._m21(this.m11 * m_sinX + nm21 * cosX)._m22(this.m12 * m_sinX + nm22 * cosX)._m23(this.m13 * m_sinX + nm23 * cosX)._m00(nm00 * cosZ + nm10 * sinZ)._m01(nm01 * cosZ + nm11 * sinZ)._m02(nm02 * cosZ + nm12 * sinZ)._m03(nm03 * cosZ + nm13 * sinZ)._m10(nm00 * m_sinZ + nm10 * cosZ)._m11(nm01 * m_sinZ + nm11 * cosZ)._m12(nm02 * m_sinZ + nm12 * cosZ)._m13(nm03 * m_sinZ + nm13 * cosZ)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateAffineYXZ(double angleY, double angleX, double angleZ) {
        return this.rotateAffineYXZ(angleY, angleX, angleZ, this);
    }

    public Matrix4d rotateAffineYXZ(double angleY, double angleX, double angleZ, Matrix4d dest) {
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
        dest._m20(this.m10 * m_sinX + nm20 * cosX)._m21(this.m11 * m_sinX + nm21 * cosX)._m22(this.m12 * m_sinX + nm22 * cosX)._m23(0.0)._m00(nm00 * cosZ + nm10 * sinZ)._m01(nm01 * cosZ + nm11 * sinZ)._m02(nm02 * cosZ + nm12 * sinZ)._m03(0.0)._m10(nm00 * m_sinZ + nm10 * cosZ)._m11(nm01 * m_sinZ + nm11 * cosZ)._m12(nm02 * m_sinZ + nm12 * cosZ)._m13(0.0)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotation(AxisAngle4f angleAxis) {
        return this.rotation(angleAxis.angle, angleAxis.x, angleAxis.y, angleAxis.z);
    }

    public Matrix4d rotation(AxisAngle4d angleAxis) {
        return this.rotation(angleAxis.angle, angleAxis.x, angleAxis.y, angleAxis.z);
    }

    public Matrix4d rotation(Quaterniond quat) {
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
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this._m00(w2 + x2 - z2 - y2)._m01(dxy + dzw)._m02(dxz - dyw)._m10(-dzw + dxy)._m11(y2 - z2 + w2 - x2)._m12(dyz + dxw)._m20(dyw + dxz)._m21(dyz - dxw)._m22(z2 - y2 - x2 + w2)._properties(18);
        return this;
    }

    public Matrix4d rotation(Quaternionf quat) {
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
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this._m00(w2 + x2 - z2 - y2)._m01(dxy + dzw)._m02(dxz - dyw)._m10(-dzw + dxy)._m11(y2 - z2 + w2 - x2)._m12(dyz + dxw)._m20(dyw + dxz)._m21(dyz - dxw)._m22(z2 - y2 - x2 + w2)._properties(18);
        return this;
    }

    public Matrix4d translationRotateScale(double tx, double ty, double tz, double qx, double qy, double qz, double qw, double sx, double sy, double sz) {
        double dqx = qx + qx;
        double dqy = qy + qy;
        double dqz = qz + qz;
        double q00 = dqx * qx;
        double q11 = dqy * qy;
        double q22 = dqz * qz;
        double q01 = dqx * qy;
        double q02 = dqx * qz;
        double q03 = dqx * qw;
        double q12 = dqy * qz;
        double q13 = dqy * qw;
        double q23 = dqz * qw;
        boolean one = Math.absEqualsOne(sx) && Math.absEqualsOne(sy) && Math.absEqualsOne(sz);
        this._m00(sx - (q11 + q22) * sx)._m01((q01 + q23) * sx)._m02((q02 - q13) * sx)._m03(0.0)._m10((q01 - q23) * sy)._m11(sy - (q22 + q00) * sy)._m12((q12 + q03) * sy)._m13(0.0)._m20((q02 + q13) * sz)._m21((q12 - q03) * sz)._m22(sz - (q11 + q00) * sz)._m23(0.0)._m30(tx)._m31(ty)._m32(tz)._m33(1.0).properties = 2 | (one ? 16 : 0);
        return this;
    }

    public Matrix4d translationRotateScale(Vector3f translation, Quaternionf quat, Vector3f scale) {
        return this.translationRotateScale(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale.x, scale.y, scale.z);
    }

    public Matrix4d translationRotateScale(Vector3d translation, Quaterniond quat, Vector3d scale) {
        return this.translationRotateScale(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale.x, scale.y, scale.z);
    }

    public Matrix4d translationRotateScale(double tx, double ty, double tz, double qx, double qy, double qz, double qw, double scale) {
        return this.translationRotateScale(tx, ty, tz, qx, qy, qz, qw, scale, scale, scale);
    }

    public Matrix4d translationRotateScale(Vector3d translation, Quaterniond quat, double scale) {
        return this.translationRotateScale(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale, scale, scale);
    }

    public Matrix4d translationRotateScale(Vector3f translation, Quaternionf quat, double scale) {
        return this.translationRotateScale(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale, scale, scale);
    }

    public Matrix4d translationRotateScaleInvert(double tx, double ty, double tz, double qx, double qy, double qz, double qw, double sx, double sy, double sz) {
        boolean one = Math.absEqualsOne(sx) && Math.absEqualsOne(sy) && Math.absEqualsOne(sz);
        if (one) {
            return this.translationRotateInvert(tx, ty, tz, qx, qy, qz, qw);
        } else {
            double nqx = -qx;
            double nqy = -qy;
            double nqz = -qz;
            double dqx = nqx + nqx;
            double dqy = nqy + nqy;
            double dqz = nqz + nqz;
            double q00 = dqx * nqx;
            double q11 = dqy * nqy;
            double q22 = dqz * nqz;
            double q01 = dqx * nqy;
            double q02 = dqx * nqz;
            double q03 = dqx * qw;
            double q12 = dqy * nqz;
            double q13 = dqy * qw;
            double q23 = dqz * qw;
            double isx = 1.0 / sx;
            double isy = 1.0 / sy;
            double isz = 1.0 / sz;
            this._m00(isx * (1.0 - q11 - q22))._m01(isy * (q01 + q23))._m02(isz * (q02 - q13))._m03(0.0)._m10(isx * (q01 - q23))._m11(isy * (1.0 - q22 - q00))._m12(isz * (q12 + q03))._m13(0.0)._m20(isx * (q02 + q13))._m21(isy * (q12 - q03))._m22(isz * (1.0 - q11 - q00))._m23(0.0)._m30(-this.m00 * tx - this.m10 * ty - this.m20 * tz)._m31(-this.m01 * tx - this.m11 * ty - this.m21 * tz)._m32(-this.m02 * tx - this.m12 * ty - this.m22 * tz)._m33(1.0).properties = 2;
            return this;
        }
    }

    public Matrix4d translationRotateScaleInvert(Vector3d translation, Quaterniond quat, Vector3d scale) {
        return this.translationRotateScaleInvert(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale.x, scale.y, scale.z);
    }

    public Matrix4d translationRotateScaleInvert(Vector3f translation, Quaternionf quat, Vector3f scale) {
        return this.translationRotateScaleInvert(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale.x, scale.y, scale.z);
    }

    public Matrix4d translationRotateScaleInvert(Vector3d translation, Quaterniond quat, double scale) {
        return this.translationRotateScaleInvert(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale, scale, scale);
    }

    public Matrix4d translationRotateScaleInvert(Vector3f translation, Quaternionf quat, double scale) {
        return this.translationRotateScaleInvert(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale, scale, scale);
    }

    public Matrix4d translationRotateScaleMulAffine(double tx, double ty, double tz, double qx, double qy, double qz, double qw, double sx, double sy, double sz, Matrix4d m) {
        double w2 = qw * qw;
        double x2 = qx * qx;
        double y2 = qy * qy;
        double z2 = qz * qz;
        double zw = qz * qw;
        double xy = qx * qy;
        double xz = qx * qz;
        double yw = qy * qw;
        double yz = qy * qz;
        double xw = qx * qw;
        double nm00 = w2 + x2 - z2 - y2;
        double nm01 = xy + zw + zw + xy;
        double nm02 = xz - yw + xz - yw;
        double nm10 = -zw + xy - zw + xy;
        double nm11 = y2 - z2 + w2 - x2;
        double nm12 = yz + yz + xw + xw;
        double nm20 = yw + xz + xz + yw;
        double nm21 = yz + yz - xw - xw;
        double nm22 = z2 - y2 - x2 + w2;
        double m00 = nm00 * m.m00 + nm10 * m.m01 + nm20 * m.m02;
        double m01 = nm01 * m.m00 + nm11 * m.m01 + nm21 * m.m02;
        this.m02 = nm02 * m.m00 + nm12 * m.m01 + nm22 * m.m02;
        this.m00 = m00;
        this.m01 = m01;
        this.m03 = 0.0;
        double m10 = nm00 * m.m10 + nm10 * m.m11 + nm20 * m.m12;
        double m11 = nm01 * m.m10 + nm11 * m.m11 + nm21 * m.m12;
        this.m12 = nm02 * m.m10 + nm12 * m.m11 + nm22 * m.m12;
        this.m10 = m10;
        this.m11 = m11;
        this.m13 = 0.0;
        double m20 = nm00 * m.m20 + nm10 * m.m21 + nm20 * m.m22;
        double m21 = nm01 * m.m20 + nm11 * m.m21 + nm21 * m.m22;
        this.m22 = nm02 * m.m20 + nm12 * m.m21 + nm22 * m.m22;
        this.m20 = m20;
        this.m21 = m21;
        this.m23 = 0.0;
        double m30 = nm00 * m.m30 + nm10 * m.m31 + nm20 * m.m32 + tx;
        double m31 = nm01 * m.m30 + nm11 * m.m31 + nm21 * m.m32 + ty;
        this.m32 = nm02 * m.m30 + nm12 * m.m31 + nm22 * m.m32 + tz;
        this.m30 = m30;
        this.m31 = m31;
        this.m33 = 1.0;
        boolean one = Math.absEqualsOne(sx) && Math.absEqualsOne(sy) && Math.absEqualsOne(sz);
        this.properties = 2 | (one && (m.properties & 16) != 0 ? 16 : 0);
        return this;
    }

    public Matrix4d translationRotateScaleMulAffine(Vector3f translation, Quaterniond quat, Vector3f scale, Matrix4d m) {
        return this.translationRotateScaleMulAffine(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale.x, scale.y, scale.z, m);
    }

    public Matrix4d translationRotate(double tx, double ty, double tz, double qx, double qy, double qz, double qw) {
        double w2 = qw * qw;
        double x2 = qx * qx;
        double y2 = qy * qy;
        double z2 = qz * qz;
        double zw = qz * qw;
        double xy = qx * qy;
        double xz = qx * qz;
        double yw = qy * qw;
        double yz = qy * qz;
        double xw = qx * qw;
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
        this.m33 = 1.0;
        this.properties = 18;
        return this;
    }

    public Matrix4d translationRotate(double tx, double ty, double tz, Quaterniond quat) {
        return this.translationRotate(tx, ty, tz, quat.x, quat.y, quat.z, quat.w);
    }

    public Matrix4d translationRotate(Vector3d translation, Quaterniond quat) {
        return this.translationRotate(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w);
    }

    public Matrix4d translationRotateInvert(double tx, double ty, double tz, double qx, double qy, double qz, double qw) {
        double nqx = -qx;
        double nqy = -qy;
        double nqz = -qz;
        double dqx = nqx + nqx;
        double dqy = nqy + nqy;
        double dqz = nqz + nqz;
        double q00 = dqx * nqx;
        double q11 = dqy * nqy;
        double q22 = dqz * nqz;
        double q01 = dqx * nqy;
        double q02 = dqx * nqz;
        double q03 = dqx * qw;
        double q12 = dqy * nqz;
        double q13 = dqy * qw;
        double q23 = dqz * qw;
        return this._m00(1.0 - q11 - q22)._m01(q01 + q23)._m02(q02 - q13)._m03(0.0)._m10(q01 - q23)._m11(1.0 - q22 - q00)._m12(q12 + q03)._m13(0.0)._m20(q02 + q13)._m21(q12 - q03)._m22(1.0 - q11 - q00)._m23(0.0)._m30(-this.m00 * tx - this.m10 * ty - this.m20 * tz)._m31(-this.m01 * tx - this.m11 * ty - this.m21 * tz)._m32(-this.m02 * tx - this.m12 * ty - this.m22 * tz)._m33(1.0)._properties(18);
    }

    public Matrix4d translationRotateInvert(Vector3f translation, Quaternionf quat) {
        return this.translationRotateInvert(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w);
    }

    public Matrix4d rotate(Quaterniond quat, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotation(quat);
        } else if ((this.properties & 8) != 0) {
            return this.rotateTranslation(quat, dest);
        } else {
            return (this.properties & 2) != 0 ? this.rotateAffine(quat, dest) : this.rotateGeneric(quat, dest);
        }
    }

    private Matrix4d rotateGeneric(Quaterniond quat, Matrix4d dest) {
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
        double rm10 = -dzw + dxy;
        double rm11 = y2 - z2 + w2 - x2;
        double rm12 = dyz + dxw;
        double rm20 = dyw + dxz;
        double rm21 = dyz - dxw;
        double rm22 = z2 - y2 - x2 + w2;
        double nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        double nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        double nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        double nm03 = this.m03 * rm00 + this.m13 * rm01 + this.m23 * rm02;
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        double nm13 = this.m03 * rm10 + this.m13 * rm11 + this.m23 * rm12;
        dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotate(Quaternionf quat, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotation(quat);
        } else if ((this.properties & 8) != 0) {
            return this.rotateTranslation(quat, dest);
        } else {
            return (this.properties & 2) != 0 ? this.rotateAffine(quat, dest) : this.rotateGeneric(quat, dest);
        }
    }

    private Matrix4d rotateGeneric(Quaternionf quat, Matrix4d dest) {
        double w2 = quat.w * quat.w;
        double x2 = quat.x * quat.x;
        double y2 = quat.y * quat.y;
        double z2 = quat.z * quat.z;
        double zw = quat.z * quat.w;
        double xy = quat.x * quat.y;
        double xz = quat.x * quat.z;
        double yw = quat.y * quat.w;
        double yz = quat.y * quat.z;
        double xw = quat.x * quat.w;
        double rm00 = w2 + x2 - z2 - y2;
        double rm01 = xy + zw + zw + xy;
        double rm02 = xz - yw + xz - yw;
        double rm10 = -zw + xy - zw + xy;
        double rm11 = y2 - z2 + w2 - x2;
        double rm12 = yz + yz + xw + xw;
        double rm20 = yw + xz + xz + yw;
        double rm21 = yz + yz - xw - xw;
        double rm22 = z2 - y2 - x2 + w2;
        double nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        double nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        double nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        double nm03 = this.m03 * rm00 + this.m13 * rm01 + this.m23 * rm02;
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        double nm13 = this.m03 * rm10 + this.m13 * rm11 + this.m23 * rm12;
        dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotate(Quaterniond quat) {
        return this.rotate(quat, this);
    }

    public Matrix4d rotate(Quaternionf quat) {
        return this.rotate(quat, this);
    }

    public Matrix4d rotateAffine(Quaterniond quat, Matrix4d dest) {
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
        double rm10 = -dzw + dxy;
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
        dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(0.0)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0.0)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateAffine(Quaterniond quat) {
        return this.rotateAffine(quat, this);
    }

    public Matrix4d rotateTranslation(Quaterniond quat, Matrix4d dest) {
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
        double rm10 = -dzw + dxy;
        double rm11 = y2 - z2 + w2 - x2;
        double rm12 = dyz + dxw;
        double rm20 = dyw + dxz;
        double rm21 = dyz - dxw;
        double rm22 = z2 - y2 - x2 + w2;
        dest._m20(rm20)._m21(rm21)._m22(rm22)._m23(0.0)._m00(rm00)._m01(rm01)._m02(rm02)._m03(0.0)._m10(rm10)._m11(rm11)._m12(rm12)._m13(0.0)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(1.0)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateTranslation(Quaternionf quat, Matrix4d dest) {
        double w2 = quat.w * quat.w;
        double x2 = quat.x * quat.x;
        double y2 = quat.y * quat.y;
        double z2 = quat.z * quat.z;
        double zw = quat.z * quat.w;
        double xy = quat.x * quat.y;
        double xz = quat.x * quat.z;
        double yw = quat.y * quat.w;
        double yz = quat.y * quat.z;
        double xw = quat.x * quat.w;
        double rm00 = w2 + x2 - z2 - y2;
        double rm01 = xy + zw + zw + xy;
        double rm02 = xz - yw + xz - yw;
        double rm10 = -zw + xy - zw + xy;
        double rm11 = y2 - z2 + w2 - x2;
        double rm12 = yz + yz + xw + xw;
        double rm20 = yw + xz + xz + yw;
        double rm21 = yz + yz - xw - xw;
        double rm22 = z2 - y2 - x2 + w2;
        dest._m20(rm20)._m21(rm21)._m22(rm22)._m23(0.0)._m00(rm00)._m01(rm01)._m02(rm02)._m03(0.0)._m10(rm10)._m11(rm11)._m12(rm12)._m13(0.0)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(1.0)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateLocal(Quaterniond quat, Matrix4d dest) {
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
        double lm10 = -dzw + dxy;
        double lm11 = y2 - z2 + w2 - x2;
        double lm12 = dyz + dxw;
        double lm20 = dyw + dxz;
        double lm21 = dyz - dxw;
        double lm22 = z2 - y2 - x2 + w2;
        double nm00 = lm00 * this.m00 + lm10 * this.m01 + lm20 * this.m02;
        double nm01 = lm01 * this.m00 + lm11 * this.m01 + lm21 * this.m02;
        double nm02 = lm02 * this.m00 + lm12 * this.m01 + lm22 * this.m02;
        double nm03 = this.m03;
        double nm10 = lm00 * this.m10 + lm10 * this.m11 + lm20 * this.m12;
        double nm11 = lm01 * this.m10 + lm11 * this.m11 + lm21 * this.m12;
        double nm12 = lm02 * this.m10 + lm12 * this.m11 + lm22 * this.m12;
        double nm13 = this.m13;
        double nm20 = lm00 * this.m20 + lm10 * this.m21 + lm20 * this.m22;
        double nm21 = lm01 * this.m20 + lm11 * this.m21 + lm21 * this.m22;
        double nm22 = lm02 * this.m20 + lm12 * this.m21 + lm22 * this.m22;
        double nm23 = this.m23;
        double nm30 = lm00 * this.m30 + lm10 * this.m31 + lm20 * this.m32;
        double nm31 = lm01 * this.m30 + lm11 * this.m31 + lm21 * this.m32;
        double nm32 = lm02 * this.m30 + lm12 * this.m31 + lm22 * this.m32;
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateLocal(Quaterniond quat) {
        return this.rotateLocal(quat, this);
    }

    public Matrix4d rotateAffine(Quaternionf quat, Matrix4d dest) {
        double w2 = quat.w * quat.w;
        double x2 = quat.x * quat.x;
        double y2 = quat.y * quat.y;
        double z2 = quat.z * quat.z;
        double zw = quat.z * quat.w;
        double xy = quat.x * quat.y;
        double xz = quat.x * quat.z;
        double yw = quat.y * quat.w;
        double yz = quat.y * quat.z;
        double xw = quat.x * quat.w;
        double rm00 = w2 + x2 - z2 - y2;
        double rm01 = xy + zw + zw + xy;
        double rm02 = xz - yw + xz - yw;
        double rm10 = -zw + xy - zw + xy;
        double rm11 = y2 - z2 + w2 - x2;
        double rm12 = yz + yz + xw + xw;
        double rm20 = yw + xz + xz + yw;
        double rm21 = yz + yz - xw - xw;
        double rm22 = z2 - y2 - x2 + w2;
        double nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        double nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        double nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(0.0)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0.0)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateAffine(Quaternionf quat) {
        return this.rotateAffine(quat, this);
    }

    public Matrix4d rotateLocal(Quaternionf quat, Matrix4d dest) {
        double w2 = quat.w * quat.w;
        double x2 = quat.x * quat.x;
        double y2 = quat.y * quat.y;
        double z2 = quat.z * quat.z;
        double zw = quat.z * quat.w;
        double xy = quat.x * quat.y;
        double xz = quat.x * quat.z;
        double yw = quat.y * quat.w;
        double yz = quat.y * quat.z;
        double xw = quat.x * quat.w;
        double lm00 = w2 + x2 - z2 - y2;
        double lm01 = xy + zw + zw + xy;
        double lm02 = xz - yw + xz - yw;
        double lm10 = -zw + xy - zw + xy;
        double lm11 = y2 - z2 + w2 - x2;
        double lm12 = yz + yz + xw + xw;
        double lm20 = yw + xz + xz + yw;
        double lm21 = yz + yz - xw - xw;
        double lm22 = z2 - y2 - x2 + w2;
        double nm00 = lm00 * this.m00 + lm10 * this.m01 + lm20 * this.m02;
        double nm01 = lm01 * this.m00 + lm11 * this.m01 + lm21 * this.m02;
        double nm02 = lm02 * this.m00 + lm12 * this.m01 + lm22 * this.m02;
        double nm03 = this.m03;
        double nm10 = lm00 * this.m10 + lm10 * this.m11 + lm20 * this.m12;
        double nm11 = lm01 * this.m10 + lm11 * this.m11 + lm21 * this.m12;
        double nm12 = lm02 * this.m10 + lm12 * this.m11 + lm22 * this.m12;
        double nm13 = this.m13;
        double nm20 = lm00 * this.m20 + lm10 * this.m21 + lm20 * this.m22;
        double nm21 = lm01 * this.m20 + lm11 * this.m21 + lm21 * this.m22;
        double nm22 = lm02 * this.m20 + lm12 * this.m21 + lm22 * this.m22;
        double nm23 = this.m23;
        double nm30 = lm00 * this.m30 + lm10 * this.m31 + lm20 * this.m32;
        double nm31 = lm01 * this.m30 + lm11 * this.m31 + lm21 * this.m32;
        double nm32 = lm02 * this.m30 + lm12 * this.m31 + lm22 * this.m32;
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotateLocal(Quaternionf quat) {
        return this.rotateLocal(quat, this);
    }

    public Matrix4d rotate(AxisAngle4f axisAngle) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Matrix4d rotate(AxisAngle4f axisAngle, Matrix4d dest) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dest);
    }

    public Matrix4d rotate(AxisAngle4d axisAngle) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Matrix4d rotate(AxisAngle4d axisAngle, Matrix4d dest) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dest);
    }

    public Matrix4d rotate(double angle, Vector3d axis) {
        return this.rotate(angle, axis.x, axis.y, axis.z);
    }

    public Matrix4d rotate(double angle, Vector3d axis, Matrix4d dest) {
        return this.rotate(angle, axis.x, axis.y, axis.z, dest);
    }

    public Matrix4d rotate(double angle, Vector3f axis) {
        return this.rotate(angle, axis.x, axis.y, axis.z);
    }

    public Matrix4d rotate(double angle, Vector3f axis, Matrix4d dest) {
        return this.rotate(angle, axis.x, axis.y, axis.z, dest);
    }

    public Vector4d getRow(int row, Vector4d dest) throws IndexOutOfBoundsException {
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
            case 3:
                dest.x = this.m03;
                dest.y = this.m13;
                dest.z = this.m23;
                dest.w = this.m33;
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

        return dest;
    }

    public Vector3d getRow(int row, Vector3d dest) throws IndexOutOfBoundsException {
        switch (row) {
            case 0:
                dest.x = this.m00;
                dest.y = this.m10;
                dest.z = this.m20;
                break;
            case 1:
                dest.x = this.m01;
                dest.y = this.m11;
                dest.z = this.m21;
                break;
            case 2:
                dest.x = this.m02;
                dest.y = this.m12;
                dest.z = this.m22;
                break;
            case 3:
                dest.x = this.m03;
                dest.y = this.m13;
                dest.z = this.m23;
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

        return dest;
    }

    public Matrix4d setRow(int row, Vector4d src) throws IndexOutOfBoundsException {
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

    public Vector4d getColumn(int column, Vector4d dest) throws IndexOutOfBoundsException {
        switch (column) {
            case 0:
                dest.x = this.m00;
                dest.y = this.m01;
                dest.z = this.m02;
                dest.w = this.m03;
                break;
            case 1:
                dest.x = this.m10;
                dest.y = this.m11;
                dest.z = this.m12;
                dest.w = this.m13;
                break;
            case 2:
                dest.x = this.m20;
                dest.y = this.m21;
                dest.z = this.m22;
                dest.w = this.m23;
                break;
            case 3:
                dest.x = this.m30;
                dest.y = this.m31;
                dest.z = this.m32;
                dest.w = this.m33;
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

        return dest;
    }

    public Vector3d getColumn(int column, Vector3d dest) throws IndexOutOfBoundsException {
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

    public Matrix4d setColumn(int column, Vector4d src) throws IndexOutOfBoundsException {
        switch (column) {
            case 0:
                return this._m00(src.x)._m01(src.y)._m02(src.z)._m03(src.w)._properties(0);
            case 1:
                return this._m10(src.x)._m11(src.y)._m12(src.z)._m13(src.w)._properties(0);
            case 2:
                return this._m20(src.x)._m21(src.y)._m22(src.z)._m23(src.w)._properties(0);
            case 3:
                return this._m30(src.x)._m31(src.y)._m32(src.z)._m33(src.w)._properties(0);
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public double get(int column, int row) {
        return MemUtil.INSTANCE.get(this, column, row);
    }

    public Matrix4d set(int column, int row, double value) {
        return MemUtil.INSTANCE.set(this, column, row, value);
    }

    public double getRowColumn(int row, int column) {
        return MemUtil.INSTANCE.get(this, column, row);
    }

    public Matrix4d setRowColumn(int row, int column, double value) {
        return MemUtil.INSTANCE.set(this, column, row, value);
    }

    public Matrix4d normal() {
        return this.normal(this);
    }

    public Matrix4d normal(Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.identity();
        } else {
            return (this.properties & 16) != 0 ? this.normalOrthonormal(dest) : this.normalGeneric(dest);
        }
    }

    private Matrix4d normalOrthonormal(Matrix4d dest) {
        if (dest != this) {
            dest.set(this);
        }

        return dest._properties(18);
    }

    private Matrix4d normalGeneric(Matrix4d dest) {
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
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(0.0)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)._m20(nm20)._m21(nm21)._m22(nm22)._m23(0.0)._m30(0.0)._m31(0.0)._m32(0.0)._m33(1.0)._properties((this.properties | 2) & -10);
    }

    public Matrix3d normal(Matrix3d dest) {
        return (this.properties & 16) != 0 ? this.normalOrthonormal(dest) : this.normalGeneric(dest);
    }

    private Matrix3d normalOrthonormal(Matrix3d dest) {
        dest.set(this);
        return dest;
    }

    private Matrix3d normalGeneric(Matrix3d dest) {
        double m00m11 = this.m00 * this.m11;
        double m01m10 = this.m01 * this.m10;
        double m02m10 = this.m02 * this.m10;
        double m00m12 = this.m00 * this.m12;
        double m01m12 = this.m01 * this.m12;
        double m02m11 = this.m02 * this.m11;
        double det = (m00m11 - m01m10) * this.m22 + (m02m10 - m00m12) * this.m21 + (m01m12 - m02m11) * this.m20;
        double s = 1.0 / det;
        return dest._m00((this.m11 * this.m22 - this.m21 * this.m12) * s)._m01((this.m20 * this.m12 - this.m10 * this.m22) * s)._m02((this.m10 * this.m21 - this.m20 * this.m11) * s)._m10((this.m21 * this.m02 - this.m01 * this.m22) * s)._m11((this.m00 * this.m22 - this.m20 * this.m02) * s)._m12((this.m20 * this.m01 - this.m00 * this.m21) * s)._m20((m01m12 - m02m11) * s)._m21((m02m10 - m00m12) * s)._m22((m00m11 - m01m10) * s);
    }

    public Matrix4d cofactor3x3() {
        return this.cofactor3x3(this);
    }

    public Matrix3d cofactor3x3(Matrix3d dest) {
        return dest._m00(this.m11 * this.m22 - this.m21 * this.m12)._m01(this.m20 * this.m12 - this.m10 * this.m22)._m02(this.m10 * this.m21 - this.m20 * this.m11)._m10(this.m21 * this.m02 - this.m01 * this.m22)._m11(this.m00 * this.m22 - this.m20 * this.m02)._m12(this.m20 * this.m01 - this.m00 * this.m21)._m20(this.m01 * this.m12 - this.m02 * this.m11)._m21(this.m02 * this.m10 - this.m00 * this.m12)._m22(this.m00 * this.m11 - this.m01 * this.m10);
    }

    public Matrix4d cofactor3x3(Matrix4d dest) {
        double nm10 = this.m21 * this.m02 - this.m01 * this.m22;
        double nm11 = this.m00 * this.m22 - this.m20 * this.m02;
        double nm12 = this.m20 * this.m01 - this.m00 * this.m21;
        double nm20 = this.m01 * this.m12 - this.m11 * this.m02;
        double nm21 = this.m02 * this.m10 - this.m12 * this.m00;
        double nm22 = this.m00 * this.m11 - this.m10 * this.m01;
        return dest._m00(this.m11 * this.m22 - this.m21 * this.m12)._m01(this.m20 * this.m12 - this.m10 * this.m22)._m02(this.m10 * this.m21 - this.m20 * this.m11)._m03(0.0)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)._m20(nm20)._m21(nm21)._m22(nm22)._m23(0.0)._m30(0.0)._m31(0.0)._m32(0.0)._m33(1.0)._properties((this.properties | 2) & -10);
    }

    public Matrix4d normalize3x3() {
        return this.normalize3x3(this);
    }

    public Matrix4d normalize3x3(Matrix4d dest) {
        double invXlen = Math.invsqrt(this.m00 * this.m00 + this.m01 * this.m01 + this.m02 * this.m02);
        double invYlen = Math.invsqrt(this.m10 * this.m10 + this.m11 * this.m11 + this.m12 * this.m12);
        double invZlen = Math.invsqrt(this.m20 * this.m20 + this.m21 * this.m21 + this.m22 * this.m22);
        dest._m00(this.m00 * invXlen)._m01(this.m01 * invXlen)._m02(this.m02 * invXlen)._m10(this.m10 * invYlen)._m11(this.m11 * invYlen)._m12(this.m12 * invYlen)._m20(this.m20 * invZlen)._m21(this.m21 * invZlen)._m22(this.m22 * invZlen)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties);
        return dest;
    }

    public Matrix3d normalize3x3(Matrix3d dest) {
        double invXlen = Math.invsqrt(this.m00 * this.m00 + this.m01 * this.m01 + this.m02 * this.m02);
        double invYlen = Math.invsqrt(this.m10 * this.m10 + this.m11 * this.m11 + this.m12 * this.m12);
        double invZlen = Math.invsqrt(this.m20 * this.m20 + this.m21 * this.m21 + this.m22 * this.m22);
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

    public Vector4d unproject(double winX, double winY, double winZ, int[] viewport, Vector4d dest) {
        double a = this.m00 * this.m11 - this.m01 * this.m10;
        double b = this.m00 * this.m12 - this.m02 * this.m10;
        double c = this.m00 * this.m13 - this.m03 * this.m10;
        double d = this.m01 * this.m12 - this.m02 * this.m11;
        double e = this.m01 * this.m13 - this.m03 * this.m11;
        double f = this.m02 * this.m13 - this.m03 * this.m12;
        double g = this.m20 * this.m31 - this.m21 * this.m30;
        double h = this.m20 * this.m32 - this.m22 * this.m30;
        double i = this.m20 * this.m33 - this.m23 * this.m30;
        double j = this.m21 * this.m32 - this.m22 * this.m31;
        double k = this.m21 * this.m33 - this.m23 * this.m31;
        double l = this.m22 * this.m33 - this.m23 * this.m32;
        double det = a * l - b * k + c * j + d * i - e * h + f * g;
        det = 1.0 / det;
        double im00 = (this.m11 * l - this.m12 * k + this.m13 * j) * det;
        double im01 = (-this.m01 * l + this.m02 * k - this.m03 * j) * det;
        double im02 = (this.m31 * f - this.m32 * e + this.m33 * d) * det;
        double im03 = (-this.m21 * f + this.m22 * e - this.m23 * d) * det;
        double im10 = (-this.m10 * l + this.m12 * i - this.m13 * h) * det;
        double im11 = (this.m00 * l - this.m02 * i + this.m03 * h) * det;
        double im12 = (-this.m30 * f + this.m32 * c - this.m33 * b) * det;
        double im13 = (this.m20 * f - this.m22 * c + this.m23 * b) * det;
        double im20 = (this.m10 * k - this.m11 * i + this.m13 * g) * det;
        double im21 = (-this.m00 * k + this.m01 * i - this.m03 * g) * det;
        double im22 = (this.m30 * e - this.m31 * c + this.m33 * a) * det;
        double im23 = (-this.m20 * e + this.m21 * c - this.m23 * a) * det;
        double im30 = (-this.m10 * j + this.m11 * h - this.m12 * g) * det;
        double im31 = (this.m00 * j - this.m01 * h + this.m02 * g) * det;
        double im32 = (-this.m30 * d + this.m31 * b - this.m32 * a) * det;
        double im33 = (this.m20 * d - this.m21 * b + this.m22 * a) * det;
        double ndcX = (winX - viewport[0]) / viewport[2] * 2.0 - 1.0;
        double ndcY = (winY - viewport[1]) / viewport[3] * 2.0 - 1.0;
        double ndcZ = winZ + winZ - 1.0;
        double invW = 1.0 / (im03 * ndcX + im13 * ndcY + im23 * ndcZ + im33);
        dest.x = (im00 * ndcX + im10 * ndcY + im20 * ndcZ + im30) * invW;
        dest.y = (im01 * ndcX + im11 * ndcY + im21 * ndcZ + im31) * invW;
        dest.z = (im02 * ndcX + im12 * ndcY + im22 * ndcZ + im32) * invW;
        dest.w = 1.0;
        return dest;
    }

    public Vector3d unproject(double winX, double winY, double winZ, int[] viewport, Vector3d dest) {
        double a = this.m00 * this.m11 - this.m01 * this.m10;
        double b = this.m00 * this.m12 - this.m02 * this.m10;
        double c = this.m00 * this.m13 - this.m03 * this.m10;
        double d = this.m01 * this.m12 - this.m02 * this.m11;
        double e = this.m01 * this.m13 - this.m03 * this.m11;
        double f = this.m02 * this.m13 - this.m03 * this.m12;
        double g = this.m20 * this.m31 - this.m21 * this.m30;
        double h = this.m20 * this.m32 - this.m22 * this.m30;
        double i = this.m20 * this.m33 - this.m23 * this.m30;
        double j = this.m21 * this.m32 - this.m22 * this.m31;
        double k = this.m21 * this.m33 - this.m23 * this.m31;
        double l = this.m22 * this.m33 - this.m23 * this.m32;
        double det = a * l - b * k + c * j + d * i - e * h + f * g;
        det = 1.0 / det;
        double im00 = (this.m11 * l - this.m12 * k + this.m13 * j) * det;
        double im01 = (-this.m01 * l + this.m02 * k - this.m03 * j) * det;
        double im02 = (this.m31 * f - this.m32 * e + this.m33 * d) * det;
        double im03 = (-this.m21 * f + this.m22 * e - this.m23 * d) * det;
        double im10 = (-this.m10 * l + this.m12 * i - this.m13 * h) * det;
        double im11 = (this.m00 * l - this.m02 * i + this.m03 * h) * det;
        double im12 = (-this.m30 * f + this.m32 * c - this.m33 * b) * det;
        double im13 = (this.m20 * f - this.m22 * c + this.m23 * b) * det;
        double im20 = (this.m10 * k - this.m11 * i + this.m13 * g) * det;
        double im21 = (-this.m00 * k + this.m01 * i - this.m03 * g) * det;
        double im22 = (this.m30 * e - this.m31 * c + this.m33 * a) * det;
        double im23 = (-this.m20 * e + this.m21 * c - this.m23 * a) * det;
        double im30 = (-this.m10 * j + this.m11 * h - this.m12 * g) * det;
        double im31 = (this.m00 * j - this.m01 * h + this.m02 * g) * det;
        double im32 = (-this.m30 * d + this.m31 * b - this.m32 * a) * det;
        double im33 = (this.m20 * d - this.m21 * b + this.m22 * a) * det;
        double ndcX = (winX - viewport[0]) / viewport[2] * 2.0 - 1.0;
        double ndcY = (winY - viewport[1]) / viewport[3] * 2.0 - 1.0;
        double ndcZ = winZ + winZ - 1.0;
        double invW = 1.0 / (im03 * ndcX + im13 * ndcY + im23 * ndcZ + im33);
        dest.x = (im00 * ndcX + im10 * ndcY + im20 * ndcZ + im30) * invW;
        dest.y = (im01 * ndcX + im11 * ndcY + im21 * ndcZ + im31) * invW;
        dest.z = (im02 * ndcX + im12 * ndcY + im22 * ndcZ + im32) * invW;
        return dest;
    }

    public Vector4d unproject(Vector3d winCoords, int[] viewport, Vector4d dest) {
        return this.unproject(winCoords.x, winCoords.y, winCoords.z, viewport, dest);
    }

    public Vector3d unproject(Vector3d winCoords, int[] viewport, Vector3d dest) {
        return this.unproject(winCoords.x, winCoords.y, winCoords.z, viewport, dest);
    }

    public Matrix4d unprojectRay(double winX, double winY, int[] viewport, Vector3d originDest, Vector3d dirDest) {
        double a = this.m00 * this.m11 - this.m01 * this.m10;
        double b = this.m00 * this.m12 - this.m02 * this.m10;
        double c = this.m00 * this.m13 - this.m03 * this.m10;
        double d = this.m01 * this.m12 - this.m02 * this.m11;
        double e = this.m01 * this.m13 - this.m03 * this.m11;
        double f = this.m02 * this.m13 - this.m03 * this.m12;
        double g = this.m20 * this.m31 - this.m21 * this.m30;
        double h = this.m20 * this.m32 - this.m22 * this.m30;
        double i = this.m20 * this.m33 - this.m23 * this.m30;
        double j = this.m21 * this.m32 - this.m22 * this.m31;
        double k = this.m21 * this.m33 - this.m23 * this.m31;
        double l = this.m22 * this.m33 - this.m23 * this.m32;
        double det = a * l - b * k + c * j + d * i - e * h + f * g;
        det = 1.0 / det;
        double im00 = (this.m11 * l - this.m12 * k + this.m13 * j) * det;
        double im01 = (-this.m01 * l + this.m02 * k - this.m03 * j) * det;
        double im02 = (this.m31 * f - this.m32 * e + this.m33 * d) * det;
        double im03 = (-this.m21 * f + this.m22 * e - this.m23 * d) * det;
        double im10 = (-this.m10 * l + this.m12 * i - this.m13 * h) * det;
        double im11 = (this.m00 * l - this.m02 * i + this.m03 * h) * det;
        double im12 = (-this.m30 * f + this.m32 * c - this.m33 * b) * det;
        double im13 = (this.m20 * f - this.m22 * c + this.m23 * b) * det;
        double im20 = (this.m10 * k - this.m11 * i + this.m13 * g) * det;
        double im21 = (-this.m00 * k + this.m01 * i - this.m03 * g) * det;
        double im22 = (this.m30 * e - this.m31 * c + this.m33 * a) * det;
        double im23 = (-this.m20 * e + this.m21 * c - this.m23 * a) * det;
        double im30 = (-this.m10 * j + this.m11 * h - this.m12 * g) * det;
        double im31 = (this.m00 * j - this.m01 * h + this.m02 * g) * det;
        double im32 = (-this.m30 * d + this.m31 * b - this.m32 * a) * det;
        double im33 = (this.m20 * d - this.m21 * b + this.m22 * a) * det;
        double ndcX = (winX - viewport[0]) / viewport[2] * 2.0 - 1.0;
        double ndcY = (winY - viewport[1]) / viewport[3] * 2.0 - 1.0;
        double px = im00 * ndcX + im10 * ndcY + im30;
        double py = im01 * ndcX + im11 * ndcY + im31;
        double pz = im02 * ndcX + im12 * ndcY + im32;
        double invNearW = 1.0 / (im03 * ndcX + im13 * ndcY - im23 + im33);
        double nearX = (px - im20) * invNearW;
        double nearY = (py - im21) * invNearW;
        double nearZ = (pz - im22) * invNearW;
        double invW0 = 1.0 / (im03 * ndcX + im13 * ndcY + im33);
        double x0 = px * invW0;
        double y0 = py * invW0;
        double z0 = pz * invW0;
        originDest.x = nearX;
        originDest.y = nearY;
        originDest.z = nearZ;
        dirDest.x = x0 - nearX;
        dirDest.y = y0 - nearY;
        dirDest.z = z0 - nearZ;
        return this;
    }

    public Matrix4d unprojectRay(Vector2d winCoords, int[] viewport, Vector3d originDest, Vector3d dirDest) {
        return this.unprojectRay(winCoords.x, winCoords.y, viewport, originDest, dirDest);
    }

    public Vector4d unprojectInv(Vector3d winCoords, int[] viewport, Vector4d dest) {
        return this.unprojectInv(winCoords.x, winCoords.y, winCoords.z, viewport, dest);
    }

    public Vector4d unprojectInv(double winX, double winY, double winZ, int[] viewport, Vector4d dest) {
        double ndcX = (winX - viewport[0]) / viewport[2] * 2.0 - 1.0;
        double ndcY = (winY - viewport[1]) / viewport[3] * 2.0 - 1.0;
        double ndcZ = winZ + winZ - 1.0;
        double invW = 1.0 / (this.m03 * ndcX + this.m13 * ndcY + this.m23 * ndcZ + this.m33);
        dest.x = (this.m00 * ndcX + this.m10 * ndcY + this.m20 * ndcZ + this.m30) * invW;
        dest.y = (this.m01 * ndcX + this.m11 * ndcY + this.m21 * ndcZ + this.m31) * invW;
        dest.z = (this.m02 * ndcX + this.m12 * ndcY + this.m22 * ndcZ + this.m32) * invW;
        dest.w = 1.0;
        return dest;
    }

    public Vector3d unprojectInv(Vector3d winCoords, int[] viewport, Vector3d dest) {
        return this.unprojectInv(winCoords.x, winCoords.y, winCoords.z, viewport, dest);
    }

    public Vector3d unprojectInv(double winX, double winY, double winZ, int[] viewport, Vector3d dest) {
        double ndcX = (winX - viewport[0]) / viewport[2] * 2.0 - 1.0;
        double ndcY = (winY - viewport[1]) / viewport[3] * 2.0 - 1.0;
        double ndcZ = winZ + winZ - 1.0;
        double invW = 1.0 / (this.m03 * ndcX + this.m13 * ndcY + this.m23 * ndcZ + this.m33);
        dest.x = (this.m00 * ndcX + this.m10 * ndcY + this.m20 * ndcZ + this.m30) * invW;
        dest.y = (this.m01 * ndcX + this.m11 * ndcY + this.m21 * ndcZ + this.m31) * invW;
        dest.z = (this.m02 * ndcX + this.m12 * ndcY + this.m22 * ndcZ + this.m32) * invW;
        return dest;
    }

    public Matrix4d unprojectInvRay(Vector2d winCoords, int[] viewport, Vector3d originDest, Vector3d dirDest) {
        return this.unprojectInvRay(winCoords.x, winCoords.y, viewport, originDest, dirDest);
    }

    public Matrix4d unprojectInvRay(double winX, double winY, int[] viewport, Vector3d originDest, Vector3d dirDest) {
        double ndcX = (winX - viewport[0]) / viewport[2] * 2.0 - 1.0;
        double ndcY = (winY - viewport[1]) / viewport[3] * 2.0 - 1.0;
        double px = this.m00 * ndcX + this.m10 * ndcY + this.m30;
        double py = this.m01 * ndcX + this.m11 * ndcY + this.m31;
        double pz = this.m02 * ndcX + this.m12 * ndcY + this.m32;
        double invNearW = 1.0 / (this.m03 * ndcX + this.m13 * ndcY - this.m23 + this.m33);
        double nearX = (px - this.m20) * invNearW;
        double nearY = (py - this.m21) * invNearW;
        double nearZ = (pz - this.m22) * invNearW;
        double invW0 = 1.0 / (this.m03 * ndcX + this.m13 * ndcY + this.m33);
        double x0 = px * invW0;
        double y0 = py * invW0;
        double z0 = pz * invW0;
        originDest.x = nearX;
        originDest.y = nearY;
        originDest.z = nearZ;
        dirDest.x = x0 - nearX;
        dirDest.y = y0 - nearY;
        dirDest.z = z0 - nearZ;
        return this;
    }

    public Vector4d project(double x, double y, double z, int[] viewport, Vector4d winCoordsDest) {
        double invW = 1.0 / Math.fma(this.m03, x, Math.fma(this.m13, y, Math.fma(this.m23, z, this.m33)));
        double nx = Math.fma(this.m00, x, Math.fma(this.m10, y, Math.fma(this.m20, z, this.m30))) * invW;
        double ny = Math.fma(this.m01, x, Math.fma(this.m11, y, Math.fma(this.m21, z, this.m31))) * invW;
        double nz = Math.fma(this.m02, x, Math.fma(this.m12, y, Math.fma(this.m22, z, this.m32))) * invW;
        return winCoordsDest.set(Math.fma(Math.fma(nx, 0.5, 0.5), viewport[2], viewport[0]), Math.fma(Math.fma(ny, 0.5, 0.5), viewport[3], viewport[1]), Math.fma(0.5, nz, 0.5), 1.0);
    }

    public Vector3d project(double x, double y, double z, int[] viewport, Vector3d winCoordsDest) {
        double invW = 1.0 / Math.fma(this.m03, x, Math.fma(this.m13, y, Math.fma(this.m23, z, this.m33)));
        double nx = Math.fma(this.m00, x, Math.fma(this.m10, y, Math.fma(this.m20, z, this.m30))) * invW;
        double ny = Math.fma(this.m01, x, Math.fma(this.m11, y, Math.fma(this.m21, z, this.m31))) * invW;
        double nz = Math.fma(this.m02, x, Math.fma(this.m12, y, Math.fma(this.m22, z, this.m32))) * invW;
        winCoordsDest.x = Math.fma(Math.fma(nx, 0.5, 0.5), viewport[2], viewport[0]);
        winCoordsDest.y = Math.fma(Math.fma(ny, 0.5, 0.5), viewport[3], viewport[1]);
        winCoordsDest.z = Math.fma(0.5, nz, 0.5);
        return winCoordsDest;
    }

    public Vector4d project(Vector3d position, int[] viewport, Vector4d dest) {
        return this.project(position.x, position.y, position.z, viewport, dest);
    }

    public Vector3d project(Vector3d position, int[] viewport, Vector3d dest) {
        return this.project(position.x, position.y, position.z, viewport, dest);
    }

    public Matrix4d reflect(double a, double b, double c, double d, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.reflection(a, b, c, d);
        } else {
            return (this.properties & 2) != 0 ? this.reflectAffine(a, b, c, d, dest) : this.reflectGeneric(a, b, c, d, dest);
        }
    }

    private Matrix4d reflectAffine(double a, double b, double c, double d, Matrix4d dest) {
        double da = a + a;
        double db = b + b;
        double dc = c + c;
        double dd = d + d;
        double rm00 = 1.0 - da * a;
        double rm01 = -da * b;
        double rm02 = -da * c;
        double rm10 = -db * a;
        double rm11 = 1.0 - db * b;
        double rm12 = -db * c;
        double rm20 = -dc * a;
        double rm21 = -dc * b;
        double rm22 = 1.0 - dc * c;
        double rm30 = -dd * a;
        double rm31 = -dd * b;
        double rm32 = -dd * c;
        double nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        double nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        double nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32)._m33(this.m33)._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(0.0)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0.0)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)._properties(this.properties & -14);
        return dest;
    }

    private Matrix4d reflectGeneric(double a, double b, double c, double d, Matrix4d dest) {
        double da = a + a;
        double db = b + b;
        double dc = c + c;
        double dd = d + d;
        double rm00 = 1.0 - da * a;
        double rm01 = -da * b;
        double rm02 = -da * c;
        double rm10 = -db * a;
        double rm11 = 1.0 - db * b;
        double rm12 = -db * c;
        double rm20 = -dc * a;
        double rm21 = -dc * b;
        double rm22 = 1.0 - dc * c;
        double rm30 = -dd * a;
        double rm31 = -dd * b;
        double rm32 = -dd * c;
        double nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        double nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        double nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        double nm03 = this.m03 * rm00 + this.m13 * rm01 + this.m23 * rm02;
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        double nm13 = this.m03 * rm10 + this.m13 * rm11 + this.m23 * rm12;
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m23 * rm32 + this.m33)._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m23(this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d reflect(double a, double b, double c, double d) {
        return this.reflect(a, b, c, d, this);
    }

    public Matrix4d reflect(double nx, double ny, double nz, double px, double py, double pz) {
        return this.reflect(nx, ny, nz, px, py, pz, this);
    }

    public Matrix4d reflect(double nx, double ny, double nz, double px, double py, double pz, Matrix4d dest) {
        double invLength = Math.invsqrt(nx * nx + ny * ny + nz * nz);
        double nnx = nx * invLength;
        double nny = ny * invLength;
        double nnz = nz * invLength;
        return this.reflect(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz, dest);
    }

    public Matrix4d reflect(Vector3d normal, Vector3d point) {
        return this.reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z);
    }

    public Matrix4d reflect(Quaterniond orientation, Vector3d point) {
        return this.reflect(orientation, point, this);
    }

    public Matrix4d reflect(Quaterniond orientation, Vector3d point, Matrix4d dest) {
        double num1 = orientation.x + orientation.x;
        double num2 = orientation.y + orientation.y;
        double num3 = orientation.z + orientation.z;
        double normalX = orientation.x * num3 + orientation.w * num2;
        double normalY = orientation.y * num3 - orientation.w * num1;
        double normalZ = 1.0 - (orientation.x * num1 + orientation.y * num2);
        return this.reflect(normalX, normalY, normalZ, point.x, point.y, point.z, dest);
    }

    public Matrix4d reflect(Vector3d normal, Vector3d point, Matrix4d dest) {
        return this.reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z, dest);
    }

    public Matrix4d reflection(double a, double b, double c, double d) {
        double da = a + a;
        double db = b + b;
        double dc = c + c;
        double dd = d + d;
        this._m00(1.0 - da * a)._m01(-da * b)._m02(-da * c)._m03(0.0)._m10(-db * a)._m11(1.0 - db * b)._m12(-db * c)._m13(0.0)._m20(-dc * a)._m21(-dc * b)._m22(1.0 - dc * c)._m23(0.0)._m30(-dd * a)._m31(-dd * b)._m32(-dd * c)._m33(1.0).properties = 18;
        return this;
    }

    public Matrix4d reflection(double nx, double ny, double nz, double px, double py, double pz) {
        double invLength = Math.invsqrt(nx * nx + ny * ny + nz * nz);
        double nnx = nx * invLength;
        double nny = ny * invLength;
        double nnz = nz * invLength;
        return this.reflection(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz);
    }

    public Matrix4d reflection(Vector3d normal, Vector3d point) {
        return this.reflection(normal.x, normal.y, normal.z, point.x, point.y, point.z);
    }

    public Matrix4d reflection(Quaterniond orientation, Vector3d point) {
        double num1 = orientation.x + orientation.x;
        double num2 = orientation.y + orientation.y;
        double num3 = orientation.z + orientation.z;
        double normalX = orientation.x * num3 + orientation.w * num2;
        double normalY = orientation.y * num3 - orientation.w * num1;
        double normalZ = 1.0 - (orientation.x * num1 + orientation.y * num2);
        return this.reflection(normalX, normalY, normalZ, point.x, point.y, point.z);
    }

    public Matrix4d ortho(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.setOrtho(left, right, bottom, top, zNear, zFar, zZeroToOne) : this.orthoGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4d orthoGeneric(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        double rm00 = 2.0 / (right - left);
        double rm11 = 2.0 / (top - bottom);
        double rm22 = (zZeroToOne ? 1.0 : 2.0) / (zNear - zFar);
        double rm30 = (left + right) / (left - right);
        double rm31 = (top + bottom) / (bottom - top);
        double rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m23 * rm32 + this.m33)._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m20(this.m20 * rm22)._m21(this.m21 * rm22)._m22(this.m22 * rm22)._m23(this.m23 * rm22)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4d ortho(double left, double right, double bottom, double top, double zNear, double zFar, Matrix4d dest) {
        return this.ortho(left, right, bottom, top, zNear, zFar, false, dest);
    }

    public Matrix4d ortho(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne) {
        return this.ortho(left, right, bottom, top, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4d ortho(double left, double right, double bottom, double top, double zNear, double zFar) {
        return this.ortho(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4d orthoLH(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.setOrthoLH(left, right, bottom, top, zNear, zFar, zZeroToOne) : this.orthoLHGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4d orthoLHGeneric(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        double rm00 = 2.0 / (right - left);
        double rm11 = 2.0 / (top - bottom);
        double rm22 = (zZeroToOne ? 1.0 : 2.0) / (zFar - zNear);
        double rm30 = (left + right) / (left - right);
        double rm31 = (top + bottom) / (bottom - top);
        double rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m23 * rm32 + this.m33)._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m20(this.m20 * rm22)._m21(this.m21 * rm22)._m22(this.m22 * rm22)._m23(this.m23 * rm22)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4d orthoLH(double left, double right, double bottom, double top, double zNear, double zFar, Matrix4d dest) {
        return this.orthoLH(left, right, bottom, top, zNear, zFar, false, dest);
    }

    public Matrix4d orthoLH(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne) {
        return this.orthoLH(left, right, bottom, top, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4d orthoLH(double left, double right, double bottom, double top, double zNear, double zFar) {
        return this.orthoLH(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4d setOrtho(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne) {
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this._m00(2.0 / (right - left))._m11(2.0 / (top - bottom))._m22((zZeroToOne ? 1.0 : 2.0) / (zNear - zFar))._m30((right + left) / (left - right))._m31((top + bottom) / (bottom - top))._m32((zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar)).properties = 2;
        return this;
    }

    public Matrix4d setOrtho(double left, double right, double bottom, double top, double zNear, double zFar) {
        return this.setOrtho(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4d setOrthoLH(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne) {
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this._m00(2.0 / (right - left))._m11(2.0 / (top - bottom))._m22((zZeroToOne ? 1.0 : 2.0) / (zFar - zNear))._m30((right + left) / (left - right))._m31((top + bottom) / (bottom - top))._m32((zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar)).properties = 2;
        return this;
    }

    public Matrix4d setOrthoLH(double left, double right, double bottom, double top, double zNear, double zFar) {
        return this.setOrthoLH(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4d orthoSymmetric(double width, double height, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.setOrthoSymmetric(width, height, zNear, zFar, zZeroToOne) : this.orthoSymmetricGeneric(width, height, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4d orthoSymmetricGeneric(double width, double height, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        double rm00 = 2.0 / width;
        double rm11 = 2.0 / height;
        double rm22 = (zZeroToOne ? 1.0 : 2.0) / (zNear - zFar);
        double rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        dest._m30(this.m20 * rm32 + this.m30)._m31(this.m21 * rm32 + this.m31)._m32(this.m22 * rm32 + this.m32)._m33(this.m23 * rm32 + this.m33)._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m20(this.m20 * rm22)._m21(this.m21 * rm22)._m22(this.m22 * rm22)._m23(this.m23 * rm22)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4d orthoSymmetric(double width, double height, double zNear, double zFar, Matrix4d dest) {
        return this.orthoSymmetric(width, height, zNear, zFar, false, dest);
    }

    public Matrix4d orthoSymmetric(double width, double height, double zNear, double zFar, boolean zZeroToOne) {
        return this.orthoSymmetric(width, height, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4d orthoSymmetric(double width, double height, double zNear, double zFar) {
        return this.orthoSymmetric(width, height, zNear, zFar, false, this);
    }

    public Matrix4d orthoSymmetricLH(double width, double height, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.setOrthoSymmetricLH(width, height, zNear, zFar, zZeroToOne) : this.orthoSymmetricLHGeneric(width, height, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4d orthoSymmetricLHGeneric(double width, double height, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        double rm00 = 2.0 / width;
        double rm11 = 2.0 / height;
        double rm22 = (zZeroToOne ? 1.0 : 2.0) / (zFar - zNear);
        double rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        dest._m30(this.m20 * rm32 + this.m30)._m31(this.m21 * rm32 + this.m31)._m32(this.m22 * rm32 + this.m32)._m33(this.m23 * rm32 + this.m33)._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m20(this.m20 * rm22)._m21(this.m21 * rm22)._m22(this.m22 * rm22)._m23(this.m23 * rm22)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4d orthoSymmetricLH(double width, double height, double zNear, double zFar, Matrix4d dest) {
        return this.orthoSymmetricLH(width, height, zNear, zFar, false, dest);
    }

    public Matrix4d orthoSymmetricLH(double width, double height, double zNear, double zFar, boolean zZeroToOne) {
        return this.orthoSymmetricLH(width, height, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4d orthoSymmetricLH(double width, double height, double zNear, double zFar) {
        return this.orthoSymmetricLH(width, height, zNear, zFar, false, this);
    }

    public Matrix4d setOrthoSymmetric(double width, double height, double zNear, double zFar, boolean zZeroToOne) {
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this._m00(2.0 / width)._m11(2.0 / height)._m22((zZeroToOne ? 1.0 : 2.0) / (zNear - zFar))._m32((zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar)).properties = 2;
        return this;
    }

    public Matrix4d setOrthoSymmetric(double width, double height, double zNear, double zFar) {
        return this.setOrthoSymmetric(width, height, zNear, zFar, false);
    }

    public Matrix4d setOrthoSymmetricLH(double width, double height, double zNear, double zFar, boolean zZeroToOne) {
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this._m00(2.0 / width)._m11(2.0 / height)._m22((zZeroToOne ? 1.0 : 2.0) / (zFar - zNear))._m32((zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar)).properties = 2;
        return this;
    }

    public Matrix4d setOrthoSymmetricLH(double width, double height, double zNear, double zFar) {
        return this.setOrthoSymmetricLH(width, height, zNear, zFar, false);
    }

    public Matrix4d ortho2D(double left, double right, double bottom, double top, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.setOrtho2D(left, right, bottom, top) : this.ortho2DGeneric(left, right, bottom, top, dest);
    }

    private Matrix4d ortho2DGeneric(double left, double right, double bottom, double top, Matrix4d dest) {
        double rm00 = 2.0 / (right - left);
        double rm11 = 2.0 / (top - bottom);
        double rm30 = (right + left) / (left - right);
        double rm31 = (top + bottom) / (bottom - top);
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m32)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m33)._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(-this.m23)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4d ortho2D(double left, double right, double bottom, double top) {
        return this.ortho2D(left, right, bottom, top, this);
    }

    public Matrix4d ortho2DLH(double left, double right, double bottom, double top, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.setOrtho2DLH(left, right, bottom, top) : this.ortho2DLHGeneric(left, right, bottom, top, dest);
    }

    private Matrix4d ortho2DLHGeneric(double left, double right, double bottom, double top, Matrix4d dest) {
        double rm00 = 2.0 / (right - left);
        double rm11 = 2.0 / (top - bottom);
        double rm30 = (right + left) / (left - right);
        double rm31 = (top + bottom) / (bottom - top);
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m32)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m33)._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4d ortho2DLH(double left, double right, double bottom, double top) {
        return this.ortho2DLH(left, right, bottom, top, this);
    }

    public Matrix4d setOrtho2D(double left, double right, double bottom, double top) {
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this._m00(2.0 / (right - left))._m11(2.0 / (top - bottom))._m22(-1.0)._m30((right + left) / (left - right))._m31((top + bottom) / (bottom - top)).properties = 2;
        return this;
    }

    public Matrix4d setOrtho2DLH(double left, double right, double bottom, double top) {
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this._m00(2.0 / (right - left))._m11(2.0 / (top - bottom))._m30((right + left) / (left - right))._m31((top + bottom) / (bottom - top)).properties = 2;
        return this;
    }

    public Matrix4d lookAlong(Vector3d dir, Vector3d up) {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this);
    }

    public Matrix4d lookAlong(Vector3d dir, Vector3d up, Matrix4d dest) {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dest);
    }

    public Matrix4d lookAlong(double dirX, double dirY, double dirZ, double upX, double upY, double upZ, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.setLookAlong(dirX, dirY, dirZ, upX, upY, upZ) : this.lookAlongGeneric(dirX, dirY, dirZ, upX, upY, upZ, dest);
    }

    private Matrix4d lookAlongGeneric(double dirX, double dirY, double dirZ, double upX, double upY, double upZ, Matrix4d dest) {
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
        double nm03 = this.m03 * leftX + this.m13 * upnX + this.m23 * dirX;
        double nm10 = this.m00 * leftY + this.m10 * upnY + this.m20 * dirY;
        double nm11 = this.m01 * leftY + this.m11 * upnY + this.m21 * dirY;
        double nm12 = this.m02 * leftY + this.m12 * upnY + this.m22 * dirY;
        double nm13 = this.m03 * leftY + this.m13 * upnY + this.m23 * dirY;
        dest._m20(this.m00 * leftZ + this.m10 * upnZ + this.m20 * dirZ)._m21(this.m01 * leftZ + this.m11 * upnZ + this.m21 * dirZ)._m22(this.m02 * leftZ + this.m12 * upnZ + this.m22 * dirZ)._m23(this.m03 * leftZ + this.m13 * upnZ + this.m23 * dirZ)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d lookAlong(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
        return this.lookAlong(dirX, dirY, dirZ, upX, upY, upZ, this);
    }

    public Matrix4d setLookAlong(Vector3d dir, Vector3d up) {
        return this.setLookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    public Matrix4d setLookAlong(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
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
        this._m00(leftX)._m01(upnX)._m02(dirX)._m03(0.0)._m10(leftY)._m11(upnY)._m12(dirY)._m13(0.0)._m20(leftZ)._m21(upnZ)._m22(dirZ)._m23(0.0)._m30(0.0)._m31(0.0)._m32(0.0)._m33(1.0).properties = 18;
        return this;
    }

    public Matrix4d setLookAt(Vector3d eye, Vector3d center, Vector3d up) {
        return this.setLookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z);
    }

    public Matrix4d setLookAt(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) {
        double dirX = eyeX - centerX;
        double dirY = eyeY - centerY;
        double dirZ = eyeZ - centerZ;
        double invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= invDirLength;
        dirY *= invDirLength;
        dirZ *= invDirLength;
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
        return this._m00(leftX)._m01(upnX)._m02(dirX)._m03(0.0)._m10(leftY)._m11(upnY)._m12(dirY)._m13(0.0)._m20(leftZ)._m21(upnZ)._m22(dirZ)._m23(0.0)._m30(-(leftX * eyeX + leftY * eyeY + leftZ * eyeZ))._m31(-(upnX * eyeX + upnY * eyeY + upnZ * eyeZ))._m32(-(dirX * eyeX + dirY * eyeY + dirZ * eyeZ))._m33(1.0)._properties(18);
    }

    public Matrix4d lookAt(Vector3d eye, Vector3d center, Vector3d up, Matrix4d dest) {
        return this.lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dest);
    }

    public Matrix4d lookAt(Vector3d eye, Vector3d center, Vector3d up) {
        return this.lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, this);
    }

    public Matrix4d lookAt(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.setLookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        } else {
            return (this.properties & 1) != 0 ? this.lookAtPerspective(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dest) : this.lookAtGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dest);
        }
    }

    private Matrix4d lookAtGeneric(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ, Matrix4d dest) {
        double dirX = eyeX - centerX;
        double dirY = eyeY - centerY;
        double dirZ = eyeZ - centerZ;
        double invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= invDirLength;
        dirY *= invDirLength;
        dirZ *= invDirLength;
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
        double rm30 = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ);
        double rm31 = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ);
        double rm32 = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ);
        double nm00 = this.m00 * leftX + this.m10 * upnX + this.m20 * dirX;
        double nm01 = this.m01 * leftX + this.m11 * upnX + this.m21 * dirX;
        double nm02 = this.m02 * leftX + this.m12 * upnX + this.m22 * dirX;
        double nm03 = this.m03 * leftX + this.m13 * upnX + this.m23 * dirX;
        double nm10 = this.m00 * leftY + this.m10 * upnY + this.m20 * dirY;
        double nm11 = this.m01 * leftY + this.m11 * upnY + this.m21 * dirY;
        double nm12 = this.m02 * leftY + this.m12 * upnY + this.m22 * dirY;
        double nm13 = this.m03 * leftY + this.m13 * upnY + this.m23 * dirY;
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m23 * rm32 + this.m33)._m20(this.m00 * leftZ + this.m10 * upnZ + this.m20 * dirZ)._m21(this.m01 * leftZ + this.m11 * upnZ + this.m21 * dirZ)._m22(this.m02 * leftZ + this.m12 * upnZ + this.m22 * dirZ)._m23(this.m03 * leftZ + this.m13 * upnZ + this.m23 * dirZ)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d lookAt(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) {
        return this.lookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, this);
    }

    public Matrix4d lookAtPerspective(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ, Matrix4d dest) {
        double dirX = eyeX - centerX;
        double dirY = eyeY - centerY;
        double dirZ = eyeZ - centerZ;
        double invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= invDirLength;
        dirY *= invDirLength;
        dirZ *= invDirLength;
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
        double rm30 = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ);
        double rm31 = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ);
        double rm32 = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ);
        double nm10 = this.m00 * leftY;
        double nm20 = this.m00 * leftZ;
        double nm21 = this.m11 * upnZ;
        double nm30 = this.m00 * rm30;
        double nm31 = this.m11 * rm31;
        double nm32 = this.m22 * rm32 + this.m32;
        double nm33 = this.m23 * rm32;
        return dest._m00(this.m00 * leftX)._m01(this.m11 * upnX)._m02(this.m22 * dirX)._m03(this.m23 * dirX)._m10(nm10)._m11(this.m11 * upnY)._m12(this.m22 * dirY)._m13(this.m23 * dirY)._m20(nm20)._m21(nm21)._m22(this.m22 * dirZ)._m23(this.m23 * dirZ)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
    }

    public Matrix4d setLookAtLH(Vector3d eye, Vector3d center, Vector3d up) {
        return this.setLookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z);
    }

    public Matrix4d setLookAtLH(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) {
        double dirX = centerX - eyeX;
        double dirY = centerY - eyeY;
        double dirZ = centerZ - eyeZ;
        double invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= invDirLength;
        dirY *= invDirLength;
        dirZ *= invDirLength;
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
        this._m00(leftX)._m01(upnX)._m02(dirX)._m03(0.0)._m10(leftY)._m11(upnY)._m12(dirY)._m13(0.0)._m20(leftZ)._m21(upnZ)._m22(dirZ)._m23(0.0)._m30(-(leftX * eyeX + leftY * eyeY + leftZ * eyeZ))._m31(-(upnX * eyeX + upnY * eyeY + upnZ * eyeZ))._m32(-(dirX * eyeX + dirY * eyeY + dirZ * eyeZ))._m33(1.0).properties = 18;
        return this;
    }

    public Matrix4d lookAtLH(Vector3d eye, Vector3d center, Vector3d up, Matrix4d dest) {
        return this.lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dest);
    }

    public Matrix4d lookAtLH(Vector3d eye, Vector3d center, Vector3d up) {
        return this.lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, this);
    }

    public Matrix4d lookAtLH(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ, Matrix4d dest) {
        if ((this.properties & 4) != 0) {
            return dest.setLookAtLH(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        } else {
            return (this.properties & 1) != 0 ? this.lookAtPerspectiveLH(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dest) : this.lookAtLHGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dest);
        }
    }

    private Matrix4d lookAtLHGeneric(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ, Matrix4d dest) {
        double dirX = centerX - eyeX;
        double dirY = centerY - eyeY;
        double dirZ = centerZ - eyeZ;
        double invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= invDirLength;
        dirY *= invDirLength;
        dirZ *= invDirLength;
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
        double rm30 = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ);
        double rm31 = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ);
        double rm32 = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ);
        double nm00 = this.m00 * leftX + this.m10 * upnX + this.m20 * dirX;
        double nm01 = this.m01 * leftX + this.m11 * upnX + this.m21 * dirX;
        double nm02 = this.m02 * leftX + this.m12 * upnX + this.m22 * dirX;
        double nm03 = this.m03 * leftX + this.m13 * upnX + this.m23 * dirX;
        double nm10 = this.m00 * leftY + this.m10 * upnY + this.m20 * dirY;
        double nm11 = this.m01 * leftY + this.m11 * upnY + this.m21 * dirY;
        double nm12 = this.m02 * leftY + this.m12 * upnY + this.m22 * dirY;
        double nm13 = this.m03 * leftY + this.m13 * upnY + this.m23 * dirY;
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m23 * rm32 + this.m33)._m20(this.m00 * leftZ + this.m10 * upnZ + this.m20 * dirZ)._m21(this.m01 * leftZ + this.m11 * upnZ + this.m21 * dirZ)._m22(this.m02 * leftZ + this.m12 * upnZ + this.m22 * dirZ)._m23(this.m03 * leftZ + this.m13 * upnZ + this.m23 * dirZ)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d lookAtLH(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) {
        return this.lookAtLH(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, this);
    }

    public Matrix4d lookAtPerspectiveLH(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ, Matrix4d dest) {
        double dirX = centerX - eyeX;
        double dirY = centerY - eyeY;
        double dirZ = centerZ - eyeZ;
        double invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= invDirLength;
        dirY *= invDirLength;
        dirZ *= invDirLength;
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
        double rm30 = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ);
        double rm31 = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ);
        double rm32 = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ);
        double nm00 = this.m00 * leftX;
        double nm01 = this.m11 * upnX;
        double nm02 = this.m22 * dirX;
        double nm03 = this.m23 * dirX;
        double nm10 = this.m00 * leftY;
        double nm11 = this.m11 * upnY;
        double nm12 = this.m22 * dirY;
        double nm13 = this.m23 * dirY;
        double nm20 = this.m00 * leftZ;
        double nm21 = this.m11 * upnZ;
        double nm22 = this.m22 * dirZ;
        double nm23 = this.m23 * dirZ;
        double nm30 = this.m00 * rm30;
        double nm31 = this.m11 * rm31;
        double nm32 = this.m22 * rm32 + this.m32;
        double nm33 = this.m23 * rm32;
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0);
        return dest;
    }

    public Matrix4d tile(int x, int y, int w, int h) {
        return this.tile(x, y, w, h, this);
    }

    public Matrix4d tile(int x, int y, int w, int h, Matrix4d dest) {
        float tx = (float)(w - 1 - (x << 1));
        float ty = (float)(h - 1 - (y << 1));
        return dest._m30(Math.fma(this.m00, tx, Math.fma(this.m10, ty, this.m30)))._m31(Math.fma(this.m01, tx, Math.fma(this.m11, ty, this.m31)))._m32(Math.fma(this.m02, tx, Math.fma(this.m12, ty, this.m32)))._m33(Math.fma(this.m03, tx, Math.fma(this.m13, ty, this.m33)))._m00(this.m00 * w)._m01(this.m01 * w)._m02(this.m02 * w)._m03(this.m03 * w)._m10(this.m10 * h)._m11(this.m11 * h)._m12(this.m12 * h)._m13(this.m13 * h)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._properties(this.properties & -30);
    }

    public Matrix4d perspective(double fovy, double aspect, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.setPerspective(fovy, aspect, zNear, zFar, zZeroToOne) : this.perspectiveGeneric(fovy, aspect, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4d perspectiveGeneric(double fovy, double aspect, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        double h = Math.tan(fovy * 0.5);
        double rm00 = 1.0 / (h * aspect);
        double rm11 = 1.0 / h;
        boolean farInf = zFar > 0.0 && Double.isInfinite(zFar);
        boolean nearInf = zNear > 0.0 && Double.isInfinite(zNear);
        double rm22;
        double rm32;
        double e;
        if (farInf) {
            e = 1.0E-6;
            rm22 = e - 1.0;
            rm32 = (e - (zZeroToOne ? 1.0 : 2.0)) * zNear;
        } else if (nearInf) {
            e = 1.0E-6;
            rm22 = (zZeroToOne ? 0.0 : 1.0) - e;
            rm32 = ((zZeroToOne ? 1.0 : 2.0) - e) * zFar;
        } else {
            rm22 = (zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar);
            rm32 = (zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar);
        }

        e = this.m20 * rm22 - this.m30;
        double nm21 = this.m21 * rm22 - this.m31;
        double nm22 = this.m22 * rm22 - this.m32;
        double nm23 = this.m23 * rm22 - this.m33;
        dest._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m30(this.m20 * rm32)._m31(this.m21 * rm32)._m32(this.m22 * rm32)._m33(this.m23 * rm32)._m20(e)._m21(nm21)._m22(nm22)._m23(nm23)._properties(this.properties & -31);
        return dest;
    }

    public Matrix4d perspective(double fovy, double aspect, double zNear, double zFar, Matrix4d dest) {
        return this.perspective(fovy, aspect, zNear, zFar, false, dest);
    }

    public Matrix4d perspective(double fovy, double aspect, double zNear, double zFar, boolean zZeroToOne) {
        return this.perspective(fovy, aspect, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4d perspective(double fovy, double aspect, double zNear, double zFar) {
        return this.perspective(fovy, aspect, zNear, zFar, this);
    }

    public Matrix4d perspectiveRect(double width, double height, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.setPerspectiveRect(width, height, zNear, zFar, zZeroToOne) : this.perspectiveRectGeneric(width, height, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4d perspectiveRectGeneric(double width, double height, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        double rm00 = (zNear + zNear) / width;
        double rm11 = (zNear + zNear) / height;
        boolean farInf = zFar > 0.0 && Double.isInfinite(zFar);
        boolean nearInf = zNear > 0.0 && Double.isInfinite(zNear);
        double rm22;
        double rm32;
        double e;
        if (farInf) {
            e = 9.999999974752427E-7;
            rm22 = e - 1.0;
            rm32 = (e - (zZeroToOne ? 1.0 : 2.0)) * zNear;
        } else if (nearInf) {
            e = 9.999999974752427E-7;
            rm22 = (zZeroToOne ? 0.0 : 1.0) - e;
            rm32 = ((zZeroToOne ? 1.0 : 2.0) - e) * zFar;
        } else {
            rm22 = (zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar);
            rm32 = (zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar);
        }

        e = this.m20 * rm22 - this.m30;
        double nm21 = this.m21 * rm22 - this.m31;
        double nm22 = this.m22 * rm22 - this.m32;
        double nm23 = this.m23 * rm22 - this.m33;
        dest._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m30(this.m20 * rm32)._m31(this.m21 * rm32)._m32(this.m22 * rm32)._m33(this.m23 * rm32)._m20(e)._m21(nm21)._m22(nm22)._m23(nm23)._properties(this.properties & -31);
        return dest;
    }

    public Matrix4d perspectiveRect(double width, double height, double zNear, double zFar, Matrix4d dest) {
        return this.perspectiveRect(width, height, zNear, zFar, false, dest);
    }

    public Matrix4d perspectiveRect(double width, double height, double zNear, double zFar, boolean zZeroToOne) {
        return this.perspectiveRect(width, height, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4d perspectiveRect(double width, double height, double zNear, double zFar) {
        return this.perspectiveRect(width, height, zNear, zFar, this);
    }

    public Matrix4d perspectiveOffCenter(double fovy, double offAngleX, double offAngleY, double aspect, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.setPerspectiveOffCenter(fovy, offAngleX, offAngleY, aspect, zNear, zFar, zZeroToOne) : this.perspectiveOffCenterGeneric(fovy, offAngleX, offAngleY, aspect, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4d perspectiveOffCenterGeneric(double fovy, double offAngleX, double offAngleY, double aspect, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        double h = Math.tan(fovy * 0.5);
        double xScale = 1.0 / (h * aspect);
        double yScale = 1.0 / h;
        double offX = Math.tan(offAngleX);
        double offY = Math.tan(offAngleY);
        double rm20 = offX * xScale;
        double rm21 = offY * yScale;
        boolean farInf = zFar > 0.0 && Double.isInfinite(zFar);
        boolean nearInf = zNear > 0.0 && Double.isInfinite(zNear);
        double rm22;
        double rm32;
        double e;
        if (farInf) {
            e = 1.0E-6;
            rm22 = e - 1.0;
            rm32 = (e - (zZeroToOne ? 1.0 : 2.0)) * zNear;
        } else if (nearInf) {
            e = 1.0E-6;
            rm22 = (zZeroToOne ? 0.0 : 1.0) - e;
            rm32 = ((zZeroToOne ? 1.0 : 2.0) - e) * zFar;
        } else {
            rm22 = (zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar);
            rm32 = (zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar);
        }

        e = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22 - this.m30;
        double nm21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22 - this.m31;
        double nm22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22 - this.m32;
        double nm23 = this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22 - this.m33;
        dest._m00(this.m00 * xScale)._m01(this.m01 * xScale)._m02(this.m02 * xScale)._m03(this.m03 * xScale)._m10(this.m10 * yScale)._m11(this.m11 * yScale)._m12(this.m12 * yScale)._m13(this.m13 * yScale)._m30(this.m20 * rm32)._m31(this.m21 * rm32)._m32(this.m22 * rm32)._m33(this.m23 * rm32)._m20(e)._m21(nm21)._m22(nm22)._m23(nm23)._properties(this.properties & ~(30 | (rm20 == 0.0 && rm21 == 0.0 ? 0 : 1)));
        return dest;
    }

    public Matrix4d perspectiveOffCenter(double fovy, double offAngleX, double offAngleY, double aspect, double zNear, double zFar, Matrix4d dest) {
        return this.perspectiveOffCenter(fovy, offAngleX, offAngleY, aspect, zNear, zFar, false, dest);
    }

    public Matrix4d perspectiveOffCenter(double fovy, double offAngleX, double offAngleY, double aspect, double zNear, double zFar, boolean zZeroToOne) {
        return this.perspectiveOffCenter(fovy, offAngleX, offAngleY, aspect, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4d perspectiveOffCenter(double fovy, double offAngleX, double offAngleY, double aspect, double zNear, double zFar) {
        return this.perspectiveOffCenter(fovy, offAngleX, offAngleY, aspect, zNear, zFar, this);
    }

    public Matrix4d perspectiveOffCenterFov(double angleLeft, double angleRight, double angleDown, double angleUp, double zNear, double zFar, boolean zZeroToOne) {
        return this.perspectiveOffCenterFov(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4d perspectiveOffCenterFov(double angleLeft, double angleRight, double angleDown, double angleUp, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        return this.frustum(Math.tan(angleLeft) * zNear, Math.tan(angleRight) * zNear, Math.tan(angleDown) * zNear, Math.tan(angleUp) * zNear, zNear, zFar, zZeroToOne, dest);
    }

    public Matrix4d perspectiveOffCenterFov(double angleLeft, double angleRight, double angleDown, double angleUp, double zNear, double zFar) {
        return this.perspectiveOffCenterFov(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, this);
    }

    public Matrix4d perspectiveOffCenterFov(double angleLeft, double angleRight, double angleDown, double angleUp, double zNear, double zFar, Matrix4d dest) {
        return this.frustum(Math.tan(angleLeft) * zNear, Math.tan(angleRight) * zNear, Math.tan(angleDown) * zNear, Math.tan(angleUp) * zNear, zNear, zFar, dest);
    }

    public Matrix4d perspectiveOffCenterFovLH(double angleLeft, double angleRight, double angleDown, double angleUp, double zNear, double zFar, boolean zZeroToOne) {
        return this.perspectiveOffCenterFovLH(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4d perspectiveOffCenterFovLH(double angleLeft, double angleRight, double angleDown, double angleUp, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        return this.frustumLH(Math.tan(angleLeft) * zNear, Math.tan(angleRight) * zNear, Math.tan(angleDown) * zNear, Math.tan(angleUp) * zNear, zNear, zFar, zZeroToOne, dest);
    }

    public Matrix4d perspectiveOffCenterFovLH(double angleLeft, double angleRight, double angleDown, double angleUp, double zNear, double zFar) {
        return this.perspectiveOffCenterFovLH(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, this);
    }

    public Matrix4d perspectiveOffCenterFovLH(double angleLeft, double angleRight, double angleDown, double angleUp, double zNear, double zFar, Matrix4d dest) {
        return this.frustumLH(Math.tan(angleLeft) * zNear, Math.tan(angleRight) * zNear, Math.tan(angleDown) * zNear, Math.tan(angleUp) * zNear, zNear, zFar, dest);
    }

    public Matrix4d setPerspective(double fovy, double aspect, double zNear, double zFar, boolean zZeroToOne) {
        double h = Math.tan(fovy * 0.5);
        this._m00(1.0 / (h * aspect))._m01(0.0)._m02(0.0)._m03(0.0)._m10(0.0)._m11(1.0 / h)._m12(0.0)._m13(0.0)._m20(0.0)._m21(0.0);
        boolean farInf = zFar > 0.0 && Double.isInfinite(zFar);
        boolean nearInf = zNear > 0.0 && Double.isInfinite(zNear);
        double e;
        if (farInf) {
            e = 1.0E-6;
            this._m22(e - 1.0)._m32((e - (zZeroToOne ? 1.0 : 2.0)) * zNear);
        } else if (nearInf) {
            e = 1.0E-6;
            this._m22((zZeroToOne ? 0.0 : 1.0) - e)._m32(((zZeroToOne ? 1.0 : 2.0) - e) * zFar);
        } else {
            this._m22((zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar))._m32((zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar));
        }

        this._m23(-1.0)._m30(0.0)._m31(0.0)._m33(0.0).properties = 1;
        return this;
    }

    public Matrix4d setPerspective(double fovy, double aspect, double zNear, double zFar) {
        return this.setPerspective(fovy, aspect, zNear, zFar, false);
    }

    public Matrix4d setPerspectiveRect(double width, double height, double zNear, double zFar, boolean zZeroToOne) {
        this.zero();
        this._m00((zNear + zNear) / width);
        this._m11((zNear + zNear) / height);
        boolean farInf = zFar > 0.0 && Double.isInfinite(zFar);
        boolean nearInf = zNear > 0.0 && Double.isInfinite(zNear);
        double e;
        if (farInf) {
            e = 1.0E-6;
            this._m22(e - 1.0);
            this._m32((e - (zZeroToOne ? 1.0 : 2.0)) * zNear);
        } else if (nearInf) {
            e = 9.999999974752427E-7;
            this._m22((zZeroToOne ? 0.0 : 1.0) - e);
            this._m32(((zZeroToOne ? 1.0 : 2.0) - e) * zFar);
        } else {
            this._m22((zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar));
            this._m32((zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar));
        }

        this._m23(-1.0);
        this.properties = 1;
        return this;
    }

    public Matrix4d setPerspectiveRect(double width, double height, double zNear, double zFar) {
        return this.setPerspectiveRect(width, height, zNear, zFar, false);
    }

    public Matrix4d setPerspectiveOffCenter(double fovy, double offAngleX, double offAngleY, double aspect, double zNear, double zFar) {
        return this.setPerspectiveOffCenter(fovy, offAngleX, offAngleY, aspect, zNear, zFar, false);
    }

    public Matrix4d setPerspectiveOffCenter(double fovy, double offAngleX, double offAngleY, double aspect, double zNear, double zFar, boolean zZeroToOne) {
        this.zero();
        double h = Math.tan(fovy * 0.5);
        double xScale = 1.0 / (h * aspect);
        double yScale = 1.0 / h;
        this._m00(xScale)._m11(yScale);
        double offX = Math.tan(offAngleX);
        double offY = Math.tan(offAngleY);
        this._m20(offX * xScale)._m21(offY * yScale);
        boolean farInf = zFar > 0.0 && Double.isInfinite(zFar);
        boolean nearInf = zNear > 0.0 && Double.isInfinite(zNear);
        double e;
        if (farInf) {
            e = 1.0E-6;
            this._m22(e - 1.0)._m32((e - (zZeroToOne ? 1.0 : 2.0)) * zNear);
        } else if (nearInf) {
            e = 1.0E-6;
            this._m22((zZeroToOne ? 0.0 : 1.0) - e)._m32(((zZeroToOne ? 1.0 : 2.0) - e) * zFar);
        } else {
            this._m22((zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar))._m32((zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar));
        }

        this._m23(-1.0)._m30(0.0)._m31(0.0)._m33(0.0).properties = offAngleX == 0.0 && offAngleY == 0.0 ? 1 : 0;
        return this;
    }

    public Matrix4d setPerspectiveOffCenterFov(double angleLeft, double angleRight, double angleDown, double angleUp, double zNear, double zFar) {
        return this.setPerspectiveOffCenterFov(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, false);
    }

    public Matrix4d setPerspectiveOffCenterFov(double angleLeft, double angleRight, double angleDown, double angleUp, double zNear, double zFar, boolean zZeroToOne) {
        return this.setFrustum(Math.tan(angleLeft) * zNear, Math.tan(angleRight) * zNear, Math.tan(angleDown) * zNear, Math.tan(angleUp) * zNear, zNear, zFar, zZeroToOne);
    }

    public Matrix4d setPerspectiveOffCenterFovLH(double angleLeft, double angleRight, double angleDown, double angleUp, double zNear, double zFar) {
        return this.setPerspectiveOffCenterFovLH(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, false);
    }

    public Matrix4d setPerspectiveOffCenterFovLH(double angleLeft, double angleRight, double angleDown, double angleUp, double zNear, double zFar, boolean zZeroToOne) {
        return this.setFrustumLH(Math.tan(angleLeft) * zNear, Math.tan(angleRight) * zNear, Math.tan(angleDown) * zNear, Math.tan(angleUp) * zNear, zNear, zFar, zZeroToOne);
    }

    public Matrix4d perspectiveLH(double fovy, double aspect, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.setPerspectiveLH(fovy, aspect, zNear, zFar, zZeroToOne) : this.perspectiveLHGeneric(fovy, aspect, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4d perspectiveLHGeneric(double fovy, double aspect, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        double h = Math.tan(fovy * 0.5);
        double rm00 = 1.0 / (h * aspect);
        double rm11 = 1.0 / h;
        boolean farInf = zFar > 0.0 && Double.isInfinite(zFar);
        boolean nearInf = zNear > 0.0 && Double.isInfinite(zNear);
        double rm22;
        double rm32;
        double e;
        if (farInf) {
            e = 1.0E-6;
            rm22 = 1.0 - e;
            rm32 = (e - (zZeroToOne ? 1.0 : 2.0)) * zNear;
        } else if (nearInf) {
            e = 1.0E-6;
            rm22 = (zZeroToOne ? 0.0 : 1.0) - e;
            rm32 = ((zZeroToOne ? 1.0 : 2.0) - e) * zFar;
        } else {
            rm22 = (zZeroToOne ? zFar : zFar + zNear) / (zFar - zNear);
            rm32 = (zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar);
        }

        e = this.m20 * rm22 + this.m30;
        double nm21 = this.m21 * rm22 + this.m31;
        double nm22 = this.m22 * rm22 + this.m32;
        double nm23 = this.m23 * rm22 + this.m33;
        dest._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m30(this.m20 * rm32)._m31(this.m21 * rm32)._m32(this.m22 * rm32)._m33(this.m23 * rm32)._m20(e)._m21(nm21)._m22(nm22)._m23(nm23)._properties(this.properties & -31);
        return dest;
    }

    public Matrix4d perspectiveLH(double fovy, double aspect, double zNear, double zFar, boolean zZeroToOne) {
        return this.perspectiveLH(fovy, aspect, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4d perspectiveLH(double fovy, double aspect, double zNear, double zFar, Matrix4d dest) {
        return this.perspectiveLH(fovy, aspect, zNear, zFar, false, dest);
    }

    public Matrix4d perspectiveLH(double fovy, double aspect, double zNear, double zFar) {
        return this.perspectiveLH(fovy, aspect, zNear, zFar, this);
    }

    public Matrix4d setPerspectiveLH(double fovy, double aspect, double zNear, double zFar, boolean zZeroToOne) {
        double h = Math.tan(fovy * 0.5);
        this._m00(1.0 / (h * aspect))._m01(0.0)._m02(0.0)._m03(0.0)._m10(0.0)._m11(1.0 / h)._m12(0.0)._m13(0.0)._m20(0.0)._m21(0.0);
        boolean farInf = zFar > 0.0 && Double.isInfinite(zFar);
        boolean nearInf = zNear > 0.0 && Double.isInfinite(zNear);
        double e;
        if (farInf) {
            e = 1.0E-6;
            this._m22(1.0 - e)._m32((e - (zZeroToOne ? 1.0 : 2.0)) * zNear);
        } else if (nearInf) {
            e = 1.0E-6;
            this._m22((zZeroToOne ? 0.0 : 1.0) - e)._m32(((zZeroToOne ? 1.0 : 2.0) - e) * zFar);
        } else {
            this._m22((zZeroToOne ? zFar : zFar + zNear) / (zFar - zNear))._m32((zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar));
        }

        this._m23(1.0)._m30(0.0)._m31(0.0)._m33(0.0).properties = 1;
        return this;
    }

    public Matrix4d setPerspectiveLH(double fovy, double aspect, double zNear, double zFar) {
        return this.setPerspectiveLH(fovy, aspect, zNear, zFar, false);
    }

    public Matrix4d frustum(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.setFrustum(left, right, bottom, top, zNear, zFar, zZeroToOne) : this.frustumGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4d frustumGeneric(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        double rm00 = (zNear + zNear) / (right - left);
        double rm11 = (zNear + zNear) / (top - bottom);
        double rm20 = (right + left) / (right - left);
        double rm21 = (top + bottom) / (top - bottom);
        boolean farInf = zFar > 0.0 && Double.isInfinite(zFar);
        boolean nearInf = zNear > 0.0 && Double.isInfinite(zNear);
        double rm22;
        double rm32;
        double e;
        if (farInf) {
            e = 1.0E-6;
            rm22 = e - 1.0;
            rm32 = (e - (zZeroToOne ? 1.0 : 2.0)) * zNear;
        } else if (nearInf) {
            e = 1.0E-6;
            rm22 = (zZeroToOne ? 0.0 : 1.0) - e;
            rm32 = ((zZeroToOne ? 1.0 : 2.0) - e) * zFar;
        } else {
            rm22 = (zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar);
            rm32 = (zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar);
        }

        e = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22 - this.m30;
        double nm21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22 - this.m31;
        double nm22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22 - this.m32;
        double nm23 = this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22 - this.m33;
        dest._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m30(this.m20 * rm32)._m31(this.m21 * rm32)._m32(this.m22 * rm32)._m33(this.m23 * rm32)._m20(e)._m21(nm21)._m22(nm22)._m23(nm23)._properties(0);
        return dest;
    }

    public Matrix4d frustum(double left, double right, double bottom, double top, double zNear, double zFar, Matrix4d dest) {
        return this.frustum(left, right, bottom, top, zNear, zFar, false, dest);
    }

    public Matrix4d frustum(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne) {
        return this.frustum(left, right, bottom, top, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4d frustum(double left, double right, double bottom, double top, double zNear, double zFar) {
        return this.frustum(left, right, bottom, top, zNear, zFar, this);
    }

    public Matrix4d setFrustum(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne) {
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this._m00((zNear + zNear) / (right - left))._m11((zNear + zNear) / (top - bottom))._m20((right + left) / (right - left))._m21((top + bottom) / (top - bottom));
        boolean farInf = zFar > 0.0 && Double.isInfinite(zFar);
        boolean nearInf = zNear > 0.0 && Double.isInfinite(zNear);
        double e;
        if (farInf) {
            e = 1.0E-6;
            this._m22(e - 1.0)._m32((e - (zZeroToOne ? 1.0 : 2.0)) * zNear);
        } else if (nearInf) {
            e = 1.0E-6;
            this._m22((zZeroToOne ? 0.0 : 1.0) - e)._m32(((zZeroToOne ? 1.0 : 2.0) - e) * zFar);
        } else {
            this._m22((zZeroToOne ? zFar : zFar + zNear) / (zNear - zFar))._m32((zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar));
        }

        this._m23(-1.0)._m33(0.0).properties = this.m20 == 0.0 && this.m21 == 0.0 ? 1 : 0;
        return this;
    }

    public Matrix4d setFrustum(double left, double right, double bottom, double top, double zNear, double zFar) {
        return this.setFrustum(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4d frustumLH(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        return (this.properties & 4) != 0 ? dest.setFrustumLH(left, right, bottom, top, zNear, zFar, zZeroToOne) : this.frustumLHGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dest);
    }

    private Matrix4d frustumLHGeneric(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne, Matrix4d dest) {
        double rm00 = (zNear + zNear) / (right - left);
        double rm11 = (zNear + zNear) / (top - bottom);
        double rm20 = (right + left) / (right - left);
        double rm21 = (top + bottom) / (top - bottom);
        boolean farInf = zFar > 0.0 && Double.isInfinite(zFar);
        boolean nearInf = zNear > 0.0 && Double.isInfinite(zNear);
        double rm22;
        double rm32;
        double e;
        if (farInf) {
            e = 1.0E-6;
            rm22 = 1.0 - e;
            rm32 = (e - (zZeroToOne ? 1.0 : 2.0)) * zNear;
        } else if (nearInf) {
            e = 1.0E-6;
            rm22 = (zZeroToOne ? 0.0 : 1.0) - e;
            rm32 = ((zZeroToOne ? 1.0 : 2.0) - e) * zFar;
        } else {
            rm22 = (zZeroToOne ? zFar : zFar + zNear) / (zFar - zNear);
            rm32 = (zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar);
        }

        e = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22 + this.m30;
        double nm21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22 + this.m31;
        double nm22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22 + this.m32;
        double nm23 = this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22 + this.m33;
        dest._m00(this.m00 * rm00)._m01(this.m01 * rm00)._m02(this.m02 * rm00)._m03(this.m03 * rm00)._m10(this.m10 * rm11)._m11(this.m11 * rm11)._m12(this.m12 * rm11)._m13(this.m13 * rm11)._m30(this.m20 * rm32)._m31(this.m21 * rm32)._m32(this.m22 * rm32)._m33(this.m23 * rm32)._m20(e)._m21(nm21)._m22(nm22)._m23(nm23)._properties(0);
        return dest;
    }

    public Matrix4d frustumLH(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne) {
        return this.frustumLH(left, right, bottom, top, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4d frustumLH(double left, double right, double bottom, double top, double zNear, double zFar, Matrix4d dest) {
        return this.frustumLH(left, right, bottom, top, zNear, zFar, false, dest);
    }

    public Matrix4d frustumLH(double left, double right, double bottom, double top, double zNear, double zFar) {
        return this.frustumLH(left, right, bottom, top, zNear, zFar, this);
    }

    public Matrix4d setFrustumLH(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne) {
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this._m00((zNear + zNear) / (right - left))._m11((zNear + zNear) / (top - bottom))._m20((right + left) / (right - left))._m21((top + bottom) / (top - bottom));
        boolean farInf = zFar > 0.0 && Double.isInfinite(zFar);
        boolean nearInf = zNear > 0.0 && Double.isInfinite(zNear);
        double e;
        if (farInf) {
            e = 1.0E-6;
            this._m22(1.0 - e)._m32((e - (zZeroToOne ? 1.0 : 2.0)) * zNear);
        } else if (nearInf) {
            e = 1.0E-6;
            this._m22((zZeroToOne ? 0.0 : 1.0) - e)._m32(((zZeroToOne ? 1.0 : 2.0) - e) * zFar);
        } else {
            this._m22((zZeroToOne ? zFar : zFar + zNear) / (zFar - zNear))._m32((zZeroToOne ? zFar : zFar + zFar) * zNear / (zNear - zFar));
        }

        this._m23(1.0)._m33(0.0).properties = this.m20 == 0.0 && this.m21 == 0.0 ? 1 : 0;
        return this;
    }

    public Matrix4d setFrustumLH(double left, double right, double bottom, double top, double zNear, double zFar) {
        return this.setFrustumLH(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4d setFromIntrinsic(double alphaX, double alphaY, double gamma, double u0, double v0, int imgWidth, int imgHeight, double near, double far) {
        double l00 = 2.0 / imgWidth;
        double l11 = 2.0 / imgHeight;
        double l22 = 2.0 / (near - far);
        this.m00 = l00 * alphaX;
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m03 = 0.0;
        this.m10 = l00 * gamma;
        this.m11 = l11 * alphaY;
        this.m12 = 0.0;
        this.m13 = 0.0;
        this.m20 = l00 * u0 - 1.0;
        this.m21 = l11 * v0 - 1.0;
        this.m22 = l22 * -(near + far) + (far + near) / (near - far);
        this.m23 = -1.0;
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = l22 * -near * far;
        this.m33 = 0.0;
        this.properties = 1;
        return this;
    }

    public Vector4d frustumPlane(int plane, Vector4d dest) {
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

    public Vector3d frustumCorner(int corner, Vector3d dest) {
        double d1;
        double d2;
        double d3;
        double n1x;
        double n1y;
        double n1z;
        double n2x;
        double n2y;
        double n2z;
        double n3x;
        double n3y;
        double n3z;
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

        double c23x = n2y * n3z - n2z * n3y;
        double c23y = n2z * n3x - n2x * n3z;
        double c23z = n2x * n3y - n2y * n3x;
        double c31x = n3y * n1z - n3z * n1y;
        double c31y = n3z * n1x - n3x * n1z;
        double c31z = n3x * n1y - n3y * n1x;
        double c12x = n1y * n2z - n1z * n2y;
        double c12y = n1z * n2x - n1x * n2z;
        double c12z = n1x * n2y - n1y * n2x;
        double invDot = 1.0 / (n1x * c23x + n1y * c23y + n1z * c23z);
        dest.x = (-c23x * d1 - c31x * d2 - c12x * d3) * invDot;
        dest.y = (-c23y * d1 - c31y * d2 - c12y * d3) * invDot;
        dest.z = (-c23z * d1 - c31z * d2 - c12z * d3) * invDot;
        return dest;
    }

    public Vector3d perspectiveOrigin(Vector3d dest) {
        double n1x = this.m03 + this.m00;
        double n1y = this.m13 + this.m10;
        double n1z = this.m23 + this.m20;
        double d1 = this.m33 + this.m30;
        double n2x = this.m03 - this.m00;
        double n2y = this.m13 - this.m10;
        double n2z = this.m23 - this.m20;
        double d2 = this.m33 - this.m30;
        double n3x = this.m03 - this.m01;
        double n3y = this.m13 - this.m11;
        double n3z = this.m23 - this.m21;
        double d3 = this.m33 - this.m31;
        double c23x = n2y * n3z - n2z * n3y;
        double c23y = n2z * n3x - n2x * n3z;
        double c23z = n2x * n3y - n2y * n3x;
        double c31x = n3y * n1z - n3z * n1y;
        double c31y = n3z * n1x - n3x * n1z;
        double c31z = n3x * n1y - n3y * n1x;
        double c12x = n1y * n2z - n1z * n2y;
        double c12y = n1z * n2x - n1x * n2z;
        double c12z = n1x * n2y - n1y * n2x;
        double invDot = 1.0 / (n1x * c23x + n1y * c23y + n1z * c23z);
        dest.x = (-c23x * d1 - c31x * d2 - c12x * d3) * invDot;
        dest.y = (-c23y * d1 - c31y * d2 - c12y * d3) * invDot;
        dest.z = (-c23z * d1 - c31z * d2 - c12z * d3) * invDot;
        return dest;
    }

    public Vector3d perspectiveInvOrigin(Vector3d dest) {
        double invW = 1.0 / this.m23;
        dest.x = this.m20 * invW;
        dest.y = this.m21 * invW;
        dest.z = this.m22 * invW;
        return dest;
    }

    public double perspectiveFov() {
        double n1x = this.m03 + this.m01;
        double n1y = this.m13 + this.m11;
        double n1z = this.m23 + this.m21;
        double n2x = this.m01 - this.m03;
        double n2y = this.m11 - this.m13;
        double n2z = this.m21 - this.m23;
        double n1len = Math.sqrt(n1x * n1x + n1y * n1y + n1z * n1z);
        double n2len = Math.sqrt(n2x * n2x + n2y * n2y + n2z * n2z);
        return Math.acos((n1x * n2x + n1y * n2y + n1z * n2z) / (n1len * n2len));
    }

    public double perspectiveNear() {
        return this.m32 / (this.m23 + this.m22);
    }

    public double perspectiveFar() {
        return this.m32 / (this.m22 - this.m23);
    }

    public Vector3d frustumRayDir(double x, double y, Vector3d dest) {
        double a = this.m10 * this.m23;
        double b = this.m13 * this.m21;
        double c = this.m10 * this.m21;
        double d = this.m11 * this.m23;
        double e = this.m13 * this.m20;
        double f = this.m11 * this.m20;
        double g = this.m03 * this.m20;
        double h = this.m01 * this.m23;
        double i = this.m01 * this.m20;
        double j = this.m03 * this.m21;
        double k = this.m00 * this.m23;
        double l = this.m00 * this.m21;
        double m = this.m00 * this.m13;
        double n = this.m03 * this.m11;
        double o = this.m00 * this.m11;
        double p = this.m01 * this.m13;
        double q = this.m03 * this.m10;
        double r = this.m01 * this.m10;
        double m1x = (d + e + f - a - b - c) * (1.0 - y) + (a - b - c + d - e + f) * y;
        double m1y = (j + k + l - g - h - i) * (1.0 - y) + (g - h - i + j - k + l) * y;
        double m1z = (p + q + r - m - n - o) * (1.0 - y) + (m - n - o + p - q + r) * y;
        double m2x = (b - c - d + e + f - a) * (1.0 - y) + (a + b - c - d - e + f) * y;
        double m2y = (h - i - j + k + l - g) * (1.0 - y) + (g + h - i - j - k + l) * y;
        double m2z = (n - o - p + q + r - m) * (1.0 - y) + (m + n - o - p - q + r) * y;
        dest.x = m1x * (1.0 - x) + m2x * x;
        dest.y = m1y * (1.0 - x) + m2y * x;
        dest.z = m1z * (1.0 - x) + m2z * x;
        return dest.normalize(dest);
    }

    public Vector3d positiveZ(Vector3d dir) {
        return (this.properties & 16) != 0 ? this.normalizedPositiveZ(dir) : this.positiveZGeneric(dir);
    }

    private Vector3d positiveZGeneric(Vector3d dir) {
        return dir.set(this.m10 * this.m21 - this.m11 * this.m20, this.m20 * this.m01 - this.m21 * this.m00, this.m00 * this.m11 - this.m01 * this.m10).normalize();
    }

    public Vector3d normalizedPositiveZ(Vector3d dir) {
        return dir.set(this.m02, this.m12, this.m22);
    }

    public Vector3d positiveX(Vector3d dir) {
        return (this.properties & 16) != 0 ? this.normalizedPositiveX(dir) : this.positiveXGeneric(dir);
    }

    private Vector3d positiveXGeneric(Vector3d dir) {
        return dir.set(this.m11 * this.m22 - this.m12 * this.m21, this.m02 * this.m21 - this.m01 * this.m22, this.m01 * this.m12 - this.m02 * this.m11).normalize();
    }

    public Vector3d normalizedPositiveX(Vector3d dir) {
        return dir.set(this.m00, this.m10, this.m20);
    }

    public Vector3d positiveY(Vector3d dir) {
        return (this.properties & 16) != 0 ? this.normalizedPositiveY(dir) : this.positiveYGeneric(dir);
    }

    private Vector3d positiveYGeneric(Vector3d dir) {
        return dir.set(this.m12 * this.m20 - this.m10 * this.m22, this.m00 * this.m22 - this.m02 * this.m20, this.m02 * this.m10 - this.m00 * this.m12).normalize();
    }

    public Vector3d normalizedPositiveY(Vector3d dir) {
        return dir.set(this.m01, this.m11, this.m21);
    }

    public Vector3d originAffine(Vector3d dest) {
        double a = this.m00 * this.m11 - this.m01 * this.m10;
        double b = this.m00 * this.m12 - this.m02 * this.m10;
        double d = this.m01 * this.m12 - this.m02 * this.m11;
        double g = this.m20 * this.m31 - this.m21 * this.m30;
        double h = this.m20 * this.m32 - this.m22 * this.m30;
        double j = this.m21 * this.m32 - this.m22 * this.m31;
        dest.x = -this.m10 * j + this.m11 * h - this.m12 * g;
        dest.y = this.m00 * j - this.m01 * h + this.m02 * g;
        dest.z = -this.m30 * d + this.m31 * b - this.m32 * a;
        return dest;
    }

    public Vector3d origin(Vector3d dest) {
        return (this.properties & 2) != 0 ? this.originAffine(dest) : this.originGeneric(dest);
    }

    private Vector3d originGeneric(Vector3d dest) {
        double a = this.m00 * this.m11 - this.m01 * this.m10;
        double b = this.m00 * this.m12 - this.m02 * this.m10;
        double c = this.m00 * this.m13 - this.m03 * this.m10;
        double d = this.m01 * this.m12 - this.m02 * this.m11;
        double e = this.m01 * this.m13 - this.m03 * this.m11;
        double f = this.m02 * this.m13 - this.m03 * this.m12;
        double g = this.m20 * this.m31 - this.m21 * this.m30;
        double h = this.m20 * this.m32 - this.m22 * this.m30;
        double i = this.m20 * this.m33 - this.m23 * this.m30;
        double j = this.m21 * this.m32 - this.m22 * this.m31;
        double k = this.m21 * this.m33 - this.m23 * this.m31;
        double l = this.m22 * this.m33 - this.m23 * this.m32;
        double det = a * l - b * k + c * j + d * i - e * h + f * g;
        double invDet = 1.0 / det;
        double nm30 = (-this.m10 * j + this.m11 * h - this.m12 * g) * invDet;
        double nm31 = (this.m00 * j - this.m01 * h + this.m02 * g) * invDet;
        double nm32 = (-this.m30 * d + this.m31 * b - this.m32 * a) * invDet;
        double nm33 = det / (this.m20 * d - this.m21 * b + this.m22 * a);
        double x = nm30 * nm33;
        double y = nm31 * nm33;
        double z = nm32 * nm33;
        return dest.set(x, y, z);
    }

    public Matrix4d shadow(Vector4d light, double a, double b, double c, double d) {
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, this);
    }

    public Matrix4d shadow(Vector4d light, double a, double b, double c, double d, Matrix4d dest) {
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, dest);
    }

    public Matrix4d shadow(double lightX, double lightY, double lightZ, double lightW, double a, double b, double c, double d) {
        return this.shadow(lightX, lightY, lightZ, lightW, a, b, c, d, this);
    }

    public Matrix4d shadow(double lightX, double lightY, double lightZ, double lightW, double a, double b, double c, double d, Matrix4d dest) {
        double invPlaneLen = Math.invsqrt(a * a + b * b + c * c);
        double an = a * invPlaneLen;
        double bn = b * invPlaneLen;
        double cn = c * invPlaneLen;
        double dn = d * invPlaneLen;
        double dot = an * lightX + bn * lightY + cn * lightZ + dn * lightW;
        double rm00 = dot - an * lightX;
        double rm01 = -an * lightY;
        double rm02 = -an * lightZ;
        double rm03 = -an * lightW;
        double rm10 = -bn * lightX;
        double rm11 = dot - bn * lightY;
        double rm12 = -bn * lightZ;
        double rm13 = -bn * lightW;
        double rm20 = -cn * lightX;
        double rm21 = -cn * lightY;
        double rm22 = dot - cn * lightZ;
        double rm23 = -cn * lightW;
        double rm30 = -dn * lightX;
        double rm31 = -dn * lightY;
        double rm32 = -dn * lightZ;
        double rm33 = dot - dn * lightW;
        double nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02 + this.m30 * rm03;
        double nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02 + this.m31 * rm03;
        double nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02 + this.m32 * rm03;
        double nm03 = this.m03 * rm00 + this.m13 * rm01 + this.m23 * rm02 + this.m33 * rm03;
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12 + this.m30 * rm13;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12 + this.m31 * rm13;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12 + this.m32 * rm13;
        double nm13 = this.m03 * rm10 + this.m13 * rm11 + this.m23 * rm12 + this.m33 * rm13;
        double nm20 = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22 + this.m30 * rm23;
        double nm21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22 + this.m31 * rm23;
        double nm22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22 + this.m32 * rm23;
        double nm23 = this.m03 * rm20 + this.m13 * rm21 + this.m23 * rm22 + this.m33 * rm23;
        dest._m30(this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30 * rm33)._m31(this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31 * rm33)._m32(this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32 * rm33)._m33(this.m03 * rm30 + this.m13 * rm31 + this.m23 * rm32 + this.m33 * rm33)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._properties(this.properties & -30);
        return dest;
    }

    public Matrix4d shadow(Vector4d light, Matrix4d planeTransform, Matrix4d dest) {
        double a = planeTransform.m10;
        double b = planeTransform.m11;
        double c = planeTransform.m12;
        double d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32;
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, dest);
    }

    public Matrix4d shadow(Vector4d light, Matrix4d planeTransform) {
        return this.shadow(light, planeTransform, this);
    }

    public Matrix4d shadow(double lightX, double lightY, double lightZ, double lightW, Matrix4d planeTransform, Matrix4d dest) {
        double a = planeTransform.m10;
        double b = planeTransform.m11;
        double c = planeTransform.m12;
        double d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32;
        return this.shadow(lightX, lightY, lightZ, lightW, a, b, c, d, dest);
    }

    public Matrix4d shadow(double lightX, double lightY, double lightZ, double lightW, Matrix4d planeTransform) {
        return this.shadow(lightX, lightY, lightZ, lightW, planeTransform, this);
    }

    public Matrix4d billboardCylindrical(Vector3d objPos, Vector3d targetPos, Vector3d up) {
        double dirX = targetPos.x - objPos.x;
        double dirY = targetPos.y - objPos.y;
        double dirZ = targetPos.z - objPos.z;
        double leftX = up.y * dirZ - up.z * dirY;
        double leftY = up.z * dirX - up.x * dirZ;
        double leftZ = up.x * dirY - up.y * dirX;
        double invLeftLen = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLen;
        leftY *= invLeftLen;
        leftZ *= invLeftLen;
        dirX = leftY * up.z - leftZ * up.y;
        dirY = leftZ * up.x - leftX * up.z;
        dirZ = leftX * up.y - leftY * up.x;
        double invDirLen = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= invDirLen;
        dirY *= invDirLen;
        dirZ *= invDirLen;
        this._m00(leftX)._m01(leftY)._m02(leftZ)._m03(0.0)._m10(up.x)._m11(up.y)._m12(up.z)._m13(0.0)._m20(dirX)._m21(dirY)._m22(dirZ)._m23(0.0)._m30(objPos.x)._m31(objPos.y)._m32(objPos.z)._m33(1.0).properties = 18;
        return this;
    }

    public Matrix4d billboardSpherical(Vector3d objPos, Vector3d targetPos, Vector3d up) {
        double dirX = targetPos.x - objPos.x;
        double dirY = targetPos.y - objPos.y;
        double dirZ = targetPos.z - objPos.z;
        double invDirLen = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX *= invDirLen;
        dirY *= invDirLen;
        dirZ *= invDirLen;
        double leftX = up.y * dirZ - up.z * dirY;
        double leftY = up.z * dirX - up.x * dirZ;
        double leftZ = up.x * dirY - up.y * dirX;
        double invLeftLen = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLen;
        leftY *= invLeftLen;
        leftZ *= invLeftLen;
        double upX = dirY * leftZ - dirZ * leftY;
        double upY = dirZ * leftX - dirX * leftZ;
        double upZ = dirX * leftY - dirY * leftX;
        this._m00(leftX)._m01(leftY)._m02(leftZ)._m03(0.0)._m10(upX)._m11(upY)._m12(upZ)._m13(0.0)._m20(dirX)._m21(dirY)._m22(dirZ)._m23(0.0)._m30(objPos.x)._m31(objPos.y)._m32(objPos.z)._m33(1.0).properties = 18;
        return this;
    }

    public Matrix4d billboardSpherical(Vector3d objPos, Vector3d targetPos) {
        double toDirX = targetPos.x - objPos.x;
        double toDirY = targetPos.y - objPos.y;
        double toDirZ = targetPos.z - objPos.z;
        double x = -toDirY;
        double w = Math.sqrt(toDirX * toDirX + toDirY * toDirY + toDirZ * toDirZ) + toDirZ;
        double invNorm = Math.invsqrt(x * x + toDirX * toDirX + w * w);
        x *= invNorm;
        double y = toDirX * invNorm;
        w *= invNorm;
        double q00 = (x + x) * x;
        double q11 = (y + y) * y;
        double q01 = (x + x) * y;
        double q03 = (x + x) * w;
        double q13 = (y + y) * w;
        this._m00(1.0 - q11)._m01(q01)._m02(-q13)._m03(0.0)._m10(q01)._m11(1.0 - q00)._m12(q03)._m13(0.0)._m20(q13)._m21(-q03)._m22(1.0 - q11 - q00)._m23(0.0)._m30(objPos.x)._m31(objPos.y)._m32(objPos.z)._m33(1.0).properties = 18;
        return this;
    }

    public int hashCode() {
        int result = 1;
        long temp = Double.doubleToLongBits(this.m00);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m01);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m02);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m03);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m10);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m11);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m12);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m13);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m20);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m21);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m22);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m23);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m30);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m31);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m32);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m33);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof Matrix4d)) {
            return false;
        } else {
            Matrix4d other = (Matrix4d)obj;
            if (Double.doubleToLongBits(this.m00) != Double.doubleToLongBits(other.m00)) {
                return false;
            } else if (Double.doubleToLongBits(this.m01) != Double.doubleToLongBits(other.m01)) {
                return false;
            } else if (Double.doubleToLongBits(this.m02) != Double.doubleToLongBits(other.m02)) {
                return false;
            } else if (Double.doubleToLongBits(this.m03) != Double.doubleToLongBits(other.m03)) {
                return false;
            } else if (Double.doubleToLongBits(this.m10) != Double.doubleToLongBits(other.m10)) {
                return false;
            } else if (Double.doubleToLongBits(this.m11) != Double.doubleToLongBits(other.m11)) {
                return false;
            } else if (Double.doubleToLongBits(this.m12) != Double.doubleToLongBits(other.m12)) {
                return false;
            } else if (Double.doubleToLongBits(this.m13) != Double.doubleToLongBits(other.m13)) {
                return false;
            } else if (Double.doubleToLongBits(this.m20) != Double.doubleToLongBits(other.m20)) {
                return false;
            } else if (Double.doubleToLongBits(this.m21) != Double.doubleToLongBits(other.m21)) {
                return false;
            } else if (Double.doubleToLongBits(this.m22) != Double.doubleToLongBits(other.m22)) {
                return false;
            } else if (Double.doubleToLongBits(this.m23) != Double.doubleToLongBits(other.m23)) {
                return false;
            } else if (Double.doubleToLongBits(this.m30) != Double.doubleToLongBits(other.m30)) {
                return false;
            } else if (Double.doubleToLongBits(this.m31) != Double.doubleToLongBits(other.m31)) {
                return false;
            } else if (Double.doubleToLongBits(this.m32) != Double.doubleToLongBits(other.m32)) {
                return false;
            } else {
                return Double.doubleToLongBits(this.m33) == Double.doubleToLongBits(other.m33);
            }
        }
    }

    public boolean equals(Matrix4d m, double delta) {
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

    public Matrix4d pick(double x, double y, double width, double height, int[] viewport, Matrix4d dest) {
        double sx = viewport[2] / width;
        double sy = viewport[3] / height;
        double tx = (viewport[2] + 2.0 * (viewport[0] - x)) / width;
        double ty = (viewport[3] + 2.0 * (viewport[1] - y)) / height;
        dest._m30(this.m00 * tx + this.m10 * ty + this.m30)._m31(this.m01 * tx + this.m11 * ty + this.m31)._m32(this.m02 * tx + this.m12 * ty + this.m32)._m33(this.m03 * tx + this.m13 * ty + this.m33)._m00(this.m00 * sx)._m01(this.m01 * sx)._m02(this.m02 * sx)._m03(this.m03 * sx)._m10(this.m10 * sy)._m11(this.m11 * sy)._m12(this.m12 * sy)._m13(this.m13 * sy)._properties(0);
        return dest;
    }

    public Matrix4d pick(double x, double y, double width, double height, int[] viewport) {
        return this.pick(x, y, width, height, viewport, this);
    }

    public boolean isAffine() {
        return this.m03 == 0.0 && this.m13 == 0.0 && this.m23 == 0.0 && this.m33 == 1.0;
    }

    public Matrix4d swap(Matrix4d other) {
        double tmp = this.m00;
        this.m00 = other.m00;
        other.m00 = tmp;
        tmp = this.m01;
        this.m01 = other.m01;
        other.m01 = tmp;
        tmp = this.m02;
        this.m02 = other.m02;
        other.m02 = tmp;
        tmp = this.m03;
        this.m03 = other.m03;
        other.m03 = tmp;
        tmp = this.m10;
        this.m10 = other.m10;
        other.m10 = tmp;
        tmp = this.m11;
        this.m11 = other.m11;
        other.m11 = tmp;
        tmp = this.m12;
        this.m12 = other.m12;
        other.m12 = tmp;
        tmp = this.m13;
        this.m13 = other.m13;
        other.m13 = tmp;
        tmp = this.m20;
        this.m20 = other.m20;
        other.m20 = tmp;
        tmp = this.m21;
        this.m21 = other.m21;
        other.m21 = tmp;
        tmp = this.m22;
        this.m22 = other.m22;
        other.m22 = tmp;
        tmp = this.m23;
        this.m23 = other.m23;
        other.m23 = tmp;
        tmp = this.m30;
        this.m30 = other.m30;
        other.m30 = tmp;
        tmp = this.m31;
        this.m31 = other.m31;
        other.m31 = tmp;
        tmp = this.m32;
        this.m32 = other.m32;
        other.m32 = tmp;
        tmp = this.m33;
        this.m33 = other.m33;
        other.m33 = tmp;
        int props = this.properties;
        this.properties = other.properties;
        other.properties = props;
        return this;
    }

    public Matrix4d arcball(double radius, double centerX, double centerY, double centerZ, double angleX, double angleY, Matrix4d dest) {
        double m30 = this.m20 * -radius + this.m30;
        double m31 = this.m21 * -radius + this.m31;
        double m32 = this.m22 * -radius + this.m32;
        double m33 = this.m23 * -radius + this.m33;
        double sin = Math.sin(angleX);
        double cos = Math.cosFromSin(sin, angleX);
        double nm10 = this.m10 * cos + this.m20 * sin;
        double nm11 = this.m11 * cos + this.m21 * sin;
        double nm12 = this.m12 * cos + this.m22 * sin;
        double nm13 = this.m13 * cos + this.m23 * sin;
        double m20 = this.m20 * cos - this.m10 * sin;
        double m21 = this.m21 * cos - this.m11 * sin;
        double m22 = this.m22 * cos - this.m12 * sin;
        double m23 = this.m23 * cos - this.m13 * sin;
        sin = Math.sin(angleY);
        cos = Math.cosFromSin(sin, angleY);
        double nm00 = this.m00 * cos - m20 * sin;
        double nm01 = this.m01 * cos - m21 * sin;
        double nm02 = this.m02 * cos - m22 * sin;
        double nm03 = this.m03 * cos - m23 * sin;
        double nm20 = this.m00 * sin + m20 * cos;
        double nm21 = this.m01 * sin + m21 * cos;
        double nm22 = this.m02 * sin + m22 * cos;
        double nm23 = this.m03 * sin + m23 * cos;
        dest._m30(-nm00 * centerX - nm10 * centerY - nm20 * centerZ + m30)._m31(-nm01 * centerX - nm11 * centerY - nm21 * centerZ + m31)._m32(-nm02 * centerX - nm12 * centerY - nm22 * centerZ + m32)._m33(-nm03 * centerX - nm13 * centerY - nm23 * centerZ + m33)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d arcball(double radius, Vector3d center, double angleX, double angleY, Matrix4d dest) {
        return this.arcball(radius, center.x, center.y, center.z, angleX, angleY, dest);
    }

    public Matrix4d arcball(double radius, double centerX, double centerY, double centerZ, double angleX, double angleY) {
        return this.arcball(radius, centerX, centerY, centerZ, angleX, angleY, this);
    }

    public Matrix4d arcball(double radius, Vector3d center, double angleX, double angleY) {
        return this.arcball(radius, center.x, center.y, center.z, angleX, angleY, this);
    }

    public Matrix4d frustumAabb(Vector3d min, Vector3d max) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for(int t = 0; t < 8; ++t) {
            double x = ((t & 1) << 1) - 1.0;
            double y = ((t >>> 1 & 1) << 1) - 1.0;
            double z = ((t >>> 2 & 1) << 1) - 1.0;
            double invW = 1.0 / (this.m03 * x + this.m13 * y + this.m23 * z + this.m33);
            double nx = (this.m00 * x + this.m10 * y + this.m20 * z + this.m30) * invW;
            double ny = (this.m01 * x + this.m11 * y + this.m21 * z + this.m31) * invW;
            double nz = (this.m02 * x + this.m12 * y + this.m22 * z + this.m32) * invW;
            minX = java.lang.Math.min(minX, nx);
            minY = java.lang.Math.min(minY, ny);
            minZ = java.lang.Math.min(minZ, nz);
            maxX = java.lang.Math.max(maxX, nx);
            maxY = java.lang.Math.max(maxY, ny);
            maxZ = java.lang.Math.max(maxZ, nz);
        }

        min.x = minX;
        min.y = minY;
        min.z = minZ;
        max.x = maxX;
        max.y = maxY;
        max.z = maxZ;
        return this;
    }

    public Matrix4d projectedGridRange(Matrix4d projector, double sLower, double sUpper, Matrix4d dest) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        boolean intersection = false;

        for(int t = 0; t < 12; ++t) {
            double c0X;
            double c0Y;
            double c0Z;
            double c1X;
            double c1Y;
            double c1Z;
            if (t < 4) {
                c0X = -1.0;
                c1X = 1.0;
                c0Y = c1Y = ((t & 1) << 1) - 1.0;
                c0Z = c1Z = ((t >>> 1 & 1) << 1) - 1.0;
            } else if (t < 8) {
                c0Y = -1.0;
                c1Y = 1.0;
                c0X = c1X = ((t & 1) << 1) - 1.0;
                c0Z = c1Z = ((t >>> 1 & 1) << 1) - 1.0;
            } else {
                c0Z = -1.0;
                c1Z = 1.0;
                c0X = c1X = ((t & 1) << 1) - 1.0;
                c0Y = c1Y = ((t >>> 1 & 1) << 1) - 1.0;
            }

            double invW = 1.0 / (this.m03 * c0X + this.m13 * c0Y + this.m23 * c0Z + this.m33);
            double p0x = (this.m00 * c0X + this.m10 * c0Y + this.m20 * c0Z + this.m30) * invW;
            double p0y = (this.m01 * c0X + this.m11 * c0Y + this.m21 * c0Z + this.m31) * invW;
            double p0z = (this.m02 * c0X + this.m12 * c0Y + this.m22 * c0Z + this.m32) * invW;
            invW = 1.0 / (this.m03 * c1X + this.m13 * c1Y + this.m23 * c1Z + this.m33);
            double p1x = (this.m00 * c1X + this.m10 * c1Y + this.m20 * c1Z + this.m30) * invW;
            double p1y = (this.m01 * c1X + this.m11 * c1Y + this.m21 * c1Z + this.m31) * invW;
            double p1z = (this.m02 * c1X + this.m12 * c1Y + this.m22 * c1Z + this.m32) * invW;
            double dirX = p1x - p0x;
            double dirY = p1y - p0y;
            double dirZ = p1z - p0z;
            double invDenom = 1.0 / dirY;

            for(int s = 0; s < 2; ++s) {
                double isectT = -(p0y + (s == 0 ? sLower : sUpper)) * invDenom;
                if (isectT >= 0.0 && isectT <= 1.0) {
                    intersection = true;
                    double ix = p0x + isectT * dirX;
                    double iz = p0z + isectT * dirZ;
                    invW = 1.0 / (projector.m03 * ix + projector.m23 * iz + projector.m33);
                    double px = (projector.m00 * ix + projector.m20 * iz + projector.m30) * invW;
                    double py = (projector.m01 * ix + projector.m21 * iz + projector.m31) * invW;
                    minX = java.lang.Math.min(minX, px);
                    minY = java.lang.Math.min(minY, py);
                    maxX = java.lang.Math.max(maxX, px);
                    maxY = java.lang.Math.max(maxY, py);
                }
            }
        }

        if (!intersection) {
            return null;
        } else {
            dest.set(maxX - minX, 0.0, 0.0, 0.0, 0.0, maxY - minY, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, minX, minY, 0.0, 1.0)._properties(2);
            return dest;
        }
    }

    public Matrix4d perspectiveFrustumSlice(double near, double far, Matrix4d dest) {
        double invOldNear = (this.m23 + this.m22) / this.m32;
        double invNearFar = 1.0 / (near - far);
        dest._m00(this.m00 * invOldNear * near)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(this.m10)._m11(this.m11 * invOldNear * near)._m12(this.m12)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22((far + near) * invNearFar)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32((far + far) * near * invNearFar)._m33(this.m33)._properties(this.properties & -29);
        return dest;
    }

    public Matrix4d orthoCrop(Matrix4d view, Matrix4d dest) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for(int t = 0; t < 8; ++t) {
            double x = ((t & 1) << 1) - 1.0;
            double y = ((t >>> 1 & 1) << 1) - 1.0;
            double z = ((t >>> 2 & 1) << 1) - 1.0;
            double invW = 1.0 / (this.m03 * x + this.m13 * y + this.m23 * z + this.m33);
            double wx = (this.m00 * x + this.m10 * y + this.m20 * z + this.m30) * invW;
            double wy = (this.m01 * x + this.m11 * y + this.m21 * z + this.m31) * invW;
            double wz = (this.m02 * x + this.m12 * y + this.m22 * z + this.m32) * invW;
            invW = 1.0 / (view.m03 * wx + view.m13 * wy + view.m23 * wz + view.m33);
            double vx = view.m00 * wx + view.m10 * wy + view.m20 * wz + view.m30;
            double vy = view.m01 * wx + view.m11 * wy + view.m21 * wz + view.m31;
            double vz = (view.m02 * wx + view.m12 * wy + view.m22 * wz + view.m32) * invW;
            minX = java.lang.Math.min(minX, vx);
            maxX = java.lang.Math.max(maxX, vx);
            minY = java.lang.Math.min(minY, vy);
            maxY = java.lang.Math.max(maxY, vy);
            minZ = java.lang.Math.min(minZ, vz);
            maxZ = java.lang.Math.max(maxZ, vz);
        }

        return dest.setOrtho(minX, maxX, minY, maxY, -maxZ, -minZ);
    }

    public Matrix4d trapezoidCrop(double p0x, double p0y, double p1x, double p1y, double p2x, double p2y, double p3x, double p3y) {
        double aX = p1y - p0y;
        double aY = p0x - p1x;
        double nm10 = -aX;
        double nm30 = aX * p0y - aY * p0x;
        double nm31 = -(aX * p0x + aY * p0y);
        double c3x = aY * p3x + nm10 * p3y + nm30;
        double c3y = aX * p3x + aY * p3y + nm31;
        double s = -c3x / c3y;
        double nm00 = aY + s * aX;
        nm10 += s * aY;
        nm30 += s * nm31;
        double d1x = nm00 * p1x + nm10 * p1y + nm30;
        double d2x = nm00 * p2x + nm10 * p2y + nm30;
        double d = d1x * c3y / (d2x - d1x);
        nm31 += d;
        double sx = 2.0 / d2x;
        double sy = 1.0 / (c3y + d);
        double u = (sy + sy) * d / (1.0 - sy * d);
        double m03 = aX * sy;
        double m13 = aY * sy;
        double m33 = nm31 * sy;
        double nm01 = (u + 1.0) * m03;
        double nm11 = (u + 1.0) * m13;
        nm31 = (u + 1.0) * m33 - u;
        nm00 = sx * nm00 - m03;
        nm10 = sx * nm10 - m13;
        nm30 = sx * nm30 - m33;
        this.set(nm00, nm01, 0.0, m03, nm10, nm11, 0.0, m13, 0.0, 0.0, 1.0, 0.0, nm30, nm31, 0.0, m33);
        this.properties = 0;
        return this;
    }

    public Matrix4d transformAab(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vector3d outMin, Vector3d outMax) {
        double xax = this.m00 * minX;
        double xay = this.m01 * minX;
        double xaz = this.m02 * minX;
        double xbx = this.m00 * maxX;
        double xby = this.m01 * maxX;
        double xbz = this.m02 * maxX;
        double yax = this.m10 * minY;
        double yay = this.m11 * minY;
        double yaz = this.m12 * minY;
        double ybx = this.m10 * maxY;
        double yby = this.m11 * maxY;
        double ybz = this.m12 * maxY;
        double zax = this.m20 * minZ;
        double zay = this.m21 * minZ;
        double zaz = this.m22 * minZ;
        double zbx = this.m20 * maxZ;
        double zby = this.m21 * maxZ;
        double zbz = this.m22 * maxZ;
        double xmaxx;
        double xminx;
        if (xax < xbx) {
            xminx = xax;
            xmaxx = xbx;
        } else {
            xminx = xbx;
            xmaxx = xax;
        }

        double xmaxy;
        double xminy;
        if (xay < xby) {
            xminy = xay;
            xmaxy = xby;
        } else {
            xminy = xby;
            xmaxy = xay;
        }

        double xmaxz;
        double xminz;
        if (xaz < xbz) {
            xminz = xaz;
            xmaxz = xbz;
        } else {
            xminz = xbz;
            xmaxz = xaz;
        }

        double ymaxx;
        double yminx;
        if (yax < ybx) {
            yminx = yax;
            ymaxx = ybx;
        } else {
            yminx = ybx;
            ymaxx = yax;
        }

        double ymaxy;
        double yminy;
        if (yay < yby) {
            yminy = yay;
            ymaxy = yby;
        } else {
            yminy = yby;
            ymaxy = yay;
        }

        double ymaxz;
        double yminz;
        if (yaz < ybz) {
            yminz = yaz;
            ymaxz = ybz;
        } else {
            yminz = ybz;
            ymaxz = yaz;
        }

        double zmaxx;
        double zminx;
        if (zax < zbx) {
            zminx = zax;
            zmaxx = zbx;
        } else {
            zminx = zbx;
            zmaxx = zax;
        }

        double zminy;
        double zmaxy;
        if (zay < zby) {
            zminy = zay;
            zmaxy = zby;
        } else {
            zminy = zby;
            zmaxy = zay;
        }

        double zminz;
        double zmaxz;
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

    public Matrix4d transformAab(Vector3d min, Vector3d max, Vector3d outMin, Vector3d outMax) {
        return this.transformAab(min.x, min.y, min.z, max.x, max.y, max.z, outMin, outMax);
    }

    public Matrix4d lerp(Matrix4d other, double t) {
        return this.lerp(other, t, this);
    }

    public Matrix4d lerp(Matrix4d other, double t, Matrix4d dest) {
        dest._m00(Math.fma(other.m00 - this.m00, t, this.m00))._m01(Math.fma(other.m01 - this.m01, t, this.m01))._m02(Math.fma(other.m02 - this.m02, t, this.m02))._m03(Math.fma(other.m03 - this.m03, t, this.m03))._m10(Math.fma(other.m10 - this.m10, t, this.m10))._m11(Math.fma(other.m11 - this.m11, t, this.m11))._m12(Math.fma(other.m12 - this.m12, t, this.m12))._m13(Math.fma(other.m13 - this.m13, t, this.m13))._m20(Math.fma(other.m20 - this.m20, t, this.m20))._m21(Math.fma(other.m21 - this.m21, t, this.m21))._m22(Math.fma(other.m22 - this.m22, t, this.m22))._m23(Math.fma(other.m23 - this.m23, t, this.m23))._m30(Math.fma(other.m30 - this.m30, t, this.m30))._m31(Math.fma(other.m31 - this.m31, t, this.m31))._m32(Math.fma(other.m32 - this.m32, t, this.m32))._m33(Math.fma(other.m33 - this.m33, t, this.m33))._properties(this.properties & other.properties());
        return dest;
    }

    public Matrix4d rotateTowards(Vector3d direction, Vector3d up, Matrix4d dest) {
        return this.rotateTowards(direction.x, direction.y, direction.z, up.x, up.y, up.z, dest);
    }

    public Matrix4d rotateTowards(Vector3d direction, Vector3d up) {
        return this.rotateTowards(direction.x, direction.y, direction.z, up.x, up.y, up.z, this);
    }

    public Matrix4d rotateTowards(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
        return this.rotateTowards(dirX, dirY, dirZ, upX, upY, upZ, this);
    }

    public Matrix4d rotateTowards(double dirX, double dirY, double dirZ, double upX, double upY, double upZ, Matrix4d dest) {
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
        double nm03 = this.m03 * leftX + this.m13 * leftY + this.m23 * leftZ;
        double nm10 = this.m00 * upnX + this.m10 * upnY + this.m20 * upnZ;
        double nm11 = this.m01 * upnX + this.m11 * upnY + this.m21 * upnZ;
        double nm12 = this.m02 * upnX + this.m12 * upnY + this.m22 * upnZ;
        double nm13 = this.m03 * upnX + this.m13 * upnY + this.m23 * upnZ;
        dest._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._m20(this.m00 * ndirX + this.m10 * ndirY + this.m20 * ndirZ)._m21(this.m01 * ndirX + this.m11 * ndirY + this.m21 * ndirZ)._m22(this.m02 * ndirX + this.m12 * ndirY + this.m22 * ndirZ)._m23(this.m03 * ndirX + this.m13 * ndirY + this.m23 * ndirZ)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d rotationTowards(Vector3d dir, Vector3d up) {
        return this.rotationTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    public Matrix4d rotationTowards(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
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
        if ((this.properties & 4) == 0) {
            this._identity();
        }

        this.m00 = leftX;
        this.m01 = leftY;
        this.m02 = leftZ;
        this.m10 = upnX;
        this.m11 = upnY;
        this.m12 = upnZ;
        this.m20 = ndirX;
        this.m21 = ndirY;
        this.m22 = ndirZ;
        this.properties = 18;
        return this;
    }

    public Matrix4d translationRotateTowards(Vector3d pos, Vector3d dir, Vector3d up) {
        return this.translationRotateTowards(pos.x, pos.y, pos.z, dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    public Matrix4d translationRotateTowards(double posX, double posY, double posZ, double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
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
        this.m03 = 0.0;
        this.m10 = upnX;
        this.m11 = upnY;
        this.m12 = upnZ;
        this.m13 = 0.0;
        this.m20 = ndirX;
        this.m21 = ndirY;
        this.m22 = ndirZ;
        this.m23 = 0.0;
        this.m30 = posX;
        this.m31 = posY;
        this.m32 = posZ;
        this.m33 = 1.0;
        this.properties = 18;
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

    public Matrix4d affineSpan(Vector3d corner, Vector3d xDir, Vector3d yDir, Vector3d zDir) {
        double a = this.m10 * this.m22;
        double b = this.m10 * this.m21;
        double c = this.m10 * this.m02;
        double d = this.m10 * this.m01;
        double e = this.m11 * this.m22;
        double f = this.m11 * this.m20;
        double g = this.m11 * this.m02;
        double h = this.m11 * this.m00;
        double i = this.m12 * this.m21;
        double j = this.m12 * this.m20;
        double k = this.m12 * this.m01;
        double l = this.m12 * this.m00;
        double m = this.m20 * this.m02;
        double n = this.m20 * this.m01;
        double o = this.m21 * this.m02;
        double p = this.m21 * this.m00;
        double q = this.m22 * this.m01;
        double r = this.m22 * this.m00;
        double s = 1.0 / (this.m00 * this.m11 - this.m01 * this.m10) * this.m22 + (this.m02 * this.m10 - this.m00 * this.m12) * this.m21 + (this.m01 * this.m12 - this.m02 * this.m11) * this.m20;
        double nm00 = (e - i) * s;
        double nm01 = (o - q) * s;
        double nm02 = (k - g) * s;
        double nm10 = (j - a) * s;
        double nm11 = (r - m) * s;
        double nm12 = (c - l) * s;
        double nm20 = (b - f) * s;
        double nm21 = (n - p) * s;
        double nm22 = (h - d) * s;
        corner.x = -nm00 - nm10 - nm20 + (a * this.m31 - b * this.m32 + f * this.m32 - e * this.m30 + i * this.m30 - j * this.m31) * s;
        corner.y = -nm01 - nm11 - nm21 + (m * this.m31 - n * this.m32 + p * this.m32 - o * this.m30 + q * this.m30 - r * this.m31) * s;
        corner.z = -nm02 - nm12 - nm22 + (g * this.m30 - k * this.m30 + l * this.m31 - c * this.m31 + d * this.m32 - h * this.m32) * s;
        xDir.x = 2.0 * nm00;
        xDir.y = 2.0 * nm01;
        xDir.z = 2.0 * nm02;
        yDir.x = 2.0 * nm10;
        yDir.y = 2.0 * nm11;
        yDir.z = 2.0 * nm12;
        zDir.x = 2.0 * nm20;
        zDir.y = 2.0 * nm21;
        zDir.z = 2.0 * nm22;
        return this;
    }

    public boolean testPoint(double x, double y, double z) {
        double nxX = this.m03 + this.m00;
        double nxY = this.m13 + this.m10;
        double nxZ = this.m23 + this.m20;
        double nxW = this.m33 + this.m30;
        double pxX = this.m03 - this.m00;
        double pxY = this.m13 - this.m10;
        double pxZ = this.m23 - this.m20;
        double pxW = this.m33 - this.m30;
        double nyX = this.m03 + this.m01;
        double nyY = this.m13 + this.m11;
        double nyZ = this.m23 + this.m21;
        double nyW = this.m33 + this.m31;
        double pyX = this.m03 - this.m01;
        double pyY = this.m13 - this.m11;
        double pyZ = this.m23 - this.m21;
        double pyW = this.m33 - this.m31;
        double nzX = this.m03 + this.m02;
        double nzY = this.m13 + this.m12;
        double nzZ = this.m23 + this.m22;
        double nzW = this.m33 + this.m32;
        double pzX = this.m03 - this.m02;
        double pzY = this.m13 - this.m12;
        double pzZ = this.m23 - this.m22;
        double pzW = this.m33 - this.m32;
        return nxX * x + nxY * y + nxZ * z + nxW >= 0.0 && pxX * x + pxY * y + pxZ * z + pxW >= 0.0 && nyX * x + nyY * y + nyZ * z + nyW >= 0.0 && pyX * x + pyY * y + pyZ * z + pyW >= 0.0 && nzX * x + nzY * y + nzZ * z + nzW >= 0.0 && pzX * x + pzY * y + pzZ * z + pzW >= 0.0;
    }

    public boolean testSphere(double x, double y, double z, double r) {
        double nxX = this.m03 + this.m00;
        double nxY = this.m13 + this.m10;
        double nxZ = this.m23 + this.m20;
        double nxW = this.m33 + this.m30;
        double invl = Math.invsqrt(nxX * nxX + nxY * nxY + nxZ * nxZ);
        nxX *= invl;
        nxY *= invl;
        nxZ *= invl;
        nxW *= invl;
        double pxX = this.m03 - this.m00;
        double pxY = this.m13 - this.m10;
        double pxZ = this.m23 - this.m20;
        double pxW = this.m33 - this.m30;
        invl = Math.invsqrt(pxX * pxX + pxY * pxY + pxZ * pxZ);
        pxX *= invl;
        pxY *= invl;
        pxZ *= invl;
        pxW *= invl;
        double nyX = this.m03 + this.m01;
        double nyY = this.m13 + this.m11;
        double nyZ = this.m23 + this.m21;
        double nyW = this.m33 + this.m31;
        invl = Math.invsqrt(nyX * nyX + nyY * nyY + nyZ * nyZ);
        nyX *= invl;
        nyY *= invl;
        nyZ *= invl;
        nyW *= invl;
        double pyX = this.m03 - this.m01;
        double pyY = this.m13 - this.m11;
        double pyZ = this.m23 - this.m21;
        double pyW = this.m33 - this.m31;
        invl = Math.invsqrt(pyX * pyX + pyY * pyY + pyZ * pyZ);
        pyX *= invl;
        pyY *= invl;
        pyZ *= invl;
        pyW *= invl;
        double nzX = this.m03 + this.m02;
        double nzY = this.m13 + this.m12;
        double nzZ = this.m23 + this.m22;
        double nzW = this.m33 + this.m32;
        invl = Math.invsqrt(nzX * nzX + nzY * nzY + nzZ * nzZ);
        nzX *= invl;
        nzY *= invl;
        nzZ *= invl;
        nzW *= invl;
        double pzX = this.m03 - this.m02;
        double pzY = this.m13 - this.m12;
        double pzZ = this.m23 - this.m22;
        double pzW = this.m33 - this.m32;
        invl = Math.invsqrt(pzX * pzX + pzY * pzY + pzZ * pzZ);
        pzX *= invl;
        pzY *= invl;
        pzZ *= invl;
        pzW *= invl;
        return nxX * x + nxY * y + nxZ * z + nxW >= -r && pxX * x + pxY * y + pxZ * z + pxW >= -r && nyX * x + nyY * y + nyZ * z + nyW >= -r && pyX * x + pyY * y + pyZ * z + pyW >= -r && nzX * x + nzY * y + nzZ * z + nzW >= -r && pzX * x + pzY * y + pzZ * z + pzW >= -r;
    }

    public boolean testAab(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double nxX = this.m03 + this.m00;
        double nxY = this.m13 + this.m10;
        double nxZ = this.m23 + this.m20;
        double nxW = this.m33 + this.m30;
        double pxX = this.m03 - this.m00;
        double pxY = this.m13 - this.m10;
        double pxZ = this.m23 - this.m20;
        double pxW = this.m33 - this.m30;
        double nyX = this.m03 + this.m01;
        double nyY = this.m13 + this.m11;
        double nyZ = this.m23 + this.m21;
        double nyW = this.m33 + this.m31;
        double pyX = this.m03 - this.m01;
        double pyY = this.m13 - this.m11;
        double pyZ = this.m23 - this.m21;
        double pyW = this.m33 - this.m31;
        double nzX = this.m03 + this.m02;
        double nzY = this.m13 + this.m12;
        double nzZ = this.m23 + this.m22;
        double nzW = this.m33 + this.m32;
        double pzX = this.m03 - this.m02;
        double pzY = this.m13 - this.m12;
        double pzZ = this.m23 - this.m22;
        double pzW = this.m33 - this.m32;
        return nxX * (nxX < 0.0 ? minX : maxX) + nxY * (nxY < 0.0 ? minY : maxY) + nxZ * (nxZ < 0.0 ? minZ : maxZ) >= -nxW && pxX * (pxX < 0.0 ? minX : maxX) + pxY * (pxY < 0.0 ? minY : maxY) + pxZ * (pxZ < 0.0 ? minZ : maxZ) >= -pxW && nyX * (nyX < 0.0 ? minX : maxX) + nyY * (nyY < 0.0 ? minY : maxY) + nyZ * (nyZ < 0.0 ? minZ : maxZ) >= -nyW && pyX * (pyX < 0.0 ? minX : maxX) + pyY * (pyY < 0.0 ? minY : maxY) + pyZ * (pyZ < 0.0 ? minZ : maxZ) >= -pyW && nzX * (nzX < 0.0 ? minX : maxX) + nzY * (nzY < 0.0 ? minY : maxY) + nzZ * (nzZ < 0.0 ? minZ : maxZ) >= -nzW && pzX * (pzX < 0.0 ? minX : maxX) + pzY * (pzY < 0.0 ? minY : maxY) + pzZ * (pzZ < 0.0 ? minZ : maxZ) >= -pzW;
    }

    public Matrix4d obliqueZ(double a, double b) {
        this.m20 += this.m00 * a + this.m10 * b;
        this.m21 += this.m01 * a + this.m11 * b;
        this.m22 += this.m02 * a + this.m12 * b;
        this.properties &= 2;
        return this;
    }

    public Matrix4d obliqueZ(double a, double b, Matrix4d dest) {
        dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(this.m00 * a + this.m10 * b + this.m20)._m21(this.m01 * a + this.m11 * b + this.m21)._m22(this.m02 * a + this.m12 * b + this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 2);
        return dest;
    }

    public static void projViewFromRectangle(Vector3d eye, Vector3d p, Vector3d x, Vector3d y, double nearFarDist, boolean zeroToOne, Matrix4d projDest, Matrix4d viewDest) {
        double zx = y.y * x.z - y.z * x.y;
        double zy = y.z * x.x - y.x * x.z;
        double zz = y.x * x.y - y.y * x.x;
        double zd = zx * (p.x - eye.x) + zy * (p.y - eye.y) + zz * (p.z - eye.z);
        double zs = zd >= 0.0 ? 1.0 : -1.0;
        zx *= zs;
        zy *= zs;
        zz *= zs;
        zd *= zs;
        viewDest.setLookAt(eye.x, eye.y, eye.z, eye.x + zx, eye.y + zy, eye.z + zz, y.x, y.y, y.z);
        double px = viewDest.m00 * p.x + viewDest.m10 * p.y + viewDest.m20 * p.z + viewDest.m30;
        double py = viewDest.m01 * p.x + viewDest.m11 * p.y + viewDest.m21 * p.z + viewDest.m31;
        double tx = viewDest.m00 * x.x + viewDest.m10 * x.y + viewDest.m20 * x.z;
        double ty = viewDest.m01 * y.x + viewDest.m11 * y.y + viewDest.m21 * y.z;
        double len = Math.sqrt(zx * zx + zy * zy + zz * zz);
        double near = zd / len;
        double far;
        if (Double.isInfinite(nearFarDist) && nearFarDist < 0.0) {
            far = near;
            near = Double.POSITIVE_INFINITY;
        } else if (Double.isInfinite(nearFarDist) && nearFarDist > 0.0) {
            far = Double.POSITIVE_INFINITY;
        } else if (nearFarDist < 0.0) {
            far = near;
            near += nearFarDist;
        } else {
            far = near + nearFarDist;
        }

        projDest.setFrustum(px, px + tx, py, py + ty, near, far, zeroToOne);
    }

    public Matrix4d withLookAtUp(Vector3d up) {
        return this.withLookAtUp(up.x, up.y, up.z, this);
    }

    public Matrix4d withLookAtUp(Vector3d up, Matrix4d dest) {
        return this.withLookAtUp(up.x, up.y, up.z);
    }

    public Matrix4d withLookAtUp(double upX, double upY, double upZ) {
        return this.withLookAtUp(upX, upY, upZ, this);
    }

    public Matrix4d withLookAtUp(double upX, double upY, double upZ, Matrix4d dest) {
        double y = (upY * this.m21 - upZ * this.m11) * this.m02 + (upZ * this.m01 - upX * this.m21) * this.m12 + (upX * this.m11 - upY * this.m01) * this.m22;
        double x = upX * this.m01 + upY * this.m11 + upZ * this.m21;
        if ((this.properties & 16) == 0) {
            x *= Math.sqrt(this.m01 * this.m01 + this.m11 * this.m11 + this.m21 * this.m21);
        }

        double invsqrt = Math.invsqrt(y * y + x * x);
        double c = x * invsqrt;
        double s = y * invsqrt;
        double nm00 = c * this.m00 - s * this.m01;
        double nm10 = c * this.m10 - s * this.m11;
        double nm20 = c * this.m20 - s * this.m21;
        double nm31 = s * this.m30 + c * this.m31;
        double nm01 = s * this.m00 + c * this.m01;
        double nm11 = s * this.m10 + c * this.m11;
        double nm21 = s * this.m20 + c * this.m21;
        double nm30 = c * this.m30 - s * this.m31;
        dest._m00(nm00)._m10(nm10)._m20(nm20)._m30(nm30)._m01(nm01)._m11(nm11)._m21(nm21)._m31(nm31);
        if (dest != this) {
            dest._m02(this.m02)._m12(this.m12)._m22(this.m22)._m32(this.m32)._m03(this.m03)._m13(this.m13)._m23(this.m23)._m33(this.m33);
        }

        dest._properties(this.properties & -14);
        return dest;
    }

    public Matrix4d mapXZY() {
        return this.mapXZY(this);
    }

    public Matrix4d mapXZY(Matrix4d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapXZnY() {
        return this.mapXZnY(this);
    }

    public Matrix4d mapXZnY(Matrix4d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapXnYnZ() {
        return this.mapXnYnZ(this);
    }

    public Matrix4d mapXnYnZ(Matrix4d dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapXnZY() {
        return this.mapXnZY(this);
    }

    public Matrix4d mapXnZY(Matrix4d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapXnZnY() {
        return this.mapXnZnY(this);
    }

    public Matrix4d mapXnZnY(Matrix4d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapYXZ() {
        return this.mapYXZ(this);
    }

    public Matrix4d mapYXZ(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapYXnZ() {
        return this.mapYXnZ(this);
    }

    public Matrix4d mapYXnZ(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapYZX() {
        return this.mapYZX(this);
    }

    public Matrix4d mapYZX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapYZnX() {
        return this.mapYZnX(this);
    }

    public Matrix4d mapYZnX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapYnXZ() {
        return this.mapYnXZ(this);
    }

    public Matrix4d mapYnXZ(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapYnXnZ() {
        return this.mapYnXnZ(this);
    }

    public Matrix4d mapYnXnZ(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapYnZX() {
        return this.mapYnZX(this);
    }

    public Matrix4d mapYnZX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapYnZnX() {
        return this.mapYnZnX(this);
    }

    public Matrix4d mapYnZnX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapZXY() {
        return this.mapZXY(this);
    }

    public Matrix4d mapZXY(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapZXnY() {
        return this.mapZXnY(this);
    }

    public Matrix4d mapZXnY(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapZYX() {
        return this.mapZYX(this);
    }

    public Matrix4d mapZYX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapZYnX() {
        return this.mapZYnX(this);
    }

    public Matrix4d mapZYnX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapZnXY() {
        return this.mapZnXY(this);
    }

    public Matrix4d mapZnXY(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapZnXnY() {
        return this.mapZnXnY(this);
    }

    public Matrix4d mapZnXnY(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapZnYX() {
        return this.mapZnYX(this);
    }

    public Matrix4d mapZnYX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapZnYnX() {
        return this.mapZnYnX(this);
    }

    public Matrix4d mapZnYnX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnXYnZ() {
        return this.mapnXYnZ(this);
    }

    public Matrix4d mapnXYnZ(Matrix4d dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnXZY() {
        return this.mapnXZY(this);
    }

    public Matrix4d mapnXZY(Matrix4d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnXZnY() {
        return this.mapnXZnY(this);
    }

    public Matrix4d mapnXZnY(Matrix4d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnXnYZ() {
        return this.mapnXnYZ(this);
    }

    public Matrix4d mapnXnYZ(Matrix4d dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnXnYnZ() {
        return this.mapnXnYnZ(this);
    }

    public Matrix4d mapnXnYnZ(Matrix4d dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnXnZY() {
        return this.mapnXnZY(this);
    }

    public Matrix4d mapnXnZY(Matrix4d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnXnZnY() {
        return this.mapnXnZnY(this);
    }

    public Matrix4d mapnXnZnY(Matrix4d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnYXZ() {
        return this.mapnYXZ(this);
    }

    public Matrix4d mapnYXZ(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnYXnZ() {
        return this.mapnYXnZ(this);
    }

    public Matrix4d mapnYXnZ(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnYZX() {
        return this.mapnYZX(this);
    }

    public Matrix4d mapnYZX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnYZnX() {
        return this.mapnYZnX(this);
    }

    public Matrix4d mapnYZnX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnYnXZ() {
        return this.mapnYnXZ(this);
    }

    public Matrix4d mapnYnXZ(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnYnXnZ() {
        return this.mapnYnXnZ(this);
    }

    public Matrix4d mapnYnXnZ(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnYnZX() {
        return this.mapnYnZX(this);
    }

    public Matrix4d mapnYnZX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnYnZnX() {
        return this.mapnYnZnX(this);
    }

    public Matrix4d mapnYnZnX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m03(this.m03)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnZXY() {
        return this.mapnZXY(this);
    }

    public Matrix4d mapnZXY(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnZXnY() {
        return this.mapnZXnY(this);
    }

    public Matrix4d mapnZXnY(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(m00)._m11(m01)._m12(m02)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnZYX() {
        return this.mapnZYX(this);
    }

    public Matrix4d mapnZYX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnZYnX() {
        return this.mapnZYnX(this);
    }

    public Matrix4d mapnZYnX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnZnXY() {
        return this.mapnZnXY(this);
    }

    public Matrix4d mapnZnXY(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(m10)._m21(m11)._m22(m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnZnXnY() {
        return this.mapnZnXnY(this);
    }

    public Matrix4d mapnZnXnY(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(this.m13)._m20(-m10)._m21(-m11)._m22(-m12)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnZnYX() {
        return this.mapnZnYX(this);
    }

    public Matrix4d mapnZnYX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(m00)._m21(m01)._m22(m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d mapnZnYnX() {
        return this.mapnZnYnX(this);
    }

    public Matrix4d mapnZnYnX(Matrix4d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(-m00)._m21(-m01)._m22(-m02)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d negateX() {
        return this._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._properties(this.properties & 18);
    }

    public Matrix4d negateX(Matrix4d dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d negateY() {
        return this._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._properties(this.properties & 18);
    }

    public Matrix4d negateY(Matrix4d dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m13(this.m13)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public Matrix4d negateZ() {
        return this._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._properties(this.properties & 18);
    }

    public Matrix4d negateZ(Matrix4d dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m03(this.m03)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m13(this.m13)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m23(this.m23)._m30(this.m30)._m31(this.m31)._m32(this.m32)._m33(this.m33)._properties(this.properties & 18);
    }

    public boolean isFinite() {
        return Math.isFinite(this.m00) && Math.isFinite(this.m01) && Math.isFinite(this.m02) && Math.isFinite(this.m03) && Math.isFinite(this.m10) && Math.isFinite(this.m11) && Math.isFinite(this.m12) && Math.isFinite(this.m13) && Math.isFinite(this.m20) && Math.isFinite(this.m21) && Math.isFinite(this.m22) && Math.isFinite(this.m23) && Math.isFinite(this.m30) && Math.isFinite(this.m31) && Math.isFinite(this.m32) && Math.isFinite(this.m33);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
