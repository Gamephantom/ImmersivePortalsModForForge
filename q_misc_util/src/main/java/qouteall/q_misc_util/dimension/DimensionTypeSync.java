package qouteall.q_misc_util.dimension;

import com.google.common.collect.Streams;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DimensionTypeSync {

    @OnlyIn(Dist.CLIENT)
    public static Map<ResourceKey<Level>, ResourceKey<DimensionType>> clientTypeMap;

    @OnlyIn(Dist.CLIENT)
    private static RegistryAccess currentDimensionTypeTracker;

    @OnlyIn(Dist.CLIENT)
    public static void onGameJoinPacketReceived(RegistryAccess tracker) {
        currentDimensionTypeTracker = tracker;
    }

    @OnlyIn(Dist.CLIENT)
    private static Map<ResourceKey<Level>, ResourceKey<DimensionType>> typeMapFromTag(CompoundTag tag) {
        Map<ResourceKey<Level>, ResourceKey<DimensionType>> result = new HashMap<>();
        tag.getAllKeys().forEach(key -> {
            ResourceKey<Level> worldKey = DimId.idToKey(key);
            
            String val = tag.getString(key);
            
            ResourceKey<DimensionType> typeKey =
                ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation(val));
            
            result.put(worldKey, typeKey);
        });
        
        return result;
    }

    @OnlyIn(Dist.CLIENT)
    public static void acceptTypeMapData(CompoundTag tag) {
        clientTypeMap = typeMapFromTag(tag);
        
        Helper.log("Received Dimension Type Sync");
        Helper.log("\n" + Helper.myToString(
            clientTypeMap.entrySet().stream().map(
                e -> e.getKey().location().toString() + " -> " + e.getValue().location()
            )
        ));
    }
    
    public static CompoundTag createTagFromServerWorldInfo() {
        RegistryAccess registryManager = MiscHelper.getServer().registryAccess();
        Registry<DimensionType> dimensionTypes = registryManager.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
        return typeMapToTag(
            Streams.stream(MiscHelper.getServer().getAllLevels()).collect(
                Collectors.toMap(
                    Level::dimension,
                    w -> {
                        DimensionType dimensionType = w.dimensionType();
                        ResourceLocation id = dimensionTypes.getKey(dimensionType);
                        if (id == null) {
                            Helper.err("Missing dim type id for " + w.dimension());
                            Helper.err("Registered dimension types " +
                                Helper.myToString(dimensionTypes.keySet().stream()));
                            return DimensionType.OVERWORLD_LOCATION;
                        }
                        return idToDimType(id);
                    }
                )
            )
        );
    }
    
    public static ResourceKey<DimensionType> idToDimType(ResourceLocation id) {
        return ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, id);
    }
    
    private static CompoundTag typeMapToTag(Map<ResourceKey<Level>, ResourceKey<DimensionType>> data) {
        CompoundTag tag = new CompoundTag();
        data.forEach((worldKey, typeKey) -> {
            tag.put(worldKey.location().toString(), StringTag.valueOf(typeKey.location().toString()));
        });
        return tag;
    }

    @OnlyIn(Dist.CLIENT)
    public static ResourceKey<DimensionType> getDimensionTypeKey(ResourceKey<Level> worldKey) {
        if (worldKey == Level.OVERWORLD) {
            return DimensionType.OVERWORLD_LOCATION;
        }
        
        if (worldKey == Level.NETHER) {
            return DimensionType.NETHER_LOCATION;
        }
        
        if (worldKey == Level.END) {
            return DimensionType.END_LOCATION;
        }
        
        ResourceKey<DimensionType> obj = clientTypeMap.get(worldKey);
        
        if (obj == null) {
            Helper.err("Missing Dimension Type For " + worldKey);
            return DimensionType.OVERWORLD_LOCATION;
        }
        
        return obj;
    }

    @OnlyIn(Dist.CLIENT)
    public static DimensionType getDimensionType(ResourceKey<DimensionType> registryKey) {
        return currentDimensionTypeTracker.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).get(registryKey);
    }
    
}
