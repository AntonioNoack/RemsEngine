package me.anno.ecs.components.ui

import me.anno.ecs.Component

// todo ui components, because we want everything to be ecs -> reuse our existing stuff? maybe

// todo what ui system do we use? idk...
// todo maybe a mix...

/**
 * as the start of an UI System, we use our UI library, which is used for the video editor and the engine
 * */
class UIComponent : Component() {

    override fun clone(): UIComponent {
        TODO("Not yet implemented")
    }

    override val className = "UIComponent"

}