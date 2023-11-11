package me.anno.tests.gfx

import me.anno.cache.data.ImageToTexture.Companion.imageTimeout
import me.anno.cache.instances.SVGMeshCache
import me.anno.config.DefaultConfig
import me.anno.ecs.components.mesh.Material.Companion.defaultMaterial
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.RenderDoc.forceLoadRenderDoc
import me.anno.gpu.drawing.SVGxGFX
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib
import me.anno.input.Input
import me.anno.ui.Panel
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.Color.white4
import me.anno.utils.OS.downloads
import org.joml.Matrix4fArrayList

fun main() {

    forceLoadRenderDoc()
    testUI3("SVG") {

        // val srcFile = downloads.getChild("2d/tiger.svg")
        val srcFile = downloads.getChild("2d/gradientSample2.svg")
        // val srcFile = downloads.getChild("2d/spreadSample.svg")
        // val srcFile = downloads.getChild("2d/recruitment.svg")
        // val srcFile = downloads.getChild("2d/polyline2.svg")
        object : Panel(DefaultConfig.style) {
            override val canDrawOverBorders get() = true
            override fun onUpdate() {
                invalidateDrawing()
            }

            // show lines with new method
            /*init {
                val v = RenderView(EditorState, PlayMode.PLAYING, style)
                v.renderMode = RenderMode.LINES
                RenderView.currentInstance = v
            }*/

            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                super.onDraw(x0, y0, x1, y1)

                val msaaBuffer = FBStack["svg", x1 - x0, y1 - y0, TargetType.UByteTarget4, 8, DepthBufferType.NONE]
                msaaBuffer.clearColor(-1, false)

                val transform = Matrix4fArrayList()
                transform.scale((y1 - y0).toFloat() / (x1 - x0).toFloat(), 1f, 1f)
                if (Input.isControlDown) transform.scale(0.2f, 0.2f, 1f)

                useFrame(msaaBuffer) {
                    if (Input.isShiftDown) {
                        // new method, uses standard shader and more complex meshes, but is incomplete
                        transform.scale(1f, -1f, 1f)
                        val shader = ECSShaderLib.pbrModelShader.value
                        shader.use()
                        shader.m4x3("localTransform", null)
                        shader.m4x4("transform", transform)
                        val mesh = MeshCache[srcFile, false]!!
                        defaultMaterial.bind(shader)
                        shader.v1i("hasVertexColors", mesh.hasVertexColors)
                        mesh.draw(shader, 0)
                    } else {
                        // old method, uses specialized shader
                        val buffer = SVGMeshCache[srcFile, imageTimeout, false]!!
                        val white = TextureLib.whiteTexture
                        SVGxGFX.draw3DSVG(
                            transform, buffer, white,
                            white4, Filtering.NEAREST,
                            Clamping.CLAMP, null
                        )
                    }
                }

                GFX.copy(msaaBuffer)
            }
        }
    }
}