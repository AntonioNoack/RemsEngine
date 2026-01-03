package me.anno.experiments

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference

/**
 * Ubuntu doesn't have a built-in .glb viewer, so let's just bind our engine as one :3
 * */
fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        val ref = getReference(args[0])
        testSceneWithUI(ref.name, ref)
    } else {
        System.err.println("Usage: <file>")
    }
}