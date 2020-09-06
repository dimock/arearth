precision highp float;

uniform vec4 sunDir;
uniform vec4 sunColor;

uniform vec4 earthAtmosphereColor;
uniform vec4 spaceAtmosphereColor;
uniform vec4 cloudsColor;

uniform mat4 matrix;
uniform mat4 matrixInv;

uniform sampler2D textureEarth;
uniform sampler2D textureHeights;
uniform sampler2D textureNormalsClouds;

uniform vec4 farPoint;
uniform vec4 nearPoint;
uniform vec4 coriginPoint;
uniform vec4 cortZ;
uniform float nearZ;

uniform float earthRadius;
uniform float spaceRadius;
uniform float cloudsRadius;
uniform float cloudsThickness;

uniform float earthDiffuse;
uniform float earthSpecular;
uniform float earthEmissive;
uniform float earthSpecularPower;

uniform float cloudsDiffuse;
uniform float cloudsSpecular;
uniform float cloudsEmissive;
uniform float cloudsSpecularPower;
uniform float cloudsAlpha;

uniform float mountainsHeight;

varying vec3 position;

bool beforeScreen(in vec3 p)
{
  return dot((p - coriginPoint.xyz), cortZ.xyz) < nearZ;
}

void calculatePrerequisites(in vec3 pos, out vec3 pmid, out vec3 dir, out float distanceToCenter, out vec3 pfar, out vec3 pnear)
{
  vec4 pf;
  vec4 pn;

  pf.xy = pos.xy;
  pf.zw = farPoint.zw;
  pf.xy = pf.xy * pf.w;
  pf = matrixInv * pf;

  pn.xy = pos.xy;
  pn.zw = nearPoint.zw;
  pn.xy = pn.xy * pn.w;
  pn = matrixInv * pn;

  vec3 pnf = pf.xyz - pn.xyz;
  float distFNrcpr = 1.0 / length(pnf);
  dir = normalize(pnf);

  float distN = -dot(pn.xyz, pnf) * distFNrcpr;
  float distF = dot(pf.xyz, pnf) * distFNrcpr;

  float tn = distN * distFNrcpr;
  float tf = distF * distFNrcpr;

  vec3 pmid1 = mix(pn.xyz, pf.xyz, tn);
  vec3 pmid2 = mix(pf.xyz, pn.xyz, tf);
  pmid = mix(pmid1, pmid2, 0.5);
  distanceToCenter = length(pmid);
  pfar = pf.xyz;
  pnear = pn.xyz;
  if(beforeScreen(pnear)) {
    float dist = dot(cortZ.xyz, dir);
    pnear = coriginPoint.xyz + (nearZ / dist) * dir;
  }
}

void sphereIntersection(in vec3 pmid, in vec3 dir, in float distanceToCenter, in float radius, out vec3 ptNear, out vec3 ptFar)
{
  float delta = sqrt((radius+distanceToCenter) * (radius-distanceToCenter));
  ptNear = pmid - dir * delta;
  ptFar  = pmid + dir * delta;
}

vec2 textureCoords(in vec3 p, in float radius)
{
  float u = atan(p.z, -p.x)     * 0.318309886; // 1/PI
  float v = asin(-p.y / radius) * 0.636619772; // 2/PI
  return vec2((u+1.0)*0.5, (v+1.0)*0.5);
}

/// if we go through 0.0
void correctTextureCoords(inout vec2 farTc, inout vec2 nearTc)
{
  bvec2 gtfn = greaterThan(farTc-nearTc, vec2(0.5, 0.5));
  bvec2 gtnf = greaterThan(nearTc-farTc, vec2(0.5, 0.5));
  nearTc += vec2(gtfn);
  farTc += vec2(gtnf);
}

vec3 calculateColor(in vec3 p, in vec3 n, in vec2 tc, in vec3 objectColor,
                    in float diffuseIntensity, in float specularIntensity, in float emissiveIntensity, in float specularPower)
{
  vec3 sun = sunDir.xyz;
  vec3 eye = normalize((matrixInv * vec4(0.0, 0.0, 0.0, 1.0)).xyz);
  vec3 rv = normalize(2.0*n + sun);
  float diffuse = clamp(dot(-n, sun), 0.0, 1.0);
  float specular = pow(max(dot(eye, rv), 0.0), specularPower);
  vec3 color = ((diffuse*diffuseIntensity + specular*specularIntensity)*sunColor.rgb + emissiveIntensity)*objectColor;
  return color;
}

