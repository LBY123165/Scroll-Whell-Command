package org.lby123165.scroll_whell_command.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "scroll_whell_command.json";

    private static ConfigData INSTANCE;

    private ConfigManager() {}

    public static ConfigData get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                INSTANCE = GSON.fromJson(reader, ConfigData.class);
            } catch (Exception e) {
                // Silent fallback to default on config load error
                INSTANCE = createDefault();
                save();
            }
        } else {
            INSTANCE = createDefault();
            save();
        }
        // null guards
        if (INSTANCE.settings == null) INSTANCE.settings = new ConfigData.GlobalSettings();
        if (INSTANCE.commands == null) INSTANCE.commands = new java.util.ArrayList<>();
    }

    public static void save() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            if (!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            // Silent error handling for config save
        }
    }

    private static ConfigData createDefault() {
        ConfigData data = new ConfigData();
        // Sample commands for first run
        ConfigData.CommandItem a = new ConfigData.CommandItem();
        a.type = "single";
        a.label = "Hello";
        a.command = "/say Hello World";
        data.commands.add(a);

        ConfigData.CommandItem b = new ConfigData.CommandItem();
        b.type = "multi";
        b.label = "Buff";
        b.subCommands = new java.util.ArrayList<>();
        ConfigData.SubCommand s1 = new ConfigData.SubCommand(); s1.command = "/effect give @s speed 5 1"; s1.delayMs = 0; b.subCommands.add(s1);
        ConfigData.SubCommand s2 = new ConfigData.SubCommand(); s2.command = "/effect give @s haste 5 1"; s2.delayMs = 250; b.subCommands.add(s2);
        data.commands.add(b);
        return data;
    }
}
