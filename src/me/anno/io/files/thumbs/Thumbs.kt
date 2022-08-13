package me.anno.io.files.thumbs

import me.anno.Build
import me.anno.Engine
import me.anno.cache.data.ImageData
import me.anno.cache.data.ImageData.Companion.imageTimeout
import me.anno.cache.instances.OldMeshCache
import me.anno.cache.instances.PDFCache
import me.anno.cache.instances.VideoCache.getVideoFrame
import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.white4
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.anim.Skeleton.Companion.boneMeshVertices
import me.anno.ecs.components.anim.Skeleton.Companion.generateSkeleton
import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.shapes.Icosahedron
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabReadable
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.engine.ui.render.Renderers.simpleNormalRenderer
import me.anno.fonts.FontManager
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.isGFXThread
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.depthMode
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.drawing.SVGxGFX
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.DrawTextures.drawTransparentBackground
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.framebuffer.*
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Renderer.Companion.colorRenderer
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.*
import me.anno.image.ImageScale.scaleMax
import me.anno.image.hdr.HDRImage
import me.anno.image.jpg.JPGReader
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
import me.anno.io.text.TextReader
import me.anno.io.unity.UnityReader
import me.anno.io.zip.InnerFolder
import me.anno.io.zip.InnerPrefabFile
import me.anno.io.zip.ZipCache
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.clamp
import me.anno.mesh.MeshData.Companion.warnMissingMesh
import me.anno.mesh.assimp.AnimGameItem
import me.anno.studio.StudioBase
import me.anno.ui.base.Font
import me.anno.utils.Clock
import me.anno.utils.Color.hex4
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
import me.anno.utils.strings.StringHelper.shorten
import me.anno.utils.types.InputStreams.readNBytes2
import me.anno.utils.types.Strings.getImportType
import me.anno.video.ffmpeg.FFMPEGMetadata.Companion.getMeta
import me.anno.video.formats.gpu.GPUFrame
import net.boeckling.crc.CRC64
import net.sf.image4j.codec.ico.ICOReader
import org.apache.logging.log4j.LogManager
import org.joml.Math.sqrt
import org.joml.Matrix4fArrayList
import org.joml.Matrix4x3f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.filechooser.FileSystemView
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * creates and caches small versions of image and video resources
 * */
object Thumbs {

    // todo right click option in file explorer to invalidate a thumbs image
    // todo right click option for images: open large image viewer panel

    private val LOGGER = LogManager.getLogger(Thumbs::class)

    private val folder = ConfigBasics.cacheFolder.getChild("thumbs")
    private val sizes = intArrayOf(32, 64, 128, 256, 512)
    private val neededSizes = IntArray(sizes.last() + 1)
    private const val timeout = 5000L

    // todo disable this, when everything works
    var useCacheFolder = !Build.isDebug

    // png/bmp/jpg?
    private const val destinationFormat = "png"
    val sphereMesh = Icosahedron.createMesh(30, 30)

