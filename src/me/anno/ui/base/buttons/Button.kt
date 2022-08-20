package me.anno.ui.base.buttons

import me.anno.Engine
import me.anno.config.DefaultStyle.black
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.input.Input
import me.anno.input.Input.mouseDownX
import me.anno.input.Input.mouseDownY
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.mixARGB
import me.anno.ui.Panel
import me.anno.ui.style.Style
import kotlin.math.abs
import kotlin.math.min

open class Button(
    var isSquare: Boolean = true,
    style: Style
) : Panel(style) {

    @NotSerializedProperty
    private var tint = 0f

    @NotSerializedProperty
    var tintColor = -1

    @NotSerializedProperty
    private var lastTime = 0L

    override fun onUpdate() {
        super.onUpdate()
        val time = Engine.gameTime
        val targetTint = when {
            Input.isLeftDown && contains(mouseDownX, mouseDownY) -> 0f
            isHovered -> 0.5f
            else -> 1f
        }
        if (abs(targetTint - tint) > 0.001f) {
            val dt = (time - lastTime) * 1e-9f * 10f
            lastTime = time
            tint = mix(tint, targetTint, min(dt, 0.2f))
            tintColor = mixARGB(0x777777 or black, -1, tint)
            invalidateDrawing()
        }
        lastTime = time
    }

    override fun clone(): Button {
        val clone = Button(isSquare, style)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Button
        clone.isSquare = isSquare
    }

    override val className = "Button"

}