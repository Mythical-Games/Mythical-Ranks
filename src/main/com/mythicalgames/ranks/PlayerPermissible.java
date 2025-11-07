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

    @Override
    public String toString() {
        return "PlayerPermissible [" + player.getOriginName() + " -> " + group.getName() + "]";
    }

    // ---------------------------------------
    // Personal Permissions
    // ---------------------------------------

    /**
     * Adds a personal permission (does not override interface methods).
     */
    public void addPersonalPermission(Permission permission) {
        personalPermissions.add(permission);
    }

    /**
     * Removes a personal permission (does not override interface methods).
     */
    public void removePersonalPermission(Permission permission) {
        personalPermissions.remove(permission);
    }

    /**
     * Checks if the player has the given permission in their personal permissions.
     */
    public boolean hasPersonalPermission(Permission permission) {
        return personalPermissions.contains(permission);
    }

    /**
     * Checks if the player has a permission, including personal permissions and group permissions.
     */
    public boolean hasPermissionIncludingPersonal(Permission permission) {
        return personalPermissions.contains(permission) || (group != null && group.hasPermission(permission));
    }

    /**
     * Returns a new set of all permissions this player has (personal + group + parent permissions).
     */
    public Set<Permission> getAllPermissions() {
        Set<Permission> all = new HashSet<>(personalPermissions);
        if (group != null) {
            all.addAll(group.getPermissions(true)); // include parent permissions
        }
        return Collections.unmodifiableSet(all);
    }
}
