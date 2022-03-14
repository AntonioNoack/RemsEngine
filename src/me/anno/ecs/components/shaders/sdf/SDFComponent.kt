package me.anno.ecs.components.shaders.sdf

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.modifiers.SDFModifier
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.Ptr
import me.anno.gpu.shader.GLSLType
import me.anno.utils.types.AABBs.clear
import org.joml.*

open class SDFComponent : PrefabSaveable() {

    // input: 3d position
    // output: float distance, int material index

    // todo list parameters with their types
    // todo somehow replace them, so multiple can be used

    // local transform
    var position = Vector3f()
        set(value) {
            field.set(value)
        }

    var rotation = Quaternionf()
        set(value) {
            field.set(value)
        }

    var scale = 1f

    val modifiers = ArrayList<SDFModifier>()
    fun add(m: SDFModifier) {
        modifiers.add(m)
    }

    var dynamicPosition = false
    var dynamicRotation = false
    var dynamicScale = false

    open fun unionBounds(aabb: AABBd) {
        aabb.clear()
    }

    open fun createSDFShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextIndex: Ptr<Int>,
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
    }

    open fun computeSDF(pos: Vector3f): Float {
        throw NotImplementedError()
    }

    fun applyTransform(pos: Vector3f) {
        // same operations as in shader
        pos.add(position)
        pos.rotate(rotation)
        pos.mul(scale)
        for (modifier in modifiers) {
            modifier.applyTransform(pos)
        }
    }

    /**
     * returns position index and scale name
     * */
    open fun createTransformShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextPosition: Ptr<Int>,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ): Pair<Int, String?> {
        var posIndex = posIndex0
        val position = position
        if (position != pos0 || dynamicPosition) {
            val prevPosition = posIndex
            posIndex = nextPosition.value++
            builder.append("vec3 pos")
            builder.append(posIndex)
            builder.append("=pos")
            builder.append(prevPosition)
            builder.append("+")
            if (dynamicPosition) {
                val uniform = defineUniform(uniforms, position)
                builder.append(uniform)
            } else writeVec(builder, position)
            builder.append(";\n")
        }
        val rotation = rotation
        if (rotation != rot0 || dynamicRotation) {
            functions += quatRot
            val prevPosition = posIndex
            posIndex = nextPosition.value++
            builder.append("vec3 pos")
            builder.append(posIndex)
            builder.append("=quatRot(pos")
            builder.append(prevPosition)
            if (dynamicRotation) {
                builder.append(",")
                val uniform = defineUniform(uniforms, rotation)
                builder.append(uniform)
                builder.append(");\n")
            } else {
                builder.append(",vec4(")
                builder.append(rotation.x)
                builder.append(",")
                builder.append(rotation.y)
                builder.append(",")
                builder.append(rotation.z)
                builder.append(",")
                builder.append(rotation.w)
                builder.append("));\n")
            }
        }
        val scaleName = if (scale != sca0 || dynamicScale) {
            val prevPosition = posIndex
            posIndex = nextPosition.value++
            builder.append("vec3 pos")
            builder.append(posIndex)
            builder.append("=pos")
            builder.append(prevPosition)
            if (dynamicScale) {
                val uniform = defineUniform(uniforms, GLSLType.V1F, { scale })
                builder.append("/")
                builder.append(uniform)
                builder.append(";\n")
                uniform
            } else {
                builder.append("*")
                builder.append(1f / scale)
                builder.append(";\n")
                scale.toString()
            }
        } else null
        if (posIndex == posIndex0 && modifiers.isNotEmpty()) {
            val prevPosition = posIndex
            posIndex = nextPosition.value++
            builder.append("vec3 pos")
            builder.append(posIndex)
            builder.append("=pos")
            builder.append(prevPosition)
            builder.append(";\n")
        }
        for (modifier in modifiers) {
            modifier.createTransform(builder, posIndex, uniforms, functions)
        }
        return Pair(posIndex, scaleName)
    }

    companion object {

        fun writeVec(builder: StringBuilder, v: Vector2f) {
            builder.append("vec2(")
            if (v.x != v.y) {
                builder.append(v.x)
                builder.append(",")
                builder.append(v.y)
            } else {
                builder.append(v.x)
            }
            builder.append(")")
        }

        fun writeVec(builder: StringBuilder, v: Vector3f) {
            builder.append("vec3(")
            if (v.x != v.y || v.y != v.z || v.x != v.z) {
                builder.append(v.x)
                builder.append(",")
                builder.append(v.y)
                builder.append(",")
                builder.append(v.z)
            } else {
                builder.append(v.x)
            }
            builder.append(")")
        }

        fun defineUniform(uniforms: HashMap<String, TypeValue>, type: GLSLType, value: Any): String {
            val uniformName = "u${uniforms.size}"
            uniforms[uniformName] = TypeValue(type, value)
            return uniformName
        }

        fun defineUniform(uniforms: HashMap<String, TypeValue>, value: Any): String {
            val type = when (value) {
                is Vector2fc -> GLSLType.V2F
                is Vector3fc -> GLSLType.V3F
                is Vector4fc, is Quaternionfc -> GLSLType.V4F
                else -> throw IllegalArgumentException("Unknown type, use defineUniforms(uniforms, type, value) instead!")
            }
            return defineUniform(uniforms, type, value)
        }

        const val quatRot = "" +
                "vec3 quatRot(vec3 v, vec4 q){\n" +
                "   return v + 2.0 * cross(q.xyz, cross(q.xyz, v) + q.w * v);\n" +
                "}\n"

        val rot0 = Quaternionf()
        val pos0 = Vector3f()
        const val sca0 = 1f
    }

    override fun clone(): SDFComponent {
        val clone = SDFComponent()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFComponent
        clone.modifiers.clear()
        clone.modifiers.addAll(clone.modifiers.map { it.clone() as SDFModifier })
        clone.position.set(position)
        clone.rotation.set(rotation)
        clone.scale = scale
        clone.dynamicPosition = dynamicPosition
        clone.dynamicRotation = dynamicRotation
        clone.dynamicScale = dynamicScale
    }

    override val className: String = "SDFComponent"

}