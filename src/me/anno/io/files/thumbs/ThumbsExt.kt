package me.anno.io.files.thumbs

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.*
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
            if (normalizeScale) modelMatrix.scale(getScaleFromAABB(getBounds()))
            if (centerMesh) centerMesh(cameraMatrix, modelMatrix, this)
        }

        val materials0 = materials
        val materials1 = comp?.materials

        if (useMaterials && (materials0.isNotEmpty() || !materials1.isNullOrEmpty())) {
            for (index in materials0.indices) {
                val m0 = materials1?.getOrNull(index)?.nullIfUndefined()
                val m1 = m0 ?: materials0.getOrNull(index)
                val material = MaterialCache[m1, Material.defaultMaterial]
                val shader2 = material.shader?.value ?: shader
                bindShader(shader2, cameraMatrix, modelMatrix)
                material.bind(shader2)
                draw(shader2, index)
            }
        } else {
            bindShader(shader, cameraMatrix, modelMatrix)
            val material = Material.defaultMaterial
            material.bind(shader)
            for (materialIndex in 0 until max(1, materials0.size)) {
                draw(shader, materialIndex)
            }
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

    fun waitForTextures(comp: MeshComponentBase, mesh: Mesh, srcFile: FileReference) {
        // wait for all textures
        val textures = HashSet<FileReference>()
        Thumbs.iterateMaterials(comp.materials, mesh.materials) { material ->
            textures += listTextures(material)
        }
        textures.removeAll { it == InvalidRef }
        textures.removeAll {
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
        textures.removeAll { it == InvalidRef }
        textures.removeAll {
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
        textures.removeAll { it == InvalidRef }
        textures.removeAll {
            if (!it.exists) {
                LOGGER.warn("Missing texture $it by $srcFile")
                true
            } else false
        }
        waitForTextures(textures)
    }

    fun waitForMeshes(entity: Entity) {
        // wait for all textures
        entity.forAllComponentsInChildren(MeshComponentBase::class) {
            // does the CPU part -> not perfect, but maybe good enough
            it.getMesh()
        }
    }

    fun collectTextures(entity: Entity, textures: MutableSet<FileReference>) {
        entity.forAllComponentsInChildren(MeshComponentBase::class) { comp ->
            val mesh = comp.getMesh()
            if (mesh != null) {
                Thumbs.iterateMaterials(comp.materials, mesh.materials) { material ->
                    textures += listTextures(material)
                }
            } else warnMissingMesh(comp, null)
        }
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
                    .filter { !TextureCache.hasImageOrCrashed(it, timeout, true) }
                    .forEach { LOGGER.warn("Missing texture $it") }
                true
            } else {
                // all images should be requested every time, so we can load them in parallel
                var hasAll = true
                for (texture in textures) {
                    if (!TextureCache.hasImageOrCrashed(texture, timeout, true)) {
                        hasAll = false
                    }
                }
                hasAll
            }
        }
    }
}