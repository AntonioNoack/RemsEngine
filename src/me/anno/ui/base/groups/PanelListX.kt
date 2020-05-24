package me.anno.ui.base.groups

import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.style.Style
import me.anno.utils.warn
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class PanelListX(sorter: Comparator<Panel>?, style: Style): PanelList(sorter, style){

    constructor(style: Style): this(null, style)

    var sumConst = 0
    var sumWeight = 0f

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        var w = 0
        var h = 0
        var weight = 0f
        for(child in children.filter { it.visibility != Visibility.GONE }){
            child.calculateSize(w, h) // calculate w,h,minw,minh
            child.applyConstraints()
            w += child.minW
            h = max(h, child.minH)
            if(child.weight >= 0f){
                // has weight
                weight += child.weight
            }
        }
        val spaceCount = children.size - 1
        w += spacing * spaceCount
        sumConst = w
        sumWeight = weight
        minW = sumConst
        minH = h

    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)

        var perWeight = 0f
        var perConst = 1f

        if(w > sumConst){
            val extraAvailable = max(0, w - sumConst)
            perWeight = extraAvailable / max(sumWeight, 1e-9f)
        } else {
            perConst = w.toFloat() / sumConst
        }

        // warn("panel list x $x $y $w $h ($minW $minH): $sumConst + $sumWeight -> per const: $perConst, per weight: $perWeight")

        // println("extra available: $extraAvailable = $w - $sumConst, per weight: $perWeight = $extraAvailable / $sumWeight")
        var x = this.x
        for(child in children.filter { it.visibility != Visibility.GONE }){
            val childW = (child.minW * perConst + perWeight * max(0f, child.weight)).roundToInt()
            child.calculateSize(childW, h)
            child.applyConstraints()
            child.placeInParent(x, y)
            // println("laying out child to $x $y += $childWidth $h")
            x += childW + spacing
        }

    }

}