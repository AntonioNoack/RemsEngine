package org.joml;

@SuppressWarnings("unused")
public class Vector4i {

    public int x;
    public int y;
    public int z;
    public int w;

    public Vector4i() {
        this.w = 1;
    }

    public Vector4i(Vector4i v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = v.w;
    }

    public Vector4i(Vector3i v, int w) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = w;
    }

    public Vector4i(Vector2i v, int z, int w) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        this.w = w;
    }

    public Vector4i(Vector3f v, float w, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
        this.z = Math.roundUsing(v.z, mode);
        this.w = Math.roundUsing(w, mode);
    }

    public Vector4i(Vector4f v, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
        this.z = Math.roundUsing(v.z, mode);
        this.w = Math.roundUsing(v.w, mode);
    }

    public Vector4i(Vector4d v, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
        this.z = Math.roundUsing(v.z, mode);
        this.w = Math.roundUsing(v.w, mode);
    }

    public Vector4i(int s) {
        this.x = s;
        this.y = s;
        this.z = s;
        this.w = s;
    }

    public Vector4i(int x, int y, int z, int w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4i(int[] xyzw) {
        this.x = xyzw[0];
        this.y = xyzw[1];
        this.z = xyzw[2];
        this.w = xyzw[3];
    }

    public Vector4i set(Vector4i v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = v.w;
        return this;
    }

    public Vector4i set(Vector4d v) {
        this.x = (int) v.x;
        this.y = (int) v.y;
        this.z = (int) v.z;
        this.w = (int) v.w;
        return this;
    }

    public Vector4i set(Vector4d v, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
        this.z = Math.roundUsing(v.z, mode);
        this.w = Math.roundUsing(v.w, mode);
        return this;
    }

    public Vector4i set(Vector4f v, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
        this.z = Math.roundUsing(v.z, mode);
        this.w = Math.roundUsing(v.w, mode);
        return this;
    }

    public Vector4i set(Vector3i v, int w) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = w;
        return this;
    }

    public Vector4i set(Vector2i v, int z, int w) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Vector4i set(int s) {
        this.x = s;
        this.y = s;
        this.z = s;
        this.w = s;
        return this;
    }

    public Vector4i set(int x, int y, int z, int w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Vector4i set(int[] xyzw) {
        this.x = xyzw[0];
        this.y = xyzw[1];
        this.z = xyzw[2];
        this.w = xyzw[3];
        return this;
    }

    public int get(int component) throws IllegalArgumentException {
        switch (component) {
            case 0:
                return this.x;
            case 1:
                return this.y;
            case 2:
                return this.z;
            case 3:
                return this.w;
            default:
                throw new IllegalArgumentException();
        }
    }

    public int maxComponent() {
        int absX = Math.abs(this.x);
        int absY = Math.abs(this.y);
        int absZ = Math.abs(this.z);
        int absW = Math.abs(this.w);
        if (absX >= absY && absX >= absZ && absX >= absW) {
            return 0;
        } else if (absY >= absZ && absY >= absW) {
            return 1;
        } else {
            return absZ >= absW ? 2 : 3;
        }
    }

    public int minComponent() {
        int absX = Math.abs(this.x);
        int absY = Math.abs(this.y);
        int absZ = Math.abs(this.z);
        int absW = Math.abs(this.w);
        if (absX < absY && absX < absZ && absX < absW) {
            return 0;
        } else if (absY < absZ && absY < absW) {
            return 1;
        } else {
            return absZ < absW ? 2 : 3;
        }
    }

    public Vector4i setComponent(int component, int value) throws IllegalArgumentException {
        switch (component) {
            case 0:
                this.x = value;
                break;
            case 1:
                this.y = value;
                break;
            case 2:
                this.z = value;
                break;
            case 3:
                this.w = value;
                break;
            default:
                throw new IllegalArgumentException();
        }

        return this;
    }

    public Vector4i sub(Vector4i v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        this.w -= v.w;
        return this;
    }

    public Vector4i sub(int x, int y, int z, int w) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        this.w -= w;
        return this;
    }

    public Vector4i sub(Vector4i v, Vector4i dest) {
        dest.x = this.x - v.x;
        dest.y = this.y - v.y;
        dest.z = this.z - v.z;
        dest.w = this.w - v.w;
        return dest;
    }

    public Vector4i sub(int x, int y, int z, int w, Vector4i dest) {
        dest.x = this.x - x;
        dest.y = this.y - y;
        dest.z = this.z - z;
        dest.w = this.w - w;
        return dest;
    }

    public Vector4i add(Vector4i v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        this.w += v.w;
        return this;
    }

    public Vector4i add(Vector4i v, Vector4i dest) {
        dest.x = this.x + v.x;
        dest.y = this.y + v.y;
        dest.z = this.z + v.z;
        dest.w = this.w + v.w;
        return dest;
    }

    public Vector4i add(int x, int y, int z, int w) {
        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;
        return this;
    }

    public Vector4i add(int x, int y, int z, int w, Vector4i dest) {
        dest.x = this.x + x;
        dest.y = this.y + y;
        dest.z = this.z + z;
        dest.w = this.w + w;
        return dest;
    }

    public Vector4i mul(Vector4i v) {
        this.x *= v.x;
        this.y *= v.y;
        this.z *= v.z;
        this.w *= v.w;
        return this;
    }

    public Vector4i mul(Vector4i v, Vector4i dest) {
        dest.x = this.x * v.x;
        dest.y = this.y * v.y;
        dest.z = this.z * v.z;
        dest.w = this.w * v.w;
        return dest;
    }

    public Vector4i div(Vector4i v) {
        this.x /= v.x;
        this.y /= v.y;
        this.z /= v.z;
        this.w /= v.w;
        return this;
    }

    public Vector4i div(Vector4i v, Vector4i dest) {
        dest.x = this.x / v.x;
        dest.y = this.y / v.y;
        dest.z = this.z / v.z;
        dest.w = this.w / v.w;
        return dest;
    }

    public Vector4i mul(int scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        this.w *= scalar;
        return this;
    }

    public Vector4i mul(int scalar, Vector4i dest) {
        dest.x = this.x * scalar;
        dest.y = this.y * scalar;
        dest.z = this.z * scalar;
        dest.w = this.w * scalar;
        return dest;
    }

    public Vector4i div(float scalar) {
        float inv = 1f / scalar;
        this.x = (int) ((float) this.x * inv);
        this.y = (int) ((float) this.y * inv);
        this.z = (int) ((float) this.z * inv);
        this.w = (int) ((float) this.w * inv);
        return this;
    }

    public Vector4i div(float scalar, Vector4i dest) {
        float inv = 1f / scalar;
        dest.x = (int) ((float) this.x * inv);
        dest.y = (int) ((float) this.y * inv);
        dest.z = (int) ((float) this.z * inv);
        dest.w = (int) ((float) this.w * inv);
        return dest;
    }

    public Vector4i div(int scalar) {
        this.x /= scalar;
        this.y /= scalar;
        this.z /= scalar;
        this.w /= scalar;
        return this;
    }

    public Vector4i div(int scalar, Vector4i dest) {
        dest.x = this.x / scalar;
        dest.y = this.y / scalar;
        dest.z = this.z / scalar;
        dest.w = this.w / scalar;
        return dest;
    }

    public long lengthSquared() {
        return (long) this.x * this.x + (long) this.y * this.y + (long) this.z * this.z + (long) this.w * this.w;
    }

    public static long lengthSquared(int x, int y, int z, int w) {
        return (long) x * x + (long) y * y + (long) z * z + (long) w * w;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public static double length(int x, int y, int z, int w) {
        return Math.sqrt(((long) x * x + (long) y * y + (long) z * z + (long) w * w));
    }

    public double distance(Vector4i v) {
        int dx = this.x - v.x;
        int dy = this.y - v.y;
        int dz = this.z - v.z;
        int dw = this.w - v.w;
        return length(dx, dy, dz, dw);
    }

    public double distance(int x, int y, int z, int w) {
        int dx = this.x - x;
        int dy = this.y - y;
        int dz = this.z - z;
        int dw = this.w - w;
        return length(dx, dy, dz, dw);
    }

    public long gridDistance(Vector4i v) {
        return Math.abs(v.x - this.x) + Math.abs(v.y - this.y) + Math.abs(v.z - this.z) + Math.abs(v.w - this.w);
    }

    public long gridDistance(int x, int y, int z, int w) {
        return Math.abs(x - this.x) + Math.abs(y - this.y) + Math.abs(z - this.z) + Math.abs(w - this.w);
    }

    public long distanceSquared(Vector4i v) {
        int dx = this.x - v.x;
        int dy = this.y - v.y;
        int dz = this.z - v.z;
        int dw = this.w - v.w;
        return lengthSquared(dx, dy, dz, dw);
    }

    public long distanceSquared(int x, int y, int z, int w) {
        int dx = this.x - x;
        int dy = this.y - y;
        int dz = this.z - z;
        int dw = this.w - w;
        return lengthSquared(dx, dy, dz, dw);
    }

    public static double distance(int x1, int y1, int z1, int w1, int x2, int y2, int z2, int w2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        int dz = z1 - z2;
        int dw = w1 - w2;
        return Math.sqrt(lengthSquared(dx, dy, dz, dw));
    }

    public static long distanceSquared(int x1, int y1, int z1, int w1, int x2, int y2, int z2, int w2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        int dz = z1 - z2;
        int dw = w1 - w2;
        return lengthSquared(dx, dy, dz, dw);
    }

    public int dot(Vector4i v) {
        return this.x * v.x + this.y * v.y + this.z * v.z + this.w * v.w;
    }

    public Vector4i zero() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.w = 0;
        return this;
    }

    public Vector4i negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        this.w = -this.w;
        return this;
    }

    public Vector4i negate(Vector4i dest) {
        dest.x = -this.x;
        dest.y = -this.y;
        dest.z = -this.z;
        dest.w = -this.w;
        return dest;
    }

    public String toString() {
        return "(" + x + "," + y + "," +z + "," + w + ")";
    }

    public Vector4i min(Vector4i v) {
        this.x = java.lang.Math.min(this.x, v.x);
        this.y = java.lang.Math.min(this.y, v.y);
        this.z = java.lang.Math.min(this.z, v.z);
        this.w = java.lang.Math.min(this.w, v.w);
        return this;
    }

    public Vector4i min(Vector4i v, Vector4i dest) {
        dest.x = java.lang.Math.min(this.x, v.x);
        dest.y = java.lang.Math.min(this.y, v.y);
        dest.z = java.lang.Math.min(this.z, v.z);
        dest.w = java.lang.Math.min(this.w, v.w);
        return dest;
    }

    public Vector4i max(Vector4i v) {
        this.x = java.lang.Math.max(this.x, v.x);
        this.y = java.lang.Math.max(this.y, v.y);
        this.z = java.lang.Math.max(this.z, v.z);
        this.w = java.lang.Math.max(this.w, v.w);
        return this;
    }

    public Vector4i max(Vector4i v, Vector4i dest) {
        dest.x = java.lang.Math.max(this.x, v.x);
        dest.y = java.lang.Math.max(this.y, v.y);
        dest.z = java.lang.Math.max(this.z, v.z);
        dest.w = java.lang.Math.max(this.w, v.w);
        return dest;
    }

    public Vector4i absolute() {
        this.x = Math.abs(this.x);
        this.y = Math.abs(this.y);
        this.z = Math.abs(this.z);
        this.w = Math.abs(this.w);
        return this;
    }

    public Vector4i absolute(Vector4i dest) {
        dest.x = Math.abs(this.x);
        dest.y = Math.abs(this.y);
        dest.z = Math.abs(this.z);
        dest.w = Math.abs(this.w);
        return dest;
    }

    public int hashCode() {
        int result = 1;
        result = 31 * result + this.x;
        result = 31 * result + this.y;
        result = 31 * result + this.z;
        result = 31 * result + this.w;
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
            Vector4i other = (Vector4i) obj;
            if (this.x != other.x) {
                return false;
            } else if (this.y != other.y) {
                return false;
            } else if (this.z != other.z) {
                return false;
            } else {
                return this.w == other.w;
            }
        }
    }

    public boolean equals(int x, int y, int z, int w) {
        if (this.x != x) {
            return false;
        } else if (this.y != y) {
            return false;
        } else if (this.z != z) {
            return false;
        } else {
            return this.w == w;
        }
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
