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
import me.anno.utils.Color.withAlpha
import org.lwjgl.glfw.GLFW

open class Checkbox(startValue: Boolean, val defaultValue: Boolean, var size: Int, style: Style) :
    Panel(style.getChild("checkbox")), InputPanel<Boolean> {

    companion object {
        fun getImage(checked: Boolean): Texture2D? =
            getInternalTexture(if (checked) "checked.png" else "unchecked.png", true)
    }

    var isChecked = startValue

    override var isInputAllowed = true
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    override val lastValue: Boolean get() = isChecked

    private var resetListener: () -> Boolean? = { defaultValue }
    private var changeListener: ((Boolean) -> Unit)? = null
    private var wasHovered = false

    override fun isOpaqueAt(x: Int, y: Int): Boolean {
        return true
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minW = size + 2
        minH = size + 2
    }

    private var lastImage = -1
    override fun onUpdate() {
        super.onUpdate()
        val leImage = getImage(isChecked)
        val leImageState = leImage?.state ?: 0
        if (wasHovered != isHovered || leImageState != lastImage) {
            wasHovered = isHovered
            lastImage = leImageState
            invalidateDrawing()
        }
    }

    override fun setValue(value: Boolean, notify: Boolean): Checkbox {
        if (isChecked != value) toggle(notify)
        return this
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val color = (if (isHovered) 0xccffffff.toInt() else -1)
            .withAlpha(if (isInputAllowed) 1f else 0.5f)
        val texture = getImage(isChecked) ?: whiteTexture
        drawTexture(
            x + (w - size) / 2,
            y + (h - size) / 2,
            size, size, texture, color, null
        )
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
        if (isInputAllowed) toggle(true)
        else super.onMouseClicked(x, y, button, long)
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        if (isInputAllowed) toggle(true)
        else super.onDoubleClick(x, y, button)
    }

    override fun onEnterKey(x: Float, y: Float) {
        if (isInputAllowed) toggle(true)
        else super.onEnterKey(x, y)
    }

    override fun onKeyTyped(x: Float, y: Float, key: Int) {
        when (key) {
            GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_UP -> {
                if (isInputAllowed) toggle(true)
                else super.onKeyTyped(x, y, key)
            }
            else -> super.onKeyTyped(x, y, key)
        }
    }

    override fun onEmpty(x: Float, y: Float) {
        if (isInputAllowed) {
            val resetValue = resetListener() ?: defaultValue
            if (resetValue != isChecked) toggle(true)
        } else {
            super.onEmpty(x, y)
        }
    }

    fun setResetListener(listener: () -> Boolean?): Checkbox {
        resetListener = listener
        return this
    }

    override fun acceptsChar(char: Int) = false // ^^
    override fun isKeyInput() = true

    override fun clone(): Checkbox {
        val clone = Checkbox(defaultValue, defaultValue, size, style)
        copy(clone)
        return clone
    }

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