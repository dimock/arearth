package ru.dimock.arearth;

import android.opengl.GLES20;
import com.google.ar.core.Frame;

public class ArEarthSplashScreen extends ArEarthRect {
    public ArEarthSplashScreen(ArEarthActivity c) {
        super(c);
        createShader(R.raw.vshaderss, R.raw.fshaderss);
        GLES20.glBindAttribLocation(program_, 0, "vPosition");
        GLES20.glLinkProgram(program_);
    }

    @Override
    public void draw(Frame frame, Matrix4 matrixMVP, Matrix4 matrixMV, Matrix4 matrixP, Matrix4 matrixR, Vector3 sun) {
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_DEPTH_WRITEMASK);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glUseProgram(program_);

        int positionHandle = GLES20.glGetAttribLocation(program_, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        vertices.rewind();
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertices);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, triangles);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}
