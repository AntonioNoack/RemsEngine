package me.anno.ui.input.components

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.objects.Text
import me.anno.objects.cache.Cache
import me.anno.objects.cache.TextureCache
import me.anno.ui.base.Panel
import me.anno.ui.style.Style
import org.joml.Vector4f
import kotlin.math.min

class Checkbox(startValue: Boolean, val size: Int, style: Style): Panel(style.getChild("checkbox")){

    companion object {
        fun getImage(checked: Boolean): Texture2D = Cache.getIcon(if(checked) "checked.png" else "unchecked.png")
    }

    var isChecked = startValue

    init {
        minW = size + 2
        minH = size + 2
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)

        val size = min(w, h)
        if(size > 0){
            // draw the icon on/off
            GFX.drawTexture(x0+(w-size)/2, y0+(h-size)/2, size, size, getImage(isChecked), -1)
        }

    }

    private var onCheckedChanged: ((Boolean) -> Unit)? = null

    fun setChangeListener(listener: (Boolean) -> Unit): Checkbox {
        onCheckedChanged = listener
        return this
    }

    fun change(){
        isChecked = !isChecked
        onCheckedChanged?.invoke(isChecked)
    }

    override fun onMouseClicked(x: Float, y: Float, button: Int, long: Boolean) {
        change()
    }

    override fun onDoubleClick(x: Float, y: Float, button: Int) {
        change()
    }

    override fun onEnterKey(x: Float, y: Float) {
        change()
    }

    override fun getClassName() = "CheckBox"

}