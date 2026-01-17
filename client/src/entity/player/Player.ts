import * as THREE from 'three';
import { World } from '../../world/World';
import { Entity } from '../entity';
import NpcInteraction from '../../npcInteractions/NpcInteraction';
import { canvas2d } from '../../graphics/2DCanvas';
import Draw2D from '../../graphics/Draw2D';
import { ExperienceUtils } from '../../util/ExperienceCurve';

export default class Player extends Entity {
	public storyProgress: number = 0;
	private attackTimer: number = 0;
	private readonly ATTACK_DURATION = 0.25; // seconds
	public attackStyle = '';

	constructor(world: World, entityID: string) {
		super(world, entityID);
		this.setupPlayer();
	}

	private setupPlayer(): void {
		this.makeHumanModel(0x335533, 0x222222);
		if (this.world.currentPlayerID !== this.entityID) {
			this.model.traverse(obj => {
				if ((obj as THREE.Mesh).isMesh) {
					obj.userData.type = 'player';
					obj.userData.player = this;
				}
			});
		}
	}

	update(player: SocketPlayer): void {
		if (player.lastTickX !== undefined && player.lastTickY !== undefined) {
			this.lastTickX = player.lastTickX;
			this.lastTickY = player.lastTickY;

			// reset interpolation timer
			this.interpTime = 0;
		}

		if (player.worldX !== undefined) {
			this.worldX = player.worldX;
		}
		if (player.worldY !== undefined) {
			this.worldY = player.worldY;
		}

		if (player.attackStyle !== undefined) {
			this.attackStyle = player.attackStyle;
		}

		if (player.facingDirection !== undefined) {
			this.facingDirection = player.facingDirection;
		}

		if (player.username !== undefined) this.name = player.username;
		if (player.inventory !== undefined) this.inventory = player.inventory;
		if (player.inventoryAmounts !== undefined) this.inventoryAmounts = player.inventoryAmounts;
		if (player.storyProgress !== undefined) this.storyProgress = player.storyProgress;
		if (player.currentHitpoints !== undefined) this.currentHitpoints = player.currentHitpoints;
		if (player.isInCombat !== undefined) this.isInCombat = player.isInCombat;
		if (player.skills !== undefined) this.skills = player.skills;

		if (player.weapon !== undefined) {
			if (player.weapon === -1) {
				this.removeWeapon();
				this.weapon = player.weapon || -1;
			} else {
				const itemIndex = this.inventory[player.weapon];
				this.loadWieldable(itemIndex);
				this.weapon = player.weapon;
			}
		}

		if (player.shield !== undefined) {
			if (player.shield === -1) {
				this.removeShield();
				this.shield = player.shield || -1;
			} else {
				const itemIndex = this.inventory[player.shield];
				this.loadWieldable(itemIndex);
				this.shield = player.shield;
			}
		}

		if (this.isDying && !this.hasDied) {
			this.deathTimer = this.DEATH_DURATION;
			this.hasDied = true;
		}

		// Respawn / revive detected
		if (player.isDying !== undefined && player.isDying === false && this.hasDied) {
			this.resetPose();
		}

		if (!this.isAddedToScene) {
			this.world.scene.add(this.model);
			this.isAddedToScene = true;
		}
	}

	drawPlayer(dt: number): void {
		if (!this.limbs) return;

		this.interpTime += dt;

		const alpha = THREE.MathUtils.clamp(this.interpTime / this.TICK_DURATION, 0, 1);

		// snap on teleport / respawn
		const dist = Math.abs(this.worldX - this.lastTickX) + Math.abs(this.worldY - this.lastTickY);

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

		if (this.isInCombat) {
			const worldPos = this.model.position.clone();
			worldPos.y += 3.2; // above head

			const screenPos = worldPos.project(this.world.camera);

			const screenX = (screenPos.x * 0.5 + 0.5) * canvas2d.canvas.width;
			const screenY = (-screenPos.y * 0.5 + 0.5) * canvas2d.canvas.height;

			const hitpoints = ExperienceUtils.getLevelByExp(this.skills[3]);
			Draw2D.drawHealthBar2D(screenX | 0, screenY | 0, this.currentHitpoints, hitpoints);
		}

		const playerTalkEvents = this.world.talkEvents.filter(event => event.talkerID === this.world.currentPlayerID);

		if (playerTalkEvents.length > 0) {
			new NpcInteraction(this.world).startNpcInteraction(
				playerTalkEvents[0].targetIndex,
				playerTalkEvents[0].targetID,
				playerTalkEvents[0].dialogueNumber,
			);
			// remove talk event
			this.world.talkEvents = this.world.talkEvents.filter(event => event.talkerID !== this.entityID);
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

		if (this.facingDirection) {
			const desiredYaw = this.normalizeYaw(
				this.getYawFromFacing(this.facingDirection as any) + this.MODEL_YAW_OFFSET,
			);

			const currentYaw = this.normalizeYaw(this.model.rotation.y);
			const delta = this.wrapAngleRad(desiredYaw - currentYaw);

			this.model.rotation.y =
				currentYaw + THREE.MathUtils.clamp(delta, -this.TURN_SPEED * dt, this.TURN_SPEED * dt);
		}
	}
}
