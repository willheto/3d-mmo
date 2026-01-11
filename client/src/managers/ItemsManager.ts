import Cache from '../cache/index';

interface StaticItemData {
	itemID: number;
	name: string;
	examine: string;
	isStackable: boolean;
	value: number;
	modelName: string;
	isWieldable: boolean;
	isEdible: boolean;
	type: string;
	iconName: string;
}

export default class ItemsManager {
	private items: StaticItemData[] = [];

	constructor() {
		this.loadItems();
	}

	private async loadItems(): Promise<void> {
		const blob = await Cache.getObjectURLByAssetName('items.json');

		if (!blob) {
			throw new Error('Items blob not found');
		}

		const items = await fetch(blob).then(response => response.json());
		this.items = items;
	}

	public getItemInfoById(id: number): StaticItemData | null {
		return this.items.find(item => item.itemID === id) || null;
	}
}
