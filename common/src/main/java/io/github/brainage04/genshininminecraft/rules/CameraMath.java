package io.github.brainage04.genshininminecraft.rules;

/** Minecraft yaw: zero faces +Z, positive ninety faces -X; positive strafe means left. */
public final class CameraMath {
    public static final float TURN_DEGREES_PER_TICK = 24;
    public static final double LOCK_RANGE = 6;
    public static final double LOCK_CONE_COSINE = .5; // Adapted 120-degree horizontal cone.
    private CameraMath() {}

    public static double movementX(float yaw, double left, double forward) {
        double angle = Math.toRadians(yaw);
        return (left * Math.cos(angle) - forward * Math.sin(angle)) / Math.max(1, Math.hypot(left, forward));
    }
    public static double movementZ(float yaw, double left, double forward) {
        double angle = Math.toRadians(yaw);
        return (forward * Math.cos(angle) + left * Math.sin(angle)) / Math.max(1, Math.hypot(left, forward));
    }
    public static float movementYaw(float cameraYaw, double left, double forward) {
        return wrap(cameraYaw - (float) Math.toDegrees(Math.atan2(left, forward)));
    }
    public static float wrap(float angle) {
        float result = angle % 360;
        if (result >= 180) result -= 360;
        if (result < -180) result += 360;
        return result;
    }
    public static float smoothYaw(float current, float target, float maximumStep) {
        return current + Math.clamp(wrap(target - current), -maximumStep, maximumStep);
    }
    public static float facingYaw(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
    /** Nearest wins inside the camera's cone; infinity means ineligible. Distances use player feet. */
    public static double lockDistanceSquared(float cameraYaw, double dx, double dy, double dz) {
        double distance = dx * dx + dy * dy + dz * dz;
        double horizontal = Math.hypot(dx, dz);
        if (horizontal < 1e-6 || distance > LOCK_RANGE * LOCK_RANGE) return Double.POSITIVE_INFINITY;
        double angle = Math.toRadians(cameraYaw);
        double dot = (-dx * Math.sin(angle) + dz * Math.cos(angle)) / horizontal;
        return dot + 1e-12 >= LOCK_CONE_COSINE ? distance : Double.POSITIVE_INFINITY;
    }
    public static boolean preferTarget(double distance, int id, double bestDistance, int bestId) {
        return Double.isFinite(distance) && (distance < bestDistance || distance == bestDistance && id < bestId);
    }
}
