package me.anno.extensions.events

abstract class Event {

    var isCancelled = false

    fun cancel() {
        isCancelled = true
    }

    fun call(): Event {
        return EventBroadcasting.callEvent(this)
    }

}