package me.anno.extensions

/**
 * Default implementation for FileReaderRegistry
 * */
open class FileReaderRegistryImpl<Value> : FileReaderRegistry<Value> {
    override val readerBySignature = HashMap<String, ArrayList<Value>>(64)
    override val readerByFileExtension = HashMap<String, ArrayList<Value>>(64)

    override fun registerFileExtensions(fileExtensions: String, reader: Value) {
        register(fileExtensions, reader, readerByFileExtension)
    }

    override fun registerSignatures(signatures: String, reader: Value) {
        register(signatures, reader, readerBySignature)
    }

    private fun register(keys: String, reader: Value, readerByKey: HashMap<String, ArrayList<Value>>) {
        for (key in keys.split(',')) {
            readerByKey.getOrPut(key, ::ArrayList).add(reader)
        }
    }

    override fun unregisterSignatures(signatures: String) {
        unregister(signatures, readerBySignature)
    }

    override fun unregisterFileExtensions(fileExtensions: String) {
        unregister(fileExtensions, readerByFileExtension)
    }

    private fun unregister(keys: String, readerByKey: HashMap<String, *>) {
        for (key in keys.split(',')) {
            readerByKey.remove(key)
        }
    }
}