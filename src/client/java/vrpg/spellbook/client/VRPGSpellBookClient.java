package vrpg.spellbook.client;

import me.jaffe2718.mcmti.config.McmtiConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.sourceforge.pinyin4j.PinyinHelper;
import vrpg.spellbook.VRPGSpellBook;

import java.util.Set;

public class VRPGSpellBookClient implements ClientModInitializer {
	private static final Set<String> supportedLanguages = Set.of("zh", "en");

	@Override
	public void onInitializeClient() {
		ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
			if (!message.startsWith(McmtiConfig.prefix)) {
				return true;
			}
            return supportedLanguages.contains(McmtiConfig.language);
        });
		ClientSendMessageEvents.MODIFY_CHAT.register((message) -> {
			if (!message.startsWith(McmtiConfig.prefix)) {
				return message;
			}
			return McmtiConfig.prefix + " " + formatSpell(message);
		});
	}

	private String formatSpell(String message) {
		var language = McmtiConfig.language;
		if (language.equals("zh")) {
			return toPinYin(message);
		} else if (language.equals("en")) {
			return message
					.replace(McmtiConfig.prefix, "")
					.toLowerCase()
					.replaceAll("\\p{Punct}", " ")
					.replaceAll("\\s+", " ")
					.trim();
		}
		VRPGSpellBook.LOGGER.error("Unexpected language: {}", language);
		throw new IllegalStateException("Unexpected language: " + language);
	}

	private String toPinYin(String chinese) {
		StringBuilder pinyinBuilder = new StringBuilder();
		for (int i = 0; i < chinese.length(); i++) {
			char ch = chinese.charAt(i);
			String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(ch);
			if (pinyins == null || pinyins.length == 0 || pinyins[0].equals("none0")) {
				continue;
			}
			String pinyin = pinyins[0].replaceAll("\\d", "");
			pinyinBuilder.append(pinyin).append(" ");
		}
		//todo 多音字，比如说重新的重，如果默认取0，永远只能拿到zhong
		return pinyinBuilder.toString().trim();
	}
}