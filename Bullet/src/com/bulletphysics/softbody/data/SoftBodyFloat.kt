package com.bulletphysics.softbody.data

import com.bulletphysics.collision.dispatch.CollisionObject

class SoftBodyFloat {

    lateinit var collisionObject: CollisionObject
    lateinit var pose: SoftBodyPose
    lateinit var materials: List<SoftBodyMaterial>
    lateinit var nodes: List<SoftBodyNode>
    lateinit var links: List<SoftBodyLink>
    lateinit var faces: List<SoftBodyFace>
    lateinit var tetrahedra: List<SoftBodyTetra>
    lateinit var anchors: List<SoftRigidAnchor>
    lateinit var clusters: List<SoftBodyCluster>
    lateinit var joints: List<SoftBodyJoint>

    lateinit var config: SoftBodyConfig

}