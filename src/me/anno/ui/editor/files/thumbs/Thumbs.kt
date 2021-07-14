package me.anno.ui.editor.files.thumbs

import me.anno.cache.data.ImageData
import me.anno.cache.instances.LastModifiedCache
import me.anno.cache.instances.MeshCache
import me.anno.cache.instances.TextureCache.getLateinitTexture
import me.anno.cache.instances.VideoCache.getVideoFrame
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.RenderState.depthMode
import me.anno.gpu.RenderState.renderPurely
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.SVGxGFX
import me.anno.gpu.TextureLib
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.*
import me.anno.image.HDRImage
import me.anno.image.tar.TGAImage
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.mesh.assimp.StaticMeshesLoader
import me.anno.objects.Video
import me.anno.objects.documents.pdf.PDFCache
import me.anno.objects.meshes.Mesh.Companion.loadModel
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import me.anno.utils.Sleep.waitUntilDefined
import me.anno.utils.Threads.threadWithName
import me.anno.utils.files.Files.use
import me.anno.utils.image.ImageScale.scale
import me.anno.utils.input.readNBytes2
import me.anno.utils.types.Strings.getImportType
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import net.boeckling.crc.CRC64
import org.apache.commons.imaging.Imaging
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.joml.Math.toRadians
import org.lwjgl.opengl.GL11.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * creates and caches small versions of image and video resources
 * */
object Thumbs {

    private val folder = ConfigBasics.cacheFolder.getChild("thumbs")
    private val sizes = intArrayOf(32, 64, 128, 256, 512)
    private val neededSizes = IntArray(sizes.last() + 1)
    private const val timeout = 5000L

    private fun FileReference.getCacheFile(size: Int): FileReference {
        val hashReadLimit = 256
        val info = LastModifiedCache[this]
        var hash = info.lastModified xor (454781903L * this.length())
        if (!info.isDirectory) {
            val reader = inputStream().buffered()
            val bytes = reader.readNBytes2(hashReadLimit, false)
            reader.close()
            hash = hash xor CRC64.fromInputStream(bytes.inputStream()).value
        }
        var hashString = hash.toULong().toString(16)
        while (hashString.length < 16) hashString = "0$hashString"
        return folder.getChild("$size/${hashString.substring(0, 2)}/${hashString.substring(2)}.$destinationFormat")!!
    }

    init {
        var index = 0
        for (size in sizes) {
            while (index <= size) {
                neededSizes[index++] = size
            }
        }
    }

    private fun getSize(neededSize: Int): Int {
        return if (neededSize < neededSizes.size) {
            neededSizes[neededSize]
        } else sizes.last()
    }

    fun getThumbnail(file: FileReference, neededSize: Int): ITexture2D? {
        val size = getSize(neededSize)
        val key = ThumbnailKey(file, size)
        return getLateinitTexture(key, timeout) { callback ->
            thread(name = key.file.nameWithoutExtension) {
                generate(file, size, callback)
            }
        }.texture
    }

    private fun upload(srcFile: FileReference, dst: BufferedImage, callback: (Texture2D) -> Unit) {
        val rotation = ImageData.getRotation(srcFile)
        GFX.addGPUTask(dst.width, dst.height) {
            val texture = Texture2D(dst)
            texture.rotation = rotation
            callback(texture)
        }
    }

    private fun saveNUpload(
        srcFile: FileReference,
        dstFile: FileReference,
        dst: BufferedImage,
        callback: (Texture2D) -> Unit
    ) {
        dstFile.getParent()!!.mkdirs()
        use(dstFile.outputStream()) { ImageIO.write(dst, destinationFormat, it) }
        upload(srcFile, dst, callback)
    }

