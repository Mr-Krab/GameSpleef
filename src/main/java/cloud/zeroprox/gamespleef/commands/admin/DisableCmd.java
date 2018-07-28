package cloud.zeroprox.gamespleef.commands.admin;

import cloud.zeroprox.gamespleef.GameSpleef;
import cloud.zeroprox.gamespleef.game.IGame;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Optional;

public class DisableCmd implements CommandExecutor {
	
	GameSpleef plugin;
	
	public DisableCmd(GameSpleef plugin) {
		this.plugin = plugin;
	}

    @SuppressWarnings("static-access")
	@Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Optional<String> gameName = args.getOne(Text.of("game"));
        if (!gameName.isPresent()) {
            throw new CommandException(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.disable.1")));
        }
        Optional<IGame> gameOptional = GameSpleef.getGameManager().getGame(gameName.get());
        gameOptional.ifPresent(game -> game.toggleStatus());
        src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.disable.2")));
        return CommandResult.success();
    }
}
