import { canvas, canvas2d } from '../graphics/2DCanvas';
import { ExperienceUtils } from '../util/ExperienceCurve';
import { World } from './World';
import Cache from '../cache/index';

const SKILL_NAMES = ['Attack', 'Strength', 'Defence', 'Hitpoints'];
const SKILL_ATTACK_STYLE: Record<string, string> = {
	Attack: 'attack',
	Strength: 'strength',
	Defence: 'defence',
};

export class Hud {
	private world: World;
	private readonly hudWidth: number = 380;
	private readonly hudHeight: number = 140;
	private openTab: 'items' | 'skills' = 'items';
	private itemImages = new Map<string, HTMLImageElement>();

	constructor(world: World) {
		this.world = world;
	}

	private readonly iconSize = 25;

	private get itemsIconRect() {
		return {
			x: 610,
			y: canvas.height - 175,
			w: this.iconSize * 2,
			h: this.iconSize,
		};
	}

	private get skillsIconRect() {
		return {
			x: 665,
			y: canvas.height - 175,
			w: this.iconSize * 2,
			h: this.iconSize,
		};
	}

	loadItemImage(iconName: string) {
		if (this.itemImages.has(iconName)) return;

		const url = Cache.getCachedObjectURL(iconName);
		if (!url) return;

		const img = new Image();
		img.src = url;

		this.itemImages.set(iconName, img);
	}

	private handleSkillClick(): void {
		if (this.openTab !== 'skills') return;

		const startX = 615;
		const startY = canvas.height - this.hudHeight - 5 + 15;
		const lineHeight = 18;
		const clickWidth = 140; // width of skill name text

		for (let i = 0; i < SKILL_NAMES.length; i++) {
			const skillName = SKILL_NAMES[i];
			const attackStyle = SKILL_ATTACK_STYLE[skillName];
			if (!attackStyle) continue;

			const y = startY + i * lineHeight - lineHeight + 4;

			if (this.isMouseInRect(startX, y, clickWidth, lineHeight)) {
				const playerID = this.world.currentPlayerID;
				if (playerID) {
					this.world.actions?.changeAttackStyle(playerID, attackStyle);
				}
				return;
			}
		}
	}

	private isMouseInRect(x: number, y: number, w: number, h: number): boolean {
		canvas.style.userSelect = 'auto';
		return (
			this.world.mouseScreenX >= x &&
			this.world.mouseScreenX <= x + w &&
			this.world.mouseScreenY >= y &&
			this.world.mouseScreenY <= y + h
		);
	}

	private handleRightClick(): void {
		if (this.openTab !== 'items') return;

		const currentPlayer = this.world.players.find(p => p.entityID === this.world.currentPlayerID);
		if (!currentPlayer) return;

		const startX = 625;
		const startY = canvas.height - this.hudHeight - 5 + 15;
		const iconSize = 50;
		const paddingX = 10;
		const paddingY = 10;

		const cols = 6;

		for (let slot = 0; slot < currentPlayer.inventory.length; slot++) {
			const row = Math.floor(slot / cols);
			const col = slot % cols;

			const x = startX + col * (iconSize + paddingX);
			const y = startY + row * (iconSize + paddingY);

			if (this.isMouseInRect(x, y, iconSize, iconSize)) {
				const itemId = currentPlayer.inventory[slot];
				if (itemId == null || itemId < 0) return;

				this.onItemRightClick(itemId, slot);
				return;
			}
		}
	}

	public handleMouseDown(): void {
		if (this.world.mouseButton === 2) {
			this.handleRightClick();
			return;
		}

		// LEFT CLICK ONLY BELOW
		const items = this.itemsIconRect;
		const skills = this.skillsIconRect;

		if (this.isMouseInRect(items.x, items.y, items.w, items.h)) {
			this.openTab = 'items';
			return;
		}

		if (this.isMouseInRect(skills.x, skills.y, skills.w, skills.h)) {
			this.openTab = 'skills';
			return;
		}

		this.handleSkillClick();
	}

