package me.anno.engine.ui.render

import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.engine.ui.render.Renderers.boneIndicesRenderer
import me.anno.engine.ui.render.Renderers.boneWeightsRenderer
import me.anno.engine.ui.render.Renderers.frontBackRenderer
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.engine.ui.render.Renderers.simpleNormalRenderer
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.renderer.Renderer.Companion.randomIdRenderer
import me.anno.gpu.shader.renderer.Renderer.Companion.triangleVisRenderer
import me.anno.gpu.shader.renderer.Renderer.Companion.uvRenderer
import me.anno.graph.render.QuickPipeline
import me.anno.graph.render.effects.*
import me.anno.graph.render.effects.ColorBlindnessNode.Companion.createRenderGraph
import me.anno.graph.render.scene.CombineLightsNode
import me.anno.graph.render.scene.RenderLightsNode
import me.anno.graph.render.scene.RenderSceneNode
import me.anno.graph.types.FlowGraph
import org.joml.Vector4f

// todo remove all specific implementations in RenderView with RenderGraphs, as far as possible
// todo all-metallic / all-rough/smooth render modes
@Suppress("unused")
class RenderMode(
    val name: String,
    val renderer: Renderer? = null,
    val renderGraph: FlowGraph? = null,
) {

    constructor(renderer: Renderer) : this(renderer.name, renderer, null)
    constructor(name: String, renderer: Renderer) : this(name, renderer, null)
    constructor(name: String, renderGraph: FlowGraph?) : this(name, null, renderGraph)
    constructor(name: String, dlt: DeferredLayerType) : this(name, attributeRenderers[dlt])
    constructor(name: String, base: RenderMode) : this(name, base.renderer, base.renderGraph)

    init {
        values.add(this)
    }

    companion object {

        val values = ArrayList<RenderMode>()

        val DEFAULT = RenderMode(
            "Default",
            QuickPipeline()
                .then(RenderSceneNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(OutlineEffectSelectNode())
                .then1(OutlineEffectNode(), mapOf("Fill Colors" to listOf(Vector4f()), "Radius" to 1))
                .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val WITHOUT_POST_PROCESSING = RenderMode(
            "Without Post-Processing",
            QuickPipeline()
                .then(RenderSceneNode())
                .then(RenderLightsNode())
                .then(CombineLightsNode(), mapOf("Apply Tone Mapping" to true), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val MSAA_DEFERRED = RenderMode(
            "MSAA Deferred",
            QuickPipeline()
                .then1(RenderSceneNode(), mapOf("Samples" to 8))
                .then1(RenderLightsNode(), mapOf("Samples" to 8))
                .then(SSAONode())
                .then1(CombineLightsNode(), mapOf("Samples" to 8))
                .then(SSRNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val CLICK_IDS = RenderMode("ClickIds (Random)", randomIdRenderer)

        val DEPTH = RenderMode("Depth", attributeRenderers[DeferredLayerType.DEPTH])

        val NO_DEPTH = RenderMode("No Depth", Renderers.pbrRenderer)

        val NON_DEFERRED = RenderMode("Non-Deferred", Renderers.pbrRenderer)
        val MSAA_NON_DEFERRED = RenderMode("MSAA Non-Deferred", Renderers.pbrRenderer)

        val ALL_DEFERRED_LAYERS = RenderMode("All Deferred Layers")
        val ALL_DEFERRED_BUFFERS = RenderMode("All Deferred Buffers")

        val COLOR = RenderMode("Color", DeferredLayerType.COLOR)
        val NORMAL = RenderMode("Normal", DeferredLayerType.NORMAL)
        val ALPHA = RenderMode("Opacity (Alpha)", DeferredLayerType.ALPHA)

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

        // todo implement dust-light-spilling for impressive fog

        val LIGHT_SUM = RenderMode(
            "Light Sum",
            QuickPipeline()
                .then(RenderSceneNode())
                .then(RenderLightsNode(), mapOf("Light" to listOf("Illuminated")))
                .then(ToneMappingNode())
                .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val LIGHT_SUM_MSAA = RenderMode(
            "Light Sum MSAAx8",
            QuickPipeline()
                .then1(RenderSceneNode(), mapOf("Samples" to 8))
                .then(RenderLightsNode(), mapOf("Samples" to 8), mapOf("Light" to listOf("Illuminated")))
                .then(ToneMappingNode())
                .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val LIGHT_COUNT = RenderMode("Light Count")

        val SSAO = RenderMode(
            "SSAO",
            QuickPipeline()
                .then(RenderSceneNode())
                .then(SSAONode(), mapOf("Ambient Occlusion" to listOf("Illuminated")))
                .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val SSAO_MS = RenderMode(
            "SSAO MSAAx8",
            QuickPipeline()
                .then1(RenderSceneNode(), mapOf("Samples" to 8))
                .then(SSAONode(), mapOf("Ambient Occlusion" to listOf("Illuminated")))
                .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val SS_REFLECTIONS = RenderMode(
            "SS-Reflections",
            QuickPipeline()
                .then(RenderSceneNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then1(CombineLightsNode(), mapOf("Ambient Occlusion" to 1f))
                .then(SSRNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val INVERSE_DEPTH = RenderMode("Inverse Depth", Renderers.pbrRenderer)
        val OVERDRAW = RenderMode("Overdraw", Renderers.overdrawRenderer)

        val WITH_DEPTH_PREPASS = RenderMode(
            "With Depth-Prepass",
            QuickPipeline()
                /**
                 * prepass for depth only: depth is the only value for RenderSceneNode,
                 * which is accepted as an input too, and such, it will render first only the depth
                 * (optimization to only render what's needed),
                 * and then all other attributes;
                 * */
                .then1(RenderSceneNode(), mapOf("Skybox Resolution" to 0))
                /**
                 * actual scene rendering
                 * */
                .then(RenderSceneNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val MONO_WORLD_SCALE = RenderMode("Mono World-Scale", DEFAULT)
        val GHOSTING_DEBUG = RenderMode("Ghosting Debug", Renderers.pbrRenderer)

        val FSR_SQRT2 = RenderMode("FSRx1.41", FSR1Node.createPipeline(0.707f))
        val FSR_X2 = RenderMode("FSRx2", FSR1Node.createPipeline(0.5f))
        val FSR_X4 = RenderMode("FSRx4", FSR1Node.createPipeline(0.25f))

        val FSR_MSAA_X4 = RenderMode(
            "FSR+MSAAx4", QuickPipeline()
                .then1(FSR1HelperNode(), mapOf("Fraction" to 0.25f))
                .then1(RenderSceneNode(), mapOf("Samples" to 8))
                .then1(RenderLightsNode(), mapOf("Samples" to 8))
                .then(SSAONode())
                .then1(CombineLightsNode(), mapOf("Samples" to 8))
                .then(SSRNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode()) // gizmo node depends on 1:1 depth scale, so we cannot do FSR before it
                .then(FSR1Node(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        // todo make these modes use a render graph, too
        val FSR2_X2 = RenderMode("FSR2x2")
        val FSR2_X8 = RenderMode("FSR2x8")

        val NEAREST_X4 = RenderMode(
            "Nearest 4x",
            QuickPipeline()
                .then1(FSR1HelperNode(), mapOf("Fraction" to 0.25f)) // reduces resolution 4x
                .then(RenderSceneNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode(), mapOf("Samples" to 8), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val LINES = RenderMode("Lines", DEFAULT)
        val LINES_MSAA = RenderMode("Lines MSAA", MSAA_DEFERRED)
        val FRONT_BACK = RenderMode("Front/Back", frontBackRenderer)

        /** visualize the triangle structure by giving each triangle its own color */
        val SHOW_TRIANGLES = RenderMode("Show Triangles", triangleVisRenderer)

        val SHOW_AABB = RenderMode(
            "Show AABBs",
            QuickPipeline()
                .then(RenderSceneNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode(), mapOf("AABBs" to true), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val PHYSICS = RenderMode("Physics", DEFAULT)

        val POST_OUTLINE = RenderMode(
            "Post-Outline",
            QuickPipeline()
                .then(RenderSceneNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(OutlineEffectSelectNode())
                .then(OutlineEffectNode())
                .then(FXAANode())
                .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        // color blindness modes
        val GRAYSCALE = RenderMode("Grayscale", createRenderGraph(ColorBlindnessMode.GRAYSCALE))
        val PROTANOPIA = RenderMode("Protanopia", createRenderGraph(ColorBlindnessMode.PROTANOPIA))
        val DEUTERANOPIA = RenderMode("Deuteranopia", createRenderGraph(ColorBlindnessMode.DEUTERANOPIA))
        val TRITANOPIA = RenderMode("Tritanopia", createRenderGraph(ColorBlindnessMode.TRITANOPIA))

        val RAY_TEST = RenderMode("Raycast Test", DEFAULT)

        val DEPTH_OF_FIELD = RenderMode(
            "Depth Of Field",
            QuickPipeline()
                .then(RenderSceneNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(DepthOfFieldNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val MOTION_BLUR = RenderMode(
            "Motion Blur",
            QuickPipeline()
                .then(RenderSceneNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(MotionBlurNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val SMOOTH_NORMALS = RenderMode(
            "Smooth Normals",
            QuickPipeline()
                .then(RenderSceneNode())
                .then(SmoothNormalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(MotionBlurNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val DEPTH_TEST = RenderMode(
            "Depth Test",
            QuickPipeline()
                .then(RenderSceneNode())
                .then(DepthTestNode())
                .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val FOG_TEST = RenderMode(
            "Fog Test",
            QuickPipeline()
                .then(RenderSceneNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(HeightExpFogNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
                .finish()
        )

        val BONE_INDICES = RenderMode("Bone Indices", boneIndicesRenderer)
        val BONE_WEIGHTS = RenderMode("Bone Weights", boneWeightsRenderer)
    }
}