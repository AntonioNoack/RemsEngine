package com.bulletphysics.dynamics.vehicle

import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.ContactConstraint
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint
import com.bulletphysics.linearmath.MiscUtil.resize
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.util.DoubleArrayList
import cz.advel.stack.Stack
import org.joml.Vector3d
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

    val wheels = ArrayList<WheelInfo>()

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
        suspensionRestLength: Double, wheelRadius: Double, tuning: VehicleTuning
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
        ci.maxSuspensionTravel = tuning.maxSuspensionTravel

        wheels.add(WheelInfo(ci))

        val wheel = wheels.last()

        updateWheelTransformsWS(wheel)
        updateWheelTransform(numWheels - 1)
        return wheel
    }

    fun getWheelTransformWS(wheelIndex: Int, out: Transform): Transform {
        assert(wheelIndex < numWheels)
        val wheel = wheels[wheelIndex]
        out.set(wheel.worldTransform)
        return out
    }

    fun updateWheelTransform(wheelIndex: Int) {
        val wheel = wheels[wheelIndex]
        updateWheelTransformsWS(wheel)
        val up = Stack.newVec()
        wheel.raycastInfo.wheelDirectionWS.negate(up)
        val right = wheel.raycastInfo.wheelAxleWS
        val fwd = Stack.newVec()
        up.cross(right, fwd)
        fwd.normalize()

        // rotate around steering over de wheelAxleWS
        val steering = wheel.steering

        val steeringOrn = Stack.newQuat().setAngleAxis(steering, up)
        val steeringMat = Stack.newMat().set(steeringOrn)

        val rotatingOrn = Stack.newQuat().setAngleAxis(-wheel.rotation, right)
        val rotatingMat = Stack.newMat().set(rotatingOrn)

        val basis2 = Stack.newMat()
        basis2.setRow(0, right.x, fwd.x, up.x)
        basis2.setRow(1, right.y, fwd.y, up.y)
        basis2.setRow(2, right.z, fwd.z, up.z)

        val wheelBasis = wheel.worldTransform.basis
        steeringMat.mul(rotatingMat, wheelBasis)
        wheelBasis.mul(basis2)

        wheel.raycastInfo.wheelDirectionWS.mulAdd(
            wheel.raycastInfo.suspensionLength,
            wheel.raycastInfo.hardPointWS,
            wheel.worldTransform.origin
        )

        Stack.subVec(2)
        Stack.subMat(3)
        Stack.subQuat(2)
    }

    fun resetSuspension() {
        for (i in wheels.indices) {
            val wheel = wheels[i]
            wheel.raycastInfo.suspensionLength = wheel.suspensionRestLength
            wheel.suspensionRelativeVelocity = 0.0

            wheel.raycastInfo.wheelDirectionWS.negate(wheel.raycastInfo.contactNormalWS)
            wheel.clippedInvContactDotSuspension = 1.0
        }
    }

    fun updateWheelTransformsWS(wheel: WheelInfo) {
        wheel.raycastInfo.isInContact = false

        val chassisTrans = rigidBody.worldTransform
        wheel.raycastInfo.hardPointWS.set(wheel.chassisConnectionPointCS)
        chassisTrans.transformPosition(wheel.raycastInfo.hardPointWS)

        wheel.raycastInfo.wheelDirectionWS.set(wheel.wheelDirectionCS)
        chassisTrans.transformDirection(wheel.raycastInfo.wheelDirectionWS)

        wheel.raycastInfo.wheelAxleWS.set(wheel.wheelAxleCS)
        chassisTrans.transformDirection(wheel.raycastInfo.wheelAxleWS)
    }

    fun rayCast(wheel: WheelInfo): Double {
        updateWheelTransformsWS(wheel)

        var depth = -1.0

        val rayLength = wheel.suspensionRestLength + wheel.wheelRadius

        val rayVector = Stack.newVec()
        wheel.raycastInfo.wheelDirectionWS.mul(rayLength, rayVector)
        val source = wheel.raycastInfo.hardPointWS
        source.add(rayVector, wheel.raycastInfo.contactPointWS)
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
            val minSuspensionLength = wheel.suspensionRestLength - wheel.maxSuspensionTravel
            val maxSuspensionLength = wheel.suspensionRestLength + wheel.maxSuspensionTravel
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
            wheel.raycastInfo.contactPointWS.sub(
                rigidBody.worldTransform.origin,
                relativePosition
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
            Stack.subVec(3)
        } else {
            // put wheel info as in rest position
            wheel.raycastInfo.suspensionLength = wheel.suspensionRestLength
            wheel.suspensionRelativeVelocity = 0.0
            wheel.raycastInfo.wheelDirectionWS.negate(wheel.raycastInfo.contactNormalWS)
            wheel.clippedInvContactDotSuspension = 1.0
            Stack.subVec(1)
        }

        // todo stack cleanup

        return depth
    }

    fun getChassisWorldTransform(out: Transform): Transform {
        return rigidBody.getCenterOfMassTransform(out)
    }

    fun updateVehicle(step: Double) {
        for (i in wheels.indices) {
            updateWheelTransform(i)
        }

        currentSpeedKmHour = 3.6f * rigidBody.linearVelocity.length()

        val forwardW = Stack.newVec()
        val chassisTrans = rigidBody.worldTransform
        forwardW.set(
            chassisTrans.basis[forwardAxis, 0],
            chassisTrans.basis[forwardAxis, 1],
            chassisTrans.basis[forwardAxis, 2]
        )

        if (forwardW.dot(rigidBody.linearVelocity) < 0.0) {
            currentSpeedKmHour *= -1.0
        }

        //
        // simulate suspension
        //
        for (i in wheels.indices) {
            rayCast(wheels[i])
        }

        updateSuspension(step)

        val tmp = Stack.newVec()

        for (i in wheels.indices) {
            // apply suspension force
            val wheel = wheels[i]

            var suspensionForce = wheel.wheelsSuspensionForce

            val gMaxSuspensionForce = 6000.0
            if (suspensionForce > gMaxSuspensionForce) {
                suspensionForce = gMaxSuspensionForce
            }
            val impulse = Stack.newVec()
            wheel.raycastInfo.contactNormalWS.mul(suspensionForce * step, impulse)
            val relPos = Stack.newVec()
            wheel.raycastInfo.contactPointWS.sub(rigidBody.worldTransform.origin, relPos)

            this.rigidBody.applyImpulse(impulse, relPos)
            Stack.subVec(2)
        }

        updateFriction(step)

        val relPos = Stack.newVec()
        val vel = Stack.newVec()
        for (i in wheels.indices) {
            val wheel = wheels[i]
            wheel.raycastInfo.hardPointWS.sub(rigidBody.worldTransform.origin, relPos)
            this.rigidBody.getVelocityInLocalPoint(relPos, vel)

            if (wheel.raycastInfo.isInContact) {
                val chassisWorldTransform = rigidBody.worldTransform
                val fwd = Stack.newVec()
                fwd.set(
                    chassisWorldTransform.basis[forwardAxis, 0],
                    chassisWorldTransform.basis[forwardAxis, 1],
                    chassisWorldTransform.basis[forwardAxis, 2]
                )

                val proj = fwd.dot(wheel.raycastInfo.contactNormalWS)
                wheel.raycastInfo.contactNormalWS.mul(proj, tmp)
                fwd.sub(tmp)

                val proj2 = fwd.dot(vel)

                wheel.deltaRotation = (proj2 * step) / (wheel.wheelRadius)
                Stack.subVec(1)
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
        return wheels[index]
    }

    fun setBrake(brake: Double, wheelIndex: Int) {
        assert((wheelIndex >= 0) && (wheelIndex < this.numWheels))
        getWheelInfo(wheelIndex).brake = brake
    }

    fun updateSuspension(deltaTime: Double) {
        val chassisMass = 1.0 / rigidBody.inverseMass

        for (wheelIndex in wheels.indices) {
            val wheelInfo = this.wheels[wheelIndex]

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
        val contactPosWorld = contactPoint.frictionPositionWorld

        val relPos1 = Stack.newVec()
        contactPosWorld.sub(contactPoint.body0.worldTransform.origin, relPos1)
        val relPos2 = Stack.newVec()
        contactPosWorld.sub(contactPoint.body1.worldTransform.origin, relPos2)

        val maxImpulse = contactPoint.maxImpulse

        val vel1 = contactPoint.body0.getVelocityInLocalPoint(relPos1, Stack.newVec())
        val vel2 = contactPoint.body1.getVelocityInLocalPoint(relPos2, Stack.newVec())
        val vel = vel1.sub(vel2)

        val relativeVelocity = contactPoint.frictionDirectionWorld.dot(vel)

        // calculate j that moves us to zero relative velocity
        var impulse = -relativeVelocity * contactPoint.jacDiagABInv / numWheelsOnGround
        impulse = min(impulse, maxImpulse)
        impulse = max(impulse, -maxImpulse)

        Stack.subVec(4)

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
        for (i in wheels.indices) {
            val wheelInfo = this.wheels[i]
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
            for (i in wheels.indices) {
                val wheelInfo = wheels[i]
                val groundObject = wheelInfo.raycastInfo.groundObject

                if (groundObject != null) {
                    getWheelTransformWS(i, wheelTrans)

                    wheelTrans.basis.getColumn(rightAxis,axle[i])

                    val surfNormalWS = wheelInfo.raycastInfo.contactNormalWS
                    val proj = axle[i].dot(surfNormalWS)
                    surfNormalWS.mul(proj, tmp)
                    axle[i].sub(tmp)
                    axle[i].normalize()

                    surfNormalWS.cross(axle[i], forwardWS[i])
                    forwardWS[i].normalize()

                    ContactConstraint.resolveSingleBilateral(
                        rigidBody, wheelInfo.raycastInfo.contactPointWS,
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
        for (wheel in wheels.indices) {
            val wheelI = wheels[wheel]
            val groundObject = wheelI.raycastInfo.groundObject

            var rollingFriction = 0.0

            if (groundObject != null) {
                if (wheelI.engineForce != 0.0) {
                    rollingFriction = wheelI.engineForce * timeStep
                } else {
                    val defaultRollingFrictionImpulse = 0.0
                    val maxImpulse = if (wheelI.brake != 0.0) wheelI.brake else defaultRollingFrictionImpulse
                    val contactPt = WheelContactPoint(
                        rigidBody, groundObject, wheelI.raycastInfo.contactPointWS,
                        forwardWS[wheel], maxImpulse
                    )
                    rollingFriction = calcRollingFriction(contactPt, numWheelsOnGround)
                }
            }

            // switch between active rolling (throttle), braking and non-active rolling friction (no throttle/break)
            forwardImpulse.set(wheel, 0.0)
            wheels[wheel].skidInfo = 1.0

            if (groundObject != null) {
                wheels[wheel].skidInfo = 1.0

                val maxImpulse = wheelI.wheelsSuspensionForce * timeStep * wheelI.frictionSlip

                forwardImpulse.set(wheel, rollingFriction) //wheelInfo.m_engineForce* timeStep;

                val x = (forwardImpulse.get(wheel)) * fwdFactor
                val y = (sideImpulse.get(wheel)) * sideFactor

                val impulseSquared = (x * x + y * y)
                if (impulseSquared > maxImpulse * maxImpulse) {
                    sliding = true

                    val factor = maxImpulse / sqrt(impulseSquared)
                    wheels[wheel].skidInfo *= factor
                }
            }
        }

        if (sliding) {
            for (wheel in wheels.indices) {
                if (sideImpulse.get(wheel) != 0.0) {
                    if (wheels[wheel].skidInfo < 1.0) {
                        forwardImpulse.set(wheel, forwardImpulse.get(wheel) * wheels[wheel].skidInfo)
                        sideImpulse.set(wheel, sideImpulse.get(wheel) * wheels[wheel].skidInfo)
                    }
                }
            }
        }

        // apply the impulses
        run {
            val relPos = Stack.newVec()
            for (wheel in wheels.indices) {
                val wheelInfo = wheels[wheel]
                wheelInfo.raycastInfo.contactPointWS.sub(rigidBody.worldTransform.origin, relPos)

                if (forwardImpulse.get(wheel) != 0.0) {
                    forwardWS[wheel].mul(forwardImpulse.get(wheel), tmp)
                    rigidBody.applyImpulse(tmp, relPos)
                }
                if (sideImpulse.get(wheel) != 0.0) {
                    val groundObject = wheels[wheel].raycastInfo.groundObject!!

                    val relPos2 = Stack.newVec()
                    wheelInfo.raycastInfo.contactPointWS.sub(groundObject.worldTransform.origin, relPos2)

                    val sideImp = Stack.newVec()
                    axle[wheel].mul(sideImpulse.get(wheel), sideImp)

                    relPos.z *= wheelInfo.rollInfluence
                    rigidBody.applyImpulse(sideImp, relPos)

                    // apply friction impulse on the ground
                    sideImp.negate(tmp)
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
        get() = wheels.size

    fun setPitchControl(pitch: Double) {
        this.pitchControl = pitch
    }

    /**
     * World space forward vector.
     */
    fun getForwardVector(out: Vector3d): Vector3d {
        val chassisTrans = rigidBody.worldTransform
        chassisTrans.basis.getColumn(forwardAxis, out)
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
        private val FIXED_OBJECT = RigidBody(0.0, SphereShape((0.0)))
        private const val SIDE_FRICTION_STIFFNESS2 = 1.0
    }
}
