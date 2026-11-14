package arondight.balance;

public final class LoomWallClamp {

    public static final float MAX_STEP = 1f / 30f;

    private LoomWallClamp() {}

    public static float bodyRadius(boolean hasShield, float shieldRadius, float collisionRadius,
            float fraction) {
        float r = hasShield && shieldRadius > 0f ? shieldRadius : collisionRadius * fraction;
        return Math.min(r, collisionRadius);
    }

    public static boolean holdsAtSpeed(float speed, float halfThick, float radius) {
        return speed * MAX_STEP < halfThick + radius;
    }

    public static boolean resolve(float[] spans, float halfThick, float radius,
            float along, float across, float vAlong, float vAcross, float[] out) {
        if (spans == null) return false;
        boolean moved = false;
        for (int i = 0; i + 1 < spans.length; i += 2) {
            float from = spans[i];
            float to = spans[i + 1];
            if (along >= from && along <= to) {
                float reach = halfThick + radius;
                if (across <= -reach || across >= reach) continue;
                float side = across >= 0f ? 1f : -1f;
                across = side * reach;
                if (vAcross * side < 0f) vAcross = 0f;
                moved = true;
                continue;
            }
            float endAlong = along < from ? from : to;
            float nearAcross = across < -halfThick ? -halfThick
                    : (across > halfThick ? halfThick : across);
            float dAlong = along - endAlong;
            float dAcross = across - nearAcross;
            float dist2 = dAlong * dAlong + dAcross * dAcross;
            if (dist2 >= radius * radius) continue;
            float dist = (float) Math.sqrt(dist2);
            float nAlong = dAlong / dist;
            float nAcross = dAcross / dist;
            along = endAlong + nAlong * radius;
            across = nearAcross + nAcross * radius;
            float into = vAlong * nAlong + vAcross * nAcross;
            if (into < 0f) {
                vAlong -= into * nAlong;
                vAcross -= into * nAcross;
            }
            moved = true;
        }
        if (moved) {
            out[0] = along;
            out[1] = across;
            out[2] = vAlong;
            out[3] = vAcross;
        }
        return moved;
    }
}
