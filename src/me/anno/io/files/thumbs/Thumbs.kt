package me.anno.io.files.thumbs

import me.anno.Build
import me.anno.cache.data.ImageData
import me.anno.cache.instances.MeshCache
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
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshBaseComponent
import me.anno.ecs.components.mesh.shapes.Icosahedron
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabReadable
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.engine.ui.render.Renderers.simpleNormalRenderer
import me.anno.fonts.FontManager
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.isGFXThread
import me.anno.gpu.OpenGL
import me.anno.gpu.OpenGL.depthMode
import me.anno.gpu.OpenGL.renderPurely
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.SVGxGFX
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.copying.FramebufferToMemory.createBufferedImage
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.DrawTextures.drawTransparentBackground
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.drawing.Perspective.setPerspective
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Renderer.Companion.colorRenderer
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.*
import me.anno.image.ImageScale.scaleMax
import me.anno.image.tar.TGAImage
import me.anno.io.ISaveable
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.text.TextReader
import me.anno.io.unity.UnityReader
import me.anno.io.zip.ZipCache
import me.anno.mesh.assimp.AnimGameItem
import me.anno.objects.Video
import me.anno.objects.documents.pdf.PDFCache
import me.anno.objects.meshes.MeshData
import me.anno.ui.base.Font
import me.anno.utils.Color.hex4
import me.anno.utils.ShutdownException
import me.anno.utils.Sleep.waitForGFXThread
import me.anno.utils.Sleep.waitForGFXThreadUntilDefined
import me.anno.utils.Sleep.waitUntilDefined
import me.anno.utils.files.Files.use
import me.anno.utils.input.Input.readNBytes2
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.strings.StringHelper.shorten
import me.anno.utils.types.Strings.getImportType
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import net.boeckling.crc.CRC64
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.joml.Math.sqrt
import org.joml.Math.toRadians
import org.lwjgl.opengl.GL11.*
import sun.awt.shell.ShellFolder
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

    fun invalidate(file: FileReference, neededSize: Int) {
        val size = getSize(neededSize)
        ImageGPUCache.remove {
            val key = it.key
            key is ThumbnailKey && key.file == file && key.size == size
        }
    }

    fun getThumbnail(file: FileReference, neededSize: Int, async: Boolean): ITexture2D? {

        if (file is ImageReadable) {
            return ImageGPUCache.getImage(file, timeout, async)
        }

        // currently not supported
        if (file.isDirectory) return null

        // was deleted
        if (!file.exists) return null

        val size = getSize(neededSize)
        val key = ThumbnailKey(file, file.lastModified, file.isDirectory, size)

        return ImageGPUCache.getLateinitTextureLimited(key, timeout, async, 4) { callback ->
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
    }

    private fun FileReference.getCacheFile(size: Int): FileReference {

        val hashReadLimit = 4096
        val length = this.length()
        var hash: Long = lastModified xor (454781903L * length)
        if (!isDirectory && length > 0) {
            val reader = inputStream().buffered()
            val bytes = reader.readNBytes2(hashReadLimit, false)
            reader.close()
            hash = hash xor CRC64.fromInputStream(bytes.inputStream()).value
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

    private fun upload(srcFile: FileReference, dst: Image, callback: (Texture2D) -> Unit) {
        val rotation = ImageData.getRotation(srcFile)
        GFX.addGPUTask(dst.width, dst.height) {
            val texture = Texture2D(srcFile.name, dst.width, dst.height, 1)
            dst.createTexture(texture, true)
            texture.rotation = rotation
            callback(texture)
        }
    }

    private fun upload(srcFile: FileReference, fb: Framebuffer, callback: (Texture2D) -> Unit) {
        val texture = (fb.msBuffer?.textures ?: fb.textures).first()
        texture.rotation = ImageData.getRotation(srcFile)
        callback(texture)
        GFX.addGPUTask(1) { fb.destroyExceptTextures(true) }
    }

    private fun upload(srcFile: FileReference, dst: BufferedImage, callback: (Texture2D) -> Unit) {
        val rotation = ImageData.getRotation(srcFile)
        if (isGFXThread()) {
            val texture = Texture2D(dst, true)
            texture.rotation = rotation
            callback(texture)
        } else {
            GFX.addGPUTask(dst.width, dst.height) {
                val texture = Texture2D(dst, true)
                texture.rotation = rotation
                callback(texture)
            }
        }
    }

    private fun saveNUpload(
        srcFile: FileReference,
        dstFile: FileReference,
        dst: Image,
        callback: (Texture2D) -> Unit
    ) {
        if (useCacheFolder) {
            dstFile.getParent()!!.mkdirs()
            use(dstFile.outputStream()) { ImageIO.write(dst.createBufferedImage(), destinationFormat, it) }
        }
        upload(srcFile, dst, callback)
    }

    private fun saveNUpload(
        srcFile: FileReference,
        dstFile: FileReference,
        fb: Framebuffer,
        dst: BufferedImage,
        callback: (Texture2D) -> Unit
    ) {
        if (useCacheFolder) {
            // don't wait to upload the image
            thread(name = "Writing ${dstFile.name} for cached thumbs") {
                dstFile.getParent()!!.mkdirs()
                use(dstFile.outputStream()) { ImageIO.write(dst, destinationFormat, it) }
            }
        }
        upload(srcFile, fb, callback)
    }

    private fun saveNUpload(
        srcFile: FileReference,
        dstFile: FileReference,
        dst: BufferedImage,
        callback: (Texture2D) -> Unit
    ) {
        if (useCacheFolder) {
            // don't wait to upload the image
            thread(name = "Writing ${dstFile.name} for cached thumbs") {
                dstFile.getParent()!!.mkdirs()
                use(dstFile.outputStream()) { ImageIO.write(dst, destinationFormat, it) }
            }
        }
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

        // if it matches the size, just upload it
        // we have loaded it anyways already
        if (max(sw, sh) < size) {
            saveNUpload(srcFile, dstFile, src, callback)
            return
            // return generate(srcFile, size / 2, callback)
        }

        val (w, h) = scaleMax(sw, sh, size)
        if (min(w, h) < 1) return
        if (w == sw && h == sh) {
            saveNUpload(srcFile, dstFile, src, callback)
        } else {
            val dst = BufferedImage(w, h, src.type)
            val gfx = dst.createGraphics()
            gfx.drawImage(src, 0, 0, w, h, null)
            gfx.dispose()
            saveNUpload(srcFile, dstFile, dst, callback)
        }
    }

    private fun transformNSaveNUpload(
        srcFile: FileReference,
        src: Image,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        val sw = src.width
        val sh = src.height
        if (min(sw, sh) < 1) return

        // if it matches the size, just upload it
        // we have loaded it anyways already
        if (max(sw, sh) < size) {
            saveNUpload(srcFile, dstFile, src, callback)
            return
            // return generate(srcFile, size / 2, callback)
        }

        val (w, h) = scaleMax(sw, sh, size)
        if (min(w, h) < 1) return
        if (w == sw && h == sh) {
            saveNUpload(srcFile, dstFile, src, callback)
        } else {
            val dst = src.createBufferedImage(w, h)
            saveNUpload(srcFile, dstFile, dst, callback)
        }
    }

    fun renderToBufferedImage(
        srcForRotation: FileReference,
        dstFile: FileReference,
        withDepth: Boolean,
        renderer: Renderer = colorRenderer,
        flipY: Boolean,
        callback: (Texture2D) -> Unit,
        w: Int, h: Int, render: () -> Unit
    ) {
        if (isGFXThread()) {
            renderToBufferedImage2(
                srcForRotation, dstFile, withDepth, renderer,
                flipY, callback, w, h, render
            )
        } else {
            GFX.addGPUTask(w, h) {
                renderToBufferedImage2(
                    srcForRotation, dstFile, withDepth, renderer,
                    flipY, callback, w, h, render
                )
            }
        }
    }

    private fun renderToBufferedImage2(
        srcFile: FileReference,
        dstFile: FileReference,
        withDepth: Boolean,
        renderer: Renderer,
        flipY: Boolean,
        callback: (Texture2D) -> Unit,
        w: Int, h: Int,
        render: () -> Unit
    ) {
        GFX.check()

        val fb2 = FBStack[srcFile.name, w, h, 4, false, 4, withDepth]

        renderPurely {

            if (!withDepth) {
                useFrame(0, 0, w, h, false, fb2, colorRenderer) {
                    drawTransparentBackground(0, 0, w, h)
                }
            }

            useFrame(0, 0, w, h, false, fb2, renderer) {
                if (withDepth) {
                    depthMode.use(DepthMode.GREATER) {
                        Frame.bind()
                        glClearColor(0f, 0f, 0f, 0f)
                        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
                        render()
                    }
                } else render()
            }

        }

        val dst = createBufferedImage(w, h, fb2, flipY, true)
        saveNUpload(srcFile, dstFile, dst, callback)

    }

    private fun loadVideo(srcFile: FileReference) {
        if (isGFXThread()) {
            val video = Video(srcFile)
            for (i in 0 until 20) {
                video.draw(Matrix4fArrayList(), 1.0, Vector4f(1f), Vector4f(1f))
                GFX.workGPUTasks(false)
                Thread.sleep(10)
            }
        }
    }

    fun generateVideoFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit,
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

        loadVideo(srcFile)

        val src = waitForGFXThreadUntilDefined(true) {
            getVideoFrame(srcFile, scale, index, 0, fps, 1000L, true)
        }

        waitForGFXThread(true) { src.isCreated }

        renderToBufferedImage(srcFile, dstFile, false, colorRenderer, false, callback, w, h) {
            drawTexture(src)
        }

    }

    fun generateSVGFrame(
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
        transform.scale((buffer.maxY / buffer.maxX).toFloat(), 1f, 1f)
        renderToBufferedImage(InvalidRef, dstFile, false, colorRenderer, true, callback, w, h) {
            SVGxGFX.draw3DSVG(
                null, 0.0,
                transform, buffer, whiteTexture,
                white4, Filtering.NEAREST,
                whiteTexture.clamping!!, null
            )
        }

    }

    val defaultAngleY = -25f

    private fun createPerspective(y: Float, aspectRatio: Float, stack: Matrix4f) {

        setPerspective(stack, 0.7f, aspectRatio, 0.001f, 10f, 0f, 0f)
        stack.translate(0f, 0f, -1f)// move the camera back a bit
        stack.rotateX(toRadians(15f))// rotate it into a nice viewing angle
        stack.rotateY(toRadians(y))

        // calculate the scale, such that everything can be visible
        // half, because it's half the size, 1.05f for a small border
        stack.scale(1.05f * 0.5f)

    }

    private fun createPerspectiveList(y: Float, aspectRatio: Float): Matrix4fArrayList {
        val stack = Matrix4fArrayList()
        createPerspective(y, aspectRatio, stack)
        return stack
    }

    private fun createPerspective(y: Float, aspectRatio: Float): Matrix4f {
        val stack = Matrix4f()
        createPerspective(y, aspectRatio, stack)
        return stack
    }

    // just render it using the simplest shader
    fun generateFrame(
        dstFile: FileReference,
        data: MeshData,
        size: Int,
        renderer: Renderer,
        waitForTextures: Boolean,
        callback: (Texture2D) -> Unit
    ) {
        if (waitForTextures) waitForTextures(data)
        renderToBufferedImage(InvalidRef, dstFile, true, renderer, true, callback, size, size) {
            data.drawAssimp(
                true, null, createPerspectiveList(defaultAngleY, 1f), 0.0, white4, "",
                useMaterials = true, centerMesh = true, normalizeScale = true, drawSkeletons = false
            )
        }
    }

    private fun waitForTextures(mesh: Mesh) {
        // wait for all textures
        val textures = HashSet<FileReference>()
        for (material in mesh.materials) {
            textures += listTextures(material)
        }
        textures.removeIf { it == InvalidRef }
        textures.removeIf {
            if (!it.exists) {
                LOGGER.warn("Missing texture $it")
                true
            } else false
        }
        // LOGGER.info("Textures: $textures")
        waitForTextures(textures)
        // LOGGER.info("done waiting")
    }

    private fun waitForTextures(data: MeshData) {
        // wait for all textures
        val textures = HashSet<FileReference>()
        collectTextures(data.assimpModel!!.hierarchy, textures)
        textures.removeIf { it == InvalidRef }
        textures.removeIf {
            if (!it.exists) {
                LOGGER.warn("Missing texture $it")
                true
            } else false
        }
        // LOGGER.info("Textures: $textures")
        waitForTextures(textures)
        // LOGGER.info("done waiting")
    }

    private fun collectTextures(entity: Entity, textures: MutableSet<FileReference>) {
        for (comp in entity.getComponentsInChildren(MeshBaseComponent::class, false)) {
            // LOGGER.info("mesh comp ${comp.name}")
            val mesh = comp.getMesh()
            if (mesh == null) {
                LOGGER.warn("Missing mesh $comp")
                continue
            }
            // LOGGER.info("mesh ${comp.mesh} has the materials ${mesh.materials}")
            for (material in mesh.materials) {
                textures += listTextures(material)
            }
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
            PrefabCache.loadPrefab(srcFile)
            // loadAssimpStatic(srcFile, null)
        }.getSampleInstance() as Entity
        // generateFrame(dstFile, data, size, previewRenderer, true, callback)
        generateEntityFrame(dstFile, size, data, callback)
    }

    fun generateVOXMeshFrame(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        val data = waitUntilDefined(true) {
            PrefabCache.loadPrefab(srcFile)
            // loadVOX(srcFile, null)
        }.getSampleInstance() as Entity
        // generateFrame(dstFile, data, size, previewRenderer, true, callback)
        generateEntityFrame(dstFile, size, data, callback)
    }

    fun generateEntityFrame(
        dstFile: FileReference,
        size: Int,
        entity: Entity,
        callback: (Texture2D) -> Unit
    ) {
        val data = MeshData()
        data.assimpModel = AnimGameItem(entity)
        // todo draw gui (colliders), entity positions
        waitForTextures(data)
        entity.validateTransform()
        val drawSkeletons = !entity.hasComponent(MeshBaseComponent::class)
        renderToBufferedImage(InvalidRef, dstFile, true, previewRenderer, true, callback, size, size) {
            data.drawAssimp(
                true, null, createPerspectiveList(defaultAngleY, 1f), 0.0, white4, "",
                useMaterials = true, centerMesh = true, normalizeScale = true, drawSkeletons = drawSkeletons
            )
        }
    }

    fun generateMeshFrame(
        dstFile: FileReference,
        size: Int,
        mesh: Mesh,
        callback: (Texture2D) -> Unit
    ) {
        mesh.checkCompleteness()
        mesh.ensureBuffer()
        waitForTextures(mesh)
        // sometimes black: because of vertex colors, which are black
        // render everything without color
        renderToBufferedImage(InvalidRef, dstFile, true, simpleNormalRenderer, true, callback, size, size) {
            mesh.drawAssimp(
                createPerspective(defaultAngleY, 1f),
                useMaterials = true,
                centerMesh = true,
                normalizeScale = true
            )
        }
    }

    private val materialCamTransform = createPerspective(0f, 1f).scale(0.62f)

    // todo if we have preview images, we could use them as cheaper textures
    // todo for some scales, the image is just blank
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
        sphereMesh.ensureBuffer()
        val mesh = sphereMesh.clone()
        mesh.material = srcFile
        waitForTextures(material)
        renderToBufferedImage(InvalidRef, dstFile, true, previewRenderer, true, callback, size, size) {
            OpenGL.blendMode.use(BlendMode.DEFAULT) {
                mesh.drawAssimp(materialCamTransform, useMaterials = true, centerMesh = false, normalizeScale = false)
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
        dstFile: FileReference,
        materials: List<FileReference>,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        sphereMesh.ensureBuffer()
        waitForTextures(materials.mapNotNull { MaterialCache[it] })
        renderMultiWindowImage(
            dstFile, materials.size, size, false,
            previewRenderer, callback
        ) { it, _ ->
            OpenGL.blendMode.use(BlendMode.DEFAULT) {
                val mesh = sphereMesh.clone()
                mesh.material = materials[it]
                mesh.drawAssimp(
                    materialCamTransform,
                    useMaterials = true,
                    centerMesh = false,
                    normalizeScale = false
                )
            }
        }
    }

    fun renderMultiWindowImage(
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
        renderToBufferedImage(
            InvalidRef, dstFile, true, renderer0, true,
            callback, w, h
        ) {
            val frame = OpenGL.currentBuffer!!
            val renderer = OpenGL.currentRenderer
            for (i in 0 until count) {
                val ix = i % sx
                val iy = i / sx
                val x0 = ix * sizePerElement
                val y0 = (iy * h) / sy
                val y1 = (iy + 1) * h / sy
                useFrame(x0, y0, sizePerElement, y1 - y0, false, frame, renderer) {
                    drawFunction(i, aspect)
                }
            }
        }
    }

    fun generateSkeletonFrame(
        dstFile: FileReference,
        skeleton: Skeleton,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {

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
        generateMeshFrame(dstFile, size, mesh, callback)
    }

    fun generateAnimationFrame(
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
        val dt = if (hasMotion) duration / count else 0.0
        val bones = skeleton.bones
        val boneCount = bones.size // actually just joints
        val meshVertices = FloatArray(boneCount * boneMeshVertices.size)
        mesh.positions = meshVertices
        val skinningMatrices = Array(boneCount) { Matrix4x3f() }
        val animPositions = Array(boneCount) { Vector3f() }
        renderMultiWindowImage(dstFile, count, size, true, simpleNormalRenderer, callback) { it, aspect ->
            val time = it * dt
            // generate the matrices
            animation.getMatrices(entity, time.toFloat(), skinningMatrices)
            // apply the matrices to the bone positions
            for (i in animPositions.indices) {
                val position = animPositions[i].set(bones[i].bindPosition)
                skinningMatrices[i].transformPosition(position)
            }
            generateSkeleton(bones, animPositions, meshVertices, null)
            mesh.invalidateGeometry()
            // draw the skeleton in that portion of the frame
            mesh.ensureBuffer()
            mesh.drawAssimp(
                createPerspective(defaultAngleY, aspect),
                useMaterials = false,
                centerMesh = true,
                normalizeScale = true
            )
        }
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
        val endTime = GFX.gameTime + timeout * 1e6.toLong()
        waitForGFXThread(true) {
            if (GFX.gameTime > endTime) {
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
            is Mesh -> generateMeshFrame(dstFile, size, asset, callback)
            is Material -> generateMaterialFrame(srcFile, dstFile, asset, size, callback)
            is Skeleton -> generateSkeletonFrame(dstFile, asset, size, callback)
            is Animation -> generateAnimationFrame(dstFile, asset, size, callback)
            is Entity -> generateEntityFrame(dstFile, size, asset, callback)
            is Component -> {
                // todo render component somehow... just return an icon?
                // todo render debug ui :)
            }
            is MeshBaseComponent -> {
                generateMeshFrame(dstFile, size, asset.getMesh() ?: return, callback)
            }
            is Prefab -> {
                val instance = asset.getSampleInstance()
                generateSomething(instance, srcFile, dstFile, size, callback)
            }
            else -> {
                LOGGER.warn("Unknown item from prefab: ${asset?.className}")
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
                GFX.addGPUTask(image.width, image.height) {
                    val texture = Texture2D(image, true)
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

    private fun generate(srcFile: FileReference, size: Int, callback: (Texture2D) -> Unit) {

        if (size < 3) return

        val dstFile = srcFile.getCacheFile(size)
        if (returnIfExists(srcFile, dstFile, callback)) return

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
                transformNSaveNUpload(srcFile, image, dstFile, size, callback)
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
                saveNUpload(srcFile, dstFile, dst, callback)
            }
            "pdf" -> {
                val ref = PDFCache.getDocumentRef(srcFile, borrow = true, async = false) ?: return
                val image = PDFCache.getImageCachedBySize(ref.doc, size, 0)
                ref.returnInstance()
                saveNUpload(srcFile, dstFile, image, callback)
            }
            "png", "jpg", "bmp", "ico", "psd" -> generateImage(srcFile, dstFile, size, callback)
            "blend" -> generateSomething(
                PrefabCache.getPrefabPair(srcFile, null)?.instance,
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

                    // todo start exe files from explorer
                    // todo preview icon for exe files / links

                    // done thumbnails and import for .vox files (MagicaVoxel voxel meshes)

                    // todo thumbnails for meshes, and components
                    // todo thumbnails for scripts?
                    // todo thumbnails for Rem's Studio transforms
                    "obj", "fbx", "gltf", "glb", "dae", "md2", "md5mesh" -> {
                        // todo list all mesh extensions, which are supported by assimp
                        // preview for mtl file? idk...
                        generateAssimpMeshFrame(srcFile, dstFile, size, callback)
                    }
                    "mat", "prefab", "unity", "asset", "controller" -> {
                        try {
                            // parse unity files
                            val decoded = UnityReader.readAsAsset(srcFile)
                            if (decoded != InvalidRef) {
                                when {
                                    decoded is PrefabReadable ->
                                        generateSomething(decoded.readPrefab(), srcFile, dstFile, size, callback)
                                    decoded.length() > 0 -> {
                                        // try to read the file as an asset
                                        val sth = TextReader.read(decoded).firstOrNull()
                                        generateSomething(sth, srcFile, dstFile, size, callback)
                                    }
                                    else -> LOGGER.warn("File $decoded is empty")
                                }
                            } else LOGGER.warn("Could not understand unity asset $srcFile, result is InvalidRef")
                        } catch (e: Throwable) {
                            LOGGER.warn("$e in $srcFile")
                            e.printStackTrace()
                        }
                    }
                    "json" -> {
                        try {
                            // try to read the file as an asset
                            val data = PrefabCache.getPrefabPair(srcFile, HashSet())
                            val something = data?.prefab ?: data?.instance
                            generateSomething(something, srcFile, dstFile, size, callback)
                        } catch (e: Throwable) {
                            LOGGER.info("${e.message} in $srcFile")
                            e.printStackTrace()
                        }
                    }
                    "tga" -> {
                        val src = use(srcFile.inputStream()) { TGAImage.read(it, false) }
                        val dst = findScale(src, srcFile, size, callback) ?: return
                        saveNUpload(srcFile, dstFile, dst, callback)
                    }
                    "svg" -> generateSVGFrame(srcFile, dstFile, size, callback)
                    "mtl" -> {
                        // read as folder
                        val children = ZipCache.unzip(srcFile, false)?.listChildren() ?: emptyList()
                        if (children.isNotEmpty()) {
                            val maxSize = 25 // with more, too many details are lost
                            generateMaterialFrame(
                                dstFile, if (children.size < maxSize) children else
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
                    "txt", "html", "md" -> generateTextImage(srcFile, size, callback)
                    // png, jpg, jpeg, ico, webp, mp4, ...
                    else -> generateImage(srcFile, dstFile, size, callback)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                LOGGER.warn("Could not load image from $srcFile: ${e.message}")
            }
        }
    }

    private fun generateTextImage(
        srcFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        // todo html preview???
        // todo markdown preview (?)
        // generate text preview
        // scale text with size?
        val maxLineCount = clamp((size + 8) / 16, 3, 40)
        val maxLineLength = maxLineCount * 5 / 2
        val maxLength = min(2048, maxLineCount * maxLineLength)
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
            val key = Font(DefaultConfig.defaultFontName, size * 1.5f / lineCount, isBold = false, isItalic = false)
            val font2 = FontManager.getFont(key)
            GFX.addGPUTask(100) {
                GFX.loadTexturesSync.push(true)
                val texture = font2.generateTexture(
                    text, key.size, -1, -1,
                    portableImages = true,
                    textColor = 255 shl 24,
                    backgroundColor = -1,
                    extraPadding = key.sizeInt / 2
                )
                if (texture != null) callback(texture as Texture2D)
                GFX.loadTexturesSync.pop()
            }
        }
    }

    private fun generateImage(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        // small timeout, because we need that image shortly only
        val totalMillis = 30_000L
        val smallStep = 5L
        val tries = totalMillis / smallStep
        var image: Image? = null
        for (i in 0 until tries) {
            image = ImageCPUCache.getImage(srcFile, 50, true)
            if (image != null) break
            Thread.sleep(smallStep)
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
        } else {
            transformNSaveNUpload(srcFile, image, dstFile, size, callback)
        }
    }

    private fun generateSystemIcon(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        val icon = srcFile.toFile {
            try {
                val sf = ShellFolder.getShellFolder(it)
                ImageIcon(sf.getIcon(true))
            } catch (e: Exception) {
                FileSystemView.getFileSystemView().getSystemIcon(it)
            }
        }
        val image = BufferedImage(icon.iconWidth + 2, icon.iconHeight + 2, 2)
        val gfx = image.createGraphics()
        icon.paintIcon(null, gfx, 1, 1)
        gfx.dispose()
        // respect the size
        transformNSaveNUpload(srcFile, image, dstFile, size, callback)
    }

}