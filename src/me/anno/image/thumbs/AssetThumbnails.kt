package me.anno.image.thumbs

import me.anno.config.DefaultConfig
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.anim.SkeletonCache
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.light.sky.SkyboxBase
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.SimpleMesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.projects.GameEngineProject
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView0
import me.anno.engine.ui.render.Renderers
import me.anno.gpu.CullMode
import me.anno.gpu.DitherMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.graph.hdb.HDBKey
import me.anno.image.thumbs.AssetThumbHelper.drawAssimp
import me.anno.image.thumbs.AssetThumbHelper.findModelMatrix
import me.anno.image.thumbs.AssetThumbHelper.listTextures
import me.anno.image.thumbs.AssetThumbHelper.removeTextures
import me.anno.image.thumbs.AssetThumbHelper.waitForTextures
import me.anno.io.saveable.Saveable
import me.anno.io.files.FileReference
import me.anno.utils.InternalAPI
import me.anno.utils.Warning
import me.anno.utils.hpc.ThreadLocal2
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.Callback
import me.anno.utils.structures.lists.Lists
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.min

@InternalAPI
object AssetThumbnails {

    private val LOGGER = LogManager.getLogger(AssetThumbnails::class)

    fun register() {
        Thumbs.registerFileExtensions("json", AssetThumbnails::generateAssetFrame)
        Thumbs.registerSignatures("vox,maya", AssetThumbnails::generateAssetFrame)
    }

