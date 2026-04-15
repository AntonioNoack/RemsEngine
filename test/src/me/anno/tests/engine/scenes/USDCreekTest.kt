package me.anno.tests.engine.scenes

import me.anno.engine.OfficialExtensions
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.InnerFolderCache
import me.anno.mesh.usd.USDReader
import org.apache.logging.log4j.LogManager

// todo this needs USD reading support, which we need to implement...
//  our USD reader currently outputs nothing...
fun main() {

    LogManager.disableInfoLogs("Saveable,DefaultConfig,ExtensionManager,JVMExtension,DefaultRenderingHints")
    OfficialExtensions.initForTests()

    InnerFolderCache.registerFileExtensions("usda", USDReader::readAsFolder)

    val folder = getReference("/media/antonio/4TB WDRed/Assets/SampleScenes/JungleRuins_1_0_1b")
    val sceneRef = folder.getChild("USD/elements/Creek/Creek.usd")

    USDReader.readAsFolder(sceneRef) { scene, thrown ->
        println(scene)
        thrown?.printStackTrace()
    }

}
