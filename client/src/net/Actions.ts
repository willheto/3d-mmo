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

	public logOut(playerID: string): void {
		//
	}
}