    private fun transformNSaveNUpload(
        srcFile: FileReference,
        src: BufferedImage,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        val sw = src.width
        val sh = src.height
        if (min(sw, sh) < 1) return
        if (max(sw, sh) < size) {
            return generate(srcFile, size / 2, callback)
        }
        val (w, h) = scale(sw, sh, size)
        if (min(w, h) < 1) return
        if (w == sw && h == sh) {
            upload(srcFile, src, callback)
        } else {
            val dst = BufferedImage(
                w, h,
                if (src.colorModel.hasAlpha()) BufferedImage.TYPE_INT_ARGB
                else BufferedImage.TYPE_INT_RGB
            )
            // todo better, custom interpolation? would be required in TGA & HDR as well
            val gfx = dst.createGraphics()
            gfx.drawImage(src, 0, 0, w, h, null)
            gfx.dispose()
            saveNUpload(srcFile, dstFile, dst, callback)
        }
    }

    private fun renderToBufferedImage(
        srcFile: FileReference,
        dstFile: FileReference,
        withDepth: Boolean,
        callback: (Texture2D) -> Unit,
        w: Int, h: Int, render: () -> Unit
    ) {

        val buffer = IntArray(w * h)

        GFX.addGPUTask(w, h) {

            GFX.check()

            val fb2 = FBStack["generateVideoFrame", w, h, 4, false, 8]

            renderPurely {

                useFrame(0, 0, w, h, false, fb2, Renderer.colorRenderer) {

                    Frame.bind()

                    glClearColor(0f, 0f, 0f, 1f)
                    glClear(GL_COLOR_BUFFER_BIT)

                    // todo why is no tiling visible when rendering 3d models???
                    drawTexture(
                        0, 0, w, h,
                        TextureLib.colorShowTexture,
                        -1, Vector4f(4f, 4f, 0f, 0f)
                    )

                    if (withDepth) glClear(GL_DEPTH_BUFFER_BIT)

                    if (withDepth) {
                        depthMode.use(DepthMode.LESS_EQUAL) {
                            render()
                        }
                    } else {
                        render()
                    }

                }

                // cannot read from separate framebuffer, only from null... why ever...
                useFrame(0, 0, w, h, false, null, Renderer.colorRenderer) {

                    fb2.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

                    Frame.bind()
                    GFX.copy()

                    // draw only the clicked area?
                    glFlush(); glFinish() // wait for everything to be drawn
                    glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

                    glReadPixels(0, GFX.height - h, w, h, GL_RGBA, GL_UNSIGNED_BYTE, buffer)

                    GFX.check()

                }

            }

            threadWithName("Thumbs::renderToBufferedImage()") {
                val dst = BufferedImage(w, h, 2)
                val buffer2 = dst.raster.dataBuffer
                for (i in 0 until w * h) {
                    val col = buffer[i]
                    // swizzle colors, because rgba != argb
                    buffer2.setElem(i, rgba(col.b(), col.g(), col.r(), col.a()))
                }
                saveNUpload(srcFile, dstFile, dst, callback)
            }

        }
    }

