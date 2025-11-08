package com.mythicalgames.ranks.commands;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.allaymc.api.command.Command;
import org.allaymc.api.command.tree.CommandTree;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.permission.Permission;

import com.mythicalgames.ranks.RankHelper;
import com.mythicalgames.ranks.RankSystem;

public class PlayerCommand extends Command {

    private final RankSystem plugin;

    public PlayerCommand(RankSystem plugin) {
        super("player", "Manage permission groups and player ranks");
        this.plugin = plugin;
        getPermissions().add(Permission.create("mythical.groups.players.manage"));
    }

    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot()
            // /player addperm <player> <permission>
            .key("addperm")
            .playerTarget("Player Target")
            .msg("Permission")
            .exec(context -> {
                EntityPlayer sender = context.getSender().asPlayer();
                String permission = context.getResult(2);
                List<EntityPlayer> targets = context.getResult(1);
                EntityPlayer target = targets.get(0);
                
                if (targets.isEmpty()) {
                    if (sender != null) sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "Invalid player provided");
                    return context.fail();
                }

                CompletableFuture<Void> result = RankHelper.addPermission(target, permission);

                result.thenRun(() -> {
                    sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§aSuccessfully added permission §e" + permission + " §ato §b" + target.getOriginName() + "§a.");
                    target.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§aYou have been granted the permission §e" + permission + "§a!");
                }).exceptionally(e -> {
                    sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§cAn error occurred while adding the permission. Check console for details.");
                    e.printStackTrace();
                    return null;
                });

                return context.success();
            })

            .root()
            // /player removeperm <player> <permission>
            .key("removeperm")
            .playerTarget("Player Target")
            .msg("Permission")
            .exec(context -> {
                EntityPlayer sender = context.getSender().asPlayer();
                String permission = context.getResult(2);
                List<EntityPlayer> targets = context.getResult(1);
                EntityPlayer target = targets.get(0);
                
                if (targets.isEmpty()) {
                    if (sender != null) sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "Invalid player provided");
                    return context.fail();
                }

                CompletableFuture<Void> result = RankHelper.removePermission(target, permission);

                result.thenRun(() -> {
                    sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§aRemoved permission §e" + permission + " §afrom §b" + target.getOriginName() + "§a.");
                    target.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§cThe permission §e" + permission + " §chas been removed from your account.");
                }).exceptionally(e -> {
                    sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§cAn error occurred while removing the permission. Check console for details.");
                    e.printStackTrace();
                    return null;
                });

                return context.success();
            })

            .root()
            // /player listperms <player>
            .key("listperms")
            .playerTarget("Player Target")
            .exec(context -> {
                EntityPlayer sender = context.getSender().asPlayer();
                List<EntityPlayer> targets = context.getResult(1);
                EntityPlayer target = targets.get(0);
                
                if (targets.isEmpty()) {
                    if (sender != null) sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "Invalid player provided");
                    return context.fail();
                }

                RankHelper.getOrCreatePermissible(target, plugin.config).thenAccept(permissible -> {
                    if (permissible == null) {
                        sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§cCould not load permissions for §e" + target.getOriginName());
                        return;
                    }

                    var allPerms = permissible.getAllPermissions();

                    if (allPerms.isEmpty()) {
                        sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§e" + target.getOriginName() + " §7has no permissions.");
                        return;
                    }

                    sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§aPermissions for §b" + target.getOriginName() + "§a:");
                    allPerms.forEach(perm -> sender.sendMessage(" §7- §f" + perm.getName()));
                }).exceptionally(e -> {
                    sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§cAn error occurred while loading permissions for §e" + target.getOriginName());
                    e.printStackTrace();
                    return null;
                });


                return context.success();
            });
    }
}

