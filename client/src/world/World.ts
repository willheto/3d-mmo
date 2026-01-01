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

export class World {
	public modalObject: ModalObject | null = null;
	public canvas: HTMLCanvasElement;

	// 3D RENDERING
	public renderer: THREE.WebGLRenderer;
	public scene: THREE.Scene;

	private inputTracker: InputTracker;
	public groundGroups: THREE.Group[] = [];

	public eventListenersManager: EventListenersManager = new EventListenersManager();
	public chat: Chat = new Chat(this);

	public mouseDown: boolean;
	public mouseScreenX: number;
	public mouseScreenY: number;
	public mouseTileX: number;
	public mouseTileY: number;

	public gameSocket: WebSocket | null = null;
	public client: Client;
	public currentPlayerID: string = '';
	public players: Player[] = [];
	public npcs: Npc[] = [];
	public items: SocketItem[] = [];
	public modalJustClosed: boolean = false;
	public chatMessages: SocketChatMessage[] = [];

	public entitiesManager: EntitiesManager = new EntitiesManager();
	public tileManager: TileManager = new TileManager(this);

	public actions: Actions | null = null;

	public talkEvents: SocketTalkEvent[] = [];
	public attackEvents: SocketAttackEvent[] = [];
	public soundEvents: SocketSoundEvent[] = [];

	// --- Timing ---
	private running = false;
	private isLoading = false;

	// --- Camera ---
	private readonly CAM_ROT_SPEED = 1.6;
	private readonly MIN_PITCH = 0.35;
	private readonly MAX_PITCH = 1.25;
	public camera: THREE.PerspectiveCamera;
	private camYaw = Math.PI / 4;
	private camPitch = 0.8;
	private camDistance = 14;

	// --- World ---
	public readonly TILE_SIZE = 1;
	public groundGroup = new THREE.Group();

	// --- Marker ---
	public marker: THREE.Mesh;

	private uiCanvas!: HTMLCanvasElement;
	private uiCtx!: CanvasRenderingContext2D;
	private loadingProgress = 0;
	private loadingMessage = 'Loading...';

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
		this.mouseScreenX = 0;
		this.mouseScreenY = 0;
		this.mouseTileX = 0;
		this.mouseTileY = 0;

		const canvas = this.prepareCanvasForRenderer();
		this.canvas = canvas;
		this.renderer = new THREE.WebGLRenderer({
			canvas: canvas,
			antialias: true,
		});

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
		ctx.font = '14px Pkmn';
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

		document.addEventListener('pointerdown', e => {
			this.mouseDown = true;
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

	// world.ts
	update(dt: number) {
		canvas2d.clearRect(0, 0, canvas.width, canvas.height);

		this.updatePlayers(dt);
		this.updateCamera(dt);

		if (this.isLoading) {
			return;
		}

		this.chat.drawChat();
		Draw2D.drawModal(this);
		this.playSoundEvents();
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

	updatePlayers(dt: number) {
		this.players.forEach(player => {
			player.drawPlayer(dt);
		});

		this.npcs.forEach(npc => npc.drawNpc(dt));
	}

	private onPointerDown(e: PointerEvent) {
		const currentPlayer = this.players.find(p => p.entityID === this.currentPlayerID);
		if (!currentPlayer) return;

		currentPlayer.mouseNdc.set((e.clientX / window.innerWidth) * 2 - 1, -(e.clientY / window.innerHeight) * 2 + 1);

		currentPlayer.raycaster.setFromCamera(currentPlayer.mouseNdc, this.camera);

		if (e.button === 2) {
			const npcMeshes = this.npcs.flatMap(npc => {
				const meshes: THREE.Mesh[] = [];
				npc.model.traverse(obj => {
					if (obj instanceof THREE.Mesh) {
						meshes.push(obj);
					}
				});
				return meshes;
			});

			const npcHits = currentPlayer.raycaster.intersectObjects(npcMeshes, false);

			if (npcHits.length) {
				const hit = npcHits[0].object;

				const npc = hit.userData.npc as Npc;

				this.onNpcClick(npc);
				return;
			}
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

	private onNpcClick(npc: Npc) {
		// prevent immediate close
		if (this.modalJustClosed) return;
		canvas.style.pointerEvents = 'auto';

		this.modalObject = {
			modalX: this.mouseScreenX + 8,
			modalY: this.mouseScreenY + 8,
			modalOptions: [],
		};

		const npcData = this.entitiesManager.getEntityInfoByIndex(npc.entityIndex);
		if (!npcData) {
			this.actions?.sendChatMessage(this.currentPlayerID, 'No data', false);
			return;
		}

		if (npcData?.isTalkable) {
			this.modalObject.modalOptions.push({
				optionText: 'Talk-to ',
				optionSecondaryText: {
					text: npcData.name,
					color: '#ffff66',
				},
				optionFunction: () => {
					this.actions?.moveAndTalk(this.currentPlayerID, npc.entityID);
					this.modalObject = null;
				},
			});
		}

		if (npcData?.type === 2) {
			this.modalObject.modalOptions.push({
				optionText: 'Attack ',
				optionSecondaryText: {
					text: npcData.name,
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
				text: npcData.name,
				color: '#66ff66',
			},
			optionFunction: () => {
				this.actions?.sendChatMessage(this.currentPlayerID, npcData?.examine || 'No data', false);
			},
		});
	}
}
