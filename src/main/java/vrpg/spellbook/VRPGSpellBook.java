package vrpg.spellbook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;

public class VRPGSpellBook implements ModInitializer {
	public static final String MOD_ID = "vrpg-spell-book";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	static final Config CONFIG = Config.load();

	private final Map<String, BiConsumer<ServerPlayerEntity, SpellInfo>> spellActions = new HashMap<>();

	@Override
	public void onInitialize() {
		initSpellActions();
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, player, param) -> {
			var content = message.getContent().getString();
			if (!content.startsWith(CONFIG.prefix)) {
				return true;
			}
			var spell = content.replace(CONFIG.prefix + " ", "");
			castSpell(player, spell);
			return false;
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			DelayTask.tick();
		});
	}

	private void initSpellActions() {
		spellActions.put("addStatusEffect", this::addStatusEffect);
		spellActions.put("addEnchantment", this::addEnchantment);
		spellActions.put("summonLightning", this::summonLightning);
		spellActions.put("shootFireball", this::shootFireball);
	}

	private void castSpell(ServerPlayerEntity player, String spell) {
		LOGGER.info(spell);
		//todo Levenshtein distance, allow some wrong character in result
		if (!CONFIG.spellMap.containsKey(spell)) {
			LOGGER.info("Failed to recognize spell: {}", spell);
			return;
		}
		var spellInfo = CONFIG.spellMap.get(spell);
		var actionKey = spellInfo.action;
		if (!spellActions.containsKey(actionKey)) {
			LOGGER.warn("Spell action not found: {}", actionKey);
			return;
		}
		var action = spellActions.get(actionKey);
		action.accept(player, spellInfo);
		//todo add some sounds when casting the spell
	}

	private void addStatusEffect(ServerPlayerEntity player, SpellInfo spell) {
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
	}

	private void addEnchantment(ServerPlayerEntity player, SpellInfo spell) {
		var mainHand = player.getMainHandStack();
		if (mainHand.isEmpty()) {
			LOGGER.info("Player's main hand is empty");
			return;
		}
		var enchantment = getRegistryEntry(player, RegistryKeys.ENCHANTMENT, spell.enchantmentName);
		if (enchantment == null) {
			LOGGER.warn("Failed to get enchantment: {}", spell.enchantmentName);
			return;
		}
		var acceptable = mainHand.canBeEnchantedWith(enchantment, EnchantingContext.ACCEPTABLE);
		if (!acceptable) {
			LOGGER.info("Main hand item is not acceptable for this enchantment: {} {}", mainHand, enchantment);
			return;
		}
		mainHand.addEnchantment(enchantment, spell.enchantmentLevel);
		var uuid = addUUID(mainHand);
		DelayTask.add(() -> {
			var item = findPlayersItemWithUUID(player, uuid);
			if (item.isEmpty()) {
				//todo if it's not in player's inventory now
				LOGGER.warn("Enchantment item lost");
				return;
			}
			removeUUID(item, uuid);
			EnchantmentHelper.apply(item, builder -> builder.remove(entry -> entry.matchesKey(enchantment.getKey().get())));
		}, spell.duration);
	}

	private void summonLightning(ServerPlayerEntity player, SpellInfo spell) {
		for (int i = 0; i < spell.repeatCount; i++) {
			DelayTask.add(() -> {
				ServerWorld world = player.getServerWorld();
				var range = spell.maxDistance;
				var center = player.getPos();
				Box box = new Box(
						center.x - range, center.y - range, center.z - range,
						center.x + range, center.y + range, center.z + range
				);
				var entities = world.getEntitiesByType((
								TypeFilter.instanceOf(LivingEntity.class)),
						box,
						entity -> entity.isAlive() && !(entity instanceof PlayerEntity));
				entities.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(center)));
				var limited = entities.subList(0, Math.min(spell.maxTarget, entities.size()));
				for (var e : limited) {
					var lightning = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.TRIGGERED);
					lightning.refreshPositionAfterTeleport(e.getPos());
					world.spawnEntity(lightning);
				}
			}, i * spell.repeatDelay);
		}
	}

	private void shootFireball(ServerPlayerEntity player, SpellInfo spell) {
		for (int i = 0; i < spell.repeatCount; i++) {
			DelayTask.add(() -> {
				ServerWorld world = player.getServerWorld();
				Vec3d direction = player.getRotationVec(1.0F).normalize();
				Vec3d velocity = direction.multiply(spell.flyingSpeed);
				var fireball = spell.fireballType.equals("big")  ?
						new FireballEntity(world, player, velocity, spell.fireballExplosionPower):
						new SmallFireballEntity(world, player, velocity);
				fireball.setPosition(player.getEyePos());
				world.spawnEntity(fireball);
			}, i * spell.repeatDelay);
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

	private String addUUID(ItemStack item) {
		var customData = item.get(DataComponentTypes.CUSTOM_DATA);
		var nbt = customData != null ? customData.copyNbt() : new NbtCompound();
		String uuid = UUID.randomUUID().toString();
		nbt.putString(uuid, "");
		item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
		return uuid;
	}

	private ItemStack findPlayersItemWithUUID(ServerPlayerEntity player, String uuid) {
		var inventory = player.getInventory();
		List<ItemStack> allItems = new ArrayList<>();
		allItems.addAll(inventory.main);
		allItems.addAll(inventory.armor);
		allItems.addAll(inventory.offHand);
		for (ItemStack item : allItems) {
			if (item.isEmpty()) {
				continue;
			}
			var customData = item.get(DataComponentTypes.CUSTOM_DATA);
			if (customData == null) {
				continue;
			}
			if (!customData.copyNbt().contains(uuid)) {
				continue;
			}
			return item;
		}
		return ItemStack.EMPTY;
	}

	private void removeUUID(ItemStack item, String uuid) {
		if (item.isEmpty()) return;

		var customData = item.get(DataComponentTypes.CUSTOM_DATA);
		if (customData == null) return;

		NbtCompound nbt = customData.copyNbt();

		if (nbt.contains(uuid)) {
			nbt.remove(uuid);
			if (nbt.isEmpty()) {
				item.remove(DataComponentTypes.CUSTOM_DATA);
			} else {
				item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
			}
		}
	}

}