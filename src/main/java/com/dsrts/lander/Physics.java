package com.dsrts.lander;

import com.badlogic.gdx.math.MathUtils;

public class Physics {
    /**
     * Advances the lander's physics by dt seconds, applying controls and updating state.
     * @param lander The lander state to update
     * @param dt Time step in seconds
     */
    public static final float GRAVITY = 1.62f; // m/s^2 (moon gravity)
    public static final float ENGINE_THRUST = 44000f; // Newtons
    public static final float FUEL_BURN_RATE = 14.5f; // kg/s
    public static final float THROTTLE_CHANGE_RATE = 0.5f; // percent per second (0.5 = 50%/sec)
    
    // --- New Constants for Drone Physics ---
    public static final float NACELLE_DIST = 3.5f; // Meters from center
    public static final float MOMENT_OF_INERTIA = 40000f; // Resistance to rotation (kg*m^2)
    public static final float ROTATION_CONTROL_AUTHORITY = 0.2f; // Differential throttle strength
    public static final float FBW_ANGULAR_DAMPING = 1.2f; // Passive roll rate decay in FBW mode
    
    public static void advance(LanderState lander, double dt) {
        // --- Throttle control (manual) ---
        float throttleDelta = THROTTLE_CHANGE_RATE * (float)dt;
        if (lander.up) {
            lander.throttle += throttleDelta;
        }
        if (lander.down) {
            lander.throttle -= throttleDelta;
        }
        lander.throttle = MathUtils.clamp(lander.throttle, 0f, 1f);

        // --- Differential Thrust (Manual Rotation) ---
        float leftThrust = lander.throttle;
        float rightThrust = lander.throttle;
        
        if (lander.left) {
            leftThrust -= ROTATION_CONTROL_AUTHORITY;
            rightThrust += ROTATION_CONTROL_AUTHORITY;
        }
        if (lander.right) {
            leftThrust += ROTATION_CONTROL_AUTHORITY;
            rightThrust -= ROTATION_CONTROL_AUTHORITY;
        }
        
        // Clamp individual throttles (allowing bi-directional -1.0 to 1.0 eventually, 
        // but for now 0.0 to 1.0 is safer for fuel)
        leftThrust = MathUtils.clamp(leftThrust, -0.5f, 1.0f);
        rightThrust = MathUtils.clamp(rightThrust, -0.5f, 1.0f);
        
        lander.throttleLeft = leftThrust;
        lander.throttleRight = rightThrust;
        
        // --- Calculate Torque and Forces ---
        float mass = lander.getTotalMass();
        float fLeft = leftThrust * (ENGINE_THRUST / 2f);
        float fRight = rightThrust * (ENGINE_THRUST / 2f);
        
        // Torque = (F_left - F_right) * d
        // F_left > F_right -> Positive Torque -> Clockwise (Increasing Angle)
        float torque = (fLeft - fRight) * NACELLE_DIST;
        
        // Angular Acceleration (alpha = torque / I)
        // Note: angle is in degrees, so convert torque/I (rad/s^2) to degrees/s^2
        float alpha = (torque / MOMENT_OF_INERTIA) * MathUtils.radiansToDegrees;
        
        // Update Angular Velocity and Angle
        lander.omega += alpha * dt;
        lander.angle += lander.omega * dt;
        
        // Clamp angle to [-180, 180]
        if (lander.angle > 180) lander.angle -= 360;
        if (lander.angle < -180) lander.angle += 360;

        // --- Linear Physics ---
        float prevVy = lander.vy;
        float totalThrust = fLeft + fRight;
        
        if (lander.fuelMass > 0) {
            float rad = lander.angle * MathUtils.degreesToRadians;
            // Acceleration from thrust (F = ma)
            float ax = MathUtils.sin(rad) * totalThrust / mass;
            float ay = MathUtils.cos(rad) * totalThrust / mass;
            lander.vx += ax * dt;
            lander.vy += ay * dt;
            
            // Fuel consumption based on absolute thrust used
            float combinedThrottle = (Math.abs(leftThrust) + Math.abs(rightThrust)) / 2f;
            float fuelUsed = FUEL_BURN_RATE * combinedThrottle * (float)dt;
            lander.fuelMass -= fuelUsed;
            if (lander.fuelMass < 0) lander.fuelMass = 0;
        }
        
        // Gravity
        lander.vy -= GRAVITY * dt;
        
        // Update position
        lander.x += lander.vx * dt;
        lander.y += lander.vy * dt;
        
        // HUD stats
        lander.verticalAccel = (lander.vy - prevVy) / (float)dt;
        lander.prevVy = prevVy;
    }

    public static void flybywire(LanderState lander, double dt) {
        float mass = lander.getTotalMass();
        
        // --- 1. Vertical Control (PID for throttle) ---
        float vyError = lander.goalVy - lander.vy;
        float baseThrottle = lander.landed ? 0f : (GRAVITY * mass) / ENGINE_THRUST;
        float Kp_v = 8.0f;
        lander.throttle = baseThrottle + Kp_v * vyError;
        if (vyError > 2.0f) lander.throttle = 1.0f;
        lander.throttle = MathUtils.clamp(lander.throttle, 0f, 1f);

        // --- 3. Attitude Control (PD Attitude Hold) ---
        // We use differential thrust to reach and hold lander.goalAngle
        float angleError = lander.goalAngle - lander.angle;
        float Kp_a = 0.05f; // Gain for angle correction
        float Kd_a = 0.02f; // Gain for angular velocity damping
        
        float diffCommand = (angleError * Kp_a) - (lander.omega * Kd_a);
        
        float leftThrust = lander.throttle + diffCommand;
        float rightThrust = lander.throttle - diffCommand;
        
        leftThrust = MathUtils.clamp(leftThrust, -0.5f, 1.0f);
        rightThrust = MathUtils.clamp(rightThrust, -0.5f, 1.0f);

        lander.throttleLeft = leftThrust;
        lander.throttleRight = rightThrust;

        // --- 4. Physics Integration ---
        float fLeft = leftThrust * (ENGINE_THRUST / 2f);
        float fRight = rightThrust * (ENGINE_THRUST / 2f);
        // Torque = (F_left - F_right) * d
        float torque = (fLeft - fRight) * NACELLE_DIST;
        float alpha = (torque / MOMENT_OF_INERTIA) * MathUtils.radiansToDegrees;
        
        lander.omega += alpha * dt;
        // Apply passive damping to roll rate
        lander.omega *= (1.0f - FBW_ANGULAR_DAMPING * (float)dt);
        
        lander.angle += lander.omega * dt;
        
        float prevVy = lander.vy;
        float totalThrust = fLeft + fRight;
        if (lander.fuelMass > 0) {
            float rad = lander.angle * MathUtils.degreesToRadians;
            float ax = MathUtils.sin(rad) * totalThrust / mass;
            float ay = MathUtils.cos(rad) * totalThrust / mass;
            lander.vx += ax * dt;
            lander.vy += ay * dt;
            float combinedThrottle = (Math.abs(leftThrust) + Math.abs(rightThrust)) / 2f;
            lander.fuelMass -= FUEL_BURN_RATE * combinedThrottle * (float)dt;
            if (lander.fuelMass < 0) lander.fuelMass = 0;
        }
        
        lander.vy -= GRAVITY * dt;
        lander.x += lander.vx * dt;
        lander.y += lander.vy * dt;
        lander.verticalAccel = (lander.vy - prevVy) / (float)dt;
        lander.prevVy = prevVy;
    }
}
