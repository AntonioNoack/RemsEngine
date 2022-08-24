package org.joml;

import java.text.NumberFormat;

@SuppressWarnings("unused")
public class Matrix2d {

    public double m00;
    public double m01;
    public double m10;
    public double m11;

    public Matrix2d() {
        this.m00 = 1.0;
        this.m11 = 1.0;
    }

    public Matrix2d(Matrix2d mat) {
        this.setMatrix2d(mat);
    }

    public Matrix2d(Matrix2f mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
    }

    public Matrix2d(Matrix3d mat) {
        this.setMatrix3d(mat);
    }

    public Matrix2d(Matrix3f mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
    }

    public Matrix2d(double m00, double m01, double m10, double m11) {
        this.m00 = m00;
        this.m01 = m01;
        this.m10 = m10;
        this.m11 = m11;
    }

    public Matrix2d(Vector2d col0, Vector2d col1) {
        this.m00 = col0.x;
        this.m01 = col0.y;
        this.m10 = col1.x;
        this.m11 = col1.y;
    }

    public double m00() {
        return this.m00;
    }

    public double m01() {
        return this.m01;
    }

    public double m10() {
        return this.m10;
    }

    public double m11() {
        return this.m11;
    }

    public Matrix2d m00(double m00) {
        this.m00 = m00;
        return this;
    }

    public Matrix2d m01(double m01) {
        this.m01 = m01;
        return this;
    }

    public Matrix2d m10(double m10) {
        this.m10 = m10;
        return this;
    }

    public Matrix2d m11(double m11) {
        this.m11 = m11;
        return this;
    }

    Matrix2d _m00(double m00) {
        this.m00 = m00;
        return this;
    }

    Matrix2d _m01(double m01) {
        this.m01 = m01;
        return this;
    }

    Matrix2d _m10(double m10) {
        this.m10 = m10;
        return this;
    }

    Matrix2d _m11(double m11) {
        this.m11 = m11;
        return this;
    }

    public Matrix2d set(Matrix2d m) {
        this.setMatrix2d(m);
        return this;
    }

    private void setMatrix2d(Matrix2d mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
    }

    public Matrix2d set(Matrix2f m) {
        this.m00 = m.m00;
        this.m01 = m.m01;
        this.m10 = m.m10;
        this.m11 = m.m11;
        return this;
    }

    public Matrix2d set(Matrix3x2d m) {
        this.m00 = m.m00;
        this.m01 = m.m01;
        this.m10 = m.m10;
        this.m11 = m.m11;
        return this;
    }

    public Matrix2d set(Matrix3x2f m) {
        this.m00 = m.m00;
        this.m01 = m.m01;
        this.m10 = m.m10;
        this.m11 = m.m11;
        return this;
    }

    public Matrix2d set(Matrix3d m) {
        this.setMatrix3d(m);
        return this;
    }

    private void setMatrix3d(Matrix3d mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
    }

    public Matrix2d set(Matrix3f m) {
        this.m00 = m.m00;
        this.m01 = m.m01;
        this.m10 = m.m10;
        this.m11 = m.m11;
        return this;
    }

    public Matrix2d mul(Matrix2d right) {
        return this.mul(right, this);
    }

    public Matrix2d mul(Matrix2d right, Matrix2d dest) {
        double nm00 = this.m00 * right.m00 + this.m10 * right.m01;
        double nm01 = this.m01 * right.m00 + this.m11 * right.m01;
        double nm10 = this.m00 * right.m10 + this.m10 * right.m11;
        double nm11 = this.m01 * right.m10 + this.m11 * right.m11;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        return dest;
    }

    public Matrix2d mul(Matrix2f right) {
        return this.mul(right, this);
    }

    public Matrix2d mul(Matrix2f right, Matrix2d dest) {
        double nm00 = this.m00 * (double) right.m00 + this.m10 * (double) right.m01;
        double nm01 = this.m01 * (double) right.m00 + this.m11 * (double) right.m01;
        double nm10 = this.m00 * (double) right.m10 + this.m10 * (double) right.m11;
        double nm11 = this.m01 * (double) right.m10 + this.m11 * (double) right.m11;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        return dest;
    }

    public Matrix2d mulLocal(Matrix2d left) {
        return this.mulLocal(left, this);
    }

    public Matrix2d mulLocal(Matrix2d left, Matrix2d dest) {
        double nm00 = left.m00 * this.m00 + left.m10 * this.m01;
        double nm01 = left.m01 * this.m00 + left.m11 * this.m01;
        double nm10 = left.m00 * this.m10 + left.m10 * this.m11;
        double nm11 = left.m01 * this.m10 + left.m11 * this.m11;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        return dest;
    }

    public Matrix2d set(double m00, double m01, double m10, double m11) {
        this.m00 = m00;
        this.m01 = m01;
        this.m10 = m10;
        this.m11 = m11;
        return this;
    }

    public Matrix2d set(double[] m) {
        MemUtil.INSTANCE.copy(m, 0, this);
        return this;
    }

    public Matrix2d set(Vector2d col0, Vector2d col1) {
        this.m00 = col0.x;
        this.m01 = col0.y;
        this.m10 = col1.x;
        this.m11 = col1.y;
        return this;
    }

