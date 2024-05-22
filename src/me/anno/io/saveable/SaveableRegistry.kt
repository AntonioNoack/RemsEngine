package me.anno.io.saveable

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.graph.visual.Graph
import me.anno.io.saveable.Saveable.Companion.IRegistryEntry
import me.anno.io.files.ReadLineIterator
import me.anno.io.files.Reference.getReference
import me.anno.io.yaml.generic.SimpleYAMLReader
import me.anno.ui.Style

object SaveableRegistry {
    private val styleClass = Style::class.java
    private val saveableClass = Saveable::class.java

    class LazyRegistryEntry(override val classPath: String) : IRegistryEntry {
        private val clazz by lazy {
            try {
                Class.forName(classPath)
            } catch (ifModuleMissing: ClassNotFoundException) {
                saveableClass
            }
        }

        private val constructor by lazy {
            clazz.constructors.first {
                (it.parameterCount == 0) || (it.parameterCount == 1 && it.parameters[0].type == styleClass)
            }
        }

        private fun newInstance(): Saveable {
            return (if (constructor.parameterCount == 0) constructor.newInstance()
            else constructor.newInstance(style)) as? Saveable ?: Saveable()
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
        val pairs = SimpleYAMLReader.read(lines)
        for ((name, classPath) in pairs) {
            Saveable.registerCustomClass(name, LazyRegistryEntry(classPath), print = false)
        }
    }

    fun load() {
        registerClasses(
            getReference("res://saveables.yaml")
                .readLinesSync(256)
        )
    }
}