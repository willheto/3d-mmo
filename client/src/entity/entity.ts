import * as THREE from 'three';
import { World } from '../world/World';
import { ExperienceUtils } from '../util/ExperienceCurve';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import Cache from '../cache/index';
import Draw2D from '../graphics/Draw2D';
import { canvas2d } from '../graphics/2DCanvas';

type Direction = 'NONE' | 'UP' | 'DOWN' | 'LEFT' | 'RIGHT' | 'UP_LEFT' | 'UP_RIGHT' | 'DOWN_LEFT' | 'DOWN_RIGHT';

const DIR_VECS: Record<Direction, { dx: number; dy: number }> = {
	NONE: { dx: 0, dy: 0 },
	UP: { dx: 0, dy: -1 },
	DOWN: { dx: 0, dy: 1 },
	LEFT: { dx: -1, dy: 0 },
	RIGHT: { dx: 1, dy: 0 },
	UP_LEFT: { dx: -1, dy: -1 },
	UP_RIGHT: { dx: 1, dy: -1 },
	DOWN_LEFT: { dx: -1, dy: 1 },
	DOWN_RIGHT: { dx: 1, dy: 1 },
};

export class Entity {
	protected readonly MODEL_YAW_OFFSET = -Math.PI;
	protected readonly SPEED = 1.6;
	protected readonly STOP_RADIUS = 0.08;
	protected readonly TURN_SPEED = 6;
	protected readonly DEATH_DURATION = 0.6; // seconds
	protected readonly TICK_DURATION = 0.6; // must match server (600ms)
	protected readonly ATTACK_DURATION = 0.25; // seconds

	protected attackTimer: number = 0;

	protected deathTimer: number = 0;
	protected hasDied: boolean = false;

	protected world: World;
	public entityID: string;
	public worldX: number = 0;
	public worldY: number = 0;
	protected lastTickX = 0;
	protected lastTickY = 0;

	protected interpTime = 0;

	public name: string = '';
	public type: number = 0;
	public facingDirection: string = 'DOWN';
	public currentChunk: number = 0;
	public inventory: number[] = [];
	public inventoryAmounts: number[] = [];
	public currentHitpoints: number = 0;
	public isInCombat: boolean = false;
	public isDying = false;
	public skills: number[] = [];
	public examine: string = '';

	public weapon: number = -1;
	public shield: number = -1;

	public model = new THREE.Group();

	protected equippedWeapon: THREE.Object3D | null = null;
	protected equippedShield: THREE.Object3D | null = null;

	protected isAddedToScene = false;
	private legHeight = 1.05;
	private legTopWidth = 0.25;
	private legBottomWidth = 0.35;
	private legDepth = 0.22;

	public limbs!: {
		leftArm: THREE.Group;
		rightArm: THREE.Group;
		leftLeg: THREE.Mesh;
		rightLeg: THREE.Mesh;
		rightHand: THREE.Group;
		leftHand: THREE.Group;
	};

	public raycaster = new THREE.Raycaster();
	public mouseNdc = new THREE.Vector2();
	public moveTarget = new THREE.Vector3();
	protected walkTime = 0;

	protected currentTileTarget: THREE.Vector3 | null = null;

	constructor(world: World, entityID: string) {
		this.entityID = entityID;
		this.world = world;
	}

	public update(entity: SocketEntity) {
		if (entity.lastTickX !== undefined && entity.lastTickY !== undefined) {
			this.lastTickX = entity.lastTickX;
			this.lastTickY = entity.lastTickY;
			this.interpTime = 0;
		}

		if (entity.worldX !== undefined) this.worldX = entity.worldX;
		if (entity.worldY !== undefined) this.worldY = entity.worldY;
		if (entity.facingDirection !== undefined) this.facingDirection = entity.facingDirection;
		if (entity.currentHitpoints !== undefined) this.currentHitpoints = entity.currentHitpoints;
		if (entity.isDying !== undefined) this.isDying = entity.isDying;
		if (entity.isInCombat !== undefined) this.isInCombat = entity.isInCombat;
		if (entity.isDying !== undefined && entity.isDying === false && this.hasDied) this.resetPose();

		// handle attack events first

		// then death
		if (this.isDying && !this.hasDied) {
			if (this.attackTimer > 0) {
				// wait
			} else {
				this.deathTimer = this.DEATH_DURATION;
				this.hasDied = true;
				this.attackTimer = 0; // hard cancel any late-start
			}
		}

		if (!this.isAddedToScene) {
			this.world.scene.add(this.model);
			this.isAddedToScene = true;
		}
	}

