package me.anno.ui.base.groups

import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.style.Style
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class PanelListX(sorter: Comparator<Panel>?, style: Style): PanelList(sorter, style){

    constructor(style: Style): this(null, style)

    var sumConst = 0
    var sumWeight = 0f

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        var sumW = 0
        var maxH = 0
        var weight = 0f
        val dc = disableConstantSpaceForWeightedChildren
        for(child in children.filter { it.visibility != Visibility.GONE }){
            child.calculateSize(w, h) // calculate w,h,minw,minh
            // child.applyConstraints()
            maxH = max(maxH, child.minH)
            val hasWeight = child.weight >= 0f
            if(hasWeight){
                weight += child.weight
            }
            if(!(hasWeight && dc)){
                sumW += child.minW
            }
        }
        val spaceCount = children.size - 1
        sumW += spacing * spaceCount
        sumConst = sumW
        sumWeight = weight
        minW = sumConst
        minH = min(h, maxH)

    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)

        var perWeight = 0f
        val perConst = 1f

        if(w > sumConst){
            val extraAvailable = max(0, w - sumConst)
            perWeight = extraAvailable / max(sumWeight, 1e-9f)
        }/* else {
            perConst = w.toFloat() / sumConst
        }*/

        // warn("panel list x $x $y $w $h ($minW $minH): $sumConst + $sumWeight -> per const: $perConst, per weight: $perWeight")

        // ("extra available: $extraAvailable = $w - $sumConst, per weight: $perWeight = $extraAvailable / $sumWeight")
        var posX = this.x
        val dc = disableConstantSpaceForWeightedChildren
        for(child in children.filter { it.visibility != Visibility.GONE }){
            val hasWeight = child.weight >= 0f
            val childConstW = if(!hasWeight || !dc) child.minW * perConst else 0f
            val childW = (childConstW + perWeight * max(0f, child.weight)).roundToInt()
            child.calculateSize(childW, h)
            child.place(posX, y, childW, h)
            posX += childW + spacing
        }

    }

}