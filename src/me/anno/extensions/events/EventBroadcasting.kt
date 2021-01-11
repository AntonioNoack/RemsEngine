package me.anno.extensions.events

import me.anno.extensions.ExtensionLoader.loadedMods
import me.anno.extensions.ExtensionLoader.loadedPlugins

object EventBroadcasting {

    fun callEvent(event: Event): Event? {

        if(event.isCancelled) return null

        for(mod in loadedMods){
            mod.onEvent(event)
            if(event.isCancelled) return null
        }

        for(plugin in loadedPlugins){
            plugin.onEvent(event)
            if(event.isCancelled) return null
        }

        return event

    }

}