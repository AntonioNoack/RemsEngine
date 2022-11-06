#ifdef FSR_EASU_F
//------------------------------------------------------------------------------------------------------------------------------
 // Filtering for a given tap for the scalar.
 void FsrEasuTapF(
 inout vec3 aC, // Accumulated color, with negative lobe.
 inout float aW, // Accumulated weight.
 vec2 off, // Pixel offset from resolve position to tap.
 vec2 dir, // Gradient direction.
 vec2 len, // Length.
 float lob, // Negative lobe strength.
 float clp, // Clipping point.
 vec3 c){ // Tap color.
  // Rotate offset by direction.
  vec2 v;
  v.x=(off.x*( dir.x))+(off.y*dir.y);
  v.y=(off.x*(-dir.y))+(off.y*dir.x);
  // Anisotropy.
  v*=len;
  // Compute distance^2.
  float d2=v.x*v.x+v.y*v.y;
  // Limit to the window as at corner, 2 taps can easily be outside.
  d2=min(d2,clp);
  // Approximation of lanczos2 without sin() or rcp(), or sqrt() to get x.
  //  (25/16 * (2/5 * x^2 - 1)^2 - (25/16 - 1)) * (1/4 * x^2 - 1)^2
  //  |_______________________________________|   |_______________|
  //                   base                             window
  // The general form of the 'base' is,
  //  (a*(b*x^2-1)^2-(a-1))
  // Where 'a=1/(2*b-b^2)' and 'b' moves around the negative lobe.
  float wB=float(2.0/5.0)*d2+float(-1.0);
  float wA=lob*d2+float(-1.0);
  wB*=wB;
  wA*=wA;
  wB=float(25.0/16.0)*wB+float(-(25.0/16.0-1.0));
  float w=wB*wA;
  // Do weighted average.
  aC+=c*w;aW+=w;}
//------------------------------------------------------------------------------------------------------------------------------
 // Accumulate direction and length.
 void FsrEasuSetF(
 inout vec2 dir,
 inout float len,
 vec2 pp,
 bool biS,bool biT,bool biU,bool biV,
 float lA,float lB,float lC,float lD,float lE){
  // Compute bilinear weight, branches factor out as predicates are compiler time immediates.
  //  s t
  //  u v
  float w = 0.0;
  if(biS)w=(1.0-pp.x)*(1.0-pp.y);
  if(biT)w=           pp.x *(1.0-pp.y);
  if(biU)w=(1.0-pp.x)*           pp.y ;
  if(biV)w=           pp.x *           pp.y ;
  // Direction is the '+' diff.
  //    a
  //  b c d
  //    e
  // Then takes magnitude from abs average of both sides of 'c'.
  // Length converts gradient reversal to 0, smoothly to non-reversal at 1, shaped, then adding horz and vert terms.
  float dc=lD-lC;
  float cb=lC-lB;
  float lenX=max(abs(dc),abs(cb));
  lenX=APrxLoRcpF1(lenX);
  float dirX=lD-lB;
  dir.x+=dirX*w;
  lenX=ASatF1(abs(dirX)*lenX);
  lenX*=lenX;
  len+=lenX*w;
  // Repeat for the y axis.
  float ec=lE-lC;
  float ca=lC-lA;
  float lenY=max(abs(ec),abs(ca));
  lenY=APrxLoRcpF1(lenY);
  float dirY=lE-lA;
  dir.y+=dirY*w;
  lenY=ASatF1(abs(dirY)*lenY);
  lenY*=lenY;
  len+=lenY*w;}
