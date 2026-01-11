import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import Cache from '../cache/index';

export class ItemPreviewRenderer {
	private renderer: THREE.WebGLRenderer;
	private gltfLoader: GLTFLoader;

	private size: number;

	// Offscreen rendering
	private scene: THREE.Scene;
	private camera: THREE.PerspectiveCamera;
	private renderTarget: THREE.WebGLRenderTarget;

	// Lighting
	private keyLight: THREE.DirectionalLight;
	private fillLight: THREE.DirectionalLight;
	private ambientLight: THREE.AmbientLight;

	// Pixel buffers
	private pixels: Uint8Array;
	private workCanvas: HTMLCanvasElement;
	private workCtx: CanvasRenderingContext2D;

	// Caches
	private modelCache = new Map<string, THREE.Object3D>();
	private iconCache = new Map<string, HTMLCanvasElement>();

	constructor(opts: { renderer: THREE.WebGLRenderer; gltfLoader: GLTFLoader; size?: number }) {
		this.renderer = opts.renderer;
		this.gltfLoader = opts.gltfLoader;
		this.size = opts.size ?? 128;

		// --- Scene ---
		this.scene = new THREE.Scene();

		// --- Camera ---
		this.camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100);
		this.camera.position.set(0, 0.9, 1.8);
		this.camera.lookAt(0, 0.5, 0);

		// --- Lights ---
		this.keyLight = new THREE.DirectionalLight(0xffffff, 1.2);
		this.keyLight.position.set(3, 4, 5);

		this.fillLight = new THREE.DirectionalLight(0xffffff, 0.6);
		this.fillLight.position.set(-3, 2, 2);

		this.ambientLight = new THREE.AmbientLight(0xffffff, 0.35);

		this.scene.add(this.keyLight, this.fillLight, this.ambientLight);

		// --- Render target ---
		this.renderTarget = new THREE.WebGLRenderTarget(this.size, this.size, {
			format: THREE.RGBAFormat,
			type: THREE.UnsignedByteType,
			depthBuffer: true,
			stencilBuffer: false,
		});

		this.pixels = new Uint8Array(this.size * this.size * 4);

		// --- 2D canvas ---
		this.workCanvas = document.createElement('canvas');
		this.workCanvas.width = this.size;
		this.workCanvas.height = this.size;
		this.workCtx = this.workCanvas.getContext('2d')!;
	}

	/* ======================= PUBLIC API ======================= */

	/**
	 * Returns a cached icon canvas if available.
	 * First call triggers async generation.
	 */
	async getIcon(modelName: string, basePath = '/models/'): Promise<HTMLCanvasElement> {
		if (this.iconCache.has(modelName)) {
			return this.iconCache.get(modelName)!;
		}

		const model = await this.loadModel(modelName);
		this.normalizeModel(model);



		const icon = this.renderModel(model);
		this.iconCache.set(modelName, icon);

		return icon;
	}

	/* ======================= INTERNAL ======================= */

	private async loadModel(modelName: string): Promise<THREE.Object3D> {
		if (this.modelCache.has(modelName)) {
			return this.modelCache.get(modelName)!.clone(true);
		}

		const model = await Cache.getObjectURLByAssetName(modelName);
		if (!model) {
			return;
		}
		const url = new URL(model, import.meta.url).href;
		const gltf = await this.gltfLoader.loadAsync(url);

		const scene = gltf.scene;

		scene.traverse(obj => {
			if ((obj as THREE.Mesh).isMesh) {
				const mesh = obj as THREE.Mesh;
				mesh.castShadow = false;
				mesh.receiveShadow = false;
			}
		});

		this.modelCache.set(modelName, scene);
		return scene.clone(true);
	}

	private normalizeModel(model: THREE.Object3D): void {
		const box = new THREE.Box3().setFromObject(model);
		const center = box.getCenter(new THREE.Vector3());
		const size = box.getSize(new THREE.Vector3());

		model.position.sub(center);

		const maxDim = Math.max(size.x, size.y, size.z, 0.0001);
		const scale = 1 / maxDim;
		model.scale.setScalar(scale);

		model.position.y += 0.15;
	}

	private renderModel(model: THREE.Object3D): HTMLCanvasElement {
		// Reset scene but keep lights
		this.scene.clear();
		this.scene.add(this.keyLight, this.fillLight, this.ambientLight);
		this.scene.add(model);

		this.renderer.setRenderTarget(this.renderTarget);
		this.renderer.clear();
		this.renderer.render(this.scene, this.camera);
		this.renderer.setRenderTarget(null);

		// Read pixels
		this.renderer.readRenderTargetPixels(this.renderTarget, 0, 0, this.size, this.size, this.pixels);

		// Flip Y for canvas
		const flipped = new Uint8ClampedArray(this.pixels.length);
		const rowSize = this.size * 4;

		for (let y = 0; y < this.size; y++) {
			const src = (this.size - 1 - y) * rowSize;
			const dst = y * rowSize;
			flipped.set(this.pixels.subarray(src, src + rowSize), dst);
		}

		const imgData = new ImageData(flipped, this.size, this.size);
		this.workCtx.putImageData(imgData, 0, 0);

		// Copy to persistent canvas (workCanvas is reused)
		const finalCanvas = document.createElement('canvas');
		finalCanvas.width = this.size;
		finalCanvas.height = this.size;
		finalCanvas.getContext('2d')!.drawImage(this.workCanvas, 0, 0);

		return finalCanvas;
	}
}
