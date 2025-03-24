package me.anno.io.saveable

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.graph.visual.Graph
import me.anno.io.files.ReadLineIterator
import me.anno.io.saveable.Saveable.Companion.IRegistryEntry
import me.anno.io.yaml.generic.SimpleYAMLReader
import me.anno.ui.Style
import me.anno.utils.OS.res
import org.apache.logging.log4j.LogManager
import java.lang.reflect.Constructor

object SaveableRegistry {

    private val LOGGER = LogManager.getLogger(SaveableRegistry::class)

    class LazyRegistryEntry(override val classPath: String) : IRegistryEntry {
        private val clazz by lazy {
            try {
                Class.forName(classPath)
            } catch (ifModuleMissing: ClassNotFoundException) {
                null
            }
        }

        private val constructor by lazy {
            getConstructor() ?: getConstructor(Style::class.java)
        }

        private fun getConstructor(vararg args: Class<*>): Constructor<*>? {
            return try {
                clazz?.getConstructor(*args)
            } catch (e: NoSuchMethodException) {
                null
            }
        }

        private fun newInstance(): Saveable {
            val constructor = constructor
            return if (constructor == null) {
                LOGGER.warn("Missing usable constructor for $classPath")
                UnknownSaveable()
            } else {
                (if (constructor.parameterCount == 0) constructor.newInstance()
                else constructor.newInstance(style)) as? Saveable ?: Saveable()
            }
        }

        override val sampleInstance: Saveable by lazy {
            newInstance()
        }

        override fun generate(): Saveable {
            val sampleInstance = sampleInstance
            return if (sampleInstance is PrefabSaveable && sampleInstance !is Graph) sampleInstance.clone()
            else newInstance()
        }
    }

    fun registerClasses(lines: ReadLineIterator) {
        val pairs = SimpleYAMLReader.read(lines, false)
        for ((name, classPath) in pairs) {
            Saveable.registerCustomClass(name, LazyRegistryEntry(classPath), print = false)
        }
    }

    fun load() {
        registerClasses(
            res.getChild("saveables.yaml")
                .readLinesSync(256)
        )
    }
}