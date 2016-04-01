package mcjty.rftoolsdim.dimensions.world;

import mcjty.rftoolsdim.config.WorldgenConfiguration;
import mcjty.rftoolsdim.dimensions.DimensionInformation;
import mcjty.rftoolsdim.dimensions.RfToolsDimensionManager;
import mcjty.rftoolsdim.dimensions.description.MobDescriptor;
import mcjty.rftoolsdim.dimensions.types.FeatureType;
import mcjty.rftoolsdim.dimensions.types.StructureType;
import mcjty.rftoolsdim.dimensions.types.TerrainType;
import mcjty.rftoolsdim.dimensions.world.mapgen.*;
import mcjty.rftoolsdim.dimensions.world.terrain.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.gen.ChunkProviderSettings;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.MapGenRavine;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import net.minecraft.world.gen.feature.WorldGenLakes;
import net.minecraft.world.gen.structure.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.TerrainGen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.*;

public class GenericChunkGenerator implements IChunkGenerator {

    public Random rand;
    public long seed;

    private World worldObj;
    public DimensionInformation dimensionInformation;
    public WorldType worldType;
    private final BaseTerrainGenerator terrainGenerator;

    // @todo, examine and consider customizing
    private ChunkProviderSettings settings = new ChunkProviderSettings.Factory().func_177864_b();

    private List<BiomeGenBase.SpawnListEntry> extraSpawns;
    private List<Integer> extraSpawnsMax;

    public BiomeGenBase[] biomesForGeneration;

    private MapGenBase caveGenerator = new MapGenCaves();

    // RFTools specific features.
    private MapGenTendrils tendrilGenerator = new MapGenTendrils(this);
    private MapGenCanyons canyonGenerator = new MapGenCanyons(this);
    private MapGenPyramids pyramidGenerator = new MapGenPyramids(this);
    private MapGenOrbs sphereGenerator = new MapGenOrbs(this, false);
    private MapGenOrbs hugeSphereGenerator = new MapGenOrbs(this, true);
    private MapGenRuinedCities ruinedCitiesGenerator = new MapGenRuinedCities(this);
    private MapGenLiquidOrbs liquidSphereGenerator = new MapGenLiquidOrbs(this, false);
    private MapGenLiquidOrbs hugeLiquidSphereGenerator = new MapGenLiquidOrbs(this, true);
    private MapGenBase denseCaveGenerator = new MapGenDenseCaves(this);

    private MapGenStronghold strongholdGenerator = new MapGenStronghold();
    private StructureOceanMonument oceanMonumentGenerator = new StructureOceanMonument();
    private MapGenVillage villageGenerator = new MapGenVillage();
    private MapGenMineshaft mineshaftGenerator = new MapGenMineshaft();
    public MapGenNetherBridge genNetherBridge = new MapGenNetherBridge();
    private MapGenScatteredFeature scatteredFeatureGenerator = new MapGenScatteredFeature();

    // Holds ravine generator
    private MapGenBase ravineGenerator = new MapGenRavine();

    {
        caveGenerator = TerrainGen.getModdedMapGen(caveGenerator, CAVE);
//        tendrilGenerator = TerrainGen.getModdedMapGen(tendrilGenerator, CAVE);
//        canyonGenerator = TerrainGen.getModdedMapGen(canyonGenerator, RAVINE);
//        sphereGenerator = TerrainGen.getModdedMapGen(sphereGenerator, RAVINE);
        strongholdGenerator = (MapGenStronghold) TerrainGen.getModdedMapGen(strongholdGenerator, STRONGHOLD);
        villageGenerator = (MapGenVillage) TerrainGen.getModdedMapGen(villageGenerator, VILLAGE);
        mineshaftGenerator = (MapGenMineshaft) TerrainGen.getModdedMapGen(mineshaftGenerator, MINESHAFT);
        scatteredFeatureGenerator = (MapGenScatteredFeature) TerrainGen.getModdedMapGen(scatteredFeatureGenerator, SCATTERED_FEATURE);
        ravineGenerator = TerrainGen.getModdedMapGen(ravineGenerator, RAVINE);
        genNetherBridge = (MapGenNetherBridge) TerrainGen.getModdedMapGen(genNetherBridge, NETHER_BRIDGE);
        oceanMonumentGenerator = (StructureOceanMonument) net.minecraftforge.event.terraingen.TerrainGen.getModdedMapGen(oceanMonumentGenerator, net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.OCEAN_MONUMENT);
    }

