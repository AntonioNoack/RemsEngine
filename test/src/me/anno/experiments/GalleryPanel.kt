package me.anno.experiments

import me.anno.config.DefaultConfig.style
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS.pictures

// todo implement gallery like on Android:
//  wide-mode:
//   - wide images take up one row,
//   - all rows are the same height
//   - zoom in, hide borders
//   - black borders with light gray edge
//   - upto 2-3 skinny images per row (depending on how many fit)
//  overview modes (zooming out):
//   - exactly 3+ images per row
//   - all rows are the squares (of the same height)
//  make this a valid mode in FileExplorer somehow... it is very nice for viewing images
//   -> create a button to toggle between all modes
//  or switch to it if we have thumbnails for all items???
//  or make it default???
//  -> this kind of is a special PanelList2D mode
//  -> (optionally) make PanelList2D behave special for 2 elements
//  -> and change our FileExplorer items to use the whole space,
//     and write the text on top (bigger preview image = better)

fun main() {
    testUI3("Gallery", FileExplorer(pictures, true, style))
}