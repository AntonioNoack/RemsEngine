package me.anno.extensions

open class FileRegistry<Value> : IFileRegistry<Value> {
    override val readerBySignature = HashMap<String, Value>(64)
    override val readerByFileExtension = HashMap<String, Value>(64)

    override fun registerFileExtensions(fileExtensions: String, reader: Value) {
        register(fileExtensions, reader, readerByFileExtension)
    }

    override fun registerSignatures(signatures: String, reader: Value) {
        register(signatures, reader, readerBySignature)
    }

    private fun register(keys: String, reader: Value, readerByKey: HashMap<String, Value>) {
        for (key in keys.split(',')) {
            readerByKey[key] = reader
        }
    }

    override fun unregisterSignatures(signatures: String) {
        unregister(signatures, readerBySignature)
    }

    override fun unregisterFileExtensions(fileExtensions: String) {
        unregister(fileExtensions, readerByFileExtension)
    }

    private fun unregister(keys: String, readerByKey: HashMap<String, Value>) {
        for (key in keys.split(',')) {
            readerByKey.remove(key)
        }
    }
}