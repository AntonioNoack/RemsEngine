package org.joml;

import java.text.NumberFormat;

@SuppressWarnings("unused")
public class Matrix2f {

    public float m00;
    public float m01;
    public float m10;
    public float m11;

    public Matrix2f() {
        this.m00 = 1.0F;
        this.m11 = 1.0F;
    }

    public Matrix2f(Matrix2f mat) {
        if (mat instanceof Matrix2f) {
            MemUtil.INSTANCE.copy((Matrix2f) mat, this);
        } else {
            this.setMatrix2f(mat);
        }

    }

    public Matrix2f(Matrix3f mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
    }

    public Matrix2f(float m00, float m01, float m10, float m11) {
        this.m00 = m00;
        this.m01 = m01;
        this.m10 = m10;
        this.m11 = m11;
    }

    public Matrix2f(Vector2f col0, Vector2f col1) {
        this.m00 = col0.x;
        this.m01 = col0.y;
        this.m10 = col1.x;
        this.m11 = col1.y;
    }

    public float m00() {
        return this.m00;
    }

    public float m01() {
        return this.m01;
    }

    public float m10() {
        return this.m10;
    }

    public float m11() {
        return this.m11;
    }

    public Matrix2f m00(float m00) {
        this.m00 = m00;
        return this;
    }

    public Matrix2f m01(float m01) {
        this.m01 = m01;
        return this;
    }

    public Matrix2f m10(float m10) {
        this.m10 = m10;
        return this;
    }

    public Matrix2f m11(float m11) {
        this.m11 = m11;
        return this;
    }

    Matrix2f _m00(float m00) {
        this.m00 = m00;
        return this;
    }

    Matrix2f _m01(float m01) {
        this.m01 = m01;
        return this;
    }

    Matrix2f _m10(float m10) {
        this.m10 = m10;
        return this;
    }

    Matrix2f _m11(float m11) {
        this.m11 = m11;
        return this;
    }

    public Matrix2f set(Matrix2f m) {
        if (m instanceof Matrix2f) {
            MemUtil.INSTANCE.copy((Matrix2f) m, this);
        } else {
            this.setMatrix2f(m);
        }

        return this;
    }

    private void setMatrix2f(Matrix2f mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
    }

    public Matrix2f set(Matrix3x2f m) {
        this.m00 = m.m00;
        this.m01 = m.m01;
        this.m10 = m.m10;
        this.m11 = m.m11;
        return this;
    }

    public Matrix2f set(Matrix3f m) {
        this.m00 = m.m00;
        this.m01 = m.m01;
        this.m10 = m.m10;
        this.m11 = m.m11;
        return this;
    }

    private void setMatrix3f(Matrix3f mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
    }

    public Matrix2f mul(Matrix2f right) {
        return this.mul(right, this);
    }

    public Matrix2f mul(Matrix2f right, Matrix2f dest) {
        float nm00 = this.m00 * right.m00 + this.m10 * right.m01;
        float nm01 = this.m01 * right.m00 + this.m11 * right.m01;
        float nm10 = this.m00 * right.m10 + this.m10 * right.m11;
        float nm11 = this.m01 * right.m10 + this.m11 * right.m11;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        return dest;
    }

    public Matrix2f mulLocal(Matrix2f left) {
        return this.mulLocal(left, this);
    }

    public Matrix2f mulLocal(Matrix2f left, Matrix2f dest) {
        float nm00 = left.m00 * this.m00 + left.m10 * this.m01;
        float nm01 = left.m01 * this.m00 + left.m11 * this.m01;
        float nm10 = left.m00 * this.m10 + left.m10 * this.m11;
        float nm11 = left.m01 * this.m10 + left.m11 * this.m11;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        return dest;
    }

    public Matrix2f set(float m00, float m01, float m10, float m11) {
        this.m00 = m00;
        this.m01 = m01;
        this.m10 = m10;
        this.m11 = m11;
        return this;
    }

    public Matrix2f set(float[] m) {
        MemUtil.INSTANCE.copy(m, 0, this);
        return this;
    }

    public Matrix2f set(Vector2f col0, Vector2f col1) {
        this.m00 = col0.x;
        this.m01 = col0.y;
        this.m10 = col1.x;
        this.m11 = col1.y;
        return this;
    }

    public float determinant() {
        return this.m00 * this.m11 - this.m10 * this.m01;
    }

