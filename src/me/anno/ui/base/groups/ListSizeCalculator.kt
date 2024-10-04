package me.anno.ui.base.groups

import me.anno.ui.Panel
import me.anno.utils.pooling.Stack
import kotlin.math.max

class ListSizeCalculator {
    companion object {
        private val stack = Stack(ListSizeCalculator::class)
        fun push() = stack.create()
        fun pop() = stack.sub(1)
    }

    var maxX = 0
    var maxY = 0
    var constantSum = 0
    var constantSumWW = 0
    var weightSum = 0f

    var availableW = 0
    var availableH = 0

    fun init(panel: PanelList, w: Int, h: Int) {
        constantSum = 0
        constantSumWW = 0
        weightSum = 0f
        maxX = panel.x
        maxY = panel.y
        availableW = w - panel.padding.width
        availableH = h - panel.padding.height
    }

    fun addChildSizeX(child: Panel, childCount: Int) {
        child.calculateSize(availableW, availableH)
        val dx = child.minW * childCount
        maxY = max(maxY, child.y + child.minH)
        availableW = max(0, availableW - dx)
        addWeight(child.weight, childCount, dx)
    }

    fun addChildSizeY(child: Panel, childCount: Int) {
        child.calculateSize(availableW, availableH)
        val dy = child.minH * childCount
        maxX = max(maxX, child.x + child.minW)
        availableH = max(0, availableH - dy)
        addWeight(child.weight, childCount, dy)
    }

    fun copySizeOntoChildren(child: Panel, children: List<Panel>) {
        for (i in 1 until children.size) {
            val childI = children[i]
            childI.width = child.width
            childI.height = child.height
            childI.minW = child.minW
            childI.minH = child.minH
        }
    }

    private fun addWeight(weight: Float, childCount: Int, dy: Int) {
        constantSum += dy
        if (weight > 0f) {
            weightSum += weight * childCount
        } else {
            constantSumWW += dy
        }
    }

    fun addSpacing(spacing: Int, count: Int) {
        val spaceCount = max(0, count - 1)
        val totalSpace = spacing * spaceCount
        constantSum += totalSpace
        constantSumWW += totalSpace
    }
}