	protected resetPose(): void {
		if (!this.limbs) return;

		// Reset model rotation
		this.model.rotation.set(0, 0, 0);

		// Reset limb rotations
		for (const limb of Object.values(this.limbs)) {
			limb.rotation.set(0, 0, 0);
		}

		// Reset death state
		this.deathTimer = 0;
		this.hasDied = false;
	}

	protected worldToTile(pos: THREE.Vector3) {
		return {
			x: Math.floor((pos.x + this.world.TILE_SIZE / 2) / this.world.TILE_SIZE),
			z: Math.floor((pos.z + this.world.TILE_SIZE / 2) / this.world.TILE_SIZE),
		};
	}

	protected tileToWorld(tx: number, tz: number) {
		const height = this.world.tileManager.getTileHeight(tx, tz);
		return new THREE.Vector3(tx * this.world.TILE_SIZE, height, tz * this.world.TILE_SIZE);
	}

	protected wrapAngleRad(a: number): number {
		a = (a + Math.PI) % (Math.PI * 2);
		if (a < 0) a += Math.PI * 2;
		return a - Math.PI;
	}

	protected normalizeYaw(a: number): number {
		return this.wrapAngleRad(a);
	}

	protected yawFromFacing(dir: Direction): number {
		const { dx, dy } = DIR_VECS[dir];
		return Math.atan2(-dx, dy);
	}

	protected makeZombieModel(): void {
		this.limbs = {} as any;

		const skinMat = new THREE.MeshStandardMaterial({
			color: 0x6f8463, // undead green
			flatShading: true,
		});

		const clothMat = new THREE.MeshStandardMaterial({
			color: 0x2f3b2f, // dark ragged cloth
			flatShading: true,
		});

		// ===== LEGS =====
		const legHeight = 1.05;
		const legGeo = new THREE.BoxGeometry(1, legHeight, 0.22);
		const legPos = legGeo.attributes.position;

		for (let i = 0; i < legPos.count; i++) {
			const y = legPos.getY(i);
			const t = (y + legHeight / 2) / legHeight;
			const scale = THREE.MathUtils.lerp(0.35, 0.22, t); // OSRS taper
			legPos.setX(i, legPos.getX(i) * scale);
		}

		legGeo.computeVertexNormals();

		const hipY = legHeight + 0.02;

		const leftLeg = new THREE.Mesh(legGeo, clothMat);
		leftLeg.position.set(-0.22, hipY - legHeight / 2, 0);
		this.model.add(leftLeg);

		const rightLeg = new THREE.Mesh(legGeo, clothMat);
		rightLeg.position.set(0.22, hipY - legHeight / 2, 0);
		this.model.add(rightLeg);

		// ===== TORSO =====
		const torsoHeight = 1.05;
		const torsoGeo = new THREE.BoxGeometry(1, torsoHeight, 0.28);
		const torsoPos = torsoGeo.attributes.position;

		for (let i = 0; i < torsoPos.count; i++) {
			const y = torsoPos.getY(i);
			const t = (y + torsoHeight / 2) / torsoHeight;
			const scale = THREE.MathUtils.lerp(0.65, 0.8, t);
			torsoPos.setX(i, torsoPos.getX(i) * scale);
		}

		torsoGeo.computeVertexNormals();

		const torso = new THREE.Mesh(torsoGeo, clothMat);
		torso.position.y = hipY + torsoHeight / 2;
		torso.rotation.x = 0.12; // slight hunch
		this.model.add(torso);

		// ===== HEAD =====
		const headGeo = new THREE.IcosahedronGeometry(0.26, 0);
		const head = new THREE.Mesh(headGeo, skinMat);
		head.scale.set(1.15, 1.35, 1.0);
		head.position.set(0, torso.position.y + torsoHeight / 2 + 0.38, -0.05);
		head.rotation.x = -0.15;
		this.model.add(head);

		// ===== ARMS =====
		const armHeight = 1.15;
		const armGeo = new THREE.BoxGeometry(1, armHeight, 0.22);
		const armPos = armGeo.attributes.position;

		for (let i = 0; i < armPos.count; i++) {
			const y = armPos.getY(i);
			const t = (y + armHeight / 2) / armHeight;
			const scale = THREE.MathUtils.lerp(0.32, 0.2, t);
			armPos.setX(i, armPos.getX(i) * scale);
		}

		armGeo.computeVertexNormals();

		const shoulderY = torso.position.y + torsoHeight / 2 - 0.15;
		const shoulderZ = 0.06;
		const shoulderX = 0.48;

		// Left shoulder
		const leftShoulder = new THREE.Group();
		leftShoulder.position.set(-shoulderX, shoulderY, shoulderZ);
		this.model.add(leftShoulder);

		const leftArm = new THREE.Group();
		leftShoulder.add(leftArm);

		const leftArmMesh = new THREE.Mesh(armGeo, skinMat);
		leftArmMesh.position.y = -armHeight / 2;
		leftArm.add(leftArmMesh);

		leftArm.rotation.x = 0.45;
		leftArm.rotation.z = 0.15;

		const leftHand = new THREE.Group();
		leftHand.position.set(0, -armHeight, 0);
		leftArm.add(leftHand);

		// Right shoulder
		const rightShoulder = new THREE.Group();
		rightShoulder.position.set(shoulderX, shoulderY, shoulderZ);
		this.model.add(rightShoulder);

		const rightArm = new THREE.Group();
		rightShoulder.add(rightArm);

		const rightArmMesh = new THREE.Mesh(armGeo, skinMat);
		rightArmMesh.position.y = -armHeight / 2;
		rightArm.add(rightArmMesh);

		rightArm.rotation.x = 0.25;
		rightArm.rotation.z = -0.18;

		const rightHand = new THREE.Group();
		rightHand.position.set(0, -armHeight, 0);
		rightArm.add(rightHand);

		// ===== STORE LIMBS =====
		this.limbs.leftArm = leftArm;
		this.limbs.rightArm = rightArm;
		this.limbs.leftHand = leftHand;
		this.limbs.rightHand = rightHand;
		this.limbs.leftLeg = leftLeg as any;
		this.limbs.rightLeg = rightLeg as any;
	}

