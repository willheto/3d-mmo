import Cache from '../cache/index';

type MusicArea = {
	name: string;
	music: string;
	topLeftTile: {
		x: number;
		y: number;
	};
	bottomRightTile: {
		x: number;
		y: number;
	};
};

export default class AudioManager {
	public currentSong: HTMLAudioElement | null = null;
	public currentSongName: string = '';
	public isAutoplayOn: boolean = true;
	private musicAreas: MusicArea[] = [];

	constructor() {}

	public async loadMusicAreas(): Promise<void> {
		const blob = await Cache.getObjectURLByAssetName('musicAreas.json');

		if (!blob) {
			throw new Error('Music areas blob not found');
		}

		const musicAreas = await fetch(blob).then(response => response.json());
		this.musicAreas = musicAreas;
	}

	public playAreaMusic(worldX: number, worldY: number) {
		if (this.isAutoplayOn === false) {
			return;
		}
		const musicArea = this.musicAreas.find(area => {
			return (
				worldX >= area.topLeftTile.x &&
				worldX <= area.bottomRightTile.x &&
				worldY >= area.topLeftTile.y &&
				worldY <= area.bottomRightTile.y
			);
		});

		if (!musicArea) {
			return;
		}

		if (this.currentSongName === musicArea.music) {
			return;
		}

		this.playMusic(musicArea.music);
	}

	public async playMusic(fileName: string): Promise<void> {
		const audioFile = await Cache.getObjectURLByAssetName(fileName);

		if (audioFile === null) {
			console.error('Audio file not found:', fileName);
			return;
		}

		this.currentSong?.pause();

		const audio = new Audio(audioFile);
		audio.volume = 0.1;
		audio.loop = true;
		audio.play();

		this.currentSong = audio;
		this.currentSongName = fileName;
	}

	public async playSfx(fileName: string, pauseMusic: boolean = false): Promise<void> {
		const audioFile = await Cache.getObjectURLByAssetName(fileName);

		if (audioFile === null) {
			console.error('Audio file not found:', fileName);
			return;
		}
		const audioLength = await this.getAudioLength(fileName);

		if (pauseMusic) {
			// briefly silence the music
			this.currentSong?.pause();

			// resume the music
			setTimeout(() => {
				this.currentSong?.play();
			}, audioLength);
		}

		const audio = new Audio(audioFile);
		audio.volume = 0.5;
		audio.play();
	}

	public async getAudioLength(fileName: string): Promise<number> {
		const audioFile = await Cache.getObjectURLByAssetName(fileName);

		if (audioFile === null) {
			console.error('Audio file not found:', fileName);
			return 0;
		}

		return new Promise((resolve, reject) => {
			const audio = new Audio(audioFile);

			// Event listener to wait for the metadata to load
			audio.addEventListener('loadedmetadata', () => {
				// Resolve with the audio duration in milliseconds
				resolve(audio.duration * 1000);
			});

			// Error handling in case the audio file cannot be loaded
			audio.addEventListener('error', err => {
				console.error('Error loading audio file:', err);
				reject(0);
			});
		});
	}

	public stopMusic(): void {
		this.currentSong?.pause();
	}
}
