#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES uTexture;
varying vec2 oTexCoords;
uniform vec4 vColor;

void main() {
   gl_FragColor = vColor * texture2D(uTexture, oTexCoords);
}
