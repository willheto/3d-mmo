import { World } from '../world/World';

export class Item {
	public uniqueID: string = '';
	public itemID: number = -1;
	public amount: number = -1;
	public worldX: number | null = -1;
	public worldY: number | null = -1;

	constructor() {}

	public update(item: SocketItem): void {
		if (item.uniqueID !== undefined) {
			this.uniqueID = item.uniqueID;
		}

		if (item.itemID !== undefined) {
			this.itemID = item.itemID;
		}

		if (item.amount !== undefined) {
			this.amount = item.amount;
		}

		if (item.worldX !== undefined) {
			this.worldX = item.worldX;
		}

		if (item.worldY !== undefined) {
			this.worldY = item.worldY;
		}
	}
}
