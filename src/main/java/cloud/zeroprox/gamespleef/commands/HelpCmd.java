package cloud.zeroprox.gamespleef.commands;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import cloud.zeroprox.gamespleef.GameSpleef;

public class HelpCmd implements CommandExecutor {
	
	GameSpleef plugin;
	
	public HelpCmd(GameSpleef plugin) {
		this.plugin = plugin;
	}
	
    @SuppressWarnings("static-access")
	@Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        PaginationList.builder()
                .title(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.help-cmd.title")))
                .padding(Text.of(TextColors.GOLD, "="))
                .contents(
                        Text.builder("/spleef join ").color(TextColors.GREEN).onHover(TextActions.showText(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.help-cmd.content.hover-text.1")))).append(Text.of(TextColors.WHITE, "[area]")).onClick(TextActions.suggestCommand("/spleef join ")).build(),
                        Text.builder("/spleef leave").color(TextColors.GREEN).onHover(TextActions.showText(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.help-cmd.content.hover-text.2")))).onClick(TextActions.runCommand("/spleef leave")).build(),
                        Text.builder("/spleef list").color(TextColors.GREEN).onHover(TextActions.showText(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.help-cmd.content.hover-text.3")))).onClick(TextActions.runCommand("/spleef list")).build(),
                        Text.builder("/spleef admin").color(src.hasPermission("gamespleef.admin") ? TextColors.GREEN : TextColors.RED).onHover(TextActions.showText(TextSerializers.FORMATTING_CODE.deserialize(plugin.loc.getString("commands.help-cmd.content.hover-text.4")))).onClick(TextActions.runCommand("/spleef admin")).build()
                )
                .build()
                .sendTo(src);
        return CommandResult.success();
    }
}
