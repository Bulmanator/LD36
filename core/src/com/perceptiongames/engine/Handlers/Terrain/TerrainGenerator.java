package com.perceptiongames.engine.Handlers.Terrain;

import com.badlogic.gdx.graphics.Texture;
import com.perceptiongames.engine.Entities.AABB;
import com.perceptiongames.engine.Entities.Entity;
import com.perceptiongames.engine.Handlers.Animation;
import com.perceptiongames.engine.Handlers.Content;
import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;

import java.util.*;

public class TerrainGenerator {

    private enum RoomType {

        None(0),
        Standard(1),
        Down(2),
        Up(3),
        Cross(4);

        public final int VALUE;
        RoomType(int v) { VALUE = v; }

        public static RoomType getEnum(int value) {
            for(RoomType t : RoomType.values()) {
                if(t.VALUE == value)
                    return t;
            }

            return null;
        }
    }

    public static final int ROOM_WIDTH = 10;
    public static final int ROOM_HEIGHT = 8;
    public static final int GRID_SIZE = 5;

    private Tile[][] terrain;
    private Texture[] textures;

    private static final Random random = new Random();

    private boolean left, down;

    public TerrainGenerator(Content content) {
        textures = new Texture[3];
        textures[0] = content.getTexture("Wall");
        textures[1] = content.getTexture("Ground");
        textures[2] = content.getTexture("Ladder");

        terrain = new Tile[GRID_SIZE * ROOM_WIDTH][GRID_SIZE * ROOM_HEIGHT];

        generate();
    }


    /**
     * Creates a new pseudo-generated world
     */
    public void generate() {

        for (int i = 0; i < (GRID_SIZE * ROOM_WIDTH); i++) {
            for (int j = 0; j < (GRID_SIZE * ROOM_HEIGHT); j++) {
                terrain[i][j] = null;
            }
        }

        RoomType[][] rooms = new RoomType[GRID_SIZE][GRID_SIZE];
        for(int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                rooms[i][j] = RoomType.None;
            }
        }

        left = down = false;
        int x = random.nextInt(GRID_SIZE), y = 0;
        while(true) {
            down = false;
            getDir();

            if(left) {
                x--;
            }
            else {
                x++;
            }

            if(x >= GRID_SIZE) {
                x--;
                down = true;
            }
            else if(x < 0) {
                x++;
                down = true;
            }

            if(down) {
                rooms[x][y] = RoomType.Down;
                y++;
                left = !left;
                if(y >= GRID_SIZE) {
                    rooms[x][y - 1] = RoomType.Standard;
                    break;
                }
            }

            if(!down) {
                rooms[x][y] = RoomType.Standard;
            }
            else {
                rooms[x][y] = RoomType.getEnum(random.nextInt(1) + 3);
            }
        }

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                generateRoom(i, j, rooms[i][j]);
            }
        }
    }

    private void getDir() {
        boolean currentDir = left;

        int next = random.nextInt(5) + 1;
        if(next < 3) { left = true; }
        else if(next < 5) { left = false; }
        else { down = true; }

        if(!down && currentDir != left) left = currentDir;
    }

    /**
     * Creates a new room base on the Room Type given
     * @param xStart Position on the grid width
     * @param yStart Position on the grid height
     * @param type Type of room to create
     */
    private void generateRoom(int xStart, int yStart, RoomType type) {
        if(xStart >= GRID_SIZE || yStart >= GRID_SIZE)
            throw new ValueException("Error: xStart and yStart must be less than GRID SIZE\nxStart: " +
                    xStart + "\nyStart: " + yStart + "\nGRID_SIZE: " + GRID_SIZE);

        float xOffset = xStart * ROOM_WIDTH * Tile.SIZE + 40;
        float yOffset = yStart * ROOM_HEIGHT * Tile.SIZE + 40;

        int xIndex = (xStart * ROOM_WIDTH);
        int yIndex = (yStart * ROOM_HEIGHT);

        float halfSize = Tile.SIZE / 2;

        int seed;
        boolean createTile;
        for (int i = 0; i < ROOM_WIDTH; i++) {
            for (int j = 0; j < ROOM_HEIGHT; j++) {
                seed = 0;
                createTile = false;
                switch (type) {
                    case Standard:
                        if(j != 4 && seed < 15) { createTile = true; }
                        break;
                    case Down:
                        if(j < 4 && seed < 10) { createTile = true; }
                        else if(j != 4 && i != 5 && seed < 13) { createTile = true; }
                        break;
                    case Up:
                        if(j > 4 && seed < 17) { createTile = true; }
                        else if(j != 4 && i != 5 && seed < 14) { createTile = true; }
                        break;
                    case Cross:
                        if(j != 4 && i != 5 && seed < 15) { createTile = true; }
                        break;
                    case None:
                        if(seed < 15) { createTile = true; }
                        break;
                }

                if(createTile) {
                    terrain[xIndex + i][yIndex + j] = new Tile(new Animation(textures[0], 1, 1, 1f),
                            new AABB(xOffset + (Tile.SIZE * i), yOffset + (Tile.SIZE * j), halfSize, halfSize));
                }

            }
        }
    }

    /*public void update(float dt) {
        for (int i = 0; i < (GRID_SIZE * ROOM_WIDTH); i++) {
            for (int j = 0; j < (GRID_SIZE * ROOM_HEIGHT); j++) {
                if(terrain[i][j] != null)
                    terrain[i][j].update(dt);
            }
        }
    }
    public void render(SpriteBatch batch) {
        for (int i = 0; i < (GRID_SIZE * ROOM_WIDTH); i++) {
            for (int j = 0; j < (GRID_SIZE * ROOM_HEIGHT); j++) {
                if(terrain[i][j] != null)
                    terrain[i][j].render(batch);
            }
        }
    }

    public void debugRender(ShapeRenderer sr) {
        for (int i = 0; i < (GRID_SIZE * ROOM_WIDTH); i++) {
            for (int j = 0; j < (GRID_SIZE * ROOM_HEIGHT); j++) {
                if(terrain[i][j] != null)
                    terrain[i][j].getAABB().debugRender(sr);
            }
        }
    }*/

    public List<Entity> getTerrain() {
        List<Entity> list = new ArrayList<Entity>();
        for(Tile[] t : terrain)
            list.addAll(Arrays.asList(t));

        list.removeAll(Collections.singleton(null));
        return list;
    }
}