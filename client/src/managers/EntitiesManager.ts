import Cache from '../cache/index';

interface StaticEntityData {
	entityIndex: number;
	name: string;
	examine: string;
	respawnTime: number;
	skills: number[];
	isTalkable: boolean;
	type: 1 | 2;
	dropTable: {
		itemID: number;
		dropChance: number;
		amount: number;
	};
}

export default class EntitiesManager {
	private entities: StaticEntityData[] = [];

	constructor() {
		this.loadEntities();
	}

	private async loadEntities(): Promise<void> {
		const blob = await Cache.getObjectURLByAssetName('entities.json');

		if (!blob) {
			throw new Error('Entities blob not found');
		}

		const entities = await fetch(blob).then(response => response.json());
		this.entities = entities;
	}

	public getEntityInfoByIndex(index: number): StaticEntityData | null {
		return this.entities.find(entity => entity.entityIndex === index) || null;
	}

	public getAllEntities(): StaticEntityData[] {
		return this.entities;
	}
}
