import { World } from '../world/World';

export class Actions {
	private gameSocket: WebSocket;
	private world: World;

	constructor(gameSocket: WebSocket, world: World) {
		this.gameSocket = gameSocket;
		this.world = world;
	}

	public movePlayer(playerID: string, x: number, y: number): void {
		this.gameSocket.send(
			JSON.stringify({
				action: 'playerMove',
				playerID: playerID,
				data: {
					x: x,
					y: y,
				},
			}),
		);
	}

	public moveAndTalk(playerID: string, entityID: string): void {
		this.gameSocket?.send(
			JSON.stringify({
				action: 'playerTalkMove',
				playerID: playerID,
				data: {
					entityID: entityID,
				},
			}),
		);
	}

	public moveAndAttack(playerID: string, entityID: string): void {
		this.gameSocket?.send(
			JSON.stringify({
				action: 'playerAttackMove',
				playerID: playerID,
				data: {
					entityID: entityID,
				},
			}),
		);
	}

	public sendChatMessage(playerID: string, message: string, isGlobal: boolean): void {
		this.gameSocket?.send(
			JSON.stringify({
				action: 'chatMessage',
				playerID: playerID,
				data: {
					message: message,
					timeSent: Date.now(),
					isGlobal: isGlobal,
				},
			}),
		);
	}

	public changeAttackStyle(playerID: string, attackStyle: string): void {
		this.gameSocket?.send(
			JSON.stringify({
				action: 'changeAttackStyle',
				playerID: playerID,
				data: {
					attackStyle: attackStyle,
				},
			}),
		);
	}

	public wield(playerID: string, inventoryIndex: number): void {
		this.gameSocket?.send(
			JSON.stringify({
				action: 'wieldItem',
				playerID: playerID,
				data: {
					inventoryIndex: inventoryIndex,
				},
			}),
		);
	}

	public unWield(playerID: string, inventoryIndex: number): void {
		this.gameSocket?.send(
			JSON.stringify({
				action: 'unwieldItem',
				playerID: playerID,
				data: {
					inventoryIndex: inventoryIndex,
				},
			}),
		);
	}

	public dropItem(playerID: string, inventoryIndex: number): void {
		this.gameSocket?.send(
			JSON.stringify({
				action: 'dropItem',
				playerID: playerID,
				data: {
					inventoryIndex: inventoryIndex,
				},
			}),
		);
	}

	public takeGroundItem(playerID: string, uniqueItemID: string): void {
		this.gameSocket?.send(
			JSON.stringify({
				action: 'playerTakeMove',
				playerID: playerID,
				data: {
					uniqueItemID: uniqueItemID,
				},
			}),
		);
	}

	public swapInventorySlots(playerID: string, draggingSlot: number, targetSlot: number): void {
		console.log(draggingSlot, targetSlot);
		this.gameSocket?.send(
			JSON.stringify({
				action: 'swapInventorySlots',
				playerID: playerID,
				fromSlot: draggingSlot,
				toSlot: targetSlot,
			}),
		);
	}

	public logOut(playerID: string): void {
		//
	}
}
