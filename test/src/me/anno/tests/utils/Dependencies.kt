package me.anno.tests.utils

import me.anno.config.DefaultConfig.style
import me.anno.fonts.Font
import me.anno.gpu.GFX
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.drawing.DrawCurves
import me.anno.gpu.drawing.DrawCurves.drawLine
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.gpu.drawing.GFXx2D.drawCircle
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.max
import me.anno.maths.Maths.sq
import me.anno.ui.UIColors.cornFlowerBlue
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.MapPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS.documents
import me.anno.utils.structures.Iterators.filter
import me.anno.utils.structures.Iterators.map
import me.anno.utils.structures.lists.Lists.none2
import kotlin.math.cos
import kotlin.math.sin

class Package(val name: String) {
    val dependencies = HashSet<Package>()
    val dependencies2 = HashMap<Package, HashSet<Package>>()
    val children = ArrayList<Package>()
    var path: String = ""
    var parent: Package? = null
    var isCollapsed = true
    var depth = 0
    var px = 0f
    var py = 0f
    var r = 0f
}

// find project parts visually, which could be extracted
fun main() {

    val source = documents.getChild("IdeaProjects/RemsEngine/src")

    val ignoredPaths = listOf(
        "kotlin.",
        "java.",
        "javax.",
        "org.",
        "com.",
        "net.",
    )

    val packages = HashMap<String, Package>()

    // create dependency graph between folders and files
    fun traverse(
        folder: FileReference,
        pkg: Package
    ) {
        for (file in folder.listChildren()) {
            if (file.isDirectory) {
                val path = file.absolutePath.substring(source.absolutePath.length + 1)
                    .replace('/', '.')
                if (ignoredPaths.none2 { path.startsWith(it) }) {
                    val pkg1 = packages.getOrPut(path) { Package(file.name) }
                    traverse(file, pkg1)
                    pkg.children.add(pkg1)
                    pkg.dependencies.addAll(pkg1.dependencies)
                }
            } else {
                when (file.lcExtension) {
                    "java", "kt" -> {
                        val abs = file.absolutePath
                        val path = abs.substring(source.absolutePath.length + 1, abs.lastIndexOf('.'))
                            .replace('/', '.')
                        val pkg1 = packages.getOrPut(path) { Package(file.name) }
                        val imports = file.readLinesSync(Int.MAX_VALUE)
                            .map { it.trim() }
                            .filter { it.startsWith("import ") }
                            .map {
                                it.substring("import ".length)
                                    .replace(';', ' ')
                                    .trim()
                            }
                        for (line in imports) {
                            if (ignoredPaths.none { line.startsWith(it) }) {
                                pkg1.dependencies.add(packages.getOrPut(line) {
                                    Package(line.split('.').last())
                                })
                            }
                        }
                        pkg.children.add(pkg1)
                        pkg.dependencies.addAll(pkg1.dependencies)
                    }
                }
            }
        }
    }

    val root = packages.getOrPut("") { Package("*") }
    root.isCollapsed = false
    traverse(source, root)

    for (pck in packages) {
        pck.value.path = pck.key
        pck.value.depth = -1
    }

    do {// assign missing parents
        var changed = false
        for (pck in packages.values.toList()) {
            if (pck != root && pck.parent == null) {
                val path0 = pck.path
                val path1 = path0.substring(0, max(path0.lastIndexOf('.'), 0))
                val parent = packages.getOrPut(path1) {
                    Package(path1.split('.').last()).apply {
                        path = path1
                    }
                }
                pck.parent = parent
                parent.children.add(pck)
                parent.dependencies.addAll(pck.dependencies)
                changed = true
            }
        }
    } while (changed)

    fun sortChildren(node: Package) {
        node.children.sortBy { it.name }
        for (child in node.children) {
            sortChildren(child)
        }
    }
    sortChildren(root)

    for (pck in packages.values) pck.depth = pck.path.count { it == '.' }

    // remove external dependencies
    for (pck in packages.values) pck.dependencies.removeAll { it.depth < 0 || it.dependencies.isEmpty() }
    for (pck in packages.values) pck.dependencies.removeAll { it.depth < 0 || it.dependencies.isEmpty() }
    for (pck in packages.values) pck.dependencies.removeAll { it.depth < 0 || it.dependencies.isEmpty() }
    for (pck in packages.values) pck.children.removeAll { it.dependencies.isEmpty() }

    // traverse graph, and open/close children
    // draw each node as a circle
    disableRenderDoc()
    testUI3("Dependencies") {
        object : MapPanel(style) {

            val textColor = -1
            val circleColor = textColor.withAlpha(120)

            var hoveredDist = 0f
            var hoveredPck: Package? = null

            fun place(pck: Package, x: Float, y: Float, angle0: Float, radius0: Float) {
                // draw circle
                pck.px = x
                pck.py = y
                if (pck.dependencies.isEmpty()) {
                    pck.r = 0f
                    return
                } else pck.r = radius0
                val radius = radius0 * 0.5f
                if (radius < 1f) return
                val window = window!!
                val dist = sq(x - window.mouseX, y - window.mouseY)
                if (dist < hoveredDist) {
                    hoveredPck = if (dist < sq(radius) * 2f) pck else null
                    hoveredDist = dist
                }
                if (!pck.isCollapsed && pck.children.isNotEmpty()) {
                    val children = pck.children
                    val r1 = radius0 * 2f / (2f + children.size)
                    val da = if (children.size == 1) 0f else TAUf / children.size
                    for (ci in children.indices) {
                        val a1 = angle0 + (ci + 0.5f) * da
                        place(children[ci], x + cos(a1) * radius0, y + sin(a1) * radius0, a1, r1)
                    }
                }
            }

            fun drawPackage(pck: Package, font: Font) {
                val r0 = pck.r
                val x = pck.px
                val y = pck.py
                // draw circle
                val radius = r0 * 0.5f
                if (radius < 1f) return
                drawCircle(x, y, radius, radius, 0f, 0f, 0f, circleColor)
                // draw name into it
                val rx = radius * 3f
                if (x + rx > this.x && y + rx > this.y && x - rx < this.x + width && y - rx < this.y + height) {
                    if (font.sizeInt in 5..(height / 3)) {
                        drawText(
                            x.toInt(), y.toInt(), font, pck.name,
                            if (pck == hoveredPck) cornFlowerBlue or black else textColor, backgroundColor.withAlpha(0),
                            -1, -1, AxisAlignment.CENTER, AxisAlignment.CENTER
                        )
                    }
                    if (!pck.isCollapsed && pck.children.isNotEmpty()) {
                        val children = pck.children
                        val r1 = children.maxOf { it.r }
                        val font1 = Font("Verdana", r1 * 0.3f)
                        for (ci in children.indices) {
                            drawPackage(children[ci], font1)
                        }
                    }
                }
            }

            init {
                minScale.set(0.1)
                maxScale.set(1e9)
                calculateDependencies(root)
            }

            override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {

                // enable compute-shader-based rendering, which looks better
                // only possible on separate framebuffer
                GFX.someWindow.windowStack.first().drawDirectly = false

                super.draw(x0, y0, x1, y1)
                var x = x
                var y = y
                val size = scale.y.toFloat() * 100f
                hoveredDist = Float.POSITIVE_INFINITY
                hoveredPck = null
                place(
                    root,
                    x + width / 2 - (center.x * scale.x).toFloat(),
                    y + height / 2 - (center.y * scale.y).toFloat(),
                    0f, size
                )
                val tc = textColor
                val bc = backgroundColor.withAlpha(0)
                // draw all connections
                val pck = hoveredPck
                if (pck != null) {
                    val v = DrawCurves.lineBatch.start()
                    for ((dep, _) in pck.dependencies2) {
                        drawLine(pck.px, pck.py, backgroundColor, dep.px, dep.py, tc, 1f, bc, true)
                    }
                    DrawCurves.lineBatch.finish(v)
                }
                // draw circles with names
                val pbb = DrawTexts.pushBetterBlending(true)
                drawPackage(root, Font("Verdana", size * 0.3f))
                if (pck != null) {
                    x += 2
                    y += 2
                    val ts = monospaceFont.sizeInt
                    drawSimpleTextCharByChar(x, y, 1, "--- ${pck.path} ---", tc, bc)
                    y += ts * 3 / 2
                    for ((dep, times) in pck.dependencies2.entries
                        .sortedBy { it.key.path }
                        .sortedByDescending { it.value.size }) {
                        drawSimpleTextCharByChar(
                            x, y, 1,
                            if (times.size == 1) {
                                times.first().path
                            } else {
                                "${dep.path}: ${times.size}x, ${
                                    times
                                        .map {
                                            if (it.path != dep.path) it.path.substring(dep.path.length + 1)
                                            else it.path
                                        }
                                        .sorted()
                                        .run {
                                            if (this.size < 5) toString()
                                            else subList(0, 5).joinToString(", ", "[", ", ...]")
                                        }
                                }"
                            }, tc, bc
                        )
                        y += ts
                    }
                }
                DrawTexts.popBetterBlending(pbb)
            }

            fun collapseAll(pck: Package) {
                pck.isCollapsed = true
                for (child in pck.children) collapseAll(child)
            }

            fun calculateDependencies(pck: Package) {
                if (!pck.isCollapsed && pck.children.isNotEmpty()) {
                    for (child in pck.children) calculateDependencies(child)
                }
                // calculate, which things are visible :)
                pck.dependencies2.clear()
                for (dep0 in pck.dependencies) {
                    var dep: Package? = dep0
                    while (dep != null && dep != pck && dep.parent?.isCollapsed == true) dep = dep.parent
                    if (dep != null && dep != pck)
                        pck.dependencies2.getOrPut(dep, ::HashSet).add(dep0)
                }
            }

            override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
                val clickedPck = hoveredPck
                if (clickedPck != null) {
                    clickedPck.isCollapsed = !clickedPck.isCollapsed
                    if (clickedPck.isCollapsed) collapseAll(clickedPck)
                    calculateDependencies(root)
                }
            }
        }
    }
}