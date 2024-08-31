package me.anno.image

import me.anno.utils.async.Callback

fun interface AsyncImageReader<S> {
    fun read(source: S, callback: Callback<Image>)
}