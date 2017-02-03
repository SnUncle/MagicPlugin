package com.elmakers.mine.bukkit.action.builtin;

import com.elmakers.mine.bukkit.action.BaseSpellAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.elmakers.mine.bukkit.spell.BaseSpell;
import com.elmakers.mine.bukkit.utility.CompatibilityUtils;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Collection;

public class MountArmorStandAction extends BaseSpellAction
{
    private boolean armorStandInvisible;
    private boolean armorStandSmall;
    private boolean armorStandMarker;
    private boolean armorStandGravity;
    private boolean mountWand;
    private double armorStandPitch = 0;
    private double armorStandYaw = 0;
    private double moveDistance = 0;
    private double startSpeed = 0;
    private double liftoffThrust = 0;
    private double crashDistance = 0;
    private int liftoffDuration = 0;
    private int maxHeightAboveGround;
    private int maxHeight;
    private CreatureSpawnEvent.SpawnReason armorStandSpawnReason = CreatureSpawnEvent.SpawnReason.CUSTOM;
    private Collection<PotionEffect> crashEffects;

    private ItemStack item;
    private int slotNumber;
    private long liftoffTime;
    private ArmorStand armorStand;
    private Vector direction;
    private double speed;
    private boolean mounted;

    @Override
    public void initialize(Spell spell, ConfigurationSection parameters) {
        super.initialize(spell, parameters);
        
        crashEffects = ConfigurationUtils.getPotionEffects(parameters.getConfigurationSection("crash_effects"));
    }

    @Override
    public void reset(CastContext context)
    {
        super.reset(context);
        mounted = false;
        item = null;
        if (armorStand != null) {
            armorStand.remove();
        }
        armorStand = null;
    }
    
    @Override
    public void prepare(CastContext context, ConfigurationSection parameters)
    {
        super.prepare(context, parameters);
        armorStandInvisible = parameters.getBoolean("armor_stand_invisible", true);
        armorStandSmall = parameters.getBoolean("armor_stand_small", false);
        armorStandMarker = parameters.getBoolean("armor_stand_marker", true);
        armorStandGravity = parameters.getBoolean("armor_stand_gravity", true);
        armorStandPitch = parameters.getDouble("armor_stand_pitch", 0.3);
        armorStandYaw = ConfigurationUtils.getDouble(parameters, "armor_stand_yaw", 0.0);
        moveDistance = parameters.getDouble("steer_speed", 0);
        startSpeed = parameters.getDouble("start_speed", 0);
        liftoffThrust = parameters.getDouble("liftoff_thrust", 0);
        liftoffDuration = parameters.getInt("liftoff_duration", 0);
        crashDistance = parameters.getDouble("crash_distance", 0);
        mountWand = parameters.getBoolean("mount_wand", false);
        maxHeight = parameters.getInt("max_height", 0);
        maxHeightAboveGround = parameters.getInt("max_height_above_ground", 0);

        if (parameters.contains("armor_stand_reason")) {
            String reasonText = parameters.getString("armor_stand_reason").toUpperCase();
            try {
                armorStandSpawnReason = CreatureSpawnEvent.SpawnReason.valueOf(reasonText);
            } catch (Exception ex) {
                context.getMage().sendMessage("Unknown spawn reason: " + reasonText);
            }
        }
    }

	@Override
	public SpellResult perform(CastContext context) {
        if (!mounted) {
            return mount(context);
        }
        Entity target = context.getTargetEntity();
        if (target == null)
        {
            return SpellResult.CAST;
        }
        Entity mount = target.getVehicle();
        if (mount == null || mount != armorStand)
        {
            return SpellResult.CAST;
        }
        
        if (crashDistance > 0)
        {
            Vector threshold = direction.clone().multiply(crashDistance);
            if (checkForCrash(context, target.getLocation(), threshold)) {
                crash(context);
                return SpellResult.CAST;
            }
        }
        
        adjustHeading(context);
        if (System.currentTimeMillis() > liftoffTime + liftoffDuration) {
            applyThrust(context);
        }
        
        return SpellResult.PENDING;
    }
    
