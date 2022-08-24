package org.joml;

@SuppressWarnings("unused")
public class Vector2i {

    public int x;
    public int y;

    public Vector2i() {
    }

    public Vector2i(int s) {
        this.x = s;
        this.y = s;
    }

    public Vector2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Vector2i(float x, float y, int mode) {
        this.x = Math.roundUsing(x, mode);
        this.y = Math.roundUsing(y, mode);
    }

    public Vector2i(double x, double y, int mode) {
        this.x = Math.roundUsing(x, mode);
        this.y = Math.roundUsing(y, mode);
    }

    public Vector2i(Vector2i v) {
        this.x = v.x;
        this.y = v.y;
    }

    public Vector2i(Vector2f v, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
    }

    public Vector2i(Vector2d v, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
    }

    public Vector2i(int[] xy) {
        this.x = xy[0];
        this.y = xy[1];
    }

    public Vector2i set(int s) {
        this.x = s;
        this.y = s;
        return this;
    }

    public Vector2i set(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector2i set(Vector2i v) {
        this.x = v.x;
        this.y = v.y;
        return this;
    }

    public Vector2i set(Vector2d v) {
        this.x = (int) v.x;
        this.y = (int) v.y;
        return this;
    }

    public Vector2i set(Vector2d v, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
        return this;
    }

    public Vector2i set(Vector2f v, int mode) {
        this.x = Math.roundUsing(v.x, mode);
        this.y = Math.roundUsing(v.y, mode);
        return this;
    }

    public Vector2i set(int[] xy) {
        this.x = xy[0];
        this.y = xy[1];
        return this;
    }

    public int get(int component) throws IllegalArgumentException {
        switch (component) {
            case 0:
                return this.x;
            case 1:
                return this.y;
            default:
                throw new IllegalArgumentException();
        }
    }

    public Vector2i setComponent(int component, int value) throws IllegalArgumentException {
        switch (component) {
            case 0:
                this.x = value;
                break;
            case 1:
                this.y = value;
                break;
            default:
                throw new IllegalArgumentException();
        }

        return this;
    }

    public Vector2i sub(Vector2i v) {
        this.x -= v.x;
        this.y -= v.y;
        return this;
    }

    public Vector2i sub(Vector2i v, Vector2i dest) {
        dest.x = this.x - v.x;
        dest.y = this.y - v.y;
        return dest;
    }

    public Vector2i sub(int x, int y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    public Vector2i sub(int x, int y, Vector2i dest) {
        dest.x = this.x - x;
        dest.y = this.y - y;
        return dest;
    }

    public long lengthSquared() {
        return (long) this.x * this.x + (long) this.y * this.y;
    }

    public static long lengthSquared(int x, int y) {
        return (long) x * x + (long) y * y;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public static double length(int x, int y) {
        return Math.sqrt(((long) x * x + (long) y * y));
    }

    public double distance(Vector2i v) {
        int dx = this.x - v.x;
        int dy = this.y - v.y;
        return Math.sqrt(((long) dx * dx + (long) dy * dy));
    }

    public double distance(int x, int y) {
        int dx = this.x - x;
        int dy = this.y - y;
        return Math.sqrt(((long) dx * dx + (long) dy * dy));
    }

    public long distanceSquared(Vector2i v) {
        int dx = this.x - v.x;
        int dy = this.y - v.y;
        return (long) dx * dx + (long) dy * dy;
    }

    public long distanceSquared(int x, int y) {
        int dx = this.x - x;
        int dy = this.y - y;
        return (long) dx * dx + (long) dy * dy;
    }

    public long gridDistance(Vector2i v) {
        return Math.abs(v.x - this.x) + Math.abs(v.y - this.y);
    }

    public long gridDistance(int x, int y) {
        return Math.abs(x - this.x) + Math.abs(y - this.y);
    }

    public static double distance(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return Math.sqrt((long) dx * dx + (long) dy * dy);
    }

    public static long distanceSquared(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return (long) dx * dx + (long) dy * dy;
    }

    public Vector2i add(Vector2i v) {
        this.x += v.x;
        this.y += v.y;
        return this;
    }

    public Vector2i add(Vector2i v, Vector2i dest) {
        dest.x = this.x + v.x;
        dest.y = this.y + v.y;
        return dest;
    }

    public Vector2i add(int x, int y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public Vector2i add(int x, int y, Vector2i dest) {
        dest.x = this.x + x;
        dest.y = this.y + y;
        return dest;
    }

    public Vector2i mul(int scalar) {
        this.x *= scalar;
        this.y *= scalar;
        return this;
    }

    public Vector2i mul(int scalar, Vector2i dest) {
        dest.x = this.x * scalar;
        dest.y = this.y * scalar;
        return dest;
    }

    public Vector2i mul(Vector2i v) {
        this.x *= v.x;
        this.y *= v.y;
        return this;
    }

    public Vector2i mul(Vector2i v, Vector2i dest) {
        dest.x = this.x * v.x;
        dest.y = this.y * v.y;
        return dest;
    }

    public Vector2i mul(int x, int y) {
        this.x *= x;
        this.y *= y;
        return this;
    }

    public Vector2i mul(int x, int y, Vector2i dest) {
        dest.x = this.x * x;
        dest.y = this.y * y;
        return dest;
    }

    public Vector2i div(float scalar) {
        float inv = 1f / scalar;
        this.x = (int) ((float) this.x * inv);
        this.y = (int) ((float) this.y * inv);
        return this;
    }

    public Vector2i div(float scalar, Vector2i dest) {
        float inv = 1f / scalar;
        dest.x = (int) ((float) this.x * inv);
        dest.y = (int) ((float) this.y * inv);
        return dest;
    }

    public Vector2i div(int scalar) {
        this.x /= scalar;
        this.y /= scalar;
        return this;
    }

    public Vector2i div(int scalar, Vector2i dest) {
        dest.x = this.x / scalar;
        dest.y = this.y / scalar;
        return dest;
    }

    public Vector2i zero() {
        this.x = 0;
        this.y = 0;
        return this;
    }

    public Vector2i negate() {
        this.x = -this.x;
        this.y = -this.y;
        return this;
    }

    public Vector2i negate(Vector2i dest) {
        dest.x = -this.x;
        dest.y = -this.y;
        return dest;
    }

    public Vector2i min(Vector2i v) {
        this.x = java.lang.Math.min(this.x, v.x);
        this.y = java.lang.Math.min(this.y, v.y);
        return this;
    }

    public Vector2i min(Vector2i v, Vector2i dest) {
        dest.x = java.lang.Math.min(this.x, v.x);
        dest.y = java.lang.Math.min(this.y, v.y);
        return dest;
    }

    public Vector2i max(Vector2i v) {
        this.x = java.lang.Math.max(this.x, v.x);
        this.y = java.lang.Math.max(this.y, v.y);
        return this;
    }

    public Vector2i max(Vector2i v, Vector2i dest) {
        dest.x = java.lang.Math.max(this.x, v.x);
        dest.y = java.lang.Math.max(this.y, v.y);
        return dest;
    }

    public int maxComponent() {
        int absX = Math.abs(this.x);
        int absY = Math.abs(this.y);
        return absX >= absY ? 0 : 1;
    }

    public int minComponent() {
        int absX = Math.abs(this.x);
        int absY = Math.abs(this.y);
        return absX < absY ? 0 : 1;
    }

    public Vector2i absolute() {
        this.x = Math.abs(this.x);
        this.y = Math.abs(this.y);
        return this;
    }

    public Vector2i absolute(Vector2i dest) {
        dest.x = Math.abs(this.x);
        dest.y = Math.abs(this.y);
        return dest;
    }

    public int hashCode() {
        int result = 1;
        result = 31 * result + this.x;
        result = 31 * result + this.y;
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
            Vector2i other = (Vector2i) obj;
            if (this.x != other.x) {
                return false;
            } else {
                return this.y == other.y;
            }
        }
    }

    public boolean equals(int x, int y) {
        if (this.x != x) {
            return false;
        } else {
            return this.y == y;
        }
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
