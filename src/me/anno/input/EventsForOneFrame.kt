package me.anno.input

/**
 * single-frame state of which keys went down, went up or were typed
 * */
class EventsForOneFrame {

    val keysWentDown = HashSet<Key>()
    val keysWentUp = HashSet<Key>()
    val keysWentTyped = HashSet<Key>()

    fun clear() {
        keysWentTyped.clear()
        keysWentUp.clear()
        keysWentDown.clear()
    }
}