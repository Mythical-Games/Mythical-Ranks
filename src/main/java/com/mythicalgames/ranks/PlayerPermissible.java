package com.mythicalgames.ranks;

import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.permission.Permission;
import org.allaymc.api.permission.PermissionGroup;
import org.allaymc.api.permission.Permissible;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PlayerPermissible implements Permissible {

    private final EntityPlayer player;
    private PermissionGroup group;
    private final Set<Permission> personalPermissions = new HashSet<>();

    public PlayerPermissible(EntityPlayer player, PermissionGroup group) {
        this.player = player;
        this.group = group;
    }

    @Override
    public PermissionGroup getPermissionGroup() {
        return group;
    }

    public void setPermissionGroup(PermissionGroup group) {
        this.group = group;
    }

    @Override
    public Permissible getPermissible() {
        return this;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    // ---------------------------------------
    // Personal Permissions
    // ---------------------------------------
    public void addPersonalPermission(Permission permission) {
        personalPermissions.add(permission);
    }

    public void removePersonalPermission(Permission permission) {
        personalPermissions.remove(permission);
    }

    public boolean hasPersonalPermission(Permission permission) {
        return personalPermissions.contains(permission);
    }

    public boolean hasPermissionIncludingPersonal(Permission permission) {
        return personalPermissions.contains(permission) || (group != null && group.hasPermission(permission));
    }

    public Set<Permission> getAllPermissions() {
        Set<Permission> all = new HashSet<>(personalPermissions);
        if (group != null) {
            all.addAll(group.getPermissions(true));
        }
        return Collections.unmodifiableSet(all);
    }
}
