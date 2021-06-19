package me.anno.scripting.visual

/**
 * a function has multiple inputs and multiple outputs
 * it has a multiple entry points, and multiple exits
 * */
open class VisualFunction : NamedVisual() {

    val inputValues = ArrayList<VisualProperty>()
    val outputValues = ArrayList<VisualProperty>()

    val inputNodes = ArrayList<VisualNode>()

    var nodes = ArrayList<VisualNode>()

    fun run(node: VisualNode? = null) {
        if(node == null) return run(inputNodes[0])
        // todo clear all values, create a copy, if required
        // todo then run all following nodes...
    }

    override fun getClassName(): String = "VisualFunction"

}