package cloud.zeroprox.gamespleef.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.serializer.TextSerializers;

import cloud.zeroprox.gamespleef.GameSpleef;

import java.util.Arrays;

public class AdminCmd implements CommandExecutor {
	
	GameSpleef plugin;
	
	public AdminCmd(GameSpleef plugin) {
		this.plugin = plugin;
	}

    @SuppressWarnings("static-access")
	@Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        PaginationList.builder()
                .title(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.admin-cmd.title")))
                .contents(
                        Arrays.asList(
                                Text.builder(plugin.deserializer(plugin.loc.getString("commands.admin-cmd.content.1"))).onClick(TextActions.suggestCommand("/spleef admin build name <name>")).build(),
                                Text.builder(plugin.deserializer(plugin.loc.getString("commands.admin-cmd.content.2"))).onClick(TextActions.suggestCommand("/spleef admin build stop")).build(),
                                Text.builder(plugin.deserializer(plugin.loc.getString("commands.admin-cmd.content.3"))).onClick(TextActions.runCommand("/spleef admin build")).build(),
                                Text.builder(plugin.deserializer(plugin.loc.getString("commands.admin-cmd.content.4"))).onClick(TextActions.suggestCommand("/spleef admin toggle <name>")).build(),
                                Text.builder(plugin.deserializer(plugin.loc.getString("commands.admin-cmd.content.5"))).onClick(TextActions.suggestCommand("/spleef admin clearstats <name>")).build(),
                                Text.builder(plugin.deserializer(plugin.loc.getString("commands.admin-cmd.content.6"))).onClick(TextActions.suggestCommand("/spleef admin remove <name>")).build()
                                )
                )
                .build()
        .sendTo(src);
        return CommandResult.success();
    }
}
