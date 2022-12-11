package me.anno.utils.pooling;

import org.joml.*;

// can be converted to Kotlin without any issues
// is in Java to prevent a lot of tiny classes from being created for lambdas, and objects (that's the worst part...)
// (small optimization for JVM2WASM, might help JVM as well)
public class JomlPools {

    public static final Stack<Vector2i> vec2i = new Stack<>(Vector2i.class);
    public static final Stack<Vector2f> vec2f = new Stack<>(Vector2f.class);
    public static final Stack<Vector2d> vec2d = new Stack<>(Vector2d.class);

    public static final Stack<Vector3i> vec3i = new Stack<>(Vector3i.class);
    public static final Stack<Vector3f> vec3f = new Stack<>(Vector3f.class);
    public static final Stack<Vector3d> vec3d = new Stack<>(Vector3d.class);

    public static final Stack<Vector4i> vec4i = new Stack<>(Vector4i.class);
    public static final Stack<Vector4f> vec4f = new Stack<>(Vector4f.class);
    public static final Stack<Vector4d> vec4d = new Stack<>(Vector4d.class);

    public static final Stack<Quaternionf> quat4f = new Stack<>(Quaternionf.class);
    public static final Stack<Quaterniond> quat4d = new Stack<>(Quaterniond.class);

    public static final Stack<Matrix2f> mat2f = new Stack<>(Matrix2f.class);
    public static final Stack<Matrix2d> mat2d = new Stack<>(Matrix2d.class);

    public static final Stack<Matrix3f> mat3f = new Stack<>(Matrix3f.class);
    public static final Stack<Matrix3d> mat3d = new Stack<>(Matrix3d.class);

    public static final Stack<Matrix4f> mat4f = new Stack<>(Matrix4f.class);
    public static final Stack<Matrix4d> mat4d = new Stack<>(Matrix4d.class);

    public static final Stack<Matrix4x3f> mat4x3f = new Stack<>(Matrix4x3f.class);
    public static final Stack<Matrix4x3d> mat4x3d = new Stack<>(Matrix4x3d.class);

    public static final Stack<AABBf> aabbf = new Stack<>(AABBf.class);
    public static final Stack<AABBd> aabbd = new Stack<>(AABBd.class);

    public static void reset() {

        vec2i.reset();
        vec2f.reset();
        vec2d.reset();

        vec3i.reset();
        vec3f.reset();
        vec3d.reset();

        vec4i.reset();
        vec4f.reset();
        vec4d.reset();

        quat4f.reset();
        quat4d.reset();

        mat2f.reset();
        mat2d.reset();

        mat3f.reset();
        mat3d.reset();

        mat4f.reset();
        mat4d.reset();

        mat4x3f.reset();
        mat4x3d.reset();

        aabbf.reset();
        aabbd.reset();

    }

}