package me.anno.games.minesweeper

import me.anno.config.DefaultConfig.style
import me.anno.gpu.texture.Filtering
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths
import me.anno.ui.Panel
import me.anno.ui.base.image.IconPanel

/**
 * This shows how to code Minesweeper in 200 lines of code
 * */
class Minesweeper(
    val sx: Int, val sy: Int,
    val totalBombs: Int
) {

    var atlasImages: List<FileReference>? = null
    val bombs = BooleanArray(sx * sy)

    val discovered = BooleanArray(sx * sy)
    val flags = BooleanArray(sx * sy)
    var isGameOver = false

    val ui = SquareGridLayoutPanel(sx, sy) { xi, yi ->
        IconPanel(InvalidRef, style)
            .apply { filtering = Filtering.NEAREST }
            .addLeftClickListener { leftClick(xi, yi) }
            .addRightClickListener { rightClick(xi, yi, it) }
    }

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
        return atlasImages!![image]
    }

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
        recalculateAllTiles()
    }

    fun leftClick(xi: Int, yi: Int) {
        val idx = xi + sx * yi
        if (isGameOver) {
            startGame()
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

    fun rightClick(xi: Int, yi: Int, it: Panel) {
        if (isGameOver) {
            startGame()
        } else {
            val idx = xi + sx * yi
            // invalidate this' image
            flags[idx] = !flags[idx]
            (it as IconPanel).source = getTileImage(xi, yi)
        }
    }
}