    // todo exclude lights from AABB calculations for thumbnails?
    //  (except when only having lights, then add a floor)
    @JvmStatic
    fun generateEntityFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        scene: Entity,
        callback: Callback<ITexture2D>
    ) {
        // todo draw gui (colliders), entity positions
        val sceneMaterials = AssetThumbHelper.collectMaterials(scene)
        val sceneTextures = HashSet(sceneMaterials.flatMap(AssetThumbHelper::listTextures))
        removeTextures(sceneTextures, srcFile)
        for (i in 0 until 3) { // make sure both are loaded
            AssetThumbHelper.waitForMeshes(scene)
            waitForTextures(sceneTextures)
        }
        scene.validateTransform()
        scene.getBounds()
        val bounds = scene.aabb
        ThumbsRendering.renderToImage(
            srcFile, false,
            dstFile, true,
            Renderers.previewRenderer,
            true, callback, size, size
        ) {
            GFX.checkIsGFXThread()
            val rv = rv
            val cam = rv.editorCamera
            if (!bounds.isEmpty() && bounds.volume.isFinite()) {
                rv.radius = 100.0 * max(bounds.centerX, max(bounds.centerY, bounds.centerZ))
                rv.orbitCenter.set(bounds.centerX, bounds.centerY, bounds.centerZ)
                rv.updateEditorCameraTransform()
                rv.setRenderState()
                // calculate ideal transform like previously
                // for that, calculate bounds on screen, then rescale/recenter
                val visualBounds = AABBf()
                val tmp = Matrix4x3d()
                val totalMatrix = Matrix4f()
                val vec0 = Vector3f()
                val cameraMatrix = Matrix4x3d(rv.editorCamera.transform!!.globalTransform)

                fun addMesh(mesh: IMesh?, transform: Transform) {
                    if (mesh !is Mesh) return
                    // calculate transform
                    val pos = mesh.positions ?: return
                    val modelMatrix = transform.globalTransform
                    totalMatrix.set(cameraMatrix.mul(modelMatrix, tmp))
                    // to do for performance first check if bounds would be increasing the size
                    for (i in pos.indices step 3) {
                        vec0.set(pos, i).mulProject(totalMatrix)
                        visualBounds.union(vec0)
                    }
                }
                scene.forAll {
                    when (it) {
                        is MeshComponentBase -> addMesh(it.getMesh(), it.transform ?: scene.transform)
                        is MeshSpawner -> it.forEachMesh { mesh, _, transform ->
                            addMesh(mesh, transform)
                        }
                    }
                }
                rv.radius = 400.0 * max(visualBounds.deltaX, visualBounds.deltaY).toDouble()
            } else {
                rv.radius = 1.0
                rv.orbitCenter.set(0.0)
            }
            rv.near = rv.radius * 0.01
            rv.far = rv.radius * 2.0
            rv.updateEditorCameraTransform()
            rv.setRenderState()
            rv.prepareDrawScene(size, size, 1f, cam, false)
            // don't use EditorState
            GFXState.ditherMode.use(DitherMode.DITHER2X2) {
                rv.pipeline.clear()
                rv.pipeline.fill(scene)
                rv.setRenderState()
                rv.pipeline.singlePassWithoutSky(true)
            }
        }
    }

    @JvmStatic
    fun generateMaterialFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        materials: List<FileReference>,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        listTextures(materials, srcFile) { textures ->
            waitForTextures(textures) {
                ThumbsRendering.renderMultiWindowImage(
                    srcFile, dstFile, materials.size, size, false,
                    Renderers.previewRenderer, callback
                ) { it, _ ->
                    GFXState.blendMode.use(BlendMode.DEFAULT) {
                        GFX.checkIsGFXThread()
                        val mesh = SimpleMesh.sphereMesh
                        mesh.material = materials[it]
                        mesh.drawAssimp(
                            matCameraMatrix,
                            matModelMatrix,
                            null,
                            useMaterials = true,
                            centerMesh = false,
                            normalizeScale = false
                        )
                    }
                }
            }
        }
    }

    private val rv by lazy {
        val rv = RenderView0(PlayMode.EDITING, DefaultConfig.style)
        rv.enableOrbiting = true
        rv.editorCamera.fovY = 10f.toRadians()
        rv.orbitRotation.identity()
            .rotateY(25.0.toRadians())
            .rotateX((-15.0).toRadians())
        rv.pipeline.defaultStage.cullMode = CullMode.BOTH
        val sky = SkyboxBase()
        rv.pipeline.skybox = sky
        rv
    }

    @JvmStatic
    private fun generateColliderFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        collider: Collider,
        callback: Callback<ITexture2D>
    ) {
        Warning.unused(srcFile)
        val cameraMatrix = AssetThumbHelper.createCameraMatrix(1f)
        val modelMatrix = AssetThumbHelper.createModelMatrix()
        collider.findModelMatrix(cameraMatrix, modelMatrix, centerMesh = true, normalizeScale = true)
        ThumbsRendering.renderToImage(
            srcFile,
            false,
            dstFile,
            true,
            Renderers.previewRenderer,
            true,
            callback,
            size,
            size
        ) {
            collider.drawAssimp(cameraMatrix, modelMatrix)
        }
    }

    @JvmStatic
    private fun generateMeshFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        mesh: Mesh,
        callback: Callback<ITexture2D>
    ) {
        mesh.checkCompleteness()
        mesh.ensureBuffer()
        waitForTextures(mesh, srcFile) {
            ThumbsRendering.renderToImage(
                srcFile, false,
                dstFile, true,
                Renderers.simpleNormalRenderer,
                true, callback, size, size
            ) {
                GFXState.cullMode.use(CullMode.BOTH) {
                    mesh.drawAssimp(
                        1f, null,
                        useMaterials = true,
                        centerMesh = true,
                        normalizeScale = true
                    )
                }
            }
        }
    }

    @JvmStatic
    private fun generateMeshFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        comp: MeshComponentBase,
        callback: Callback<ITexture2D>
    ) {
        val mesh = comp.getMesh() as? Mesh ?: return
        mesh.checkCompleteness()
        mesh.ensureBuffer()
        waitForTextures(comp, mesh, srcFile) {
            ThumbsRendering.renderToImage(
                srcFile, false,
                dstFile, true,
                Renderers.simpleNormalRenderer,
                true, callback, size, size
            ) {
                GFXState.cullMode.use(CullMode.BOTH) {
                    mesh.drawAssimp(
                        1f, comp,
                        useMaterials = true,
                        centerMesh = true,
                        normalizeScale = true
                    )
                }
            }
        }
    }

    @JvmStatic
    private fun generateMeshFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        comp: Renderable,
        callback: Callback<ITexture2D>
    ) {
        if (comp is Component) {
            val entity = Entity()
            entity.add(comp.clone() as Component)
            generateEntityFrame(srcFile, dstFile, size, entity, callback)
        } else {
            LOGGER.warn("Cannot render ${comp::class}")
        }
    }

    private val matCameraMatrix = AssetThumbHelper.createCameraMatrix(1f)
    private val matModelMatrix = AssetThumbHelper.createModelMatrix().scale(0.62f)

    @JvmStatic
    fun generateMaterialFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        material: Material,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        waitForTextures(material, srcFile) {
            ThumbsRendering.renderToImage(
                srcFile, false, dstFile, true, Renderers.previewRenderer,
                true, callback, size, size
            ) {
                GFXState.blendMode.use(BlendMode.DEFAULT) {
                    val mesh = SimpleMesh.sphereMesh
                    mesh.material = srcFile
                    mesh.drawAssimp(
                        matCameraMatrix,
                        matModelMatrix,
                        null,
                        useMaterials = true,
                        centerMesh = false,
                        normalizeScale = false
                    )
                }
            }
        }
    }

    @JvmStatic
    fun generateSkeletonFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        skeleton: Skeleton,
        size: Int,
        callback: Callback<ITexture2D>
    ) {

        // todo the transform can be different from the original...
        // todo can we show it with the default transform somehow?

        // on working skeletons, this works
        // therefore it probably is correct

        // transform bones into image
        val bones = skeleton.bones
        if (bones.isEmpty()) return
        val mesh = Mesh()

        // in a tree with N nodes, there is N-1 lines
        val positions = FloatArray((bones.size - 1) * Skeleton.boneMeshVertices.size)
        val bonePositions = bones.map { it.bindPosition }
        Skeleton.generateSkeleton(bones, bonePositions, positions, null)
        mesh.positions = positions
        generateMeshFrame(srcFile, dstFile, size, mesh, callback)
    }

    @JvmStatic
    private fun generateAnimationFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        animation: Animation,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        val skeleton = SkeletonCache[animation.skeleton] ?: return
        val mesh = Mesh()
        val duration = animation.duration
        val hasMotion = duration > 0.0
        val count = if (hasMotion) 6 else 1
        val dt = if (hasMotion) animation.numFrames.toFloat() / count else 0f
        val bones = skeleton.bones
        val meshVertices = Texture2D.floatArrayPool[bones.size * Skeleton.boneMeshVertices.size, false, true]
        mesh.positions = meshVertices
        val (skinningMatrices, animPositions) = threadLocalBoneMatrices.get()
        ThumbsRendering.renderMultiWindowImage(srcFile, dstFile, count, size,
            true, Renderers.simpleNormalRenderer, { it, e ->
                callback.call(it, e)
                Texture2D.floatArrayPool.returnBuffer(meshVertices)
                mesh.destroy()
            }) { it, aspect ->
            val frameIndex = it * dt
            // generate the matrices
            animation.getMatrices(frameIndex, skinningMatrices)
            // apply the matrices to the bone positions
            for (i in 0 until min(animPositions.size, bones.size)) {
                val position = animPositions[i].set(bones[i].bindPosition)
                skinningMatrices[i].transformPosition(position)
            }
            Skeleton.generateSkeleton(bones, animPositions, meshVertices, null)
            mesh.invalidateGeometry()
            // draw the skeleton in that portion of the frame
            mesh.ensureBuffer()
            mesh.drawAssimp(
                aspect, null,
                useMaterials = false,
                centerMesh = true,
                normalizeScale = true
            )
        }
    }

    private val threadLocalBoneMatrices = ThreadLocal2 {
        val boneCount = 256
        val skinningMatrices = Lists.createArrayList(boneCount) { Matrix4x3f() }
        val animPositions = Lists.createArrayList(boneCount) { Vector3f() }
        skinningMatrices to animPositions
    }

    @JvmStatic
    fun drawAnimatedSkeleton(
        animation: Animation,
        frameIndex: Float,
        aspect: Float
    ) {
        // todo center on bounds by all frames combined
        val skeleton = SkeletonCache[animation.skeleton] ?: return
        val (skinningMatrices, _) = threadLocalBoneMatrices.get()
        // generate the matrices
        animation.getMatrices(frameIndex, skinningMatrices)
        drawAnimatedSkeleton(skeleton, skinningMatrices, aspect)
    }

    @JvmStatic
    private fun drawAnimatedSkeleton(
        skeleton: Skeleton, skinningMatrices: List<Matrix4x3f>,
        aspect: Float
    ) {
        buildAnimatedSkeleton(skeleton, skinningMatrices) { mesh ->
            mesh.drawAssimp(
                aspect, null,
                useMaterials = false,
                centerMesh = true,
                normalizeScale = true
            )
        }
    }

    @JvmStatic
    fun buildAnimatedSkeleton(
        skeleton: Skeleton,
        skinningMatrices: List<Matrix4x3f>,
        useGeneratedMesh: (Mesh) -> Unit,
    ) {
        val mesh = Mesh()
        val bones = skeleton.bones
        val (_, animPositions) = threadLocalBoneMatrices.get()
        val numBones = min(animPositions.size, bones.size)
        // apply the matrices to the bone positions
        for (i in 0 until numBones) {
            val position = animPositions[i].set(bones[i].bindPosition)
            skinningMatrices[i].transformPosition(position)
        }
        val meshVertices = Texture2D.floatArrayPool[numBones * Skeleton.boneMeshVertices.size, false, true]
        mesh.positions = meshVertices
        Skeleton.generateSkeleton(bones, animPositions, meshVertices, null)
        mesh.invalidateGeometry()
        // draw the skeleton in that portion of the frame
        mesh.ensureBuffer()
        useGeneratedMesh(mesh)
        Texture2D.floatArrayPool.returnBuffer(meshVertices)
        mesh.destroy()
    }

    @JvmStatic
    fun generateAssetFrame(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>
    ) {
        PrefabCache.getPrefabInstanceAsync(srcFile) { prefab, err ->
            if (prefab != null) generateAssetFrame(prefab, srcFile, dstFile, size, callback)
            else callback.err(err)
        }
    }

    @JvmStatic
    fun generateAssetFrame(
        asset: Saveable,
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        when (asset) {
            is Mesh -> generateMeshFrame(srcFile, dstFile, size, asset, callback)
            is Material -> generateMaterialFrame(srcFile, dstFile, asset, size, callback)
            is Skeleton -> generateSkeletonFrame(srcFile, dstFile, asset, size, callback)
            is Animation -> generateAnimationFrame(srcFile, dstFile, asset, size, callback)
            is Entity -> generateEntityFrame(srcFile, dstFile, size, asset, callback)
            is MeshComponentBase -> generateMeshFrame(srcFile, dstFile, size, asset, callback)
            is Renderable -> generateMeshFrame(srcFile, dstFile, size, asset, callback)
            is Collider -> generateColliderFrame(srcFile, dstFile, size, asset, callback)
            is Component -> {
                val gt = JomlPools.mat4x3d.borrow()
                val ab = JomlPools.aabbd.borrow()
                if (asset.fillSpace(gt, ab)) {
                    // todo render debug ui :)
                    LOGGER.warn("UI rendering for components not yet implemented")
                }
            }
            is Prefab -> {
                val instance = asset.getSampleInstance()
                generateAssetFrame(instance, srcFile, dstFile, size, callback)
            }
            // todo thumbnails for graphs
            is GameEngineProject -> {}
            else -> {
                // todo can we create a json preview or sth like that?
                LOGGER.warn("Unknown item from prefab: ${asset.className}")
            }
            // is Transform -> todo show transform for Rem's Studio
        }
    }
}