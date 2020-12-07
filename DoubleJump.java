package com.randomrobinnnn.minigameapi.model.defaultabilities;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.randomrobinnnn.minigameapi.Main;
import com.randomrobinnnn.minigameapi.model.GamePlayer;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class DoubleJump extends KitAbility {

    // Name: DoubleJump.java
    // Author: RandomRobinnnn
    // CopyRight: Feel free to use or modify this however you wish. Please do give some sort of credit if you decide to use (parts of) this class.
    // TODO: Reset doublejump on player death (Player might fly off when respawning)
    
    private boolean enabled;
    private double horizontalModifier;
    private double verticalModifier;
    private boolean cancelJump;
    private boolean hadJumpCanceled;
    private boolean overrideVelocity;
    private Vector lastVelocity;

    public DoubleJump(GamePlayer player) {
        super(player);
        setHorizontalModifier(0.7);
        setVerticalModifier(0.76);

        cancelJump = false;
        hadJumpCanceled = false;
    }

    public void setHorizontalModifier(double horizontalModifier) {
        this.horizontalModifier = horizontalModifier;
    }

    public void setVerticalModifier(double verticalModifier) {
        this.verticalModifier = verticalModifier;
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent e) {
        if (e.getPlayer() != player)
            return;

        if (!enabled)
            return;

        if (player.getAllowFlight()) {
            e.setCancelled(true);

            Location loc = player.getLocation();
            Vector dir = loc.getDirection();
            Vector velocity = new Vector(dir.getX() * horizontalModifier, verticalModifier, dir.getZ() * horizontalModifier);
            Block currentBlock = loc.getBlock();
            Block blockJustUnder = loc.subtract(0, 0.00001, 0).getBlock();
            Block blockBitUnder = loc.subtract(0, 1.122, 0).getBlock();

            if ((blockJustUnder != currentBlock && !blockJustUnder.isPassable()) || blockBitUnder != currentBlock && !blockBitUnder.isPassable()) {
                // When player is on ground level (or close), but isOnGround is still false
                cancelNextJump();
            }

            player.setAllowFlight(false);
            player.setFlying(false);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);
            player.setVelocity(velocity);
            player.setFallDistance(-20F);

            lastVelocity = velocity;
            overrideVelocity = true;
        }
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent e) {
        if (e.getPlayer() == player) {
            if (cancelJump) {
                // Player is in air
                cancelJump = false;
                hadJumpCanceled = true;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVelocityChange(PlayerVelocityEvent e) {
        // This event handler fixes a issue when setVelocity() is used, the Y value gets overridden by something else.
        // With this method we make sure that IF we set the velocity in this class, the result of the event has the same velocity
        // This is possible because we use the highest event priority, which means this handler gets called last

        if (overrideVelocity) {
            // Only override the velocity if it has been set in this class
            e.setVelocity(lastVelocity);

            overrideVelocity = false;
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getPlayer() != player)
            return;

        if (shouldRecharge()) {
            player.setAllowFlight(true);
            player.setFlying(false);
            player.setFallDistance(0);
        }
    }

    @Override
    public ItemStack getItem() {
        return null;
    }

    @Override
    public void onStart() {
        enabled = true;
        player.setAllowFlight(true);
    }

    @Override
    public void onStop() {
        enabled = false;
    }

    @Override
    public void onItemUse(PlayerInteractEvent e) {
        // We use no item
    }

    @Override
    public void onItemDrop(PlayerDropItemEvent e) {
        // We use no item
    }

    private boolean shouldRecharge() {
        boolean shouldRecharge = player.isOnGround();

        if (!shouldRecharge) {
            Location location = player.getLocation();
            double x = location.getX();
            double y = location.getY() - 0.009;
            double z = location.getZ();
            World world = location.getWorld();
            Location locationUnderPlayer = new Location(world, x, y, z);
            List<Location> toCheck = new ArrayList<>();
            toCheck.add(locationUnderPlayer.clone().add(0.3, 0, -0.3));
            toCheck.add(locationUnderPlayer.clone().add(-0.3, 0, -0.3));
            toCheck.add(locationUnderPlayer.clone().add(0.3, 0, 0.3));
            toCheck.add(locationUnderPlayer.clone().add(-0.3, 0, 0.3));

            for (Location loc : toCheck) {
                if (!loc.getBlock().isPassable()) {
                    shouldRecharge = true;
                    break;
                }
            }
        }

        return shouldRecharge;
    }

    private void cancelNextJump() {
        cancelJump = true;

        new BukkitRunnable() {
            @Override
            public void run() {
                cancelJump = false;

                if (!hadJumpCanceled) {
                    // When for some reason jump didn't trigger, but the Y is still messed up
                    player.setVelocity(lastVelocity);
                } else {
                    hadJumpCanceled = false;
                }
            }
        }.runTaskLater(Main.getPlugin(), 2);
    }
}
