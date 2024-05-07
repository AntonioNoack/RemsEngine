package me.anno.engine.ui.render

import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.engine.ui.render.Renderers.boneIndicesRenderer
import me.anno.engine.ui.render.Renderers.boneWeightsRenderer
import me.anno.engine.ui.render.Renderers.frontBackRenderer
import me.anno.engine.ui.render.Renderers.isInstancedRenderer
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.engine.ui.render.Renderers.simpleNormalRenderer
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.renderer.Renderer.Companion.randomIdRenderer
import me.anno.gpu.shader.renderer.Renderer.Companion.triangleVisRenderer
import me.anno.gpu.shader.renderer.Renderer.Companion.uvRenderer
import me.anno.graph.render.QuickPipeline
import me.anno.graph.render.effects.AnimeOutlineNode
import me.anno.graph.render.effects.BloomNode
import me.anno.graph.render.effects.ColorBlindnessMode
import me.anno.graph.render.effects.ColorBlindnessNode.Companion.createRenderGraph
import me.anno.graph.render.effects.DepthOfFieldNode
import me.anno.graph.render.effects.DepthTestNode
import me.anno.graph.render.effects.FSR1HelperNode
import me.anno.graph.render.effects.FSR1Node
import me.anno.graph.render.effects.FXAANode
import me.anno.graph.render.effects.GizmoNode
import me.anno.graph.render.effects.HeightExpFogNode
import me.anno.graph.render.effects.MSAAHelperNode
import me.anno.graph.render.effects.MotionBlurNode
import me.anno.graph.render.effects.NightNode
import me.anno.graph.render.effects.OutlineEffectNode
import me.anno.graph.render.effects.OutlineEffectSelectNode
import me.anno.graph.render.effects.SSAONode
import me.anno.graph.render.effects.SSRNode
import me.anno.graph.render.effects.SmoothNormalsNode
import me.anno.graph.render.effects.ToneMappingNode
import me.anno.graph.render.scene.CombineLightsNode
import me.anno.graph.render.scene.DrawSkyMode
import me.anno.graph.render.scene.RenderLightsNode
import me.anno.graph.render.scene.RenderSceneDeferredNode
import me.anno.graph.render.scene.RenderSceneForwardNode
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.utils.Color.withAlpha
import org.joml.Vector4f

/**
 * How a RenderView shall render a scene;
 * Defined by either a Renderer, a render graph, or a Material.
 * */
