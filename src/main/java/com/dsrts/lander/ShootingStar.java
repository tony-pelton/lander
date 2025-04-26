package com.dsrts.lander;

import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShootingStar {
    private float x, y;                    // Current position
    private float vx, vy;                  // Velocity
    private float distanceTraveled;        // Track total distance
    private float maxDistance;             // Maximum travel distance
    private boolean active;                // Whether star is currently animating
    private boolean goingRight;            // Direction
    private float size;                    // Size of the star
    private static final float ANGLE = 30f;  // Descent angle in degrees
    private static final float SPEED = 50f; // Meters per second
    private static final int TRAIL_LENGTH = 10;
    private List<TrailParticle> trail;
    private boolean poofing;               // Whether in end animation
    private float poofTime;                // Time in poof animation
    private static final float POOF_DURATION = 0.3f; // Seconds
    
    private static class TrailParticle {
        float x, y;
        float alpha;
        
        TrailParticle(float x, float y, float alpha) {
            this.x = x;
            this.y = y;
            this.alpha = alpha;
        }
    }

    public ShootingStar() {
        this.active = false;
        this.trail = new ArrayList<>();
        this.size = Lander.LANDER_WIDTH_M * 0.4f; // Slightly smaller than lander
    }

    public void spawn(float landerX) {
        Random rand = new Random();
        this.goingRight = rand.nextBoolean();
        this.x = landerX;
        this.y = Lander.WORLD_HEIGHT_M * 0.8f; // Start near top of screen
        
        // Calculate velocities based on angle and direction
        float angleRad = (float)Math.toRadians(ANGLE);
        this.vx = SPEED * (float)Math.cos(angleRad) * (goingRight ? 1 : -1);
        this.vy = -SPEED * (float)Math.sin(angleRad); // Negative because Y increases downward
        
        this.distanceTraveled = 0;
        this.maxDistance = Lander.SCREEN_WIDTH / Lander.PIXELS_PER_METER_X * 0.35f; // 70% of half screen
        this.active = true;
        this.poofing = false;
        this.trail.clear();
    }

    public void update(float dt) {
        if (!active) return;
        
        if (poofing) {
            poofTime += dt;
            if (poofTime >= POOF_DURATION) {
                active = false;
            }
            return;
        }

        // Update position
        float oldX = x;
        float oldY = y;
        x += vx * dt;
        y += vy * dt;
        
        // Update trail
        trail.add(0, new TrailParticle(oldX, oldY, 1.0f));
        if (trail.size() > TRAIL_LENGTH) {
            trail.remove(trail.size() - 1);
        }
        // Fade trail particles
        for (int i = 0; i < trail.size(); i++) {
            trail.get(i).alpha *= 0.9f;
        }

        // Update distance and check if we should start poof
        float dx = x - oldX;
        float dy = y - oldY;
        distanceTraveled += Math.sqrt(dx * dx + dy * dy);
        if (distanceTraveled >= maxDistance) {
            poofing = true;
            poofTime = 0;
        }
    }

    public void render() {
        if (!active) return;

        if (poofing) {
            renderPoof();
            return;
        }

        // Render trail
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        float trailWidth = size * 0.3f;
        for (TrailParticle particle : trail) {
            GL11.glColor4f(1.0f, 1.0f, 0.8f, particle.alpha * 0.3f);
            float px = particle.x * Lander.PIXELS_PER_METER_X;
            float py = Lander.SCREEN_HEIGHT - particle.y * Lander.PIXELS_PER_METER_Y;
            GL11.glVertex2f(px, py - trailWidth);
            GL11.glVertex2f(px, py + trailWidth);
        }
        GL11.glEnd();

        // Render star
        GL11.glColor4f(1.0f, 1.0f, 0.8f, 1.0f);
        float px = x * Lander.PIXELS_PER_METER_X;
        float py = Lander.SCREEN_HEIGHT - y * Lander.PIXELS_PER_METER_Y;
        
        // Draw star shape with 8 points (4 long, 4 short)
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(px, py); // Center point
        
        float longPoint = size * Lander.PIXELS_PER_METER_X;
        float shortPoint = longPoint * 0.4f;
        
        // Draw 8 points alternating between long and short
        for (int i = 0; i <= 8; i++) {
            float angle = (float)(i * Math.PI / 4);
            float radius = (i % 2 == 0) ? longPoint : shortPoint;
            float sx = px + (float)Math.cos(angle) * radius;
            float sy = py + (float)Math.sin(angle) * radius;
            GL11.glVertex2f(sx, sy);
        }
        GL11.glEnd();

        // Add a bright core
        float coreSize = size * 0.3f * Lander.PIXELS_PER_METER_X;
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // Pure white for the core
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(px, py);
        for (int i = 0; i <= 8; i++) {
            float angle = (float)(i * Math.PI / 4);
            float sx = px + (float)Math.cos(angle) * coreSize;
            float sy = py + (float)Math.sin(angle) * coreSize;
            GL11.glVertex2f(sx, sy);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);
    }

    private void renderPoof() {
        float progress = poofTime / POOF_DURATION;
        float alpha = 1.0f - progress;
        float expandedSize = size * (1.0f + progress * 3.0f);
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0f, 1.0f, 0.8f, alpha);
        
        float px = x * Lander.PIXELS_PER_METER_X;
        float py = Lander.SCREEN_HEIGHT - y * Lander.PIXELS_PER_METER_Y;
        
        // Draw expanding star shape for poof
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(px, py);
        
        float longPoint = expandedSize * Lander.PIXELS_PER_METER_X;
        float shortPoint = longPoint * 0.4f;
        
        for (int i = 0; i <= 8; i++) {
            float angle = (float)(i * Math.PI / 4);
            float radius = (i % 2 == 0) ? longPoint : shortPoint;
            float sx = px + (float)Math.cos(angle) * radius;
            float sy = py + (float)Math.sin(angle) * radius;
            GL11.glVertex2f(sx, sy);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_BLEND);
    }

    public boolean isActive() {
        return active;
    }
} 