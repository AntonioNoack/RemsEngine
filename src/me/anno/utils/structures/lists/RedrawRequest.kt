package me.anno.utils.structures.lists

import me.anno.ui.Panel

data class RedrawRequest(val panel: Panel, val x0: Int, val y0: Int, val x1: Int, val y1: Int)