	protected makeHumanModel(shirtColor: number, legsColor: number): void {
		this.limbs = {};
		this.makeLegs(legsColor);
		this.makeUpperBody(shirtColor);
		this.makeHead();
		this.makeArms(shirtColor);

		const forward = new THREE.Vector3(0, 0, 1);
		forward.applyQuaternion(this.model.quaternion);
	}

	private makeEyes(
		head: THREE.Mesh<THREE.IcosahedronGeometry, THREE.MeshStandardMaterial, THREE.Object3DEventMap>,
	): void {
		const eyeMat = new THREE.MeshStandardMaterial({
			color: 0x000000,
			flatShading: true,
			side: THREE.DoubleSide,
		});

		const eyeGeo = new THREE.BufferGeometry();
		const vertices = new Float32Array([
			-0.06,
			0.03,
			0.0, // left
			0.06,
			0.03,
			0.0, // right
			0.0,
			-0.04,
			0.0, // bottom
		]);
		eyeGeo.setAttribute('position', new THREE.BufferAttribute(vertices, 3));
		eyeGeo.computeVertexNormals();

		const eyeY = 0.06;
		const eyeZ = -0.235;
		const eyeX = 0.085;

		// Left eye
		const leftEye = new THREE.Mesh(eyeGeo, eyeMat);
		leftEye.position.set(-eyeX, eyeY, eyeZ);
		head.add(leftEye);

		// Right eye
		const rightEye = new THREE.Mesh(eyeGeo, eyeMat);
		rightEye.position.set(eyeX, eyeY, eyeZ);
		head.add(rightEye);
	}

	protected async loadWieldable(itemIndex: number): Promise<void> {
		const loader = new GLTFLoader();
		const itemData = this.world.itemsManager.getItemInfoById(itemIndex);
		if (!itemData) {
			this.world.actions?.sendChatMessage(this.world.currentPlayerID, 'No item data on cache.', false);
			return;
		}
		const model = await Cache.getObjectURLByAssetName(itemData.modelName);
		if (!model) {
			this.world.actions?.sendChatMessage(this.world.currentPlayerID, 'No model for item available.', false);
			return;
		}
		loader.load(
			new URL(model, import.meta.url).href,
			gltf => {
				if (itemData.type === 'weapon') {
					this.attachWeapon(gltf.scene);
				}

				if (itemData.type === 'shield') {
					this.attachShield(gltf.scene);
				}
			},
			undefined,
			err => console.error(err),
		);
	}