//------------------------------------------------------------------------------------------------------------------------------

 void FsrEasuF(
 out vec3 pix,
 vec2 ip, // Integer pixel position in output.
 vec4 con0, // Constants generated by FsrEasuCon().
 vec4 con1,
 vec4 con2,
 vec4 con3){
//------------------------------------------------------------------------------------------------------------------------------
  // Get position of 'f'.
  vec2 pp=ip*con0.xy+con0.zw;
  vec2 fp=floor(pp);
  pp-=fp;// now it's fract
//------------------------------------------------------------------------------------------------------------------------------
  // 12-tap kernel.
  //    b c
  //  e f g h
  //  i j k l
  //    n o
  // Gather 4 ordering.
  //  a b
  //  r g
  // For packed FP16, need either {rg} or {ab} so using the following setup for gather in all versions,
  //    a b    <- unused (z)
  //    r g
  //  a b a b
  //  r g r g
  //    a b
  //    r g    <- unused (z)
  // Allowing dead-code removal to remove the 'z's.
  vec2 p0=fp*(con1.xy)+(con1.zw);
  // These are from p0 to avoid pulling two constants on pre-Navi hardware.
  vec2 p1=p0+(con2.xy);
  vec2 p2=p0+(con2.zw);
  vec2 p3=p0+(con3.xy);
  #ifdef NO_GATHER
  vec2 dx = vec2(con1.x,0.0);
  vec2 dy = vec2(0.0,con1.y);
  vec2 dxy = con1.xy;
  vec3 b0 = FsrEasuRGBF(p0);
  vec3 b1 = FsrEasuRGBF(p0+dx);
  vec4 bczzR=vec4(b0.r,b1.r,0.0,0.0);
  vec4 bczzG=vec4(b0.g,b1.g,0.0,0.0);
  vec4 bczzB=vec4(b0.b,b1.b,0.0,0.0);
  vec3 i0 = FsrEasuRGBF(p1);
  vec3 i1 = FsrEasuRGBF(p1+dx);
  vec3 i2 = FsrEasuRGBF(p1+dy);
  vec3 i3 = FsrEasuRGBF(p1+dxy);
  vec4 ijfeR=vec4(i0.r,i1.r,i2.r,i3.r);
  vec4 ijfeG=vec4(i0.g,i1.g,i2.g,i3.g);
  vec4 ijfeB=vec4(i0.b,i1.b,i2.b,i3.b);
  vec3 k0 = FsrEasuRGBF(p2);
  vec3 k1 = FsrEasuRGBF(p2+dx);
  vec3 k2 = FsrEasuRGBF(p2+dy);
  vec3 k3 = FsrEasuRGBF(p2+dxy);
  vec4 klhgR=vec4(k0.r,k1.r,k2.r,k3.r);
  vec4 klhgG=vec4(k0.g,k1.g,k2.g,k3.g);
  vec4 klhgB=vec4(k0.b,k1.b,k2.b,k3.b);
  vec3 z2 = FsrEasuRGBF(p3+dy);
  vec3 z3 = FsrEasuRGBF(p3+dxy);
  vec4 zzonR=vec4(0.0,0.0,z2.r,z3.r);
  vec4 zzonG=vec4(0.0,0.0,z2.g,z3.g);
  vec4 zzonB=vec4(0.0,0.0,z2.b,z3.b);
  #else
  vec4 alpha0 = FsrEasuAF(p0);
  vec4 bczzR=FsrEasuRF(p0,alpha0);
  vec4 bczzG=FsrEasuGF(p0,alpha0);
  vec4 bczzB=FsrEasuBF(p0,alpha0);
  vec4 alpha1 = FsrEasuAF(p1);
  vec4 ijfeR=FsrEasuRF(p1,alpha1);
  vec4 ijfeG=FsrEasuGF(p1,alpha1);
  vec4 ijfeB=FsrEasuBF(p1,alpha1);
  vec4 alpha2 = FsrEasuAF(p2);
  vec4 klhgR=FsrEasuRF(p2,alpha2);
  vec4 klhgG=FsrEasuGF(p2,alpha2);
  vec4 klhgB=FsrEasuBF(p2,alpha2);
  vec4 alpha3 = FsrEasuAF(p3);
  vec4 zzonR=FsrEasuRF(p3,alpha3);
  vec4 zzonG=FsrEasuGF(p3,alpha3);
  vec4 zzonB=FsrEasuBF(p3,alpha3);
  #endif
//------------------------------------------------------------------------------------------------------------------------------
  // Simplest multi-channel approximate luma possible (luma times 2, in 2 FMA/MAD).
  vec4 bczzL=bczzB*0.5+(bczzR*0.5+bczzG);
  vec4 ijfeL=ijfeB*0.5+(ijfeR*0.5+ijfeG);
  vec4 klhgL=klhgB*0.5+(klhgR*0.5+klhgG);
  vec4 zzonL=zzonB*0.5+(zzonR*0.5+zzonG);
  // Rename.
  float bL=bczzL.x;
  float cL=bczzL.y;
  float iL=ijfeL.x;
  float jL=ijfeL.y;
  float fL=ijfeL.z;
  float eL=ijfeL.w;
  float kL=klhgL.x;
  float lL=klhgL.y;
  float hL=klhgL.z;
  float gL=klhgL.w;
  float oL=zzonL.z;
  float nL=zzonL.w;
  // Accumulate for bilinear interpolation.
  vec2 dir=vec2(0.0);
  float len=0.0;
  FsrEasuSetF(dir,len,pp,true, false,false,false,bL,eL,fL,gL,jL);
  FsrEasuSetF(dir,len,pp,false,true ,false,false,cL,fL,gL,hL,kL);
  FsrEasuSetF(dir,len,pp,false,false,true ,false,fL,iL,jL,kL,nL);
  FsrEasuSetF(dir,len,pp,false,false,false,true ,gL,jL,kL,lL,oL);
//------------------------------------------------------------------------------------------------------------------------------
  // Normalize with approximation, and cleanup close to zero.
  vec2 dir2=dir*dir;
  float dirR=dir2.x+dir2.y;
  bool zro=dirR<float(1.0/32768.0);
  dirR=APrxLoRsqF1(dirR);
  dirR=zro?1.0:dirR;
  dir.x=zro?1.0:dir.x;
  dir*=dirR;
  // Transform from {0 to 2} to {0 to 1} range, and shape with square.
  len=len*0.5;
  len*=len;
  // Stretch kernel {1.0 vert|horz, to sqrt(2.0) on diagonal}.
  float stretch=(dir.x*dir.x+dir.y*dir.y)*APrxLoRcpF1(max(abs(dir.x),abs(dir.y)));
  // Anisotropic length after rotation,
  //  x := 1.0 lerp to 'stretch' on edges
  //  y := 1.0 lerp to 2x on edges
  vec2 len2=vec2(1.0+(stretch-1.0)*len,1.0+float(-0.5)*len);
  // Based on the amount of 'edge',
  // the window shifts from +/-{sqrt(2.0) to slightly beyond 2.0}.
  float lob=0.5+float((1.0/4.0-0.04)-0.5)*len;
  // Set distance^2 clipping point to the end of the adjustable window.
  float clp=APrxLoRcpF1(lob);
//------------------------------------------------------------------------------------------------------------------------------
  // Accumulation mixed with min/max of 4 nearest.
  //    b c
  //  e f g h
  //  i j k l
  //    n o
  vec3 min4=min(AMin3F3(vec3(ijfeR.z,ijfeG.z,ijfeB.z),vec3(klhgR.w,klhgG.w,klhgB.w),vec3(ijfeR.y,ijfeG.y,ijfeB.y)),
               vec3(klhgR.x,klhgG.x,klhgB.x));
  vec3 max4=max(AMax3F3(vec3(ijfeR.z,ijfeG.z,ijfeB.z),vec3(klhgR.w,klhgG.w,klhgB.w),vec3(ijfeR.y,ijfeG.y,ijfeB.y)),
               vec3(klhgR.x,klhgG.x,klhgB.x));
  // Accumulation.
  vec3 aC=vec3(0.0);
  float aW=0.0;
  FsrEasuTapF(aC,aW,vec2( 0.0,-1.0)-pp,dir,len2,lob,clp,vec3(bczzR.x,bczzG.x,bczzB.x)); // b
  FsrEasuTapF(aC,aW,vec2( 1.0,-1.0)-pp,dir,len2,lob,clp,vec3(bczzR.y,bczzG.y,bczzB.y)); // c
  FsrEasuTapF(aC,aW,vec2(-1.0, 1.0)-pp,dir,len2,lob,clp,vec3(ijfeR.x,ijfeG.x,ijfeB.x)); // i
  FsrEasuTapF(aC,aW,vec2( 0.0, 1.0)-pp,dir,len2,lob,clp,vec3(ijfeR.y,ijfeG.y,ijfeB.y)); // j
  FsrEasuTapF(aC,aW,vec2( 0.0, 0.0)-pp,dir,len2,lob,clp,vec3(ijfeR.z,ijfeG.z,ijfeB.z)); // f
  FsrEasuTapF(aC,aW,vec2(-1.0, 0.0)-pp,dir,len2,lob,clp,vec3(ijfeR.w,ijfeG.w,ijfeB.w)); // e
  FsrEasuTapF(aC,aW,vec2( 1.0, 1.0)-pp,dir,len2,lob,clp,vec3(klhgR.x,klhgG.x,klhgB.x)); // k
  FsrEasuTapF(aC,aW,vec2( 2.0, 1.0)-pp,dir,len2,lob,clp,vec3(klhgR.y,klhgG.y,klhgB.y)); // l
  FsrEasuTapF(aC,aW,vec2( 2.0, 0.0)-pp,dir,len2,lob,clp,vec3(klhgR.z,klhgG.z,klhgB.z)); // h
  FsrEasuTapF(aC,aW,vec2( 1.0, 0.0)-pp,dir,len2,lob,clp,vec3(klhgR.w,klhgG.w,klhgB.w)); // g
  FsrEasuTapF(aC,aW,vec2( 1.0, 2.0)-pp,dir,len2,lob,clp,vec3(zzonR.z,zzonG.z,zzonB.z)); // o
  FsrEasuTapF(aC,aW,vec2( 0.0, 2.0)-pp,dir,len2,lob,clp,vec3(zzonR.w,zzonG.w,zzonB.w)); // n
//------------------------------------------------------------------------------------------------------------------------------
  // Normalize and dering.
  pix=min(max4,max(min4,aC*(ARcpF1(aW))));
}
#endif // FSR_EASU_F

