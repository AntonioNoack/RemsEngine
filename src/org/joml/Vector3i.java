package org.joml;

@SuppressWarnings("unused")
public class Vector3i {

    public int x;
    public int y;
    public int z;

    public Vector3i() {
    }

    public Vector3i(int d) {
        this.x = d;
        this.y = d;
        this.z = d;
    }

    public Vector3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3i(Vector3i v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    public Vector3i(Vector2i v, int z) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
    }

    public Vector3i(float x, float y, float z, int mode) {
        this.x = Math.roundUsing(x, mode);
        this.y = Math.roundUsing(y, mode);
        this.z = Math.roundUsing(z, mode);
    }

    public Vector3i(double x, double y, double z, int mode) {
        this.x = Math.roundUsing(x, mode);
        this.y = Math.roundUsing(y, mode);
        this.z = Math.roundUsing(z, mode);
    }

    public Vector3i(Vector2f v, float z, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
        this.z = Math.roundUsing(z, mode);
    }

    public Vector3i(Vector3f v, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
        this.z = Math.roundUsing(v.z, mode);
    }

    public Vector3i(Vector2d v, float z, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
        this.z = Math.roundUsing(z, mode);
    }

    public Vector3i(Vector3d v, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
        this.z = Math.roundUsing(v.z, mode);
    }

    public Vector3i(int[] xyz) {
        this.x = xyz[0];
        this.y = xyz[1];
        this.z = xyz[2];
    }

    public Vector3i set(Vector3i v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        return this;
    }

    public Vector3i set(Vector3d v) {
        this.x = (int) v.x;
        this.y = (int) v.y;
        this.z = (int) v.z;
        return this;
    }

    public Vector3i set(Vector3d v, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
        this.z = Math.roundUsing(v.z, mode);
        return this;
    }

    public Vector3i set(Vector3f v, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
        this.z = Math.roundUsing(v.z, mode);
        return this;
    }

