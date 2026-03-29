package com.dsrts.lander;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.MassData;

public class Physics {
    public static final float GRAVITY = 1.62f; // m/s^2 (moon gravity)
    public static final float ENGINE_THRUST = 44000f; // Newtons
    public static final float FUEL_BURN_RATE = 14.5f; // kg/s
    public static final float THROTTLE_CHANGE_RATE = 0.5f; // percent per second
    
    public static final float NACELLE_DIST = 3.5f; 
    public static final float MOMENT_OF_INERTIA = 40000f; 
    public static final float ROTATION_CONTROL_AUTHORITY = 0.2f; 
    public static final float FBW_ANGULAR_DAMPING = 1.2f; 

    public static void advance(LanderState lander, double dt) {
        // --- 1. Control Input (Throttle) ---
        float throttleDelta = THROTTLE_CHANGE_RATE * (float)dt;
        if (lander.up) lander.throttle += throttleDelta;
        if (lander.down) lander.throttle -= throttleDelta;
        lander.throttle = MathUtils.clamp(lander.throttle, 0f, 1f);

        // --- 2. Control Input (Rotation) ---
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
        leftThrust = MathUtils.clamp(leftThrust, -0.5f, 1.0f);
        rightThrust = MathUtils.clamp(rightThrust, -0.5f, 1.0f);
        
        lander.throttleLeft = leftThrust;
        lander.throttleRight = rightThrust;

        // --- 3. Apply Box2D Forces ---
        applyForces(lander, dt);
        
        // --- 4. Sync State from Box2D ---
        syncState(lander, dt);
    }

    public static void flybywire(LanderState lander, double dt) {
        float mass = lander.getTotalMass();
        
        // --- 1. Vertical Control (PID) ---
        float vyError = lander.goalVy - lander.vy;
        float baseThrottle = lander.landed ? 0f : (GRAVITY * mass) / ENGINE_THRUST;
        float Kp_v = 8.0f;
        lander.throttle = baseThrottle + Kp_v * vyError;
        if (vyError > 2.0f) lander.throttle = 1.0f;
        lander.throttle = MathUtils.clamp(lander.throttle, 0f, 1f);

        // --- 2. Attitude Control (PD) ---
        float angleError = lander.goalAngle - lander.angle;
        float Kp_a = 0.05f; 
        float Kd_a = 0.02f; 
        
        float diffCommand = (angleError * Kp_a) - (lander.omega * Kd_a);

        // CCW Positive: Right engine > Left engine -> rotate CCW (positive)
        float leftThrust = lander.throttle - diffCommand;
        float rightThrust = lander.throttle + diffCommand;

        
        leftThrust = MathUtils.clamp(leftThrust, -0.5f, 1.0f);
        rightThrust = MathUtils.clamp(rightThrust, -0.5f, 1.0f);

        lander.throttleLeft = leftThrust;
        lander.throttleRight = rightThrust;

        // --- 3. Apply Box2D Forces ---
        applyForces(lander, dt);
        
        // Apply passive damping in FBW
        lander.body.setAngularVelocity(lander.body.getAngularVelocity() * (1.0f - FBW_ANGULAR_DAMPING * (float)dt));

        // --- 4. Sync State from Box2D ---
        syncState(lander, dt);
    }

    private static void applyForces(LanderState lander, double dt) {
        if (lander.fuelMass <= 0) return;

        // Update Mass Data (fuel consumption)
        MassData massData = new MassData();
        massData.mass = lander.getTotalMass();
        massData.center.set(0, 0);
        massData.I = MOMENT_OF_INERTIA;
        lander.body.setMassData(massData);

        // Calculate forces in local space (Y is up)
        float fL = lander.throttleLeft * (ENGINE_THRUST / 2f);
        float fR = lander.throttleRight * (ENGINE_THRUST / 2f);

        // Apply forces at nacelle positions
        // Left nacelle at (-NACELLE_DIST, 0), force pointing UP (relative to craft)
        Vector2 forceL = lander.body.getWorldVector(new Vector2(0, fL));
        Vector2 posL = lander.body.getWorldPoint(new Vector2(-NACELLE_DIST, 0));
        lander.body.applyForce(forceL, posL, true);

        // Right nacelle at (NACELLE_DIST, 0)
        Vector2 forceR = lander.body.getWorldVector(new Vector2(0, fR));
        Vector2 posR = lander.body.getWorldPoint(new Vector2(NACELLE_DIST, 0));
        lander.body.applyForce(forceR, posR, true);

        // Burn fuel
        float combinedThrottle = (Math.abs(lander.throttleLeft) + Math.abs(lander.throttleRight)) / 2f;
        lander.fuelMass -= FUEL_BURN_RATE * combinedThrottle * (float)dt;
        if (lander.fuelMass < 0) lander.fuelMass = 0;
    }

    private static void syncState(LanderState lander, double dt) {
        Vector2 pos = lander.body.getPosition();
        Vector2 vel = lander.body.getLinearVelocity();
        
        float prevVy = lander.vy;
        
        lander.x = pos.x;
        lander.y = pos.y;
        lander.vx = vel.x;
        lander.vy = vel.y;
        
        // Box2D uses CCW radians. Our game uses CCW degrees (LibGDX standard now)
        lander.angle = lander.body.getAngle() * MathUtils.radiansToDegrees;
        lander.omega = lander.body.getAngularVelocity() * MathUtils.radiansToDegrees;

        // HUD stats
        lander.verticalAccel = (lander.vy - prevVy) / (float)dt;
        lander.prevVy = prevVy;
    }
}
