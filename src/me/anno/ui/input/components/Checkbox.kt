package me.anno.ui.input.components

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.ImageGPUCache.getInternalTexture
import me.anno.input.MouseButton
import me.anno.ui.Panel
import me.anno.ui.input.InputPanel
import me.anno.ui.style.Style
import org.lwjgl.glfw.GLFW
import kotlin.math.min

open class Checkbox(startValue: Boolean, val defaultValue: Boolean, val size: Int, style: Style) :
    Panel(style.getChild("checkbox")), InputPanel<Boolean> {

    constructor(base: Checkbox) : this(base.isChecked, base.defaultValue, base.size, base.style) {
        base.copy(this)
    }

    // todo hover/toggle/focus color change

    companion object {
        fun getImage(checked: Boolean): Texture2D? =
            getInternalTexture(if (checked) "checked.png" else "unchecked.png", true)
    }

    var isChecked = startValue

    override val lastValue: Boolean get() = isChecked

    private var resetListener: () -> Boolean? = { defaultValue }
    private var changeListener: ((Boolean) -> Unit)? = null
    private var wasHovered = false

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minW = size + 2
        minH = size + 2
    }

    override fun tickUpdate() {
        super.tickUpdate()
        if (wasHovered != isHovered) {
            wasHovered = isHovered
            invalidateDrawing()
        }
    }

    override fun getVisualState(): Any? = getImage(isChecked)?.state

    override fun setValue(value: Boolean, notify: Boolean): Checkbox {
        if (isChecked != value) toggle(notify)
        return this
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)

        val size = min(w, h)
        if (size > 0) {
            val color = if (isHovered) 0xccffffff.toInt() else -1
            // draw the icon on/off
            drawTexture(
                x0 + (w - size) / 2,
                y0 + (h - size) / 2,
                size, size,
                getImage(isChecked) ?: whiteTexture,
                color, null
            )
        }

    }

    fun setChangeListener(listener: (Boolean) -> Unit): Checkbox {
        changeListener = listener
        return this
    }

    fun toggle(notify: Boolean) {
        if (notify) {
            isChecked = !isChecked
            changeListener?.invoke(isChecked)
        } else isChecked = !isChecked
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        toggle(true)
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        toggle(true)
    }

    override fun onEnterKey(x: Float, y: Float) {
        toggle(true)
    }

    override fun onKeyTyped(x: Float, y: Float, key: Int) {
        when (key) {
            GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_UP -> toggle(true)
        }
    }

    override fun onEmpty(x: Float, y: Float) {
        val resetValue = resetListener() ?: defaultValue
        if (resetValue != isChecked) toggle(true)
    }

    fun setResetListener(listener: () -> Boolean?): Checkbox {
        resetListener = listener
        return this
    }

    override fun acceptsChar(char: Int) = false // ^^
    override fun isKeyInput() = true

    override fun clone() = Checkbox(this)

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Checkbox
        clone.isChecked = isChecked
        // !! can be incorrect, if there is references within the listener
        clone.resetListener = resetListener
        // !! can be incorrect, if there is references within the listener
        clone.changeListener = changeListener
        clone.wasHovered = wasHovered
    }

}