vec3 earthCenterColor(in vec3 p)
{
  float pleng = length(p);
  vec2 tc = textureCoords(p, pleng);
  float h = texture2D(textureHeights, tc).r * mountainsHeight;
  float radius = earthRadius + h;
  float r = pleng / radius;
  vec3 tcolor = texture2D(textureEarth, tc).rgb;
  vec3 centerColors[4];
  centerColors[0] = vec3(1.0, 1.0, 1.0);
  centerColors[1] = vec3(0.98, 0.95, 0.22);
  centerColors[2] = vec3(1.0, 0.2, 0.12);
  centerColors[3] = vec3(0.46, 0.22, 0.0);
  if(r >= 1.0) {
    return tcolor;
  }
  int index = int(min((floor(r*3.0)), 2.0));
  float t = (3.0*r - float(index));
  vec3 color = mix(centerColors[index], centerColors[index+1], t);
  float d = radius - pleng;
  if(d < mountainsHeight && d >= 0.0) {
    float t = d / mountainsHeight;
    color = mix(tcolor, color, t);
  }
  return color;
}

bool calculateEarthColor(in vec3 pmid, in vec3 dir, in vec3 pfar, in vec3 pnear, in float distanceToCenter,
                         inout vec3 farPt, inout vec4 accumulatedColor)
{
  if(distanceToCenter > earthRadius+mountainsHeight) {
    return false;
  }

  bool terrainFound = false;
  vec3 terrainPt;
  vec2 terrainTc;

  vec3 terrainNear;
  vec3 terrainFar;
  sphereIntersection(pmid, dir, distanceToCenter, earthRadius+mountainsHeight, terrainNear, terrainFar);

  vec2 terrainTcFar;
  vec2 terrainTcNear = textureCoords(terrainNear, earthRadius+mountainsHeight);

  float lengthFN = length(terrainFar - terrainNear);
  float stepL = mountainsHeight * 0.05;
  float t0 = 0.0;
  vec3 ptFar = terrainFar;

  if(distanceToCenter < earthRadius) {
    vec3 earthNear;
    vec3 earthFar;
    sphereIntersection(pmid, dir, distanceToCenter, earthRadius, earthNear, earthFar);

    if (beforeScreen(earthFar)) {
      return false;
    }
    if(beforeScreen(earthNear)) {
      accumulatedColor.rgb = earthCenterColor(pnear);
      accumulatedColor.a = 1.0;
      return true;
    }

    t0 = floor((length(earthNear - terrainFar)/lengthFN) / stepL)*stepL;
    ptFar = mix(terrainFar, terrainNear, t0);
    terrainTc = textureCoords(earthNear, earthRadius);
    terrainTcFar = textureCoords(ptFar, length(ptFar));
    terrainPt = earthNear;
    terrainFound = true;
  }
  else {
    if(beforeScreen(terrainFar)) {
      return false;
    }    
    if(beforeScreen(terrainNear)) {
      terrainNear = pnear;
      terrainTcNear = textureCoords(terrainNear, length(terrainNear));
      lengthFN = length(terrainFar - terrainNear);
    }    
    terrainTcFar = textureCoords(terrainFar, length(terrainFar));
  }

  float stepsN = max(floor(lengthFN / stepL), 1.0);
  float dt = 1.0 / stepsN;

  vec2 mountainTc;
  bool mountainFound = false;
  float heightMax= 0.0;

  vec3 terrainMid = mix(ptFar, terrainNear, 0.5);
  vec2 terrainTcMid = textureCoords(terrainMid, length(terrainMid));

  correctTextureCoords(terrainTcFar, terrainTcNear);
  correctTextureCoords(terrainTcFar, terrainTcMid);

  vec2 a = 2.0*(terrainTcNear + terrainTcFar) - 4.0*terrainTcMid;
  vec2 b = 4.0*terrainTcMid - 3.0*terrainTcFar - terrainTcNear;
  vec2 c = terrainTcFar;

  float ds = dt / (1.0 - t0);
  for(float t = t0, s = 0.0; t <= 1.0; t += dt, s += ds) {
    vec3 p = mix(terrainFar, terrainNear, t);
    float radius = length(p);
    vec2 tc = fract(a*s*s + b*s + c);
    float h = texture2D(textureHeights, tc).r * mountainsHeight;
    if(radius < earthRadius + h) {
      terrainFound = true;
      terrainPt = p;
      terrainTc = tc;
    }
  }

  if(terrainFound) {
    if(beforeScreen(terrainPt)) {
      accumulatedColor.rgb = earthCenterColor(pnear);
    }
    else {
      vec3 tnor = texture2D(textureNormalsClouds, terrainTc).xyz;
      tnor.z = 1.0;
      tnor = normalize(tnor - vec3(0.5, 0.5, 0.5));
      tnor.y = -tnor.y;
      vec3 oz = normalize(terrainPt);
      vec3 ox = normalize(cross(vec3(0.0, 1.0, 0.0), oz));
      vec3 oy = cross(oz, ox);
      mat3 mtx = mat3(ox, oy, oz);
      vec3 n = mtx * tnor;
      accumulatedColor.rgb = calculateColor(terrainPt, n, terrainTc, texture2D(textureEarth, terrainTc).rgb, earthDiffuse,
                                            earthSpecular, earthEmissive, earthSpecularPower);
    }
    accumulatedColor.a = 1.0;
    farPt = terrainPt;
  }

  return terrainFound;
}

