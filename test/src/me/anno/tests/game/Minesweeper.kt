package me.anno.tests.game

import me.anno.config.DefaultConfig.style
import me.anno.engine.EngineBase.Companion.showFPS
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.texture.Filtering
import me.anno.image.ImageCache
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.maths.Maths
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.image.IconPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import kotlin.math.min

/**
 * Calculates layout for child elements, such that they are grouped together, are all square,
 * all have the same size, same width and height, and are centered
 * */
class SquareGridLayoutPanel(val sx: Int, val sy: Int, createPanel: (xi: Int, yi: Int) -> Panel) :
    PanelList(style) {

    init {
        for (yi in 0 until sy) {
            for (xi in 0 until sx) {
                add(createPanel(xi, yi))
            }
        }
    }

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        val childSize = min(width / sx, height / sy)
        val usedWidth = childSize * sx
        val usedHeight = childSize * sy
        val paddingX = (width - usedWidth) shr 1
        val paddingY = (height - usedHeight) shr 1
        for (yi in 0 until sy) {
            for (xi in 0 until sx) {
                val cx = x + paddingX + xi * childSize
                val cy = y + paddingY + yi * childSize
                children[xi + sx * yi]
                    .setPosSize(cx, cy, childSize, childSize)
            }
        }
    }
}

/**
 * This shows how to code Minesweeper in 200 lines of code
 * */
fun main() {

    val sx = 10
    val sy = 10
    val totalBombs = 10

    disableRenderDoc()
    testUI3("Minesweeper") {

        showFPS = false // clean up UI a little

        val atlasSource = getReference( // if this becomes unavailable some day, just replace it
            "https://raw.githubusercontent.com/Minesweeper-World/MS-Texture/main/png/cells/WinmineXP.png"
        )

        val atlasImage = ImageCache[atlasSource, false]!!
        val atlasImages = atlasImage.split(4, 4)
        val bombs = BooleanArray(sx * sy)

        val discovered = BooleanArray(sx * sy)
        val flags = BooleanArray(sx * sy)
        var isGameOver = false

        fun countBombs(xi: Int, yi: Int): Int {
            var sum = 0
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val x = xi + dx
                    val y = yi + dy
                    if (x in 0 until sx && y in 0 until sy &&
                        bombs[x + sx * y]
                    ) sum++
                }
            }
            return sum
        }

        fun getTileImage(xi: Int, yi: Int): FileReference {
            val numBombs = countBombs(xi, yi)
            val idx = xi + sx * yi
            val image = if (discovered[idx]) {
                if (bombs[idx]) 15
                else when {
                    numBombs == 0 -> 8
                    else -> numBombs - 1
                }
            } else if (isGameOver && bombs[idx]) {
                if (flags[idx]) 11 else 14
            } else {
                if (flags[idx]) 10 else 9
            }
            return atlasImages[image].ref
        }

        lateinit var ui: SquareGridLayoutPanel

        fun recalculateTile(xi: Int, yi: Int) {
            val idx = xi + sx * yi
            (ui.children[idx] as IconPanel).source = getTileImage(xi, yi)
        }

        fun recalculateAllTiles() {
            for (y in 0 until sy) {
                for (x in 0 until sx) {
                    recalculateTile(x, y)
                }
            }
        }

        fun discover(xi: Int, yi: Int) {
            if (xi !in 0 until sx || yi !in 0 until sy) return // out of bounds
            val idx = xi + sx * yi
            if (discovered[idx]) return // done
            discovered[idx] = true
            recalculateTile(xi, yi)
            if (countBombs(xi, yi) == 0) {
                // discover surroundings
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        discover(xi + dx, yi + dy)
                    }
                }
            }
        }

        fun startGame() {
            discovered.fill(false)
            bombs.fill(false)
            flags.fill(false)
            isGameOver = false
            for (i in 0 until totalBombs) {
                // not perfect, but good enough imo
                bombs[Maths.randomInt(0, bombs.size)] = true
            }
        }

        startGame()

        ui = SquareGridLayoutPanel(sx, sy) { xi, yi ->
            val idx = xi + sx * yi
            IconPanel(getTileImage(xi, yi), style)
                .apply { filtering = Filtering.NEAREST }
                .addLeftClickListener {
                    if (isGameOver) {
                        startGame()
                        recalculateAllTiles()
                    } else if (bombs[idx] && discovered.any { it }) { // first click is safe
                        discovered[idx] = true
                        isGameOver = true
                        // validate full field
                        recalculateAllTiles()
                    } else {
                        bombs[idx] = false
                        discover(xi, yi)
                        if ((0 until sx * sy).all {
                                discovered[it] || bombs[it]
                            }) { // player won
                            isGameOver = true
                            recalculateAllTiles()
                        }
                    }
                }
                .addRightClickListener {
                    if (isGameOver) {
                        startGame()
                        recalculateAllTiles()
                    } else {
                        // invalidate this' image
                        flags[idx] = !flags[idx]
                        (it as IconPanel).source = getTileImage(xi, yi)
                    }
                }
        }
        ui
    }
}