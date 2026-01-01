import { World } from '../world/World';
import Man from './Man';

export default class NpcInteraction {
	private world: World;

	constructor(world: World) {
		this.world = world;
	}

	public startNpcInteraction(npcIndex: number, targetID: string, dialogueNumber: number): void {
		if (npcIndex === 3) {
			new Man(this.world, targetID);
		} else {
			this.world.actions?.sendChatMessage(
				this.world.currentPlayerID,
				"He doesn't seem to want to talk to you.",
				false,
			);
		}
	}
}
