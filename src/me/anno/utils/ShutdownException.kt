package me.anno.utils

/**
 * Is thrown when the game is being shutdown, and a component was waiting.
 * Likely, it no longer needs to wait, and therefore needs to quit its usual execution.
 * */
class ShutdownException : Exception()