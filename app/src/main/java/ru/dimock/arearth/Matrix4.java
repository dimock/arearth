package ru.dimock.arearth;

import android.opengl.Matrix;

public class Matrix4 {
    private float [] matrix = new float[16];

    public Matrix4() {
        setIdentity();
    }

    public float[] get() {
        return matrix;
    }

    public void vectorMultiply(Vector3 source, Vector3 dest) {
        Matrix.multiplyMV(dest.get(), 0, matrix, 0, source.get(), 0);
    }

    public void matrixMultiply(Matrix4 right, Matrix4 result) {
        Matrix.multiplyMM(result.matrix, 0, matrix, 0, right.matrix, 0);
    }

    public void setLookAt(Vector3 eye, Vector3 center, Vector3 up) {
        Matrix.setLookAtM(matrix, 0, eye.x(), eye.y(), eye.z(), center.x(), center.y(), center.z(), up.x(), up.y(), up.z());
    }

    public void setIdentity() {
        Matrix.setIdentityM(matrix, 0);
    }

    public void translate(Vector3 tr) {
        Matrix.translateM(matrix, 0, tr.x(), tr.y(), tr.z());
    }

    public void rotate(float angle, Vector3 axis) {
        Matrix.setRotateM(matrix, 0, angle, axis.x(), axis.y(), axis.z());
    }

    public void scale(Vector3 s) {
        Matrix.scaleM(matrix, 0, s.x(), s.y(), s.z());
    }

    public void invert(Matrix4 result) {
        Matrix.invertM(result.matrix, 0, matrix, 0);
    }

    public void normalize() {
        float l0 = Matrix.length(matrix[0], matrix[1], matrix[2]);
        float l1 = Matrix.length(matrix[4], matrix[5], matrix[6]);
        float l2 = Matrix.length(matrix[8], matrix[9], matrix[10]);

        matrix[0] /= l0;
        matrix[1] /= l0;
        matrix[2] /= l0;

        matrix[4] /= l1;
        matrix[5] /= l1;
        matrix[6] /= l1;

        matrix[8] /= l2;
        matrix[9] /= l2;
        matrix[10] /= l2;
    }

    public void assignRotation(Matrix4 other) {
        for(int i = 0; i < 16; ++i) {
            if(i != 12 && i != 13 && i != 14)
                matrix[i] = other.matrix[i];
            else
                matrix[i] = 0;
        }
    }
}