	protected removeWeapon(): void {
		if (!this.limbs.rightHand || !this.equippedWeapon) return;

		this.limbs.rightHand.remove(this.equippedWeapon);

		this.equippedWeapon.traverse(obj => {
			if ((obj as THREE.Mesh).geometry) {
				(obj as THREE.Mesh).geometry.dispose();
			}
			if ((obj as THREE.Mesh).material) {
				const mat = (obj as THREE.Mesh).material;
				if (Array.isArray(mat)) {
					mat.forEach(m => m.dispose());
				} else {
					mat.dispose();
				}
			}
		});

		this.equippedWeapon = null;
	}

	protected removeShield(): void {
		if (!this.limbs.leftHand || !this.equippedShield) return;

		this.limbs.leftHand.remove(this.equippedShield);

		this.equippedShield?.traverse(obj => {
			if ((obj as THREE.Mesh).geometry) {
				(obj as THREE.Mesh).geometry.dispose();
			}
			if ((obj as THREE.Mesh).material) {
				const mat = (obj as THREE.Mesh).material;
				if (Array.isArray(mat)) {
					mat.forEach(m => m.dispose());
				} else {
					mat.dispose();
				}
			}
		});

		this.equippedShield = null;
	}

	private attachShield(shield: THREE.Object3D): void {
		if (!this.limbs.leftHand) return;

		this.removeShield();

		shield.position.set(0, 0, 0);
		shield.rotation.set(0, 0, 0);
		shield.scale.set(1, 1, 1);

		this.limbs.leftHand.add(shield);
		this.equippedShield = shield;
	}

	private attachWeapon(weapon: THREE.Object3D): void {
		if (!this.limbs.rightHand) return;

		this.removeWeapon();

		weapon.position.set(0, 0, 0);
		weapon.rotation.set(0, 0, 0);
		weapon.scale.set(1, 1, 1);

		this.limbs.rightHand.add(weapon);
		this.equippedWeapon = weapon;
	}

	private makeArms(armsColor: number): void {
		const clothMat = new THREE.MeshStandardMaterial({
			color: armsColor,
			flatShading: true,
		});

		const armHeight = 1.15;
		const armGeo = new THREE.BoxGeometry(0.2, armHeight, 0.23);

		const shoulderY = 2.05;
		const shoulderZ = 0.05;

		// ===== LEFT ARM =====
		const leftShoulder = new THREE.Group();
		leftShoulder.position.set(-0.5, shoulderY, shoulderZ);
		this.model.add(leftShoulder);

		const leftUpperArm = new THREE.Group();
		leftShoulder.add(leftUpperArm);

		const leftArmMesh = new THREE.Mesh(armGeo, clothMat);
		leftArmMesh.position.y = -armHeight / 2;
		leftUpperArm.add(leftArmMesh);

		const leftHand = new THREE.Group();
		leftHand.position.set(0, -armHeight, 0);
		leftUpperArm.add(leftHand);

		// ===== RIGHT ARM =====
		const rightShoulder = new THREE.Group();
		rightShoulder.position.set(0.5, shoulderY, shoulderZ);
		this.model.add(rightShoulder);

		const rightUpperArm = new THREE.Group();
		rightShoulder.add(rightUpperArm);

		const rightArmMesh = new THREE.Mesh(armGeo, clothMat);
		rightArmMesh.position.y = -armHeight / 2;
		rightUpperArm.add(rightArmMesh);

		const rightHand = new THREE.Group();
		rightHand.position.set(0, -armHeight, 0);
		rightUpperArm.add(rightHand);

		// ===== STORE REFERENCES =====
		this.limbs.leftArm = leftUpperArm;
		this.limbs.rightArm = rightUpperArm;
		this.limbs.leftHand = leftHand;
		this.limbs.rightHand = rightHand;
	}

	private makeHead(): void {
		const headGeo = new THREE.IcosahedronGeometry(0.25, 0);
		const headMat = new THREE.MeshStandardMaterial({
			color: 0xffccaa,
			flatShading: true,
		});

		const head = new THREE.Mesh(headGeo, headMat);
		head.scale.set(1.2, 1.5, 1.0);
		head.position.y = 2.45;
		this.model.add(head);

		this.makeEyes(head);
	}

