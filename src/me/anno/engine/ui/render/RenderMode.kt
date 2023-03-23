package me.anno.engine.ui.render

import me.anno.ecs.components.camera.effects.CameraEffect
import me.anno.ecs.components.camera.effects.ColorBlindnessEffect
import me.anno.ecs.components.camera.effects.DepthOfFieldEffect
import me.anno.ecs.components.camera.effects.OutlineEffect
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.engine.ui.render.Renderers.simpleNormalRenderer
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.shader.RandomEffect
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Renderer.Companion.uvRenderer

// @Deprecated("Shall be replaced by RenderGraph")
// todo all-metallic / all-rough/smooth render modes?
@Suppress("unused")
class RenderMode(
    val name: String,
    val dlt: DeferredLayerType? = null,
    val effect: CameraEffect? = null,
    val renderer: Renderer? = null
) {

    constructor(name: String, effect: CameraEffect) : this(name, null, effect)
    constructor(name: String, renderer: Renderer) : this(name, null, null, renderer)
    constructor(renderer: Renderer) : this(renderer.name, null, null, renderer)
    constructor(dlt: DeferredLayerType) : this(dlt.name, dlt)

    init {
        values.add(this)
    }

    companion object {

        val values = ArrayList<RenderMode>()

        val DEFAULT = RenderMode("Default")
        val WITHOUT_POST_PROCESSING = RenderMode("Without Post-Processing")
        val CLICK_IDS = RenderMode("ClickIds", RandomEffect)
        val DEPTH = RenderMode("Depth")
        val NO_DEPTH = RenderMode("No Depth")
        val FORCE_DEFERRED = RenderMode("Force Deferred")
        val FORCE_NON_DEFERRED = RenderMode("Force Non-Deferred")
        val ALL_DEFERRED_LAYERS = RenderMode("All Deferred Layers")
        val ALL_DEFERRED_BUFFERS = RenderMode("All Deferred Buffers")

        val COLOR = RenderMode("Color", DeferredLayerType.COLOR)
        val NORMAL = RenderMode("Normal", DeferredLayerType.NORMAL)

        val UV = RenderMode("UVs", uvRenderer)
        // mode to show bone weights? ask for it, if you're interested :)

        val TANGENT = RenderMode("Tangent", DeferredLayerType.TANGENT)
        val BITANGENT = RenderMode("Bitangent", DeferredLayerType.BITANGENT)
        val EMISSIVE = RenderMode("Emissive", DeferredLayerType.EMISSIVE)
        val ROUGHNESS = RenderMode("Roughness", DeferredLayerType.ROUGHNESS)
        val METALLIC = RenderMode("Metallic", DeferredLayerType.METALLIC)
        val POSITION = RenderMode("Position", DeferredLayerType.POSITION)
        val TRANSLUCENCY = RenderMode("Translucency", DeferredLayerType.TRANSLUCENCY)
        val OCCLUSION = RenderMode("Baked Occlusion", DeferredLayerType.OCCLUSION)
        val SHEEN = RenderMode("Sheen", DeferredLayerType.SHEEN)
        val ANISOTROPY = RenderMode("Anisotropy", DeferredLayerType.ANISOTROPIC)
        val MOTION_VECTORS = RenderMode("Motion Vectors", DeferredLayerType.MOTION)

        val PREVIEW = RenderMode("Preview", previewRenderer)
        val SIMPLE = RenderMode("Simple", simpleNormalRenderer)

        // ALPHA, // currently not defined
        val LIGHT_SUM = RenderMode("Light Sum") // todo implement dust-light-spilling for impressive fog
        val LIGHT_COUNT = RenderMode("Light Count")

        val SSAO = RenderMode("SSAO")
        val SS_REFLECTIONS = RenderMode("SS-Reflections")

        val INVERSE_DEPTH = RenderMode("Inverse Depth")
        val OVERDRAW = RenderMode("Overdraw")
        val WITH_DEPTH_PREPASS = RenderMode("With Depth-Prepass")
        val MONO_WORLD_SCALE = RenderMode("Mono World-Scale")
        val GHOSTING_DEBUG = RenderMode("Ghosting Debug")

        val MSAA_X8 = RenderMode("MSAAx8")

        // implement this properly:
        //  - https://www.reddit.com/r/opengl/comments/kvuolj/deferred_rendering_and_msaa/
        //  - https://docs.nvidia.com/gameworks/content/gameworkslibrary/graphicssamples/d3d_samples/antialiaseddeferredrendering.htm
        //  - https://registry.khronos.org/OpenGL-Refpages/gl4/html/texelFetch.xhtml
        val MSAA_DEFERRED = RenderMode("MSAAx8 Deferred")
        val FSR_SQRT2 = RenderMode("FSRx1.41")
        val FSR_X2 = RenderMode("FSRx2")
        val FSR_X4 = RenderMode("FSRx4")
        val FSR_MSAA_X4 = RenderMode("FSR+MSAAx4")
        val FSR2_X2 = RenderMode("FSR2x2")
        val FSR2_X8 = RenderMode("FSR2x8")
        val NEAREST_X4 = RenderMode("Nearest 4x")

        val LINES = RenderMode("Lines")
        val LINES_MSAA = RenderMode("Lines MSAA")
        val FRONT_BACK = RenderMode("Front/Back")

        /** visualize the triangle structure by giving each triangle its own color */
        val SHOW_TRIANGLES = RenderMode("Show Triangles")

        val SHOW_AABB = RenderMode("Show AABBs")
        val PHYSICS = RenderMode("Physics")

        val POST_OUTLINE = RenderMode("Post-Outline", OutlineEffect())

        // color blindness modes
        val GRAYSCALE = RenderMode("Grayscale", ColorBlindnessEffect(ColorBlindnessEffect.Mode.GRAYSCALE))
        val PROTANOPIA = RenderMode("Protanopia", ColorBlindnessEffect(ColorBlindnessEffect.Mode.PROTANOPIA))
        val DEUTERANOPIA = RenderMode("Deuteranopia", ColorBlindnessEffect(ColorBlindnessEffect.Mode.DEUTERANOPIA))
        val TRITANOPIA = RenderMode("Tritanopia", ColorBlindnessEffect(ColorBlindnessEffect.Mode.TRITANOPIA))

        val RAY_TEST = RenderMode("Raycast Test")

        val DEPTH_OF_FIELD = RenderMode("Depth Of Field", DepthOfFieldEffect())

    }

}