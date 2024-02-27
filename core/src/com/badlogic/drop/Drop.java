package com.badlogic.drop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class Drop extends ApplicationAdapter {
	//Instantiate variables for our assets
	private Texture dropImage;
	private Texture bucketImage;
	private Sound dropSound;
	private Music rainMusic;

	//Assets are usually loaded in ApplicationAdapter.create()
	@Override
	public void create() {
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
	}
}
