package org.popcraft.chunky.command.suggestion;

import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;

import java.util.ArrayList;
import java.util.List;

public class TrimModeSuggestionProvider implements ValueCompleter {
    private static final List<String> TRIM_MODES = List.of("inside", "outside");

    @Override
    public List<CommandCompletion> complete(final CommandContext context, final String currentInput) {
        final List<CommandCompletion> completions = new ArrayList<>();
        TRIM_MODES.forEach(mode -> {
            if (mode.contains(currentInput.toLowerCase())) {
                completions.add(CommandCompletion.of(mode));
            }
        });
        return completions;
    }
}
