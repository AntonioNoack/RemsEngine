package me.anno.image.thumbs

import me.anno.Time
import me.anno.cache.AsyncCacheData
import me.anno.cache.IgnoredException
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.anim.Skeleton.Companion.boneMeshVertices
import me.anno.ecs.components.anim.Skeleton.Companion.generateSkeleton
import me.anno.ecs.components.anim.SkeletonCache
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.light.sky.SkyboxBase
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.ecs.components.mesh.shapes.UVSphereModel
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabReadable
import me.anno.engine.projects.GameEngineProject
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView0
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.engine.ui.render.Renderers.simpleNormalRenderer
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.DitherMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.addGPUTask
import me.anno.gpu.GFX.isGFXThread
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.DrawTextures.drawTransparentBackground
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.renderer.Renderer.Companion.colorRenderer
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureReader
import me.anno.graph.hdb.ByteSlice
import me.anno.graph.hdb.HDBKey
import me.anno.graph.hdb.HDBKey.Companion.InvalidKey
import me.anno.graph.hdb.HierarchicalDatabase
import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.image.ImageReadable
import me.anno.image.ImageReader
import me.anno.image.ImageScale.scaleMax
import me.anno.image.ImageTransform
import me.anno.image.hdr.HDRReader
import me.anno.image.thumbs.ThumbsExt.createCameraMatrix
import me.anno.image.thumbs.ThumbsExt.createModelMatrix
import me.anno.image.thumbs.ThumbsExt.drawAssimp
import me.anno.image.thumbs.ThumbsExt.findModelMatrix
import me.anno.image.thumbs.ThumbsExt.unityExtensions
import me.anno.image.thumbs.ThumbsExt.waitForMeshes
import me.anno.image.thumbs.ThumbsExt.waitForTextures
import me.anno.io.MediaMetadata.Companion.getMeta
import me.anno.io.Saveable
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.InnerStreamFile
import me.anno.io.files.inner.temporary.InnerTmpFile
import me.anno.maths.Maths.clamp
import me.anno.utils.Color.black
import me.anno.utils.OS
import me.anno.utils.Sleep.waitForGFXThread
import me.anno.utils.Sleep.waitForGFXThreadUntilDefined
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.Sleep.waitUntilDefined
import me.anno.utils.Warning.unused
import me.anno.utils.hpc.ThreadLocal2
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.Callback
import me.anno.utils.structures.Iterators.firstOrNull
import me.anno.utils.structures.Iterators.subList
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.InputStreams.readNBytes2
import me.anno.utils.types.Strings.getImportType
import me.anno.video.VideoCache.getVideoFrame
import me.anno.video.formats.gpu.GPUFrame
import net.boeckling.crc.CRC64
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Vector3f
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * creates and caches small versions of image and video resources
 *
 * // todo we have a race-condition issue: sometimes, matrices are transformed incorrectly
 * */
object Thumbs {

    private val LOGGER = LogManager.getLogger(Thumbs::class)

    private val folder = ConfigBasics.cacheFolder.getChild("thumbs")

    private val hdb = HierarchicalDatabase(
        "Thumbs", folder, 5_000_000, 10_000L,
        2 * 7 * 24 * 64 * 64 * 1000L
    )

    private val sizes = intArrayOf(32, 64, 128, 256, 512)

    private val neededSizes = IntArray(sizes.last() + 1)
    private const val timeout = 5000L

    var useCacheFolder = true

    // todo choose jpg/png depending on where alpha is present;
    //  use webp if possible
    private const val destinationFormat = "png"

    val sphereMesh = UVSphereModel.createUVSphere(30, 30)

    init {
        var index = 0
        for (size in sizes) {
            while (index <= size) {
                neededSizes[index++] = size
            }
        }
    }

    @JvmStatic
    fun invalidate(file: FileReference, neededSize: Int) {
        if (file == InvalidRef) return
        val size = getSize(neededSize)
        TextureCache.remove { key, _ ->
            key is ThumbnailKey && key.file == file && key.size == size
        }
        // invalidate database, too
        file.getFileHash { hash ->
            hdb.remove(getCacheKey(file, hash, size))
        }
    }

    @JvmStatic
    fun invalidate(file: FileReference) {
        if (file == InvalidRef) return
        TextureCache.remove { key, _ ->
            key is ThumbnailKey && key.file == file
        }
        // invalidate database, too
        file.getFileHash { hash ->
            for (size in sizes) {
                hdb.remove(getCacheKey(file, hash, size))
            }
        }
    }

