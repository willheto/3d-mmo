import { World } from '../world/World';
import { Interaction } from './Interaction';

export default class Man extends Interaction {
	public dialogueNumber: number = 0;

	constructor(world: World, targetID: string, dialogueNumber: number = 0) {
		super(world, targetID, 'Man');
		this.dialogueNumber = dialogueNumber;
		this.startDialogue();
	}

	private getRandomDialogue(): number {
		return Math.floor(Math.random() * 23) + 1;
	}

	public async startDialogue(): Promise<void> {
		const dialogue = this.dialogueNumber === 0 ? this.getRandomDialogue() : this.dialogueNumber;

		switch (dialogue) {
			case 1:
				await this.npcSays("I'm a little worried about the increase of goblins these days.");
				await this.playerSays("Don't worry, I'll kill them.");
				break;

			case 2:
				await this.npcSays('How can I help you?');
				const choice = await this.playerChoice([
					{ optionText: 'Do you want to trade?' },
					{ optionText: "I'm in search of a quest." },
					{ optionText: "I'm in search of enemies to kill." },
				]);

				if (choice === 0) {
					await this.npcSays(
						'No, I have nothing I wish to get rid of. If you want to do some trading, there are plenty of shops and market stalls around though.',
					);
				} else if (choice === 1) {
					await this.npcSays("I'm sorry I can't help you there.");
				} else {
					await this.npcSays("I've heard there are many fearsome creatures that dwell under the ground...");
				}
				break;

			case 3:
				await this.npcSays("Get out of my way, I'm in a hurry!");
				break;

			case 4:
				await this.npcSays("I'm fine, how are you?");
				await this.playerSays('Very well thank you.');
				break;

			case 5:
				await this.npcSays("Hello there! Nice weather we've been having.");
				break;

			case 6:
				await this.npcSays("I'm very well thank you.");
				break;

			case 7:
				await this.npcSays('Who are you?');
				await this.playerSays("I'm a bold adventurer.");
				await this.npcSays('Ah, a very noble profession.');
				break;

			case 8:
				await this.npcSays("Do I know you? I'm in a hurry!");
				break;

			case 9:
				await this.npcSays("I think we need a new king. The one we've got isn't very good.");
				break;

			case 10:
				await this.npcSays('Not too bad thanks.');
				break;

			case 11:
				await this.npcSays('Are you asking for a fight?');
				// trigger combat here
				break;

			case 12:
				await this.npcSays("I'm busy right now.");
				break;

			case 13:
				await this.npcSays('Hello.');
				break;

			case 14:
				await this.npcSays('None of your business.');
				break;

			case 15:
				await this.npcSays(
					'No, I have nothing I wish to get rid of. If you want to do some trading, there are plenty of shops and market stalls around though.',
				);
				break;

			case 16:
				await this.npcSays("I'm sorry I can't help you there.");
				break;

			case 17:
				await this.npcSays("I've heard there are many fearsome creatures that dwell under the ground...");
				break;

			case 18:
				await this.npcSays("No I don't have any spare change.");
				break;

			case 19:
				await this.npcSays(
					"I'm a little worried - I've heard there's lots of people going about, killing citizens at random.",
				);
				break;

			case 20:
				await this.npcSays("No, I don't want to buy anything!");
				break;

			case 21:
				await this.npcSays('That is classified information.');
				break;

			case 22:
				await this.npcSays('Have this flyer...');
				// give flyer item here
				break;

			case 23:
				await this.npcSays('Yo, wassup!');
				break;
		}

		this.endDialogue();
	}
}
