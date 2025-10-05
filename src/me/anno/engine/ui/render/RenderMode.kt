package me.anno.engine.ui.render

import me.anno.ecs.annotations.ExtendableEnum
import me.anno.graph.visual.render.effects.SnowNode
import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.engine.ui.render.Renderers.boneIndicesRenderer
import me.anno.engine.ui.render.Renderers.boneWeightsRenderer
import me.anno.engine.ui.render.Renderers.diffFromNormalRenderer
import me.anno.engine.ui.render.Renderers.frontBackRenderer
import me.anno.engine.ui.render.Renderers.isIndexedRenderer
import me.anno.engine.ui.render.Renderers.isInstancedRenderer
import me.anno.engine.ui.render.Renderers.normalMapRenderer
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.engine.ui.render.Renderers.simpleRenderer
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.renderer.Renderer.Companion.randomIdRenderer
import me.anno.gpu.shader.renderer.Renderer.Companion.triangleVisRenderer
import me.anno.gpu.shader.renderer.Renderer.Companion.uvRenderer
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.actions.ActionNode
import me.anno.graph.visual.render.QuickPipeline
import me.anno.graph.visual.render.compiler.ShaderExprNode
import me.anno.graph.visual.render.effects.AnimeOutlineNode
import me.anno.graph.visual.render.effects.BloomNode
import me.anno.graph.visual.render.effects.ColorBlindnessMode
import me.anno.graph.visual.render.effects.ColorBlindnessNode.Companion.createRenderGraph
import me.anno.graph.visual.render.effects.DepthOfFieldNode
import me.anno.graph.visual.render.effects.DepthTestNode
import me.anno.graph.visual.render.effects.DepthToNormalNode
import me.anno.graph.visual.render.effects.FSR1HelperNode
import me.anno.graph.visual.render.effects.FSR1Node
import me.anno.graph.visual.render.effects.FSR2Node
import me.anno.graph.visual.render.effects.FXAANode
import me.anno.graph.visual.render.effects.GizmoNode
import me.anno.graph.visual.render.effects.HeightExpFogNode
import me.anno.graph.visual.render.effects.LUTColorMapNode
import me.anno.graph.visual.render.effects.MSAAHelperNode
import me.anno.graph.visual.render.effects.MotionBlurNode
import me.anno.graph.visual.render.effects.NightNode
import me.anno.graph.visual.render.effects.OutlineEffectNode
import me.anno.graph.visual.render.effects.OutlineEffectSelectNode
import me.anno.graph.visual.render.effects.PixelationNode
import me.anno.graph.visual.render.effects.SSAONode
import me.anno.graph.visual.render.effects.SSGINode
import me.anno.graph.visual.render.effects.SSRNode
import me.anno.graph.visual.render.effects.SmoothNormalsNode
import me.anno.graph.visual.render.effects.TAAHelperNode
import me.anno.graph.visual.render.effects.TAANode
import me.anno.graph.visual.render.effects.ToneMappingNode
import me.anno.graph.visual.render.effects.UnditherNode
import me.anno.graph.visual.render.effects.VignetteNode
import me.anno.graph.visual.render.effects.framegen.FrameGenInitNode
import me.anno.graph.visual.render.effects.framegen.FrameGenMixingNode
import me.anno.graph.visual.render.effects.framegen.FrameGenPredictiveNode
import me.anno.graph.visual.render.effects.framegen.FrameGenProjective1Node
import me.anno.graph.visual.render.effects.framegen.FrameGenProjectiveXNode
import me.anno.graph.visual.render.scene.BoxCullingNode
import me.anno.graph.visual.render.scene.CellShadingNode
import me.anno.graph.visual.render.scene.CombineLightsNode
import me.anno.graph.visual.render.scene.DepthPrepassNode
import me.anno.graph.visual.render.scene.DrawSkyMode
import me.anno.graph.visual.render.scene.FillLightBucketsNode
import me.anno.graph.visual.render.scene.RenderDecalsNode
import me.anno.graph.visual.render.scene.RenderDeferredNode
import me.anno.graph.visual.render.scene.RenderForwardNode
import me.anno.graph.visual.render.scene.RenderForwardPlusNode
import me.anno.graph.visual.render.scene.RenderTransparentNode
import me.anno.graph.visual.render.scene.RenderLightsNode
import me.anno.graph.visual.scalar.FloatMathBinary
import me.anno.graph.visual.vector.MathF2XNode
import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc
import me.anno.utils.OS.res
import org.joml.Vector4f

