package org.recast4j.detour.crowd

class ObstacleAvoidanceParams() {
    var velocityBias = 0.4f
    var weightDesiredVelocity = 2f
    var weightActualVelocity = 0.75f // "weightCurVel"
    var weightSide = 0.75f
    var weightToi = 2.5f
    var horizTime = 2.5f
    var gridSize = 33
    var numAdaptiveDivs = 7
    var numAdaptiveRings = 2
    var adaptiveDepth = 5

    constructor(params: ObstacleAvoidanceParams) : this() {
        velocityBias = params.velocityBias
        weightDesiredVelocity = params.weightDesiredVelocity
        weightActualVelocity = params.weightActualVelocity
        weightSide = params.weightSide
        weightToi = params.weightToi
        horizTime = params.horizTime
        gridSize = params.gridSize
        numAdaptiveDivs = params.numAdaptiveDivs
        numAdaptiveRings = params.numAdaptiveRings
        adaptiveDepth = params.adaptiveDepth
    }
}