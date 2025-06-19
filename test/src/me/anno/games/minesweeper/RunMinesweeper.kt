package me.anno.games.minesweeper

import me.anno.engine.WindowRenderFlags.showFPS
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.image.ImageCache
import me.anno.io.files.Reference.getReference
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Sleep

fun main() {
    disableRenderDoc()
    testUI3("Minesweeper") {

        showFPS = false // clean up UI a little

        val sx = 10
        val sy = 10
        val totalBombs = 10

        val minesweeper = Minesweeper(sx, sy, totalBombs)
        val atlasSource = getReference( // if this becomes unavailable some day, just replace it
            "https://raw.githubusercontent.com/Minesweeper-World/MS-Texture/main/png/cells/WinmineXP.png"
        )

        // load texture atlas asynchronously as long as texture atlases don't have a path
        Sleep.waitUntilDefined(true, {
            ImageCache[atlasSource].value
        }, { atlasImage ->
            minesweeper.atlasImages = atlasImage.split(4, 4).map { it.ref }
            minesweeper.startGame()
        })

        minesweeper.ui
    }
}