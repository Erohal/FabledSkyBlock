package com.songoda.skyblock.gui.wip;

import com.songoda.core.compatibility.CompatibleBiome;
import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.CompatibleSound;
import com.songoda.core.gui.AnvilGui;
import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.hooks.EconomyManager;
import com.songoda.core.utils.TextUtils;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.bank.BankManager;
import com.songoda.skyblock.bank.Transaction;
import com.songoda.skyblock.biome.BiomeManager;
import com.songoda.skyblock.cooldown.Cooldown;
import com.songoda.skyblock.cooldown.CooldownManager;
import com.songoda.skyblock.cooldown.CooldownPlayer;
import com.songoda.skyblock.cooldown.CooldownType;
import com.songoda.skyblock.gui.bank.GuiBankSelector;
import com.songoda.skyblock.island.Island;
import com.songoda.skyblock.island.IslandEnvironment;
import com.songoda.skyblock.island.IslandManager;
import com.songoda.skyblock.island.IslandWorld;
import com.songoda.skyblock.message.MessageManager;
import com.songoda.skyblock.sound.SoundManager;
import com.songoda.skyblock.utils.NumberUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GuiBiome extends Gui {
    private final SkyBlock plugin;
    private final Island island;
    private final FileConfiguration languageLoad;
    private final FileConfiguration config;
    private final Gui returnGui;
    private final Player player;
    private final IslandWorld world;
    private final boolean admin;

    public GuiBiome(SkyBlock plugin, Player player, Island island, IslandWorld world, Gui returnGui, boolean admin) {
        super(returnGui);
        this.plugin = plugin;
        this.island = island;
        this.world = world;
        this.player = player;
        this.returnGui = returnGui;
        this.admin = admin;
        this.config = plugin.getFileManager()
                .getConfig(new File(plugin.getDataFolder(), "config.yml")).getFileConfiguration();
        this.languageLoad = plugin.getFileManager()
                .getConfig(new File(plugin.getDataFolder(), "language.yml")).getFileConfiguration();
        setDefaultItem(null);
        setTitle(TextUtils.formatText(languageLoad.getString("Menu.Input.Title")));
        paint();
    }

    public void paint() {
        SoundManager soundManager = plugin.getSoundManager();
        BiomeManager biomeManager = plugin.getBiomeManager();
        CooldownManager cooldownManager = plugin.getCooldownManager();
        MessageManager messageManager = plugin.getMessageManager();
        IslandManager islandManager = plugin.getIslandManager();

        if (inventory != null)
            inventory.clear();
        setActionForRange(0, 0, 1, 8, null);

        setButton(0, GuiUtils.createButtonItem(CompatibleMaterial.OAK_FENCE_GATE, // Exit
                TextUtils.formatText(languageLoad.getString("Menu.Bans.Item.Exit.Displayname"))), (event) -> {
            CompatibleSound.BLOCK_CHEST_CLOSE.play(event.player);
            event.player.closeInventory();
        });

        setButton(8, GuiUtils.createButtonItem(CompatibleMaterial.OAK_FENCE_GATE, // Exit
                TextUtils.formatText(languageLoad.getString("Menu.Bans.Item.Exit.Displayname"))), (event) -> {
            CompatibleSound.BLOCK_CHEST_CLOSE.play(event.player);
            event.player.closeInventory();
        });

        for(int i=9; i<18; i++){
            setItem(i, CompatibleMaterial.BLACK_STAINED_GLASS_PANE.getItem());
        }

        List<BiomeIcon> biomes = new ArrayList<>();
        for(CompatibleBiome biome : CompatibleBiome.getCompatibleBiomes()) {
            if(biome.isCompatible()
                    && player.hasPermission("fabledskyblock.biome." + biome.name().toLowerCase())
                    && config.getBoolean("Island.Biome." + world.name() + "." + biome.name(), false)){
                BiomeIcon icon = new BiomeIcon(plugin,  biome);
                switch(world){
                    case Normal:
                        if(icon.normal){
                            biomes.add(icon);
                        }
                        break;
                    case Nether:
                        if(icon.nether){
                            biomes.add(icon);
                        }
                        break;
                    case End:
                        if(icon.end){
                            biomes.add(icon);
                        }
                        break;
                }
            }
        }

        if(biomes.size() > 0){
            this.pages = (int) Math.max(1, Math.ceil((double) biomes.size() / 27d));

            if (page != 1)
                setButton(5, 2, GuiUtils.createButtonItem(CompatibleMaterial.ARROW,
                        TextUtils.formatText(languageLoad.getString("Menu.Bank.Item.Last.Displayname"))),
                        (event) -> {
                            page--;
                            paint();
                        });

            if (page != pages)
                setButton(5, 6, GuiUtils.createButtonItem(CompatibleMaterial.ARROW,
                        TextUtils.formatText(languageLoad.getString("Menu.Bank.Item.Next.Displayname"))),
                        (event) -> {
                            page++;
                            paint();
                        });

            for (int i = 9; i < ((getRows()-1)*9)+9; i++) {
                int current = ((page - 1) * 36) - 9;
                if (current + i >= biomes.size()) {
                    setItem(i, null);
                    continue;
                }
                BiomeIcon icon = biomes.get(current + i);
                if (icon == null) continue;

                setButton(i, icon.displayItem, event -> {
                    if (cooldownManager.hasPlayer(CooldownType.Biome, player) && !player.hasPermission("fabledskyblock.bypass.cooldown")) {
                        CooldownPlayer cooldownPlayer = cooldownManager.getCooldownPlayer(CooldownType.Biome, player);
                        Cooldown cooldown = cooldownPlayer.getCooldown();

                        if (cooldown.getTime() < 60) {
                            messageManager.sendMessage(player,
                                    languageLoad.getString("Island.Biome.Cooldown.Message")
                                            .replace("%time",
                                                    cooldown.getTime() + " " + languageLoad
                                                            .getString("Island.Biome.Cooldown.Word.Second")));
                        } else {
                            long[] durationTime = NumberUtil.getDuration(cooldown.getTime());
                            messageManager.sendMessage(player,
                                    languageLoad.getString("Island.Biome.Cooldown.Message")
                                            .replace("%time", durationTime[2] + " "
                                                    + languageLoad.getString("Island.Biome.Cooldown.Word.Minute")
                                                    + " " + durationTime[3] + " "
                                                    + languageLoad.getString("Island.Biome.Cooldown.Word.Second")));
                        }

                        soundManager.playSound(player,  CompatibleSound.ENTITY_VILLAGER_NO.getSound(), 1.0F, 1.0F);

                        return;
                    }
                    cooldownManager.createPlayer(CooldownType.Biome, player);
                    biomeManager.setBiome(island, icon.biome.getBiome());
                    island.setBiome(icon.biome.getBiome());
                    island.save();

                    soundManager.playSound(island.getLocation(IslandWorld.Normal, IslandEnvironment.Island),
                            CompatibleSound.ENTITY_GENERIC_SPLASH.getSound(), 1.0F, 1.0F);

                    if (!islandManager.isPlayerAtIsland(island, player, IslandWorld.Normal)) {
                        soundManager.playSound(player, CompatibleSound.ENTITY_GENERIC_SPLASH.getSound(), 1.0F, 1.0F);
                    }
                });
            }
        } else {
            setItem(31, CompatibleMaterial.BARRIER.getItem());
        }
    }
}
