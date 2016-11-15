/*
 * AdvertiserRateMetricsPanel.java
 * 
 * Copyright (C) 2006-2009 Patrick R. Jordan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.umich.eecs.tac.viewer.role.advertiser;

import edu.umich.eecs.tac.viewer.TACAASimulationPanel;
import edu.umich.eecs.tac.viewer.TACAAViewerConstants;
import edu.umich.eecs.tac.viewer.ViewAdaptor;
import se.sics.isl.transport.Transportable;
import se.sics.tasim.viewer.TickListener;
import tau.tac.adx.agents.CampaignData;
import tau.tac.adx.demand.CampaignStats;
import tau.tac.adx.report.demand.*;
import tau.tac.adx.sim.TACAdxConstants;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Patrick R. Jordan
 */
public class AdvertiserRateMetricsPanel extends JPanel {
	private final int agent;
	private final String advertiser;
	private final Set<AdNetworkDailyNotification> campaigns;

	private final boolean advertiserBorder;
	private JTextArea area;
	private int day;
	private CampaignData pendingCampaign;

	public AdvertiserRateMetricsPanel(int agent, String advertiser,
			TACAASimulationPanel simulationPanel, boolean advertiserBorder) {
		this.agent = agent;
		this.advertiser = advertiser;
		this.advertiserBorder = advertiserBorder;
		initialize();

		simulationPanel.addViewListener(new DataUpdateListener());
		campaigns = new HashSet<AdNetworkDailyNotification>();
		simulationPanel.addTickListener(new DayListener());
	}

	protected class DayListener implements TickListener {

		@Override
		public void tick(long serverTime) {
		}

		@Override
		public void simulationTick(long serverTime, int simulationDate) {
			day = simulationDate;
		}
	}

	private void initialize() {
		setLayout(new GridLayout(1, 1));
		setBackground(TACAAViewerConstants.CHART_BACKGROUND);

		area = new JTextArea();
		add(new JScrollPane(area));

	}

	public int getAgent() {
		return agent;
	}

	public String getAdvertiser() {
		return advertiser;
	}

	protected void updateCampaigns(
			AdNetworkDailyNotification adNetworkDailyNotification) {
		if ((pendingCampaign.getId() == adNetworkDailyNotification
				.getCampaignId())
				&& (adNetworkDailyNotification.getCostMillis() != 0)) {

			String message = "Day "
					+ adNetworkDailyNotification.getEffectiveDay()
					+ ": Campaign ID: " + pendingCampaign.getId() + "\tRange ["
					+ pendingCampaign.getDayStart() + ", "
					+ pendingCampaign.getDayEnd() + "]\t"
					+ pendingCampaign.getTargetSegment() + "\tReach: "
					+ pendingCampaign.getReachImps() + "\twon at cost (Millis)"
					+ adNetworkDailyNotification.getCostMillis();

			area.append(message);
			area.append("\r\n");
		}
	}

	protected void updateCampaigns(InitialCampaignMessage campaignMessage) {
		String message = "Day 0: Campaign ID: " + campaignMessage.getId()
				+ "\tRange [" + campaignMessage.getDayStart() + ", "
				+ campaignMessage.getDayEnd() + "]\t"
				+ campaignMessage.getTargetSegment() + "\tReach: "
				+ campaignMessage.getReachImps() + "\ttrecieved";
		area.append(message);
		area.append("\r\n");
	}

	private void updateCampaigns(CampaignReport campaignReport) {
		for (CampaignReportKey campaignKey : campaignReport.keys()) {
			int cmpId = campaignKey.getCampaignId();
			CampaignStats cstats = campaignReport.getCampaignReportEntry(
					campaignKey).getCampaignStats();
			String message = "Day " + day + ": Updating campaign " + cmpId
					+ " stats: " + cstats.getTargetedImps() + " tgtImps "
					+ cstats.getOtherImps() + " nonTgtImps. Cost of imps is "
					+ cstats.getCost();
			area.append(message);
			area.append("\r\n");
		}
	}

	protected void handleCampaignOpportunityMessage(
			CampaignOpportunityMessage com) {
		pendingCampaign = new CampaignData(com);
	}

	private class DataUpdateListener extends ViewAdaptor {

		@Override
		public void dataUpdated(final int agentId, final int type,
				final Transportable value) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					if (agentId == agent) {
						switch (type) {
						case TACAdxConstants.DU_DEMAND_DAILY_REPORT:
							updateCampaigns((AdNetworkDailyNotification) value);
							break;
						case TACAdxConstants.DU_INITIAL_CAMPAIGN:
							updateCampaigns((InitialCampaignMessage) value);
							break;
						}
					}

				}
			});

		}

		@Override
		public void dataUpdated(final int type, final Transportable value) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					switch (type) {
					case TACAdxConstants.DU_CAMPAIGN_OPPORTUNITY:
						handleCampaignOpportunityMessage((CampaignOpportunityMessage) value);
						break;
					}
				}
			});

		}
	}
}
