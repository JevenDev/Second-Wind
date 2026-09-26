package com.jvn.secondwind.event;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.common.PlayerRepair;
import com.jvn.secondwind.state.ReviveReason;
import com.jvn.secondwind.state.SecondWindService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = SecondWindMod.MOD_ID)
public final class SecondWindCommands {
    private static final int ADMIN_PERMISSION_LEVEL = 2;

    private SecondWindCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("secondwind")
                .requires(source -> source.hasPermission(ADMIN_PERMISSION_LEVEL))
                .then(buildReviveCommand("revive"))
                .then(buildDownCommand("down"))
                .then(Commands.literal("repair")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> repairPlayer(context.getSource(), EntityArgument.getPlayer(context, "player"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildReviveCommand(String name) {
        return Commands.literal(name)
                .executes(context -> reviveAll(context.getSource()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> revivePlayer(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "player"))));
    }

                private static LiteralArgumentBuilder<CommandSourceStack> buildDownCommand(String name) {
                return Commands.literal(name)
                    .executes(context -> downAll(context.getSource()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> downPlayer(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "player"))));
                }

    private static int reviveAll(CommandSourceStack source) {
        List<ServerPlayer> downedPlayers = source.getServer().getPlayerList().getPlayers().stream()
                .filter(SecondWindService::isDowned)
                .toList();
        if (downedPlayers.isEmpty()) {
            source.sendFailure(Component.translatable("commands.secondwind.revive.all.none"));
            return 0;
        }

        int revived = 0;
        for (ServerPlayer player : downedPlayers) {
            if (SecondWindService.tryRevive(player, ReviveReason.ADMIN)) {
                revived++;
            } else {
                source.sendFailure(Component.translatable("commands.secondwind.revive.single.failed", player.getDisplayName()));
            }
        }
        int revivedCount = revived;
        if (revivedCount > 0) {
            source.sendSuccess(() -> Component.translatable("commands.secondwind.revive.all.success", revivedCount), true);
        }
        return revivedCount;
    }

    private static int revivePlayer(CommandSourceStack source, ServerPlayer player) {
        if (!SecondWindService.isDowned(player)) {
            source.sendFailure(Component.translatable("commands.secondwind.revive.single.not_downed", player.getDisplayName()));
            return 0;
        }

        if (!SecondWindService.tryRevive(player, ReviveReason.ADMIN)) {
            source.sendFailure(Component.translatable("commands.secondwind.revive.single.failed", player.getDisplayName()));
            return 0;
        }
        source.sendSuccess(
                () -> Component.translatable("commands.secondwind.revive.single.success", player.getDisplayName()),
                true);
        return 1;
    }

    private static int repairPlayer(CommandSourceStack source, ServerPlayer player) {
        PlayerRepair.Result result = SecondWindService.repair(player);
        if (result != PlayerRepair.Result.SUCCESS) {
            String message = switch (result) {
                case NEEDS_RESPAWN -> "commands.secondwind.repair.needs_respawn";
                case INVALID_MAX_HEALTH -> "commands.secondwind.repair.invalid_max_health";
                case INVALID_MAX_ABSORPTION -> "commands.secondwind.repair.invalid_max_absorption";
                case RECOVERY_FAILED -> "commands.secondwind.repair.failed";
                default -> throw new IllegalStateException("Unexpected repair result: " + result);
            };
            source.sendFailure(Component.translatable(message, player.getDisplayName()));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.secondwind.repair.success", player.getDisplayName()), true);
        return 1;
    }

    private static int downAll(CommandSourceStack source) {
        List<ServerPlayer> availablePlayers = source.getServer().getPlayerList().getPlayers().stream()
                .filter(player -> player.isAlive() && SecondWindService.canEnterDownedState(player))
                .toList();
        if (availablePlayers.isEmpty()) {
            source.sendFailure(Component.translatable("commands.secondwind.down.all.none"));
            return 0;
        }

        int downed = 0;
        for (ServerPlayer player : availablePlayers) {
            if (SecondWindService.down(player, player.damageSources().generic())) {
                downed++;
            } else {
                source.sendFailure(Component.translatable("commands.secondwind.down.single.failed", player.getDisplayName()));
            }
        }
        int downedCount = downed;
        if (downedCount > 0) {
            source.sendSuccess(() -> Component.translatable("commands.secondwind.down.all.success", downedCount), true);
        }
        return downedCount;
    }

    private static int downPlayer(CommandSourceStack source, ServerPlayer player) {
        if (SecondWindService.isDowned(player)) {
            source.sendFailure(Component.translatable("commands.secondwind.down.single.already_downed", player.getDisplayName()));
            return 0;
        }

        if (player.isCreative() || player.isSpectator()) {
            source.sendFailure(Component.translatable("commands.secondwind.down.single.invalid_target", player.getDisplayName()));
            return 0;
        }

        if (!player.isAlive() || !SecondWindService.down(player, player.damageSources().generic())) {
            source.sendFailure(Component.translatable("commands.secondwind.down.single.failed", player.getDisplayName()));
            return 0;
        }
        source.sendSuccess(
                () -> Component.translatable("commands.secondwind.down.single.success", player.getDisplayName()),
                true);
        return 1;
    }
}