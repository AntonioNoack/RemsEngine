package me.anno.tests.shader

/*private fun compileCachedShaders() {
    GFX.checkIsGFXThread()
    ShaderCache.init()
    val list = ShaderCache.cacheFolder.listChildren().toSet()
    val fine = list.filter { sample ->
        if (sample.lcExtension == "bin") {
            val prefix = "${sample.name}."
            list.any { o -> o != sample && o.name.startsWith(prefix) }
        } else false
    }
    for (sample in fine) {
        val prefix = "${sample.nameWithoutExtension}."
        val resp = list.filter { o -> o != sample && o.name.startsWith(prefix) }
        if (resp.size == 1) {
            // compute shader
            val f0 = resp[0].readTextSync()
            ShaderCache.createShader(f0, null)
        } else {
            // graphics shader
            val i = if (resp[0].name.contains(".vs.")) 0 else 1
            val vs = resp[i].readTextSync()
            val fs = resp[1 - i].readTextSync()
            ShaderCache.createShader(vs, fs)
        }
    }
}

fun main() {
    // glProgramBinary() doesn't work with RenderDoc :/
    if (true) {
        disableRenderDoc()
        testUI3("ShaderCache") {
            compileCachedShaders()
            Panel(DefaultConfig.style)
        }
    } else {
        // works just fine
        HiddenOpenGLContext.createOpenGL()
        compileCachedShaders()
    }
}*/