    public ChunkProviderSettings getSettings() {
        return settings;
    }


    public GenericChunkGenerator(World world, long seed) {
        this.worldObj = world;

        dimensionInformation = RfToolsDimensionManager.getDimensionManager(world).getDimensionInformation(world.provider.getDimension());

        this.worldType = world.getWorldInfo().getTerrainType();

        if (dimensionInformation.getTerrainType() == TerrainType.TERRAIN_AMPLIFIED) {
            worldType = WorldType.AMPLIFIED;
        } else if (dimensionInformation.getTerrainType() == TerrainType.TERRAIN_NORMAL && !WorldgenConfiguration.normalTerrainInheritsOverworld) {
            worldType = WorldType.DEFAULT;
        } else if (dimensionInformation.getTerrainType() == TerrainType.TERRAIN_FLAT) {
            worldType = WorldType.FLAT;
        }

        this.seed = seed;
        this.rand = new Random((seed + 516) * 314);

        switch (dimensionInformation.getTerrainType()) {
            case TERRAIN_VOID:
                terrainGenerator = new VoidTerrainGenerator();
                break;
            case TERRAIN_FLAT:
                terrainGenerator = new FlatTerrainGenerator((byte) 63);
                break;
            case TERRAIN_AMPLIFIED:
                terrainGenerator = new AmplifiedTerrainGenerator();
                break;
            case TERRAIN_NEARLANDS:
                terrainGenerator = new NearlandsTerrainGenerator();
                break;
            case TERRAIN_NORMAL:
                terrainGenerator = new NormalTerrainGenerator();
                break;
            case TERRAIN_ISLAND:
                terrainGenerator = new IslandTerrainGenerator(IslandTerrainGenerator.NORMAL);
                break;
            case TERRAIN_ISLANDS:
                terrainGenerator = new IslandTerrainGenerator(IslandTerrainGenerator.ISLANDS);
                break;
            case TERRAIN_CHAOTIC:
                terrainGenerator = new IslandTerrainGenerator(IslandTerrainGenerator.CHAOTIC);
                break;
            case TERRAIN_PLATEAUS:
                terrainGenerator = new IslandTerrainGenerator(IslandTerrainGenerator.PLATEAUS);
                break;
            case TERRAIN_GRID:
                terrainGenerator = new GridTerrainGenerator();
                break;
            case TERRAIN_CAVERN:
                terrainGenerator = new CavernTerrainGenerator(null);
                break;
            case TERRAIN_LOW_CAVERN:
                terrainGenerator = new CavernTerrainGenerator(CavernTerrainGenerator.CavernHeight.HEIGHT_128);
                break;
            case TERRAIN_FLOODED_CAVERN:
                terrainGenerator = new CavernTerrainGenerator(CavernTerrainGenerator.CavernHeight.HEIGHT_128);
                break;
            case TERRAIN_LIQUID:
                terrainGenerator = new LiquidTerrainGenerator();
                break;
            case TERRAIN_SOLID:
                terrainGenerator = new FlatTerrainGenerator((byte) 127);
                break;
            case TERRAIN_WAVES:
                terrainGenerator = new WavesTerrainGenerator(false);
                break;
            case TERRAIN_FILLEDWAVES:
                terrainGenerator = new WavesTerrainGenerator(true);
                break;
            case TERRAIN_ROUGH:
                terrainGenerator = new RoughTerrainGenerator(false);
                break;
            default:
                terrainGenerator = new VoidTerrainGenerator();
                break;
        }

        terrainGenerator.setup(world, this);

        extraSpawns = new ArrayList<>();
        extraSpawnsMax = new ArrayList<>();
        for (MobDescriptor mob : dimensionInformation.getExtraMobs()) {
            Class<? extends Entity> entityClass = mob.getEntityClass();
            extraSpawns.add(new BiomeGenBase.SpawnListEntry((Class<? extends EntityLiving>) entityClass, mob.getSpawnChance(), mob.getMinGroup(), mob.getMaxGroup()));
            extraSpawnsMax.add(mob.getMaxLoaded());
        }

    }

