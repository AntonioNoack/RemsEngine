package me.anno.io.binary;

public class BinaryTypes {
    // for the values, a system would be nice

    // objects
    public static final char OBJECT_NULL = 0;
    public static final char OBJECT_IMPL = 1;
    public static final char OBJECT_ARRAY = 2;
    public static final char OBJECT_ARRAY_2D = 3;
    public static final char OBJECTS_HOMOGENOUS_ARRAY = 4;
    public static final char OBJECT_LIST_UNKNOWN_LENGTH = 5;
    public static final char OBJECT_PTR = 6;

    // number types
    public static final char BOOL = 7;
    public static final char BOOL_ARRAY = 8;
    public static final char BOOL_ARRAY_2D = 9;
    public static final char BYTE = 10;
    public static final char BYTE_ARRAY = 11;
    public static final char BYTE_ARRAY_2D = 12;
    public static final char CHAR = 13;
    public static final char CHAR_ARRAY = 14;
    public static final char CHAR_ARRAY_2D = 15;
    public static final char SHORT = 16;
    public static final char SHORT_ARRAY = 17;
    public static final char SHORT_ARRAY_2D = 18;
    public static final char INT = 19;
    public static final char INT_ARRAY = 20;
    public static final char INT_ARRAY_2D = 21;
    public static final char LONG = 22;
    public static final char LONG_ARRAY = 23;
    public static final char LONG_ARRAY_2D = 24;
    public static final char FLOAT = 25;
    public static final char FLOAT_ARRAY = 26;
    public static final char FLOAT_ARRAY_2D = 27;
    public static final char DOUBLE = 28;
    public static final char DOUBLE_ARRAY = 29;
    public static final char DOUBLE_ARRAY_2D = 30;

    // strings
    public static final char STRING = 31;
    public static final char STRING_ARRAY = 32;
    public static final char STRING_ARRAY_2D = 33;

    // single precision vectors
    public static final char VECTOR2F = 34;
    public static final char VECTOR2F_ARRAY = 35;
    public static final char VECTOR2F_ARRAY_2D = 36;
    public static final char VECTOR3F = 37;
    public static final char VECTOR3F_ARRAY = 38;
    public static final char VECTOR3F_ARRAY_2D = 39;
    public static final char VECTOR4F = 40;
    public static final char VECTOR4F_ARRAY = 41;
    public static final char VECTOR4F_ARRAY_2D = 42;

    // double precision vectors
    public static final char VECTOR2D = 43;
    public static final char VECTOR2D_ARRAY = 44;
    public static final char VECTOR2D_ARRAY_2D = 45;
    public static final char VECTOR3D = 46;
    public static final char VECTOR3D_ARRAY = 47;
    public static final char VECTOR3D_ARRAY_2D = 48;
    public static final char VECTOR4D = 49;
    public static final char VECTOR4D_ARRAY = 50;
    public static final char VECTOR4D_ARRAY_2D = 51;

    // quaternions
    public static final char QUATERNION32 = 52;
    public static final char QUATERNION32_ARRAY = 53;
    public static final char QUATERNION32_ARRAY_2D = 54;
    public static final char QUATERNION64 = 55;
    public static final char QUATERNION64_ARRAY = 56;
    public static final char QUATERNION64_ARRAY_2D = 57;

    // integer vectors
    public static final char VECTOR2I = 58;
    public static final char VECTOR2I_ARRAY = 59;
    public static final char VECTOR2I_ARRAY_2D = 60;
    public static final char VECTOR3I = 61;
    public static final char VECTOR3I_ARRAY = 62;
    public static final char VECTOR3I_ARRAY_2D = 63;
    public static final char VECTOR4I = 64;
    public static final char VECTOR4I_ARRAY = 65;
    public static final char VECTOR4I_ARRAY_2D = 66;

    // single precision matrices
    public static final char MATRIX2X2F = 67;
    public static final char MATRIX2X2F_ARRAY = 68;
    public static final char MATRIX2X2F_ARRAY_2D = 69;
    public static final char MATRIX3X2F = 70;
    public static final char MATRIX3X2F_ARRAY = 71;
    public static final char MATRIX3X2F_ARRAY_2D = 72;
    public static final char MATRIX4X2F = 73;
    public static final char MATRIX4X2F_ARRAY = 74;
    public static final char MATRIX4X2F_ARRAY_2D = 75;
    public static final char MATRIX3X3F = 76;
    public static final char MATRIX3X3F_ARRAY = 77;
    public static final char MATRIX3X3F_ARRAY_2D = 78;
    public static final char MATRIX4X3F = 79;
    public static final char MATRIX4X3F_ARRAY = 80;
    public static final char MATRIX4X3F_ARRAY_2D = 81;
    public static final char MATRIX4X4F = 82;
    public static final char MATRIX4X4F_ARRAY = 83;
    public static final char MATRIX4X4F_ARRAY_2D = 84;

    // double precision matrices
    public static final char MATRIX2X2D = 85;
    public static final char MATRIX2X2D_ARRAY = 86;
    public static final char MATRIX2X2D_ARRAY_2D = 87;
    public static final char MATRIX3X2D = 88;
    public static final char MATRIX3X2D_ARRAY = 89;
    public static final char MATRIX3X2D_ARRAY_2D = 90;
    public static final char MATRIX4X2D = 91;
    public static final char MATRIX4X2D_ARRAY = 92;
    public static final char MATRIX4X2D_ARRAY_2D = 93;
    public static final char MATRIX3X3D = 94;
    public static final char MATRIX3X3D_ARRAY = 95;
    public static final char MATRIX3X3D_ARRAY_2D = 96;
    public static final char MATRIX4X3D = 97;
    public static final char MATRIX4X3D_ARRAY = 98;
    public static final char MATRIX4X3D_ARRAY_2D = 99;
    public static final char MATRIX4X4D = 100;
    public static final char MATRIX4X4D_ARRAY = 101;
    public static final char MATRIX4X4D_ARRAY_2D = 102;

    // other geometric types
    public static final char AABB32 = 103;
    public static final char AABB64 = 106;
    public static final char PLANE32 = 109;
    public static final char PLANE64 = 112;

    // files
    public static final char FILE = 115;
    public static final char FILE_ARRAY = 116;
    public static final char FILE_ARRAY_2D = 117;
}
