package me.anno.image.thumbs

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.ecs.components.mesh.material.Materials
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.TextureCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths
import me.anno.mesh.MeshUtils.centerMesh
import me.anno.mesh.MeshUtils.getScaleFromAABB
import me.anno.utils.Sleep
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.mapCallback
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.lists.Lists.flatten
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import kotlin.math.max

/**
 * extra functions for thumbnail generator, because that class is growing too big
 * */
object AssetThumbHelper {

    val unityExtensions = "mat,prefab,unity,asset,controller,meta"
    val unityExtensions1 = unityExtensions.split(',')
    private val LOGGER = LogManager.getLogger(AssetThumbHelper::class)

    private fun iterateMaterials(l0: List<FileReference>, l1: List<FileReference>): List<FileReference> {
        return when {
            l0.isEmpty() -> l1
            l1.isEmpty() -> l0
            else -> createArrayList(max(l0.size, l1.size)) { index ->
                val li = l0.getOrNull(index)?.nullIfUndefined() ?: l1.getOrNull(index)
                if (li != null && li != InvalidRef) li else InvalidRef
            }
        }
    }

    fun createCameraMatrix(aspectRatio: Float): Matrix4f {
        val cameraMatrix = Matrix4f()
        Perspective.setPerspective(cameraMatrix, 0.7f, aspectRatio, 0.001f, 10f, 0f, 0f)
        return cameraMatrix
    }

    fun createModelMatrix(): Matrix4x3f {
        return Matrix4x3f()
            .translate(0f, 0f, -1f)// move the camera back a bit
            .rotateX((15f).toRadians())// rotate it into a nice viewing angle
            .rotateY((-25f).toRadians())
            // calculate the scale, such that everything can be visible
            // half, because it's half the size, 1.05f for a small border
            .scale(1.05f * 0.5f)
    }

    fun Mesh.drawAssimp(
        aspectRatio: Float,
        comp: MeshComponentBase?,
        useMaterials: Boolean,
        centerMesh: Boolean,
        normalizeScale: Boolean
    ) = drawAssimp(
        createCameraMatrix(aspectRatio),
        createModelMatrix(), comp,
        useMaterials, centerMesh,
        normalizeScale
    )

    fun Mesh.drawAssimp(
        cameraMatrix: Matrix4f,
        modelMatrix: Matrix4x3f,
        comp: MeshComponentBase?,
        useMaterials: Boolean,
        centerMesh: Boolean,
        normalizeScale: Boolean
    ) {

        val shader = ECSShaderLib.pbrModelShader.value
        shader.use()

        if (normalizeScale || centerMesh) {
            if (normalizeScale) modelMatrix.scale(getScaleFromAABB(getBounds()))
            if (centerMesh) centerMesh(cameraMatrix, modelMatrix, this)
        }

        val materials0 = materials
        val materials1 = comp?.materials

        for (index in 0 until numMaterials) {
            val material =
                if (useMaterials) Materials.getMaterial(materials1, materials0, index)
                else Material.defaultMaterial
            val shader2 = material.shader?.value ?: shader
            bindShader(shader2, cameraMatrix, modelMatrix)
            material.bind(shader2)
            draw(null, shader2, index)
        }
    }

    fun bindShader(shader: Shader, cameraMatrix: Matrix4f, modelMatrix: Matrix4x3f) {
        shader.use()
        shader.v4f("tint", 1f)
        shader.v1b("hasAnimation", false)
        shader.m4x3("localTransform", modelMatrix)
        if (shader["invLocalTransform"] >= 0) {
            val tmp = JomlPools.mat4x3f.borrow()
            tmp.set(modelMatrix).invert()
            shader.m4x3("invLocalTransform", tmp)
        }
        shader.v1f("worldScale", 1f)
        GFXx3D.shader3DUniforms(shader, cameraMatrix, -1)
    }

    fun Collider.drawAssimp(
        stack: Matrix4f,
        localStack: Matrix4x3f?
    ) {
        drawShape()
        finishLines(stack, localStack)
    }

    fun Collider.findModelMatrix(
        cameraMatrix: Matrix4f,
        modelMatrix: Matrix4x3f,
        centerMesh: Boolean,
        normalizeScale: Boolean
    ): Matrix4x3f {
        if (normalizeScale || centerMesh) {
            val aabb = AABBd()
            fillSpace(Matrix4x3d(), aabb)
            if (normalizeScale) modelMatrix.scale(getScaleFromAABB(aabb))
            if (centerMesh) centerMesh(cameraMatrix, modelMatrix, this)
        }
        return modelMatrix
    }

    fun finishLines(cameraMatrix: Matrix4f, worldMatrix: Matrix4x3f? = null): Boolean {
        return if (LineBuffer.bytes.position() > 0) {
            if (worldMatrix == null) {
                LineBuffer.finish(cameraMatrix)
            } else {
                val m = JomlPools.mat4f.create()
                m.set(cameraMatrix)
                m.mul(worldMatrix)
                LineBuffer.finish(m)
                JomlPools.mat4f.sub(1)
            }
            true
        } else false
    }