    init {
        LogManager.disableLogger("GlyphRenderer")
        LogManager.disableLogger("PDSimpleFont")
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

    fun invalidate(file: FileReference?, neededSize: Int) {
        file ?: return
        val size = getSize(neededSize)
        ImageGPUCache.remove { key, _ ->
            key is ThumbnailKey && key.file == file && key.size == size
        }
    }

    fun invalidate(file: FileReference?) {
        file ?: return
        ImageGPUCache.remove { key, _ ->
            key is ThumbnailKey && key.file == file
        }
    }

    fun getThumbnail(file: FileReference, neededSize: Int, async: Boolean): ITexture2D? {

        if (file == InvalidRef) return null
        if (file is ImageReadable) {
            return ImageGPUCache.getImage(file, timeout, async)
        }

        // currently not supported
        if (file.isDirectory) return null

        // was deleted
        if (!file.exists) return null

        val size = getSize(neededSize)
        val key = ThumbnailKey(file, file.lastModified, file.isDirectory, size)

        if (size == 1) LOGGER.warn("Requested size 1")

        val texture = ImageGPUCache.getLateinitTextureLimited(key, timeout, async, 4) { callback ->
            if (async) {
                thread(name = "Thumbs/${key.file.name}") {
                    try {
                        // LOGGER.info("Loading $file")
                        generate(file, size, callback)
                    } catch (e: ShutdownException) {
                        // don't care
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            } else generate(file, size, callback)
        }?.texture
        return when (texture) {
            is GPUFrame -> if (texture.isCreated) texture else null
            is Texture2D -> if (texture.isCreated && !texture.isDestroyed) texture else null
            else -> texture
        }
    }

    private fun FileReference.getCacheFile(size: Int): FileReference {

        val hashReadLimit = 4096
        val length = this.length()
        var hash: Long = lastModified xor (454781903L * length)
        if (!isDirectory && length > 0) {
            val reader = inputStream().buffered()
            val bytes = reader.readNBytes2(hashReadLimit, false)
            reader.close()
            hash = hash xor CRC64.fromInputStream(bytes.inputStream())
        }
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

    private fun getSize(neededSize: Int): Int {
        return if (neededSize < neededSizes.size) {
            neededSizes[neededSize]
        } else sizes.last()
    }

    private fun upload(
        srcFile: FileReference,
        checkRotation: Boolean,
        dst: Image,
        callback: (Texture2D) -> Unit
    ) {
        val rotation = if (checkRotation) ImageData.getRotation(srcFile) else null
        val texture = Texture2D(srcFile.name, dst.width, dst.height, 1)
        dst.createTexture(texture, sync = false, checkRedundancy = true)
        texture.rotation = rotation
        callback(texture)
    }

    private fun upload(
        srcFile: FileReference,
        checkRotation: Boolean,
        dst: BufferedImage,
        callback: (Texture2D) -> Unit
    ) {
        val rotation = if (checkRotation) ImageData.getRotation(srcFile) else null
        if (isGFXThread()) {
            val texture = Texture2D(srcFile.name, dst, true)
            texture.rotation = rotation
            callback(texture)
        } else {
            val texture = Texture2D(srcFile.name, dst.width, dst.height, 1)
            texture.create(dst, sync = false, checkRedundancy = false)
            texture.rotation = rotation
            callback(texture)
        }
    }

    private fun saveNUpload(
        srcFile: FileReference,
        checkRotation: Boolean,
        dstFile: FileReference,
        dst: Image,
        callback: (Texture2D) -> Unit
    ) {
        if (useCacheFolder) {
            dstFile.getParent()?.tryMkdirs()
            dstFile.outputStream().use { ImageIO.write(dst.createBufferedImage(), destinationFormat, it) }
        }
        upload(srcFile, checkRotation, dst, callback)
    }

    private fun saveNUpload(
        srcFile: FileReference,
        checkRotation: Boolean,
        dstFile: FileReference,
        dst: BufferedImage,
        callback: (Texture2D) -> Unit
    ) {
        if (useCacheFolder) {
            // don't wait to upload the image
            thread(name = "Writing ${dstFile.name} for cached thumbs") {
                dstFile.getParent()?.tryMkdirs()
                dstFile.outputStream().use { ImageIO.write(dst, destinationFormat, it) }
            }
        }
        upload(srcFile, checkRotation, dst, callback)
    }

    private fun transformNSaveNUpload(
        srcFile: FileReference,
        checkRotation: Boolean,
        src: BufferedImage,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        val sw = src.width
        val sh = src.height
        if (min(sw, sh) < 1) return

        // if it matches the size, just upload it
        // we have loaded it anyway already
        if (max(sw, sh) < size) {
            saveNUpload(srcFile, checkRotation, dstFile, src, callback)
            return
            // return generate(srcFile, size / 2, callback)
        }

        val (w, h) = scaleMax(sw, sh, size)
        if (min(w, h) < 1) return
        if (w == sw && h == sh) {
            saveNUpload(srcFile, checkRotation, dstFile, src, callback)
        } else {
            val dst = BufferedImage(w, h, src.type)
            val gfx = dst.createGraphics()
            gfx.drawImage(src, 0, 0, w, h, null)
            gfx.dispose()
            saveNUpload(srcFile, checkRotation, dstFile, dst, callback)
        }
    }

    private fun transformNSaveNUpload(
        srcFile: FileReference,
        checkRotation: Boolean,
        src: Image,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
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
            val dst = src.createBufferedImage(w, h)
            saveNUpload(srcFile, checkRotation, dstFile, dst, callback)
        }
    }

    fun renderToImage(
        src: FileReference,
        checkRotation: Boolean,
        dstFile: FileReference,
        withDepth: Boolean,
        renderer: Renderer = colorRenderer,
        flipY: Boolean,
        callback: (Texture2D) -> Unit,
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

    private fun renderToImagePart2(
        srcFile: FileReference,
        checkRotation: Boolean,
        dstFile: FileReference,
        withDepth: Boolean,
        renderer: Renderer,
        flipY: Boolean,
        callback: (Texture2D) -> Unit,
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
                callback(texture)
            } else {
                // todo check if this is working (Android or disable MSAA), should be, it's really simple
                val texture = renderTarget.textures[0]
                renderTarget.destroyExceptTextures(false)
                callback(texture)
            }
        }
    }

    val flipYRot = RotateJPEG(mirrorHorizontal = false, mirrorVertical = true, 0)

    fun generateVideoFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (ITexture2D) -> Unit,
        wantedTime: Double
    ) {

        val meta = getMeta(srcFile, false) ?: throw RuntimeException("Could not load metadata for $srcFile")
        if (max(meta.videoWidth, meta.videoHeight) < size) return generate(srcFile, size / 2, callback)

        val scale = floor(max(meta.videoWidth, meta.videoHeight).toFloat() / size).toInt()

        val sw = meta.videoWidth / scale
        val sh = meta.videoHeight / scale

        val (w, h) = scaleMax(sw, sh, size)
        if (w < 2 || h < 2) return

        val fps = min(5.0, meta.videoFPS)
        val time = max(min(wantedTime, meta.videoDuration - 1 / fps), 0.0)
        val index = (time * fps).roundToInt()

        val src = waitForGFXThreadUntilDefined(true) {
            getVideoFrame(srcFile, scale, index, 0, fps, 1000L, true)
        }

        waitForGFXThread(true) { src.isCreated }

        renderToImage(srcFile, false, dstFile, false, colorRenderer, false, callback, w, h) {
            drawTexture(src)
        }

    }

    fun generateSVGFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {

        val buffer = OldMeshCache.getSVG(srcFile, imageTimeout, false)!!

        val maxSize = max(buffer.maxX, buffer.maxY)
        val w = (size * buffer.maxX / maxSize).roundToInt()
        val h = (size * buffer.maxY / maxSize).roundToInt()

        if (w < 2 || h < 2) return

        val transform = Matrix4fArrayList()
        transform.scale((buffer.maxY / buffer.maxX).toFloat(), 1f, 1f)
        renderToImage(srcFile, false, dstFile, false, colorRenderer, true, callback, w, h) {
            SVGxGFX.draw3DSVG(
                transform, buffer, whiteTexture,
                white4, Filtering.NEAREST,
                whiteTexture.clamping!!, null
            )
        }

    }

    private fun waitForTextures(comp: MeshComponentBase, mesh: Mesh, srcFile: FileReference) {
        // wait for all textures
        val textures = HashSet<FileReference>()
        iterateMaterials(comp.materials, mesh.materials) { material ->
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

    private fun waitForTextures(mesh: Mesh, file: FileReference) {
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

    private fun waitForTextures(entity: Entity, srcFile: FileReference) {
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

    private fun waitForMeshes(entity: Entity) {
        // wait for all textures
        entity.forAll {
            if (it is MeshComponentBase) {
                // does the CPU part -> not perfect, but maybe good enough
                it.ensureBuffer()
            }
        }
    }

    private fun collectTextures(entity: Entity, textures: MutableSet<FileReference>) {
        for (comp in entity.getComponentsInChildren(MeshComponentBase::class, false)) {
            val mesh = comp.getMesh()
            if (mesh == null) {
                warnMissingMesh(comp, null)
                continue
            }
            iterateMaterials(comp.materials, mesh.materials) { material ->
                textures += listTextures(material)
            }
        }
    }

    inline fun iterateMaterials(l0: List<FileReference>, l1: List<FileReference>, run: (FileReference) -> Unit) {
        for (index in 0 until max(l0.size, l1.size)) {
            val li = l0.getOrNull(index)?.nullIfUndefined() ?: l1.getOrNull(index)
            if (li != null && li != InvalidRef) run(li)
        }
    }

    // just render it using the simplest shader
    fun generateAssimpMeshFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        // statically loading is easier, but we may load things twice ->
        // only load them once, use our cache
        val data = waitUntilDefined(true) {
            PrefabCache[srcFile, maxPrefabDepth, true]
            // loadAssimpStatic(srcFile, null)
        }.getSampleInstance()
        generateSomething(data, srcFile, dstFile, size, callback)
    }

    fun generateVOXMeshFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        val data = waitUntilDefined(true) {
            PrefabCache[srcFile, maxPrefabDepth, true]
            // loadVOX(srcFile, null)
        }.getSampleInstance() as Entity
        // generateFrame(dstFile, data, size, previewRenderer, true, callback)
        generateEntityFrame(srcFile, dstFile, size, data, callback)
    }

    fun generateEntityFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        entity: Entity,
        callback: (Texture2D) -> Unit
    ) {
        val agi = AnimGameItem(entity)
        entity.validateTransform()
        val cameraMatrix = createCameraMatrix(1f)
        val modelMatrices =
            agi.findModelMatrix(cameraMatrix, createModelMatrix(), centerMesh = true, normalizeScale = true)
        // todo draw gui (colliders), entity positions
        for (i in 0 until 3) { // make sure both are loaded
            waitForMeshes(entity)
            waitForTextures(entity, srcFile)
        }
        val drawSkeletons = !entity.hasComponent(MeshComponentBase::class)
        renderToImage(srcFile, false, dstFile, true, previewRenderer, true, callback, size, size) {
            agi.drawAssimp(
                useECSShader = true, cameraMatrix, modelMatrices, 0.0, white4, "",
                useMaterials = true, drawSkeletons = drawSkeletons
            )
        }
    }

    fun generateColliderFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        collider: Collider,
        callback: (Texture2D) -> Unit
    ) {
        unused(srcFile)
        val cameraMatrix = createCameraMatrix(1f)
        val modelMatrix = createModelMatrix()
        collider.findModelMatrix(cameraMatrix, modelMatrix, centerMesh = true, normalizeScale = true)
        renderToImage(srcFile, false, dstFile, true, previewRenderer, true, callback, size, size) {
            collider.drawAssimp(cameraMatrix, modelMatrix)
        }
    }

    fun generateMeshFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        mesh: Mesh,
        callback: (Texture2D) -> Unit
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

    fun generateMeshFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        comp: MeshComponentBase,
        callback: (Texture2D) -> Unit
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

    val matCameraMatrix = createCameraMatrix(1f)
    val matModelMatrix = createModelMatrix().scale(0.62f)

    // todo if we have preview images, we could use them as cheaper textures
    fun generateMaterialFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        val material = MaterialCache[srcFile] ?: return
        generateMaterialFrame(srcFile, dstFile, material, size, callback)
    }

