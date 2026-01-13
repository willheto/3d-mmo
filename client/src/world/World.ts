import * as THREE from 'three';
import { InputTracker } from '../io/inputTracker';
import Client from '../client/Client';
import { Actions } from '../net/Actions';
import Npc from '../entity/npcs/Npc';
import Player from '../entity/player/Player';
import EventListenersManager from '../managers/EventListenersManager';
import { TileManager } from '../tile/TileManager';
import Draw2D from '../graphics/Draw2D';
import { canvas, canvas2d } from '../graphics/2DCanvas';
import Chat from './Chat';
import EntitiesManager from '../managers/EntitiesManager';
import { Hud } from './Hud';
import ItemsManager from '../managers/ItemsManager';
import { ItemPreviewRenderer } from '../graphics/ItemPreviewRenderer';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { Item } from '../item/Item';
import { ItemRenderer } from './ItemRenderer';

export class World {
	public gameSocket: WebSocket | null = null;
	public client: Client;

	public modalObject: ModalObject | null = null;
	public canvas: HTMLCanvasElement;
	public chat: Chat = new Chat(this);
	public hud: Hud = new Hud(this);

	// 3D RENDERING
	public renderer: THREE.WebGLRenderer;
	public scene: THREE.Scene;
	public itemPreviewRenderer!: ItemPreviewRenderer;
	private gltfLoader!: GLTFLoader;

	// --- Camera ---
	private readonly CAM_ROT_SPEED = 1.6;
	private readonly MIN_PITCH = 0.35;
	private readonly MAX_PITCH = 1.25;
	public camera: THREE.PerspectiveCamera;
	private camYaw = Math.PI / 4;
	private camPitch = 0.8;
	private camDistance = 14;

	private inputTracker: InputTracker;

	public groundGroups: THREE.Group[] = [];
	public groundGroup = new THREE.Group();

	public mouseDown: boolean;
	public mouseButton: number;
	public mouseScreenX: number;
	public mouseScreenY: number;
	public mouseTileX: number;
	public mouseTileY: number;

	public currentPlayerID: string = '';
	public itemRenderer: ItemRenderer;
	public chatMessages: SocketChatMessage[] = [];
	public players: Player[] = [];
	public npcs: Npc[] = [];
	public items: Item[] = [];

	public currentPlayer: Player | null = null;

	public eventListenersManager: EventListenersManager = new EventListenersManager();
	public itemsManager: ItemsManager = new ItemsManager();
	public entitiesManager: EntitiesManager = new EntitiesManager();
	public tileManager: TileManager = new TileManager(this);

	public actions: Actions | null = null;

	public talkEvents: SocketTalkEvent[] = [];
	public attackEvents: SocketAttackEvent[] = [];
	public soundEvents: SocketSoundEvent[] = [];

	// --- Timing ---
	private running = false;
	private isLoading = false;

	// --- World ---
	public readonly TILE_SIZE = 1;

	// --- Marker ---
	public marker: THREE.Mesh;

	private uiCanvas!: HTMLCanvasElement;
	private uiCtx!: CanvasRenderingContext2D;
	private loadingProgress = 0;
	private loadingMessage = 'Loading...';
	public showDebug: boolean = true;

	private prepareCanvasForRenderer(): HTMLCanvasElement {
		const oldCanvas = document.getElementById('viewport')!;
		const newCanvas = document.createElement('canvas');
		newCanvas.id = 'viewport';
		oldCanvas.replaceWith(newCanvas);
		return newCanvas;
	}

