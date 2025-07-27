package vrpg.spellbook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.sourceforge.pinyin4j.PinyinHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

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
			if (!content.startsWith(CONFIG.prefix)) {
				return true;
			}
			var spell = formatSpell(content);
			if (spell == null) {
				LOGGER.warn("Failed to get formatted spell: {}", content);
				return false;
			}
			LOGGER.info(spell);
			if (!CONFIG.spellInfoMap.containsKey(spell)) {
				LOGGER.info("Failed to recognize spell: {}", spell);
				return false;
			}
			castSpell(player,CONFIG.spellInfoMap.get(spell));
			return false;
		});
	}

	private String formatSpell(String input) {
		var language = "zh";
		if (language.equals("zh")) {
			return toPinYin(input.replace(CONFIG.prefix, ""))
					.replaceAll("\\s+", " ")
					.trim();
		} else if (language.equals("en")) {
			return input
					.replace(CONFIG.prefix, "")
					.toLowerCase()
					.replaceAll("\\p{Punct}", "")
					.replaceAll("\\s+", " ")
					.trim();
		} else {
			LOGGER.warn("Language not supported: {}", language);
			return null;
		}
	}

	private String toPinYin(String chineseInput) {
		StringBuilder pinyinBuilder = new StringBuilder();
		for (int i = 0; i < chineseInput.length(); i++) {
			char ch = chineseInput.charAt(i);
			String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(ch);
			if (pinyins == null || pinyins.length == 0 || pinyins[0].equals("none0")) {
				continue;
			}
			String pinyin = pinyins[0].replaceAll("\\d", "");
			pinyinBuilder.append(pinyin).append(" ");
		}
		return pinyinBuilder.toString().trim();
	}

	private void castSpell(PlayerEntity player, SpellInfo spell) {
		if (Objects.equals(spell.action, "addStatusEffect")) {
			var effect = getStatusEffect(spell.statusEffectType);
			if (effect == null) {
				LOGGER.warn("Failed to get status effect: {}", spell.statusEffectType);
				return;
			}
			player.addStatusEffect(new StatusEffectInstance(
					effect,
					spell.duration * 20,
					spell.statusEffectLevel - 1
			));
		} else {
			LOGGER.warn("Spell action not found: {}", spell.action);
		}
	}

	private RegistryEntry<StatusEffect> getStatusEffect(String type) {
		var id = Identifier.tryParse(type);
		if (id == null) {
			LOGGER.warn("Failed to parse identifier: {}", type);
			return  null;
		}
		var entry = Registries.STATUS_EFFECT.getEntry(id);
		if (entry.isEmpty()) {
			LOGGER.warn("No such status effect: {}", type);
			return null;
		}
		return entry.get();
	}
}