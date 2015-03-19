package com.elmakers.mine.bukkit.api.action;

import com.elmakers.mine.bukkit.api.block.MaterialBrush;
import com.elmakers.mine.bukkit.api.block.UndoList;
import com.elmakers.mine.bukkit.api.effect.EffectPlayer;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.spell.Spell;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Random;
import java.util.logging.Logger;

public interface CastContext {
    public Entity getEntity();
    public Location getLocation();
    public Location getTargetLocation();
    public Block getTargetBlock();
    public Block getInteractBlock();
    public Entity getTargetEntity();
    public void setTargetEntity(Entity targetEntity);
    public void setTargetLocation(Location targetLocation);
    public Spell getSpell();
    public Mage getMage();
    public Collection<EffectPlayer> getEffects(String key);
    public MageController getController();
    public void registerForUndo(Runnable runnable);
    public void registerModified(Entity entity);
    public void registerForUndo(Entity entity);
    public void registerForUndo(Block block);
    public void registerForUndo(Block block, boolean addNeighbors);
    public void updateBlock(Block block);
    public void registerVelocity(Entity entity);
    public void registerMoved(Entity entity);
    public void registerPotionEffects(Entity entity);
    public Block getPreviousBlock();
    public Vector getDirection();
    public World getWorld();
    public Location getEyeLocation();
    public boolean isIndestructible(Block block);
    public boolean hasBuildPermission(Block block);
    public void playEffects(String key);
    public String getMessage(String key);
    public String getMessage(String key, String def);
    public Location tryFindPlaceToStand(Location location);
    public void castMessage(String message);
    public void sendMessage(String message);
    public void setTargetedLocation(Location location);
    public Block findBlockUnder(Block block);
    public Block findSpaceAbove(Block block);
    public boolean isTransparent(Material material);
    public boolean isDestructible(Block block);
    public boolean isTargetable(Material material);
    public boolean canTarget(Entity entity);
    public MaterialBrush getBrush();
    public void setBrush(MaterialBrush brush);
    public void addTargetEntity(Entity entity);
    public Collection<Entity> getTargetEntities();
    public void messageTargets(String messageKey);
    public Random getRandom();
    public UndoList getUndoList();
    public void setTargetName(String name);
    public String getTargetName();
    public Logger getLogger();
    public int getWorkAllowed();
    public void setWorkAllowed(int work);
    public void addWork(int work);
    public void performedActions(int count);
    public int getActionsPerformed();
    public void finish();
    public void retarget(int range, double fov, double closeRange, double closeFOV, boolean useHitbox);
    public CastContext getBaseContext();
}
