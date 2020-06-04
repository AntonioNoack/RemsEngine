package me.anno.ui.input

import me.anno.gpu.GFX
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.color.ColorChooser
import me.anno.ui.style.Style
import org.joml.Vector4f

// todo implement a filmic shader?

class ColorInput(style: Style, title: String,
                 oldValue: Vector4f,
                 private val owningProperty: AnimatedProperty<*>? = null): PanelListY(style){

    private val titleView = object: TextPanel(title, style){
        override fun onCopyRequested(x: Float, y: Float) = parent?.onCopyRequested(x,y) }
    private val contentView = ColorChooser(style, true)

    init {
        this += titleView
        this += contentView
        contentView.setRGBA(oldValue.x, oldValue.y, oldValue.z, oldValue.w)
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused1 = titleView.isInFocus || contentView.listOfAll.count { it.isInFocus } > 0
        if(focused1) isSelectedListener?.invoke()
        val focused2 = focused1 || owningProperty == GFX.selectedProperty
        contentView.visibility = if(focused2) Visibility.VISIBLE else Visibility.GONE
        super.draw(x0, y0, x1, y1)
    }

    fun setChangeListener(listener: (x: Float, y: Float, z: Float, w: Float) -> Unit): ColorInput {
        contentView.setChangeRGBListener(listener)
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): ColorInput {
        isSelectedListener = listener
        return this
    }

}