package me.anno.graph.visual.render.compiler

import me.anno.Time
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.components.mesh.material.utils.TypeValueV2
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.ReturnNode
import me.anno.graph.visual.actions.ActionNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeConnector
import me.anno.graph.visual.node.NodeInput
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.MaterialGraph.convert
import me.anno.graph.visual.render.MaterialGraph.floatVecTypes
import me.anno.graph.visual.render.MaterialGraph.kotlinToGLSL
import me.anno.graph.visual.render.MaterialReturnNode
import me.anno.graph.visual.render.MovieNode
import me.anno.graph.visual.render.Texture
import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.arrays.BooleanArrayList
import me.anno.utils.types.AnyToFloat
import me.anno.utils.types.AnyToLong
import me.anno.video.VideoCache
import me.anno.video.formats.gpu.GPUFrame
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.max

abstract class GraphCompiler(val g: FlowGraph) {
    companion object {
        private val LOGGER = LogManager.getLogger(GraphCompiler::class)
    }

    abstract val currentShader: Shader

    val builder = StringBuilder()
    val recursionCheck = HashSet<Node>(g.nodes.size)

    val typeValues = HashMap<String, TypeValue>()

    val prefix = "tmp_"
    val conDefines = HashMap<NodeOutput, String>()

    var loopIndexCtr = 0

    val localVars = HashMap<String, Pair<String, String>>() // name -> newName,type
    fun getLocalVarName(name: String, type: String?): String {
        return localVars.getOrPut("$name/$type") {
            Pair("l${localVars.size}", type!!)
        }.first
    }

    fun shallExport(l: DeferredLayerType, c: NodeInput): Boolean {
        return l == DeferredLayerType.COLOR || c.others.isNotEmpty() || c.currValue != c.defaultValue
    }

    val extraFunctions = StringBuilder()
    val extraVariables = ArrayList<Variable>()
    val typeToFunc = HashMap<String, String?>() // key -> name
    val movies = HashMap<MovieNode, Pair<String, Boolean>>() // file -> name, linear
    val textures = HashMap<FileReference, Pair<String, Boolean>>() // file -> name, linear
    val textures2 = HashMap<NodeInput, Triple<String, GLSLType, Boolean>>() // file -> name, linear

    init {
        typeToFunc["ONP"] = ""
        typeToFunc["R2D"] = ""
        extraFunctions.append(octNormalPacking)
        extraFunctions.append(rawToDepth)
        extraVariables.addAll(depthVars)
    }

    fun defineFunc(name: String, prefix: String, suffix: String?): String? {
        suffix ?: return null
        extraFunctions.append(prefix).append(' ').append(name).append(suffix).append('\n')
        return name
    }

    fun aType(an: NodeConnector, bn: NodeInput): String {
        if (an.type == "Texture") {
            val tex = bn.getValue() as? Texture ?: return "Vector4f"
            val map = tex.mapping
            val enc = tex.encoding
            val dim = enc?.workDims ?: map.length
            return if (dim == 0) { // ??
                if (tex.tex == whiteTexture) "Float" else "Vector4f"
            } else floatVecTypes[dim - 1]
        } else return an.type
    }

    private fun buildExpression(out: NodeOutput, n: Node) {
        val v = conDefines[out]
        if (v != null) {
            builder.append(v)
        } else if (n is GLSLExprNode) {
            n.buildExprCode(this, out, n)
        } else when (out.type) {
            "Texture" -> buildTextureExpression(out)
            "Bool", "Boolean", "Float", "Double", "Int", "Long", "Vector2f", "Vector3f", "Vector4f" -> {
                appendSimple(out.type, out.currValue, out.defaultValue)
            }
            else -> throw IllegalArgumentException("Unknown node type ${out.type} by ${n.className}")
        }
    }

    private fun buildTextureExpression(out: NodeOutput) {
        val input = out.others.firstOrNull() as? NodeInput
        val tex = input?.getValue() as? Texture
        if (tex != null) {
            appendTextureLoad(tex, input, out)
        } else {
            appendMissingTexturePattern()
        }
    }

    private fun appendMissingTexturePattern() {
        builder.append("((int(floor(uv.x*4.0)+floor(uv.y*4.0)) & 1) != 0 ? vec4(1,0,1,1) : vec4(0,0,0,1))")
    }

    private fun needsDataToWorkConversion(enc: DeferredLayerType?): Boolean {
        return enc != null && !enc.dataToWork.endsWith('=')
    }

    private fun appendDataToWorkPrefix(enc: DeferredLayerType?) {
        builder.append('(').append(enc?.dataToWork).append("(")
    }

    private fun appendDataToWorkSuffix() {
        builder.append("))")
    }

    private fun appendMapping(mapping: String) {
        if (mapping.isNotEmpty()) {
            builder.append('.').append(mapping)
        }
    }

