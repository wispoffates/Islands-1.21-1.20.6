package me.aleksilassila.islands.generation;

import me.aleksilassila.islands.Islands;
import me.aleksilassila.islands.IslandsConfig;
import me.aleksilassila.islands.utils.Messages;
import me.aleksilassila.islands.utils.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public enum IslandGeneration {
    INSTANCE;

    protected static final Queue<Task> queue = new LinkedList<>();
    protected static final Queue<Task> VIPQueue = new LinkedList<>();
    private QueueRunnerTask queueRunner;


    IslandGeneration() {
        this.queueRunner = new QueueRunnerTask();
        this.queueRunner.runTaskTimer(Islands.instance, 0, 20);
    }


    public boolean copyIsland(Player player, IslandsConfig.IslandEntry updatedIsland, boolean shouldClearArea, boolean noShape) {
        if (!canAddQueueItem(player))
            return false;
        Task task = new FindSuitableLocationTask(updatedIsland, player, shouldClearArea, noShape);
        addToQueue(task);
        return true;
    }

    public boolean clearIsland(Player player, IslandsConfig.IslandEntry island) {
        if (!canAddQueueItem(player))
            return false;

        CopyTask task = new CopyTask(player, island);
        addToQueue(task);
        return true;
    }

    public void addToQueue(Task task) {
        if (queueContainsPlayer(task.getPlayer()) && !task.getPlayer().hasPermission(Permissions.bypass.queueLimit)) {
            removeFromQueue(task.getPlayer());
        }

        if (task.getPlayer().hasPermission(Permissions.bypass.queue)) {
            VIPQueue.offer(task);
            if (queue.size() > 1) {
                Messages.send(task.getPlayer(), "info.QUEUE_STATUS", VIPQueue.size() - 1);
            }
        } else {
            queue.add(task);
            if (queue.size() > 1) {
                Messages.send(task.getPlayer(), "info.QUEUE_STATUS", VIPQueue.size() + queue.size() - 1);
            }
        }
    }

    /**
     * Check if a player is allowed to add a task to the queue.
     * @param player The player adding the task.
     * @return True if the player has the bypass permission or is below the max of two tasks.
     */
    public boolean canAddQueueItem(Player player) {
        if (queue.isEmpty() || player.hasPermission(Permissions.bypass.queueLimit)) return true;
        int count = 0;
        for (Task item : queue) {
            if (item.getPlayer().getUniqueId().equals(player.getUniqueId())) count++;
        }
        return count>0;
    }

    public void removeFromQueue(Player player) {
        int index = 0;
        for (Task task : VIPQueue) {
            if (index != 0 && task.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                queue.remove(task);
                return;
            }
            index++;
        }

        for (Task task : queue) {
            if (index != 0 && task.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                queue.remove(task);
                return;
            }
            index++;
        }
    }

    private boolean queueIsEmpty() {
        return VIPQueue.isEmpty() && queue.isEmpty();
    }

    public boolean queueContainsPlayer(Player player) {
        for (Task item : VIPQueue) {
            if (item.getPlayer().getUniqueId().equals(player.getUniqueId())) return true;
        }

        for (Task item : queue) {
            if (item.getPlayer().getUniqueId().equals(player.getUniqueId())) return true;
        }

        return false;
    }

    public boolean isIslandInQueue(String islandId) {
        for (Task item : VIPQueue) {
            if (item.getIslandId().equals(islandId)) return true;
        }

        for (Task item : queue) {
            if (item.getIslandId().equals(islandId)) return true;
        }
        return false;
    }

    public boolean worldExists(String name) {
        try {
            for (File f : Bukkit.getWorldContainer().listFiles()) {
                if (f.getName().equals(name)) return true;
            }
        } catch (NullPointerException e) {
            return false;
        }

        return false;
    }

        /** Executes anything in the queue. */
    public class QueueRunnerTask extends BukkitRunnable {

        Task currentTask;

        @Override
        public void run() {
            if(currentTask != null && Bukkit.getServer().getCurrentTick() % (20*60) == 0) {
                Islands.instance.getLogger().info("Queue Depth: " + (VIPQueue.size() + queue.size()) + " Current Task: " + (currentTask != null ? currentTask.toString() : "None"));
            }
            //if nothing in the queue we are done here
            if(IslandGeneration.INSTANCE.queueIsEmpty() && currentTask == null) {
                return;
            }

            if(currentTask == null) {
                currentTask = pollQueue();
                currentTask.setParent(this);
                currentTask.runTaskTimer(Islands.instance, 0, currentTask.getDelay());
            }
        }

        /**
         * Called when a task is complete.
         */
        public void taskComplete() {
            //set current task to null so we can pick up the next one.
            this.currentTask.cancel();
            this.currentTask = null;
        }

        private Task pollQueue() {
            Task task = VIPQueue.poll();
            if(task == null)
                task = queue.poll();
    
            return task;
        }    
        
    }
}
