package me.anno.games.trainbuilder

import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference

val scale = 100f

val folder = getReference("E:/Assets/Unity/Simple/Trains.unitypackage/Assets/SimpleTrains")
val vehiclesFolder = folder.getChild("Prefabs/Vehicles")

fun List<String>.mapChildren(folder: FileReference): List<FileReference>{
    return map { name -> folder.getChild(name) }
}

val personTrainModels = listOf(
    "SM_Veh_Bullet_01.prefab",
    "SM_Veh_Bullet_02.prefab",
    "SM_Veh_Bullet_03.prefab",
).mapChildren(vehiclesFolder)

val cargoTrainModels = listOf(
    "SM_Veh_Freight_01.prefab",
    "SM_Veh_Freight_02.prefab",
    "SM_Veh_Freight_03.prefab"
).mapChildren(vehiclesFolder)

val handlerModels = listOf(
    "SM_Veh_HandCar_01.prefab"
).mapChildren(vehiclesFolder)

val personCarrierModels = listOf(
    "SM_Veh_Bullet_Carriage_01.prefab",
    "SM_Veh_Bullet_Carriage_02.prefab",
    "SM_Veh_Bullet_Carriage_03.prefab"
).mapChildren(vehiclesFolder)

val coalCarrierModels = listOf(
    "SM_Veh_Carriage_Coal_01.prefab"
).mapChildren(vehiclesFolder)

val containerCarrierModels = listOf(
    "SM_Veh_Carriage_Container_01.prefab",
    "SM_Veh_Carriage_Container_02.prefab",
    "SM_Veh_Carriage_Container_03.prefab"
).mapChildren(vehiclesFolder)

val cylinderCarrierModels = listOf(
    "SM_Veh_Carriage_Cylinder_01.prefab",
    "SM_Veh_Carriage_Cylinder_02.prefab",
    "SM_Veh_Carriage_Cylinder_03.prefab"
).mapChildren(vehiclesFolder)

val dirtCarrierModels = listOf(
    "SM_Veh_Carriage_Dirt_01.prefab",
    "SM_Veh_Carriage_Dirt_02.prefab",
).mapChildren(vehiclesFolder)

val emptyCarrierModels = listOf(
    "SM_Veh_Carriage_Empty_01.prefab",
    "SM_Veh_Carriage_Empty_02.prefab",
    "SM_Veh_Carriage_Empty_03.prefab"
).mapChildren(vehiclesFolder)

val fuelCarrierModels = listOf(
    "SM_Veh_Carriage_Fuel_01.prefab"
).mapChildren(vehiclesFolder)

val logsCarrierModels = listOf(
    "SM_Veh_Carriage_Logs_01.prefab"
).mapChildren(vehiclesFolder)

val pipesCarrierModels = listOf(
    "SM_Veh_Carriage_Pipes_01.prefab"
).mapChildren(vehiclesFolder)

val metroTrains = listOf(
    "SM_Veh_Metro_01.prefab",
    "SM_Veh_Metro_02.prefab",
    "SM_Veh_Metro_03.prefab"
).mapChildren(vehiclesFolder)

val metroCarriers = listOf(
    "SM_Veh_Metro_Carriage_01.prefab",
    "SM_Veh_Metro_Carriage_02.prefab",
    "SM_Veh_Metro_Carriage_03.prefab"
).mapChildren(vehiclesFolder)

val cargoCarriers = listOf(
    coalCarrierModels,
    containerCarrierModels,
    cylinderCarrierModels,
    dirtCarrierModels,
    emptyCarrierModels,
    fuelCarrierModels,
    logsCarrierModels,
    pipesCarrierModels,
)

val envFolder = folder.getChild("Prefabs/Environments")

val envModels = listOf(
    "SM_Env_Bridge_Pillar_01.prefab", "SM_Env_GroundMound_01.prefab", "SM_Env_Hills_01.prefab",
    "SM_Env_Hills_02.prefab", "SM_Env_Hills_03.prefab", "SM_Env_Hills_04.prefab",
    "SM_Env_Hills_05.prefab", "SM_Env_Road_Corner_01.prefab", "SM_Env_Road_Corner_02.prefab",
    "SM_Env_Road_Straight_01.prefab", "SM_Env_Road_Straight_02.prefab", "SM_Env_Stair_Walkway_01.prefab",
    "SM_Env_Station_Platform_01.prefab", "SM_Env_SubwayEntrance_01.prefab", "SM_Env_Tile_Grass_01.prefab",
    "SM_Env_Track_Cross_01.prefab", "SM_Env_Track_Diagonal_01.prefab", "SM_Env_Track_End_01.prefab",
    "SM_Env_Track_Raised_Bridge_End_01.prefab", "SM_Env_Track_Raised_Bridge_Straight_01.prefab",
    "SM_Env_Track_Raised_Bridge_Straight_02.prefab", "SM_Env_Track_Raised_Bridge_Straight_03.prefab",
    "SM_Env_Track_Raised_Corner_01.prefab", "SM_Env_Track_Raised_Straight_01.prefab",
    "SM_Env_Track_Ramp_01.prefab", "SM_Env_Track_Ridge_Bridge_01.prefab", "SM_Env_Track_Ridge_Corner_01.prefab",
    "SM_Env_Track_Ridge_Straight_01.prefab", "SM_Env_Track_Ridge_Transition_01.prefab",
    "SM_Env_Track_RoadCrossing_01.prefab", "SM_Env_Track_Split_01.prefab",
    "SM_Env_Track_Switch_01.prefab", "SM_Env_Track_Tunnel_01.prefab",
    "SM_Env_Tree_01.prefab", "SM_Env_Tree_02.prefab", "SM_Env_Tree_03.prefab", "SM_Env_Tree_04.prefab",
    "SM_Env_UnderpassEntrance_01.prefab"
)

val straightRail10 = envFolder.getChild("SM_Env_Track_Straight_01.prefab")
val straightRail5 = envFolder.getChild("SM_Env_Track_Straight_02.prefab")
val curvedRail40 = envFolder.getChild("SM_Env_Track_Corner_01.prefab")
val curvedRail20 = envFolder.getChild("SM_Env_Track_Corner_02.prefab")
val curvedRail10 = envFolder.getChild("SM_Env_Track_Corner_03.prefab")