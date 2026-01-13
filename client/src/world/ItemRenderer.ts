import * as THREE from 'three';
import { World } from '../world/World';
import { Item } from '../item/Item';

export class ItemRenderer {
	private world: World;
	private meshes = new Map<string, THREE.Mesh>();

	constructor(world: World) {
		this.world = world;
	}

	public update() {
		const seen = new Set<string>();

		for (const item of this.world.items) {
			if (!item.uniqueID) continue;
			if (item.worldX == null || item.worldY == null) continue;

			seen.add(item.uniqueID);

			let mesh = this.meshes.get(item.uniqueID);
			if (!mesh) {
				mesh = this.createLootMesh(item);
				this.meshes.set(item.uniqueID, mesh);
				this.world.scene.add(mesh);
			}

			this.updateMeshPosition(mesh, item);
		}

		// Remove deleted items
		for (const [id, mesh] of this.meshes) {
			if (!seen.has(id)) {
				this.world.scene.remove(mesh);
				mesh.geometry.dispose();
				(mesh.material as THREE.Material).dispose();
				this.meshes.delete(id);
			}
		}
	}

	private createLootMesh(item: Item): THREE.Mesh {
		const geo = new THREE.IcosahedronGeometry(0.25, 0);
		const mat = new THREE.MeshStandardMaterial({
			color: 0xffd36a,
			emissive: 0x332200,
			flatShading: true,
		});

		const mesh = new THREE.Mesh(geo, mat);
		mesh.castShadow = true;
		mesh.userData.itemID = item.itemID;

		return mesh;
	}

	private updateMeshPosition(mesh: THREE.Mesh, item: Item) {
		const x = item.worldX!;
		const z = item.worldY!;

		const groundY = this.world.tileManager.getTileHeight(x, z);

		mesh.position.set(
			x,
			groundY + 0.35, // offset above tile
			z,
		);

		// Simple idle animation (visual clarity)
		mesh.rotation.y += 0.02;
		mesh.position.y += Math.sin(performance.now() * 0.003) * 0.002;
	}
}
