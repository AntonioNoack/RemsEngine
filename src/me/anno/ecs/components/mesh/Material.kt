package me.anno.ecs.components.mesh

import me.anno.cache.instances.ImageCache
import me.anno.ecs.prefab.Prefab.Companion.loadPrefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ECSRegistry
import me.anno.gpu.ShaderLib.pbrModelShader
import me.anno.gpu.TextureLib
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.utils.OS
import org.joml.*
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL21

class Material : PrefabSaveable() {

    @SerializedProperty
    val shaderOverrides = HashMap<String, TypeValue>()

    // or not yet...
    @NotSerializedProperty
    var pipelineStage: PipelineStage? = null

    // or not yet...
    @NotSerializedProperty
    var shader: BaseShader? = null

    var diffuseBase = Vector4f(1f)
    var diffuseMap: FileReference = InvalidRef

    var normalMap: FileReference = InvalidRef
    var normalStrength = 1f

    // base * map
    var emissiveBase = Vector3f(1f)
    var emissiveMap: FileReference = InvalidRef

    // base * map
    var roughnessBase = 1f
    var roughnessMap: FileReference = InvalidRef

    // base * map
    var metallicBase = 0f
    var metallicMap: FileReference = InvalidRef

    var displacementMap: FileReference = InvalidRef

    var occlusionMap: FileReference = InvalidRef

    operator fun set(name: String, type: GLSLType, value: Any) {
        shaderOverrides[name] = TypeValue(type, value)
    }

    operator fun set(name: String, value: TypeValue) {
        shaderOverrides[name] = value
    }

    fun defineShader(shader: Shader) {
        // todo there will be shadow maps: find the correct texture indices!
        // all the data, the shader needs to know from the material
        val timeout = 1000L
        // todo allow swizzling for alpha, roughness, metallic and such
        val metallicTex = ImageCache.getImage(metallicMap, timeout, true)
        val roughnessTex = ImageCache.getImage(roughnessMap, timeout, true)
        val emissiveTex = ImageCache.getImage(emissiveMap, timeout, true)
        val normalTex = ImageCache.getImage(normalMap, timeout, true)
        val albedoTex = ImageCache.getImage(diffuseMap, timeout, true)
        val white = TextureLib.whiteTexture
        val black = TextureLib.blackTexture
        val n001 = TextureLib.normalTexture
        val filtering = GPUFiltering.LINEAR
        val clamping = Clamping.REPEAT
        (metallicTex ?: white).bind(4, filtering, clamping)
        (roughnessTex ?: white).bind(3, filtering, clamping)
        (emissiveTex ?: black).bind(2, filtering, clamping)
        (normalTex ?: n001).bind(1, filtering, clamping)
        (albedoTex ?: white).bind(0, filtering, clamping)
        // todo normal strength, emissive strength, ...
        // todo we also could use an albedo multiplier, when we set the color for a material manually :)
        shader.v1("normalStrength", if (normalTex == null) 0f else normalStrength)
        shader.v4("diffuseBase", diffuseBase)
        shader.v3("emissiveBase", emissiveBase)
        shader.v3("roughnessBase", roughnessBase)
        shader.v3("metallicBase", metallicBase)
        for ((uniformName, valueType) in shaderOverrides) {
            valueType.bind(shader, uniformName)
        }
    }

    enum class GLSLType {
        V1I, V2I, V3I, V4I,
        V1F, V2F, V3F, V4F,
        M3x3, M4x3, M4x4
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

    override fun readSomething(name: String, value: Any?) {
        if (!readSerializableProperty(name, value)) {
            super.readSomething(name, value)
        }
    }

    class TypeValue(val type: GLSLType, val value: Any) {

        fun bind(shader: Shader, uniformName: String) {
            val location = shader[uniformName]
            if (location >= 0) bind(shader, location)
        }

        fun bind(shader: Shader, location: Int) {
            when (type) {
                GLSLType.V1I -> shader.v1(location, value as Int)
                GLSLType.V2I -> {
                    value as Vector2ic
                    GL21.glUniform2i(location, value.x(), value.y())
                }
                GLSLType.V3I -> {
                    value as Vector3ic
                    GL20.glUniform3i(location, value.x(), value.y(), value.z())
                }
                GLSLType.V4I -> {
                    value as Vector4ic
                    GL20.glUniform4i(location, value.x(), value.y(), value.z(), value.w())
                }
                GLSLType.V1F -> shader.v1(location, value as Float)
                GLSLType.V2F -> shader.v2(location, value as Vector2fc)
                GLSLType.V3F -> shader.v3(location, value as Vector3fc)
                GLSLType.V4F -> shader.v4(location, value as Vector4fc)
                GLSLType.M3x3 -> shader.m3x3(location, value as Matrix3fc)
                GLSLType.M4x3 -> shader.m4x3(location, value as Matrix4x3fc)
                GLSLType.M4x4 -> shader.m4x4(location, value as Matrix4fc)
                // else -> LOGGER.warn("Type ${valueType.type} is not yet supported")
            }
        }

    }

    override val className: String = "Material"

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            pbrModelShader = BaseShader()
            ECSRegistry.init()
            val prefab = loadPrefab(OS.documents.getChild("cube bricks.glb"))!!
            for (change in prefab.changes!!) {
                println(change)
            }
            val instance = prefab.createInstance()
            println(instance)
        }

    }

}