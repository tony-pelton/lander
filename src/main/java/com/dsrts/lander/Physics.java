package com.dsrts.lander;

public class Physics {
    /**
     * Advances the lander's physics by dt seconds, applying controls and updating state.
     * @param lander The lander state to update
     * @param up True if up (main) thrust is applied
     * @param left True if left rotation is applied
     * @param right True if right rotation is applied
     * @param space True if side thrust is applied
     * @param dt Time step in seconds
     */
    public static final float GRAVITY = 1.62f; // m/s^2 (moon gravity)
    public static final float THRUST_POWER = 10.0f; // m/s^2 (main engine)
    public static final float ROTATE_SPEED = 2f; // deg/frame
    public static final float SIDE_THRUST = 2.5f; // m/s^2 (side jets)

    public static void advance(LanderState lander, double dt) {
        // Rotation (negative = clockwise in screen coordinates)
        if (lander.left) lander.angle -= ROTATE_SPEED;  // rotate counter-clockwise
        if (lander.right) lander.angle += ROTATE_SPEED; // rotate clockwise
        // Thrust (main engine)
        if (lander.up && lander.fuel > 0) { // Use THRUST_POWER
            // At 0 degrees, tip points up, so thrust should push down
            float rad = (float)Math.toRadians(lander.angle);
            // At 0 degrees (pointing up): sin=0, cos=1 -> (0,1) pushing down
            // At 90 degrees (pointing right): sin=1, cos=0 -> (1,0) pushing right
            float ax = (float)Math.sin(rad) * THRUST_POWER;
            float ay = (float)Math.cos(rad) * THRUST_POWER;
            lander.vx += ax * dt;
            lander.vy += ay * dt;
            lander.fuel -= 1*dt;
        }
        // Side thrust (with left/right + space)
        if (lander.space && lander.fuel > 0) {
            if (lander.left) { lander.vx -= SIDE_THRUST * dt; lander.fuel -= 0.5*dt; }
            if (lander.right) { lander.vx += SIDE_THRUST * dt; lander.fuel -= 0.5*dt; }
        }
        // Gravity (pulls down)
        lander.vy -= GRAVITY * dt;
        // Update position
        lander.x += lander.vx * dt;
        lander.y += lander.vy * dt;
    }
}
