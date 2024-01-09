package me.anno.ui.utils

import me.anno.ui.Style

// where should we move this class?
/**
 * Panel, which draws a numeric function as a graph, and lets the user navigate like on a map.
 * */
open class FunctionPanelNd(val functions: List<Pair<Function1d, Int>>, style: Style) : FunctionPanel(style) {

    constructor(function: Function1d, style: Style) : this(listOf(function to -1), style)

    override fun getNumFunctions(): Int {
        return functions.size
    }

    override fun getValue(index: Int, x: Double): Double {
        return functions[index].first.calc(x)
    }

    override fun getColor(index: Int): Int {
        return functions[index].second
    }
}