    public Matrix2f invert() {
        return this.invert(this);
    }

    public Matrix2f invert(Matrix2f dest) {
        float s = 1.0F / this.determinant();
        float nm00 = this.m11 * s;
        float nm01 = -this.m01 * s;
        float nm10 = -this.m10 * s;
        float nm11 = this.m00 * s;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        return dest;
    }

    public Matrix2f transpose() {
        return this.transpose(this);
    }

    public Matrix2f transpose(Matrix2f dest) {
        dest.set(this.m00, this.m10, this.m01, this.m11);
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
        return Runtime.format(this.m00, formatter) + " " + Runtime.format(this.m10, formatter) + "\n" + Runtime.format(this.m01, formatter) + " " + Runtime.format(this.m11, formatter) + "\n";
    }

    public Matrix2f get(Matrix2f dest) {
        return dest.set((Matrix2f) this);
    }

    public Matrix3x2f get(Matrix3x2f dest) {
        return dest.set(this);
    }

    public Matrix3f get(Matrix3f dest) {
        return dest.set(this);
    }

    public float getRotation() {
        return Math.atan2(this.m01, this.m11);
    }

    public float[] get(float[] arr, int offset) {
        MemUtil.INSTANCE.copy(this, arr, offset);
        return arr;
    }

    public float[] get(float[] arr) {
        return this.get(arr, 0);
    }

    public Matrix2f zero() {
        MemUtil.INSTANCE.zero(this);
        return this;
    }

    public Matrix2f identity() {
        MemUtil.INSTANCE.identity(this);
        return this;
    }

    public Matrix2f scale(Vector2f xy, Matrix2f dest) {
        return this.scale(xy.x, xy.y, dest);
    }

    public Matrix2f scale(Vector2f xy) {
        return this.scale(xy.x, xy.y, this);
    }

    public Matrix2f scale(float x, float y, Matrix2f dest) {
        dest.m00 = this.m00 * x;
        dest.m01 = this.m01 * x;
        dest.m10 = this.m10 * y;
        dest.m11 = this.m11 * y;
        return dest;
    }

    public Matrix2f scale(float x, float y) {
        return this.scale(x, y, this);
    }

    public Matrix2f scale(float xy, Matrix2f dest) {
        return this.scale(xy, xy, dest);
    }

    public Matrix2f scale(float xy) {
        return this.scale(xy, xy);
    }

    public Matrix2f scaleLocal(float x, float y, Matrix2f dest) {
        dest.m00 = x * this.m00;
        dest.m01 = y * this.m01;
        dest.m10 = x * this.m10;
        dest.m11 = y * this.m11;
        return dest;
    }

    public Matrix2f scaleLocal(float x, float y) {
        return this.scaleLocal(x, y, this);
    }

    public Matrix2f scaling(float factor) {
        MemUtil.INSTANCE.zero(this);
        this.m00 = factor;
        this.m11 = factor;
        return this;
    }

    public Matrix2f scaling(float x, float y) {
        MemUtil.INSTANCE.zero(this);
        this.m00 = x;
        this.m11 = y;
        return this;
    }

    public Matrix2f scaling(Vector2f xy) {
        return this.scaling(xy.x, xy.y);
    }

