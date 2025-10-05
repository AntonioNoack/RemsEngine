package me.anno.fonts.signeddistfields

class Contours(val contours: List<Contour>, val needsInsideCheck: Boolean) {
    companion object {
        val emptyContours = Contours(emptyList(), false)
    }
}