	private makeUpperBody(shirtColor: number): void {
		const torsoHeight = 1.1;
		const bottomWidth = 0.65;
		const topWidth = 0.8;
		const depth = 0.25;

		const geometry = new THREE.BoxGeometry(1, torsoHeight, depth);
		const pos = geometry.attributes.position;

		// widen top vertices
		for (let i = 0; i < pos.count; i++) {
			const y = pos.getY(i);
			const t = (y + torsoHeight / 2) / torsoHeight; // 0 bottom → 1 top
			const scale = THREE.MathUtils.lerp(bottomWidth, topWidth, t);

			pos.setX(i, pos.getX(i) * scale);
		}

		geometry.computeVertexNormals();

		const clothMat = new THREE.MeshStandardMaterial({
			color: shirtColor, // slightly dark green
			flatShading: true,
		});

		const torso = new THREE.Mesh(geometry, clothMat);
		torso.position.y = this.legHeight + torsoHeight / 2;
		this.model.add(torso);
	}

	private makeLegs(legsColor: number) {
		const legGeo = new THREE.BoxGeometry(1, this.legHeight, this.legDepth);
		const legPos = legGeo.attributes.position;

		for (let i = 0; i < legPos.count; i++) {
			const y = legPos.getY(i);
			const t = (y + this.legHeight / 2) / this.legHeight; // 0 = bottom, 1 = top
			const scale = THREE.MathUtils.lerp(this.legBottomWidth, this.legTopWidth, t);

			legPos.setX(i, legPos.getX(i) * scale);
		}

		legGeo.computeVertexNormals();

		const hipY = this.legHeight + 0.02; // hips sit slightly inside torso
		const hipZ = 0;

		const legMat = new THREE.MeshStandardMaterial({
			color: legsColor,
			flatShading: true,
		});

		// Left hip
		const leftHip = new THREE.Group();
		leftHip.position.set(-0.22, hipY, hipZ);
		this.model.add(leftHip);

		const leftLeg = new THREE.Mesh(legGeo, legMat);
		leftLeg.position.y = -this.legHeight / 2;
		leftHip.add(leftLeg);

		// Right hip
		const rightHip = new THREE.Group();
		rightHip.position.set(0.22, hipY, hipZ);
		this.model.add(rightHip);

		const rightLeg = new THREE.Mesh(legGeo, legMat);
		rightLeg.position.y = -this.legHeight / 2;
		rightHip.add(rightLeg);

		this.limbs.leftLeg = leftLeg;
		this.limbs.rightLeg = rightLeg;
	}

	public getCombatLevel(): number {
		const hitpointsLevel = ExperienceUtils.getLevelByExp(this.skills[3]);
		const attackLevel = ExperienceUtils.getLevelByExp(this.skills[1]);
		const strengthLevel = ExperienceUtils.getLevelByExp(this.skills[2]);
		const defenceLevel = ExperienceUtils.getLevelByExp(this.skills[4]);

		const base = 0.25 * (defenceLevel + hitpointsLevel);
		const melee = 0.325 * (attackLevel + strengthLevel);

		return Math.floor(base + melee);
	}

