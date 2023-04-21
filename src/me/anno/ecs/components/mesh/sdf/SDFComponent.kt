package me.anno.ecs.components.mesh.sdf

import me.anno.Build
import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.*
import me.anno.ecs.components.mesh.*
import me.anno.ecs.components.mesh.sdf.arrays.SDFGroupArray
import me.anno.ecs.components.mesh.sdf.modifiers.DistanceMapper
import me.anno.ecs.components.mesh.sdf.modifiers.PositionMapper
import me.anno.ecs.components.script.QuickScriptComponent
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.Projection.projectRayToAABBBack
import me.anno.engine.raycast.Projection.projectRayToAABBFront
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.Raycast
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.input.Input.setClipboardContent
import me.anno.io.files.FileReference
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.sq
import me.anno.mesh.Shapes
import me.anno.ui.editor.stacked.Option
import me.anno.utils.pooling.JomlPools
import me.anno.utils.pooling.ObjectPool
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Matrices.set2
import org.joml.*
import kotlin.math.abs
import kotlin.math.floor

// todo mobius ring like https://www.shadertoy.com/view/XldSDs
// todo signed distance fields, e.g. from meshes

// todo sdf images and maybe text like https://github.com/fogleman/sdf
// todo color maps like https://iquilezles.org/www/articles/palettes/palettes.htm

// this principle could be applied on mobile platforms:
// lighting & environment independent, nice shading
// mapcaps like https://observablehq.com/@makio135/matcaps?ui=classic
// we then could directly link an online library for fast development
// ... or generate them synthetically ...

open class SDFComponent : ProceduralMesh(), Renderable {

    override var isEnabled: Boolean
        get() = super.isEnabled
        set(value) {
            if (super.isEnabled != value) {
                invalidateShader()
                super.isEnabled = value
            }
        }

    @NotSerializedProperty
    private val internalComponents = ArrayList<Component>(4)

    @SerializedProperty
    override val components: List<Component>
        get() = internalComponents

    @DebugProperty
    @NotSerializedProperty
    var camNear = 0f

    @DebugProperty
    @NotSerializedProperty
    var camFar = 0f

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

    // todo hideInInspector seems to be broken...
    @HideInInspector("isSDFChild")
    var debugMode = DebugMode.DEFAULT

    val material = Material()

    @Type("List<Material/Reference>")
    @SerializedProperty
    @HideInInspector("isSDFChild")
    var sdfMaterials = emptyList<FileReference>()
        set(value) {
            field = value
            invalidateShader()
        }

    @DebugProperty
    @NotSerializedProperty
    var shaderVersion = 0

    @Docs(
        "Operations like twisting introduce artifacts, because they warp the space non-uniformly. " +
                "If you encounter artifacts, reduce the reliability (trustworthy-ness) for the distance field. " +
                "Halving the reliability, halves performance."
    )
    @Group("Tracing")
    @Range(0.0, 2.0)
    @HideInInspector("isSDFChild")
    var globalReliability = 1f

    @Group("Tracing")
    @Range(0.0, 2.0)
    var localReliability = 1f
        set(value) {
            if ((field == 1f) != (value == 1f)) {
                // activating/deactivating dynamic values automatically
                invalidateShader()
            }
            field = value
        }

    var globalDynamic
        get() = Companion.globalDynamic
        set(value) {
            Companion.globalDynamic = value
        }

    @Group("Tracing")
    @Docs("How many steps the raytracer shall take at maximum")
    @Range(0.0, 1e4)
    @HideInInspector("isSDFChild")
    var maxSteps = 70

    @Group("Tracing")
    @Docs("How much the ray is allowed to deviate from a perfect hit")
    @Range(0.0, 1.0)
    @HideInInspector("isSDFChild")
    var maxRelativeError = 0.001f

    @Group("Tracing")
    @Docs("In local units, from which distance the normals shall be sampled")
    @Range(0.0, 1e3)
    @HideInInspector("isSDFChild")
    var normalEpsilon = 0.005f

