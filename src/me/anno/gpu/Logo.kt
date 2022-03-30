package me.anno.gpu

import me.anno.ecs.prefab.change.Path
import me.anno.io.ResourceHelper
import me.anno.io.files.InvalidRef
import me.anno.io.zip.InnerPrefabFile
import me.anno.mesh.obj.OBJReader2
import org.lwjgl.opengl.GL11
import java.io.IOException

// can be set by the application
var frame0BackgroundColor = 0
var frame0IconColor = 0x172040

fun drawLogo(window: WindowX){

    // load icon.obj as file, and draw it using OpenGL 1.0
    var c = frame0BackgroundColor
    GL11.glClearColor((c shr 16 and 255) / 255f, (c shr 8 and 255) / 255f, (c and 255) / 255f, 1f)
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
    GL11.glMatrixMode(GL11.GL_PROJECTION)
    GL11.glLoadIdentity()

    // extend space left+right/top+bottom (zooming out a little)
    val width = window.width
    val height = window.height
    if (width > height) {
        val dx = width.toFloat() / height
        GL11.glOrtho(-dx.toDouble(), dx.toDouble(), -1.0, +1.0, -1.0, +1.0)
    } else {
        val dy = height.toFloat() / width
        GL11.glOrtho(-1.0, +1.0, -dy.toDouble(), dy.toDouble(), -1.0, +1.0)
    }

    c = frame0IconColor
    GL11.glColor3f((c shr 16 and 255) / 255f, (c shr 8 and 255) / 255f, (c and 255) / 255f)
    try {
        val stream = ResourceHelper.loadResource("icon.obj")
        val reader = OBJReader2(stream, InvalidRef)
        val file = reader.meshesFolder
        for (child in file.listChildren()) {
            // we could use the name as color... probably a nice idea :)
            val prefab = (child as InnerPrefabFile).prefab
            val sets = prefab.sets
            val positions = sets[Path.ROOT_PATH, "positions"] as FloatArray?
            val indices = sets[Path.ROOT_PATH, "indices"] as IntArray?
            if (positions != null) {
                GL11.glBegin(GL11.GL_TRIANGLES)
                if (indices == null) {
                    var i = 0
                    while (i < positions.size) {
                        GL11.glVertex2f(positions[i], positions[i + 1])
                        i += 3
                    }
                } else {
                    for (index in indices) {
                        val j = index * 3
                        GL11.glVertex2f(positions[j], positions[j + 1])
                    }
                }
                GL11.glEnd()
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}