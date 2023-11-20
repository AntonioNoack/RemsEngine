package me.anno.ui.input.components

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.ImageGPUCache
import me.anno.input.Key
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.ui.Panel
import me.anno.ui.input.InputPanel
import me.anno.ui.Style
import me.anno.utils.Color.withAlpha

open class Checkbox(startValue: Boolean, val defaultValue: Boolean, var size: Int, style: Style) :
    Panel(style.getChild("checkbox")), InputPanel<Boolean> {

    companion object {
        val checked = getReference("res://textures/Checked.png")
        val unchecked = getReference("res://textures/Unchecked.png")
        fun getImage(isChecked: Boolean): Texture2D? =
            ImageGPUCache[if (isChecked) checked else unchecked, true]
    }

    var isChecked = startValue

    override var isInputAllowed = true
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    override val value: Boolean get() = isChecked

    private var resetListener: () -> Boolean? = { defaultValue }
    private var changeListener: ((Boolean) -> Unit)? = null

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
        val leImage = getImage(isChecked)
        val leImageState = leImage?.state ?: 0
        if (wasHovered != isHovered || leImageState != lastImage) {
            lastImage = leImageState
            invalidateDrawing()
        }
        super.onUpdate()
    }

    override fun setValue(newValue: Boolean, mask: Int, notify: Boolean): Panel {
        if (isChecked != newValue) toggle(notify)
        return this
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val color = (if (isHovered) 0xccffffff.toInt() else -1)
            .withAlpha(if (isInputAllowed) 255 else 127)
        val texture = getImage(isChecked) ?: whiteTexture
        texture.bind(0, GPUFiltering.LINEAR, Clamping.CLAMP)
        drawTexture(
            x + (width - size) / 2,
            y + (height - size) / 2,
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

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        if (isInputAllowed) toggle(true)
        else super.onMouseClicked(x, y, button, long)
    }

    override fun onDoubleClick(x: Float, y: Float, button: Key) {
        if (isInputAllowed) toggle(true)
        else super.onDoubleClick(x, y, button)
    }

    override fun onEnterKey(x: Float, y: Float) {
        if (isInputAllowed) toggle(true)
        else super.onEnterKey(x, y)
    }

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        when (key) {
            Key.KEY_ARROW_DOWN, Key.KEY_ARROW_UP -> {
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
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Checkbox
        dst.isChecked = isChecked
        // !! can be incorrect, if there is references within the listener
        dst.resetListener = resetListener
        // !! can be incorrect, if there is references within the listener
        dst.changeListener = changeListener
        dst.wasHovered = wasHovered
    }
}