package me.anno.objects

import me.anno.gpu.GFX
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.FloatInput
import me.anno.ui.style.Style
import org.joml.*

class Camera(parent: Transform?): Transform(parent){

    // todo allow cameras to be merged
    // todo allow cameras to film camera (post processing) -> todo create a stack of cameras/scenes?

    var nearZ = AnimatedProperty.float().set(0.001f)
    var farZ = AnimatedProperty.float().set(1000f)
    var fovDegrees = AnimatedProperty.float().set(90f)

    var onlyShowTarget = true
    var useDepth = true

    init {
        position.add(0f, Vector3f(0f, 0f, 1f))
    }

    override fun getClassName() = "Camera"

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += FloatInput(style, "Near Z", nearZ, lastLocalTime)
            .setChangeListener { putValue(nearZ, it) }
            .setIsSelectedListener { show(nearZ) }
        list += FloatInput(style, "Far Z", farZ, lastLocalTime)
            .setChangeListener { putValue(farZ, it) }
            .setIsSelectedListener { show(farZ) }
        list += FloatInput(style, "FOV", fovDegrees, lastLocalTime)
            .setChangeListener { putValue(fovDegrees, it) }
            .setIsSelectedListener { show(fovDegrees) }
        list += BooleanInput("Only Show Target", onlyShowTarget, style)
            .setChangeListener { onlyShowTarget = it }
            .setIsSelectedListener { show(null) }
        list += BooleanInput("Use Depth", useDepth, style)
            .setChangeListener { useDepth = it }
            .setIsSelectedListener { show(null) }
    }

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f) {
        super.onDraw(stack, time, color)
        // todo draw a 3D line camera
    }

}