	constructor(client: Client) {
		this.client = client;
		this.mouseDown = false;
		this.mouseButton = -1;
		this.mouseScreenX = 0;
		this.mouseScreenY = 0;
		this.mouseTileX = 0;
		this.mouseTileY = 0;

		const canvas = this.prepareCanvasForRenderer();
		this.canvas = canvas;
		this.renderer = new THREE.WebGLRenderer({
			canvas: canvas,
			antialias: true,
			alpha: true,
		});

		this.gltfLoader = new GLTFLoader();

		this.itemPreviewRenderer = new ItemPreviewRenderer({
			renderer: this.renderer,
			gltfLoader: this.gltfLoader,
			size: 85,
		});

		// Color management (required for correct Blender/BaseColor look)
		this.renderer.outputColorSpace = THREE.SRGBColorSpace;

		// Optional but recommended
		this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
		this.renderer.toneMappingExposure = 1.0;

		this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
		this.renderer.setSize(window.innerWidth, window.innerHeight);
		document.body.appendChild(this.renderer.domElement);

		this.uiCanvas = document.createElement('canvas');
		this.uiCanvas.style.position = 'absolute';
		this.uiCanvas.style.top = '0';
		this.uiCanvas.style.left = '0';
		this.uiCanvas.style.pointerEvents = 'none';
		this.uiCanvas.width = window.innerWidth;
		this.uiCanvas.height = window.innerHeight;

		window.addEventListener('resize', () => {
			this.uiCanvas.width = window.innerWidth;
			this.uiCanvas.height = window.innerHeight;
		});

		document.body.appendChild(this.uiCanvas);

		this.uiCtx = this.uiCanvas.getContext('2d')!;

		// Scene
		this.scene = new THREE.Scene();
		this.scene.background = new THREE.Color(0x111111);
		this.scene.fog = new THREE.Fog(0x111111, 15, 60);

		// Camera
		this.camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 200);

		// Input
		this.inputTracker = new InputTracker();

		// Lights
		this.setupLights();

		this.marker = this.createMarker();
		this.scene.add(this.groundGroup);
		this.scene.add(this.marker);

		// Events
		this.onPointerDown = this.onPointerDown.bind(this);

