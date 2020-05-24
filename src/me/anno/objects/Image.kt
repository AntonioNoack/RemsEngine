package me.anno.objects

import java.io.File

class Image(var file: File, parent: Transform?): Transform(parent){

    init {

    }

    // todo via URL? no
    // todo if given an url, download the file and store it in the project folder

    override fun getClassName(): String = "Image"



}