package io.github.iandesuyo;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import org.bukkit.plugin.java.JavaPlugin;

public class App extends JavaPlugin {

    JedisPool pool;
    Jedis j = null;

    Tracker tracker;

    private void initRedisConnection() {
        try {
            closeRedisConnection();
            pool = new JedisPool(getConfig().getString("redis-host"), getConfig().getInt("redis-port"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeRedisConnection() {
        if (pool != null) {
            pool.close();
        }
    }

    @Override
    public void onEnable() {
        getLogger().info("Starting...");
        saveDefaultConfig();
        initRedisConnection();
        tracker = new Tracker(this, pool);
        getLogger().info("Done");
    }

    @Override
    public void onDisable() {
        getLogger().info("Exiting...");
        if (getConfig().getBoolean("clean-when-close")) {
            j = pool.getResource();
            j.flushDB();
            j.close();
        }
        tracker.shutdown();
        closeRedisConnection();
        getLogger().info("Bye");
    }
}