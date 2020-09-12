package me.anno.ui.editor.files

import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style
import me.anno.utils.OS
import java.io.File

class PathPanel(file: File?, style: Style) : PanelListX(style) {

    var separatorChar = '/'
    var partLimit = 5
    var paddingOffset = 2

    var oldFile: File? = null
    var file: File? = file
        set(value) {
            if (value != oldFile) {
                field = value
                update()
            }
        }

    var onChangeListener: ((File?) -> Unit)? = null

    // todo update visibility depending on size? (show more/less on the left side)
    fun update() {
        children.clear()
        if (file == null) {
            children.add(TextPanel("This Computer", style))
        } else {
            val paddingLeft = -paddingOffset/2
            val paddingRight = -(paddingOffset+1)/2
            var parent = file
            while (parent != null) {
                val file = parent
                var name = file.name
                if (name.isEmpty()) name = file.toString()
                    .replace("\\", "") // C:\ -> C:
                if (name.isEmpty()) break
                if (children.size+1 == partLimit) name = "...$separatorChar"
                else if (children.isNotEmpty()) name = "$name$separatorChar"
                val panel = TextPanel(name, style)
                panel.setSimpleClickListener {
                    onChangeListener?.invoke(file)
                }
                panel.padding.left = paddingLeft
                panel.padding.right = paddingRight
                children += panel
                parent = file.parentFile
                if (children.size >= partLimit) {
                    break
                }
            }
            if (children.size < partLimit && OS.isWindows) {
                // we need a this-computer-tab
                children += TextPanel("This Computer$separatorChar", style)
                    .setSimpleClickListener {
                        onChangeListener?.invoke(null)
                    }
            }
            children.reverse()
        }
    }

}