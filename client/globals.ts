const loginServerAddress = () => {
	switch (process.env.NODE_ENV) {
		case 'local':
			return 'ws://localhost:8886';
		case 'production':
			return 'wss://game-login-socket.henriwillman.fi';
	}
};

const updateServerAddress = () => {
	switch (process.env.NODE_ENV) {
		case 'local':
			return 'ws://localhost:8887';
		case 'production':
			return 'wss://game-update-socket.henriwillman.fi';
	}
};

const gameServerAddress = () => {
	switch (process.env.NODE_ENV) {
		case 'local':
			return 'ws://localhost:8085';
		case 'production':
			return 'wss://game-game-socket.henriwillman.fi';
	}
};

const LOGIN_SERVER_ADDRESS = JSON.stringify(loginServerAddress());
const UPDATE_SERVER_ADDRESS = JSON.stringify(updateServerAddress());
const GAME_SERVER_ADDRESS = JSON.stringify(gameServerAddress());
export default {
	LOGIN_SERVER_ADDRESS,
	UPDATE_SERVER_ADDRESS,
	GAME_SERVER_ADDRESS,
};
