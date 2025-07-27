package vrpg.spellbook.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

public class VRPGSpellBookClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
			return true;
		});
	}
}