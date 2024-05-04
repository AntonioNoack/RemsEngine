package me.anno.io

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.Saveable.Companion.IRegistryEntry
import me.anno.io.files.ReadLineIterator
import me.anno.io.files.Reference.getReference
import me.anno.ui.Style
import org.apache.logging.log4j.LogManager

object SaveableRegistry {
    private val LOGGER get() = LogManager.getLogger(SaveableRegistry::class)
    private val styleClass = Style::class.java

    class LazyRegistryEntry(override val classPath: String) : IRegistryEntry {
        private val clazz by lazy { Class.forName(classPath) } // is this ok, if it throws???

        private val constructor by lazy {
            clazz.constructors.firstOrNull {
                it.parameterCount == 1 && it.parameters[0].type == styleClass
            } ?: clazz.constructors.first {
                it.parameterCount == 0
            }
        }

        private fun newInstance(): Saveable {
            return (if (constructor.parameterCount == 0) constructor.newInstance()
            else constructor.newInstance(style)) as Saveable
        }

        override val sampleInstance: Saveable by lazy {
            newInstance()
        }

        override fun generate(): Saveable {
            val sampleInstance = sampleInstance
            return if (sampleInstance is PrefabSaveable) sampleInstance.clone()
            else newInstance()
        }
    }

    fun parse(lines: ReadLineIterator, callback: (name: String, classPath: String) -> Unit) {
        for (line in lines) {
            var colonIndex = line.indexOf(':')
            if (colonIndex < 1) continue
            val name = line.substring(0, colonIndex)
            if (line[colonIndex + 1] == ' ') colonIndex++
            val path = line.substring(colonIndex + 1)
            callback(name, path)
        }
    }

    fun register(lines: ReadLineIterator) {
        parse(lines) { name, classPath ->
            Saveable.registerCustomClass(name, LazyRegistryEntry(classPath), print = false)
        }
        LOGGER.info("Registered classes")
    }

    fun load() {
        register(
            getReference("res://saveables.yaml")
                .readLinesSync(256)
        )
    }
}