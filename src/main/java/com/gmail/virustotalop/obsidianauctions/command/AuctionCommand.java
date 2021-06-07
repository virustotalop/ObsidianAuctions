package com.gmail.virustotalop.obsidianauctions.command;

import com.github.ravenlab.commander.command.Command;
import com.github.ravenlab.commander.command.CommandArgs;
import com.github.ravenlab.commander.command.CommanderCommand;
import org.bukkit.command.CommandSender;

@Command(value = "auction", aliases = {"auc", "sauc"})
public class AuctionCommand extends CommanderCommand<CommandSender> {
    @Override
    public boolean execute(CommandSender sender, String name, CommandArgs args) {
        return true;
    }
}
