package com.perceptiongames.engine.States;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.perceptiongames.engine.Entities.AABB;
import com.perceptiongames.engine.Entities.Button;
import com.perceptiongames.engine.Entities.Enemy;
import com.perceptiongames.engine.Entities.Player;
import com.perceptiongames.engine.Game;
import com.perceptiongames.engine.Handlers.Animation;
import com.perceptiongames.engine.Handlers.GameStateManager;
import com.perceptiongames.engine.Handlers.Terrain.*;

import javax.sound.sampled.Line;
import java.util.ArrayList;
import java.util.List;

public class Play extends State {

    public static float MUSIC_VOLUME;
    public static float AUDIO_VOLUME;

    private ShapeRenderer debug;
    private BitmapFont debugFont;

    private int levelNumber;

    private Player player;
    private List<Enemy> enemies;

    private List<Vector2> deathPoints;

    private TerrainGenerator generator;
    private Tile[][] terrain;
    private Texture bg;
    private Texture outline;

    private boolean showDeathPoints;

    private float timeTaken;
    private int enemyReset;

    private float cameraXOffset;
    private float cameraYOffset;
    private OrthographicCamera hudCamera;

    private boolean musicOn;
    private boolean audioOn;
    private Button musicToggle;
    private Button audioToggle;

    private boolean touched;
    private int totalKills;
    private float totalTime;

    public Play(GameStateManager gsm) {
        super(gsm);

        levelNumber = 1;
        loadContent();
        generateEntities();
        totalKills=0;

        debug = new ShapeRenderer();
        debug.setColor(1, 0, 0, 1);
        debugFont = content.getFont("Ubuntu");
        camera.zoom = 0.7f;
        bg = content.getTexture("Background");
        bg.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        outline = content.getTexture("BrokenWall1");
        outline.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        deathPoints = new ArrayList<Vector2>();
        showDeathPoints = false;
        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(true);
        timeTaken = 0;
        enemyReset = 0;

        MUSIC_VOLUME = 0.1f;
        AUDIO_VOLUME = 0.2f;

        content.getMusic("Music").setLooping(true);
        content.getMusic("Music").setVolume(MUSIC_VOLUME);
        content.getMusic("Music").play();

        musicOn = true;
        audioOn = true;
    }

    private void renderHUD() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        debug.setProjectionMatrix(hudCamera.combined);
        batch.setProjectionMatrix(hudCamera.combined);
        debug.begin(ShapeRenderer.ShapeType.Filled);
        debugFont.setColor(0,0,0,1);
        mouse.set(0,0,0);
        hudCamera.unproject(mouse);
        mouse.y--;

        Color a = new Color(253f/255,0,0,0.6f);
        Color b = new Color(253f/255,0,0,0.75f);
        debug.rect(mouse.x, mouse.y, 200, 75, a , a, a, a);
        debug.triangle(mouse.x + 200, mouse.y, mouse.x + 200, mouse.y + 75, mouse.x + 375, mouse.y, a, a, b);

        mouse.set(viewport.getScreenWidth(), 0, 0);
        hudCamera.unproject(mouse);
        mouse.y--;

        musicToggle.setPosition(mouse.x - 170, mouse.y + 22);
        audioToggle.setPosition(mouse.x - 70, mouse.y + 22);

        debug.rect(mouse.x - 200, mouse.y, 200, 75, a , a, a, a);
        debug.triangle(mouse.x - 200, mouse.y, mouse.x - 200, mouse.y + 75, mouse.x - 375, mouse.y, a, a, b);

