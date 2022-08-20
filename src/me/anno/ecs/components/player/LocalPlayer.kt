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

        /**
         * which player frame currently is processed;
         * allows for simple local multiplayer
         * */
        var currentLocalPlayer: LocalPlayer? = null
        val localPlayers = ArrayList<LocalPlayer>()

        // todo a game instance would then set localPlayer, and localPlayers
        // RenderView would combine all local players, or a single one, depending on setup...
        // probably we should implement a multi-renderView, which does that :)

    }

    override val className = "LocalPlayer"

}