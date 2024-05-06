package me.anno.extensions

interface IFileRegistry<Value> {
    val readerBySignature : Map<String, Value>
    val readerByFileExtension : Map<String, Value>
    fun registerFileExtensions(fileExtensions: String, reader: Value)
    fun registerSignatures(signatures: String, reader: Value)
    fun unregisterSignatures(signatures: String)
    fun unregisterFileExtensions(fileExtensions: String)
    fun getReader(signature: String?, fileExtension: String): Value? {
        return readerBySignature[signature] ?: readerByFileExtension[fileExtension]
    }
}