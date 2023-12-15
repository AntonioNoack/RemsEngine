package me.anno.ui.input.components

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.input.Key
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.input.InputPanel
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha

open class Checkbox(startValue: Boolean, val defaultValue: Boolean, var size: Int, style: Style) :
    Panel(style.getChild("checkbox")), InputPanel<Boolean> {

    companion object {
        private val checked = getReference("res://textures/Checked.png")
        private val unchecked = getReference("res://textures/Unchecked.png")
    }

    open fun getImage(isChecked: Boolean): Texture2D? =
        TextureCache[if (isChecked) checked else unchecked, true]

    open fun getColor(): Int {
        return white.withAlpha((if (isInputAllowed) if (isHovered) 200 else 255 else 127))
    }

    override var isInputAllowed = true
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    override var value: Boolean = startValue

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
        val leImage = getImage(value)
        val leImageState = leImage?.state ?: 0
        if (wasHovered != isHovered || leImageState != lastImage) {
            lastImage = leImageState
            invalidateDrawing()
        }
        super.onUpdate()
    }

    override fun setValue(newValue: Boolean, mask: Int, notify: Boolean): Panel {
        if (value != newValue) toggle(notify)
        return this
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val texture = getImage(value) ?: whiteTexture
        texture.bind(0, Filtering.LINEAR, Clamping.CLAMP)
        drawTexture(
            x + (width - size) / 2,
            y + (height - size) / 2,
            size, size, texture, getColor(), null
        )
    }

    fun setChangeListener(listener: (Boolean) -> Unit): Checkbox {
        changeListener = listener
        return this
    }

    fun toggle(notify: Boolean) {
        value = !value
        if (notify) {
            changeListener?.invoke(value)
        }
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
            if (resetValue != value) toggle(true)
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
        dst.value = value
        // !! can be incorrect, if there is references within the listener
        dst.resetListener = resetListener
        // !! can be incorrect, if there is references within the listener
        dst.changeListener = changeListener
        dst.wasHovered = wasHovered
    }
}