package com.dsrts.lander;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShootingStar {
    private float x, y;
    private float vx, vy;
    private float distanceTraveled;
    private float maxDistance;
    private boolean active;
    private boolean goingRight;
    private float size;
    private static final float ANGLE = 30f;
    private static final float SPEED = 50f;
    private static final int TRAIL_LENGTH = 10;
    private List<TrailParticle> trail;
    private boolean poofing;
    private float poofTime;
    private static final float POOF_DURATION = 0.3f;
    
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
        this.size = 1.0f; // 1 meter
    }

    public void spawn(float landerX) {
        Random rand = new Random();
        this.goingRight = rand.nextBoolean();
        this.x = landerX;
        this.y = Lander.WORLD_HEIGHT_M * 0.8f;
        
        float angleRad = MathUtils.degreesToRadians * ANGLE;
        this.vx = SPEED * MathUtils.cos(angleRad) * (goingRight ? 1 : -1);
        this.vy = -SPEED * MathUtils.sin(angleRad);
        
        this.distanceTraveled = 0;
        this.maxDistance = 60f; // 60 meters travel
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

        float oldX = x;
        float oldY = y;
        x += vx * dt;
        y += vy * dt;
        
        trail.add(0, new TrailParticle(oldX, oldY, 1.0f));
        if (trail.size() > TRAIL_LENGTH) {
            trail.remove(trail.size() - 1);
        }
        for (TrailParticle particle : trail) {
            particle.alpha *= 0.85f;
        }

        float dx = x - oldX;
        float dy = y - oldY;
        distanceTraveled += Math.sqrt(dx * dx + dy * dy);
        if (distanceTraveled >= maxDistance) {
            poofing = true;
            poofTime = 0;
        }
    }

    public void render(ShapeRenderer shapeRenderer) {
        if (!active) return;

        // Render Trail
        shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
        float trailWidth = size * 0.3f;
        for (TrailParticle p : trail) {
            shapeRenderer.setColor(1.0f, 1.0f, 0.8f, p.alpha * 0.4f);
            shapeRenderer.circle(p.x, p.y, trailWidth, 6);
        }

        if (poofing) {
            renderPoof(shapeRenderer);
            return;
        }

        // Render Star shape (8 points)
        shapeRenderer.setColor(1.0f, 1.0f, 0.8f, 1.0f);
        float longPoint = size;
        float shortPoint = size * 0.4f;
        
        // Draw 8 points as triangles
        for (int i = 0; i < 8; i++) {
            float angle1 = i * MathUtils.PI / 4f;
            float angle2 = (i + 1) * MathUtils.PI / 4f;
            float r1 = (i % 2 == 0) ? longPoint : shortPoint;
            float r2 = (i % 2 == 0) ? shortPoint : longPoint;
            
            shapeRenderer.triangle(x, y, 
                                   x + MathUtils.cos(angle1) * r1, y + MathUtils.sin(angle1) * r1,
                                   x + MathUtils.cos(angle2) * r2, y + MathUtils.sin(angle2) * r2);
        }

        // Core
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(x, y, size * 0.3f, 8);
        
        shapeRenderer.set(ShapeRenderer.ShapeType.Line);
    }

    private void renderPoof(ShapeRenderer shapeRenderer) {
        float progress = poofTime / POOF_DURATION;
        float alpha = 1.0f - progress;
        float expandedSize = size * (1.0f + progress * 3.0f);
        
        shapeRenderer.setColor(1.0f, 1.0f, 0.8f, alpha);
        
        float longPoint = expandedSize;
        float shortPoint = expandedSize * 0.4f;
        
        for (int i = 0; i < 8; i++) {
            float angle1 = i * MathUtils.PI / 4f;
            float angle2 = (i + 1) * MathUtils.PI / 4f;
            float r1 = (i % 2 == 0) ? longPoint : shortPoint;
            float r2 = (i % 2 == 0) ? shortPoint : longPoint;
            
            shapeRenderer.triangle(x, y, 
                                   x + MathUtils.cos(angle1) * r1, y + MathUtils.sin(angle1) * r1,
                                   x + MathUtils.cos(angle2) * r2, y + MathUtils.sin(angle2) * r2);
        }
        shapeRenderer.set(ShapeRenderer.ShapeType.Line);
    }

    public boolean isActive() {
        return active;
    }
}
