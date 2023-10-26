package me.anno.tests.gfx.textures

import me.anno.Build
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    // todo scene loading is too slow
    // todo performance is bad, then textures are loading
    //  > image creation is taking lots of time; probably mipmap generation
    Build.isDebug = false // disable glGetError()
    testSceneWithUI("GLB missing textures", downloads.getChild("Blender 2.glb"))
}

// todo Rem's Studio is using bad (non-fx) file chooser... fix that
// todo FileExplorer MUST NOT lag
