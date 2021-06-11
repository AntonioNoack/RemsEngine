package me.anno.mesh.gltf

import de.javagl.jgltf.model.GltfModel
import de.javagl.jgltf.viewer.AbstractGltfViewer
import de.javagl.jgltf.viewer.GlContext
import de.javagl.jgltf.viewer.lwjgl.GlContextLwjgl
import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D

class GltfViewerLwjgl : AbstractGltfViewer<Any?>() {

    override fun getRenderComponent(): Nothing? = null

    /**
     * The [GlContext]
     */
    private val glContext: GlContextLwjgl

    /**
     * Whether the component was resized, and glViewport has to be called
     */
    public override fun getGlContext(): GlContext {
        return glContext
    }

    override fun getWidth(): Int {
        return GFX.width
    }

    override fun getHeight(): Int {
        return GFX.height
    }

    override fun triggerRendering() {
        // Warning.warn("got redraw request")
    }

    override fun prepareRender() {
        // Nothing to do here
    }

    public override fun render() {
        // GL11.glColorMask(true, true, true, true)
        // GL11.glDepthMask(true)
        // GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
        renderGltfModels()
    }

    fun glRender() {
        doRender()
        // todo just use normal textures, so we don't have to invalidate the state
        Texture2D.invalidateBinding()
    }

    /**
     * Creates a new GltfViewerJogl
     */
    init {
        glContext = CustomGlContext
    }

    fun setup(extCamera: ExternalCameraImpl, model: GltfModel) {
        setExternalCamera(extCamera)
        addGltfModel(model)
        setCurrentCameraModel(null, extCamera.model)
        setAnimationsRunning(false)
    }

}