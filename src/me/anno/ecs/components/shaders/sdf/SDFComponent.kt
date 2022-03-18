package me.anno.ecs.components.shaders.sdf

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.modifiers.DistanceMapper
import me.anno.ecs.components.shaders.sdf.modifiers.PositionMapper
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.Ptr
import me.anno.gpu.shader.GLSLType
import me.anno.utils.pooling.JomlPools
import me.anno.utils.pooling.ObjectPool
import org.joml.*
import kotlin.math.abs

// todo soft (barrel) distortion
open class SDFComponent : PrefabSaveable() {

    // input: 3d position
    // output: float distance, int material index

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

    val positionMappers = ArrayList<PositionMapper>()
    fun add(m: PositionMapper) {
        positionMappers.add(m)
    }

    val distanceMappers = ArrayList<DistanceMapper>()
    fun add(m: DistanceMapper) {
        distanceMappers.add(m)
    }

    var dynamicPosition = false
    var dynamicRotation = false
    var dynamicScale = false

    // todo calculate bounds based on transform
    // todo calculate bounds based on modifiers & such
    open fun calculateBaseBounds(dst: AABBf) {
        // accurate for most things
        dst.union(-1f, 0f, 0f)
        dst.union(+1f, 0f, 0f)
        dst.union(0f, -1f, 0f)
        dst.union(0f, +1f, 0f)
        dst.union(0f, 0f, -1f)
        dst.union(0f, 0f, +1f)
    }

    fun buildDMShader(
        builder: StringBuilder,
        posIndex: Int,
        dstName: String,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
        val mappers = distanceMappers
        for (index in mappers.indices) {
            val mapper = mappers[index]
            mapper.buildShader(builder, posIndex, dstName, nextVariableId, uniforms, functions)
        }
    }

    open fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstName: String,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ) {
    }

    open fun computeSDFBase(pos: Vector4f): Float {
        throw NotImplementedError()
    }

    fun computeSDF(pos: Vector4f): Float {
        var base = computeSDFBase(pos)
        for (index in distanceMappers.indices) {
            val mapper = distanceMappers[index]
            base = mapper.calcTransform(pos, base)
        }
        return base
    }

    open fun raycast(
        origin: Vector3f,
        direction: Vector3f,
        near: Float,
        far: Float,
        maxIterations: Int,
        sdfReliability: Float = 1f,
        maxRelativeError: Float = 0.001f
    ): Float {
        var distance = near
        val pos = JomlPools.vec4f.create()
        for (i in 0 until maxIterations) {
            pos.x = origin.x + distance * direction.x
            pos.y = origin.y + distance * direction.y
            pos.z = origin.z + distance * direction.z
            pos.w = 0f
            val sd = computeSDF(pos)
            if (abs(sd) <= maxRelativeError * distance) {
                JomlPools.vec4f.sub(1)
                return distance
            }
            distance += sdfReliability * sd
            if (distance > far) break
        }
        JomlPools.vec4f.sub(1)
        return Float.POSITIVE_INFINITY
    }

    open fun calcNormal(hit: Vector3f, dst: Vector3f = Vector3f(), epsilon: Float = 0.0005f): Vector3f {
        val x = 0.5773f * epsilon
        val y = -x
        val pos4 = JomlPools.vec4f.create()
        pos4.set(hit, 0f).add(x, y, y, 0f)
        var sdf = computeSDF(pos4)
        var nx = +sdf
        var ny = -sdf
        var nz = -sdf
        pos4.set(hit, 0f).add(y, y, x, 0f)
        sdf = computeSDF(pos4)
        nx -= sdf
        ny -= sdf
        nz += sdf
        pos4.set(hit, 0f).add(y, x, y, 0f)
        sdf = computeSDF(pos4)
        nx -= sdf
        ny += sdf
        nz -= sdf
        pos4.set(hit, 0f).add(x, x, x, 0f)
        sdf = computeSDF(pos4)
        nx += sdf
        ny += sdf
        nz += sdf
        JomlPools.vec4f.sub(1)
        return dst.set(nx, ny, nz).normalize()
    }

    fun applyTransform(pos: Vector4f) {
        // same operations as in shader
        val tmp = JomlPools.vec3f.create()
        tmp.set(pos.x, pos.y, pos.z)
        tmp.add(position)
        tmp.rotate(rotation)
        pos.set(tmp, pos.w)
        pos.mul(scale)
        for (modifier in positionMappers) {
            modifier.calcTransform(pos)
        }
    }

    /**
     * returns position index and scale name
     * */
    open fun buildTransform(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ): SDFTransform {
        var posIndex = posIndex0
        val position = position
        if (position != pos0 || dynamicPosition) {
            val prevPosition = posIndex
            posIndex = nextVariableId.next()
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
            posIndex = nextVariableId.next()
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
            posIndex = nextVariableId.next()
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
        if (posIndex == posIndex0 && positionMappers.isNotEmpty()) {
            val prevPosition = posIndex
            posIndex = nextVariableId.next()
            builder.append("vec3 pos")
            builder.append(posIndex)
            builder.append("=pos")
            builder.append(prevPosition)
            builder.append(";\n")
        }
        var offsetName: String? = null
        for (modifier in positionMappers) {
            val offsetName1 = modifier.buildShader(builder, posIndex, nextVariableId, uniforms, functions)
            if (offsetName1 != null) {
                if (offsetName == null) {
                    offsetName = offsetName1
                } else {
                    builder.append(offsetName)
                    builder.append("+=")
                    builder.append(offsetName1)
                    builder.append(";\n")
                }
            }
        }
        return sdfTransPool.create().set(posIndex, scaleName, offsetName)
    }

    override fun clone(): SDFComponent {
        val clone = SDFComponent()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFComponent
        clone.positionMappers.clear()
        clone.positionMappers.addAll(clone.positionMappers.map { it.clone() as PositionMapper })
        clone.position.set(position)
        clone.rotation.set(rotation)
        clone.scale = scale
        clone.dynamicPosition = dynamicPosition
        clone.dynamicRotation = dynamicRotation
        clone.dynamicScale = dynamicScale
    }

    override val className: String = "SDFComponent"

    companion object {

        val sdfTransPool = ObjectPool { SDFTransform() }

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

        fun writeVec(builder: StringBuilder, v: Vector4f) {
            builder.append("vec4(")
            builder.append(v.x)
            builder.append(",")
            builder.append(v.y)
            builder.append(",")
            builder.append(v.z)
            builder.append(",")
            builder.append(v.w)
            builder.append(")")
        }

        fun writeVec(builder: StringBuilder, v: Planef) {
            builder.append("vec4(")
            builder.append(v.a)
            builder.append(",")
            builder.append(v.b)
            builder.append(",")
            builder.append(v.c)
            builder.append(",")
            builder.append(v.d)
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
                is Vector2ic -> GLSLType.V2I
                is Vector3fc -> GLSLType.V3F
                is Vector3ic -> GLSLType.V3I
                is Vector4fc, is Quaternionfc, is Planef -> GLSLType.V4F
                is Vector4ic -> GLSLType.V4I
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

}