import * as THREE from 'three';
import { World } from '../../world/World';
import { Entity } from '../entity';
import { canvas2d } from '../../graphics/2DCanvas';
import Draw2D from '../../graphics/Draw2D';
import { ExperienceUtils } from '../../util/ExperienceCurve';

export default class Npc extends Entity {
	public entityIndex: number = 0;

	constructor(world: World, entityID: string) {
		super(world, entityID);
		this.setupNpc();
	}

	public update(npc: SocketNpc): void {
		if (npc.npcIndex !== undefined) this.entityIndex = npc.npcIndex;
		super.update(npc);
	}

	public setupNpc(): void {
		const staticData = this.world.entitiesManager.getEntityInfoByIndex(this.entityIndex);
		this.skills = staticData?.skills || [];

		if (staticData?.entityIndex === 1) {
			this.makeZombieModel();
		} else {
			this.makeHumanModel(0x8b1e1e, 0x000000);
		}
		this.model.traverse(obj => {
			if ((obj as THREE.Mesh).isMesh) {
				obj.userData.type = 'npc';
				obj.userData.npc = this;
			}
		});
	}

	public drawNpc(dt: number): void {
		super.draw(dt);
	}

	public onClick() {
		// prevent immediate close
		this.world.canvas.style.pointerEvents = 'auto';

		this.world.modalObject = {
			modalX: this.world.mouseScreenX + 8,
			modalY: this.world.mouseScreenY + 8,
			modalOptions: [],
		};

		const npcData = this.world.entitiesManager.getEntityInfoByIndex(this.entityIndex);
		if (!npcData) {
			this.world.actions?.sendChatMessage(this.world.currentPlayerID, 'No data', false);
			return;
		}

		if (npcData?.isTalkable) {
			this.world.modalObject.modalOptions.push({
				optionText: 'Talk-to ',
				optionSecondaryText: {
					text: npcData.name + ` (Level-${this.getCombatLevel()})`,
					color: '#ffff66',
				},
				optionFunction: () => {
					this.world.actions?.moveAndTalk(this.world.currentPlayerID, this.entityID);
					this.world.modalObject = null;
				},
			});
		}

		if (npcData?.type === 2) {
			this.world.modalObject.modalOptions.push({
				optionText: 'Attack ',
				optionSecondaryText: {
					text: npcData.name + ` (Level-${this.getCombatLevel()})`,
					color: '#ffff66',
				},
				optionFunction: () => {
					this.world.actions?.moveAndAttack(this.world.currentPlayerID, this.entityID);
					this.world.modalObject = null;
				},
			});
		}

		this.world.modalObject.modalOptions.push({
			optionText: 'Examine ',
			optionSecondaryText: {
				text: npcData.name + ` (Level-${this.getCombatLevel()})`,
				color: '#ffff66',
			},
			optionFunction: () => {
				this.world.actions?.sendChatMessage(this.world.currentPlayerID, npcData?.examine || 'No data', false);
			},
		});
	}
}
