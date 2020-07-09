package me.anno.ui.editor.treeView

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.io.text.TextReader
import me.anno.io.utils.StringMap
import me.anno.objects.*
import me.anno.objects.Transform.Companion.toTransform
import me.anno.objects.effects.MaskLayer
import me.anno.objects.geometric.Circle
import me.anno.objects.geometric.Polygon
import me.anno.objects.particles.ParticleSystem
import me.anno.studio.Studio
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.treeView.TreeView.Companion.addChildFromFile
import me.anno.ui.style.Style
import me.anno.utils.clamp
import me.anno.utils.mixARGB
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.File
import java.lang.Exception

class TreeViewPanel(val getElement: () -> Transform, style: Style): TextPanel("", style){

    val accentColor = style.getColor("accentColor", DefaultStyle.black or 0xff0000)
    val defaultBackground = backgroundColor
    val cameraBackground = mixARGB(accentColor, defaultBackground, 0.9f)

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        val transform = getElement()
        textColor = DefaultStyle.black or (transform.getLocalColor().toRGB(180))
        backgroundColor = if(transform === Studio.selectedCamera) cameraBackground else defaultBackground
        val isInFocus = isInFocus || Studio.selectedTransform == transform
        if(isInFocus) textColor = accentColor
        drawText(x, y, text, textColor)
    }

    override fun onMouseClicked(x: Float, y: Float, button: Int, long: Boolean) {

        val transform = getElement()
        when(button){
            0 -> {
                GFX.select(transform)
            }
            1 -> {// right click

                fun add(action: (Transform) -> Transform): (Int, Boolean) -> Boolean {
                    return { b, l ->
                        if(b == 0){
                            transform.apply { GFX.select(action(this)) }
                            true
                        } else false
                    }
                }

                val options = DefaultConfig["createNewInstancesList"] as? StringMap
                if(options != null){
                    GFX.openMenu(
                        Input.mouseX, Input.mouseY, "Add Child",
                        options.entries.map { (key, value) ->
                            key to add {
                                val newT = if(value is Transform) value.clone() else value.toString().toTransform()
                                it.addChild(newT)
                                newT
                            }
                        }
                    )
                } else println("Reset the config, to enable this menu!")
                /*GFX.openMenu(
                    Input.mouseX, Input.mouseY, "Add Child",
                    listOf(// todo make these options customizable :)
                        "Folder" to add { Transform(it) },
                        "Text" to add { Text("Sample Text", it) },
                        "Image" to add { Image(File(""), it) },
                        "Video/GIF" to add { Video(File(""), it) },
                        "Audio" to add { Audio(File(""), it) },
                        "Circle" to add { Circle(it) },
                        "Polygon" to add { Polygon(it) },
                        "Camera" to add { Camera(it) },
                        "Particle System" to add { ParticleSystem(it) },
                        "Mask" to add {
                            val layer = MaskLayer(it)
                            val mask = Transform(layer)
                            mask.name = "Mask"
                            val maskElement = Circle(mask)
                            val circleScale = 0.75f
                            maskElement.scale.addKeyframe(0f,
                                Vector3f(circleScale, circleScale, circleScale),
                                0.1f)
                            maskElement.innerRadius.addKeyframe(0f, 0.25f, 0.1f)
                            maskElement.name = "Mask-Defining Circle"
                            val masked = Transform(layer)
                            masked.name = "Masked"
                            val maskedElement = Polygon(masked)
                            maskedElement.name = "Shaped Star"
                            maskedElement.vertexCount.addKeyframe(0f, 10f, 0.1f)
                            maskedElement.starNess.addKeyframe(0f, 0.6f, 0.1f)
                            layer
                        }
                    )
                )*/
            }
        }
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        return getElement().stringify()
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        try {
            val child = TextReader.fromText(data).firstOrNull { it is Transform } as? Transform ?: return super.onPaste(x, y, data, type)
            getElement().addChild(child)
        } catch (e: Exception){
            e.printStackTrace()
            super.onPaste(x, y, data, type)
        }
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<File>) {
        val transform = getElement()
        if(files.size == 1){
            // todo check if it matches

            // return // if it matches
        }
        files.forEach {
            addChildFromFile(transform, it)
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when(action){
            "DragStart" -> {
                val transform = getElement()
                if(Studio.dragged?.getOriginal() != transform){
                    Studio.dragged = Draggable(transform.stringify(), "Transform", transform, TextPanel(transform.name, style))
                }
            }
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun onEmpty(x: Float, y: Float) {
        onDeleteKey(x, y)
    }

    override fun onDeleteKey(x: Float, y: Float) {
        val transform = getElement()
        val parent = transform.parent
        if(parent != null){
            GFX.select(parent)
            transform.removeFromParent()
            transform.onDestroy()
        }
    }

    override fun onBackKey(x: Float, y: Float) = onDeleteKey(x,y)
    override fun getCursor() = Cursor.drag

    override fun getTooltipText(x: Float, y: Float): String? {
        val transform = getElement()
        return if(transform is Camera) "Shift-Click to set current" else null
    }

    fun Vector4f.toRGB(scale: Int = 255): Int {
        return clamp((x * scale).toInt(), 0, 255).shl(16) or
                clamp((y * scale).toInt(), 0, 255).shl(8) or
                clamp((z * scale).toInt(), 0, 255) or
                clamp((w * 255).toInt(), 0, 255).shl(24)
    }

    // multiple values can be selected
    override fun getMultiSelectableParent() = this

    override fun getClassName() = "TreeViewPanel"

}