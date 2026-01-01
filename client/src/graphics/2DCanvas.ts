export const canvas: HTMLCanvasElement = document.getElementById(
  "hud"
) as HTMLCanvasElement;

export const canvas2d: CanvasRenderingContext2D = canvas.getContext("2d", {
  willReadFrequently: true,
})!;
