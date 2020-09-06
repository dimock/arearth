package ru.dimock.arearth;

public class Vector3 {
    private float[] vec = new float[4];

    public Vector3() {
        this.set(0, 0, 0);
    }

    public Vector3(float x, float y, float z) {
        this.set(x, y, z);
    }

    public void set(float x, float y, float z) {
        vec[0] = x;
        vec[1] = y;
        vec[2] = z;
        vec[3] = 1.0f;
    }

    public void set(float[] xyz) {
        vec[0] = xyz[0];
        vec[1] = xyz[1];
        vec[2] = xyz[2];
        vec[3] = 1.0f;
    }

    public void setAll(float v) {
        vec[0] = v;
        vec[1] = v;
        vec[2] = v;
        vec[3] = 1.0f;
    }

    public void invert() {
        vec[0] = -vec[0];
        vec[1] = -vec[1];
        vec[2] = -vec[2];
    }

    public float[] get() {
        return vec;
    }

    public float x() {
        return vec[0];
    }

    public float y() {
        return vec[1];
    }

    public float z() {
        return vec[2];
    }

    public float setx(float x) {
        return vec[0] = x;
    }

    public float sety(float y) {
        return vec[1] = y;
    }

    public float setz(float z) {
        return vec[2] = z;
    }

    public float length() {
        float l = (float)Math.sqrt((double)(vec[0]*vec[0] + vec[1]*vec[1] + vec[2]*vec[2]));
        return l;
    }

    public void normalize() {
        float l = this.length();
        if(l > 0.0f) {
            l = 1.0f / l;
        }
        for(int i = 0; i < 3; ++i) {
            this.vec[i] *= l;
        }
    }

    public void subtract(Vector3 other, Vector3 result) {
        result.vec[0] = this.vec[0]-other.vec[0];
        result.vec[1] = this.vec[1]-other.vec[1];
        result.vec[2] = this.vec[2]-other.vec[2];
    }

    public void add(Vector3 other, Vector3 result) {
        result.vec[0] = this.vec[0]+other.vec[0];
        result.vec[1] = this.vec[1]+other.vec[1];
        result.vec[2] = this.vec[2]+other.vec[2];
    }

    public void multiply(float value, Vector3 result) {
        result.vec[0] = this.vec[0]*value;
        result.vec[1] = this.vec[1]*value;
        result.vec[2] = this.vec[2]*value;
    }

    public float dot(Vector3 other) {
        return this.vec[0]*other.vec[0] + this.vec[1]*other.vec[1] + this.vec[2]*other.vec[2];
    }

    public void cross(Vector3 other, Vector3 r) {
        r.setx(this.y()*other.z()-this.z()*other.y());
        r.sety(-this.x()*other.z()+this.z()*other.x());
        r.setz(this.x()*other.y()-this.y()*other.x());
    }

    public void assign(Vector3 other) {
        for(int i = 0; i < 3; ++i) {
            this.vec[i] = other.vec[i];
        }
    }
}
