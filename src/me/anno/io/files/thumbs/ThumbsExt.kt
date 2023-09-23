package me.anno.io.files.thumbs

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MaterialCache
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.shader.Shader
import me.anno.image.ImageGPUCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths
import me.anno.mesh.MeshData
import me.anno.mesh.MeshUtils.centerMesh
import me.anno.mesh.MeshUtils.getScaleFromAABB
import me.anno.utils.Sleep
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import kotlin.math.max

object ThumbsExt {

    private val LOGGER = LogManager.getLogger(ThumbsExt::class)

    fun createCameraMatrix(aspectRatio: Float): Matrix4f {
        val stack = Matrix4f()
        Perspective.setPerspective(stack, 0.7f, aspectRatio, 0.001f, 10f, 0f, 0f)
        return stack
    }

    fun createModelMatrix(): Matrix4x3f {
        val stack = Matrix4x3f()
        stack.translate(0f, 0f, -1f)// move the camera back a bit
        stack.rotateX((15f).toRadians())// rotate it into a nice viewing angle
        stack.rotateY((-25f).toRadians())
        // calculate the scale, such that everything can be visible
        // half, because it's half the size, 1.05f for a small border
        stack.scale(1.05f * 0.5f)
        return stack
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
            if (normalizeScale) modelMatrix.scale(getScaleFromAABB(aabb))
            if (centerMesh) centerMesh(cameraMatrix, modelMatrix, this)
        }

        val materials0 = materials
        val materials1 = comp?.materials

        if (useMaterials && (materials0.isNotEmpty() || !materials1.isNullOrEmpty())) {
            for (index in materials0.indices) {
                val m0 = materials1?.getOrNull(index)?.nullIfUndefined()
                val m1 = m0 ?: materials0.getOrNull(index)
                val material = MaterialCache[m1, Mesh.defaultMaterial]
                val shader2 = material.shader?.value ?: shader
                bindShader(shader2, cameraMatrix, modelMatrix)
                material.bind(shader2)
                draw(shader2, index)
            }
        } else {
            bindShader(shader, cameraMatrix, modelMatrix)
            val material = Mesh.defaultMaterial
            material.bind(shader)
            for (materialIndex in 0 until max(1, materials0.size)) {
                draw(shader, materialIndex)
            }
        }
    }

    fun bindShader(shader: Shader, cameraMatrix: Matrix4f, modelMatrix: Matrix4x3f) {
        shader.use()
        shader.v4f("tint", -1)
        shader.v1b("hasAnimation", false)
        shader.m4x3("localTransform", modelMatrix)
        if (shader["invLocalTransform"] >= 0) {
            val tmp = JomlPools.mat4x3f.borrow()
            tmp.set(modelMatrix).invert()
            shader.m4x3("invLocalTransform", tmp)
        }
        shader.v1f("worldScale", 1f)
        GFXx3D.shader3DUniforms(shader, cameraMatrix, -1)
        GFXx3D.uploadAttractors0(shader)
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

    fun waitForTextures(comp: MeshComponentBase, mesh: Mesh, srcFile: FileReference) {
        // wait for all textures
        val textures = HashSet<FileReference>()
        Thumbs.iterateMaterials(comp.materials, mesh.materials) { material ->
            textures += listTextures(material)
        }
        textures.removeIf { it == InvalidRef }
        textures.removeIf {
            if (!it.exists) {
                LOGGER.warn("Missing texture $it by $srcFile")
                true
            } else false
        }
        waitForTextures(textures)
    }

    fun waitForTextures(mesh: Mesh, file: FileReference) {
        // wait for all textures
        val textures = HashSet<FileReference>()
        for (material in mesh.materials) {
            textures += listTextures(material)
        }
        textures.removeIf { it == InvalidRef }
        textures.removeIf {
            if (!it.exists) {
                LOGGER.warn("Missing texture $it by $file")
                true
            } else false
        }
        waitForTextures(textures)
    }

    fun waitForTextures(entity: Entity, srcFile: FileReference) {
        // wait for all textures
        val textures = HashSet<FileReference>()
        collectTextures(entity, textures)
        textures.removeIf { it == InvalidRef }
        textures.removeIf {
            if (!it.exists) {
                LOGGER.warn("Missing texture $it by $srcFile")
                true
            } else false
        }
        waitForTextures(textures)
    }

    fun waitForMeshes(entity: Entity) {
        // wait for all textures
        entity.forAll {
            if (it is MeshComponentBase) {
                // does the CPU part -> not perfect, but maybe good enough
                it.ensureBuffer()
            }
        }
    }

    fun collectTextures(entity: Entity, textures: MutableSet<FileReference>) {
        for (comp in entity.getComponentsInChildren(MeshComponentBase::class, false)) {
            val mesh = comp.getMesh()
            if (mesh == null) {
                MeshData.warnMissingMesh(comp, null)
                continue
            }
            Thumbs.iterateMaterials(comp.materials, mesh.materials) { material ->
                textures += listTextures(material)
            }
        }
    }

    fun listTextures(materialReference: FileReference): List<FileReference> {
        if (materialReference == InvalidRef) return emptyList()
        val material = MaterialCache[materialReference]
        if (material == null) LOGGER.warn("Missing material '$materialReference'")
        return material?.listTextures() ?: emptyList()
    }

    fun waitForTextures(materials: List<Material>, timeout: Long = 25000) {
        // listing all textures
        // does not include personal materials / shaders...
        val textures = ArrayList<FileReference>()
        for (material in materials) {
            textures += material.listTextures()
        }
        waitForTextures(textures, timeout)
    }

    fun waitForTextures(material: Material, timeout: Long = 25000) {
        // listing all textures
        // does not include personal materials / shaders...
        val textures = material.listTextures().filter { it != InvalidRef && it.exists }
        waitForTextures(textures, timeout)
    }

    fun waitForTextures(textures: Collection<FileReference>, timeout: Long = 25000) {
        // 25s timeout, because unzipping all can take its time
        // wait for textures
        if (textures.isEmpty()) return
        val endTime = Time.gameTimeN + timeout * Maths.MILLIS_TO_NANOS
        Sleep.waitForGFXThread(true) {
            if (Time.gameTimeN > endTime) {
                // textures may be missing; just ignore them, if they cannot be read
                textures
                    .filter { !ImageGPUCache.hasImageOrCrashed(it, timeout, true) }
                    .forEach { LOGGER.warn("Missing texture $it") }
                true
            } else textures.all { ImageGPUCache.hasImageOrCrashed(it, timeout, true) }
        }
    }
}