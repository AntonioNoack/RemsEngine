package me.anno.gpu.framebuffer

enum class DepthBufferType(val write: Boolean, val read: Boolean?) {
    NONE(false, false),
    INTERNAL(true, false),
    TEXTURE(true, true),
    TEXTURE_16(true, true),
    ATTACHMENT(true, null)
}