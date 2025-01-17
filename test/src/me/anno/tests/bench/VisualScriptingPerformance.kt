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
    // ~0.8ns/iteration on Ryzen 5 2600,
    // 0.6ns on Ryzen 9 7950x3d
    clock.benchmark(50, 1000, n, "Kotlin") {
        n.factorial()
    }
    val (g, start) = createFactorialGraph(n)
    // ~300ns/iteration on Ryzen 5 2600,
    // 150-160ns on Ryzen 9 7950x3d
    clock.benchmark(5, 100, n, "Visual Scripting") {
        g.execute(start)
    }
}

// todo add a VisualScriptComponent
// todo visual scripting has become quite large, so I think it should become its own module,
//  and only the very basics stay in core :)
// todo define a graph into a file, and use them
// todo node to call a function (be it Kotlin, Lua or graph)
// todo define sub-functions in the same graph? group them, and then call them?
// todo can we compile/generate efficient Kotlin/Java/JVM-ByteCode from a graph? 😁

// todo logic for dragging-reordering in array-panels