    private fun generateVideoFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit,
        wantedTime: Double
    ) {

        val meta = getMeta(srcFile, false)!!
        if (max(meta.videoWidth, meta.videoHeight) < size) return generate(srcFile, size / 2, callback)

        val scale = floor(max(meta.videoWidth, meta.videoHeight).toFloat() / size).toInt()

        val sw = meta.videoWidth / scale
        val sh = meta.videoHeight / scale

        val (w, h) = scale(sw, sh, size)
        if (w < 2 || h < 2) return

        if (w > GFX.width || h > GFX.height) {
            // cannot use this large size...
            // would cause issues
            return generate(srcFile, size, callback)
        }

        val fps = min(5.0, meta.videoFPS)
        val time = max(min(wantedTime, meta.videoDuration - 1 / fps), 0.0)
        val index = (time * fps).roundToInt()

        // LOGGER.info("requesting frame $index / $time / $fps fps from $srcFile")

        val src = waitUntilDefined(true) {
            getVideoFrame(srcFile, scale, index, 1, fps, 1000L, true)
        }

        // LOGGER.info("got frame for $srcFile")

        src.waitToLoad()

        // LOGGER.info("loaded frame for $srcFile")

        renderToBufferedImage(srcFile, dstFile, false, callback, w, h) {
            renderPurely {
                drawTexture(src)
            }
        }

        // LOGGER.info("rendered $srcFile")

    }

    private fun generateSVGFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {

        val buffer = MeshCache.getSVG(srcFile, Video.imageTimeout, false)!!

        val maxSize = max(buffer.maxX, buffer.maxY)
        val w = (size * buffer.maxX / maxSize).roundToInt()
        val h = (size * buffer.maxY / maxSize).roundToInt()

        if (w < 2 || h < 2) return

        val transform = Matrix4fArrayList()
        transform.scale(1f / (buffer.maxX / buffer.maxY).toFloat(), -1f, 1f)
        renderToBufferedImage(srcFile, dstFile, false, callback, w, h) {
            SVGxGFX.draw3DSVG(
                null, 0.0,
                transform, buffer, whiteTexture,
                Vector4f(1f), Filtering.NEAREST,
                whiteTexture.clamping, null
            )
        }

    }

    private fun updateTransforms(entity: Entity) {
        entity.transform.update(entity.parent?.transform)
        for (child in entity.children) updateTransforms(child)
    }

    private fun AABBf.toDouble(): AABBd {
        return AABBd(
            minX.toDouble(), minY.toDouble(), minZ.toDouble(),
            maxX.toDouble(), maxY.toDouble(), maxZ.toDouble()
        )
    }

    private fun calculateAABB(root: Entity): AABBd {
        val joint = AABBd()
        root.simpleTraversal(true) { entity ->
            // todo rendering all points is only a good idea, if there are no meshes
            // todo render all them points, and use them for the bbx calculation (only if no meshes present)
            // because animated clothing may be too small to see
            val local = AABBd()
            val meshes = entity.getComponents<MeshComponent>(false).mapNotNull { it.mesh }
            for (mesh in meshes) {
                mesh.ensureBuffer()
                local.union(mesh.aabb.toDouble())
            }
            local.transform(Matrix4d(entity.transform.globalTransform))
            joint.union(local)
            false
        }
        return joint
    }

    // just render it using the simplest shader
    private fun generateAssimpMeshFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {

        if (size < 3) return

        val data = waitUntilDefined(true) {
            loadModel(srcFile, "Assimp-Static", null, { meshData ->
                val reader = StaticMeshesLoader()
                val meshes = reader.load(srcFile)
                meshData.assimpModel = meshes
            }) { it.assimpModel }
        }

        val model = data.assimpModel!!

        // render the whole model
        val rootEntity = model.hierarchy
        updateTransforms(rootEntity)

        // calculate/get the size
        val aabb = calculateAABB(rootEntity)

        val stack = Matrix4fArrayList()
        stack.perspective(0.7f, 1f, 0.001f, 5f)
        stack.translate(0f, 0f, -1f)// move the camera back a bit
        stack.rotateX(toRadians(-15f))// rotate it into a nice viewing angle
        stack.rotateY(toRadians(-25f))

        // calculate the scale, such that everything can be visible
        val delta = max(aabb.maxX - aabb.minX, max(aabb.maxY - aabb.minY, aabb.maxZ - aabb.minZ))
        // half, because it's half the size, 1.05f for a small border
        val scale = 0.5f * 1.05f / delta.toFloat()
        stack.scale(scale, -scale, scale)

        // apply the required offset for centering
        stack.translate(
            -(aabb.minX + aabb.maxX).toFloat() / 2,
            -(aabb.minY + aabb.maxY).toFloat() / 2,
            -(aabb.minZ + aabb.maxZ).toFloat() / 2
        )

        // render everything without color
        renderToBufferedImage(srcFile, dstFile, true, callback, size, size) {
            data.drawAssimp(null, stack, 0.0, Vector4f(1f), "", false)
        }

    }

    // png/bmp/jpg?
    private const val destinationFormat = "png"
    private fun generate(srcFile: FileReference, size: Int, callback: (Texture2D) -> Unit) {

        if (size < 1) return

        val dstFile = srcFile.getCacheFile(size)
        if (dstFile.exists) {

            // LOGGER.info("cached preview for $srcFile exists")
            val image = ImageIO.read(dstFile.inputStream())
            val rotation = ImageData.getRotation(srcFile)
            GFX.addGPUTask(size, size) {
                val texture = Texture2D(image)
                texture.rotation = rotation
                callback(texture)
            }

        } else {

            // LOGGER.info("cached preview for $srcFile needs to be created")

            // generate the image,
            // upload the result to the gpu
            // save the file

            try {
                when (val ext = srcFile.extension.lowercase()) {

                    // todo start exe files from explorer
                    // todo preview icon for exe files / links

                    // todo thumbnails and import for .vox files (MagicaVoxel voxel meshes)

                    // todo thumbnails for meshes, and components
                    // todo thumbnails for scripts?
                    // todo thumbnails for Rem's Studio transforms
                    "obj", "fbx", "gltf", "glb", "dae", "md2", "md5mesh" -> {
                        // todo list all mesh extensions, which are supported by assimp
                        // preview for mtl file? idk...
                        generateAssimpMeshFrame(srcFile, dstFile, size, callback)
                    }

                    "hdr" -> {
                        val src = HDRImage(srcFile, true)
                        val sw = src.width
                        val sh = src.height
                        if (max(sw, sh) < size) return generate(srcFile, size / 2, callback)
                        val (w, h) = scale(sw, sh, size)
                        if (w < 2 || h < 2) return
                        val dst = src.createBufferedImage(w, h)
                        saveNUpload(srcFile, dstFile, dst, callback)
                    }
                    "tga" -> {
                        val src = use(srcFile.inputStream()) { TGAImage.read(it, false) }
                        val sw = src.width
                        val sh = src.height
                        if (max(sw, sh) < size) return generate(srcFile, size / 2, callback)
                        val (w, h) = scale(sw, sh, size)
                        if (w < 2 || h < 2) return
                        val dst = src.createBufferedImage(w, h)
                        saveNUpload(srcFile, dstFile, dst, callback)
                    }
                    "svg" -> generateSVGFrame(srcFile, dstFile, size, callback)
                    "pdf" -> {
                        val doc = PDFCache.getDocument(srcFile, false) ?: return
                        transformNSaveNUpload(
                            srcFile, PDFCache.getImage(doc, 1f, 0),
                            dstFile, size, callback
                        )
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
                            println("'$iconFile'")
                            generate(FileReference.getReference(iconFile), size, callback)
                        }
                    }
                    // ImageIO says it can do webp, however it doesn't understand most pics...
                    "webp" -> generateVideoFrame(srcFile, dstFile, size, callback, 0.0)
                    "lnk", "desktop" -> {
                        // not images, and I don't know yet how to get the image from them
                    }
                    else -> { // png, jpg, jpeg, ico
                        val image = try {
                            ImageIO.read(srcFile.inputStream())!!
                        } catch (e: Exception) {
                            try {
                                Imaging.getBufferedImage(srcFile.inputStream())!!
                            } catch (e: Exception) {
                                when (val importType = ext.getImportType()) {
                                    "Video" -> {
                                        LOGGER.info("Generating frame for $srcFile")
                                        generateVideoFrame(srcFile, dstFile, size, callback, 1.0)
                                        null
                                    }
                                    // else nothing to do
                                    else -> {
                                        LOGGER.info("ImageIO failed, Imaging failed, importType '$importType' != getImportType for $srcFile")
                                        null
                                    }
                                }
                            }
                        }
                        if (image != null) {
                            transformNSaveNUpload(srcFile, image, dstFile, size, callback)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                LOGGER.warn("Could not load image from $srcFile: ${e.message}")
            }

        }
    }

    private val LOGGER = LogManager.getLogger(Thumbs::class)

}