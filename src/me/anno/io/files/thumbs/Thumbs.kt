package me.anno.io.files.thumbs

import me.anno.Build
import me.anno.Engine
import me.anno.cache.data.ImageData
import me.anno.cache.data.ImageData.Companion.imageTimeout
import me.anno.cache.instances.OldMeshCache
import me.anno.cache.instances.VideoCache.getVideoFrame
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.anim.Skeleton.Companion.boneMeshVertices
import me.anno.ecs.components.anim.Skeleton.Companion.generateSkeleton
import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.shapes.Icosahedron
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabReadable
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.engine.ui.render.Renderers.simpleNormalRenderer
import me.anno.fonts.FontManager
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.isGFXThread
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.depthMode
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.DrawTextures.drawTransparentBackground
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.drawing.SVGxGFX
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Renderer.Companion.colorRenderer
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.*
import me.anno.image.ImageScale.scaleMax
import me.anno.image.hdr.HDRImage
import me.anno.image.jpg.JPGThumbnails
import me.anno.image.raw.toImage
import me.anno.image.tar.TGAImage
import me.anno.io.ISaveable
import me.anno.io.base.InvalidClassException
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.files.thumbs.ThumbsExt.createCameraMatrix
import me.anno.io.files.thumbs.ThumbsExt.createModelMatrix
import me.anno.io.files.thumbs.ThumbsExt.drawAssimp
import me.anno.io.files.thumbs.ThumbsExt.findModelMatrix
import me.anno.io.files.thumbs.ThumbsExt.waitForMeshes
import me.anno.io.files.thumbs.ThumbsExt.waitForTextures
import me.anno.io.text.TextReader
import me.anno.io.unity.UnityReader
import me.anno.io.zip.InnerFolderCache
import me.anno.io.zip.InnerFolderReader
import me.anno.io.zip.InnerPrefabFile
import me.anno.maths.Maths.clamp
import me.anno.mesh.MeshUtils
import me.anno.mesh.assimp.AnimGameItem
import me.anno.studio.StudioBase
import me.anno.ui.base.Font
import me.anno.utils.Clock
import me.anno.utils.Color.hex4
import me.anno.utils.Color.white4
import me.anno.utils.OS
import me.anno.utils.OS.desktop
import me.anno.utils.ShutdownException
import me.anno.utils.Sleep.waitForGFXThread
import me.anno.utils.Sleep.waitForGFXThreadUntilDefined
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.Sleep.waitUntilDefined
import me.anno.utils.Warning.unused
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.hpc.ThreadLocal2
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.Iterators.firstOrNull
import me.anno.utils.structures.Iterators.subList
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.InputStreams.readNBytes2
import me.anno.utils.types.Strings.getImportType
import me.anno.video.ffmpeg.FFMPEGMetadata.Companion.getMeta
import me.anno.video.formats.gpu.GPUFrame
import net.boeckling.crc.CRC64
import net.sf.image4j.codec.ico.ICOReader
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4fArrayList
import org.joml.Matrix4x3f
import org.joml.Vector3d
import org.joml.Vector3f
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.filechooser.FileSystemView
import kotlin.concurrent.thread
import kotlin.math.*

/**
 * creates and caches small versions of image and video resources
 * */
object Thumbs {

    // todo right click option in file explorer to invalidate a thumbs image
    // todo right click option for images: open large image viewer panel

    @JvmStatic
    private val LOGGER = LogManager.getLogger(Thumbs::class)

    @JvmStatic
    private val folder = ConfigBasics.cacheFolder.getChild("thumbs")

    @JvmStatic
    private val sizes = intArrayOf(32, 64, 128, 256, 512)

    @JvmStatic
    private val neededSizes = IntArray(sizes.last() + 1)
    private const val timeout = 5000L

    // todo disable this, when everything works
    @JvmField
    var useCacheFolder = !Build.isDebug

    // png/bmp/jpg?
    private const val destinationFormat = "png"

    @JvmField
    val sphereMesh = Icosahedron.createUVSphere(30, 30)

    init {
        if (!useCacheFolder) {
            folder.listChildren()?.forEach { it.deleteRecursively() }
        }
        var index = 0
        for (size in sizes) {
            while (index <= size) {
                neededSizes[index++] = size
            }
        }
    }

    @JvmStatic
    fun invalidate(file: FileReference?, neededSize: Int) {
        file ?: return
        val size = getSize(neededSize)
        ImageGPUCache.remove { key, _ ->
            key is ThumbnailKey && key.file == file && key.size == size
        }
    }

