package me.anno.image.exr;

import com.sun.istack.internal.Nullable;
import me.anno.image.raw.FloatImage;
import me.anno.image.raw.HalfImage;
import me.anno.utils.OS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class OpenEXRJava {
    private static final Logger LOGGER = LogManager.getLogger(OpenEXRJava.class);

    public static @Nullable FloatImage readFloatImage(@NotNull FloatImage image, @NotNull byte[] data) {
        return canUseLibrary && readFloatImageImpl(image.getWidth(), image.getHeight(), image.getNumChannels(), image.getData(), data) ? image : null;
    }

    public static @Nullable HalfImage readHalfImage(@NotNull HalfImage image, @NotNull byte[] data) {
        return canUseLibrary && readHalfImageImpl(image.getWidth(), image.getHeight(), image.getNumChannels(), image.getData(), data) ? image : null;
    }

    private static native boolean readFloatImageImpl(int width, int height, int channels, float[] dst, @NotNull byte[] data);

    private static native boolean readHalfImageImpl(int width, int height, int channels, short[] dst, @NotNull byte[] data);

    private static boolean canUseLibrary;

    static {
        if (OS.isLinux) {
            try {
                NativeLoader.INSTANCE.load("/libopenexr_java.so", "OpenEXR");
                canUseLibrary = true;
            } catch (Exception e) {
                LOGGER.warn("Failed to load OpenEXR", e);
                canUseLibrary = false;
            }
        } else canUseLibrary = false;
    }
}
