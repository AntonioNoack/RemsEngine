package me.anno.ecs.components.player

// a special component, which can be added to one entity only? idk...
// multiple roots? this sounds like a kind-of-solution :)

open class LocalPlayer : Player() {

    override fun clone(): LocalPlayer {
        val clone = LocalPlayer()
        copy(clone)
        return clone
    }

    companion object {
        // which player frame currently is processed; allows for simple local multiplayer <3
        var currentLocalPlayer: LocalPlayer? = null
    }

    override val className: String = "LocalPlayer"

}