    @Override
    public Chunk provideChunk(int chunkX, int chunkZ) {
        this.rand.setSeed((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L);
        ChunkPrimer chunkprimer = new ChunkPrimer();
        terrainGenerator.generate(chunkX, chunkZ, chunkprimer);
        this.biomesForGeneration = this.worldObj.getBiomeProvider().loadBlockGeneratorData(this.biomesForGeneration, chunkX * 16, chunkZ * 16, 16, 16);
        terrainGenerator.replaceBlocksForBiome(chunkX, chunkZ, chunkprimer, this.biomesForGeneration);

        if (dimensionInformation.hasFeatureType(FeatureType.FEATURE_TENDRILS)) {
            this.tendrilGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }
        if (dimensionInformation.hasFeatureType(FeatureType.FEATURE_CANYONS)) {
            this.canyonGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }
        if (dimensionInformation.hasFeatureType(FeatureType.FEATURE_PYRAMIDS)) {
            this.pyramidGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }
        if (dimensionInformation.hasFeatureType(FeatureType.FEATURE_ORBS)) {
            this.sphereGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }
        if (dimensionInformation.hasFeatureType(FeatureType.FEATURE_HUGEORBS)) {
            this.hugeSphereGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }
        if (dimensionInformation.hasFeatureType(FeatureType.FEATURE_LIQUIDORBS)) {
            this.liquidSphereGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }
        if (dimensionInformation.hasFeatureType(FeatureType.FEATURE_HUGELIQUIDORBS)) {
            this.hugeLiquidSphereGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }
        if (dimensionInformation.hasFeatureType(FeatureType.FEATURE_CAVES)) {
            this.caveGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }
        if (dimensionInformation.hasFeatureType(FeatureType.FEATURE_DENSE_CAVES)) {
            this.denseCaveGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }
        if (dimensionInformation.hasFeatureType(FeatureType.FEATURE_RAVINES)) {
            this.ravineGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }

        if (dimensionInformation.hasStructureType(StructureType.STRUCTURE_MINESHAFT)) {
            this.mineshaftGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }
        if (dimensionInformation.hasStructureType(StructureType.STRUCTURE_VILLAGE)) {
            this.villageGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }
        if (dimensionInformation.hasStructureType(StructureType.STRUCTURE_STRONGHOLD)) {
            this.strongholdGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }
        if (dimensionInformation.hasStructureType(StructureType.STRUCTURE_FORTRESS)) {
            this.genNetherBridge.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }
        if (dimensionInformation.hasStructureType(StructureType.STRUCTURE_SCATTERED)) {
            this.scatteredFeatureGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }

        //@todo
        if (true) {
            this.oceanMonumentGenerator.generate(this.worldObj, chunkX, chunkZ, chunkprimer);
        }

//        this.ruinedCitiesGenerator.generate(this.worldObj, chunkX, chunkZ, ablock, abyte);

        Chunk chunk = new Chunk(this.worldObj, chunkprimer, chunkX, chunkZ);
        byte[] abyte = chunk.getBiomeArray();

        for (int i = 0; i < abyte.length; ++i) {
            abyte[i] = (byte) BiomeGenBase.getIdForBiome(this.biomesForGeneration[i]);
        }

        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public void populate(int chunkX, int chunkZ) {
        BlockFalling.fallInstantly = true;
        int x = chunkX * 16;
        int z = chunkZ * 16;
        BiomeGenBase biomegenbase = this.worldObj.getBiomeGenForCoords(new BlockPos(x + 16, 0, z + 16));
        this.rand.setSeed(this.worldObj.getSeed());
        long i1 = this.rand.nextLong() / 2L * 2L + 1L;
        long j1 = this.rand.nextLong() / 2L * 2L + 1L;
        this.rand.setSeed(chunkX * i1 + chunkZ * j1 ^ this.worldObj.getSeed());
        boolean flag = false;

        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(this, worldObj, rand, chunkX, chunkZ, flag));

        ChunkCoordIntPair cp = new ChunkCoordIntPair(chunkX, chunkZ);

        if (dimensionInformation.hasStructureType(StructureType.STRUCTURE_MINESHAFT)) {
            this.mineshaftGenerator.generateStructure(this.worldObj, this.rand, cp);
        }
        if (dimensionInformation.hasStructureType(StructureType.STRUCTURE_VILLAGE)) {
            flag = this.villageGenerator.generateStructure(this.worldObj, this.rand, cp);
        }
        if (dimensionInformation.hasStructureType(StructureType.STRUCTURE_STRONGHOLD)) {
            this.strongholdGenerator.generateStructure(this.worldObj, this.rand, cp);
        }
        if (dimensionInformation.hasStructureType(StructureType.STRUCTURE_FORTRESS)) {
            this.genNetherBridge.generateStructure(this.worldObj, this.rand, cp);
        }
        if (dimensionInformation.hasStructureType(StructureType.STRUCTURE_SCATTERED)) {
            this.scatteredFeatureGenerator.generateStructure(this.worldObj, this.rand, cp);
        }

        int k1;
        int l1;
        int i2;


        if (dimensionInformation.hasFeatureType(FeatureType.FEATURE_LAKES)) {
            if (dimensionInformation.getFluidsForLakes().length == 0) {
                // No specific liquid dimlets specified: we generate default lakes (water and lava were appropriate).
                if (biomegenbase != Biomes.desert && biomegenbase != Biomes.desertHills && !flag && this.rand.nextInt(4) == 0
                        && TerrainGen.populate(this, worldObj, rand, chunkX, chunkZ, flag, PopulateChunkEvent.Populate.EventType.LAKE)) {
                    k1 = x + this.rand.nextInt(16) + 8;
                    l1 = this.rand.nextInt(256);
                    i2 = z + this.rand.nextInt(16) + 8;
                    (new WorldGenLakes(Blocks.water)).generate(this.worldObj, this.rand, new BlockPos(k1, l1, i2));
                }

                if (TerrainGen.populate(this, worldObj, rand, chunkX, chunkZ, flag, PopulateChunkEvent.Populate.EventType.LAVA) && !flag && this.rand.nextInt(8) == 0) {
                    k1 = x + this.rand.nextInt(16) + 8;
                    l1 = this.rand.nextInt(this.rand.nextInt(248) + 8);
                    i2 = z + this.rand.nextInt(16) + 8;

                    if (l1 < 63 || this.rand.nextInt(10) == 0) {
                        (new WorldGenLakes(Blocks.lava)).generate(this.worldObj, this.rand, new BlockPos(k1, l1, i2));
                    }
                }
            } else {
                // Generate lakes for the specified biomes.
                for (Block liquid : dimensionInformation.getFluidsForLakes()) {
                    if (!flag && this.rand.nextInt(4) == 0
                            && TerrainGen.populate(this, worldObj, rand, chunkX, chunkZ, flag, PopulateChunkEvent.Populate.EventType.LAKE)) {
                        k1 = x + this.rand.nextInt(16) + 8;
                        l1 = this.rand.nextInt(256);
                        i2 = z + this.rand.nextInt(16) + 8;
                        (new WorldGenLakes(liquid)).generate(this.worldObj, this.rand, new BlockPos(k1, l1, i2));
                    }
                }
            }
        }

        boolean doGen = false;
        if (dimensionInformation.hasStructureType(StructureType.STRUCTURE_DUNGEON)) {
            doGen = TerrainGen.populate(this, worldObj, rand, chunkX, chunkZ, flag, PopulateChunkEvent.Populate.EventType.DUNGEON);
            for (k1 = 0; doGen && k1 < 8; ++k1) {
                l1 = x + this.rand.nextInt(16) + 8;
                i2 = this.rand.nextInt(256);
                int j2 = z + this.rand.nextInt(16) + 8;
                (new WorldGenDungeons()).generate(this.worldObj, this.rand, new BlockPos(l1, i2, j2));
            }
        }

        biomegenbase.decorate(this.worldObj, this.rand, new BlockPos(x, 0, z));
        if (TerrainGen.populate(this, worldObj, rand, chunkX, chunkZ, flag, PopulateChunkEvent.Populate.EventType.ANIMALS)) {
            WorldEntitySpawner.performWorldGenSpawning(this.worldObj, biomegenbase, x + 8, z + 8, 16, 16, this.rand);
        }
        x += 8;
        z += 8;

        doGen = TerrainGen.populate(this, worldObj, rand, chunkX, chunkZ, flag, PopulateChunkEvent.Populate.EventType.ICE);
        for (k1 = 0; doGen && k1 < 16; ++k1) {
            for (l1 = 0; l1 < 16; ++l1) {
                i2 = this.worldObj.getPrecipitationHeight(new BlockPos(x + k1, 0, z + l1)).getY();

                if (this.worldObj.canBlockFreeze(new BlockPos(k1 + x, i2 - 1, l1 + z), false)) {
                    this.worldObj.setBlockState(new BlockPos(k1 + x, i2 - 1, l1 + z), Blocks.ice.getDefaultState(), 2);
                }

                if (this.worldObj.canSnowAt(new BlockPos(k1 + x, i2, l1 + z), true)) {
                    this.worldObj.setBlockState(new BlockPos(k1 + x, i2, l1 + z), Blocks.snow_layer.getDefaultState(), 2);
                }
            }
        }

        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(this, worldObj, rand, chunkX, chunkZ, flag));

        BlockFalling.fallInstantly = false;

    }