    private fun appendTextureLoad(tex: Texture, input: NodeInput, out: NodeOutput) {
        val textureEncoding = tex.encoding
        val needsConversion = needsDataToWorkConversion(textureEncoding)
        if (needsConversion) appendDataToWorkPrefix(textureEncoding)
        appendTextureLoadCore(tex, input, out)
        appendMapping(tex.mapping)
        if (needsConversion) appendDataToWorkSuffix()
    }

    private fun appendTextureLoadCore(tex: Texture, input: NodeInput, out: NodeOutput) {
        if (tex.tex != whiteTexture) {
            val currValue = input.currValue as? Texture
            val currValue1 = currValue?.texMS
            appendTextureLoadCore(currValue1, input, out)
        } else builder.append("vec4(1.0)")
    }

    private fun appendTextureLoadCore(currValue1: ITexture2D?, input: NodeInput, out: NodeOutput) {
        val useMS = currValue1 != null && currValue1.samples > 1
        val (texName, texType) = textures2.getOrPut(input) {
            val name = createTextureName(out.name, textures2.size)
            val type = if (useMS) GLSLType.S2DMS else GLSLType.S2D
            Triple(name, type, true)
        }
        appendTextureLoad(texName, texType)
    }

    private fun appendTextureLoad(texName: String, texType: GLSLType) {
        if (texType == GLSLType.S2DMS) {
            builder.append("texelFetch(").append(texName)
                .append(",ivec2(uv*vec2(textureSize(").append(texName)
                .append("))),gl_SampleID)")
        } else {
            builder.append("texture(").append(texName).append(",uv)")
        }
    }

    private fun createTextureName(baseName: String, numTextures: Int): String {
        return "tex2I${baseName.filter { it in 'A'..'Z' || it in 'a'..'z' }}$numTextures"
    }

    fun appendVector2f(v: Any?) {
        if (v is Vector2f) {
            builder.append("vec2(")
                .append(v.x).append(',')
                .append(v.y).append(')')
        } else {
            builder.append("vec2(0.0)")
        }
    }

    fun appendVector3f(v: Any?) {
        if (v is Vector3f) {
            builder.append("vec3(")
                .append(v.x).append(',')
                .append(v.y).append(',')
                .append(v.z).append(')')
        } else {
            builder.append("vec3(0.0)")
        }
    }

    fun appendVector4f(v: Any?) {
        if (v is Vector4f) {
            builder.append("vec4(")
                .append(v.x).append(',')
                .append(v.y).append(',')
                .append(v.z).append(',')
                .append(v.w).append(')')
        } else {
            builder.append("vec4(0.0)")
        }
    }

    private fun appendSimple(type: String?, v: Any?, defaultValue: Any?) {
        when (type) {
            "Float", "Double" -> builder.append(AnyToFloat.getFloat(v, 0, defaultValue as? Float ?: 0f))
            "Int", "Long" -> builder.append(AnyToLong.getLong(v, 0, defaultValue as? Long ?: 0L))
            "Vector2f" -> appendVector2f(v)
            "Vector3f" -> appendVector3f(v)
            "Vector4f" -> appendVector4f(v)
            "Bool", "Boolean" -> {
                builder.append(
                    if (v !is Boolean) "false"
                    else v.toString()
                )
            }
            else -> throw IllegalArgumentException("Unknown type $type")
        }
    }

    fun expr(input: NodeInput) {
        if (input.type == "Flow") throw IllegalArgumentException("Cannot request value of flow type")
        val other = input.others.firstOrNull()
        if (other is NodeOutput) { // it is connected
            val aType0 = aType(other, input)
            convert(builder, aType0, input.type) {
                buildExpression(other, other.node!!)
            } ?: throw IllegalStateException("Cannot convert ${other.type}->$aType0 to ${input.type}!")
            return
        } else {
            appendSimple(input.type, input.currValue, input.defaultValue)
        }
    }

    fun constEval(c: NodeInput): Any? {
        return if (c.others.isEmpty()) c.currValue ?: c.defaultValue
        else null
    }

    abstract fun handleReturnNode(node: ReturnNode)

    /**
     * creates code; returns true, if extra return is needed
     * */
    fun buildCode(n: Node?, depth: Int): Boolean {
        n ?: return true
        assertTrue(recursionCheck.add(n)) {
            "Illegal loop for ${n.className}; Recursion isn't supported"
        }
        val answer = when (n) {
            is GLSLFlowNode -> n.buildCode(this, depth)
            // we could use it later for debugging :)
            is ActionNode -> {
                LOGGER.warn("Ignored unknown type ${n.className}, implement GLSLFlowNode maybe")
                buildCode(n.getOutputNode(0), depth)
            }
            else -> {
                LOGGER.warn("Unsupported node type ${n.className}")
                true
            }
        }
        recursionCheck.remove(n)
        return answer
    }

    fun filter(shader: Shader, name: String, tex: ITexture2D, linear: Boolean): ITexture2D {
        if (tex is Texture2D) filter(shader, name, tex, linear)
        return tex
    }

