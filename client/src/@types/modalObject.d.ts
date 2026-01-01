type ModalObject = {
	modalX: number;
	modalY: number;
	modalWidth?: number;
	modalHeight?: number;
	modalOptions: {
		optionText: string;
		optionSecondaryText: {
			text: string;
			color: string;
		};
		optionFunction: () => void;
	}[];
};
