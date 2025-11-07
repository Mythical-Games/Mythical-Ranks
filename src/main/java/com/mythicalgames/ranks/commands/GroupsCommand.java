package com.mythicalgames.ranks.commands;

import java.util.List;

import org.allaymc.api.command.Command;
import org.allaymc.api.command.tree.CommandTree;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.permission.Permission;
import org.allaymc.api.permission.PermissionGroup;

import com.mythicalgames.ranks.Config;
import com.mythicalgames.ranks.RankHelper;
import com.mythicalgames.ranks.RankSystem;

public class GroupsCommand extends Command {

    private final RankSystem plugin;

    public GroupsCommand(RankSystem plugin) {
        super("groups", "Manage permission groups and player ranks");
        this.plugin = plugin;
        getPermissions().add(Permission.create("mythical.groups.manage"));
    }

    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot()
    // /groups create <groupName> [inherit <parentGroup>]
    .key("create")
    .str("group name")
    .key("inherit").optional()
    .str("Parent Group").optional()
    .exec(context -> {
        EntityPlayer sender = context.getSender().asPlayer();
        String groupName = context.getResult(1);
        String parentName = context.getResult(3);

        boolean success = RankHelper.createAndSaveGroup(plugin.config, groupName, parentName);

        if (success) {
            if (parentName != null) {
                sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§aCreated group §e" + groupName + " §awith parent §e" + parentName);
            } else {
                sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§aSuccessfully created group §e" + groupName);
            }
            return context.success();
        } else {
            sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§cGroup §e" + groupName + " §calready exists!");
            return context.fail();
        }
    })

    .root()
    // /groups set <player> <group>
    .key("set")
    .playerTarget("Player Name")
    .str("Group Name")
    .exec(context -> {
        EntityPlayer sender = context.getSender().asPlayer();
         List<EntityPlayer> targets = context.getResult(1);
                
         if (targets.isEmpty()) {
            if (sender != null) sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "Invalid player provided");
            return context.fail();
        }

        EntityPlayer target = targets.get(0);
        String groupName = context.getResult(2);

        PermissionGroup group = RankHelper.getGroup(plugin.config, groupName);
        if (group == null) {
            sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§cGroup §f" + groupName + " §cdoes not exist!");
            return context.fail();
        }

        RankHelper.setPlayerGroup(target, plugin.config, groupName)
            .thenRun(() -> sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§aSuccessfully set §f" + target.getOriginName() +
                " §ato group §f" + groupName))
            .exceptionally(e -> {
                sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§cFailed to set group for §f" + target.getOriginName());
                return null;
            });

        return context.success();
    })

    .root()
    // /groups setformat <group> <format>
    .key("setformat")
    .str("Group Name")
    .msg("Chat Format")
    .exec(context -> {
        String groupName = context.getResult(1);
        String chatFormat = context.getResult(2);

        EntityPlayer sender = context.getSender().asPlayer();
        Config config = RankSystem.getInstance().config;

        boolean success = RankHelper.setChatFormat(config, groupName, chatFormat);
        if (!success) {
            sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§cGroup '" + groupName + "' does not exist!");
        } else {
            sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§aChat format for group '" + groupName + "' updated successfully!");
        }

        return context.success();
    })

    .root()
    // /groups setnametag <group> <format>
    .key("setnametag")
    .str("Group Name")
    .msg("Format")
    .exec(context -> {
        String groupName = context.getResult(1);
        String nameTagFormat = context.getResult(2);

        EntityPlayer sender = context.getSender().asPlayer();
        Config config = RankSystem.getInstance().config;

        boolean success = RankHelper.setNameTagFormat(config, groupName, nameTagFormat);
        if (!success) {
            sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§cGroup '" + groupName + "' does not exist!");
        } else {
            sender.sendMessage(RankSystem.getInstance().PLUGIN_PREFIX + "§aNametag format for group '" + groupName + "' updated successfully!");
        }

        return context.success();
    });
    }
}
