package corgitaco.riderdenier;

import com.google.gson.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.registry.Registry;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("deprecation")
public class ConfigSerializer implements JsonDeserializer<ConfigSerializer.Holder> {


    public static final HashMap<ResourceLocation, Set<EntityType<?>>> BLACKLISTED_ENTITIES = new HashMap<>();

    @Override
    public ConfigSerializer.Holder deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Set<Map.Entry<String, JsonElement>> dimensionEntries = json.getAsJsonObject().entrySet();
        HashMap<ResourceLocation, Set<EntityType<?>>> output = new HashMap<>();
        for (Map.Entry<String, JsonElement> dimensionEntry : dimensionEntries) {
            ResourceLocation dimension = tryParse(dimensionEntry.getKey());
            if (dimension == null) {
                continue;
            }

            JsonArray blackListedRideables = dimensionEntry.getValue().getAsJsonArray();
            ObjectOpenHashSet<EntityType<?>> entityTypes = new ObjectOpenHashSet<>();
            for (JsonElement blackListedRideable : blackListedRideables) {
                String entityID = blackListedRideable.getAsString();
                ResourceLocation entityTypeID = tryParse(entityID.toLowerCase());
                if (entityTypeID != null && !Registry.ENTITY_TYPE.keySet().contains(entityTypeID)) {
                    RiderDenier.LOGGER.error("\"" + entityID + "\" is not a valid entity ID. Skipping entry...");
                    continue;
                }
                entityTypes.add(Registry.ENTITY_TYPE.get(entityTypeID));

            }

            output.put(dimension, entityTypes);
        }
        return new Holder(output);
    }

    @Nullable
    public static ResourceLocation tryParse(String id) {
        try {
            return new ResourceLocation(id);
        } catch (ResourceLocationException resourcelocationexception) {
            RiderDenier.LOGGER.error(resourcelocationexception.getMessage());
            return null;
        }
    }

    public static <T extends Map<?, ?>> void handleConfig(Path path, T defaults) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Holder.class, new ConfigSerializer()).setPrettyPrinting().disableHtmlEscaping().create();

        final File CONFIG_FILE = new File(String.valueOf(path));

        if (!CONFIG_FILE.exists()) {
            createJson(path, defaults);
        }
        try (Reader reader = new FileReader(path.toString())) {
            Holder holder = gson.fromJson(reader, Holder.class);
            if (holder != null) {
                BLACKLISTED_ENTITIES.clear();
                BLACKLISTED_ENTITIES.putAll(holder.getMap());
            } else {
                RiderDenier.LOGGER.error("\"" + path.toString() + "\" failed to read.");
            }

        } catch (IOException e) {
            RiderDenier.LOGGER.error("\"" + path.toString() + "\" failed.\n" + e.toString());
        }
    }

    public static <T extends Map<?, ?>> void createJson(Path path, T map) {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        String jsonString = gson.toJson(map);

        try {
            Files.createDirectories(path.getParent());
            Files.write(path, jsonString.getBytes());
        } catch (IOException e) {
        }
    }

    public static class Holder {

        private final HashMap<ResourceLocation, Set<EntityType<?>>> map;

        public Holder(HashMap<ResourceLocation, Set<EntityType<?>>> map) {
            this.map = map;
        }

        public HashMap<ResourceLocation, Set<EntityType<?>>> getMap() {
            return map;
        }
    }
}
