package me.txmc.core.deathmessages;

import java.util.Arrays;
import java.util.Set;
import static org.apache.logging.log4j.LogManager.getLogger;
import java.util.stream.Collectors;

/**
 * Enum representing different causes of death in Minecraft, each associated with a specific
 * message path for customized death messages.
 *
 * <p>This enum is part of the 8b8tDeathMessages plugin, which provides unique and detailed
 * messages when a player dies in various scenarios.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Mapping death causes to specific message paths</li>
 *     <li>Retrieving the message path for a given death cause</li>
 *     <li>Finding death causes by enum name or message path</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 *     DeathCause cause = DeathCause.from(EntityType.ZOMBIE);
 *     String messagePath = cause.getPath();
 * </pre>
 *
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/07/15 8:26 PM
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
    GIANT("zombie-messages"), // Maybe change to giant-messages?
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
    public static final Set<String> PATH_SET = Arrays.stream(DeathCause.values()).map(DeathCause::getPath).collect(Collectors.toSet());

    DeathCause(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public static <E extends Enum<E>> DeathCause from(Enum<E> namedEnum) {
        for (DeathCause deathCause : values()) {
            if (deathCause.name().equalsIgnoreCase(namedEnum.name())) {
                return deathCause;
            }
        }

        getLogger().info("Unknown entity or damage cause '" + namedEnum.name());
        return DeathCause.UNKNOWN;
    }

    public static Set<DeathCause> deathCauseWithPath(DeathCause cause) {
        return Arrays.stream(DeathCause.values()).filter(deathCause ->
                deathCause.getPath().equalsIgnoreCase(cause.getPath())).collect(Collectors.toSet());
    }

    public static DeathCause fromPathSingle(String path) {
        for (DeathCause deathCause : values()) {
            if (deathCause.getPath().equalsIgnoreCase(path)) {
                return deathCause;
            }
        }

        return null;
    }
}