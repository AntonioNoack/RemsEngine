package me.anno.gpu

import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.NullFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.mesh.obj.SimpleOBJReader
import me.anno.utils.OS
import me.anno.utils.OS.res
import me.anno.utils.Sleep.waitUntilDefined
import java.io.IOException
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Draws the Rem logo when the engine starts.
 * */
@Suppress("unused")
object Logo {

    // can be set by the application
    var logoBackgroundColor = 0
    var logoIconColor = 0xff212256.toInt()

    private val logoSrc = res.getChild("icon.obj")

    private val shader = Shader( // very simple shader, drawing a single color
        "logo", listOf(
            Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
            Variable(GLSLType.V3F, "size")
        ), "void main(){ gl_Position = vec4(coords * size, 1.0); }",
        emptyList(), listOf(
            Variable(GLSLType.V4F, "logoColor")
        ), "void main(){ gl_FragColor = logoColor; }"
    )

    init {
        shader.ignoreNameWarnings("normals,uvs,tangents,colors")
    }

    val frame by lazy { // lazy, so we can get GFX.maxSamples
        Framebuffer(
            "logo", 1, 1, min(8, GFX.maxSamples),
            TargetType.UInt8x4, DepthBufferType.NONE
        )
    }

    fun drawLogo(width: Int, height: Int, destroy: Boolean): Boolean {

        GFX.check()

        // extend space left+right/top+bottom (zooming out a little)
        val maxSize = max(width, height)
        val sw = height.toFloat() / maxSize
        val sh = width.toFloat() / maxSize

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
                useFrame(NullFramebuffer) {
                    GFX.copy(frame)
                }
            } else drawLogo(shader)
        }

        if (destroy) {
            frame?.destroy()
            shader.destroy()
        }

        GFX.check()
        return success
    }

    var mesh: Mesh? = null
    var requested = false
    var hasMesh = false

    fun getLogoMesh(async: Boolean): Mesh? {
        if (requested) {
            if (!async) {
                waitUntilDefined(true) { mesh }
            }
        } else {
            requested = true
            if (async) {
                logoSrc.inputStream { i, e ->
                    e?.printStackTrace()
                    readMesh(i)
                }
            } else {
                readMesh(logoSrc.inputStreamSync())
            }
        }
        return mesh
    }

    private fun readMesh(i: InputStream?) {
        mesh = if (i != null) SimpleOBJReader(i).mesh else null
        hasMesh = true
    }

    fun drawLogo(shader: Shader): Boolean {
        // load icon.obj as file, and draw it
        GFXState.currentBuffer.clearColor(logoBackgroundColor, 1f)
        var success = false
        renderPurely {
            try {
                // you can override this file, if you want to change the logo
                // but please show Rem's Engine somewhere in there!
                val async = OS.isWeb // must be async on Web; maybe Android too later
                val mesh = getLogoMesh(async)
                if (mesh != null) {
                    mesh.draw(null, shader, 0)
                    success = true
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return success
    }
}