bool calculateCloudsColor(bool terrainFound, in vec3 pmid, in vec3 dir, in vec3 pfar, in vec3 pnear, in float distanceToCenter, inout vec3 farPt, inout vec4 accumulatedColor)
{
  if((distanceToCenter > cloudsRadius+0.5*cloudsThickness) || (terrainFound && beforeScreen(farPt))) {
    return false;
  }

  vec3 cloudsOuterNear;
  vec3 cloudsOuterFar;
  sphereIntersection(pmid, dir, distanceToCenter, cloudsRadius+0.5*cloudsThickness, cloudsOuterNear, cloudsOuterFar);
  if(terrainFound && length(cloudsOuterNear) < length(farPt)) {
    return false;
  }

  if(!terrainFound) {
    farPt = cloudsOuterFar;
    if(beforeScreen(farPt)) {
      return false;
    }
  }

  if(beforeScreen(cloudsOuterNear)) {
    cloudsOuterNear = pnear;
  }

  vec2 nearTc = textureCoords(cloudsOuterNear, length(cloudsOuterNear));
  vec2 farTc = textureCoords(farPt, length(farPt));
  vec3 midPt = mix(farPt, cloudsOuterNear, 0.5);
  vec2 midTc = textureCoords(midPt, length(midPt));

  correctTextureCoords(farTc, nearTc);
  correctTextureCoords(farTc, midTc);

  vec2 a = 2.0*(nearTc + farTc) - 4.0*midTc;
  vec2 b = 4.0*midTc - 3.0*farTc - nearTc;
  vec2 c = farTc;

  float lengthFN = length(farPt - cloudsOuterNear);
  float stepL = cloudsThickness * 0.1;
  float stepsN = max(floor(lengthFN / stepL), 1.0);
  float dt = 1.0 / stepsN;

  float rcprThickness = 2.0 / cloudsThickness;
  for(float t = 0.0; t <= 1.0; t += dt) {
    vec3 p = mix(farPt, cloudsOuterNear, t);
    vec2 tc = fract(a*t*t + b*t + c);
    float r = length(p);
    float density = max(1.0 - abs(r - cloudsRadius) * rcprThickness, 0.0);

    float alpha = texture2D(textureNormalsClouds, tc).b * cloudsAlpha * density;
    vec3 color = calculateColor(p, normalize(p), tc, cloudsColor.rgb, cloudsDiffuse, cloudsSpecular, cloudsEmissive, cloudsSpecularPower);

    accumulatedColor.rgb = mix(accumulatedColor.rgb, color, alpha);
    accumulatedColor.a += alpha;
  }
  farPt = cloudsOuterNear;
  return true;
}

bool calculateFragColor(in vec3 pos, out vec4 accumulatedColor)
{
  vec3 pmid;
  vec3 pfar;
  vec3 pnear;
  vec3 dir;
  float distanceToCenter;
  calculatePrerequisites(pos, pmid, dir, distanceToCenter, pfar, pnear);

  if(distanceToCenter > spaceRadius) {
    return false;
  }

  accumulatedColor = vec4(0.0, 0.0, 0.0, 0.0);

  vec3 nearPt;
  vec3 farPt;
  sphereIntersection(pmid, dir, distanceToCenter, spaceRadius, nearPt, farPt);

  bool terrainFound = calculateEarthColor(pmid, dir, pfar, pnear, distanceToCenter, farPt, accumulatedColor);
  if(!calculateCloudsColor(terrainFound, pmid, dir, pfar, pnear, distanceToCenter, farPt, accumulatedColor)) {
    return terrainFound;
  }

  return true;
}

void main() {

  vec4 accumulatedColor;
  if(!calculateFragColor(position, accumulatedColor)) {
    return;
  }
  gl_FragColor = clamp(accumulatedColor, vec4(0.0, 0.0, 0.0, 0.0), vec4(1.0, 1.0, 1.0, 1.0));
}