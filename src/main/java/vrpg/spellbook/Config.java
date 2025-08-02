package vrpg.spellbook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

class Config {
    public String prefix = "vrpg_spell";
    private SpellInfo[] spells = new SpellInfo[0];
    public transient Map<String, SpellInfo> spellMap = null;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("vrpg/config/spell-book.json");
    //todo: add spell-book.schema.json
    //todo: perform multiple action at once
    //todo: dynamic read config file, no need to restart the game

    public static Config load() {
        File configDir = CONFIG_FILE.getParentFile();
        File vrpgDir = configDir.getParentFile();

        if (!vrpgDir.exists() && !vrpgDir.mkdir()) {
            VRPGSpellBook.LOGGER.error("Failed to create directory: {}", vrpgDir.getPath());
            throw new RuntimeException("Failed to create directory: " + vrpgDir.getPath());
        }

        if (!configDir.exists() && !configDir.mkdir()) {
            VRPGSpellBook.LOGGER.error("Failed to create directory: {}", configDir.getPath());
            throw new RuntimeException("Failed to create directory: " + configDir.getPath());
        }

        if (!CONFIG_FILE.exists()) {
            Config defaultConfig = new Config();
            save(defaultConfig);
            return defaultConfig;
        }

        try (Reader reader = new FileReader(CONFIG_FILE)) {
            var config = GSON.fromJson(reader, Config.class);
            var map = new HashMap<String, SpellInfo>();
            for (var spell : config.spells) {
                for (var entry : spell.localizedSpell.entrySet()) {
                    var spellText = entry.getValue();
                    map.put(spellText, spell);
                }
            }
            config.spellMap = map;
            return config;
        } catch (IOException e) {
            VRPGSpellBook.LOGGER.error("Failed to read config: {}", CONFIG_FILE.getPath(), e);
            throw new RuntimeException(e);
        }
    }

    private static void save(Config config) {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            VRPGSpellBook.LOGGER.error("Failed to write config: {}", CONFIG_FILE.getPath(), e);
            throw new RuntimeException(e);
        }
    }
}
