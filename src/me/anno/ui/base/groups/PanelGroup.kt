package me.anno.ui.base.groups

import me.anno.gpu.GFX
import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.style.Style
import java.lang.Exception
import kotlin.math.max
import kotlin.math.min

abstract class PanelGroup(style: Style): Panel(style){

    abstract val children: List<Panel>
    abstract fun remove(child: Panel)

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        var hadVisibleChild = false
        children@ for(child in children){
            if(child.visibility == Visibility.VISIBLE){
                try {
                    hadVisibleChild = drawChild(child, x0, y0, x1, y1) or hadVisibleChild
                } catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
        if(hadVisibleChild){
            GFX.clip2(x0, y0, x1, y1)
        }
    }

    fun drawChild(child: Panel, x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        val x02 = max(child.x, x0)
        val y02 = max(child.y, y0)
        val x12 = min(child.x + child.w, x1)
        val y12 = min(child.y + child.h, y1)
        return if(x12 > x02 && y12 > y02){
            GFX.clip2(x02, y02, x12, y12)
            child.draw(x02, y02, x12, y12)
            /*val color = black or 0x777777
            GFX.drawRect(x02, y02, 1, 1, color)
            GFX.drawRect(x02, y12-1, 1, 1, color)
            GFX.drawRect(x12-1, y02, 1, 1, color)
            GFX.drawRect(x12-1, y12-1, 1, 1, color)*/
            true
        } else false
    }

    override fun getClassName(): String = "PanelGroup"

    override fun printLayout(tabDepth: Int) {
        super.printLayout(tabDepth)
        for(child in children){
            child.printLayout(tabDepth+1)
        }
    }

}