package me.anno.gpu.texture

import org.lwjgl.opengl.GL11C.*

/** Sensible filtering settings for OpenGL. Besides min/max for depth, nothing else makes much sense. */
enum class GPUFiltering(val mag: Int, val min: Int, val needsMipmap: Boolean) {
    /**
     * Pixels up close, smooth when far away; like Minecraft on high settings
     * */
    NEAREST(GL_NEAREST, GL_LINEAR_MIPMAP_LINEAR, true),
    /**
     * Pixels in all cases,
     * moire-patterns when under sampling (far away), like Minecraft on low settings
     * */
    TRULY_NEAREST(GL_NEAREST, GL_NEAREST, false),
    /**
     * Smooth-like look, but quad pattern still visible (cubic fixes this, isn't part of OpenGL itself though);
     * smooth when far away, too.
     * */
    LINEAR(GL_LINEAR, GL_LINEAR_MIPMAP_LINEAR, true),
    /**
     * Smooth-like look, but quad pattern still visible (cubic fixes this, isn't part of OpenGL itself though);
     * moire-patterns when under sampling (far away), like Minecraft on low settings
     * */
    TRULY_LINEAR(GL_LINEAR, GL_LINEAR, false)
}