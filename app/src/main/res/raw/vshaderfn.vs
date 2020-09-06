attribute vec4 vPosition;
uniform mat4 mvpMatrix;
attribute vec2 vTexCoords;
varying vec2 oTexCoords;

void main() {
   oTexCoords = vTexCoords;
   gl_Position = mvpMatrix * vPosition;
}
