import Draw2D from '../graphics/Draw2D';
import LoginServerEvents from './LoginServerEvents';
import Login, { LOGIN_REQUEST } from '../login/Login';
import Cache from '../cache/index';
import Player from '../entity/player/Player';
import Npc from '../entity/npcs/Npc';
import pako from 'pako';
import AudioManager from '../managers/AudioManager';
import { canvas, canvas2d } from '../graphics/2DCanvas';
import { World } from '../world/World';

const UPDATE_REQUEST = {
	CHECK_FOR_UPDATES: 1,
};

const UPDATE_RESPONSE = {
	CACHE_UP_TO_DATE: 1,
	UPDATE_AVAILABLE: 2,
};

export default class Client {
	private targetFPS: number = 50;
	private frameInterval: number = 1000 / this.targetFPS;
	public lastFrameTime: number = performance.now();
	private accumulator: number = 0;
	public fps: number = 0;

	private loginTime: number = Date.now();

	public latency: number = 0;
	private lastPingTime: number = 0;
	public lastPacketSize: number = 0;

	public loginSocket: WebSocket | null = null;

	public login: Login | null = null;
	public world: World | null = null;

	public cacheNumber: number | null = null;

	private pingInterval: any;
	private animationFrame: number = 0;

	public audioManager: AudioManager = new AudioManager();

	constructor() {
		//
	}

	public stopGameAndGoToLogin(): void {
		this.disconnectFromGameServer();
		//this.world?.destroy();
		this.world = null;
		clearInterval(this.pingInterval);
		cancelAnimationFrame(this.animationFrame);

		this.loginSocket?.send(
			JSON.stringify({
				world: 1,
				type: LOGIN_REQUEST.LOGOUT,
			}),
		);

		this.login = new Login(this);
		if (!this.loginSocket) return;
		this.login.init(this.loginSocket);
	}

	private disconnectFromGameServer(): void {
		const playerID = this.world?.currentPlayerID;
		if (!playerID) return;
		this.world?.actions?.logOut(playerID);
	}

	public async startClient(): Promise<void> {
		canvas2d.imageSmoothingEnabled = false;

		Draw2D.fillCanvas('black');
		Draw2D.showProgress(0, 'Connecting to update server');
		const updateServerSocket = await this.connectToUpdateServer();
		Draw2D.fillCanvas('black');
		Draw2D.showProgress(20, 'Downloading updates...');

		updateServerSocket.onmessage = async (event): Promise<void> => {
			const data = JSON.parse(event.data);
			if (data.type === UPDATE_RESPONSE.UPDATE_AVAILABLE) {
				Draw2D.fillCanvas('black');
				Draw2D.showProgress(50, 'Unpacking assets');
				await Cache.saveNewCache(data.assets, data.cacheNumber);
				this.audioManager.loadMusicAreas();
				this.cacheNumber = data.cacheNumber;
			} else if (data.type === UPDATE_RESPONSE.CACHE_UP_TO_DATE) {
				this.cacheNumber = await Cache.getCacheNumber();
				this.audioManager.loadMusicAreas();
			}

			Draw2D.fillCanvas('black');
			Draw2D.showProgress(90, 'Connecting to login server');
			updateServerSocket.close();
			await this.connectToLoginServer();
		};

		const currentCacheNumber = await Cache.getCacheNumber();
		updateServerSocket.send(
			JSON.stringify({
				cacheNumber: currentCacheNumber,
				type: UPDATE_REQUEST.CHECK_FOR_UPDATES,
			}),
		);
	}

	private async connectToUpdateServer(): Promise<WebSocket> {
		return new Promise(resolve => {
			const socket = new WebSocket(UPDATE_SERVER_ADDRESS);
			socket.onopen = (): void => {
				resolve(socket);
			};
			socket.onerror = (): void => {
				Draw2D.showProgress(0, 'Error connecting to update server, trying again in 10 seconds...');
				setTimeout(() => {
					this.startClient();
				}, 10000);
			};
		});
	}

	public async startGame(loginToken: string, reconnectInterval?: any): Promise<void> {
		this.world = new World(this);
		this.world.start();
		canvas.style.pointerEvents = 'none';
		canvas2d.clearRect(0, 0, canvas2d.canvas.width, canvas2d.canvas.height);
		setTimeout(async (): Promise<void> => {
			if (!this.world) return;
			this.world.setLoading(10, 'Connecting to server...');
			const socket = await this.connectToGameServer(loginToken);
			this.world.setLoading(75, 'Loading world...');

			this.world.setSocket(socket);

			socket.onmessage = async (event): Promise<void> => {
				if (reconnectInterval) clearInterval(reconnectInterval);
				try {
					if (event.data == 'pong') {
						const endTime = Date.now();
						this.latency = endTime - this.lastPingTime;
						return;
					}
				} catch (error) {
					console.error('Error parsing event data:', error);
				}

				const arrayBuffer = await event.data.arrayBuffer();
				this.lastPacketSize = arrayBuffer.byteLength;
				const compressedData = new Uint8Array(arrayBuffer);
				const decompressedData = pako.inflate(compressedData, { to: 'string' });
				const gameData: SocketGameState = JSON.parse(decompressedData);

				if (gameData.playerID) {
					if (!this.world) return;
					this.world.currentPlayerID = gameData.playerID;
					this.updateGameState(gameData);
					this.startGameLoop();
					setTimeout(() => {
						this.world?.finishLoading();
					}, 2000);
				} else {
					this.updateGameState(gameData);
				}
			};
		}, 2000);
	}