#define FSR_RCAS_LIMIT (0.25-(1.0/16.0))

//==============================================================================================================================
//                                                   NON-PACKED 32-BIT VERSION
//==============================================================================================================================
#ifdef FSR_RCAS_F
 // Input callback prototypes that need to be implemented by calling shader
vec4 FsrRcasLoadF(ivec2 p);
 void FsrRcasInputF(inout float r,inout float g,inout float b);
//------------------------------------------------------------------------------------------------------------------------------
 void FsrRcasF(
 out float pixR, // Output values, non-vector so port between RcasFilter() and RcasFilterH() is easy.
 out float pixG,
 out float pixB,
 #ifdef FSR_RCAS_PASSTHROUGH_ALPHA
  out float pixA,
 #endif
 ivec2 sp, // Integer pixel position in output.
 float con // Constant generated by RcasSetup().
){
  // Algorithm uses minimal 3x3 pixel neighborhood.
  //    b 
  //  d e f
  //    h
  vec3 b=FsrRcasLoadF(sp+ivec2( 0,-1)).rgb;
  vec3 d=FsrRcasLoadF(sp+ivec2(-1, 0)).rgb;
  #ifdef FSR_RCAS_PASSTHROUGH_ALPHA
   vec4 ee=FsrRcasLoadF(sp);
   vec3 e=ee.rgb;pixA=ee.a;
  #else
   vec3 e=FsrRcasLoadF(sp).rgb;
  #endif
  vec3 f=FsrRcasLoadF(sp+ivec2( 1, 0)).rgb;
  vec3 h=FsrRcasLoadF(sp+ivec2( 0, 1)).rgb;
  // Rename (32-bit) or regroup (16-bit).
  float bR=b.r;
  float bG=b.g;
  float bB=b.b;
  float dR=d.r;
  float dG=d.g;
  float dB=d.b;
  float eR=e.r;
  float eG=e.g;
  float eB=e.b;
  float fR=f.r;
  float fG=f.g;
  float fB=f.b;
  float hR=h.r;
  float hG=h.g;
  float hB=h.b;
  // Run optional input transform.
  FsrRcasInputF(bR,bG,bB);
  FsrRcasInputF(dR,dG,dB);
  FsrRcasInputF(eR,eG,eB);
  FsrRcasInputF(fR,fG,fB);
  FsrRcasInputF(hR,hG,hB);
  // Luma times 2.
  float bL=bB*0.5+(bR*0.5+bG);
  float dL=dB*0.5+(dR*0.5+dG);
  float eL=eB*0.5+(eR*0.5+eG);
  float fL=fB*0.5+(fR*0.5+fG);
  float hL=hB*0.5+(hR*0.5+hG);
  // Noise detection.
  float nz=0.25*bL+0.25*dL+0.25*fL+0.25*hL-eL;
  nz=ASatF1(abs(nz)*APrxMedRcpF1(AMax3F1(AMax3F1(bL,dL,eL),fL,hL)-AMin3F1(AMin3F1(bL,dL,eL),fL,hL)));
  nz=float(-0.5)*nz+1.0;
  // Min and max of ring.
  float mn4R=min(AMin3F1(bR,dR,fR),hR);
  float mn4G=min(AMin3F1(bG,dG,fG),hG);
  float mn4B=min(AMin3F1(bB,dB,fB),hB);
  float mx4R=max(AMax3F1(bR,dR,fR),hR);
  float mx4G=max(AMax3F1(bG,dG,fG),hG);
  float mx4B=max(AMax3F1(bB,dB,fB),hB);
  // Immediate constants for peak range.
  vec2 peakC=vec2(1.0,-1.0*4.0);
  // Limiters, these need to be high precision RCPs.
  float hitMinR=min(mn4R,eR)*ARcpF1(4.0*mx4R);
  float hitMinG=min(mn4G,eG)*ARcpF1(4.0*mx4G);
  float hitMinB=min(mn4B,eB)*ARcpF1(4.0*mx4B);
  float hitMaxR=(peakC.x-max(mx4R,eR))*ARcpF1(4.0*mn4R+peakC.y);
  float hitMaxG=(peakC.x-max(mx4G,eG))*ARcpF1(4.0*mn4G+peakC.y);
  float hitMaxB=(peakC.x-max(mx4B,eB))*ARcpF1(4.0*mn4B+peakC.y);
  float lobeR=max(-hitMinR,hitMaxR);
  float lobeG=max(-hitMinG,hitMaxG);
  float lobeB=max(-hitMinB,hitMaxB);
  float lobe=max(float(-FSR_RCAS_LIMIT),min(AMax3F1(lobeR,lobeG,lobeB),0.0))*con;
  // Apply noise removal.
  #ifdef FSR_RCAS_DENOISE
   lobe*=nz;
  #endif
  // Resolve, which needs the medium precision rcp approximation to avoid visible tonality changes.
  float rcpL=APrxMedRcpF1(4.0*lobe+1.0);
  pixR=(lobe*bR+lobe*dR+lobe*hR+lobe*fR+eR)*rcpL;
  pixG=(lobe*bG+lobe*dG+lobe*hG+lobe*fG+eG)*rcpL;
  pixB=(lobe*bB+lobe*dB+lobe*hB+lobe*fB+eB)*rcpL;
  return;
}
#endif // FSR_RCAS_F

