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
        float throttleDelta = THROTTLE_CHANGE_RATE * (float)dt;
        if (lander.up) lander.throttle += throttleDelta;
        if (lander.down) lander.throttle -= throttleDelta;
        lander.throttle = MathUtils.clamp(lander.throttle, 0f, 1f);

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

        applyForces(lander, dt);
        syncState(lander, dt);
    }

    public static void flybywire(LanderState lander, double dt) {
        float mass = lander.getTotalMass();
        
        // --- 1. Vertical Control (High-Smoothing PID) ---
        float rawVyError = lander.goalVy - lander.vy;
        
        // Error Smoothing (Low Pass on the input signal)
        float errorSmoothing = 0.1f;
        lander.filteredVyError += (rawVyError - lander.filteredVyError) * errorSmoothing;
        
        float cosTheta = MathUtils.cosDeg(lander.angle);
        float baseThrottle = 0f;
        if (!lander.landed) {
            baseThrottle = (GRAVITY * mass) / (ENGINE_THRUST * Math.max(0.1f, Math.abs(cosTheta)));
        }
        
        // PID Gains - Tuned for smoothed error
        float Kp_v = 3.0f; 
        float Ki_v = 1.0f; 
        float Kd_v = 1.5f; 
        
        if (lander.fuelMass > 0 && !lander.landed) {
            // I-Term using filtered error
            lander.integralVyError += lander.filteredVyError * (float)dt;
            lander.integralVyError = MathUtils.clamp(lander.integralVyError, -1.0f, 1.0f);
            
            // D-Term Smoothing
            float rawDerivative = -lander.verticalAccel;
            float dSmoothing = 0.05f; // Even more smoothing for D
            lander.derivativeVyError += (rawDerivative - lander.derivativeVyError) * dSmoothing;
        } else {
            lander.integralVyError = 0;
            lander.derivativeVyError = 0;
            lander.filteredVyError = 0;
        }
        
        // Non-linear P-Term: Squares the error to make it ultra-soft near zero
        float err = lander.filteredVyError;
        float pTerm = Math.signum(err) * (err * err) * Kp_v;
        // If error is large (> 1.0), fall back to linear to maintain authority
        if (Math.abs(err) > 1.0f) pTerm = err * Kp_v;
        
        float iTerm = lander.integralVyError * Ki_v;
        float dTerm = lander.derivativeVyError * Kd_v;
        
        lander.throttle = baseThrottle + pTerm + iTerm + dTerm;
        lander.throttle = MathUtils.clamp(lander.throttle, 0f, 1f);

        // --- 2. Attitude Control (PD) ---
        float angleError = lander.goalAngle - lander.angle;
        float Kp_a = 0.05f; 
        float Kd_a = 0.02f; 
        float diffCommand = (angleError * Kp_a) - (lander.omega * Kd_a);
        
        float leftThrust = lander.throttle - diffCommand;
        float rightThrust = lander.throttle + diffCommand;
        lander.throttleLeft = MathUtils.clamp(leftThrust, -0.5f, 1.0f);
        lander.throttleRight = MathUtils.clamp(rightThrust, -0.5f, 1.0f);

        applyForces(lander, dt);
        lander.body.setAngularVelocity(lander.body.getAngularVelocity() * (1.0f - FBW_ANGULAR_DAMPING * (float)dt));
        syncState(lander, dt);
    }

    private static void applyForces(LanderState lander, double dt) {
        if (lander.fuelMass <= 0) return;

        MassData massData = new MassData();
        massData.mass = lander.getTotalMass();
        massData.center.set(0, 0);
        massData.I = MOMENT_OF_INERTIA;
        lander.body.setMassData(massData);

        float fL = lander.throttleLeft * (ENGINE_THRUST / 2f);
        float fR = lander.throttleRight * (ENGINE_THRUST / 2f);

        Vector2 forceL = lander.body.getWorldVector(new Vector2(0, fL));
        Vector2 posL = lander.body.getWorldPoint(new Vector2(-NACELLE_DIST, 0));
        lander.body.applyForce(forceL, posL, true);

        Vector2 forceR = lander.body.getWorldVector(new Vector2(0, fR));
        Vector2 posR = lander.body.getWorldPoint(new Vector2(NACELLE_DIST, 0));
        lander.body.applyForce(forceR, posR, true);

        float combinedThrottle = (Math.abs(lander.throttleLeft) + Math.abs(lander.throttleRight)) / 2f;
        lander.fuelMass -= FUEL_BURN_RATE * combinedThrottle * (float)dt;
        if (lander.fuelMass < 0) lander.fuelMass = 0;
    }

    private static void syncState(LanderState lander, double dt) {
        Vector2 pos = lander.body.getPosition();
        Vector2 vel = lander.body.getLinearVelocity();
        float prevVy = lander.vy;
        lander.x = pos.x; lander.y = pos.y;
        lander.vx = vel.x; lander.vy = vel.y;
        lander.angle = lander.body.getAngle() * MathUtils.radiansToDegrees;
        lander.omega = lander.body.getAngularVelocity() * MathUtils.radiansToDegrees;
        lander.verticalAccel = (lander.vy - prevVy) / (float)dt;
        lander.prevVy = prevVy;
        
        float smoothingFactor = 0.01f; 
        lander.smoothedVerticalAccel += (lander.verticalAccel - lander.smoothedVerticalAccel) * smoothingFactor;
        lander.smoothedThrottle += (lander.throttle - lander.smoothedThrottle) * smoothingFactor;
    }
}
