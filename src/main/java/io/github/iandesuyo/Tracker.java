package io.github.iandesuyo;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Tracker implements Listener {

    JedisPool pool;
    Jedis j;
    Boolean skipOp = false;
    Boolean skipNonSurvival = false;

    public Tracker(JavaPlugin plugin, JedisPool p) {
        this.pool = p;
        this.skipOp = plugin.getConfig().getBoolean("ignore-op");
        this.skipNonSurvival = plugin.getConfig().getBoolean("ignore-non-survival");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void shutdown() {
        this.pool = null;
    }

    private boolean playerFilter(Player player) {
        // True = block
        if ((skipOp && player.isOp()) || (skipNonSurvival && player.getGameMode() != GameMode.SURVIVAL)) {
            return true;
        }
        return false;
    }

    private String getHP(double val) {
        int hp = (int) val;
        hp = hp >= 20 ? 20 : hp;
        hp = hp <= 0 ? 0 : hp;
        return String.valueOf(hp);
    }

    private String getFood(int val) {
        val = val >= 20 ? 20 : val;
        val = val <= 0 ? 0 : val;
        return String.valueOf(val);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        j = pool.getResource();
        j.hset(player.getDisplayName(), "hp", getHP(player.getHealth()));
        j.hset(player.getDisplayName(), "foodLv", getFood(player.getFoodLevel()));
        j.close();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (playerFilter(player)) {
            return;
        }

        if (block.getType() == Material.STONE) {
            j = pool.getResource();
            j.hincrBy(player.getDisplayName(), "stoneBroke", 1);
            j.close();

        } else if (block.getType() == Material.DIAMOND_ORE) {
            j = pool.getResource();
            j.hincrBy(player.getDisplayName(), "diamondFound", 1);
            j.close();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        if (playerFilter(player)) {
            return;
        }
        if (item.getType() == Material.GOLDEN_APPLE) {
            j = pool.getResource();
            j.hincrBy(player.getDisplayName(), "goldenAppleAte", 1);
            j.close();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            j = pool.getResource();
            j.hincrBy(killer.getDisplayName(), "killed", 1);
            j.close();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (playerFilter(player)) {
                return;
            }
            j = pool.getResource();
            j.hset(player.getDisplayName(), "hp", getHP(player.getHealth() - event.getFinalDamage()));
            j.close();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerHeal(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (playerFilter(player)) {
                return;
            }
            j = pool.getResource();
            j.hset(player.getDisplayName(), "hp", getHP(player.getHealth() + event.getAmount()));
            j.close();
        }

    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (playerFilter(player)) {
                return;
            }
            j = pool.getResource();
            j.hset(player.getDisplayName(), "foodLv", getFood(event.getFoodLevel()));
            j.close();
        }
    }
}