//==============================================================================================================================
//
//                                          FSR - [LFGA] LINEAR FILM GRAIN APPLICATOR
//
//------------------------------------------------------------------------------------------------------------------------------
// Adding output-resolution film grain after scaling is a good way to mask both rendering and scaling artifacts.
// Suggest using tiled blue noise as film grain input, with peak noise frequency set for a specific look and feel.
// The 'Lfga*()' functions provide a convenient way to introduce grain.
// These functions limit grain based on distance to signal limits.
// This is done so that the grain is temporally energy preserving, and thus won't modify image tonality.
// Grain application should be done in a linear colorspace.
// The grain should be temporally changing, but have a temporal sum per pixel that adds to zero (non-biased).
//------------------------------------------------------------------------------------------------------------------------------
// Usage,
//   FsrLfga*(
//    color, // In/out linear colorspace color {0 to 1} ranged.
//    grain, // Per pixel grain texture value {-0.5 to 0.5} ranged, input is 3-channel to support colored grain.
//    amount); // Amount of grain (0 to 1} ranged.
//------------------------------------------------------------------------------------------------------------------------------
// Example if grain texture is monochrome: 'FsrLfgaF(color,AF3_(grain),amount)'
//==============================================================================================================================

// Maximum grain is the minimum distance to the signal limit.
void FsrLfgaF(inout vec3 c,vec3 t,float a){c+=(t*vec3(a))*min(vec3(1.0)-c,c);}

