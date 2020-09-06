package ru.dimock.arearth;

import android.opengl.GLES20;

import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class ArEarthPlane extends ArEarthRect {
    private float radius = 0.0f;
    private Vector3 origin = new Vector3(0, 0, 0);
    private Vector3 cameraPos = new Vector3(0, 0, 0);
    private Vector3 earthCenter = new Vector3(0, 0, 0);
    private Matrix4 matrixInv = new Matrix4();
    private Matrix4 matrixVP = new Matrix4();
    private Matrix4 matrixW = new Matrix4();
    private static Vector3 planeNorm = new Vector3(0, 0,0);
    private static Vector3 planeOrig = new Vector3(0, 0,0);

    public ArEarthPlane(float earthR, ArEarthActivity c) {
        super(c);
        radius = earthR;
        createShader(R.raw.vshaderpn, R.raw.fshaderpn);
        GLES20.glBindAttribLocation(program_, 0, "vPosition");
        GLES20.glLinkProgram(program_);
    }

    public void drawShadow(Frame frame, Matrix4 matrixMVP, Matrix4 matrixMV, Matrix4 matrixP, Matrix4 matrixV,
                           Plane pln, Vector3 sun)
    {
        if(pln == null)
            return;

        pln.getCenterPose().getTransformedAxis(1, 1.0f, planeNorm.get(), 0);
        planeOrig.setx(pln.getCenterPose().tx());
        planeOrig.sety(pln.getCenterPose().ty());
        planeOrig.setz(pln.getCenterPose().tz());

        origin.set(0, 0, 0);
        matrixV.invert(matrixInv);
        matrixInv.vectorMultiply(origin, cameraPos);
        planeOrig.subtract(cameraPos, origin);
        float v = origin.dot(planeNorm);
        if(v > 0.0f)
            return;

        if(!initializePlaneVertices(pln))
            return;

        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_DEPTH_WRITEMASK);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glUseProgram(program_);

        earthCenter.set(0, 0, 0);
        matrixMV.vectorMultiply(earthCenter, origin);
        matrixInv.vectorMultiply(origin, earthCenter);

        int handleSunDir = GLES20.glGetUniformLocation(program_, "sunDir");
        GLES20.glUniform4fv(handleSunDir, 1, sun.get(), 0);

        int handleEarthCenter = GLES20.glGetUniformLocation(program_, "earthCenter");
        GLES20.glUniform4fv(handleEarthCenter, 1, earthCenter.get(), 0);

        int handleEarthRadius = GLES20.glGetUniformLocation(program_, "earthRadius");
        GLES20.glUniform1f(handleEarthRadius, radius);

        matrixP.matrixMultiply(matrixV, matrixVP);
        int handleVP = GLES20.glGetUniformLocation(program_, "matrixVP");
        GLES20.glUniformMatrix4fv(handleVP, 1, false, matrixVP.get(), 0);

        Pose pose = pln.getCenterPose();
        pose.toMatrix(matrixW.get(), 0);
        int handleW = GLES20.glGetUniformLocation(program_, "matrixW");
        GLES20.glUniformMatrix4fv(handleW, 1, false, matrixW.get(), 0);

        int positionHandle = GLES20.glGetAttribLocation(program_, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        vertices.rewind();
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertices);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, triangles);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private boolean initializePlaneVertices(Plane pln) {
        FloatBuffer polygon = pln.getPolygon();
        float[] pcoords = polygon.array();
        vertexCount = pcoords.length / 2;
        int trianglesN = vertexCount - 2;
        if(trianglesN < 1)
            return  false;

        int positionCount = vertexCount * 3;
        if(vertices == null || vertices.capacity() < positionCount) {
            ByteBuffer bb = ByteBuffer.allocateDirect(positionCount * Float.BYTES);
            bb.order(ByteOrder.nativeOrder());
            vertices = bb.asFloatBuffer();
        }

        vertices.rewind();
        for(int i = 0; i < vertexCount; ++i) {
            vertices.put(pcoords[i*2+0]);
            vertices.put(0);
            vertices.put(pcoords[i*2+1]);
        }
        vertices.rewind();
        vertexStride = COORDS_PER_VERTEX * 4;

        indexCount = trianglesN * 3;
        if(triangles == null || triangles.capacity() < indexCount) {
            ByteBuffer tbb = ByteBuffer.allocateDirect(indexCount * Short.BYTES);
            tbb.order(ByteOrder.nativeOrder());
            triangles = tbb.asShortBuffer();
        }
        triangles.rewind();
        for(int i = 0; i < trianglesN; ++i) {
            triangles.put((short)0);
            triangles.put((short)(i+1));
            triangles.put((short)(i+2));
        }
        triangles.rewind();
        return true;
    }
}
