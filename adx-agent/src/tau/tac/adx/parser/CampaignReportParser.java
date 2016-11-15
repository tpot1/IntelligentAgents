package tau.tac.adx.parser;

import edu.umich.eecs.tac.Parser;
import org.apache.commons.lang3.StringUtils;
import se.sics.isl.transport.Transportable;
import se.sics.tasim.logtool.LogReader;
import se.sics.tasim.logtool.ParticipantInfo;
import se.sics.tasim.props.SimulationStatus;
import tau.tac.adx.demand.CampaignStats;
import tau.tac.adx.report.demand.CampaignReport;
import tau.tac.adx.report.demand.CampaignReportEntry;
import tau.tac.adx.report.demand.CampaignReportKey;
import tau.tac.adx.sim.TACAdxConstants;

/**
 * <code>BankStatusParser</code> is a simple example of a TAC AA parser that
 * prints out all advertiser's BankStatus received in a simulation from the
 * simulation log file.
 * <p>
 * <p/>
 * The class <code>Parser</code> is inherited to provide base functionality for
 * TAC AA log processing.
 * 
 * @author - Lee Callender
 * 
 * @see edu.umich.eecs.tac.Parser
 */
public class CampaignReportParser extends Parser {

	private int day = 0;
	private final String[] participantNames;
	private final boolean[] is_Advertiser;
	private final ParticipantInfo[] participants;

	public CampaignReportParser(LogReader reader) {
		super(reader);

		// Print agent indexes/gather names
		System.out.println("****AGENT INDEXES****");
		participants = reader.getParticipants();
		if (participants == null) {
			throw new IllegalStateException("no participants");
		}
		int agent;
		participantNames = new String[participants.length];
		is_Advertiser = new boolean[participants.length];
		for (int i = 0, n = participants.length; i < n; i++) {
			ParticipantInfo info = participants[i];
			agent = info.getIndex();
			System.out.println(info.getName() + ": " + agent);
			participantNames[agent] = info.getName();
			if (info.getRole() == TACAdxConstants.ADVERTISER) {
				is_Advertiser[agent] = true;
			} else
				is_Advertiser[agent] = false;
		}

		System.out.println("****Campaign data***");
		System.out.println(StringUtils.rightPad("Day", 20) + "\t"
				+ StringUtils.rightPad("Agent", 20) + "\t"
				+ StringUtils.rightPad("Campaign ID", 20) + "\t"
				+ StringUtils.rightPad("Targeted", 20) + "\t"
				+ StringUtils.rightPad("Non Taregted", 20) + "\tCost");
	}

	@Override
	protected void dataUpdated(int type, Transportable content) {

	}

	@Override
	protected void message(int sender, int receiver, Transportable content) {
		if (content instanceof CampaignReport) {
			CampaignReport campaignReport = (CampaignReport) content;
			for (CampaignReportKey campaignReportKey : campaignReport) {
				CampaignReportEntry reportEntry = campaignReport
						.getEntry(campaignReportKey);
				CampaignStats campaignStats = reportEntry.getCampaignStats();
				System.out.println(StringUtils.rightPad("" + day, 20)
						+ "\t"
						+ StringUtils.rightPad(participantNames[receiver], 20)
						+ "\t #"
						+ StringUtils.rightPad(
								"" + campaignReportKey.getCampaignId(), 20)
						+ "\t"
						+ StringUtils.rightPad(
								"" + campaignStats.getTargetedImps(), 20)
						+ "\t"
						+ StringUtils.rightPad(
								"" + campaignStats.getOtherImps(), 20) + "\t"
						+ campaignStats.getCost());
			}

		} else if (content instanceof SimulationStatus) {
			SimulationStatus ss = (SimulationStatus) content;
			day = ss.getCurrentDate();
		}
	}
}
