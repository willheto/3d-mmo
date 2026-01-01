import * as THREE from 'three';
import { World } from '../../world/World';
import { Entity } from '../entity';
import { canvas2d } from '../../graphics/2DCanvas';
import Draw2D from '../../graphics/Draw2D';

export default class Npc extends Entity {
	private attackTimer: number = 0;
	public entityIndex: number = 0;

	private readonly ATTACK_DURATION = 0.25; // seconds

	constructor(world: World, entityID: string) {
		super(world, entityID);
		this.setupNpc();
	}

	update(npc: SocketNpc): void {
		this.worldX = npc.worldX;
		this.worldY = npc.worldY;
		this.currentChunk = npc.currentChunk;
		this.nextTileDirection = npc.nextTileDirection;
		this.facingDirection = npc.facingDirection;
		this.currentHitpoints = npc.currentHitpoints;
		this.isInCombat = npc.isInCombat;
		this.entityIndex = npc.entityIndex;
		this.isDying = npc.isDying;

		if (this.isDying && !this.hasDied) {
			this.deathTimer = this.DEATH_DURATION;
			this.hasDied = true;
		}

		// Respawn / revive detected
		if (!npc.isDying && this.hasDied) {
			this.resetPose();
		}

		if (!this.isAddedToScene) {
			this.world.scene.add(this.model);
			this.isAddedToScene = true;
		}
	}

	setupNpc() {
		this.makeHumanModel(0x8b1e1e, 0x000000);
		this.model.traverse(obj => {
			if ((obj as THREE.Mesh).isMesh) {
				obj.userData.type = 'npc';
				obj.userData.npc = this;
			}
		});
	}

	drawNpc(dt: number): void {
		if (!this.limbs) return;

		if (this.isInCombat) {
			const worldPos = this.model.position.clone();
			worldPos.y += 3.2; // above head

			const screenPos = worldPos.project(this.world.camera);

			const screenX = (screenPos.x * 0.5 + 0.5) * canvas2d.canvas.width;
			const screenY = (-screenPos.y * 0.5 + 0.5) * canvas2d.canvas.height;

			Draw2D.drawHealthBar2D(screenX | 0, screenY | 0, this.currentHitpoints, 10);
		}

		if (this.facingDirection) {
			const desiredYaw = this.normalizeYaw(
				this.getYawFromFacing(this.facingDirection as any) + this.MODEL_YAW_OFFSET,
			);

			const currentYaw = this.normalizeYaw(this.model.rotation.y);
			const delta = this.wrapAngleRad(desiredYaw - currentYaw);

			this.model.rotation.y =
				currentYaw + THREE.MathUtils.clamp(delta, -this.TURN_SPEED * dt, this.TURN_SPEED * dt);
		}
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

		const playerAttackEvents = this.world.attackEvents.filter(event => event.attackerID === this.entityID);

		if (playerAttackEvents.length > 0 && this.attackTimer <= 0) {
			this.attackTimer = this.ATTACK_DURATION;

			// remove event so it doesn't retrigger
			this.world.attackEvents = this.world.attackEvents.filter(event => event.attackerID !== this.entityID);
		}

		if (this.attackTimer > 0 && this.limbs) {
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

			return; // IMPORTANT: skip walk animation this frame
		}

		if (this?.nextTileDirection) {
			this.walkTime += dt * 8;

			let nextX = this.worldX;
			let nextY = this.worldY;

			switch (this.nextTileDirection) {
				case 'UP':
					nextY -= 1;
					break;
				case 'DOWN':
					nextY += 1;
					break;
				case 'LEFT':
					nextX -= 1;
					break;
				case 'RIGHT':
					nextX += 1;
					break;
			}

			const npcTile = this.worldToTile(this.model.position);
			const targetTile = this.worldToTile(new THREE.Vector3(nextX, 0, nextY));

			// reached final tile
			if (npcTile.x === targetTile.x && npcTile.z === targetTile.z && !this.currentTileTarget) {
				this.model.position.copy(this.tileToWorld(npcTile.x, npcTile.z));
				this.world.marker.visible = false;
				return;
			}

			// pick next tile if needed
			if (!this.currentTileTarget) {
				const dx = targetTile.x - npcTile.x;
				const dz = targetTile.z - npcTile.z;

				const stepX = THREE.MathUtils.clamp(dx, -1, 1);
				const stepZ = THREE.MathUtils.clamp(dz, -1, 1);

				const nextTx = npcTile.x + stepX;
				const nextTz = npcTile.z + stepZ;

				const height = this.world.tileManager.getTileHeight(nextTx, nextTz);

				this.currentTileTarget = new THREE.Vector3(
					nextTx * this.world.TILE_SIZE,
					height,
					nextTz * this.world.TILE_SIZE,
				);
			}

			// move toward current tile target
			const dir = this.currentTileTarget.clone().sub(this.model.position);
			const dist = dir.length();

			if (dist <= this.STOP_RADIUS) {
				this.model.position.copy(this.currentTileTarget);
				this.currentTileTarget = null;
				return;
			}

			dir.normalize();

			const step = this.SPEED * dt;
			this.model.position.addScaledVector(dir, Math.min(step, dist));

			const swing = Math.sin(this.walkTime) * 0.6;
			this.limbs.leftArm.rotation.x = swing;
			this.limbs.rightArm.rotation.x = -swing;
			this.limbs.leftLeg.rotation.x = -swing;
			this.limbs.rightLeg.rotation.x = swing;
		} else {
			const height = this.world.tileManager.getTileHeight(this.worldX, this.worldY);

			const expectedWorldPos = new THREE.Vector3(
				this.worldX * this.world.TILE_SIZE,
				height,
				this.worldY * this.world.TILE_SIZE,
			);
			const drift = this.model.position.distanceTo(expectedWorldPos);

			if (drift > 0.001) {
				this.model.position.copy(expectedWorldPos);
				this.currentTileTarget = null;
				this.walkTime = 0;
			}

			// settle limbs
			for (const limb of Object.values(this.limbs)) {
				limb.rotation.x *= 0.8;
			}
			return;
		}
	}
}
