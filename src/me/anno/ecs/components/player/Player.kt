package me.anno.ecs.components.player

import me.anno.ecs.Component
import me.anno.ecs.components.camera.CameraState
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.utils.StringMap

// a special component, which can be added to one entity only? idk...
// multiple roots? this sounds like a kind-of-solution :)

open class Player : Component() {

    val cameraState = CameraState()

    // todo save all kind of information here
    val sessionInfo = StringMap()

    // todo needs to be saved every once in a while, preferably onSave ;)
    val persistentInfo = StringMap()

    override fun save(writer: BaseWriter) {
        super.save(writer)
        // writer.writeMap("session", sessionInfo)
        writer.writeObject(null, "persistent", persistentInfo)
    }

    override fun readObject(name: String, value: ISaveable?) {
        if (name == "persistent") {
            if (value !is StringMap) return
            persistentInfo.clear()
            persistentInfo.putAll(value)
        } else super.readObject(name, value)
    }

    override fun clone(): Player {
        val clone = Player()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Player
        cameraState.copy(clone.cameraState)
        clone.sessionInfo.clear()
        clone.sessionInfo.putAll(sessionInfo)
        clone.persistentInfo.clear()
        clone.persistentInfo.putAll(persistentInfo)
    }

    override val className: String = "Player"

}