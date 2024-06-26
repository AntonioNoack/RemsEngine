package me.anno.ecs.components.player

// a special component, which can be added to one entity only? idk...
// multiple roots? this sounds like a kind-of-solution :)

open class LocalPlayer : Player() {

    companion object {

        /**
         * which player frame currently is processed;
         * allows for simple local multiplayer
         * */
        var currentLocalPlayer: LocalPlayer? = null
        val localPlayers = ArrayList<LocalPlayer>()

        // to do a game instance would then set localPlayer, and localPlayers
        // RenderView would combine all local players, or a single one, depending on setup...
        // probably we should implement a multi-renderView, which does that :)

    }
}