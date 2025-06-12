package com.bulletphysics.collision.dispatch

enum class ActivationState {
    ACTIVE,
    // todo currently, the whole island needs to sleep for this to happen...
    //  can we make it per-instance?
    SLEEPING,
    WANTS_DEACTIVATION,
    DISABLE_DEACTIVATION,
    DISABLE_SIMULATION,
}