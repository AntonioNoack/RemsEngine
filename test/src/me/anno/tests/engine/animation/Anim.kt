package me.anno.tests.engine.animation

import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS.downloads

fun main() {

    // LWJGL Assimp 3.2.3 is better than 3.3.1
    //  why? longstanding bug in Assimp
    // - https://github.com/assimp/assimp/issues/4620

    // 3.2.3: 4.1.844643017
    // 3.3.1: 5.2.1054918762
    println(org.lwjgl.assimp.Assimp.aiGetVersionMajor())
    println(org.lwjgl.assimp.Assimp.aiGetVersionMinor())
    println(org.lwjgl.assimp.Assimp.aiGetVersionRevision())

    testUI3("AnimTest") {
        // to do downgrade LWJGL again, or upgrade, if available
        ECSRegistry.init()
        FileExplorer(downloads.getChild("3d/Driving.fbx"), true, style)
    }
}