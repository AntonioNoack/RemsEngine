package me.anno.tests.gmaps

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.utils.structures.Callback
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFX
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32
import me.anno.io.Streams.readLE32F
import me.anno.io.files.FileReference
import me.anno.maths.Maths.align
import me.anno.maths.Maths.hasFlag
import me.anno.utils.Color.black
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.Sleep
import me.anno.utils.types.InputStreams.readNBytes2
import org.joml.Vector3d
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL13C.glCompressedTexImage2D
import java.awt.image.BufferedImage
import java.io.InputStream

fun main() {
    // load all pck files
    val folder = downloads.getChild("gmaps-decent")
    val meshes = ArrayList<Pair<Int, ByteArray>>()
    for (file in folder.listChildren()) {
        if (file.name.startsWith("gmaps") && file.lcExtension == "pck") {
            val input = file.inputStreamSync()
            val files = input.readLE32()
            for (i in 0 until files) {
                val id = input.readLE32()
                val type = input.readLE32()
                val size = (input.readLE32() + 3).and(3.inv())
                val data = input.readNBytes2(size, true)
                when (type) {
                    0 -> meshes.add(Pair(id, data))
                    1 -> images[id] = readImageFile(data.inputStream()).ref
                    else -> throw IllegalStateException()
                }
            }
        }
    }
    // todo why/how was loading all textures preventing them from being loaded???
    /* for (img in images.values) {
        TextureCache[img, 1_000_000_000L, true]
    }*/
    // read meshes like files
    val entity = Entity()
    for ((id, data) in meshes) {
        val mesh = readMeshFile(data.inputStream(), id, folder)
        if (true || mesh.scale.y in 100.0..120.0)
            entity.add(mesh)
    }
    // draw mesh data :)
    testSceneWithUI("GMaps Reader", entity)
}

fun unpack565(v: Int): Int {
    val b = v.and(31).shl(3)
    val g = v.shr(5).and(63).shl(10)
    val r = v.shr(11).and(31).shl(19)
    return r + g + b + black
}

class CompressedTexture(w: Int, h: Int, val format: Int, val data: ByteArray) : Image(w, h, 3, false) {
    override fun getRGB(index: Int): Int {
        throw NotImplementedError()
    }

    override fun createIntImage(): IntImage {
        val tex = Texture2D("gmaps", width, height, 1)
        var isReady = false
        createTexture(tex, false, false) { ready, _ ->
            isReady = ready != null
        }
        Sleep.waitForGFXThread(true) { isReady && tex.isCreated() }
        return tex.createImage(false, false)
    }

    override fun createBufferedImage(): BufferedImage {
        return createIntImage().createBufferedImage()
    }

    override fun createTexture(
        texture: Texture2D, sync: Boolean, checkRedundancy: Boolean,
        callback: Callback<ITexture2D>
    ) {
        if (!GFX.isGFXThread()) {
            GFX.addGPUTask("CompressedTexture", width, height) {
                createTexture(texture, true, checkRedundancy, callback)
            }
        } else {
            texture.beforeUpload(0, 0)
            val tmp = Texture2D.bufferPool[data.size, false, false]
            tmp.put(data).flip()
            GFX.check()
            glCompressedTexImage2D(texture.target, 0, format, width, height, 0, tmp)
            GFX.check()
            Texture2D.bufferPool.returnBuffer(tmp)
            texture.internalFormat = format
            texture.createdW = width
            texture.createdH = height
            // bytes per pixel isn't really correct, just a (bad) guess
            texture.afterUpload(false, 4, 4)
            callback.ok(texture)
        }
    }
}

fun readImageFile(stream: InputStream): Image {
    val w = stream.readLE32()
    val h = stream.readLE32()
    val format = stream.readLE32()
    val dataType = stream.readLE32()
    val length = stream.readLE32()
    val data = stream.readNBytes2(length, true)
    if (true) return CompressedTexture(w, h, format, data)
    return when (format) {
        33776 -> {
            // COMPRESSED_RGB_S3TC_DXT1_EXT
            // 4x4 groups
            var i = 0
            val image = IntArray(align(w, 4) * align(h, 4))
            val colors = IntArray(4)
            for (y0 in 0 until h step 4) {
                for (x0 in 0 until w step 4) {
                    val c0lo = data[i].toInt().and(255)
                    val c0hi = data[i + 1].toInt().and(255)
                    val c1lo = data[i + 2].toInt().and(255)
                    val c1hi = data[i + 3].toInt().and(255)
                    val bits0 = data[i + 4].toInt().and(255)
                    val bits1 = data[i + 5].toInt().and(255)
                    val bits2 = data[i + 6].toInt().and(255)
                    val bits3 = data[i + 7].toInt().and(255)
                    val color0i = c0lo + c0hi.shl(8) // 565
                    val color1i = c1lo + c1hi.shl(8) // 565
                    val color0 = unpack565(color0i)
                    val color1 = unpack565(color1i)
                    val bits = bits0 + bits1.shl(8) + bits2.shl(16) + bits3.shl(24)
                    colors[0] = color0
                    colors[1] = color1
                    // the fast mixing only works because the last two bits of each component are guaranteed to be zero
                    if (color0i > color1i) {
                        colors[2] = (color0 * 3 + color1) shr 2
                        colors[3] = (color0 + color1 * 3) shr 2
                    } else {
                        colors[2] = (color0 + color1) shr 1
                        colors[3] = black
                    }
                    var idx = 0
                    for (dy in 0 until 4) {
                        val i0 = x0 + w * (y0 + dy)
                        for (j in i0 until i0 + 4) {
                            val code = bits.ushr(idx) and 3
                            image[j] = colors[code]
                            idx += 2
                        }
                    }
                    i += 8
                }
            }
            val result = IntImage(w, h, image, false)
            if (false) {
                if (ctr == 0) desktop.getChild("gmaps").tryMkdirs()
                result.write(desktop.getChild("gmaps/${ctr++}.png"))
            }
            result
        }
        else -> throw NotImplementedError("Unknown format $w,$h,$format,$dataType,$length")
    }
}