/**
 * How a RenderView shall render a scene;
 * Defined by either a Renderer, a render graph, or a Material.
 * */
@Suppress("unused")
class RenderMode private constructor(
    override val nameDesc: NameDesc,
    val renderer: Renderer? = null,
    val renderGraph: FlowGraph? = null
) : ExtendableEnum {

    constructor(renderer: Renderer) : this(renderer.nameDesc, renderer, null)
    constructor(name: NameDesc, renderer: Renderer) : this(name, renderer, null)
    constructor(name: NameDesc, renderGraph: FlowGraph?) : this(name, null, renderGraph)
    constructor(name: NameDesc, dlt: DeferredLayerType) : this(name, attributeRenderers[dlt])
    constructor(name: NameDesc, base: RenderMode) : this(name, base.renderer, base.renderGraph)
    constructor(name: String, renderer: Renderer) : this(name, renderer, null)
    constructor(name: String, renderGraph: FlowGraph?) : this(name, null, renderGraph)
    constructor(name: String, dlt: DeferredLayerType) : this(name, attributeRenderers[dlt], null)
    constructor(name: String, base: RenderMode) : this(name, base.renderer, base.renderGraph)
    constructor(name: String) : this(NameDesc(name))

    private constructor(name: String, renderer: Renderer?, renderGraph: FlowGraph?) :
            this(NameDesc(name), renderer, renderGraph)

    init {
        Companion.values.add(this)
        renderGraph?.name = nameDesc.name
    }

    override val id: Int = values.indexOf(this)

    override val values: List<ExtendableEnum>
        get() = Companion.values

    var renderLines: Boolean = false

    fun onlyRenderLines(): RenderMode {
        renderLines = true
        return this
    }

    companion object {

        val opaqueNodeSettings = mapOf(
            "Stage" to PipelineStage.OPAQUE,
            "Skybox Resolution" to 256,
            "Draw Sky" to DrawSkyMode.AFTER_GEOMETRY
        )

        fun QuickPipeline.thenAlphaBlendPass(): QuickPipeline {
            return then1(
                RenderTransparentNode(), mapOf(
                    "Stage" to PipelineStage.TRANSPARENT,
                    "Transparent Pass" to 1
                )
            )
        }

        val values = ArrayList<RenderMode>()

        val DEFAULT = RenderMode(
            "Default",
            QuickPipeline()
                .then(BoxCullingNode())
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(RenderTransparentNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .then(OutlineEffectSelectNode())
                .then1(OutlineEffectNode(), mapOf("Fill Colors" to listOf(Vector4f()), "Radius" to 1))
                .then(GizmoNode())
                .then(UnditherNode())
                .then(FXAANode())
                .finish()
        )

        val WITHOUT_POST_PROCESSING = RenderMode(
            "Without Post-Processing",
            QuickPipeline()
                .then(BoxCullingNode())
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(RenderLightsNode())
                .then(RenderTransparentNode()) // todo bug: this is invisible :/
                .then1(CombineLightsNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .finish()
        )

        // todo MSAA DEFERRED and FORWARD are very slow in VR (1 fps), and task manager shows a mysterious
        //  ~8GB (100%) VRAM usage although Rem's Engine reports ~2GB -> is VRAM being put onto system memory?
        val MSAA_DEFERRED = RenderMode(
            "MSAA Deferred",
            QuickPipeline()
                .then(BoxCullingNode())
                .then(MSAAHelperNode())
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(RenderTransparentNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .then(OutlineEffectSelectNode())
                .then1(OutlineEffectNode(), mapOf("Fill Colors" to listOf(Vector4f()), "Radius" to 1))
                .then(GizmoNode())
                .then(FXAANode())
                .finish()
        )

        val CLICK_IDS = RenderMode("ClickIds (Random)", randomIdRenderer)
        val DRAW_CALL_ID = RenderMode("Draw Call ID (Random)", randomIdRenderer)

        val DEPTH = RenderMode("Depth", attributeRenderers[DeferredLayerType.DEPTH])

        val NO_DEPTH = RenderMode("No Depth", Renderers.pbrRenderer)

        fun QuickPipeline.depthToSSAO(): QuickPipeline {
            return this
                .mapOutputs(mapOf("Illuminated" to listOf("A")))
                .then(DepthToNormalNode())
                .then(SSAONode(), mapOf("Inverse" to true), mapOf("Ambient Occlusion" to listOf("B")))
                .then(
                    // multiplying the emissive with ambient occlusion, too, is incorrect,
                    // but it should be generally a nice approximation :)
                    MathF2XNode().setDataType("Vector3f").setEnumType(FloatMathBinary.MUL),
                    mapOf("Result" to listOf("Data"))
                )
                .then(ShaderExprNode(), mapOf("Result" to listOf("Illuminated")))
        }

        private fun defineForwardPipeline(pipeline: QuickPipeline): QuickPipeline {
            return pipeline
                .then(BoxCullingNode())
                .then1(RenderForwardNode(), opaqueNodeSettings)
                .then1(RenderForwardNode(), mapOf("Stage" to PipelineStage.DECAL))
                .then(RenderTransparentNode())
                .depthToSSAO()
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .then(GizmoNode())
        }

        val FORWARD = RenderMode("Forward", defineForwardPipeline(QuickPipeline()).finish())
        val MSAA_FORWARD = RenderMode(
            "MSAA Forward",
            defineForwardPipeline(QuickPipeline().then(MSAAHelperNode())).finish()
        )

        val FORWARD_PLUS = RenderMode(
            "Forward Plus",
            QuickPipeline()
                .then(BoxCullingNode())
                .then(FillLightBucketsNode())
                .then1(RenderForwardPlusNode(), opaqueNodeSettings)
                .then1(RenderForwardPlusNode(), mapOf("Stage" to PipelineStage.DECAL))
                .then(RenderTransparentNode())
                .depthToSSAO()
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .then(GizmoNode())
                .finish()
        )

        val ALL_DEFERRED_LAYERS = RenderMode("All Deferred Layers")
        val ALL_DEFERRED_BUFFERS = RenderMode("All Deferred Buffers")

        val COLOR = RenderMode("Color", DeferredLayerType.COLOR)
        val NORMAL = RenderMode("Normal", DeferredLayerType.NORMAL)
        val ALPHA = RenderMode("Opacity (Alpha)", DeferredLayerType.ALPHA)

        val UV = RenderMode("UVs", uvRenderer)

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
        val REFLECTIVITY = RenderMode("Reflectivity", DeferredLayerType.REFLECTIVITY)

        val PREVIEW = RenderMode("Preview", previewRenderer)
        val SIMPLE = RenderMode("Simple", simpleRenderer)

        // todo implement dust-light-spilling for impressive fog

        val LIGHT_SUM = RenderMode(
            "Light Sum",
            QuickPipeline()
                .then(BoxCullingNode())
                .then(RenderDeferredNode())
                .then(RenderLightsNode(), mapOf("Light" to listOf("Illuminated")))
                .then1(ToneMappingNode(), mapOf("Exposure" to 0x22 / 255f))
                .then(GizmoNode())
                .finish()
        )

        val LIGHT_SUM_MSAA = RenderMode(
            "Light Sum MSAAx8",
            QuickPipeline()
                .then(BoxCullingNode())
                .then(MSAAHelperNode())
                .then1(RenderDeferredNode(), mapOf("Samples" to 8))
                .then(RenderLightsNode(), mapOf("Samples" to 8), mapOf("Light" to listOf("Illuminated")))
                .then1(ToneMappingNode(), mapOf("Exposure" to 0x22 / 255f))
                .then(GizmoNode())
                .finish()
        )

        val LIGHT_COUNT = RenderMode("Light Count")

        val SSAO = RenderMode(
            "SSAO",
            QuickPipeline()
                .then(BoxCullingNode())
                .then(RenderDeferredNode())
                .then(SSAONode(), mapOf("Ambient Occlusion" to listOf("Illuminated")))
                .then(GizmoNode())
                .finish()
        )

        val SSAO_MS = RenderMode(
            "SSAO MSAAx8",
            QuickPipeline()
                .then(BoxCullingNode())
                .then1(RenderDeferredNode(), mapOf("Samples" to 8))
                .then(SSAONode(), mapOf("Ambient Occlusion" to listOf("Illuminated")))
                .then(GizmoNode())
                .finish()
        )

        val SS_REFLECTIONS = RenderMode(
            "SS-Reflections",
            QuickPipeline()
                .then(BoxCullingNode())
                .then(RenderDeferredNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then1(CombineLightsNode(), mapOf("Ambient Occlusion" to 1f))
                .then(SSRNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(GizmoNode())
                .finish()
        )

        // todo SSGI is broken :(
        val SSGI = RenderMode(
            "SSGI",
            QuickPipeline()
                .then(BoxCullingNode())
                .then1(
                    RenderDeferredNode(), mapOf(
                        "Stage" to PipelineStage.OPAQUE,
                        "Skybox Resolution" to 256,
                        "Draw Sky" to DrawSkyMode.AFTER_GEOMETRY
                    )
                )
                .then1(RenderDeferredNode(), mapOf("Stage" to PipelineStage.DECAL))
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSGINode())
                .then(SSRNode())
                .then(RenderTransparentNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .then(OutlineEffectSelectNode())
                .then1(OutlineEffectNode(), mapOf("Fill Colors" to listOf(Vector4f()), "Radius" to 1))
                .then(GizmoNode())
                .then(FXAANode())
                .finish()
        )

        val INVERSE_DEPTH = RenderMode("Inverse Depth", Renderers.pbrRenderer)
        val OVERDRAW = RenderMode("Overdraw", Renderers.overdrawRenderer)
        val TRIANGLE_SIZE = RenderMode("Triangle Size", Renderers.triangleSizeRenderer)

        val WITH_DEPTH_PREPASS = RenderMode(
            "With Depth-Prepass",
            QuickPipeline()
                .then(BoxCullingNode())
                /**
                 * prepass for depth only: depth is the only value for RenderSceneNode,
                 * which is accepted as an input too, and such, it will render first only the depth
                 * (optimization to only render what's needed),
                 * and then all other attributes;
                 * */
                .then(DepthPrepassNode())
                /** actual scene rendering */
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(RenderTransparentNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .then(OutlineEffectSelectNode())
                .then1(OutlineEffectNode(), mapOf("Fill Colors" to listOf(Vector4f()), "Radius" to 1))
                .then(GizmoNode())
                .then(UnditherNode())
                .then(FXAANode())
                .finish()
        )

        val GHOSTING_DEBUG = RenderMode("Ghosting Debug", Renderers.pbrRenderer)

        val FSR_SQRT2 = RenderMode("FSRx1.41", FSR1Node.createPipeline(0.707f))
        val FSR_X2 = RenderMode("FSRx2", FSR1Node.createPipeline(0.5f))
        val FSR_X4 = RenderMode("FSRx4", FSR1Node.createPipeline(0.25f))

        val FSR_MSAA_X4 = RenderMode(
            "FSR+MSAAx4", QuickPipeline()
                .then(BoxCullingNode())
                .then(MSAAHelperNode())
                .then1(FSR1HelperNode(), mapOf("Fraction" to 0.25f))
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(RenderTransparentNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .then(GizmoNode()) // gizmo node depends on 1:1 depth scale, so we cannot do FSR before it
                .then(FSR1Node())
                .finish()
        )

        val FSR2_X2 = RenderMode("FSR2x2", FSR2Node.createPipeline(1f / 2f))
        val FSR2_X4 = RenderMode("FSR2x4", FSR2Node.createPipeline(1f / 4f))
        val FSR2_X8 = RenderMode("FSR2x8", FSR2Node.createPipeline(1f / 8f))

        val FSR3_MIXING = RenderMode(
            "FrameGen-Mixing",
            FrameGenInitNode.createPipeline(FrameGenMixingNode())
        )
        val FSR3_PREDICTIVE = RenderMode(
            "FrameGen-Predictive",
            FrameGenInitNode.createPipeline(FrameGenPredictiveNode())
        )
        val FSR3_PROJECTIVE_X = RenderMode(
            "FrameGen-ProjectiveX",
            FrameGenInitNode.createPipeline(FrameGenProjectiveXNode())
        )
        val FSR3_PROJECTIVE_1 = RenderMode(
            "FrameGen-Projective1",
            FrameGenInitNode.createPipeline(FrameGenProjective1Node())
        )

        val NEAREST_X4 = RenderMode(
            "Nearest 4x",
            QuickPipeline()
                .then(BoxCullingNode())
                .then1(FSR1HelperNode(), mapOf("Fraction" to 0.25f)) // reduces resolution 4x
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(RenderTransparentNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .then1(GizmoNode(), mapOf("Samples" to 8))
                .then(FXAANode())
                .finish()
        )

        val LINES = RenderMode("Lines", FORWARD.renderGraph)
            .onlyRenderLines()
        val LINES_MSAA = RenderMode("Lines MSAA", MSAA_FORWARD.renderGraph)
            .onlyRenderLines()
        val FRONT_BACK = RenderMode("Front/Back", frontBackRenderer)

        /** visualize the triangle structure by giving each triangle its own color */
        val SHOW_TRIANGLES = RenderMode("Show Triangles", triangleVisRenderer)

        val SHOW_AABB = RenderMode(
            "Show AABBs",
            QuickPipeline()
                .then(BoxCullingNode())
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(RenderTransparentNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .then(OutlineEffectSelectNode())
                .then1(OutlineEffectNode(), mapOf("Fill Colors" to listOf(Vector4f()), "Radius" to 1))
                .then1(GizmoNode(), mapOf("AABBs" to true))
                .then(FXAANode())
                .finish()
        )

        val POST_OUTLINE = RenderMode(
            "Post-Outline",
            QuickPipeline()
                .then(BoxCullingNode())
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(RenderTransparentNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
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

        val TAA = RenderMode(
            "TAA",
            QuickPipeline()
                .then(BoxCullingNode())
                .then(TAAHelperNode())
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(RenderTransparentNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .then1(GizmoNode(), mapOf("Samples" to 1))
                .then(TAANode())
                .finish()
        )

        val LINES_TAA = RenderMode("Lines TAA", TAA.renderGraph)
            .onlyRenderLines()

        val DEPTH_OF_FIELD = RenderMode(
            "Depth Of Field",
            QuickPipeline()
                .then(BoxCullingNode())
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(RenderTransparentNode())
                .then(DepthOfFieldNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .then(GizmoNode())
                .finish()
        )

        val MOTION_BLUR = RenderMode(
            "Motion Blur",
            QuickPipeline()
                .then(BoxCullingNode())
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(RenderTransparentNode())
                .then(MotionBlurNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .then(GizmoNode())
                .finish()
        )

        val SMOOTH_NORMALS = RenderMode(
            "Smooth Normals",
            QuickPipeline()
                .then(BoxCullingNode())
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(SmoothNormalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(RenderTransparentNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .then(GizmoNode())
                .finish()
        )

        val DEPTH_TEST = RenderMode(
            "Depth Test",
            QuickPipeline()
                .then(BoxCullingNode())
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(DepthTestNode())
                .then(GizmoNode())
                .finish()
        )

        fun createPostProcessGraphBase(): QuickPipeline {
            return QuickPipeline()
                .then(BoxCullingNode())
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(RenderLightsNode())
                .then(SSAONode())
                .then(CombineLightsNode())
                .then(SSRNode())
                .then(RenderTransparentNode())
        }

        fun createHDRPostProcessGraph(postProcessNode: ActionNode): FlowGraph {
            return createPostProcessGraphBase()
                .then(postProcessNode)
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .thenAlphaBlendPass()
                .then(GizmoNode())
                .finish()
        }

        fun createLUTGraph(source: FileReference): FlowGraph {
            return createPostProcessGraphBase()
                .then(BloomNode())
                .then1(LUTColorMapNode(), mapOf("LUT Source" to source, "Apply Tone Mapping" to true))
                .thenAlphaBlendPass() // mmh ... todo we might need a "make-tone-mappable"-parameter, so it can be tonemapped
                .then(GizmoNode())
                .finish()
        }

        val FOG_TEST = RenderMode("Fog Test", createHDRPostProcessGraph(HeightExpFogNode()))
        val NIGHT_TEST = RenderMode("Night Test", createHDRPostProcessGraph(NightNode()))
        val ANIME_OUTLINES = RenderMode("Anime Outlines", createHDRPostProcessGraph(AnimeOutlineNode()))

        val CELL_SHADING = RenderMode(
            "Cell Shading",
            QuickPipeline()
                .then(BoxCullingNode())
                .then1(RenderDeferredNode(), opaqueNodeSettings)
                .then(RenderDecalsNode())
                .then(RenderLightsNode())
                .then(CellShadingNode())
                .then(SSRNode())
                .then(RenderTransparentNode())
                .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
                .then(AnimeOutlineNode())
                .thenAlphaBlendPass() // cannot handle outlines for transparent things properly anyway -> after anime outlines
                .then(OutlineEffectSelectNode())
                .then1(OutlineEffectNode(), mapOf("Fill Colors" to listOf(Vector4f()), "Radius" to 1))
                .then(GizmoNode())
                .then(UnditherNode())
                .then(FXAANode())
                .finish()
        )

        val VIGNETTE = RenderMode("Vignette", createHDRPostProcessGraph(VignetteNode()))
        val PIXELATION = RenderMode("Pixelation", createHDRPostProcessGraph(PixelationNode()))
        val SNOW = RenderMode("Snow", createHDRPostProcessGraph(SnowNode()))

        val COLD_LUT = RenderMode("Cold LUT", createLUTGraph(res.getChild("textures/lut/coldLUT.png")))
        val SEPIA_LUT = RenderMode("Sepia LUT", createLUTGraph(res.getChild("textures/lut/sepiaLUT.png")))

        val IS_INSTANCED = RenderMode("Is Instanced", isInstancedRenderer)
        val IS_INDEXED = RenderMode("Is Indexed", isIndexedRenderer)

        val BONE_INDICES = RenderMode("Bone Indices", boneIndicesRenderer)
        val BONE_WEIGHTS = RenderMode("Bone Weights", boneWeightsRenderer)

        val DIFF_FROM_NORMAL = RenderMode("Diff From Normal", diffFromNormalRenderer)
        val NORMAL_MAP = RenderMode("Normal Map", normalMapRenderer)
    }
}