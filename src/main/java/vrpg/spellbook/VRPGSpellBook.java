package vrpg.spellbook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VRPGSpellBook implements ModInitializer {
	public static final String MOD_ID = "vrpg-spell-book";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	static final Config CONFIG = Config.load();

	@Override
	public void onInitialize() {
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, player, param) -> {
			var content = message.getContent().getString();
			if (!content.startsWith(CONFIG.prefix)) {
				return true;
			}
			var spell = content.replace(CONFIG.prefix + " ", "");
			LOGGER.info(spell);
			//todo Levenshtein distance, allow some wrong character in result
			if (!CONFIG.spellInfoMap.containsKey(spell)) {
				LOGGER.info("Failed to recognize spell: {}", spell);
				return false;
			}
			castSpell(player,CONFIG.spellInfoMap.get(spell));
			//todo add some sounds when casting the spell
			return false;
		});
	}

	private void castSpell(ServerPlayerEntity player, SpellInfo spell) {
		if (spell.action.equals("addStatusEffect")) {
			var effect = getRegistryEntry(player, RegistryKeys.STATUS_EFFECT, spell.statusEffectName);
			if (effect == null) {
				LOGGER.warn("Failed to get status effect: {}", spell.statusEffectName);
				return;
			}
			player.addStatusEffect(new StatusEffectInstance(
					effect,
					spell.duration * 20,
					spell.statusEffectLevel - 1
			));
		} else if (spell.action.equals("addEnchantment")) {
			var mainHand = player.getMainHandStack();
			var enchantment = getRegistryEntry(player, RegistryKeys.ENCHANTMENT, spell.enchantmentName);
			mainHand.addEnchantment(enchantment, spell.enchantmentLevel);
		} else {
			LOGGER.warn("Spell action not found: {}", spell.action);
		}
	}

	private <T> RegistryEntry<T> getRegistryEntry(ServerPlayerEntity player, RegistryKey<Registry<T>> keyType, String name) {
		Identifier id = Identifier.tryParse(name);
		if (id == null) {
			LOGGER.warn("Failed to parse identifier: {}", name);
			return null;
		}
		var key = RegistryKey.of(keyType, id);
		var result = player.getRegistryManager().getOptionalEntry(key);
		if (result.isEmpty()){
			LOGGER.warn("Failed to get registry entry: {}", key);
			return null;
		}
		return result.get();
	}
}