    @JvmStatic
    fun invalidate(file: FileReference?) {
        file ?: return
        ImageGPUCache.remove { key, _ ->
            key is ThumbnailKey && key.file == file
        }
    }

    @JvmStatic
    fun getThumbnail(file: FileReference, neededSize: Int, async: Boolean): ITexture2D? {

        if (file == InvalidRef) return null
        if (file is ImageReadable) {
            return ImageGPUCache[file, timeout, async]
        }

        // currently not supported
        if (file.isDirectory) return null

        // was deleted
        if (!file.exists) return null

        if (neededSize < 1) return null
        val size = getSize(neededSize)
        val key = ThumbnailKey(file, file.lastModified, file.isDirectory, size)

        val texture = ImageGPUCache.getLateinitTextureLimited(key, timeout, async, 4) { callback ->
            if (async) {
                thread(name = "Thumbs/${key.file.name}") {
                    try {
                        // LOGGER.info("Loading $file")
                        generate(file, size) { it, exc ->
                            callback(it)
                            exc?.printStackTrace()
                        }
                    } catch (e: ShutdownException) {
                        // don't care
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            } else generate(file, size) { it, exc ->
                callback(it)
                exc?.printStackTrace()
            }
        }?.texture
        return when (texture) {
            is GPUFrame -> if (texture.isCreated) texture else null
            is Texture2D -> if (texture.isCreated && !texture.isDestroyed) texture else null
            else -> texture
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
                    callback(baseHash xor CRC64.fromInputStream(bytes.inputStream()))
                } else callback(baseHash)
            }

        } else callback(baseHash)
    }

    @JvmStatic
    private fun FileReference.getCacheFile(size: Int, callback: (FileReference) -> Unit) {
        getFileHash { hash ->
            callback(createCacheFile(hash, size))
        }
    }

    @JvmStatic
    private fun createCacheFile(hash: Long, size: Int): FileReference {
        val dstFormat = destinationFormat
        val str1 = CharArray(16 + 1 + dstFormat.length) { '0' }
        for (i in 0 until 16) {
            val base4 = hash.shr((15 - i) * 4).and(15)
            str1[i] = hex4(base4)
        }
        str1[16] = '.'
        for (i in dstFormat.indices) {
            str1[i + 17] = dstFormat[i]
        }
        // LOGGER.info("$hash -> ${String(str1)}")
        return folder.getChild("$size")
            .getChild(String(str1, 0, 2))
            .getChild(String(str1, 2, str1.size - 2))
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
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        val rotation = if (checkRotation) ImageData.getRotation(srcFile) else null
        val texture = Texture2D(srcFile.name, dst.width, dst.height, 1)
        dst.createTexture(texture, sync = false, checkRedundancy = true)
        texture.rotation = rotation
        callback(texture, null)
    }

    @JvmStatic
    fun saveNUpload(
        srcFile: FileReference,
        checkRotation: Boolean,
        dstFile: FileReference,
        dst: Image,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        if (useCacheFolder) {
            dstFile.getParent()?.tryMkdirs()
            dst.write(dstFile)
        }
        upload(srcFile, checkRotation, dst, callback)
    }

