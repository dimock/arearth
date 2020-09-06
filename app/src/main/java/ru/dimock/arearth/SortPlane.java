package ru.dimock.arearth;

import com.google.ar.core.Plane;

public class SortPlane {
    float dist = 0;
    Plane plane = null;

    SortPlane(Plane p, Vector3 cameraPos) {
        Vector3 center = new Vector3(p.getCenterPose().tx(), p.getCenterPose().ty(), p.getCenterPose().tz());
        cameraPos.subtract(center, center);
        dist = center.length();
        plane = p;
    }
}