/*
 * QueryReportManagerImpl.java
 *
 * COPYRIGHT  2008
 * THE REGENTS OF THE UNIVERSITY OF MICHIGAN
 * ALL RIGHTS RESERVED
 *
 * PERMISSION IS GRANTED TO USE, COPY, CREATE DERIVATIVE WORKS AND REDISTRIBUTE THIS
 * SOFTWARE AND SUCH DERIVATIVE WORKS FOR NONCOMMERCIAL EDUCATION AND RESEARCH
 * PURPOSES, SO LONG AS NO FEE IS CHARGED, AND SO LONG AS THE COPYRIGHT NOTICE
 * ABOVE, THIS GRANT OF PERMISSION, AND THE DISCLAIMER BELOW APPEAR IN ALL COPIES
 * MADE; AND SO LONG AS THE NAME OF THE UNIVERSITY OF MICHIGAN IS NOT USED IN ANY
 * ADVERTISING OR PUBLICITY PERTAINING TO THE USE OR DISTRIBUTION OF THIS SOFTWARE
 * WITHOUT SPECIFIC, WRITTEN PRIOR AUTHORIZATION.
 *
 * THIS SOFTWARE IS PROVIDED AS IS, WITHOUT REPRESENTATION FROM THE UNIVERSITY OF
 * MICHIGAN AS TO ITS FITNESS FOR ANY PURPOSE, AND WITHOUT WARRANTY BY THE
 * UNIVERSITY OF MICHIGAN OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT
 * LIMITATION THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE REGENTS OF THE UNIVERSITY OF MICHIGAN SHALL NOT BE LIABLE FOR ANY
 * DAMAGES, INCLUDING SPECIAL, INDIRECT, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, WITH
 * RESPECT TO ANY CLAIM ARISING OUT OF OR IN CONNECTION WITH THE USE OF THE SOFTWARE,
 * EVEN IF IT HAS BEEN OR IS HEREAFTER ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */
package tau.tac.adx.report.adn;

import com.google.common.eventbus.Subscribe;
import tau.tac.adx.AdxManager;
import tau.tac.adx.bids.BidInfo;
import tau.tac.adx.messages.AuctionMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Patrick Jordan, Lee Callender, Akshat Kaul
 * @author greenwald, Mariano Schain
 */
public class AdNetworkReportManagerImpl implements AdNetworkReportManager {
	/**
	 * {@link Logger}.
	 */
	private Logger log = Logger.getLogger(AdNetworkReportManagerImpl.class
			.getName());

	/**
	 * {@link AdNetworkReport}s, each matches and <b>AdNetwork</b>.
	 */
	private final Map<String, AdNetworkReport> adNetworkReports = new HashMap<String, AdNetworkReport>();

	/**
	 * The {@link AdNetworkReportSender}.
	 */
	private final AdNetworkReportSender adNetworkReportSender;

	/**
	 * Create a new publisher report manager
	 * 
	 * @param adNetworkReportSender
	 *            The {@link AdNetworkReportSender}.
	 */
	public AdNetworkReportManagerImpl(
			AdNetworkReportSender adNetworkReportSender) {
		this.adNetworkReportSender = adNetworkReportSender;
		AdxManager.getInstance().getSimulation().getEventBus().register(this);
		log.info("AdxQueryReportManager created.");
	}

	// ------------------------------------------------------------------------------------------------------
	/**
	 * @see tau.tac.adx.report.publisher.AdxPublisherReportManager#size()
	 */
	@Override
	public int size() {
		return adNetworkReports.size();
	}

	/**
	 * @param message
	 *            {@link AuctionMessage}.
	 */
	@Subscribe
	public void auctionPerformed(AuctionMessage message) {
		for (BidInfo bidInfo : message.getAuctionResult().getBidInfos()) {
			String participant = bidInfo.getBidder().getName();
			AdNetworkReport report = adNetworkReports.get(participant );
			if (report == null) {
				report = new AdNetworkReport();
				adNetworkReports.put(participant, report);
			}
			boolean hasWon = false;
			BidInfo winningBidInfo = message.getAuctionResult()
					.getWinningBidInfo();
			if (winningBidInfo != null) {
				hasWon = winningBidInfo.getBidder().getName()
						.equals(participant);
			}
			if (!bidInfo.getCampaign().getAdvertiser()
					.equals(bidInfo.getBidder().getName())) {
				log.log(Level.SEVERE, bidInfo.getBidder().getName()
						+ " placed a bid for campaign  #"
						+ bidInfo.getCampaign().getId() + " which belongs to "
						+ bidInfo.getCampaign().getAdvertiser());
			}
			report.addBid(message, bidInfo.getCampaign().getId(), hasWon);
		}
	}

	/**
	 * @see tau.tac.adx.report.publisher.AdxPublisherReportManager#sendReportsToAll()
	 */
	@Override
	public void sendReportsToAll() {
		for (Entry<String, AdNetworkReport> entry : adNetworkReports.entrySet()) {
			adNetworkReportSender.broadcastAdNetowrkReport(entry.getKey(),
					entry.getValue());
		}
		adNetworkReports.clear();
	}
	
	/**
	 * @return the adNetworkReports
	 */
	public Map<String, AdNetworkReport> getAdNetworkReports() {
		return adNetworkReports;
	}
}
