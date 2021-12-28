package me.anno.io.zip

import me.anno.io.files.FileReference

interface IHeavyIterable<Item, Stream : Iterator<Item>, Processable> {
    fun openStream(source: FileReference): Stream
    fun hasInterest(stream: Stream, item: Item): Boolean
    fun process(stream: Stream, item: Item, previous: Processable?, index: Int, total: Int): Processable?
    fun closeStream(source: FileReference, stream: Stream)
    fun onOpen(stream: Iterator<Item>) {}
    fun onClose(stream: Iterator<Item>) {}
}