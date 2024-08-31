package me.anno.tests.engine

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.documents

// test loading the main scene, and get rid of all warnings
// -> there weren't any? :/ I remembered some...
fun main() {
    OfficialExtensions.initForTests()
    workspace = documents.getChild("RemsEngine/YandereSim")
    val asset = PrefabCache[workspace.getChild("School.json")]
    println("Finished loading asset")
    Engine.requestShutdown()
}