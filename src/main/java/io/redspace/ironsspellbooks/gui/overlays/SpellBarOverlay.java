package io.redspace.ironsspellbooks.gui.overlays;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.config.ClientConfigs;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.player.ClientRenderCache;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.settings.KeyModifier;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static io.redspace.ironsspellbooks.config.ClientConfigs.*;

public class SpellBarOverlay implements IGuiOverlay {
    public static final SpellBarOverlay instance = new SpellBarOverlay();

    public final static ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, "textures/gui/icons.png");
    static final int IMAGE_HEIGHT = 21;
    static final int IMAGE_WIDTH = 21;
    static final int HOTBAR_HALFWIDTH = 91;
    static final int boxSize = 20;
    static int screenHeight;
    static int screenWidth;

    public enum Anchor {
        Hotbar(0, 0),
        TopLeft(0, 0),
        TopRight(0, 1),
        BottomLeft(0, 1),
        BottomRight(1, 1);
        final int m1, m2;

        Anchor(int mx, int my) {
            this.m1 = mx;
            this.m2 = my;
        }
    }

    static final int CONTEXTUAL_FADE_WAIT = 80;
    static int fadeoutDelay;
    static int lastTick;
    static float alpha;
    static SpellSelectionManager lastSelection;

    public void render(ForgeGui gui, GuiGraphics guiHelper, float partialTick, int screenWidth, int screenHeight) {
        Player player = Minecraft.getInstance().player;
        ManaBarOverlay.Display displayMode = ClientConfigs.SPELL_BAR_DISPLAY.get();
        if (displayMode == ManaBarOverlay.Display.Never || player == null) {
            return;
        } else if (displayMode == ManaBarOverlay.Display.Contextual) {
            handleFading(player);
            if (fadeoutDelay <= 0) {
                return;
            }
        } else {
            alpha = 1f;
        }

        var ssm = ClientMagicData.getSpellSelectionManager();
        if (ssm != lastSelection) {
            lastSelection = ssm;
            ClientRenderCache.generateRelativeLocations(ssm, 20, 22);
            if (displayMode == ManaBarOverlay.Display.Contextual) {
                fadeoutDelay = CONTEXTUAL_FADE_WAIT;
            }
        }
        if (ssm.getSpellCount() <= 0) {
            return;
        }
        //System.out.println("SpellBarDisplay: Holding Spellbook");

        int centerX, centerY;
        int configOffsetY = ClientConfigs.SPELL_BAR_Y_OFFSET.get();
        int configOffsetX = ClientConfigs.SPELL_BAR_X_OFFSET.get();
        Anchor anchor = ClientConfigs.SPELL_BAR_ANCHOR.get();
        if (anchor == Anchor.Hotbar) {
            centerX = screenWidth / 2 - Math.max(110, screenWidth / 4);
            centerY = screenHeight - Math.max(55, screenHeight / 8);
        } else {
            centerX = screenWidth * anchor.m1;
            centerY = screenHeight * anchor.m2;
        }
        centerX += configOffsetX;
        centerY += configOffsetY;

        //
        //  Render Spells
        //
        List<SpellData> spells = ssm.getAllSpells().stream().map(slot -> slot.spellData).toList();
        int spellbookCount = ssm.getSpellsForSlot(Curios.SPELLBOOK_SLOT).size();
        var locations = ClientRenderCache.relativeSpellBarSlotLocations;
        int approximateWidth = locations.size() / 3;
        //Move spellbar away from hotbar as it gets bigger
        centerX -= approximateWidth * 5;
        //var spellSelection = ClientMagicData.getSyncedSpellData(player).getSpellSelection();
        int selectedSpellIndex = ssm.getGlobalSelectionIndex();

        //Slot Border
        //setTranslucentTexture(TEXTURE);
        for (Vec2 location : locations) {
            guiHelper.blit(TEXTURE, centerX + (int) location.x, centerY + (int) location.y, 66, 84, 22, 22);

        }


        //Spell Icons
        for (int i = 0; i < locations.size(); i++) {
            guiHelper.blit(spells.get(i).getSpell().getSpellIconResource(), centerX + (int) locations.get(i).x + 3, centerY + (int) locations.get(i).y + 3, 0, 0, 16, 16, 16, 16);
        }

        if(ENABLE_QUICK_CAST_KEYBINDING_LABELS.get()){
            var quickCastKeyMappings = getQuickCastKeyMappings();
            for (int i = 0; i < locations.size(); i++) {

                if(quickCastKeyMappings.containsKey(i)){
                    drawQuickCastKeybinding(
                            centerX + (int) locations.get(i).x,
                            centerY + (int) locations.get(i).y,
                            gui,
                            guiHelper,
                            quickCastKeyMappings.get(i)
                    );
                }
            }
        }

        //Border + Cooldowns
        for (int i = 0; i < locations.size(); i++) {
            setTranslucentTexture(TEXTURE);
            if (i != selectedSpellIndex) {
                guiHelper.blit(TEXTURE, centerX + (int) locations.get(i).x, centerY + (int) locations.get(i).y, 22 + (!ssm.getAllSpells().get(i).slot.equals(Curios.SPELLBOOK_SLOT) ? 110 : 0), 84, 22, 22);
            }
            float f = ClientMagicData.getCooldownPercent(spells.get(i).getSpell());
            if (f > 0) {
                int pixels = (int) (16 * f + 1f);
                guiHelper.blit(TEXTURE, centerX + (int) locations.get(i).x + 3, centerY + (int) locations.get(i).y + 19 - pixels, 47, 87, 16, pixels);
            }
        }
        //Selected Outline
        for (int i = 0; i < locations.size(); i++) {
            //setTranslucentTexture(TEXTURE);
            if (i == selectedSpellIndex) {
                guiHelper.blit(TEXTURE, centerX + (int) locations.get(i).x, centerY + (int) locations.get(i).y, 0, 84, 22, 22);
            }
        }
    }

    private static void handleFading(Player player) {
        if (lastTick != player.tickCount) {
            lastTick = player.tickCount;
            if (ClientMagicData.isCasting() || ClientMagicData.getCooldowns().hasCooldownsActive() || ClientMagicData.getRecasts().hasRecastsActive()) {
                fadeoutDelay = CONTEXTUAL_FADE_WAIT;
            }
            if (fadeoutDelay > 0) {
                fadeoutDelay--;
            }
        }
        alpha = Mth.clamp(fadeoutDelay / 20f, 0, 1);
        if (fadeoutDelay <= 0) {
            return;
        }
    }

    private static void setTranslucentTexture(ResourceLocation texture) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getRendertypeTranslucentShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        RenderSystem.setShaderTexture(0, texture);
    }

    private static void  drawQuickCastKeybinding(int x, int y, ForgeGui gui, GuiGraphics guiGraphics, KeyMapping keyMapping){

        var scale = ClientConfigs.QUICK_CAST_KEYBINDING_LABEL_SCALE.get().floatValue();
        var modifierScale = QUICK_CAST_KEYBINDING_MODIFIER_LABEL_SCALE.get().floatValue();
        int fontHeight = gui.getFont().lineHeight;

        var centerX = x + 11;
        var yPos = y + 13;
        var lineHeight = (int)(14 * scale);

        var color = new Color(223, 223, 223).getRGB();
        var modifierColor = new Color(158, 158, 158).getRGB();
        var unboundColor = new Color(147, 147, 147).getRGB();
        var textBackgroundColor = new Color(4, 4, 4).getRGB();

        guiGraphics.enableScissor(x, y, x + 22, y + 22);

            guiGraphics.pose().pushPose();

                guiGraphics.pose().translate(centerX,yPos,0);

                guiGraphics.pose().scale(scale, scale, 1.0f);

                if(keyMapping.isUnbound()) {

                    var text = Component.translatable("key.irons_spellbooks.quick_cast.unbound");
                    if(DRAW_BACKGROUND_FOR_QUICK_CAST_KEYBINDING_LABELS.get()){
                        int halfWidth = gui.getFont().width(text.getString()) / 2;
                        guiGraphics.fill(-halfWidth - 2, - 2, halfWidth + 1, fontHeight + 1, textBackgroundColor);
                    }

                    guiGraphics.drawCenteredString(
                            gui.getFont(),
                            text,
                            0,
                            0,
                            unboundColor
                    );

                }else{

                    if(keyMapping.getKeyModifier().equals(KeyModifier.NONE)){

                        var text = keyMapping.getTranslatedKeyMessage();
                        if(DRAW_BACKGROUND_FOR_QUICK_CAST_KEYBINDING_LABELS.get()){
                            int halfWidth = gui.getFont().width(text.getString()) / 2;
                            guiGraphics.fill(-halfWidth - 2, - 2, halfWidth + 1, fontHeight + 1, textBackgroundColor);

                        }
                        guiGraphics.drawCenteredString(
                                gui.getFont(),
                                text,
                                0,
                                0,
                                color
                        );
                    }else {

                        guiGraphics.pose().pushPose();
                            guiGraphics.pose().translate(0,-lineHeight,0);
                            guiGraphics.pose().scale(modifierScale, modifierScale, 1.0f);

                            Component text = switch (keyMapping.getKeyModifier()) {
                                case CONTROL -> Component.translatable("key.irons_spellbooks.modifier.control");
                                case SHIFT   -> Component.translatable("key.irons_spellbooks.modifier.shift");
                                case ALT     -> Component.translatable("key.irons_spellbooks.modifier.alt");
                                default      -> Component.literal(keyMapping.getKeyModifier().toString());
                            };
                            if(DRAW_BACKGROUND_FOR_QUICK_CAST_KEYBINDING_LABELS.get()){
                                int halfWidth = gui.getFont().width(text.getString()) / 2;
                                guiGraphics.fill(-halfWidth - 2, - 2, halfWidth + 1, fontHeight + 1, textBackgroundColor);
                            }

                            guiGraphics.drawCenteredString(
                                    gui.getFont(),
                                    text,
                                    0,
                                    0,
                                    modifierColor
                            );

                        guiGraphics.pose().popPose();

                        guiGraphics.drawCenteredString(
                                gui.getFont(),
                                keyMapping.getKey().getDisplayName(),
                                0,
                                0,
                                color
                        );
                    }
                }

            guiGraphics.pose().popPose();

        guiGraphics.disableScissor();

    }

    private static Map<Integer, KeyMapping> getQuickCastKeyMappings(){

        var categoryName = "key.irons_spellbooks.group_2";
        var keyPrefix = "key.irons_spellbooks.spell_quick_cast_";

        return Arrays.stream(Minecraft.getInstance().options.keyMappings)
            .filter(
                    binding ->
                            binding.getCategory().equalsIgnoreCase(categoryName)
                            &&
                            binding.getName().startsWith(keyPrefix)
                            &&
                            isParsableToInt(binding.getName().substring(keyPrefix.length()))
            )
            .collect(
                Collectors.toMap(
                        x->Integer.parseInt(x.getName().substring(keyPrefix.length())) -1,
                        x->x
                )
        );
    }

    private static boolean isParsableToInt(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
