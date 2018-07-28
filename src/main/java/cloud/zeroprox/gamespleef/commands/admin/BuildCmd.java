package cloud.zeroprox.gamespleef.commands.admin;

import cloud.zeroprox.gamespleef.GameSpleef;
import cloud.zeroprox.gamespleef.utils.AABBSerialize;
import cloud.zeroprox.gamespleef.utils.GameSerialize;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class BuildCmd implements CommandExecutor {
	
	GameSpleef plugin;
    GameSerialize gameSerialize;
	
	public BuildCmd(GameSpleef plugin) {
		this.plugin = plugin;
	}

    @SuppressWarnings("static-access")
	@Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            throw new CommandException(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.build.only-player")));
        }
        Player player = (Player)src;
        Optional<GameSpleef.AdminBuildTypes> adminOptional = args.getOne(Text.of("type"));
        if (!adminOptional.isPresent()) {
            showProgress(src);
            return CommandResult.empty();
        }
        GameSpleef.AdminBuildTypes adminType = adminOptional.get();
        if (adminType.equals(GameSpleef.AdminBuildTypes.SAVE)) {

            gameSerialize.area = new AABBSerialize(gameSerialize.corner_area_1.getBlockX(),
                    gameSerialize.corner_area_1.getBlockY(),
                    gameSerialize.corner_area_1.getBlockZ(),
                    gameSerialize.corner_area_2.getBlockX(),
                    gameSerialize.corner_area_2.getBlockY(),
                    gameSerialize.corner_area_2.getBlockZ());

            GameSpleef.getInstance().addArena(gameSerialize);

            src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.build.saved")));
            return CommandResult.success();
        }
        if (adminType.equals(GameSpleef.AdminBuildTypes.NAME)) {
            Optional<String> name = args.getOne(Text.of("name"));
            this.gameSerialize = new GameSerialize();
            this.gameSerialize.gameType = GameSpleef.GameType.CLASSIC;
            this.gameSerialize.name = name.orElse(new Random().nextLong() + "");
            this.gameSerialize.floors = new ArrayList<>();
            this.gameSerialize.campInterval = 7;
            this.gameSerialize.campPlayers = 5;
            this.gameSerialize.campRadius = 2;
            this.gameSerialize.saveInv = true;
            this.gameSerialize.playerLimit = 20;
            showProgress(src);
            return CommandResult.success();
        }
        if (adminType.equals(GameSpleef.AdminBuildTypes.STOP)) {
            this.gameSerialize = null;
            src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.build.stop")));
            return CommandResult.success();
        }

        switch (adminType) {
            case LOBBY:
                gameSerialize.lobby = player.getTransform();
                break;
            case SPAWN:
                gameSerialize.spawn = player.getTransform();
                break;
            case CORNER_FLOOR_1:
                gameSerialize.corner_floor_1 = player.getLocation();
                if (gameSerialize.corner_floor_2 != null) {
                    gameSerialize.floors.add(new AABBSerialize(
                            gameSerialize.corner_floor_1.getBlockX(),
                            gameSerialize.corner_floor_1.getBlockY(),
                            gameSerialize.corner_floor_1.getBlockZ(),
                            gameSerialize.corner_floor_2.getBlockX(),
                            gameSerialize.corner_floor_2.getBlockY(),
                            gameSerialize.corner_floor_2.getBlockZ()));
                    gameSerialize.corner_floor_1 = null;
                    gameSerialize.corner_floor_2 = null;
                }
                break;
            case CORNER_FLOOR_2:
                gameSerialize.corner_floor_2 = player.getLocation();
                if (gameSerialize.corner_floor_1 != null) {
                    gameSerialize.floors.add(new AABBSerialize(
                            gameSerialize.corner_floor_1.getBlockX(),
                            gameSerialize.corner_floor_1.getBlockY(),
                            gameSerialize.corner_floor_1.getBlockZ(),
                            gameSerialize.corner_floor_2.getBlockX(),
                            gameSerialize.corner_floor_2.getBlockY(),
                            gameSerialize.corner_floor_2.getBlockZ()));
                    gameSerialize.corner_floor_1 = null;
                    gameSerialize.corner_floor_2 = null;
                }
                break;
            case CORNER_AREA_1:
                gameSerialize.corner_area_1 = player.getLocation();
                break;
            case CORNER_AREA_2:
                gameSerialize.corner_area_2 = player.getLocation();
                break;
            case SAVE_INV:
                gameSerialize.saveInv = !gameSerialize.saveInv;
                break;
            default:
        }
        showProgress(src);
        return CommandResult.empty();
    }

    @SuppressWarnings("static-access")
	private void showProgress(CommandSource src) {
        List<Text> textArray = new ArrayList<>();
        if (gameSerialize == null) {
            textArray.add(Text.builder(plugin.deserializer(plugin.loc.getString("commands.build.progress.1"))).onClick(TextActions.suggestCommand("/spleef admin build NAME <name>")).build());
        } else {
            textArray.add(Text.builder(plugin.deserializer(plugin.loc.getString("commands.build.progress.2"))).append(Text.builder(gameSerialize.name).color(TextColors.GREEN).build()).build());
            textArray.add(Text.builder(plugin.deserializer(plugin.loc.getString("commands.build.progress.3"))).append(colorVariable(gameSerialize.lobby)).onClick(TextActions.runCommand("/spleef admin build LOBBY")).build());
            textArray.add(Text.builder(plugin.deserializer(plugin.loc.getString("commands.build.progress.4"))).append(Text.of(TextColors.GREEN, gameSerialize.saveInv)).append(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.build.progress.5"))).onClick(TextActions.runCommand("/spleef admin build SAVE_INV")).build());
            textArray.add(Text.builder(plugin.deserializer(plugin.loc.getString("commands.build.progress.6"))).append(colorVariable(gameSerialize.spawn)).onClick(TextActions.runCommand("/spleef admin build SPAWN")).build());
            if (gameSerialize.corner_area_1 != null && gameSerialize.corner_area_2 != null) {
                textArray.add(Text.builder(plugin.deserializer(plugin.loc.getString("commands.build.progress.7"))).append(Text.builder(plugin.deserializer(plugin.loc.getString("commands.build.progress.13"))).build()).build());
            } else {
                textArray.add(Text.builder(plugin.deserializer(plugin.loc.getString("commands.build.progress.7"))).append(colorVariable(gameSerialize.area)).build());
            }
            textArray.add(Text.builder(plugin.loc.getString("commands.build.progress.8") + "1").color((gameSerialize.corner_area_1 == null ? TextColors.RED : TextColors.AQUA)).onClick(TextActions.runCommand("/spleef admin build CORNER_AREA_1")).build());
            textArray.add(Text.builder(plugin.loc.getString("commands.build.progress.8") + "2").color((gameSerialize.corner_area_2 == null ? TextColors.RED : TextColors.AQUA)).onClick(TextActions.runCommand("/spleef admin build CORNER_AREA_2")).build());
            textArray.add(Text.builder(plugin.deserializer(plugin.loc.getString("commands.build.progress.9"))).append(colorVariable(gameSerialize.floors)).build());
            textArray.add(Text.builder(plugin.loc.getString("commands.build.progress.10") + "1").color((gameSerialize.corner_floor_1 == null ? TextColors.RED : TextColors.AQUA)).onClick(TextActions.runCommand("/spleef admin build CORNER_FLOOR_1")).build());
            textArray.add(Text.builder(plugin.loc.getString("commands.build.progress.10") + "2").color((gameSerialize.corner_floor_2 == null ? TextColors.RED : TextColors.AQUA)).onClick(TextActions.runCommand("/spleef admin build CORNER_FLOOR_2")).build());
            if (gameSerialize.corner_area_2 != null
                    && gameSerialize.corner_area_1 != null
                    && gameSerialize.floors.size() > 0
                    && gameSerialize.spawn != null
                    && gameSerialize.lobby != null) {
                textArray.add(Text.builder(plugin.deserializer(plugin.loc.getString("commands.build.progress.11"))).onClick(TextActions.runCommand("/spleef admin build SAVE")).build());
            }
        }
        PaginationList.builder()
                .title(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.build.title")))
                .contents(textArray)
                .build()
        .sendTo(src);
    }

    @SuppressWarnings("static-access")
	private Text colorVariable(Object object) {
        if (object == null) {
            return Text.builder(" --").color(TextColors.GREEN).build();
        } else if (object instanceof List) {
            int amount = ((List)object).size();
            return Text.builder(plugin.loc.getString("commands.build.progress.12") + amount).color((amount == 0 ? TextColors.RED: TextColors.GREEN)).build();
        } else {
            return Text.builder(" " + plugin.deserializer(plugin.loc.getString("commands.build.progress.13"))).build();
        }
    }
}
