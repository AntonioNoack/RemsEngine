package me.anno.openxr

import me.anno.openxr.OpenXRUtils.checkXR
import org.lwjgl.openxr.EXTHandTracking.XR_TYPE_HAND_JOINTS_LOCATE_INFO_EXT
import org.lwjgl.openxr.EXTHandTracking.XR_TYPE_HAND_JOINT_LOCATIONS_EXT
import org.lwjgl.openxr.EXTHandTracking.xrLocateHandJointsEXT
import org.lwjgl.openxr.FBHandTrackingMesh.XR_TYPE_HAND_TRACKING_MESH_FB
import org.lwjgl.openxr.FBHandTrackingMesh.xrGetHandMeshFB
import org.lwjgl.openxr.XrHandJointLocationsEXT
import org.lwjgl.openxr.XrHandJointsLocateInfoEXT
import org.lwjgl.openxr.XrHandTrackerEXT
import org.lwjgl.openxr.XrHandTrackingMeshFB

// todo hand tracking aim
// todo check that extension is available
// The XR_EXT_hand_tracking extension provides a list of hand joint poses which represent the current configuration of the tracked hands. This extension adds a layer of gesture recognition that is used by the system.
// The XR_EXT_hand_tracking extension provides a list of hand joint poses but no mechanism to render a skinned hand mesh.
class OpenXRHandMeshes(val handTracker: XrHandTrackerEXT) {

    // todo try this,
    // todo visualize this,
    // todo use this?

    val handTrackingMesh: XrHandTrackingMeshFB = XrHandTrackingMeshFB.calloc()
        .type(XR_TYPE_HAND_TRACKING_MESH_FB).next(0)

    val locateInfo: XrHandJointsLocateInfoEXT = XrHandJointsLocateInfoEXT.calloc()
        .type(XR_TYPE_HAND_JOINTS_LOCATE_INFO_EXT).next(0)

    val locations: XrHandJointLocationsEXT = XrHandJointLocationsEXT.calloc()
        .type(XR_TYPE_HAND_JOINT_LOCATIONS_EXT).next(0)

    init {
        checkXR(xrGetHandMeshFB(handTracker, handTrackingMesh))
    }

    fun update() {
        checkXR(xrLocateHandJointsEXT(handTracker, locateInfo, locations))
        locations.jointCount()
        locations.jointLocations()[0].pose()
    }
}