	public drawHud() {
		if (this.world.mouseDown) {
			this.handleMouseDown();
		}

		canvas2d.fillStyle = '#6E5235';
		canvas2d.fillRect(610, canvas.height - 140 - 5, this.hudWidth, this.hudHeight);

		// Chat background
		canvas2d.fillStyle = '#6E5235';
		canvas2d.fillRect(610, canvas.height - 140 - 5, this.hudWidth, this.hudHeight);

		// Dark outer border
		canvas2d.strokeStyle = '#000000';
		canvas2d.lineWidth = 2;
		canvas2d.strokeRect(610, canvas.height - 140 - 5, this.hudWidth, this.hudHeight);

		// Optional inner highlight border (subtle depth)
		canvas2d.strokeStyle = 'rgba(255,255,255,0.2)';
		canvas2d.lineWidth = 1;
		canvas2d.strokeRect(610, canvas.height - 140 - 4, this.hudWidth - 2, this.hudHeight - 2);

		this.drawTabIcons();

		canvas2d.textAlign = 'left';

		if (this.openTab === 'skills') {
			this.drawSkills();
		}

		if (this.openTab === 'items') {
			this.drawItems();
		}
	}

	private onItemRightClick(itemIndex: number, invSlot: number) {
		// prevent immediate close
		canvas.style.pointerEvents = 'auto';

		const itemData = this.world.itemsManager.getItemInfoById(itemIndex);
		if (!itemData) {
			//this.world.actions?.sendChatMessage(this.world.currentPlayerID, 'No data', false);
			return;
		}

		this.world.modalObject = {
			modalX: this.world.mouseScreenX + 8,
			modalY: this.world.mouseScreenY + 8,
			modalOptions: [],
		};

		const currentPlayer = this.world.players.find(player => player.entityID === this.world.currentPlayerID);
		if (!currentPlayer) return;

		const isWielded = currentPlayer.weapon === invSlot || currentPlayer.shield === invSlot;

		if (itemData.isWieldable) {
			this.world.modalObject.modalOptions.push({
				optionText: isWielded ? 'Unwield ' : 'Wield ',
				optionSecondaryText: {
					text: itemData.name,
					color: '#ffff66',
				},
				optionFunction: () => {
					if (isWielded) {
						this.world.actions?.unWield(this.world.currentPlayerID, invSlot);
					} else {
						this.world.actions?.wield(this.world.currentPlayerID, invSlot);
					}
				},
			});
		}

		if (itemData.isEdible) {
			this.world.modalObject.modalOptions.push({
				optionText: 'Eat ',
				optionSecondaryText: {
					text: itemData.name,
					color: '#ffff66',
				},
				optionFunction: () => {},
			});
		}

		this.world.modalObject.modalOptions.push({
			optionText: 'Examine ',
			optionSecondaryText: {
				text: itemData.name,
				color: '#ffff66',
			},
			optionFunction: () => {
				this.world.actions?.sendChatMessage(this.world.currentPlayerID, itemData?.examine || 'No data', false);
			},
		});
		this.world.modalObject.modalOptions.push({
			optionText: 'Drop ',
			optionSecondaryText: {
				text: itemData.name,
				color: '#ffff66',
			},
			optionFunction: () => {
				this.world.actions?.dropItem(this.world.currentPlayerID, invSlot);
			},
		});
	}

	private async drawItems() {
		const currentPlayer = this.world.players.find(player => player.entityID === this.world.currentPlayerID);
		if (!currentPlayer) return;

		const items = currentPlayer.inventory;
		const amounts = currentPlayer.inventoryAmounts;

		const startX = 625;
		const startY = canvas.height - this.hudHeight - 5 + 15;

		const iconSize = 50;
		const paddingX = 10;
		const paddingY = 10;

		const cols = 6;
		const rows = 2;
		const totalSlots = cols * rows;

		canvas2d.font = '14px Pkmn';
		canvas2d.fillStyle = 'white';
		canvas2d.textAlign = 'left';
		canvas2d.textBaseline = 'top';

		for (let slot = 0; slot < totalSlots; slot++) {
			const row = Math.floor(slot / cols);
			const col = slot % cols;

			const x = startX + col * (iconSize + paddingX);
			const y = startY + row * (iconSize + paddingY);

			// slot background
			canvas2d.save();
			canvas2d.fillStyle = '#C4A484';
			canvas2d.fillRect(x, y, iconSize, iconSize);
			canvas2d.strokeStyle = 'rgba(255,255,255,0.25)';
			canvas2d.strokeRect(x, y, iconSize, iconSize);
			canvas2d.restore();

			if (currentPlayer.weapon === slot || currentPlayer.shield === slot) {
				canvas2d.save();
				canvas2d.fillStyle = 'rgba(0, 255, 0, 0.25)';
				canvas2d.fillRect(x, y, iconSize, iconSize);
				canvas2d.restore();
			}

			const itemId = items[slot];
			if (itemId == null || itemId < 0) continue;

			const itemData = this.world.itemsManager.getItemInfoById(itemId);
			if (!itemData) continue;

			// Pick ONE stable key. iconName is safest for images.
			const key = itemData.iconName.toLowerCase();

			// create image once
			if (!this.itemImages.has(key)) {
				const iconUrl = await Cache.getObjectURLByAssetName(key);
				if (!iconUrl) continue;

				const img = new Image();
				img.src = iconUrl;
				this.itemImages.set(key, img);
			}

			// draw sync
			const img = this.itemImages.get(key);
			if (img && img.complete) {
				canvas2d.drawImage(img, x, y, 50, 50);
			}

			// stack count
			if (itemData.isStackable) {
				const amt = amounts?.[slot] ?? 1;
				if (amt > 1) canvas2d.fillText(`x${amt}`, x + iconSize - 14, y + iconSize - 14);
			}
		}

		canvas2d.textBaseline = 'middle';
	}

