package com.meeple.citybuild.server;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.joml.Vector2i;

import com.meeple.citybuild.server.LevelData.Chunk;
import com.meeple.shared.utils.CollectionSuppliers;
import com.meeple.shared.utils.FrameUtils;

public class WorldGenerator {

	public static enum TileTypes {
		//		Hole, Ground, Other, Housing, FoodProduction, WaterProduction;
		Terrain,
		Stockpile,
		Housing,
		Food,
		Power,
		Resources;

	}

	public static enum TileSize {
		Small,
		Medium,
		Large;
	}

	public static enum TerrainType { 
		Empty, 
		Water,
		Sand,
		Grass;
		private TerrainType(){
			terrainTypesSet.add(this);
		}
	}

	public static Map<TileTypes, Set<Tiles>> typesByTypes = new CollectionSuppliers.MapSupplier<TileTypes, Set<Tiles>>().get();
	public static Set<TerrainType> terrainTypesSet = new CollectionSuppliers.SetSupplier<TerrainType>().get();
	

	//TODO allow small kitchens to be put into buildings eg houses/factories
	public static enum Tiles {
		/*
		 * 
		 */
		Tent(TileTypes.Housing),
		House(TileTypes.Housing),
		/*
		 * 
		 */
		CropFarm(TileTypes.Food),
		MeatFarm(TileTypes.Food),
		Kitchens(TileTypes.Food),
		/*
		 * 
		 */
		WaterWheel(TileTypes.Power),
		/*
		 * 
		 */
		TreeFarm(TileTypes.Resources),
		StoneMine(TileTypes.Resources),
		NormalMetalMine(TileTypes.Resources),
		SpecialMetalMine(TileTypes.Resources);

		TileTypes type;

		private Tiles(TileTypes type) {
			this.type = type;
			FrameUtils.addToSetMap(typesByTypes, type, this, new CollectionSuppliers.SetSupplier<>());
		}

	}
	public WorldGenerator(){
		//minor hack to get the enum and static sets to load
		@SuppressWarnings("unused")
		Tiles a = Tiles.CropFarm;
		@SuppressWarnings("unused")
		TerrainType b = TerrainType.Empty;
	}

	//TODO customise this
	public void create(LevelData level, long seed) {
		
		int radi = 2;
		int minRadi = 2;
		Random random = new Random(seed);
		for (int x = -minRadi; x < radi; x++) {
			for (int y = -minRadi; y < radi; y++) {
				Vector2i chunkIndex = new Vector2i(x, y);
				Chunk mainChunk = level.new Chunk(chunkIndex);
				for (int tx = 0; tx < mainChunk.tiles.length; tx++) {
					for (int ty = 0; ty < mainChunk.tiles[tx].length; ty++) {

						//if (tx == 0 || ty == 0 || tx == mainChunk.tiles.length - 1 || ty == mainChunk.tiles[0].length - 1 ) {
						//	mainChunk.tiles[tx][ty].type = Tiles.Hole;
						//} else {
							mainChunk.tiles[tx][ty].terrain = TerrainType.Grass;
							mainChunk.tiles[tx][ty].height = random.nextInt(5);
							//mainChunk.tiles[tx][ty].type = Tiles.Ground;
						//}
					}
				}

				level.chunks.put(chunkIndex, mainChunk);
			}
		}
	}
}
