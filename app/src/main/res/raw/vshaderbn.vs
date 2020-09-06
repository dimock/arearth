attribute vec4 vPosition;
attribute vec2 vTexCoords;
uniform mat4 matrixMVP;
varying vec2 oTexCoords;

void main() {
    oTexCoords = vTexCoords;
    gl_Position = matrixMVP * vPosition;
}