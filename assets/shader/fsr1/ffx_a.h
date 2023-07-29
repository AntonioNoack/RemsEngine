
//==============================================================================================================================
 #define AF1_AU1(x) uintBitsToFloat(uint(x))
 #define AF3_AU3(x) uintBitsToFloat(uvec3(x))
//------------------------------------------------------------------------------------------------------------------------------
 #define AU1_AF1(x) floatBitsToUint(float(x))
 #define AU3_AF3(x) floatBitsToUint(vec3(x))
//==============================================================================================================================
 vec3 AF3_x(float a){return vec3(a,a,a);}
 #define AF3_(a) AF3_x(float(a))
//------------------------------------------------------------------------------------------------------------------------------
 uint AU1_x(uint a){return uint(a);}
 uvec3 AU3_x(uint a){return uvec3(a,a,a);}
 #define AU1_(a) AU1_x(uint(a))
 #define AU3_(a) AU3_x(uint(a))
//==============================================================================================================================
 // V_MAX3_F32.
 float AMax3F1(float x,float y,float z){return max(x,max(y,z));}
 vec3 AMax3F3(vec3 x,vec3 y,vec3 z){return max(x,max(y,z));}
//------------------------------------------------------------------------------------------------------------------------------
 // V_MIN3_F32.
 float AMin3F1(float x,float y,float z){return min(x,min(y,z));}
 vec3 AMin3F3(vec3 x,vec3 y,vec3 z){return min(x,min(y,z));}
//------------------------------------------------------------------------------------------------------------------------------
 float ARcpF1(float x){return float(1.0)/x;}
//------------------------------------------------------------------------------------------------------------------------------
 float ASatF1(float x){return clamp(x,float(0.0),float(1.0));}
 vec3 ASatF3(vec3 x){return clamp(x,AF3_(0.0),AF3_(1.0));}
//------------------------------------------------------------------------------------------------------------------------------

 // Negative and positive infinity.
 #define A_INFP_F AF1_AU1(0x7f800000u)
//------------------------------------------------------------------------------------------------------------------------------
 // Single operation to return (useful to create a mask to use in lerp for branch free logic),
 //  m=NaN := 0
 //  m>=0  := 0
 //  m<0   := 1
 // Uses the following useful floating point logic,
 //  saturate(+a*(-INF)==-INF) := 0
 //  saturate( 0*(-INF)== NaN) := 0
 //  saturate(-a*(-INF)==+INF) := 1
 vec3 AGtZeroF3(vec3 m){return ASatF3(m*AF3_(A_INFP_F));}

//==============================================================================================================================
//                                                    FLOAT APPROXIMATIONS
//------------------------------------------------------------------------------------------------------------------------------
// Michal Drobot has an excellent presentation on these: "Low Level Optimizations For GCN",
//  - Idea dates back to SGI, then to Quake 3, etc.
//  - https://michaldrobot.files.wordpress.com/2014/05/gcn_alu_opt_digitaldragons2014.pdf
//     - sqrt(x)=rsqrt(x)*x
//     - rcp(x)=rsqrt(x)*rsqrt(x) for positive x
//  - https://github.com/michaldrobot/ShaderFastLibs/blob/master/ShaderFastMathLib.h
//------------------------------------------------------------------------------------------------------------------------------
// These below are from perhaps less complete searching for optimal.
// Used FP16 normal range for testing with +4096 32-bit step size for sampling error.
// So these match up well with the half approximations.
//==============================================================================================================================
 float APrxLoRcpF1(float a){return AF1_AU1((0x7ef07ebbu)-AU1_AF1(a));}
 float APrxMedRcpF1(float a){float b=AF1_AU1((0x7ef19fffu)-AU1_AF1(a));return b*(-b*a+float(2.0));}
 float APrxLoRsqF1(float a){return AF1_AU1((0x5f347d74u)-(AU1_AF1(a)>>(1u)));}
//------------------------------------------------------------------------------------------------------------------------------
 vec3 APrxMedRcpF3(vec3 a){vec3 b=AF3_AU3(AU3_(0x7ef19fffu)-AU3_AF3(a));return b*(-b*a+AF3_(2.0));}
