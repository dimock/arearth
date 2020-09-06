attribute vec4 vPosition;
uniform mat4 matrixVP;
uniform mat4 matrixW;
varying vec3 position;

void main() {
   vec4 v = matrixW * vPosition;
   position = v.xyz;
   gl_Position = matrixVP * v;
}