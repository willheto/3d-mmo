import Cache from '../cache/index';

interface StaticItemData {
	itemID: number;
	name: string;
	examine: string;
	isStackable: boolean;
	spriteName: string;
	value: number;
}

export default class ItemsManager {
	private items: StaticItemData[] = [];
	public itemSprites: {
		sprite: HTMLImageElement;
		name: string;
	}[] = [];

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

		items.forEach(async (item: StaticItemData) => {
			const sprite = await Cache.getObjectURLByAssetName(item.spriteName);
			if (!sprite) return;

			const image = new Image();
			image.src = sprite;
			this.itemSprites.push({ sprite: image, name: item.spriteName });
		});
	}

	public getItemInfoById(id: number): StaticItemData | null {
		return this.items.find(item => item.itemID === id) || null;
	}
}
