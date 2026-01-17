import { canvas } from '../graphics/2DCanvas';
import { World } from '../world/World';

export abstract class Interaction {
	protected world: World;

	protected npcName: string = '';
	protected targetID: string;
	constructor(world: World, targetID: string, npcName: string) {
		this.world = world;
		this.targetID = targetID;
		this.npcName = npcName;
	}

	abstract startDialogue(): void;

	protected endDialogue(): void {
		this.world.chat.talkModal = null;
	}

	protected async npcSays(text: string): Promise<void> {
		return new Promise(resolve => {
			this.world.chat.talkModal = {
				talkName: this.npcName,
				currentDialogue: {
					dialogueText: text,
					clickToContinueFunction: (): void => {
						if (!this.world.chat.talkModal) {
							throw new Error('Hud not found in world.client');
						}
						resolve();
					},
				},
				dialogueOptions: [],
			};
		});
	}

	protected async playerSays(text: string): Promise<void> {
		return new Promise(resolve => {
			this.world.chat.talkModal = {
				talkName: 'You',
				currentDialogue: {
					dialogueText: text,
					clickToContinueFunction: (): void => {
						if (!this.world.chat.talkModal) {
							throw new Error('Hud not found in world.client');
						}
						resolve();
					},
				},
				dialogueOptions: [],
			};
		});
	}

	protected async playerChoice(options: { optionText: string }[]): Promise<number> {
		return new Promise(resolve => {
			this.world.chat.talkModal = {
				talkName: 'Choose an option',
				currentDialogue: null,
				dialogueOptions: options.map(option => ({
					optionText: option.optionText,
					optionFunction: (): void => {
						resolve(options.indexOf(option));
					},
				})),
			};
		});
	}
}
