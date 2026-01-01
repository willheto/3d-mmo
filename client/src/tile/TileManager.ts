import * as THREE from 'three';
import { World } from '../world/World';

export class TileManager {
	private world: World;
	private readonly LAYER_HEIGHT = 0.5;

	private textureLoader = new THREE.TextureLoader();
	private tileMaterials: Map<number, THREE.MeshStandardMaterial> = new Map();
	private objectMaterials: Map<number, THREE.MeshStandardMaterial> = new Map();
	private tiles = new Map<string, THREE.Mesh>();

	constructor(world: World) {
		this.world = world;
		this.setupTileMaterials();
		this.setupGroundFromCsv(new URL('./map_layer1.csv', import.meta.url).href, 0);
		this.setupGroundFromCsv(new URL('./map_layer2.csv', import.meta.url).href, 1);

		this.setupObjectMaterials();
		this.setupObjectsFromCsv(new URL('./map_objects.csv', import.meta.url).href);

		setTimeout(() => {
			this.applySlopes();
		}, 1000);
	}

	async loadCsvMap(path: string): Promise<number[][]> {
		const res = await fetch(path);
		const text = await res.text();

		return text
			.trim()
			.split('\n')
			.map(row => row.split(',').map(v => Number(v)));
	}

	public getTileHeight(tx: number, tz: number): number {
		let maxY = 0;

		for (const group of this.world.groundGroups) {
			for (const child of group.children) {
				if (child.userData.tx === tx && child.userData.tz === tz) {
					maxY = Math.max(maxY, child.userData.height);
				}
			}
		}

		return maxY;
	}

	async setupObjectsFromCsv(path: string) {
		const map = await this.loadCsvMap(path);
		const group = new THREE.Group();

		for (let z = 0; z < map.length; z++) {
			for (let x = 0; x < map[z].length; x++) {
				const tileId = map[z][x];

				// Tree
				if (tileId === 2) {
					const tree = this.createTree();

					const y = this.getTileHeight(x, z);
					tree.position.set(x, y, z);

					group.add(tree);
				}
			}
		}

		this.world.scene.add(group);
	}

	async setupGroundFromCsv(path: string, layerIndex: number) {
		const map = await this.loadCsvMap(path);
		const tileGeo = new THREE.PlaneGeometry(this.world.TILE_SIZE, this.world.TILE_SIZE);

		const group = new THREE.Group();
		const y = layerIndex * this.LAYER_HEIGHT;

		for (let z = 0; z < map.length; z++) {
			for (let x = 0; x < map[z].length; x++) {
				const tileId = map[z][x];
				const mat = this.tileMaterials.get(tileId);
				if (!mat) continue;

				const tile = new THREE.Mesh(tileGeo.clone(), mat);
				tile.rotation.x = -Math.PI / 2;
				tile.position.set(x, y, z);

				// store height on the mesh (VERY IMPORTANT)
				tile.userData.height = y;
				tile.userData.tx = x;
				tile.userData.tz = z;

				const key = `${x},${z},${layerIndex}`;
				this.tiles.set(key, tile);

				group.add(tile);
			}
		}

		this.world.groundGroups.push(group);
		this.world.scene.add(group);
	}

	private createTree(): THREE.Group {
		const trunkMat = this.objectMaterials.get(2)!;
		const leafMat = this.objectMaterials.get(33)!;

		// --- Trunk ---
		const trunkHeight = 2.8;
		const trunk = new THREE.Mesh(new THREE.CylinderGeometry(0.22, 0.38, trunkHeight, 12, 4), trunkMat);
		trunk.position.y = trunkHeight / 2;
		trunk.rotation.z = (Math.random() - 0.5) * 0.08;

		// --- Leaves ---
		leafMat.transparent = true;
		leafMat.alphaTest = 0.5;
		leafMat.depthWrite = false;

		const foliage = new THREE.Group();

		const layers = [
			{ r: 1.6, h: 1.6, y: 0.0 },
			{ r: 1.3, h: 1.5, y: 0.9 },
			{ r: 1.0, h: 1.4, y: 1.7 },
		];

		layers.forEach(l => {
			const cone = new THREE.Mesh(new THREE.ConeGeometry(l.r, l.h, 12), leafMat);
			cone.position.y = trunkHeight + l.y - 0.3;
			foliage.add(cone);
		});

		// --- Group ---
		const tree = new THREE.Group();
		tree.add(trunk);
		tree.add(foliage);

		return tree;
	}

