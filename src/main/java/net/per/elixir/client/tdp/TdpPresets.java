package net.per.elixir.client.tdp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TdpPresets {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("elixir").resolve("tdp_presets.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static JsonObject cache;

    private TdpPresets() {
    }

    private static JsonObject load() {
        if (cache != null) return cache;
        var root = new JsonObject();
        if (Files.exists(FILE)) {
            try {
                root = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), JsonObject.class);
            } catch (IOException | RuntimeException e) {
                root = new JsonObject();
            }
        }
        cache = root == null ? new JsonObject() : root;
        return cache;
    }

    private static JsonArray listOf(String kind) {
        var arr = load().getAsJsonArray(kind);
        if (arr == null) {
            arr = new JsonArray();
            load().add(kind, arr);
        }
        return arr;
    }

    public static List<String> names(String kind) {
        var out = new ArrayList<String>();
        for (var e : listOf(kind)) {
            if (e.isJsonObject() && e.getAsJsonObject().has("name")) {
                out.add(e.getAsJsonObject().get("name").getAsString());
            }
        }
        return out;
    }

    public static JsonObject get(String kind, String name) {
        for (var e : listOf(kind)) {
            var o = e.getAsJsonObject();
            if (name.equals(o.get("name").getAsString())) return o.deepCopy();
        }
        return null;
    }

    public static void put(String kind, JsonObject data) {
        var list = listOf(kind);
        String name = data.get("name").getAsString();
        for (int i = 0; i < list.size(); i++) {
            if (name.equals(list.get(i).getAsJsonObject().get("name").getAsString())) {
                list.set(i, data);
                save();
                return;
            }
        }
        list.add(data);
        save();
    }

    public static void remove(String kind, String name) {
        var list = listOf(kind);
        for (int i = 0; i < list.size(); i++) {
            if (name.equals(list.get(i).getAsJsonObject().get("name").getAsString())) {
                list.remove(i);
                save();
                return;
            }
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(load()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            cache = null;
        }
    }
}