@Suppress("unused")
class RenderMode(
    val name: String,
    val renderer: Renderer? = null,
    val renderGraph: FlowGraph? = null,
    val superMaterial: Material? = null
) {

    constructor(renderer: Renderer) : this(renderer.name, renderer, null)
    constructor(name: String, renderer: Renderer) : this(name, renderer, null)
    constructor(name: String, renderGraph: FlowGraph?) : this(name, null, renderGraph)
    constructor(name: String, dlt: DeferredLayerType) : this(name, attributeRenderers[dlt])
    constructor(name: String, base: RenderMode) : this(name, base.renderer, base.renderGraph)
    constructor(name: String, material: Material) : this(name, null, DEFAULT.renderGraph, material)

    init {
        values.add(this)
    }

    companion object {

        private val deferredNodeSettings = mapOf(
            "Stage" to PipelineStage.OPAQUE,
            "Skybox Resolution" to 256,
            "Draw Sky" to DrawSkyMode.AFTER_GEOMETRY
        )

        private val decalNodeSettings = mapOf("Stage" to PipelineStage.DECAL)
        private val transparentNodeSettings = mapOf("Stage" to PipelineStage.TRANSPARENT)

        val values = ArrayList<RenderMode>()

        val DEFAULT = RenderMode(
            "Default",
            QuickPipeline()
                .then1(RenderSceneDeferredNode(), deferredNodeSettings)
                .then1(RenderSceneDeferredNode(), decalNodeSettings)
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(RenderSceneForwardNode(), transparentNodeSettings)
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(OutlineEffectSelectNode())
                .then1(OutlineEffectNode(), mapOf("Fill Colors" to listOf(Vector4f()), "Radius" to 1))
                .then(GizmoNode())
                .then(FXAANode())
                .finish()
        )

        val WITHOUT_POST_PROCESSING = RenderMode(
            "Without Post-Processing",
            QuickPipeline()
                .then1(RenderSceneDeferredNode(), deferredNodeSettings)
                .then1(RenderSceneDeferredNode(), decalNodeSettings)
                .then(RenderLightsNode())
                .then1(RenderSceneForwardNode(), transparentNodeSettings)
                .then1(CombineLightsNode(), mapOf("Apply Tone Mapping" to true))
                .finish()
        )

        val MSAA_DEFERRED = RenderMode(
            "MSAA Deferred",
            QuickPipeline()
                .then(MSAAHelperNode())
                .then1(RenderSceneDeferredNode(), deferredNodeSettings)
                .then1(RenderSceneDeferredNode(), decalNodeSettings)
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(RenderSceneForwardNode(), transparentNodeSettings)
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(OutlineEffectSelectNode())
                .then1(OutlineEffectNode(), mapOf("Fill Colors" to listOf(Vector4f()), "Radius" to 1))
                .then(GizmoNode())
                .finish()
        )

        val CLICK_IDS = RenderMode("ClickIds (Random)", randomIdRenderer)

        // todo this mode is broken, no longer showing log2-pattern
        val DEPTH = RenderMode("Depth", attributeRenderers[DeferredLayerType.DEPTH])

        val NO_DEPTH = RenderMode("No Depth", Renderers.pbrRenderer)

        // todo forward deserves bloom
        // todo helmet has much too faint smudges in forward rendering -> why?
        val FORWARD = RenderMode("Forward", Renderers.pbrRenderer)
        val MSAA_FORWARD = RenderMode("MSAA Forward", Renderers.pbrRenderer)

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
                .then(RenderSceneDeferredNode())
                .then(RenderLightsNode(), mapOf("Light" to listOf("Illuminated")))
                .then1(ToneMappingNode(), mapOf("Exposure" to 0x22 / 255f))
                .then(GizmoNode())
                .finish()
        )

        val LIGHT_SUM_MSAA = RenderMode(
            "Light Sum MSAAx8",
            QuickPipeline()
                .then(MSAAHelperNode())
                .then1(RenderSceneDeferredNode(), mapOf("Samples" to 8))
                .then(RenderLightsNode(), mapOf("Samples" to 8), mapOf("Light" to listOf("Illuminated")))
                .then1(ToneMappingNode(), mapOf("Exposure" to 0x22 / 255f))
                .then(GizmoNode())
                .finish()
        )

        val LIGHT_COUNT = RenderMode("Light Count")

        val SSAO = RenderMode(
            "SSAO",
            QuickPipeline()
                .then(RenderSceneDeferredNode())
                .then(SSAONode(), mapOf("Ambient Occlusion" to listOf("Illuminated")))
                .then(GizmoNode())
                .finish()
        )

        val SSAO_MS = RenderMode(
            "SSAO MSAAx8",
            QuickPipeline()
                .then1(RenderSceneDeferredNode(), mapOf("Samples" to 8))
                .then(SSAONode(), mapOf("Ambient Occlusion" to listOf("Illuminated")))
                .then(GizmoNode())
                .finish()
        )

        val SS_REFLECTIONS = RenderMode(
            "SS-Reflections",
            QuickPipeline()
                .then(RenderSceneDeferredNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then1(CombineLightsNode(), mapOf("Ambient Occlusion" to 1f))
                .then(SSRNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode())
                .finish()
        )

        val INVERSE_DEPTH = RenderMode("Inverse Depth", Renderers.pbrRenderer)
        val OVERDRAW = RenderMode("Overdraw", Renderers.overdrawRenderer)

        val WITH_DEPTH_PREPASS = RenderMode(
            "With Depth-Prepass",
            QuickPipeline()
                // todo this no longer works...
                /**
                 * prepass for depth only: depth is the only value for RenderSceneNode,
                 * which is accepted as an input too, and such, it will render first only the depth
                 * (optimization to only render what's needed),
                 * and then all other attributes;
                 * */
                .then1(RenderSceneDeferredNode(), mapOf("Skybox Resolution" to 0))
                /**
                 * actual scene rendering
                 * */
                .then(RenderSceneDeferredNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode())
                .finish()
        )

        val MONO_WORLD_SCALE = RenderMode("Mono World-Scale", DEFAULT)
        val GHOSTING_DEBUG = RenderMode("Ghosting Debug", Renderers.pbrRenderer)

        val FSR_SQRT2 = RenderMode("FSRx1.41", FSR1Node.createPipeline(0.707f))
        val FSR_X2 = RenderMode("FSRx2", FSR1Node.createPipeline(0.5f))
        val FSR_X4 = RenderMode("FSRx4", FSR1Node.createPipeline(0.25f))

        val FSR_MSAA_X4 = RenderMode(
            "FSR+MSAAx4", QuickPipeline()
                .then(MSAAHelperNode())
                .then1(FSR1HelperNode(), mapOf("Fraction" to 0.25f))
                .then1(RenderSceneDeferredNode(), deferredNodeSettings)
                .then1(RenderSceneDeferredNode(), decalNodeSettings)
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(RenderSceneForwardNode(), transparentNodeSettings)
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode()) // gizmo node depends on 1:1 depth scale, so we cannot do FSR before it
                .then(FSR1Node())
                .finish()
        )

        // todo make these modes use a render graph, too
        val FSR2_X2 = RenderMode("FSR2x2")
        val FSR2_X8 = RenderMode("FSR2x8")

        val NEAREST_X4 = RenderMode(
            "Nearest 4x",
            QuickPipeline()
                .then1(FSR1HelperNode(), mapOf("Fraction" to 0.25f)) // reduces resolution 4x
                .then1(RenderSceneDeferredNode(), deferredNodeSettings)
                .then1(RenderSceneDeferredNode(), decalNodeSettings)
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(RenderSceneForwardNode(), transparentNodeSettings)
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then1(GizmoNode(), mapOf("Samples" to 8))
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
                .then1(RenderSceneDeferredNode(), deferredNodeSettings)
                .then1(RenderSceneDeferredNode(), decalNodeSettings)
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(RenderSceneForwardNode(), transparentNodeSettings)
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(OutlineEffectSelectNode())
                .then1(OutlineEffectNode(), mapOf("Fill Colors" to listOf(Vector4f()), "Radius" to 1))
                .then1(GizmoNode(), mapOf("AABBs" to true))
                .then(FXAANode())
                .finish()
        )
        val PHYSICS = RenderMode("Physics", DEFAULT)

        val POST_OUTLINE = RenderMode(
            "Post-Outline",
            QuickPipeline()
                .then1(RenderSceneDeferredNode(), deferredNodeSettings)
                .then1(RenderSceneDeferredNode(), decalNodeSettings)
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(RenderSceneForwardNode(), transparentNodeSettings)
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(OutlineEffectSelectNode())
                .then(OutlineEffectNode())
                .then(FXAANode())
                .then(GizmoNode())
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
                .then1(RenderSceneDeferredNode(), deferredNodeSettings)
                .then1(RenderSceneDeferredNode(), decalNodeSettings)
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(RenderSceneForwardNode(), transparentNodeSettings)
                .then(DepthOfFieldNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode())
                .finish()
        )

        val MOTION_BLUR = RenderMode(
            "Motion Blur",
            QuickPipeline()
                .then1(RenderSceneDeferredNode(), deferredNodeSettings)
                .then1(RenderSceneDeferredNode(), decalNodeSettings)
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(RenderSceneForwardNode(), transparentNodeSettings)
                .then(MotionBlurNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode())
                .finish()
        )

        val SMOOTH_NORMALS = RenderMode(
            "Smooth Normals",
            QuickPipeline()
                .then1(RenderSceneDeferredNode(), deferredNodeSettings)
                .then1(RenderSceneDeferredNode(), decalNodeSettings)
                .then(SmoothNormalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(RenderSceneForwardNode(), transparentNodeSettings)
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode())
                .finish()
        )

        val DEPTH_TEST = RenderMode(
            "Depth Test",
            QuickPipeline()
                .then1(RenderSceneDeferredNode(), deferredNodeSettings)
                .then1(RenderSceneDeferredNode(), decalNodeSettings)
                .then(DepthTestNode())
                .then(GizmoNode())
                .finish()
        )

        fun postProcessGraph(node: ActionNode): FlowGraph {
            return QuickPipeline()
                .then1(RenderSceneDeferredNode(), deferredNodeSettings)
                .then1(RenderSceneDeferredNode(), decalNodeSettings)
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then1(RenderSceneForwardNode(), transparentNodeSettings)
                .then(node)
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode())
                .finish()
        }

        val FOG_TEST = RenderMode("Fog Test", postProcessGraph(HeightExpFogNode()))
        val NIGHT_TEST = RenderMode("Night Test", postProcessGraph(NightNode()))
        val ANIME_OUTLINES = RenderMode("Anime Outlines", postProcessGraph(AnimeOutlineNode()))

        val ALL_GLASS = RenderMode("All Glass", Material().apply {
            pipelineStage = PipelineStage.TRANSPARENT
            metallicMinMax.set(1f)
            roughnessMinMax.set(0f)
            enableVertexColors = false
        })

        val ALL_SILVER = RenderMode("All Silver", Material().apply {
            metallicMinMax.set(1f)
            roughnessMinMax.set(0f)
            diffuseBase.set(0.9f, 0.9f, 0.9f, 1f)
            enableVertexColors = false
        })

        val ALL_STEEL = RenderMode("All Steel", Material().apply {
            metallicMinMax.set(1f)
            roughnessMinMax.set(0.2f)
            diffuseBase.set(0.3f, 0.3f, 0.3f, 1f)
            enableVertexColors = false
        })

        val ALL_GOLDEN = RenderMode("All Golden", Material.diffuse(0xf5ba6c.withAlpha(255)).apply {
            roughnessMinMax.set(0.2f)
            metallicMinMax.set(1f)
            enableVertexColors = false
        })

        val ALL_WHITE = RenderMode("All White", Material().apply {
            enableVertexColors = false
        })

        val IS_INSTANCED = RenderMode("Is Instanced", isInstancedRenderer)

        val BONE_INDICES = RenderMode("Bone Indices", boneIndicesRenderer)
        val BONE_WEIGHTS = RenderMode("Bone Weights", boneWeightsRenderer)
    }
}