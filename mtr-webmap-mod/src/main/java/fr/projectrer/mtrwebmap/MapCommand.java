package fr.projectrer.mtrwebmap;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.*;

/**
 * Commande /map : envoie un lien cliquable vers la carte web dans le chat.
 * Commande /mtrexport : déclenche un export immédiat (op level 2+).
 */
public class MapCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // /map — accessible à tous les joueurs
        dispatcher.register(
            Commands.literal("map")
                .executes(MapCommand::executeMap)
        );

        // /mtrexport — réservé aux opérateurs (level 2)
        dispatcher.register(
            Commands.literal("mtrexport")
                .requires(src -> src.hasPermission(2))
                .executes(MapCommand::executeExport)
        );
    }

    private static int executeMap(CommandContext<CommandSourceStack> ctx) {
        String url = WebMapConfig.SERVER.siteUrl.get();
        String serverName = WebMapConfig.SERVER.serverName.get();

        // Build clickable message
        MutableComponent header = Component.literal("━━━ ")
            .withStyle(ChatFormatting.DARK_GRAY);
        MutableComponent title = Component.literal("🗺  Carte du réseau " + serverName)
            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        MutableComponent sep = Component.literal(" ━━━")
            .withStyle(ChatFormatting.DARK_GRAY);

        MutableComponent link = Component.literal("▶  " + url)
            .withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Clique pour ouvrir la carte dans ton navigateur")
                ))
            );

        MutableComponent footer = Component.literal("Calculateur d'itinéraire · Départs en temps réel")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);

        ctx.getSource().sendSuccess(header.append(title).append(sep), false);
        ctx.getSource().sendSuccess(link, false);
        ctx.getSource().sendSuccess(footer, false);

        return 1;
    }

    private static int executeExport(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(
            Component.literal("[MtrWebMap] Export manuel déclenché...").withStyle(ChatFormatting.YELLOW),
            false
        );

        ServerEvents.triggerManualExport();

        ctx.getSource().sendSuccess(
            Component.literal("[MtrWebMap] Les données seront poussées vers GitHub dans quelques secondes.")
                .withStyle(ChatFormatting.GREEN),
            false
        );

        return 1;
    }
}
