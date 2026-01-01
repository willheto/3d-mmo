import path from 'path';
import { v7 as uuidv7 } from 'uuid';

export const PORT = 8105;
export const HASH = uuidv7().slice(0, 8);
export const LOCAL_DEFAULT_OPEN_URL = 'http://localhost:' + PORT;

export const IMAGE_EXTENSIONS = [
	'.png',
	'.jpg',
	'.jpeg',
	'.gif',
	'.svg',
	'.webp',
];

export const ALIASES = {
	'@src': path.resolve(__dirname, 'src'),
	'@components': path.resolve(__dirname, 'src/components'),
	'@utils': path.resolve(__dirname, 'src/utils'),
	'@views': path.resolve(__dirname, 'src/views'),
};
