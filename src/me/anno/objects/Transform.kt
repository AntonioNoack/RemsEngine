package me.anno.objects

import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.GFX.toRadians
import me.anno.gpu.OpenGL
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.drawing.GFXx3D.draw3DCircle
import me.anno.gpu.shader.ShaderPlus
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.language.translation.Dict
import me.anno.objects.inspectable.Inspectable
import me.anno.objects.modes.TransformVisibility
import me.anno.objects.particles.ParticleSystem
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.editorTime
import me.anno.studio.rems.Scene
import me.anno.studio.rems.Selection.select
import me.anno.studio.rems.Selection.selectTransform
import me.anno.studio.rems.Selection.selectedTransform
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.LinkPanel
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.TimelinePanel
import me.anno.ui.editor.TimelinePanel.Companion.global2Kf
import me.anno.ui.editor.components.ComponentUI
import me.anno.ui.editor.stacked.Option
import me.anno.ui.input.TextInput
import me.anno.ui.input.TextInputML
import me.anno.ui.style.Style
import me.anno.utils.Color.mulARGB
import me.anno.maths.Maths.clamp
import me.anno.utils.structures.Hierarchical
import me.anno.utils.structures.ValueWithDefault
import me.anno.utils.structures.ValueWithDefault.Companion.writeMaybe
import me.anno.utils.structures.ValueWithDefaultFunc
import me.anno.utils.types.Casting.castToDouble
import me.anno.utils.types.Casting.castToDouble2
import me.anno.utils.types.Matrices.skew
import me.anno.utils.types.Strings.isBlank2
import me.anno.video.MissingFrameException
import org.apache.logging.log4j.LogManager
import org.joml.*
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

