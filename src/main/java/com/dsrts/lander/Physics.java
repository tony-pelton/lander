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
    public static final float ENGINE_THRUST = 44000f; // Newtons
    public static final float FUEL_BURN_RATE = 14.5f; // kg/s
    public static final float ROTATE_SPEED = 2f; // deg/frame
    public static final float SIDE_THRUST = 2.5f; // m/s^2 (side jets)

    public static void advance(LanderState lander, double dt) {
        // Track previous vy for vertical acceleration HUD
        float prevVy = lander.vy;
        // Only allow rotation if not using side thrusters and there's fuel
        if (!lander.space && lander.fuelMass > 0) {
            if (lander.left) lander.angle -= ROTATE_SPEED;  // rotate counter-clockwise
            if (lander.right) lander.angle += ROTATE_SPEED; // rotate clockwise
        }
        // Thrust (main engine, now throttle-based)
        if (lander.throttle > 0.0f && lander.fuelMass > 0) {
            float rad = (float)Math.toRadians(lander.angle);
            float mass = lander.getTotalMass();
            float thrust = ENGINE_THRUST * lander.throttle;
            // Calculate acceleration from thrust (F = ma)
            float ax = (float)Math.sin(rad) * thrust / mass;
            float ay = (float)Math.cos(rad) * thrust / mass;
            lander.vx += ax * dt;
            lander.vy += ay * dt;
            // Burn fuel proportional to throttle
            float fuelUsed = FUEL_BURN_RATE * lander.throttle * (float)dt;
            lander.fuelMass -= fuelUsed;
            if (lander.fuelMass < 0) lander.fuelMass = 0;
        }
        // Side thrust (with left/right + space)
        if (lander.space && lander.fuelMass > 0) {
            if (lander.left) {
                lander.vx -= SIDE_THRUST * dt;
                // Optional: burn a small amount of fuel for side jets
                lander.fuelMass -= 0.5f * FUEL_BURN_RATE * (float)dt;
            }
            if (lander.right) {
                lander.vx += SIDE_THRUST * dt;
                lander.fuelMass -= 0.5f * FUEL_BURN_RATE * (float)dt;
            }
            if (lander.fuelMass < 0) lander.fuelMass = 0;
        }
        // Gravity (pulls down)
        lander.vy -= GRAVITY * dt;
        // Update position
        lander.x += lander.vx * dt;
        lander.y += lander.vy * dt;
        // Compute vertical acceleration (rate of change of vy)
        lander.verticalAccel = (lander.vy - prevVy) / (float)dt;
        lander.prevVy = prevVy;
    }
}
