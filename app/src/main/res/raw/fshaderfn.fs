precision mediump float;
uniform sampler2D uTexture;
varying vec2 oTexCoords;
uniform vec4 vColor;

void main() {
   gl_FragColor = vColor * texture2D(uTexture, oTexCoords);
}
