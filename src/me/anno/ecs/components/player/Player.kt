package me.anno.ecs.components.player

import me.anno.ecs.Component
import me.anno.ecs.components.camera.CameraState
import me.anno.ecs.prefab.PrefabSaveable

// a special component, which can be added to one entity only? idk...
// multiple roots? this sounds like a kind-of-solution :)

open class Player : Component() {

    val camera = CameraState()

    // todo save all kind of information here
    val sessionInfo = HashMap<String, Any>()

    // todo needs to be saved every once in a while, preferably onSave ;)
    val persistentInfo = HashMap<String, Any>()

    override fun clone(): Player {
        val clone = Player()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Player
        camera.copy(clone.camera)
        clone.sessionInfo.clear()
        clone.sessionInfo.putAll(sessionInfo)
        clone.persistentInfo.clear()
        clone.persistentInfo.putAll(persistentInfo)
    }

    override val className: String = "Player"

}