package me.anno.graph.ui

import com.joptimizer.functions.LinearMultivariateRealFunction
import com.joptimizer.optimizers.OptimizationRequest
import com.joptimizer.optimizers.PrimalDualMethod
import org.apache.logging.log4j.LogManager

fun main() {

    val req = OptimizationRequest()
    req.f0 = LinearMultivariateRealFunction(doubleArrayOf(-1.0, -1.0), 0.0)
    req.fi = arrayOf(
        LinearMultivariateRealFunction(doubleArrayOf(-1.0, 0.0), 0.0),
        LinearMultivariateRealFunction(doubleArrayOf(0.0, -1.0), 0.0),
        LinearMultivariateRealFunction(doubleArrayOf(1.0, 0.0), -1.0),
        LinearMultivariateRealFunction(doubleArrayOf(0.0, 1.0), -1.0)
    )

    LogManager.disableLogger("com.joptimizer.solvers.BasicKKTSolver")
    LogManager.disableLogger("com.joptimizer.optimizers.BasicPhaseIPDM")
    LogManager.disableLogger("com.joptimizer.optimizers.PrimalDualMethod")
    LogManager.disableLogger("com.joptimizer.algebra.CholeskyFactorization")

    val opt = PrimalDualMethod()
    opt.setOptimizationRequest(req)

    try {
        opt.optimize()
    } catch (e: Exception) {
        e.printStackTrace()
        return
    }

    val sol = opt.optimizationResponse.solution
    println(sol.joinToString())

}