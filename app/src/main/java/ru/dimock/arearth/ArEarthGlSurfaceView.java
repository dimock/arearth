package ru.dimock.arearth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class ArEarthGlSurfaceView extends GLSurfaceView {
    private ArEarthGlRenderer renderer;
    private float prevX = 0;
    private float prevY = 0;
    private float ANGLE_SCALE_FACTOR = 300.0f;
    private Vector3 axis = new Vector3(0, 0, 0);
    private Vector3 pt = new Vector3(0, 0, 0);
    private float angle = 0;
    private double L = 0;
    private float angularVelocity = 0;

    static long durationRollMs = 1500;
    private ValueAnimator planetAnimation_ = null;

    public ArEarthGlSurfaceView(ArEarthActivity context) {
        super(context);
        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        renderer = new ArEarthGlRenderer(context);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public int viewportWidth() {
        return renderer.viewportWidth();
    }

    public int viewportHeight() {
        return renderer.viewportHeight();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        float dx = 0, dy = 0;
        pt.setx((2.0f*x)/viewportWidth() - 1.0f);
        pt.sety(1.0f - (2.0f*y)/viewportHeight());
        renderer.setAngle(0);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                stopPlanetAnimation();
                renderer.onDown(pt);
                break;
            case MotionEvent.ACTION_UP:
                startPlanetAnimation();
                renderer.onUp(pt);
                break;
            case MotionEvent.ACTION_MOVE:
                dx = x - prevX;
                dy = prevY - y;
                renderer.onMove(pt);
                break;
            default:
                return true;
        }
        prevX = x;
        prevY = y;
        if(Math.abs(dx) < 0.01f && Math.abs(dy) < 0.01f) {
            return true;
        }
        double l = Math.sqrt(dx*dx + dy*dy);
        L = Math.max(l, L);
        double t = l / L;
        dx /= l;
        dy /= l;
        axis.setx((float)(axis.x() * (1 - t) -  dy * t));
        axis.sety((float)(axis.y() * (1 - t) +  dx * t));
        if(viewportWidth() > 0) {
            ANGLE_SCALE_FACTOR = (float) (Math.sqrt(viewportWidth() * viewportWidth() + viewportHeight() * viewportHeight()) / 2.0f);
        }
        angle = (float)Math.toDegrees(l / ANGLE_SCALE_FACTOR);
        angularVelocity += angle;
        angularVelocity /= 2.0f;
        renderer.setAngle(angle);
        renderer.setAxis(axis);
        requestRender();
        return true;
    }

    private void startPlanetAnimation() {
        planetAnimation_ = ValueAnimator.ofFloat(1f, 0f);
        planetAnimation_.setDuration(durationRollMs);
        planetAnimation_.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                float t = (float)updatedAnimation.getAnimatedValue();
                updateByTimer(t);
            }
        });
        planetAnimation_.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                onAnimationCompleted();
            }
        });
        planetAnimation_.start();
    }

    private void stopPlanetAnimation() {
        if(planetAnimation_ != null) {
            planetAnimation_.cancel();
        }
        planetAnimation_ = null;
        angularVelocity = 0;
    }

    private void onAnimationCompleted() {
        angularVelocity = 0;
    }

    private void updateByTimer(float t) {
        renderer.setAngle(angularVelocity * t);
        requestRender();
    }
}
