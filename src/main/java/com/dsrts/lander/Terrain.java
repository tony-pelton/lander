package com.dsrts.lander;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;

public class Terrain {
    // Terrain: 4800 samples
    public static float[] terrainHeights = new float[4800];

    public static int[] padCenters = new int[] {
        4800 / 7,
        4800 * 2 / 7,
        4800 * 3 / 7,
        4800 * 4 / 7,
        4800 * 5 / 7,
        4800 * 6 / 7
    };

    public static int padWidth = 100; 

    public static Body terrainBody;

    public static void generateTerrain(World world, float WORLD_WIDTH_M) {
        double base = 10; 
        int n = terrainHeights.length;
        int padCount = padCenters.length;

        // --- 1. Procedural Generation (Internal array) ---
        int leftPadStart = padCenters[0] - padWidth / 2;
        for (int i = 0; i < leftPadStart; i++) {
            terrainHeights[i] = (float) base;
        }

        for (int p = 0; p < padCount - 1; p++) {
            int padAEnd = padCenters[p] + padWidth / 2;
            int padBStart = padCenters[p+1] - padWidth / 2;
            int regionLen = padBStart - padAEnd;
            int mid = regionLen / 2;
            double peak = base + Math.random() * 50 + 20;
            for (int i = 0; i <= mid; i++) {
                int idx = padAEnd + i;
                terrainHeights[idx] = (float)(base + ((double)i / mid) * (peak - base));
            }
            for (int i = mid + 1; i < regionLen; i++) {
                int idx = padAEnd + i;
                terrainHeights[idx] = (float)(peak + ((double)(i - mid) / (regionLen - 1 - mid)) * (base - peak));
            }
        }

        int rightPadEnd = padCenters[padCount-1] + padWidth / 2;
        for (int i = rightPadEnd; i < n; i++) {
            terrainHeights[i] = (float) base;
        }

        for (int c = 0; c < padCenters.length; c++) {
            int start = padCenters[c] - padWidth / 2;
            int end = padCenters[c] + padWidth / 2;
            for (int i = Math.max(0, start); i < Math.min(n, end); i++) {
                terrainHeights[i] = (float) base;
            }
        }

        // --- 2. Box2D Body Creation ---
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        terrainBody = world.createBody(bodyDef);

        // Main Surface Chain
        Vector2[] vertices = new Vector2[n];
        for (int i = 0; i < n; i++) {
            float x = (float)i * WORLD_WIDTH_M / (float)n;
            vertices[i] = new Vector2(x, terrainHeights[i]);
        }
        ChainShape chain = new ChainShape();
        chain.createChain(vertices);
        
        FixtureDef surfaceDef = new FixtureDef();
        surfaceDef.shape = chain;
        surfaceDef.friction = 0.8f;
        terrainBody.createFixture(surfaceDef).setUserData("terrain");
        chain.dispose();

        // Landing Pad Fixtures (Sensors/Markers)
        for (int c = 0; c < padCount; c++) {
            int startIdx = padCenters[c] - padWidth / 2;
            int endIdx = padCenters[c] + padWidth / 2;
            float xStart = (float)startIdx * WORLD_WIDTH_M / (float)n;
            float xEnd = (float)endIdx * WORLD_WIDTH_M / (float)n;
            
            ChainShape padChain = new ChainShape();
            padChain.createChain(new Vector2[]{new Vector2(xStart, (float)base + 0.05f), new Vector2(xEnd, (float)base + 0.05f)});
            
            FixtureDef padDef = new FixtureDef();
            padDef.shape = padChain;
            padDef.isSensor = true; // Doesn't affect physics, just for detection
            terrainBody.createFixture(padDef).setUserData("pad");
            padChain.dispose();
        }
    }
}
