precision highp float;

uniform vec4 sunDir;
uniform vec4 earthCenter;
uniform float earthRadius;

varying vec3 position;

float distanceToCenter()
{
    vec3 s = cross(position - earthCenter.xyz, sunDir.xyz);
    return length(s);
}

void main() {
  float d = distanceToCenter();
  if(d > earthRadius) {
    return;
  }
  float alpha = 0.35*(1.0 - d*d/(earthRadius*earthRadius));
  gl_FragColor = vec4(0.0, 0.0, 0.0, alpha);
}