		this.itemRenderer = new ItemRenderer(this);
	}

	private drawLoadingBar(progress: number, message: string) {
		const ctx = this.uiCtx;
		const width = this.uiCanvas.width;
		const height = this.uiCanvas.height;

		const y = height / 2 - 18;

		ctx.clearRect(0, 0, width, height);

		// background bar
		ctx.fillStyle = 'rgb(140, 17, 17)';
		ctx.fillRect(((width / 2) | 0) - 152, y, 304, 34);

		// filled progress
		ctx.fillStyle = 'rgb(200, 40, 40)';
		ctx.fillRect(((width / 2) | 0) - 150, y + 2, progress * 3, 30);

		// text
		ctx.font = '18px Pkmn';
		ctx.textAlign = 'center';
		ctx.fillStyle = 'white';
		ctx.fillText(message, width / 2, y + 22);
	}

	public setSocket(socket: WebSocket): void {
		this.gameSocket = socket;
		this.actions = new Actions(socket, this);
	}

	start() {
		if (this.running) return;
		this.running = true;

		this.inputTracker.start();
		this.tileManager.init();

		document.addEventListener('pointerdown', e => {
			this.mouseDown = true;
			this.mouseButton = e.button; // 0 = left, 2 = right
			this.mouseScreenX = e.clientX;
			this.mouseScreenY = e.clientY;
		});

		document.addEventListener('pointerup', () => {
			this.mouseDown = false;
		});

		document.addEventListener('pointermove', e => {
			this.mouseScreenX = e.clientX;
			this.mouseScreenY = e.clientY;
		});

		document.addEventListener('contextmenu', e => {
			e.preventDefault();
		});
		this.renderer.domElement.addEventListener('pointerdown', this.onPointerDown);
		this.client.audioManager.playMusic('baroque.ogg');
	}

	public setLoading(progress: number, message: string) {
		this.isLoading = true;
		this.loadingProgress = THREE.MathUtils.clamp(progress, 0, 100);
		this.loadingMessage = message;
	}

	public finishLoading() {
		this.isLoading = false;
		this.uiCtx.clearRect(0, 0, this.uiCanvas.width, this.uiCanvas.height);
	}

	stop() {
		this.running = false;
		this.renderer.domElement.removeEventListener('pointerdown', this.onPointerDown);
	}

	private setupLights() {
		const light = new THREE.DirectionalLight(0xffffff, 1);
		this.scene.fog = new THREE.Fog(0x111111, 10, 35);

		light.position.set(5, 10, 5);
		this.scene.add(light);
		this.scene.add(new THREE.AmbientLight(0xffffff, 0.45));
	}

	private createMarker() {
		const marker = new THREE.Mesh(
			new THREE.PlaneGeometry(this.TILE_SIZE, this.TILE_SIZE),
			new THREE.MeshStandardMaterial({
				color: 0xffd166,
				transparent: true,
				opacity: 0.5,
			}),
		);
		marker.rotation.x = -Math.PI / 2;
		marker.position.y = 0.01;
		marker.visible = false;
		return marker;
	}

	private worldToScreen(pos: THREE.Vector3) {
		const v = pos.clone().project(this.camera);

		if (v.z < -1 || v.z > 1) return null;

		const width = this.renderer.domElement.width;
		const height = this.renderer.domElement.height;

		return {
			x: (v.x * 0.5 + 0.5) * width,
			y: (-v.y * 0.5 + 0.5) * height,
		};
	}

	update(dt: number) {
		canvas2d.clearRect(0, 0, canvas.width, canvas.height);

		this.updatePlayers(dt);
		this.updateCamera(dt);
		this.camera.updateMatrixWorld(true);
		this.camera.updateProjectionMatrix();

		if (this.isLoading) {
			return;
		}

		this.chat.drawChat();
		this.hud.drawHud();
		this.itemRenderer.update();

		Draw2D.drawModal(this);
		this.playSoundEvents();
		if (this.showDebug === true) {
			this.drawDebug(this.client.renderFps);
		}
	}

	private drawDebug(fps: number): void {
		canvas2d.textBaseline = 'middle';

		canvas2d.fillStyle = 'rgba(0, 0, 0, 0.5)';
		canvas2d.fillRect(0, 0, 350, 205);

		canvas2d.font = '18px Pkmn';
		canvas2d.fillStyle = 'white';
		canvas2d.fillText('FPS: ' + fps, 10, 25);
		canvas2d.fillText('Latency: ', 10, 45);
		const latencyTextWidth = canvas2d.measureText('Latency: ').width;
		if (this.client?.latency <= 250) {
			canvas2d.fillStyle = 'yellow';
		} else {
			canvas2d.fillStyle = 'red';
		}

		canvas2d.fillText(Math.floor(this.client.latency || 0) + 'ms', 10 + latencyTextWidth, 45);
		canvas2d.fillStyle = 'white';
		canvas2d.fillText('Last packet size: ' + Math.floor(this.client?.lastPacketSize || 0) + ' bytes', 10, 65);
		const currentPlayer = this.players.find(player => player.entityID === this.currentPlayerID);
		canvas2d.fillText('Player tile: ' + currentPlayer?.worldX + ', ' + currentPlayer?.worldY, 10, 85);
		canvas2d.fillText('Mouse tile: ' + this.mouseTileX + ', ' + this.mouseTileY, 10, 105);
		canvas2d.fillText('Mouse screen: ' + this.mouseScreenX + ', ' + this.mouseScreenY, 10, 125);

		canvas2d.fillText('Current chunk: ' + currentPlayer?.currentChunk, 10, 165);

		canvas2d.fillStyle = 'yellow';
		canvas2d.fillText('Type ::debug to hide', 10, 195);
	}

	private playSoundEvents(): void {
		const eventsToPlay = this.soundEvents;
		this.soundEvents = [];

		eventsToPlay.forEach(sound => {
			if (sound.isGlobal) {
				this.client.audioManager.playSfx(sound.soundName, sound.shouldInterrupt ? true : false);
			} else if (this.currentPlayerID === sound.entityID) {
				this.client.audioManager.playSfx(sound.soundName, sound.shouldInterrupt ? true : false);
			}
		});
	}

	render() {
		if (this.isLoading) {
			this.drawLoadingBar(this.loadingProgress, this.loadingMessage);
		} else {
			this.renderer.render(this.scene, this.camera);
		}
	}

	updateCamera(dt: number) {
		if (this.inputTracker.keys['arrowleft']) this.camYaw -= this.CAM_ROT_SPEED * dt;
		if (this.inputTracker.keys['arrowright']) this.camYaw += this.CAM_ROT_SPEED * dt;
		if (this.inputTracker.keys['arrowup']) this.camPitch += (this.CAM_ROT_SPEED - 0.6) * dt;
		if (this.inputTracker.keys['arrowdown']) this.camPitch -= (this.CAM_ROT_SPEED - 0.6) * dt;

		this.camPitch = THREE.MathUtils.clamp(this.camPitch, this.MIN_PITCH, this.MAX_PITCH);
		const currentPlayer = this.players.find(player => {
			return player.entityID === this.currentPlayerID;
		});

		if (!currentPlayer) {
			return;
		}

		const target = currentPlayer?.model.position;
		const x = target.x + Math.sin(this.camYaw) * Math.cos(this.camPitch) * this.camDistance;
		const y = target.y + Math.sin(this.camPitch) * this.camDistance;
		const z = target.z + Math.cos(this.camYaw) * Math.cos(this.camPitch) * this.camDistance;

		this.camera.position.set(x, y, z);
		this.camera.lookAt(target);
	}

	private updatePlayers(dt: number): void {
		this.players.forEach(player => {
			player.drawPlayer(dt);
		});

		this.npcs.forEach(npc => npc.drawNpc(dt));
	}

	private onGroundItemsClick(items: Item[]) {
		this.modalObject = {
			modalX: this.mouseScreenX + 8,
			modalY: this.mouseScreenY + 8,
			modalOptions: [],
		};

		for (const item of items) {
			const itemData = this.itemsManager.getItemInfoById(item.itemID);
			if (!itemData) continue;

			this.modalObject.modalOptions.push({
				optionText: 'Take ',
				optionSecondaryText: {
					text: itemData.name,
					color: '#ffff66',
				},
				optionFunction: () => {
					this.actions?.takeGroundItem(this.currentPlayerID, item.uniqueID);
					this.modalObject = null;
				},
			});

			this.modalObject.modalOptions.push({
				optionText: 'Examine ',
				optionSecondaryText: {
					text: itemData.name,
					color: '#ffff66',
				},
				optionFunction: () => {
					this.actions?.sendChatMessage(
						this.currentPlayerID,
						itemData.examine || 'Nothing interesting.',
						false,
					);
				},
			});
		}
	}

	private getItemsAtTile(tx: number, ty: number): Item[] {
		return this.items.filter(item => item.worldX === tx && item.worldY === ty);
	}

	private updateMouseTileFromRay(raycaster: THREE.Raycaster): boolean {
		const hits = raycaster.intersectObjects(
			this.groundGroups.flatMap(g => g.children),
			false,
		);

		if (!hits.length) return false;

		const hit = hits[0].object;
		this.mouseTileX = hit.userData.tx;
		this.mouseTileY = hit.userData.tz;
		return true;
	}

	private onPointerDown(e: PointerEvent) {
		if (
			this.modalObject &&
			!(
				this.mouseScreenX < this.modalObject.modalX - 10 ||
				this.mouseScreenX > this.modalObject.modalX + this.modalObject.modalWidth + 10 ||
				this.mouseScreenY < this.modalObject.modalY - 10 ||
				this.mouseScreenY > this.modalObject.modalY + this.modalObject.modalHeight + 10
			)
		) {
			return;
		}

		// Over UI
		if (this.mouseScreenY > 520) {
			return;
		}
		const currentPlayer = this.players.find(p => p.entityID === this.currentPlayerID);
		if (!currentPlayer) return;

		currentPlayer.mouseNdc.set((e.clientX / window.innerWidth) * 2 - 1, -(e.clientY / window.innerHeight) * 2 + 1);
		currentPlayer.raycaster.setFromCamera(currentPlayer.mouseNdc, this.camera);

		if (e.button === 2) {
			const raycaster = currentPlayer.raycaster;

			// --- NPC meshes ---
			const npcMeshes = this.npcs.flatMap(npc => {
				const meshes: THREE.Mesh[] = [];
				npc.model.traverse(obj => {
					if (obj instanceof THREE.Mesh) meshes.push(obj);
				});
				return meshes;
			});

			// --- Player meshes ---
			const playerMeshes = this.players.flatMap(player => {
				const meshes: THREE.Mesh[] = [];
				player.model.traverse(obj => {
					if (obj instanceof THREE.Mesh) meshes.push(obj);
				});
				return meshes;
			});

			// Raycast once
			const hits = raycaster.intersectObjects([...npcMeshes, ...playerMeshes], false);

			// Collect entities
			const npcs = this.collectNpcHits(hits);
			const players = this.collectPlayerHits(hits);

			// Tile lookup (items)
			let items: Item[] = [];
			if (this.updateMouseTileFromRay(raycaster)) {
				items = this.getItemsAtTile(this.mouseTileX, this.mouseTileY);
			}

			// Nothing clicked
			if (!npcs.length && !players.length && !items.length) {
				return;
			}

			this.openStackedClickModal({ npcs, players, items });
			return;
		} else {
			const groundHits = currentPlayer.raycaster.intersectObjects(
				this.groundGroups.flatMap(g => g.children),
				false,
			);

			if (!groundHits.length) return;

			const hitObj = groundHits[0].object;
			const y = hitObj.userData.height;
			const tx = hitObj.userData.tx;
			const tz = hitObj.userData.tz;

			this.actions?.movePlayer(this.currentPlayerID, tx, tz);
			currentPlayer.moveTarget.set(tx, y, tz);
			this.marker.position.set(tx, y + 0.02, tz);
			this.marker.visible = true;
		}
	}

	private openStackedClickModal(data: { npcs: Npc[]; players: Player[]; items: Item[] }) {
		this.modalObject = {
			modalX: this.mouseScreenX + 8,
			modalY: this.mouseScreenY + 8,
			modalOptions: [],
		};

		// --- NPCs ---
		for (const npc of data.npcs) {
			const npcData = this.entitiesManager.getEntityInfoByIndex(npc.entityIndex);
			if (!npcData) continue;

			if (npcData.isTalkable) {
				this.modalObject.modalOptions.push({
					optionText: 'Talk-to ',
					optionSecondaryText: {
						text: npcData.name + ` (Level-${npc.getCombatLevel()})`,
						color: '#ffff66',
					},
					optionFunction: () => {
						this.actions?.moveAndTalk(this.currentPlayerID, npc.entityID);
						this.modalObject = null;
					},
				});
			}

			if (npcData.type === 2) {
				this.modalObject.modalOptions.push({
					optionText: 'Attack ',
					optionSecondaryText: {
						text: npcData.name + ` (Level-${npc.getCombatLevel()})`,
						color: '#ffff66',
					},
					optionFunction: () => {
						this.actions?.moveAndAttack(this.currentPlayerID, npc.entityID);
						this.modalObject = null;
					},
				});
			}

			this.modalObject.modalOptions.push({
				optionText: 'Examine ',
				optionSecondaryText: {
					text: npcData.name + ` (Level-${npc.getCombatLevel()})`,
					color: '#ffff66',
				},
				optionFunction: () => {
					this.actions?.sendChatMessage(this.currentPlayerID, npcData.examine || 'No data', false);
				},
			});
		}

		for (const player of data.players) {
			this.modalObject.modalOptions.push({
				optionText: 'Attack ',
				optionSecondaryText: {
					text: player.name + ` (Level-${player.getCombatLevel()})`,
					color: '#ffff66',
				},
				optionFunction: () => {
					this.actions?.moveAndAttack(this.currentPlayerID, player.entityID);
					this.modalObject = null;
				},
			});

			this.modalObject.modalOptions.push({
				optionText: 'Examine ',
				optionSecondaryText: {
					text: player.name + ` (Level-${player.getCombatLevel()})`,
					color: '#ffff66',
				},
				optionFunction: () => {
					this.actions?.sendChatMessage(this.currentPlayerID, 'Nothing interesting.', false);
				},
			});
		}

		// --- Items ---
		for (const item of data.items) {
			const itemData = this.itemsManager.getItemInfoById(item.itemID);
			if (!itemData) continue;

			this.modalObject.modalOptions.push({
				optionText: 'Take ',
				optionSecondaryText: {
					text: itemData.name,
					color: '#ffff66',
				},
				optionFunction: () => {
					this.actions?.takeGroundItem(this.currentPlayerID, item.uniqueID);
					this.modalObject = null;
				},
			});

			this.modalObject.modalOptions.push({
				optionText: 'Examine ',
				optionSecondaryText: {
					text: itemData.name,
					color: '#ffff66',
				},
				optionFunction: () => {
					this.actions?.sendChatMessage(
						this.currentPlayerID,
						itemData.examine || 'Nothing interesting.',
						false,
					);
				},
			});
		}
	}

	private collectPlayerHits(hits: THREE.Intersection[]): Player[] {
		const set = new Set<Player>();

		for (const hit of hits) {
			const player = hit.object.userData.player as Player | undefined;
			if (player && player.entityID !== this.currentPlayerID) {
				set.add(player);
			}
		}

		return Array.from(set);
	}

	private collectNpcHits(hits: THREE.Intersection[]): Npc[] {
		const set = new Set<Npc>();

		for (const hit of hits) {
			const npc = hit.object.userData.npc as Npc | undefined;
			if (npc) set.add(npc);
		}

		return Array.from(set);
	}
}
