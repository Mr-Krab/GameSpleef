package cloud.zeroprox.gamespleef.commands;

import cloud.zeroprox.gamespleef.GameSpleef;
import cloud.zeroprox.gamespleef.game.IGame;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Optional;

public class LeaveCmd implements CommandExecutor {
	
	GameSpleef plugin;
	
	public LeaveCmd(GameSpleef plugin) {
		this.plugin = plugin;
	}
	
    @SuppressWarnings("static-access")
	@Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            throw new CommandException(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.only-player")));
        }
        Player player = (Player) src;

        Optional<IGame> game = GameSpleef.getGameManager().getPlayerGame(player);
        if (!game.isPresent()) {
            throw new CommandException(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.leave.not-in-a-game")));
        }

        game.get().leavePlayer(player, args.hasAny("f"));

        return CommandResult.success();
    }
}
