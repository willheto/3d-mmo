interface DialogueStep {
	speaker: string;
	text: string;
	options?: DialogueOption[];
	onStepComplete?: () => void;
}

interface DialogueOption {
	optionText: string;
	optionAction: () => void;
}
