package me.anno.sdf

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * like http://mercury.sexy/hg_sdf/
 * */
object SDFCombiningFunctions {

    fun sMinCubic(a: Float, b: Float, k: Float): Float {
        if (k <= 0f) return min(a, b)
        val h = max(k - abs(a - b), 0f) / k
        val m = h * h * h * 0.5f
        val s = m * k / 3f
        return min(a, b) - s
    }

    fun sMaxCubic(a: Float, b: Float, k: Float): Float {
        if (k <= 0f) return max(a, b)
        val h = max(k - abs(a - b), 0f) / k
        val m = h * h * h * 0.5f
        val s = m * k / 3f
        return max(a, b) + s
    }

    const val hgFunctions = "" +
            // some of these types could use an additional material index for the intersection...
            // but we'll change the material system anyway
            "void pR45(inout vec2 p) {\n" +
            "   p = (p + vec2(p.y, -p.x))*sqrt(0.5);\n" +
            "}\n" +
            "float pMod1(inout float p, float size) {\n" +
            "   float halfSize = size * 0.5;\n" +
            "   float c = round(p/size);\n" +
            "   p = mod(p + halfSize, size) - halfSize;\n" +
            "   return c;\n" +
            "}\n" +
            "float unionChamfer(float a, float b, float r){\n" +
            "   return min(min(a,b),(a+b-r)*sqrt(0.5));\n" +
            "}\n" +
            "vec4 unionChamfer(vec4 a, vec4 b, float r){\n" +
            "   return vec4(unionChamfer(a.x,b.x,r),a.x+r<b.x?a.yzw:b.yzw);\n" +
            "}\n" +
            "float interChamfer(float a, float b, float r){\n" +
            "   return max(max(a,b),(a+b+r)*sqrt(0.5));\n" +
            "}\n" +
            "vec4 interChamfer(vec4 a, vec4 b, float r){\n" +
            "   return vec4(interChamfer(a.x,b.x,r),a.x>b.x?a.yzw:b.yzw);\n" +
            "}\n" +
            "float diffChamfer(float a, float b, float r){\n" +
            "   return interChamfer(a,-b,r);\n" +
            "}\n" +
            // quarter circle
            "float unionRound(float a, float b, float r){\n" +
            "   vec2 u = max(r-vec2(a,b), vec2(0.0));\n" +
            "   return max(r,min(a,b)) - length(u);\n" +
            "}\n" +
            "vec4 unionRound(vec4 a, vec4 b, float r){\n" +
            "   return vec4(unionRound(a.x,b.x,r),a.x<b.x?a.yzw:b.yzw);\n" +
            "}\n" +
            "float interRound(float a, float b, float r){\n" +
            "   vec2 u = max(r+vec2(a,b), vec2(0.0));\n" +
            "   return min(-r,max(a,b)) + length(u);" +
            "}\n" +
            "vec4 interRound(vec4 a, vec4 b, float r){\n" +
            "   return vec4(interRound(a.x,b.x,r),a.x>b.x?a.yzw:b.yzw);\n" +
            "}\n" +
            "float diffRound(float a, float b, float r){\n" +
            "   return interRound(a,-b,r);\n" +
            "}\n" +
            // n-1 circular columns at 45° angle
            "float unionColumn(float a, float b, float r, float n){\n" +
            "   if ((a < r) && (b < r)) {\n" +// todo is there a way to make this smooth over n?
            "      vec2 p = vec2(a, b);\n" +
            "      float columnRadius = r*sqrt(2.0)/((n-1.0)*2.0+sqrt(2.0));\n" +
            "      pR45(p);\n" +
            "      p.x -= sqrt(0.5)*r;\n" +
            "      p.x += columnRadius*sqrt(2.0);\n" +
            "      if (mod(n,2.0) >= 1.0) {\n" + // mmh..
            "         p.y += columnRadius;\n" +
            "      }\n" +
            // At this point, we have turned 45 degrees and moved at a point on the
            // diagonal that we want to place the columns on.
            // Now, repeat the domain along this direction and place a circle.
            "      pMod1(p.y, columnRadius*2.0);\n" +
            "      return min(length(p) - columnRadius, min(p.x, a));\n" +
            "   } else return min(a, b);\n" + // saving computations
            "}\n" +
            "vec4 unionColumn(vec4 a, vec4 b, float r, float n){\n" +
            // +r*(1.0-1.0/n)
            "   return vec4(unionColumn(a.x,b.x,r,n),a.x<b.x?a.yzw:b.yzw);\n" +
            "}\n" +
            // todo inter-column would need to have it's sign reversed...
            "float interColumn(float a, float b, float r, float n){\n" +
            "   return -unionColumn(-a,-b,r,n);\n" +
            "}\n" +
            "vec4 interColumn(vec4 a, vec4 b, float r, float n){\n" +
            "   return vec4(interColumn(a.x,b.x,r,n),a.x>b.x?a.yzw:b.yzw);\n" +
            "}\n" +
            "float unionStairs(float a, float b, float r, float n){\n" +
            "   float s = r/n;\n" +
            "   float u = b-r;\n" +
            "   return min(min(a,b), 0.5*(u+a+abs((mod(u-a+s, 2.0*s))-s)));" +
            "}\n" +
            "vec4 unionStairs(vec4 a, vec4 b, float r, float n){\n" +
            // +r*(1.0-1.0/n)
            "   return vec4(unionStairs(a.x,b.x,r,n),a.x<b.x?a.yzw:b.yzw);\n" +
            "}\n" +
            "float interStairs(float a, float b, float r, float n){\n" +
            "   return -unionStairs(-a,-b,r,n);\n" +
            "}\n" +
            "vec4 interStairs(vec4 a, vec4 b, float r, float n){\n" +
            "   return vec4(interStairs(a.x,b.x,r,n),a.x>b.x?a.yzw:b.yzw);\n" +
            "}\n" +
            "float unionSoft(float a, float b, float r){\n" +
            "   if(r <= 0.0) return min(a,b);\n" +
            "   float e = max(r-abs(a-b),0.0);\n" +
            "   return min(a,b)-e*e*0.25/r;\n" +
            "}\n" +
            "vec4 unionSoft(vec4 a, vec4 b, float r){\n" +
            "   return vec4(unionSoft(a.x,b.x,r),a.x<b.x?a.yzw:b.yzw);\n" +
            "}\n" +
            "float sdEngrave(float a, float b, float r){\n" +
            "   return max(a,(a+r-abs(b))*sqrt(0.5));\n" +
            "}\n" +
            "vec4 sdEngrave(vec4 a, vec4 b, float r){\n" +
            "   return vec4(sdEngrave(a.x,b.x,r),abs(b.x)<r?b.yzw:a.yzw);\n" +
            "}\n" +
            "float sdGroove(float a, float b, vec2 r){\n" +
            "   return max(a,min(a+r.x,r.y-abs(b)));\n" +
            "}\n" +
            "vec4 sdGroove(vec4 a, vec4 b, vec2 r){\n" +
            "   return vec4(sdGroove(a.x,b.x,r),abs(b.x)<r.y?b.yzw:a.yzw);\n" +
            "}\n" +
            "float sdTongue(float a, float b, vec2 r){\n" +
            "   return min(a,max(a-r.x,abs(b)-r.y));\n" +
            "}\n" +
            "vec4 sdTongue(vec4 a, vec4 b, vec2 r){\n" +
            "   return vec4(sdTongue(a.x,b.x,r),abs(b.x)<r.y?b.yzw:a.yzw);\n" +
            "}\n" +
            "float sdPipe(float a, float b, float r){\n" +
            "   return length(vec2(a,b))-r;\n" +
            "}\n" +
            "vec4 sdPipe(vec4 a, vec4 b, float r){\n" +
            "   return vec4(sdPipe(a.x,b.x,r),a.x<b.x?a.yzw:b.yzw);\n" +
            "}\n"