    public Vector3i set(Vector2i v, int z) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        return this;
    }

    public Vector3i set(int d) {
        this.x = d;
        this.y = d;
        this.z = d;
        return this;
    }

    public Vector3i set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3i set(int[] xyz) {
        this.x = xyz[0];
        this.y = xyz[1];
        this.z = xyz[2];
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
            default:
                throw new IllegalArgumentException();
        }
    }

    public Vector3i setComponent(int component, int value) throws IllegalArgumentException {
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
            default:
                throw new IllegalArgumentException();
        }

        return this;
    }

    public Vector3i sub(Vector3i v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        return this;
    }

    public Vector3i sub(Vector3i v, Vector3i dest) {
        dest.x = this.x - v.x;
        dest.y = this.y - v.y;
        dest.z = this.z - v.z;
        return dest;
    }

    public Vector3i sub(int x, int y, int z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    public Vector3i sub(int x, int y, int z, Vector3i dest) {
        dest.x = this.x - x;
        dest.y = this.y - y;
        dest.z = this.z - z;
        return dest;
    }

    public Vector3i add(Vector3i v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        return this;
    }

    public Vector3i add(Vector3i v, Vector3i dest) {
        dest.x = this.x + v.x;
        dest.y = this.y + v.y;
        dest.z = this.z + v.z;
        return dest;
    }

    public Vector3i add(int x, int y, int z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Vector3i add(int x, int y, int z, Vector3i dest) {
        dest.x = this.x + x;
        dest.y = this.y + y;
        dest.z = this.z + z;
        return dest;
    }

    public Vector3i mul(int scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        return this;
    }

    public Vector3i mul(int scalar, Vector3i dest) {
        dest.x = this.x * scalar;
        dest.y = this.y * scalar;
        dest.z = this.z * scalar;
        return dest;
    }

    public Vector3i mul(Vector3i v) {
        this.x *= v.x;
        this.y *= v.y;
        this.z *= v.z;
        return this;
    }

    public Vector3i mul(Vector3i v, Vector3i dest) {
        dest.x = this.x * v.x;
        dest.y = this.y * v.y;
        dest.z = this.z * v.z;
        return dest;
    }

    public Vector3i mul(int x, int y, int z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
        return this;
    }

    public Vector3i mul(int x, int y, int z, Vector3i dest) {
        dest.x = this.x * x;
        dest.y = this.y * y;
        dest.z = this.z * z;
        return dest;
    }

    public Vector3i div(float scalar) {
        float inv = 1f / scalar;
        this.x = (int) ((float) this.x * inv);
        this.y = (int) ((float) this.y * inv);
        this.z = (int) ((float) this.z * inv);
        return this;
    }

    public Vector3i div(float scalar, Vector3i dest) {
        float inv = 1f / scalar;
        dest.x = (int) ((float) this.x * inv);
        dest.y = (int) ((float) this.y * inv);
        dest.z = (int) ((float) this.z * inv);
        return dest;
    }

    public Vector3i div(int scalar) {
        this.x /= scalar;
        this.y /= scalar;
        this.z /= scalar;
        return this;
    }

    public Vector3i div(int scalar, Vector3i dest) {
        dest.x = this.x / scalar;
        dest.y = this.y / scalar;
        dest.z = this.z / scalar;
        return dest;
    }

    public long lengthSquared() {
        return (long) this.x * this.x + (long) this.y * this.y + (long) this.z * this.z;
    }

    public static long lengthSquared(int x, int y, int z) {
        return (long) x * x + (long) y * y + (long) z * z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public static double length(int x, int y, int z) {
        return Math.sqrt(lengthSquared(x, y, z));
    }

    public double distance(Vector3i v) {
        int dx = this.x - v.x;
        int dy = this.y - v.y;
        int dz = this.z - v.z;
        return length(dx, dy, dz);
    }

    public double distance(int x, int y, int z) {
        int dx = this.x - x;
        int dy = this.y - y;
        int dz = this.z - z;
        return lengthSquared(dx, dy, dz);
    }

    public long gridDistance(Vector3i v) {
        return Math.abs(v.x - this.x) + Math.abs(v.y - this.y) + Math.abs(v.z - this.z);
    }

    public long gridDistance(int x, int y, int z) {
        return Math.abs(x - this.x) + Math.abs(y - this.y) + Math.abs(z - this.z);
    }

    public long distanceSquared(Vector3i v) {
        int dx = this.x - v.x;
        int dy = this.y - v.y;
        int dz = this.z - v.z;
        return lengthSquared(dx, dy, dz);
    }

    public long distanceSquared(int x, int y, int z) {
        int dx = this.x - x;
        int dy = this.y - y;
        int dz = this.z - z;
        return lengthSquared(dx, dy, dz);
    }

    public static double distance(int x1, int y1, int z1, int x2, int y2, int z2) {
        return Math.sqrt(lengthSquared(x1 - x2, y1 - y2, z1 - z2));
    }

    public static long distanceSquared(int x1, int y1, int z1, int x2, int y2, int z2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        int dz = z1 - z2;
        return lengthSquared(dx, dy, dz);
    }

    public Vector3i zero() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        return this;
    }

    public String toString() {
        return "(" + x + "," + y + "," + z + ")";
    }

    public Vector3i negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        return this;
    }

    public Vector3i negate(Vector3i dest) {
        dest.x = -this.x;
        dest.y = -this.y;
        dest.z = -this.z;
        return dest;
    }

    public Vector3i min(Vector3i v) {
        this.x = java.lang.Math.min(this.x, v.x);
        this.y = java.lang.Math.min(this.y, v.y);
        this.z = java.lang.Math.min(this.z, v.z);
        return this;
    }

    public Vector3i min(Vector3i v, Vector3i dest) {
        dest.x = java.lang.Math.min(this.x, v.x);
        dest.y = java.lang.Math.min(this.y, v.y);
        dest.z = java.lang.Math.min(this.z, v.z);
        return dest;
    }

    public Vector3i max(Vector3i v) {
        this.x = java.lang.Math.max(this.x, v.x);
        this.y = java.lang.Math.max(this.y, v.y);
        this.z = java.lang.Math.max(this.z, v.z);
        return this;
    }

    public Vector3i max(Vector3i v, Vector3i dest) {
        dest.x = java.lang.Math.max(this.x, v.x);
        dest.y = java.lang.Math.max(this.y, v.y);
        dest.z = java.lang.Math.max(this.z, v.z);
        return dest;
    }

    public int maxComponent() {
        float absX = (float) Math.abs(this.x);
        float absY = (float) Math.abs(this.y);
        float absZ = (float) Math.abs(this.z);
        if (absX >= absY && absX >= absZ) {
            return 0;
        } else {
            return absY >= absZ ? 1 : 2;
        }
    }

    public int minComponent() {
        float absX = (float) Math.abs(this.x);
        float absY = (float) Math.abs(this.y);
        float absZ = (float) Math.abs(this.z);
        if (absX < absY && absX < absZ) {
            return 0;
        } else {
            return absY < absZ ? 1 : 2;
        }
    }

    public Vector3i absolute() {
        this.x = Math.abs(this.x);
        this.y = Math.abs(this.y);
        this.z = Math.abs(this.z);
        return this;
    }

    public Vector3i absolute(Vector3i dest) {
        dest.x = Math.abs(this.x);
        dest.y = Math.abs(this.y);
        dest.z = Math.abs(this.z);
        return dest;
    }

    public int hashCode() {
        int result = 1;
        result = 31 * result + this.x;
        result = 31 * result + this.y;
        result = 31 * result + this.z;
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
            Vector3i other = (Vector3i) obj;
            if (this.x != other.x) {
                return false;
            } else if (this.y != other.y) {
                return false;
            } else {
                return this.z == other.z;
            }
        }
    }

    public boolean equals(int x, int y, int z) {
        if (this.x != x) {
            return false;
        } else if (this.y != y) {
            return false;
        } else {
            return this.z == z;
        }
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
