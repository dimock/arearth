package ru.dimock.arearth;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.google.ar.core.Frame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class ArEarthRect {
    protected static final int COORDS_PER_VERTEX = 3;
    protected FloatBuffer vertices;
    protected ShortBuffer triangles;
    protected static float coords[] = {
            -1.0f, -1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f,
            1.0f,  1.0f, 0.0f,
            1.0f, -1.0f, 0.0f
    };
    protected static short indices[] = {
            0, 1, 2,
            0, 2, 3
    };
    protected Vector3 color = new Vector3(1, 1, 1);
    protected int program_ = -1;
    protected int vertexStride;
    protected int vertexCount;
    protected int indexCount;
    protected ArEarthActivity context;

    public ArEarthRect(ArEarthActivity c) {
        context = c;
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * Float.BYTES);
        bb.order(ByteOrder.nativeOrder());
        vertices = bb.asFloatBuffer();
        vertices.put(coords);
        vertices.rewind();
        vertexStride = COORDS_PER_VERTEX * 4;
        vertexCount = coords.length / COORDS_PER_VERTEX;

        ByteBuffer tbb = ByteBuffer.allocateDirect(indices.length * Short.BYTES);
        tbb.order(ByteOrder.nativeOrder());
        triangles = tbb.asShortBuffer();
        triangles.put(indices);
        triangles.rewind();
        indexCount = indices.length;
    }

    protected void createShader(int vshaderId, int fshaderId) {
        ArEarthShader shader = new ArEarthShader(context, vshaderId, fshaderId);
        program_ = shader.program();
    }

    public int loadTexture(int texture_id, int minmagFilter) {
        int textureHandle[] = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        if(textureHandle[0] != 0) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), texture_id, options);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, minmagFilter);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, minmagFilter);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        }
        if(textureHandle[0] == 0)
            throw new RuntimeException("Error loading texture.");
        return textureHandle[0];
    }

    public void draw(Frame frame, Matrix4 matrixMVP, Matrix4 matrixMV, Matrix4 matrixP, Matrix4 matrixR, Vector3 sun) {
    }
}
