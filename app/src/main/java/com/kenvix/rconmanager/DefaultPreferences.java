package com.kenvix.rconmanager;

import static com.kenvix.rconmanager.utils.Invoker.getString;

public final class DefaultPreferences {
    public static final String KeyCommandPrompt = "command_prompt";
    public static final String DefaultCommandPrompt = getString(R.string.result_prompt);
    //"λ > "

    public static final String KeyTerminalTextSize = "terminal_text_size";
    public static final String DefaultTerminalTextSize = "14";
}