    @JvmStatic
    operator fun get(file: FileReference, neededSize: Int, async: Boolean): ITexture2D? {

        if (file == InvalidRef) return null
        if (file is ImageReadable) {
            return TextureCache[file, timeout, async]
        }

        // currently not supported
        if (file.isDirectory) return null

        // was deleted
        if (!file.exists) return null

        if (neededSize < 1) return null
        val size = getSize(neededSize)
        val lastModified = file.lastModified
        val key = ThumbnailKey(file, lastModified, size)

        val texture = TextureCache.getLateinitTextureLimited(key, timeout, async, 4) { callback ->
            generate0(key, callback)
        }?.value
        val value = when (texture) {
            is GPUFrame -> if (texture.wasCreated) texture else null
            is Texture2D -> if (texture.wasCreated && !texture.isDestroyed) texture else null
            else -> texture
        }
        if (value != null) return value
        // return lower resolutions, if they are available
        var size1 = size shr 1
        while (size1 >= sizes.first()) {
            val key1 = ThumbnailKey(file, lastModified, size1)
            val gen = TextureCache.getEntryWithoutGenerator(key1, 50) as? AsyncCacheData<*>
            val tex = gen?.value as? ITexture2D
            if (tex != null) return tex
            size1 = size1 shr 1
        }
        return null
    }

    @JvmStatic
    private fun generate0(key: ThumbnailKey, callback: Callback<ITexture2D>) {
        val srcFile = key.file
        val size = key.size
        // if larger texture exists in cache, use it and scale it down
        val idx = sizes.indexOf(size) + 1
        for (i in idx until sizes.size) {
            val sizeI = sizes[i]
            val keyI = ThumbnailKey(key.file, key.lastModified, sizeI)
            val gen = TextureCache.getEntryWithoutGenerator(keyI, 500) as? AsyncCacheData<*>
            val tex = gen?.value as? ITexture2D
            if (tex != null && tex.isCreated()) {
                copyTexIfPossible(srcFile, size, tex, callback)
                return
            }
        }
        generate(srcFile, size, callback)
    }

    private fun copyTexIfPossible(
        srcFile: FileReference,
        size: Int,
        tex: ITexture2D,
        callback: Callback<ITexture2D>
    ) {
        val (w, h) = scaleMax(tex.width, tex.height, size)
        if (w < 2 || h < 2) return // cannot generate texture anyway, no point in loading it
        if (isGFXThread()) {
            if (tex is Texture2D && tex.isDestroyed) {
                // fail, we were too slow waiting for a GFX queue call
                generate(srcFile, size, callback)
            } else {
                val newTex = Texture2D(srcFile.name, w, h, 1)
                newTex.createRGBA()
                useFrame(newTex, 0) {
                    GFX.copy(tex)
                }
                callback.ok(newTex)
            }
        } else addGPUTask("Copy", size, size) {
            copyTexIfPossible(srcFile, size, tex, callback)
        }
    }

    @JvmStatic
    private fun FileReference.getFileHash(callback: (Long) -> Unit) {
        val hashReadLimit = 4096
        val length = this.length()
        val baseHash = lastModified xor (454781903L * length)
        if (!isDirectory && length > 0) {
            inputStream(hashReadLimit.toLong()) { reader, _ ->
                if (reader != null) {
                    val bytes = reader.readNBytes2(hashReadLimit, false)
                    reader.close()
                    callback(baseHash xor CRC64.fromInputStream(ByteArrayInputStream(bytes)))
                } else callback(baseHash)
            }
        } else callback(baseHash)
    }

    @JvmStatic
    private fun getCacheKey(srcFile: FileReference, hash: Long, size: Int): HDBKey {
        if (srcFile is InnerTmpFile) return InvalidKey
        val split = srcFile.absolutePath.split('/')
        return HDBKey(split.subList(0, max(split.lastIndex, 0)), hash * 31 + size)
    }

    @JvmStatic
    private fun getSize(neededSize: Int): Int {
        if (neededSize < 1) return 0
        return if (neededSize < neededSizes.size) {
            neededSizes[neededSize]
        } else sizes.last()
    }

    @JvmStatic
    private fun upload(
        srcFile: FileReference,
        checkRotation: Boolean,
        dst: Image,
        callback: Callback<ITexture2D>
    ) {
        val rotation = if (checkRotation) TextureReader.getRotation(srcFile) else null
        val texture = Texture2D(srcFile.name, dst.width, dst.height, 1)
        dst.createTexture(texture, sync = false, checkRedundancy = true) { tex, exc ->
            if (tex is Texture2D) tex.rotation = rotation
            callback.call(tex, exc)
        }
    }

