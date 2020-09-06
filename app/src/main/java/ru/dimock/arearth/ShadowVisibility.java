package ru.dimock.arearth;

import com.google.ar.core.Plane;

import java.nio.FloatBuffer;

public class ShadowVisibility {
    private Vector3 point = new Vector3(0, 0, 0);
    private Vector3 point0 = new Vector3(0, 0, 0);
    private Vector3 dir0p = new Vector3(0, 0, 0);
    private Vector3 dir0c = new Vector3(0, 0, 0);
    private Vector3 center = new Vector3(0, 0, 0);
    private Vector3 sun = new Vector3(0, 0, 0);
    private Vector3 planeNorm = new Vector3(0, 1, 0);
    private Matrix4 matrix = new Matrix4();
    private Matrix4 matrixInv = new Matrix4();
    private Matrix4 rmatrixInv = new Matrix4();
    private float radius = 0;

    public ShadowVisibility(float r) {
        radius = r;
    }

    public boolean isVisible(Plane pln, Vector3 c, Vector3 s) {
        FloatBuffer polygon = pln.getPolygon();
        float[] pcoords = polygon.array();
        int vertexCount = pcoords.length / 2;
        if(vertexCount < 3)
            return false;
        pln.getCenterPose().toMatrix(matrix.get(), 0);
        matrix.invert(matrixInv);
        matrixInv.vectorMultiply(c, center);
        pln.getCenterPose().extractRotation().toMatrix(matrix.get(), 0);
        matrix.invert(rmatrixInv);
        rmatrixInv.vectorMultiply(s, sun);
        float dist = planeNorm.dot(center);
        if(dist < 0.0f)
            return false;
        float cosa = sun.dot(planeNorm);
        if(cosa > -0.0001f)
            return false;
        dist /= cosa;
        sun.multiply(dist, sun);
        center.subtract(sun, center);
        for(int i = 0; i <= vertexCount; ++i) {
            int j = i % vertexCount;
            point.setx(pcoords[j*2+0]);
            point.sety(0);
            point.setz(pcoords[j*2+1]);
            if(i > 0) {
                point.subtract(point0, dir0p);
                dir0p.normalize();
                center.subtract(point0, dir0c);
                dir0p.cross(dir0c, point0);
                float t = point0.dot(planeNorm);
                if(t < radius)
                    return false;
            }
            point0.assign(point);
        }
        return true;
    }
}