    public Matrix2f rotation(float angle) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        this.m00 = cos;
        this.m01 = sin;
        this.m10 = -sin;
        this.m11 = cos;
        return this;
    }

    public Vector2f transform(Vector2f v) {
        return v.mul(this);
    }

    public Vector2f transform(Vector2f v, Vector2f dest) {
        v.mul(this, dest);
        return dest;
    }

    public Vector2f transform(float x, float y, Vector2f dest) {
        dest.set(this.m00 * x + this.m10 * y, this.m01 * x + this.m11 * y);
        return dest;
    }

    public Vector2f transformTranspose(Vector2f v) {
        return v.mulTranspose(this);
    }

    public Vector2f transformTranspose(Vector2f v, Vector2f dest) {
        v.mulTranspose(this, dest);
        return dest;
    }

    public Vector2f transformTranspose(float x, float y, Vector2f dest) {
        dest.set(this.m00 * x + this.m01 * y, this.m10 * x + this.m11 * y);
        return dest;
    }

    public Matrix2f rotate(float angle) {
        return this.rotate(angle, this);
    }

    public Matrix2f rotate(float angle, Matrix2f dest) {
        float s = Math.sin(angle);
        float c = Math.cosFromSin(s, angle);
        float nm00 = this.m00 * c + this.m10 * s;
        float nm01 = this.m01 * c + this.m11 * s;
        float nm10 = this.m10 * c - this.m00 * s;
        float nm11 = this.m11 * c - this.m01 * s;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        return dest;
    }

    public Matrix2f rotateLocal(float angle) {
        return this.rotateLocal(angle, this);
    }

    public Matrix2f rotateLocal(float angle, Matrix2f dest) {
        float s = Math.sin(angle);
        float c = Math.cosFromSin(s, angle);
        float nm00 = c * this.m00 - s * this.m01;
        float nm01 = s * this.m00 + c * this.m01;
        float nm10 = c * this.m10 - s * this.m11;
        float nm11 = s * this.m10 + c * this.m11;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        return dest;
    }

    public Vector2f getRow(int row, Vector2f dest) throws IndexOutOfBoundsException {
        switch (row) {
            case 0:
                dest.x = this.m00;
                dest.y = this.m10;
                break;
            case 1:
                dest.x = this.m01;
                dest.y = this.m11;
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

        return dest;
    }

    public Matrix2f setRow(int row, Vector2f src) throws IndexOutOfBoundsException {
        return this.setRow(row, src.x, src.y);
    }

    public Matrix2f setRow(int row, float x, float y) throws IndexOutOfBoundsException {
        switch (row) {
            case 0:
                this.m00 = x;
                this.m10 = y;
                break;
            case 1:
                this.m01 = x;
                this.m11 = y;
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

        return this;
    }

    public Vector2f getColumn(int column, Vector2f dest) throws IndexOutOfBoundsException {
        switch (column) {
            case 0:
                dest.x = this.m00;
                dest.y = this.m01;
                break;
            case 1:
                dest.x = this.m10;
                dest.y = this.m11;
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

        return dest;
    }

    public Matrix2f setColumn(int column, Vector2f src) throws IndexOutOfBoundsException {
        return this.setColumn(column, src.x, src.y);
    }

    public Matrix2f setColumn(int column, float x, float y) throws IndexOutOfBoundsException {
        switch (column) {
            case 0:
                this.m00 = x;
                this.m01 = y;
                break;
            case 1:
                this.m10 = x;
                this.m11 = y;
                break;
            default:
                throw new IndexOutOfBoundsException();
        }

        return this;
    }

    public float get(int column, int row) {
        switch (column) {
            case 0:
                switch (row) {
                    case 0:
                        return this.m00;
                    case 1:
                        return this.m01;
                    default:
                        throw new IndexOutOfBoundsException();
                }
            case 1:
                switch (row) {
                    case 0:
                        return this.m10;
                    case 1:
                        return this.m11;
                }
        }

        throw new IndexOutOfBoundsException();
    }

    public Matrix2f set(int column, int row, float value) {
        switch (column) {
            case 0:
                switch (row) {
                    case 0:
                        this.m00 = value;
                        return this;
                    case 1:
                        this.m01 = value;
                        return this;
                    default:
                        throw new IndexOutOfBoundsException();
                }
            case 1:
                switch (row) {
                    case 0:
                        this.m10 = value;
                        return this;
                    case 1:
                        this.m11 = value;
                        return this;
                }
        }

        throw new IndexOutOfBoundsException();
    }

    public Matrix2f normal() {
        return this.normal(this);
    }

    public Matrix2f normal(Matrix2f dest) {
        float det = this.m00 * this.m11 - this.m10 * this.m01;
        float s = 1.0F / det;
        float nm00 = this.m11 * s;
        float nm01 = -this.m10 * s;
        float nm10 = -this.m01 * s;
        float nm11 = this.m00 * s;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        return dest;
    }

    public Vector2f getScale(Vector2f dest) {
        dest.x = Math.sqrt(this.m00 * this.m00 + this.m01 * this.m01);
        dest.y = Math.sqrt(this.m10 * this.m10 + this.m11 * this.m11);
        return dest;
    }

    public Vector2f positiveX(Vector2f dir) {
        if (this.m00 * this.m11 < this.m01 * this.m10) {
            dir.x = -this.m11;
            dir.y = this.m01;
        } else {
            dir.x = this.m11;
            dir.y = -this.m01;
        }

        return dir.normalize(dir);
    }

    public Vector2f normalizedPositiveX(Vector2f dir) {
        if (this.m00 * this.m11 < this.m01 * this.m10) {
            dir.x = -this.m11;
            dir.y = this.m01;
        } else {
            dir.x = this.m11;
            dir.y = -this.m01;
        }

        return dir;
    }

    public Vector2f positiveY(Vector2f dir) {
        if (this.m00 * this.m11 < this.m01 * this.m10) {
            dir.x = this.m10;
            dir.y = -this.m00;
        } else {
            dir.x = -this.m10;
            dir.y = this.m00;
        }

        return dir.normalize(dir);
    }

    public Vector2f normalizedPositiveY(Vector2f dir) {
        if (this.m00 * this.m11 < this.m01 * this.m10) {
            dir.x = this.m10;
            dir.y = -this.m00;
        } else {
            dir.x = -this.m10;
            dir.y = this.m00;
        }

        return dir;
    }

    public int hashCode() {
        int result = 1;
        result = 31 * result + Float.floatToIntBits(this.m00);
        result = 31 * result + Float.floatToIntBits(this.m01);
        result = 31 * result + Float.floatToIntBits(this.m10);
        result = 31 * result + Float.floatToIntBits(this.m11);
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
            Matrix2f other = (Matrix2f) obj;
            if (Float.floatToIntBits(this.m00) != Float.floatToIntBits(other.m00)) {
                return false;
            } else if (Float.floatToIntBits(this.m01) != Float.floatToIntBits(other.m01)) {
                return false;
            } else if (Float.floatToIntBits(this.m10) != Float.floatToIntBits(other.m10)) {
                return false;
            } else {
                return Float.floatToIntBits(this.m11) == Float.floatToIntBits(other.m11);
            }
        }
    }

    public boolean equals(Matrix2f m, float delta) {
        if (this == m) {
            return true;
        } else if (m == null) {
            return false;
        } else if (!Runtime.equals(this.m00, m.m00, delta)) {
            return false;
        } else if (!Runtime.equals(this.m01, m.m01, delta)) {
            return false;
        } else if (!Runtime.equals(this.m10, m.m10, delta)) {
            return false;
        } else {
            return Runtime.equals(this.m11, m.m11, delta);
        }
    }

    public Matrix2f swap(Matrix2f other) {
        MemUtil.INSTANCE.swap(this, other);
        return this;
    }

    public Matrix2f add(Matrix2f other) {
        return this.add(other, this);
    }

    public Matrix2f add(Matrix2f other, Matrix2f dest) {
        dest.m00 = this.m00 + other.m00;
        dest.m01 = this.m01 + other.m01;
        dest.m10 = this.m10 + other.m10;
        dest.m11 = this.m11 + other.m11;
        return dest;
    }

    public Matrix2f sub(Matrix2f subtrahend) {
        return this.sub(subtrahend, this);
    }

    public Matrix2f sub(Matrix2f other, Matrix2f dest) {
        dest.m00 = this.m00 - other.m00;
        dest.m01 = this.m01 - other.m01;
        dest.m10 = this.m10 - other.m10;
        dest.m11 = this.m11 - other.m11;
        return dest;
    }

    public Matrix2f mulComponentWise(Matrix2f other) {
        return this.sub(other, this);
    }

    public Matrix2f mulComponentWise(Matrix2f other, Matrix2f dest) {
        dest.m00 = this.m00 * other.m00;
        dest.m01 = this.m01 * other.m01;
        dest.m10 = this.m10 * other.m10;
        dest.m11 = this.m11 * other.m11;
        return dest;
    }

    public Matrix2f lerp(Matrix2f other, float t) {
        return this.lerp(other, t, this);
    }

    public Matrix2f lerp(Matrix2f other, float t, Matrix2f dest) {
        dest.m00 = Math.fma(other.m00 - this.m00, t, this.m00);
        dest.m01 = Math.fma(other.m01 - this.m01, t, this.m01);
        dest.m10 = Math.fma(other.m10 - this.m10, t, this.m10);
        dest.m11 = Math.fma(other.m11 - this.m11, t, this.m11);
        return dest;
    }

    public boolean isFinite() {
        return Math.isFinite(this.m00) && Math.isFinite(this.m01) && Math.isFinite(this.m10) && Math.isFinite(this.m11);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
