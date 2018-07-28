package cloud.zeroprox.gamespleef.commands;

import cloud.zeroprox.gamespleef.GameSpleef;
import cloud.zeroprox.gamespleef.game.IGame;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Optional;

public class JoinCmd implements CommandExecutor {
	
	GameSpleef plugin;
	
	public JoinCmd(GameSpleef plugin) {
		this.plugin = plugin;
	}

    @SuppressWarnings("static-access")
	@Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            throw new CommandException(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.only-player")));
        }
        Player player = (Player) src;

        /*if (player.getInventory().totalItems() != 0) {
            throw new CommandException(Text.of(TextColors.RED, "You need a empty inventory to join"));
        }
        */
        String gameName = args.<String>getOne(Text.of("game")).orElse(GameSpleef.getGameManager().getDefaultName());
        Optional<IGame> game = GameSpleef.getGameManager().getGame(gameName);
        if (!game.isPresent()) {
            src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.join.game-not-found").replace("%name%", gameName)));
            return CommandResult.empty();
        }
        game.get().addPlayer(player);
        return CommandResult.success();
    }
}
