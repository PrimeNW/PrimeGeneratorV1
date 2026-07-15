package com.weark.itemgenerator;

import com.weark.itemgenerator.listener.GeneratorCommand;
import com.weark.itemgenerator.listener.GeneratorListener;
import com.weark.itemgenerator.listener.WandListener;
import com.weark.itemgenerator.manager.GeneratorManager;
import com.weark.itemgenerator.util.MessageManager;
import com.weark.itemgenerator.util.SchematicManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemGeneratorPlugin extends JavaPlugin {

    private GeneratorManager generatorManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messageManager = new MessageManager(this);
        messageManager.load();

        generatorManager = new GeneratorManager(this);
        generatorManager.init();

        getServer().getPluginManager().registerEvents(new GeneratorListener(this), this);
        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        getCommand("itemgen").setExecutor(new GeneratorCommand(this));

        getLogger().info("ItemGenerator etkinlestirildi. Dil: " + messageManager.getLanguage());
    }

    @Override
    public void onDisable() {
        if (generatorManager != null) {
            generatorManager.shutdown();
        }
        getLogger().info("ItemGenerator devre disi birakildi.");
    }

    public GeneratorManager getGeneratorManager() {
        return generatorManager;
    }

    public SchematicManager getSchematicManager() {
        return generatorManager.getSchematicManager();
    }

    public MessageManager getMessages() {
        return messageManager;
    }
}