//==============================================================================================================================
//
//                                          FSR - [SRTM] SIMPLE REVERSIBLE TONE-MAPPER
//
//------------------------------------------------------------------------------------------------------------------------------
// This provides a way to take linear HDR color {0 to FP16_MAX} and convert it into a temporary {0 to 1} ranged post-tonemapped linear.
// The tonemapper preserves RGB ratio, which helps maintain HDR color bleed during filtering.
//------------------------------------------------------------------------------------------------------------------------------
// Reversible tonemapper usage,
//  FsrSrtm*(color); // {0 to FP16_MAX} converted to {0 to 1}.
//  FsrSrtmInv*(color); // {0 to 1} converted into {0 to 32768, output peak safe for FP16}.
//==============================================================================================================================

void FsrSrtmF(inout vec3 c){c*=AF3_(ARcpF1(AMax3F1(c.r,c.g,c.b)+1.0));}
// The extra max solves the c=1.0 case (which is a /0).
void FsrSrtmInvF(inout vec3 c){c*=AF3_(ARcpF1(max(float(1.0/32768.0),1.0-AMax3F1(c.r,c.g,c.b))));}

//==============================================================================================================================
//
//                                       FSR - [TEPD] TEMPORAL ENERGY PRESERVING DITHER
//
//------------------------------------------------------------------------------------------------------------------------------
// Temporally energy preserving dithered {0 to 1} linear to gamma 2.0 conversion.
// Gamma 2.0 is used so that the conversion back to linear is just to square the color.
// The conversion comes in 8-bit and 10-bit modes, designed for output to 8-bit UNORM or 10:10:10:2 respectively.
// Given good non-biased temporal blue noise as dither input,
// the output dither will temporally conserve energy.
// This is done by choosing the linear nearest step point instead of perceptual nearest.
// See code below for details.
//------------------------------------------------------------------------------------------------------------------------------
// DX SPEC RULES FOR FLOAT->UNORM 8-BIT CONVERSION
// ===============================================
// - Output is 'uint(floor(saturate(n)*255.0+0.5))'.
// - Thus rounding is to nearest.
// - NaN gets converted to zero.
// - INF is clamped to {0.0 to 1.0}.
//==============================================================================================================================

 // Hand tuned integer position to dither value, with more values than simple checkerboard.
 // Only 32-bit has enough precision for this computation.
 // Output is {0 to <1}.
