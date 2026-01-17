import * as THREE from 'three';
import { World } from '../../world/World';
import { Entity } from '../entity';
import NpcInteraction from '../../npcInteractions/NpcInteraction';
import { canvas2d } from '../../graphics/2DCanvas';
import Draw2D from '../../graphics/Draw2D';
import { ExperienceUtils } from '../../util/ExperienceCurve';

export default class Player extends Entity {
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
		super.update(player);

		if (player.attackStyle !== undefined) this.attackStyle = player.attackStyle;
		if (player.username !== undefined) this.name = player.username;
		if (player.inventory !== undefined) this.inventory = player.inventory;
		if (player.inventoryAmounts !== undefined) this.inventoryAmounts = player.inventoryAmounts;
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
	}

	public drawPlayer(dt: number): void {
		super.draw(dt);
	}
}
