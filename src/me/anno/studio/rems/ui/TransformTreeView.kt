package me.anno.studio.rems.ui

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.io.utils.StringMap
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.objects.Rectangle
import me.anno.objects.Transform
import me.anno.objects.Transform.Companion.toTransform
import me.anno.objects.effects.MaskLayer
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.nullCamera
import me.anno.studio.rems.RemsStudio.root
import me.anno.studio.rems.Selection
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.treeView.AbstractTreeView
import me.anno.ui.style.Style
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import java.util.*

// todo select multiple elements, filter for common properties, and apply them all together :)

class TransformTreeView(style: Style) :
    AbstractTreeView<Transform>(listOf(nullCamera!!, root), Companion::openAddMenu, TransformFileImporter, style) {

    override val selectedTransform: Transform?
        get() = Selection.selectedTransform

    override fun selectElement(element: Transform?) {
        Selection.selectTransform(element)
    }

    override fun focusOnElement(element: Transform) {
        zoomToObject(element)
    }

    companion object {

        fun zoomToObject(obj: Transform) {
            // instead of asking for the name, move the camera towards the target
            // todo also zoom in/out correctly to match the object...
            // identify the currently used camera
            val camera = GFX.lastTouchedCamera ?: nullCamera ?: return
            val time = RemsStudio.editorTime
            // calculate the movement, which would be necessary
            val cameraToWorld = camera.parent?.getGlobalTransform(time)
            val objectToWorld = obj.getGlobalTransform(time)
            val objectWorldPosition = objectToWorld.transformPosition(Vector3f(0f, 0f, 0f))
            val objectCameraPosition = if (cameraToWorld == null) objectWorldPosition else cameraToWorld.invert()
                .transformPosition(objectWorldPosition)
            println(objectCameraPosition)
            // apply this movement
            RemsStudio.largeChange("Move Camera to Object") {
                camera.position.addKeyframe(camera.lastLocalTime, objectCameraPosition)
            }
            /* askName(this.x, this.y, NameDesc(), getElement().name, NameDesc("Change Name"), { textColor }) {
                 getElement().name = it
             }*/
        }

        private val LOGGER = LogManager.getLogger(TransformTreeView::class)
        fun openAddMenu(baseTransform: Transform) {
            fun add(action: (Transform) -> Transform): () -> Unit = { Selection.selectTransform(action(baseTransform)) }
            val options = DefaultConfig["createNewInstancesList"] as? StringMap
            if (options != null) {
                val extras = ArrayList<MenuOption>()
                if (baseTransform.parent != null) {
                    extras += Menu.menuSeparator1
                    extras += MenuOption(
                        NameDesc(
                            "Add Mask",
                            "Creates a mask component, which can be used for many effects",
                            "ui.objects.addMask"
                        )
                    ) {
                        val parent = baseTransform.parent!!
                        val i = parent.children.indexOf(baseTransform)
                        if (i < 0) throw RuntimeException()
                        val mask = MaskLayer.create(listOf(Rectangle.create()), listOf(baseTransform))
                        mask.isFullscreen = true
                        parent.setChildAt(mask, i)
                    }
                }
                val additional = baseTransform.getAdditionalChildrenOptions().map { option ->
                    MenuOption(NameDesc(option.title, option.description, "")) {
                        RemsStudio.largeChange("Added ${option.title}") {
                            val new = option.generator() as Transform
                            baseTransform.addChild(new)
                            Selection.selectTransform(new)
                        }
                    }
                }
                if (additional.isNotEmpty()) {
                    extras += Menu.menuSeparator1
                    extras += additional
                }
                Menu.openMenu(
                    Input.mouseX, Input.mouseY, NameDesc("Add Child", "", "ui.objects.add"),
                    options.entries
                        .sortedBy { (key, _) -> key.lowercase(Locale.getDefault()) }
                        .map { (key, value) ->
                            val sample = if (value is Transform) value.clone() else value.toString().toTransform()
                            MenuOption(NameDesc(key, sample?.defaultDisplayName ?: "", ""), add {
                                val newT = if (value is Transform) value.clone() else value.toString().toTransform()
                                newT!!
                                it.addChild(newT)
                                newT
                            })
                        } + extras
                )
            } else LOGGER.warn(Dict["Reset the config to enable this menu!", "config.warn.needsReset.forMenu"])
        }
    }

}