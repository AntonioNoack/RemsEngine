package me.anno.extensions.events

import me.anno.extensions.ExtensionLoader.managers

object EventBroadcasting {

    fun callEventNullable(event: Event?): Event? {

        event ?: return null
        if (event.isCancelled) return null

        for (manager in managers) {
            for (ext in manager.loaded) {
                ext.onEvent(event)
                if (event.isCancelled) return null
            }
        }

        return event

    }

    fun callEvent(event: Event): Event {
        if (callEventNullable(event) == null) event.isCancelled = true
        return event
    }

}