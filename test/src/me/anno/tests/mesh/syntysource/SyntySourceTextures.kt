package me.anno.tests.mesh.syntysource

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.FileReference.Companion.getReference

fun main() {
    // load synty source mesh,
    //  - where texture was on weird PC on drive, which we don't have
    //  - find such a file
    //  - introduce path mappings
    //  - make them work
    val path = getReference("E:/Assets/Sources/POLYGON_Construction_Source_Files.zip/SourceFiles/FBX/SK_Veh_Crane_01.fbx")
    testSceneWithUI("Synty", path)
}