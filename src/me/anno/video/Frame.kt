package me.anno.video

abstract class Frame(val w: Int, val h: Int){
    var isLoaded = false
    abstract fun bind(offset: Int)
    abstract fun destroy()
}