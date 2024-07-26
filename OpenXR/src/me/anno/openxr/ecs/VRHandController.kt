package me.anno.openxr.ecs

import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate
import me.anno.openxr.OpenXRController.Companion.xrControllers
import me.anno.utils.types.Booleans.toInt

class VRHandController : Component(), OnUpdate {
    var rightHand = true

    override fun onUpdate() {
        val controller = xrControllers[rightHand.toInt()]
        if (!controller.isConnected) {
            // todo hide all visuals
            return
        }
        // todo update this :)
        //  - teleporting to areas
        //  - teleporting to anchors
        //  - picking stuff up
        //  - placing stuff
    }
}