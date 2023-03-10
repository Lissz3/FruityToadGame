package com.isabelrosado.fruitytoad.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.isabelrosado.fruitytoad.FruityToad;
import com.isabelrosado.fruitytoad.Scenes.HUD;
import com.isabelrosado.fruitytoad.Sprites.Enemies.FatBird;
import com.isabelrosado.fruitytoad.Sprites.Frog;
import com.isabelrosado.fruitytoad.Sprites.Items.Fruit;
import com.isabelrosado.fruitytoad.Sprites.Items.Item;
import com.isabelrosado.fruitytoad.Sprites.Items.ItemDef;
import com.isabelrosado.fruitytoad.Tools.MyInputProcessor;
import com.isabelrosado.fruitytoad.Tools.WorldContactListener;
import com.isabelrosado.fruitytoad.Tools.WorldCreator;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Screen used to play the game.
 */
public class PlayScreen implements Screen {
    /**
     * Main screen.
     */
    private FruityToad newGame;

    /**
     * Textures used in the screen.
     */
    private TextureAtlas atlas;

    /**
     * Camera with orthographic projection.
     */
    private OrthographicCamera gameCam;

    /**
     * Determines how world coordinates are mapped to and from the screen.
     * <p>Also manages the {@link #gameCam}.</p>
     */
    private Viewport gamePort;

    /**
     * Game UI.
     * @see HUD
     */
    private HUD hud;

    /**
     * Map loaded.
     */
    private TiledMap map;

    /**
     * Responsible to draw the map in the scene.
     */
    private OrthogonalTiledMapRenderer renderer;

    /**
     * Manager for physic entities and dynamic simulation.
     */
    private World world;

    /**
     * Debugger for simple shapes to visualize Box2D World.
     */
    private Box2DDebugRenderer b2dr;

    /**
     * Tool to create or add the body fixtures of every item of the game map.
     *
     * @see WorldCreator
     */
    private WorldCreator creator;

    /**
     * Music to play.
     */
    private Music music;

    /**
     * Main character.
     */
    public Frog frog;

    /**
     * Items to spawn
     */
    private Array<Item> items;

    /**
     * Items spawn in the order you hit the boxes.
     */
    private LinkedBlockingQueue<ItemDef> itemsToSpawn;

    /**
     * Actual game level.
     */
    private int gameLevel;

    boolean gyroscopeAvail;

    /**
     * <p>Creates a play screen with the given values.</p>
     *
     * @param game  main screen
     * @param level game level
     */
    public PlayScreen(FruityToad game, int level) {
        atlas = new TextureAtlas("Sprites/Frog.atlas");
        newGame = game;
        gameLevel = level;

        gyroscopeAvail = Gdx.input.isPeripheralAvailable(Input.Peripheral.Gyroscope);

        //follows the Frog through world
        gameCam = new OrthographicCamera();

        //maintains the virtual aspect ratio despite screen size
        gamePort = new FitViewport(FruityToad.V_WIDTH / FruityToad.PIXEL_PER_METER, FruityToad.V_HEIGHT / FruityToad.PIXEL_PER_METER, gameCam);

        //load map and renderer setup
        TmxMapLoader mapLoader = new TmxMapLoader();
        switch (gameLevel) {
            case 2:
                map = mapLoader.load("Maps/nivel2.tmx");
                break;
            default:
            case 1:
                map = mapLoader.load("Maps/nivel1.tmx");
                break;
        }

        renderer = new OrthogonalTiledMapRenderer(map, 1 / FruityToad.PIXEL_PER_METER);

        //setup the gamecam at the start of the game
        gameCam.position.set(gamePort.getWorldWidth(), gamePort.getWorldHeight() / 2, 0);

        world = new World(new Vector2(0, -10), true);
        b2dr = new Box2DDebugRenderer();

        creator = new WorldCreator(newGame, this);

        //create Frog in our world
        frog = new Frog(newGame, this);

        //create the HUD
        hud = new HUD(game, frog, gameLevel);

        //create Items in our world
        items = new Array<Item>();
        itemsToSpawn = new LinkedBlockingQueue<ItemDef>();

        world.setContactListener(new WorldContactListener());

        music = game.getAssetManager().get("Audio/Music/Gameplay.mp3", Music.class);
        music.setLooping(true);
        music.setVolume(FruityToad.MUSIC_VOLUME);
        music.play();

        MyInputProcessor processor = new MyInputProcessor(frog);

        InputMultiplexer multiplexer = new InputMultiplexer();

        multiplexer.addProcessor(hud.getHudInput());
        multiplexer.addProcessor(processor);

        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float dt) {
        //update logic
        update(dt);

        //clear game screen
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //map render
        renderer.render();

        //box2D debug renderer
        //b2dr.render(world, gameCam.combined);

        //draw the sprites
        newGame.sprite.setProjectionMatrix(gameCam.combined);
        newGame.sprite.begin();
        frog.draw(newGame.sprite);

        for (FatBird fb : creator.getFatBirds()) {
            fb.draw(newGame.sprite);
        }

        for (Item item : items) {
            item.draw(newGame.sprite);
        }
        newGame.sprite.end();

        //set batch with the HUD
        newGame.sprite.setProjectionMatrix(hud.stg.getCamera().combined);
        hud.stg.draw();

        if (gameOver()) {
            hud.getGameOverScreen().setVisible(true);
            hud.setPaused(true);
            music.stop();
        }


    }

    /**
     * @param width
     * @param height
     */
    @Override
    public void resize(int width, int height) {
        gamePort.update(width, height);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        atlas.dispose();
        map.dispose();
        renderer.dispose();
        world.dispose();
        b2dr.dispose();
        hud.dispose();
        music.stop();
    }


    public void update(float dt) {
        //updates if game is not paused
        if (!hud.isPaused()) {
            //handle item creation
            handleSpawningItems();

            //frame ratio
            world.step(1 / 60f, 6, 2);

            //update the duck sprite
            frog.update(dt);

            //update the fatBird sprite
            for (FatBird fb : creator.getFatBirds()) {
                fb.update(dt);
            }

            //update the item sprite
            for (Item fruit : items) {
                fruit.update(dt);
            }

            //the game cam follows the main char
            if (frog.currentState != Frog.State.HITTED) {
                gameCam.position.x = frog.dBody.getPosition().x;
            }

            //update gamecam with correct coordinates
            gameCam.update();

            //tell render to draw only what camera sees in the world
            renderer.setView(gameCam);
        }

        if (gyroscopeAvail) {
            float gyro = Gdx.input.getGyroscopeX();
//            Gdx.app.log("Gyro x", gyro + "");
        }

        //updates hud
        hud.update();
    }

    public TextureAtlas getAtlas() {
        return atlas;
    }

    public TiledMap getMap() {
        return map;
    }

    public World getWorld() {
        return world;
    }

    public void spawnItem(ItemDef itemDef) {
        itemsToSpawn.add(itemDef);
    }

    public void handleSpawningItems() {
        if (!itemsToSpawn.isEmpty()) {
            ItemDef itemDef = itemsToSpawn.poll();
            if (itemDef.type.equals(Fruit.class)) {
                items.add(new Fruit(newGame, this, itemDef.position.x, itemDef.position.y));
            }
        }
    }

    public boolean gameOver() {
        if (frog.isHit() && frog.getStateTimer() > 3.5) {
            return true;
        }
        return false;
    }

    public HUD getHud() {
        return hud;
    }

}