    fun generateMaterialFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        material: Material,
        size: Int,
        callback: (Texture2D) -> Unit
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

    fun generateMaterialFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        materials: List<FileReference>,
        size: Int,
        callback: (Texture2D) -> Unit
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

    fun renderMultiWindowImage(
        srcFile: FileReference,
        dstFile: FileReference,
        count: Int, size: Int,
        // whether the aspect ratio of the parts can be adjusted to keep the result quadratic
        // if false, the result will be rectangular
        changeSubFrameAspectRatio: Boolean,
        renderer0: Renderer,
        callback: (Texture2D) -> Unit,
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
            val frame = GFXState.currentBuffer!!
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

    fun generateSkeletonFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        skeleton: Skeleton,
        size: Int,
        callback: (Texture2D) -> Unit
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

    fun generateAnimationFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        animation: Animation,
        size: Int,
        callback: (Texture2D) -> Unit
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
        renderMultiWindowImage(srcFile, dstFile, count, size, true, simpleNormalRenderer, callback) { it, aspect ->
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
        Texture2D.floatArrayPool.returnBuffer(meshVertices)
        mesh.destroy()
    }

    val threadLocalBoneMatrices = ThreadLocal2 {
        val boneCount = 256
        val skinningMatrices = Array(boneCount) { Matrix4x3f() }
        val animPositions = Array(boneCount) { Vector3f() }
        skinningMatrices to animPositions
    }

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

    private fun listTextures(materialReference: FileReference): List<FileReference> {
        if (materialReference == InvalidRef) return emptyList()
        val material = MaterialCache[materialReference]
        if (material == null) LOGGER.warn("Missing material '$materialReference'")
        return if (material != null) listTextures(material) else emptyList()
    }

    private fun listTextures(material: Material): List<FileReference> {
        // LOGGER.info("$material: ${material.diffuseMap}, ${material.emissiveMap}, ${material.normalMap}")
        return listOf(
            material.diffuseMap,
            material.emissiveMap,
            material.normalMap,
            material.roughnessMap,
            material.metallicMap,
            material.occlusionMap,
            material.displacementMap,
        )
    }

    private fun waitForTextures(materials: List<Material>, timeout: Long = 25000) {
        // listing all textures
        // does not include personal materials / shaders...
        val textures = ArrayList<FileReference>()
        for (material in materials) {
            textures += listTextures(material)
        }
        waitForTextures(textures, timeout)
    }

    private fun waitForTextures(material: Material, timeout: Long = 25000) {
        // listing all textures
        // does not include personal materials / shaders...
        val textures = listTextures(material).filter { it != InvalidRef && it.exists }
        waitForTextures(textures, timeout)
    }

    private fun waitForTextures(textures: Collection<FileReference>, timeout: Long = 25000) {
        // 25s timeout, because unzipping all can take its time
        // wait for textures
        if (textures.isEmpty()) return
        val endTime = Engine.gameTime + timeout * MILLIS_TO_NANOS
        waitForGFXThread(true) {
            if (Engine.gameTime > endTime) {
                // textures may be missing; just ignore them, if they cannot be read
                textures
                    .filter { !ImageGPUCache.hasImageOrCrashed(it, timeout, true) }
                    .forEach { LOGGER.warn("Missing texture $it") }
                true
            } else textures.all { ImageGPUCache.hasImageOrCrashed(it, timeout, true) }
        }
    }

    fun generateSomething(
        asset: ISaveable?,
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        when (asset) {
            is Mesh -> generateMeshFrame(srcFile, dstFile, size, asset, callback)
            is Material -> generateMaterialFrame(srcFile, dstFile, asset, size, callback)
            is Skeleton -> generateSkeletonFrame(srcFile, dstFile, asset, size, callback)
            is Animation -> generateAnimationFrame(srcFile, dstFile, asset, size, callback)
            is Entity -> generateEntityFrame(srcFile, dstFile, size, asset, callback)
            is MeshComponentBase -> generateMeshFrame(srcFile, dstFile, size, asset, callback)
            is Collider -> generateColliderFrame(srcFile, dstFile, size, asset, callback)
            is Component -> {
                val gt = JomlPools.mat4x3d.borrow()
                val ab = JomlPools.aabbd.borrow()
                if (asset.fillSpace(gt, ab)) {
                    // todo render component somehow... just return an icon?
                    // todo render debug ui :)
                }
            }
            is Prefab -> {
                val instance = asset.getSampleInstance()
                generateSomething(instance, srcFile, dstFile, size, callback)
            }
            null -> {}
            else -> {
                // todo can we create a json preview or sth like that?
                LOGGER.warn("Unknown item from prefab: ${asset.className}")
            }
            // is Transform -> todo show transform for Rem's Studio
        }
    }

    fun returnIfExists(srcFile: FileReference, dstFile: FileReference, callback: (Texture2D) -> Unit): Boolean {
        if (dstFile.exists) {
            // LOGGER.info("cached preview for $srcFile exists")
            val image = ImageIO.read(dstFile.inputStream())
            if (image == null) LOGGER.warn("Could not read $dstFile")
            else {
                val rotation = ImageData.getRotation(srcFile)
                GFX.addGPUTask("Thumbs.returnIfExists", image.width, image.height) {
                    val texture = Texture2D(srcFile.name, image, true)
                    texture.rotation = rotation
                    callback(texture)
                }
                return true
            }
        }
        return false
    }

    private fun findScale(
        src: Image,
        srcFile: FileReference,
        size0: Int,
        callback: (Texture2D) -> Unit
    ): BufferedImage? {
        var dstFile: FileReference
        var size = size0
        val sw = src.width
        val sh = src.height
        while (max(sw, sh) < size) {
            size /= 2
            if (size < 3) return null
            dstFile = srcFile.getCacheFile(size)
            if (returnIfExists(srcFile, dstFile, callback)) return null
        }
        val (w, h) = scaleMax(sw, sh, size)
        if (w < 2 || h < 2) return null
        return src.createBufferedImage(w, h)
    }

    private fun generate(srcFile: FileReference, size: Int, callback: (ITexture2D) -> Unit) {

        if (size < 3) return

        val dstFile = if (useCacheFolder) {
            val dstFile = srcFile.getCacheFile(size)
            if (returnIfExists(srcFile, dstFile, callback)) return
            dstFile
        } else InvalidRef

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

        when (Signature.findName(srcFile)) {
            // list all signatures, which can be assigned strictly by their signature
            "vox" -> generateVOXMeshFrame(srcFile, dstFile, size, callback)
            "hdr" -> {
                val src = HDRImage(srcFile)
                val dst = findScale(src, srcFile, size, callback) ?: return
                saveNUpload(srcFile, false, dstFile, dst, callback)
            }
            "pdf" -> {
                try {
                    val ref = PDFCache.getDocumentRef(srcFile, borrow = true, async = false) ?: return
                    val image = PDFCache.getImageCachedBySize(ref.doc, size, 0)
                    ref.returnInstance()
                    saveNUpload(srcFile, false, dstFile, image, callback)
                } catch (_: NullPointerException) {
                    // can happen ^^
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            "jpg" -> {
                val data2 = JPGReader.extractThumbnail(srcFile)
                if (data2 != null) {
                    try {
                        val image = ImageIO.read(data2.inputStream())
                        transformNSaveNUpload(srcFile, true, image, dstFile, size, callback)
                    } catch (e: Exception) {
                        generateImage(srcFile, dstFile, size, callback)
                    }
                } else generateImage(srcFile, dstFile, size, callback)
            }
            // for ico we could find the best image from looking at the headers
            "ico" -> {
                val image = ICOReader.read(srcFile.inputStream(), size)
                transformNSaveNUpload(srcFile, false, image, dstFile, size, callback)
            }
            "png", "bmp", "psd", "qoi" ->
                generateImage(srcFile, dstFile, size, callback)
            "blend" -> generateSomething(
                PrefabCache.getPrefabInstance(srcFile),
                srcFile,
                dstFile,
                size,
                callback
            )
            "zip", "bz2", "tar", "gzip", "xz", "lz4", "7z", "xar" -> {
            }
            "sims" -> {
            }
            "ttf", "woff1", "woff2" -> {
                // for woff does not show the contents :/
                // generateSystemIcon(srcFile, dstFile, size, callback)
                // todo generate font preview
            }
            "lua-bytecode" -> {
            }
            "exe" -> generateSystemIcon(srcFile, dstFile, size, callback)
            "media" -> generateVideoFrame(srcFile, dstFile, size, callback, 1.0)
            else -> try {
                when (srcFile.lcExtension) {

                    // done start exe files from explorer
                    // done preview icon for exe files / links using generateSystemIcon

                    // done thumbnails and import for .vox files (MagicaVoxel voxel meshes)

                    // done thumbnails for meshes, and components
                    // todo thumbnails for visual scripts, when they exist?
                    // todo thumbnails for Rem's Studio transforms
                    "obj", "fbx", "gltf", "glb", "dae", "md2", "md5mesh" -> {
                        // todo list all mesh extensions, which are supported by assimp
                        // preview for mtl file? idk...
                        generateAssimpMeshFrame(srcFile, dstFile, size, callback)
                    }
                    in UnityReader.unityExtensions -> {
                        try {
                            // parse unity files
                            val decoded = UnityReader.readAsAsset(srcFile)
                            if (decoded != InvalidRef) {
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
                            } else LOGGER.warn("Could not understand unity asset $srcFile, result is InvalidRef")
                        } catch (e: Throwable) {
                            LOGGER.warn("${e.message}; by $srcFile")
                            e.printStackTrace()
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
                        val src = srcFile.inputStream().use { TGAImage.read(it, false) }
                        val dst = findScale(src, srcFile, size, callback) ?: return
                        saveNUpload(srcFile, false, dstFile, dst, callback)
                    }
                    "svg" -> generateSVGFrame(srcFile, dstFile, size, callback)
                    "mtl" -> {
                        // read as folder
                        val children = ZipCache.unzip(srcFile, false)?.listChildren() ?: emptyList()
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
                        val text = srcFile.readText()
                        val lines = text.split('\n')
                        val iconFileLine = lines.firstOrNull { it.startsWith("IconFile=", true) }
                        if (iconFileLine != null) {
                            val iconFile = iconFileLine
                                .substring(9)
                                .trim() // against \r
                                .replace('\\', '/')
                            // LOGGER.info("Found icon file from URL '$srcFile': '$iconFile'")
                            generate(getReference(iconFile), size, callback)
                        }
                    }
                    // ImageIO says it can do webp, however it doesn't understand most pics...
                    "webp", "dds" -> generateVideoFrame(srcFile, dstFile, size, callback, 0.0)
                    "lnk", "desktop" -> {
                        // not images, and I don't know yet how to get the image from them
                    }
                    "ico" -> {
                        val image = ICOReader.read(srcFile.inputStream(), size)
                        transformNSaveNUpload(srcFile, false, image, dstFile, size, callback)
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

    private fun generateTextImage(
        srcFile: FileReference,
        size: Int,
        callback: (ITexture2D) -> Unit
    ) {
        // todo draw text with cheap/mono letters, if possible
        // todo html preview???
        // todo markdown preview (?)
        // generate text preview
        // scale text with size?
        val maxLineCount = clamp(size / 24, 3, 40)
        val maxLineLength = maxLineCount * 5 / 2
        val maxLength = 64 * 1024
        val bytes = srcFile.inputStream().use {
            it.readNBytes2(maxLength, false)
        }
        if (bytes.isNotEmpty()) {
            if (bytes.last() < 0) {
                bytes[bytes.lastIndex] = ' '.code.toByte()
            }
            var lines = String(bytes)
                .split('\n')
                .map { it.shorten(maxLineLength) }
                .toMutableList()
            if (lines.size > maxLineCount) {
                lines = lines.subList(0, maxLineCount)
                lines[lines.lastIndex] = "..."
            }
            // remove empty lines at the end
            while (lines.isNotEmpty() && lines.last().isEmpty()) {
                lines = lines.subList(0, lines.size - 1)
            }
            val text = lines
                .joinToString("\n")
            val lineCount = lines.size
            val key = Font(DefaultConfig.defaultFontName, size * 0.7f / lineCount, isBold = false, isItalic = false)
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
                callback(texture)
            }
        }
    }

    private fun generateImage(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (ITexture2D) -> Unit
    ) {
        // a small timeout, because we need that image shortly only
        val totalNanos = 30_000_000_000L
        val timeout = 50L
        var image: Image? = null
        val startTime = System.nanoTime()
        waitUntil(true) {
            if (System.nanoTime() < startTime + totalNanos) {
                image = ImageCPUCache.getImage(srcFile, timeout, true)
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

    private fun generateSystemIcon(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        val icon = srcFile.toFile {
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
        }
        val image = BufferedImage(icon.iconWidth + 2, icon.iconHeight + 2, 2)
        val gfx = image.createGraphics()
        icon.paintIcon(null, gfx, 1, 1)
        gfx.dispose()
        // respect the size
        transformNSaveNUpload(srcFile, true, image, dstFile, size, callback)
    }

    fun testGeneration(
        src: FileReference,
        readAsFolder: (FileReference) -> InnerFolder,
        dst: FileReference = desktop.getChild("test.png"),
        size: Int = 512
    ) {
        // time for debugger to attach
        // for (i in 0 until 100) Thread.sleep(100)
        val clock = Clock()
        LOGGER.info("File Size: ${src.length().formatFileSize()}")
        val folder = readAsFolder(src)
        clock.stop("read file")
        ECSRegistry.initWithGFX(size)
        clock.stop("inited opengl")
        val scene = folder.getChild("Scene.json") as InnerPrefabFile
        useCacheFolder = true
        generateSomething(scene.prefab, src, dst, size) {}
        clock.stop("rendered & saved image")
        Engine.requestShutdown()
    }

}