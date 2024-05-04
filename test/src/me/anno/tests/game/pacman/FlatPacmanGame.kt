package me.anno.tests.game.pacman

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.drawing.DrawCurves.drawLine
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.io.files.Reference.getReference
import me.anno.maths.Maths.min
import me.anno.tests.game.pacman.logic.PacmanLogic
import me.anno.ui.Panel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.white
import org.joml.Vector2f

// textures are from https://www.flaticon.com/ from Tahsin Tahil (Pacman.png), Freepik (Gem.png) and Creative Squad (Ghost.png)
/**
 * renders a pacman game with standard flat UI
 * */
class FlatPacmanGame : Panel(style) {
    val game = PacmanLogic()
    override fun onUpdate() {
        super.onUpdate()
        game.updateControls()
        invalidateDrawing()
    }

    private val gemPath = getReference("res://textures/Gem.png")
    private val enemyPath = getReference("res://textures/Ghost.png")
    private val playerPath = getReference("res://textures/Pacman.png")
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)

        // calculate positioning
        val padding = 10
        val sx = (width - 2f * padding) / game.size.x
        val sy = (height - 2f * padding) / game.size.y
        val scale = min(sx, sy)
        val ox = x + (width - game.size.x * scale) / 2
        val oy = y + (height - game.size.y * scale) / 2

        fun x(xi: Float) = ox + scale * xi
        fun y(yi: Float) = oy + scale * yi
        fun x(xi: Int) = x(xi.toFloat())
        fun y(yi: Int) = y(yi.toFloat())

        // walls
        for (wall in game.walls) {
            drawLine(
                x(wall.start.x), y(wall.start.y), x(wall.end.x), y(wall.end.y), 2f,
                white, backgroundColor, false
            )
        }

        val pathColor = mixARGB(backgroundColor, white, 0.3f)
        for (node in game.nodes) {
            val start = node.position
            for (nei in node.neighbors) {
                val end = nei.position
                if (start.x > end.x || start.y > end.y) { // only draw each line once
                    drawLine(
                        x(start.x + 0.5f), y(start.y + 0.5f), x(end.x + 0.5f), y(end.y + 0.5f),
                        scale * 0.25f,
                        pathColor, backgroundColor, false
                    )
                }
            }
        }

        fun drawTex(pos: Vector2f, ix0: Float, ix1: Float, iy0: Float, iy1: Float, texture: ITexture2D) {
            val x2 = x(pos.x + ix0).toInt()
            val y2 = y(pos.y + iy0).toInt()
            val x3 = x(pos.x + ix1).toInt()
            val y3 = y(pos.y + iy1).toInt()
            texture.bind(0, Filtering.LINEAR, Clamping.CLAMP)
            drawTexture(x2, y2, x3 - x2, y3 - y2, texture)
        }

        fun drawTex(pos: Vector2f, i0: Float, texture: ITexture2D) {
            val i1 = 1f - i0
            drawTex(pos, i0, i1, i0, i1, texture)
        }

        val collectibleTexture = TextureCache[gemPath, true] ?: missingTexture
        for (collectible in game.collectables) {
            drawTex(collectible, 0.3f, collectibleTexture)
        }

        val enemyTexture = TextureCache[enemyPath, true] ?: missingTexture
        for (enemy in game.enemies) {
            drawTex(enemy.currPosition, 0.2f, enemyTexture)
        }

        val playerTexture = TextureCache[playerPath, true] ?: missingTexture
        val lookLeft = game.player.lookLeft
        drawTex(
            game.player.currPosition, if (lookLeft) 0.8f else 0.2f, if (lookLeft) 0.2f else 0.8f,
            0.2f, 0.8f, playerTexture
        )
    }
}

fun main() {
    // todo show lives and score
    disableRenderDoc()
    testUI3("Flat Pacman", FlatPacmanGame())
}