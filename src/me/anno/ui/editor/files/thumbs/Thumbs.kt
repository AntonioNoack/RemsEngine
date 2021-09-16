package me.anno.ui.editor.files.thumbs

import me.anno.cache.data.ImageData
import me.anno.cache.instances.LastModifiedCache
import me.anno.cache.instances.MeshCache
import me.anno.cache.instances.VideoCache.getVideoFrame
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
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.Icosahedron
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabReadable
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.engine.ui.render.Renderers.simpleNormalRenderer
import me.anno.gpu.*
import me.anno.gpu.GFX.isGFXThread
import me.anno.gpu.RenderState.depthMode
import me.anno.gpu.RenderState.renderPurely
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.copying.FramebufferToMemory.createBufferedImage
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.drawing.Perspective.setPerspective
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Renderer.Companion.colorRenderer
import me.anno.gpu.texture.*
import me.anno.gpu.texture.Texture2D.Companion.packAlignment
import me.anno.image.*
import me.anno.image.tar.TGAImage
import me.anno.io.ISaveable
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.text.TextReader
import me.anno.io.unity.UnityReader
import me.anno.io.zip.ZipCache
import me.anno.mesh.assimp.AnimGameItem
import me.anno.objects.Video
import me.anno.objects.documents.pdf.PDFCache
import me.anno.objects.meshes.MeshData
import me.anno.studio.Build
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.hex4
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import me.anno.utils.Sleep.waitForGFXThread
import me.anno.utils.Sleep.waitForGFXThreadUntilDefined
import me.anno.utils.Sleep.waitUntilDefined
import me.anno.utils.files.Files.use
import me.anno.utils.hpc.Threads.threadWithName
import me.anno.utils.image.ImageScale.scale
import me.anno.utils.input.readNBytes2
import me.anno.utils.types.Strings.getImportType
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import net.boeckling.crc.CRC64
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.joml.Math.sqrt
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

    // todo right click option in file explorer to invalidate a thumbs image

    private val folder = ConfigBasics.cacheFolder.getChild("thumbs")
    private val sizes = intArrayOf(32, 64, 128, 256, 512)
    private val neededSizes = IntArray(sizes.last() + 1)
    private const val timeout = 5000L

    // todo disable this, when everything works
    var useCacheFolder = !Build.isDebug

    init {
        LogManager.disableLogger("GlyphRenderer")
        LogManager.disableLogger("PDSimpleFont")
        if (!useCacheFolder) {
            folder.listChildren()?.forEach { it.deleteRecursively() }
        }
    }

    private fun FileReference.getCacheFile(size: Int): FileReference {

        val hashReadLimit = 256
        val info = LastModifiedCache[this]
        val length = this.length()
        var hash: Long = info.lastModified xor (454781903L * length)
        if (!info.isDirectory && length > 0) {
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

    fun invalidate(file: FileReference, neededSize: Int) {
        val size = getSize(neededSize)
        val key = ThumbnailKey(file, size)
        ImageGPUCache.removeEntry(key)
    }

    fun getThumbnail(file: FileReference, neededSize: Int, async: Boolean): ITexture2D? {

        if (file is ImageReadable) {
            return ImageGPUCache.getImage(file, timeout, async)
        }

        val size = getSize(neededSize)
        val key = ThumbnailKey(file, size)
        return ImageGPUCache.getLateinitTexture(key, timeout, async) { callback ->
            if (async) {
                thread(name = key.file.nameWithoutExtension) {
                    generate(file, size, callback)
                }
            } else generate(file, size, callback)
        }?.texture
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

        val (w, h) = scale(sw, sh, size)
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
        val buffer = IntArray(w * h)
        if (isGFXThread()) {
            renderToBufferedImage2(
                srcForRotation, dstFile, withDepth, renderer,
                flipY, callback, w, h, buffer, render
            )
        } else {
            GFX.addGPUTask(w, h) {
                renderToBufferedImage2(
                    srcForRotation, dstFile, withDepth, renderer,
                    flipY, callback, w, h, buffer, render
                )
            }
        }
    }

    /*fun BufferedImage.flipY() {
        for (y0 in 0 until height / 2) {
            val y1 = height - 1 - y0
            for (x in 0 until width) {
                val rgb0 = getRGB(x, y0)
                setRGB(x, y0, getRGB(x, y1))
                setRGB(x, y1, rgb0)
            }
        }
    }*/

    private fun renderToBufferedImage2(
        srcFile: FileReference,
        dstFile: FileReference,
        withDepth: Boolean,
        renderer: Renderer,
        flipY: Boolean,
        callback: (Texture2D) -> Unit,
        w: Int, h: Int,
        buffer: IntArray,
        render: () -> Unit
    ) {
        GFX.check()

        val fb2 = Framebuffer(
            "generateVideoFrame", w, h, 4, 1, false,
            if (withDepth) DepthBufferType.TEXTURE else DepthBufferType.NONE
        )

        renderPurely {

            if (!withDepth) {
                useFrame(0, 0, w, h, false, fb2, colorRenderer) {
                    drawTexture(
                        0, 0, w, h,
                        TextureLib.colorShowTexture,
                        -1, Vector4f(4f * w.toFloat() / h, 4f, 0f, 0f)
                    )
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
                } else {
                    render()
                }
            }

            if (true || w > GFX.width || h > GFX.height) {
                fb2.bindTextures(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                val dst = createBufferedImage(w, h, fb2, flipY, true)
                saveNUpload(srcFile, dstFile, dst, callback)
            } else {
                // cannot read from separate framebuffer, only from null... why ever...
                useFrame(0, 0, w, h, false, null, colorRenderer) {

                    GFX.copy(fb2)

                    glFlush(); glFinish() // wait for everything to be drawn

                    GFX.check()

                    packAlignment(4 * w)
                    glReadPixels(0, GFX.height - h, w, h, GL_RGBA, GL_UNSIGNED_BYTE, buffer)

                    GFX.check()

                }

                threadWithName("Thumbs::renderToBufferedImage()") {
                    val dst = BufferedImage(w, h, 2)
                    val buffer2 = dst.raster.dataBuffer
                    if (flipY) {
                        var i = 0
                        val dy = h - 1
                        for (y in 0 until h) {
                            for (x in 0 until w) {
                                val col = buffer[i]
                                // swizzle colors, because rgba != argb
                                // and flip y
                                buffer2.setElem(x + (dy - y) * w, rgba(col.b(), col.g(), col.r(), col.a()))
                                i++
                            }
                        }
                    } else {
                        for (i in 0 until w * h) {
                            val col = buffer[i]
                            // swizzle colors, because rgba != argb
                            buffer2.setElem(i, rgba(col.b(), col.g(), col.r(), col.a()))
                        }
                    }
                    saveNUpload(srcFile, dstFile, fb2, dst, callback)
                }

            }


        }


    }

    fun loadVideo(srcFile: FileReference) {
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

        // todo this is a bad option
        val meta = getMeta(srcFile, false)!!
        if (max(meta.videoWidth, meta.videoHeight) < size) return generate(srcFile, size / 2, callback)

        val scale = floor(max(meta.videoWidth, meta.videoHeight).toFloat() / size).toInt()

        val sw = meta.videoWidth / scale
        val sh = meta.videoHeight / scale

        val (w, h) = scale(sw, sh, size)
        if (w < 2 || h < 2) return

        if (w > GFX.width || h > GFX.height) {
            TODO("change reading to the generic function, which works for all sizes")
        }

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

    fun createPerspective(y: Float, aspectRatio: Float, stack: Matrix4f) {

        setPerspective(stack, 0.7f, aspectRatio, 0.001f, 10f)
        stack.translate(0f, 0f, -1f)// move the camera back a bit
        stack.rotateX(toRadians(15f))// rotate it into a nice viewing angle
        stack.rotateY(toRadians(y))

        // calculate the scale, such that everything can be visible
        // half, because it's half the size, 1.05f for a small border
        stack.scale(1.05f * 0.5f)

    }

    fun createPerspectiveList(y: Float, aspectRatio: Float): Matrix4fArrayList {
        val stack = Matrix4fArrayList()
        createPerspective(y, aspectRatio, stack)
        return stack
    }

    fun createPerspective(y: Float, aspectRatio: Float): Matrix4f {
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

    fun waitForTextures(data: MeshData) {
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
        for (comp in entity.getComponentsInChildren(MeshComponent::class, false)) {
            // LOGGER.info("mesh comp ${comp.name}")
            val mesh = me.anno.ecs.components.cache.MeshCache[comp.mesh]
            if (mesh == null) {
                LOGGER.warn("Missing mesh ${comp.mesh}")
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
        // todo draw gui, entity positions
        waitForTextures(data)
        val drawSkeletons = !entity.hasComponent(MeshComponent::class)
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
        // sometimes black: because of vertex colors, which are black
        // render everything without color
        renderToBufferedImage(InvalidRef, dstFile, true, simpleNormalRenderer, true, callback, size, size) {
            mesh.drawAssimp(
                createPerspective(defaultAngleY, 1f),
                useMaterials = false,
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
            RenderState.blendMode.use(BlendMode.DEFAULT) {
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
            RenderState.blendMode.use(BlendMode.DEFAULT) {
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
            val frame = RenderState.currentBuffer!!
            val renderer = RenderState.currentRenderer
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

    val sphereMesh = Icosahedron.createMesh(30, 30)

    fun generateSomething(
        asset: ISaveable?,
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        when (asset) {
            is Mesh -> generateMeshFrame(dstFile, size, asset, callback)
            is Material -> generateMaterialFrame(srcFile, dstFile, size, callback)
            is Skeleton -> generateSkeletonFrame(dstFile, asset, size, callback)
            is Animation -> generateAnimationFrame(dstFile, asset, size, callback)
            is Entity -> generateEntityFrame(dstFile, size, asset, callback)
            is Component -> {
                // todo render component somehow... just return an icon?
            }
            is Prefab -> {
                val instance = asset.getSampleInstance()
                generateSomething(instance, srcFile, dstFile, size, callback)
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
        val (w, h) = scale(sw, sh, size)
        if (w < 2 || h < 2) return null
        return src.createBufferedImage(w, h)
    }

    // png/bmp/jpg?
    private const val destinationFormat = "png"
    private fun generate(srcFile: FileReference, size: Int, callback: (Texture2D) -> Unit) {

        if (size < 3) return

        val dstFile = srcFile.getCacheFile(size)
        if (returnIfExists(srcFile, dstFile, callback)) return

        if (srcFile.isDirectory) {
            // todo thumbnails for folders: what files are inside, including their preview images
            return
        }

        // LOGGER.info("cached preview for $srcFile needs to be created")

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
                val image = srcFile.readPrefab()
                generateSomething(image, srcFile, dstFile, size, callback)
                return
            }
        }

        val signature = Signature.find(srcFile)
        when (signature?.name) {
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
            "blend" -> generateSomething(PrefabCache.getPrefabPair(srcFile)?.second, srcFile, dstFile, size, callback)
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
                                if (decoded.length() > 0) {
                                    // try to read the file as an asset
                                    val sth = TextReader.read(decoded).firstOrNull()
                                    generateSomething(sth, srcFile, dstFile, size, callback)
                                } else LOGGER.warn("File $decoded is empty")
                            }
                        } catch (e: Throwable) {
                            LOGGER.warn("$e in $srcFile")
                            e.printStackTrace()
                        }
                    }
                    "json" -> {
                        try {
                            // try to read the file as an asset
                            val data = PrefabCache.getPrefabPair(srcFile)
                            val something = data?.first ?: data?.second
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
                        val children = ZipCache.getMeta(srcFile, false)?.listChildren() ?: emptyList()
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
                            LOGGER.info("'$iconFile'")
                            generate(FileReference.getReference(iconFile), size, callback)
                        }
                    }
                    // ImageIO says it can do webp, however it doesn't understand most pics...
                    "webp" -> generateVideoFrame(srcFile, dstFile, size, callback, 0.0)
                    "lnk", "desktop" -> {
                        // not images, and I don't know yet how to get the image from them
                    }
                    // png, jpg, jpeg, ico, webp, mp4, ...
                    else -> generateImage(srcFile, dstFile, size, callback)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                LOGGER.warn("Could not load image from $srcFile: ${e.message}")
            }
        }
    }

    private fun generateImage(
        srcFile: FileReference,
        dstFile: FileReference,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        val image = ImageCPUCache.getImage(srcFile, false)
        if (image == null) {
            val ext = srcFile.lcExtension
            when (val importType = ext.getImportType()) {
                "Video" -> {
                    LOGGER.info("Generating frame for $srcFile")
                    generateVideoFrame(srcFile, dstFile, size, callback, 1.0)
                }
                // else nothing to do
                else -> LOGGER.info("ImageIO failed, Imaging failed, importType '$importType' != getImportType for $srcFile")
            }
        } else {
            transformNSaveNUpload(srcFile, image, dstFile, size, callback)
        }
    }

    private val LOGGER = LogManager.getLogger(Thumbs::class)

}