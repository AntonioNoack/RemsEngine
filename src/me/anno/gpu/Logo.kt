package me.anno.gpu

import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.NullFramebuffer
import me.anno.gpu.framebuffer.NullFramebuffer.setFrameNullSize
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.mesh.obj.SimpleOBJReader
import me.anno.utils.OS.res
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
            Variable(GLSLType.V3F, "positions", VariableMode.ATTR),
            Variable(GLSLType.V3F, "size")
        ), "void main(){ gl_Position = vec4(positions * size, 1.0); }",
        emptyList(), listOf(
            Variable(GLSLType.V4F, "logoColor")
        ), "void main(){ gl_FragColor = logoColor; }"
    )

    val frame by lazy { // lazy, so we can get GFX.maxSamples
        Framebuffer(
            "logo", 1, 1, min(8, GFX.maxSamples),
            TargetType.UInt8x4, DepthBufferType.NONE
        )
    }

    fun drawLogo(width: Int, height: Int) {

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
        GFXState.blendMode.use(null) {
            if (frame != null) {
                setFrameNullSize(width, height)
                useFrame(width, height, true, frame) {
                    drawLogo(shader)
                }
                useFrame(NullFramebuffer) {
                    Blitting.copyColor(frame, true)
                }
            } else drawLogo(shader)
        }

        GFX.check()
    }

    fun destroy() {
        frame.destroy()
        shader.destroy()
        mesh = null
        requested = false
        hasMesh = false
    }

    var mesh: Mesh? = null
    var requested = false
    var hasMesh = false

    fun getLogoMesh(): Mesh? {
        if (!requested) {
            requested = true
            logoSrc.inputStream { i, e ->
                e?.printStackTrace()
                readMesh(i)
            }
        }
        return mesh
    }

    private fun readMesh(i: InputStream?) {
        mesh = if (i != null) SimpleOBJReader(i).read() else null
        hasMesh = true
    }

    fun drawLogo(shader: Shader) {
        // load icon.obj as file, and draw it
        GFXState.currentBuffer.clearColor(logoBackgroundColor, 1f)
        renderPurely {
            try {
                // you can override this file, if you want to change the logo
                // but please show Rem's Engine somewhere in there!
                getLogoMesh()?.draw(null, shader, 0)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}