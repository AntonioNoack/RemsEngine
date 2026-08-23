package me.anno.ui.canvas

enum class CanvasDrawMode {
    RECTANGLE,

    // todo nearest/linear filtering flags
    TEXTURE,
    TEXTURE_NO_ALPHA,
    CIRCLE,
    SQUIRCLE,
    ROUNDED_RECT,
    TEXT,
    LINE
}