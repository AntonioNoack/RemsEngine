package me.anno.tests.game.creeperworld

class AgentPixel(
    path: IntArray, color: Int,
    val agent: Agent
) : Pixel(path, color) {

    override fun update(world: CreeperWorld) {
        val nextPos = path[progress + 1]
        val pixelIsSet = world.pixelIsSet
        if (!pixelIsSet[nextPos]) {
            pixelIsSet[path[progress++]] = false
            pixelIsSet[nextPos] = true
        }
    }

    override fun onFinish(world: CreeperWorld) {
        super.onFinish(world)
        world.pixelIsSet[path.last()] = false
        agent.loadingState++
    }
}