package com.bulletphysics.dynamics.vehicle

import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.ContactConstraint
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint
import com.bulletphysics.linearmath.MatrixUtil.setRotation
import com.bulletphysics.linearmath.MiscUtil.resize
import com.bulletphysics.linearmath.QuaternionUtil.setRotation
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.util.DoubleArrayList
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.getElement
import com.bulletphysics.util.setMul
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setCross
import com.bulletphysics.util.setNegate
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setScaleAdd
import com.bulletphysics.util.setSub
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Raycast vehicle, very special constraint that turn a rigidbody into a vehicle.
 *
 * @author jezek2
 */
@Suppress("unused")
class RaycastVehicle(tuning: VehicleTuning?, val rigidBody: RigidBody, private val vehicleRaycaster: VehicleRaycaster) :
    TypedConstraint() {

    val forwardWS = ArrayList<Vector3d>()
    val axle = ArrayList<Vector3d>()
    val forwardImpulse = DoubleArrayList()
    val sideImpulse = DoubleArrayList()

    private val tau = 0.0
    private val damping = 0.0
    private var pitchControl = 0.0
    private var steeringValue = 0.0

    /**
     * Velocity of vehicle (positive if velocity vector has same direction as foward vector).
     */
    var currentSpeedKmHour: Double = 0.0
        private set

    var rightAxis: Int = 0
        private set
    var upAxis: Int = 2
        private set
    var forwardAxis: Int = 1
        private set

    val wheelInfo = ArrayList<WheelInfo>()

    // constructor to create a car from an existing rigidbody
    init {
        defaultInit(tuning)
    }

    private fun defaultInit(tuning: VehicleTuning?) {
        this.currentSpeedKmHour = 0.0
        steeringValue = 0.0
    }

    /**
     * Basically most of the code is general for 2 or 4-wheel vehicles, but some of it needs to be reviewed.
     */
    fun addWheel(
        connectionPointCS: Vector3d, wheelDirectionCS0: Vector3d, wheelAxleCS: Vector3d,
        suspensionRestLength: Double, wheelRadius: Double, tuning: VehicleTuning, isFrontWheel: Boolean
    ): WheelInfo {
        val ci = WheelInfoConstructionInfo()

        ci.chassisConnectionCS.set(connectionPointCS)
        ci.wheelDirectionCS.set(wheelDirectionCS0)
        ci.wheelAxleCS.set(wheelAxleCS)
        ci.suspensionRestLength = suspensionRestLength
        ci.wheelRadius = wheelRadius
        ci.suspensionStiffness = tuning.suspensionStiffness
        ci.wheelsDampingCompression = tuning.suspensionCompression
        ci.wheelsDampingRelaxation = tuning.suspensionDamping
        ci.frictionSlip = tuning.frictionSlip
        ci.bIsFrontWheel = isFrontWheel
        ci.maxSuspensionTravelCm = tuning.maxSuspensionTravelCm

        wheelInfo.add(WheelInfo(ci))

        val wheel = wheelInfo.last()

        updateWheelTransformsWS(wheel, false)
        updateWheelTransform(this.numWheels - 1, false)
        return wheel
    }

    fun getWheelTransformWS(wheelIndex: Int, out: Transform): Transform {
        assert(wheelIndex < this.numWheels)
        val wheel = wheelInfo[wheelIndex]
        out.set(wheel.worldTransform)
        return out
    }

    @JvmOverloads
    fun updateWheelTransform(wheelIndex: Int, interpolatedTransform: Boolean = true) {
        val wheel = wheelInfo[wheelIndex]
        updateWheelTransformsWS(wheel, interpolatedTransform)
        val up = Stack.newVec()
        up.setNegate(wheel.raycastInfo.wheelDirectionWS)
        val right = wheel.raycastInfo.wheelAxleWS
        val fwd = Stack.newVec()
        fwd.setCross(up, right)
        fwd.normalize()

        // rotate around steering over de wheelAxleWS
        val steering = wheel.steering

        val steeringOrn = Stack.newQuat()
        setRotation(steeringOrn, up, steering) //wheel.m_steering);
        val steeringMat = Stack.newMat()
        setRotation(steeringMat, steeringOrn)

        val rotatingOrn = Stack.newQuat()
        setRotation(rotatingOrn, right, -wheel.rotation)
        val rotatingMat = Stack.newMat()
        setRotation(rotatingMat, rotatingOrn)

        val basis2 = Stack.newMat()
        basis2.setRow(0, right.x, fwd.x, up.x)
        basis2.setRow(1, right.y, fwd.y, up.y)
        basis2.setRow(2, right.z, fwd.z, up.z)

        val wheelBasis = wheel.worldTransform.basis
        wheelBasis.setMul(steeringMat, rotatingMat)
        wheelBasis.mul(basis2)

        wheel.worldTransform.origin.setScaleAdd(
            wheel.raycastInfo.suspensionLength,
            wheel.raycastInfo.wheelDirectionWS,
            wheel.raycastInfo.hardPointWS
        )

        Stack.subVec(2)
        Stack.subMat(3)
        Stack.subQuat(2)
    }

    fun resetSuspension() {
        var i: Int
        i = 0
        while (i < wheelInfo.size) {
            val wheel = wheelInfo[i]
            wheel.raycastInfo.suspensionLength = wheel.suspensionRestLength
            wheel.suspensionRelativeVelocity = 0.0

            wheel.raycastInfo.contactNormalWS.setNegate(wheel.raycastInfo.wheelDirectionWS)
            //wheel_info.setContactFriction(btScalar(0.0));
            wheel.clippedInvContactDotSuspension = 1.0
            i++
        }
    }

    @JvmOverloads
    fun updateWheelTransformsWS(wheel: WheelInfo, interpolatedTransform: Boolean = true) {
        wheel.raycastInfo.isInContact = false

        val chassisTrans = getChassisWorldTransform(Stack.newTrans())
        if (interpolatedTransform) {
            this.rigidBody.motionState?.getWorldTransform(chassisTrans)
        }

        wheel.raycastInfo.hardPointWS.set(wheel.chassisConnectionPointCS)
        chassisTrans.transform(wheel.raycastInfo.hardPointWS)

        wheel.raycastInfo.wheelDirectionWS.set(wheel.wheelDirectionCS)
        chassisTrans.basis.transform(wheel.raycastInfo.wheelDirectionWS)

        wheel.raycastInfo.wheelAxleWS.set(wheel.wheelAxleCS)
        chassisTrans.basis.transform(wheel.raycastInfo.wheelAxleWS)
        Stack.subTrans(1)
    }

    fun rayCast(wheel: WheelInfo): Double {
        updateWheelTransformsWS(wheel, false)

        var depth = -1.0

        val rayLength = wheel.suspensionRestLength + wheel.wheelRadius

        val rayVector = Stack.newVec()
        rayVector.setScale(rayLength, wheel.raycastInfo.wheelDirectionWS)
        val source = wheel.raycastInfo.hardPointWS
        wheel.raycastInfo.contactPointWS.setAdd(source, rayVector)
        val target = wheel.raycastInfo.contactPointWS

        val param: Double

        val rayResults = VehicleRaycasterResult()

        checkNotNull(vehicleRaycaster)

        val instance = vehicleRaycaster.castRay(source, target, rayResults)

        wheel.raycastInfo.groundObject = null

        if (instance != null) {
            param = rayResults.distFraction
            depth = rayLength * rayResults.distFraction
            wheel.raycastInfo.contactNormalWS.set(rayResults.hitNormalInWorld)
            wheel.raycastInfo.isInContact = true

            wheel.raycastInfo.groundObject = FIXED_OBJECT // todo for driving on dynamic/movable objects!;

            //wheel.m_raycastInfo.m_groundObject = object;
            val hitDistance = param * rayLength
            wheel.raycastInfo.suspensionLength = hitDistance - wheel.wheelRadius

            // clamp on max suspension travel
            val minSuspensionLength = wheel.suspensionRestLength - wheel.maxSuspensionTravelCm * 0.01f
            val maxSuspensionLength = wheel.suspensionRestLength + wheel.maxSuspensionTravelCm * 0.01f
            if (wheel.raycastInfo.suspensionLength < minSuspensionLength) {
                wheel.raycastInfo.suspensionLength = minSuspensionLength
            }
            if (wheel.raycastInfo.suspensionLength > maxSuspensionLength) {
                wheel.raycastInfo.suspensionLength = maxSuspensionLength
            }

            wheel.raycastInfo.contactPointWS.set(rayResults.hitPointInWorld)

            val denominator = wheel.raycastInfo.contactNormalWS.dot(wheel.raycastInfo.wheelDirectionWS)

            val chassisVelocityAtContactPoint = Stack.newVec()
            val relativePosition = Stack.newVec()
            relativePosition.setSub(
                wheel.raycastInfo.contactPointWS,
                rigidBody.getCenterOfMassPosition(Stack.newVec())
            )

            rigidBody.getVelocityInLocalPoint(relativePosition, chassisVelocityAtContactPoint)

            val projVel = wheel.raycastInfo.contactNormalWS.dot(chassisVelocityAtContactPoint)

            if (denominator >= -0.1) {
                wheel.suspensionRelativeVelocity = 0.0
                wheel.clippedInvContactDotSuspension = 1.0 / 0.1
            } else {
                val inv = -1.0 / denominator
                wheel.suspensionRelativeVelocity = projVel * inv
                wheel.clippedInvContactDotSuspension = inv
            }
        } else {
            // put wheel info as in rest position
            wheel.raycastInfo.suspensionLength = wheel.suspensionRestLength
            wheel.suspensionRelativeVelocity = 0.0
            wheel.raycastInfo.contactNormalWS.setNegate(wheel.raycastInfo.wheelDirectionWS)
            wheel.clippedInvContactDotSuspension = 1.0
        }

        return depth
    }

    fun getChassisWorldTransform(out: Transform): Transform {
        /*if (getRigidBody()->getMotionState()){
			btTransform chassisWorldTrans;
			getRigidBody()->getMotionState()->getWorldTransform(chassisWorldTrans);
			return chassisWorldTrans;
		}*/
        return rigidBody.getCenterOfMassTransform(out)
    }

    fun updateVehicle(step: Double) {
        for (i in 0 until this.numWheels) {
            updateWheelTransform(i, false)
        }

        val tmp = Stack.newVec()

        this.currentSpeedKmHour = 3.6f * this.rigidBody.getLinearVelocity(tmp).length()

        val forwardW = Stack.newVec()
        val chassisTrans = getChassisWorldTransform(Stack.newTrans())
        forwardW.set(
            chassisTrans.basis.getElement(0, this.forwardAxis),
            chassisTrans.basis.getElement(1, this.forwardAxis),
            chassisTrans.basis.getElement(2, this.forwardAxis)
        )
        Stack.subTrans(1) // chassisTrans

        if (forwardW.dot(this.rigidBody.getLinearVelocity(tmp)) < 0.0) {
            this.currentSpeedKmHour *= -1.0
        }

        //
        // simulate suspension
        //
        for (i in wheelInfo.indices) {
            rayCast(wheelInfo[i])
        }

        updateSuspension(step)

        for (i in wheelInfo.indices) {
            // apply suspension force
            val wheel = wheelInfo[i]

            var suspensionForce = wheel.wheelsSuspensionForce

            val gMaxSuspensionForce = 6000.0
            if (suspensionForce > gMaxSuspensionForce) {
                suspensionForce = gMaxSuspensionForce
            }
            val impulse = Stack.newVec()
            impulse.setScale(suspensionForce * step, wheel.raycastInfo.contactNormalWS)
            val relPos = Stack.newVec()
            relPos.setSub(wheel.raycastInfo.contactPointWS, this.rigidBody.getCenterOfMassPosition(tmp))

            this.rigidBody.applyImpulse(impulse, relPos)
            Stack.subVec(2)
        }

        updateFriction(step)

        val relPos = Stack.newVec()
        val vel = Stack.newVec()
        for (i in wheelInfo.indices) {
            val wheel = wheelInfo[i]
            relPos.setSub(wheel.raycastInfo.hardPointWS, this.rigidBody.getCenterOfMassPosition(tmp))
            this.rigidBody.getVelocityInLocalPoint(relPos, vel)

            if (wheel.raycastInfo.isInContact) {
                val chassisWorldTransform = getChassisWorldTransform(Stack.newTrans())

                val fwd = Stack.newVec()
                fwd.set(
                    chassisWorldTransform.basis.getElement(0, this.forwardAxis),
                    chassisWorldTransform.basis.getElement(1, this.forwardAxis),
                    chassisWorldTransform.basis.getElement(2, this.forwardAxis)
                )

                val proj = fwd.dot(wheel.raycastInfo.contactNormalWS)
                tmp.setScale(proj, wheel.raycastInfo.contactNormalWS)
                fwd.sub(tmp)

                val proj2 = fwd.dot(vel)

                wheel.deltaRotation = (proj2 * step) / (wheel.wheelRadius)
                Stack.subVec(1)
                Stack.subTrans(1)
            }

            wheel.rotation += wheel.deltaRotation
            wheel.deltaRotation *= 0.99 // damping of rotation when not in contact
        }
        Stack.subVec(4)
    }

    fun setSteeringValue(steering: Double, wheel: Int) {
        assert(wheel >= 0 && wheel < this.numWheels)

        val wheelInfo = getWheelInfo(wheel)
        wheelInfo.steering = steering
    }

    fun getSteeringValue(wheel: Int): Double {
        return getWheelInfo(wheel).steering
    }

    fun applyEngineForce(force: Double, wheel: Int) {
        assert(wheel >= 0 && wheel < this.numWheels)
        val wheelInfo = getWheelInfo(wheel)
        wheelInfo.engineForce = force
    }

    fun getWheelInfo(index: Int): WheelInfo {
        return wheelInfo[index]
    }

    fun setBrake(brake: Double, wheelIndex: Int) {
        assert((wheelIndex >= 0) && (wheelIndex < this.numWheels))
        getWheelInfo(wheelIndex).brake = brake
    }

    fun updateSuspension(deltaTime: Double) {
        val chassisMass = 1.0 / rigidBody.inverseMass

        for (wheelIndex in 0 until this.numWheels) {
            val wheelInfo = this.wheelInfo[wheelIndex]

            if (wheelInfo.raycastInfo.isInContact) {
                var force: Double
                //	Spring
                run {
                    val suspensionLength = wheelInfo.suspensionRestLength
                    val currentLength = wheelInfo.raycastInfo.suspensionLength

                    val lengthDifference = (suspensionLength - currentLength)
                    force = wheelInfo.suspensionStiffness * lengthDifference * wheelInfo.clippedInvContactDotSuspension
                }

                // Damper
                run {
                    val projectedRelVel = wheelInfo.suspensionRelativeVelocity
                    run {
                        val suspensionDamping = if (projectedRelVel < 0.0) {
                            wheelInfo.wheelDampingCompression
                        } else {
                            wheelInfo.wheelDampingRelaxation
                        }
                        force -= suspensionDamping * projectedRelVel
                    }
                }

                // RESULT
                wheelInfo.wheelsSuspensionForce = force * chassisMass
                if (wheelInfo.wheelsSuspensionForce < 0.0) {
                    wheelInfo.wheelsSuspensionForce = 0.0
                }
            } else {
                wheelInfo.wheelsSuspensionForce = 0.0
            }
        }
    }

    private fun calcRollingFriction(contactPoint: WheelContactPoint, numWheelsOnGround: Int): Double {
        val tmp = Stack.newVec()

        val contactPosWorld = contactPoint.frictionPositionWorld

        val relPos1 = Stack.newVec()
        relPos1.setSub(contactPosWorld, contactPoint.body0.getCenterOfMassPosition(tmp))
        val relPos2 = Stack.newVec()
        relPos2.setSub(contactPosWorld, contactPoint.body1.getCenterOfMassPosition(tmp))

        val maxImpulse = contactPoint.maxImpulse

        val vel1 = contactPoint.body0.getVelocityInLocalPoint(relPos1, Stack.newVec())
        val vel2 = contactPoint.body1.getVelocityInLocalPoint(relPos2, Stack.newVec())
        val vel = Stack.newVec()
        vel.setSub(vel1, vel2)

        val relativeVelocity = contactPoint.frictionDirectionWorld.dot(vel)

        // calculate j that moves us to zero relative velocity
        var impulse = -relativeVelocity * contactPoint.jacDiagABInv / numWheelsOnGround
        impulse = min(impulse, maxImpulse)
        impulse = max(impulse, -maxImpulse)

        Stack.subVec(6)

        return impulse
    }

    fun updateFriction(timeStep: Double) {
        // calculate the impulse, so that the wheels don't move sidewards
        val numWheel = this.numWheels
        if (numWheel == 0) {
            return
        }

        resize(forwardWS, numWheel, Vector3d::class.java)
        resize(axle, numWheel, Vector3d::class.java)
        resize(forwardImpulse, numWheel, 0.0)
        resize(sideImpulse, numWheel, 0.0)

        val tmp = Stack.newVec()

        var numWheelsOnGround = 0

        // collapse all those loops into one!
        for (i in 0 until this.numWheels) {
            val wheelInfo = this.wheelInfo[i]
            val groundObject = wheelInfo.raycastInfo.groundObject
            if (groundObject != null) {
                numWheelsOnGround++
            }
            sideImpulse.set(i, 0.0)
            forwardImpulse.set(i, 0.0)
        }

        run {
            val wheelTrans = Stack.newTrans()
            val impulse = Stack.newDoublePtr()
            for (i in 0 until this.numWheels) {
                val wheelInfo = this.wheelInfo[i]
                val groundObject = wheelInfo.raycastInfo.groundObject

                if (groundObject != null) {
                    getWheelTransformWS(i, wheelTrans)

                    axle[i].set(
                        wheelTrans.basis.getElement(0, this.rightAxis),
                        wheelTrans.basis.getElement(1, this.rightAxis),
                        wheelTrans.basis.getElement(2, this.rightAxis)
                    )

                    val surfNormalWS = wheelInfo.raycastInfo.contactNormalWS
                    val proj = axle[i].dot(surfNormalWS)
                    tmp.setScale(proj, surfNormalWS)
                    axle[i].sub(tmp)
                    axle[i].normalize()

                    forwardWS[i].setCross(surfNormalWS, axle[i])
                    forwardWS[i].normalize()

                    ContactConstraint.resolveSingleBilateral(
                        this.rigidBody, wheelInfo.raycastInfo.contactPointWS,
                        groundObject, wheelInfo.raycastInfo.contactPointWS,
                        axle[i], impulse
                    )
                    sideImpulse.set(i, impulse[0])
                    sideImpulse.set(i, sideImpulse.get(i) * SIDE_FRICTION_STIFFNESS2)
                }
            }
            Stack.subTrans(1)
            Stack.subDoublePtr(1)
        }

        val sideFactor = 1.0
        val fwdFactor = 0.5

        var sliding = false
        for (wheel in 0 until this.numWheels) {
            val wheelInfo = this.wheelInfo[wheel]
            val groundObject = wheelInfo.raycastInfo.groundObject

            var rollingFriction = 0.0

            if (groundObject != null) {
                if (wheelInfo.engineForce != 0.0) {
                    rollingFriction = wheelInfo.engineForce * timeStep
                } else {
                    val defaultRollingFrictionImpulse = 0.0
                    val maxImpulse = if (wheelInfo.brake != 0.0) wheelInfo.brake else defaultRollingFrictionImpulse
                    val contactPt = WheelContactPoint(
                        rigidBody, groundObject, wheelInfo.raycastInfo.contactPointWS,
                        forwardWS[wheel], maxImpulse
                    )
                    rollingFriction = calcRollingFriction(contactPt, numWheelsOnGround)
                }
            }

            // switch between active rolling (throttle), braking and non-active rolling friction (no throttle/break)
            forwardImpulse.set(wheel, 0.0)
            this.wheelInfo[wheel].skidInfo = 1.0

            if (groundObject != null) {
                this.wheelInfo[wheel].skidInfo = 1.0

                val maxImpulse = wheelInfo.wheelsSuspensionForce * timeStep * wheelInfo.frictionSlip

                forwardImpulse.set(wheel, rollingFriction) //wheelInfo.m_engineForce* timeStep;

                val x = (forwardImpulse.get(wheel)) * fwdFactor
                val y = (sideImpulse.get(wheel)) * sideFactor

                val impulseSquared = (x * x + y * y)
                if (impulseSquared > maxImpulse * maxImpulse) {
                    sliding = true

                    val factor = maxImpulse / sqrt(impulseSquared)
                    this.wheelInfo[wheel].skidInfo *= factor
                }
            }
        }

        if (sliding) {
            for (wheel in 0 until this.numWheels) {
                if (sideImpulse.get(wheel) != 0.0) {
                    if (wheelInfo[wheel].skidInfo < 1.0) {
                        forwardImpulse.set(wheel, forwardImpulse.get(wheel) * wheelInfo[wheel].skidInfo)
                        sideImpulse.set(wheel, sideImpulse.get(wheel) * wheelInfo[wheel].skidInfo)
                    }
                }
            }
        }

        // apply the impulses
        run {
            val relPos = Stack.newVec()
            for (wheel in 0 until this.numWheels) {
                val wheelInfo = this.wheelInfo[wheel]
                relPos.setSub(wheelInfo.raycastInfo.contactPointWS, rigidBody.getCenterOfMassPosition(tmp))

                if (forwardImpulse.get(wheel) != 0.0) {
                    tmp.setScale(forwardImpulse.get(wheel), forwardWS[wheel])
                    rigidBody.applyImpulse(tmp, relPos)
                }
                if (sideImpulse.get(wheel) != 0.0) {
                    val groundObject = this.wheelInfo[wheel].raycastInfo.groundObject

                    val relPos2 = Stack.newVec()
                    relPos2.setSub(wheelInfo.raycastInfo.contactPointWS, groundObject!!.getCenterOfMassPosition(tmp))

                    val sideImp = Stack.newVec()
                    sideImp.setScale(sideImpulse.get(wheel), axle[wheel])

                    relPos.z *= wheelInfo.rollInfluence
                    rigidBody.applyImpulse(sideImp, relPos)

                    // apply friction impulse on the ground
                    tmp.setNegate(sideImp)
                    groundObject.applyImpulse(tmp, relPos2)

                    Stack.subVec(2)
                }
            }
            Stack.subVec(1) // relPos
        }
        Stack.subVec(1) // tmp
    }

    override fun buildJacobian() {
        // not yet
    }

    override fun solveConstraint(timeStep: Double) {
        // not yet
    }

    val numWheels: Int
        get() = wheelInfo.size

    fun setPitchControl(pitch: Double) {
        this.pitchControl = pitch
    }

    /**
     * Worldspace forward vector.
     */
    fun getForwardVector(out: Vector3d): Vector3d {
        val chassisTrans = getChassisWorldTransform(Stack.newTrans())
        out.set(
            chassisTrans.basis.getElement(0, this.forwardAxis),
            chassisTrans.basis.getElement(1, this.forwardAxis),
            chassisTrans.basis.getElement(2, this.forwardAxis)
        )
        Stack.subTrans(1)
        return out
    }

    fun setCoordinateSystem(rightIndex: Int, upIndex: Int, forwardIndex: Int) {
        this.rightAxis = rightIndex
        this.upAxis = upIndex
        this.forwardAxis = forwardIndex
    }

    /** ///////////////////////////////////////////////////////////////////////// */
    private class WheelContactPoint(
        var body0: RigidBody,
        var body1: RigidBody,
        frictionPosWorld: Vector3d,
        frictionDirectionWorld: Vector3d,
        maxImpulse: Double
    ) {
        val frictionPositionWorld: Vector3d = Vector3d()
        val frictionDirectionWorld: Vector3d = Vector3d()
        var jacDiagABInv: Double
        var maxImpulse: Double

        init {
            this.frictionPositionWorld.set(frictionPosWorld)
            this.frictionDirectionWorld.set(frictionDirectionWorld)
            this.maxImpulse = maxImpulse

            val denom0 = body0.computeImpulseDenominator(frictionPosWorld, frictionDirectionWorld)
            val denom1 = body1.computeImpulseDenominator(frictionPosWorld, frictionDirectionWorld)
            val relaxation = 1.0
            jacDiagABInv = relaxation / (denom0 + denom1)
        }
    }

    companion object {
        private val FIXED_OBJECT = RigidBody(0.0, null, SphereShape((0.0)))
        private const val SIDE_FRICTION_STIFFNESS2 = 1.0
    }
}
