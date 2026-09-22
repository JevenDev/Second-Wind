package com.jvn.secondwind.network;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.state.FailureReason;
import com.jvn.secondwind.state.SecondWindPlayerState;
import com.jvn.secondwind.state.SecondWindService;
import com.jvn.secondwind.state.SecondWindEntityService;
import com.jvn.secondwind.state.SecondWindEntityState;
import com.jvn.secondwind.api.ResolvedEntityPolicy;
import com.jvn.secondwind.config.SecondWindConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import com.jvn.toucanlib.neoforge.network.ToucanNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

public final class SecondWindNetworking {
    private static final String NETWORK_VERSION = "7";

    private SecondWindNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        ToucanNetwork network = ToucanNetwork.create(SecondWindMod.MOD_ID, NETWORK_VERSION, event);
        network.playToServer(ServerboundGiveUpPayload.TYPE, ServerboundGiveUpPayload.STREAM_CODEC, SecondWindNetworking::handleGiveUp);
        network.playToServer(ServerboundReviveHoldPayload.TYPE, ServerboundReviveHoldPayload.STREAM_CODEC, SecondWindNetworking::handleReviveHold);
        network.safePlayToClient(
                ClientboundReviveProgressPayload.TYPE,
                ClientboundReviveProgressPayload.STREAM_CODEC,
                "com.jvn.secondwind.client.SecondWindClient",
                "applyReviveProgress");
        network.safePlayToClient(
                ClientboundSecondWindStatePayload.TYPE,
                ClientboundSecondWindStatePayload.STREAM_CODEC,
                "com.jvn.secondwind.client.ClientSecondWindState",
                "apply");
        network.safePlayToClient(
                ClientboundTrackedDownedPlayerPayload.TYPE,
                ClientboundTrackedDownedPlayerPayload.STREAM_CODEC,
                "com.jvn.secondwind.client.ClientTrackedDownedPlayers",
                "apply");
    }

    public static void syncToPlayer(ServerPlayer player) {
        syncToPlayer(player, false, 0);
    }

    public static void syncToPlayer(ServerPlayer player, boolean showReviveFlash) {
        syncToPlayer(player, showReviveFlash, 0);
    }

    public static void syncToPlayer(ServerPlayer player, boolean showReviveFlash, int damageTicksLost) {
        SecondWindPlayerState state = SecondWindService.getState(player);
        int cooldownSeconds = SecondWindService.getCooldownRemainingSeconds(player);
        safeSendToPlayer(player, new ClientboundSecondWindStatePayload(
                state.isDowned(),
                state.getDownedTicksRemaining(),
                state.getDownedMaxTicks(),
                Math.max(0, damageTicksLost),
                state.isDowned(),
                SecondWindConfig.FORCE_CRAWLING_POSE.get(),
                state.getReviveChannelProgress(),
                SecondWindService.isBeingRevived(player),
                cooldownSeconds,
                showReviveFlash,
                currentReviverName(player, state)));

        syncTrackedDownedState(player, state);
    }

    private static String currentReviverName(ServerPlayer player, SecondWindPlayerState state) {
        return state.getReviveChannelReviver()
                .map(player.server.getPlayerList()::getPlayer)
                .map(ServerPlayer::getName)
                .map(component -> component.getString())
                .orElse("");
    }

    private static void syncTrackedDownedState(ServerPlayer player, SecondWindPlayerState state) {
        ClientboundTrackedDownedPlayerPayload payload = trackedPlayerPayload(player, state);
        safeSendToPlayer(player, payload);
        for (ServerPlayer other : player.serverLevel().getChunkSource().chunkMap.getPlayersWatching(player)) {
            safeSendToPlayer(other, payload);
        }
    }

    private static ClientboundTrackedDownedPlayerPayload trackedPlayerPayload(ServerPlayer player, SecondWindPlayerState state) {
        return new ClientboundTrackedDownedPlayerPayload(
                player.getId(),
                state.isDowned(),
                true,
                state.getDownedTicksRemaining(),
                state.getDownedMaxTicks(),
                SecondWindService.isBeingRevived(player),
                SecondWindConfig.MULTIPLAYER_REVIVE.get(),
                (int) Math.ceil(SecondWindConfig.REVIVE_CHANNEL_SECONDS.get() * 20.0D),
                SecondWindConfig.REVIVE_DISTANCE.get(),
                ResourceLocation.fromNamespaceAndPath(SecondWindMod.MOD_ID, "crawl"));
    }

    public static void syncTrackedEntity(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            syncToPlayer(player);
            return;
        }
        ClientboundTrackedDownedPlayerPayload payload = trackedPayload(entity);
        if (entity.level().getChunkSource() instanceof net.minecraft.server.level.ServerChunkCache chunkCache) {
            for (ServerPlayer player : chunkCache.chunkMap.getPlayersWatching(entity)) {
                safeSendToPlayer(player, payload);
            }
        }
    }

    public static void sendTrackedEntity(ServerPlayer player, LivingEntity entity) {
        safeSendToPlayer(player, trackedPayload(entity));
    }

    private static ClientboundTrackedDownedPlayerPayload trackedPayload(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return trackedPlayerPayload(player, SecondWindService.getState(player));
        }
        SecondWindEntityState state = SecondWindEntityService.getState(entity);
        ResolvedEntityPolicy policy = state.policy();
        return new ClientboundTrackedDownedPlayerPayload(
                entity.getId(), state.isDowned(), policy != null && policy.showTimer(), state.ticksRemaining(), state.maxTicks(),
                state.reviveChannelReviver().isPresent(), policy != null && policy.reviveEnabled(), policy == null ? 0 : policy.reviveChannelTicks(),
                policy == null ? 0.0D : policy.reviveDistance(),
                policy == null ? ResourceLocation.fromNamespaceAndPath(SecondWindMod.MOD_ID, "sideways") : policy.pose());
    }

    private static void safeSendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (NetworkRegistry.hasChannel(player.connection, payload.type().id())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static void sendGiveUpRequest() {
        PacketDistributor.sendToServer(ServerboundGiveUpPayload.INSTANCE);
    }

    public static void sendReviveReleaseRequest() {
        sendReviveHoldRequest(ServerboundReviveHoldPayload.RELEASE_TARGET_ID);
    }

    public static void sendReviveHoldRequest(int targetEntityId) {
        PacketDistributor.sendToServer(new ServerboundReviveHoldPayload(targetEntityId));
    }

    private static void handleGiveUp(ServerboundGiveUpPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        ToucanNetwork.withServerPlayer(context, player -> {
            if (SecondWindService.isDowned(player)) {
                SecondWindService.failAndKill(player, FailureReason.GIVE_UP);
            }
        });
    }

    private static void sendReviveProgress(ServerPlayer reviver, LivingEntity target, boolean accepted) {
        int completed = 0;
        int required = 0;
        if (accepted && target instanceof ServerPlayer player) {
            SecondWindPlayerState state = SecondWindService.getState(player);
            completed = state.getReviveChannelTicks();
            required = state.getReviveChannelRequiredTicks();
        } else if (accepted) {
            SecondWindEntityState state = SecondWindEntityService.getState(target);
            completed = state.reviveChannelTicks();
            required = state.policy() == null ? 0 : state.policy().reviveChannelTicks();
        }
        safeSendToPlayer(reviver, new ClientboundReviveProgressPayload(target.getId(), completed, required));
    }

    private static void handleReviveHold(ServerboundReviveHoldPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        ToucanNetwork.withServerPlayer(context, reviver -> {
            if (payload.targetEntityId() == ServerboundReviveHoldPayload.RELEASE_TARGET_ID) {
                SecondWindService.releaseReviveChannelsFor(reviver);
                SecondWindEntityService.interruptReviveChannelsFor(reviver);
            } else if (reviver.serverLevel().getEntity(payload.targetEntityId()) instanceof LivingEntity target) {
                boolean accepted = SecondWindEntityService.refreshReviveChannel(reviver, target);
                sendReviveProgress(reviver, target, accepted);
            }
        });
    }
}
