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
    public static final float ROTATE_SPEED = 20f; // deg/frame
    public static final float SIDE_THRUST = 2.5f; // m/s^2 (side jets)
    public static final float THROTTLE_CHANGE_RATE = 0.5f; // percent per second (0.5 = 50%/sec)
    
    public static void advance(LanderState lander, double dt) {
        // System.out.println("advance");
        // --- Throttle control (manual) ---
        float throttleDelta = THROTTLE_CHANGE_RATE * (float)dt;
        if (lander.up) {
            lander.throttle += throttleDelta;
        }
        if (lander.down) {
            lander.throttle -= throttleDelta;
        }
        if (lander.throttle > 1.0f) lander.throttle = 1.0f;
        if (lander.throttle < 0.0f) lander.throttle = 0.0f;

        // Track previous vy for vertical acceleration HUD
        float prevVy = lander.vy;
        // --- Manual rotation: left/right keys directly control angle ---
        if(!lander.space) {
            if (lander.left) {
                lander.angle -= ROTATE_SPEED * dt;
            }
            if (lander.right) {
                lander.angle += ROTATE_SPEED * dt;
            }
        }
        // Clamp angle to [-180, 180] for sanity (optional)
        if (lander.angle > 180) lander.angle -= 360;
        if (lander.angle < -180) lander.angle += 360;
        // System.out.println("Angle: " + lander.angle);
        
        // Clamp angle to [-180, 180] for sanity (optional)
        if (lander.angle > 180) lander.angle -= 360;
        if (lander.angle < -180) lander.angle += 360;

        // System.out.println("Angle: " + lander.angle);
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

    // Fly-by-wire mode: PID (P-only) control to achieve goalVx and goalVy
    public static void flybywire(LanderState lander, double dt) {
        // System.out.println("flybywire");

        // --- Throttle control (PID for vy) ---
        float vyError = lander.goalVy - lander.vy;
        // Nonlinear P-controller: gain increases with error magnitude
        float baseKp = 2.0f; // tune as needed
        float nonlinearKp = baseKp * (1.0f + Math.abs(vyError));
        lander.throttle += nonlinearKp * vyError * dt;
        if (lander.throttle > 1.0f) lander.throttle = 1.0f;
        if (lander.throttle < 0.0f) lander.throttle = 0.0f;

        // --- Side thrust (PID for vx) ---
        float vxError = lander.goalVx - lander.vx;

        // Use left/right keys to increment goalVx, but here we just control
        // Use side thrusters to minimize vx error
        if (lander.fuelMass > 0) {
            if (vxError > 0.2f) {
                // Need to go right
                lander.right = true;
                lander.left = false;
            } else if (vxError < -0.2f) {
                // Need to go left
                lander.left = true;
                lander.right = false;
            } else {
                lander.left = false;
                lander.right = false;
            }
        }

        // --- Angle control in flybywire: P-controller to keep angle at 0 ---
        float angleError = 0.0f - lander.angle; // target is 0 degrees
        float Kp_angle = 0.2f; // Tune as needed for responsiveness
        float angleCorrection = Kp_angle * angleError;
        lander.angle += angleCorrection * dt; // Adjust angle toward 0

        float prevVy = lander.vy;
        if (lander.throttle > 0.0f && lander.fuelMass > 0) {
            float rad = (float)Math.toRadians(lander.angle);
            float mass = lander.getTotalMass();
            float thrust = ENGINE_THRUST * lander.throttle;
            float ax = (float)Math.sin(rad) * thrust / mass;
            float ay = (float)Math.cos(rad) * thrust / mass;
            lander.vx += ax * dt;
            lander.vy += ay * dt;
            float fuelUsed = FUEL_BURN_RATE * lander.throttle * (float)dt;
            lander.fuelMass -= fuelUsed;
            if (lander.fuelMass < 0) lander.fuelMass = 0;
        }
        if (lander.fuelMass > 0) {
            if (lander.left) {
                lander.vx -= SIDE_THRUST * dt;
                lander.fuelMass -= 0.5f * FUEL_BURN_RATE * (float)dt;
            }
            if (lander.right) {
                lander.vx += SIDE_THRUST * dt;
                lander.fuelMass -= 0.5f * FUEL_BURN_RATE * (float)dt;
            }
            if (lander.fuelMass < 0) lander.fuelMass = 0;
        }
        lander.vy -= GRAVITY * dt;
        lander.x += lander.vx * dt;
        lander.y += lander.vy * dt;
        lander.verticalAccel = (lander.vy - prevVy) / (float)dt;
        lander.prevVy = prevVy;
    }

}