    val isSDFChild get() = parent is SDFComponent

    @DebugProperty
    @NotSerializedProperty
    var hasInvalidBounds = true

    // input: 3d position
    // output: float distance, int material index

    // local transform
    @Group("Transform")
    var position = Vector3f()
        set(value) {
            if (dynamicPosition || globalDynamic) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    @Group("Transform")
    var rotation = Quaternionf()
        set(value) {
            if (dynamicRotation || globalDynamic) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    @Group("Transform")
    var scale = 1f
        set(value) {
            if (field != value) {
                if (dynamicScale || globalDynamic) invalidateBounds()
                else invalidateShader()
                field = value
            }
        }

    @Group("Transform")
    var dynamicPosition = false
        set(value) {
            if (field != value) {
                if (!globalDynamic) invalidateShader()
                field = value
            }
        }

    @Group("Transform")
    var dynamicRotation = false
        set(value) {
            if (field != value) {
                if (!globalDynamic) invalidateShader()
                field = value
            }
        }

    @Group("Transform")
    var dynamicScale = false
        set(value) {
            if (field != value) {
                if (!globalDynamic) invalidateShader()
                field = value
            }
        }

    val positionMappers = ArrayList<PositionMapper>()
    val distanceMappers = ArrayList<DistanceMapper>()

    @DebugAction
    fun createShaderToyScript() {
        setClipboardContent(SDFComposer.createShaderToyShader(this))
    }

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int
    ): Int {
        val mesh = getMesh()
        this.clickId = clickId
        pipeline.addMesh(mesh, this, entity, gfxId)
        lastDrawn = Engine.gameTime
        return clickId + 1
    }

    @DebugAction
    fun invalidateBounds() {
        val parent = parent
        if (parent is SDFComponent) {
            parent.invalidateBounds()
        } else hasInvalidBounds = true
    }

    fun computeGlobalTransform(dst: Matrix4x3f): Matrix4x3f {
        when (val parent = parent) {
            is Entity -> dst.set2(parent.transform.globalTransform)
            is SDFGroup -> parent.computeGlobalTransform(dst)
            // else idk
        }
        dst.translate(position)
        dst.rotate(rotation)
        dst.scale(scale)
        return dst
    }

    override fun onDestroy() {
        super.onDestroy()
        val components = internalComponents
        for (index in components.indices) {
            val child = components[index]
            if (child.isEnabled) child.onDestroy()
        }
    }

    override fun onUpdate(): Int {
        super.onUpdate()
        if (hasInvalidBounds) {
            hasInvalidBounds = false
            // recalculate bounds & recreate mesh
            updateMesh(data, false)
            invalidateAABB()
        }
        val components = internalComponents
        for (index in components.indices) {
            val child = components[index]
            if (child.isEnabled) child.callUpdate()
        }
        return 1
    }

    override fun hasRaycastType(typeMask: Int): Boolean {
        return typeMask.and(Raycast.TRIANGLES) != 0 || typeMask.and(Raycast.SDFS) != 0
    }

    override fun raycast(
        entity: Entity,
        start: Vector3d,
        direction: Vector3d,
        end: Vector3d,
        radiusAtOrigin: Double,
        radiusPerUnit: Double,
        typeMask: Int,
        includeDisabled: Boolean,
        result: RayHit
    ): Boolean {
        return if (typeMask.and(Raycast.SDFS) == 0) {
            // approximately only
            super.raycast(
                entity, start, direction, end,
                radiusAtOrigin, radiusPerUnit,
                typeMask, includeDisabled, result
            )
        } else {
            // raycast
            val globalTransform = entity.transform.globalTransform // local -> global
            val globalInv = result.tmpMat4x3d.set(globalTransform).invert()
            val vec3f = result.tmpVector3fs
            val vec3d = result.tmpVector3ds
            val vec4f = result.tmpVector4fs
            // compute ray positions in the wild
            val localSrt0 = globalInv.transformPosition(start, vec3d[0])
            val localDir0 = globalInv.transformDirection(direction, vec3d[1]).normalize()
            val localEnd0 = globalInv.transformPosition(end, vec3d[2])
            // project start & end onto aabb for better results in single precision
            val maxLocalDistanceSq0 = localSrt0.distanceSquared(localEnd0)
            val startOffset = projectRayToAABBFront(localSrt0, localDir0, localAABB, dst = localSrt0)
            projectRayToAABBBack(localEnd0, localDir0, localAABB, dst = localEnd0)
            val localSrt = vec3f[0].set(localSrt0)
            val localDir = vec3f[1].set(localDir0)
            val localEnd = vec3f[2].set(localEnd0)
            val near = 0f
            val far = localSrt.distance(localEnd)
            val maxSteps = max(maxSteps, 250)
            // todo if already inside body, return it?
            // we could use different parameters for higher accuracy...
            val seeds = IntArrayList(8)
            val localDistance = raycast(localSrt, localDir, near, far, maxSteps, seeds)
            val localDistance0 = localDistance + startOffset
            if (sq(localDistance0) < maxLocalDistanceSq0) {
                val localHit = vec3f[3].set(localDir).mul(localDistance).add(localSrt)
                val localNormal = calcNormal(localHit, vec3f[4], seeds, normalEpsilon)
                val bestHit = findClosestComponent(vec4f[0].set(localHit, 0f), seeds)
                result.setFromLocal(
                    globalTransform,
                    localHit, localNormal,
                    start, direction, end
                )
                result.component = bestHit
                true
            } else false
        }
    }

    @DebugAction
    fun invalidateShader() {
        val parent = parent
        if (parent is SDFComponent) {
            parent.invalidateShader()
        } else {
            invalidateMesh()
            invalidateAABB()
        }
    }

    // allow manual sdf?
    // done easy script components for sdfs?
    // no, just use lua scripts
    // todo allow (script) components to be added to sdf components

    open fun calculateBounds(dst: AABBf) {
        dst.clear()
        calculateBaseBounds(dst)
        if (parent !is Entity) localAABB.set(dst)
        // transform bounds using position, rotation, scale, mappers
        transform(dst)
        // not truly the global one; rather the one inside our parent
        if (parent !is Entity) globalAABB.set(dst)
    }

    open fun transform(src: AABBf, dst: AABBf = src) {
        val transform = JomlPools.mat4x3f.create()
        transform.identity()
        transform.translate(position)
        transform.rotate(rotation)
        transform.scale(scale)
        // add margin
        val rms = relativeMeshMargin
        if (rms != 0f) {
            val marginScale = 1f + rms
            transform.scale(marginScale)
        }
        for (index in positionMappers.indices.reversed()) {
            val mapper = positionMappers[index]
            if (mapper.isEnabled) {
                clampBounds(dst)
                mapper.applyTransform(dst)
            }
        }
        if (this is SDFGroupArray) {
            clampBounds(dst)
            applyArrayTransform(dst)
            val mi = min(modulatorIndex, children.lastIndex)
            if (mi > 0) {
                val tmp = JomlPools.aabbf.create()
                tmp.clear()
                calculateBaseBounds(tmp, children.subList(0, mi))
                dst.intersect(tmp)
                JomlPools.aabbf.sub(1)
            }
        }
        // reversed as well?
        for (index in distanceMappers.indices) {
            val mapper = distanceMappers[index]
            if (mapper.isEnabled) {
                clampBounds(dst)
                mapper.applyTransform(dst)
            }
        }
        // make bounds reasonable, so we can actually use them in computations
        clampBounds(dst)
        src.transform(transform, dst)
        JomlPools.mat4x3f.sub(1)
    }

    @Docs("Limit for reasonable coordinates; use this against precision issues")
    var limit = 1e3f
        set(value) {
            if (field != value) {
                invalidateBounds()
                field = value
            }
        }

    private fun clampBounds(src: AABBf, dst: AABBf = src) {
        val lim = limit
        dst.minX = max(src.minX, -lim)
        dst.minY = max(src.minY, -lim)
        dst.minZ = max(src.minZ, -lim)
        dst.maxX = min(src.maxX, +lim)
        dst.maxY = min(src.maxY, +lim)
        dst.maxZ = min(src.maxZ, +lim)
    }

    override fun generateMesh(mesh: Mesh) {
        updateMesh(mesh, true)
    }

    fun updateMesh(mesh: Mesh, generateShader: Boolean) {
        val aabb = JomlPools.aabbf.create()
        aabb.clear()
        calculateBounds(aabb)
        // for testing only
        Shapes.createCube(
            mesh, aabb.deltaX(), aabb.deltaY(), aabb.deltaZ(), aabb.avgX(), aabb.avgY(), aabb.avgZ(),
            withNormals = false, front = false, back = true,
        )
        mesh.aabb.set(aabb)
        if (generateShader) {
            shaderVersion++
            val (overrides, shader) = SDFComposer.createECSShader(this)
            material.shader = shader
            material.shaderOverrides.clear()
            material.shaderOverrides.putAll(overrides)
            mesh.material = material.ref
        }
        mesh.inverseOutline = true
        mesh.invalidateGeometry()
        JomlPools.aabbf.sub(1)
    }

    override fun getOptionsByType(type: Char) = when (type) {
        'p' -> getOptionsByClass(this, PositionMapper::class)
        'd' -> getOptionsByClass(this, DistanceMapper::class)
        else -> getOptionsByClass(this, QuickScriptComponent::class)
    }

    override fun listChildTypes(): String = "pdx"

    override fun getChildListByType(type: Char): List<PrefabSaveable> = when (type) {
        'p' -> positionMappers
        'd' -> distanceMappers
        else -> components
    }

    override fun getTypeOf(child: PrefabSaveable): Char = when (child) {
        is PositionMapper -> 'p'
        is DistanceMapper -> 'd'
        else -> 'x'
    }

    override fun addChild(index: Int, child: PrefabSaveable) = addChildByType(index, ' ', child)
    override fun getChildListNiceName(type: Char) = when (type) {
        'p' -> "PositionMappers"
        'd' -> "DistanceMappers"
        else -> "Components"
    }

    override fun addChildByType(index: Int, type: Char, child: PrefabSaveable) {
        when (child) {
            is PositionMapper -> {
                positionMappers.add(clamp(index, 0, positionMappers.size), child)
                child.parent = this
                invalidateShader()
            }
            is DistanceMapper -> {
                distanceMappers.add(clamp(index, 0, distanceMappers.size), child)
                child.parent = this
                invalidateShader()
            }
            is Component -> {
                internalComponents.add(clamp(index, 0, internalComponents.size), child)
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

    open fun calculateBaseBounds(dst: AABBf) {
        // ok for most things
        dst.setMin(-1f, -1f, -1f)
        dst.setMax(+1f, +1f, +1f)
    }

    fun buildDistanceMapperShader(
        builder: StringBuilder,
        posIndex: Int,
        dstIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
        val mappers = distanceMappers
        for (index in mappers.indices) {
            val mapper = mappers[index]
            if (mapper.isEnabled) {
                mapper.buildShader(builder, posIndex, dstIndex, nextVariableId, uniforms, functions, seeds)
            }
        }
    }

    open fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
    }

    open fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        throw NotImplementedError()
    }

    fun computeSDF(pos: Vector4f, seeds: IntArrayList): Float {
        applyTransform(pos, seeds)
        var base = computeSDFBase(pos, seeds)
        for (index in distanceMappers.indices) {
            val mapper = distanceMappers[index]
            if (mapper.isEnabled) {
                base = mapper.calcTransform(pos, base)
            }
        }
        return base * localReliability * scale
    }

    open fun findClosestComponent(pos: Vector4f, seeds: IntArrayList): SDFComponent {
        return this
    }

    fun raycast(
        origin: Vector3f,
        direction: Vector3f,
        near: Float,
        far: Float,
        maxSteps: Int,
        seeds: IntArrayList
    ): Float {
        return raycast(
            origin, direction, near, far, maxSteps,
            this.globalReliability,
            this.maxRelativeError,
            seeds
        )
    }

    open fun raycast(
        origin: Vector3f,
        direction: Vector3f,
        near: Float,
        far: Float,
        maxSteps: Int = this.maxSteps,
        sdfReliability: Float = this.globalReliability,
        maxRelativeError: Float = this.maxRelativeError,
        seeds: IntArrayList
    ): Float {
        var distance = near
        val pos = JomlPools.vec4f.create()
        for (i in 0 until maxSteps) {
            pos.set(
                origin.x + distance * direction.x,
                origin.y + distance * direction.y,
                origin.z + distance * direction.z, 0f
            )
            val sd = computeSDF(pos, seeds)
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

    open fun calcNormal(
        hit: Vector3f,
        dst: Vector3f = Vector3f(),
        seeds: IntArrayList = IntArrayList(8),
        epsilon: Float = 0.0005f
    ): Vector3f {
        val x = 0.5773f * epsilon
        val y = -x
        val pos4 = JomlPools.vec4f.create()
        pos4.set(hit, 0f).add(x, y, y, 0f)
        var sdf = computeSDF(pos4, seeds)
        seeds.clear()
        var nx = +sdf
        var ny = -sdf
        var nz = -sdf
        pos4.set(hit, 0f).add(y, y, x, 0f)
        sdf = computeSDF(pos4, seeds)
        seeds.clear()
        nx -= sdf
        ny -= sdf
        nz += sdf
        pos4.set(hit, 0f).add(y, x, y, 0f)
        sdf = computeSDF(pos4, seeds)
        seeds.clear()
        nx -= sdf
        ny += sdf
        nz -= sdf
        pos4.set(hit, 0f).add(x, x, x, 0f)
        sdf = computeSDF(pos4, seeds)
        seeds.clear()
        nx += sdf
        ny += sdf
        nz += sdf
        JomlPools.vec4f.sub(1)
        return dst.set(nx, ny, nz).normalize()
    }

    open fun applyTransform(pos: Vector4f, seeds: IntArrayList) {
        // same operations as in shader
        val tmp = JomlPools.vec3f.borrow()
        tmp.set(pos.x, pos.y, pos.z)
        tmp.sub(position)
        val tmp2 = JomlPools.quat4f.borrow()
        tmp.rotate(tmp2.set(-rotation.x, -rotation.y, -rotation.z, rotation.w))
        pos.set(tmp, pos.w)
        pos.div(scale)
        val mappers = positionMappers
        for (index in mappers.indices) {
            val mapper = mappers[index]
            if (mapper.isEnabled) {
                mapper.calcTransform(pos, seeds)
            }
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
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ): SDFTransform {
        var currIndex = posIndex0
        val position = position
        val dynamicPosition = dynamicRotation || globalDynamic
        val dynamicRotation = dynamicRotation || globalDynamic
        val dynamicScale = dynamicScale || globalDynamic
        if (position != pos0 || dynamicPosition) {
            val prevIndex = currIndex
            currIndex = nextVariableId.next()
            builder.append("vec3 pos")
            builder.append(currIndex)
            builder.append("=pos")
            builder.append(prevIndex)
            builder.append("-")
            if (dynamicPosition) {
                val uniform = defineUniform(uniforms, position)
                builder.append(uniform)
            } else builder.appendVec(position)
            builder.append(";\n")
            appendIdentityDir(builder, currIndex, prevIndex)
            appendIdentitySca(builder, currIndex, prevIndex)
        }
        val rotation = rotation
        if (rotation != rot0 || dynamicRotation) {
            functions += quatRot
            val prevIndex = currIndex
            currIndex = nextVariableId.next()
            if (dynamicRotation) {
                val uniform = defineUniform(uniforms, rotation)
                builder.append("vec3 pos").append(currIndex)
                builder.append("=quatRotInv(pos").append(prevIndex).append(",")
                builder.append(uniform).append(");\n")
                builder.append("vec3 dir").append(currIndex)
                builder.append("=quatRotInv(dir").append(prevIndex).append(",")
                builder.append(uniform).append(");\n")
            } else {
                builder.append("vec4 rot").append(currIndex)
                    .append("=").appendVec(rotation).append(";\n")
                builder.append("vec3 pos").append(currIndex)
                builder.append("=quatRotInv(pos").append(prevIndex)
                builder.append(",rot").append(currIndex).append(");\n")
                builder.append("vec3 dir").append(currIndex)
                builder.append("=quatRotInv(dir").append(prevIndex)
                builder.append(",rot").append(currIndex).append(");\n")
            }
            appendIdentitySca(builder, currIndex, prevIndex)
        }
        val scaleName = if (scale != sca0 || dynamicScale) {
            val prevIndex = currIndex
            currIndex = nextVariableId.next()
            appendIdentityDir(builder, currIndex, prevIndex)
            builder.append("vec3 pos")
            builder.append(currIndex)
            builder.append("=pos")
            builder.append(prevIndex)
            if (dynamicScale) {
                val uniform = defineUniform(uniforms, GLSLType.V1F) { scale }
                builder.append("/")
                builder.append(uniform)
                builder.append(";\n")
                builder.append("float sca").append(currIndex)
                    .append("=sca").append(prevIndex).append("*").append(uniform).append(";\n")
                uniform
            } else {
                val invScale = 1f / scale
                builder.append("*")
                builder.append(invScale)
                builder.append(";\n")
                builder.append("float sca").append(currIndex)
                    .append("=sca").append(prevIndex).append("*").append(invScale).append(";\n")
                scale.toString()
            }
        } else null
        val mappers = positionMappers
        if (currIndex == posIndex0 && mappers.any2 { it.isEnabled }) {
            val prevIndex = currIndex
            currIndex = nextVariableId.next()
            builder.append("vec3 pos")
            builder.append(currIndex)
            builder.append("=pos")
            builder.append(prevIndex)
            builder.append(";\n")
            appendIdentityDir(builder, currIndex, prevIndex)
            appendIdentitySca(builder, currIndex, prevIndex)
        }
        var offsetName: String? = null
        for (index in mappers.indices) {
            val mapper = mappers[index]
            if (mapper.isEnabled) {
                val offsetName1 = mapper.buildShader(builder, currIndex, nextVariableId, uniforms, functions, seeds)
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
        }
        return sdfTransPool.create().set(currIndex, scaleName, offsetName)
    }

    fun appendIdentityDir(builder: StringBuilder, posIndex: Int, prevPosition: Int) {
        builder.append("#define dir$posIndex dir$prevPosition\n")
    }

    fun appendIdentitySca(builder: StringBuilder, posIndex: Int, prevPosition: Int) {
        builder.append("#define sca$posIndex sca$prevPosition\n")
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFComponent
        dst.positionMappers.clear()
        dst.positionMappers.addAll(dst.positionMappers.map {
            val mapper = it.clone() as PositionMapper
            mapper.parent = dst
            mapper
        })
        dst.distanceMappers.clear()
        dst.distanceMappers.addAll(dst.distanceMappers.map {
            val mapper = it.clone() as DistanceMapper
            mapper.parent = dst
            mapper
        })
        dst.position.set(position)
        dst.rotation.set(rotation)
        dst.scale = scale
        dst.dynamicPosition = dynamicPosition
        dst.dynamicRotation = dynamicRotation
        dst.dynamicScale = dynamicScale
        dst.globalReliability = globalReliability
        dst.normalEpsilon = normalEpsilon
        dst.maxSteps = maxSteps
        dst.maxRelativeError = maxRelativeError
        dst.relativeMeshMargin = relativeMeshMargin
    }

    override val className: String get() = "SDFComponent"

    companion object {

        // private val LOGGER = LogManager.getLogger(SDFComponent::class)

        var globalDynamic = Build.isDebug

        val sdfTransPool = ObjectPool { SDFTransform() }

        fun mod(x: Float, y: Float): Float {
            return x - y * floor(x / y)
        }

        fun StringBuilder.appendVec(v: Vector2f): StringBuilder {
            append("vec2(")
            append(v.x)
            if (v.x != v.y) {
                append(',')
                append(v.y)
            }
            append(')')
            return this
        }

        fun StringBuilder.appendVec(v: Vector3f): StringBuilder {
            append("vec3(")
            append(v.x)
            if (v.x != v.y || v.y != v.z) {
                append(',')
                append(v.y)
                append(',')
                append(v.z)
            }
            append(')')
            return this
        }

        fun StringBuilder.appendVec(v: Vector3i): StringBuilder {
            append("ivec3(")
            append(v.x)
            if (v.x != v.y || v.y != v.z) {
                append(',')
                append(v.y)
                append(',')
                append(v.z)
            }
            append(')')
            return this
        }

        fun StringBuilder.appendVec(v: Vector4f): StringBuilder {
            append("vec4(")
            append(v.x)
            if (v.x != v.y || v.x != v.z || v.x != v.w) {
                append(',')
                append(v.y)
                append(',')
                append(v.z)
                append(',')
                append(v.w)
            }
            append(')')
            return this
        }

        fun StringBuilder.appendVec(v: Quaternionf): StringBuilder {
            append("vec4(")
            append(v.x)
            append(',')
            append(v.y)
            append(',')
            append(v.z)
            append(',')
            append(v.w)
            append(')')
            return this
        }

        fun StringBuilder.appendVec(v: Planef): StringBuilder {
            append("vec4(")
            append(v.a)
            append(',')
            append(v.b)
            append(',')
            append(v.c)
            append(',')
            append(v.d)
            append(')')
            return this
        }

        fun StringBuilder.appendUniform(uniforms: HashMap<String, TypeValue>, value: Any): StringBuilder {
            append(defineUniform(uniforms, value))
            return this
        }

        fun StringBuilder.appendUniform(
            uniforms: HashMap<String, TypeValue>,
            type: GLSLType,
            value: Any
        ): StringBuilder {
            append(defineUniform(uniforms, type, value))
            return this
        }

        fun StringBuilder.appendUniform(
            uniforms: HashMap<String, TypeValue>,
            type: GLSLType,
            value: () -> Any
        ): StringBuilder {
            append(defineUniform(uniforms, type, value))
            return this
        }

        fun StringBuilder.appendMinus(uniform: Int): StringBuilder {
            append("vec4(-res").append(uniform).append(".x,res").append(uniform).append(".yzw)")
            return this
        }

        fun defineUniform(uniforms: HashMap<String, TypeValue>, type: GLSLType, value: () -> Any): String {
            val uniformName = "u${uniforms.size}"
            uniforms[uniformName] = TypeValueV2(type, value)
            return uniformName
        }

        fun defineUniform(uniforms: HashMap<String, TypeValue>, type: GLSLType, value: Any): String {
            val uniformName = "u${uniforms.size}"
            uniforms[uniformName] = TypeValue(type, value)
            return uniformName
        }

        fun defineUniform(uniforms: HashMap<String, TypeValue>, value: Any): String {
            val type = when (value) {
                is Vector2f -> GLSLType.V2F
                is Vector2i -> GLSLType.V2I
                is Vector3f -> GLSLType.V3F
                is Vector3i -> GLSLType.V3I
                is Vector4f, is Quaternionf, is Planef -> GLSLType.V4F
                is Vector4i -> GLSLType.V4I
                else -> throw IllegalArgumentException("Unknown type, use defineUniforms(uniforms, type, value) instead!")
            }
            return defineUniform(uniforms, type, value)
        }

        val rot0 = Quaternionf()
        val pos0 = Vector3f()
        const val sca0 = 1f
    }

}