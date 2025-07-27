package vrpg.spellbook;

import net.fabricmc.api.ClientModInitializer;

public class VRPGSpellBookClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		//todo add mcmti mod as dependency, get language code from client and send to server
	}
}