package me.anno.ecs.components.player

import me.anno.ecs.Entity
import me.anno.ecs.components.camera.CameraState

// todo which player frame currently is processed; allows for simple local multiplayer <3

// a special component, which can be added to one entity only? idk...
// multiple roots? this sounds like a kind-of-solution :)

open class LocalPlayer : Entity() {

    val camera = CameraState()

    // todo save all kind of information here
    val sessionInfo = HashMap<String, Any>()

    // todo needs to be saved every once in a while, preferably onSave ;)
    val persistentInfo = HashMap<String, Any>()

}