package org.recast4j.detour.crowd

class ObstacleAvoidanceParams() {
    var velBias = 0.4f
    var weightDesVel = 2f
    var weightCurVel = 0.75f
    var weightSide = 0.75f
    var weightToi = 2.5f
    var horizTime = 2.5f
    var gridSize = 33
    var adaptiveDivs = 7
    var adaptiveRings = 2
    var adaptiveDepth = 5

    constructor(params: ObstacleAvoidanceParams) : this() {
        velBias = params.velBias
        weightDesVel = params.weightDesVel
        weightCurVel = params.weightCurVel
        weightSide = params.weightSide
        weightToi = params.weightToi
        horizTime = params.horizTime
        gridSize = params.gridSize
        adaptiveDivs = params.adaptiveDivs
        adaptiveRings = params.adaptiveRings
        adaptiveDepth = params.adaptiveDepth
    }
}