package ru.dimock.arearth;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class ArEarthBkgnd extends ArEarthRect {
    protected static final int TEXCOORDS_PER_VERTEX = 2;
    protected FloatBuffer textureCoordinates;
    protected FloatBuffer textureCoordinatesTransformed;
    protected static float texcoords[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };
    protected  int texcoordStride;
    protected  int texcoordsCount;
    protected  int textureDataHandle = -1;
    private static final float DARK_COLOR = 0.3f;

    public ArEarthBkgnd(ArEarthActivity c) {
        super(c);
        textureDataHandle = createBkgndTexture();

        ByteBuffer txb = ByteBuffer.allocateDirect(texcoords.length * Float.BYTES);
        txb.order(ByteOrder.nativeOrder());
        textureCoordinates = txb.asFloatBuffer();
        textureCoordinates.put(texcoords);
        textureCoordinates.position(0);
        texcoordStride = TEXCOORDS_PER_VERTEX * 4;
        texcoordsCount = texcoords.length / TEXCOORDS_PER_VERTEX;

        ByteBuffer txbt = ByteBuffer.allocateDirect(texcoords.length * Float.BYTES);
        txbt.order(ByteOrder.nativeOrder());
        textureCoordinatesTransformed = txbt.asFloatBuffer();

        createShader(R.raw.vshaderbg, R.raw.fshaderbg);

        GLES20.glBindAttribLocation(program_, 0, "vPosition");
        GLES20.glLinkProgram(program_);
    }

    public int getTextureId() {
        return textureDataHandle;
    }

    @Override
    public void draw(Frame frame, Matrix4 matrixMVP, Matrix4 matrixMV, Matrix4 matrixP, Matrix4 matrixR, Vector3 sun) {
        if (frame.hasDisplayGeometryChanged()) {
            frame.transformCoordinates2d(Coordinates2d.VIEW_NORMALIZED, textureCoordinates,
                    Coordinates2d.TEXTURE_NORMALIZED, textureCoordinatesTransformed);
        }
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_DEPTH_WRITEMASK);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureDataHandle);
        GLES20.glUseProgram(program_);

        int colorHandle = GLES20.glGetUniformLocation(program_, "vColor");
        GLES20.glUniform4fv(colorHandle, 1, color.get(), 0);

        int positionHandle = GLES20.glGetAttribLocation(program_, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);

        vertices.rewind();
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, vertexStride, vertices);

        int texcoordsHandle = GLES20.glGetAttribLocation(program_, "vTexCoords");
        GLES20.glEnableVertexAttribArray(texcoordsHandle);

        textureCoordinatesTransformed.rewind();
        GLES20.glVertexAttribPointer(texcoordsHandle, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, texcoordStride, textureCoordinatesTransformed);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, triangles);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private int createBkgndTexture() {
        int textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        int textureHandle[] = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        int textureId = textureHandle[0];
        GLES20.glBindTexture(textureTarget, textureId);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        return textureId;
    }

    public void setHelpMode(boolean hlp) {
        if(hlp) {
            color.setAll(DARK_COLOR);
        } else {
            color.setAll(1);
        }
    }
}
