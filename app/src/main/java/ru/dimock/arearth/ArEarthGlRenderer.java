package ru.dimock.arearth;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ArEarthGlRenderer implements GLSurfaceView.Renderer {
    private ArEarthBkgnd background;
    private ArEarthPlanet planet;
    private ArEarthPlane plane;
    private ShadowVisibility shadowVisibility;
    private ArEarthInputManager imanager;

    private Matrix4 rotationMatrix = new Matrix4();
    private Matrix4 translationMatrix = new Matrix4();
    private Matrix4 modelMatrix = new Matrix4();
    private Matrix4 viewMatrix = new Matrix4();
    private Matrix4 viewMatrixI = new Matrix4();
    private Matrix4 viewMatrixIax = new Matrix4();
    private Matrix4 modelViewProjection = new Matrix4();
    private Matrix4 projectionMatrix = new Matrix4();
    private Matrix4 modelViewMatrix = new Matrix4();
    private Matrix4 rotationCurrentMatrix = new Matrix4();
    private Matrix4 tempMatrix = new Matrix4();
    private Matrix4 tempMatrixAx = new Matrix4();
    private float currentAngle = 0;
    private Vector3 currentAxis = new Vector3(0, 1, 0);
    private Vector3 sun = new Vector3(0, 0, 1);
    private int viewportWidth_ = 0;
    private int viewportHeight_ = 0;
    Vector3 planetCenter = new Vector3(0, 0,0);
    Vector3 center = new Vector3(0, 0,0);
    float planetOffsetZ = -1.0f;
    private static float zNear = 0.01f;
    private static float zFar = 100.0f;

    private ArEarthActivity context;

    private InputManagerCallback icallback = new InputManagerCallback() {
        @Override
        public void onInput(ArEarthRenderMode rmode) {
            if(rmode == ArEarthRenderMode.ARM_CLOSE) {
                context.runOnUiThread(new Runnable() {
                    public void run() {
                        context.finishAffinity();
                    }
                });
            }
        }
    };

    private Comparator<SortPlane> sortPlaneComparator = new Comparator<SortPlane>() {
        @Override
        public int compare(SortPlane sp1, SortPlane sp2) {
            return Float.compare(sp1.dist, sp2.dist);
        }
    };

    ArEarthGlRenderer(ArEarthActivity c) {
        viewMatrix.setLookAt(new Vector3(0, 0, 2), new Vector3(0, 0, 0), new Vector3(0, 1, 0));
        rotationMatrix.setIdentity();
        translationMatrix.setIdentity();
        translationMatrix.translate(new Vector3(0, 0, -2));
        context = c;
    }

    public void onSurfaceCreated(GL10 var1, EGLConfig var2) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        background = new ArEarthBkgnd(context);
        planet = new ArEarthPlanet(zNear, context);
        plane = new ArEarthPlane(planet.radius(), context);
        shadowVisibility = new ShadowVisibility(planet.radius());
        imanager = new ArEarthInputManager(context, icallback);
    }

    public int viewportWidth() {
        return viewportWidth_;
    }

    public int viewportHeight() {
        return viewportHeight_;
    }

    @Override
    public void onSurfaceChanged(GL10 var1, int width, int height) {
        viewportWidth_ = width;
        viewportHeight_ = height;
        context.changeViewport();
        GLES20.glViewport(0,0, width, height);
        imanager.updateViewport(width, height);
    }

    @Override
    public void onDrawFrame(GL10 var1) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        try {
            context.updateViewport();
            if(imanager.getRenderMode() == ArEarthRenderMode.ARM_SPLASH_SCREEN) {
                imanager.drawSplashScreen();
                return;
            }

            Session session = context.getSession();
            if (session == null) {
                return;
            }
            session.setCameraTextureName(background.getTextureId());

            Frame frame = session.update();
            Camera camera = frame.getCamera();

            sun.set(frame.getLightEstimate().getEnvironmentalHdrMainLightDirection());
            sun.invert();
            sun.normalize();

            camera.getProjectionMatrix(projectionMatrix.get(), 0, zNear, zFar);
            camera.getViewMatrix(viewMatrix.get(), 0);

            background.setHelpMode(imanager.getRenderMode() == ArEarthRenderMode.ARM_SHOW_HELP);
            background.draw(frame, null, null, null, null, null);

            if(imanager.getRenderMode() == ArEarthRenderMode.ARM_SHOW_HELP) {
                imanager.drawHelpScreen();
                return;
            }

            if (camera.getTrackingState() == TrackingState.PAUSED) {
                if(imanager.getRenderMode() == ArEarthRenderMode.ARM_POSITIONING) {
                    imanager.onModeChange(ArEarthRenderMode.ARM_NOT_READY);
                }
                else if(imanager.getRenderMode() == ArEarthRenderMode.ARM_NORMAL_RENDER) {
                    imanager.onModeChange(ArEarthRenderMode.ARM_PAUSED);
                }
            }
            else if(camera.getTrackingState() == TrackingState.TRACKING) {
                if(imanager.getRenderMode() == ArEarthRenderMode.ARM_NOT_READY) {
                    imanager.onModeChange(ArEarthRenderMode.ARM_POSITIONING);
                }
                else if(imanager.getRenderMode() == ArEarthRenderMode.ARM_PAUSED) {
                    imanager.onModeChange(ArEarthRenderMode.ARM_NORMAL_RENDER);
                }
            }
            else if(camera.getTrackingState() == TrackingState.STOPPED) {
                imanager.onModeChange(ArEarthRenderMode.ARM_NOT_READY);
            }

            if(imanager.getRenderMode() == ArEarthRenderMode.ARM_PAUSED) {
                imanager.drawPausedScreen();
                return;
            }
            if(imanager.getRenderMode() == ArEarthRenderMode.ARM_NOT_READY) {
                imanager.drawNotReadyScreen();
                return;
            }

            if(imanager.getRenderMode() == ArEarthRenderMode.ARM_POSITIONING) {
                viewMatrix.invert(viewMatrixI);
                planetCenter.setx(0);
                planetCenter.sety(0);
                planetCenter.setz(planetOffsetZ);
                tempMatrix.setIdentity();
                tempMatrix.translate(planetCenter);
                viewMatrixI.matrixMultiply(tempMatrix, translationMatrix);
            }

            translationMatrix.matrixMultiply(rotationMatrix, modelMatrix);
            viewMatrix.matrixMultiply(modelMatrix, modelViewMatrix);
            projectionMatrix.matrixMultiply(modelViewMatrix, modelViewProjection);

            modelMatrix.vectorMultiply(center, planetCenter);

            List<SortPlane> splanes = new ArrayList<>();
            Collection<Plane> planes = session.getAllTrackables(Plane.class);
            for (Plane p : planes) {
                if (p.getTrackingState() != TrackingState.TRACKING ||
                    p.getSubsumedBy() != null ||
                    p.getPolygon() == null ||
                    p.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                    continue;
                }
                if(shadowVisibility.isVisible(p, planetCenter, sun)) {
                    splanes.add(new SortPlane(p, planetCenter));
                }
            }

            Collections.sort(splanes, sortPlaneComparator);

            for(SortPlane sp : splanes) {
                plane.drawShadow(frame, modelViewProjection, modelViewMatrix, projectionMatrix, viewMatrix, sp.plane, sun);
                break;
            }

            planet.draw(frame, modelViewProjection, modelViewMatrix, projectionMatrix, modelMatrix, sun);
            imanager.draw();
        }
        catch (Throwable t) {
        }
    }

    public void onDown(Vector3 p) {
        imanager.onDown(p);
    }

    public void onUp(Vector3 p) {
        imanager.onUp(p);
    }

    public void onMove(Vector3 p) {
        imanager.onMove(p);
    }

    public void setAngle(float angle) {
        currentAngle = angle;
        if(imanager.getRenderMode() != ArEarthRenderMode.ARM_POSITIONING &&
           imanager.getRenderMode() != ArEarthRenderMode.ARM_NORMAL_RENDER) {
            return;
        }
        rotationCurrentMatrix.rotate(currentAngle, currentAxis);
        rotationCurrentMatrix.matrixMultiply(rotationMatrix, rotationMatrix);
        rotationMatrix.normalize();
    }

    public void setAxis(Vector3 axis) {
        if(imanager.getRenderMode() != ArEarthRenderMode.ARM_POSITIONING &&
           imanager.getRenderMode() != ArEarthRenderMode.ARM_NORMAL_RENDER) {
            return;
        }
        viewMatrix.matrixMultiply(translationMatrix, tempMatrix);
        tempMatrixAx.assignRotation(tempMatrix);
        tempMatrixAx.invert(viewMatrixIax);
        viewMatrixIax.vectorMultiply(axis, currentAxis);
        currentAxis.normalize();
    }
}
