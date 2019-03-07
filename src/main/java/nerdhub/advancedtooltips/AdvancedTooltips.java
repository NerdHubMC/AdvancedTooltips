package nerdhub.advancedtooltips;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.*;
import net.minecraft.nbt.Tag;
import net.minecraft.text.StringTextComponent;
import net.minecraft.text.TextComponent;
import net.minecraft.text.TextFormat;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import nerdhub.textilelib.event.client.render.tooltip.TooltipCreationCallback;

/**
 * Advanced Tooltips
 *
 * @author BrockWS
 */
public class AdvancedTooltips implements ModInitializer {

    public static final String MOD_ID = "advancedtooltips";
    public static final String MOD_NAME = "Advanced Tooltips";

    @Override
    public void onInitialize() {
        System.out.println("Initializing Advanced Tooltips!");
        TooltipCreationCallback.EVENT.register(AdvancedTooltips::onTooltipEvent);
    }

    private static void onTooltipEvent(ItemStack stack, TooltipContext context, List<TextComponent> tooltips) {
        if (stack.isEmpty())
            return;
        boolean advanced = MinecraftClient.getInstance().options.advancedItemTooltips;
        boolean shift = Screen.isShiftPressed(); // Show NBT as formatted, requires ctrl
        boolean ctrl = Screen.isControlPressed(); // Show NBT
        boolean alt = Screen.isAltPressed(); // Show Item Based debug information
        Item item = stack.getItem();
        Identifier id = Registry.ITEM.getId(item);
        StringBuilder format = new StringBuilder();

        if (advanced && alt) {
            if (item instanceof FoodItem) {
                FoodItem foodItem = (FoodItem) item;
                AdvancedTooltips.resetFormat(format, TextFormat.GOLD);

                format.append("Hunger: ");
                format.append(foodItem.getHungerRestored(stack));
                AdvancedTooltips.appendAndClear(format, tooltips, TextFormat.GOLD);

                format.append("Saturation: ");
                format.append(Math.round(foodItem.getSaturationModifier(stack) * 20)); // TODO: Add option
                AdvancedTooltips.appendAndClear(format, tooltips);
            }
            if (item instanceof ToolItem) {
                ToolItem toolItem = (ToolItem) item;
                ToolMaterial toolMaterial = toolItem.getType();
                AdvancedTooltips.resetFormat(format, TextFormat.GOLD);

                format.append("Enchantability: ");
                format.append(toolItem.getEnchantability());
                AdvancedTooltips.appendAndClear(format, tooltips, TextFormat.GOLD);

                format.append("Mining Level: ");
                format.append(toolMaterial.getMiningLevel());
                AdvancedTooltips.appendAndClear(format, tooltips, TextFormat.GOLD);

                format.append("Block Breaking Speed: ");
                format.append(toolMaterial.getBlockBreakingSpeed());
                AdvancedTooltips.appendAndClear(format, tooltips);
            }
            if (item instanceof ArmorItem) {
                ArmorItem armorItem = (ArmorItem) item;
                ArmorMaterial armorMaterial = armorItem.getMaterial();
                AdvancedTooltips.resetFormat(format, TextFormat.GOLD);

                format.append("Enchantability: ");
                format.append(armorMaterial.getEnchantability());
                AdvancedTooltips.appendAndClear(format, tooltips);
            }
        }

        tooltips.sort((a, b) -> {
            if (a.getString().toLowerCase().contains(id.toString().toLowerCase()))
                return 1;
            if (b.getString().toLowerCase().contains(id.toString().toLowerCase()))
                return -1;
            return 0;
        });

        AdvancedTooltips.resetFormat(format, TextFormat.BLUE);

        if (id.getNamespace().equalsIgnoreCase("minecraft")) // Since Minecraft isn't a mod
            AdvancedTooltips.appendAndClear(format.append("Minecraft"), tooltips);

        FabricLoader.getInstance().getAllMods()
                .stream()
                .filter(mod ->
                        mod.getMetadata().getId().equalsIgnoreCase(id.getNamespace()))
                .findAny()
                .ifPresent(mod ->
                        AdvancedTooltips.appendAndClear(format.append(mod.getMetadata().getName()), tooltips)
                );
        if (advanced && ctrl && stack.hasTag()) {
            tooltips.removeIf(textComponent -> textComponent.getString().contains("NBT: "));
            AdvancedTooltips.appendColouredNBT(format, tooltips, stack.getTag(), shift);
            if (!shift)
                AdvancedTooltips.appendAndClear(format.append("Press Shift to format NBT"), tooltips);
        }
        if (advanced && !ctrl && stack.hasTag()) {
            AdvancedTooltips.resetFormat(format, TextFormat.DARK_GRAY);
            AdvancedTooltips.appendAndClear(format.append("Press Control for NBT"), tooltips);
        }
    }

    private static void appendColouredNBT(StringBuilder builder, List<TextComponent> tooltips, Tag tag, boolean formatted) {
        if (tag == null) {
            AdvancedTooltips.appendAndClear(builder.append("null"), tooltips);
            return;
        }
        if (formatted) {
            TextComponent tc = tag.toTextComponent(" ", 0);
            String f = tc.getFormattedText();
            String[] split = f.split("\n");
            for (String s : split) {
                tooltips.add(new StringTextComponent(s));
            }
        } else {
            TextComponent tc = tag.toTextComponent();
            String f = tc.getFormattedText();
            int max = 100;
            if (f.length() > max) {
                int lines = (int) Math.ceil((float) f.length() / max);
                for (int i = 0; i < lines; i++)
                    tooltips.add(new StringTextComponent(f.substring(i * max, Math.min((i * max) + max, f.length()))));
            } else {
                tooltips.add(tag.toTextComponent());
            }
        }
    }

    private static void appendAndClear(StringBuilder builder, List<TextComponent> tooltips) {
        AdvancedTooltips.appendAndClear(builder, tooltips, TextFormat.DARK_GRAY);
    }

    private static void appendAndClear(StringBuilder builder, List<TextComponent> tooltips, TextFormat format) {
        if (builder.length() < 1)
            return;
        tooltips.add(new StringTextComponent(builder.toString()));
        AdvancedTooltips.resetFormat(builder, format);
    }

    private static void append(StringBuilder builder, List<TextComponent> tooltips) {
        if (builder.length() < 1)
            return;
        tooltips.add(new StringTextComponent(builder.toString()));
    }

    private static void resetFormat(StringBuilder builder) {
        AdvancedTooltips.resetFormat(builder, TextFormat.DARK_GRAY);
    }

    private static void resetFormat(StringBuilder builder, TextFormat format) {
        builder.setLength(0);
        builder.append(format);
    }
}
