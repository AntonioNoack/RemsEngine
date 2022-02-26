package org.apache.commons.lang3;

import java.util.Arrays;

// used by JOptimizer
public class ArrayUtils {
    public static String toString(Object obj) {
        if (obj instanceof double[]) return Arrays.toString((double[]) obj);
        if (obj instanceof Object[]) return Arrays.deepToString((Object[]) obj);
        return obj == null ? "null" : obj.toString();
    }
}
