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

    /*override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        var sumW = 0
        var maxH = 0
        var weight = 0f
        val dc = disableConstantSpaceForWeightedChildren
        for(child in children.filter { it.visibility != Visibility.GONE }){
            child.calculateSize(w, h) // calculate w,h,minW,minH
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

        sumW += padding.width
        maxH += padding.height

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
        var posX = this.x + padding.width
        val dc = disableConstantSpaceForWeightedChildren
        for(child in children.filter { it.visibility != Visibility.GONE }){
            val hasWeight = child.weight >= 0f
            val childConstW = if(!hasWeight || !dc) child.minW * perConst else 0f
            val childW = (childConstW + perWeight * max(0f, child.weight)).roundToInt()
            child.calculateSize(childW, h)
            child.place(posX, y, childW, h)
            posX += childW + spacing
        }

    }*/

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        val y = y
        var maxY = y
        var constantSum = 0
        var weightSum = 0f

        val availableW = w - padding.width
        val availableH = h - padding.height

        for (child in children.filter { it.visibility != Visibility.GONE }) {
            child.calculateSize(availableW, availableH)
            // apply constraints?
            constantSum += child.minW
            maxY = max(maxY, child.y + child.minH)
            weightSum += max(0f, child.weight)
        }

        val spaceCount = children.size - 1
        constantSum += spacing * spaceCount
        sumConst = constantSum
        sumWeight = weightSum

        minW = constantSum + padding.height
        minH = (maxY - y) + padding.width

    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)

        var perWeight = 0f
        val perConst = 1f // could be used to force elements into the available space

        val availableW = w - padding.width
        val availableH = h - padding.height

        if (availableW > sumConst && sumWeight > 1e-16f) {
            val extraAvailable = availableW - sumConst
            perWeight = extraAvailable / sumWeight
        }

        var currentX = x + padding.left
        val childY = y + padding.top

        for (child in children.filter { it.visibility != Visibility.GONE }) {
            var childW = (perConst * child.minW + perWeight * max(0f, child.weight)).roundToInt()
            val currentW = currentX - x
            val remainingW = availableW - currentW
            childW = min(childW, remainingW)
            child.calculateSize(childW, availableH)
            child.placeInParent(currentX, childY)
            child.applyPlacement(childW, availableH)
            currentX += childW + spacing
        }

    }

}