export class ExperienceCurve {
	/**
	 * Calculates the total experience required to reach a given level.
	 *
	 * @param level The target level (must be >= 1)
	 * @return The total experience required
	 */
	public static getExperienceForLevel(level: number): number {
		if (level < 1) {
			throw new Error('Level must be at least 1');
		}
		return level ** 3; // EXP = n^3
	}

	/**
	 * Calculates the level corresponding to a given experience amount.
	 *
	 * @param experience The total experience
	 * @return The corresponding level
	 */
	public static getLevelByExperience(experience: number): number {
		if (experience < 0) {
			throw new Error('Experience cannot be negative');
		}
		return Math.cbrt(experience) | 0; // Level = cubic root of EXP, floored
	}

	/**
	 * Calculates the experience required to reach the next level from the current level.
	 *
	 * @param level The current level (must be >= 1)
	 * @return The experience required to reach the next level
	 */
	public static getExperienceToNextLevel(level: number): number {
		if (level < 1) {
			throw new Error('Level must be at least 1');
		}
		// Experience required for the next level
		return this.getExperienceForLevel(level + 1) - this.getExperienceForLevel(level);
	}
}