    fun waitForTextures(comp: List<FileReference>, mesh: Mesh, srcFile: FileReference, callback: () -> Unit) {
        // wait for all textures
        iterateMaterials(comp, mesh.materials)
            .mapCallback<FileReference, List<FileReference>>(
                { _, ref, cb ->
                    listTextures(ref, cb)
                }, { res, err ->
                    err?.printStackTrace()
                    if (res != null) {
                        val textures = res.flatten().toHashSet()
                        removeMissingFiles(textures, srcFile)
                        waitForTextures(textures, callback)
                    } else {
                        callback()
                    }
                })
    }

    fun waitForTextures(mesh: Mesh, srcFile: FileReference, callback: () -> Unit) {
        waitForTextures(emptyList(), mesh, srcFile, callback)
    }

    fun removeMissingFiles(files: MutableSet<FileReference>, srcFile: FileReference) {
        files.removeAll { it == InvalidRef || isFileMissing(it, srcFile) }
    }

    private fun isFileMissing(it: FileReference, srcFile: FileReference): Boolean {
        return if (!it.exists) {
            LOGGER.warn("Missing file '$it' by '$srcFile'")
            true
        } else false
    }

    @Deprecated("Use async method")
    fun waitForMeshes(entity: Entity) {
        // wait for all textures
        entity.forAllComponentsInChildren(MeshComponentBase::class) {
            // does the CPU part -> not perfect, but maybe good enough
            it.getMesh()
        }
    }

    fun collectMaterials(entity: Entity, callback: Callback<HashSet<FileReference>>): HashSet<FileReference> {
        val materials = HashSet<FileReference>()
        entity.forAllComponentsInChildren(MeshComponentBase::class) { comp ->
            val mesh = comp.getMesh()
            if (mesh != null) {
                materials += iterateMaterials(comp.materials, mesh.materials)
            } else warnMissingMesh(comp, null)
        }
        return materials
    }

    fun warnMissingMesh(comp: MeshComponentBase, mesh: Mesh?) {
        val msg = if (mesh == null) {
            if (comp is MeshComponent) {
                if (comp.meshFile == InvalidRef)
                    "${comp.className} '${comp.name}' is missing path (${comp.meshFile})"
                else
                    "Mesh '${comp.name}'/'${comp.meshFile}' is missing from ${comp.className}"
            } else "Missing mesh $comp, ${comp::class.simpleName} from ${comp.className}"
        } else "Missing positions ${comp.getMesh()}"
        LOGGER.warn(msg)
    }

    fun listTextures(matRef: FileReference): List<FileReference> {
        if (matRef == InvalidRef) return emptyList()
        val material = MaterialCache[matRef]
        if (material == null) warnMissingMaterial(matRef)
        return material?.listTextures() ?: emptyList()
    }

    fun listTextures(matRef: FileReference?, callback: Callback<List<FileReference>>) {
        if (matRef == null || matRef == InvalidRef) return callback.ok(emptyList())
        MaterialCache.getAsync(matRef) { material, err ->
            callback.ok(material?.listTextures() ?: emptyList())
            err?.printStackTrace()
        }
    }

    fun listTextures(matRefs: List<FileReference>, srcFile: FileReference, callback: (HashSet<FileReference>) -> Unit) {
        return matRefs.mapCallback<FileReference, List<FileReference>>(
            { _, matRef, cb -> listTextures(matRef, cb) },
            { res, err ->
                val textures = HashSet((res ?: emptyList()).flatten())
                removeMissingFiles(textures, srcFile)
                callback(textures)
                err?.printStackTrace()
            })
    }

    private fun warnMissingMaterial(materialReference: FileReference) {
        LOGGER.warn("Missing material '{}'", materialReference)
    }

    fun waitForTextures(material: Material, srcFile: FileReference, callback: () -> Unit) {
        // listing all textures
        // does not include personal materials / shaders...
        val textures = material.listTextures().toHashSet()
        removeMissingFiles(textures, srcFile)
        waitForTextures(textures, callback)
    }

    private const val TEXTURE_TIMEOUT = 25000L

    fun doneCondition(textures: Collection<FileReference>, endTime: Long): Boolean {
        // textures may be missing; just ignore them, if they cannot be read
        return if (Time.gameTimeN > endTime) {
            warnMissingTextures(textures)
            true
        } else {
            hasLoadedAllTextures(textures)
        }
    }

    fun getEndTime(): Long {
        return Time.gameTimeN + TEXTURE_TIMEOUT * Maths.MILLIS_TO_NANOS
    }

    private fun hasLoadedAllTextures(textureSources: Collection<FileReference>): Boolean {
        // all images should be requested every time, so we can load them in parallel
        return textureSources.all { src ->
            TextureCache.hasImageOrCrashed(src, TEXTURE_TIMEOUT, true)
        }
    }

    private fun warnMissingTextures(textureSources: Collection<FileReference>) {
        val timeout = TEXTURE_TIMEOUT
        for (src in textureSources) {
            if (!TextureCache.hasImageOrCrashed(src, timeout, true)) {
                LOGGER.warn("Missing texture $src")
            }
        }
    }

    fun waitForTextures(textureSources: Collection<FileReference>, callback: () -> Unit) {
        val endTime = getEndTime()
        Sleep.waitUntil(true, {
            doneCondition(textureSources, endTime)
        }, callback)
    }
}