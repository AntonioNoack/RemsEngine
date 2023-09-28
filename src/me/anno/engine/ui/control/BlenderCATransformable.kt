package me.anno.engine.ui.control

interface BlenderCATransformable {
    fun transform(self: BlenderControlsAddon, x: Float, y: Float, reset: Boolean)
}