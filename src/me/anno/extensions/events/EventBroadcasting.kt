package me.anno.extensions.events

import me.anno.extensions.ExtensionLoader.managers

object EventBroadcasting {

    fun callEvent(event: Event): Event? {

        if(event.isCancelled) return null

        for(manager in managers){
            for(ext in manager.loaded){
                ext.onEvent(event)
                if(event.isCancelled) return null
            }
        }

        return event

    }

}