    const val smoothMinCubic = "" +
            // to do when we have material colors, use the first one as mixing parameter
            // inputs: sd.a, sd.b, k
            // outputs: sd.mix, mix factor
            "vec2 sMinCubic(float a, float b, float k){\n" +
            "    float h = max(k-abs(a-b), 0.0)/k;\n" +
            "    float m = h*h*h*0.5;\n" +
            "    float s = m*k*(1.0/3.0); \n" +
            "    return (a<b) ? vec2(a-s,m) : vec2(b-s,1.0-m);\n" +
            "}\n" +
            "vec2 sMaxCubic(float a, float b, float k){\n" +
            "    float h = max(k-abs(a-b), 0.0)/k;\n" +
            "    float m = h*h*h*0.5;\n" +
            "    float s = m*k*(1.0/3.0); \n" +
            "    return (a>b) ? vec2(a+s,m) : vec2(b+s,1.0-m);\n" +
            "}\n" +
            // inputs: sd.a, sd.b, k
            // outputs: sd.mix
            "float sMinCubic1(float a, float b, float k){\n" +
            "   if(k <= 0.0) return min(a,b);\n" +
            "   float h = max(k-abs(a-b), 0.0)/k;\n" +
            "   float m = h*h*h*0.5;\n" +
            "   float s = m*k*(1.0/3.0); \n" +
            "   return min(a,b)-s;\n" +
            "}\n" +
            "float sMaxCubic1(float a, float b, float k){\n" +
            "   if(k <= 0.0) return max(a,b);\n" +
            "   float h = max(k-abs(a-b), 0.0)/k;\n" +
            "   float m = h*h*h*0.5;\n" +
            "   float s = m*k*(1.0/3.0); \n" +
            "   return max(a,b)+s;\n" +
            "}\n" +
            // inputs: sd/m1, sd/m2, k
            // outputs: sd/m-mix
            "vec4 sMinCubic2(vec4 a, vec4 b, float k){\n" +
            "   if(k <= 0.0) return (a.x<b.x) ? a : b;\n" +
            "   float h = max(k-abs(a.x-b.x), 0.0)/k;\n" +
            "   float m = h*h*h*0.5;\n" +
            "   float s = m*k*(1.0/3.0); \n" +
            "   return (a.x<b.x) ? vec4(a.x-s,a.yzw) : vec4(b.x-s,b.yzw);\n" +
            "}\n" +
            "vec4 sMaxCubic2(vec4 a, vec4 b, float k){\n" +
            "   if(k <= 0.0) return (a.x>b.x) ? a : b;\n" +
            "   float h = max(k-abs(a.x-b.x), 0.0)/k;\n" +
            "   float m = h*h*h*0.5;\n" +
            "   float s = m*k*(1.0/3.0); \n" +
            "   return (a.x>b.x) ? vec4(a.x+s,a.yzw) : vec4(b.x+s,b.yzw);\n" +
            "}\n"
    const val sdMin = "" +
            "float sdMin3(float a, float b, float c){ return min(a,min(b,c)); }\n" +
            "float sdMin3(float a, float b, float c, float k){ return sMinCubic1(a,sMinCubic1(b,c,k),k); }\n" +
            "float sdMin(float d1, float d2){ return min(d1,d2); }\n" +
            "vec4 sdMin(vec4 d1, vec4 d2){ return d1.x < d2.x ? d1 : d2; }\n" +
            "vec4 sdMin(vec4 d1, vec4 d2, float k){ return sMinCubic2(d1,d2,k); }\n"
    const val sdMax = "" +
            "float sdMax(float d1, float d2){ return max(d1,d2); }\n" +
            "vec4 sdMax(vec4 d1, vec4 d2){ return d1.x < d2.x ? d2 : d1; }\n" +
            "float sdMax(float d1, float d2, float k){ return sMaxCubic1(d1,d2,k); }\n" +
            "vec4 sdMax(vec4 d1, vec4 d2, float k){ return sMaxCubic2(d1,d2,k); }\n"
    const val sdDiff = "" +
            "vec4 sdDiff3(vec4 d1, vec4 d2){\n" +
            "  vec4 e1 = sdMin(d1,d2);\n" +
            "  vec4 e2 = sdMax(d1,d2);\n" +
            "  return sdDiff1(e1,e2); }\n" +
            "vec4 sdDiff3(vec4 d1, vec4 d2, float k){\n" +
            "  vec4 e1 = sdMin(d1,d2,k);\n" +
            "  vec4 e2 = sdMax(d1,d2,k);\n" +
            "  return sdDiff1(e1,e2,k); }\n"
    const val sdDiff1 = "" + // max(+-)
            "float sdDiff1(float d1, float d2){ return max(d1, -d2); }\n" +
            "float sdDiff1(float d1, float d2, float k){ return sdMax(d1, -d2, k); }\n" +
            "vec4 sdDiff1(vec4 d1, vec4 d2){ return sdMax(d1, vec4(-d2.x, d2.yzw)); }\n" +
            "vec4 sdDiff1(vec4 d1, vec4 d2, float k){ return sdMax(d1, vec4(-d2.x, d2.yzw), k); }\n"
    const val sdInt = "vec4 sdInt(vec4 sum, vec4 di, float weight){\n" +
            "weight = 1.0-abs(weight);\n" +
            "if(weight < 0.0) return sum;\n" +
            "return vec4(sum.x + di.x * weight, weight >= 0.5 ? di.yzw : sum.yzw); }\n"
}