	private applySlopes() {
		for (const [key, tile] of this.tiles) {
			const [x, z, layer] = key.split(',').map(Number);

			// Only upper layer slopes
			if (layer !== 1) continue;

			const neighbors = {
				north: this.tiles.has(`${x},${z - 1},0`),
				south: this.tiles.has(`${x},${z + 1},0`),
				west: this.tiles.has(`${x - 1},${z},0`),
				east: this.tiles.has(`${x + 1},${z},0`),

				nw: this.tiles.has(`${x - 1},${z - 1},0`),
				ne: this.tiles.has(`${x + 1},${z - 1},0`),
				sw: this.tiles.has(`${x - 1},${z + 1},0`),
				se: this.tiles.has(`${x + 1},${z + 1},0`),
			};

			this.slopeTileAdvanced(tile, neighbors);
		}
	}

	private slopeTileAdvanced(
		tile: THREE.Mesh,
		n: {
			north: boolean;
			south: boolean;
			west: boolean;
			east: boolean;
			nw: boolean;
			ne: boolean;
			sw: boolean;
			se: boolean;
		},
	) {
		const geom = tile.geometry as THREE.PlaneGeometry;
		const pos = geom.attributes.position as THREE.BufferAttribute;

		const DROP = -this.LAYER_HEIGHT;

		// Vertex indices:
		// 0 = NW, 1 = NE, 2 = SW, 3 = SE
		const drop = [false, false, false, false];

		// Edge neighbors
		if (n.north) {
			drop[0] = true;
			drop[1] = true;
		}
		if (n.south) {
			drop[2] = true;
			drop[3] = true;
		}
		if (n.west) {
			drop[0] = true;
			drop[2] = true;
		}
		if (n.east) {
			drop[1] = true;
			drop[3] = true;
		}

		// Diagonal-only neighbors
		if (n.nw) drop[0] = true;
		if (n.ne) drop[1] = true;
		if (n.sw) drop[2] = true;
		if (n.se) drop[3] = true;

		// Apply (single-step drop, clamped)
		for (let i = 0; i < 4; i++) {
			pos.setZ(i, drop[i] ? DROP : 0);
		}

		pos.needsUpdate = true;
		geom.computeVertexNormals();
	}

	private setupTileMaterials() {
		const grassTex = this.loadTexture(new URL('./textures/brown_mud_leaves_01_diff_1k.jpg', import.meta.url).href);
		const waterTex = this.loadTexture(new URL('./textures/water.png', import.meta.url).href);
		const pebbles = this.loadTexture(new URL('./textures/pebblefloor.png', import.meta.url).href);

		this.tileMaterials.set(0, new THREE.MeshStandardMaterial({ map: grassTex, flatShading: true }));
		this.tileMaterials.set(1, new THREE.MeshStandardMaterial({ map: waterTex, flatShading: true }));
		this.tileMaterials.set(3, new THREE.MeshStandardMaterial({ map: pebbles, flatShading: true }));
	}

	private setupObjectMaterials() {
		const leafTex = this.loadTexture(new URL('./textures/leafybase.png', import.meta.url).href);
		const barkTex = this.loadTexture(new URL('./textures/bark.png', import.meta.url).href);

		this.objectMaterials.set(2, new THREE.MeshStandardMaterial({ map: barkTex, flatShading: true }));

		this.objectMaterials.set(
			33, // leaves (internal ID, not from CSV)
			new THREE.MeshStandardMaterial({ map: leafTex, flatShading: true }),
		);
	}

	private loadTexture(path: string, repeat = 1) {
		const tex = this.textureLoader.load(path);
		tex.wrapS = tex.wrapT = THREE.RepeatWrapping;
		tex.repeat.set(repeat, repeat);
		tex.magFilter = THREE.NearestFilter;
		tex.minFilter = THREE.NearestMipMapNearestFilter;
		tex.anisotropy = 1;
		return tex;
	}
}
