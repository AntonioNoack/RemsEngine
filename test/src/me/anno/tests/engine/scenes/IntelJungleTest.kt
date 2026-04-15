package me.anno.tests.engine.scenes

import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.InnerFolderCache
import me.anno.mesh.usd.USDReader
import me.anno.tests.engine.material.createLighting
import org.apache.logging.log4j.LogManager

// todo this needs USD reading support, which we need to implement...
//  our USD reader currently outputs nothing...
fun main() {

    LogManager.disableInfoLogs("Saveable")
    OfficialExtensions.initForTests()

    InnerFolderCache.registerFileExtensions("usda", USDReader::readAsFolder)

    val folder = getReference("/media/antonio/4TB WDRed/Assets/SampleScenes/JungleRuins_1_0_1b")
    val sceneRef = folder.getChild("USD/JungleRuins_Karma.usda")

    if (false) {
        USDReader.readAsFolder(sceneRef) { scene, thrown ->
            println(scene)
            thrown?.printStackTrace()
        }
        return
    }

    val scene = Entity("Scene")
        .add(PrefabCache.newInstance(sceneRef).waitFor() as Entity)

    // todo it would be nice to have some GI
    createLighting(scene, 20f)

    testSceneWithUI("Sponza", scene)
}
