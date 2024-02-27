package com.badlogic.drop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.Iterator;

public class Drop extends ApplicationAdapter {
	//Instantiate variables for our assets
	private Texture dropImage;
	private Texture bucketImage;
	private Sound dropSound;
	private Music rainMusic;

	//camera
	private OrthographicCamera camera;
	//spritebatch object
	private SpriteBatch batch;

	//bucket object
	private Rectangle bucket;

	//raindrop related variables
	private Array<Rectangle> raindrops;
	//use libgdx Array rather than Java array to reduce garbage collection needed (same for sets and hashmaps)

	private long lastDropTime;
	//time since we last spawned a raindrop
	//stored in nanoseconds so use a long

	private void spawnRaindrop() {
		Rectangle raindrop = new Rectangle();
		raindrop.x = MathUtils.random(0, 800-64); //random x-pos that the bucket can reach
		raindrop.y = 480; //top of the screen
		raindrop.width = 64; raindrop.height = 64;
		raindrops.add(raindrop); //add raindrop object to raindrops array
		lastDropTime = TimeUtils.nanoTime();
	}

	//CONSIDER create() LIKE SETUP() - RUNS ONCE, BEFORE render()
	@Override
	public void create() {
		//Assets are usually loaded in ApplicationAdapter.create()

		//load the images for the droplet and the bucket
		//they are 64x64 pixels each
		dropImage = new Texture(Gdx.files.internal("droplet.png"));
		bucketImage = new Texture(Gdx.files.internal("bucket.png"));
		//A texture represents a loaded image stored in VRAM
		//You can't draw directly to a texture in LibGDX - you have to load into one from a file
		//Do this using the Gdx.files.internal() method (to load a file from the assets folder)

		//load the drop sound effect and the rain background music
		dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.wav"));
		rainMusic = Gdx.audio.newMusic(Gdx.files.internal("rain.mp3"));
		//Sound effects are stored in memory
		//Music is streamed from wherever it is stored on disk as it is too large to be kept in RAM
		//If audio < 10s long => use Sound, else use Music

		//start the playback of the background music immediately
		rainMusic.setLooping(true);
		rainMusic.play();

		//create a camera
		camera = new OrthographicCamera();
		camera.setToOrtho(false, 800, 480);
		//this camera will always show an area of the game world 800x480 units - don't hardcode this!

		//create a spritebatch
		batch = new SpriteBatch();

		//create our bucket
		bucket = new Rectangle();
		bucket.x = (800/2) - (64/2); //centered halfway across the screen
		//origin is bottom left of bucket so have to minus (object width / 2) for it to appear centered
		//btw this is terrible - don't hardcode window dimensions like this
		bucket.y = 20; //20 pixels from bottom of screen
		bucket.width = 64;
		bucket.height = 64;
		//LibGDX transformation origin is BOTTOM LEFT of the screen!!

		//---------------------------------------------
		//--------------RAINDROP SETUP-----------------
		//---------------------------------------------

		raindrops = new Array<Rectangle>();
		spawnRaindrop();

	}

	//CONSIDER render() LIKE DRAW - RUNS ONCE PER FRAME
	@Override
	public void render() {
		//---------------------------------------------
		//--------------SETUP CAMERA-------------------
		//---------------------------------------------

		//render background colour (takes RGBA values from 0-1)
		ScreenUtils.clear(0,0,0.2f,1);

		//update camera position
		camera.update();

		//render the bucket using a spritebatch:
		//tell batch to use the coordinate system specified by the camera (camera.combined matrix)
		batch.setProjectionMatrix(camera.combined);
		//start a new sprite batch - allows OpenGL optimisations in batching
		batch.begin();

		//draw() takes a Texture and an x,y pos

		//draw the bucket
		batch.draw(bucketImage, bucket.x, bucket.y);
		//draw all raindrops
		//for each raindrop in raindrops
		for (Rectangle raindrop : raindrops) {
			batch.draw(dropImage, raindrop.x, raindrop.y);
		}

		//calling .end() submits all draw() calls at once
		batch.end();
		//rendering this way makes it much faster

		//---------------------------------------------
		//--------------RAINDROP LOGIC-----------------
		//---------------------------------------------

		if (TimeUtils.nanoTime() - lastDropTime > 1000000000) {
			//spawn a new raindrop every 1000000000ns (1 second)
			spawnRaindrop();
		}

		//make the raindrops fall
		//for each raindrop in raindrops array
		for (Iterator<Rectangle> iter = raindrops.iterator(); iter.hasNext();) {
			Rectangle raindrop = iter.next();
			//move down by 200 pixels
			raindrop.y -= 200 * Gdx.graphics.getDeltaTime();
			//if off the bottom of the screen, remove it from the array
			if (raindrop.y + 64 < 0) {
				iter.remove();
			}
			//if caught by a bucket
			if (raindrop.overlaps(bucket)) {
				dropSound.play();
				iter.remove();
			}
		}

		//---------------------------------------------
		//--------------MOVEMENT LOGIC-----------------
		//---------------------------------------------

		//center the button horizontally around the clicked position
		//check is screen is pressed / mouse is pressed
		if (Gdx.input.isTouched()) {
			//!!VERY BAD!! TO INSTANTIATE A new V3 (or any variable) IN RENDER()
			//THIS IS LEAKING MEMORY - INSTANTIATE AS A CLASS VARIABLE THEN REUSE / REASSIGN IN RENDER()
			//the garbage collector will remove all the old references to created V3()'s, but this is SLOW and hurts performance
			Vector3 touchPos = new Vector3();
			//get x,y position of click
			//store as V3 to make it easy to map back into 2D using a camera un-projection
			touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
			//map back to our camera's coordinate system
			//the camera is actually rendering in 3D, just ignoring Z-coordinates (think Unity)
			camera.unproject(touchPos);
			bucket.x = touchPos.x - (64/2);
		}

		//keyboard movement
		//we are going to do this smoothly, without acceleration => we need the time since the last frame

		//left
		if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
			//here, 200 is our 'speed' variable
			//multiplying by getDeltaTime() smooths the movement so it's apparent speed is independent of framerate
			//so any drops in framerate don't cause juttery movement
			bucket.x -= 200f * Gdx.graphics.getDeltaTime();
		}
		//right
		if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
			bucket.x += 200f * Gdx.graphics.getDeltaTime();
		}

		//---------------------------------------------
		//--------------COLLISIONS---------------------
		//---------------------------------------------

		//make sure the bucket does not go off the sides of the screen
		if (bucket.x < 0) bucket.x = 0;
		if (bucket.x > (800 - 64)) bucket.x = 800 - 64;
	}
}
