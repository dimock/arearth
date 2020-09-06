package ru.dimock.arearth;

import android.opengl.GLES20;
import com.google.ar.core.Frame;

public class ArEarthPlanet extends ArEarthRect {
    private static float SCALE = 0.2f;
    private static float earthRadius = 1.0f*SCALE;
    private static float spaceRadius = 1.1f*SCALE;
    private static float cloudsRadius = 1.02f*SCALE;
    private static float cloudsThickness = 0.015f*SCALE;
    private static float mountainsHeight = 0.025f*SCALE;
    private static float cloudsAlpha = 0.06f;
    private static float earthDiffuse = 1.3f;
    private static float earthSpecular = 0.5f;
    private static float earthEmissive = 0.2f;
    private static float earthSpecularPower = 60.0f;
    private static float cloudsDiffuse = 1.0f;
    private static float cloudsSpecular = 0.3f;
    private static float cloudsEmissive = 0.2f;
    private static float cloudsSpecularPower = 50.0f;

    private static Vector3 sunColor = new Vector3(1.0f, 0.97f, 0.84f);
    private static Vector3 earthAtmosphereColor = new Vector3(0.5f, 0.59f, 0.97f);
    private static Vector3 spaceAtmosphereColor = new Vector3(0.5f, 0.59f, 0.97f);
    private static Vector3 cloudsColor = new Vector3(1.0f, 1.0f, 1.0f);

    private int textureEarthHandle;
    private int textureHeightsHandle;
    private int textureNormalsCloudsHandle;

    private float zNear;
    private Vector3 nearPt = new Vector3(0, 0, 0);
    private Vector3 farPt = new Vector3(0, 0, 0);
    private Vector3 corigPt = new Vector3(0,0,0);
    private Vector3 cortZ = new Vector3(0, 0, 0);
    private Vector3 screenPt = new Vector3(0, 0, 0);
    private float nearZ = 0;
    private Vector3 origin = new Vector3(0, 0, 0);
    private Vector3 offset = new Vector3(0, 0, 0);
    private Vector3 vdir = new Vector3(0, 0, 0);
    private Vector3 vtemp = new Vector3(0, 0, 0);
    private Matrix4 matrixInv = new Matrix4();
    private Matrix4 matrixInvR = new Matrix4();

    public ArEarthPlanet(float zn, ArEarthActivity c) {
        super(c);
        zNear = zn;
        textureEarthHandle = loadTexture(R.drawable.earth, GLES20.GL_LINEAR);
        textureHeightsHandle = loadTexture(R.drawable.earth_heights, GLES20.GL_LINEAR);
        textureNormalsCloudsHandle = loadTexture(R.drawable.normals_clouds, GLES20.GL_LINEAR);
        createShader(R.raw.vshaderpl, R.raw.fshaderpl);
        GLES20.glBindAttribLocation(program_, 0, "vPosition");
        GLES20.glLinkProgram(program_);
    }

    public float radius() {
        return earthRadius;
    }

    private boolean calculateRangeAndCheckVisibility(Matrix4 matrixMV, Matrix4 matrixP, float R, float zNear)
    {
        origin.set(0, 0, 0);
        offset.set(0, 0, 1);

        matrixMV.invert(matrixInv);
        matrixInv.vectorMultiply(origin, farPt);
        corigPt.assign(farPt);
        nearZ = zNear;
        origin.subtract(offset, vtemp);
        matrixInv.vectorMultiply(vtemp, screenPt);
        screenPt.subtract(corigPt, cortZ);
        cortZ.normalize();

        origin.add(offset, vtemp);
        matrixInv.vectorMultiply(vtemp, nearPt);

        nearPt.subtract(farPt, vdir);
        vdir.normalize();

        vdir.multiply(R, vdir);

        origin.subtract(vdir, vtemp);
        matrixMV.vectorMultiply(vtemp, farPt);

        if(farPt.z() > -zNear)
            return false;

        origin.add(vdir, vtemp);
        matrixMV.vectorMultiply(vtemp, nearPt);

        vtemp.assign(nearPt);
        matrixP.vectorMultiply(vtemp, nearPt);

        vtemp.assign(farPt);
        matrixP.vectorMultiply(vtemp, farPt);

        return true;
    }

    private void setFshaderArguments(Matrix4 matrixMVP, Matrix4 matrixMV, Matrix4 matrixM, Vector3 sun) {

        int handlerEarth = GLES20.glGetUniformLocation(program_, "textureEarth");
        GLES20.glUniform1i(handlerEarth, 0);

        int handlerHeights = GLES20.glGetUniformLocation(program_, "textureHeights");
        GLES20.glUniform1i(handlerHeights, 1);

        int handlerNormalsClouds = GLES20.glGetUniformLocation(program_, "textureNormalsClouds");
        GLES20.glUniform1i(handlerNormalsClouds, 2);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureEarthHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHeightsHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNormalsCloudsHandle);

        matrixM.invert(matrixInv);
        matrixInvR.assignRotation(matrixInv);
        matrixInvR.vectorMultiply(sun, vtemp);
        int handleSunDir = GLES20.glGetUniformLocation(program_, "sunDir");
        GLES20.glUniform4fv(handleSunDir, 1, vtemp.get(), 0);

