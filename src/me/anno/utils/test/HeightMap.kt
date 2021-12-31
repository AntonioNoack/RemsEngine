package me.anno.utils.test

import me.anno.image.ImageCPUCache
import me.anno.image.ImageWriter
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.json.ObjectMapper.write
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.hpc.HeavyProcessing
import kotlin.math.roundToInt

fun main() {
    // convert()
    createMesh()
}

fun createMesh() {

    val file = getReference("C:/XAMPP/htdocs/DigitalCampus/images/map/h375.png")
    val image = ImageCPUCache.getImage(file, false)!!

    val w = image.width
    val h = image.height

    /*
    val scene = AIScene.create()
    val node = AINode.create()
    scene.mRootNode(node)

    val meshes = ByteBuffer.allocateDirect(4)
        .order(ByteOrder.nativeOrder())
        .asIntBuffer()
    meshes.put(0, 0)
    node.mMeshes(meshes)

    val material = AIMaterial.create()
    val materials = PointerBuffer.allocateDirect(1)
    materials.put(0, material.address())
    scene.mMaterials(materials)

    val metadata = AIMetaData.create()
    metadata.mKeys(AIString.calloc(1))
    metadata.mValues(AIMetaDataEntry.calloc(1))
    metadata.mNumProperties(0)
    scene.mMetaData(metadata)

    val mesh = AIMesh.create()
    mesh.mMaterialIndex(0)
    val meshesBuffer0 = ByteBuffer.allocateDirect(8)
        .order(ByteOrder.nativeOrder())
    meshesBuffer0.putLong(mesh.address())
    val meshesBuffer = PointerBuffer.create(meshesBuffer0)
    scene.mMeshes(meshesBuffer)

    val vertices = AIVector3D.calloc(w * h)
    var index = 0
    while (vertices.hasRemaining()) {
        val v = vertices.get()
        val i = index % w
        val j = index / w
        val rgb = image.getRGB(i, j)
        val y = (rgb.r() * 256 + rgb.g()) * 0.1f
        v.x(i.toFloat())
        v.y(y)
        v.z(j.toFloat())
        index++
    }
    vertices.flip()
    mesh.mVertices(vertices)

    val faces = AIFace.calloc((w - 1) * (h - 1))
    index = 0
    while (faces.hasRemaining()) {
        val f = faces.get()
        val x = index % (w - 1)
        val y = index / (w - 1)
        val i = x + y * w
        val buffer = ByteBuffer.allocateDirect(4 * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
        buffer.put(0, i)
        buffer.put(1, i + 1)
        buffer.put(2, i + 1 + w)
        buffer.put(3, i + w)
        f.mIndices(buffer)
    }
    faces.flip()
    mesh.mFaces(faces)

    for (i in 0 until Assimp.aiGetExportFormatCount()) {
        println(Assimp.aiGetExportFormatDescription(i)!!.idString())
    }

    println("start writing")

    val format = "fbx"
    val dst = getReference(desktop, "${file.nameWithoutExtension}.fbx").absolutePath
    val rep = Assimp.aiExportScene(scene, format, dst, 0)
    println(rep)

    return*/
    val fileOut = getReference(desktop, "${file.nameWithoutExtension}.obj")
    val out = fileOut.outputStream()
    for (j in 0 until h) {
        for (i in 0 until w) {
            val rgb = image.getRGB(i, j)
            val y = (rgb.r() * 256 + rgb.g()) * 0.1
            out.write("v $i $y $j\n")
        }
    }
    for (y in 1 until h) {
        for (x in 1 until w) {
            val i = x + y * w - w
            out.write("f $i ${i + 1} ${i + w + 1} ${i + w}\n")
        }
    }
    out.close()
}

// https://www.geoportal-th.de/de-de/Downloadbereiche/Download-Offene-Geodaten-Th%C3%BCringen/Download-H%C3%B6hendaten
fun convert() {
    val tw = 3000
    val th = 3000
    val joined = IntArray(tw * th)
    val combined = downloads.getChild("heights-combined-jena.zip")
    val children = combined.listChildren()!!
    HeavyProcessing.processUnbalanced(0, children.size, true) { ix0, ix1 ->
        for (ci in ix0 until ix1) {
            val zip = children[ci]
            val name = zip.name.split('_')
            val x0 = (name[1].toInt() - 680) * 1000
            val y0 = (name[2].toInt() - 5644) * 1000
            val i0 = x0 + y0 * tw
            val data = zip.listChildren()!!.first { it.lcExtension == "xyz" }
            val text = data.readText()
            val lines = text
                .split('\n')
                .filter { it.isNotEmpty() }
            val uniqueX = HashSet<Float>()
            val uniqueY = HashSet<Float>()
            for (line in lines) {
                val split = line.split(' ')
                uniqueX += split[0].toFloat()
                uniqueY += split[1].toFloat()
            }
            val width = uniqueX.size
            val height = uniqueY.size
            val minX = uniqueX.minOrNull()!!
            val maxX = uniqueX.maxOrNull()!!
            val minY = uniqueY.minOrNull()!!
            val maxY = uniqueY.maxOrNull()!!
            for (line in lines) {
                val split = line.split(' ')
                val xf = split[0].toFloat()
                val yf = split[1].toFloat()
                val z = split[2].toFloat()
                val x = ((width - 0.01f) * (xf - minX) / (maxX - minX)).toInt()
                val y = ((height - 0.01f) * (yf - minY) / (maxY - minY)).toInt()
                val i = i0 + x + y * tw
                val h = (z * 10f).roundToInt()
                joined[i] = rgba(h.shr(8).and(255), h.and(255), 0, 255)
            }
        }
    }
    /*ImageWriter.writeRGBImageInt(tw, th, "result3000", 16) { _, _, i ->
        joined[i]
    }
    ImageWriter.writeRGBImageInt(tw / 2, th / 2, "result1500", 16) { x, y, _ ->
        val i = (x * 2) + (y * 2) * tw
        var h = 0
        for (a in 0 until 2) {
            for (b in 0 until 2) {
                val c = joined[i + a + b * tw]
                h += c.r() * 256 + c.g()
            }
        }
        h /= 4
        rgba(h.shr(8).and(255), h.and(255), 0, 255)
    }
    ImageWriter.writeRGBImageInt(tw / 4, th / 4, "result750", 16) { x, y, _ ->
        val i = (x * 4) + (y * 4) * tw
        var h = 0
        for (a in 0 until 4) {
            for (b in 0 until 4) {
                val c = joined[i + a + b * tw]
                h += c.r() * 256 + c.g()
            }
        }
        h /= 16
        rgba(h.shr(8).and(255), h.and(255), 0, 255)
    }*/
    ImageWriter.writeRGBImageInt(tw / 8, th / 8, "result375", 16) { x, y, _ ->
        val i = (x * 8) + (y * 8) * tw
        var h = 0
        for (a in 0 until 8) {
            for (b in 0 until 8) {
                val c = joined[i + a + b * tw]
                h += c.r() * 256 + c.g()
            }
        }
        h /= 64
        rgba(h.shr(8).and(255), h.and(255), 0, 255)
    }
}