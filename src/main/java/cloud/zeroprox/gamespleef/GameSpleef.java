package cloud.zeroprox.gamespleef;

import cloud.zeroprox.gamespleef.commands.*;
import cloud.zeroprox.gamespleef.commands.admin.BuildCmd;
import cloud.zeroprox.gamespleef.commands.admin.DisableCmd;
import cloud.zeroprox.gamespleef.commands.admin.KickCmd;
import cloud.zeroprox.gamespleef.commands.admin.RemoveCmd;
import cloud.zeroprox.gamespleef.game.GameClassic;
import cloud.zeroprox.gamespleef.game.GameManager;
import cloud.zeroprox.gamespleef.game.IGame;
import cloud.zeroprox.gamespleef.utils.AABBSerialize;
import cloud.zeroprox.gamespleef.utils.GameSerialize;
import cloud.zeroprox.gamespleef.utils.Locale;
import cloud.zeroprox.gamespleef.utils.TransformWorldSerializer;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.*;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Plugin(id = "gamespleef", name = "GameSpleef", description = "A spleef minigame", url = "https://zeroprox.cloud", authors = {"ewoutvs_", "Alagild"})
public class GameSpleef {

    @Inject
    private Logger logger;
    private static GameSpleef instance;
    private static GameManager gameManager;
    public static Locale loc;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private Path defaultConfig;
    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;
    Path config;
    
    @Inject
    @ConfigDir(sharedRoot = false)
    public File configFile;

    private ConfigurationNode rootNode;
	
	public MessageChannel consoleMessage(){
		return MessageChannel.TO_CONSOLE;
	}
	
	public String deserializer(String string) {
		return TextSerializers.formattingCode('§').serialize(TextSerializers.FORMATTING_CODE.deserialize(string));
	}

    CommandSpec joinCmd = CommandSpec.builder()
            .description(Text.of("Join a game"))
            .arguments(
                    GenericArguments.optional(new GameArgument(Text.of("game")))
            )
            .permission("gamespleef.join")
            .executor(new JoinCmd(this))
            .build();

    CommandSpec leaveCmd = CommandSpec.builder()
            .description(Text.of("Leave game"))
            .arguments(GenericArguments.flags().flag("f").buildWith(GenericArguments.none()))
            .permission("gamespleef.leave")
            .executor(new LeaveCmd(this))
            .build();

    CommandSpec adminBuildCmd = CommandSpec.builder()
            .description(Text.of("Build"))
            .arguments(
                    GenericArguments.optional(GenericArguments.enumValue(Text.of("type"), AdminBuildTypes.class)),
                    GenericArguments.optional(GenericArguments.string(Text.of("name")))
            )
            .executor(new BuildCmd(this))
            .build();

    CommandSpec adminToggleCmd = CommandSpec.builder()
            .description(Text.of("Toggle arena"))
            .arguments(
                    GenericArguments.optional(new GameArgument(Text.of("game")))
            )
            .executor(new DisableCmd(this))
            .build();

    CommandSpec adminRemoveCmd = CommandSpec.builder()
            .description(Text.of("Remove arena"))
            .arguments(
                    GenericArguments.optional(new GameArgument(Text.of("game")))
            )
            .executor(new RemoveCmd(this))
            .build();

    CommandSpec adminKickCmd = CommandSpec.builder()
            .description(Text.of("Kick player"))
            .arguments(GenericArguments.onlyOne(GenericArguments.player(Text.of("player"))))
            .executor(new KickCmd(this))
            .build();

    CommandSpec adminCmd = CommandSpec.builder()
            .description(Text.of("Area management"))
            .permission("gamespleef.admin")
            .executor(new AdminCmd(this))
            .child(adminBuildCmd, "build")
            .child(adminToggleCmd, "toggle")
            .child(adminRemoveCmd, "remove")
            .child(adminKickCmd, "kick")
            .build();

    CommandSpec listCmd = CommandSpec.builder()
            .description(Text.of("Show game list"))
            .executor(new ListCmd(this))
            .permission("gamespleef.join")
            .build();