    @Override
    public boolean generateStructures(Chunk chunkIn, int x, int z) {
        boolean flag = false;

        boolean mapFeaturesEnabled = true;  //@todo configurable!
        if (this.settings.useMonuments && mapFeaturesEnabled && chunkIn.getInhabitedTime() < 3600L) {
            flag |= this.oceanMonumentGenerator.generateStructure(this.worldObj, this.rand, new ChunkCoordIntPair(x, z));
        }

        return flag;
    }

    @Override
    public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        List creatures = getDefaultCreatures(creatureType, pos);
        if (extraSpawns.isEmpty()) {
            return creatures;
        }

        if (creatureType == EnumCreatureType.AMBIENT) {
            creatures = new ArrayList(creatures);
            for (int i = 0; i < extraSpawns.size(); i++) {
                Class entityClass = extraSpawns.get(i).entityClass;
                if (IAnimals.class.isAssignableFrom(entityClass)) {
                    int count = worldObj.countEntities(entityClass);
                    if (count < extraSpawnsMax.get(i)) {
                        creatures.add(extraSpawns.get(i));
                    }
                }
            }
        } else if (creatureType == EnumCreatureType.MONSTER) {
            creatures = new ArrayList(creatures);
            for (int i = 0; i < extraSpawns.size(); i++) {
                Class entityClass = extraSpawns.get(i).entityClass;
                if (IMob.class.isAssignableFrom(entityClass)) {
                    int count = worldObj.countEntities(entityClass);
                    if (count < extraSpawnsMax.get(i)) {
                        creatures.add(extraSpawns.get(i));
                    }
                }
            }
        }


