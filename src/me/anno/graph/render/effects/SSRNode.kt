package me.anno.graph.render.effects

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.render.Texture
import me.anno.graph.render.scene.RenderSceneNode0

class SSRNode : RenderSceneNode0(
    "Screen Space Reflections",
    listOf(
        // todo add width and height?
        "Int", "Samples",
        "Float", "Radius",
        "Float", "Strength",
        "Bool", "Blur",
        "Texture", "Normal", // optional
        "Texture", "Depth",
    ), listOf("Texture", "Ambient Occlusion")
) {

    // todo "depth texture to normal"-node

    init {
        setInput(1, 64) // samples
        setInput(2, 2f) // radius
        setInput(3, 1f) // strength
        setInput(4, true) // blur
        setInput(5, null) // normals
        setInput(6, null) // depth
    }

    override fun invalidate() {
        framebuffer?.destroy()
    }

    override fun executeAction() {

        val samples = getInput(1) as Int
        if (samples < 1) return

        val radius = getInput(2) as Float
        val strength = getInput(3) as Float
        val blur = getInput(4) == true

        val normal = getInput(5) as? Texture
        val normalZW = normal?.mapping == "zw"
        val normalT = ((normal)?.tex as? Texture2D) ?: whiteTexture
        val depthT = ((getInput(6) as? Texture)?.tex as? Texture2D) ?: return

        val transform = RenderState.cameraMatrix

        // todo get all inputs
        //  depth: ITexture2D,
        //        normal: ITexture2D,
        //        normalZW: Boolean,
        //        color: ITexture2D,
        //        emissive: ITexture2D,
        //        metallic: ITexture2D,
        //        metallicMask: String,
        //        roughness: ITexture2D,
        //        roughnessMask: String,
        //        illuminated: ITexture2D,
        //        transform: Matrix4f,
        //        skyBox: SkyBox?,
        //        skyCubeMap: CubemapTexture?,
        //        skyColor: Vector4f,
        //        strength: Float = 1f,
        //        maskSharpness: Float = 1f,
        //        wallThickness: Float = 0.2f,
        //        fineSteps: Int = 10, // 10 are enough, if there are only rough surfaces
        //        maxDistance: Float = 8f,
        //        applyToneMapping: Boolean,
        //        dst: IFramebuffer = FBStack["ss-reflections", depth.w, depth.h, 4, true, 1, false]

        //val result = ScreenSpaceReflections.compute()
        //setOutput(Texture(result as Texture2D), 1)

    }
}