    protected void adjustHeading(CastContext context) {
        Location targetLocation = context.getTargetEntity().getLocation();
        Vector targetDirection = targetLocation.getDirection();
        if (moveDistance == 0) {
            direction = targetDirection;
        } else {
            double moveDistanceSquared = moveDistance * moveDistance;
            double distanceSquared = direction.distanceSquared(targetDirection);
            if (distanceSquared <= moveDistanceSquared) {
                direction = targetDirection;
            } else {
                targetDirection = targetDirection.subtract(direction).normalize().multiply(moveDistance);
                direction.add(targetDirection);
            }
        }
        
        float targetPitch = targetLocation.getPitch();
        targetLocation.setDirection(direction);
        CompatibilityUtils.setYawPitch(armorStand, targetLocation.getYaw() + (float)armorStandYaw, targetLocation.getPitch());
        if (armorStandPitch != 0) {
            armorStand.setHeadPose(new EulerAngle(armorStandPitch * targetPitch / 180 * Math.PI, 0, 0));
        }
    }
    
    protected void applyThrust(CastContext context) {
        Location currentLocation = context.getTargetEntity().getLocation();
        boolean grounded = false;
        if (maxHeight > 0 && currentLocation.getY() >= maxHeight) {
            grounded = true;
        } else if (maxHeightAboveGround > 0) {
            Block block = currentLocation.getBlock();
            int height = 0;
            while (height < maxHeightAboveGround && context.isPassthrough(block.getType()))
            {
                block = block.getRelative(BlockFace.DOWN);
                height++;
            }
            if (context.isPassthrough(block.getType())) {
                grounded = true;
            }
        }
        if (grounded) {
            direction.setY(0).normalize();
        }
        if (speed > 0) {
            armorStand.setVelocity(direction.normalize().multiply(speed));
        }
    }
    
    protected SpellResult mount(CastContext context) {
        Mage mage = context.getMage();
        Player player = mage.getPlayer();
        if (player == null)
        {
            return SpellResult.PLAYER_REQUIRED;
        }
        
        Entity entity = context.getTargetEntity();
        if (entity == null)
        {
            return SpellResult.NO_TARGET;
        }

        item = null;
        if (mountWand) {
            Wand activeWand = mage.getActiveWand();

            // Check for trying to mount an item from the offhand slot
            // Not handling this for now.
            if (activeWand != context.getWand()) {
                return SpellResult.NO_TARGET;
            }

            if (activeWand != null) {
                activeWand.deactivate();
            }

            item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR)
            {
                return SpellResult.FAIL;
            }
            slotNumber = player.getInventory().getHeldItemSlot();
        }

        armorStand = CompatibilityUtils.spawnArmorStand(mage.getLocation());

        if (armorStandInvisible) {
            CompatibilityUtils.setInvisible(armorStand, true);
        }
        if (armorStandMarker) {
            armorStand.setMarker(true);
        }
        if (!armorStandGravity) {
            armorStand.setGravity(false);
        }
        CompatibilityUtils.setDisabledSlots(armorStand, 2039552);
        if (armorStandSmall) {
            armorStand.setSmall(true);
        }

        MageController controller = context.getController();
        armorStand.setMetadata("notarget", new FixedMetadataValue(controller.getPlugin(), true));
        controller.setForceSpawn(true);
        try {
            CompatibilityUtils.addToWorld(entity.getWorld(), armorStand, armorStandSpawnReason);
        } catch (Exception ex) {
            ex.printStackTrace();
            return SpellResult.FAIL;
        }
        controller.setForceSpawn(false);
        