    @JvmStatic
    fun saveNUpload(
        srcFile: FileReference,
        checkRotation: Boolean,
        dstKey: HDBKey,
        dst: Image,
        callback: Callback<ITexture2D>
    ) {
        if (dstKey != InvalidKey) {
            val bos = ByteArrayOutputStream()
            dst.write(bos, destinationFormat)
            bos.close()
            // todo we could skip toByteArray() by using our own type,
            //  and putting a ByteSlice
            val bytes = bos.toByteArray()
            hdb.put(dstKey, bytes)
        }
        upload(srcFile, checkRotation, dst, callback)
    }

    @JvmStatic
    fun transformNSaveNUpload(
        srcFile: FileReference,
        checkRotation: Boolean,
        src: Image,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        val sw = src.width
        val sh = src.height
        if (min(sw, sh) < 1) return

        // if it matches the size, just upload it
        // we have loaded it anyway already
        if (max(sw, sh) < size) {
            saveNUpload(srcFile, checkRotation, dstFile, src, callback)
            return
        }

        val (w, h) = scaleMax(sw, sh, size)
        if (min(w, h) < 1) return
        if (w == sw && h == sh) {
            saveNUpload(srcFile, checkRotation, dstFile, src, callback)
        } else {
            val dst = src.resized(w, h, false)
            saveNUpload(srcFile, checkRotation, dstFile, dst, callback)
        }
    }

    @JvmStatic
    fun renderToImage(
        src: FileReference,
        checkRotation: Boolean,
        dstFile: HDBKey,
        withDepth: Boolean,
        renderer: Renderer = colorRenderer,
        flipY: Boolean,
        callback: Callback<ITexture2D>,
        w: Int, h: Int, render: () -> Unit
    ) {
        if (isGFXThread()) {
            renderToImagePart2(
                src, checkRotation, dstFile, withDepth, renderer,
                flipY, callback, w, h, render
            )
        } else {
            addGPUTask("Thumbs.render($src)", w, h) {
                renderToImagePart2(
                    src, checkRotation, dstFile, withDepth, renderer,
                    flipY, callback, w, h, render
                )
            }
        }
    }

    @JvmStatic
    private fun renderToImagePart2(
        srcFile: FileReference,
        checkRotation: Boolean,
        dstFile: HDBKey,
        withDepth: Boolean,
        renderer: Renderer,
        flipY: Boolean,
        callback: Callback<ITexture2D>,
        w: Int, h: Int,
        render: () -> Unit
    ) {
        GFX.check()

        val depthType = if (withDepth) DepthBufferType.INTERNAL else DepthBufferType.NONE
        val renderTarget = if (GFX.maxSamples > 1 || useCacheFolder) {
            FBStack[srcFile.name, w, h, 4, false, 4, depthType] as Framebuffer
        } else {
            Framebuffer(srcFile.name, w, h, 1, 1, false, depthType)
        }

        renderPurely {
            if (!withDepth) {
                useFrame(w, h, false, renderTarget, colorRenderer) {
                    drawTransparentBackground(0, 0, w, h)
                }
            }
            useFrame(w, h, false, renderTarget, renderer) {
                if (withDepth) {
                    val depthMode = if (GFX.supportsClipControl) DepthMode.CLOSE
                    else DepthMode.FORWARD_CLOSE
                    GFXState.depthMode.use(depthMode) {
                        renderTarget.clearColor(0, true)
                        render()
                    }
                } else render()
            }
        }

        if (useCacheFolder) {
            val dst = renderTarget.createImage(flipY, true)
            saveNUpload(srcFile, checkRotation, dstFile, dst, callback)
        } else {// more efficient path, without useless GPU->CPU->GPU data transfer
            if (GFX.maxSamples > 1) {
                val newBuffer = Framebuffer(
                    srcFile.name, w, h, 1,
                    arrayOf(TargetType.UInt8x4), DepthBufferType.NONE
                )
                renderTarget.needsBlit = true // needed?
                useFrame(newBuffer) {
                    GFX.copy(renderTarget)
                }
                val texture = newBuffer.textures!![0]
                newBuffer.destroyExceptTextures(false)
                texture.rotation = if (flipY) flipYRot else null
                callback.ok(texture)
            } else {
                val texture = renderTarget.textures!![0]
                renderTarget.destroyExceptTextures(true)
                callback.ok(texture)
            }
        }
    }

    private val flipYRot = ImageTransform(mirrorHorizontal = false, mirrorVertical = true, 0)

