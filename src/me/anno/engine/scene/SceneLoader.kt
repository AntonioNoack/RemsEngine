package me.anno.engine.scene

import me.anno.ecs.Entity
import me.anno.io.files.FileReference
import me.anno.io.text.TextReader

// todo in Unity, there is also the state "was changed, but is the same as the original"
//  - we should have that! how do we keep track of that? we need another property, I think

class SceneLoader {

    fun load2(file: FileReference) {

    }

    // entity
    //      components; maybe inherited, maybe custom
    //      children entities; maybe inherited, maybe custom

    // todo load the scene from a tree of files
    // todo for the shipped game, pack all scene files into a separate zip file,
    // todo and preload all values, so we get faster access times

    fun load(file: FileReference): Entity {
        // todo load the basic values, json
        // todo load all children: either json or a reference
        // todo or just load all, and then replace the references? yes :)
        val entity = TextReader.read(file.readText()).first() as Entity
        entity.depthFirstTraversal(true) {
            val components = it.components
            for (index in components.indices) {
                // todo also load all super properties...
                val component = components[index]
                if (component is RefComponent) {

                }
            }
            false
        }
        return entity
    }

}