open class Transform() : Saveable(),
    Inspectable, Hierarchical<Transform> {

    // todo generally "play" the animation of a single transform for testing purposes?
    // todo maybe only for video or audio? for audio it would be simple :)
    // useful for audio, video, particle systems, generally animations
    // only available if the rest is stopped? yes.

    final override var parent: Transform? = null

    constructor(parent: Transform?) : this() {
        this.parent = parent
        parent?.children?.add(this)
    }

    override val symbol: String
        get() = DefaultConfig["ui.symbol.folder", "\uD83D\uDCC1"]
    override val description: String
        get() = ""
    override val defaultDisplayName: String
        get() = if (className == "Transform") Dict["Folder", "obj.folder"] else className

    val clickId = nextClickId.incrementAndGet()

    val timelineSlot = ValueWithDefault(-1)

    var visibility = TransformVisibility.VISIBLE
    override var isEnabled = true

    var position = AnimatedProperty.pos()
    var scale = AnimatedProperty.scale()
    var rotationYXZ = AnimatedProperty.rotYXZ()

    var skew = AnimatedProperty.skew()
    var alignWithCamera = AnimatedProperty.float01()
    var color = AnimatedProperty.color()
    var colorMultiplier = AnimatedProperty.floatPlus(1f)

    val fadeIn = AnimatedProperty.floatPlus(0.1f)
    val fadeOut = AnimatedProperty.floatPlus(0.1f)

    open fun getStartTime(): Double = Double.NEGATIVE_INFINITY
    open fun getEndTime(): Double = Double.POSITIVE_INFINITY

    var blendMode = BlendMode.INHERIT

    var timeOffset = ValueWithDefault(0.0)
    var timeDilation = ValueWithDefault(1.0)

    var timeAnimated = AnimatedProperty.double()

    // update this value before drawing everything
    override var indexInParent = 0
    var drawnChildCount = 0

    var nameI = ValueWithDefaultFunc { defaultDisplayName }
    override var name: String
        get() = nameI.value
        set(value) {
            val v = value.trim()
            if (v.isBlank2()) {
                nameI.reset()
            } else {
                nameI.value = v
            }
        }

    var comment = ""
    var tags = ""

    // todo display warnings, if applicable
    var lastWarning: String? = null

    open fun getDocumentationURL(): URL? = null

    open fun isVisible(localTime: Double) = true

    val rightPointingTriangle = "▶"
    val bottomPointingTriangle = "▼"
    val folder = "\uD83D\uDCC1"

    override fun add(child: Transform) {
        children.add(child)
        child.parent = this
    }

    override fun add(index: Int, child: Transform) {
        children.add(index, child)
        child.parent = this
    }

    override fun deleteChild(child: Transform) {
        children.remove(child)
    }

    override val children = ArrayList<Transform>()

    val isCollapsedI = ValueWithDefault(false)
    override var isCollapsed: Boolean
        get() = isCollapsedI.value
        set(value) {
            isCollapsedI.value = value
        }

    var lastLocalColor = Vector4f()
    var lastLocalTime = 0.0

    private val weightI = ValueWithDefault(1f)
    var weight: Float
        get() = weightI.value
        set(value) {
            weightI.value = value
        }

    fun putValue(list: AnimatedProperty<*>, value: Any, updateHistory: Boolean) {
        val time = global2Kf(editorTime)
        if (updateHistory) {
            RemsStudio.incrementalChange("Change Keyframe Value") {
                list.addKeyframe(time, value, TimelinePanel.propertyDt)
            }
        } else {
            list.addKeyframe(time, value, TimelinePanel.propertyDt)
        }
    }

    open fun clearCache() {}

    fun setChildAt(child: Transform, index: Int) {
        if (this in child.listOfAll) throw RuntimeException()
        if (index >= children.size) {
            children.add(child)
        } else children[index] = child
        child.parent = this
    }

    fun show(anim: AnimatedProperty<*>?) {
        select(this, anim)
    }

    private val tmp0 = Vector4f()
    private val tmp1 = Vector4f()
    open fun claimResources(pTime0: Double, pTime1: Double, pAlpha0: Float, pAlpha1: Float) {
        val lTime0 = getLocalTime(pTime0)
        val lAlpha0 = getLocalColor(tmp0.set(0f, 0f, 0f, pAlpha0), lTime0, tmp0).w
        val lTime1 = getLocalTime(pTime1)
        val lAlpha1 = getLocalColor(tmp1.set(0f, 0f, 0f, pAlpha1), lTime0, tmp1).w
        if (lAlpha0 > minAlpha || lAlpha1 > minAlpha) {
            claimLocalResources(lTime0, lTime1)
            children.forEach {
                it.claimResources(lTime0, lTime1, lAlpha0, lAlpha1)
            }
        }
    }

    open fun claimLocalResources(lTime0: Double, lTime1: Double) {
        // here is nothing to claim
        // only for things using video textures
    }

    open fun usesFadingDifferently() = false

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {

        list += TextInput("Name ($className)", "", name, style)
            .addChangeListener { name = if (it.isEmpty()) "-" else it }
            .setIsSelectedListener { show(null) }
        list += TextInputML("Comment", comment, style)
            .addChangeListener { comment = it }
            .setIsSelectedListener { show(null) }

        val warningPanel = UpdatingTextPanel(500, style) { lastWarning }
        warningPanel.textColor = warningPanel.textColor.mulARGB(0xffff3333.toInt())
        list += warningPanel

        val docs = getDocumentationURL()
        if (docs != null) {
            val docsPanel = LinkPanel(docs, style)
            docsPanel.setTooltip("Learn more")
            list += docsPanel
        }

        // todo dedicated tags-input field
        // todo sort for tags
        // - crosses to remove tags
        // - sort them?
        // - a field to add new ones
        list += TextInput("Tags", "", tags, style)
            .addChangeListener { tags = it }
            .setIsSelectedListener { show(null) }
            .setTooltip("For Search | Not implemented yet")

        // transforms
        val transform = getGroup("Transform", "Translation Scale, Rotation, Skewing", "transform")
        transform += vi("Position", "Location of this object", position, style)
        transform += vi("Scale", "Makes it bigger/smaller", scale, style)
        transform += vi("Rotation", "Pitch,Yaw,Roll", rotationYXZ, style)
        transform += vi("Skew", "Transform it similar to a shear", skew, style)
        transform += vi(
            "Alignment with Camera", "0 = in 3D, 1 = looking towards the camera; billboards",
            alignWithCamera, style
        )

        // color
        val colorGroup = getGroup("Color", "", "color")
        colorGroup += vi("Color", "Tint, applied to this & children", color, style)
        colorGroup += vi("Color Multiplier", "To make things brighter than usually possible", colorMultiplier, style)

        val ufd = usesFadingDifferently()
        if (ufd || getStartTime().isFinite()) colorGroup += vi("Fade In", "Transparency at the start", fadeIn, style)
        if (ufd || getEndTime().isFinite()) colorGroup += vi("Fade Out", "Transparency at the end", fadeOut, style)

        // kind of color...
        colorGroup += vi("Blend Mode", "", null, blendMode, style) { blendMode = it }

        // time
        val timeGroup = getGroup("Time", "", "time")
        timeGroup += vi("Start Time", "Delay the animation", "", null, timeOffset, style)
        timeGroup += vi("Time Multiplier", "Speed up the animation", "", dilationType, timeDilation, style)
        timeGroup += vi("Advanced Time", "Add acceleration/deceleration to your elements", timeAnimated, style)

        val editorGroup = getGroup("Editor", "", "editor")
        editorGroup += vi(
            "Timeline Slot", "< 1 means invisible", Type.INT_PLUS, timelineSlot.value, style
        ) { timelineSlot.value = it }
        // todo warn of invisible elements somehow!...
        editorGroup += vi("Visibility", "", null, visibility, style) { visibility = it }

        if (parent?.acceptsWeight() == true) {
            val psGroup = getGroup("Particle System Child", "", "particles")
            psGroup += vi("Weight", "For particle systems", Type.FLOAT_PLUS, weight, style) {
                weight = it
                (parent as? ParticleSystem)?.apply {
                    if (children.size > 1) clearCache()
                }
            }
        }

    }

    open fun getLocalTime(parentTime: Double): Double {
        var localTime0 = (parentTime - timeOffset.value) * timeDilation.value
        localTime0 += timeAnimated[localTime0]
        return localTime0
    }

    fun getLocalColor(dst: Vector4f = Vector4f()): Vector4f {
        return getLocalColor(
            parent?.getLocalColor(dst),
            lastLocalTime, dst
        )
    }

    fun updateLocalColor(parentColor: Vector4fc, localTime: Double) {
        lastLocalColor = getLocalColor(parentColor, localTime, lastLocalColor)
    }

    fun getLocalAlpha(parentAlpha: Float, localTime: Double, tmp: Vector4f): Float {
        var col = color.getValueAt(localTime, tmp).w()
        val fadeIn = fadeIn[localTime]
        val fadeOut = fadeOut[localTime]
        val m1 = clamp((localTime - getStartTime()) / fadeIn, 0.0, 1.0)
        val m2 = clamp((getEndTime() - localTime) / fadeOut, 0.0, 1.0)
        val fading = (m1 * m2).toFloat()
        col *= parentAlpha * fading
        return col
    }

    fun getLocalColor(parentColor: Vector4fc?, localTime: Double, dst: Vector4f = Vector4f()): Vector4f {
        // we would need a temporary value for the parent color, as may be parentColor == dst
        var px = 1f
        var py = 1f
        var pz = 1f
        var pw = 1f
        if (parentColor != null) {
            px = parentColor.x()
            py = parentColor.y()
            pz = parentColor.z()
            pw = parentColor.w()
        }
        val col = color.getValueAt(localTime, dst)
        val mul = colorMultiplier[localTime]
        val fadeIn = fadeIn[localTime]
        val fadeOut = fadeOut[localTime]
        val m1 = clamp((localTime - getStartTime()) / fadeIn, 0.0, 1.0)
        val m2 = clamp((getEndTime() - localTime) / fadeOut, 0.0, 1.0)
        val fading = (m1 * m2).toFloat()
        if (dst !== col) dst.set(col)
        if (parentColor != null) dst.mul(px, py, pz, pw)
        dst.mul(mul, mul, mul, fading)
        return dst
    }

    fun applyTransformLT(transform: Matrix4f, time: Double) {

        val position = position[time]
        val scale = scale[time]
        val euler = rotationYXZ[time]
        val skew = skew[time]
        val alignWithCamera = alignWithCamera[time]

        if (position.x != 0f || position.y != 0f || position.z != 0f) {
            transform.translate(position)
        }

        if (euler.y() != 0f) transform.rotate(toRadians(euler.y()), yAxis)
        if (euler.x() != 0f) transform.rotate(toRadians(euler.x()), xAxis)
        if (euler.z() != 0f) transform.rotate(toRadians(euler.z()), zAxis)

        if (scale.x() != 1f || scale.y() != 1f || scale.z() != 1f) transform.scale(scale)

        if (skew.x() != 0f || skew.y() != 0f) transform.skew(skew)

        if (alignWithCamera != 0f) {
            transform.alignWithCamera(alignWithCamera)
        }

    }

    fun Matrix4f.alignWithCamera(alignWithCamera: Float) {
        // lerp rotation instead of full transform?
        if (alignWithCamera != 0f) {
            val local = Scene.lGCTInverted
            val up = local.transformDirection(Vector3f(0f, 1f, 0f))
            val forward = local.transformDirection(Vector3f(0f, 0f, -1f))
            if (alignWithCamera == 1f) {
                lookAlong(forward, up)
            } else {
                lerp(Matrix4f(this).lookAlong(forward, up), alignWithCamera)
            }
        }
    }

    fun applyTransformPT(transform: Matrix4f, parentTime: Double) =
        applyTransformLT(transform, getLocalTime(parentTime))

    /**
     * stack with camera already included
     * */
    fun draw(stack: Matrix4fArrayList, parentTime: Double, parentColor: Vector4fc) {

        val time = getLocalTime(parentTime)
        val color = getLocalColor(parentColor, time, tmp0)

        draw(stack, time, parentColor, color)

    }

    fun draw(stack: Matrix4fArrayList, time: Double, parentColor: Vector4fc, color: Vector4fc) {

        if (color.w() > minAlpha && visibility.isVisible) {
            applyTransformLT(stack, time)
            drawDirectly(stack, time, parentColor, color)
        }

    }

    fun drawDirectly(stack: Matrix4fArrayList, time: Double, parentColor: Vector4fc, color: Vector4fc) {

        GFX.drawnId = clickId
        val doBlending = when (GFX.drawMode) {
            ShaderPlus.DrawMode.COLOR_SQUARED, ShaderPlus.DrawMode.COLOR -> true
            else -> false
        }

        if (doBlending) {
            OpenGL.blendMode.use(blendMode) {
                onDraw(stack, time, color)
                drawChildren(stack, time, color, parentColor)
            }
        } else {
            onDraw(stack, time, color)
            drawChildren(stack, time, color, parentColor)
        }
    }

    fun drawChildren(stack: Matrix4fArrayList, time: Double, color: Vector4fc, parentColor: Vector4fc) {
        val passesOnColor = passesOnColor()
        val childColor = if (passesOnColor) color else parentColor
        if (drawChildrenAutomatically()) {
            drawChildren(stack, time, childColor)
        }
    }

    open fun drawChildrenAutomatically() = true

    fun drawChildren(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        val size = children.size
        drawnChildCount = size
        for (i in 0 until size) {
            val child = children[i]
            child.indexInParent = i
            drawChild(stack, time, color, child)
        }
    }

    fun drawChild(stack: Matrix4fArrayList, time: Double, color: Vector4fc, child: Transform?) {
        if (child != null) {
            stack.next {
                child.draw(stack, time, color)
            }
        }
    }

    open fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        drawUICircle(stack, 0.02f, 0.7f, color)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        // many properties are only written if they changed; to reduce file sizes and make things clearer
        // when copy-pasting stuff
        // writer.writeObject(this, "parent", parent) // no longer required, as far I can see...
        writer.writeMaybe(this, "name", nameI)
        writer.writeString("comment", comment)
        writer.writeString("tags", tags)
        writer.writeMaybe(this, "collapsed", isCollapsedI)
        writer.writeMaybe(this, "weight", weightI)
        writer.writeObject(this, "position", position)
        writer.writeObject(this, "scale", scale)
        writer.writeObject(this, "rotationYXZ", rotationYXZ)
        writer.writeObject(this, "skew", skew)
        writer.writeObject(this, "alignWithCamera", alignWithCamera)
        writer.writeMaybe(this, "timeOffset", timeOffset)
        writer.writeMaybe(this, "timeDilation", timeDilation)
        writer.writeObject(this, "timeAnimated", timeAnimated)
        writer.writeObject(this, "color", color)
        writer.writeObject(this, "colorMultiplier", colorMultiplier)
        writer.writeObject(this, "fadeIn", fadeIn)
        writer.writeObject(this, "fadeOut", fadeOut)
        if (blendMode !== BlendMode.INHERIT) writer.writeString("blendMode", blendMode.id)
        writer.writeObjectList(this, "children", children)
        writer.writeMaybe(this, "timelineSlot", timelineSlot)
        writer.writeInt("visibility", visibility.id, false)
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "collapsed" -> isCollapsed = value
            else -> super.readBoolean(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "timelineSlot" -> timelineSlot.value = value
            "visibility" -> visibility = TransformVisibility[value]
            else -> super.readInt(name, value)
        }
    }

    override fun readLong(name: String, value: Long) {
        when (name) {
            "uuid" -> Unit// hide the warning
            else -> super.readLong(name, value)
        }
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "weight" -> weight = value
            else -> super.readFloat(name, value)
        }
    }

    override fun readDouble(name: String, value: Double) {
        when (name) {
            "timeDilation" -> timeDilation.value = value
            "timeOffset" -> timeOffset.value = value
            "fadeIn" -> if (value >= 0.0) fadeIn.set(value.toFloat())
            "fadeOut" -> if (value >= 0.0) fadeOut.set(value.toFloat())
            else -> super.readDouble(name, value)
        }
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "name" -> this.name = value ?: ""
            "comment" -> comment = value ?: ""
            "tags" -> tags = value ?: ""
            "blendMode" -> blendMode = BlendMode[value ?: ""]
            else -> super.readString(name, value)
        }
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "children" -> values.filterIsInstance<Transform>().forEach(::addChild)
            else -> super.readObjectArray(name, values)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "parent" -> {
                if (value is Transform) {
                    try {
                        value.addChild(this)
                    } catch (e: RuntimeException) {
                        LOGGER.warn(e.message.toString())
                    }
                }
            }
            "children" -> {
                if (value is Transform) {
                    addChild(value)
                }
            }
            "position" -> position.copyFrom(value)
            "scale" -> scale.copyFrom(value)
            "rotationYXZ" -> rotationYXZ.copyFrom(value)
            "skew" -> skew.copyFrom(value)
            "alignWithCamera" -> alignWithCamera.copyFrom(value)
            "timeAnimated" -> timeAnimated.copyFrom(value)
            "color" -> color.copyFrom(value)
            "colorMultiplier" -> colorMultiplier.copyFrom(value)
            "fadeIn" -> fadeIn.copyFrom(value)
            "fadeOut" -> fadeOut.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override val className get() = "Transform"
    override val approxSize get() = 1024 + listOfAll.count()

    /*fun contains(t: Transform): Boolean {
        if (t === this) return true
        if (children != null) {// can be null on init
            for (child in children) {
                if (child === t || child.contains(t)) return true
            }
        }
        return false
    }*/

    /*override fun addBefore(child: Transform) {
        val p = parent!!
        val index = p.children.indexOf(this)
        p.children.add(index, child)
        child.parent = p
    }

    override fun addAfter(child: Transform) {
        val p = parent!!
        val index = p.children.indexOf(this)
        p.children.add(index + 1, child)
        child.parent = p
    }

    override fun addChild(child: Transform) {
        if (
            glThread != null &&
            Thread.currentThread() != glThread &&
            this in root.listOfAll
        ) throw RuntimeException("Called from wrong thread!")
        if (child.contains(this)) throw RuntimeException("this cannot contain its parent!")
        child.parent?.removeChild(child)
        child.parent = this
        children += child
    }

    override fun removeChild(child: Transform) {
        child.parent = null
        children.remove(child)
    }*/

    fun stringify(): String {
        val myParent = parent
        parent = null
        val data = TextWriter.toText(this)
        parent = myParent
        return data
    }

    fun setName(name: String): Transform {
        this.name = name
        return this
    }

    fun getLocalTransform(globalTime: Double, reference: Transform): Matrix4f {
        val (parentTransform, parentTime) =
            if (reference === parent) Matrix4f() to globalTime
            else parent?.getGlobalTransformTime(globalTime) ?: Matrix4f() to globalTime
        val localTime = getLocalTime(parentTime)
        applyTransformLT(parentTransform, localTime)
        return parentTransform
    }

    fun getLocalTime(globalTime: Double, reference: Transform): Double {
        val parentTime =
            if (reference === parent) globalTime
            else parent?.getGlobalTime(globalTime) ?: globalTime
        return getLocalTime(parentTime)
    }

    fun getLocalTransformTime(globalTime: Double, reference: Transform): Pair<Matrix4f, Double> {
        val (parentTransform, parentTime) =
            if (reference === parent) Matrix4f() to globalTime
            else parent?.getGlobalTransformTime(globalTime) ?: Matrix4f() to globalTime
        val localTime = getLocalTime(parentTime)
        applyTransformLT(parentTransform, localTime)
        return parentTransform to localTime
    }

    fun getGlobalTransform(globalTime: Double): Matrix4f {
        val (parentTransform, parentTime) = parent?.getGlobalTransformTime(globalTime) ?: Matrix4f() to globalTime
        val localTime = getLocalTime(parentTime)
        applyTransformLT(parentTransform, localTime)
        return parentTransform
    }

    fun getGlobalTime(globalTime: Double): Double {
        val parentTime = parent?.getGlobalTime(globalTime) ?: globalTime
        return getLocalTime(parentTime)
    }

    fun getGlobalTransformTime(globalTime: Double, dst: Matrix4f = Matrix4f()): Pair<Matrix4f, Double> {
        val (parentTransform, parentTime) = parent?.getGlobalTransformTime(globalTime, dst) ?: dst to globalTime
        val localTime = getLocalTime(parentTime)
        applyTransformLT(parentTransform, localTime)
        return parentTransform to localTime
    }

    /*fun removeFromParent() {
        parent?.removeChild(this)
        parent = null
    }*/

    override fun isDefaultValue() = false

    open fun clone(): Transform {
        val asString = try {
            TextWriter.toText(this)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
        try {
            return asString.toTransform()!!
        } catch (e: Exception) {
            LOGGER.warn(asString)
            e.printStackTrace()
            throw RuntimeException("Failed to parse!")
        }
    }

    open fun acceptsWeight() = false
    open fun passesOnColor() = true

    fun <V> vi(
        title: String, ttt: String, dictPath: String,
        type: Type?, value: V,
        style: Style, setValue: (V) -> Unit
    ): Panel {
        return vi(Dict[title, "obj.$dictPath"], Dict[ttt, "obj.$dictPath.desc"], type, value, style, setValue)
    }

    fun <V> vi(
        title: String, ttt: String, dictPath: String,
        type: Type?, value: ValueWithDefault<V>,
        style: Style
    ) = vi(Dict[title, "obj.$dictPath"], Dict[ttt, "obj.$dictPath.desc"], type, value, style)

    fun <V> vi(
        title: String, ttt: String,
        type: Type?, value: ValueWithDefault<V>,
        style: Style
    ): Panel {
        return vi(title, ttt, type, value.value, style) {
            value.value = it
        }
    }

    /**
     * creates a panel with the correct input for the type, and sets the default values:
     * title, tool tip text, type, start value
     * callback is used to adjust the value
     * */
    @Suppress("UNCHECKED_CAST") // all casts are checked in all known use-cases ;)
    fun <V> vi(
        title: String, ttt: String,
        type: Type?, value: V,
        style: Style, setValue: (V) -> Unit
    ): Panel {
        return ComponentUI.vi(this, title, ttt, type, value, style, setValue)
    }

    fun vi(title: String, ttt: String, dictSubPath: String, values: AnimatedProperty<*>, style: Style): Panel {
        return vi(Dict[title, "obj.$dictSubPath"], Dict[ttt, "obj.$dictSubPath.desc"], values, style)
    }

    /**
     * creates a panel with the correct input for the type, and sets the default values:
     * title, tool tip text, type, start value
     * modifies the AnimatedProperty-Object, so no callback is needed
     * */
    fun vi(title: String, ttt: String, values: AnimatedProperty<*>, style: Style): Panel {
        return ComponentUI.vi(this, title, ttt, values, style)
    }

    override fun onDestroy() {
        if (selectedTransform === this) {
            selectTransform(null)
        }
    }

    override fun destroy() {
        removeFromParent()
        onDestroy()
    }

    /**
     * return from this to root all parents
     * */
    val listOfInheritance: Sequence<Transform>
        get() = sequence {
            yield(this@Transform)
            val parent = parent
            if (parent != null) {
                yieldAll(parent.listOfInheritance)
            }
        }

    fun getDepth(): Int {
        var element: Transform? = this
        var ctr = 0
        while (element != null) {
            element = element.parent
            ctr++
        }
        return ctr
    }

    private var tmpHierarchy: Array<Transform?>? = null

    /**
     * get the local time at that globalTime;
     * withAllocation = false may allocate as well ;p, but at least it's thread safe,
     * and it will allocate only, if the hierarchy changes
     * */
    fun getLocalTimeFromRoot(globalTime: Double, withAllocation: Boolean): Double {
        if (withAllocation) {
            val inh = listOfInheritance.toList().reversed()
            var localTime = globalTime
            for (e in inh) {
                localTime = e.getLocalTime(localTime)
            }
            return localTime
        } else {
            val depth = getDepth()
            var hierarchy = tmpHierarchy
            if (hierarchy == null || hierarchy.size < depth) {
                hierarchy = arrayOfNulls(depth)
                this.tmpHierarchy = hierarchy
            }
            var element: Transform? = this
            var ctr = 0
            while (element != null) {
                hierarchy[ctr++] = element
                element = element.parent
            }
            var localTime = globalTime
            for (i in ctr - 1 downTo 0) {
                localTime = hierarchy[i]!!
                    .getLocalTime(localTime)
            }
            return localTime
        }
    }

    fun checkFinalRendering() {
        if (isFinalRendering) throw MissingFrameException(this)
    }

    open fun getAdditionalChildrenOptions(): List<Option> = emptyList()

    open val areChildrenImmutable: Boolean = false

    open fun getRelativeSize() = Vector3f(1f)

    companion object {

        fun drawUICircle(stack: Matrix4fArrayList, scale: Float = 0.02f, inner: Float = 0.7f, color: Vector4fc) {
            // draw a small symbol to indicate pivot
            if (!isFinalRendering) {
                stack.pushMatrix()
                /*val w = GFX.windowWidth
                val h = GFX.windowHeight
                stack.m00(scale*h/w);stack.m01(0f);stack.m02(0f)
                stack.m10(0f);stack.m11(scale);stack.m12(0f)
                stack.m20(0f);stack.m21(0f);stack.m22(scale)*/
                stack.scale(scale)
                draw3DCircle(null, 0.0, stack, inner, 0f, 360f, color)
                stack.popMatrix()
            }
        }

        // these values MUST NOT be changed
        // they are universal constants, and are used
        // within shaders, too
        val xAxis: Vector3fc = Vector3f(1f, 0f, 0f)
        val yAxis: Vector3fc = Vector3f(0f, 1f, 0f)
        val zAxis: Vector3fc = Vector3f(0f, 0f, 1f)

        val nextClickId = AtomicInteger()

        fun String.toTransform() = TextReader.read(this, true).first() as? Transform

        const val minAlpha = 0.5f / 255f
        private val LOGGER = LogManager.getLogger(Transform::class)

        val dilationType = Type(1.0, 1, 1f, true, true, ::castToDouble2, ::castToDouble)

    }


}