package me.anno.extensions

open class FileRegistry<Value> : IFileRegistry<Value> {
    override val readerBySignature = HashMap<String, Value>(64)
    override val readerByFileExtension = HashMap<String, Value>(64)

    override fun registerFileExtensions(fileExtensions: String, reader: Value) {
        for (fileExtension in fileExtensions.split(',')) {
            readerBySignature[fileExtension] = reader
        }
    }

    override fun registerSignatures(signatures: String, reader: Value) {
        for (signature in signatures.split(',')) {
            readerBySignature[signature] = reader
        }
    }

    override fun unregisterSignatures(signatures: String) {
        for (signature in signatures.split(',')) {
            readerBySignature.remove(signature)
        }
    }

    override fun unregisterFileExtensions(fileExtensions: String) {
        for (fileExtension in fileExtensions.split(',')) {
            readerByFileExtension.remove(fileExtension)
        }
    }
}