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
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Render {
    private ShapeRenderer shapeRenderer;
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
        // Negate angle for clockwise physics -> counter-clockwise GDX
        newTransform.rotate(0, 0, 1, -lander.angle);
        shapeRenderer.setTransformMatrix(newTransform);

        if (!lander.alive) shapeRenderer.setColor(Color.RED);
        else if (lander.landed) shapeRenderer.setColor(Color.GREEN);
        else shapeRenderer.setColor(Color.WHITE);

        float bodyW = Lander.LANDER_HALF_W * 1.2f;
        float bodyH = Lander.LANDER_HALF_H * 0.9f;
        
        // Body
        shapeRenderer.rect(-bodyW, -bodyH * 0.5f, bodyW * 2, bodyH);

        // Cab
        float cabRadius = bodyW * 0.5f;
        float cabCenterY = bodyH * 0.5f + cabRadius * (float)Math.cos(Math.PI/8);
        for (int i = 0; i < 8; i++) {
            float theta1 = (float)(-Math.PI / 8 + 2.0 * Math.PI * i / 8);
            float theta2 = (float)(-Math.PI / 8 + 2.0 * Math.PI * (i+1) / 8);
            shapeRenderer.line(cabRadius * MathUtils.cos(theta1), cabRadius * MathUtils.sin(theta1) + cabCenterY,
                               cabRadius * MathUtils.cos(theta2), cabRadius * MathUtils.sin(theta2) + cabCenterY);
        }

        // Legs
        float legLen = bodyH * 1.2f;
        float legSpread = bodyW * 0.8f;
        float baseY = -bodyH * 0.5f;
        float legAngle = (float) Math.toRadians(30);
        float sinA = MathUtils.sin(legAngle);
        float cosA = MathUtils.cos(legAngle);

        shapeRenderer.line(-legSpread, baseY, -legSpread - legLen * sinA, baseY - legLen * cosA);
        shapeRenderer.line(legSpread, baseY, legSpread + legLen * sinA, baseY - legLen * cosA);
        shapeRenderer.line(-legSpread * 0.6f, baseY, -legSpread * 0.6f - legLen * 0.5f * sinA, baseY - legLen * 0.7f * cosA);
        shapeRenderer.line(legSpread * 0.6f, baseY, legSpread * 0.6f + legLen * 0.5f * sinA, baseY - legLen * 0.7f * cosA);

        // Flames & RCS
        if (lander.fuelMass > 0 && lander.alive && !lander.landed) {
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            
            if (lander.throttle > 0.0f) {
                shapeRenderer.setColor(1, 0.6f, 0, 1);
                float fH = (legLen * 0.2f + lander.throttle * legLen * 0.6f) + MathUtils.random(2f) * lander.throttle;
                shapeRenderer.triangle(-bodyW * 0.4f, baseY, bodyW * 0.4f, baseY, 0, baseY - fH);
            }
            
            shapeRenderer.setColor(1, 1, 1, 0.6f);
            float gasL = bodyW * (1.3f + MathUtils.random(0.4f));
            float gasW = bodyH * (0.3f + MathUtils.random(0.2f));
            if (lander.left) shapeRenderer.triangle(bodyW, 0, bodyW + gasL, -gasW, bodyW + gasL, gasW);
            if (lander.right) shapeRenderer.triangle(-bodyW, 0, -bodyW - gasL, -gasW, -bodyW - gasL, gasW);
            
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        }

        shapeRenderer.flush();
        shapeRenderer.setTransformMatrix(oldTransform);
    }

    private void drawHud(LanderState lander) {
        float screenW = hudViewport.getWorldWidth();
        float screenH = hudViewport.getWorldHeight();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        drawVerticalIndicator(50, screenH - 150, 18, 100, "dVy", lander.verticalAccel, 2.0f);
        drawVerticalIndicator(120, screenH - 150, 18, 100, "Vy", lander.vy, 50.0f);
        shapeRenderer.end();
        
        batch.begin();
        // Labels for indicators
        font.draw(batch, "dVy", 50 - 15, screenH - 150 + 125);
        font.draw(batch, "Vy", 120 - 10, screenH - 150 + 125);

        float textX = screenW - 250;
        float textY = screenH - 40;
        float lineH = 25;
        
        font.draw(batch, String.format("Vy: %6.2f m/s", lander.vy), textX, textY);
        font.draw(batch, String.format("Vx: %6.2f m/s", lander.vx), textX, textY - lineH);
        font.draw(batch, String.format("Goal Vy: %6.2f m/s", lander.goalVy), textX, textY - 2 * lineH);
        font.draw(batch, String.format("Goal Vx: %6.2f m/s", lander.goalVx), textX, textY - 3 * lineH);
        font.draw(batch, String.format("Altitude: %6.2f m", lander.y), textX, textY - 4 * lineH);
        font.draw(batch, String.format("Fuel: %6.1f kg", lander.fuelMass), textX, textY - 5 * lineH);
        font.draw(batch, String.format("Throttle: %3d%%", (int)(lander.throttle * 100)), textX, textY - 6 * lineH);
        
        if (lander.landed) font.draw(batch, "LANDED! Press ESC to quit.", screenW/2 - 100, screenH/2);
        else if (!lander.alive) font.draw(batch, "CRASHED! Press ESC to quit.", screenW/2 - 100, screenH/2);
        batch.end();
    }

    private void drawVerticalIndicator(float x, float y, float width, float height, String label, float value, float maxValue) {
        float halfBar = height / 2f;
        float centerY = y + halfBar;
        
        // Outline (Line mode)
        shapeRenderer.set(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x - width / 2, y, width, height);
        shapeRenderer.line(x - width / 2 - 2, centerY, x + width / 2 + 2, centerY);
        
        // Fill (Filled mode)
        if (value != 0) {
            float v = MathUtils.clamp(value, -maxValue, maxValue);
            float barH = (v / maxValue) * halfBar;
            
            shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
            if (value > 0) shapeRenderer.setColor(Color.GREEN);
            else shapeRenderer.setColor(Color.RED);
            
            if (barH > 0) {
                shapeRenderer.rect(x - width / 2, centerY, width, barH);
            } else {
                shapeRenderer.rect(x - width / 2, centerY + barH, width, -barH);
            }
        }
    }

    public void resize(int width, int height) {
        worldViewport.update(width, height);
        hudViewport.update(width, height, true);
    }

    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
    }
}
