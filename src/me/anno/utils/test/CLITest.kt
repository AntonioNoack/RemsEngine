package me.anno.utils.test

import me.anno.studio.rems.RemsStudio
import me.anno.utils.OS
import java.io.File

fun main(){
    val argsList = ArrayList<String>()
    argsList += "-y"
    argsList += listOf("-w", "512")
    argsList += listOf("-h", "512")
    argsList += listOf("-i", File(OS.documents, "RemsStudio/Audio Tests/scenes/root.json").toString())
    argsList += listOf("-o", File(OS.desktop, "output.mp4").toString())
    val args = argsList.toTypedArray()
    RemsStudio.main(args)
}