var ctr = 0
val pos = Vector3d()
val imgToMat = HashMap<Int, FileReference>()
val images = HashMap<Int, FileReference>()

fun readMeshFile(input: InputStream, idx: Int, parent: FileReference): Entity {
    val mode = input.readLE32() // should always be triangle strips
    val type = input.readLE32() // should always be u16
    val texWidth = input.readLE32()
    val texHeight = input.readLE32()
    val px = input.readLE32F()
    val py = input.readLE32F()
    val pz = input.readLE32F()
    val sx = input.readLE32F()
    val sy = input.readLE32F()
    val sz = input.readLE32F()
    if (mode != GL_TRIANGLE_STRIP) throw NotImplementedError("mode: $mode, type: $type, wxh: ${texWidth}x${texHeight}, $px,$py,$pz,$sx,$sy,$sz")
    if (type != GL_UNSIGNED_SHORT) throw NotImplementedError("type: $type")
    val numElements = input.readLE32() / 2
    val elements = ShortArray(numElements) { input.readLE16().toShort() }
    // skip bytes if numElements is odd
    if (numElements.hasFlag(1)) input.readLE16()
    val numAttributes = input.readLE32()
    val buff = ArrayList<ByteArray>()
    val mesh = Mesh()
    idx(mesh, elements)
    for (k in 0 until numAttributes) {
        val index = input.readLE32()
        val components = input.readLE32()
        val dataType = input.readLE32()
        val normalized = input.readLE32()
        val stride = input.readLE32()
        val offset = input.readLE32()
        val dataLength = input.readLE32()
        val data: ByteArray
        if (dataLength < 0) {
            data = buff[-1 - dataLength]
        } else {
            data = input.readNBytes2(dataLength, true)
            buff.add(data)
            // skip bytes for alignment
            val rem = dataLength and 3
            if (rem > 0) {
                for (i in rem until 4) input.read()
            }
        }
        if (components == 4 && dataType == GL_UNSIGNED_BYTE) {
            comp4(mesh, offset, stride, data)
        } else if (components == 2 && dataType == GL_UNSIGNED_SHORT) {
            comp2(mesh, offset, stride, data, texWidth, texHeight)
        } else println("Unknown attribute! $index,$components,$dataType,$normalized,$stride,$offset, len: $dataLength")
    }
    val tex0 = input.readLE32()
    if (tex0 > 0) {
        // load albedo texture
        mesh.material = imgToMat.getOrPut(tex0) {
            val mat = Material()
            if (tex0.hasFlag(1)) {
                // load binary image
                val imgFile = images[tex0]
                if (imgFile != null) {
                    mat.diffuseMap = imgFile
                } else println("Missing $tex0!")
            } else {
                // load normal image
                val imageFile = parent.getChild("img$tex0.png")
                mat.diffuseMap = imageFile
            }
            mat.ref
        }
    }
    val entity = Entity()
    entity.name = "$idx"
    val pos1 = Vector3d(-px.toDouble(), py.toDouble(), pz.toDouble())
    if (pos.length() > 0) {
        pos1.sub(pos)
        entity.position = pos1
    } else {
        pos.set(pos1)
    }
    entity.setScale(sx.toDouble(), sy.toDouble(), sz.toDouble())
    entity.add(MeshComponent(mesh))
    return entity
}

fun comp4(mesh: Mesh, offset: Int, stride: Int, data: ByteArray) {
    val numValues = data.size / stride
    val pos = FloatArray(numValues * 3)
    for (k in 0 until numValues) {
        val i = k * stride + offset
        val j = k * 3
        pos[j + 1] = +data[i].toInt().and(255) / 255f
        pos[j + 0] = -data[i + 1].toInt().and(255) / 255f
        pos[j + 2] = +data[i + 2].toInt().and(255) / 255f
        // data[i + 3] is some sort of sorting id for visibility, 0..7
        // probably for culling :)
    }
    mesh.positions = pos
}

fun comp2(mesh: Mesh, offset: Int, stride: Int, data: ByteArray, texWidth: Int, texHeight: Int) {
    val numValues = data.size / stride
    val uvs = FloatArray(numValues * 3)
    val invW = 1f / (texWidth - 1)
    val invH = 1f / (texHeight - 1)
    for (k in 0 until numValues) {
        val i = k * stride + offset
        val j = k * 2
        uvs[j] = (data[i + 1].toInt().and(255).shl(8) + data[i].toInt().and(255)) * invW
        uvs[j + 1] = (data[i + 3].toInt().and(255).shl(8) + data[i + 2].toInt().and(255)) * invH
    }
    mesh.uvs = uvs
}

fun idx(mesh: Mesh, elements: ShortArray) {
    // mesh.indices = elements.stripToIndexed()
    mesh.indices = IntArray(elements.size) { elements[it].toInt() and 0xffff }
    mesh.drawMode = DrawMode.TRIANGLE_STRIP
}
