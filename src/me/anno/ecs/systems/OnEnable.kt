package me.anno.ecs.systems

/**
 * Listener for Components for when they get enabled/created/disabled
 * */
interface OnEnable {
    fun onEnable()
    fun onDisable() {}
}