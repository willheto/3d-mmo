export class ExperienceUtils {
	private static readonly levelExperience: number[] = (() => {
		const arr: number[] = new Array(99);
		let acc = 0;

		for (let i = 0; i < 99; i++) {
			const level = i + 1;
			const delta = Math.floor(level + Math.pow(2.0, level / 7.0) * 300.0);
			acc += delta;
			arr[i] = Math.floor(acc / 4.0);
		}

		return arr;
	})();

	static getLevelByExp(exp: number): number {
		for (let i = 98; i >= 0; i--) {
			if (exp >= this.levelExperience[i]) {
				return Math.min(i + 2, 99);
			}
		}
		return 1;
	}

	static getExpForLevel(level: number): number {
		if (level < 1 || level > 99) {
			throw new Error('Level must be between 1 and 99.');
		}
		if (level === 1) return 0;
		return this.levelExperience[level - 2];
	}

	static calculateExperienceDifference(level: number): number {
		if (level < 2) {
			throw new Error('Level must be 2 or higher.');
		}
		const exponentPart = Math.pow(2.0, (level - 1) / 7.0);
		const expressionValue = level - 1 + 300 * exponentPart;
		return Math.floor(expressionValue / 4);
	}

	static getExperienceUntilNextLevel(experience: number): number {
		const currentLevel = this.getLevelByExp(experience);
		const nextLevel = this.getExpForLevel(currentLevel + 1);

		return nextLevel - experience;
	}
}
