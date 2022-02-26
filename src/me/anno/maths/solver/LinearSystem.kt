package me.anno.maths.solver

import cern.colt.matrix.impl.DenseDoubleMatrix1D
import cern.colt.matrix.impl.SparseDoubleMatrix1D
import cern.colt.matrix.impl.SparseDoubleMatrix2D
import com.joptimizer.functions.ConvexMultivariateRealFunction
import com.joptimizer.functions.LinearMultivariateRealFunction
import com.joptimizer.functions.PSDQuadraticMultivariateRealFunction
import com.joptimizer.optimizers.OptimizationRequest
import com.joptimizer.optimizers.PrimalDualMethod
import me.anno.utils.structures.arrays.ExpandingDoubleArray
import java.lang.RuntimeException

class LinearSystem(val numVariables: Int) {

    class Variable(val dim: Int, val startIndex: Int)

    // todo make this as comfortable as possible...

    val variables = HashMap<Any?, Variable>()
    var nextIndex = 0
    val obj1 = ExpandingDoubleArray(16)
    var obj2 = HashMap<Pair<Int, Int>, Double>()

    val inequalities = ArrayList<ConvexMultivariateRealFunction>()

    fun registerVariable(variable: Any?, dimensions: Int){
        variables.getOrPut(variable) {
            val v = Variable(dimensions, nextIndex)
            nextIndex += dimensions
            v
        }
    }

    fun indexOf(variable: Any?, dim: Int): Int {
        val variable2 = variables[variable] ?: throw RuntimeException("Variable $variable was not registered")
        if (dim !in 0 until variable2.dim) throw ArrayIndexOutOfBoundsException("Variable $variable only has ${variable2.dim} dimensions, but requested $dim")
        return variable2.startIndex + dim
    }

    /**
     * a * af < b
     * */
    fun lessThan(a: Any?, ai: Int, af: Double, b: Double): LinearSystem {
        return lessThan(indexOf(a, ai), af, b)
    }

    /**
     * a * af < b
     * */
    private fun lessThan(a: Int, af: Double, b: Double): LinearSystem {
        val row = SparseDoubleMatrix1D(numVariables)
        row[a] = af
        inequalities.add(LinearMultivariateRealFunction(row, b))
        return this
    }

    /**
     * a * af + b * bf < c
     * */
    fun lessThan(a: Any?, ai: Int, af: Double, b: Any?, bi: Int, bf: Double, c: Double): LinearSystem {
        return lessThan(indexOf(a, ai), af, indexOf(b, bi), bf, c)
    }

    /**
     * a * af + b * bf < c
     * */
    private fun lessThan(a: Int, af: Double, b: Int, bf: Double, c: Double): LinearSystem {
        val row = SparseDoubleMatrix1D(numVariables)
        row[a] = af
        row[b] = bf
        inequalities.add(LinearMultivariateRealFunction(row, c))
        return this
    }

    fun minimize(a: Any?, ai: Int, weight: Double = 1.0): LinearSystem {
        return minimize(indexOf(a, ai), weight)
    }

    private fun minimize(a: Int, weight: Double = 1.0): LinearSystem {
        obj1[a] += weight
        return this
    }

    fun minimizeL2(a: Any?, ai: Int, weight: Double = 1.0): LinearSystem {
        return minimizeL2(indexOf(a, ai), weight)
    }

    private fun minimizeL2(a: Int, weight: Double = 1.0): LinearSystem {
        val key = a to a
        obj2[key] = (obj2[key] ?: 0.0) + weight
        return this
    }

    fun solve(): DoubleArray? {
        val req = OptimizationRequest()
        req.f0 = if (obj2.isEmpty()) {
            while (obj1.size < nextIndex) obj1.add(0.0)
            val vec = obj1.toArray()
            LinearMultivariateRealFunction(vec, 0.0)
        } else {
            // todo can we ensure that it's pos-definite?
            // pd = positive definite
            // psd = positive semi-definite
            // PDQuadraticMultivariateRealFunction()
            while (obj1.size < nextIndex) obj1.add(0.0)
            val vec = DenseDoubleMatrix1D(nextIndex)
            for (index in 0 until obj1.size) {
                vec[index] = obj1[index]
            }
            val mat = SparseDoubleMatrix2D(nextIndex, nextIndex)
            for ((key, value) in obj2) {
                val (j, i) = key
                mat[j, i] = value
            }
            PSDQuadraticMultivariateRealFunction(mat, vec, 0.0)
        }
        req.fi = inequalities.toTypedArray()
        val opt = PrimalDualMethod()
        opt.setOptimizationRequest(req)
        try {
            opt.optimize()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return opt.optimizationResponse.solution
    }

}