    fun filter(shader: Shader, name: String, tex: Texture2D, linear: Boolean): Texture2D {
        val filter = if (linear) Filtering.LINEAR else Filtering.NEAREST
        if (tex.filtering != filter || tex.clamping != Clamping.REPEAT) {
            val idx = shader.getTextureIndex(name)
            if (idx >= 0) {
                tex.bind(idx)
                tex.ensureFilterAndClamping(filter, Clamping.REPEAT)
            }
        }
        return tex
    }

    fun defineLocalVars(builder: StringBuilder) {
        for ((k, v) in localVars) {
            val type = kotlinToGLSL(v.second)
            builder.append(type).append(' ').append(v.first)
                .append('=').append(type).append("(0);//").append(k).append("\n")
        }
        defineTextures()
        defineMovies()
    }

    fun defineTextures() {
        for ((file, data) in textures) {
            val (name, linear) = data
            typeValues[name] = TypeValueV2(GLSLType.S2D) {
                val tex = TextureCache[file, true]
                if (tex != null) filter(currentShader, name, tex, linear)
                else TextureLib.missingTexture
            }
        }
        for ((node, data) in textures2) {
            val (name, type, linear) = data
            typeValues[name] = TypeValueV2(type) {
                when (val tex = node.getValue()) {
                    is ITexture2D -> filter(currentShader, name, tex, linear)
                    is Texture -> if (type == GLSLType.S2DMS) tex.texMS else tex.tex
                    else -> TextureLib.missingTexture
                }
            }
        }
    }

    fun defineMovies() {
        for ((node, data) in movies) {
            val (name, linear) = data
            typeValues[name] = TypeValueV2(GLSLType.S2D) {
                getMovieTexture(node, name, linear)
            }
        }
    }

    private fun getMovieTexture(node: MovieNode, name: String, linear: Boolean): ITexture2D {
        val file = node.file
        val meta = MediaMetadata.getMeta(file, true)
        return if (meta != null && meta.hasVideo) {
            getMovieTexture(node, name, linear, meta)
        } else TextureLib.blackTexture
    }

    private fun getMovieTexture(node: MovieNode, name: String, linear: Boolean, meta: MediaMetadata): ITexture2D {
        val file = node.file
        val time1 = Time.nanoTime
        if (time1 != g.lastInvalidation) {
            g.invalidate()
            g.lastInvalidation = time1
        }
        val time = node.getFloatInput(2)
        val frameCount = max(1, meta.videoFrameCount)
        var frameIndex = (time * meta.videoFPS).toInt() % frameCount
        if (frameIndex < 0) frameIndex += frameCount
        val bufferLength = 64
        val timeout = 1000L
        val fps = meta.videoFPS
        // load future and previous frames
        for (di in -2..2) {
            if (di != 0) VideoCache.getVideoFrame(
                file, 1, (frameIndex + bufferLength * di) % frameCount,
                bufferLength, fps, timeout, meta, true
            )
        }
        val tex = VideoCache.getVideoFrame(
            file, 1, frameIndex,
            bufferLength, fps, timeout, meta, true
        )
        // to do implement other types, too??
        return if (tex != null && tex.getShaderStage() == GPUFrame.swizzleStageMono) {
            val tex2 = tex.getTextures()[0]
            filter(currentShader, name, tex2, linear)
        } else TextureLib.blackTexture
    }

    fun defineBudget(builder: StringBuilder, budget: Int) {
        builder.append("int budget=").append(budget).append(";\n")
    }

    fun findExportSet(start: Node, layers: List<DeferredLayerType>): BooleanArrayList {
        val exportedLayers = BooleanArrayList(layers.size)
        findExportSetTraverse(start, layers, exportedLayers, 0)
        return exportedLayers
    }

    private fun findExportSetTraverse(
        node: Node,
        layers: List<DeferredLayerType>,
        exportedLayers: BooleanArrayList,
        depth: Int
    ) {
        if (!recursionCheck.add(node)) {
            throw IllegalStateException("Illegal loop for ${node.className}: Recursion isn't supported")
        }
        val outputs = node.outputs
        for (i in outputs.indices) {
            val output = outputs[i]
            if (output.type == "Flow") {
                val inputs = output.others
                for (j in inputs.indices) {
                    val nodeJ = inputs[j].node ?: continue
                    findExportSetTraverse(nodeJ, layers, exportedLayers, depth + 1)
                }
            }
        }
        recursionCheck.remove(node)
        if (node is MaterialReturnNode) {
            findExportSetMark(node, layers, exportedLayers)
        }
    }

    private fun findExportSetMark(
        node: MaterialReturnNode,
        layers: List<DeferredLayerType>,
        exportedLayers: BooleanArrayList
    ) {
        for (i in layers.indices) {
            if (!exportedLayers[i] && shallExport(layers[i], node.inputs[i + 1])) {
                exportedLayers[i] = true
            }
        }
    }

    fun finish(): Pair<Shader, Map<String, TypeValue>> {
        return Pair(currentShader, typeValues)
    }
}