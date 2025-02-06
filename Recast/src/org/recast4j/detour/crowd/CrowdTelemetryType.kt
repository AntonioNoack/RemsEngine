package org.recast4j.detour.crowd

enum class CrowdTelemetryType {
    CHECK_PATH_VALIDITY,
    UPDATE_MOVE_REQUEST,
    PATH_QUEUE_UPDATE,
    UPDATE_TOPOLOGY_OPTIMIZATION,
    BUILD_PROXIMITY_GRID,
    BUILD_NEIGHBORS,
    FIND_CORNERS,
    TRIGGER_OFF_MESH_CONNECTIONS,
    CALCULATE_STEERING,
    PLAN_VELOCITY,
    INTEGRATE,
    HANDLE_COLLISIONS,
    MOVE_AGENTS,
    UPDATE_OFF_MESH_CONNECTIONS,
}