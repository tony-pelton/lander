package com.dsrts.lander;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.ScreenUtils;

public class Lander extends ApplicationAdapter {
    // --- METRIC SCALE CONSTANTS ---
    static final float WORLD_WIDTH_M = 750f;
    static final float WORLD_HEIGHT_M = 150f;
    static final int SCREEN_WIDTH = 1280;
    static final int SCREEN_HEIGHT = 768;

    static final float LANDER_WIDTH_M = 5f;
    static final float LANDER_HEIGHT_M = 5f;
    static final float LANDER_HALF_W = LANDER_WIDTH_M / 2;
    static final float LANDER_HALF_H = LANDER_HEIGHT_M / 2;

    private LanderState lander;
    private Render renderer;
    private World world;
    private double accumulator = 0.0;
    private final double dt = 1.0 / 60.0;

    @Override
    public void create() {
        Terrain.generateTerrain();
        
        // Initialize Box2D World with Moon Gravity
        world = new World(new Vector2(0, -1.62f), true);
        
        lander = new LanderState(Terrain.terrainHeights, WORLD_WIDTH_M, WORLD_HEIGHT_M, LANDER_WIDTH_M,
                LANDER_HEIGHT_M, LANDER_HALF_W, LANDER_HALF_H);
        
        // Create Lander Body
        lander.body = createLanderBody(world, lander.x, lander.y, lander.angle);
        
        renderer = new Render();
        renderer.init(this);
    }

    private Body createLanderBody(World world, float x, float y, float angle) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        bodyDef.angle = -angle * MathUtils.degreesToRadians;
        bodyDef.linearDamping = 0.0f;
        bodyDef.angularDamping = 0.0f; 

        Body body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(LANDER_HALF_W, LANDER_HALF_H);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1.0f; 
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0.1f;

        body.createFixture(fixtureDef);
        shape.dispose();

        return body;
    }

    @Override
    public void render() {
        // --- Input Polling ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        lander.up = Gdx.input.isKeyPressed(Input.Keys.UP);
        lander.down = Gdx.input.isKeyPressed(Input.Keys.DOWN);
        lander.left = Gdx.input.isKeyPressed(Input.Keys.LEFT);
        lander.right = Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        lander.space = Gdx.input.isKeyPressed(Input.Keys.SPACE);

        // Toggle fly-by-wire mode with A key (on key press, edge detection)
        float GOAL_INCREMENT = 0.5f; // Incremented per tap
        float ANGLE_INCREMENT = 5.0f; // Degrees per tap

        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            lander.flyByWireMode = !lander.flyByWireMode;
            if (lander.flyByWireMode) {
                // Sync goals to actual state when turning FBW ON
                lander.goalVy = lander.vy;
                // Snap goalAngle to the closest lower value divisible by ANGLE_INCREMENT
                // This allows reaching 0.0 even if manual angle is e.g. 13.4
                lander.goalAngle = (float)(Math.floor(lander.angle / ANGLE_INCREMENT) * ANGLE_INCREMENT);
            }
        }

        if (lander.flyByWireMode) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) lander.goalVy += GOAL_INCREMENT;
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) lander.goalVy -= GOAL_INCREMENT;
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) lander.goalAngle += ANGLE_INCREMENT;
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) lander.goalAngle -= ANGLE_INCREMENT;
        } else {
            // Manual mode: keep goals updated for potential future FBW entry
            lander.goalVx = lander.vx;
            lander.goalVy = lander.vy;
            lander.goalAngle = lander.angle;
        }

        // --- Physics (Fixed Timestep) ---
        accumulator += Gdx.graphics.getDeltaTime();
        while (accumulator >= dt) {
            if (lander.alive) {
                // Step Box2D World
                world.step((float)dt, 6, 2);
                
                if (lander.flyByWireMode) {
                    Physics.flybywire(lander, dt);
                } else {
                    Physics.advance(lander, dt);
                }
                Collision.collision(lander);
            }
            accumulator -= dt;
        }

        // --- Render ---
        ScreenUtils.clear(0.05f, 0.05f, 0.08f, 1.0f);
        renderer.render(lander);
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void dispose() {
        renderer.dispose();
        if (world != null) world.dispose();
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Lander - LibGDX Port");
        config.setWindowedMode(SCREEN_WIDTH, SCREEN_HEIGHT);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new Lander(), config);
    }
}