        if (mountWand) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            armorStand.setHelmet(item);
        }
        armorStand.setPassenger(entity);
        speed = startSpeed;
        direction = entity.getLocation().getDirection();
        if (liftoffThrust > 0) {
            armorStand.setVelocity(new Vector(0, liftoffThrust, 0));
        }
        liftoffTime = System.currentTimeMillis();
        adjustHeading(context);
        mounted = true;
        
        return SpellResult.PENDING;
	}
	
	@Override
    public void finish(CastContext context) {
        if (armorStand != null) {
            armorStand.removeMetadata("notarget", context.getPlugin());
            armorStand.remove();
            armorStand = null;
        }
        Player player = context.getMage().getPlayer();
        if (player == null || item == null) return;
        
        ItemStack currentItem = player.getInventory().getItemInMainHand();
        if (currentItem == null) {
            player.getInventory().setItemInMainHand(item);
        } else {
            currentItem = player.getInventory().getItem(slotNumber);
            if (currentItem != null) {
                context.getMage().giveItem(item);
            } else {
                player.getInventory().setItem(slotNumber, item);
            }
        }
        
        item = null;
    }

    protected void crash(CastContext context)
    {
        context.sendMessageKey("crash");
        context.playEffects("crash");
        Entity targetEntity = context.getTargetEntity();
        if (crashEffects != null && targetEntity != null && crashEffects.size() > 0 && targetEntity instanceof LivingEntity) {
            CompatibilityUtils.applyPotionEffects((LivingEntity)targetEntity, crashEffects);
        }
    }

    protected boolean checkForCrash(CastContext context, Location source, Vector threshold)
    {
        Block facingBlock = source.getBlock();
        Block targetBlock = source.add(threshold).getBlock();

        if (!targetBlock.equals(facingBlock) && !context.isPassthrough(targetBlock.getType())) {
            return true;
        }

        return false;
    }

	@Override
	public boolean isUndoable()
	{
		return false;
	}
    
    @Override
    public void getParameterNames(Spell spell, Collection<String> parameters)
    {
        super.getParameterNames(spell, parameters);
        parameters.add("armor_stand_invisible");
        parameters.add("armor_stand_small");
        parameters.add("armor_stand_marker");
        parameters.add("armor_stand_gravity");
        parameters.add("armor_stand_reason");
        parameters.add("armor_stand_pitch");
        parameters.add("armor_stand_yaw");
        parameters.add("steer_speed");
        parameters.add("liftoff_duration");
        parameters.add("liftoff_thrust");
        parameters.add("crash_distance");
        parameters.add("mount_wand");
        parameters.add("max_height");
        parameters.add("max_height_above_ground");
    }

    @Override
    public void getParameterOptions(Spell spell, String parameterKey, Collection<String> examples)
    {
        if (parameterKey.equals("crash_distance")
                || parameterKey.equals("max_height")
                || parameterKey.equals("max_height_above_ground")) {
            examples.addAll(Arrays.asList(BaseSpell.EXAMPLE_SIZES));
        } else if (parameterKey.equals("armor_stand_invisible") 
                || parameterKey.equals("armor_stand_marker") 
                || parameterKey.equals("armor_stand_small")
                || parameterKey.equals("armor_stand_gravity")
                || parameterKey.equals("mount_wand")) {
            examples.addAll(Arrays.asList(BaseSpell.EXAMPLE_BOOLEANS));
        } else if (parameterKey.equals("steer_speed")
                || parameterKey.equals("speed")
                || parameterKey.equals("armor_stand_pitch")
                || parameterKey.equals("armor_stand_yaw")
                || parameterKey.equals("liftoff_thrust")) {
            examples.addAll(Arrays.asList(BaseSpell.EXAMPLE_VECTOR_COMPONENTS));
        } else if (parameterKey.equals("liftoff_duration")) {
            examples.addAll(Arrays.asList(BaseSpell.EXAMPLE_DURATIONS));
        } else if (parameterKey.equals("armor_stand_reason")) {
            for (CreatureSpawnEvent.SpawnReason reason : CreatureSpawnEvent.SpawnReason.values()) {
                examples.add(reason.name().toLowerCase());
            }
        } else {
            super.getParameterOptions(spell, parameterKey, examples);
        }
    }
}