        return creatures;
    }

    private List getDefaultCreatures(EnumCreatureType creatureType, BlockPos pos) {
        BiomeGenBase biomegenbase = this.worldObj.getBiomeGenForCoords(pos);
        if (creatureType == EnumCreatureType.MONSTER) {
            if (dimensionInformation.isPeaceful()) {
                return Collections.emptyList();
            }
            if (dimensionInformation.hasStructureType(StructureType.STRUCTURE_SCATTERED)) {
                if (this.scatteredFeatureGenerator.func_175798_a(pos)) {
                    return this.scatteredFeatureGenerator.getScatteredFeatureSpawnList();
                }
            }

            if (dimensionInformation.hasStructureType(StructureType.STRUCTURE_FORTRESS)) {
                if (this.genNetherBridge.func_175795_b(pos)) {
                    return this.genNetherBridge.getSpawnList();
                }

                if (this.genNetherBridge.isPositionInStructure(this.worldObj, pos) && this.worldObj.getBlockState(pos.down()).getBlock() == Blocks.nether_brick) {
                    return this.genNetherBridge.getSpawnList();
                }
            }
        } else if (creatureType == EnumCreatureType.AMBIENT) {
            if (dimensionInformation.isNoanimals()) {
                return Collections.emptyList();
            }
        }

        return biomegenbase.getSpawnableList(creatureType);
    }

    @Override
    public BlockPos getStrongholdGen(World worldIn, String structureName, BlockPos position) {
        return "Stronghold".equals(structureName) && this.strongholdGenerator != null ? this.strongholdGenerator.getClosestStrongholdPos(worldIn, position) : null;
    }

    @Override
    public void recreateStructures(Chunk chunkIn, int x, int z) {
        boolean mapFeaturesEnabled = true;  //@todo configurable!
        if (mapFeaturesEnabled) {
            if (this.settings.useMineShafts) {
                this.mineshaftGenerator.generate(this.worldObj, x, z, null);
            }

            if (this.settings.useVillages) {
                this.villageGenerator.generate(this.worldObj, x, z, null);
            }

            if (this.settings.useStrongholds) {
                this.strongholdGenerator.generate(this.worldObj, x, z, null);
            }

            if (this.settings.useTemples) {
                this.scatteredFeatureGenerator.generate(this.worldObj, x, z, null);
            }

            if (this.settings.useMonuments) {
                this.oceanMonumentGenerator.generate(this.worldObj, x, z, null);
            }
        }
    }
}