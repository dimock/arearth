package ru.dimock.arearth;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class ArEarthButton extends ArEarthRect {
    protected static final int TEXCOORDS_PER_VERTEX = 2;
    private int [][] textureHandles = new int[2][2];
    private int pressed_ = 0;
    private int enabled_ = 0;
    private Matrix4 matrixMVP = new Matrix4();
    private Matrix4 matrixInv = new Matrix4();
    private Vector3 vec = new Vector3();
    protected FloatBuffer textureCoordinates;
    protected static float texcoords[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };
    protected  int texcoordStride;
    protected  int texcoordsCount;

    public ArEarthButton(ArEarthActivity c, int [][] textureIds) {
        super(c);
        if(textureIds.length != 2 || textureIds[0].length != 2) {
            throw new RuntimeException("Error creating Button.");
        }
        for(int i = 0; i < 2; ++i) {
            for(int j = 0; j < 2; ++j) {
                textureHandles[i][j] = loadTexture(textureIds[i][j], GLES20.GL_LINEAR);
            }
        }
        buildTextureCoordinates();
        createShader(R.raw.vshaderbn, R.raw.fshaderbn);
        GLES20.glBindAttribLocation(program_, 0, "vPosition");
        GLES20.glLinkProgram(program_);
    }

    public void setPosition(Vector3 position, Vector3 size) {
        prepareMatrix(position, size);
    }

    private void buildTextureCoordinates() {
        ByteBuffer txb = ByteBuffer.allocateDirect(texcoords.length * Float.BYTES);
        txb.order(ByteOrder.nativeOrder());
        textureCoordinates = txb.asFloatBuffer();
        textureCoordinates.put(texcoords);
        textureCoordinates.position(0);
        texcoordStride = TEXCOORDS_PER_VERTEX * 4;
        texcoordsCount = texcoords.length / TEXCOORDS_PER_VERTEX;
    }

    public void setEnabled(boolean e) {
        enabled_ = e ? 1 : 0;
    }

    public boolean isEnabled() {
        return enabled_ == 1;
    }

    public boolean onButtonDown(Vector3 p) {
        if(enabled_ == 0 || !isPtInside(p)) {
            return false;
        }
        pressed_ = 1;
        return true;
    }

    public boolean onButtonUp(Vector3 p) {
        if(pressed_ == 0 || enabled_ == 0 || !isPtInside(p)) {
            return false;
        }
        pressed_ = 0;
        return true;
    }

    public boolean onButtonMove(Vector3 p) {
        if(isPtInside(p)) {
            return true;
        }
        pressed_ = 0;
        return false;
    }

    public void draw() {
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_DEPTH_WRITEMASK);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glUseProgram(program_);

        int handleMVP = GLES20.glGetUniformLocation(program_, "matrixMVP");
        GLES20.glUniformMatrix4fv(handleMVP, 1, false, matrixMVP.get(), 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[enabled_][pressed_]);

        int positionHandle = GLES20.glGetAttribLocation(program_, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        vertices.rewind();
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertices);

        int texcoordsHandle = GLES20.glGetAttribLocation(program_, "vTexCoords");
        GLES20.glEnableVertexAttribArray(texcoordsHandle);

        textureCoordinates.rewind();
        GLES20.glVertexAttribPointer(texcoordsHandle, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, texcoordStride, textureCoordinates);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, triangles);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void prepareMatrix(Vector3 position, Vector3 size) {
        Matrix4 matrixS = new Matrix4();
        matrixS.scale(size);
        Matrix4 matrixT = new Matrix4();
        matrixT.translate(position);
        matrixT.matrixMultiply(matrixS, matrixMVP);
        matrixMVP.invert(matrixInv);
    }

    private boolean isPtInside(Vector3 p) {
        matrixInv.vectorMultiply(p, vec);
        return  vec.x() >= -1.0f && vec.x() <= 1.0f && vec.y() >= -1.0f && vec.y() <= 1.0f;
    }
}