    @JvmStatic
    private fun transformNSaveNUpload(
        srcFile: FileReference,
        checkRotation: Boolean,
        src: Image,
        dstFile: FileReference,
        size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
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
            val dst = src.resized(w, h)
            saveNUpload(srcFile, checkRotation, dstFile, dst, callback)
        }
    }

    @JvmStatic
    fun renderToImage(
        src: FileReference,
        checkRotation: Boolean,
        dstFile: FileReference,
        withDepth: Boolean,
        renderer: Renderer = colorRenderer,
        flipY: Boolean,
        callback: (ITexture2D?, Exception?) -> Unit,
        w: Int, h: Int, render: () -> Unit
    ) {
        if (isGFXThread()) {
            renderToImagePart2(
                src, checkRotation, dstFile, withDepth, renderer,
                flipY, callback, w, h, render
            )
        } else {
            GFX.addGPUTask("Thumbs.render($src)", w, h) {
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
        dstFile: FileReference,
        withDepth: Boolean,
        renderer: Renderer,
        flipY: Boolean,
        callback: (ITexture2D?, Exception?) -> Unit,
        w: Int, h: Int,
        render: () -> Unit
    ) {
        GFX.check()

        val renderTarget = if (GFX.maxSamples > 1 || useCacheFolder) {
            FBStack[srcFile.name, w, h, 4, false, 4, withDepth]
        } else {
            Framebuffer(
                srcFile.name, w, h, 1, 1, false,
                if (withDepth) DepthBufferType.INTERNAL else DepthBufferType.NONE
            )
        }

        renderPurely {
            if (!withDepth) {
                useFrame(w, h, false, renderTarget, colorRenderer) {
                    drawTransparentBackground(0, 0, w, h)
                }
            }
            useFrame(w, h, false, renderTarget, renderer) {
                if (withDepth) {
                    depthMode.use(DepthMode.CLOSER) {
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
                    arrayOf(TargetType.UByteTarget4), DepthBufferType.NONE
                )
                renderTarget.needsBlit = true
                renderTarget.blitTo(newBuffer)
                val texture = newBuffer.textures[0]
                newBuffer.destroyExceptTextures(false)
                texture.rotation = if (flipY) flipYRot else null
                callback(texture, null)
            } else {
                val texture = renderTarget.textures[0]
                renderTarget.destroyExceptTextures(true)
                callback(texture, null)
            }
        }
    }

    @JvmField
    val flipYRot = ImageTransform(mirrorHorizontal = false, mirrorVertical = true, 0)

    @JvmStatic
    fun generateVideoFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (ITexture2D?, Exception?) -> Unit,
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

        renderToImage(srcFile, false, dstFile, false, colorRenderer, false, callback, w, h) {
            drawTexture(src)
        }

    }

    @JvmStatic
    fun generateSVGFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {

        val buffer = OldMeshCache.getSVG(srcFile, imageTimeout, false)!!

        val maxSize = max(buffer.maxX, buffer.maxY)
        val w = (size * buffer.maxX / maxSize).roundToInt()
        val h = (size * buffer.maxY / maxSize).roundToInt()

        if (w < 2 || h < 2) return

        val transform = Matrix4fArrayList()
        transform.scale(buffer.maxY / buffer.maxX, 1f, 1f)
        renderToImage(srcFile, false, dstFile, false, colorRenderer, true, callback, w, h) {
            SVGxGFX.draw3DSVG(
                transform, buffer, whiteTexture,
                white4, Filtering.NEAREST,
                whiteTexture.clamping!!, null
            )
        }

    }

    inline fun iterateMaterials(l0: List<FileReference>, l1: List<FileReference>, run: (FileReference) -> Unit) {
        for (index in 0 until max(l0.size, l1.size)) {
            val li = l0.getOrNull(index)?.nullIfUndefined() ?: l1.getOrNull(index)
            if (li != null && li != InvalidRef) run(li)
        }
    }

    // just render it using the simplest shader
    @JvmStatic
    fun generateAssimpMeshFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        // statically loading is easier, but we may load things twice ->
        // only load them once, use our cache
        val data = waitUntilDefined(true) {
            PrefabCache[srcFile, maxPrefabDepth, true]
            // loadAssimpStatic(srcFile, null)
        }.getSampleInstance()
        generateSomething(data, srcFile, dstFile, size, callback)
    }

    @JvmStatic
    fun generateVOXMeshFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        val data = waitUntilDefined(true) {
            PrefabCache[srcFile, maxPrefabDepth, true]
            // loadVOX(srcFile, null)
        }.getSampleInstance() as Entity
        // generateFrame(dstFile, data, size, previewRenderer, true, callback)
        generateEntityFrame(srcFile, dstFile, size, data, callback)
    }

    @JvmStatic
    fun generateEntityFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        entity: Entity,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        entity.validateTransform()
        entity.validateAABBs()
        val bounds = entity.aabb
        // todo draw gui (colliders), entity positions
        for (i in 0 until 3) { // make sure both are loaded
            waitForMeshes(entity)
            waitForTextures(entity, srcFile)
        }
        val rv = RenderView(EditorState, PlayMode.EDITING, style)
        rv.enableOrbiting = true
        rv.radius = 500.0 * max(bounds.deltaX(), max(bounds.deltaY(), bounds.deltaZ()))
        rv.editorCamera.fovY = 10f.toRadians()
        rv.rotation.identity()
            .rotateY(25.0.toRadians())
            .rotateX((-15.0).toRadians())
        rv.position.set(bounds.avgX(), bounds.avgY(), bounds.avgZ())
        val cam = rv.editorCamera
        rv.updateEditorCameraTransform()
        rv.prepareDrawScene(size, size, 1f, cam, cam, 0f, false)
        // don't use EditorState
        rv.pipeline.clear()
        rv.pipeline.fill(entity)
        renderToImage(srcFile, false, dstFile, true, previewRenderer, true, callback, size, size) {
            rv.setRenderState()
            rv.pipeline.draw()
        }
    }

    @JvmStatic
    fun generateColliderFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        collider: Collider,
        callback: (ITexture2D?, Exception?) -> Unit
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
    fun generateMeshFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        mesh: Mesh,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        mesh.checkCompleteness()
        mesh.ensureBuffer()
        waitForTextures(mesh, srcFile)
        // sometimes black: because of vertex colors, which are black
        // render everything without color
        renderToImage(srcFile, false, dstFile, true, simpleNormalRenderer, true, callback, size, size) {
            mesh.drawAssimp(
                1f, null,
                useMaterials = true,
                centerMesh = true,
                normalizeScale = true
            )
        }
    }

    @JvmStatic
    fun generateMeshFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        comp: MeshComponentBase,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        comp.ensureBuffer()
        val mesh = comp.getMesh() ?: return
        mesh.checkCompleteness()
        mesh.ensureBuffer()
        waitForTextures(comp, mesh, srcFile)
        // sometimes black: because of vertex colors, which are black
        // render everything without color
        renderToImage(srcFile, false, dstFile, true, simpleNormalRenderer, true, callback, size, size) {
            mesh.drawAssimp(
                1f, comp,
                useMaterials = true,
                centerMesh = true,
                normalizeScale = true
            )
        }
    }

    @JvmStatic
    fun generateMeshFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        comp: Renderable,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {

        // todo check that this is correct

        // todo how could we wait for resources here?
        /*comp.ensureBuffer()
        val mesh = comp.getMesh() ?: return
        mesh.checkCompleteness()
        mesh.ensureBuffer()
        waitForTextures(comp, mesh, srcFile)*/

        val pipeline = Pipeline(null)
        pipeline.defaultStage = PipelineStage(
            "default", Sorting.FRONT_TO_BACK, 16,
            null, DepthMode.CLOSER, true, CullMode.BACK, pbrModelShader
        )

        val sampleEntity = Entity()
        sampleEntity.add(comp as Component)

        val cm = createCameraMatrix(1f)
        val mm = createModelMatrix()

        sampleEntity.validateAABBs()
        mm.scale(AnimGameItem.getScaleFromAABB(sampleEntity.aabb))
        MeshUtils.centerMesh(cm, mm, sampleEntity)

        cm.mul(mm) // join matrices; is this order correct?

        // todo set camera position
        val cameraPosition = Vector3d()
        val worldScale = 1.0

        fun defineRenderState() {
            // setup full render state
            RenderState.cameraPosition.set(cameraPosition)
            RenderState.worldScale = worldScale
            RenderState.cameraMatrix.set(cm)
            RenderState.cameraRotation.identity()
                .rotateX((15.0).toRadians())// rotate it into a nice viewing angle
                .rotateY((-25.0).toRadians())
            RenderState.calculateDirections()

            RenderState.fovYRadians = 1f
            RenderState.isPerspective = true
        }

        defineRenderState()
        comp.fill(pipeline, sampleEntity, 0)

        renderToImage(srcFile, false, dstFile, true, simpleNormalRenderer, true, callback, size, size) {
            // setup full render state
            defineRenderState()
            pipeline.defaultStage.drawColors(pipeline)
        }
    }

    @JvmField
    val matCameraMatrix = createCameraMatrix(1f)

    @JvmField
    val matModelMatrix = createModelMatrix().scale(0.62f)

    // todo if we have preview images, we could use them as cheaper textures
    @JvmStatic
    fun generateMaterialFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        val material = MaterialCache[srcFile] ?: return
        generateMaterialFrame(srcFile, dstFile, material, size, callback)
    }

    @JvmStatic
    fun generateMaterialFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        material: Material,
        size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
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
    fun split(total: Int): Int {
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
        dstFile: FileReference,
        materials: List<FileReference>,
        size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        waitForTextures(materials.mapNotNull { MaterialCache[it] })
        renderMultiWindowImage(
            srcFile, dstFile, materials.size, size, false,
            previewRenderer, callback
        ) { it, _ ->
            GFXState.blendMode.use(BlendMode.DEFAULT) {
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
    fun renderMultiWindowImage(
        srcFile: FileReference,
        dstFile: FileReference,
        count: Int, size: Int,
        // whether the aspect ratio of the parts can be adjusted to keep the result quadratic
        // if false, the result will be rectangular
        changeSubFrameAspectRatio: Boolean,
        renderer0: Renderer,
        callback: (ITexture2D?, Exception?) -> Unit,
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
        dstFile: FileReference,
        skeleton: Skeleton,
        size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
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
    fun generateAnimationFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        animation: Animation,
        size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        val skeleton = SkeletonCache[animation.skeleton] ?: return
        val entity = Entity()
        val mesh = Mesh()
        val duration = animation.duration
        val hasMotion = duration > 0.0
        val count = if (hasMotion) 6 else 1
        val dt = if (hasMotion) duration / count else 0f
        val bones = skeleton.bones
        val meshVertices = Texture2D.floatArrayPool[bones.size * boneMeshVertices.size, false, true]
        mesh.positions = meshVertices
        val (skinningMatrices, animPositions) = threadLocalBoneMatrices.get()
        renderMultiWindowImage(srcFile, dstFile, count, size, true, simpleNormalRenderer, { it, e ->
            callback(it, e)
            Texture2D.floatArrayPool.returnBuffer(meshVertices)
            mesh.destroy()
        }) { it, aspect ->
            val time = it * dt
            // generate the matrices
            animation.getMatrices(entity, time, skinningMatrices)
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
        time: Float,
        aspect: Float
    ) {
        // todo center on bounds by all frames combined
        val skeleton = SkeletonCache[animation.skeleton] ?: return
        val entity = Entity()
        val mesh = Mesh()
        val bones = skeleton.bones
        val meshVertices = Texture2D.floatArrayPool[bones.size * boneMeshVertices.size, false, true]
        mesh.positions = meshVertices
        val (skinningMatrices, animPositions) = threadLocalBoneMatrices.get()
        // generate the matrices
        animation.getMatrices(entity, time, skinningMatrices)
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
        Texture2D.floatArrayPool.returnBuffer(meshVertices)
        mesh.destroy()
    }

    @JvmStatic
    fun generateSomething(
        asset: ISaveable?,
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
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
                }
            }
            is Prefab -> {
                val instance = asset.getSampleInstance()
                generateSomething(instance, srcFile, dstFile, size, callback)
            }
            // todo thumbnails for graphs
            null -> {}
            else -> {
                // todo can we create a json preview or sth like that?
                LOGGER.warn("Unknown item from prefab: ${asset.className}")
            }
            // is Transform -> todo show transform for Rem's Studio
        }
    }

    @JvmStatic
    fun shallReturnIfExists(
        srcFile: FileReference,
        dstFile: FileReference,
        callback: (ITexture2D?, Exception?) -> Unit,
        callback1: (Boolean) -> Unit
    ) {
        if (dstFile.exists) {
            // LOGGER.info("cached preview for $srcFile exists")
            dstFile.inputStream { it, _ ->
                val hasImage = if (it != null) {
                    val image = ImageIO.read(it)
                    if (image == null) {
                        LOGGER.warn("Could not read $dstFile")
                        false
                    } else {
                        val rotation = ImageData.getRotation(srcFile)
                        GFX.addGPUTask("Thumbs.returnIfExists", image.width, image.height) {
                            val texture = Texture2D(srcFile.name, image.toImage(), true)
                            texture.rotation = rotation
                            callback(texture, null)
                        }
                        true
                    }
                } else {
                    LOGGER.warn("Could not read $dstFile")
                    false
                }
                callback1(hasImage)
            }
        } else callback1(false)
    }

    @JvmStatic
    private fun findScale(
        src: Image,
        srcFile: FileReference,
        size0: Int,
        callback: (ITexture2D?, Exception?) -> Unit,
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
            callback1(src.resized(w, h))
        }
    }

    @JvmStatic
    private fun findScale(
        src: Image,
        srcFile: FileReference,
        size0: Int,
        hash: Long,
        callback: (ITexture2D?, Exception?) -> Unit,
        callback1: (Image) -> Unit
    ) {
        var size = size0
        val sw = src.width
        val sh = src.height
        if (max(sw, sh) < size) {
            size /= 2
            if (size < 3) return
            val dstFile = createCacheFile(hash, size)
            shallReturnIfExists(srcFile, dstFile, callback) { shallReturn ->
                if (!shallReturn) {
                    findScale(src, srcFile, size, hash, callback, callback1)
                }
            }
        } else {
            val (w, h) = scaleMax(sw, sh, size)
            if (w < 2 || h < 2) return
            callback1(src.resized(w, h))
        }
    }

    @JvmStatic
    private fun generate(srcFile: FileReference, size: Int, callback: (ITexture2D?, Exception?) -> Unit) {
        if (size < 3) return
        if (useCacheFolder) {
            srcFile.getCacheFile(size) { dstFile ->
                shallReturnIfExists(srcFile, dstFile, callback) { shallReturn ->
                    if (!shallReturn) {
                        generate(srcFile, size, dstFile, callback)
                    }
                }
            }
        } else {
            generate(srcFile, size, InvalidRef, callback)
        }
    }

    @JvmStatic
    private val readerBySignature =
        HashMap<String, (FileReference, Int, FileReference, (ITexture2D?, Exception?) -> Unit) -> Unit>()

    @JvmStatic
    fun register(
        signature: String,
        reader: (srcFile: FileReference, size: Int, dstFile: FileReference, callback: (ITexture2D?, Exception?) -> Unit) -> Unit
    ) {
        readerBySignature[signature] = reader
    }

    @JvmStatic
    fun unregister(signature: String) {
        readerBySignature.remove(signature)
    }

    init {
        register("vox") { srcFile, size, dstFile, callback ->
            generateVOXMeshFrame(srcFile, dstFile, size, callback)
        }
        register("hdr") { srcFile, size, dstFile, callback ->
            val src = HDRImage(srcFile)
            findScale(src, srcFile, size, callback) { dst ->
                saveNUpload(srcFile, false, dstFile, dst, callback)
            }
        }
        register("jpg") { srcFile, size, dstFile, callback ->
            JPGThumbnails.extractThumbnail(srcFile) { data2 ->
                if (data2 != null) {
                    try {
                        val image = ImageIO.read(data2.inputStream())
                        transformNSaveNUpload(srcFile, true, image.toImage(), dstFile, size, callback)
                    } catch (e: Exception) {
                        generateImage(srcFile, dstFile, size, callback)
                    }
                } else generateImage(srcFile, dstFile, size, callback)
            }
        }
        register("ico") { srcFile, size, dstFile, callback ->
            // for ico we could find the best image from looking at the headers
            srcFile.inputStream { it, exc ->
                if (it != null) {
                    val image = ICOReader.read(it, size)
                    transformNSaveNUpload(srcFile, false, image, dstFile, size, callback)
                } else exc?.printStackTrace()
            }
        }
    }

    @JvmStatic
    private fun generate(
        srcFile: FileReference,
        size: Int,
        dstFile: FileReference,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {

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

        if (OS.isWindows) when (srcFile.absolutePath) {
            "C:/pagefile.sys", "C:/hiberfil.sys",
            "C:/DumpStack.log", "C:/DumpStack.log.tmp",
            "C:/swapfile.sys" -> return
        }

        when (srcFile) {
            is ImageReadable -> {
                val image = srcFile.readImage()
                transformNSaveNUpload(srcFile, false, image, dstFile, size, callback)
                return
            }
            is PrefabReadable -> {
                val prefab = srcFile.readPrefab()
                generateSomething(prefab, srcFile, dstFile, size, callback)
                return
            }
        }

        val windowsLink = srcFile.windowsLnk.value
        if (windowsLink != null) {
            val dst = getReference(windowsLink.absolutePath)
            generate(dst, size, callback)
            return
        }

        Signature.findName(srcFile) { signature ->
            val reader = readerBySignature[signature]
            if (reader != null) {
                reader(srcFile, size, dstFile, callback)
            } else when (signature) {
                // list all signatures, which can be assigned strictly by their signature
                // for ico we could find the best image from looking at the headers
                "png", "bmp", "psd", "qoi" -> generateImage(srcFile, dstFile, size, callback)
                "blend" -> generateSomething(
                    PrefabCache.getPrefabInstance(srcFile),
                    srcFile, dstFile, size, callback
                )
                "zip", "bz2", "tar", "gzip", "xz", "lz4", "7z", "xar" -> {
                }
                "sims" -> {
                }
                "ttf", "woff1", "woff2" -> {
                    // generate font preview
                    val text = "The quick\nbrown fox\njumps over\nthe lazy dog"
                    val lineCount = 4
                    val key = Font(srcFile.absolutePath, size * 0.7f / lineCount, isBold = false, isItalic = false)
                    val font = FontManager.getFont(key)
                    val texture = font.generateTexture(
                        text, key.size, size * 2, size * 2,
                        portableImages = true,
                        textColor = 255 shl 24,
                        backgroundColor = -1,
                        extraPadding = key.sizeInt / 2
                    )
                    if (texture is ITexture2D) {
                        if (texture is Texture2D)
                            waitUntil(true) { texture.isCreated || texture.isDestroyed }
                        callback(texture, null)
                    }
                }
                "lua-bytecode" -> {
                }
                // todo MIP images... are used by gradient domain samples
                "dds" -> generateVideoFrame(srcFile, dstFile, size, callback, 0.0)
                "exe" -> generateSystemIcon(srcFile, dstFile, size, callback)
                "media" -> generateVideoFrame(srcFile, dstFile, size, callback, 1.0)
                "mitsuba-scene", "mitsuba-meshes" -> generateSomething(
                    PrefabCache.getPrefabInstance(srcFile),
                    srcFile, dstFile, size, callback
                )
                else -> try {
                    when (srcFile.lcExtension) {

                        // done start exe files from explorer
                        // done preview icon for exe files / links using generateSystemIcon

                        // done thumbnails and import for .vox files (MagicaVoxel voxel meshes)

                        // done thumbnails for meshes, and components
                        // todo thumbnails for Rem's Studio transforms
                        "obj", "fbx", "gltf", "glb", "dae", "md2", "md5mesh" -> {
                            // todo list all mesh extensions, which are supported by assimp
                            // preview for mtl file? idk...
                            generateAssimpMeshFrame(srcFile, dstFile, size, callback)
                        }
                        // parse unity files
                        in UnityReader.unityExtensions -> UnityReader.readAsAsset(srcFile) { decoded, e ->
                            if (decoded != InvalidRef && decoded != null) {
                                when {
                                    decoded is PrefabReadable ->
                                        generateSomething(decoded.readPrefab(), srcFile, dstFile, size, callback)
                                    decoded.length() > 0 -> {
                                        // try to read the file as an asset
                                        // not sure about using the workspace here...
                                        val sth = TextReader.read(decoded, StudioBase.workspace, true).firstOrNull()
                                        generateSomething(sth, srcFile, dstFile, size, callback)
                                    }
                                    else -> LOGGER.warn("File $decoded is empty")
                                }
                            } else {
                                LOGGER.warn("Could not understand unity asset $srcFile, result is InvalidRef")
                                LOGGER.warn("${e?.message}; by $srcFile")
                                e?.printStackTrace()
                            }
                        }
                        "json" -> {
                            try {
                                // try to read the file as an asset
                                val something = PrefabCache.getPrefabInstance(srcFile)
                                generateSomething(something, srcFile, dstFile, size, callback)
                            } catch (_: ShutdownException) {
                            } catch (e: InvalidClassException) {
                                LOGGER.info("${e.message}; by $srcFile")
                            } catch (e: Throwable) {
                                LOGGER.info("${e.message}; by $srcFile")
                                e.printStackTrace()
                            }
                        }
                        "tga" -> {
                            srcFile.inputStream { it, exc ->
                                if (it != null) {
                                    val src = it.use { TGAImage.read(it, false) }
                                    findScale(src, srcFile, size, callback) { dst ->
                                        saveNUpload(srcFile, false, dstFile, dst, callback)
                                    }
                                }
                                exc?.printStackTrace()
                            }
                        }
                        "svg" -> generateSVGFrame(srcFile, dstFile, size, callback)
                        "mtl" -> {
                            // read as folder
                            val children = InnerFolderCache.readAsFolder(srcFile, false)?.listChildren() ?: emptyList()
                            if (children.isNotEmpty()) {
                                val maxSize = 25 // with more, too many details are lost
                                generateMaterialFrame(
                                    srcFile, dstFile,
                                    if (children.size < maxSize) children else
                                        children.subList(0, maxSize), size, callback
                                )
                            } else {
                                // just an empty material to symbolize, that the file is empty
                                // we maybe could do better with some kind of texture...
                                generateMaterialFrame(srcFile, dstFile, Material(), size, callback)
                            }
                        }
                        "url" -> {
                            // try to read the url, and redirect to the icon
                            val lineLengthLimit = 1024
                            srcFile.readLines(lineLengthLimit) { lines, exc ->
                                if (lines != null) {
                                    val iconFileLine = lines.firstOrNull { it.startsWith("IconFile=", true) }
                                    if (iconFileLine != null) {
                                        val iconFile = iconFileLine
                                            .substring(9)
                                            .trim() // against \r
                                            .replace('\\', '/')
                                        // LOGGER.info("Found icon file from URL '$srcFile': '$iconFile'")
                                        generate(getReference(iconFile), size, callback)
                                    }
                                    lines.close()
                                } else exc?.printStackTrace()
                            }
                        }
                        // ImageIO says it can do webp, however it doesn't understand most pics...
                        "webp", "dds" -> generateVideoFrame(srcFile, dstFile, size, callback, 0.0)
                        "lnk", "desktop" -> {
                            // not images, and I don't know yet how to get the image from them
                        }
                        "ico" -> {
                            srcFile.inputStream { it, exc ->
                                if (it != null) {
                                    val image = ICOReader.read(it, size)
                                    transformNSaveNUpload(srcFile, false, image, dstFile, size, callback)
                                } else exc?.printStackTrace()
                            }
                        }
                        "txt", "html", "md" -> generateTextImage(srcFile, size, callback)
                        // png, jpg, jpeg, ico, webp, mp4, ...
                        else -> generateImage(srcFile, dstFile, size, callback)
                    }
                } catch (e: ShutdownException) {
                    // don't care
                } catch (e: Exception) {
                    e.printStackTrace()
                    LOGGER.warn("Could not load image from $srcFile: ${e.message}")
                }
            }
        }
    }

    @JvmStatic
    private fun generateTextImage(
        srcFile: FileReference,
        size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        // todo draw text with cheap/mono letters, if possible
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
                    lines = lines.subList(0, lines.size - 1)
                }
                val text = lines.joinToString("\n")
                val lineCount = lines.size
                val key =
                    Font(DefaultConfig.defaultFontName, size * 0.7f / lineCount, isBold = false, isItalic = false)
                val font2 = FontManager.getFont(key)
                val texture = font2.generateTexture(
                    text, key.size, size * 2, size * 2,
                    portableImages = true,
                    textColor = 255 shl 24,
                    backgroundColor = -1,
                    extraPadding = key.sizeInt / 2
                )
                if (texture is ITexture2D) {
                    if (texture is Texture2D)
                        waitUntil(true) { texture.isCreated || texture.isDestroyed }
                    callback(texture, null)
                }
            }
        }
    }

    @JvmStatic
    private fun generateImage(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        // a small timeout, because we need that image shortly only
        val totalNanos = 30_000_000_000L
        val timeout = 50L
        var image: Image? = null
        val startTime = System.nanoTime()
        waitUntil(true) {
            if (System.nanoTime() < startTime + totalNanos) {
                image = ImageCPUCache[srcFile, timeout, true]
                image != null || ImageCPUCache.hasFileEntry(srcFile, timeout)
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
                    LOGGER.info("ImageIO failed, Imaging failed, importType '$importType' != getImportType for $srcFile")
                    generateTextImage(srcFile, size, callback)
                }
            }
        } else transformNSaveNUpload(srcFile, true, image!!, dstFile, size, callback)
    }

    @JvmStatic
    private fun generateSystemIcon(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        srcFile.toFile({
            try {
                val shellFolder = javaClass.classLoader.loadClass("sun.awt.shell.ShellFolder")
                val shellMethod = shellFolder.getMethod("getShellFolder", java.io.File::class.java)
                // val sf = ShellFolder.getShellFolder(it)
                val sf = shellMethod.invoke(null, it)
                val iconMethod = shellFolder.getMethod("getIcon", Boolean::class.java)
                // val icon = sf.getIcon(true)
                val icon = iconMethod.invoke(sf, true) as java.awt.Image
                ImageIcon(icon)
            } catch (e: Exception) {
                FileSystemView.getFileSystemView().getSystemIcon(it)
            }
        }, { icon, exc ->
            if (icon != null) {
                val image = BufferedImage(icon.iconWidth + 2, icon.iconHeight + 2, 2)
                val gfx = image.createGraphics()
                icon.paintIcon(null, gfx, 1, 1)
                gfx.dispose()
                // respect the size
                transformNSaveNUpload(srcFile, true, image.toImage(), dstFile, size, callback)
            } else exc?.printStackTrace()
        })
    }

    @JvmStatic
    fun testGeneration(
        src: FileReference,
        readAsFolder: InnerFolderReader,
        dst: FileReference = desktop.getChild("test.png"),
        size: Int = 512
    ) {
        // time for debugger to attach
        // for (i in 0 until 100) Thread.sleep(100)
        val clock = Clock()
        LOGGER.info("File Size: ${src.length().formatFileSize()}")
        readAsFolder(src) { folder, _ ->
            folder!!
            clock.stop("read file")
            ECSRegistry.initWithGFX(size)
            clock.stop("inited opengl")
            val scene = folder.getChild("Scene.json") as InnerPrefabFile
            useCacheFolder = true
            generateSomething(scene.prefab, src, dst, size) { _, exc ->
                exc?.printStackTrace()
            }
            clock.stop("rendered & saved image")
            Engine.requestShutdown()
        }

    }

}