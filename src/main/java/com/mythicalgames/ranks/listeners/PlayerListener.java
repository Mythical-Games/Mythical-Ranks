package com.mythicalgames.ranks.listeners;

import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.player.PlayerChatEvent;
import org.allaymc.api.eventbus.event.player.PlayerJoinEvent;
import org.allaymc.api.permission.PermissionGroup;

import com.mythicalgames.ranks.Config;
import com.mythicalgames.ranks.RankHelper;
import com.mythicalgames.ranks.RankSystem;

public class PlayerListener {

    @EventHandler
public void onPlayerChat(PlayerChatEvent ev) {
    EntityPlayer player = ev.getPlayer();
    RankSystem rankSystem = RankSystem.getInstance();
    var config = rankSystem.config;

    RankHelper.getOrCreatePermissible(player, config)
        .thenAccept(permissible -> {
            PermissionGroup group = permissible.getPermissionGroup();
            var groupData = config.groups.get(group.getName().toLowerCase());

            //fallback def format
            String chatFormat = "{prefix}{player}: {message}";
            if (groupData != null && groupData.chatFormat != null) {
                chatFormat = groupData.chatFormat;
            }

            chatFormat = chatFormat
                    .replace("{prefix}", groupData != null ? groupData.prefix : "")
                    .replace("{player}", player.getOriginName())
                    .replace("{message}", ev.getMessage())
                    .replace("&", "§");

            ev.setHeader(chatFormat);
            ev.setMessage("§r");
        })
        .exceptionally(ex -> {
            rankSystem.getPluginLogger().error("Failed to format chat for {}", player.getOriginName(), ex);
            return null;
        });
}


@EventHandler
public void onPlayerJoin(PlayerJoinEvent ev) {
    EntityPlayer player = ev.getPlayer();
    RankSystem rankSystem = RankSystem.getInstance();
    Config config = rankSystem.config;

    RankHelper.getOrCreatePermissible(player, config).thenAccept(permissible -> {
        PermissionGroup group = permissible.getPermissionGroup();
        Config.GroupData groupData = config.groups.get(group.getName().toLowerCase());
        String nametag = (groupData != null && groupData.nametag != null) ? groupData.nametag : "§f{player}";
        player.setNameTag(nametag);

        if (rankSystem.config.debug) rankSystem.getPluginLogger().info("{} has joined and is in group '{}'", player.getOriginName(), group.getName());
    });
}
}