        debug.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();
        musicToggle.render(batch);
        audioToggle.render(batch);
        debugFont.draw(batch, "Level: " + levelNumber, mouse.x - 140, mouse.y + 5);
        mouse.set(0, 0, 0);
        hudCamera.unproject(mouse);
        debugFont.draw(batch, "Time: " + Math.round(timeTaken), mouse.x + 20,mouse.y + 5);
        debugFont.draw(batch, "Death Count: "+player.getNumberDeaths(),mouse.x+20,mouse.y+30);
        debugFont.draw(batch, "Enemies Killed: "+ player.getEnemiesKilled(), mouse.x + 20, mouse.y + 55);
        batch.end();
        debug.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
    }


    @Override
    public void update(float dt) {


        if(Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
        boolean movingCamera=false;
        if(Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            movingCamera=true;
            if(cameraXOffset-player.getAABB().getHeight()>-Game.WIDTH/2)
                cameraXOffset-=8;
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            movingCamera=true;
            if(cameraXOffset+player.getAABB().getWidth()<Game.WIDTH/2)
                cameraXOffset+=8;
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.UP)) {
            movingCamera=true;
            if(cameraYOffset-player.getAABB().getHeight()/2>-Game.HEIGHT/2)
                cameraYOffset-=8;
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            movingCamera=true;
            if(cameraYOffset+player.getAABB().getHeight()/2<Game.HEIGHT/2)
                cameraYOffset+=8;
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.Q)) {
            camera.zoom = Math.max(camera.zoom - 0.02f, 0);
        } else if (Gdx.input.isKeyPressed(Input.Keys.E)) {
            camera.zoom = Math.min(camera.zoom + 0.02f, 4f);
        } else if (!player.isLive() && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetPlayer();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            showDeathPoints = !showDeathPoints;
        }

        if (!movingCamera) {
            if (Math.abs(cameraXOffset) > 10)
                cameraXOffset -= (cameraXOffset / Math.abs(cameraXOffset)) * 3f;
            if (Math.abs(cameraYOffset) > 10)
                cameraYOffset -= (cameraYOffset / Math.abs(cameraYOffset)) * 3f;
            if (Math.abs(cameraXOffset) < 10) {
                cameraXOffset = 0;
            }
            if (Math.abs(cameraYOffset) < 10) {
                cameraYOffset = 0;
            }
        }

        player.update(dt);

        for(Enemy e : enemies) {
            if((e.getPosition().x-player.getAABB().getCentre().x)*(e.getPosition().x-player.getAABB().getCentre().x)
                    + (e.getPosition().y-player.getAABB().getCentre().y)*(e.getPosition().y-player.getAABB().getCentre().y)<300*300)
            {
                e.playerDirection(-(e.getPosition().x-player.getAABB().getCentre().x)/Math.abs((e.getPosition().x-player.getAABB().getCentre().x)));
            }
            else
            {
                e.playerDirection(0);
            }
            e.update(dt);
        }


        mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        hudCamera.unproject(mouse);
        if(musicToggle.getAABB().contains(new Vector2(mouse.x, mouse.y)) && isJustClicked()) {
            musicOn = !musicOn;
            if(musicOn) {
                musicToggle.setCurrentAnimation("On");
                content.getMusic("Music").play();
            }
            else {
                musicToggle.setCurrentAnimation("Off");
                content.getMusic("Music").stop();
                System.out.println(enemies.size());
            }
        }

        if(audioToggle.getAABB().contains(new Vector2(mouse.x, mouse.y)) && isJustClicked()) {
            audioOn = !audioOn;
            if(audioOn) {
                AUDIO_VOLUME = 0.1f;
                audioToggle.setCurrentAnimation("On");
            }
            else {
                AUDIO_VOLUME = 0;
                audioToggle.setCurrentAnimation("Off");
            }
        }

        for (int i = 0; i < terrain.length; i++) {
            for(int j = 0; j < terrain[0].length; j++) {
                Tile current = terrain[i][j];
                if(current == null) continue;

                if(current instanceof SpearBlock) current.update(dt);
                if(current instanceof FallingBlock) {
                    current.update(dt);

                    if(current.isActive() && player.getPosition().y > current.getAABB().getMaximum().y) {
                        if(Math.abs(player.getPosition().x - current.getAABB().getPosition().x) < Tile.SIZE) {
                            ((FallingBlock) current).setVelocity(450f);
                            ((FallingBlock) current).setPlayerColliding(true);
                        }
                    }
                }

                if(player.getAABB().overlaps(current.getAABB())) {
                    player.getAnimation(player.getAnimationKey()).setPosition(player.getPosition());

                    if(current instanceof StandardTile) { standardTileCollision((StandardTile) current); }
                    else if(current instanceof SpearBlock) { spearBlockCollision((SpearBlock) current); }
                    else if(current instanceof Sensor) { sensorCollision((Sensor) current, -1); }
                    else if(current instanceof FallingBlock) { fallingBlockCollision((FallingBlock) current); }
                }

                for(int e = 0; e < enemies.size(); e++) {

                    if(enemies.get(e).getAABB().overlaps(current.getAABB())) {
                        if(current instanceof Sensor) { sensorCollision((Sensor) current, e); }
                    }

                    if(enemies.get(e).isAttacking() && enemies.get(e).getWeapon().overlaps(player.getAABB())) {
                        player.hit();
                    }
                    if(player.getWeapon().overlaps(enemies.get(e).getAABB()) && player.isAttacking()) {
                        if(enemies.get(e).isLive())
                            player.incrementEnemiesKillled();

                        enemies.get(e).hit();
                    }
                }
            }
        }

        if(player.isLive()) {
            camera.position.set(
                    Math.max(Math.min(player.getAABB().getPosition().x + 16+cameraXOffset, Game.WORLD_WIDTH - 320), 320),
                    Math.max(Math.min(player.getAABB().getPosition().y + 32+cameraYOffset, Game.WORLD_HEIGHT - 180), 180),
                    0);
        }

        camera.update();
        hudCamera.update();

        if(player.isLive())
            timeTaken += dt;
    }

    @Override
    public void render() {

        batch.setProjectionMatrix(camera.combined);
        debug.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(outline, -320, -180,
                Game.WORLD_WIDTH + 640, Game.WORLD_HEIGHT + 360, 0, 0,
                Game.WORLD_WIDTH / bg.getWidth(), Game.WORLD_HEIGHT / bg.getHeight());

        batch.draw(bg, 0, 0,
                Game.WORLD_WIDTH , Game.WORLD_HEIGHT , 0, 0,
                Game.WORLD_WIDTH / bg.getWidth(), Game.WORLD_HEIGHT / bg.getHeight());


        player.render(batch);

        for(Tile[] column : terrain) {
            for(Tile tile : column) {
                if(tile != null) { tile.render(batch); }
            }
        }



        for(Enemy e : enemies) { e.render(batch);}
        batch.end();

        if(showDeathPoints) {
            debug.begin(ShapeRenderer.ShapeType.Filled);
            for (Vector2 p : deathPoints) {
                debug.circle(p.x, p.y, 7f);
            }
            debug.end();
        }

        renderHUD();

        touched = Gdx.input.isTouched();
    }

    @Override
    public void dispose() {}

    private void spearBlockCollision(SpearBlock tile) {
        float x = Math.abs(player.getPosition().x - tile.getAnimation().getPosition().x);
        if(tile.isFacingLeft()) {
            if (x < 80 && player.getAABB().hasCollisionBit(AABB.LEFT_BITS))
                player.hit();
        }
        else {
            if (x > 80 && player.getAABB().hasCollisionBit(AABB.RIGHT_BITS))
                player.hit();
        }
    }

    private void sensorCollision(Sensor tile, int index) {
        if(tile.getData().contains("Enemy") && index >= 0) {
            if(tile.getData().equals("EnemyLeft")) {
                enemies.get(index).setCurrent(2);
            }
            else if(tile.getData().equals("EnemyRight")) {
                enemies.get(index).setCurrent(1);
            }
        }
        else if(!tile.getData().contains("Enemy")) {
            int row = tile.getRow();
            int col = tile.getColumn();

            Tile[] s = new Tile[]{null, null, null, null, null, null, null, null};

            int xMax = TerrainGenerator.ROOM_WIDTH * TerrainGenerator.GRID_SIZE;
            int yMax = TerrainGenerator.ROOM_HEIGHT * TerrainGenerator.GRID_SIZE;


            if (row + 1 < yMax && col + 1 < xMax) { s[0] = terrain[col + 1][row + 1]; }
            if (row - 1 > 0 && col - 1 > 0) { s[1] = terrain[col - 1][row - 1]; }
            if (row + 1 < yMax) { s[2] = terrain[col][row + 1]; }
            if (row - 1 > 0) { s[3] = terrain[col][row - 1]; }
            if (col + 1 < xMax && row - 1 > 0) { s[4] = terrain[col + 1][row - 1]; }
            if (col - 1 > 0) { s[5] = terrain[col - 1][row]; }
            if (col + 1 < xMax) { s[6] = terrain[col + 1][row]; }
            if (col - 1 > 0 && row + 1 < yMax) { s[7] = terrain[col - 1][row + 1]; }

            for (Tile t : s) { if (t != null) t.setActive(true); }
        }
    }

    private void fallingBlockCollision(FallingBlock tile) {
        if(tile.isActive()) {
            tile.setPlayerColliding(true);
            tile.setVelocity(450f);
            if(player.getAABB().hasCollisionBit(AABB.BOTTOM_BITS)) {
                player.hit();
            }
        }

    }

    private void standardTileCollision(StandardTile tile) {
        if(tile.getDamage() > 0) { player.hit(); }
        else if(tile.getDamage() == -4) {
            enemyReset = player.getEnemiesKilled();
            totalKills+=enemyReset;
            gsm.pushState(GameStateManager.END_LEVEL);
        }
        else if(tile.getDamage()==-100)
        {
            enemyReset = player.getEnemiesKilled();
            totalKills+=enemyReset;
            totalTime+=timeTaken;
            gsm.pushState(GameStateManager.FINISH);
        }
    }

    public void resetPlayer() {
        player.incrementDeaths();
        deathPoints.add(new Vector2(player.getAABB().getCentre()));
        player.reset(generator.getStartPosition());

        player.setEnemiesKilled(enemyReset);

        for(Enemy e : enemies) {
            if(!e.isLive()) {
                e.setLive(true);
            }
        }

        for(Tile[] t : terrain) {
            for(Tile current : t) {
                if(current instanceof FallingBlock) {
                    if(!((FallingBlock) current).isAlive())
                        ((FallingBlock) current).reset();
                    else
                        current.setActive(false);
                }
            }
        }
    }

    public void resetLevel() {

        generator.generate();
        player.reset(generator.getStartPosition());
        camera.position.set(
                Math.max(Math.min(player.getAABB().getPosition().x + 16, Game.WORLD_WIDTH - 320), 320),
                Math.max(Math.min(player.getAABB().getPosition().y + 32, Game.WORLD_HEIGHT - 180), 180),
                0);

        camera.zoom = 0.7f;

        enemies.clear();
        enemies.addAll(generator.getEnemies());

        deathPoints.clear();
        totalTime+=timeTaken;
        timeTaken=0;
        terrain = generator.getTerrain();
        levelNumber++;
        System.out.println("Floor "+levelNumber);
        if(levelNumber==7)
        {
            generator.setFinalLevel(true);
        }
    }

    private void loadContent() {

        content.loadTexture("PlayerIdle", "PlayerIdle.png");
        content.loadTexture("PlayerMove", "PlayerRun.png");
        content.loadTexture("PlayerPush", "PlayerPush.png");
        content.loadTexture("PlayerAttackLeft", "PlayerAttackLeft.png");
        content.loadTexture("PlayerAttackRight", "PlayerAttackRight.png");
        content.loadTexture("Badlogic", "badlogic.jpg");
        content.loadTexture("Block", "testBlock.png");

        content.loadTexture("Enemy0", "Soldier.png");
        content.loadTexture("EnemyMove0", "SoldierMove.png");
        content.loadTexture("EnemyAttack0", "SoldierAttack.png");

        content.loadTexture("Enemy1", "Soldier2.png");
        content.loadTexture("EnemyMove1", "Soldier2Move.png");
        content.loadTexture("EnemyAttack1", "Soldier2Attack.png");

        content.loadTexture("Enemy2", "Soldier3.png");
        content.loadTexture("EnemyMove2", "Soldier3Move.png");
        content.loadTexture("EnemyAttack2", "Soldier3Attack.png");

        content.loadTexture("AudioOn", "Icons/audioOn.png");
        content.loadTexture("AudioOff", "Icons/audioOff.png");

        content.loadTexture("MusicOn", "Icons/musicOn.png");
        content.loadTexture("MusicOff", "Icons/musicOff.png");

        content.loadTexture("Ladder", "Terrain/Ladder.png");
        content.loadTexture("EndDoor", "Terrain/EndDoor.png");
        content.loadTexture("SpearBlock", "Terrain/SpearBlock.png");
        content.loadTexture("Wall", "Terrain/Wall.png");
        content.loadTexture("BrokenWall", "Terrain/BrokenWall1.png");
        content.loadTexture("BrokenWall1", "Terrain/BrokenWall2.png");
        content.loadTexture("BrokenWall2", "Terrain/BrokenWall3.png");
        content.loadTexture("Spikes", "Terrain/Spikes.png");
        content.loadTexture("Ground", "Terrain/Ground.png");
        content.loadTexture("Amulet", "Terrain/Amulet.png");

        content.loadFont("Ubuntu", "UbuntuBold.ttf", 20);

        content.loadMusic("Music", "backgroundMusic.mp3");
        content.loadSound("Attack", "attack1.mp3");
        content.loadSound("Land", "Land.mp3");
        content.loadSound("Jump", "Jump.mp3");
    }

    private void generateEntities() {

        generator =  new TerrainGenerator(content);

        Animation playerStill = new Animation(content.getTexture("PlayerIdle"), 1, 24, 0.2f);

        Animation playerLeft = new Animation(content.getTexture("PlayerMove"), 1, 5, 0.1f);
        Animation playerRight = new Animation(content.getTexture("PlayerMove"), 1, 5, 0.1f);
        Animation playerPushLeft = new Animation(content.getTexture("PlayerPush"), 1, 5, 0.1f);
        Animation playerPushRight = new Animation(content.getTexture("PlayerPush"), 1, 5, 0.1f);
        Animation playerAttackLeft = new Animation(content.getTexture("PlayerAttackLeft"), 1, 4, 0.05f);
        Animation playerAttackRight = new Animation(content.getTexture("PlayerAttackRight"), 1, 4, 0.05f);
        playerAttackLeft.setOffset(-32, 0);

        playerLeft.setFlipX(true);
        playerPushLeft.setFlipX(true);

        AABB aabb = new AABB(new Vector2(100, 100), new Vector2(16, 32));
        player = new Player(playerStill, "idle", aabb);

        player.addAnimation("moveLeft", playerLeft);
        player.addAnimation("moveRight", playerRight);

        player.addAnimation("pushLeft", playerPushLeft);
        player.addAnimation("pushRight", playerPushRight);

        player.addAnimation("attackRight", playerAttackRight);
        player.addAnimation("attackLeft", playerAttackLeft);
        player.getSounds().add(content.getSound("Attack"));
        player.getSounds().add(content.getSound("Land"));
        player.getSounds().add(content.getSound("Jump"));

        player.setWeapon(new AABB(player.getAABB().getCentre().x, player.getAABB().getCentre().y + 4, 6, 3));

        enemies = new ArrayList<Enemy>();
        enemies.addAll(generator.getEnemies());


        terrain = generator.getTerrain();

        player.setPosition(generator.getStartPosition());

        musicToggle = new Button(new Animation(content.getTexture("MusicOn"), 1, 1), "On", new AABB(100, 100, 24, 24));
        musicToggle.addAnimation("Off", new Animation(content.getTexture("MusicOff"), 1, 1));

        audioToggle = new Button(new Animation(content.getTexture("AudioOn"), 1, 1), "On", new AABB(200, 100, 24, 24));
        audioToggle.addAnimation("Off", new Animation(content.getTexture("AudioOff"), 1, 1));
    }

    public float getTime() { return timeTaken; }
    public Player getPlayer() { return player; }

    public boolean isJustClicked() { return Gdx.input.isTouched() && !touched; }

    public int getTotalKills() {
        return totalKills;
    }

    public float getTotalTime() {
        return totalTime;
    }
}