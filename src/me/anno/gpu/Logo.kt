package me.anno.gpu

import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.NullFramebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.files.FileReference
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import java.io.IOException
import kotlin.math.min

// can be set by the application
var logoBackgroundColor = 0
var logoIconColor = 0x212256

val logoSrc = FileReference.getReference("res://icon.obj")

val shader by lazy {

    val shader = Shader(
        "logo", listOf(
            Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
            Variable(GLSLType.V3F, "size")
        ), "void main(){ gl_Position = vec4(coords * size, 1.0); }",
        emptyList(), listOf(
            Variable(GLSLType.V4F, "logoColor")
        ), "void main(){ gl_FragColor = logoColor; }"
    )

    shader.use()
    shader.ignoreNameWarnings("normals", "uvs", "tangents", "colors")

    shader

}

val frame by lazy {
    Framebuffer(
        "logo", 1, 1, min(8, GFX.maxSamples),
        1, false, DepthBufferType.NONE
    )
}

fun drawLogo(width: Int, height: Int, destroy: Boolean): Boolean {

    GFX.check()

    val logger = LogManager.getLogger("Logo")
    logger.info("Showing Engine Logo")

    // extend space left+right/top+bottom (zooming out a little)
    val sw: Float
    val sh: Float
    if (width > height) {
        sw = height.toFloat() / width
        sh = 1f
    } else {
        sw = 1f
        sh = width.toFloat() / height
    }

    val shader = shader
    shader.use()
    shader.v3f("size", sw, sh, 0f)
    shader.v4f("logoColor", logoIconColor)

    GFX.check()

    val frame = if (GFX.maxSamples > 1) frame else null

    var success = false
    GFXState.blendMode.use(null) {
        if (frame != null) {
            GFX.setFrameNullSize(width, height)
            useFrame(width, height, true, frame) {
                success = drawLogo(shader)
            }
            frame.blitTo(NullFramebuffer)
        } else drawLogo(shader)
    }

    if (destroy) {
        frame?.destroy()
        shader.destroy()
    }

    GFX.check()
    return success

}

fun drawLogo(shader: Shader): Boolean {
    // load icon.obj as file, and draw it
    val c = logoBackgroundColor
    GFXState.currentBuffer.clearColor(c.r01(), c.g01(), c.b01(), 1f)
    var success = false
    renderPurely {
        try {
            // ensure mesh is a known class
            registerCustomClass(Mesh())
            // you can override this file, if you want to change the logo
            // but please show Rem's Engine somewhere in there!
            val async = OS.isWeb // must be async on Web; maybe Android too later
            val mesh = MeshCache[logoSrc, async]
            if (mesh != null) {
                mesh.ensureBuffer()
                for (i in 0 until mesh.numMaterials) {
                    mesh.draw(shader, i)
                }
                success = true
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    return success
}