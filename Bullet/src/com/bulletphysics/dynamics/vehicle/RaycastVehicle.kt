package com.bulletphysics.dynamics.vehicle

import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.ContactConstraint
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint
import com.bulletphysics.linearmath.MiscUtil.resize
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.posMod
import me.anno.utils.structures.arrays.FloatArrayList
import org.joml.Vector3d
import org.joml.Vector3f
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
    TypedConstraint() { // why is this a constraint???

    val forwardWS = ArrayList<Vector3f>()
    val axle = ArrayList<Vector3f>()
    val forwardImpulse = FloatArrayList(8)
    val sideImpulse = FloatArrayList(8)

    // not used, I think
    override var breakingImpulse: Float
        get() = Float.POSITIVE_INFINITY
        set(value) {}

    private val tau = 0f
    private val damping = 0f
    private var pitchControl = 0f
    private var steeringValue = 0f

    /**
     * Velocity of vehicle (positive if velocity vector has same direction as foward vector).
     */
    var currentSpeedKmHour = 0f
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
        this.currentSpeedKmHour = 0f
        steeringValue = 0f
    }

    /**
     * Basically most of the code is general for 2 or 4-wheel vehicles, but some of it needs to be reviewed.
     */
    fun addWheel(
        connectionPointCS: Vector3d, wheelDirectionCS0: Vector3d, wheelAxleCS: Vector3d,
        suspensionRestLength: Float, wheelRadius: Float, tuning: VehicleTuning
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
        val up = Stack.newVec3f()
        wheel.raycastInfo.wheelDirectionWS.negate(up)
        val right = wheel.raycastInfo.wheelAxleWS
        val fwd = Stack.newVec3f()
        up.cross(right, fwd)
        fwd.normalize()

        // rotate around steering over de wheelAxleWS
        val steering = wheel.steering

        val steeringOrn = Stack.newQuat().setAngleAxis(steering, up)
        val steeringMat = Stack.newMat().set(steeringOrn)

        val wheelRotation = posMod(-wheel.rotation, TAU).toFloat()
        val rotatingOrn = Stack.newQuat().setAngleAxis(wheelRotation, right)
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

        Stack.subVec3f(2)
        Stack.subMat(3)
        Stack.subQuat(2)
    }

    fun resetSuspension() {
        for (i in wheels.indices) {
            val wheel = wheels[i]
            wheel.raycastInfo.suspensionLength = wheel.suspensionRestLength
            wheel.suspensionRelativeVelocity = 0f

            wheel.raycastInfo.wheelDirectionWS.negate(wheel.raycastInfo.contactNormalWS)
            wheel.clippedInvContactDotSuspension = 1f
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

    fun rayCast(wheel: WheelInfo): Float {
        updateWheelTransformsWS(wheel)

        var depth = -1f

        val rayLength = wheel.suspensionRestLength + wheel.wheelRadius

        val rayVector = Stack.newVec3f()
        wheel.raycastInfo.wheelDirectionWS.mul(rayLength, rayVector)
        val source = wheel.raycastInfo.hardPointWS
        source.add(rayVector, wheel.raycastInfo.contactPointWS)
        val target = wheel.raycastInfo.contactPointWS

        val param: Float
        val rayResults = VehicleRaycasterResult()
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

            val chassisVelocityAtContactPoint = Stack.newVec3f()
            val relativePosition = Stack.newVec3f()
            wheel.raycastInfo.contactPointWS.sub(
                rigidBody.worldTransform.origin,
                relativePosition
            )

            rigidBody.getVelocityInLocalPoint(relativePosition, chassisVelocityAtContactPoint)

            val projVel = wheel.raycastInfo.contactNormalWS.dot(chassisVelocityAtContactPoint)

            if (denominator >= -0.1f) {
                wheel.suspensionRelativeVelocity = 0f
                wheel.clippedInvContactDotSuspension = 1f / 0.1f
            } else {
                val inv = -1f / denominator
                wheel.suspensionRelativeVelocity = projVel * inv
                wheel.clippedInvContactDotSuspension = inv
            }
            Stack.subVec3f(3)
        } else {
            // put wheel info as in rest position
            wheel.raycastInfo.suspensionLength = wheel.suspensionRestLength
            wheel.suspensionRelativeVelocity = 0f
            wheel.raycastInfo.wheelDirectionWS.negate(wheel.raycastInfo.contactNormalWS)
            wheel.clippedInvContactDotSuspension = 1f
            Stack.subVec3f(1)
        }

        // todo stack cleanup

        return depth
    }

    fun getChassisWorldTransform(out: Transform): Transform {
        return rigidBody.getCenterOfMassTransform(out)
    }

    fun updateVehicle(timeStep: Float) {
        for (i in wheels.indices) {
            updateWheelTransform(i)
        }

        currentSpeedKmHour = 3.6f * rigidBody.linearVelocity.length()

        val forwardW = Stack.newVec3d()
        val chassisTrans = rigidBody.worldTransform
        forwardW.set(
            chassisTrans.basis[forwardAxis, 0],
            chassisTrans.basis[forwardAxis, 1],
            chassisTrans.basis[forwardAxis, 2]
        )

        if (forwardW.dot(rigidBody.linearVelocity) < 0f) {
            currentSpeedKmHour = -currentSpeedKmHour
        }

        //
        // simulate suspension
        //
        for (i in wheels.indices) {
            rayCast(wheels[i])
        }

        updateSuspension(timeStep)

        val tmp = Stack.newVec3f()

        for (i in wheels.indices) {
            // apply suspension force
            val wheel = wheels[i]

            var suspensionForce = wheel.wheelsSuspensionForce

            val gMaxSuspensionForce = 6000f
            if (suspensionForce > gMaxSuspensionForce) {
                suspensionForce = gMaxSuspensionForce
            }
            val impulse = Stack.newVec3f()
            wheel.raycastInfo.contactNormalWS.mul(suspensionForce * timeStep, impulse)
            val relPos = Stack.newVec3f()
            wheel.raycastInfo.contactPointWS.sub(rigidBody.worldTransform.origin, relPos)

            this.rigidBody.applyImpulse(impulse, relPos)
            Stack.subVec3f(2)
        }

        updateFriction(timeStep)

        val relPos = Stack.newVec3f()
        val vel = Stack.newVec3f()
        for (i in wheels.indices) {
            val wheel = wheels[i]
            wheel.raycastInfo.hardPointWS.sub(rigidBody.worldTransform.origin, relPos)
            this.rigidBody.getVelocityInLocalPoint(relPos, vel)

            if (wheel.raycastInfo.isInContact) {
                val chassisWorldTransform = rigidBody.worldTransform
                val fwd = Stack.newVec3f()
                fwd.set(
                    chassisWorldTransform.basis[forwardAxis, 0],
                    chassisWorldTransform.basis[forwardAxis, 1],
                    chassisWorldTransform.basis[forwardAxis, 2]
                )

                val proj = fwd.dot(wheel.raycastInfo.contactNormalWS)
                wheel.raycastInfo.contactNormalWS.mul(proj, tmp)
                fwd.sub(tmp)

                val proj2 = fwd.dot(vel)

                wheel.deltaRotation = (proj2 * timeStep) / (wheel.wheelRadius)
                Stack.subVec3f(1)
            }

            wheel.rotation += wheel.deltaRotation
            wheel.deltaRotation *= 0.99f // damping of rotation when not in contact
        }
        Stack.subVec3f(3)
        Stack.subVec3d(1)
    }

    fun setSteeringValue(steering: Float, wheel: Int) {
        assert(wheel >= 0 && wheel < this.numWheels)

        val wheelInfo = getWheelInfo(wheel)
        wheelInfo.steering = steering
    }

    fun getSteeringValue(wheel: Int): Float {
        return getWheelInfo(wheel).steering
    }

    fun applyEngineForce(force: Float, wheel: Int) {
        assert(wheel >= 0 && wheel < this.numWheels)
        val wheelInfo = getWheelInfo(wheel)
        wheelInfo.engineForce = force
    }

    fun getWheelInfo(index: Int): WheelInfo {
        return wheels[index]
    }

    fun setBrake(brake: Float, wheelIndex: Int) {
        assert((wheelIndex >= 0) && (wheelIndex < this.numWheels))
        getWheelInfo(wheelIndex).brake = brake
    }

    fun updateSuspension(deltaTime: Float) {
        val chassisMass = 1f / rigidBody.inverseMass

        for (wheelIndex in wheels.indices) {
            val wheelInfo = this.wheels[wheelIndex]

            if (wheelInfo.raycastInfo.isInContact) {
                var force: Float
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
                if (wheelInfo.wheelsSuspensionForce < 0f) {
                    wheelInfo.wheelsSuspensionForce = 0f
                }
            } else {
                wheelInfo.wheelsSuspensionForce = 0f
            }
        }
    }

    private fun calcRollingFriction(contactPoint: WheelContactPoint, numWheelsOnGround: Int): Float {
        val contactPosWorld = contactPoint.frictionPositionWorld

        val relPos1 = Stack.newVec3f()
        val relPos2 = Stack.newVec3f()
        contactPosWorld.sub(contactPoint.body0.worldTransform.origin, relPos1)
        contactPosWorld.sub(contactPoint.body1.worldTransform.origin, relPos2)

        val maxImpulse = contactPoint.maxImpulse

        val vel1 = contactPoint.body0.getVelocityInLocalPoint(relPos1, Stack.newVec3f())
        val vel2 = contactPoint.body1.getVelocityInLocalPoint(relPos2, Stack.newVec3f())
        val vel = vel1.sub(vel2)

        val relativeVelocity = contactPoint.frictionDirectionWorld.dot(vel)

        // calculate j that moves us to zero relative velocity
        var impulse = -relativeVelocity * contactPoint.jacDiagABInv / numWheelsOnGround
        impulse = min(impulse, maxImpulse)
        impulse = max(impulse, -maxImpulse)

        Stack.subVec3f(4)
        return impulse
    }

    fun updateFriction(timeStep: Float) {
        // calculate the impulse, so that the wheels don't move sidewards
        val numWheel = this.numWheels
        if (numWheel == 0) {
            return
        }

        resize(forwardWS, numWheel, Vector3f::class.java)
        resize(axle, numWheel, Vector3f::class.java)
        resize(forwardImpulse, numWheel, 0f)
        resize(sideImpulse, numWheel, 0f)

        val tmp = Stack.newVec3f()

        var numWheelsOnGround = 0

        // collapse all those loops into one!
        for (i in wheels.indices) {
            val wheelInfo = this.wheels[i]
            val groundObject = wheelInfo.raycastInfo.groundObject
            if (groundObject != null) {
                numWheelsOnGround++
            }
            sideImpulse[i] = 0f
            forwardImpulse[i] = 0f
        }

        run {
            val wheelTrans = Stack.newTrans()
            for (i in wheels.indices) {
                val wheelInfo = wheels[i]
                val groundObject = wheelInfo.raycastInfo.groundObject

                if (groundObject != null) {
                    getWheelTransformWS(i, wheelTrans)

                    wheelTrans.basis.getColumn(rightAxis, axle[i])

                    val surfNormalWS = wheelInfo.raycastInfo.contactNormalWS
                    val proj = axle[i].dot(surfNormalWS)
                    surfNormalWS.mul(proj, tmp)
                    axle[i].sub(tmp)
                    axle[i].normalize()

                    surfNormalWS.cross(axle[i], forwardWS[i])
                    forwardWS[i].normalize()

                    val impulse = ContactConstraint.resolveSingleBilateral(
                        rigidBody, wheelInfo.raycastInfo.contactPointWS,
                        groundObject, wheelInfo.raycastInfo.contactPointWS,
                        axle[i]
                    )
                    sideImpulse[i] = impulse * SIDE_FRICTION_STIFFNESS2
                }
            }
            Stack.subTrans(1)
        }

        val sideFactor = 1.0f
        val fwdFactor = 0.5f

        var sliding = false
        for (wheel in wheels.indices) {
            val wheelI = wheels[wheel]
            val groundObject = wheelI.raycastInfo.groundObject

            var rollingFriction = 0f

            if (groundObject != null) {
                if (wheelI.engineForce != 0f) {
                    rollingFriction = wheelI.engineForce * timeStep
                } else {
                    val defaultRollingFrictionImpulse = 0f
                    val maxImpulse = if (wheelI.brake != 0f) wheelI.brake else defaultRollingFrictionImpulse
                    val contactPt = WheelContactPoint(
                        rigidBody, groundObject, wheelI.raycastInfo.contactPointWS,
                        forwardWS[wheel], maxImpulse
                    )
                    rollingFriction = calcRollingFriction(contactPt, numWheelsOnGround)
                }
            }

            // switch between active rolling (throttle), braking and non-active rolling friction (no throttle/break)
            forwardImpulse[wheel] = 0f
            wheels[wheel].skidInfo = 1f

            if (groundObject != null) {
                wheels[wheel].skidInfo = 1f

                val maxImpulse = wheelI.wheelsSuspensionForce * timeStep * wheelI.frictionSlip

                forwardImpulse[wheel] = rollingFriction //wheelInfo.m_engineForce* timeStep;

                val x = (forwardImpulse[wheel]) * fwdFactor
                val y = (sideImpulse[wheel]) * sideFactor

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
                if (sideImpulse[wheel] != 0f) {
                    if (wheels[wheel].skidInfo < 1f) {
                        forwardImpulse[wheel] = forwardImpulse[wheel] * wheels[wheel].skidInfo
                        sideImpulse[wheel] = sideImpulse[wheel] * wheels[wheel].skidInfo
                    }
                }
            }
        }

        // apply the impulses
        run {
            val relPos = Stack.newVec3f()
            for (wheel in wheels.indices) {
                val wheelInfo = wheels[wheel]
                wheelInfo.raycastInfo.contactPointWS.sub(rigidBody.worldTransform.origin, relPos)

                if (forwardImpulse[wheel] != 0f) {
                    forwardWS[wheel].mul(forwardImpulse[wheel], tmp)
                    rigidBody.applyImpulse(tmp, relPos)
                }
                if (sideImpulse[wheel] != 0f) {
                    val groundObject = wheels[wheel].raycastInfo.groundObject!!

                    val relPos2 = Stack.newVec3f()
                    wheelInfo.raycastInfo.contactPointWS.sub(groundObject.worldTransform.origin, relPos2)

                    val sideImp = Stack.newVec3f()
                    axle[wheel].mul(sideImpulse[wheel], sideImp)

                    relPos.z *= wheelInfo.rollInfluence
                    rigidBody.applyImpulse(sideImp, relPos)

                    // apply friction impulse on the ground
                    sideImp.negate(tmp)
                    groundObject.applyImpulse(tmp, relPos2)

                    Stack.subVec3f(2)
                }
            }
            Stack.subVec3f(1) // relPos
        }
        Stack.subVec3f(1) // tmp
    }

    override fun buildJacobian() {
        // not yet
    }

    override fun solveConstraint(timeStep: Float) {
        // not yet
    }

    val numWheels: Int
        get() = wheels.size

    fun setPitchControl(pitch: Float) {
        this.pitchControl = pitch
    }

    /**
     * World space forward vector.
     */
    fun getForwardVector(out: Vector3f): Vector3f {
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
        frictionDirectionWorld: Vector3f,
        maxImpulse: Float
    ) {
        val frictionPositionWorld = Vector3d()
        val frictionDirectionWorld = Vector3f()
        var jacDiagABInv: Float
        var maxImpulse: Float

        init {
            this.frictionPositionWorld.set(frictionPosWorld)
            this.frictionDirectionWorld.set(frictionDirectionWorld)
            this.maxImpulse = maxImpulse

            val denom0 = body0.computeImpulseDenominator(frictionPosWorld, frictionDirectionWorld)
            val denom1 = body1.computeImpulseDenominator(frictionPosWorld, frictionDirectionWorld)
            jacDiagABInv = 1f / (denom0 + denom1)
        }
    }

    companion object {
        private val FIXED_OBJECT = RigidBody(0f, SphereShape(0f))
        private const val SIDE_FRICTION_STIFFNESS2 = 1f
    }
}