	public sendPing(): void {
		const startTime = Date.now();
		this.lastPingTime = startTime;
		this.world?.gameSocket?.send(JSON.stringify({ action: 'ping', time: startTime }));
	}

	async connectToGameServer(loginToken: string): Promise<WebSocket> {
		return new Promise((resolve, reject) => {
			const socket = new WebSocket(`${GAME_SERVER_ADDRESS}/?loginToken=${loginToken}`);
			socket.onopen = (): void => {
				this.login?.destroy();
				resolve(socket);
			};
			socket.onerror = (): void => {
				console.error('Error connecting to game server');
				const reconnectInterval = setInterval(() => {
					this.startGame(loginToken, reconnectInterval);
				}, 5000);
				reject();
			};
			socket.onclose = (): void => {
				console.error('Connection to game server closed');
				this.world?.stop();
				this.world = null;
				reject();
			};
		});
	}

	private startGameLoop(): void {
		if (!this.world) return;
		this.animationFrame = requestAnimationFrame(this.gameLoop);

		this.pingInterval = setInterval(() => {
			this.sendPing();
		}, 1000);
	}

	private gameLoop = (now: number) => {
		this.animationFrame = requestAnimationFrame(this.gameLoop);

		if (!this.world) return;

		let deltaTime = now - this.lastFrameTime;
		this.lastFrameTime = now;

		// HARD reset when backgrounded or stalled
		if (deltaTime > 250) {
			this.accumulator = 0;
			return;
		}

		this.accumulator += deltaTime;

		while (this.accumulator >= this.frameInterval) {
			this.world.update(this.frameInterval / 1000);
			this.accumulator -= this.frameInterval;
		}

		// Render exactly once per frame
		this.world.render();
	};

	private async connectToLoginServer(): Promise<void> {
		const loginPromise = new Promise(resolve => {
			const socket = new WebSocket(LOGIN_SERVER_ADDRESS);

			// Listen for successful connection
			socket.onopen = (): void => {
				resolve(socket);
			};
		});

		this.loginSocket = (await loginPromise) as WebSocket;
		new LoginServerEvents(this);
		this.login = new Login(this);
		this.login.init(this.loginSocket);
	}

	private updateGameState(gameData: SocketGameState): void {
		if (!this.world) return;
		const { players, npcs, chatMessages, onlinePlayers } = gameData;

		// remove players that are no longer online
		this.world.players.forEach(player => {
			if (onlinePlayers.includes(player.entityID) === false) {
				this.world?.scene.remove(player.model);
			}
		});
		this.world.players = this.world.players.filter(player => onlinePlayers.includes(player.entityID));

		players.forEach(player => {
			const matchingPlayer = this.world?.players.find(p => p.entityID === player.entityID);
			if (!matchingPlayer) {
				if (!this.world) return;
				const newPlayer = new Player(this.world, player.entityID);
				newPlayer.update(player);
				this.world?.players.push(newPlayer);

				return;
			}

			matchingPlayer.update(player);
		});

		if (!this.world) return;

		// Create a map of existing NPCs by entityID (as a string) for fast lookup
		const npcMap = new Map<string, Npc>();
		this.world.npcs.forEach(existingNpc => {
			npcMap.set(existingNpc.entityID, existingNpc);
		});

		npcs.forEach(npc => {
			const matchingNpc = npcMap.get(npc.entityID);

			if (matchingNpc) {
				// Update existing NPC
				matchingNpc.update(npc);
			} else {
				if (!this.world) return;
				// Create a new NPC if it doesn't exist and add it to the world
				const newNpc = new Npc(this.world, npc.entityID);
				newNpc.update(npc);
				this.world.npcs.push(newNpc);
			}
		});

		this.world.chatMessages = chatMessages.filter(chatMessage => {
			// time sent after login
			if (chatMessage.timeSent < this.loginTime) {
				return false;
			}

			const currentPlayer = this.world?.players.find(player => player.entityID === this.world?.currentPlayerID);

			if (chatMessage.senderName === currentPlayer?.name || chatMessage.isGlobal === true) {
				return true;
			}
		});

		this.world.attackEvents = gameData.tickAttackEvents;
		this.world.soundEvents = gameData.tickSoundEvents;
		this.world.talkEvents = gameData.tickTalkEvents;
		this.world.items = gameData.items;
	}
}
