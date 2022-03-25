package me.anno.ecs.components.mesh.sdf

import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.modifiers.DistanceMapper
import me.anno.ecs.components.mesh.sdf.modifiers.PositionMapper
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.io.serialization.NotSerializedProperty
import me.anno.mesh.Shapes
import me.anno.ui.editor.stacked.Option
import me.anno.utils.pooling.JomlPools
import me.anno.utils.pooling.ObjectPool
import me.anno.utils.types.AABBs.avgX
import me.anno.utils.types.AABBs.avgY
import me.anno.utils.types.AABBs.avgZ
import me.anno.utils.types.AABBs.clear
import me.anno.utils.types.AABBs.deltaX
import me.anno.utils.types.AABBs.deltaY
import me.anno.utils.types.AABBs.deltaZ
import me.anno.utils.types.AABBs.transformUnion
import org.joml.*
import kotlin.math.abs

open class SDFComponent : ProceduralMesh() {

    /**
     * how much larger the underlying mesh needs to be to cover this sdf mesh;
     * 0 = no larger than usual,
     * 1 = twice as large,
     * ...
     * 9 = 10x larger than normal;
     * the best value depends on your modifiers & such
     * */
    var relativeMeshMargin = 0f
        set(value) {
            if (field != value) {
                invalidateBounds()
                field = value
            }
        }

    val material by lazy { Material.create() }
    val materialRef get() = material.ref!!

    @DebugProperty
    @NotSerializedProperty
    var shaderVersion = 0

    var hasValidBounds = false
    fun invalidateBounds() {
        hasValidBounds = false
    }

    override fun onUpdate(): Int {
        super.onUpdate()
        if (!hasValidBounds) {
            hasValidBounds = true
            // recalculate bounds & recreate mesh
            updateMesh(mesh2, false)
            entity?.invalidateAABBsCompletely()
        }
        return 1
    }

    fun invalidateShader() {
        val parent = parent
        if (parent is SDFComponent) {
            parent.invalidateShader()
        } else invalidateMesh()
    }

    // allow manual sdf?
    // todo script components for sdfs?

    open fun calculateBounds(dstUnion: AABBf) {
        val base = JomlPools.aabbf.create()
        base.clear()
        calculateBaseBounds(base)
        transformBounds(base, dstUnion)
        JomlPools.aabbf.sub(1)
    }

    open fun transformBounds(base: AABBf, dstUnion: AABBf) {
        // todo check if this matches the actual operation
        // probably we need to inverse the computation in buildTransform
        val transform = JomlPools.mat4x3f.create()
        transform.translate(position)
        transform.rotate(rotation)
        transform.scale(scale)
        // add margin
        val rms = relativeMeshMargin
        if (rms != 0f) {
            val marginScale = 1f + rms
            transform.scale(marginScale)
        }
        // todo apply position mappers like arrays, mirrors and such
        base.transformUnion(transform, dstUnion)
        JomlPools.mat4x3f.sub(1)
    }

    override fun generateMesh(mesh: Mesh) {
        updateMesh(mesh, true)
    }

    fun updateMesh(mesh: Mesh, generateShader: Boolean) {
        shaderVersion++
        // todo compute bounds for size...
        // todo parameters for extra size, e.g. for dynamic positions & rotations & such
        val aabb = JomlPools.aabbf.create()
        aabb.clear()
        calculateBaseBounds(aabb)
        // for testing only
        Shapes.createCube(
            mesh,
            aabb.deltaX(), aabb.deltaY(), aabb.deltaZ(),
            aabb.avgX(), aabb.avgY(), aabb.avgZ(),
            withNormals = false, front = false, back = true,
        )
        if (generateShader) {
            val (overrides, shader) = SDFComposer.createECSShader(this)
            material.shader = shader
            material.shaderOverrides.clear()
            material.shaderOverrides.putAll(overrides)
            mesh.material = materialRef
            mesh.inverseOutline = true
        }
        JomlPools.aabbf.sub(1)
    }

    // input: 3d position
    // output: float distance, int material index

    // local transform
    var position = Vector3f()
        set(value) {
            if (!dynamicPosition) invalidateShader()
            field.set(value)
        }

    var rotation = Quaternionf()
        set(value) {
            if (!dynamicRotation) invalidateShader()
            field.set(value)
        }

    override fun getOptionsByType(type: Char): List<Option>? {
        return if (type == 'p') getOptionsByClass(this, PositionMapper::class)
        else getOptionsByClass(this, DistanceMapper::class)
    }

    override fun listChildTypes(): String = "pd"
    override fun getChildListByType(type: Char) = if (type == 'p') positionMappers else distanceMappers
    override fun getTypeOf(child: PrefabSaveable) = if (child is PositionMapper) 'p' else 'd'
    override fun addChild(index: Int, child: PrefabSaveable) = addChildByType(index, ' ', child)
    override fun getChildListNiceName(type: Char) = if (type == 'p') "PositionMappers" else "DistanceMappers"
    override fun addChildByType(index: Int, type: Char, child: PrefabSaveable) {
        when (child) {
            is PositionMapper -> {
                positionMappers.add(index, child)
                child.parent = this
                invalidateShader()
            }
            is DistanceMapper -> {
                distanceMappers.add(index, child)
                child.parent = this
                invalidateShader()
            }
            else -> super.addChildByType(index, type, child)
        }
    }

    override fun removeChild(child: PrefabSaveable) {
        when (child) {
            is PositionMapper -> {
                positionMappers.remove(child)
                invalidateShader()
            }
            is DistanceMapper -> {
                distanceMappers.remove(child)
                invalidateShader()
            }
        }
        super.removeChild(child)
    }

    var scale = 1f
        set(value) {
            if (field != value) {
                if (!dynamicScale) invalidateShader()
                field = value
            }
        }

    val positionMappers = ArrayList<PositionMapper>()
    fun add(m: PositionMapper) {
        positionMappers.add(m)
        val parent = m.parent
        if (parent != null && parent !== this) parent.removeChild(m)
        m.parent = this
    }

    val distanceMappers = ArrayList<DistanceMapper>()
    fun add(m: DistanceMapper) {
        distanceMappers.add(m)
        val parent = m.parent
        if (parent != null && parent !== this) parent.removeChild(m)
        m.parent = this
    }

    var dynamicPosition = false
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    var dynamicRotation = false
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    var dynamicScale = false
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

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
        tmp.sub(position)
        val tmp2 = JomlPools.quat4f.borrow()
        tmp.rotate(tmp2.set(-rotation.x, -rotation.y, -rotation.z, rotation.w))
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
            builder.append("-")
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
            builder.append("=quatRotInv(pos")
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
                "}\n" +
                "vec3 quatRotInv(vec3 v, vec4 q){\n" +
                "   return v - 2.0 * cross(q.xyz, q.w * v - cross(q.xyz, v));\n" +
                "}\n"

        val rot0 = Quaternionf()
        val pos0 = Vector3f()
        const val sca0 = 1f
    }

}