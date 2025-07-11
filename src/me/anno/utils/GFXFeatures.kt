package me.anno.utils

/**
 * General information about graphics capabilities.
 * */
object GFXFeatures {

    /**
     * Whether the engine is able to toggle VSync
     * */
    var canToggleVSync = true

    /**
     * Whether we can open extra windows
     * */
    var canOpenNewWindows = true

    /**
     * Whether we run on the limited OpenGL ES subset.
     * This is the case for Android and Web.
     * */
    var isOpenGLES = false

    /**
     * Whether the GPU weak.
     * Weak is anything below a GTX 1080 TI.
     * todo create a mini benchmark to determine this???
     * */
    var hasWeakGPU = false

    /**
     * For OpenGL ES only supported with version 3.2.
     * For WebGL this isn't supported.
     * */
    var supportsTextureGather = true

    /**
     * Not supported in WebGL.
     * */
    var supportsBorderColors = true

    /**
     * Only supported in OpenGL ES 3.1 and newer.
     * WebGL not tested, but probably not supported.
     * */
    var supportsShaderStorageBuffers = true
}