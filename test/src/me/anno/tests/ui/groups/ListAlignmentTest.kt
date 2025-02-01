package me.anno.tests.ui.groups

import me.anno.config.DefaultConfig.style
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.ListAlignment
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.custom.CustomList
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class ListAlignmentTest {

    class ExactSizePanel(val w: Int, val h: Int) : Panel(style) {
        override fun calculateSize(w: Int, h: Int) {
            minW = this.w
            minH = this.h
        }
    }

    private fun getTestAlignment(i: Int): AxisAlignment {
        return when (i) {
            0 -> AxisAlignment.MIN
            1 -> AxisAlignment.CENTER
            2 -> AxisAlignment.MAX
            else -> AxisAlignment.FILL
        }
    }

    @Test
    fun testPanelListX() {
        val list = PanelListX(style)
        list.padding.set(1, 2, 3, 4)
        for (i in 0 until 5) {
            val child = ExactSizePanel(100, 60)
            list.add(child)
            child.alignmentY = getTestAlignment(i)
        }
        val weighted1 = ExactSizePanel(0, 100).fill(1f) // ->  50
        val weighted2 = ExactSizePanel(0, 100).fill(3f) // -> 150
        weighted1.alignmentY = AxisAlignment.MIN
        list.add(weighted1)
        list.add(weighted2)
        val w = 5 * 100 + 50 + 150 + list.padding.width
        val h = 120 + list.padding.height
        list.calculateSize(w, h)
        assertEquals(list.minW, 5 * 100 + 4)
        assertEquals(list.minH, 106)
        list.setPosSize(10, 10, w, h)
        checkPosSize(list, 10, 10, w, h)
        checkPosSize(list.children[0], 11, 12, 100, 60) // min-y
        checkPosSize(list.children[1], 111, 42, 100, 60) // center-y
        checkPosSize(list.children[2], 211, 72, 100, 60) // max-y
        checkPosSize(list.children[3], 311, 12, 100, 120) // fill-y
        checkPosSize(list.children[4], 411, 12, 100, 120) // fill-y
        checkPosSize(list.children[5], 511, 12, 50, 100) // weight 1.0
        checkPosSize(list.children[6], 561, 12, 150, 120) // weight 3.0
    }

    @Test
    fun testPanelListY() {
        val list = PanelListY(style)
        list.padding.set(1, 2, 3, 4)
        for (i in 0 until 5) {
            val child = ExactSizePanel(60, 100)
            list.add(child)
            child.alignmentX = getTestAlignment(i)
        }
        val weighted1 = ExactSizePanel(100, 0).fill(1f) // ->  50
        val weighted2 = ExactSizePanel(100, 0).fill(3f) // -> 150
        weighted1.alignmentX = AxisAlignment.MIN
        list.add(weighted1)
        list.add(weighted2)
        val w = 120 + list.padding.width
        val h = 5 * 100 + 50 + 150 + list.padding.height
        list.calculateSize(w, h)
        assertEquals(list.minW, 104)
        assertEquals(list.minH, 5 * 100 + 6)
        list.setPosSize(10, 10, w, h)
        checkPosSize(list, 10, 10, w, h)
        checkPosSize(list.children[0], 11, 12, 60, 100) // min-y
        checkPosSize(list.children[1], 41, 112, 60, 100) // center-y
        checkPosSize(list.children[2], 71, 212, 60, 100) // max-y
        checkPosSize(list.children[3], 11, 312, 120, 100) // fill-y
        checkPosSize(list.children[4], 11, 412, 120, 100) // fill-y
        checkPosSize(list.children[5], 11, 512, 100, 50) // weight 1.0
        checkPosSize(list.children[6], 11, 562, 120, 150) // weight 3.0
    }

    @Test
    fun testPanelStack() {
        val list = PanelStack(style)
        list.padding.set(1, 2, 3, 4)
        for (i in 0 until 5) {
            val child = ExactSizePanel(60, 100)
            list.add(child)
            child.alignmentX = getTestAlignment(i)
        }
        val weighted1 = ExactSizePanel(100, 0).fill(1f) // ->  50
        val weighted2 = ExactSizePanel(100, 0).fill(3f) // -> 150
        weighted1.alignmentX = AxisAlignment.MIN
        list.add(weighted1)
        list.add(weighted2)
        val w = 100
        val h = 100
        list.calculateSize(w, h)
        assertEquals(list.minW, 104) // 4 = left + right
        assertEquals(list.minH, 106) // 6 = top + bottom
        list.setPosSize(10, 10, w, h)
        checkPosSize(list, 10, 10, w, h)
        checkPosSize(list.children[0], 11, 12, 60, 94) // min-y
        checkPosSize(list.children[1], 29, 12, 60, 94) // center-y
        checkPosSize(list.children[2], 47, 12, 60, 94) // max-y
        checkPosSize(list.children[3], 11, 12, 96, 94) // fill-y
        checkPosSize(list.children[4], 11, 12, 96, 94) // fill-y
        checkPosSize(list.children[5], 11, 12, 96, 94) // weight 1.0
        checkPosSize(list.children[6], 11, 12, 96, 94) // weight 3.0
    }

    private fun setupPanelList2D(isY: Boolean, listAlignment: ListAlignment): PanelList2D {
        val list = PanelList2D(isY, null, style)
        list.childWidth = 50
        list.childHeight = 70
        list.listAlignmentX = listAlignment
        list.listAlignmentY = listAlignment
        list.spacing = 2
        list.padding.set(10)
        for (i in 0 until 5) {
            val child = ExactSizePanel(40, 60)
            list.add(child)
            child.alignmentX = getTestAlignment(i)
            child.alignmentY = getTestAlignment(i)
        }
        list.calculateSize(202, 202)
        list.setPosSize(10, 10, 202, 202)
        checkPosSize(list, 10, 10, 202, 202)
        return list
    }

    @Test
    fun testPanelList2DAlignY() {
        testPanelList2DAlignY(ListAlignment.ALIGN_MIN, 0, 0)
        testPanelList2DAlignY(ListAlignment.ALIGN_CENTER, 12, 18)
        testPanelList2DAlignY(ListAlignment.ALIGN_MAX, 24, 36)
    }

    @Test
    fun testPanelList2DAlignX() {
        testPanelList2DAlignX(ListAlignment.ALIGN_MIN, 0, 0)
        testPanelList2DAlignX(ListAlignment.ALIGN_CENTER, 12, 18)
        testPanelList2DAlignX(ListAlignment.ALIGN_MAX, 24, 36)
    }

    fun testPanelList2DAlignY(alignment: ListAlignment, dx: Int, dy: Int) {
        val list = setupPanelList2D(true, alignment)
        checkPosSize(list.children[0], dx + 22, dy + 22, 40, 60)
        checkPosSize(list.children[1], dx + 74 + 5, dy + 22 + 5, 40, 60)
        checkPosSize(list.children[2], dx + 126 + 10, dy + 22 + 10, 40, 60)
        checkPosSize(list.children[3], dx + 22, dy + 94, 50, 70)
        checkPosSize(list.children[4], dx + 74, dy + 94, 50, 70)
    }

    fun testPanelList2DAlignX(alignment: ListAlignment, dx: Int, dy: Int) {
        val list = setupPanelList2D(false, alignment)
        checkPosSize(list.children[0], dx + 22, dy + 22, 40, 60)
        checkPosSize(list.children[1], dx + 22 + 5, dy + 94 + 5, 40, 60)
        checkPosSize(list.children[2], dx + 74 + 10, dy + 22 + 10, 40, 60)
        checkPosSize(list.children[3], dx + 74, dy + 94, 50, 70)
        checkPosSize(list.children[4], dx + 126, dy + 22, 50, 70)
    }

    @Test
    fun testPanelList2DScaleChildren() {
        val list = setupPanelList2D(true, ListAlignment.SCALE_CHILDREN)
        checkPosSize(list.children[0], 22, 22, 40, 60) // alignment min -> size & pos not actually changed
        checkPosSize(list.children[1], 91, 36, 40, 60)
        checkPosSize(list.children[2], 160, 50, 40, 60)
        checkPosSize(list.children[3], 22, 112, 58, 88)
        checkPosSize(list.children[4], 82, 112, 58, 88)
    }

    @Test
    fun testCustomListX() {
        val list = CustomList(false, style)
        list.spacing = 10
        list.padding.set(10)
        // 10 (pos) | 10 (padding) | 30 (child#0) | 10 (spacer) | 60 (child#1) | 10 (spacer) | 90 (child#2) | 10 (padding)
        for (i in 0 until 3) {
            val child = ExactSizePanel(10, 10)
            list.add(child, 1f + i)
            // these alignments should be ignored
            child.alignmentX = getTestAlignment(i)
            child.alignmentY = getTestAlignment(i)
        }
        list.calculateSize(220, 120)
        list.setPosSize(10, 10, 220, 120)
        checkPosSize(list, 10, 10, 220, 120)
        checkPosSize(list.children[0], 20, 20, 30, 100)
        checkPosSize(list.children[1], 60, 20, 60, 100)
        checkPosSize(list.children[2], 130, 20, 90, 100)
    }

    @Test
    fun testCustomListY() {
        val list = CustomList(true, style)
        list.spacing = 10
        list.padding.set(10)
        // 10 (pos) | 10 (padding) | 30 (child#0) | 10 (spacer) | 60 (child#1) | 10 (spacer) | 90 (child#2) | 10 (padding)
        for (i in 0 until 3) {
            val child = ExactSizePanel(10, 10)
            list.add(child, 1f + i)
            // these alignments should be ignored
            child.alignmentX = getTestAlignment(i)
            child.alignmentY = getTestAlignment(i)
        }
        list.calculateSize(120, 220)
        list.setPosSize(10, 10, 120, 220)
        checkPosSize(list, 10, 10, 120, 220)
        checkPosSize(list.children[0], 20, 20, 100, 30)
        checkPosSize(list.children[1], 20, 60, 100, 60)
        checkPosSize(list.children[2], 20, 130, 100, 90)
    }

    fun checkPosSize(child: Panel, x: Int, y: Int, w: Int, h: Int) {
        assertTrue(child.x == x && child.y == y && child.width == w && child.height == h) {
            "expected [$x,$y,$w,$h], got [${child.x},${child.y},${child.width},${child.height}]"
        }
    }
}