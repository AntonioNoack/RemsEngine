package me.anno.tests.export

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.desktop

fun main() {
    // names are missing in paths... why ever...
    // -> it was an old data error, no longer happens
    OfficialExtensions.initForTests()
    val source = desktop.getChild("TestGame.jar/res/0.json")
    val prefab = PrefabCache[source]!!
    for (add in prefab.adds.keys) {
        println("add: $add")
    }
    prefab.sets.forEach { k1, k2, v ->
        println("'$k1'.'$k2' = $v")
    }
    prefab.createInstance()
    Engine.requestShutdown()
}