    CommandSpec spleefCmd = CommandSpec.builder()
            .description(Text.of("Main command"))
            .child(joinCmd, "join")
            .child(leaveCmd, "leave")
            .child(listCmd, "list")
            .child(adminCmd, "admin")
            .executor(new HelpCmd(this))
            .build();
    
    
    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        Sponge.getCommandManager().register(this, spleefCmd, "gamespleef", "spleef");
        Sponge.getEventManager().registerListeners(this, new Listeners());
        TypeToken<Transform<World>> transformTypeToken = new TypeToken<Transform<World>>() {};
        TypeSerializers.getDefaultSerializers().registerType(transformTypeToken, new TransformWorldSerializer());
        gameManager = new GameManager();
        instance = this;
        config = configDir.resolve("config.conf");
        configManager = HoconConfigurationLoader.builder().setPath(config).build();
        saveConfig();
        try {
            rootNode = configManager.load();
            loadConfig();
        } catch(IOException e) {
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }
        checkConfigVersion();
        loc = new Locale(this, String.valueOf(rootNode.getNode("Lang").getValue()));
		try {
			loc.init();
		} catch (IOException e) {
			e.printStackTrace();
		}
		checkLocaleVersion();
    }
	
	public void checkConfigVersion() {
		Asset asset = Sponge.getAssetManager().getAsset(this, "config.conf").get();
		File newFile = new File(configDir + File.separator + "ConfigOld.txt");
		if (!rootNode.getNode("ConfigVersion").isVirtual()) {
			if ((Integer) rootNode.getNode("ConfigVersion").getValue(Integer.class) != 1) {
				consoleMessage().send(TextSerializers.FORMATTING_CODE.deserialize("&eAttention!!! The version of your configuration file does not match the current one!"));
				config.toFile().renameTo(newFile);
				consoleMessage().send(TextSerializers.FORMATTING_CODE.deserialize("&aYour config has been replaced with the default config. Old config see in the file ConfigOld.txt."));
				try {
					asset.copyToFile(config);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			consoleMessage().send(TextSerializers.FORMATTING_CODE.deserialize("&eAttention!!! The version of your configuration file does not match the current one!"));
			config.toFile().renameTo(newFile);
			consoleMessage().send(TextSerializers.FORMATTING_CODE.deserialize("&aYour config has been replaced with the default config. Old config see in the file ConfigOld.txt."));
			try {
				asset.copyToFile(config);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
        try {
            loadConfig();
        } catch(IOException e) {
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }
	}
	
	public void checkLocaleVersion() {
		String current = "1";
		if (!loc.getString("locale.version").trim().equals(current)) {
			consoleMessage().send(TextSerializers.FORMATTING_CODE.deserialize(loc.getString("locale.version.old")));
		}
	}
	
	public void saveConfig() {
		Asset asset = Sponge.getAssetManager().getAsset(this, "config.conf").get();
		if (Files.notExists(config)) {
			try {
				asset.copyToFile(config);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

    public static GameSpleef getInstance() {
        return instance;
    }

    public static GameManager getGameManager() {
        return gameManager;
    }


    @Listener
    public void onGameReload(GameReloadEvent event) {
        try {
            loadConfig();
        } catch (IOException e) {
        } catch (ObjectMappingException e) {
        }
		try {
			loc.init();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    private void loadConfig() throws IOException, ObjectMappingException {
        if (rootNode.getNode("areas").isVirtual()) {
            logger.info("Creating configuration");

            rootNode.getNode("areas").setValue(new TypeToken<List<GameSerialize>>(){}, Arrays.asList());
            configManager.save(rootNode);
            loadConfig();
        } else {
            getGameManager().iGames.clear();
            List<GameSerialize> gameSerializeList = rootNode.getNode("areas").getList(TypeToken.of(GameSerialize.class));
            for (GameSerialize gameSerialize : gameSerializeList) {
                IGame iGame = null;

                List<AABB> floors = new ArrayList<>();
                for (AABBSerialize aabbSerialize : gameSerialize.floors) {
                    floors.add(aabbSerialize.toAABB());
                }

                if (gameSerialize.gameType == GameType.CLASSIC) {
                    iGame = new GameClassic(gameSerialize.name,
                            gameSerialize.area.toAABB(),
                            floors,
                            gameSerialize.spawn,
                            gameSerialize.lobby,
                            gameSerialize.playerLimit,
                            gameSerialize.campRadius,
                            gameSerialize.campInterval,
                            gameSerialize.campPlayers,
                            gameSerialize.saveInv
                    );
                }
                getGameManager().iGames.add(iGame);
            }
            logger.info("Loaded: " + getGameManager().iGames.size() + " games");
        }
    }

    public void addArena(GameSerialize gameSerialize) {
        try {
            List<GameSerialize> gameSerializeList = rootNode.getNode("areas").getList(TypeToken.of(GameSerialize.class));
            List<GameSerialize> gameList = new ArrayList<>();
            gameList.addAll(gameSerializeList);
            gameList.add(gameSerialize);
            rootNode.getNode("areas").setValue(new TypeToken<List<GameSerialize>>(){}, gameList);
            configManager.save(rootNode);
            loadConfig();
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeArena(IGame iGame) {
        try {
            List<GameSerialize> gameSerializeList = rootNode.getNode("areas").getList(TypeToken.of(GameSerialize.class));
            List<GameSerialize> gameList = new ArrayList<>();
            gameList.addAll(gameSerializeList);
            gameList.removeIf(gameSerialize -> gameSerialize.name.equalsIgnoreCase(iGame.getName()));
            rootNode.getNode("areas").setValue(new TypeToken<List<GameSerialize>>(){}, gameList);
            configManager.save(rootNode);
            loadConfig();
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public enum Mode {
        DISABLED, READY, COUNTDOWN, PLAYING
    }

    public enum GameType {
        CLASSIC
    }

    public enum AdminBuildTypes {
        NAME, LOBBY, SPAWN, CORNER_FLOOR_1, CORNER_FLOOR_2, CORNER_AREA_1, CORNER_AREA_2, SAVE, STOP, TYPE, SAVE_INV
    }

    public class GameArgument extends CommandElement {

        protected GameArgument(@Nullable Text key) {
            super(key);
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return args.next();
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            List<String> games = new ArrayList<>();
            for (IGame iGame : GameSpleef.getGameManager().iGames) {
                games.add(iGame.getName());
            }
            return games;
        }
    }

}
