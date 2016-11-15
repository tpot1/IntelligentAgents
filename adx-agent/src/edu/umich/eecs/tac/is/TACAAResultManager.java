package edu.umich.eecs.tac.is;

/**
 * @author Patrick Jordan, Lee Callender
 */

import com.botbox.html.HtmlWriter;
import edu.umich.eecs.tac.Participant;
import se.sics.isl.util.FormatUtils;
import se.sics.tasim.is.common.InfoServer;
import se.sics.tasim.is.common.ResultManager;
import se.sics.tasim.logtool.LogReader;
import se.sics.tasim.logtool.ParticipantInfo;
import tau.tac.adx.TACAdxSimulationInfo;
import tau.tac.adx.props.AdxInfoContextFactory;
import tau.tac.adx.sim.TACAdxConstants;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;


public class TACAAResultManager extends ResultManager {

	private static final Logger log = Logger.getLogger(TACAAResultManager.class
			.getName());

	private static final String POSITIVE_PARTICIPANT_COLOR = "#0000c0";
	private static final String NEUTRAL_PARTICIPANT_COLOR = null;
	private static final String NEGATIVE_PARTICIPANT_COLOR = "#c00000";

	public TACAAResultManager() {
	}

	protected void generateResult() throws IOException {
		TACAdxSimulationInfo simInfo;
		LogReader reader = getLogReader();
		int simulationID = reader.getSimulationID();
		String serverName = reader.getServerName();
		reader.setContext(new AdxInfoContextFactory().createContext());
		try {
			simInfo = new TACAdxSimulationInfo(reader);
		} catch (Exception e) {
			throw (IOException) new IOException(
                    "could not parse simulation log "+simulationID)
					.initCause(e);
		}

		// go through the whole logfile and find the scores of the agents...

		String destinationFile = getDestinationPath() + "index.html";

		log.info("generating results for simulation " + simulationID + " to "
				+ destinationFile);

		HtmlWriter html = new HtmlWriter(new FileWriter(destinationFile));

		Participant[] participants = simInfo
				.getParticipantsByRole(TACAdxConstants.AD_NETOWRK_ROLE_ID);

		if (participants != null) {
			participants = participants.clone();
			Arrays.sort(participants, Participant.getResultComparator());
		}

		html.pageStart("Results for game " + simulationID + '@' + serverName);

		html.h3("Result for game " + simulationID + '@' + serverName
				+ " played at "
				// Should not access InfoServer! FIX THIS!!!
				+ InfoServer.getServerTimeAsString(reader.getStartTime()));

		html.table("border=1").colgroup(1).colgroup(11, "align=right").tr().th(
				"Player").th("Revenue", "align=center").th("ADX Cost", "align=center")
				.th("UCS Cost", "align=center")
				.th("Impressions", "align=center").th("Result", "align=center");

		ParticipantInfo[] agentInfos = null;
		String[] agentColors = null;
		double[] agentScores = null;
		if (participants != null) {
			agentInfos = new ParticipantInfo[participants.length];
			agentColors = new String[participants.length];
			agentScores = new double[participants.length];
			for (int i = 0, n = participants.length; i < n; i++) {
				Participant player = participants[i];
				ParticipantInfo agentInfo = player.getInfo();
				String name = agentInfo.getName();
				double adxCost = player.getADXCost();
				double ucsCost = player.getUCSCost();
				double revenue = player.getRevenue();
				double result = player.getResult();

				long impressions = player.getImpressions();

				agentInfos[i] = agentInfo;
				if (result < 0) {
					agentColors[i] = NEGATIVE_PARTICIPANT_COLOR;
				} else if (result > 0) {
					agentColors[i] = POSITIVE_PARTICIPANT_COLOR;
				} else {
					agentColors[i] = NEUTRAL_PARTICIPANT_COLOR;
				}

				agentScores[i] = result;
				html.tr().td(
						agentInfo.isBuiltinAgent() ? "<em>" + name + "</em>"
								: name).td(getAmountAsString(revenue)).td(
						getAmountAsString(adxCost)).td(getAmountAsString(ucsCost)).td(
						getAmountAsString(impressions));

				html.td();
				formatAmount(html, result);
			}
		}

		html.tableEnd();

		html.text("Download game data ").tag('a')
				.attr("href", getGameLogName()).text("here").tagEnd('a').p();

		/*
		 * html.table("border=1").colgroup(1).colgroup(2,
		 * "align=right").colgroup(2).colgroup(1,
		 * "align=right").tr().th("Player").th("Orders",
		 * "align=center").th("Utilization",
		 * "align=center").th("Deliveries (on&nbsp;time/late/missed)",
		 * "colspan=2").th("DPerf", "align=center");
		 * 
		 * if (participants != null) { for (int i = 0, n = participants.length;
		 * i < n; i++) { Participant player = participants[i]; ParticipantInfo
		 * agentInfo = player.getInfo(); String name = agentInfo.getName();
		 * 
		 * int orders = player.getCustomerOrders(); int deliveries =
		 * player.getCustomerDeliveries();
		 * html.tr().td(agentInfo.isBuiltinAgent() ? "<em>" + name + "</em>" :
		 * name).td("" + orders) .td("" +
		 * player.getAverageUtilization()).text('%').td(); if (orders > 0) { int
		 * lateDeliveries = player.getCustomerLateDeliveries(); int
		 * missedDeliveries = player.getCustomerMissedDeliveries();
		 * 
		 * int onTime = (int) (100d (deliveries - lateDeliveries) / orders +
		 * 0.5); int late = (int) (100d lateDeliveries / orders + 0.5); int
		 * missed = (int) (100d missedDeliveries / orders + 0.5);
		 * HtmlUtils.progress(html, 100, 8, onTime, late, missed);
		 * 
		 * html.td().text(deliveries -
		 * lateDeliveries).text("&nbsp;/&nbsp;").text(lateDeliveries)
		 * .text("&nbsp;/&nbsp;").text(missedDeliveries); html.td("" + onTime +
		 * '%'); // html.td().text(onTime).text("%&nbsp;/&nbsp;") //
		 * .text(late).text("%&nbsp;/&nbsp;") // .text(missed).text('%'); } else
		 * { html.text("&nbsp;").td("&nbsp;").td("0%"); } } } html.tableEnd();
		 */
		html.p();

		/*
		 * html.table("border=1").tr().th("Simulation Parameters",
		 * "colspan=2").tr().td("Simulation:").td( "" + simulationID + " (" +
		 * simInfo.getSimulationType() + ')').tr().td("Server:").td( serverName
		 * + " (" + simInfo.getServerVersion() + ')'); // ServerConfig sConfig =
		 * simInfo.getServerConfig(); // if (sConfig != null) { // } int
		 * bankDebtInterestRate = simInfo.getBankDebtInterestRate(); int
		 * bankDepositInterestRate = simInfo.getBankDepositInterestRate(); if
		 * (bankDepositInterestRate >= 0 && bankDebtInterestRate >= 0) {
		 * html.tr().td("Bank interest (debt/deposit):").td( "" +
		 * bankDebtInterestRate + "% / " + bankDepositInterestRate + '%'); } int
		 * storageCost = simInfo.getStorageCost(); if (storageCost >= 0) {
		 * html.tr().td("Storage Cost:").td("" + storageCost + '%'); }
		 * 
		 * html.tableEnd();
		 */

		html.p().tag("hr");

		html.pageEnd();
		html.close();

		addSimulationToHistory(agentInfos, agentColors);
		addSimulationResult(agentInfos, agentScores);
	}

	private String getAmountAsString(double amount) {
		return FormatUtils.formatDouble(amount, "&nbsp;");

		// String text = Long.toString(amount);
		// int length = text.length();
		// if (length > 3) {
		// StringBuffer sb = new StringBuffer();
		// int pos = 0;
		// int part = length % 3;
		// if (part > 0) {
		// sb.append(text.substring(0, part));
		// pos += part;
		// }
		// while (pos < length) {
		// if (pos > 1 || (pos == 1 && text.charAt(0) != '-')) {
		// sb.append("&nbsp;");
		// }
		// sb.append(text.substring(pos, pos + 3));
		// pos += 3;
		// }
		// text = sb.toString();
		// }
		// return text;
	}

	private void formatAmount(HtmlWriter html, double amount) {
		formatAmount(html, amount, "");
	}

	private void formatAmount(HtmlWriter html, double amount, String postfix) {
		if (amount < 0) {
			html.tag("font", "color=red").text(getAmountAsString(amount)).text(
					postfix).tagEnd("font");
		} else {
			html.text(getAmountAsString(amount)).text(postfix);
		}
	}
}