float FsrTepdDitF(uvec2 p, uint f){
  float x=float(p.x+f);
  float y=float(p.y);
  // The 1.61803 golden ratio.
  float a=((1.0+sqrt(5.0))/2.0);
  // Number designed to provide a good visual pattern.
  float b=(1.0/3.69);
  x=x*a+(y*b);
  return fract(x);
}
//------------------------------------------------------------------------------------------------------------------------------
 // This version is 8-bit gamma 2.0.
 // The 'c' input is {0 to 1}.
 // Output is {0 to 1} ready for image store.
void FsrTepdC8F(inout vec3 c, float dit){
  vec3 n=sqrt(c);
  n=floor(n*(255.0))*(1.0/255.0);
  vec3 a=n*n;
  vec3 b=n+(1.0/255.0);b=b*b;
  // Ratio of 'a' to 'b' required to produce 'c'.
  // APrxLoRcpF1() won't work here (at least for very high dynamic ranges).
  // APrxMedRcpF1() is an IADD,FMA,MUL.
  vec3 r=(c-b)*APrxMedRcpF3(a-b);
  // Use the ratio as a cutoff to choose 'a' or 'b'.
  // AGtZeroF1() is a MUL.
  c=ASatF3(n+AGtZeroF3(vec3(dit)-r)*AF3_(1.0/255.0));
}
//------------------------------------------------------------------------------------------------------------------------------
 // This version is 10-bit gamma 2.0.
 // The 'c' input is {0 to 1}.
 // Output is {0 to 1} ready for image store.
 void FsrTepdC10F(inout vec3 c, float dit){
  vec3 n=sqrt(c);
  n=floor(n*(1023.0))*(1.0/1023.0);
  vec3 a=n*n;
  vec3 b=n+(1.0/1023.0);b=b*b;
  vec3 r=(c-b)*APrxMedRcpF3(a-b);
  c=ASatF3(n+AGtZeroF3(vec3(dit)-r)*(1.0/1023.0));
}

