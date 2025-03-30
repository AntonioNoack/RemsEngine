package me.anno.gpu.framebuffer

enum class DepthBufferType(val write: Boolean, val read: Boolean?) {
    NONE(false, false),
    INTERNAL(true, false), // using a renderbuffer; 24 or 32 bits typically
    TEXTURE(true, true), // 24 or 32 bits
    TEXTURE_16(true, true), // 16 bits
    ATTACHMENT(true, null) // attached to another framebuffer
    ;

    val isTexture get() = this === TEXTURE || this === TEXTURE_16

    fun chooseDepthFormat(): TargetType {
        return if (this == TEXTURE_16) TargetType.DEPTH16
        else TargetType.DEPTH32F
    }
}