package cloud.zeroprox.gamespleef.game;

import cloud.zeroprox.gamespleef.GameSpleef;
import cloud.zeroprox.gamespleef.stats.PlayerStats;
import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class GameClassic implements IGame {
	
	GameSpleef plugin;
	
	public GameClassic(GameSpleef plugin) {
		this.plugin = plugin;
	}

    private String name;
    private GameSpleef.Mode mode;
    private UUID world;
    private AABB area;
    private List<AABB> floors;
    private Transform<World> spawn, lobby;
    private int limit, lowestY, campRadius, campInterval, campPlayers;
    private boolean saveInventories;
    private HashMap<BlockSnapshot, UUID> brokenBlocks;
    private Task countTask, campTask;
    Map<UUID, PlayerStats> activePlayers = new HashMap<>();
    Map<UUID, PlayerStats> inactivePlayers = new HashMap<>();
    Map<UUID, Vector3i> playerPos = new HashMap<>();
    Map<UUID, List<Optional<ItemStack>>> inventories = new HashMap<>();
    HashSet<UUID> campWarnings = new HashSet<>();

    public GameClassic(String name, AABB area, List<AABB> floors, Transform<World> spawn, Transform<World> lobby, int limit, int campRadius, int campInterval, int campPlayers, boolean saveInventories) {
        this.name = name;
        this.area = area;
        this.floors = floors;
        this.spawn = spawn;
        this.lobby = lobby;
        this.limit = limit;
        this.world = lobby.getExtent().getUniqueId();
        this.lowestY = 255;
        for (AABB floor : floors) {
            if (floor.getMin().getFloorY() <= lowestY) {
                lowestY = floor.getMin().getFloorY();
            }
        }
        this.brokenBlocks = new HashMap<>();
        this.mode = GameSpleef.Mode.READY;

        this.campRadius = campRadius;
        this.campInterval = campInterval;
        this.campPlayers = campPlayers;
        this.saveInventories = saveInventories;
    }

    @Override
    public GameSpleef.Mode getMode() {
        return this.mode;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Collection<UUID> getAllPlayers() {
        return this.activePlayers.keySet();
    }

    @Override
    public Transform<World> getSpawn() {
        return this.spawn;
    }

    @Override
    public Transform<World> getLobby() {
        return this.lobby;
    }

    @Override
    public boolean isInsideArea(Location<World> location) {
        return this.area.contains(location.getX(), location.getY(), location.getZ()) && world.equals(location.getExtent().getUniqueId());
    }

    @Override
    public boolean isInsideFloor(Location<World> location) {
        return world.equals(location.getExtent().getUniqueId()) && this.floors.stream().anyMatch(floor -> floor.contains(location.getX(), location.getY(), location.getZ()));
    }

    @Override
    public Optional<PlayerStats> getPlayerStats(Player player) {
        return this.getPlayerStats(player.getUniqueId());
    }

    @Override
    public Optional<PlayerStats> getPlayerStats(UUID uuid) {
        if (this.activePlayers.containsKey(uuid)) {
            return Optional.of(this.activePlayers.get(uuid));
        }
        if (this.inactivePlayers.containsKey(uuid)) {
            return Optional.of(this.inactivePlayers.get(uuid));
        }
        return Optional.empty();
    }

    @Override
    public void showStats(Player player) {

    }

    @SuppressWarnings("static-access")
	@Override
    public void addPlayer(Player player) {
        if (this.activePlayers.size() >= this.limit) {
            player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("game.join.1")));
            return;
        }
        if (this.mode == GameSpleef.Mode.DISABLED) {
            player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("game.join.2")));
            return;
        }
        if (this.mode == GameSpleef.Mode.PLAYING) {
            player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("game.join.3")));
            return;
        }
        if (this.saveInventories == false && player.getInventory().totalItems() != 0) {
            player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("game.join.4")));
            return;
        }
        player.health().set(20D);
        player.maxHealth().set(20D);
        player.foodLevel().set(20);
        player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
        player.offer(Keys.CAN_FLY, false);
        player.offer(Keys.FIRE_TICKS, 0);
        player.offer(Keys.POTION_EFFECTS, Arrays.asList(
                PotionEffect.builder().amplifier(5).duration(20 * 60 * 60 * 60).particles(false).potionType(PotionEffectTypes.RESISTANCE).build(),
                PotionEffect.builder().amplifier(1).duration(20 * 60 * 60 * 60).particles(false).potionType(PotionEffectTypes.SATURATION).build()));
        player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("game.join.5")));

        if (this.saveInventories) {
            List<Optional<ItemStack>> items = new ArrayList<>();
            for (Inventory slot : player.getInventory().slots())  {
                items.add(slot.peek());
            }
            this.inventories.put(player.getUniqueId(), items);
        }


        ((PlayerInventory) player.getInventory()).getHotbar().setSelectedSlotIndex(0);
        player.getInventory().clear();
        player.setTransform(this.getSpawn());

        this.activePlayers.put(player.getUniqueId(), new PlayerStats(player.getUniqueId()));

        if (this.activePlayers.size() >= 2) {
            if (this.mode == GameSpleef.Mode.READY) {
                this.mode = GameSpleef.Mode.COUNTDOWN;
                if (countTask == null)
                    countTask = Task.builder().execute(new StartingTimerTask()).interval(1, TimeUnit.SECONDS).name("Game timer").submit(GameSpleef.getInstance());
            }
        }
    }

    @Override
    public void leavePlayer(Player player, boolean resetStats) {
        if (this.activePlayers.size() <= 1) {
            resetGame();
        }
        this.activePlayers.remove(player.getUniqueId());
        player.getInventory().clear();

        if (this.saveInventories) {
            List<Optional<ItemStack>> items = this.inventories.get(player.getUniqueId());
            int index = 0;
            for (Inventory slot : player.getInventory().slots())  {
                slot.set(items.get(index).orElse(ItemStack.empty()));
                index++;
            }
        }

        player.offer(Keys.GAME_MODE, GameModes.SURVIVAL);
        player.offer(Keys.POTION_EFFECTS, new ArrayList<>());
        player.setScoreboard(null);
        player.setTransform(this.getLobby());
    }

    @Override
    public boolean containsPlayer(Player player) {
        return this.activePlayers.containsKey(player.getUniqueId());
    }

    @Override
    public boolean addBreakBlock(Player player, BlockSnapshot targetBlock) {
        if (this.mode != GameSpleef.Mode.PLAYING) return false;
        if (targetBlock.getState().equals(BlockTypes.AIR.getDefaultState())) return false;
        PlayerStats playerStats = this.getPlayerStats(player).get();
        playerStats.addBlocksBroken(1);

        targetBlock.getLocation().get().getExtent().setBlockType(targetBlock.getLocation().get().getBlockPosition(), BlockTypes.AIR);
        brokenBlocks.put(targetBlock, player.getUniqueId());
        return true;
    }

    @Override
    public void toggleStatus() {
        this.mode = this.mode.equals(GameSpleef.Mode.READY) ? GameSpleef.Mode.DISABLED : GameSpleef.Mode.READY;
    }

    @Override
    public void startGame() {
        this.mode = GameSpleef.Mode.PLAYING;
        this.inactivePlayers = new HashMap<>();
        for (UUID uuid : this.activePlayers.keySet()) {
            Sponge.getServer().getPlayer(uuid).get().offer(Keys.GAME_MODE, GameModes.SURVIVAL);
        }

        if (this.activePlayers.size() <= campPlayers) {
            if (campTask == null) {
                campTask = Task.builder().execute(new CampTimerTask()).interval(this.campInterval, TimeUnit.SECONDS).name("Game timer").submit(GameSpleef.getInstance());
            }
        }
    }

    @Override
    public void checkPlayerFall(Player player) {
        if (player.getLocation().getBlockY() > this.lowestY) {
            return;
        }
        if (this.mode == GameSpleef.Mode.PLAYING) {
            this.killPlayer(player);
        } else {
            this.activePlayers.remove(player.getUniqueId());
            player.setTransform(this.getSpawn());
            this.activePlayers.put(player.getUniqueId(), new PlayerStats(player.getUniqueId()));
        }
    }

    @SuppressWarnings("static-access")
	@Override
    public void killPlayer(Player player) {
        for (BlockSnapshot bs : this.brokenBlocks.keySet()) {
            if (bs.getLocation().isPresent()) {
                if (bs.getLocation().get().getBlockPosition().getX() == player.getLocation().getBlockPosition().getX()
                        && bs.getLocation().get().getBlockPosition().getZ() == player.getLocation().getBlockPosition().getZ()
                        && bs.getLocation().get().getBlockPosition().getY() == this.lowestY) {
                    Player killer = Sponge.getServer().getPlayer(this.brokenBlocks.get(bs)).get();
                    getPlayerStats(killer).get().addKnockout(player);
                    player.sendMessage(
                    		TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("plugin-prefix") + plugin.loc.getString("game.knock-out.1").replace("%killer%", killer.getName()))
                    );
                }
            }
        }
        this.inactivePlayers.put(player.getUniqueId(), this.activePlayers.get(player.getUniqueId()));
        this.activePlayers.remove(player.getUniqueId());
        player.getInventory().clear();
        player.offer(Keys.GAME_MODE, GameModes.SURVIVAL);
        player.offer(Keys.POTION_EFFECTS, new ArrayList<>());
        player.offer(Keys.FOOD_LEVEL, 20);
        player.offer(Keys.HEALTH, player.get(Keys.MAX_HEALTH).get());
        player.setScoreboard(null);
        player.setTransform(this.getLobby());
        if (this.activePlayers.size() <= 1) {
            Sponge.getServer().getBroadcastChannel().send(
            		TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("plugin-prefix") + plugin.loc.getString("game.win").replace("%player%", Sponge.getServer().getPlayer(this.activePlayers.keySet().iterator().next()).get().getName()).replace("%arena%", this.name))
            );
            resetGame();
        }
        if (this.activePlayers.size() <= campPlayers) {
            if (campTask == null) {
                campTask = Task.builder().execute(new CampTimerTask()).interval(this.campInterval, TimeUnit.SECONDS).name("Game timer").submit(GameSpleef.getInstance());
            }
        }
    }

    @SuppressWarnings("static-access")
	@Override
    public void resetGame() {
        if (this.mode == GameSpleef.Mode.PLAYING) {
            for (UUID playerUid : this.activePlayers.keySet()) {
                Player player = Sponge.getServer().getPlayer(playerUid).get();
                player.getInventory().clear();
                player.offer(Keys.GAME_MODE, GameModes.SURVIVAL);
                player.offer(Keys.POTION_EFFECTS, new ArrayList<>());
                player.offer(Keys.FOOD_LEVEL, 20);
                player.offer(Keys.HEALTH, player.get(Keys.MAX_HEALTH).get());
                player.setScoreboard(null);
                player.setTransform(this.getLobby());
                this.inactivePlayers.put(playerUid, this.activePlayers.get(playerUid));
            }


            for (BlockSnapshot bs : this.brokenBlocks.keySet()) {
                bs.restore(true, BlockChangeFlags.NONE);
            }
            this.brokenBlocks.clear();
            this.mode = GameSpleef.Mode.READY;

            UUID max_kills_player = null;
            double max_kills = -1;

            for (UUID playerUid : this.inactivePlayers.keySet()) {
                double k = this.inactivePlayers.get(playerUid).getKnockouts().size();
                Sponge.getServer().getPlayer(playerUid).get().sendMessage(
                		TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("plugin-prefix") + plugin.loc.getString("game.knock-out.2").replace("%players%", String.valueOf((int) k)).replace("", String.valueOf((int)(k/(this.inactivePlayers.size()-1) * 100.0))))
                );
                if (k > max_kills) {
                    max_kills = k;
                    max_kills_player = playerUid;
                }
            }

            UUID max_breaks_player = null;
            double max_breaks = -1;
            int totalBlocks = 0;
            for (AABB aabb : this.floors)
                totalBlocks += (aabb.getSize().getFloorX() * aabb.getSize().getFloorZ());

            for (UUID playerUid : this.inactivePlayers.keySet()) {
                double i = this.inactivePlayers.get(playerUid).getBlocksBroken();
                Sponge.getServer().getPlayer(playerUid).get().sendMessage(
                		TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("plugin-prefix") + plugin.loc.getString("game.broke.1").replace("%blocks%", String.valueOf((int) i)).replace("%percent%", String.valueOf((int)(i/totalBlocks * 100.0))))
                );
                if (i > max_breaks) {
                    max_breaks = i;
                    max_breaks_player = playerUid;
                }
            }

            for (UUID playerUid : this.inactivePlayers.keySet()) {
                Sponge.getServer().getPlayer(playerUid).get().sendMessage(
                        Text.of(TextColors.GRAY, "-------------------------------------------------")
                );
                Sponge.getServer().getPlayer(playerUid).get().sendMessage(
                		TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("game.knock-out.2").replace("%player%", Sponge.getServer().getPlayer(max_kills_player).get().getName()).replace("%max%", String.valueOf((int)max_kills)).replace("%percent%", String.valueOf((int)(max_kills/(this.inactivePlayers.size()-1)*100.0))))
                );
                Sponge.getServer().getPlayer(playerUid).get().sendMessage(
                		TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("game.broke.2").replace("%player%", Sponge.getServer().getPlayer(max_breaks_player).get().getName()).replace("%max%", String.valueOf((int)max_breaks)).replace("%percent%", String.valueOf((int)(max_breaks/totalBlocks*100.0))))
                );
                Sponge.getServer().getPlayer(playerUid).get().sendMessage(
                        Text.of(TextColors.GRAY, "-------------------------------------------------")
                );
            }
        }

        this.mode = GameSpleef.Mode.READY;
        this.inactivePlayers = new HashMap<>();
        this.activePlayers = new HashMap<>();
        this.playerPos = new HashMap<>();
        if (this.campTask != null)
            this.campTask.cancel();
        if (this.countTask != null)
            this.countTask.cancel();
        this.campTask = null;
        this.countTask = null;
    }

    @Override
    public boolean checkPlayerMoved(Optional<Player> player) {
        Vector3i l = this.playerPos.get(player.get().getUniqueId());
        Vector3i pl = player.get().getLocation().getBlockPosition();
        this.playerPos.put(player.get().getUniqueId(), pl);
        return l == null || (pl.getX() > l.getX() + campRadius || pl.getX() < l.getX() - campRadius || pl.getZ() > l.getZ() + campRadius || pl.getZ() < l.getZ() - campRadius);
    }

    private class CampTimerTask implements Consumer<Task> {

        HashSet<UUID> temp = new HashSet<>();

        @SuppressWarnings("static-access")
		@Override
        public void accept(Task task) {
            UUID[] array;

            if (mode != GameSpleef.Mode.PLAYING)
                return;

            for (int length = (array = activePlayers.keySet().toArray(new UUID[0])).length, i = 0; i < length; ++i) {
                UUID playerUid = array[i];
                Player player = Sponge.getServer().getPlayer(playerUid).get();
                player.offer(Keys.HEALTH, player.get(Keys.MAX_HEALTH).get());
                player.offer(Keys.FOOD_LEVEL, 20);
                if (!checkPlayerMoved(Sponge.getServer().getPlayer(playerUid))) {
                    temp.add(playerUid);
                    if (campWarnings.contains(playerUid)) {
                        player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("game.camp.1")));
                        killPlayer(player);
                        if (activePlayers.size() <= 1) {
                            campWarnings.clear();
                            return;
                        }
                    } else {
                        player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("game.camp.2")));
                    }
                }
            }
            campWarnings.clear();
            campWarnings.addAll(temp);
        }
    }

    private class StartingTimerTask implements Consumer<Task> {

        private int seconds = 16;

        @SuppressWarnings("static-access")
		@Override
        public void accept(Task task) {
            seconds--;
            if (seconds % 5 == 0 || seconds < 6) {
                for (UUID playerUid : activePlayers.keySet()) {
                    Sponge.getServer().getPlayer(playerUid).ifPresent(player -> player.sendMessage(
                    		TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("plugin-prefix") + plugin.loc.getString("game.camp.2").replace("%time%", String.valueOf(seconds)))
                    ));
                }
            }
            if (seconds <= 0) {
                startGame();
                task.cancel();
            }
        }
    }
}
