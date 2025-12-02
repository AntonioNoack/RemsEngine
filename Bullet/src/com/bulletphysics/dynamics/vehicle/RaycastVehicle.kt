package com.bulletphysics.dynamics.vehicle

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.ContactConstraint
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint
import cz.advel.stack.Stack
import me.anno.bullet.bodies.VehicleWheel
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.posMod
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
class RaycastVehicle(val rigidBody: RigidBody, private val vehicleRaycaster: VehicleRaycaster) :
    TypedConstraint() { // why is this a constraint???

    // not used, I think
    override var breakingImpulse: Float
        get() = Float.POSITIVE_INFINITY
        set(value) {}

    /**
     * Velocity of vehicle (positive if velocity vector has same direction as foward vector).
     */
    var currentSpeedKmHour = 0f
        private set

    var rightAxis: Int = 0
        private set

    var upAxis: Int = 1
        private set

    var forwardAxis: Int = 2
        private set

    val wheels = ArrayList<WheelInstance>()

    /**
     * Basically most of the code is general for 2 or 4-wheel vehicles, but some of it needs to be reviewed.
     */
    fun addWheel(
        connectionPointCS: Vector3f, wheelDirectionCS: Vector3f, wheelAxleCS: Vector3f, tuning: VehicleWheel,
        time1: Long
    ): WheelInstance {
        val wheel = WheelInstance(tuning, connectionPointCS, wheelDirectionCS, wheelAxleCS, time1)
        wheels.add(wheel)

        updateWheelTransformsWS(wheel)
        updateWheelTransform(wheels.lastIndex)
        return wheel
    }

    fun updateWheelTransform(wheelIndex: Int) {
        val wheel = wheels[wheelIndex]
        updateWheelTransformsWS(wheel)
        val up = Stack.newVec3f()
        wheel.directionWS.negate(up)
        val right = wheel.axleWS
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

        wheel.directionWS.mulAdd(wheel.suspensionLength, wheel.connectionPointWS, wheel.worldTransform.origin)

        Stack.subVec3f(2)
        Stack.subMat(3)
        Stack.subQuat(2)
    }

    fun resetSuspension() {
        for (i in wheels.indices) {
            val wheel = wheels[i]
            wheel.suspensionLength = wheel.suspensionRestLength
            wheel.suspensionRelativeVelocity = 0f

            wheel.directionWS.negate(wheel.contactNormalWS)
            wheel.clippedInvContactDotSuspension = 1f
        }
    }

    fun clampSuspension(target: Float, wheel: WheelInstance): Float {
        val center = wheel.suspensionRestLength
        val min = center - wheel.maxSuspensionTravel
        val max = center + wheel.maxSuspensionTravel
        return clamp(target, min, max)
    }

    fun updateWheelTransformsWS(wheel: WheelInstance) {
        val chassisTrans = rigidBody.worldTransform
        chassisTrans.transformPosition(wheel.connectionPointCS, wheel.connectionPointWS)
        chassisTrans.transformDirection(wheel.directionCS, wheel.directionWS)
        chassisTrans.transformDirection(wheel.axleCS, wheel.axleWS)
    }

    fun rayCast(wheel: WheelInstance): Float {
        updateWheelTransformsWS(wheel)

        val rayLength = wheel.suspensionRestLength + wheel.radius

        val rayVector = Stack.newVec3f()
        wheel.directionWS.mul(rayLength, rayVector)
        val source = wheel.connectionPointWS
        source.add(rayVector, wheel.contactPointWS)
        val target = wheel.contactPointWS

        val rayResults = VehicleRaycasterResult()
        val hitObject = vehicleRaycaster.castRay(source, target, rayResults)
        wheel.groundObject = hitObject
        wheel.isInContact = hitObject != null

        if (hitObject != null) {
            val distFraction = rayResults.distFraction
            val depth = rayLength * distFraction
            wheel.contactNormalWS.set(rayResults.hitNormalInWorld)

            // clamp on max suspension travel
            wheel.suspensionLength = clampSuspension(depth - wheel.radius, wheel)

            wheel.contactPointWS.set(rayResults.hitPointInWorld)

            val denominator = wheel.contactNormalWS.dot(wheel.directionWS)

            val chassisVelocityAtContactPoint = Stack.newVec3f()
            val relativePosition = Stack.newVec3f()
            wheel.contactPointWS.sub(rigidBody.worldTransform.origin, relativePosition)

            rigidBody.getVelocityInLocalPoint(relativePosition, chassisVelocityAtContactPoint)

            val projVel = wheel.contactNormalWS.dot(chassisVelocityAtContactPoint)
            if (denominator >= -0.1f) {
                wheel.suspensionRelativeVelocity = 0f
                wheel.clippedInvContactDotSuspension = 1f / 0.1f
            } else {
                val inv = -1f / denominator
                wheel.suspensionRelativeVelocity = projVel * inv
                wheel.clippedInvContactDotSuspension = inv
            }
            Stack.subVec3f(3)
            return depth
        } else {
            // put wheel info as in rest position
            wheel.directionWS.negate(wheel.contactNormalWS)
            wheel.clippedInvContactDotSuspension = 1f
            Stack.subVec3f(1)
            return -1f
        }
    }

    fun updateVehicle(timeStep: Float) {
        for (i in wheels.indices) {
            updateWheelTransform(i)
        }

        currentSpeedKmHour = 3.6f * rigidBody.linearVelocity.length()

        val forwardW = Stack.newVec3f()
        rigidBody.worldTransform.basis.getColumn(forwardAxis, forwardW)

        if (forwardW.dot(rigidBody.linearVelocity) < 0f) {
            currentSpeedKmHour = -currentSpeedKmHour
        }

        // simulate suspension
        for (i in wheels.indices) {
            rayCast(wheels[i])
        }

        updateSuspension(timeStep)

        val tmp = Stack.newVec3f()
        for (i in wheels.indices) {
            // apply suspension force
            val wheel = wheels[i]

            val maxSuspensionForce = 6000f
            val suspensionForce = min(wheel.suspensionForce, maxSuspensionForce)
            val impulse = Stack.newVec3f()
            val relPos = Stack.newVec3f()

            wheel.contactNormalWS.mul(suspensionForce * timeStep, impulse)
            wheel.contactPointWS.sub(rigidBody.worldTransform.origin, relPos)

            rigidBody.applyImpulse(impulse, relPos)
            Stack.subVec3f(2)
        }

        updateFriction(timeStep)

        val relPos = Stack.newVec3f()
        val vel = Stack.newVec3f()
        for (i in wheels.indices) {
            val wheel = wheels[i]
            wheel.connectionPointWS.sub(rigidBody.worldTransform.origin, relPos)
            rigidBody.getVelocityInLocalPoint(relPos, vel)

            if (wheel.isInContact) {
                val forward = Stack.newVec3f()
                rigidBody.worldTransform.basis.getColumn(forwardAxis, forward)

                val proj1 = forward.dot(wheel.contactNormalWS)
                wheel.contactNormalWS.mul(proj1, tmp)
                forward.sub(tmp)

                val proj2 = forward.dot(vel)
                wheel.deltaRotation = (proj2 * timeStep) / (wheel.radius)
                Stack.subVec3f(1)
            }

            wheel.rotation += wheel.deltaRotation
            wheel.deltaRotation *= 0.99f // damping of rotation when not in contact
        }
        Stack.subVec3f(4)
    }

    /**
     * returns length in meters
     * */
    private fun getSuspensionDelta(wheel: WheelInstance): Float {
        return wheel.suspensionRestLength - wheel.suspensionLength
    }

    private fun getSuspensionForce(wheel: WheelInstance): Float {
        return wheel.suspensionStiffness * getSuspensionDelta(wheel) * wheel.clippedInvContactDotSuspension
    }

    private fun getSuspensionDampingForce(wheel: WheelInstance): Float {
        val suspensionVelocity = wheel.suspensionRelativeVelocity
        val suspensionDamping =
            if (suspensionVelocity < 0f) wheel.suspensionDampingCompression
            else wheel.suspensionDampingRelaxation
        return suspensionDamping * suspensionVelocity
    }

    private fun updateSuspension(deltaTime: Float) {
        for (i in wheels.indices) {
            val wheel = wheels[i]
            if (wheel.isInContact) {
                // springy
                val spring = getSuspensionForce(wheel)
                val damper = getSuspensionDampingForce(wheel)
                val force = spring - damper
                wheel.suspensionForce = max(force, 0f)
            } else {
                // Apply spring & damping even when airborne
                val spring = wheel.suspensionStiffness * getSuspensionDelta(wheel)
                val damper = wheel.suspensionDampingRelaxation * wheel.suspensionRelativeVelocity
                val force = spring - damper

                // integrate suspension: acceleration = F / m  (use inverse mass)
                val acceleration = force * rigidBody.inverseMass
                wheel.suspensionRelativeVelocity += acceleration * deltaTime
                val newSuspension = wheel.suspensionLength + wheel.suspensionRelativeVelocity * deltaTime

                // clamp so wheels don’t explode
                wheel.suspensionLength = clampSuspension(newSuspension, wheel)

                // airborne wheels don’t apply force into the chassis
                wheel.suspensionForce = 0f
            }
        }
    }

    private fun calcRollingFriction(
        body0: RigidBody,
        body1: RigidBody,
        frictionPositionWorld: Vector3d,
        frictionDirectionWorld: Vector3f,
        maxImpulse: Float,
        numWheelsOnGround: Int
    ): Float {

        val jacDiagABInv = run {
            val denominator0 = body0.computeImpulseDenominator(frictionPositionWorld, frictionDirectionWorld)
            val denominator1 = body1.computeImpulseDenominator(frictionPositionWorld, frictionDirectionWorld)
            1f / (denominator0 + denominator1)
        }

        val relPos1 = Stack.newVec3f()
        val relPos2 = Stack.newVec3f()
        frictionPositionWorld.sub(body0.worldTransform.origin, relPos1)
        frictionPositionWorld.sub(body1.worldTransform.origin, relPos2)

        val vel1 = body0.getVelocityInLocalPoint(relPos1, Stack.newVec3f())
        val vel2 = body1.getVelocityInLocalPoint(relPos2, Stack.newVec3f())
        val relVel = vel1.sub(vel2)

        val relativeVelocity = frictionDirectionWorld.dot(relVel)
        Stack.subVec3f(4)

        // calculate j that moves us to zero relative velocity
        val rawImpulse = -relativeVelocity * jacDiagABInv / numWheelsOnGround
        return clamp(rawImpulse, -maxImpulse, maxImpulse)
    }

    fun updateFriction(timeStep: Float) {
        // calculate the impulse, so that the wheels don't move sidewards
        if (wheels.isEmpty()) return

        val tmp = Stack.newVec3f()

        var numWheelsOnGround = 0

        // collapse all those loops into one!
        for (i in wheels.indices) {
            val wheel = wheels[i]
            if (wheel.groundObject != null) {
                numWheelsOnGround++
            }
            wheel.sideImpulse = 0f
            wheel.forwardImpulse = 0f
        }

        for (i in wheels.indices) {
            val wheel = wheels[i]
            val groundObject = wheel.groundObject
            if (groundObject != null) {
                wheel.worldTransform.basis.getColumn(rightAxis, wheel.axle)

                val surfNormalWS = wheel.contactNormalWS
                val proj = wheel.axle.dot(surfNormalWS)
                surfNormalWS.mul(proj, tmp)
                wheel.axle.sub(tmp).normalize()

                surfNormalWS.cross(wheel.axle, wheel.forwardWS)
                wheel.forwardWS.normalize()

                val impulse = ContactConstraint.resolveSingleBilateral(
                    rigidBody, wheel.contactPointWS,
                    groundObject, wheel.contactPointWS,
                    wheel.axle
                )
                wheel.sideImpulse = impulse
            }
        }

        val sideFactor = 1.0f
        val fwdFactor = 0.5f

        var sliding = false
        for (i in wheels.indices) {
            val wheel = wheels[i]
            val groundObject = wheel.groundObject

            var rollingFriction = 0f

            if (groundObject != null) {
                if (wheel.engineForce != 0f) {
                    rollingFriction = wheel.engineForce * timeStep
                } else {
                    val defaultRollingFrictionImpulse = 0f
                    val maxImpulse = if (wheel.brakeForce != 0f) wheel.brakeForce else defaultRollingFrictionImpulse
                    rollingFriction = calcRollingFriction(
                        rigidBody, groundObject, wheel.contactPointWS,
                        wheel.forwardWS, maxImpulse, numWheelsOnGround
                    )
                }
            }

            // switch between active rolling (throttle), braking and non-active rolling friction (no throttle/break)
            wheel.forwardImpulse = 0f
            wheel.skidInfo = 1f

            if (groundObject != null) {
                wheel.skidInfo = 1f

                val maxImpulse = wheel.suspensionForce * timeStep * wheel.frictionSlip

                wheel.forwardImpulse = rollingFriction //wheelInfo.m_engineForce* timeStep;

                val x = wheel.forwardImpulse * fwdFactor
                val y = wheel.sideImpulse * sideFactor

                val impulseSquared = (x * x + y * y)
                if (impulseSquared > maxImpulse * maxImpulse) {
                    sliding = true

                    val factor = maxImpulse / sqrt(impulseSquared)
                    wheel.skidInfo *= factor
                }
            }
        }

        if (sliding) {
            for (i in wheels.indices) {
                val wheel = wheels[i]
                if (wheel.sideImpulse != 0f && wheel.skidInfo < 1f) {
                    wheel.forwardImpulse *= wheel.skidInfo
                    wheel.sideImpulse *= wheel.skidInfo
                }
            }
        }

        // apply the impulses
        run {
            val relPos = Stack.newVec3f()
            for (i in wheels.indices) {
                val wheel = wheels[i]
                wheel.contactPointWS.sub(rigidBody.worldTransform.origin, relPos)

                if (wheel.forwardImpulse != 0f) {
                    wheel.forwardWS.mul(wheel.forwardImpulse, tmp)
                    rigidBody.applyImpulse(tmp, relPos)
                }
                if (wheel.sideImpulse != 0f) {
                    val groundObject = wheels[i].groundObject ?: continue

                    val relPos2 = Stack.newVec3f()
                    wheel.contactPointWS.sub(groundObject.worldTransform.origin, relPos2)

                    val sideImp = Stack.newVec3f()
                    wheel.axle.mul(wheel.sideImpulse, sideImp)

                    relPos.z *= wheel.rollInfluence
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

    fun setCoordinateSystem(rightIndex: Int, upIndex: Int, forwardIndex: Int) {
        this.rightAxis = rightIndex
        this.upAxis = upIndex
        this.forwardAxis = forwardIndex
    }
}
