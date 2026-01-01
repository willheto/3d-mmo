import * as THREE from 'three';
import { World } from '../world/World';

export class Entity {
	protected readonly MODEL_YAW_OFFSET = -Math.PI;
	protected readonly SPEED = 1.6;
	protected readonly STOP_RADIUS = 0.08;
	protected readonly TURN_SPEED = 6;

	protected isAddedToScene = false;
	private legHeight = 1.05;
	private legTopWidth = 0.25;
	private legBottomWidth = 0.35;
	private legDepth = 0.22;

	protected deathTimer: number = 0;
	protected readonly DEATH_DURATION = 0.6; // seconds
	protected hasDied: boolean = false;

	protected world: World;
	public nextTileDirection: Direction | null = null;
	public entityID: string;
	public worldX: number = 0;
	public worldY: number = 0;
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

	public model = new THREE.Group();
	public limbs!: {
		leftArm: THREE.Group;
		rightArm: THREE.Group;
		leftLeg: THREE.Mesh;
		rightLeg: THREE.Mesh;
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

	protected getYawFromFacing(dir: 'UP' | 'DOWN' | 'LEFT' | 'RIGHT'): number {
		switch (dir) {
			case 'DOWN':
				return 0; // +Z
			case 'UP':
				return Math.PI; // -Z
			case 'RIGHT':
				return Math.PI / 2; // +X
			case 'LEFT':
				return -Math.PI / 2; // -X
		}
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

		// Single triangle geometry
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

	private makeArms(armsColor: number): void {
		const clothMat = new THREE.MeshStandardMaterial({
			color: armsColor, // slightly dark green
			flatShading: true,
		});

		const armHeight = 1.15;
		const armGeo = new THREE.BoxGeometry(0.2, armHeight, 0.23);

		const shoulderY = 2.05; // adjust if torso changes
		const shoulderZ = 0.05;

		const leftShoulder = new THREE.Group();
		leftShoulder.position.set(-0.5, shoulderY, shoulderZ);
		this.model.add(leftShoulder);

		const rightShoulder = new THREE.Group();
		rightShoulder.position.set(0.5, shoulderY, shoulderZ);
		this.model.add(rightShoulder);
		const leftArm = new THREE.Mesh(armGeo, clothMat);
		leftArm.position.y = -armHeight / 2;
		leftShoulder.add(leftArm);

		const rightArm = new THREE.Mesh(armGeo, clothMat);
		rightArm.position.y = -armHeight / 2;
		rightShoulder.add(rightArm);

		this.limbs.rightArm = rightShoulder;
		this.limbs.leftArm = leftShoulder;
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
}
