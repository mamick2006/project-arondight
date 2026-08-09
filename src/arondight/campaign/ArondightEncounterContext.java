package arondight.campaign;

import java.util.List;

import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;

public class ArondightEncounterContext extends FleetEncounterContext {

	@Override
	public void calculateAndApplyCrewLosses(EngagementResultForFleetAPI result, boolean playerInvolved) {
		List<FleetMemberAPI> lifted = ArondightSoloBattle.liftTransactionMothballs();
		try {
			super.calculateAndApplyCrewLosses(result, playerInvolved);
		} finally {
			ArondightSoloBattle.reapplyTransactionMothballs(lifted);
		}
	}
}
