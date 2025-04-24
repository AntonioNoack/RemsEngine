package me.anno.extensions

import org.apache.logging.log4j.LogManager

abstract class ExtensionManager<V : Extension>(val instanceName: String) {

    val loaded = HashSet<V>()

    fun enable(extensions: List<V>) {
        printStatus("Enabling", extensions)
        onEnable(extensions)
        loaded.addAll(extensions)
    }

    fun disable(extensions: List<V> = loaded.toList()) {
        printStatus("Disabling", extensions)
        onDisable(extensions)
        if (extensions === loaded) loaded.clear()
        else loaded.removeAll(extensions.toSet())
    }

    private fun printStatus(type: String, extensions: List<V>) {
        if (extensions.isNotEmpty()) {
            LOGGER.info("$type ${instanceName}s ${extensions.map { "\"${it.info?.name}\"" }}")
        }
    }

    abstract fun onEnable(extensions: List<V>)
    abstract fun onDisable(extensions: List<V>)

    operator fun contains(uuid: String): Boolean = loaded.any { it.info?.uuid == uuid }

    companion object {
        private val LOGGER = LogManager.getLogger(ExtensionManager::class)
    }
}