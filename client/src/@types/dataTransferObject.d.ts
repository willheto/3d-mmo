interface SocketGameState {
	tickTalkEvents?: SocketTalkEvent[];
	tickSoundEvents?: SocketSoundEvent[];
	tickAttackEvents?: SocketAttackEvent[];

	players?: SocketPlayer[];
	npcs?: SocketNpc[];

	chatMessages?: SocketChatMessage[];
	items?: SocketItem[];
	shops?: SocketShop[];

	playerID?: string;
	onlinePlayers?: string[];
}

interface SocketTalkEvent {
	talkerID: string;
	targetID: string;
	targetIndex: number;
	dialogueNumber: number;
}

interface SocketAttackEvent {
	attackerID: string;
	targetID: string;
}

interface SocketSoundEvent {
	isGlobal: boolean;
	soundName: string;
	isSfx: boolean;
	shouldInterrupt: boolean;
	entityID: string; // Played to whom
}

interface SocketNpc extends SocketEntity {
	npcIndex: number;
}

interface SocketPlayer extends SocketEntity {
	storyProgress: number;
	inventory: number[];
	inventoryAmounts: number[];
	skills: number[];
	attackStyle: string;
	username: string;
}

type Direction = 'UP' | 'DOWN' | 'LEFT' | 'RIGHT' | 'UP_LEFT' | 'UP_RIGHT' | 'DOWN_LEFT' | 'DOWN_RIGHT' | 'NONE';

interface SocketEntity {
	entityID: string;
	worldX: number;
	worldY: number;
	lastTickX: number;
	lastTickY: number;
	nextTileDirection: Direction;
	facingDirection: Direction;
	currentChunk: number;
	currentHitpoints: number;
	isInCombat: boolean;
	isDying: boolean;

	weapon: undefined | number;
	shield: undefined | number;
}

interface SocketChatMessage {
	senderName: string;
	message: string;
	timeSent: string;
	isGlobal: boolean;
	isChallenge: boolean;
	challengerID: string;
}

interface SocketItem {
	itemID: number;
	name: string;
	spriteName: string;
	value: number;
	isStackable: boolean;
	amount: number;
	uniqueID: string;
	isDeleted: boolean;
	worldX: number | null;
	worldY: number | null;
}

interface SocketShop {
	shopID: string;
	shopName: string;
	sellsAtPercentage: number;
	buysAtPercentage: number;
	buysAnything: boolean;
}
