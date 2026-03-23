package me.txmc.core.deathmessages;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2026/03/22
 * This file was created as a part of 8b8tCore
*/

public enum DeathCause {
    UNKNOWN("unknown-messages"),
    CUSTOM("unknown-messages"),
    ENTITY_ATTACK("unknown-messages"),
    CUSTOM_NAMED_ENTITY("custom-name-entity-messages"),
    PROJECTILE("Projectile-Arrow"),
    PLAYER("global-pvp-death-messages"),
    BLOCK("falling-block-messages"),
    VOID("void-messages"),
    FALL("fall-damage-messages"),
    FIRE("fire-messages"),
    FIRE_TICK("fire-tick-messages"),
    SUICIDE("suicide-messages"),
    KILL("suicide-messages"),
    LAVA("lava-messages"),
    DROWNING("drowning-messages"),
    STARVATION("starvation-messages"),
    POISON("potion-messages"),
    MAGIC("potion-messages"),
    WITHER("wither-messages"),
    WITHER_BOSS("witherboss-messages"),
    FALLING_BLOCK("falling-block-messages"),
    THORNS("thorns-messages"),
    SUFFOCATION("suffocation-messages"),
    CONTACT("cactus-messages"),
    BLOCK_EXPLOSION("tnt-messages"),
    ENTITY_EXPLOSION("creeper-messages"),
    LIGHTNING("lightning-messages"),
    DRAGON_BREATH("dragon-breath-messages"),
    FLY_INTO_WALL("elytra-messages"),
    HOT_FLOOR("magma-block-messages"),
    CRAMMING("cramming-messages"),
    FREEZE("freeze-messages"),
    SONIC_BOOM("warden-sonic-boom-messages"),
    ELDER_GUARDIAN("elderguardian-messages"),
    WITHER_SKELETON("witherskeleton-messages"),
    STRAY("stray-messages"),
    ARROW("arrow-messages"),
    FIREBALL("fireball-messages"),
    SMALL_FIREBALL("fireball-messages"),
    WITHER_SKULL("witherboss-messages"),
    PRIMED_TNT("tnt-messages"),
    FIREWORK("firework-messages"),
    HUSK("husk-messages"),
    SPECTRAL_ARROW("arrow-messages"),
    SHULKER_BULLET("shulker-messages"),
    DRAGON_FIREBALL("dragon-messages"),
    ZOMBIE_VILLAGER("zombievillager-messages"),
    EVOKER_FANGS("evoker-messages"),
    EVOKER("evoker-messages"),
    VEX("vex-messages"),
    VINDICATOR("vindicator-messages"),
    ILLUSIONER("illusioner-messages"),
    CREEPER("creeper-messages"),
    SKELETON("skeleton-messages"),
    SPIDER("spider-messages"),
    GIANT("zombie-messages"),
    ZOMBIE("zombie-messages"),
    SLIME("slime-messages"),
    GHAST("ghast-messages"),
    ZOMBIFIED_PIGLIN("zombified-piglin-messages"),
    ENDERMAN("enderman-messages"),
    CAVE_SPIDER("cavespider-messages"),
    SILVERFISH("silverfish-messages"),
    BLAZE("blaze-messages"),
    MAGMA_CUBE("magmacube-messages"),
    ENDER_DRAGON("dragon-messages"),
    WITCH("witch-messages"),
    ENDERMITE("endermite-messages"),
    GUARDIAN("guardian-messages"),
    SHULKER("shulker-messages"),
    WOLF("wolf-messages"),
    IRON_GOLEM("golem-messages"),
    POLAR_BEAR("polar-bear-messages"),
    LLAMA("llama-messages"),
    LLAMA_SPIT("llama-messages"),
    END_CRYSTAL("end-crystal-messages"),
    PHANTOM("phantom-messages"),
    TRIDENT("drowned-messages"),
    PUFFERFISH("pufferfish-messages"),
    DROWNED("drowned-messages"),
    DOLPHIN("dolphin-messages"),
    PANDA("panda-messages"),
    PILLAGER("pillager-messages"),
    RAVAGER("ravager-messages"),
    FOX("fox-messages"),
    BEE("bee-messages"),
    HOGLIN("hoglin-messages"),
    PIGLIN("piglin-messages"),
    PIG_ZOMBIE("pigman-messages"),
    ZOGLIN("zoglin-messages"),
    PIGLIN_BRUTE("piglin-messages"),
    GOAT("goat-messages"),
    WARDEN("warden-messages"),
    MELEE_DEATH("melee-death-messages");

    private final String path;

    private static final Map<String, DeathCause> NAME_MAP = new HashMap<>();
    private static final Set<String> PATH_SET;

    static {
        for (DeathCause cause : values()) {
            NAME_MAP.put(cause.name(), cause);
        }
        PATH_SET = Arrays.stream(values()).map(DeathCause::getPath).collect(Collectors.toSet());
    }

    DeathCause(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public static <E extends Enum<E>> DeathCause from(Enum<E> namedEnum) {
        DeathCause result = NAME_MAP.get(namedEnum.name());
        if (result == null) {
            getLogger().info("Unknown entity or damage cause '" + namedEnum.name() + "'");
            return UNKNOWN;
        }
        return result;
    }

    public static boolean hasPath(String path) {
        return PATH_SET.contains(path);
    }
}
