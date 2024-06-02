package me.anno.extensions

import me.anno.io.files.Signature

/**
 * Files are identified by extension or signature.
 * This interface represents a registry, where either one can be registered.
 *
 * Signature has a higher weight than extension, because it's harder to tamper with without invalidating the file.
 * */
interface FileReaderRegistry<Value> {
    val readerBySignature : Map<String, Value>
    val readerByFileExtension : Map<String, Value>
    fun registerFileExtensions(fileExtensions: String, reader: Value)
    fun registerSignatures(signatures: String, reader: Value)
    fun unregisterSignatures(signatures: String)
    fun unregisterFileExtensions(fileExtensions: String)
    fun getReader(signature: Signature?, fileExtension: String): Value? {
        return readerBySignature[signature?.name] ?: readerByFileExtension[fileExtension]
    }
}