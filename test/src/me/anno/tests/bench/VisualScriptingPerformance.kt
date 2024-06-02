package me.anno.tests.bench

import me.anno.maths.Maths.factorial
import me.anno.tests.graph.visual.FlowGraphTest.createFactorialGraph
import me.anno.utils.Clock

/**
 * test visual scripting performance -> ~400x slower for now, which is to be expected
 * */
fun main() {
    val n = 10000
    val clock = Clock("VisualScriptingBench")
    clock.benchmark(50, 1000, n, "Kotlin") { // ~0.8ns/iteration
        n.factorial()
    }
    val (g, start) = createFactorialGraph(n)
    clock.benchmark(5, 100, n, "Visual Scripting") { // ~300ns/iteration
        g.execute(start)
    }
}

// todo add a VisualScriptComponent
// todo visual scripting has become quite large, so I think it should become its own module,
//  and only the very basics stay in core :)
// todo define a graph into a file, and use them
// todo node to call a function (be it Kotlin, Lua or graph)
// todo define sub-functions in the same graph? group them, and then call them?
// todo can we compile/generate efficient Kotlin from a graph? 😁