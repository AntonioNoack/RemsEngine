package com.bulletphysics.collision.dispatch

enum class ActivationState {
    /**
     * Happy and kicking!
     * Will be simulated as you'd expect.
     * */
    ACTIVE,

    // todo currently, the whole island needs to sleep for this to happen...
    //  can we make it per-instance?
    /**
     * When a simulation island is in rest, it may be deactivated.
     *
     * Will not be simulated.
     * */
    SLEEPING,

    /**
     * This member of the simulation island may rest.
     *
     * Will be simulated.
     * */
    WANTS_DEACTIVATION,

    /**
     * Won't even think about sleeping.
     *
     * Will be simulated.
     * */
    ALWAYS_ACTIVE,

    /**
     * This status is set when a numeric issue occurs (like NaN),
     * so the simulation isn't ruined.
     *
     * Won't be simulated any further.
     * */
    DISABLE_SIMULATION,
}