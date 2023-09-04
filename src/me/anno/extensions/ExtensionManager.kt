package me.anno.extensions

import org.apache.logging.log4j.LogManager

abstract class ExtensionManager<V : Extension>(val instanceName: String) {

    val loaded = HashSet<V>()

    fun enable(extensions: List<V>) {
        onEnable(extensions)
        loaded += extensions
        for (ex in extensions) {
            LOGGER.info("Enabled $instanceName \"${ex.name}\"")
        }
    }

    fun disable(extensions: List<V> = loaded.toList()) {
        onDisable(extensions)
        for (ex in extensions) {
            LOGGER.info("Disabled $instanceName \"${ex.name}\"")
        }
        if (extensions === loaded) loaded.clear()
        else loaded.removeAll(extensions.toHashSet())
    }

    abstract fun onEnable(extensions: List<V>)
    abstract fun onDisable(extensions: List<V>)

    operator fun contains(uuid: String) = loaded.any { it.uuid == uuid }

    companion object {
        private val LOGGER = LogManager.getLogger(ExtensionManager::class)
    }
}