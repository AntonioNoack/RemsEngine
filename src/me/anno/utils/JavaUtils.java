package me.anno.utils;

public class JavaUtils {
    /**
     * get the class of an object of generic type; not allowed in Kotlin
     * */
    public static Class<?> getClass(Object obj){
        return obj.getClass();
    }
}
