package me.anno.tests.game.flatworld

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.input.Key
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.language.translation.NameDesc
import me.anno.tests.game.flatworld.buildings.controls.BuildingDeleteControls
import me.anno.tests.game.flatworld.buildings.controls.BuildingPlaceControls
import me.anno.tests.game.flatworld.streets.StreetSegmentData
import me.anno.tests.game.flatworld.streets.controls.StreetBuildingControls
import me.anno.tests.game.flatworld.streets.controls.StreetDeletingControls
import me.anno.tests.game.flatworld.vehicles.RandomVehicle.spawnRandomVehicle
import me.anno.ui.Panel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.buttons.TextButton.Companion.drawButtonBorder
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.NineTilePanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Color.black
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.white
import me.anno.utils.OS.documents
import me.anno.utils.structures.lists.Lists.firstInstance2

// todo a 3d world has more complicated maths,
//  so get all the basics right on a plane,
//  and then abstract away any raycasting logic in the future
fun main() {

    // todo 1. grid-less street building like in Cities Skylines / Workers & Resources
    // done 1b. delete street segments
    // todo
    //  - move nodes like in https://www.youtube.com/watch?v=Zy6XdcICMIo
    //  - road markings
    //  - place complex intersections?
    //  - import street layouts from OSM
    // todo 2. place buildings
    //  - align (rotate, place?) buildings with nearest street
    // done 2b. delete buildings
    // todo 3. terrain
    //  - streets -> they flatten the terrain until we just can project streets onto them
    //  - buildings
    // todo 4. traffic
    //  - proper traffic rules
    //  - keeping distance
    //  - accelerating/decelerating
    //  - visibility at intersections ~ speed at intersections?
    //  - don't drive into each other
    // todo 5. economy simulation?
    //  - resources
    //  - resource storage
    //  - resource transport
    //  - producers
    //  - consumers
    //  - money
    // todo civilisation simulation
    //  - workers
    //  - work places
    //  - qualifications
    //  - happiness
    //  - fun rides
    //  - red districts?
    //  - parks
    //  - sportive stuff
    // todo 6. serialization
    //  - streets
    //  - buildings
    //  - more stuff?
    // todo 7. sounds
    //  - traffic
    //  - ambient
    //  - people
    //  - factories
    // todo world variety
    //  - trees
    //  - bushes
    //  - animals walking around (deer, boar, bunnies?, cats and dogs in cities, Waschbären, eagles, other birds)


    // the first step is street building, so let's do that:
    //  1. click = place anchor
    //  dragging = show whether straight street can be placed like that
    //  2. click = place middle anchor
    //  dragging = show whether curved street can be placed like that
    //  3. click = place curved street; or if too close to 2nd click, a straight street
    // escape = cancel
    // done differently: when building a street, it gets split into many smaller pieces, for potential deletion
    // done when two streets are crossing over each other, place an anchor at their center, so they form an intersection
    // todo refuse street building, when two are too close
    // done build crossing mesh

    registerCustomClass(FlatWorld::class)
    registerCustomClass(StreetSegmentData::class)

    val saveFile = documents.getChild("RemsEngine/FlatWorldCity/TestCity.json")
    saveFile.getParent().mkdirs()

    val world = JsonStringReader.readFirstOrNull(
        if (saveFile.exists) saveFile.readTextSync() else "", InvalidRef, FlatWorld::class
    ) ?: FlatWorld()
    world.validateMeshes()

    // terrain
    val grassMaterial = Material.diffuse(0x88dd88)
    world.terrain.setScale(100.0)
        .add(MeshComponent(DefaultAssets.plane, grassMaterial))

    testUI3("FlatWorld City") {
        val ui = NineTilePanel(style)
        val sceneUI = testScene(world.scene)
        ui.add(sceneUI)
        val sceneView = sceneUI.listOfAll.firstInstance2(SceneView::class)
        val renderView = sceneView.renderView
        val buildStreets = StreetBuildingControls(world, renderView)
        buildStreets.rotationTargetDegrees.set(-60.0, 40.0, 0.0)
        sceneView.editControls = buildStreets
        val list = PanelListY(style)
        list.add(EditTypeButton(sceneView, "+Street", buildStreets))
        list.add(EditTypeButton(sceneView, "-Street", StreetDeletingControls(world, renderView)))
        list.add(EditTypeButton(sceneView, "+Building", BuildingPlaceControls(world, renderView)))
        list.add(EditTypeButton(sceneView, "-Building", BuildingDeleteControls(world, renderView)))
        list.add(TextButton(NameDesc("Test Vehicle"), style).addLeftClickListener { spawnRandomVehicle(world) })
        list.add(TextButton(NameDesc("Save"), style).addLeftClickListener {
            JsonStringWriter.save(world, saveFile, InvalidRef)
        })
        list.alignmentX = AxisAlignment.MAX
        list.alignmentY = AxisAlignment.CENTER
        ui.add(list)
        ui
    }
}

class EditTypeButton(val sceneView: SceneView, val text: String, val controls: ControlScheme) : Panel(style) {

    // todo icons for these buttons

    val leftColor = style.getColor("borderColorLeft", black or 0x999999)
    val rightColor = style.getColor("borderColorRight", black or 0x111111)
    val topColor = style.getColor("borderColorTop", black or 0x999999)
    val bottomColor = style.getColor("borderColorBottom", black or 0x111111)

    val borderSize = style.getPadding("borderSize", 2)

    val bg0 = backgroundColor
    val bg1 = mixARGB(backgroundColor, white, 0.5f)

    var isPressed = false

    override fun calculateSize(w: Int, h: Int) {
        minW = 96
        minH = 32
    }

    override fun onUpdate() {
        super.onUpdate()
        backgroundColor = if (sceneView.editControls == controls) bg1 else bg0
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        drawButtonBorder(
            leftColor, topColor, rightColor, bottomColor,
            true, borderSize, isPressed
        )
        drawSimpleTextCharByChar(
            x + width / 2, y + height / 2, 2,
            text, -1, backgroundColor,
            AxisAlignment.CENTER, AxisAlignment.CENTER
        )
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        isPressed = true
        val src = sceneView.editControls
        val dst = controls
        sceneView.editControls = dst
        dst.rotationTargetDegrees.set(src.rotationTargetDegrees)
        invalidateDrawing()
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        isPressed = false
        invalidateDrawing()
    }
}

