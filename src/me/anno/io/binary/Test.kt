package me.anno.io.binary

import me.anno.io.text.TextWriter
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.objects.Video
import me.anno.objects.geometric.Circle
import me.anno.objects.geometric.Polygon
import me.anno.objects.particles.ParticleSystem
import me.anno.objects.text.Text
import me.anno.utils.FileHelper.use
import me.anno.utils.OS
import java.io.*
import java.util.zip.DeflaterOutputStream

fun main(){

    val candidates = listOf(
        Video(),
        Text(),
        ParticleSystem(),
        Transform(),
        Camera(),
        Polygon(),
        Video(),
        Text(),
        ParticleSystem(),
        Transform(),
        Camera(),
        Polygon(),
        Transform(),
        Text(),
        ParticleSystem(),
        Transform(),
        Camera(),
        Polygon(),
        Video(),
        Text(),
        ParticleSystem(),
        Transform(),
        Camera(),
        Polygon(),
        Circle()
    )

    // load all files into the cache
    candidates.forEach { it.save(BinaryWriter(DataOutputStream(ByteArrayOutputStream()))) }

    val file = File(OS.desktop, "raw.bin")

    // binary
    val bin0 = System.nanoTime()
    var bos: ByteArrayOutputStream
    lateinit var binaryValue: ByteArray
    for(i in 0 until 100){
        bos = ByteArrayOutputStream(4096)
        use(DataOutputStream(bos)){ dos ->
            val writer = BinaryWriter(dos)
            candidates.forEach { writer.add(it) }
            writer.writeAllInList()
        }
        binaryValue = bos.toByteArray()
    }
    val binaryWriteTime = System.nanoTime() - bin0

    file.writeBytes(binaryValue)

    // load all files into the cache
    candidates.forEach { it.save(TextWriter(false)) }

    // text
    val text0 = System.nanoTime()
    lateinit var textValue: String
    for(i in 0 until 100){
        val writer = TextWriter(false)
        candidates.forEach { writer.add(it) }
        writer.writeAllInList()
        textValue = writer.toString()
    }
    val textWriteTime = System.nanoTime() - text0

    println("text write time: ${textWriteTime/1e9}")
    println("binary write time: ${binaryWriteTime/1e9}")

    println("length as text: ${textValue.length}")
    println("length in binary: ${binaryValue.size}")

    println("length text, compressed: ${compress(textValue.toByteArray())}")
    println("length binary, compressed: ${compress(binaryValue)}")

    use(DataInputStream(ByteArrayInputStream(binaryValue))){ dis ->
        val reader = BinaryReader(dis)
        reader.readAllInList()
        val content = reader.sortedContent
        for(c in content) println(c.getClassName())
    }

}

fun compress(bytes: ByteArray): Int {
    val out0 = ByteArrayOutputStream()
    val out = DeflaterOutputStream(out0)
    out.write(bytes)
    out.finish()
    return out0.toByteArray().size
}