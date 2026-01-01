const registerServerAddress = () => {
	switch (process.env.NODE_ENV) {
		case 'local':
			return 'http://192.168.33.10:8601';
		case 'production':
			return 'https://game-register.henriwillman.fi';
		default:
			return 'http://localhost:8000';
	}
};

const clientAddress = () => {
	switch (process.env.NODE_ENV) {
		case 'local':
			return 'http://localhost:5173/';
		case 'production':
			return 'https://client-ts.henriwillman.fi';
		default:
			return 'http://localhost:5173/';
	}
};

const REGISTER_SERVER_ADDRESS = JSON.stringify(registerServerAddress());
const CLIENT_ADDRESS = JSON.stringify(clientAddress());
export default {
	REGISTER_SERVER_ADDRESS,
	CLIENT_ADDRESS,
};
