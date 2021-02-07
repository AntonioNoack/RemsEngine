package me.anno.utils.test

import me.anno.studio.rems.RemsStudio

fun main(){
    val args = "-y -w 100 -h 100 -i C:\\Users\\Antonio\\Desktop\\Root.json".split(' ').toTypedArray()
    RemsStudio.main(args)
}