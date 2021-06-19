package me.anno.io.binary;

public class BinaryTypes {
    public static final char OBJECT_IMPL = 'o';
    public static final char OBJECT_ARRAY = 'O';
    public static final char OBJECTS_HOMOGENOUS_ARRAY = '#';
    public static final char OBJECT_LIST_UNKNOWN_LENGTH = '&';
    public static final char OBJECT_PTR = '*';
    public static final char OBJECT_NULL = (char) 0;
    public static final char BOOL = 'z';
    public static final char BOOL_ARRAY = 'Z';
    public static final char BYTE = 'b';
    public static final char BYTE_ARRAY = 'B';
    public static final char CHAR = 'c';
    public static final char CHAR_ARRAY = 'C';
    public static final char SHORT = 's';
    public static final char SHORT_ARRAY = 'S';
    public static final char INT = 'i';
    public static final char INT_ARRAY = 'I';
    public static final char LONG = 'l';
    public static final char LONG_ARRAY = 'L';
    public static final char FLOAT = 'f';
    public static final char FLOAT_ARRAY = 'F';
    public static final char FLOAT_ARRAY_2D = 'F' + 128;
    public static final char DOUBLE = 'd';
    public static final char DOUBLE_ARRAY = 'D';
    public static final char DOUBLE_ARRAY_2D = 'D' + 128;
    public static final char STRING = 'q';
    public static final char STRING_ARRAY = 'Q';
    public static final char VECTOR2F = '2';
    public static final char VECTOR3F = '3';
    public static final char VECTOR4F = '4';
    public static final char VECTOR2D = '5';
    public static final char VECTOR3D = '6';
    public static final char VECTOR4D = '7';
    public static final char VECTOR2F_ARRAY = '2' + 128;
    public static final char VECTOR3F_ARRAY = '3' + 128;
    public static final char VECTOR4F_ARRAY = '4' + 128;
    public static final char VECTOR2D_ARRAY = '5' + 128;
    public static final char VECTOR3D_ARRAY = '6' + 128;
    public static final char VECTOR4D_ARRAY = '7' + 128;
    // todo matrix types...
}