    @JvmStatic
    private fun generateVideoFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>,
        wantedTime: Double
    ) {

        val meta = getMeta(srcFile, false) ?: throw RuntimeException("Could not load metadata for $srcFile")
        val mx = max(meta.videoWidth, meta.videoHeight)
        if (mx < size) {
            var sizeI = size shr 1
            while (mx < sizeI) sizeI = sizeI shr 1
            return generate(srcFile, sizeI, callback)
        }

        val scale = floor(max(meta.videoWidth, meta.videoHeight).toFloat() / size).toInt()

        val sw = meta.videoWidth / scale
        val sh = meta.videoHeight / scale

        val (w, h) = scaleMax(sw, sh, size)
        if (w < 2 || h < 2) return

        val fps = min(5.0, meta.videoFPS)
        val time = max(min(wantedTime, meta.videoDuration - 1 / fps), 0.0)
        val index = max(min((time * fps).roundToInt(), meta.videoFrameCount - 1), 0)

        val src = waitForGFXThreadUntilDefined(true) {
            getVideoFrame(srcFile, scale, index, 1, fps, 1000L, true)
        }

        waitForGFXThread(true) { src.isCreated }

        renderToImage(srcFile, false, dstFile, false, colorRenderer, true, callback, w, h) {
            drawTexture(0, 0, w, h, src)
        }
    }

    @JvmStatic
    private fun generatePrefabReadableFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        val data = waitUntilDefined(true) {
            PrefabCache[srcFile, true]
        }.getSampleInstance() as Entity
        generateEntityFrame(srcFile, dstFile, size, data, callback)
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
        for (i in 0 until 3) { // make sure both are loaded
            waitForMeshes(scene)
            waitForTextures(scene, srcFile)
        }
        scene.validateTransform()
        scene.getBounds()
        val bounds = scene.aabb
        renderToImage(srcFile, false, dstFile, true, previewRenderer, true, callback, size, size) {
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
            rv.prepareDrawScene(size, size, 1f, cam, cam, 0f, false)
            // don't use EditorState
            GFXState.ditherMode.use(DitherMode.DITHER2X2) {
                rv.pipeline.clear()
                rv.pipeline.fill(scene)
                rv.setRenderState()
                rv.pipeline.singlePassWithoutSky(true)
            }
        }
    }

    private val rv by lazy {
        val rv = RenderView0(PlayMode.EDITING, style)
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
        unused(srcFile)
        val cameraMatrix = createCameraMatrix(1f)
        val modelMatrix = createModelMatrix()
        collider.findModelMatrix(cameraMatrix, modelMatrix, centerMesh = true, normalizeScale = true)
        renderToImage(srcFile, false, dstFile, true, previewRenderer, true, callback, size, size) {
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
        waitForTextures(mesh, srcFile)
        // sometimes black: because of vertex colors, which are black
        // render everything without color
        renderToImage(srcFile, false, dstFile, true, simpleNormalRenderer, true, callback, size, size) {
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
        waitForTextures(comp, mesh, srcFile)
        // sometimes black: because of vertex colors, which are black
        // render everything without color
        renderToImage(srcFile, false, dstFile, true, simpleNormalRenderer, true, callback, size, size) {
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

    private val matCameraMatrix = createCameraMatrix(1f)
    private val matModelMatrix = createModelMatrix().scale(0.62f)

    @JvmStatic
    fun generateMaterialFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        material: Material,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        waitForTextures(material)
        renderToImage(
            srcFile, false, dstFile, true, previewRenderer,
            true, callback, size, size
        ) {
            GFXState.blendMode.use(BlendMode.DEFAULT) {
                val mesh = sphereMesh
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

    @JvmStatic
    private fun split(total: Int): Int {
        // smartly split space
        val maxRatio = 3
        if (total <= maxRatio) return GFXx2D.getSize(total, 1)
        val sqrt = sqrt(total.toFloat()).toInt()
        val minDivisor = max(1, ((total + maxRatio - 1) / maxRatio))
        for (partsY in sqrt downTo min(sqrt, minDivisor)) {
            if (total % partsY == 0) {
                // we found something good
                // partsX >= partsY, because partsY <= sqrt(total)
                val partsX = total / partsY
                return GFXx2D.getSize(partsX, partsY)
            }
        }
        // we didn't find a good split -> try again
        return split(total + 1)
    }

    @JvmStatic
    fun generateMaterialFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        materials: List<FileReference>,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        waitForTextures(materials.mapNotNull { MaterialCache[it] })
        renderMultiWindowImage(
            srcFile, dstFile, materials.size, size, false,
            previewRenderer, callback
        ) { it, _ ->
            GFXState.blendMode.use(BlendMode.DEFAULT) {
                GFX.checkIsGFXThread()
                val mesh = sphereMesh
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

    @JvmStatic
    private fun renderMultiWindowImage(
        srcFile: FileReference,
        dstFile: HDBKey,
        count: Int, size: Int,
        // whether the aspect ratio of the parts can be adjusted to keep the result quadratic
        // if false, the result will be rectangular
        changeSubFrameAspectRatio: Boolean,
        renderer0: Renderer,
        callback: Callback<ITexture2D>,
        drawFunction: (i: Int, aspect: Float) -> Unit
    ) {
        val split = split(count)
        val sx = getSizeX(split)
        val sy = getSizeY(split)
        val sizePerElement = size / sx
        val w = sizePerElement * sx
        val h = if (changeSubFrameAspectRatio) w else sizePerElement * sy
        val aspect = if (changeSubFrameAspectRatio) (w * sy).toFloat() / (h * sx) else 1f
        renderToImage(
            srcFile, false, dstFile, true,
            renderer0, true, callback, w, h
        ) {
            val frame = GFXState.currentBuffer
            val renderer = GFXState.currentRenderer
            for (i in 0 until count) {
                val ix = i % sx
                val iy = i / sx
                val x0 = ix * sizePerElement
                val y0 = (iy * h) / sy
                val y1 = (iy + 1) * h / sy
                useFrame(x0, y0, sizePerElement, y1 - y0, frame, renderer) {
                    drawFunction(i, aspect)
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
        val positions = FloatArray((bones.size - 1) * boneMeshVertices.size)
        val bonePositions = Array(bones.size) { bones[it].bindPosition }
        generateSkeleton(bones, bonePositions, positions, null)
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
        val meshVertices = Texture2D.floatArrayPool[bones.size * boneMeshVertices.size, false, true]
        mesh.positions = meshVertices
        val (skinningMatrices, animPositions) = threadLocalBoneMatrices.get()
        renderMultiWindowImage(srcFile, dstFile, count, size, true, simpleNormalRenderer, { it, e ->
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
            generateSkeleton(bones, animPositions, meshVertices, null)
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

    @JvmField
    val threadLocalBoneMatrices = ThreadLocal2 {
        val boneCount = 256
        val skinningMatrices = Array(boneCount) { Matrix4x3f() }
        val animPositions = Array(boneCount) { Vector3f() }
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
    fun drawAnimatedSkeleton(
        skeleton: Skeleton,
        skinningMatrices: Array<Matrix4x3f>,
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
        skinningMatrices: Array<Matrix4x3f>,
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
        val meshVertices = Texture2D.floatArrayPool[numBones * boneMeshVertices.size, false, true]
        mesh.positions = meshVertices
        generateSkeleton(bones, animPositions, meshVertices, null)
        mesh.invalidateGeometry()
        // draw the skeleton in that portion of the frame
        mesh.ensureBuffer()
        useGeneratedMesh(mesh)
        Texture2D.floatArrayPool.returnBuffer(meshVertices)
        mesh.destroy()
    }

    @JvmStatic
    fun generateSomething(
        asset: Saveable?,
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
                generateSomething(instance, srcFile, dstFile, size, callback)
            }
            // todo thumbnails for graphs
            is GameEngineProject -> {}
            null -> {}
            else -> {
                // todo can we create a json preview or sth like that?
                LOGGER.warn("Unknown item from prefab: ${asset.className}")
            }
            // is Transform -> todo show transform for Rem's Studio
        }
    }

    private fun readImage(bytes: ByteSlice?): Image? {
        if (bytes == null) return null
        val file = InnerStreamFile("", "", InvalidRef, bytes::stream)
        return ImageReader.readImage(file, true).waitForGFX()
    }

    private fun shallReturnIfExists(
        srcFile: FileReference, dstFile: ByteSlice?,
        callback: Callback<ITexture2D>,
    ): Boolean {
        val image = readImage(dstFile) ?: return false
        val rotation = TextureReader.getRotation(srcFile)
        addGPUTask("Thumbs.returnIfExists", image.width, image.height) {
            val texture = Texture2D(srcFile.name, image, true)
            texture.rotation = rotation
            callback.ok(texture)
        }
        return true
    }

    private fun shallReturnIfExists(
        srcFile: FileReference, dstFile: HDBKey,
        callback: Callback<ITexture2D>,
        callback1: (Boolean) -> Unit
    ) {
        hdb.get(dstFile, true) {
            callback1(shallReturnIfExists(srcFile, it, callback))
        }
    }

    @JvmStatic
    fun findScale(
        src: Image,
        srcFile: FileReference,
        size0: Int,
        callback: Callback<ITexture2D>,
        callback1: (Image) -> Unit
    ) {
        var size = size0
        val sw = src.width
        val sh = src.height
        if (max(sw, sh) < size) {
            size /= 2
            if (size < 3) return
            srcFile.getFileHash { hash ->
                findScale(src, srcFile, size0, hash, callback, callback1)
            }
        } else {
            val (w, h) = scaleMax(sw, sh, size)
            if (w < 2 || h < 2) return
            callback1(src.resized(w, h, false))
        }
    }

    @JvmStatic
    private fun findScale(
        src: Image,
        srcFile: FileReference,
        size0: Int,
        hash: Long,
        callback: Callback<ITexture2D>,
        callback1: (Image) -> Unit
    ) {
        var size = size0
        val sw = src.width
        val sh = src.height
        if (max(sw, sh) < size) {
            size /= 2
            if (size < 3) return
            val key = getCacheKey(srcFile, hash, size)
            shallReturnIfExists(srcFile, key, callback) { shallReturn ->
                if (!shallReturn) {
                    findScale(src, srcFile, size, hash, callback, callback1)
                }
            }
        } else {
            val (w, h) = scaleMax(sw, sh, size)
            if (w < 2 || h < 2) return
            callback1(src.resized(w, h, false))
        }
    }

    @JvmStatic
    private fun generate(srcFile: FileReference, size: Int, callback: Callback<ITexture2D>) {
        if (size < 3) return
        if (useCacheFolder) {
            srcFile.getFileHash { hash ->
                val key = getCacheKey(srcFile, hash, size)
                hdb.get(key, false) { byteSlice ->
                    // check all higher LODs for data: if they exist, use them instead
                    val foundSolution = checkHigherResolutions(srcFile, size, hash, callback)
                    if (!foundSolution) {
                        val foundExists = shallReturnIfExists(srcFile, byteSlice, callback)
                        if (!foundExists) {
                            generate(srcFile, key, size, callback)
                        }
                    }
                }
            }
        } else {
            generate(srcFile, InvalidKey, size, callback)
        }
    }

    private fun checkHigherResolutions(
        srcFile: FileReference, size: Int, hash: Long,
        callback: Callback<ITexture2D>
    ): Boolean {
        val idx = sizes.indexOf(size)
        var foundSolution = false
        for (i in idx + 1 until sizes.size) {
            val sizeI = sizes[i]
            val keyI = getCacheKey(srcFile, hash, sizeI)
            hdb.get(keyI, false) { bytes ->
                if (bytes != null) {
                    val image = readImage(bytes)
                    if (image != null) {
                        // scale down (and save?)
                        val rotation = TextureReader.getRotation(srcFile)
                        val (w, h) = scaleMax(image.width, image.height, size)
                        val newImage = image.resized(w, h, false)
                        val texture = Texture2D("${srcFile.name}-$size", newImage.width, newImage.height, 1)
                        newImage.createTexture(texture, sync = false, checkRedundancy = false) { tex, exc ->
                            if (tex is Texture2D) tex.rotation = rotation
                            callback.call(tex, exc)
                        }
                        foundSolution = true
                    }
                }
            }
        }
        return foundSolution
    }

    @JvmStatic
    private val readerBySignature =
        HashMap<String, (FileReference, HDBKey, Int, Callback<ITexture2D>) -> Unit>()

    @JvmStatic
    private val readerByExtension =
        HashMap<String, (FileReference, HDBKey, Int, Callback<ITexture2D>) -> Unit>()

    @JvmStatic
    fun registerSignature(
        signature: String,
        reader: (srcFile: FileReference, dstFile: HDBKey, size: Int, callback: Callback<ITexture2D>) -> Unit
    ) {
        readerBySignature[signature] = reader
    }

    @JvmStatic
    fun unregisterSignatures(vararg signatures: String) {
        for (signature in signatures) {
            readerBySignature.remove(signature)
        }
    }

    @JvmStatic
    fun registerExtension(
        extension: String,
        reader: (srcFile: FileReference, dstFile: HDBKey, size: Int, callback: Callback<ITexture2D>) -> Unit
    ) {
        readerByExtension[extension] = reader
    }

    @JvmStatic
    fun unregisterExtensions(vararg extensions: String) {
        for (extension in extensions) {
            readerByExtension.remove(extension)
        }
    }

    init {
        registerSignature("vox", Thumbs::generatePrefabReadableFrame)
        registerSignature("maya", Thumbs::generatePrefabReadableFrame)
        registerSignature("hdr") { srcFile, dstFile, size, callback ->
            srcFile.inputStream { it, exc ->
                if (it != null) {
                    val src = it.use(HDRReader::readHDR)
                    findScale(src, srcFile, size, callback) { dst ->
                        saveNUpload(srcFile, false, dstFile, dst, callback)
                    }
                } else callback.err(exc)
            }
        }
        registerSignature("png", Thumbs::generateImage)
        registerSignature("bmp", Thumbs::generateImage)
        registerSignature("psd", Thumbs::generateImage)
        registerSignature("ttf", Thumbs::generateFontPreview)
        registerSignature("woff1", Thumbs::generateFontPreview)
        registerSignature("woff2", Thumbs::generateFontPreview)
        registerSignature("dds", Thumbs::generateVideoFrame0)
        registerExtension("dds", Thumbs::generateVideoFrame0)
        registerExtension("webp", Thumbs::generateVideoFrame0)
        registerSignature("media") { srcFile, dstFile, size, callback ->
            generateVideoFrame(srcFile, dstFile, size, callback, 1.0)
        }
        registerExtension("txt", Thumbs::generateTextImage)
        registerExtension("html", Thumbs::generateTextImage)
        registerExtension("md", Thumbs::generateTextImage)
        registerExtension("url") { srcFile, dstFile, size, callback ->
            // try to read the url, and redirect to the icon
            findIconLineInTxtLink(srcFile, dstFile, size, "IconFile=", callback)
        }
        registerExtension("desktop") { srcFile, dstFile, size, callback ->
            // sample data by https://help.ubuntu.com/community/UnityLaunchersAndDesktopFiles:
            //[Desktop Entry]
            //Version=1.0
            //Name=BackMeUp
            //Comment=Back up your data with one click
            //Exec=/home/alex/Documents/backup.sh
            //Icon=/home/alex/Pictures/backup.png
            //Terminal=false
            //Type=Application
            //Categories=Utility;Application;
            findIconLineInTxtLink(srcFile, dstFile, size, "Icon=", callback)
        }
        // try as an asset
        for (ext in unityExtensions) {
            registerExtension(ext, Thumbs::generateSomething)
        }
        registerExtension("json", Thumbs::generateSomething)

        val ignored = listOf(
            "zip", "bz2", "tar", "gzip", "xz", "lz4", "7z", "xar",
            "sims", "lua-bytecode"
        )
        for (signature in ignored) {
            registerExtension(signature) { _, _, _, callback ->
                callback.err(IgnoredException())
            }
        }

        // todo compressed folders shouldn't have specific icon, only zip folder
    }

    private fun generateVideoFrame0(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        generateVideoFrame(srcFile, dstFile, size, callback, 0.0)
    }

    @JvmStatic
    fun generate(srcFile: FileReference, dstFile: HDBKey, size: Int, callback: Callback<ITexture2D>) {

        if (size < 3) return

        // for some stuff, the icons are really nice
        // for others, we need our previews
        // also some folder icons are really nice, while others are boring / default :/
        // generateSystemIcon(srcFile, dstFile, size, callback)
        // return

        if (srcFile.isDirectory) {
            // todo thumbnails for folders: what files are inside, including their preview images
            // generateSystemIcon(srcFile, dstFile, size, callback)
            return
        }

        // generate the image,
        // upload the result to the gpu
        // save the file

        if (OS.isWindows) {
            @Suppress("SpellCheckingInspection")
            when (srcFile.absolutePath) {
                "C:/pagefile.sys", "C:/hiberfil.sys",
                "C:/DumpStack.log", "C:/DumpStack.log.tmp",
                "C:/swapfile.sys" -> {
                    callback.err(IOException("Cannot generate thumbnail"))
                    return
                }
            }
        }

        when (srcFile) {
            is ImageReadable -> {
                val image = if (useCacheFolder) srcFile.readCPUImage() else srcFile.readGPUImage()
                transformNSaveNUpload(srcFile, false, image, dstFile, size, callback)
                return
            }
            is PrefabReadable -> {
                val prefab = srcFile.readPrefab()
                generateSomething(prefab, srcFile, dstFile, size, callback)
                return
            }
        }

        Signature.findName(srcFile) { signature ->
            val reader = readerBySignature[signature]
            if (reader != null) {
                reader(srcFile, dstFile, size, callback)
            } else try {
                val base = readerByExtension[srcFile.lcExtension]
                if (base != null) base(srcFile, dstFile, size, callback)
                else {
                    // todo thumbnails for Rem's Studio transforms
                    // png, jpg, jpeg, ico, webp, mp4, ...
                    generateImage(srcFile, dstFile, size, callback)
                }
            } catch (_: IgnoredException) {
            } catch (e: Exception) {
                e.printStackTrace()
                LOGGER.warn("Could not load image from $srcFile: ${e.message}")
            }
        }
    }

    private fun findIconLineInTxtLink(
        srcFile: FileReference, dstFile: HDBKey, size: Int, prefix: String,
        callback: Callback<ITexture2D>
    ) {
        val lineLengthLimit = 1024
        srcFile.readLines(lineLengthLimit) { lines, exc ->
            if (lines != null) {
                val iconFileLine = lines.firstOrNull { it.startsWith(prefix, true) }
                if (iconFileLine != null) {
                    val iconFile = iconFileLine
                        .substring(prefix.length)
                        .trim() // against \r
                        .replace('\\', '/')
                    generate(getReference(iconFile), dstFile, size, callback)
                }
                lines.close()
            } else exc?.printStackTrace()
        }
    }

    fun generateSomething(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        generateSomething(
            PrefabCache.getPrefabInstance(srcFile),
            srcFile, dstFile, size, callback
        )
    }

    private fun generateFontPreview(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        unused(dstFile)
        // generate font preview
        val text = "The quick\nbrown fox\njumps over\nthe lazy dog"
        val lineCount = 4
        val key = Font(srcFile.absolutePath, size * 0.7f / lineCount, isBold = false, isItalic = false)
        val texture = FontManager.getTexture(
            key, text, size * 2, size * 2, 0, false
        )
        if (texture != null) {
            waitUntil(true) { texture.wasCreated }
            callback.ok(texture)
        } else callback.err(null)
    }

    @JvmStatic
    private fun generateTextImage(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        unused(dstFile)
        // todo html preview???
        // todo markdown preview (?)
        // generate text preview
        // scale text with size?
        val maxLineCount = clamp(size / 24, 3, 40)
        val maxLineLength = maxLineCount * 5 / 2
        srcFile.readLines(maxLineLength) { itr, exc ->
            exc?.printStackTrace()
            if (itr != null) {
                var lines = itr
                    .subList(0, maxLineCount)
                    .toMutableList()
                if (itr.hasNext()/*lines.size > maxLineCount*/) {
                    lines = lines.subList(0, maxLineCount)
                    lines[lines.lastIndex] = "..."
                }
                itr.close()
                // remove empty lines at the end
                while (lines.isNotEmpty() && lines.last().isEmpty()) {
                    lines.removeAt(lines.lastIndex)
                }
                if (lines.isNotEmpty()) {
                    val length = lines.maxOf { it.length }
                    if (length > 0) {
                        val sx = monospaceFont.sampleWidth
                        val sy = monospaceFont.sizeInt
                        val w = (length + 1) * sx
                        val h = (lines.size + 1) * sy
                        addGPUTask("textThumbs", w, h) {
                            val transform = GFXx2D.transform
                            transform.identity().scale(1f, -1f, 1f)
                            val tex = Texture2D("textThumbs", w, h, 1)
                            tex.create(TargetType.UInt8x3)
                            useFrame(tex, 0) {
                                val tc = black
                                val bg = -1
                                it.clearColor(bg)
                                val x = sx.shr(1)
                                for (yi in lines.indices) {
                                    val line = lines[yi].trimEnd()
                                    if (line.isNotEmpty()) {
                                        val y = yi * sy + sy.shr(1)
                                        drawSimpleTextCharByChar(
                                            x, y, 1, line, tc, bg
                                        )
                                    }
                                }
                            }
                            transform.identity()
                            callback.ok(tex)
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun generateImage(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        // a small timeout, because we need that image shortly only
        val totalNanos = 30_000_000_000L
        val timeout = 50L
        var image: Image? = null
        val startTime = Time.nanoTime
        waitUntil(true) {
            if (Time.nanoTime < startTime + totalNanos) {
                image = ImageCache[srcFile, timeout, true]
                image != null || ImageCache.hasFileEntry(srcFile, timeout)
            } else true
        }
        if (image == null) {
            val ext = srcFile.lcExtension
            when (val importType = ext.getImportType()) {
                "Video" -> {
                    LOGGER.info("Generating frame for $srcFile")
                    generateVideoFrame(srcFile, dstFile, size, callback, 1.0)
                }
                // else nothing to do
                else -> {
                    LOGGER.info("ImageCache failed, importType '$importType' != getImportType for $srcFile")
                    generateTextImage(srcFile, dstFile, size, callback)
                }
            }
        } else transformNSaveNUpload(srcFile, true, image!!, dstFile, size, callback)
    }
}