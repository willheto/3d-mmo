export const canvas = document.getElementById('hud') as HTMLCanvasElement;
export const canvas2d = canvas.getContext('2d', {
	alpha: true,
	desynchronized: true,
})!;

export function resizeHudCanvas() {
	const dpr = Math.min(window.devicePixelRatio, 2);
	const width = window.innerWidth;
	const height = window.innerHeight;

	canvas.width = Math.floor(width * dpr);
	canvas.height = Math.floor(height * dpr);

	canvas.style.width = width + 'px';
	canvas.style.height = height + 'px';

	canvas2d.setTransform(dpr, 0, 0, dpr, 0, 0);
	canvas2d.imageSmoothingEnabled = false;
}

// Call once at startup
resizeHudCanvas();

// Call on resize
window.addEventListener('resize', resizeHudCanvas);
