package me.anno.ui.base.buttons

import me.anno.Time
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.input.Input
import me.anno.maths.Maths.dtTo10
import me.anno.maths.Maths.mix
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.Color.black
import me.anno.utils.Color.mixARGB

open class Button(
    var isSquare: Boolean = true,
    style: Style
) : Panel(style) {

    @NotSerializedProperty
    private var tint = 0f

    @NotSerializedProperty
    var tintColor = -1

    override fun onUpdate() {
        super.onUpdate()
        val targetTint = when {
            Input.isLeftDown && isHovered -> 0f
            isHovered -> 0.5f
            else -> 1f
        }
        val dt = Time.uiDeltaTime.toFloat()
        tint = mix(tint, targetTint, dtTo10(dt * 10f))
        tintColor = mixARGB(0x777777 or black, -1, tint)
    }

    override fun clone(): Button {
        val clone = Button(isSquare, style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Button) return
        dst.isSquare = isSquare
    }
}