/*
 * Copyright (C) 2019  BrockWS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pw.brock.advancedtooltips;

import nerdhub.textilelib.eventhandlers.EventRegistry;
import nerdhub.textilelib.eventhandlers.EventSubscriber;
import nerdhub.textilelib.events.render.TooltipBuildEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.FabricLoader;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.*;
import net.minecraft.nbt.Tag;
import net.minecraft.text.StringTextComponent;
import net.minecraft.text.TextComponent;
import net.minecraft.text.TextFormat;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class AdvancedTooltips implements ModInitializer {

    public static final String MOD_ID = "advancedtooltips";
    public static final String MOD_NAME = "Advanced Tooltips";

    @Override
    public void onInitialize() {
        System.out.println("G'day from Advanced Tooltips!");
        EventRegistry.INSTANCE.registerEventHandler(this);
    }

    @EventSubscriber
    public static void onTooltipEvent(TooltipBuildEvent event) {
        if (event.getStack().isEmpty())
            return;
        boolean advanced = MinecraftClient.getInstance().options.advancedItemTooltips;
        boolean shift = Gui.isShiftPressed(); // Show NBT as formatted, requires ctrl
        boolean ctrl = Gui.isControlPressed(); // Show NBT
        boolean alt = Gui.isAltPressed(); // Show Item Based debug information
        List<TextComponent> tooltips = event.getList();
        ItemStack stack = event.getStack();
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

        FabricLoader.INSTANCE.getModContainers()
                .stream()
                .filter(mod ->
                        mod.getInfo().getId().equalsIgnoreCase(id.getNamespace()))
                .findAny()
                .ifPresent(mod ->
                        AdvancedTooltips.appendAndClear(format.append(mod.getInfo().getName()), tooltips)
                );
        if (advanced && ctrl && stack.hasTag()) {
            tooltips.removeIf(textComponent -> textComponent.getString().contains("NBT: "));
            AdvancedTooltips.appendColouredNBT(format, tooltips, stack.getTag(), shift);
        }
    }

    public static void appendColouredNBT(StringBuilder builder, List<TextComponent> tooltips, Tag tag, boolean formatted) {
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
