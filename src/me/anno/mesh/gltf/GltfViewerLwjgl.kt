package me.anno.mesh.gltf

import de.javagl.jgltf.model.GltfModel
import de.javagl.jgltf.viewer.AbstractGltfViewer
import de.javagl.jgltf.viewer.GlContext
import de.javagl.jgltf.viewer.lwjgl.GlContextLwjgl
import me.anno.gpu.GFX

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
        return GFX.windowWidth
    }

    override fun getHeight(): Int {
        return GFX.windowHeight
    }

    override fun triggerRendering() {}

    override fun prepareRender() {}

    public override fun render() {
        renderGltfModels()
    }

    fun glRender() {
        doRender()
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