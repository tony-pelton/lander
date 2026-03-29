package com.dsrts.lander;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Render {
    private ShapeRenderer shapeRenderer;
    private Box2DDebugRenderer debugRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    
    private Viewport worldViewport;
    private Viewport hudViewport;
    private OrthographicCamera worldCamera;
    
    private static final int GALAXY_STAR_COUNT = 30;
    private float[] galaxyStarX = new float[GALAXY_STAR_COUNT];
    private float[] galaxyStarY = new float[GALAXY_STAR_COUNT];
    private int galaxyThird;
    private float galaxyRotation = 0f;
    private static final float GALAXY_ROTATION_SPEED = 10f;
    
    private static final int SMALL_STARS_COUNT = 75;
    private static final int LARGE_STARS_COUNT = 25;
    private float[] smallStarX = new float[SMALL_STARS_COUNT];
    private float[] smallStarY = new float[SMALL_STARS_COUNT];
    private float[] largeStarX = new float[LARGE_STARS_COUNT];
    private float[] largeStarY = new float[LARGE_STARS_COUNT];
    
    private ShootingStar shootingStar = new ShootingStar();
    private float shootingStarTimer = 0f;
    private static final float MIN_STAR_INTERVAL = 10f;
    private static final float MAX_STAR_INTERVAL = 15f;
    private float nextStarTime = MIN_STAR_INTERVAL;

    public void init(Lander landerApp) {
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);
        debugRenderer = new Box2DDebugRenderer();
        batch = new SpriteBatch();
        
        // Font setup using FreeType
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Roboto-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 22;
        parameter.color = Color.WHITE;
        font = generator.generateFont(parameter);
        generator.dispose();
        
        worldCamera = new OrthographicCamera();
        worldViewport = new ExtendViewport(Lander.WORLD_WIDTH_M / 3f, Lander.WORLD_HEIGHT_M, worldCamera);
        hudViewport = new ScreenViewport();
        
        initStars();
    }
    
    private void initStars() {
        galaxyThird = MathUtils.random(2);
        for (int i = 0; i < GALAXY_STAR_COUNT; i++) {
            double angle = Math.random() * 2.0 * Math.PI;
            double rad = (0.5 + Math.random() * 0.5);
            galaxyStarX[i] = (float)Math.cos(angle) * (float)rad;
            galaxyStarY[i] = (float)Math.sin(angle) * (float)rad;
        }

        float minClearance = 10.0f;
        for (int i = 0; i < SMALL_STARS_COUNT; i++) {
            smallStarX[i] = MathUtils.random(Lander.WORLD_WIDTH_M);
            int terrainIndex = (int)(smallStarX[i] * Terrain.terrainHeights.length / Lander.WORLD_WIDTH_M);
            float terrainHeight = Terrain.terrainHeights[terrainIndex];
            float availableHeight = Lander.WORLD_HEIGHT_M - (terrainHeight + minClearance);
            smallStarY[i] = terrainHeight + minClearance + MathUtils.random(availableHeight);
        }

        for (int i = 0; i < LARGE_STARS_COUNT; i++) {
            largeStarX[i] = MathUtils.random(Lander.WORLD_WIDTH_M);
            int terrainIndex = (int)(largeStarX[i] * Terrain.terrainHeights.length / Lander.WORLD_WIDTH_M);
            float terrainHeight = Terrain.terrainHeights[terrainIndex];
            float availableHeight = Lander.WORLD_HEIGHT_M - (terrainHeight + minClearance);
            largeStarY[i] = terrainHeight + minClearance + MathUtils.random(availableHeight);
        }
    }

    public void render(LanderState lander) {
        float camX = lander.x;
        float halfVisibleW = worldViewport.getWorldWidth() / 2f;
        if (camX < halfVisibleW) camX = halfVisibleW;
        if (camX > Lander.WORLD_WIDTH_M - halfVisibleW) camX = Lander.WORLD_WIDTH_M - halfVisibleW;
        
        worldCamera.position.set(camX, Lander.WORLD_HEIGHT_M / 2f, 0);
        worldCamera.update();
        
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // --- 1. World Render ---
        worldViewport.apply();
        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        
        // Stars
        shapeRenderer.begin(ShapeRenderer.ShapeType.Point);
        shapeRenderer.setColor(0.8f, 0.8f, 0.9f, 1f);
        for (int i = 0; i < SMALL_STARS_COUNT; i++) {
            shapeRenderer.point(smallStarX[i], smallStarY[i], 0);
        }
        shapeRenderer.end();
        
        // Dynamic World
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        drawDynamicWorld(lander);
        shapeRenderer.end();
        
        // Debug Physics (Optional overlay)
        // debugRenderer.render(lander.body.getWorld(), worldCamera.combined);
        
        // --- 2. HUD Render ---
        hudViewport.apply();
        shapeRenderer.setProjectionMatrix(hudViewport.getCamera().combined);
        batch.setProjectionMatrix(hudViewport.getCamera().combined);
        
        drawHud(lander);
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    private void drawDynamicWorld(LanderState lander) {
        // Large stars
        shapeRenderer.setColor(0.8f, 0.8f, 0.9f, 1f);
        for (int i = 0; i < LARGE_STARS_COUNT; i++) {
            drawLargeStar(largeStarX[i], largeStarY[i]);
        }
        
        // Shooting Star
        shootingStar.update(Gdx.graphics.getDeltaTime());
        if (shootingStar.isActive()) {
            shootingStar.render(shapeRenderer);
        } else {
            shootingStarTimer += Gdx.graphics.getDeltaTime();
            if (shootingStarTimer >= nextStarTime) {
                shootingStar.spawn(lander.x);
                shootingStarTimer = 0;
                nextStarTime = MIN_STAR_INTERVAL + MathUtils.random(MAX_STAR_INTERVAL - MIN_STAR_INTERVAL);
            }
        }
        
        drawObjects();
        
        // Terrain
        shapeRenderer.setColor(Color.WHITE);
        for (int i = 0; i < Terrain.terrainHeights.length - 1; i++) {
            float x1 = i * Lander.WORLD_WIDTH_M / Terrain.terrainHeights.length;
            float y1 = Terrain.terrainHeights[i];
            float x2 = (i + 1) * Lander.WORLD_WIDTH_M / Terrain.terrainHeights.length;
            float y2 = Terrain.terrainHeights[i + 1];
            shapeRenderer.line(x1, y1, x2, y2);
        }
        
        // Pads
        shapeRenderer.setColor(Color.RED);
        for (int c = 0; c < Terrain.padCenters.length; c++) {
            int start = Terrain.padCenters[c] - Terrain.padWidth / 2;
            int end = Terrain.padCenters[c] + Terrain.padWidth / 2;
            for (int i = start; i < end - 1; i++) {
                float x1 = i * Lander.WORLD_WIDTH_M / Terrain.terrainHeights.length;
                float y1 = Terrain.terrainHeights[i];
                float x2 = (i + 1) * Lander.WORLD_WIDTH_M / Terrain.terrainHeights.length;
                float y2 = Terrain.terrainHeights[i + 1];
                shapeRenderer.line(x1, y1, x2, y2);
            }
        }
        
        renderLander(lander);
    }
    
    private void drawLargeStar(float x, float y) {
        float size = 0.5f;
        shapeRenderer.line(x - size, y, x + size, y);
        shapeRenderer.line(x, y - size, x, y + size);
    }
    
    private void drawObjects() {
        float thirdWidth = Lander.WORLD_WIDTH_M / 3f;
        
        // --- 1. Galaxy ---
        float galaxyX = (galaxyThird * thirdWidth + thirdWidth * 0.3f);
        float galaxyY = 120f;
        float galaxyRadius = 8f;
        
        galaxyRotation += GALAXY_ROTATION_SPEED * Gdx.graphics.getDeltaTime();
        
        Matrix4 oldTransform = shapeRenderer.getTransformMatrix().cpy();
        Matrix4 galaxyTransform = oldTransform.cpy().translate(galaxyX, galaxyY, 0).rotate(0, 0, 1, galaxyRotation);
        shapeRenderer.setTransformMatrix(galaxyTransform);
        
        shapeRenderer.set(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.8f, 0.6f, 1.0f, 1f);
        shapeRenderer.circle(0, 0, galaxyRadius, 20);
        
        shapeRenderer.set(ShapeRenderer.ShapeType.Point);
        shapeRenderer.setColor(Color.WHITE);
        for (int i = 0; i < GALAXY_STAR_COUNT; i++) {
            shapeRenderer.point(galaxyStarX[i] * galaxyRadius, galaxyStarY[i] * galaxyRadius, 0);
        }
        
        shapeRenderer.setTransformMatrix(oldTransform);
        
        // --- 2. Earth ---
        float earthX = (thirdWidth + thirdWidth * 0.7f);
        float earthY = 100f;
        float earthRadius = 4f;
        
        shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.4f, 1.0f, 1f); // Earth blue
        shapeRenderer.circle(earthX, earthY, earthRadius, 20);
        
        // Continents (Green)
        shapeRenderer.setColor(0.1f, 0.7f, 0.2f, 1f);
        
        // Continent 1 (Two triangles)
        shapeRenderer.triangle(
            earthX + earthRadius * 0.2f, earthY - earthRadius * 0.1f,
            earthX + earthRadius * 0.4f, earthY - earthRadius * 0.4f,
            earthX + earthRadius * 0.1f, earthY - earthRadius * 0.5f
        );
        shapeRenderer.triangle(
            earthX + earthRadius * 0.2f, earthY - earthRadius * 0.1f,
            earthX + earthRadius * 0.1f, earthY - earthRadius * 0.5f,
            earthX - earthRadius * 0.1f, earthY - earthRadius * 0.3f
        );
        
        // Continent 2 (Two triangles)
        shapeRenderer.triangle(
            earthX - earthRadius * 0.3f, earthY + earthRadius * 0.1f,
            earthX - earthRadius * 0.2f, earthY + earthRadius * 0.3f,
            earthX, earthY + earthRadius * 0.2f
        );
        shapeRenderer.triangle(
            earthX - earthRadius * 0.3f, earthY + earthRadius * 0.1f,
            earthX, earthY + earthRadius * 0.2f,
            earthX - earthRadius * 0.1f, earthY
        );
        
        shapeRenderer.set(ShapeRenderer.ShapeType.Line);
    }

    private void renderLander(LanderState lander) {
        shapeRenderer.flush();
        Matrix4 oldTransform = shapeRenderer.getTransformMatrix().cpy();
        Matrix4 newTransform = oldTransform.cpy();
        newTransform.translate(lander.x, lander.y, 0);
        // Use angle directly (CCW positive now)
        newTransform.rotate(0, 0, 1, lander.angle);
        shapeRenderer.setTransformMatrix(newTransform);

        if (!lander.alive) shapeRenderer.setColor(Color.RED);
        else if (lander.landed) shapeRenderer.setColor(Color.GREEN);
        else shapeRenderer.setColor(Color.WHITE);

        float bodyW = Lander.LANDER_HALF_W * 1.2f;
        float bodyH = Lander.LANDER_HALF_H * 0.9f;
        
        // --- Central Body (Chassis) ---
        shapeRenderer.rect(-bodyW, -bodyH * 0.5f, bodyW * 2, bodyH);

        // --- Octagon Cab (Command Module) ---
        float cabRadius = bodyW * 0.5f;
        float cabCenterY = bodyH * 0.5f + cabRadius * (float)Math.cos(Math.PI/8);
        for (int i = 0; i < 8; i++) {
            float theta1 = (float)(-Math.PI / 8 + 2.0 * Math.PI * i / 8);
            float theta2 = (float)(-Math.PI / 8 + 2.0 * Math.PI * (i+1) / 8);
            shapeRenderer.line(cabRadius * MathUtils.cos(theta1), cabRadius * MathUtils.sin(theta1) + cabCenterY,
                               cabRadius * MathUtils.cos(theta2), cabRadius * MathUtils.sin(theta2) + cabCenterY);
        }

        // --- Side Nacelles (Thruster Housings) ---
        float nacelleOffset = bodyW * 1.4f;
        float nacelleW = bodyW * 0.4f;
        float nacelleH = bodyH * 1.2f;
        
        // Left Nacelle
        shapeRenderer.rect(-nacelleOffset - nacelleW/2, -nacelleH/2, nacelleW, nacelleH);
        shapeRenderer.line(-nacelleOffset + nacelleW/2, 0, -bodyW, 0); // Strut
        
        // Right Nacelle
        shapeRenderer.rect(nacelleOffset - nacelleW/2, -nacelleH/2, nacelleW, nacelleH);
        shapeRenderer.line(nacelleOffset - nacelleW/2, 0, bodyW, 0); // Strut

        // --- Landing Gear (Legs) ---
        float legLen = bodyH * 1.2f;
        float baseY = -bodyH * 0.5f;
        float legAngle = (float) Math.toRadians(30);
        float sinA = MathUtils.sin(legAngle);
        float cosA = MathUtils.cos(legAngle);

        shapeRenderer.line(-bodyW, baseY, -bodyW - legLen * sinA, baseY - legLen * cosA); // Inner Left
        shapeRenderer.line(bodyW, baseY, bodyW + legLen * sinA, baseY - legLen * cosA);   // Inner Right
        shapeRenderer.line(-nacelleOffset, -nacelleH/2, -nacelleOffset - legLen * 0.5f * sinA, -nacelleH/2 - legLen * 0.8f * cosA); // Outer Left
        shapeRenderer.line(nacelleOffset, -nacelleH/2, nacelleOffset + legLen * 0.5f * sinA, -nacelleH/2 - legLen * 0.8f * cosA);   // Outer Right

        // --- Quad Thruster Flames (Bidirectional) ---
        if (lander.fuelMass > 0 && lander.alive && !lander.landed) {
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            
            // Left Nacelle
            float tL = lander.throttleLeft;
            if (Math.abs(tL) > 0.05f) {
                if (tL > 0) shapeRenderer.setColor(1, 0.6f, 0, 1); // Downward flame
                else shapeRenderer.setColor(0.3f, 0.5f, 1, 0.8f);   // Upward flame (blueish)
                
                float fH = (legLen * 0.2f + Math.abs(tL) * legLen * 0.6f) + MathUtils.random(2f) * Math.abs(tL);
                float sign = tL > 0 ? -1 : 1;
                shapeRenderer.triangle(-nacelleOffset - nacelleW/2, sign * nacelleH/2, 
                                       -nacelleOffset + nacelleW/2, sign * nacelleH/2, 
                                       -nacelleOffset, sign * (nacelleH/2 + fH));
            }
            
            // Right Nacelle
            float tR = lander.throttleRight;
            if (Math.abs(tR) > 0.05f) {
                if (tR > 0) shapeRenderer.setColor(1, 0.6f, 0, 1);
                else shapeRenderer.setColor(0.3f, 0.5f, 1, 0.8f);
                
                float fH = (legLen * 0.2f + Math.abs(tR) * legLen * 0.6f) + MathUtils.random(2f) * Math.abs(tR);
                float sign = tR > 0 ? -1 : 1;
                shapeRenderer.triangle(nacelleOffset - nacelleW/2, sign * nacelleH/2, 
                                       nacelleOffset + nacelleW/2, sign * nacelleH/2, 
                                       nacelleOffset, sign * (nacelleH/2 + fH));
            }
            
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        }

        shapeRenderer.flush();
        shapeRenderer.setTransformMatrix(oldTransform);
    }

    private void drawHud(LanderState lander) {
        float screenW = hudViewport.getWorldWidth();
        float screenH = hudViewport.getWorldHeight();
        
        // 1. Dashboard Background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 1f);
        shapeRenderer.rect(0, 0, screenW, screenH);
        shapeRenderer.end();
        
        // Border Line
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);
        shapeRenderer.line(0, 0, screenW, 0);
        
        // 2. Instruments (Left side)
        float instrY = screenH / 2f;
        drawCircularVVI(80, instrY, (screenH / 2f) - 10, lander.vy, 10.0f, lander.smoothedVerticalAccel, lander.flyByWireMode ? lander.goalVy : null);
        drawVerticalIndicator(160, 10, 18, screenH - 35, "THR", lander.smoothedThrottle, 1.0f, false);
        drawVerticalIndicator(230, 10, 18, screenH - 35, "FUEL", lander.fuelMass, LanderState.TOTAL_FUEL_MASS, false);
        shapeRenderer.end();
        
        batch.begin();
        // Instrument Labels
        float vviTextX = 80 - 15;
        float vviTextY = instrY + 25;
        font.draw(batch, "VVI", vviTextX, vviTextY);
        font.draw(batch, "THR", 160 - 15, screenH - 5);
        font.draw(batch, "FUEL", 230 - 20, screenH - 5);
        
        // 3. Control Mode (Center-Left)
        font.setColor(lander.flyByWireMode ? Color.CYAN : Color.YELLOW);
        font.draw(batch, lander.flyByWireMode ? "MODE: FBW" : "MODE: MANUAL", 280, screenH / 2f + 5);
        font.setColor(Color.WHITE);

        // 4. Status Text (Right side, horizontal layout)
        float rightX = screenW - 10;
        float textY = screenH / 2f + 5;
        
        // We draw right-to-left
        String alt = String.format("ALT: %6.1f m", lander.y);
        String goal = lander.flyByWireMode ? 
                      String.format("G-ANG: %3.0f°", lander.goalAngle) : "";
        String vel = String.format("V: %+5.1f / %+5.1f", lander.vx, lander.vy);

        // Calculate positions (approximate widths)
        font.draw(batch, alt, rightX - 150, textY);
        font.draw(batch, goal, rightX - 380, textY);
        font.draw(batch, vel, rightX - 610, textY);
        
        // 5. Endgame Messages (Overlay on HUD or center)
        if (lander.landed) font.draw(batch, "LANDED! Press ESC to quit.", screenW/2f - 100, screenH/2f + 5);
        else if (!lander.alive) font.draw(batch, "CRASHED! Press ESC to quit.", screenW/2f - 100, screenH/2f + 5);
        
        batch.end();
    }

    private void drawVerticalIndicator(float x, float y, float width, float height, String label, float value, float maxValue, boolean isVector) {
        float halfBar = height / 2f;
        float centerY = y + halfBar;
        
        // Outline (Line mode)
        shapeRenderer.set(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x - width / 2, y, width, height);
        
        if (isVector) {
            shapeRenderer.line(x - width / 2 - 2, centerY, x + width / 2 + 2, centerY);
        }
        
        // Fill (Filled mode)
        if (value != 0) {
            float v = MathUtils.clamp(value, isVector ? -maxValue : 0, maxValue);
            
            shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
            if (isVector) {
                if (value > 0) shapeRenderer.setColor(Color.GREEN);
                else shapeRenderer.setColor(Color.RED);
                
                float barH = (v / maxValue) * halfBar;
                if (barH > 0) {
                    shapeRenderer.rect(x - width / 2, centerY, width, barH);
                } else {
                    shapeRenderer.rect(x - width / 2, centerY + barH, width, -barH);
                }
            } else {
                shapeRenderer.setColor(Color.CYAN);
                float barH = (v / maxValue) * height;
                shapeRenderer.rect(x - width / 2, y, width, barH);
            }
        }
    }

    private void drawCircularVVI(float x, float y, float radius, float value, float maxValue, float trend, Float goal) {
        // 1. Gauge Face
        shapeRenderer.set(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(x, y, radius, 30);
        
        // Ticks (every 2 units)
        for (int i = 0; i < 10; i++) {
            float angle = 180 - (i * 360f / 10f); // 0.0 is at 180 degrees (Left)
            float innerR = radius - 5;
            shapeRenderer.line(
                x + MathUtils.cosDeg(angle) * innerR, y + MathUtils.sinDeg(angle) * innerR,
                x + MathUtils.cosDeg(angle) * radius, y + MathUtils.sinDeg(angle) * radius
            );
        }

        // 2. Trend Ribbon (Outer edge)
        if (Math.abs(trend) > 0.01f) {
            shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
            if (trend > 0) shapeRenderer.setColor(0, 1, 0, 0.5f);
            else shapeRenderer.setColor(1, 0, 0, 0.5f);
            
            float startAngle = 180; // 0.0 is at 180
            float sweep = -(trend / 2.0f) * 180f; // Scale 2m/s^2 to 180 degrees
            shapeRenderer.arc(x, y, radius + 4, startAngle, sweep, 20);
        }

        // 3. Goal Caret (Inside edge)
        if (goal != null) {
            shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.CYAN);
            float gAngle = 180 - (goal / maxValue) * 180f;
            float rInner = radius - 8;
            shapeRenderer.triangle(
                x + MathUtils.cosDeg(gAngle) * radius, y + MathUtils.sinDeg(gAngle) * radius,
                x + MathUtils.cosDeg(gAngle + 5) * rInner, y + MathUtils.sinDeg(gAngle + 5) * rInner,
                x + MathUtils.cosDeg(gAngle - 5) * rInner, y + MathUtils.sinDeg(gAngle - 5) * rInner
            );
        }

        // 4. Needle
        shapeRenderer.set(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.YELLOW);
        float needleAngle = 180 - (value / maxValue) * 180f; // 0 is 180, +maxValue is CCW
        shapeRenderer.line(x, y, x + MathUtils.cosDeg(needleAngle) * (radius - 2), y + MathUtils.sinDeg(needleAngle) * (radius - 2));
        
        // Center cap
        shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(x, y, 3);
    }

    public void resize(int width, int height) {
        int hudHeight = (int)(height * 0.2f);
        int worldHeight = height - hudHeight;
        
        // World Viewport: Bottom 80%
        worldViewport.update(width, worldHeight, true);
        worldViewport.setScreenBounds(0, 0, width, worldHeight);
        
        // HUD Viewport: Top 20%
        hudViewport.update(width, hudHeight, true);
        hudViewport.setScreenBounds(0, worldHeight, width, hudHeight);
    }

    public void dispose() {
        shapeRenderer.dispose();
        if (debugRenderer != null) debugRenderer.dispose();
        batch.dispose();
        font.dispose();
    }
}
