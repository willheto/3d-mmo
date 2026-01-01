import axios from 'axios';
import { useState } from 'react';
import styled from 'styled-components';
import { BrowserRouter as Router, Route, Routes, Link } from 'react-router-dom';

declare const REGISTER_SERVER_ADDRESS: string;
declare const CLIENT_ADDRESS: string;

const ERROR_MESSAGES = {
	2: 'Username is already taken',
};

const Home = () => {
	const [isCreatingAccount, setIsCreatingAccount] = useState<boolean>(false);
	const [accountCreated, setAccountCreated] = useState<boolean>(false);
	const [username, setUsername] = useState<string>('');
	const [password, setPassword] = useState<string>('');
	const [confirmPassword, setConfirmPassword] = useState<string>('');
	const [error, setError] = useState<string>('');

	const createAccount = async () => {
		try {
			if (username.length < 5 || username.length > 12) {
				setError('Username must be between 5 and 12 characters long');
				return;
			}

			if (!/^[a-zA-Z0-9_]*$/.test(username)) {
				setError('Username may only contain letters, numbers, and underscores');
				return;
			}

			if (password.length < 5 || password.length > 20) {
				setError('Password must be between 5 and 20 characters long');
				return;
			}

			if (password !== confirmPassword) {
				setError('Passwords do not match');
				return;
			}

			const response = await axios.post(REGISTER_SERVER_ADDRESS + '/create-account', {
				username,
				password,
			});

			if (!response.data.success) {
				// @ts-ignore
				setError(ERROR_MESSAGES[response.data.error]);
				return;
			}

			setAccountCreated(true);
			setIsCreatingAccount(false);
		} catch (error) {
			setError('Something went wrong. Please try again later.');
		}
	};

	return (
		<WebsiteContainer>
			<Disclaimer>
				{!isCreatingAccount && (
					<TextContainer>
						<span>
							<b>Disclaimer</b>
						</span>
			
						<span>The game contains bugs, incomplete features, and other issues.</span>
						{accountCreated && (
							<span style={{ color: 'green' }}>
								Account created successfully! You can now play the game.
							</span>
						)}
					</TextContainer>
				)}
				{isCreatingAccount ? (
					<>
						<TextContainer>
							<span>Usernames can be a maximum of 12 characters long and may contain letters, numbers, and underscores.</span>
							<span>Passwords may be between 5 and 20 characters long.</span>
						</TextContainer>
						<div style={{ display: 'flex', flexDirection: 'column', gap: '5px', alignItems: 'center' }}>
							<div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
								<span>Desired Username:</span>
								<input maxLength={12} type="text" onChange={(e) => setUsername(e.target.value)} />
							</div>
							<div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
								<span>Desired Password:</span>
								<input maxLength={20} minLength={5} type="password" onChange={(e) => setPassword(e.target.value)} />
							</div>
							<div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
								<span>Confirm Password:</span>
								<input maxLength={20} minLength={5} type="password" onChange={(e) => setConfirmPassword(e.target.value)} />
							</div>
							{error && <span style={{ color: 'red' }}>{error}</span>}
						</div>
						<ButtonContainer>
							<Button onClick={() => createAccount()}>Create Account</Button>
							<Button onClick={() => setIsCreatingAccount(false)}>Cancel</Button>
						</ButtonContainer>
					</>
				) : (
					<ButtonContainer>
						<Link to="/client">
							<Button>Play Game</Button>
						</Link>
						<Button onClick={() => setIsCreatingAccount(true)}>Create Account</Button>
					</ButtonContainer>
				)}
			</Disclaimer>
		</WebsiteContainer>
	);
};

const Client = () => {
	return (
		<div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
			<div style={{ width: '1000px', height: '700px', border: 'none', padding: '5px' }}>
				<div style={{ width: '100%', display: 'flex', paddingBottom: '5px' }}>
					<Link to="/">
						<Button>Back to Website</Button>
					</Link>
				</div>
				<iframe style={{ width: '100%', height: '100%', border: 'none' }} src={CLIENT_ADDRESS} title="game" />
			</div>
		</div>
	);
};

const App = () => {
	return (
		<Router>
			<Routes>
				<Route path="/" element={<Home />} />
				<Route path="/client" element={<Client />} />
			</Routes>
		</Router>
	);
};

const TextContainer = styled.div`
    display: flex;
    flex-direction: column;
    gap: 20px;
`;

const ButtonContainer = styled.div`
    display: flex;
    gap: 20px;
    justify-content: center;
`;

const Button = styled.button`
    background-color: #8b0000;
    color: white;
    border: none;
    padding: 10px 20px;
    font-size: 16px;
    cursor: pointer;
`;

const WebsiteContainer = styled.div`
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100vh;
`;

const Disclaimer = styled.div`
    justify-content: space-between;
    border: 2px solid #382418;
    width: 500px;
    color: white;
    padding: 20px;
    text-align: center;
    font-family: Arial, sans-serif;
    overflow: auto;
    display: flex;
    flex-direction: column;
    gap: 20px;
`;

export default App;
