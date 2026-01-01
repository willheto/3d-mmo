import { defineConfig } from 'vite';
import { nodePolyfills } from 'vite-plugin-node-polyfills';
import dns from 'dns';
import react from '@vitejs/plugin-react';
import {
	HASH,
	LOCAL_DEFAULT_OPEN_URL,
	PORT,
	IMAGE_EXTENSIONS,
	ALIASES,
} from './vite.constants';
import 'dotenv/config';
import globals from './globals';

dns.setDefaultResultOrder('verbatim');

const compileAsset = (assetInfo: { name?: string }): string => {
	if (assetInfo.name?.endsWith('.js')) {
		return '[name].js';
	} else if (assetInfo.name?.endsWith('.css')) {
		return `[name].${HASH}.css`;
	} else if (IMAGE_EXTENSIONS.some(ext => assetInfo.name?.endsWith(ext))) {
		return `images/[name].[ext]`;
	} else {
		return `[name].[ext]`;
	}
};

export default defineConfig({
	build: {
		rollupOptions: {
			output: {
				entryFileNames: `[name].bundle.${HASH}.js`,
				chunkFileNames: `[name].chunk.bundle.${HASH}.js`,
				assetFileNames: assetInfo => compileAsset(assetInfo),
				dir: 'dist',
			},
		},

		sourcemap: process.env.NODE_ENV !== 'production',
		minify: process.env.NODE_ENV === 'production',
	},
	server: {
		port: PORT,
		open: LOCAL_DEFAULT_OPEN_URL,
	},
	resolve: {
		alias: ALIASES,
	},
	define: {
		...globals,
	},
	plugins: [
		react(),
		nodePolyfills({
			protocolImports: true,
		}),
	],
});
