package me.anno.extensions

import me.anno.io.files.Signature

/**
 * Files are identified by extension or signature.
 * This interface represents a registry, where either one can be registered.
 *
 * Signature has a higher weight than extension, because it's harder to tamper with without invalidating the file.
 * */
interface FileReaderRegistry<Value> {
    val readerBySignature: Map<String, List<Value>>
    val readerByFileExtension: Map<String, List<Value>>

    fun registerFileExtensions(fileExtensions: String, reader: Value)
    fun registerSignatures(signatures: String, reader: Value)

    fun unregisterSignatures(signatures: String)
    fun unregisterFileExtensions(fileExtensions: String)

    fun getReaders(signature: Signature?, fileExtension: String): List<Value> {
        val bySig = readerBySignature[signature?.name]
        val byFE = readerByFileExtension[fileExtension]
        if (bySig == null || byFE == null) return bySig ?: byFE ?: emptyList()
        return bySig + byFE
    }
}