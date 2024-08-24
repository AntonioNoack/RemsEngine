package me.anno.tests.mesh.pbrt

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.InnerFolderCache
import org.apache.logging.log4j.LogManager

fun main() {
    // Disney 2016 Moana Island sample scene, downloaded as PBRT-v4
    // https://disneyanimation.com/resources/moana-island-scene/
    // -> we don't support their PTX image file format, and it seems to be some proprietary raw BS...
    // -> give up on that for now, and open proper formats
    OfficialExtensions.initForTests()
    InnerFolderCache.registerFileExtensions("pbrt", PBRTReader::readAsFolder)
    LogManager.disableLoggers("Saveable,GFXBase,GPUShader,EngineBase,OpenXRSystem,OpenXRUtils")
    val src = getReference(
        "G:\\Assets\\island\\pbrt-v4\\isPandanusA/isPandanusA_geometry.pbrt"
    )
    testSceneWithUI("PBRT", src)
}