	private drawTabIcons(): void {
		const items = this.itemsIconRect;
		const skills = this.skillsIconRect;

		// Items icon background
		canvas2d.fillStyle = this.openTab === 'items' ? 'rgba(255,255,255,0.2)' : 'rgba(22,22,22,0.5)';
		canvas2d.fillRect(items.x, items.y, items.w, items.h);

		// Skills icon background
		canvas2d.fillStyle = this.openTab === 'skills' ? 'rgba(255,255,255,0.2)' : 'rgba(22,22,22,0.5)';
		canvas2d.fillRect(skills.x, skills.y, skills.w, skills.h);

		// Icon glyphs (placeholder — swap for sprites later)
		canvas2d.fillStyle = 'white';
		canvas2d.font = '18px Pkmn';
		canvas2d.textAlign = 'center';
		canvas2d.textBaseline = 'middle';

		canvas2d.fillText('Inv', items.x + items.w / 2, items.y + items.h / 2);
		canvas2d.fillText('Skill', skills.x + skills.w / 2, skills.y + skills.h / 2);
	}

	private drawSkills(): void {
		const currentPlayer = this.world.players.find(player => {
			return player.entityID === this.world.currentPlayerID;
		});

		if (!currentPlayer) {
			return;
		}

		const skills = currentPlayer.skills;

		const startX = 615;
		const levelX = startX + 155; // fixed column for levels
		const xpX = levelX + 45; // XP-to-next column
		const startY = canvas.height - this.hudHeight - 5 + 15;
		const lineHeight = 18;

		canvas2d.font = '18px Pkmn';
		canvas2d.fillStyle = 'white';
		canvas2d.textAlign = 'left';

		for (let i = 0; i < skills.length && i < SKILL_NAMES.length; i++) {
			const exp = skills[i];
			let level = ExperienceUtils.getLevelByExp(exp);
			if (level === 0) {
				level = 1;
			}

			const xpToNext = ExperienceUtils.getExperienceUntilNextLevel(exp);

			const y = startY + i * lineHeight;

			const skillName = SKILL_NAMES[i];
			const attackStyle = SKILL_ATTACK_STYLE[skillName];
			const isClickable = attackStyle !== undefined;
			const isActive = isClickable && currentPlayer.attackStyle === attackStyle;

			// ACTIVE highlight (persistent)
			if (isActive) {
				canvas2d.fillStyle = 'rgba(255, 215, 0, 0.35)'; // gold-ish = selected
				canvas2d.fillRect(startX - 4, y - 10, 140, lineHeight);
			}

			// HOVER highlight (only if not active)
			else if (isClickable && this.isMouseInRect(startX, y - 10, 140, lineHeight)) {
				canvas2d.fillStyle = 'rgba(255,255,255,0.2)';
				canvas2d.fillRect(startX - 4, y - 10, 140, lineHeight);
			}

			canvas2d.fillStyle = 'white';

			// Skill name
			canvas2d.fillText(skillName, startX, y);

			// Level (aligned)
			if (i === 3) {
				canvas2d.fillText(currentPlayer.currentHitpoints.toString() + '/', levelX - 25, y);
			}
			canvas2d.fillText(level.toString(), levelX, y);

			// XP to next level
			canvas2d.fillText(`${xpToNext} xp to next level`, xpX, y);
		}
	}
}