	protected draw(dt: number): void {
		if (!this.limbs) return;

		if (this.hasDied && this.deathTimer > 0 && this.limbs) {
			this.deathTimer -= dt;

			const t = 1 - this.deathTimer / this.DEATH_DURATION;
			const ease = t * t; // quadratic ease-in

			// Fall forward
			this.model.rotation.x = THREE.MathUtils.lerp(0, -Math.PI / 2.2, ease);

			// Slight sideways twist (looks more natural)
			this.model.rotation.z = THREE.MathUtils.lerp(0, 0.25, ease);

			// Collapse limbs
			this.limbs.leftArm.rotation.x = THREE.MathUtils.lerp(this.limbs.leftArm.rotation.x, -Math.PI * 0.9, ease);
			this.limbs.rightArm.rotation.x = THREE.MathUtils.lerp(this.limbs.rightArm.rotation.x, -Math.PI * 0.9, ease);

			this.limbs.leftLeg.rotation.x = THREE.MathUtils.lerp(this.limbs.leftLeg.rotation.x, Math.PI * 0.4, ease);
			this.limbs.rightLeg.rotation.x = THREE.MathUtils.lerp(this.limbs.rightLeg.rotation.x, Math.PI * 0.2, ease);

			return; // HARD STOP — no walking / attacking
		}

		if (this.hasDied && this.deathTimer <= 0) {
			// Lock corpse pose
			this.model.rotation.x = -Math.PI / 2.2;
			this.model.rotation.z = 0.25;

			for (const limb of Object.values(this.limbs)) {
				limb.rotation.x *= 0.95;
			}

			return;
		}

		const playerAttackEvents = this.world.attackEvents.filter(e => e.attackerID === this.entityID);

		if (playerAttackEvents.length > 0 && this.attackTimer <= 0 && !this.isDying && !this.hasDied) {
			this.attackTimer = this.ATTACK_DURATION;
			this.world.attackEvents = this.world.attackEvents.filter(e => e.attackerID !== this.entityID);
		}

		if (this.attackTimer > 0 && !this.hasDied && !this.isDying && this.limbs) {
			this.attackTimer -= dt;

			const t = 1 - this.attackTimer / this.ATTACK_DURATION;
			// Fast forward swing: ease-out
			const swing = Math.sin(t * -Math.PI) * 1.4;

			// Decide attack arm (right-handed)
			this.limbs.rightArm.rotation.x = -swing;
			this.limbs.leftArm.rotation.x = swing * 0.25;

			// Slight torso involvement (optional but looks better)
			this.model.rotation.y += Math.sin(t * Math.PI) * 0.05;

			// Lock legs during attack
			this.limbs.leftLeg.rotation.x *= 0.5;
			this.limbs.rightLeg.rotation.x *= 0.5;
		}

		if (this.isInCombat) {
			const worldPos = this.model.position.clone();
			worldPos.y += 3.2; // above head

			const screenPos = worldPos.project(this.world.camera);

			const screenX = (screenPos.x * 0.5 + 0.5) * canvas2d.canvas.width;
			const screenY = (-screenPos.y * 0.5 + 0.5) * canvas2d.canvas.height;

			const hitpoints = ExperienceUtils.getLevelByExp(this.skills[3]);

			Draw2D.drawHealthBar2D(screenX | 0, screenY | 0, this.currentHitpoints, hitpoints);
		}

		this.interpTime += dt;

		const alpha = THREE.MathUtils.clamp(this.interpTime / this.TICK_DURATION, 0, 1);

		const dx = Math.abs(this.worldX - this.lastTickX);
		const dy = Math.abs(this.worldY - this.lastTickY);
		const dist = Math.max(dx, dy);

		if (dist > 1) {
			const h = this.world.tileManager.getTileHeight(this.worldX, this.worldY);
			this.model.position.set(this.worldX * this.world.TILE_SIZE, h, this.worldY * this.world.TILE_SIZE);
		} else {
			const ix = THREE.MathUtils.lerp(this.lastTickX, this.worldX, alpha);
			const iy = THREE.MathUtils.lerp(this.lastTickY, this.worldY, alpha);

			const tx = Math.round(ix);
			const tz = Math.round(iy);
			const h = this.world.tileManager.getTileHeight(tx, tz);

			this.model.position.set(ix * this.world.TILE_SIZE, h, iy * this.world.TILE_SIZE);
		}

		const moving = dist <= 1 && alpha < 1; // within-tick interpolation active

		if (moving && this.attackTimer <= 0 && !this.hasDied) {
			this.walkTime += dt * 8;
			const swing = Math.sin(this.walkTime) * 0.6;

			this.limbs.leftArm.rotation.x = swing;
			this.limbs.rightArm.rotation.x = -swing;
			this.limbs.leftLeg.rotation.x = -swing;
			this.limbs.rightLeg.rotation.x = swing;
		} else {
			// settle limbs when idle (but not during attack/death returns)
			for (const limb of Object.values(this.limbs)) {
				limb.rotation.x *= 0.8;
			}
		}

		if (this.facingDirection) {
			const raw = this.yawFromFacing(this.facingDirection as Direction);

			const INVERT_YAW = true;
			const desiredYaw = this.normalizeYaw((INVERT_YAW ? -raw : raw) + this.MODEL_YAW_OFFSET);

			const currentYaw = this.normalizeYaw(this.model.rotation.y);
			const delta = this.wrapAngleRad(desiredYaw - currentYaw);

			this.model.rotation.y =
				currentYaw + THREE.MathUtils.clamp(delta, -this.TURN_SPEED * dt, this.TURN_SPEED * dt);
		}
	}
}
