package me.anno.tests.deflate

import me.anno.io.files.Reference.getReference
import java.util.zip.DeflaterOutputStream

fun main(){

    val src = getReference("C:/XAMPP/htdocs/zip/linux.zip")
    val data = src.listChildren()[0].readBytesSync()

    val dst = src.getParent()!!.getChild("linux.pk")
    val out = DeflaterOutputStream(dst.outputStream())
    out.write(data)
    out.finish()
    out.close()

}