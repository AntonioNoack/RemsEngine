package me.anno.image.thumbs

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.ecs.components.mesh.material.Materials
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.pipeline.Pipeline
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
import me.anno.utils.structures.lists.Lists.flatten
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Matrix4x3f

/**
 * extra functions for thumbnail generator, because that class is growing too big
 * */
object AssetThumbHelper {

    val unityExtensions = "mat,prefab,unity,asset,controller,meta"
    val unityExtensions1 = unityExtensions.split(',')
    private val LOGGER = LogManager.getLogger(AssetThumbHelper::class)

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

    fun IMesh.drawAssimp(
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

        val materials0 = cachedMaterials
        val materials1 = comp?.cachedMaterials

        for (index in 0 until numMaterials) {
            val material =
                if (useMaterials) Materials.getMaterial(materials1, materials0, index)
                else Material.defaultMaterial
            val shader2 = material.shader?.value ?: shader
            bindShader(shader2, cameraMatrix, modelMatrix)
            material.bind(shader2)
            draw(null, shader2, index, false)
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
        GFXx3D.shader3DUniforms(shader, cameraMatrix, -1)
    }

    fun Collider.drawAssimp(
        pipeline: Pipeline,
        stack: Matrix4f,
        localStack: Matrix4x3f?
    ) {
        drawShape(pipeline)
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
            fillSpace(Matrix4x3(), aabb)
            if (normalizeScale) modelMatrix.scale(getScaleFromAABB(aabb))
            if (centerMesh) centerMesh(cameraMatrix, modelMatrix, this)
        }
        return modelMatrix
    }

    fun finishLines(cameraMatrix: Matrix4f, worldMatrix: Matrix4x3f? = null): Boolean {
        return if (LineBuffer.hasLinesToDraw()) {
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

    fun listTextures(matRef: FileReference): List<FileReference> {
        if (matRef == InvalidRef) return emptyList()
        val material = MaterialCache.getEntry(matRef).waitFor()
        if (material == null) warnMissingMaterial(matRef)
        return material?.listTextures() ?: emptyList()
    }

    fun listTextures(matRef: FileReference?, callback: Callback<List<FileReference>>) {
        if (matRef == null || matRef == InvalidRef) return callback.ok(emptyList())
        MaterialCache.getEntry(matRef).waitFor { material, err ->
            callback.ok(material?.listTextures() ?: emptyList())
            err?.printStackTrace()
        }
    }

    fun listTextures(matRefs: List<FileReference>, srcFile: FileReference, callback: (HashSet<FileReference>) -> Unit) {
        return matRefs.mapCallback(
            { _, matRef, cb -> listTextures(matRef, cb) },
            { res: List<List<FileReference>>?, err ->
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
        return if (Time.nanoTime > endTime) {
            warnMissingTextures(textures)
            true
        } else {
            hasLoadedAllTextures(textures)
        }
    }

    fun getEndTime(): Long {
        return Time.nanoTime + TEXTURE_TIMEOUT * Maths.MILLIS_TO_NANOS
    }

    private fun hasLoadedAllTextures(textureSources: Collection<FileReference>): Boolean {
        // all images should be requested every time, so we can load them in parallel
        return textureSources.all { src ->
            TextureCache.hasImageOrCrashed(src, TEXTURE_TIMEOUT)
        }
    }

    private fun warnMissingTextures(textureSources: Collection<FileReference>) {
        val timeout = TEXTURE_TIMEOUT
        for (src in textureSources) {
            if (!TextureCache.hasImageOrCrashed(src, timeout)) {
                LOGGER.warn("Missing texture $src")
            }
        }
    }

    fun waitForTextures(textureSources: Collection<FileReference>, callback: () -> Unit) {
        val endTime = getEndTime()
        Sleep.waitUntil("AssetThumbHelper:waitForTextures", true, {
            doneCondition(textureSources, endTime)
        }, callback)
    }
}