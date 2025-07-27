package vrpg.spellbook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VRPGSpellBook implements ModInitializer {
	public static final String MOD_ID = "vrpg-spell-book";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Config CONFIG = Config.load();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, player, param) -> {
			var content = message.getContent().getString();
			if (content.startsWith(CONFIG.spellPrefix)) {
				var spell = formatSpell(content);
				LOGGER.info(spell);
				if (spell.contains("god grant me strength")) {
					LOGGER.info("god heard");
				}
				return false;
			}
			return true;
		});
	}

	private String formatSpell(String input) {
		return  input
				.replace(CONFIG.spellPrefix, "")
				.toLowerCase()
				.replaceAll("\\p{Punct}", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}
}