package org.joml;

import java.text.NumberFormat;

@SuppressWarnings("unused")
public class Matrix3x2f {

    public float m00;
    public float m01;
    public float m10;
    public float m11;
    public float m20;
    public float m21;

    public Matrix3x2f() {
        this.m00 = 1f;
        this.m11 = 1f;
    }

    public Matrix3x2f(Matrix3x2f mat) {
        this.setMatrix3x2fc(mat);
    }

    public Matrix3x2f(Matrix2f mat) {
        this.setMatrix2fc(mat);
    }

    public Matrix3x2f(float m00, float m01, float m10, float m11, float m20, float m21) {
        this.m00 = m00;
        this.m01 = m01;
        this.m10 = m10;
        this.m11 = m11;
        this.m20 = m20;
        this.m21 = m21;
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

    public float m20() {
        return this.m20;
    }

    public float m21() {
        return this.m21;
    }

    Matrix3x2f _m00(float m00) {
        this.m00 = m00;
        return this;
    }

    Matrix3x2f _m01(float m01) {
        this.m01 = m01;
        return this;
    }

    Matrix3x2f _m10(float m10) {
        this.m10 = m10;
        return this;
    }

    Matrix3x2f _m11(float m11) {
        this.m11 = m11;
        return this;
    }

    Matrix3x2f _m20(float m20) {
        this.m20 = m20;
        return this;
    }

    Matrix3x2f _m21(float m21) {
        this.m21 = m21;
        return this;
    }

    public Matrix3x2f set(Matrix3x2f m) {
        this.setMatrix3x2fc(m);
        return this;
    }

    private void setMatrix3x2fc(Matrix3x2f mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
        this.m20 = mat.m20;
        this.m21 = mat.m21;
    }

    public Matrix3x2f set(Matrix2f m) {
        this.setMatrix2fc(m);
        return this;
    }

    private void setMatrix2fc(Matrix2f mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
    }

    public Matrix3x2f mul(Matrix3x2f right) {
        return this.mul(right, this);
    }

    public Matrix3x2f mul(Matrix3x2f right, Matrix3x2f dest) {
        float nm00 = this.m00 * right.m00 + this.m10 * right.m01;
        float nm01 = this.m01 * right.m00 + this.m11 * right.m01;
        float nm10 = this.m00 * right.m10 + this.m10 * right.m11;
        float nm11 = this.m01 * right.m10 + this.m11 * right.m11;
        float nm20 = this.m00 * right.m20 + this.m10 * right.m21 + this.m20;
        float nm21 = this.m01 * right.m20 + this.m11 * right.m21 + this.m21;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m20 = nm20;
        dest.m21 = nm21;
        return dest;
    }

    public Matrix3x2f mulLocal(Matrix3x2f left) {
        return this.mulLocal(left, this);
    }

    public Matrix3x2f mulLocal(Matrix3x2f left, Matrix3x2f dest) {
        float nm00 = left.m00 * this.m00 + left.m10 * this.m01;
        float nm01 = left.m01 * this.m00 + left.m11 * this.m01;
        float nm10 = left.m00 * this.m10 + left.m10 * this.m11;
        float nm11 = left.m01 * this.m10 + left.m11 * this.m11;
        float nm20 = left.m00 * this.m20 + left.m10 * this.m21 + left.m20;
        float nm21 = left.m01 * this.m20 + left.m11 * this.m21 + left.m21;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m20 = nm20;
        dest.m21 = nm21;
        return dest;
    }

    public Matrix3x2f set(float m00, float m01, float m10, float m11, float m20, float m21) {
        this.m00 = m00;
        this.m01 = m01;
        this.m10 = m10;
        this.m11 = m11;
        this.m20 = m20;
        this.m21 = m21;
        return this;
    }

    public Matrix3x2f set(float[] m) {
        MemUtil.INSTANCE.copy(m, 0, this);
        return this;
    }

    public float determinant() {
        return this.m00 * this.m11 - this.m01 * this.m10;
    }

    public Matrix3x2f invert() {
        return this.invert(this);
    }

    public Matrix3x2f invert(Matrix3x2f dest) {
        float s = 1f / (this.m00 * this.m11 - this.m01 * this.m10);
        float nm00 = this.m11 * s;
        float nm01 = -this.m01 * s;
        float nm10 = -this.m10 * s;
        float nm11 = this.m00 * s;
        float nm20 = (this.m10 * this.m21 - this.m20 * this.m11) * s;
        float nm21 = (this.m20 * this.m01 - this.m00 * this.m21) * s;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m20 = nm20;
        dest.m21 = nm21;
        return dest;
    }

    public Matrix3x2f translation(float x, float y) {
        this.m00 = 1f;
        this.m01 = 0f;
        this.m10 = 0f;
        this.m11 = 1f;
        this.m20 = x;
        this.m21 = y;
        return this;
    }

    public Matrix3x2f translation(Vector2f offset) {
        return this.translation(offset.x, offset.y);
    }

    public Matrix3x2f setTranslation(float x, float y) {
        this.m20 = x;
        this.m21 = y;
        return this;
    }

    public Matrix3x2f setTranslation(Vector2f offset) {
        return this.setTranslation(offset.x, offset.y);
    }

    public Matrix3x2f translate(float x, float y, Matrix3x2f dest) {
        dest.m20 = this.m00 * x + this.m10 * y + this.m20;
        dest.m21 = this.m01 * x + this.m11 * y + this.m21;
        dest.m00 = this.m00;
        dest.m01 = this.m01;
        dest.m10 = this.m10;
        dest.m11 = this.m11;
        return dest;
    }

    public Matrix3x2f translate(float x, float y) {
        return this.translate(x, y, this);
    }

    public Matrix3x2f translate(Vector2fc offset, Matrix3x2f dest) {
        return this.translate(offset.x(), offset.y(), dest);
    }

    public Matrix3x2f translate(Vector2fc offset) {
        return this.translate(offset.x(), offset.y(), this);
    }

    public Matrix3x2f translateLocal(Vector2fc offset) {
        return this.translateLocal(offset.x(), offset.y());
    }

    public Matrix3x2f translateLocal(Vector2fc offset, Matrix3x2f dest) {
        return this.translateLocal(offset.x(), offset.y(), dest);
    }

    public Matrix3x2f translateLocal(float x, float y, Matrix3x2f dest) {
        dest.m00 = this.m00;
        dest.m01 = this.m01;
        dest.m10 = this.m10;
        dest.m11 = this.m11;
        dest.m20 = this.m20 + x;
        dest.m21 = this.m21 + y;
        return dest;
    }

    public Matrix3x2f translateLocal(float x, float y) {
        return this.translateLocal(x, y, this);
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
        return Runtime.format(this.m00, formatter) + " " + Runtime.format(this.m10, formatter) + " " + Runtime.format(this.m20, formatter) + "\n" + Runtime.format(this.m01, formatter) + " " + Runtime.format(this.m11, formatter) + " " + Runtime.format(this.m21, formatter) + "\n";
    }

    public Matrix3x2f get(Matrix3x2f dest) {
        return dest.set(this);
    }

    public float[] get(float[] arr, int offset) {
        MemUtil.INSTANCE.copy(this, arr, offset);
        return arr;
    }

    public float[] get(float[] arr) {
        return this.get(arr, 0);
    }

    public float[] get3x3(float[] arr, int offset) {
        MemUtil.INSTANCE.copy3x3(this, arr, offset);
        return arr;
    }

    public float[] get3x3(float[] arr) {
        return this.get3x3(arr, 0);
    }

    public float[] get4x4(float[] arr, int offset) {
        MemUtil.INSTANCE.copy4x4(this, arr, offset);
        return arr;
    }

    public float[] get4x4(float[] arr) {
        return this.get4x4(arr, 0);
    }

    public Matrix3x2f zero() {
        MemUtil.INSTANCE.zero(this);
        return this;
    }

    public Matrix3x2f identity() {
        MemUtil.INSTANCE.identity(this);
        return this;
    }

    public Matrix3x2f scale(float x, float y, Matrix3x2f dest) {
        dest.m00 = this.m00 * x;
        dest.m01 = this.m01 * x;
        dest.m10 = this.m10 * y;
        dest.m11 = this.m11 * y;
        dest.m20 = this.m20;
        dest.m21 = this.m21;
        return dest;
    }

    public Matrix3x2f scale(float x, float y) {
        return this.scale(x, y, this);
    }

    public Matrix3x2f scale(Vector2fc xy) {
        return this.scale(xy.x(), xy.y(), this);
    }

    public Matrix3x2f scale(Vector2fc xy, Matrix3x2f dest) {
        return this.scale(xy.x(), xy.y(), dest);
    }

    public Matrix3x2f scale(float xy, Matrix3x2f dest) {
        return this.scale(xy, xy, dest);
    }

    public Matrix3x2f scale(float xy) {
        return this.scale(xy, xy);
    }

    public Matrix3x2f scaleLocal(float x, float y, Matrix3x2f dest) {
        dest.m00 = x * this.m00;
        dest.m01 = y * this.m01;
        dest.m10 = x * this.m10;
        dest.m11 = y * this.m11;
        dest.m20 = x * this.m20;
        dest.m21 = y * this.m21;
        return dest;
    }

    public Matrix3x2f scaleLocal(float x, float y) {
        return this.scaleLocal(x, y, this);
    }

    public Matrix3x2f scaleLocal(float xy, Matrix3x2f dest) {
        return this.scaleLocal(xy, xy, dest);
    }

    public Matrix3x2f scaleLocal(float xy) {
        return this.scaleLocal(xy, xy, this);
    }

    public Matrix3x2f scaleAround(float sx, float sy, float ox, float oy, Matrix3x2f dest) {
        float nm20 = this.m00 * ox + this.m10 * oy + this.m20;
        float nm21 = this.m01 * ox + this.m11 * oy + this.m21;
        dest.m00 = this.m00 * sx;
        dest.m01 = this.m01 * sx;
        dest.m10 = this.m10 * sy;
        dest.m11 = this.m11 * sy;
        dest.m20 = dest.m00 * -ox + dest.m10 * -oy + nm20;
        dest.m21 = dest.m01 * -ox + dest.m11 * -oy + nm21;
        return dest;
    }

    public Matrix3x2f scaleAround(float sx, float sy, float ox, float oy) {
        return this.scaleAround(sx, sy, ox, oy, this);
    }

    public Matrix3x2f scaleAround(float factor, float ox, float oy, Matrix3x2f dest) {
        return this.scaleAround(factor, factor, ox, oy, this);
    }

    public Matrix3x2f scaleAround(float factor, float ox, float oy) {
        return this.scaleAround(factor, factor, ox, oy, this);
    }

    public Matrix3x2f scaleAroundLocal(float sx, float sy, float ox, float oy, Matrix3x2f dest) {
        dest.m00 = sx * this.m00;
        dest.m01 = sy * this.m01;
        dest.m10 = sx * this.m10;
        dest.m11 = sy * this.m11;
        dest.m20 = sx * this.m20 - sx * ox + ox;
        dest.m21 = sy * this.m21 - sy * oy + oy;
        return dest;
    }

    public Matrix3x2f scaleAroundLocal(float factor, float ox, float oy, Matrix3x2f dest) {
        return this.scaleAroundLocal(factor, factor, ox, oy, dest);
    }

    public Matrix3x2f scaleAroundLocal(float sx, float sy, float sz, float ox, float oy, float oz) {
        return this.scaleAroundLocal(sx, sy, ox, oy, this);
    }

    public Matrix3x2f scaleAroundLocal(float factor, float ox, float oy) {
        return this.scaleAroundLocal(factor, factor, ox, oy, this);
    }

    public Matrix3x2f scaling(float factor) {
        return this.scaling(factor, factor);
    }

    public Matrix3x2f scaling(float x, float y) {
        this.m00 = x;
        this.m01 = 0f;
        this.m10 = 0f;
        this.m11 = y;
        this.m20 = 0f;
        this.m21 = 0f;
        return this;
    }

    public Matrix3x2f rotation(float angle) {
        float cos = Math.cos(angle);
        float sin = Math.sin(angle);
        this.m00 = cos;
        this.m10 = -sin;
        this.m20 = 0f;
        this.m01 = sin;
        this.m11 = cos;
        this.m21 = 0f;
        return this;
    }

    public Vector3f transform(Vector3f v) {
        return v.mul(this);
    }

    public Vector3f transform(Vector3f v, Vector3f dest) {
        return v.mul(this, dest);
    }

    public Vector3f transform(float x, float y, float z, Vector3f dest) {
        return dest.set(this.m00 * x + this.m10 * y + this.m20 * z, this.m01 * x + this.m11 * y + this.m21 * z, z);
    }

    public Vector2f transformPosition(Vector2f v) {
        v.set(this.m00 * v.x + this.m10 * v.y + this.m20, this.m01 * v.x + this.m11 * v.y + this.m21);
        return v;
    }

    public Vector2f transformPosition(Vector2fc v, Vector2f dest) {
        dest.set(this.m00 * v.x() + this.m10 * v.y() + this.m20, this.m01 * v.x() + this.m11 * v.y() + this.m21);
        return dest;
    }

    public Vector2f transformPosition(float x, float y, Vector2f dest) {
        return dest.set(this.m00 * x + this.m10 * y + this.m20, this.m01 * x + this.m11 * y + this.m21);
    }

    public Vector2f transformDirection(Vector2f v) {
        v.set(this.m00 * v.x + this.m10 * v.y, this.m01 * v.x + this.m11 * v.y);
        return v;
    }

    public Vector2f transformDirection(Vector2fc v, Vector2f dest) {
        dest.set(this.m00 * v.x() + this.m10 * v.y(), this.m01 * v.x() + this.m11 * v.y());
        return dest;
    }

    public Vector2f transformDirection(float x, float y, Vector2f dest) {
        return dest.set(this.m00 * x + this.m10 * y, this.m01 * x + this.m11 * y);
    }

    public Matrix3x2f rotate(float ang) {
        return this.rotate(ang, this);
    }

    public Matrix3x2f rotate(float ang, Matrix3x2f dest) {
        float cos = Math.cos(ang);
        float sin = Math.sin(ang);
        float rm10 = -sin;
        float nm00 = this.m00 * cos + this.m10 * sin;
        float nm01 = this.m01 * cos + this.m11 * sin;
        dest.m10 = this.m00 * rm10 + this.m10 * cos;
        dest.m11 = this.m01 * rm10 + this.m11 * cos;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m20 = this.m20;
        dest.m21 = this.m21;
        return dest;
    }

    public Matrix3x2f rotateLocal(float ang, Matrix3x2f dest) {
        float sin = Math.sin(ang);
        float cos = Math.cosFromSin(sin, ang);
        float nm00 = cos * this.m00 - sin * this.m01;
        float nm01 = sin * this.m00 + cos * this.m01;
        float nm10 = cos * this.m10 - sin * this.m11;
        float nm11 = sin * this.m10 + cos * this.m11;
        float nm20 = cos * this.m20 - sin * this.m21;
        float nm21 = sin * this.m20 + cos * this.m21;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m20 = nm20;
        dest.m21 = nm21;
        return dest;
    }

    public Matrix3x2f rotateLocal(float ang) {
        return this.rotateLocal(ang, this);
    }

    public Matrix3x2f rotateAbout(float ang, float x, float y) {
        return this.rotateAbout(ang, x, y, this);
    }

    public Matrix3x2f rotateAbout(float ang, float x, float y, Matrix3x2f dest) {
        float tm20 = this.m00 * x + this.m10 * y + this.m20;
        float tm21 = this.m01 * x + this.m11 * y + this.m21;
        float cos = Math.cos(ang);
        float sin = Math.sin(ang);
        float nm00 = this.m00 * cos + this.m10 * sin;
        float nm01 = this.m01 * cos + this.m11 * sin;
        dest.m10 = this.m00 * -sin + this.m10 * cos;
        dest.m11 = this.m01 * -sin + this.m11 * cos;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m20 = dest.m00 * -x + dest.m10 * -y + tm20;
        dest.m21 = dest.m01 * -x + dest.m11 * -y + tm21;
        return dest;
    }

    public Matrix3x2f rotateTo(Vector2fc fromDir, Vector2fc toDir, Matrix3x2f dest) {
        float dot = fromDir.x() * toDir.x() + fromDir.y() * toDir.y();
        float det = fromDir.x() * toDir.y() - fromDir.y() * toDir.x();
        float rm10 = -det;
        float nm00 = this.m00 * dot + this.m10 * det;
        float nm01 = this.m01 * dot + this.m11 * det;
        dest.m10 = this.m00 * rm10 + this.m10 * dot;
        dest.m11 = this.m01 * rm10 + this.m11 * dot;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m20 = this.m20;
        dest.m21 = this.m21;
        return dest;
    }

    public Matrix3x2f rotateTo(Vector2fc fromDir, Vector2fc toDir) {
        return this.rotateTo(fromDir, toDir, this);
    }

    public Matrix3x2f view(float left, float right, float bottom, float top, Matrix3x2f dest) {
        float rm00 = 2f / (right - left);
        float rm11 = 2f / (top - bottom);
        float rm20 = (left + right) / (left - right);
        float rm21 = (bottom + top) / (bottom - top);
        dest.m20 = this.m00 * rm20 + this.m10 * rm21 + this.m20;
        dest.m21 = this.m01 * rm20 + this.m11 * rm21 + this.m21;
        dest.m00 = this.m00 * rm00;
        dest.m01 = this.m01 * rm00;
        dest.m10 = this.m10 * rm11;
        dest.m11 = this.m11 * rm11;
        return dest;
    }

    public Matrix3x2f view(float left, float right, float bottom, float top) {
        return this.view(left, right, bottom, top, this);
    }

    public Matrix3x2f setView(float left, float right, float bottom, float top) {
        this.m00 = 2f / (right - left);
        this.m01 = 0f;
        this.m10 = 0f;
        this.m11 = 2f / (top - bottom);
        this.m20 = (left + right) / (left - right);
        this.m21 = (bottom + top) / (bottom - top);
        return this;
    }

    public Vector2f origin(Vector2f origin) {
        float s = 1f / (this.m00 * this.m11 - this.m01 * this.m10);
        origin.x = (this.m10 * this.m21 - this.m20 * this.m11) * s;
        origin.y = (this.m20 * this.m01 - this.m00 * this.m21) * s;
        return origin;
    }

    public float[] viewArea(float[] area) {
        float s = 1f / (this.m00 * this.m11 - this.m01 * this.m10);
        float rm00 = this.m11 * s;
        float rm01 = -this.m01 * s;
        float rm10 = -this.m10 * s;
        float rm11 = this.m00 * s;
        float rm20 = (this.m10 * this.m21 - this.m20 * this.m11) * s;
        float rm21 = (this.m20 * this.m01 - this.m00 * this.m21) * s;
        float nxnyX = -rm00 - rm10;
        float nxnyY = -rm01 - rm11;
        float pxnyX = rm00 - rm10;
        float pxnyY = rm01 - rm11;
        float nxpyX = -rm00 + rm10;
        float nxpyY = -rm01 + rm11;
        float pxpyX = rm00 + rm10;
        float pxpyY = rm01 + rm11;
        float minX = java.lang.Math.min(nxnyX, nxpyX);
        minX = java.lang.Math.min(minX, pxnyX);
        minX = java.lang.Math.min(minX, pxpyX);
        float minY = java.lang.Math.min(nxnyY, nxpyY);
        minY = java.lang.Math.min(minY, pxnyY);
        minY = java.lang.Math.min(minY, pxpyY);
        float maxX = java.lang.Math.max(nxnyX, nxpyX);
        maxX = java.lang.Math.max(maxX, pxnyX);
        maxX = java.lang.Math.max(maxX, pxpyX);
        float maxY = java.lang.Math.max(nxnyY, nxpyY);
        maxY = java.lang.Math.max(maxY, pxnyY);
        maxY = java.lang.Math.max(maxY, pxpyY);
        area[0] = minX + rm20;
        area[1] = minY + rm21;
        area[2] = maxX + rm20;
        area[3] = maxY + rm21;
        return area;
    }

    public Vector2f positiveX(Vector2f dir) {
        float s = this.m00 * this.m11 - this.m01 * this.m10;
        s = 1f / s;
        dir.x = this.m11 * s;
        dir.y = -this.m01 * s;
        return dir.normalize(dir);
    }

    public Vector2f normalizedPositiveX(Vector2f dir) {
        dir.x = this.m11;
        dir.y = -this.m01;
        return dir;
    }

    public Vector2f positiveY(Vector2f dir) {
        float s = this.m00 * this.m11 - this.m01 * this.m10;
        s = 1f / s;
        dir.x = -this.m10 * s;
        dir.y = this.m00 * s;
        return dir.normalize(dir);
    }

    public Vector2f normalizedPositiveY(Vector2f dir) {
        dir.x = -this.m10;
        dir.y = this.m00;
        return dir;
    }

    public Vector2f unproject(float winX, float winY, int[] viewport, Vector2f dest) {
        float s = 1f / (this.m00 * this.m11 - this.m01 * this.m10);
        float im00 = this.m11 * s;
        float im01 = -this.m01 * s;
        float im10 = -this.m10 * s;
        float im11 = this.m00 * s;
        float im20 = (this.m10 * this.m21 - this.m20 * this.m11) * s;
        float im21 = (this.m20 * this.m01 - this.m00 * this.m21) * s;
        float ndcX = (winX - viewport[0]) / viewport[2] * 2f - 1f;
        float ndcY = (winY - viewport[1]) / viewport[3] * 2f - 1f;
        dest.x = im00 * ndcX + im10 * ndcY + im20;
        dest.y = im01 * ndcX + im11 * ndcY + im21;
        return dest;
    }

    public Vector2f unprojectInv(float winX, float winY, int[] viewport, Vector2f dest) {
        float ndcX = (winX - viewport[0]) / viewport[2] * 2f - 1f;
        float ndcY = (winY - viewport[1]) / viewport[3] * 2f - 1f;
        dest.x = this.m00 * ndcX + this.m10 * ndcY + this.m20;
        dest.y = this.m01 * ndcX + this.m11 * ndcY + this.m21;
        return dest;
    }

    public Matrix3x2f shearX(float yFactor) {
        return this.shearX(yFactor, this);
    }

    public Matrix3x2f shearX(float yFactor, Matrix3x2f dest) {
        float nm10 = this.m00 * yFactor + this.m10;
        float nm11 = this.m01 * yFactor + this.m11;
        dest.m00 = this.m00;
        dest.m01 = this.m01;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m20 = this.m20;
        dest.m21 = this.m21;
        return dest;
    }

    public Matrix3x2f shearY(float xFactor) {
        return this.shearY(xFactor, this);
    }

    public Matrix3x2f shearY(float xFactor, Matrix3x2f dest) {
        float nm00 = this.m00 + this.m10 * xFactor;
        float nm01 = this.m01 + this.m11 * xFactor;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m10 = this.m10;
        dest.m11 = this.m11;
        dest.m20 = this.m20;
        dest.m21 = this.m21;
        return dest;
    }

    public Matrix3x2f span(Vector2f corner, Vector2f xDir, Vector2f yDir) {
        float s = 1f / (this.m00 * this.m11 - this.m01 * this.m10);
        float nm00 = this.m11 * s;
        float nm01 = -this.m01 * s;
        float nm10 = -this.m10 * s;
        float nm11 = this.m00 * s;
        corner.x = -nm00 - nm10 + (this.m10 * this.m21 - this.m20 * this.m11) * s;
        corner.y = -nm01 - nm11 + (this.m20 * this.m01 - this.m00 * this.m21) * s;
        xDir.x = 2f * nm00;
        xDir.y = 2f * nm01;
        yDir.x = 2f * nm10;
        yDir.y = 2f * nm11;
        return this;
    }

    public boolean testPoint(float x, float y) {
        float nxX = this.m00;
        float nxY = this.m10;
        float nxW = 1f + this.m20;
        float pxX = -this.m00;
        float pxY = -this.m10;
        float pxW = 1f - this.m20;
        float nyX = this.m01;
        float nyY = this.m11;
        float nyW = 1f + this.m21;
        float pyX = -this.m01;
        float pyY = -this.m11;
        float pyW = 1f - this.m21;
        return nxX * x + nxY * y + nxW >= 0f && pxX * x + pxY * y + pxW >= 0f && nyX * x + nyY * y + nyW >= 0f && pyX * x + pyY * y + pyW >= 0f;
    }

    public boolean testCircle(float x, float y, float r) {
        float nxX = this.m00;
        float nxY = this.m10;
        float nxW = 1f + this.m20;
        float invl = Math.invsqrt(nxX * nxX + nxY * nxY);
        nxX *= invl;
        nxY *= invl;
        nxW *= invl;
        float pxX = -this.m00;
        float pxY = -this.m10;
        float pxW = 1f - this.m20;
        invl = Math.invsqrt(pxX * pxX + pxY * pxY);
        pxX *= invl;
        pxY *= invl;
        pxW *= invl;
        float nyX = this.m01;
        float nyY = this.m11;
        float nyW = 1f + this.m21;
        invl = Math.invsqrt(nyX * nyX + nyY * nyY);
        nyX *= invl;
        nyY *= invl;
        nyW *= invl;
        float pyX = -this.m01;
        float pyY = -this.m11;
        float pyW = 1f - this.m21;
        invl = Math.invsqrt(pyX * pyX + pyY * pyY);
        pyX *= invl;
        pyY *= invl;
        pyW *= invl;
        return nxX * x + nxY * y + nxW >= -r && pxX * x + pxY * y + pxW >= -r && nyX * x + nyY * y + nyW >= -r && pyX * x + pyY * y + pyW >= -r;
    }

    public boolean testAar(float minX, float minY, float maxX, float maxY) {
        float nxX = this.m00;
        float nxY = this.m10;
        float nxW = 1f + this.m20;
        float pxX = -this.m00;
        float pxY = -this.m10;
        float pxW = 1f - this.m20;
        float nyX = this.m01;
        float nyY = this.m11;
        float nyW = 1f + this.m21;
        float pyX = -this.m01;
        float pyY = -this.m11;
        float pyW = 1f - this.m21;
        return nxX * (nxX < 0f ? minX : maxX) + nxY * (nxY < 0f ? minY : maxY) >= -nxW && pxX * (pxX < 0f ? minX : maxX) + pxY * (pxY < 0f ? minY : maxY) >= -pxW && nyX * (nyX < 0f ? minX : maxX) + nyY * (nyY < 0f ? minY : maxY) >= -nyW && pyX * (pyX < 0f ? minX : maxX) + pyY * (pyY < 0f ? minY : maxY) >= -pyW;
    }

    public int hashCode() {
        int result = 1;
        result = 31 * result + Float.floatToIntBits(this.m00);
        result = 31 * result + Float.floatToIntBits(this.m01);
        result = 31 * result + Float.floatToIntBits(this.m10);
        result = 31 * result + Float.floatToIntBits(this.m11);
        result = 31 * result + Float.floatToIntBits(this.m20);
        result = 31 * result + Float.floatToIntBits(this.m21);
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
            Matrix3x2f other = (Matrix3x2f) obj;
            if (Float.floatToIntBits(this.m00) != Float.floatToIntBits(other.m00)) {
                return false;
            } else if (Float.floatToIntBits(this.m01) != Float.floatToIntBits(other.m01)) {
                return false;
            } else if (Float.floatToIntBits(this.m10) != Float.floatToIntBits(other.m10)) {
                return false;
            } else if (Float.floatToIntBits(this.m11) != Float.floatToIntBits(other.m11)) {
                return false;
            } else if (Float.floatToIntBits(this.m20) != Float.floatToIntBits(other.m20)) {
                return false;
            } else {
                return Float.floatToIntBits(this.m21) == Float.floatToIntBits(other.m21);
            }
        }
    }

    public boolean equals(Matrix3x2f m, float delta) {
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
        } else if (!Runtime.equals(this.m11, m.m11, delta)) {
            return false;
        } else if (!Runtime.equals(this.m20, m.m20, delta)) {
            return false;
        } else {
            return Runtime.equals(this.m21, m.m21, delta);
        }
    }

    public boolean isFinite() {
        return Math.isFinite(this.m00) && Math.isFinite(this.m01) && Math.isFinite(this.m10) && Math.isFinite(this.m11) && Math.isFinite(this.m20) && Math.isFinite(this.m21);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
