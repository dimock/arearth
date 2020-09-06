attribute vec4 vPosition;
attribute vec2 vTexCoords;
varying vec2 oTexCoords;
void main() {
   oTexCoords = vTexCoords;
   gl_Position = vPosition;
}
