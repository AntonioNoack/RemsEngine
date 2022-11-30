package me.anno.input

@Suppress("unused")
enum class MouseButton(val id: Int) {
    LEFT(0),
    RIGHT(1),
    MIDDLE(2),
    M3(3),
    M4(4),
    M5(5),
    M6(6),
    M7(7),
    M8(8),
    UNKNOWN(-1);

    val isLeft = id == 0
    val isRight = id == 1
    val isMiddle = id == 2

    // 4 & 3 or 5 & 4???...
    val isForward = id == 5
    val isBackward = id == 4

    companion object {

        @JvmStatic
        fun Int.toMouseButton() = when (this) {
            0 -> LEFT
            1 -> RIGHT
            2 -> MIDDLE
            3 -> M3
            4 -> M4
            5 -> M5
            6 -> M6
            7 -> M7
            8 -> M8
            else -> UNKNOWN
        }

    }
}