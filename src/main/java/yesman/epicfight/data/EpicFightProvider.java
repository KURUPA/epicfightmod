package yesman.epicfight.data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import pangu_continent.init.ModItems;

public class EpicFightProvider implements DataProvider {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final DataGenerator generator;
    @VisibleForTesting
    public final Map<Item, CapabilitiesBulider> capabilities = new HashMap<>();

    public EpicFightProvider(final DataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void run(HashCache hashCache) throws IOException {
        clear();
        registerCapabilities();
        capabilities.forEach((item, build) -> {
            try {
                DataProvider.save(GSON, hashCache, build.toJson(), getPath(item));
            } catch (IOException e) {
                LOGGER.error("Couldn't save capabilities to {}", getPath(item), e);
            }
        });
    }

    protected void registerCapabilities() {
        getBuilder(Items.DIAMOND_SWORD, WeaponType.SWORD)
                .attribute(WeaponStyle.COMMON)
                .impact(1.1)
                .maxStrikes(1)
                .end();
    };

    protected void clear() {
        capabilities.clear();
    }

    @Override
    public String getName() {
        return "Epic Fight";
    }

    private Path getPath(Item item) {
        ResourceLocation loc = ForgeRegistries.ITEMS.getKey(item);
        return generator.getOutputFolder()
                .resolve("data/" + loc.getNamespace() + "/capabilities/weapons/" + loc.getPath() + ".json");
    }

    public CapabilitiesBulider getBuilder(Item item, WeaponType type) {
        if (capabilities.containsKey(item)) {
            CapabilitiesBulider old = capabilities.get(item);
            return old;
        } else {
            CapabilitiesBulider ret = new CapabilitiesBulider(type);
            capabilities.put(item, ret);
            return ret;
        }
    }

    public class CapabilitiesBulider {
        protected final List<AttributesBuilder> attributes = new ArrayList<>();
        protected final WeaponType type;

        public CapabilitiesBulider(WeaponType type) {
            this.type = type;
        }

        private CapabilitiesBulider self() {
            return (CapabilitiesBulider) this;
        }

        public AttributesBuilder attribute() {
            return attribute(WeaponStyle.COMMON);
        }

        public AttributesBuilder attribute(WeaponStyle style) {
            AttributesBuilder ret = new AttributesBuilder(style);
            attributes.add(ret);
            return ret;
        }

        public JsonElement toJson() {
            JsonObject root = new JsonObject();
            root.add("type", GSON.toJsonTree(type.name));
            JsonObject attributes = new JsonObject();
            this.attributes.forEach(buile -> {
                JsonObject attribute = new JsonObject();
                attribute.addProperty("armor_negation", buile.getArmorNegation());
                attribute.addProperty("impact", buile.getImpact());
                attribute.addProperty("max_strikes", buile.getMaxStrikes());
                attribute.addProperty("damage_bonus", buile.getDamageBonus());
                attribute.addProperty("speed_bonus", buile.getSpeedBonus());
                attributes.add(buile.getStyle().name, GSON.toJsonTree(attribute));
            });
            root.add("attributes", attributes);
            return root;
        }

        

        public class AttributesBuilder {
            private double armorNegation;
            private double impact;
            private int maxStrikes;
            private double damageBonus;
            private double speedBonus;
            private final WeaponStyle style;

            public AttributesBuilder(WeaponStyle style) {
                this.armorNegation = 0.0;
                this.impact = 0.5;
                this.maxStrikes = 1;
                this.damageBonus = 0;
                this.speedBonus = 0;
                this.style = style;
            }

            public AttributesBuilder armorNegation(double armorNegation) {
                this.armorNegation = Math.max(armorNegation, 0.0);
                return this;
            }

            public AttributesBuilder impact(double impact) {
                this.impact = Math.max(impact, 0.0);
                return this;
            }

            public AttributesBuilder maxStrikes(int maxStrikes) {
                this.maxStrikes = Math.max(maxStrikes, 1);
                return this;
            }

            public AttributesBuilder damage_bonus(double damage_bonus) {
                this.damageBonus = damage_bonus;
                return this;
            }

            public AttributesBuilder speed_bonus(double speed_bonus) {
                this.speedBonus = speed_bonus;
                return this;
            }

            public double getArmorNegation() {
                return armorNegation;
            }

            public double getImpact() {
                return impact;
            }

            public int getMaxStrikes() {
                return maxStrikes;
            }

            public double getDamageBonus() {
                return damageBonus;
            }

            public double getSpeedBonus() {
                return speedBonus;
            }

            public WeaponStyle getStyle() {
                return style;
            }

            public CapabilitiesBulider end() {
                return self();
            }
        }
    }

    public enum WeaponStyle {
        COMMON("common"),
        ONE_HAND("one_hand"),
        TWO_HAND("two_hand");

        public final String name;

        private WeaponStyle(String name) {
            this.name = name;
        }
    }

    public enum WeaponType {
        AXE("axe"),
        FIST("fist"),
        HOE("hoe"),
        PICKAXE("pickaxe"),
        SHOVEL("shovel"),
        SWORD("sword"),
        SPEAR("spear"),
        GREATSWORD("greatsword"),
        KATANA("katana"),
        TACHI("tachi"),
        LONGSWORD("longsword"),
        DAGGER("dagger"),
        BOW("bow"),
        CROSSBOW("crossbow"),
        TRIDENT("trident"),
        SHIELD("shield");

        public final String name;

        private WeaponType(String name) {
            this.name = name;
        }
    }

}
