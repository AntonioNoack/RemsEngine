package me.anno.gpu.texture

import org.lwjgl.opengl.GL46C.GL_LINEAR
import org.lwjgl.opengl.GL46C.GL_LINEAR_MIPMAP_LINEAR
import org.lwjgl.opengl.GL46C.GL_NEAREST

/** Sensible filtering settings for OpenGL. Besides min/max for depth, nothing else makes much sense. */
enum class Filtering(val mag: Int, val min: Int, val needsMipmap: Boolean) {
    /**
     * Pixels up close, smooth when far away; like Minecraft on high settings
     * */
    NEAREST(GL_NEAREST, GL_LINEAR_MIPMAP_LINEAR, true),

    /**
     * Pixels in all cases,
     * moire-patterns when under sampling (far away), like Minecraft on low settings;
     *
     * (Probably) Disables ability to sample from mip levels using textureLod().
     * */
    TRULY_NEAREST(GL_NEAREST, GL_NEAREST, false),

    /**
     * Smooth-like look, but quad pattern still visible (cubic fixes this, isn't part of OpenGL itself though);
     * smooth when far away, too.
     * */
    LINEAR(GL_LINEAR, GL_LINEAR_MIPMAP_LINEAR, true),

    /**
     * Smooth-like look, but quad pattern still visible (cubic fixes this, isn't part of OpenGL itself though);
     * moire-patterns when under sampling (far away), like Minecraft on low settings;
     *
     * Disables ability to sample from mip levels using textureLod().
     * */
    TRULY_LINEAR(GL_LINEAR, GL_LINEAR, false)
}