package me.anno.ui.anim

enum class EventType(val id: Int) {
    HOVER(0), TOUCH(1);
    companion object {
        val values2 = values()
    }
}