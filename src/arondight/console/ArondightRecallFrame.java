package arondight.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class ArondightRecallFrame implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        ArondightRecall.Result result = ArondightRecall.run();
        Console.showMessage(result.message);
        return result.outcome == ArondightRecall.Outcome.NOT_FOUND
                ? CommandResult.ERROR : CommandResult.SUCCESS;
    }
}
