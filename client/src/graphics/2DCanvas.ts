export const canvas = document.getElementById('hud') as HTMLCanvasElement;
export const canvas2d = canvas.getContext('2d', {
	alpha: true,
	desynchronized: true,
})!;