package me.anno.tests.image

import me.anno.image.raw.IntImage
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.InnerFolder
import me.anno.utils.OS.documents
import me.anno.video.ImageSequenceMeta

fun main() {
    // todo folder needs to exist...
    val image = IntImage(1, 1, true)
    val sampleFolder = InnerFolder(documents.getChild("sample")) // doesn't need to exist
    for (i in 0 until 100) {
        sampleFolder.createImageChild("$i.jpg", image)
    }
    val file = getReference("$sampleFolder/%.jpg")
    val meta = ImageSequenceMeta(file)
    println(meta.toString())
    /*meta.matches.forEach { (file, _) ->
        val src = ImageIO.read(file)
        val dst = src.withoutAlpha()
        val out = File(file.parentFile, file.nameWithoutExtension + ".jpg")
        ImageIO.write(dst, "jpg", out)
    }*/
}