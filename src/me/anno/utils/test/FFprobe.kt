package me.anno.utils.test

import me.anno.utils.OS
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.video.FFMPEGMetadata

fun main(){
    FFMPEGMetadata(getReference(OS.pictures, "Video Projects\\Cities_ Skylines 2020-01-06 19-32-23.mp4"))
}