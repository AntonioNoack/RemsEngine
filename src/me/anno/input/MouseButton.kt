package me.anno.input

enum class MouseButton(val isLeft: Boolean, val isRight: Boolean, val isMiddle: Boolean){
    LEFT(true, false, false),
    RIGHT(false, true, false),
    MIDDLE(false, false, true),
    UNKNOWN(false, false, false)
}