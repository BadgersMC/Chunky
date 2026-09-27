package org.popcraft.chunky.command.suggestion;

public final class SuggestionProviders {
    public static final PatternSuggestionProvider PATTERNS;
    public static final ShapeSuggestionProvider SHAPES;
    public static final TrimModeSuggestionProvider TRIM_MODES;

    static {
        PATTERNS = new PatternSuggestionProvider();
        SHAPES = new ShapeSuggestionProvider();
        TRIM_MODES = new TrimModeSuggestionProvider();
    }

    private SuggestionProviders() {
    }
}
