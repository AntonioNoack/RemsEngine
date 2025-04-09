package me.anno.gpu

// todo in the future pack all GPU APIs into an abstract class or interface
enum class GFXCapabilities {
    /**
     * Medium-level API used on Windows and Linux computers.
     * Default for Rem's Engine.
     *
     * Implementations: This project (https://github.com/AntonioNoack/RemsEngine).
     * */
    OPENGL,

    /**
     * Medium-level API used in Android, and WebGL is very similar to it.
     * Often much more limited and stricter than OpenGL.
     *
     * Multisampled textures don't exist, only multisampled renderbuffers.
     * Doesn't support compute shaders.
     *
     * Implementations: https://github.com/AntonioNoack/RemsEngine-Android, https://github.com/AntonioNoack/JVM2WASM.
     * */
    OPENGL_ES,

    /**
     * Medium-level API used on Windows. Very similar to OpenGL.
     *
     * Implementations: https://github.com/AntonioNoack/JDirectX11
     * */
    DIRECTX_11,

    /**
     * New, low-level API for Windows only.
     * Very comparable to Vulcan, so I might join them once implementations exist.
     *
     * Implementations: none
     * */
    DIRECTX_12,

    /**
     * New, low-level API for all platforms.
     * Very comparable to DirectX12, so I might join them once implementations exist.
     *
     * Implementations: none
     * */
    VULCAN,

    /**
     * Between Vulcan and WebGL for the web... supports compute shaders.
     *
     * Implementations: none
     * */
    WEBGPU,
}