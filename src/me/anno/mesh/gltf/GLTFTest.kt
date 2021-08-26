package me.anno.mesh.gltf

import de.javagl.jgltf.model.io.GltfModelReader
import me.anno.config.DefaultConfig.style
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.windowStack
import me.anno.gpu.RenderState.blendMode
import me.anno.gpu.RenderState.depthMode
import me.anno.gpu.Window
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.drawing.Perspective.perspective2
import me.anno.input.Input
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.studio.StudioBase
import me.anno.ui.base.Panel
import me.anno.utils.maths.Maths.pow
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Vector3f

fun main() {

    GltfLogger.setup()

    val reader = GltfModelReader()
    val model = reader.read(getReference(OS.downloads, "BarramundiFish.gltf").toUri())
    // LOGGER.info(asset)

    val extCamera = ExternalCameraImpl()

    val viewer = GltfViewerLwjgl()
    viewer.setup(extCamera, model)

    LogManager.disableLogger("MatrixOps")
    LogManager.disableLogger("RenderCommandUtils")
    LogManager.disableLogger("GlContextLwjgl")

    val studio = object : StudioBase(false, "Test", 0) {
        override fun createUI() {
            windowStack.add(Window(object : Panel(style) {

                val rot = Vector3f()
                var radius = 100f

                val transform = Matrix4f()

                override fun tickUpdate() {
                    invalidateDrawing()
                }

                override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
                    if (1 in Input.mouseKeysDown) {
                        val speed = 3f / h
                        rot.x += speed * dy
                        rot.y += speed * dx
                    }
                }

                override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
                    radius *= pow(0.2f, dy / h)
                }

                override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                    super.onDraw(x0, y0, x1, y1)

                    GFX.check()

                    transform.identity()
                    transform.perspective2(1.5f, GFX.width / GFX.height.toFloat(), 0.01f, 1000f)
                    transform.translate(0f, 0f, -radius)
                    transform.rotateX(rot.x)
                    transform.rotateY(rot.y)

                    extCamera.update(transform)

                    blendMode.use(BlendMode.DEFAULT) {
                        depthMode.use(DepthMode.GREATER_EQUAL) {
                            viewer.glRender()
                        }
                    }

                    GFX.check()

                }
            }))
        }
    }

    studio.run()

}

