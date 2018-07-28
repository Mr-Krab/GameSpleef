package cloud.zeroprox.gamespleef.commands;

import cloud.zeroprox.gamespleef.game.IGame;
import cloud.zeroprox.gamespleef.GameSpleef;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.ArrayList;
import java.util.List;

public class ListCmd implements CommandExecutor {
	
	GameSpleef plugin;
	
	public ListCmd(GameSpleef plugin) {
		this.plugin = plugin;
	}

    @SuppressWarnings("static-access")
	@Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        List<Text> arenas = new ArrayList<>();
        for (IGame IGame : GameSpleef.getGameManager().iGames) {
            arenas.add(Text.builder(IGame.getName()).color(IGame.getMode() == GameSpleef.Mode.DISABLED ? TextColors.RED : TextColors.GREEN).onHover(TextActions.showText(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.list.hover-text").replace("%arena%", IGame.getName())))).onClick(TextActions.runCommand("/spleef join " + IGame.getName())).build());
        }

        PaginationList.builder()
                .title(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.list")))
                .padding(Text.of(TextColors.GOLD, "="))
                .contents(arenas)
                .build()
                .sendTo(src);
        return CommandResult.success();
    }
}
