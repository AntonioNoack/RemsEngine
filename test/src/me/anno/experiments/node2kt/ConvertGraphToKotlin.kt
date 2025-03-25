package me.anno.experiments.node2kt

import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.ComputeNode
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.FlowGraphNode
import me.anno.graph.visual.actions.ActionNode
import me.anno.graph.visual.local.GetLocalVariableNode
import me.anno.graph.visual.local.SetLocalVariableNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeConnector
import me.anno.graph.visual.node.NodeInput
import me.anno.tests.graph.visual.FlowGraphTest.createFactorialGraph

/**
 * this is just an experiment, it doesn't work yet
 * */
fun main() {
    // todo given any graph,
    //  compile it into clean Kotlin code,
    //  so it can be executed without much overhead

    // todo each node is represented using a function
    // todo each expression input is define inline
    // todo each other input is a graph-class-local variable

    // todo reuse these class-local-variables?

    // todo also compile state-machines... we need to code them slightly differently...
    //  can we do it without exceptions??

    val (g, start) = createFactorialGraph(10)
    g.execute(start)
    println(g.localVariables["var"])

    printGraph(g)

    val nodeIds = g.nodes.withIndex()
        .associate { it.value to it.index }

    val builder = StringBuilder()
    startClass(g, nodeIds, builder)

    for (n in g.nodes) {
        if (n is FlowGraphNode) {
            convertToFunction(n, nodeIds, builder)
        }
    }

    defineMainMethod(start, nodeIds, builder)
    finishClass(g, builder)

    println(builder)
}

fun startClass(g: FlowGraph, nodeIds: Map<Node, Int>, builder: StringBuilder) {
    builder.append("class GraphToKotlin {\n")
    // todo append all used input fields
    // todo what about output fields?
    // todo how do we handle local variables???
    for (node in g.nodes) {
        val ni = nodeIds[node]
        for (ii in node.inputs.indices) {
            val i = node.inputs[ii]
            if (i.type != "Flow" && !isQuickExpression(i)) {
                val kotlinType = getKotlinType(i.type)
                builder.append("  var ").append("v").append(ni)
                    .append("_").append(ii)
                    .append(": ").append(kotlinType)
                    .append(" = ").append(getKotlinDefault(kotlinType))
                if (kotlinType != i.type) {
                    builder.append(" /* ${node.className}.${i.name}: ${i.type} */\n")
                } else {
                    builder.append(" /* ${node.className}.${i.name} */\n")
                }
            }
        }
    }
    builder.append("\n")
}

fun getKotlinType(type: String): String {
    return when (type) {
        "Bool", "Boolean" -> "Boolean"
        "Int", "Long", "Float", "Double" -> type
        "?" -> "Any?"
        else -> if (type.endsWith("?")) type
        else "${type}?"
    }
}

fun getKotlinDefault(kotlinType: String): String {
    return when (kotlinType) {
        "Boolean" -> "false"
        "Int" -> "0"
        "Long" -> "0L"
        "Float" -> "0f"
        "Double" -> "0.0"
        else -> "null"
    }
}

fun isQuickExpression(i: NodeInput): Boolean {
    val inputNode = i.others.firstOrNull()?.node ?: return true
    return inputNode is ComputeNode && inputNode !is GetLocalVariableNode
}

fun defineMainMethod(startNode: Node, nodeIds: Map<Node, Int>, builder: StringBuilder) {
    builder.append("  fun main() = n").append(nodeIds[startNode]).append("()\n")
}

fun finishClass(g: FlowGraph, builder: StringBuilder) {
    builder.append("}\n")
}

fun convertToFunction(node: Node, nodeIds: Map<Node, Int>, builder: StringBuilder) {
    builder.append("  fun n").append(nodeIds[node]).append("(/* ${node.className} */) {\n")

    // todo write function content copied from the source class...
    // todo depends on class type
    // todo replace inputs with expressions/values
    // todo replace outputs with the respective setters
    // todo call next function in ActionNode

    when(node) {
        is GetLocalVariableNode -> {} // ??
        is SetLocalVariableNode -> {} // ??
        is CalculationNode -> {
            // todo implement calculation...
        }
        // todo implement...
    }

    if (node is ActionNode) {
        val nextNode = node.outputs[0].others.firstOrNull()?.node
        if (nextNode != null) {
            builder.append("    n").append(nodeIds[nextNode]).append("()\n")
        }
    }

    builder.append("  }\n\n")
}

fun printGraph(g: FlowGraph) {
    val nodeIds = g.nodes.withIndex()
        .associate { it.value to it.index }
    for (n in g.nodes) {
        printNode(n, nodeIds)
    }
}

fun printNode(n: Node, nodeIds: Map<Node, Int>) {
    println("[${nodeIds[n]}]: ${n.className} ({")
    for (i in n.inputs) {
        val values = if (i.others.isEmpty()) "\"${i.getValue()}\""
        else i.others.joinToString { str(it, nodeIds) }
        println("    $values -> ${i.name}: ${i.type}")
    }
    println("}, {")
    for (o in n.outputs) {
        if (o.others.isEmpty()) continue
        val values = o.others.joinToString { str(it, nodeIds) }
        println("    ${o.name}: ${o.type} -> $values")
    }
    println("})")
}

fun str(n: NodeConnector, nodeIds: Map<Node, Int>): String {
    return "[${nodeIds[n.node]}]"
}