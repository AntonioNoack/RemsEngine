package me.anno.mesh.gltf

import de.javagl.jgltf.logging.Logger
import de.javagl.jgltf.model.io.GltfModelReader
import de.javagl.jgltf.viewer.AbstractGltfViewer
import de.javagl.jgltf.viewer.ExternalCamera
import de.javagl.jgltf.viewer.GlContext
import de.javagl.jgltf.viewer.lwjgl.GlContextLwjgl
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.gpu.GFX.windowStack
import me.anno.gpu.Window
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.io.FileReference
import me.anno.studio.StudioBase
import me.anno.ui.base.Panel
import me.anno.utils.OS
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import java.awt.Canvas
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

fun main() {

    GltfLogger.setup()

    // what's the difference between model and asset reader???
    // GltfAssetReader
    val reader = GltfModelReader()
    val asset = reader.read(FileReference(OS.downloads, "SimpleSkin.gltf").toUri())
    println(asset)

    // GltfModelWriterV2().writeEmbedded(asset as GltfModelV2, System.out)

    val viewer = GltfViewerLwjgl()
    viewer.addGltfModel(asset)

    // column-major
    val viewMatrix = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    val projMatrix = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 0.2f
    )

    val extCamera = object : ExternalCamera {
        override fun getViewMatrix() = viewMatrix
        override fun getProjectionMatrix() = projMatrix
    }

    viewer.setExternalCamera(extCamera)

    val studio = object : StudioBase(false, "Test", 0) {
        override fun createUI() {
            windowStack.add(Window(object : Panel(style) {
                override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                    super.onDraw(x0, y0, x1, y1)
                    GFX.check()
                    val frame = FBStack["", w, h, 4, false, 1]
                    Frame(frame) {
                        Frame.bind()
                        viewer.doRender2()
                    }
                    Shader.lastProgram = -1
                    frame.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                    GFX.copy()
                    GFX.check()
                }
            }))
        }
    }

    studio.run()

}

class GltfViewerLwjgl : AbstractGltfViewer<Component?>() {
    /**
     * The AWTGLCanvas, i.e. the rendering component of this renderer
     */
    private var glComponent: Component? = null

    /**
     * The [GlContext]
     */
    private val glContext: GlContextLwjgl

    /**
     * Whether the component was resized, and glViewport has to be called
     */
    private var viewportNeedsUpdate = true
    public override fun getGlContext(): GlContext {
        return glContext
    }

    override fun getRenderComponent(): Component? {
        return glComponent
    }

    override fun getWidth(): Int {
        return glComponent!!.width
    }

    override fun getHeight(): Int {
        return glComponent!!.height
    }

    override fun triggerRendering() {
        if (renderComponent != null) {
            renderComponent!!.repaint()
        }
    }

    override fun prepareRender() {
        // Nothing to do here
    }

    public override fun render() {
        // Enable the color and depth mask explicitly before calling glClear.
        // When they are not enabled, they will not be cleared!
        // GL11.glColorMask(true, true, true, true)
        // GL11.glDepthMask(true)
        // GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
        renderGltfModels()
    }

    companion object {
        /**
         * The logger used in this class
         */
        private val logger = Logger.getLogger(GltfViewerLwjgl::class.java.name)
    }

    fun doRender2() {
        doRender()
    }

    /**
     * Creates a new GltfViewerJogl
     */
    init {
        try {
            val glComponent = object : Component() {

                fun initGL() {
                    // correct?
                    // #anno modified, because the libraries seem to have changed
                    println("initGL()")
                    GL.createCapabilities()
                }

                fun paintGL() {
                    if (viewportNeedsUpdate) {
                        GL11.glViewport(0, 0, width, height)
                        viewportNeedsUpdate = false
                    }
                    doRender()
                    try {
                        // swapBuffers()
                    } catch (e: Exception) {
                        logger.severe("Could not swap buffers")
                    }
                }
            }

            this.glComponent = glComponent
            glComponent.addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent) {
                    viewportNeedsUpdate = true
                }
            })

        } catch (e: Exception) {
            logger.severe("Could not create AWTGLCanvas")
            glComponent = Canvas()
        }

        // Without setting the minimum size, the canvas cannot
        // be resized when it is embedded in a JSplitPane
        glComponent!!.minimumSize = Dimension(10, 10)
        glContext = GlContextLwjgl()
    }
}