package me.anno.io.files.inner

import me.anno.io.files.FileReference
import me.anno.utils.structures.Callback

interface IHeavyIterable<Item, Stream : Iterator<Item>, Processable> {
    fun openStream(source: FileReference, callback: Callback<Stream>)
    fun hasInterest(stream: Stream, item: Item): Boolean
    fun process(stream: Stream, item: Item, previous: Processable?, index: Int, total: Int): Processable?
}