    public double determinant() {
        return this.m00 * this.m11 - this.m10 * this.m01;
    }

    public Matrix2d invert() {
        return this.invert(this);
    }

    public Matrix2d invert(Matrix2d dest) {
        double s = 1.0 / this.determinant();
        double nm00 = this.m11 * s;
        double nm01 = -this.m01 * s;
        double nm10 = -this.m10 * s;
        double nm11 = this.m00 * s;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        return dest;
    }

    public Matrix2d transpose() {
        return this.transpose(this);
    }

    public Matrix2d transpose(Matrix2d dest) {
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

    public Matrix2d get(Matrix2d dest) {
        return dest.set(this);
    }

    public Matrix3x2d get(Matrix3x2d dest) {
        return dest.set(this);
    }

    public Matrix3d get(Matrix3d dest) {
        return dest.set(this);
    }

    public double getRotation() {
        return Math.atan2(this.m01, this.m11);
    }

    public double[] get(double[] arr, int offset) {
        MemUtil.INSTANCE.copy(this, arr, offset);
        return arr;
    }

    public double[] get(double[] arr) {
        return this.get(arr, 0);
    }

    public Matrix2d zero() {
        MemUtil.INSTANCE.zero(this);
        return this;
    }

    public Matrix2d identity() {
        this.m00 = 1.0;
        this.m01 = 0.0;
        this.m10 = 0.0;
        this.m11 = 1.0;
        return this;
    }

    public Matrix2d scale(Vector2d xy, Matrix2d dest) {
        return this.scale(xy.x, xy.y, dest);
    }

    public Matrix2d scale(Vector2d xy) {
        return this.scale(xy.x, xy.y, this);
    }

    public Matrix2d scale(double x, double y, Matrix2d dest) {
        dest.m00 = this.m00 * x;
        dest.m01 = this.m01 * x;
        dest.m10 = this.m10 * y;
        dest.m11 = this.m11 * y;
        return dest;
    }

    public Matrix2d scale(double x, double y) {
        return this.scale(x, y, this);
    }

    public Matrix2d scale(double xy, Matrix2d dest) {
        return this.scale(xy, xy, dest);
    }

    public Matrix2d scale(double xy) {
        return this.scale(xy, xy);
    }

    public Matrix2d scaleLocal(double x, double y, Matrix2d dest) {
        dest.m00 = x * this.m00;
        dest.m01 = y * this.m01;
        dest.m10 = x * this.m10;
        dest.m11 = y * this.m11;
        return dest;
    }

    public Matrix2d scaleLocal(double x, double y) {
        return this.scaleLocal(x, y, this);
    }

    public Matrix2d scaling(double factor) {
        MemUtil.INSTANCE.zero(this);
        this.m00 = factor;
        this.m11 = factor;
        return this;
    }

    public Matrix2d scaling(double x, double y) {
        MemUtil.INSTANCE.zero(this);
        this.m00 = x;
        this.m11 = y;
        return this;
    }

    public Matrix2d scaling(Vector2d xy) {
        return this.scaling(xy.x, xy.y);
    }

    public Matrix2d rotation(double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        this.m00 = cos;
        this.m01 = sin;
        this.m10 = -sin;
        this.m11 = cos;
        return this;
    }

    public Vector2d transform(Vector2d v) {
        return v.mul(this);
    }

    public Vector2d transform(Vector2d v, Vector2d dest) {
        v.mul(this, dest);
        return dest;
    }

    public Vector2d transform(double x, double y, Vector2d dest) {
        dest.set(this.m00 * x + this.m10 * y, this.m01 * x + this.m11 * y);
        return dest;
    }

    public Vector2d transformTranspose(Vector2d v) {
        return v.mulTranspose(this);
    }

    public Vector2d transformTranspose(Vector2d v, Vector2d dest) {
        v.mulTranspose(this, dest);
        return dest;
    }

    public Vector2d transformTranspose(double x, double y, Vector2d dest) {
        dest.set(this.m00 * x + this.m01 * y, this.m10 * x + this.m11 * y);
        return dest;
    }

    public Matrix2d rotate(double angle) {
        return this.rotate(angle, this);
    }

    public Matrix2d rotate(double angle, Matrix2d dest) {
        double s = Math.sin(angle);
        double c = Math.cosFromSin(s, angle);
        double nm00 = this.m00 * c + this.m10 * s;
        double nm01 = this.m01 * c + this.m11 * s;
        double nm10 = this.m10 * c - this.m00 * s;
        double nm11 = this.m11 * c - this.m01 * s;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        return dest;
    }

    public Matrix2d rotateLocal(double angle) {
        return this.rotateLocal(angle, this);
    }

    public Matrix2d rotateLocal(double angle, Matrix2d dest) {
        double s = Math.sin(angle);
        double c = Math.cosFromSin(s, angle);
        double nm00 = c * this.m00 - s * this.m01;
        double nm01 = s * this.m00 + c * this.m01;
        double nm10 = c * this.m10 - s * this.m11;
        double nm11 = s * this.m10 + c * this.m11;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        return dest;
    }

    public Vector2d getRow(int row, Vector2d dest) throws IndexOutOfBoundsException {
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

    public Matrix2d setRow(int row, Vector2d src) throws IndexOutOfBoundsException {
        return this.setRow(row, src.x, src.y);
    }

    public Matrix2d setRow(int row, double x, double y) throws IndexOutOfBoundsException {
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

    public Vector2d getColumn(int column, Vector2d dest) throws IndexOutOfBoundsException {
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

    public Matrix2d setColumn(int column, Vector2d src) throws IndexOutOfBoundsException {
        return this.setColumn(column, src.x, src.y);
    }

    public Matrix2d setColumn(int column, double x, double y) throws IndexOutOfBoundsException {
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

    public double get(int column, int row) {
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

    public Matrix2d set(int column, int row, double value) {
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

    public Matrix2d normal() {
        return this.normal(this);
    }

    public Matrix2d normal(Matrix2d dest) {
        double det = this.m00 * this.m11 - this.m10 * this.m01;
        double s = 1.0 / det;
        double nm00 = this.m11 * s;
        double nm01 = -this.m10 * s;
        double nm10 = -this.m01 * s;
        double nm11 = this.m00 * s;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        return dest;
    }

    public Vector2d getScale(Vector2d dest) {
        dest.x = Math.sqrt(this.m00 * this.m00 + this.m01 * this.m01);
        dest.y = Math.sqrt(this.m10 * this.m10 + this.m11 * this.m11);
        return dest;
    }

    public Vector2d positiveX(Vector2d dir) {
        if (this.m00 * this.m11 < this.m01 * this.m10) {
            dir.x = -this.m11;
            dir.y = this.m01;
        } else {
            dir.x = this.m11;
            dir.y = -this.m01;
        }

        return dir.normalize(dir);
    }

    public Vector2d normalizedPositiveX(Vector2d dir) {
        if (this.m00 * this.m11 < this.m01 * this.m10) {
            dir.x = -this.m11;
            dir.y = this.m01;
        } else {
            dir.x = this.m11;
            dir.y = -this.m01;
        }

        return dir;
    }

    public Vector2d positiveY(Vector2d dir) {
        if (this.m00 * this.m11 < this.m01 * this.m10) {
            dir.x = this.m10;
            dir.y = -this.m00;
        } else {
            dir.x = -this.m10;
            dir.y = this.m00;
        }

        return dir.normalize(dir);
    }

    public Vector2d normalizedPositiveY(Vector2d dir) {
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
        long temp = Double.doubleToLongBits(this.m00);
        result = 31 * result + (int) (temp >>> 32 ^ temp);
        temp = Double.doubleToLongBits(this.m01);
        result = 31 * result + (int) (temp >>> 32 ^ temp);
        temp = Double.doubleToLongBits(this.m10);
        result = 31 * result + (int) (temp >>> 32 ^ temp);
        temp = Double.doubleToLongBits(this.m11);
        result = 31 * result + (int) (temp >>> 32 ^ temp);
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
            Matrix2d other = (Matrix2d) obj;
            if (Double.doubleToLongBits(this.m00) != Double.doubleToLongBits(other.m00)) {
                return false;
            } else if (Double.doubleToLongBits(this.m01) != Double.doubleToLongBits(other.m01)) {
                return false;
            } else if (Double.doubleToLongBits(this.m10) != Double.doubleToLongBits(other.m10)) {
                return false;
            } else {
                return Double.doubleToLongBits(this.m11) == Double.doubleToLongBits(other.m11);
            }
        }
    }

    public boolean equals(Matrix2d m, double delta) {
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

    public Matrix2d swap(Matrix2d other) {
        MemUtil.INSTANCE.swap(this, other);
        return this;
    }

    public Matrix2d add(Matrix2d other) {
        return this.add(other, this);
    }

    public Matrix2d add(Matrix2d other, Matrix2d dest) {
        dest.m00 = this.m00 + other.m00;
        dest.m01 = this.m01 + other.m01;
        dest.m10 = this.m10 + other.m10;
        dest.m11 = this.m11 + other.m11;
        return dest;
    }

    public Matrix2d sub(Matrix2d subtrahend) {
        return this.sub(subtrahend, this);
    }

    public Matrix2d sub(Matrix2d other, Matrix2d dest) {
        dest.m00 = this.m00 - other.m00;
        dest.m01 = this.m01 - other.m01;
        dest.m10 = this.m10 - other.m10;
        dest.m11 = this.m11 - other.m11;
        return dest;
    }

    public Matrix2d mulComponentWise(Matrix2d other) {
        return this.sub(other, this);
    }

    public Matrix2d mulComponentWise(Matrix2d other, Matrix2d dest) {
        dest.m00 = this.m00 * other.m00;
        dest.m01 = this.m01 * other.m01;
        dest.m10 = this.m10 * other.m10;
        dest.m11 = this.m11 * other.m11;
        return dest;
    }

    public Matrix2d lerp(Matrix2d other, double t) {
        return this.lerp(other, t, this);
    }

    public Matrix2d lerp(Matrix2d other, double t, Matrix2d dest) {
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