        int handleSunColor = GLES20.glGetUniformLocation(program_, "sunColor");
        GLES20.glUniform4fv(handleSunColor, 1, sunColor.get(), 0);

        int handleEarthAtmosphereColor = GLES20.glGetUniformLocation(program_, "earthAtmosphereColor");
        GLES20.glUniform4fv(handleEarthAtmosphereColor, 1, earthAtmosphereColor.get(), 0);

        int handleSpaceAtmosphereColor = GLES20.glGetUniformLocation(program_, "spaceAtmosphereColor");
        GLES20.glUniform4fv(handleSpaceAtmosphereColor, 1, spaceAtmosphereColor.get(), 0);

        int handleCloudsColor = GLES20.glGetUniformLocation(program_, "cloudsColor");
        GLES20.glUniform4fv(handleCloudsColor, 1, cloudsColor.get(), 0);

        int handleMVP = GLES20.glGetUniformLocation(program_, "matrix");
        GLES20.glUniformMatrix4fv(handleMVP, 1, false, matrixMVP.get(), 0);

        matrixMVP.invert(matrixInv);
        int handleInv = GLES20.glGetUniformLocation(program_, "matrixInv");
        GLES20.glUniformMatrix4fv(handleInv, 1, false, matrixInv.get(), 0);

        int handleFar = GLES20.glGetUniformLocation(program_, "farPoint");
        GLES20.glUniform4fv(handleFar, 1,  farPt.get(), 0);

        int handleNear = GLES20.glGetUniformLocation(program_, "nearPoint");
        GLES20.glUniform4fv(handleNear, 1,  nearPt.get(), 0);

        int handleOrigPoint = GLES20.glGetUniformLocation(program_, "coriginPoint");
        GLES20.glUniform4fv(handleOrigPoint, 1,  corigPt.get(), 0);

        int handleCortZ = GLES20.glGetUniformLocation(program_, "cortZ");
        GLES20.glUniform4fv(handleCortZ, 1,  cortZ.get(), 0);

        int handleNearZ = GLES20.glGetUniformLocation(program_, "nearZ");
        GLES20.glUniform1f(handleNearZ, nearZ);

        int handleEarthRadius = GLES20.glGetUniformLocation(program_, "earthRadius");
        GLES20.glUniform1f(handleEarthRadius, earthRadius);

        int handleSpaceRadius = GLES20.glGetUniformLocation(program_, "spaceRadius");
        GLES20.glUniform1f(handleSpaceRadius, spaceRadius);

        int handleCloudsRadius = GLES20.glGetUniformLocation(program_, "cloudsRadius");
        GLES20.glUniform1f(handleCloudsRadius, cloudsRadius);

        int handleCloudsThickness = GLES20.glGetUniformLocation(program_, "cloudsThickness");
        GLES20.glUniform1f(handleCloudsThickness, cloudsThickness);

        int handleMountainsHeight = GLES20.glGetUniformLocation(program_, "mountainsHeight");
        GLES20.glUniform1f(handleMountainsHeight, mountainsHeight);

        int handleCloudsAlpha = GLES20.glGetUniformLocation(program_, "cloudsAlpha");
        GLES20.glUniform1f(handleCloudsAlpha, cloudsAlpha);

        int handleEarthDiffuse = GLES20.glGetUniformLocation(program_, "earthDiffuse");
        GLES20.glUniform1f(handleEarthDiffuse, earthDiffuse);

        int handleEarthSpecular = GLES20.glGetUniformLocation(program_, "earthSpecular");
        GLES20.glUniform1f(handleEarthSpecular, earthSpecular);

        int handleEarthEmissive = GLES20.glGetUniformLocation(program_, "earthEmissive");
        GLES20.glUniform1f(handleEarthEmissive, earthEmissive);

        int handleEarthSpecularPower = GLES20.glGetUniformLocation(program_, "earthSpecularPower");
        GLES20.glUniform1f(handleEarthSpecularPower, earthSpecularPower);

        int handleCloudsDiffuse = GLES20.glGetUniformLocation(program_, "cloudsDiffuse");
        GLES20.glUniform1f(handleCloudsDiffuse, cloudsDiffuse);

        int handleCloudsSpecular = GLES20.glGetUniformLocation(program_, "cloudsSpecular");
        GLES20.glUniform1f(handleCloudsSpecular, cloudsSpecular);

        int handleCloudsEmissive = GLES20.glGetUniformLocation(program_, "cloudsEmissive");
        GLES20.glUniform1f(handleCloudsEmissive, cloudsEmissive);

        int handleCloudsSpecularPower = GLES20.glGetUniformLocation(program_, "cloudsSpecularPower");
        GLES20.glUniform1f(handleCloudsSpecularPower, cloudsSpecularPower);
    }

    @Override
    public void draw(Frame frame, Matrix4 matrixMVP, Matrix4 matrixMV, Matrix4 matrixP, Matrix4 matrixM, Vector3 sun) {
        if(textureEarthHandle == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        if(!calculateRangeAndCheckVisibility(matrixMV, matrixP, spaceRadius, zNear))
            return;

        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_DEPTH_WRITEMASK);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glUseProgram(program_);

        setFshaderArguments(matrixMVP, matrixMV, matrixM, sun);

        int positionHandle = GLES20.glGetAttribLocation(program_, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        vertices.rewind();
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertices);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, triangles);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}
