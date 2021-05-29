package me.anno.utils.test

import de.javagl.jgltf.model.io.GltfAssetReader
import de.javagl.jgltf.viewer.AbstractGltfViewer
import de.javagl.jgltf.viewer.GlContext
import de.javagl.jgltf.viewer.lwjgl.GlContextLwjgl
import me.anno.io.FileReference
import me.anno.utils.OS
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import java.awt.Canvas
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.logging.Logger

fun main() {

    val reader = GltfAssetReader()
    val asset = reader.read(FileReference(OS.downloads, "SimpleSkin.gltf").toUri())
    println(asset)

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

    override fun render() {
        // Enable the color and depth mask explicitly before calling glClear.
        // When they are not enabled, they will not be cleared!
        GL11.glColorMask(true, true, true, true)
        GL11.glDepthMask(true)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
        renderGltfModels()
    }

    companion object {
        /**
         * The logger used in this class
         */
        private val logger = Logger.getLogger(GltfViewerLwjgl::class.java.name)
    }

    /**
     * Creates a new GltfViewerJogl
     */
    init {
        try {
            val glComponent = object: Component() {

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