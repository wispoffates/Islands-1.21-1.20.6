package me.aleksilassila.islands.commands;

import me.aleksilassila.islands.Islands;
import me.aleksilassila.islands.IslandsConfig;
import me.aleksilassila.islands.generation.Biomes;
import me.aleksilassila.islands.utils.Messages;
import me.aleksilassila.islands.utils.Permissions;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractCreateSubcommands extends Subcommand {
    protected abstract void openGui(Player player);
    protected abstract void runCommand(Player player, String[] args, boolean confirmed, int islandSize);

    protected boolean isRandomBiomeDisabled() {
        return Islands.instance.getConfig().getBoolean("disableRandomBiome");
    }

    @Override
    public void onCommand(Player player, String[] args, boolean confirmed) {
        if (args.length == 0) {
            openGui(player);
            return;
        }

        int islandSize = args.length == 2 ? Islands.instance.parseIslandSize(args[1]) : Islands.instance.parseIslandSize("");

        if (!player.hasPermission(Islands.instance.getCreatePermission(islandSize)) && !player.hasPermission(Permissions.command.createAny)) {
            player.sendMessage(Messages.get("error.NO_PERMISSION"));
            return;
        }

        if (islandSize < Islands.instance.getSmallestIslandSize() || islandSize + 4 >= IslandsConfig.INSTANCE.islandSpacing) {
            player.sendMessage(Messages.get("error.INVALID_ISLAND_SIZE"));
            return;
        }

        runCommand(player, args, confirmed, islandSize);
    }

    protected boolean hasFunds(Player player, double cost) {
        if (Islands.instance.econ == null || player.hasPermission(Permissions.bypass.economy)) return true;

        return Islands.instance.econ.has(player, cost);
    }

    protected void pay(Player player, double cost) {
        if (Islands.instance.econ == null || player.hasPermission(Permissions.bypass.economy)) return;

        if (cost > 0) {
            Islands.instance.econ.withdrawPlayer(player, cost);
            player.sendMessage(Messages.get("success.ISLAND_PURCHASED", cost));
        }
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        List<String> availableArgs = new ArrayList<>();

        if (args.length == 1) {

            if (!isRandomBiomeDisabled())
                availableArgs.add("RANDOM");

            for (Biome biome : Biomes.INSTANCE.getBiomes()) {
                availableArgs.add(biome.name());
            }

        } else if (args.length == 2) {
            for (String size : Islands.instance.definedIslandSizes.keySet()) {
                if (player.hasPermission(Islands.instance.getCreatePermission(Islands.instance.definedIslandSizes.get(size))) || player.hasPermission(Permissions.command.createAny))
                    availableArgs.add(size);
            }

            availableArgs.sort(
                    Comparator.comparingInt(key -> Islands.instance.definedIslandSizes.get(key))
            );
        }

        return availableArgs;
    }
}
