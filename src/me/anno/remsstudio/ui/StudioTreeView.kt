package me.anno.remsstudio.ui

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.io.text.TextWriter
import me.anno.io.utils.StringMap
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.remsstudio.objects.Camera
import me.anno.remsstudio.objects.Rectangle
import me.anno.remsstudio.objects.Transform
import me.anno.remsstudio.objects.Transform.Companion.toTransform
import me.anno.remsstudio.objects.effects.MaskLayer
import me.anno.remsstudio.RemsStudio
import me.anno.remsstudio.RemsStudio.nullCamera
import me.anno.remsstudio.RemsStudio.windowStack
import me.anno.remsstudio.Selection
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.treeView.TreeView
import me.anno.ui.style.Style
import me.anno.utils.Color.toARGB
import me.anno.maths.Maths.clamp
import me.anno.utils.structures.lists.UpdatingList
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*

// todo select multiple elements, filter for common properties, and apply them all together :)

class StudioTreeView(style: Style) :
    TreeView<Transform>(
        UpdatingList { listOf(nullCamera!!, RemsStudio.root) },
        StudioFileImporter, true, style
    ) {

    override fun getDragType(element: Transform): String = "Transform"

    override fun stringifyForCopy(element: Transform): String = TextWriter.toText(element)

    override fun getSymbol(element: Transform): String {
        return element.symbol
    }

    override fun addChild(element: Transform, child: Any) {
        element.addChild(child as? Transform ?: return)
    }

    override fun removeChild(element: Transform, child: Transform) {
        element.removeChild(child)
    }

    override fun addBefore(self: Transform, sibling: Any) {
        sibling as Transform
        self.addBefore(sibling)
    }

    override fun addAfter(self: Transform, sibling: Any) {
        sibling as Transform
        self.addAfter(sibling)
    }

    override fun setCollapsed(element: Transform, collapsed: Boolean) {
        element.isCollapsedI.value = collapsed
    }

    override fun isCollapsed(element: Transform): Boolean {
        return element.isCollapsed
    }

    override fun setName(element: Transform, name: String) {
        element.nameI.value = name
    }

    override fun getName(element: Transform): String {
        return element.name.ifBlank { element.defaultDisplayName }
    }

    override fun getParent(element: Transform): Transform? {
        return element.parent
    }

    override fun getChildren(element: Transform): List<Transform> {
        return element.children
    }

    override fun destroy(element: Transform) {
        element.onDestroy()
    }

    override fun canBeInserted(parent: Transform, element: Transform, index: Int): Boolean {
        val immutable = parent.listOfInheritance.any { it.areChildrenImmutable }
        return !immutable
    }

    override fun canBeRemoved(element: Transform): Boolean {
        val parent = element.parent ?: return false // root cannot be removed
        val immutable = parent.listOfInheritance.any { it.areChildrenImmutable }
        return !immutable
    }

    override fun selectElement(element: Transform?) {
        Selection.selectTransform(element)
    }

    override fun selectElementMaybe(element: Transform?) {
        // if already selected, don't inspect that property/driver
        if (Selection.selectedTransform == element) Selection.clear()
        selectElement(element)
    }

    override fun focusOnElement(element: Transform) {
        zoomToObject(element)
    }

    override fun openAddMenu(parent: Transform) {
        Companion.openAddMenu(parent)
    }

    private val tmp = Vector4f()
    override fun getLocalColor(element: Transform, isHovered: Boolean, isInFocus: Boolean): Int {
        val dst = element.getLocalColor(tmp)
        dst.w = 0.5f + 0.5f * clamp(dst.w, 0f, 1f)
        var textColor = dst.toARGB()
        val sample = sample
        if (isHovered) textColor = sample.hoverColor
        if (isInFocus) textColor = sample.accentColor
        return textColor
    }

    override fun getTooltipText(element: Transform): String? {
        return if (element is Camera) {
            element.defaultDisplayName + Dict[", drag onto scene to view", "ui.treeView.dragCameraToView"]
        } else element::class.simpleName
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        if (!tryPasteTransform(data)) {
            super.onPaste(x, y, data, type)
        }
    }

    /**
     * returns true on success
     * */
    private fun tryPasteTransform(data: String): Boolean {
        val transform = data.toTransform() ?: return false
        RemsStudio.largeChange("Pasted ${transform.name}") {
            RemsStudio.root.addChild(transform)
        }
        return true
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
            LOGGER.info(objectCameraPosition)
            // apply this movement
            RemsStudio.largeChange("Move Camera to Object") {
                camera.position.addKeyframe(camera.lastLocalTime, objectCameraPosition)
            }
            /* askName(this.x, this.y, NameDesc(), getElement().name, NameDesc("Change Name"), { textColor }) {
                 getElement().name = it
             }*/
        }

        private val LOGGER = LogManager.getLogger(StudioTreeView::class)
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
                    windowStack,
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

    override val className get() = "StudioTreeView"

}