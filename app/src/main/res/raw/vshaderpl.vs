attribute vec4 vPosition;
varying vec3 position;
void main() {
   position = vPosition.xyz;
   gl_Position = vPosition;
}