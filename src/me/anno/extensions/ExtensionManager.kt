package me.anno.extensions

import org.apache.logging.log4j.LogManager

abstract class ExtensionManager<V: Extension>(val instanceName: String) {

    val loaded = HashSet<V>()

    fun enable(extensions: List<V>){
        onEnable(extensions)
        loaded += extensions
        for(ex in extensions){
            LOGGER.info("Enabled $instanceName \"${ex.name}\"")
        }
    }

    abstract fun onEnable(extensions: List<V>)

    fun disable() {
        val list = loaded.toList()
        onDisable(list)
        for(ex in list){
            LOGGER.info("Disabled $instanceName \"${ex.name}\"")
        }
        loaded.clear()
    }

    abstract fun onDisable(extensions: List<V>)

    operator fun contains(uuid: String) = loaded.any { it.uuid == uuid }

    companion object {
        private val LOGGER = LogManager.getLogger(ExtensionManager::class)
    }

}