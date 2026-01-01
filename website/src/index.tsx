import { createRoot } from 'react-dom/client';
import './index.scss';
import App from './App';

const websiteContainer = document.getElementById('game');

if (websiteContainer) {
	const root = createRoot(websiteContainer);
	root.render(<App />);
}
