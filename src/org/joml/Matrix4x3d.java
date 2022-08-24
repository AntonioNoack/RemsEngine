package org.joml;

import java.text.NumberFormat;

@SuppressWarnings("unused")
public class Matrix4x3d {

    public double m00;
    public double m01;
    public double m02;
    public double m10;
    public double m11;
    public double m12;
    public double m20;
    public double m21;
    public double m22;
    public double m30;
    public double m31;
    public double m32;
    public int properties;

    public Matrix4x3d() {
        this.m00 = 1.0;
        this.m11 = 1.0;
        this.m22 = 1.0;
        this.properties = 28;
    }

    public Matrix4x3d(Matrix4x3d mat) {
        this.set(mat);
    }

    public Matrix4x3d(Matrix4x3f mat) {
        this.set(mat);
    }

    public Matrix4x3d(Matrix3d mat) {
        this.set(mat);
    }

    public Matrix4x3d(Matrix3f mat) {
        this.set(mat);
    }

    public Matrix4x3d(double m00, double m01, double m02, double m10, double m11, double m12, double m20, double m21, double m22, double m30, double m31, double m32) {
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

    public Matrix4x3d assume(int properties) {
        this.properties = properties;
        return this;
    }

    public Matrix4x3d determineProperties() {
        int properties = 0;
        if (this.m00 == 1.0 && this.m01 == 0.0 && this.m02 == 0.0 && this.m10 == 0.0 && this.m11 == 1.0 && this.m12 == 0.0 && this.m20 == 0.0 && this.m21 == 0.0 && this.m22 == 1.0) {
            properties |= 24;
            if (this.m30 == 0.0 && this.m31 == 0.0 && this.m32 == 0.0) {
                properties |= 4;
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

    public double m30() {
        return this.m30;
    }

    public double m31() {
        return this.m31;
    }

    public double m32() {
        return this.m32;
    }

    Matrix4x3d _properties(int properties) {
        this.properties = properties;
        return this;
    }

    Matrix4x3d _m00(double m00) {
        this.m00 = m00;
        return this;
    }

    Matrix4x3d _m01(double m01) {
        this.m01 = m01;
        return this;
    }

    Matrix4x3d _m02(double m02) {
        this.m02 = m02;
        return this;
    }

    Matrix4x3d _m10(double m10) {
        this.m10 = m10;
        return this;
    }

    Matrix4x3d _m11(double m11) {
        this.m11 = m11;
        return this;
    }

    Matrix4x3d _m12(double m12) {
        this.m12 = m12;
        return this;
    }

    Matrix4x3d _m20(double m20) {
        this.m20 = m20;
        return this;
    }

    Matrix4x3d _m21(double m21) {
        this.m21 = m21;
        return this;
    }

    Matrix4x3d _m22(double m22) {
        this.m22 = m22;
        return this;
    }

    Matrix4x3d _m30(double m30) {
        this.m30 = m30;
        return this;
    }

    Matrix4x3d _m31(double m31) {
        this.m31 = m31;
        return this;
    }

    Matrix4x3d _m32(double m32) {
        this.m32 = m32;
        return this;
    }

    public Matrix4x3d m00(double m00) {
        this.m00 = m00;
        this.properties &= -17;
        if (m00 != 1.0) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3d m01(double m01) {
        this.m01 = m01;
        this.properties &= -17;
        if (m01 != 0.0) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3d m02(double m02) {
        this.m02 = m02;
        this.properties &= -17;
        if (m02 != 0.0) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3d m10(double m10) {
        this.m10 = m10;
        this.properties &= -17;
        if (m10 != 0.0) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3d m11(double m11) {
        this.m11 = m11;
        this.properties &= -17;
        if (m11 != 1.0) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3d m12(double m12) {
        this.m12 = m12;
        this.properties &= -17;
        if (m12 != 0.0) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3d m20(double m20) {
        this.m20 = m20;
        this.properties &= -17;
        if (m20 != 0.0) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3d m21(double m21) {
        this.m21 = m21;
        this.properties &= -17;
        if (m21 != 0.0) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3d m22(double m22) {
        this.m22 = m22;
        this.properties &= -17;
        if (m22 != 1.0) {
            this.properties &= -13;
        }

        return this;
    }

    public Matrix4x3d m30(double m30) {
        this.m30 = m30;
        if (m30 != 0.0) {
            this.properties &= -5;
        }

        return this;
    }

    public Matrix4x3d m31(double m31) {
        this.m31 = m31;
        if (m31 != 0.0) {
            this.properties &= -5;
        }

        return this;
    }

    public Matrix4x3d m32(double m32) {
        this.m32 = m32;
        if (m32 != 0.0) {
            this.properties &= -5;
        }

        return this;
    }

    public Matrix4x3d identity() {
        if ((this.properties & 4) == 0) {
            this.m00 = 1.0;
            this.m01 = 0.0;
            this.m02 = 0.0;
            this.m10 = 0.0;
            this.m11 = 1.0;
            this.m12 = 0.0;
            this.m20 = 0.0;
            this.m21 = 0.0;
            this.m22 = 1.0;
            this.m30 = 0.0;
            this.m31 = 0.0;
            this.m32 = 0.0;
            this.properties = 28;
        }
        return this;
    }

    public Matrix4x3d set(Matrix4x3d m) {
        this.m00 = m.m00();
        this.m01 = m.m01();
        this.m02 = m.m02();
        this.m10 = m.m10();
        this.m11 = m.m11();
        this.m12 = m.m12();
        this.m20 = m.m20();
        this.m21 = m.m21();
        this.m22 = m.m22();
        this.m30 = m.m30();
        this.m31 = m.m31();
        this.m32 = m.m32();
        this.properties = m.properties();
        return this;
    }

    public Matrix4x3d set(Matrix4x3f m) {
        this.m00 = m.m00();
        this.m01 = m.m01();
        this.m02 = m.m02();
        this.m10 = m.m10();
        this.m11 = m.m11();
        this.m12 = m.m12();
        this.m20 = m.m20();
        this.m21 = m.m21();
        this.m22 = m.m22();
        this.m30 = m.m30();
        this.m31 = m.m31();
        this.m32 = m.m32();
        this.properties = m.properties();
        return this;
    }

    public Matrix4x3d set(Matrix4d m) {
        this.m00 = m.m00();
        this.m01 = m.m01();
        this.m02 = m.m02();
        this.m10 = m.m10();
        this.m11 = m.m11();
        this.m12 = m.m12();
        this.m20 = m.m20();
        this.m21 = m.m21();
        this.m22 = m.m22();
        this.m30 = m.m30();
        this.m31 = m.m31();
        this.m32 = m.m32();
        this.properties = m.properties() & 28;
        return this;
    }

    public Matrix4d get(Matrix4d dest) {
        return dest.set4x3(this);
    }

    public Matrix4x3d set(Matrix3d mat) {
        this.m00 = mat.m00();
        this.m01 = mat.m01();
        this.m02 = mat.m02();
        this.m10 = mat.m10();
        this.m11 = mat.m11();
        this.m12 = mat.m12();
        this.m20 = mat.m20();
        this.m21 = mat.m21();
        this.m22 = mat.m22();
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        return this.determineProperties();
    }

    public Matrix4x3d set(Matrix3f mat) {
        this.m00 = mat.m00();
        this.m01 = mat.m01();
        this.m02 = mat.m02();
        this.m10 = mat.m10();
        this.m11 = mat.m11();
        this.m12 = mat.m12();
        this.m20 = mat.m20();
        this.m21 = mat.m21();
        this.m22 = mat.m22();
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        return this.determineProperties();
    }

    public Matrix4x3d set(Vector3d col0, Vector3d col1, Vector3d col2, Vector3d col3) {
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

    public Matrix4x3d set3x3(Matrix4x3d mat) {
        this.m00 = mat.m00();
        this.m01 = mat.m01();
        this.m02 = mat.m02();
        this.m10 = mat.m10();
        this.m11 = mat.m11();
        this.m12 = mat.m12();
        this.m20 = mat.m20();
        this.m21 = mat.m21();
        this.m22 = mat.m22();
        this.properties &= mat.properties();
        return this;
    }

    public Matrix4x3d set(AxisAngle4f axisAngle) {
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
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.properties = 16;
        return this;
    }

    public Matrix4x3d set(AxisAngle4d axisAngle) {
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
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.properties = 16;
        return this;
    }

    public Matrix4x3d set(Quaternionf q) {
        return this.rotation(q);
    }

    public Matrix4x3d set(Quaterniond q) {
        return this.rotation(q);
    }

    public Matrix4x3d mul(Matrix4x3d right) {
        return this.mul(right, this);
    }

    public Matrix4x3d mul(Matrix4x3d right, Matrix4x3d dest) {
        if ((this.properties & 4) != 0) {
            return dest.set(right);
        } else if ((right.properties() & 4) != 0) {
            return dest.set(this);
        } else {
            return (this.properties & 8) != 0 ? this.mulTranslation(right, dest) : this.mulGeneric(right, dest);
        }
    }

    private Matrix4x3d mulGeneric(Matrix4x3d right, Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        double m20 = this.m20;
        double m21 = this.m21;
        double m22 = this.m22;
        double rm00 = right.m00();
        double rm01 = right.m01();
        double rm02 = right.m02();
        double rm10 = right.m10();
        double rm11 = right.m11();
        double rm12 = right.m12();
        double rm20 = right.m20();
        double rm21 = right.m21();
        double rm22 = right.m22();
        double rm30 = right.m30();
        double rm31 = right.m31();
        double rm32 = right.m32();
        return dest._m00(Math.fma(m00, rm00, Math.fma(m10, rm01, m20 * rm02)))._m01(Math.fma(m01, rm00, Math.fma(m11, rm01, m21 * rm02)))._m02(Math.fma(m02, rm00, Math.fma(m12, rm01, m22 * rm02)))._m10(Math.fma(m00, rm10, Math.fma(m10, rm11, m20 * rm12)))._m11(Math.fma(m01, rm10, Math.fma(m11, rm11, m21 * rm12)))._m12(Math.fma(m02, rm10, Math.fma(m12, rm11, m22 * rm12)))._m20(Math.fma(m00, rm20, Math.fma(m10, rm21, m20 * rm22)))._m21(Math.fma(m01, rm20, Math.fma(m11, rm21, m21 * rm22)))._m22(Math.fma(m02, rm20, Math.fma(m12, rm21, m22 * rm22)))._m30(Math.fma(m00, rm30, Math.fma(m10, rm31, Math.fma(m20, rm32, this.m30))))._m31(Math.fma(m01, rm30, Math.fma(m11, rm31, Math.fma(m21, rm32, this.m31))))._m32(Math.fma(m02, rm30, Math.fma(m12, rm31, Math.fma(m22, rm32, this.m32))))._properties(this.properties & right.properties() & 16);
    }

    public Matrix4x3d mul(Matrix4x3f right) {
        return this.mul(right, this);
    }

    public Matrix4x3d mul(Matrix4x3f right, Matrix4x3d dest) {
        if ((this.properties & 4) != 0) {
            return dest.set(right);
        } else if ((right.properties() & 4) != 0) {
            return dest.set(this);
        } else {
            return (this.properties & 8) != 0 ? this.mulTranslation(right, dest) : this.mulGeneric(right, dest);
        }
    }

    private Matrix4x3d mulGeneric(Matrix4x3f right, Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        double m20 = this.m20;
        double m21 = this.m21;
        double m22 = this.m22;
        double rm00 = right.m00();
        double rm01 = right.m01();
        double rm02 = right.m02();
        double rm10 = right.m10();
        double rm11 = right.m11();
        double rm12 = right.m12();
        double rm20 = right.m20();
        double rm21 = right.m21();
        double rm22 = right.m22();
        double rm30 = right.m30();
        double rm31 = right.m31();
        double rm32 = right.m32();
        return dest._m00(Math.fma(m00, rm00, Math.fma(m10, rm01, m20 * rm02)))._m01(Math.fma(m01, rm00, Math.fma(m11, rm01, m21 * rm02)))._m02(Math.fma(m02, rm00, Math.fma(m12, rm01, m22 * rm02)))._m10(Math.fma(m00, rm10, Math.fma(m10, rm11, m20 * rm12)))._m11(Math.fma(m01, rm10, Math.fma(m11, rm11, m21 * rm12)))._m12(Math.fma(m02, rm10, Math.fma(m12, rm11, m22 * rm12)))._m20(Math.fma(m00, rm20, Math.fma(m10, rm21, m20 * rm22)))._m21(Math.fma(m01, rm20, Math.fma(m11, rm21, m21 * rm22)))._m22(Math.fma(m02, rm20, Math.fma(m12, rm21, m22 * rm22)))._m30(Math.fma(m00, rm30, Math.fma(m10, rm31, Math.fma(m20, rm32, this.m30))))._m31(Math.fma(m01, rm30, Math.fma(m11, rm31, Math.fma(m21, rm32, this.m31))))._m32(Math.fma(m02, rm30, Math.fma(m12, rm31, Math.fma(m22, rm32, this.m32))))._properties(this.properties & right.properties() & 16);
    }

    public Matrix4x3d mulTranslation(Matrix4x3d right, Matrix4x3d dest) {
        return dest._m00(right.m00())._m01(right.m01())._m02(right.m02())._m10(right.m10())._m11(right.m11())._m12(right.m12())._m20(right.m20())._m21(right.m21())._m22(right.m22())._m30(right.m30() + this.m30)._m31(right.m31() + this.m31)._m32(right.m32() + this.m32)._properties(right.properties() & 16);
    }

    public Matrix4x3d mulTranslation(Matrix4x3f right, Matrix4x3d dest) {
        return dest._m00(right.m00())._m01(right.m01())._m02(right.m02())._m10(right.m10())._m11(right.m11())._m12(right.m12())._m20(right.m20())._m21(right.m21())._m22(right.m22())._m30((double) right.m30() + this.m30)._m31((double) right.m31() + this.m31)._m32((double) right.m32() + this.m32)._properties(right.properties() & 16);
    }

    public Matrix4x3d mulOrtho(Matrix4x3d view) {
        return this.mulOrtho(view, this);
    }

    public Matrix4x3d mulOrtho(Matrix4x3d view, Matrix4x3d dest) {
        double nm00 = this.m00 * view.m00();
        double nm01 = this.m11 * view.m01();
        double nm02 = this.m22 * view.m02();
        double nm10 = this.m00 * view.m10();
        double nm11 = this.m11 * view.m11();
        double nm12 = this.m22 * view.m12();
        double nm20 = this.m00 * view.m20();
        double nm21 = this.m11 * view.m21();
        double nm22 = this.m22 * view.m22();
        double nm30 = this.m00 * view.m30() + this.m30;
        double nm31 = this.m11 * view.m31() + this.m31;
        double nm32 = this.m22 * view.m32() + this.m32;
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

    public Matrix4x3d mul3x3(double rm00, double rm01, double rm02, double rm10, double rm11, double rm12, double rm20, double rm21, double rm22) {
        return this.mul3x3(rm00, rm01, rm02, rm10, rm11, rm12, rm20, rm21, rm22, this);
    }

    public Matrix4x3d mul3x3(double rm00, double rm01, double rm02, double rm10, double rm11, double rm12, double rm20, double rm21, double rm22, Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        double m20 = this.m20;
        double m21 = this.m21;
        double m22 = this.m22;
        return dest._m00(Math.fma(m00, rm00, Math.fma(m10, rm01, m20 * rm02)))._m01(Math.fma(m01, rm00, Math.fma(m11, rm01, m21 * rm02)))._m02(Math.fma(m02, rm00, Math.fma(m12, rm01, m22 * rm02)))._m10(Math.fma(m00, rm10, Math.fma(m10, rm11, m20 * rm12)))._m11(Math.fma(m01, rm10, Math.fma(m11, rm11, m21 * rm12)))._m12(Math.fma(m02, rm10, Math.fma(m12, rm11, m22 * rm12)))._m20(Math.fma(m00, rm20, Math.fma(m10, rm21, m20 * rm22)))._m21(Math.fma(m01, rm20, Math.fma(m11, rm21, m21 * rm22)))._m22(Math.fma(m02, rm20, Math.fma(m12, rm21, m22 * rm22)))._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(0);
    }

    public Matrix4x3d fma(Matrix4x3d other, double otherFactor) {
        return this.fma(other, otherFactor, this);
    }

    public Matrix4x3d fma(Matrix4x3d other, double otherFactor, Matrix4x3d dest) {
        dest._m00(Math.fma(other.m00(), otherFactor, this.m00))._m01(Math.fma(other.m01(), otherFactor, this.m01))._m02(Math.fma(other.m02(), otherFactor, this.m02))._m10(Math.fma(other.m10(), otherFactor, this.m10))._m11(Math.fma(other.m11(), otherFactor, this.m11))._m12(Math.fma(other.m12(), otherFactor, this.m12))._m20(Math.fma(other.m20(), otherFactor, this.m20))._m21(Math.fma(other.m21(), otherFactor, this.m21))._m22(Math.fma(other.m22(), otherFactor, this.m22))._m30(Math.fma(other.m30(), otherFactor, this.m30))._m31(Math.fma(other.m31(), otherFactor, this.m31))._m32(Math.fma(other.m32(), otherFactor, this.m32))._properties(0);
        return dest;
    }

    public Matrix4x3d fma(Matrix4x3f other, double otherFactor) {
        return this.fma(other, otherFactor, this);
    }

    public Matrix4x3d fma(Matrix4x3f other, double otherFactor, Matrix4x3d dest) {
        dest._m00(Math.fma(other.m00(), otherFactor, this.m00))._m01(Math.fma(other.m01(), otherFactor, this.m01))._m02(Math.fma(other.m02(), otherFactor, this.m02))._m10(Math.fma(other.m10(), otherFactor, this.m10))._m11(Math.fma(other.m11(), otherFactor, this.m11))._m12(Math.fma(other.m12(), otherFactor, this.m12))._m20(Math.fma(other.m20(), otherFactor, this.m20))._m21(Math.fma(other.m21(), otherFactor, this.m21))._m22(Math.fma(other.m22(), otherFactor, this.m22))._m30(Math.fma(other.m30(), otherFactor, this.m30))._m31(Math.fma(other.m31(), otherFactor, this.m31))._m32(Math.fma(other.m32(), otherFactor, this.m32))._properties(0);
        return dest;
    }

    public Matrix4x3d add(Matrix4x3d other) {
        return this.add(other, this);
    }

    public Matrix4x3d add(Matrix4x3d other, Matrix4x3d dest) {
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

    public Matrix4x3d add(Matrix4x3f other) {
        return this.add(other, this);
    }

    public Matrix4x3d add(Matrix4x3f other, Matrix4x3d dest) {
        dest.m00 = this.m00 + (double) other.m00;
        dest.m01 = this.m01 + (double) other.m01;
        dest.m02 = this.m02 + (double) other.m02;
        dest.m10 = this.m10 + (double) other.m10;
        dest.m11 = this.m11 + (double) other.m11;
        dest.m12 = this.m12 + (double) other.m12;
        dest.m20 = this.m20 + (double) other.m20;
        dest.m21 = this.m21 + (double) other.m21;
        dest.m22 = this.m22 + (double) other.m22;
        dest.m30 = this.m30 + (double) other.m30;
        dest.m31 = this.m31 + (double) other.m31;
        dest.m32 = this.m32 + (double) other.m32;
        dest.properties = 0;
        return dest;
    }

    public Matrix4x3d sub(Matrix4x3d subtrahend) {
        return this.sub(subtrahend, this);
    }

    public Matrix4x3d sub(Matrix4x3d subtrahend, Matrix4x3d dest) {
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

    public Matrix4x3d sub(Matrix4x3f subtrahend) {
        return this.sub(subtrahend, this);
    }

    public Matrix4x3d sub(Matrix4x3f subtrahend, Matrix4x3d dest) {
        dest.m00 = this.m00 - (double) subtrahend.m00;
        dest.m01 = this.m01 - (double) subtrahend.m01;
        dest.m02 = this.m02 - (double) subtrahend.m02;
        dest.m10 = this.m10 - (double) subtrahend.m10;
        dest.m11 = this.m11 - (double) subtrahend.m11;
        dest.m12 = this.m12 - (double) subtrahend.m12;
        dest.m20 = this.m20 - (double) subtrahend.m20;
        dest.m21 = this.m21 - (double) subtrahend.m21;
        dest.m22 = this.m22 - (double) subtrahend.m22;
        dest.m30 = this.m30 - (double) subtrahend.m30;
        dest.m31 = this.m31 - (double) subtrahend.m31;
        dest.m32 = this.m32 - (double) subtrahend.m32;
        dest.properties = 0;
        return dest;
    }

    public Matrix4x3d mulComponentWise(Matrix4x3d other) {
        return this.mulComponentWise(other, this);
    }

    public Matrix4x3d mulComponentWise(Matrix4x3d other, Matrix4x3d dest) {
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

    public Matrix4x3d set(double m00, double m01, double m02, double m10, double m11, double m12, double m20, double m21, double m22, double m30, double m31, double m32) {
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
        return this.determineProperties();
    }

    public Matrix4x3d set(double[] m, int off) {
        this.m00 = m[off];
        this.m01 = m[off + 1];
        this.m02 = m[off + 2];
        this.m10 = m[off + 3];
        this.m11 = m[off + 4];
        this.m12 = m[off + 5];
        this.m20 = m[off + 6];
        this.m21 = m[off + 7];
        this.m22 = m[off + 8];
        this.m30 = m[off + 9];
        this.m31 = m[off + 10];
        this.m32 = m[off + 11];
        return this.determineProperties();
    }

    public Matrix4x3d set(double[] m) {
        return this.set(m, 0);
    }

    public Matrix4x3d set(float[] m, int off) {
        this.m00 = m[off];
        this.m01 = m[off + 1];
        this.m02 = m[off + 2];
        this.m10 = m[off + 3];
        this.m11 = m[off + 4];
        this.m12 = m[off + 5];
        this.m20 = m[off + 6];
        this.m21 = m[off + 7];
        this.m22 = m[off + 8];
        this.m30 = m[off + 9];
        this.m31 = m[off + 10];
        this.m32 = m[off + 11];
        return this.determineProperties();
    }

    public Matrix4x3d set(float[] m) {
        return this.set(m, 0);
    }

    public double determinant() {
        return (this.m00 * this.m11 - this.m01 * this.m10) * this.m22 + (this.m02 * this.m10 - this.m00 * this.m12) * this.m21 + (this.m01 * this.m12 - this.m02 * this.m11) * this.m20;
    }

    public Matrix4x3d invert() {
        return this.invert(this);
    }

    public Matrix4x3d invert(Matrix4x3d dest) {
        if ((this.properties & 4) != 0) {
            return dest.identity();
        } else {
            return (this.properties & 16) != 0 ? this.invertOrthonormal(dest) : this.invertGeneric(dest);
        }
    }

    private Matrix4x3d invertGeneric(Matrix4x3d dest) {
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

    private Matrix4x3d invertOrthonormal(Matrix4x3d dest) {
        double nm30 = -(this.m00 * this.m30 + this.m01 * this.m31 + this.m02 * this.m32);
        double nm31 = -(this.m10 * this.m30 + this.m11 * this.m31 + this.m12 * this.m32);
        double nm32 = -(this.m20 * this.m30 + this.m21 * this.m31 + this.m22 * this.m32);
        double m01 = this.m01;
        double m02 = this.m02;
        double m12 = this.m12;
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

    public Matrix4x3d invertOrtho(Matrix4x3d dest) {
        double invM00 = 1.0 / this.m00;
        double invM11 = 1.0 / this.m11;
        double invM22 = 1.0 / this.m22;
        dest.set(invM00, 0.0, 0.0, 0.0, invM11, 0.0, 0.0, 0.0, invM22, -this.m30 * invM00, -this.m31 * invM11, -this.m32 * invM22);
        dest.properties = 0;
        return dest;
    }

    public Matrix4x3d invertOrtho() {
        return this.invertOrtho(this);
    }

    public Matrix4x3d transpose3x3() {
        return this.transpose3x3(this);
    }

    public Matrix4x3d transpose3x3(Matrix4x3d dest) {
        double nm00 = this.m00;
        double nm01 = this.m10;
        double nm02 = this.m20;
        double nm10 = this.m01;
        double nm11 = this.m11;
        double nm12 = this.m21;
        double nm20 = this.m02;
        double nm21 = this.m12;
        double nm22 = this.m22;
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

    public Matrix3d transpose3x3(Matrix3d dest) {
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

    public Matrix4x3d translation(double x, double y, double z) {
        if ((this.properties & 4) == 0) {
            this.identity();
        }

        this.m30 = x;
        this.m31 = y;
        this.m32 = z;
        this.properties = 24;
        return this;
    }

    public Matrix4x3d translation(Vector3f offset) {
        return this.translation(offset.x, offset.y, offset.z);
    }

    public Matrix4x3d translation(Vector3d offset) {
        return this.translation(offset.x, offset.y, offset.z);
    }

    public Matrix4x3d setTranslation(double x, double y, double z) {
        this.m30 = x;
        this.m31 = y;
        this.m32 = z;
        this.properties &= -5;
        return this;
    }

    public Matrix4x3d setTranslation(Vector3d xyz) {
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

    public Matrix4x3d get(Matrix4x3d dest) {
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
        arr[offset + 9] = this.m30;
        arr[offset + 10] = this.m31;
        arr[offset + 11] = this.m32;
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
        arr[offset + 9] = (float) this.m30;
        arr[offset + 10] = (float) this.m31;
        arr[offset + 11] = (float) this.m32;
        return arr;
    }

    public float[] get(float[] arr) {
        return this.get(arr, 0);
    }

    public float[] get4x4(float[] arr, int offset) {
        MemUtil.INSTANCE.copy4x4(this, arr, offset);
        return arr;
    }

    public float[] get4x4(float[] arr) {
        return this.get4x4(arr, 0);
    }

    public double[] get4x4(double[] arr, int offset) {
        MemUtil.INSTANCE.copy4x4(this, arr, offset);
        return arr;
    }

    public double[] get4x4(double[] arr) {
        return this.get4x4(arr, 0);
    }

    public double[] getTransposed(double[] arr, int offset) {
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

    public double[] getTransposed(double[] arr) {
        return this.getTransposed(arr, 0);
    }

    public Matrix4x3d zero() {
        this.m00 = 0.0;
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m10 = 0.0;
        this.m11 = 0.0;
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = 0.0;
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.properties = 0;
        return this;
    }

    public Matrix4x3d scaling(double factor) {
        return this.scaling(factor, factor, factor);
    }

    public Matrix4x3d scaling(double x, double y, double z) {
        if ((this.properties & 4) == 0) {
            this.identity();
        }

        this.m00 = x;
        this.m11 = y;
        this.m22 = z;
        boolean one = Math.absEqualsOne(x) && Math.absEqualsOne(y) && Math.absEqualsOne(z);
        this.properties = one ? 16 : 0;
        return this;
    }

    public Matrix4x3d scaling(Vector3d xyz) {
        return this.scaling(xyz.x, xyz.y, xyz.z);
    }

    public Matrix4x3d rotation(double angle, double x, double y, double z) {
        if (y == 0.0 && z == 0.0 && Math.absEqualsOne(x)) {
            return this.rotationX(x * angle);
        } else if (x == 0.0 && z == 0.0 && Math.absEqualsOne(y)) {
            return this.rotationY(y * angle);
        } else {
            return x == 0.0 && y == 0.0 && Math.absEqualsOne(z) ? this.rotationZ(z * angle) : this.rotationInternal(angle, x, y, z);
        }
    }

    private Matrix4x3d rotationInternal(double angle, double x, double y, double z) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double C = 1.0 - cos;
        double xy = x * y;
        double xz = x * z;
        double yz = y * z;
        this.m00 = cos + x * x * C;
        this.m01 = xy * C + z * sin;
        this.m02 = xz * C - y * sin;
        this.m10 = xy * C - z * sin;
        this.m11 = cos + y * y * C;
        this.m12 = yz * C + x * sin;
        this.m20 = xz * C + y * sin;
        this.m21 = yz * C - x * sin;
        this.m22 = cos + z * z * C;
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.properties = 16;
        return this;
    }

    public Matrix4x3d rotationX(double ang) {
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
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.properties = 16;
        return this;
    }

    public Matrix4x3d rotationY(double ang) {
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
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.properties = 16;
        return this;
    }

    public Matrix4x3d rotationZ(double ang) {
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
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.properties = 16;
        return this;
    }

    public Matrix4x3d rotationXYZ(double angleX, double angleY, double angleZ) {
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
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.properties = 16;
        return this;
    }

    public Matrix4x3d rotationZYX(double angleZ, double angleY, double angleX) {
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
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.properties = 16;
        return this;
    }

    public Matrix4x3d rotationYXZ(double angleY, double angleX, double angleZ) {
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
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.properties = 16;
        return this;
    }

    public Matrix4x3d setRotationXYZ(double angleX, double angleY, double angleZ) {
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
        this.properties &= -13;
        return this;
    }

    public Matrix4x3d setRotationZYX(double angleZ, double angleY, double angleX) {
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
        this.properties &= -13;
        return this;
    }

    public Matrix4x3d setRotationYXZ(double angleY, double angleX, double angleZ) {
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
        this.properties &= -13;
        return this;
    }

    public Matrix4x3d rotation(double angle, Vector3d axis) {
        return this.rotation(angle, axis.x, axis.y, axis.z);
    }

    public Matrix4x3d rotation(double angle, Vector3f axis) {
        return this.rotation(angle, axis.x, axis.y, axis.z);
    }

    public Vector4d transform(Vector4d v) {
        return v.mul(this);
    }

    public Vector4d transform(Vector4d v, Vector4d dest) {
        return v.mul(this, dest);
    }

    public Vector3d transformPosition(Vector3d v) {
        v.set(this.m00 * v.x + this.m10 * v.y + this.m20 * v.z + this.m30, this.m01 * v.x + this.m11 * v.y + this.m21 * v.z + this.m31, this.m02 * v.x + this.m12 * v.y + this.m22 * v.z + this.m32);
        return v;
    }

    public Vector3d transformPosition(Vector3d v, Vector3d dest) {
        dest.set(this.m00 * v.x + this.m10 * v.y + this.m20 * v.z + this.m30, this.m01 * v.x + this.m11 * v.y + this.m21 * v.z + this.m31, this.m02 * v.x + this.m12 * v.y + this.m22 * v.z + this.m32);
        return dest;
    }

    public Vector3d transformDirection(Vector3d v) {
        v.set(this.m00 * v.x + this.m10 * v.y + this.m20 * v.z, this.m01 * v.x + this.m11 * v.y + this.m21 * v.z, this.m02 * v.x + this.m12 * v.y + this.m22 * v.z);
        return v;
    }

    public Vector3d transformDirection(Vector3d v, Vector3d dest) {
        dest.set(this.m00 * v.x + this.m10 * v.y + this.m20 * v.z, this.m01 * v.x + this.m11 * v.y + this.m21 * v.z, this.m02 * v.x + this.m12 * v.y + this.m22 * v.z);
        return dest;
    }

    public Matrix4x3d set3x3(Matrix3d mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m02 = mat.m02;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
        this.m12 = mat.m12;
        this.m20 = mat.m20;
        this.m21 = mat.m21;
        this.m22 = mat.m22;
        this.properties = 0;
        return this;
    }

    public Matrix4x3d set3x3(Matrix3f mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m02 = mat.m02;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
        this.m12 = mat.m12;
        this.m20 = mat.m20;
        this.m21 = mat.m21;
        this.m22 = mat.m22;
        this.properties = 0;
        return this;
    }

    public Matrix4x3d scale(Vector3d xyz, Matrix4x3d dest) {
        return this.scale(xyz.x, xyz.y, xyz.z, dest);
    }

    public Matrix4x3d scale(Vector3d xyz) {
        return this.scale(xyz.x, xyz.y, xyz.z, this);
    }

    public Matrix4x3d scale(double x, double y, double z, Matrix4x3d dest) {
        return (this.properties & 4) != 0 ? dest.scaling(x, y, z) : this.scaleGeneric(x, y, z, dest);
    }

    private Matrix4x3d scaleGeneric(double x, double y, double z, Matrix4x3d dest) {
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

    public Matrix4x3d scale(double x, double y, double z) {
        return this.scale(x, y, z, this);
    }

    public Matrix4x3d scale(double xyz, Matrix4x3d dest) {
        return this.scale(xyz, xyz, xyz, dest);
    }

    public Matrix4x3d scale(double xyz) {
        return this.scale(xyz, xyz, xyz);
    }

    public Matrix4x3d scaleXY(double x, double y, Matrix4x3d dest) {
        return this.scale(x, y, 1.0, dest);
    }

    public Matrix4x3d scaleXY(double x, double y) {
        return this.scale(x, y, 1.0);
    }

    public Matrix4x3d scaleAround(double sx, double sy, double sz, double ox, double oy, double oz, Matrix4x3d dest) {
        double nm30 = this.m00 * ox + this.m10 * oy + this.m20 * oz + this.m30;
        double nm31 = this.m01 * ox + this.m11 * oy + this.m21 * oz + this.m31;
        double nm32 = this.m02 * ox + this.m12 * oy + this.m22 * oz + this.m32;
        boolean one = Math.absEqualsOne(sx) && Math.absEqualsOne(sy) && Math.absEqualsOne(sz);
        return dest._m00(this.m00 * sx)._m01(this.m01 * sx)._m02(this.m02 * sx)._m10(this.m10 * sy)._m11(this.m11 * sy)._m12(this.m12 * sy)._m20(this.m20 * sz)._m21(this.m21 * sz)._m22(this.m22 * sz)._m30(-dest.m00 * ox - dest.m10 * oy - dest.m20 * oz + nm30)._m31(-dest.m01 * ox - dest.m11 * oy - dest.m21 * oz + nm31)._m32(-dest.m02 * ox - dest.m12 * oy - dest.m22 * oz + nm32)._properties(this.properties & ~(12 | (one ? 0 : 16)));
    }

    public Matrix4x3d scaleAround(double sx, double sy, double sz, double ox, double oy, double oz) {
        return this.scaleAround(sx, sy, sz, ox, oy, oz, this);
    }

    public Matrix4x3d scaleAround(double factor, double ox, double oy, double oz) {
        return this.scaleAround(factor, factor, factor, ox, oy, oz, this);
    }

    public Matrix4x3d scaleAround(double factor, double ox, double oy, double oz, Matrix4x3d dest) {
        return this.scaleAround(factor, factor, factor, ox, oy, oz, dest);
    }

    public Matrix4x3d scaleLocal(double x, double y, double z, Matrix4x3d dest) {
        if ((this.properties & 4) != 0) {
            return dest.scaling(x, y, z);
        } else {
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

    public Matrix4x3d scaleLocal(double x, double y, double z) {
        return this.scaleLocal(x, y, z, this);
    }

    public Matrix4x3d rotate(double ang, double x, double y, double z, Matrix4x3d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotation(ang, x, y, z);
        } else {
            return (this.properties & 8) != 0 ? this.rotateTranslation(ang, x, y, z, dest) : this.rotateGeneric(ang, x, y, z, dest);
        }
    }

    private Matrix4x3d rotateGeneric(double ang, double x, double y, double z, Matrix4x3d dest) {
        if (y == 0.0 && z == 0.0 && Math.absEqualsOne(x)) {
            return this.rotateX(x * ang, dest);
        } else if (x == 0.0 && z == 0.0 && Math.absEqualsOne(y)) {
            return this.rotateY(y * ang, dest);
        } else {
            return x == 0.0 && y == 0.0 && Math.absEqualsOne(z) ? this.rotateZ(z * ang, dest) : this.rotateGenericInternal(ang, x, y, z, dest);
        }
    }

    private Matrix4x3d rotateGenericInternal(double ang, double x, double y, double z, Matrix4x3d dest) {
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
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3d rotate(double ang, double x, double y, double z) {
        return this.rotate(ang, x, y, z, this);
    }

    public Matrix4x3d rotateTranslation(double ang, double x, double y, double z, Matrix4x3d dest) {
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

    private Matrix4x3d rotateTranslationInternal(double ang, double x, double y, double z, Matrix4x3d dest) {
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

    public Matrix4x3d rotateAround(Quaterniond quat, double ox, double oy, double oz) {
        return this.rotateAround(quat, ox, oy, oz, this);
    }

    private Matrix4x3d rotateAroundAffine(Quaterniond quat, double ox, double oy, double oz, Matrix4x3d dest) {
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
        double tm30 = this.m00 * ox + this.m10 * oy + this.m20 * oz + this.m30;
        double tm31 = this.m01 * ox + this.m11 * oy + this.m21 * oz + this.m31;
        double tm32 = this.m02 * ox + this.m12 * oy + this.m22 * oz + this.m32;
        double nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        double nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        double nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        dest._m20(this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22)._m21(this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22)._m22(this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m10(nm10)._m11(nm11)._m12(nm12)._m30(-nm00 * ox - nm10 * oy - this.m20 * oz + tm30)._m31(-nm01 * ox - nm11 * oy - this.m21 * oz + tm31)._m32(-nm02 * ox - nm12 * oy - this.m22 * oz + tm32)._properties(this.properties & -13);
        return dest;
    }

    public Matrix4x3d rotateAround(Quaterniond quat, double ox, double oy, double oz, Matrix4x3d dest) {
        return (this.properties & 4) != 0 ? this.rotationAround(quat, ox, oy, oz) : this.rotateAroundAffine(quat, ox, oy, oz, dest);
    }

    public Matrix4x3d rotationAround(Quaterniond quat, double ox, double oy, double oz) {
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

    public Matrix4x3d rotateLocal(double ang, double x, double y, double z, Matrix4x3d dest) {
        if (y == 0.0 && z == 0.0 && Math.absEqualsOne(x)) {
            return this.rotateLocalX(x * ang, dest);
        } else if (x == 0.0 && z == 0.0 && Math.absEqualsOne(y)) {
            return this.rotateLocalY(y * ang, dest);
        } else {
            return x == 0.0 && y == 0.0 && Math.absEqualsOne(z) ? this.rotateLocalZ(z * ang, dest) : this.rotateLocalInternal(ang, x, y, z, dest);
        }
    }

    private Matrix4x3d rotateLocalInternal(double ang, double x, double y, double z, Matrix4x3d dest) {
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

    public Matrix4x3d rotateLocal(double ang, double x, double y, double z) {
        return this.rotateLocal(ang, x, y, z, this);
    }

    public Matrix4x3d rotateLocalX(double ang, Matrix4x3d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        double nm01 = cos * this.m01 - sin * this.m02;
        double nm02 = sin * this.m01 + cos * this.m02;
        double nm11 = cos * this.m11 - sin * this.m12;
        double nm12 = sin * this.m11 + cos * this.m12;
        double nm21 = cos * this.m21 - sin * this.m22;
        double nm22 = sin * this.m21 + cos * this.m22;
        double nm31 = cos * this.m31 - sin * this.m32;
        double nm32 = sin * this.m31 + cos * this.m32;
        dest.m00 = this.m00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m10 = this.m10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m20 = this.m20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        dest.m30 = this.m30;
        dest.m31 = nm31;
        dest.m32 = nm32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3d rotateLocalX(double ang) {
        return this.rotateLocalX(ang, this);
    }

    public Matrix4x3d rotateLocalY(double ang, Matrix4x3d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        double nm00 = cos * this.m00 + sin * this.m02;
        double nm02 = -sin * this.m00 + cos * this.m02;
        double nm10 = cos * this.m10 + sin * this.m12;
        double nm12 = -sin * this.m10 + cos * this.m12;
        double nm20 = cos * this.m20 + sin * this.m22;
        double nm22 = -sin * this.m20 + cos * this.m22;
        double nm30 = cos * this.m30 + sin * this.m32;
        double nm32 = -sin * this.m30 + cos * this.m32;
        dest.m00 = nm00;
        dest.m01 = this.m01;
        dest.m02 = nm02;
        dest.m10 = nm10;
        dest.m11 = this.m11;
        dest.m12 = nm12;
        dest.m20 = nm20;
        dest.m21 = this.m21;
        dest.m22 = nm22;
        dest.m30 = nm30;
        dest.m31 = this.m31;
        dest.m32 = nm32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3d rotateLocalY(double ang) {
        return this.rotateLocalY(ang, this);
    }

    public Matrix4x3d rotateLocalZ(double ang, Matrix4x3d dest) {
        double sin = Math.sin(ang);
        double cos = Math.cosFromSin(sin, ang);
        double nm00 = cos * this.m00 - sin * this.m01;
        double nm01 = sin * this.m00 + cos * this.m01;
        double nm10 = cos * this.m10 - sin * this.m11;
        double nm11 = sin * this.m10 + cos * this.m11;
        double nm20 = cos * this.m20 - sin * this.m21;
        double nm21 = sin * this.m20 + cos * this.m21;
        double nm30 = cos * this.m30 - sin * this.m31;
        double nm31 = sin * this.m30 + cos * this.m31;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = this.m02;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = this.m12;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = this.m22;
        dest.m30 = nm30;
        dest.m31 = nm31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3d rotateLocalZ(double ang) {
        return this.rotateLocalZ(ang, this);
    }

    public Matrix4x3d translate(Vector3d offset) {
        return this.translate(offset.x, offset.y, offset.z);
    }

    public Matrix4x3d translate(Vector3d offset, Matrix4x3d dest) {
        return this.translate(offset.x, offset.y, offset.z, dest);
    }

    public Matrix4x3d translate(Vector3f offset) {
        return this.translate(offset.x, offset.y, offset.z);
    }

    public Matrix4x3d translate(Vector3f offset, Matrix4x3d dest) {
        return this.translate(offset.x, offset.y, offset.z, dest);
    }

    public Matrix4x3d translate(double x, double y, double z, Matrix4x3d dest) {
        return (this.properties & 4) != 0 ? dest.translation(x, y, z) : this.translateGeneric(x, y, z, dest);
    }

    private Matrix4x3d translateGeneric(double x, double y, double z, Matrix4x3d dest) {
        dest.m00 = this.m00;
        dest.m01 = this.m01;
        dest.m02 = this.m02;
        dest.m10 = this.m10;
        dest.m11 = this.m11;
        dest.m12 = this.m12;
        dest.m20 = this.m20;
        dest.m21 = this.m21;
        dest.m22 = this.m22;
        dest.m30 = this.m00 * x + this.m10 * y + this.m20 * z + this.m30;
        dest.m31 = this.m01 * x + this.m11 * y + this.m21 * z + this.m31;
        dest.m32 = this.m02 * x + this.m12 * y + this.m22 * z + this.m32;
        dest.properties = this.properties & -5;
        return dest;
    }

    public Matrix4x3d translate(double x, double y, double z) {
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

    public Matrix4x3d translateLocal(Vector3f offset) {
        return this.translateLocal(offset.x, offset.y, offset.z);
    }

    public Matrix4x3d translateLocal(Vector3f offset, Matrix4x3d dest) {
        return this.translateLocal(offset.x, offset.y, offset.z, dest);
    }

    public Matrix4x3d translateLocal(Vector3d offset) {
        return this.translateLocal(offset.x, offset.y, offset.z);
    }

    public Matrix4x3d translateLocal(Vector3d offset, Matrix4x3d dest) {
        return this.translateLocal(offset.x, offset.y, offset.z, dest);
    }

    public Matrix4x3d translateLocal(double x, double y, double z, Matrix4x3d dest) {
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

    public Matrix4x3d translateLocal(double x, double y, double z) {
        return this.translateLocal(x, y, z, this);
    }

    public Matrix4x3d rotateX(double ang, Matrix4x3d dest) {
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

    private Matrix4x3d rotateXInternal(double ang, Matrix4x3d dest) {
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
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3d rotateX(double ang) {
        return this.rotateX(ang, this);
    }

    public Matrix4x3d rotateY(double ang, Matrix4x3d dest) {
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

    private Matrix4x3d rotateYInternal(double ang, Matrix4x3d dest) {
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
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3d rotateY(double ang) {
        return this.rotateY(ang, this);
    }

    public Matrix4x3d rotateZ(double ang, Matrix4x3d dest) {
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

    private Matrix4x3d rotateZInternal(double ang, Matrix4x3d dest) {
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
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3d rotateZ(double ang) {
        return this.rotateZ(ang, this);
    }

    public Matrix4x3d rotateXYZ(Vector3d angles) {
        return this.rotateXYZ(angles.x, angles.y, angles.z);
    }

    public Matrix4x3d rotateXYZ(double angleX, double angleY, double angleZ) {
        return this.rotateXYZ(angleX, angleY, angleZ, this);
    }

    public Matrix4x3d rotateXYZ(double angleX, double angleY, double angleZ, Matrix4x3d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationXYZ(angleX, angleY, angleZ);
        } else if ((this.properties & 8) != 0) {
            double tx = this.m30;
            double ty = this.m31;
            double tz = this.m32;
            return dest.rotationXYZ(angleX, angleY, angleZ).setTranslation(tx, ty, tz);
        } else {
            return this.rotateXYZInternal(angleX, angleY, angleZ, dest);
        }
    }

    private Matrix4x3d rotateXYZInternal(double angleX, double angleY, double angleZ, Matrix4x3d dest) {
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
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3d rotateZYX(Vector3d angles) {
        return this.rotateZYX(angles.z, angles.y, angles.x);
    }

    public Matrix4x3d rotateZYX(double angleZ, double angleY, double angleX) {
        return this.rotateZYX(angleZ, angleY, angleX, this);
    }

    public Matrix4x3d rotateZYX(double angleZ, double angleY, double angleX, Matrix4x3d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationZYX(angleZ, angleY, angleX);
        } else if ((this.properties & 8) != 0) {
            double tx = this.m30;
            double ty = this.m31;
            double tz = this.m32;
            return dest.rotationZYX(angleZ, angleY, angleX).setTranslation(tx, ty, tz);
        } else {
            return this.rotateZYXInternal(angleZ, angleY, angleX, dest);
        }
    }

    private Matrix4x3d rotateZYXInternal(double angleZ, double angleY, double angleX, Matrix4x3d dest) {
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
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3d rotateYXZ(Vector3d angles) {
        return this.rotateYXZ(angles.y, angles.x, angles.z);
    }

    public Matrix4x3d rotateYXZ(double angleY, double angleX, double angleZ) {
        return this.rotateYXZ(angleY, angleX, angleZ, this);
    }

    public Matrix4x3d rotateYXZ(double angleY, double angleX, double angleZ, Matrix4x3d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotationYXZ(angleY, angleX, angleZ);
        } else if ((this.properties & 8) != 0) {
            double tx = this.m30;
            double ty = this.m31;
            double tz = this.m32;
            return dest.rotationYXZ(angleY, angleX, angleZ).setTranslation(tx, ty, tz);
        } else {
            return this.rotateYXZInternal(angleY, angleX, angleZ, dest);
        }
    }

    private Matrix4x3d rotateYXZInternal(double angleY, double angleX, double angleZ, Matrix4x3d dest) {
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
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3d rotation(AxisAngle4f angleAxis) {
        return this.rotation(angleAxis.angle, angleAxis.x, angleAxis.y, angleAxis.z);
    }

    public Matrix4x3d rotation(AxisAngle4d angleAxis) {
        return this.rotation(angleAxis.angle, angleAxis.x, angleAxis.y, angleAxis.z);
    }

    public Matrix4x3d rotation(Quaterniond quat) {
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
        this._m00(w2 + x2 - z2 - y2);
        this._m01(dxy + dzw);
        this._m02(dxz - dyw);
        this._m10(dxy - dzw);
        this._m11(y2 - z2 + w2 - x2);
        this._m12(dyz + dxw);
        this._m20(dyw + dxz);
        this._m21(dyz - dxw);
        this._m22(z2 - y2 - x2 + w2);
        this._m30(0.0);
        this._m31(0.0);
        this._m32(0.0);
        this.properties = 16;
        return this;
    }

    public Matrix4x3d rotation(Quaternionf quat) {
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
        this._m00(w2 + x2 - z2 - y2);
        this._m01(dxy + dzw);
        this._m02(dxz - dyw);
        this._m10(dxy - dzw);
        this._m11(y2 - z2 + w2 - x2);
        this._m12(dyz + dxw);
        this._m20(dyw + dxz);
        this._m21(dyz - dxw);
        this._m22(z2 - y2 - x2 + w2);
        this._m30(0.0);
        this._m31(0.0);
        this._m32(0.0);
        this.properties = 16;
        return this;
    }

    public Matrix4x3d translationRotateScale(double tx, double ty, double tz, double qx, double qy, double qz, double qw, double sx, double sy, double sz) {
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

    public Matrix4x3d translationRotateScale(Vector3f translation, Quaternionf quat, Vector3f scale) {
        return this.translationRotateScale(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale.x, scale.y, scale.z);
    }

    public Matrix4x3d translationRotateScale(Vector3d translation, Quaterniond quat, Vector3d scale) {
        return this.translationRotateScale(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale.x, scale.y, scale.z);
    }

    public Matrix4x3d translationRotateScaleMul(double tx, double ty, double tz, double qx, double qy, double qz, double qw, double sx, double sy, double sz, Matrix4x3d m) {
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
        double nm00 = sx - (q11 + q22) * sx;
        double nm01 = (q01 + q23) * sx;
        double nm02 = (q02 - q13) * sx;
        double nm10 = (q01 - q23) * sy;
        double nm11 = sy - (q22 + q00) * sy;
        double nm12 = (q12 + q03) * sy;
        double nm20 = (q02 + q13) * sz;
        double nm21 = (q12 - q03) * sz;
        double nm22 = sz - (q11 + q00) * sz;
        double m00 = nm00 * m.m00 + nm10 * m.m01 + nm20 * m.m02;
        double m01 = nm01 * m.m00 + nm11 * m.m01 + nm21 * m.m02;
        this.m02 = nm02 * m.m00 + nm12 * m.m01 + nm22 * m.m02;
        this.m00 = m00;
        this.m01 = m01;
        double m10 = nm00 * m.m10 + nm10 * m.m11 + nm20 * m.m12;
        double m11 = nm01 * m.m10 + nm11 * m.m11 + nm21 * m.m12;
        this.m12 = nm02 * m.m10 + nm12 * m.m11 + nm22 * m.m12;
        this.m10 = m10;
        this.m11 = m11;
        double m20 = nm00 * m.m20 + nm10 * m.m21 + nm20 * m.m22;
        double m21 = nm01 * m.m20 + nm11 * m.m21 + nm21 * m.m22;
        this.m22 = nm02 * m.m20 + nm12 * m.m21 + nm22 * m.m22;
        this.m20 = m20;
        this.m21 = m21;
        double m30 = nm00 * m.m30 + nm10 * m.m31 + nm20 * m.m32 + tx;
        double m31 = nm01 * m.m30 + nm11 * m.m31 + nm21 * m.m32 + ty;
        this.m32 = nm02 * m.m30 + nm12 * m.m31 + nm22 * m.m32 + tz;
        this.m30 = m30;
        this.m31 = m31;
        this.properties = 0;
        return this;
    }

    public Matrix4x3d translationRotateScaleMul(Vector3d translation, Quaterniond quat, Vector3d scale, Matrix4x3d m) {
        return this.translationRotateScaleMul(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w, scale.x, scale.y, scale.z, m);
    }

    public Matrix4x3d translationRotate(double tx, double ty, double tz, Quaterniond quat) {
        double dqx = quat.x + quat.x;
        double dqy = quat.y + quat.y;
        double dqz = quat.z + quat.z;
        double q00 = dqx * quat.x;
        double q11 = dqy * quat.y;
        double q22 = dqz * quat.z;
        double q01 = dqx * quat.y;
        double q02 = dqx * quat.z;
        double q03 = dqx * quat.w;
        double q12 = dqy * quat.z;
        double q13 = dqy * quat.w;
        double q23 = dqz * quat.w;
        this.m00 = 1.0 - (q11 + q22);
        this.m01 = q01 + q23;
        this.m02 = q02 - q13;
        this.m10 = q01 - q23;
        this.m11 = 1.0 - (q22 + q00);
        this.m12 = q12 + q03;
        this.m20 = q02 + q13;
        this.m21 = q12 - q03;
        this.m22 = 1.0 - (q11 + q00);
        this.m30 = tx;
        this.m31 = ty;
        this.m32 = tz;
        this.properties = 16;
        return this;
    }

    public Matrix4x3d translationRotate(double tx, double ty, double tz, double qx, double qy, double qz, double qw) {
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
        this.properties = 16;
        return this;
    }

    public Matrix4x3d translationRotate(Vector3d translation, Quaterniond quat) {
        return this.translationRotate(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w);
    }

    public Matrix4x3d translationRotateMul(double tx, double ty, double tz, Quaternionf quat, Matrix4x3d mat) {
        return this.translationRotateMul(tx, ty, tz, quat.x, quat.y, quat.z, quat.w, mat);
    }

    public Matrix4x3d translationRotateMul(double tx, double ty, double tz, double qx, double qy, double qz, double qw, Matrix4x3d mat) {
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

    public Matrix4x3d translationRotateInvert(double tx, double ty, double tz, double qx, double qy, double qz, double qw) {
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
        return this._m00(1.0 - q11 - q22)._m01(q01 + q23)._m02(q02 - q13)._m10(q01 - q23)._m11(1.0 - q22 - q00)._m12(q12 + q03)._m20(q02 + q13)._m21(q12 - q03)._m22(1.0 - q11 - q00)._m30(-this.m00 * tx - this.m10 * ty - this.m20 * tz)._m31(-this.m01 * tx - this.m11 * ty - this.m21 * tz)._m32(-this.m02 * tx - this.m12 * ty - this.m22 * tz)._properties(16);
    }

    public Matrix4x3d translationRotateInvert(Vector3d translation, Quaterniond quat) {
        return this.translationRotateInvert(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w);
    }

    public Matrix4x3d rotate(Quaterniond quat, Matrix4x3d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotation(quat);
        } else {
            return (this.properties & 8) != 0 ? this.rotateTranslation(quat, dest) : this.rotateGeneric(quat, dest);
        }
    }

    private Matrix4x3d rotateGeneric(Quaterniond quat, Matrix4x3d dest) {
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
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3d rotate(Quaternionf quat, Matrix4x3d dest) {
        if ((this.properties & 4) != 0) {
            return dest.rotation(quat);
        } else {
            return (this.properties & 8) != 0 ? this.rotateTranslation(quat, dest) : this.rotateGeneric(quat, dest);
        }
    }

    private Matrix4x3d rotateGeneric(Quaternionf quat, Matrix4x3d dest) {
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

    public Matrix4x3d rotate(Quaterniond quat) {
        return this.rotate(quat, this);
    }

    public Matrix4x3d rotate(Quaternionf quat) {
        return this.rotate(quat, this);
    }

    public Matrix4x3d rotateTranslation(Quaterniond quat, Matrix4x3d dest) {
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

    public Matrix4x3d rotateTranslation(Quaternionf quat, Matrix4x3d dest) {
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

    public Matrix4x3d rotateLocal(Quaterniond quat, Matrix4x3d dest) {
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
        double nm30 = lm00 * this.m30 + lm10 * this.m31 + lm20 * this.m32;
        double nm31 = lm01 * this.m30 + lm11 * this.m31 + lm21 * this.m32;
        double nm32 = lm02 * this.m30 + lm12 * this.m31 + lm22 * this.m32;
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

    public Matrix4x3d rotateLocal(Quaterniond quat) {
        return this.rotateLocal(quat, this);
    }

    public Matrix4x3d rotateLocal(Quaternionf quat, Matrix4x3d dest) {
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
        double nm30 = lm00 * this.m30 + lm10 * this.m31 + lm20 * this.m32;
        double nm31 = lm01 * this.m30 + lm11 * this.m31 + lm21 * this.m32;
        double nm32 = lm02 * this.m30 + lm12 * this.m31 + lm22 * this.m32;
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

    public Matrix4x3d rotateLocal(Quaternionf quat) {
        return this.rotateLocal(quat, this);
    }

    public Matrix4x3d rotate(AxisAngle4f axisAngle) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Matrix4x3d rotate(AxisAngle4f axisAngle, Matrix4x3d dest) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dest);
    }

    public Matrix4x3d rotate(AxisAngle4d axisAngle) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Matrix4x3d rotate(AxisAngle4d axisAngle, Matrix4x3d dest) {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dest);
    }

    public Matrix4x3d rotate(double angle, Vector3d axis) {
        return this.rotate(angle, axis.x, axis.y, axis.z);
    }

    public Matrix4x3d rotate(double angle, Vector3d axis, Matrix4x3d dest) {
        return this.rotate(angle, axis.x, axis.y, axis.z, dest);
    }

    public Matrix4x3d rotate(double angle, Vector3f axis) {
        return this.rotate(angle, axis.x, axis.y, axis.z);
    }

    public Matrix4x3d rotate(double angle, Vector3f axis, Matrix4x3d dest) {
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
            default:
                throw new IndexOutOfBoundsException();
        }

        return dest;
    }

    public Matrix4x3d setRow(int row, Vector4d src) throws IndexOutOfBoundsException {
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

    public Matrix4x3d setColumn(int column, Vector3d src) throws IndexOutOfBoundsException {
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

    public Matrix4x3d normal() {
        return this.normal(this);
    }

    public Matrix4x3d normal(Matrix4x3d dest) {
        if ((this.properties & 4) != 0) {
            return dest.identity();
        } else {
            return (this.properties & 16) != 0 ? this.normalOrthonormal(dest) : this.normalGeneric(dest);
        }
    }

    private Matrix4x3d normalOrthonormal(Matrix4x3d dest) {
        if (dest != this) {
            dest.set(this);
        }

        return dest._properties(16);
    }

    private Matrix4x3d normalGeneric(Matrix4x3d dest) {
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
        dest.m30 = 0.0;
        dest.m31 = 0.0;
        dest.m32 = 0.0;
        dest.properties = this.properties & -9;
        return dest;
    }

    public Matrix3d normal(Matrix3d dest) {
        return (this.properties & 16) != 0 ? this.normalOrthonormal(dest) : this.normalGeneric(dest);
    }

    private Matrix3d normalOrthonormal(Matrix3d dest) {
        return dest.set(this);
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

    public Matrix4x3d cofactor3x3() {
        return this.cofactor3x3(this);
    }

    public Matrix3d cofactor3x3(Matrix3d dest) {
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

    public Matrix4x3d cofactor3x3(Matrix4x3d dest) {
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
        dest.m30 = 0.0;
        dest.m31 = 0.0;
        dest.m32 = 0.0;
        dest.properties = this.properties & -9;
        return dest;
    }

    public Matrix4x3d normalize3x3() {
        return this.normalize3x3(this);
    }

    public Matrix4x3d normalize3x3(Matrix4x3d dest) {
        double invXlen = Math.invsqrt(this.m00 * this.m00 + this.m01 * this.m01 + this.m02 * this.m02);
        double invYlen = Math.invsqrt(this.m10 * this.m10 + this.m11 * this.m11 + this.m12 * this.m12);
        double invZlen = Math.invsqrt(this.m20 * this.m20 + this.m21 * this.m21 + this.m22 * this.m22);
        dest.m00 = this.m00 * invXlen;
        dest.m01 = this.m01 * invXlen;
        dest.m02 = this.m02 * invXlen;
        dest.m10 = this.m10 * invYlen;
        dest.m11 = this.m11 * invYlen;
        dest.m12 = this.m12 * invYlen;
        dest.m20 = this.m20 * invZlen;
        dest.m21 = this.m21 * invZlen;
        dest.m22 = this.m22 * invZlen;
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

    public Matrix4x3d reflect(double a, double b, double c, double d, Matrix4x3d dest) {
        if ((this.properties & 4) != 0) {
            return dest.reflection(a, b, c, d);
        } else {
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
            dest.m30 = this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30;
            dest.m31 = this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31;
            dest.m32 = this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32;
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
            dest.properties = this.properties & -13;
            return dest;
        }
    }

    public Matrix4x3d reflect(double a, double b, double c, double d) {
        return this.reflect(a, b, c, d, this);
    }

    public Matrix4x3d reflect(double nx, double ny, double nz, double px, double py, double pz) {
        return this.reflect(nx, ny, nz, px, py, pz, this);
    }

    public Matrix4x3d reflect(double nx, double ny, double nz, double px, double py, double pz, Matrix4x3d dest) {
        double invLength = Math.invsqrt(nx * nx + ny * ny + nz * nz);
        double nnx = nx * invLength;
        double nny = ny * invLength;
        double nnz = nz * invLength;
        return this.reflect(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz, dest);
    }

    public Matrix4x3d reflect(Vector3d normal, Vector3d point) {
        return this.reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z);
    }

    public Matrix4x3d reflect(Quaterniond orientation, Vector3d point) {
        return this.reflect(orientation, point, this);
    }

    public Matrix4x3d reflect(Quaterniond orientation, Vector3d point, Matrix4x3d dest) {
        double num1 = orientation.x + orientation.x;
        double num2 = orientation.y + orientation.y;
        double num3 = orientation.z + orientation.z;
        double normalX = orientation.x * num3 + orientation.w * num2;
        double normalY = orientation.y * num3 - orientation.w * num1;
        double normalZ = 1.0 - (orientation.x * num1 + orientation.y * num2);
        return this.reflect(normalX, normalY, normalZ, point.x, point.y, point.z, dest);
    }

    public Matrix4x3d reflect(Vector3d normal, Vector3d point, Matrix4x3d dest) {
        return this.reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z, dest);
    }

    public Matrix4x3d reflection(double a, double b, double c, double d) {
        double da = a + a;
        double db = b + b;
        double dc = c + c;
        double dd = d + d;
        this.m00 = 1.0 - da * a;
        this.m01 = -da * b;
        this.m02 = -da * c;
        this.m10 = -db * a;
        this.m11 = 1.0 - db * b;
        this.m12 = -db * c;
        this.m20 = -dc * a;
        this.m21 = -dc * b;
        this.m22 = 1.0 - dc * c;
        this.m30 = -dd * a;
        this.m31 = -dd * b;
        this.m32 = -dd * c;
        this.properties = 16;
        return this;
    }

    public Matrix4x3d reflection(double nx, double ny, double nz, double px, double py, double pz) {
        double invLength = Math.invsqrt(nx * nx + ny * ny + nz * nz);
        double nnx = nx * invLength;
        double nny = ny * invLength;
        double nnz = nz * invLength;
        return this.reflection(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz);
    }

    public Matrix4x3d reflection(Vector3d normal, Vector3d point) {
        return this.reflection(normal.x, normal.y, normal.z, point.x, point.y, point.z);
    }

    public Matrix4x3d reflection(Quaterniond orientation, Vector3d point) {
        double num1 = orientation.x + orientation.x;
        double num2 = orientation.y + orientation.y;
        double num3 = orientation.z + orientation.z;
        double normalX = orientation.x * num3 + orientation.w * num2;
        double normalY = orientation.y * num3 - orientation.w * num1;
        double normalZ = 1.0 - (orientation.x * num1 + orientation.y * num2);
        return this.reflection(normalX, normalY, normalZ, point.x, point.y, point.z);
    }

    public Matrix4x3d ortho(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne, Matrix4x3d dest) {
        double rm00 = 2.0 / (right - left);
        double rm11 = 2.0 / (top - bottom);
        double rm22 = (zZeroToOne ? 1.0 : 2.0) / (zNear - zFar);
        double rm30 = (left + right) / (left - right);
        double rm31 = (top + bottom) / (bottom - top);
        double rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
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

    public Matrix4x3d ortho(double left, double right, double bottom, double top, double zNear, double zFar, Matrix4x3d dest) {
        return this.ortho(left, right, bottom, top, zNear, zFar, false, dest);
    }

    public Matrix4x3d ortho(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne) {
        return this.ortho(left, right, bottom, top, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4x3d ortho(double left, double right, double bottom, double top, double zNear, double zFar) {
        return this.ortho(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4x3d orthoLH(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne, Matrix4x3d dest) {
        double rm00 = 2.0 / (right - left);
        double rm11 = 2.0 / (top - bottom);
        double rm22 = (zZeroToOne ? 1.0 : 2.0) / (zFar - zNear);
        double rm30 = (left + right) / (left - right);
        double rm31 = (top + bottom) / (bottom - top);
        double rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
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

    public Matrix4x3d orthoLH(double left, double right, double bottom, double top, double zNear, double zFar, Matrix4x3d dest) {
        return this.orthoLH(left, right, bottom, top, zNear, zFar, false, dest);
    }

    public Matrix4x3d orthoLH(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne) {
        return this.orthoLH(left, right, bottom, top, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4x3d orthoLH(double left, double right, double bottom, double top, double zNear, double zFar) {
        return this.orthoLH(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4x3d setOrtho(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne) {
        this.m00 = 2.0 / (right - left);
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m10 = 0.0;
        this.m11 = 2.0 / (top - bottom);
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = (zZeroToOne ? 1.0 : 2.0) / (zNear - zFar);
        this.m30 = (right + left) / (left - right);
        this.m31 = (top + bottom) / (bottom - top);
        this.m32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        this.properties = 0;
        return this;
    }

    public Matrix4x3d setOrtho(double left, double right, double bottom, double top, double zNear, double zFar) {
        return this.setOrtho(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4x3d setOrthoLH(double left, double right, double bottom, double top, double zNear, double zFar, boolean zZeroToOne) {
        this.m00 = 2.0 / (right - left);
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m10 = 0.0;
        this.m11 = 2.0 / (top - bottom);
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = (zZeroToOne ? 1.0 : 2.0) / (zFar - zNear);
        this.m30 = (right + left) / (left - right);
        this.m31 = (top + bottom) / (bottom - top);
        this.m32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        this.properties = 0;
        return this;
    }

    public Matrix4x3d setOrthoLH(double left, double right, double bottom, double top, double zNear, double zFar) {
        return this.setOrthoLH(left, right, bottom, top, zNear, zFar, false);
    }

    public Matrix4x3d orthoSymmetric(double width, double height, double zNear, double zFar, boolean zZeroToOne, Matrix4x3d dest) {
        double rm00 = 2.0 / width;
        double rm11 = 2.0 / height;
        double rm22 = (zZeroToOne ? 1.0 : 2.0) / (zNear - zFar);
        double rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
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

    public Matrix4x3d orthoSymmetric(double width, double height, double zNear, double zFar, Matrix4x3d dest) {
        return this.orthoSymmetric(width, height, zNear, zFar, false, dest);
    }

    public Matrix4x3d orthoSymmetric(double width, double height, double zNear, double zFar, boolean zZeroToOne) {
        return this.orthoSymmetric(width, height, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4x3d orthoSymmetric(double width, double height, double zNear, double zFar) {
        return this.orthoSymmetric(width, height, zNear, zFar, false, this);
    }

    public Matrix4x3d orthoSymmetricLH(double width, double height, double zNear, double zFar, boolean zZeroToOne, Matrix4x3d dest) {
        double rm00 = 2.0 / width;
        double rm11 = 2.0 / height;
        double rm22 = (zZeroToOne ? 1.0 : 2.0) / (zFar - zNear);
        double rm32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
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

    public Matrix4x3d orthoSymmetricLH(double width, double height, double zNear, double zFar, Matrix4x3d dest) {
        return this.orthoSymmetricLH(width, height, zNear, zFar, false, dest);
    }

    public Matrix4x3d orthoSymmetricLH(double width, double height, double zNear, double zFar, boolean zZeroToOne) {
        return this.orthoSymmetricLH(width, height, zNear, zFar, zZeroToOne, this);
    }

    public Matrix4x3d orthoSymmetricLH(double width, double height, double zNear, double zFar) {
        return this.orthoSymmetricLH(width, height, zNear, zFar, false, this);
    }

    public Matrix4x3d setOrthoSymmetric(double width, double height, double zNear, double zFar, boolean zZeroToOne) {
        this.m00 = 2.0 / width;
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m10 = 0.0;
        this.m11 = 2.0 / height;
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = (zZeroToOne ? 1.0 : 2.0) / (zNear - zFar);
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        this.properties = 0;
        return this;
    }

    public Matrix4x3d setOrthoSymmetric(double width, double height, double zNear, double zFar) {
        return this.setOrthoSymmetric(width, height, zNear, zFar, false);
    }

    public Matrix4x3d setOrthoSymmetricLH(double width, double height, double zNear, double zFar, boolean zZeroToOne) {
        this.m00 = 2.0 / width;
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m10 = 0.0;
        this.m11 = 2.0 / height;
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = (zZeroToOne ? 1.0 : 2.0) / (zFar - zNear);
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = (zZeroToOne ? zNear : zFar + zNear) / (zNear - zFar);
        this.properties = 0;
        return this;
    }

    public Matrix4x3d setOrthoSymmetricLH(double width, double height, double zNear, double zFar) {
        return this.setOrthoSymmetricLH(width, height, zNear, zFar, false);
    }

    public Matrix4x3d ortho2D(double left, double right, double bottom, double top, Matrix4x3d dest) {
        double rm00 = 2.0 / (right - left);
        double rm11 = 2.0 / (top - bottom);
        double rm30 = -(right + left) / (right - left);
        double rm31 = -(top + bottom) / (top - bottom);
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

    public Matrix4x3d ortho2D(double left, double right, double bottom, double top) {
        return this.ortho2D(left, right, bottom, top, this);
    }

    public Matrix4x3d ortho2DLH(double left, double right, double bottom, double top, Matrix4x3d dest) {
        double rm00 = 2.0 / (right - left);
        double rm11 = 2.0 / (top - bottom);
        double rm30 = -(right + left) / (right - left);
        double rm31 = -(top + bottom) / (top - bottom);
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

    public Matrix4x3d ortho2DLH(double left, double right, double bottom, double top) {
        return this.ortho2DLH(left, right, bottom, top, this);
    }

    public Matrix4x3d setOrtho2D(double left, double right, double bottom, double top) {
        this.m00 = 2.0 / (right - left);
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m10 = 0.0;
        this.m11 = 2.0 / (top - bottom);
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = -1.0;
        this.m30 = -(right + left) / (right - left);
        this.m31 = -(top + bottom) / (top - bottom);
        this.m32 = 0.0;
        this.properties = 0;
        return this;
    }

    public Matrix4x3d setOrtho2DLH(double left, double right, double bottom, double top) {
        this.m00 = 2.0 / (right - left);
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m10 = 0.0;
        this.m11 = 2.0 / (top - bottom);
        this.m12 = 0.0;
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = 1.0;
        this.m30 = -(right + left) / (right - left);
        this.m31 = -(top + bottom) / (top - bottom);
        this.m32 = 0.0;
        this.properties = 0;
        return this;
    }

    public Matrix4x3d lookAlong(Vector3d dir, Vector3d up) {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this);
    }

    public Matrix4x3d lookAlong(Vector3d dir, Vector3d up, Matrix4x3d dest) {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dest);
    }

    public Matrix4x3d lookAlong(double dirX, double dirY, double dirZ, double upX, double upY, double upZ, Matrix4x3d dest) {
        if ((this.properties & 4) != 0) {
            return this.setLookAlong(dirX, dirY, dirZ, upX, upY, upZ);
        } else {
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
            dest.m30 = this.m30;
            dest.m31 = this.m31;
            dest.m32 = this.m32;
            dest.properties = this.properties & -13;
            return dest;
        }
    }

    public Matrix4x3d lookAlong(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
        return this.lookAlong(dirX, dirY, dirZ, upX, upY, upZ, this);
    }

    public Matrix4x3d setLookAlong(Vector3d dir, Vector3d up) {
        return this.setLookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    public Matrix4x3d setLookAlong(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
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
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.properties = 16;
        return this;
    }

    public Matrix4x3d setLookAt(Vector3d eye, Vector3d center, Vector3d up) {
        return this.setLookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z);
    }

    public Matrix4x3d setLookAt(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) {
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

    public Matrix4x3d lookAt(Vector3d eye, Vector3d center, Vector3d up, Matrix4x3d dest) {
        return this.lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dest);
    }

    public Matrix4x3d lookAt(Vector3d eye, Vector3d center, Vector3d up) {
        return this.lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, this);
    }

    public Matrix4x3d lookAt(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ, Matrix4x3d dest) {
        return (this.properties & 4) != 0 ? dest.setLookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ) : this.lookAtGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dest);
    }

    private Matrix4x3d lookAtGeneric(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ, Matrix4x3d dest) {
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
        dest.m30 = this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30;
        dest.m31 = this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31;
        dest.m32 = this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32;
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
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3d lookAt(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) {
        return this.lookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, this);
    }

    public Matrix4x3d setLookAtLH(Vector3d eye, Vector3d center, Vector3d up) {
        return this.setLookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z);
    }

    public Matrix4x3d setLookAtLH(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) {
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

    public Matrix4x3d lookAtLH(Vector3d eye, Vector3d center, Vector3d up, Matrix4x3d dest) {
        return this.lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dest);
    }

    public Matrix4x3d lookAtLH(Vector3d eye, Vector3d center, Vector3d up) {
        return this.lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, this);
    }

    public Matrix4x3d lookAtLH(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ, Matrix4x3d dest) {
        return (this.properties & 4) != 0 ? dest.setLookAtLH(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ) : this.lookAtLHGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dest);
    }

    private Matrix4x3d lookAtLHGeneric(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ, Matrix4x3d dest) {
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
        dest.m30 = this.m00 * rm30 + this.m10 * rm31 + this.m20 * rm32 + this.m30;
        dest.m31 = this.m01 * rm30 + this.m11 * rm31 + this.m21 * rm32 + this.m31;
        dest.m32 = this.m02 * rm30 + this.m12 * rm31 + this.m22 * rm32 + this.m32;
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
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3d lookAtLH(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) {
        return this.lookAtLH(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, this);
    }

    public Vector4d frustumPlane(int which, Vector4d dest) {
        switch (which) {
            case 0:
                dest.set(this.m00, this.m10, this.m20, 1.0 + this.m30).normalize();
                break;
            case 1:
                dest.set(-this.m00, -this.m10, -this.m20, 1.0 - this.m30).normalize();
                break;
            case 2:
                dest.set(this.m01, this.m11, this.m21, 1.0 + this.m31).normalize();
                break;
            case 3:
                dest.set(-this.m01, -this.m11, -this.m21, 1.0 - this.m31).normalize();
                break;
            case 4:
                dest.set(this.m02, this.m12, this.m22, 1.0 + this.m32).normalize();
                break;
            case 5:
                dest.set(-this.m02, -this.m12, -this.m22, 1.0 - this.m32).normalize();
                break;
            default:
                throw new IllegalArgumentException("which");
        }

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

    public Vector3d origin(Vector3d origin) {
        double a = this.m00 * this.m11 - this.m01 * this.m10;
        double b = this.m00 * this.m12 - this.m02 * this.m10;
        double d = this.m01 * this.m12 - this.m02 * this.m11;
        double g = this.m20 * this.m31 - this.m21 * this.m30;
        double h = this.m20 * this.m32 - this.m22 * this.m30;
        double j = this.m21 * this.m32 - this.m22 * this.m31;
        origin.x = -this.m10 * j + this.m11 * h - this.m12 * g;
        origin.y = this.m00 * j - this.m01 * h + this.m02 * g;
        origin.z = -this.m30 * d + this.m31 * b - this.m32 * a;
        return origin;
    }

    public Matrix4x3d shadow(Vector4d light, double a, double b, double c, double d) {
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, this);
    }

    public Matrix4x3d shadow(Vector4d light, double a, double b, double c, double d, Matrix4x3d dest) {
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, dest);
    }

    public Matrix4x3d shadow(double lightX, double lightY, double lightZ, double lightW, double a, double b, double c, double d) {
        return this.shadow(lightX, lightY, lightZ, lightW, a, b, c, d, this);
    }

    public Matrix4x3d shadow(double lightX, double lightY, double lightZ, double lightW, double a, double b, double c, double d, Matrix4x3d dest) {
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
        double nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12 + this.m30 * rm13;
        double nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12 + this.m31 * rm13;
        double nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12 + this.m32 * rm13;
        double nm20 = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22 + this.m30 * rm23;
        double nm21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22 + this.m31 * rm23;
        double nm22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22 + this.m32 * rm23;
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

    public Matrix4x3d shadow(Vector4d light, Matrix4x3d planeTransform, Matrix4x3d dest) {
        double a = planeTransform.m10;
        double b = planeTransform.m11;
        double c = planeTransform.m12;
        double d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32;
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, dest);
    }

    public Matrix4x3d shadow(Vector4d light, Matrix4x3d planeTransform) {
        return this.shadow(light, planeTransform, this);
    }

    public Matrix4x3d shadow(double lightX, double lightY, double lightZ, double lightW, Matrix4x3d planeTransform, Matrix4x3d dest) {
        double a = planeTransform.m10;
        double b = planeTransform.m11;
        double c = planeTransform.m12;
        double d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32;
        return this.shadow(lightX, lightY, lightZ, lightW, a, b, c, d, dest);
    }

    public Matrix4x3d shadow(double lightX, double lightY, double lightZ, double lightW, Matrix4x3d planeTransform) {
        return this.shadow(lightX, lightY, lightZ, lightW, planeTransform, this);
    }

    public Matrix4x3d billboardCylindrical(Vector3d objPos, Vector3d targetPos, Vector3d up) {
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

    public Matrix4x3d billboardSpherical(Vector3d objPos, Vector3d targetPos, Vector3d up) {
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

    public Matrix4x3d billboardSpherical(Vector3d objPos, Vector3d targetPos) {
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
        this.m00 = 1.0 - q11;
        this.m01 = q01;
        this.m02 = -q13;
        this.m10 = q01;
        this.m11 = 1.0 - q00;
        this.m12 = q03;
        this.m20 = q13;
        this.m21 = -q03;
        this.m22 = 1.0 - q11 - q00;
        this.m30 = objPos.x;
        this.m31 = objPos.y;
        this.m32 = objPos.z;
        this.properties = 16;
        return this;
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
        temp = Double.doubleToLongBits(this.m30);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m31);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.m32);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof Matrix4x3d)) {
            return false;
        } else {
            Matrix4x3d other = (Matrix4x3d) obj;
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
            } else if (Double.doubleToLongBits(this.m22) != Double.doubleToLongBits(other.m22)) {
                return false;
            } else if (Double.doubleToLongBits(this.m30) != Double.doubleToLongBits(other.m30)) {
                return false;
            } else if (Double.doubleToLongBits(this.m31) != Double.doubleToLongBits(other.m31)) {
                return false;
            } else {
                return Double.doubleToLongBits(this.m32) == Double.doubleToLongBits(other.m32);
            }
        }
    }

    public boolean equals(Matrix4x3d m, double delta) {
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

    public Matrix4x3d pick(double x, double y, double width, double height, int[] viewport, Matrix4x3d dest) {
        double sx = (double) viewport[2] / width;
        double sy = (double) viewport[3] / height;
        double tx = ((double) viewport[2] + 2.0 * ((double) viewport[0] - x)) / width;
        double ty = ((double) viewport[3] + 2.0 * ((double) viewport[1] - y)) / height;
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

    public Matrix4x3d pick(double x, double y, double width, double height, int[] viewport) {
        return this.pick(x, y, width, height, viewport, this);
    }

    public Matrix4x3d swap(Matrix4x3d other) {
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
        tmp = this.m30;
        this.m30 = other.m30;
        other.m30 = tmp;
        tmp = this.m31;
        this.m31 = other.m31;
        other.m31 = tmp;
        tmp = this.m32;
        this.m32 = other.m32;
        other.m32 = tmp;
        int props = this.properties;
        this.properties = other.properties;
        other.properties = props;
        return this;
    }

    public Matrix4x3d arcball(double radius, double centerX, double centerY, double centerZ, double angleX, double angleY, Matrix4x3d dest) {
        double m30 = this.m20 * -radius + this.m30;
        double m31 = this.m21 * -radius + this.m31;
        double m32 = this.m22 * -radius + this.m32;
        double sin = Math.sin(angleX);
        double cos = Math.cosFromSin(sin, angleX);
        double nm10 = this.m10 * cos + this.m20 * sin;
        double nm11 = this.m11 * cos + this.m21 * sin;
        double nm12 = this.m12 * cos + this.m22 * sin;
        double m20 = this.m20 * cos - this.m10 * sin;
        double m21 = this.m21 * cos - this.m11 * sin;
        double m22 = this.m22 * cos - this.m12 * sin;
        sin = Math.sin(angleY);
        cos = Math.cosFromSin(sin, angleY);
        double nm00 = this.m00 * cos - m20 * sin;
        double nm01 = this.m01 * cos - m21 * sin;
        double nm02 = this.m02 * cos - m22 * sin;
        double nm20 = this.m00 * sin + m20 * cos;
        double nm21 = this.m01 * sin + m21 * cos;
        double nm22 = this.m02 * sin + m22 * cos;
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

    public Matrix4x3d arcball(double radius, Vector3d center, double angleX, double angleY, Matrix4x3d dest) {
        return this.arcball(radius, center.x, center.y, center.z, angleX, angleY, dest);
    }

    public Matrix4x3d arcball(double radius, double centerX, double centerY, double centerZ, double angleX, double angleY) {
        return this.arcball(radius, centerX, centerY, centerZ, angleX, angleY, this);
    }

    public Matrix4x3d arcball(double radius, Vector3d center, double angleX, double angleY) {
        return this.arcball(radius, center.x, center.y, center.z, angleX, angleY, this);
    }

    public Matrix4x3d transformAab(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vector3d outMin, Vector3d outMax) {
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

    public Matrix4x3d transformAab(Vector3d min, Vector3d max, Vector3d outMin, Vector3d outMax) {
        return this.transformAab(min.x, min.y, min.z, max.x, max.y, max.z, outMin, outMax);
    }

    public Matrix4x3d lerp(Matrix4x3d other, double t) {
        return this.lerp(other, t, this);
    }

    public Matrix4x3d lerp(Matrix4x3d other, double t, Matrix4x3d dest) {
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

    public Matrix4x3d rotateTowards(Vector3d dir, Vector3d up, Matrix4x3d dest) {
        return this.rotateTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z, dest);
    }

    public Matrix4x3d rotateTowards(Vector3d dir, Vector3d up) {
        return this.rotateTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z, this);
    }

    public Matrix4x3d rotateTowards(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
        return this.rotateTowards(dirX, dirY, dirZ, upX, upY, upZ, this);
    }

    public Matrix4x3d rotateTowards(double dirX, double dirY, double dirZ, double upX, double upY, double upZ, Matrix4x3d dest) {
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
        dest.m30 = this.m30;
        dest.m31 = this.m31;
        dest.m32 = this.m32;
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
        dest.properties = this.properties & -13;
        return dest;
    }

    public Matrix4x3d rotationTowards(Vector3d dir, Vector3d up) {
        return this.rotationTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    public Matrix4x3d rotationTowards(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
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
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.properties = 16;
        return this;
    }

    public Matrix4x3d translationRotateTowards(Vector3d pos, Vector3d dir, Vector3d up) {
        return this.translationRotateTowards(pos.x, pos.y, pos.z, dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    public Matrix4x3d translationRotateTowards(double posX, double posY, double posZ, double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
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
        this.m30 = posX;
        this.m31 = posY;
        this.m32 = posZ;
        this.properties = 16;
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

    public Matrix4x3d obliqueZ(double a, double b) {
        this.m20 += this.m00 * a + this.m10 * b;
        this.m21 += this.m01 * a + this.m11 * b;
        this.m22 += this.m02 * a + this.m12 * b;
        this.properties = 0;
        return this;
    }

    public Matrix4x3d obliqueZ(double a, double b, Matrix4x3d dest) {
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

    public Matrix4x3d mapXZY() {
        return this.mapXZY(this);
    }

    public Matrix4x3d mapXZY(Matrix4x3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapXZnY() {
        return this.mapXZnY(this);
    }

    public Matrix4x3d mapXZnY(Matrix4x3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapXnYnZ() {
        return this.mapXnYnZ(this);
    }

    public Matrix4x3d mapXnYnZ(Matrix4x3d dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapXnZY() {
        return this.mapXnZY(this);
    }

    public Matrix4x3d mapXnZY(Matrix4x3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapXnZnY() {
        return this.mapXnZnY(this);
    }

    public Matrix4x3d mapXnZnY(Matrix4x3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapYXZ() {
        return this.mapYXZ(this);
    }

    public Matrix4x3d mapYXZ(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(m00)._m11(m01)._m12(m02)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapYXnZ() {
        return this.mapYXnZ(this);
    }

    public Matrix4x3d mapYXnZ(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(m00)._m11(m01)._m12(m02)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapYZX() {
        return this.mapYZX(this);
    }

    public Matrix4x3d mapYZX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapYZnX() {
        return this.mapYZnX(this);
    }

    public Matrix4x3d mapYZnX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapYnXZ() {
        return this.mapYnXZ(this);
    }

    public Matrix4x3d mapYnXZ(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapYnXnZ() {
        return this.mapYnXnZ(this);
    }

    public Matrix4x3d mapYnXnZ(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapYnZX() {
        return this.mapYnZX(this);
    }

    public Matrix4x3d mapYnZX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapYnZnX() {
        return this.mapYnZnX(this);
    }

    public Matrix4x3d mapYnZnX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m10)._m01(this.m11)._m02(this.m12)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapZXY() {
        return this.mapZXY(this);
    }

    public Matrix4x3d mapZXY(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(m00)._m11(m01)._m12(m02)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapZXnY() {
        return this.mapZXnY(this);
    }

    public Matrix4x3d mapZXnY(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(m00)._m11(m01)._m12(m02)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapZYX() {
        return this.mapZYX(this);
    }

    public Matrix4x3d mapZYX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapZYnX() {
        return this.mapZYnX(this);
    }

    public Matrix4x3d mapZYnX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapZnXY() {
        return this.mapZnXY(this);
    }

    public Matrix4x3d mapZnXY(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapZnXnY() {
        return this.mapZnXnY(this);
    }

    public Matrix4x3d mapZnXnY(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapZnYX() {
        return this.mapZnYX(this);
    }

    public Matrix4x3d mapZnYX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapZnYnX() {
        return this.mapZnYnX(this);
    }

    public Matrix4x3d mapZnYnX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(this.m20)._m01(this.m21)._m02(this.m22)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnXYnZ() {
        return this.mapnXYnZ(this);
    }

    public Matrix4x3d mapnXYnZ(Matrix4x3d dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnXZY() {
        return this.mapnXZY(this);
    }

    public Matrix4x3d mapnXZY(Matrix4x3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnXZnY() {
        return this.mapnXZnY(this);
    }

    public Matrix4x3d mapnXZnY(Matrix4x3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnXnYZ() {
        return this.mapnXnYZ(this);
    }

    public Matrix4x3d mapnXnYZ(Matrix4x3d dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnXnYnZ() {
        return this.mapnXnYnZ(this);
    }

    public Matrix4x3d mapnXnYnZ(Matrix4x3d dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnXnZY() {
        return this.mapnXnZY(this);
    }

    public Matrix4x3d mapnXnZY(Matrix4x3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnXnZnY() {
        return this.mapnXnZnY(this);
    }

    public Matrix4x3d mapnXnZnY(Matrix4x3d dest) {
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnYXZ() {
        return this.mapnYXZ(this);
    }

    public Matrix4x3d mapnYXZ(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(m00)._m11(m01)._m12(m02)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnYXnZ() {
        return this.mapnYXnZ(this);
    }

    public Matrix4x3d mapnYXnZ(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(m00)._m11(m01)._m12(m02)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnYZX() {
        return this.mapnYZX(this);
    }

    public Matrix4x3d mapnYZX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnYZnX() {
        return this.mapnYZnX(this);
    }

    public Matrix4x3d mapnYZnX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(this.m20)._m11(this.m21)._m12(this.m22)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnYnXZ() {
        return this.mapnYnXZ(this);
    }

    public Matrix4x3d mapnYnXZ(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnYnXnZ() {
        return this.mapnYnXnZ(this);
    }

    public Matrix4x3d mapnYnXnZ(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnYnZX() {
        return this.mapnYnZX(this);
    }

    public Matrix4x3d mapnYnZX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnYnZnX() {
        return this.mapnYnZnX(this);
    }

    public Matrix4x3d mapnYnZnX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m10)._m01(-this.m11)._m02(-this.m12)._m10(-this.m20)._m11(-this.m21)._m12(-this.m22)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnZXY() {
        return this.mapnZXY(this);
    }

    public Matrix4x3d mapnZXY(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(m00)._m11(m01)._m12(m02)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnZXnY() {
        return this.mapnZXnY(this);
    }

    public Matrix4x3d mapnZXnY(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(m00)._m11(m01)._m12(m02)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnZYX() {
        return this.mapnZYX(this);
    }

    public Matrix4x3d mapnZYX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnZYnX() {
        return this.mapnZYnX(this);
    }

    public Matrix4x3d mapnZYnX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnZnXY() {
        return this.mapnZnXY(this);
    }

    public Matrix4x3d mapnZnXY(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(m10)._m21(m11)._m22(m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnZnXnY() {
        return this.mapnZnXnY(this);
    }

    public Matrix4x3d mapnZnXnY(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        double m10 = this.m10;
        double m11 = this.m11;
        double m12 = this.m12;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-m10)._m21(-m11)._m22(-m12)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnZnYX() {
        return this.mapnZnYX(this);
    }

    public Matrix4x3d mapnZnYX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(m00)._m21(m01)._m22(m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d mapnZnYnX() {
        return this.mapnZnYnX(this);
    }

    public Matrix4x3d mapnZnYnX(Matrix4x3d dest) {
        double m00 = this.m00;
        double m01 = this.m01;
        double m02 = this.m02;
        return dest._m00(-this.m20)._m01(-this.m21)._m02(-this.m22)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(-m00)._m21(-m01)._m22(-m02)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d negateX() {
        return this._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._properties(this.properties & 16);
    }

    public Matrix4x3d negateX(Matrix4x3d dest) {
        return dest._m00(-this.m00)._m01(-this.m01)._m02(-this.m02)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d negateY() {
        return this._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._properties(this.properties & 16);
    }

    public Matrix4x3d negateY(Matrix4x3d dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(-this.m10)._m11(-this.m11)._m12(-this.m12)._m20(this.m20)._m21(this.m21)._m22(this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public Matrix4x3d negateZ() {
        return this._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._properties(this.properties & 16);
    }

    public Matrix4x3d negateZ(Matrix4x3d dest) {
        return dest._m00(this.m00)._m01(this.m01)._m02(this.m02)._m10(this.m10)._m11(this.m11)._m12(this.m12)._m20(-this.m20)._m21(-this.m21)._m22(-this.m22)._m30(this.m30)._m31(this.m31)._m32(this.m32)._properties(this.properties & 16);
    }

    public boolean isFinite() {
        return Math.isFinite(this.m00) && Math.isFinite(this.m01) && Math.isFinite(this.m02) && Math.isFinite(this.m10) && Math.isFinite(this.m11) && Math.isFinite(this.m12) && Math.isFinite(this.m20) && Math.isFinite(this.m21) && Math.isFinite(this.m22) && Math.isFinite(this.m30) && Math.isFinite(this.m31) && Math.isFinite(this.m32);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
