package org.recast4j.detour.crowd

enum class MoveRequestState {
    NONE,
    FAILED,
    VALID,
    REQUESTING,
    WAITING_FOR_QUEUE,
    WAITING_FOR_PATH,
    VELOCITY
}