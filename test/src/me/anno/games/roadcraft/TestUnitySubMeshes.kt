package me.anno.games.roadcraft

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference

// this experiment improved the submesh-assignment a lot
// we now base it on the name, if possible
fun main() {
    OfficialExtensions.initForTests()
    val source = getReference(
        "E:/Assets/Unity/Polygon/Construction.zip/" +
                "PolygonConstruction/Prefabs/Vehicles/SM_Veh_Pickup_01.prefab"
    )
    PrefabCache[source]
    testSceneWithUI(source.name, source)
    Engine.requestShutdown()
}