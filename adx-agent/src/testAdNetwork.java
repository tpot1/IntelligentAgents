import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;
import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.demand.CampaignStats;
import tau.tac.adx.devices.Device;
import tau.tac.adx.props.AdxBidBundle;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.props.PublisherCatalogEntry;
import tau.tac.adx.props.ReservePriceInfo;
import tau.tac.adx.report.adn.AdNetworkKey;
import tau.tac.adx.report.adn.AdNetworkReport;
import tau.tac.adx.report.adn.AdNetworkReportEntry;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.report.demand.AdNetBidMessage;
import tau.tac.adx.report.demand.AdNetworkDailyNotification;
import tau.tac.adx.report.demand.CampaignOpportunityMessage;
import tau.tac.adx.report.demand.CampaignReport;
import tau.tac.adx.report.demand.CampaignReportKey;
import tau.tac.adx.report.demand.InitialCampaignMessage;
import tau.tac.adx.report.demand.campaign.auction.CampaignAuctionReport;
import tau.tac.adx.report.publisher.AdxPublisherReport;
import tau.tac.adx.report.publisher.AdxPublisherReportEntry;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BankStatus;

/**
 *
 * @author Mariano Schain
 * Test plug-in
 * 
 */
public class testAdNetwork extends Agent {

	private final Logger log = Logger
			.getLogger(testAdNetwork.class.getName());

	/**
	 * Basic simulation information. An agent should receive the {@link
	 * StartInfo} at the beginning of the game or during recovery.
	 */
	@SuppressWarnings("unused")
	private StartInfo startInfo;

	/**
	 * Messages received:
	 *
	 * We keep all the {@link CampaignReport campaign reports} delivered to the
	 * agent. We also keep the initialization messages {@link PublisherCatalog}
	 * and {@link InitialCampaignMessage} and the most recent messages and
	 * reports {@link CampaignOpportunityMessage}, {@link CampaignReport}, and
	 * {@link AdNetworkDailyNotification}.
	 */
	private final Queue<CampaignReport> campaignReports;
	private PublisherCatalog publisherCatalog;
	private InitialCampaignMessage initialCampaignMessage;
	private AdNetworkDailyNotification adNetworkDailyNotification;

	/**
	 * The addresses of server entities to which the agent should send the daily
	 * bids data
	 */
	private String demandAgentAddress;
	private String adxAgentAddress;

	/**
	 * we maintain a list of queries - each characterized by the web site (the
	 * publisher), the device type, the ad type, and the user market segment
	 */
	private AdxQuery[] queries;

	/**
	 * Information regarding the latest campaign opportunity announced
	 */
	private CampaignData pendingCampaign;

	/**
	 * We maintain a collection (mapped by the campaign id) of the campaigns won
	 * by our agent.
	 */
	private Map<Integer, CampaignData> myCampaigns;

	/**
	 * the bidBundle to be sent daily to the AdX
	 */
	private AdxBidBundle bidBundle;

	/**
	 * The current bid level for the user classification service
	 */
	private double ucsBid;
	
	/**
	 * Stores the previous POSITIVE UCS bid, to be used by the UCS Bidder 
	 * to determine the next bid (since bids of 0 throw off the bidder)
	 */
	private double prevUcsBid;
	
	private double ucsLevel;

	/**
	 * The targeted service level for the user classification service
	 */
	private double ucsTargetLevel;

	/**
	 * current day of simulation
	 */
	private int day;
	private String[] publisherNames;
	private CampaignData currCampaign;

	private double currrcurrProfit;

	/**
	 * Unused variable used to hold the daily publisher report.
	 */
	private AdxPublisherReport pubReport;
	private boolean verbose_printing = 		false;
	private boolean ucs_printing = 			true;
	private boolean contract_printing = 	true;
	private boolean impressions_printing = 	false;
	private boolean costs_printing = 		false;

	/**
	 * Keeps list of all currently running campaigns allocated to any agent.
	 */
	private List<CampaignData> postedCampaigns;
	private Map<Integer, Long> campaignWinningBids;
	/*
	 * Tracker Objects for ucs and imp bids
	 */
	private UCSBidTracker ucsTracker;
	private ImpTracker impTracker;
	private Map<Integer, CampaignStats> myCampaignStatsHistory;

	private double currQuality = 1.0;

	private double meanVidCoeff;
	private double meanMobCoeff;
	
	private double competing_index = 2.0;
	private static double COMPETING_INDEX_MAX = 4.0;
	private static double GREED = 1.2;
	private static double UCSScaler = 0.2;
	private long previous_campaign_bid = 0;

	private Map<Integer, Double> imps_competing_indicies;
	private Map<Integer, Integer> imps_previous_results;
	private static double IMP_GREED_LOSE = 1.3;
    private static double IMP_GREED_WIN = 1.6;
	private static double IMP_COMPETING_INDEX_DEFAULT = 1.0;
	private static double IMP_COMPETING_INDEX_MAX = 2.5; //TODO: Decide on this and change it so if it goes over max valid bid, just bids that
	private static double IMP_COMPETING_INDEX_MIN = 0.2;
	private static double IMP_RESULT_MODIFIER_LOSE_LOSE = +0.3; //TODO: Properly do these values
	private static double IMP_RESULT_MODIFIER_WIN_WIN = -0.2;
	private static double IMP_RESULT_MODIFIER_WIN_LOSE = +0.3;
	private static double IMP_RESULT_MODIFIER_LOSE_WIN = -0.2;

	private PIP PIPredictor;

	public testAdNetwork() {
		campaignReports = new LinkedList<CampaignReport>();
		postedCampaigns = new ArrayList<CampaignData>();
		campaignWinningBids = new HashMap<>();
		ucsTracker = new UCSBidTracker();
		impTracker = new ImpTracker();
		myCampaignStatsHistory = new HashMap<>();

		imps_competing_indicies = new HashMap<>();
		imps_previous_results = new HashMap<>(); // -1 is loss, 0 if none, +1 if win

		PIPredictor = new PIP();
	}

	@Override
	protected void messageReceived(Message message) {
		try {
			Transportable content = message.getContent();
			if (content instanceof InitialCampaignMessage) {
				handleInitialCampaignMessage((InitialCampaignMessage) content);
			} else if (content instanceof CampaignOpportunityMessage) {
				handleICampaignOpportunityMessage((CampaignOpportunityMessage) content);
			} else if (content instanceof CampaignReport) {
				handleCampaignReport((CampaignReport) content);
			} else if (content instanceof AdNetworkDailyNotification) {
				handleAdNetworkDailyNotification((AdNetworkDailyNotification) content);
			} else if (content instanceof AdxPublisherReport) {
				handleAdxPublisherReport((AdxPublisherReport) content);
			} else if (content instanceof SimulationStatus) {
				handleSimulationStatus((SimulationStatus) content);
			} else if (content instanceof PublisherCatalog) {
				handlePublisherCatalog((PublisherCatalog) content);
			} else if (content instanceof AdNetworkReport) {
				handleAdNetworkReport((AdNetworkReport) content);
			} else if (content instanceof StartInfo) {
				handleStartInfo((StartInfo) content);
			} else if (content instanceof BankStatus) {
				handleBankStatus((BankStatus) content);
			} else if(content instanceof CampaignAuctionReport) {
				hadnleCampaignAuctionReport((CampaignAuctionReport) content);
			} else if (content instanceof ReservePriceInfo) {
				 ((ReservePriceInfo)content).getReservePriceType();
			} else {
				System.out.println("UNKNOWN Message Received: " + content);
			}

		} catch (NullPointerException e) {
			this.log.log(Level.SEVERE,
					"Exception thrown while trying to parse message." + e + "\n" + "Content:" + message.getContent());
			return;
		}
	}

	private void hadnleCampaignAuctionReport(CampaignAuctionReport content) {
		// ingoring - this message is obsolete
	}

	private void handleBankStatus(BankStatus content) {
		currrcurrProfit = content.getAccountBalance();
		if (true) { System.out.println("Day " + day + " :" + content.toString()); }
	}

	/**
	 * Processes the start information.
	 * @param startInfo the start information.
	 */
	protected void handleStartInfo(StartInfo startInfo) {
		this.startInfo = startInfo;
	}

	/**
	 * Process the reported set of publishers
	 * @param publisherCatalog
	 */
	private void handlePublisherCatalog(PublisherCatalog publisherCatalog) {
		this.publisherCatalog = publisherCatalog;
		generateAdxQuerySpace();
		getPublishersNames();
	}

	/**
	 * On day 0, a campaign (the "initial campaign") is allocated to each
	 * competing agent. The campaign starts on day 1. The address of the
	 * server's AdxAgent (to which bid bundles are sent) and DemandAgent (to
	 * which bids regarding campaign opportunities may be sent in subsequent
	 * days) are also reported in the initial campaign message
	 */
	private void handleInitialCampaignMessage(InitialCampaignMessage campaignMessage) {
		if (verbose_printing) { System.out.println(campaignMessage.toString()); }
		day = 0;

		//initialise globals
		initialCampaignMessage = campaignMessage;
		demandAgentAddress = campaignMessage.getDemandAgentAddress();
		adxAgentAddress = campaignMessage.getAdxAgentAddress();

		//intialise currCampaign
		CampaignData campaignData = new CampaignData(initialCampaignMessage);
		campaignData.setBudget(initialCampaignMessage.getBudgetMillis()/1000.0);
		currCampaign = campaignData;
		genCampaignQueries(currCampaign);

		imps_competing_indicies.put(currCampaign.id, 2.0);
		imps_previous_results.put(currCampaign.id, 0); //ie no results yet

		//Initialise coeff means
		meanMobCoeff = campaignMessage.getMobileCoef();
		meanVidCoeff = campaignMessage.getVideoCoef();

		/*
		 * The initial campaign is already allocated to our agent so we add it
		 * to our allocated-campaigns list.
		 */
		if (verbose_printing) { System.out.println("Day " + day + ": Allocated campaign - " + campaignData); }
		myCampaigns.put(initialCampaignMessage.getId(), campaignData);
	}

	/**
	 * On day n ( > 0) a campaign opportunity is announced to the competing
	 * agents. The campaign starts on day n + 2 or later and the agents may send
	 * (on day n) related bids (attempting to win the campaign). The allocation
	 * (the winner) is announced to the competing agents during day n + 1.
	 */
	private void handleICampaignOpportunityMessage(
			CampaignOpportunityMessage com) {

		day = com.getDay();

		meanVidCoeff = (meanVidCoeff + com.getVideoCoef())/2;
		meanMobCoeff = (meanMobCoeff + com.getMobileCoef())/2;

		pendingCampaign = new CampaignData(com);
		postedCampaigns.add(new CampaignData(com)); cleanPostedCampaignList(); //XXX maybe not best place?
		if (verbose_printing) { System.out.println("Day " + day + ": Campaign opportunity - " + pendingCampaign); }

		/*
		 * The campaign requires com.getReachImps() impressions. The competing
		 * Ad Networks bid for the total campaign Budget (that is, the ad
		 * network that offers the lowest budget gets the campaign allocated).
		 * The advertiser is willing to pay the AdNetwork at most 1$ CPM,
		 * therefore the total number of impressions may be treated as a reserve
		 * (upper bound) price for the auction.
		 */
		ContractBidder bidder = new ContractBidder(com);
		long cmpBidMillis = bidder.getContractBid();
		previous_campaign_bid = cmpBidMillis;
		
		if (contract_printing) { System.out.println("CAMPAIGN BID: " + cmpBidMillis); }

		if (verbose_printing) { System.out.println("Day " + day + ": Campaign total budget bid (millis): " + cmpBidMillis); }

		/*
		 * Adjust ucs bid s.t. target level is achieved. Note: The bid for the
		 * user classification service is piggybacked
		 */
		if (adNetworkDailyNotification != null) {
			ucsLevel = adNetworkDailyNotification.getServiceLevel();
			if (haveActiveCampaigns()) {
				UCSBidder ucsBidder = new UCSBidder(prevUcsBid, ucsLevel);
				ucsBid = ucsBidder.getUCSBid();
				if(ucsBid > 0){
					prevUcsBid = ucsBid;
				}
				if(ucs_printing) { System.out.println("UCS BID: " + ucsBid); }
			} else {
				ucsBid = 0.0;
			}
			if( verbose_printing) { System.out.println("Day " + day + ": ucs level reported: " + ucsLevel); }
		} else {
			if (verbose_printing) { System.out.println("Day " + day + ": Initial ucs bid is " + ucsBid); }
		}

		/* Note: Campaign bid is in millis */
		AdNetBidMessage bids = new AdNetBidMessage(ucsBid, pendingCampaign.id, cmpBidMillis);
		sendMessage(demandAgentAddress, bids);
	}

	/**
	 * On day n ( > 0), the result of the UserClassificationService and Campaign
	 * auctions (for which the competing agents sent bids during day n -1) are
	 * reported. The reported Campaign starts in day n+1 or later and the user
	 * classification service level is applicable starting from day n+1.
	 */
	private void handleAdNetworkDailyNotification(
			AdNetworkDailyNotification notificationMessage) {

		adNetworkDailyNotification = notificationMessage;

		if (verbose_printing) { System.out.println("Day " + day + ": Daily notification for campaign "
				+ adNetworkDailyNotification.getCampaignId()); }

		String campaignAllocatedTo = " allocated to "
				+ notificationMessage.getWinner();

		if ((pendingCampaign.id == adNetworkDailyNotification.getCampaignId())
				&& (notificationMessage.getCostMillis() != 0)) {
			
			// If we won legit (when the the winning bid is NOT the same as our bid - it will be second-place's bid), lower the competing index
			if(notificationMessage.getCostMillis() != previous_campaign_bid){
				competing_index = competing_index/GREED;
				if (contract_printing) { System.out.println("WE WON LEGIT. Competing Index = " + competing_index); }
			}
			else{
				if (contract_printing) { System.out.println("WE WON BY LUCK. Competing Index = " + competing_index); }
			}

			/* add campaign to list of won campaigns */
			pendingCampaign.setBudget(notificationMessage.getCostMillis()/1000.0);
			currCampaign = pendingCampaign;
			genCampaignQueries(currCampaign);
			myCampaigns.put(pendingCampaign.id, pendingCampaign);

			imps_competing_indicies.put(pendingCampaign.id, IMP_COMPETING_INDEX_DEFAULT);
			imps_previous_results.put(pendingCampaign.id, 0);
			System.out.println("Campaign ID added: " + pendingCampaign.id);

			campaignAllocatedTo = " WON at cost (Millis)"
					+ notificationMessage.getCostMillis();
		}
		else {
			// We lost, so increase the competing index
			competing_index = (competing_index * GREED > COMPETING_INDEX_MAX) ? COMPETING_INDEX_MAX : competing_index * GREED;
			if (contract_printing) { System.out.println("WE LOST. Competing Index = " + competing_index); }
		}

		//Stores the winning bid for each campaign
		campaignWinningBids.put(notificationMessage.getCampaignId(), notificationMessage.getCostMillis());

		//Stores our current quality rating
		currQuality = notificationMessage.getQualityScore();

		//Update ucs bid history with new result
		ucsTracker.handleUCSBid(day, notificationMessage);

		if (verbose_printing) {
			for (MarketSegment s : MarketSegment.values()) {
				double pop = PIPredictor.getPopAtomic(s, day+1);
				System.out.println("Segment: " + s + " - Atomic pop: " + pop);
			}
			for (Set<MarketSegment> S : MarketSegment.marketSegments()) {
				double pop = PIPredictor.getPop(S, day+1,day+2);
				System.out.println("Seg: " + S.toString() + " - pop: " + pop);
			}
		}

		if (impressions_printing) {
			for (Integer key : imps_competing_indicies.keySet()) {
				System.out.println("Campaign ID: " + key + " - Index: " + imps_competing_indicies.get(key));
			}
		}

		if (verbose_printing) { System.out.println("Day " + day + ": " + campaignAllocatedTo
				+ ". UCS Level set to " + notificationMessage.getServiceLevel()
				+ " at price " + notificationMessage.getPrice()
				+ " Quality Score is: " + notificationMessage.getQualityScore()); }
	}

	/**
	 * The SimulationStatus message received on day n indicates that the
	 * calculation time is up and the agent is requested to send its bid bundle
	 * to the AdX.
	 */
	private void handleSimulationStatus(SimulationStatus simulationStatus) {
		System.out.println("Day " + day + " : Simulation Status Received");
		sendBidAndAds();
		if (verbose_printing) { System.out.println("Day " + day + " ended. Starting next day"); }
		++day;
	}

	/**
	 *	Handles bidding for impressions - creating and sending bid bundle
	 */
	protected void sendBidAndAds() {

		bidBundle = new AdxBidBundle();

		int dayBiddingFor = day + 1;

		int pop = 1; //defaults to 1 if no pop value found
		double reservePrice = 0.0;

		/*
		 * A random bid, fixed for all queries of the campaign
		 * Note: bidding per 1000 imps (CPM) - no more than average budget
		 * revenue per imp
		 */
		double bid; // XXX impressions bid

		//Loop over all of our running campaigns
		for (int campKey : myCampaigns.keySet()) {
			CampaignData thisCampaign = myCampaigns.get(campKey);
			if (thisCampaign.dayEnd < day) {
				//Inactive campaign
				continue;
			}
			ImpressionsBidder impsBidder = new ImpressionsBidder(thisCampaign);

		/*
		 * add bid entries w.r.t. each active campaign with remaining contracted
		 * impressions.
		 *
		 * for now, a single entry per active campaign is added for queries of
		 * matching target segment.
		 */
			if ((dayBiddingFor >= thisCampaign.dayStart)
					&& (dayBiddingFor <= thisCampaign.dayEnd)) {

				int entCount = 0;

				//TODO: Determine how to handle the empty market segment calls when the ucs service doesnt give us answer

				for (AdxQuery query : thisCampaign.campaignQueries) { //all possible targets  for each publisher
					if (thisCampaign.impsTogo() - entCount > 0) { //Only bid for as many impressions as is needed
					/*
					 * among matching entries with the same campaign id, the AdX
					 * randomly chooses an entry according to the designated
					 * weight. by setting a constant weight 1, we create a
					 * uniform probability over active campaigns
					 */
						if (query.getDevice() == Device.pc) {
							if (query.getAdType() == AdType.text) {
								entCount++;
							} else {
								entCount += thisCampaign.videoCoef;
							}
						} else {
							if (query.getAdType() == AdType.text) {
								entCount += thisCampaign.mobileCoef;
							} else {
								entCount += thisCampaign.videoCoef + thisCampaign.mobileCoef;
							}
						}

						//Searches publisher report for the publisher in the query and updates popularity var
						String publisherStr = query.getPublisher();
						if (pubReport != null) {
							for (PublisherCatalogEntry pubKey : pubReport.keys()) {
								try {
									//Get current website reserve and pop
									if (pubKey != null) {
										if (pubKey.getPublisherName().equals(publisherStr)) {
											reservePrice = pubReport.getEntry(pubKey).getReservePriceBaseline();
											pop = pubReport.getEntry(pubKey).getPopularity();
										}
									}
								} catch (Exception e) {
									System.out.println(e.toString());
								}
							}
						}

						//update the rbid here with reserve info?
						bid = impsBidder.getImpressionBid();

						//Weight the bids based on popularity of the publisher
						bidBundle.addQuery(query, bid, new Ad(null), thisCampaign.id, pop, thisCampaign.budget);
						if (false) {System.out.println("day: " + day + " - camp id: " + thisCampaign.id + " - bid: " + bid + " - site: " + query.getPublisher());}
					}
				}
				if(impressions_printing) {
					System.out.println("ID: " + thisCampaign.id + " - bid: " + impsBidder.getImpressionBid());
					System.out.println("ID: " + thisCampaign.id + " - Budget: " + thisCampaign.budget + " - Current cost: " + thisCampaign.stats.getCost());
					System.out.println("ID: " + thisCampaign.id + " - Reach: " + thisCampaign.reachImps + " - Imps2Go: " + thisCampaign.impsTogo());
				}

				//Attempt to get the agent to continue bidding at 100% completion to get the extra profit and quality
				double impressionLimit = thisCampaign.impsTogo();
//				if (thisCampaign.impsTogo() == 0) {
//					impressionLimit = thisCampaign.reachImps*1.2;
//				} else if (thisCampaign.impsTogo() < 0) {
//					impressionLimit = 0;
//				}

				double budgetLimit = thisCampaign.budget/thisCampaign.impsTogo();
				bidBundle.setCampaignDailyLimit(thisCampaign.id,
						(int) impressionLimit, budgetLimit);

				if (verbose_printing) {
					System.out.println("Day " + day + ": Updated " + entCount
							+ " Bid Bundle entries for Campaign id " + thisCampaign.id);
				}
			}
		}
		//end looping over campaigns

		try {
			//Store bid bundle in history
			impTracker.handleImpBid(day, bidBundle);
		} catch (Exception e) {
			System.out.println("Handling imp bid: " + e.toString());
		}

		if (bidBundle != null) {
			if (verbose_printing) { System.out.println("Day " + day + ": Sending BidBundle:" + bidBundle.toString()); }
			sendMessage(adxAgentAddress, bidBundle);
		}
	}

	/**
	 * Campaigns performance w.r.t. each allocated campaign
	 */
	private void handleCampaignReport(CampaignReport campaignReport) {

		campaignReports.add(campaignReport);

		/*
		 * for each campaign, the accumulated statistics from day 1 up to day
		 * n-1 are reported
		 */
		for (CampaignReportKey campaignKey : campaignReport.keys()) {

			int cmpId = campaignKey.getCampaignId();
			CampaignStats cstats = campaignReport.getCampaignReportEntry(campaignKey).getCampaignStats();

			//Updates each campaign in myCampaigns with new stats
			myCampaigns.get(cmpId).setStats(cstats);
			for (Integer campID : myCampaigns.keySet()) {
				if (costs_printing) { System.out.println("Campaign ID: " + campID + " - Cost: " + myCampaigns.get(campID).stats.getCost());}
			}

			myCampaignStatsHistory.put(cmpId, cstats);

			if (true) { System.out.println("Day " + day + ": Updating campaign " + cmpId + " stats: "
					+ cstats.getTargetedImps() + " tgtImps "
					+ cstats.getOtherImps() + " nonTgtImps. Cost of imps is "
					+ cstats.getCost());
				System.out.println("ID: " + cmpId + " - Seg: " + myCampaigns.get(cmpId).targetSegment + " - Seg pop: " + PIPredictor.getPop(myCampaigns.get(cmpId).targetSegment, day, day));
			}
		}
	}

	/**
	 * Users and Publishers statistics: popularity and ad type orientation
	 */
	private void handleAdxPublisherReport(AdxPublisherReport adxPublisherReport) {
		pubReport = adxPublisherReport;

		if (verbose_printing) { System.out.println("Publishers Report: "); }
		for (PublisherCatalogEntry publisherKey : adxPublisherReport.keys()) {
			AdxPublisherReportEntry entry = adxPublisherReport
					.getEntry(publisherKey);
			if (verbose_printing) { System.out.println(entry.toString()); }
		}
	}

	/**
	 *
	 * //@param AdNetworkReport
	 */
	private void handleAdNetworkReport(AdNetworkReport adnetReport) {

		if (verbose_printing) { System.out.println("Day " + day + " : AdNetworkReport"); }

		try {
			impTracker.updateBidHistory(adnetReport);
		} catch (Exception e) {
			System.out.println("Updating imp tracker: " + e.toString());
		}
	}

	@Override
	protected void simulationSetup() {
		Random random = new Random();

		day = 0;
		bidBundle = new AdxBidBundle();

		/* initial bid between 0.1 and 0.2 */
		ucsBid = 0.2 + random.nextDouble()/10.0;
		prevUcsBid = ucsBid;

		myCampaigns = new HashMap<Integer, CampaignData>();
		log.fine("AdNet " + getName() + " simulationSetup");
	}

	@Override
	protected void simulationFinished() {
		campaignReports.clear();
		bidBundle = null;
	}

	/**
	 * A user visit to a publisher's web-site results in an impression
	 * opportunity (a query) that is characterized by the the publisher, the
	 * market segment the user may belongs to, the device used (mobile or
	 * desktop) and the ad type (text or video).
	 *
	 * An array of all possible queries is generated here, based on the
	 * publisher names reported at game initialization in the publishers catalog
	 * message
	 */
	private void generateAdxQuerySpace() {
		if (publisherCatalog != null && queries == null) {
			Set<AdxQuery> querySet = new HashSet<AdxQuery>();

			/*
			 * for each web site (publisher) we generate all possible variations
			 * of device type, ad type, and user market segment
			 */
			for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog) {
				String publishersName = publisherCatalogEntry
						.getPublisherName();
				for (MarketSegment userSegment : MarketSegment.values()) {
					Set<MarketSegment> singleMarketSegment = new HashSet<MarketSegment>();
					singleMarketSegment.add(userSegment);

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.text));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.text));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.video));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.video));
				}

				/*
				 * An empty segments set is used to indicate the "UNKNOWN"
				 * segment such queries are matched when the UCS fails to
				 * recover the user's segments.
				 */
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.mobile,
						AdType.video));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.mobile,
						AdType.text));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.pc, AdType.video));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.pc, AdType.text));
			}
			queries = new AdxQuery[querySet.size()];
			querySet.toArray(queries);
		}
	}

	/**
	 * generates an array of the publishers names
	 */
	private void getPublishersNames() { // (randomly?) chosen 5
		if (null == publisherNames && publisherCatalog != null) {
			ArrayList<String> names = new ArrayList<String>();
			for (PublisherCatalogEntry pce : publisherCatalog) {
				names.add(pce.getPublisherName());
			}

			publisherNames = new String[names.size()];
			names.toArray(publisherNames);
		}
	}

	/**
	 * generates the campaign queries relevant for the specific campaign, and assign them as the campaigns campaignQueries field
	 */
	private void genCampaignQueries(CampaignData campaignData) {
		Set<AdxQuery> campaignQueriesSet = new HashSet<AdxQuery>();
		for (String PublisherName : publisherNames) {
			campaignQueriesSet.add(new AdxQuery(PublisherName,
					campaignData.targetSegment, Device.mobile, AdType.text));
			campaignQueriesSet.add(new AdxQuery(PublisherName,
					campaignData.targetSegment, Device.mobile, AdType.video));
			campaignQueriesSet.add(new AdxQuery(PublisherName,
					campaignData.targetSegment, Device.pc, AdType.text));
			campaignQueriesSet.add(new AdxQuery(PublisherName,
					campaignData.targetSegment, Device.pc, AdType.video));
		}

		//TODO: Add in query with empty target segment map for imps where we dont know the user details - see generateAdxQuerySpace

		campaignData.campaignQueries = new AdxQuery[campaignQueriesSet.size()];
		campaignQueriesSet.toArray(campaignData.campaignQueries);
		if (verbose_printing) { System.out.println("!!!!!!!!!!!!!!!!!!!!!!"+Arrays.toString(campaignData.campaignQueries)+"!!!!!!!!!!!!!!!!"); }
	}

	/**
	 * Determines if this agent has any campaigns active
	 * @return true if there is active campaign
	 */
	private boolean haveActiveCampaigns() {
		for (Map.Entry<Integer, CampaignData> entry : myCampaigns.entrySet()) {
			CampaignData data = entry.getValue();
			if (day < data.dayEnd) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes any finished campaigns from the postedCampaign list
	 */
	private void cleanPostedCampaignList() {
		for (int iCampaign = postedCampaigns.size() -1; iCampaign>=0; iCampaign--) {
			if (postedCampaigns.get(iCampaign).dayEnd < day) {
				postedCampaigns.remove(iCampaign);
			}
		}
	}

	/**
	 * Determines how many running campaigns clash with the given campaign and by how much
	 * @param c campaign data object
	 * @return int[] {number of clashing campaigns, extent of total clashing}
	 */
	private ClashObject numClashingCampaigns(CampaignData c) {
		Set<MarketSegment> targSeg = c.targetSegment;

		List<CampaignData> clashingCamps = new ArrayList<CampaignData>();
		List<Integer> clashCampExtent = new ArrayList<Integer>();

		int clashExtent = 0;

		//Looks at each posted campaign, determines if they clash with posted campaign and by how much
		for (CampaignData camp : postedCampaigns) {
			boolean clashed = false;
			for (MarketSegment seg : camp.targetSegment) {
				if (targSeg.contains(seg)) {
					if (!clashed) { //Clashes with at least one segment
						clashingCamps.add(camp);
						clashed = true;
					}
					clashExtent++;
				}
			}
			clashCampExtent.add(clashExtent);
			clashExtent = 0;
		}
		return new ClashObject(clashingCamps, clashCampExtent);
	}

	public int getSegmentPopularity(Set<MarketSegment> seg) {
		return MarketSegment.marketSegmentSize(seg);
	}

	private class ImpTracker {
		List<ImpBidTrackingObject> history;
		public ImpTracker() {
			history = new ArrayList<>();
		}

		public List<ImpBidTrackingObject> getHistory() {
			return history;
		}

		public void handleImpBid(int day, AdxBidBundle bidBundle) {
			ImpBidTrackingObject impObj = new ImpBidTrackingObject(day, bidBundle, new HashMap<Integer,Integer>());
			history.add(impObj);
		}

		private void updateImpCompeteIndex(int campId, int newImpsWon) {
			double comp_index = imps_competing_indicies.get(campId);
			double prev_result = imps_previous_results.get(campId);

			double result_modifier = 1;

			CampaignData camp = myCampaigns.get(campId);
			int dur;
			if (camp.dayEnd == day) { dur = 1; } else { dur = (int)camp.dayEnd - day; }
			//double avImpsPerDayReq = camp.impstogo / dur
			double avImpsPerDayReq = camp.reachImps/(camp.dayEnd - camp.dayStart);

			if (newImpsWon < avImpsPerDayReq) {
			    //LOSE
				if (prev_result == -1) {
					result_modifier = IMP_RESULT_MODIFIER_LOSE_LOSE; //Add to greed
				} else if (prev_result == 1) {
					result_modifier = IMP_RESULT_MODIFIER_WIN_LOSE;
				}
				comp_index = (comp_index * (IMP_GREED_LOSE + result_modifier) > IMP_COMPETING_INDEX_MAX) ? IMP_COMPETING_INDEX_MAX : comp_index * (IMP_GREED_LOSE + result_modifier);
				if (false) { System.out.println("ID: " + campId + " - Not enough imps gained. Raising: " + comp_index);}
			} else {
				//WIN
				if (prev_result == -1) {
					result_modifier = IMP_RESULT_MODIFIER_LOSE_WIN; //Add to greed
				} else if (prev_result == 1) {
					result_modifier = IMP_RESULT_MODIFIER_WIN_WIN;
				}
				comp_index = (comp_index / IMP_GREED_WIN < (IMP_GREED_WIN + result_modifier)) ? IMP_COMPETING_INDEX_MIN : comp_index / (IMP_GREED_WIN + result_modifier);
				if (false) { System.out.println("ID: " + campId + " - Enough imps gained. Lowering: " + comp_index);}
			}

			imps_competing_indicies.put(campId,comp_index);
		}

		/**
		 * Function updates the bid history list with a new number of impressions won
		 * @param adnetReport - the network report that is issued on a given day
		 */
		private void updateBidHistory(AdNetworkReport adnetReport) {
			Map<Integer, Integer> newImps = new HashMap<>();

			//Loop over all entries in report (see example at bottom of file)
			for (AdNetworkKey adnetKey : adnetReport.keys()) {
				//initialise imps to 0 for each campaign
				if (!newImps.keySet().contains(adnetKey.getCampaignId())) {
					newImps.put(adnetKey.getCampaignId(), 0);
				}

				AdNetworkReportEntry entry = adnetReport.getEntry(adnetKey);

				//Update impressions won on that day
				int newImpsWon = entry.getWinCount();
				int sumImpsWon = newImps.get(adnetKey.getCampaignId());
				newImps.put(adnetKey.getCampaignId(), sumImpsWon+newImpsWon);

				//Find the corresponding day in bid history
				for (ImpBidTrackingObject bid : history) {
					if (bid.getDay() == day-1) {
						int currImpsWon = bid.getImpsWon(adnetKey.getCampaignId());
						bid.setImpsWon(adnetKey.getCampaignId(), currImpsWon + newImpsWon);
					}
				}
			}

			for (Integer campKey : newImps.keySet()) {
				try {
					updateImpCompeteIndex(campKey, newImps.get(campKey));
				} catch (Exception e) {
					System.out.println("Updating compete index: " + e.toString());
				}
			}

			if (verbose_printing) {
				for (ImpBidTrackingObject bid : history) {
					if (bid.getDay() == day - 1) {
						for (int campKey : bid.getImpsMap().keySet()) {
							System.out.println("Day: " + (day-1) + " - Camp: " + campKey + " - Imps won: " + bid.getImpsWon(campKey));
						}
					}
				}
			}
		}

		/**
		 * Class represents a pair:
		 * bidBundle - the bid bundle sent on a given day
		 * impsMap - map of campaign id to imps won
		 */
		private class ImpBidTrackingObject {
			int day;
			AdxBidBundle bundle;
			Map<Integer, Integer> impsMap;

			public ImpBidTrackingObject(int day, AdxBidBundle bundle, Map<Integer, Integer> impsMap) {
				this.day = day;
				this.bundle = bundle;
				this.impsMap = impsMap;
			}

			public int getImpsWon(int campID) {
				if (impsMap.get(campID) != null) {
					return impsMap.get(campID);
				} else {
					return 0;
				}
			}

			public AdxBidBundle getBundle() {
				return this.bundle;
			}

			public Map<Integer, Integer> getImpsMap() {
				return impsMap;
			}

			public int getDay() {
				return this.day;
			}

			public void setImpsWon(int campID, int impsWon) {
				impsMap.put(campID, impsWon);
			}
		}
	}

	/**
	 * Class represents a pair of lists:
	 * clashCamps - The campaigns that clash with campaign op
	 * clashExtents - The number of segments it has the same as campaign op
	 */
	private class ClashObject {
		List<CampaignData> clashCamps;
		List<Integer> clashExtents;

		public ClashObject(List<CampaignData> camps, List<Integer> extents) {
			this.clashCamps = camps;
			this.clashExtents = extents;
		}

		public List<CampaignData> getClashCamps() {
			return clashCamps;
		}

		public List<Integer> getClashExtents() {
			return clashExtents;
		}
	}


	private class UCSBidTracker {
		List<UcsBidObj> history;

		public UCSBidTracker() {
			history = new ArrayList<>();
		}

		public void handleUCSBid(int day, AdNetworkDailyNotification dailyNotification) {
			history.add(new UcsBidObj(day, dailyNotification.getPrice(), dailyNotification.getServiceLevel()));
		}

		public List<UcsBidObj> getUCSHistory() {
			return history;
		}

		/**
		 * Class represents a single Ucs bid history item with variables:
		 * bid		- The bid value for the day
		 * ucsLevel - The ucs level achieved
		 */
		private class UcsBidObj {
			int day;
			double bid;
			double ucsLevel;

			public UcsBidObj(int day, double bid, double ucsLevel) {
				this.day = day;
				this.bid = bid;
				this.ucsLevel = ucsLevel;
			}

			public int getDay() {
				return day;
			}

			public double getBid() {
				return this.bid;
			}

			public double getUcsLevel() {
				return this.ucsLevel;
			}
		}
	}

	/**
	 * Class storing data on a single campaign.
	 */
	private class CampaignData {
		/* campaign attributes as set by server */
		Long reachImps;
		long dayStart;
		long dayEnd;
		Set<MarketSegment> targetSegment;
		double videoCoef;
		double mobileCoef;
		int id;
		private AdxQuery[] campaignQueries;//array of queries relvent for the campaign.

		/* campaign info as reported */
		CampaignStats stats;
		double budget;

		public CampaignData(InitialCampaignMessage icm) {
			reachImps = icm.getReachImps();
			dayStart = icm.getDayStart();
			dayEnd = icm.getDayEnd();
			targetSegment = icm.getTargetSegment();
			videoCoef = icm.getVideoCoef();
			mobileCoef = icm.getMobileCoef();
			id = icm.getId();

			stats = new CampaignStats(0, 0, 0);
			budget = 0.0;
		}

		public void setBudget(double d) {
			budget = d;
		}

		public CampaignData(CampaignOpportunityMessage com) {
			dayStart = com.getDayStart();
			dayEnd = com.getDayEnd();
			id = com.getId();
			reachImps = com.getReachImps();
			targetSegment = com.getTargetSegment();
			mobileCoef = com.getMobileCoef();
			videoCoef = com.getVideoCoef();

			stats = new CampaignStats(0, 0, 0);
			budget = 0.0;
		}

		@Override
		public String toString() {
			return "Campaign ID " + id + ": " + "day " + dayStart + " to "
					+ dayEnd + " " + targetSegment + ", reach: " + reachImps
					+ " coefs: (v=" + videoCoef + ", m=" + mobileCoef + ")";
		}

		int impsTogo() {
			return (int) Math.max(0, reachImps - stats.getTargetedImps());
		}

		void setStats(CampaignStats s) {
			stats.setValues(s);
		}

		public AdxQuery[] getCampaignQueries() {
			return campaignQueries;
		}

		public void setCampaignQueries(AdxQuery[] campaignQueries) {
			this.campaignQueries = campaignQueries;
		}

	}
	
	
	private class ContractBidder {
		
		private double quality_threshold = 0.8;
		private double price_index_threshold = 1.0;
		
		/* campaign attributes as set by server */
		Long reachImps;
		long dayStart;
		long dayEnd;
		Set<MarketSegment> targetSegment;
		
		double price_index;

		public ContractBidder(CampaignOpportunityMessage com) {
			dayStart = com.getDayStart();
			dayEnd = com.getDayEnd();
			reachImps = com.getReachImps();
			targetSegment = com.getTargetSegment();
			price_index = PIPredictor.getPop(targetSegment, (int) dayStart, (int) dayEnd);
		}
		
		public long getContractBid(){
			System.out.println("***PRICE INDEX***: " + price_index);
			if (price_index > price_index_threshold){
				if (contract_printing) { System.out.println("PRICE INDEX HIGH at " + price_index + ". BIDDING HIGHEST VALID BID."); }
				return highestValidBid();
			}
			else if (currQuality < quality_threshold){
				if (contract_printing) { System.out.println("QUALITY LOW at " + currQuality + ". BIDDING LOWEST VALID BID."); }
				return lowestValidBid();
			}
			else{
				if (contract_printing) { System.out.println("DEFAULT - BIDDING PRIVATE VALUE"); }
				return privateValueBid();
			}
		}
		
		public long privateValueBid(){
			long privateValue = (long) ((price_index * (double) reachImps) / competing_index);
			long highestBid = highestValidBid();
			long lowestBid = lowestValidBid();
			
			if(privateValue < lowestBid){
				return lowestBid;
			}
			else if (privateValue > highestBid){
				return highestBid;
			}
			else{
				return privateValue;
			} 
		}
		
		public long lowestValidBid(){
			// Lower bound Reserve price is 0.1$ CPM
			return (long) ((0.1 * (double) reachImps)/currQuality) + 1;
		}
		
		public long highestValidBid(){
			 // Upper bound Reserve price is 1$ CPM (i.e the total number of impressions)
			 return (long) ((double) reachImps * currQuality);

		}
	}
	
	private class UCSBidder {
		
		double previousBid;
		double previousLevel;
		double minReach;
		double impressionUnitPrice;
		
		public UCSBidder(double previousBid, double previousLevel){
			this.previousBid = previousBid;
			this.previousLevel = previousLevel;
			
			this.minReach = 0.75 * getTotalReach();
			this.impressionUnitPrice = getImpressionUnitPrice() / previousLevel;
		}
		
		public double getUCSBid(){			
			if (previousLevel > 0.9){
				if(ucs_printing) { System.out.println("UCS LEVEL TOO HIGH! Lowering bid"); }
				return previousBid / (1 + UCSScaler); 
			}
			else if ((previousLevel < 0.81) && shouldIncreaseLevel()){
				if(ucs_printing) { System.out.println("UCS LEVEL TOO LOW! Raising bid"); }
				return (1 + UCSScaler) * previousBid;
			}
			else if(this.minReach > 0.0){
				if(ucs_printing) { System.out.println("UCS LEVEL PERFECT! Maintaining bid"); }
				return previousBid;
			}
			else{
				return 0;
			}
			
		}
		
		// Returns true or false, based on the expected value of increasing UCS bid
		private boolean shouldIncreaseLevel(){			
			double prev_util = (minReach / (previousBid*1000.0));
			double new_util = 20.0/3.0*((1.0+UCSScaler)/getUtilityOfIncrement());
			if(ucs_printing) { 
				System.out.println("Previous Util: " + prev_util); 
				System.out.println("Utility of increment: " + new_util);
			}
			return prev_util >= new_util;
		}
		
		// Calculates the utility gained from spending extra to go up a UCS level
		private double getUtilityOfIncrement(){
			return (1/minReach) * integrate(minReach, 2*minReach);
		}
			
		public double integration_function(double r){
			return (r*(impressionUnitPrice - (0.9*impressionUnitPrice)) - (1.0 + UCSScaler)*previousBid);
		}
		
		public double integrate(double a, double b) {
		      int N = 10000;                    // precision parameter
		      double h = (b - a) / (N - 1);     // step size
		 
		      // 1/3 terms
		      double sum = 1.0 / 3.0 * (integration_function(a) + integration_function(b));

		      // 4/3 terms
		      for (int i = 1; i < N - 1; i += 2) {
		         double x = a + h * i;
		         sum += 4.0 / 3.0 * integration_function(x);
		      }

		      // 2/3 terms
		      for (int i = 2; i < N - 1; i += 2) {
		         double x = a + h * i;
		         sum += 2.0 / 3.0 * integration_function(x);
		      }

		      return sum * h;
		   }
		
		// Returns the average impression cost across all current campaigns
		private double getImpressionUnitPrice(){
			
			Set<MarketSegment> targetSegments = new HashSet<MarketSegment>();
			
			for (CampaignData myCampaign : myCampaigns.values()){
				for (MarketSegment targetSegment : myCampaign.targetSegment){
					if(!targetSegments.contains(targetSegment)){
						targetSegments.add(targetSegment);
						System.out.println(targetSegment.name());
					}	
				}
			}
			
			cleanPostedCampaignList();
			ArrayList<CampaignData> allCampaigns = (ArrayList<CampaignData>) postedCampaigns;
			ArrayList<CampaignData> checkedCampaigns = new ArrayList<CampaignData>();
			
			double totalDemand = 0;
			double totalSupply = 0;
			for (MarketSegment s : targetSegments){
				for (CampaignData otherCampaign: allCampaigns){
					if (otherCampaign.targetSegment.contains(s) && !checkedCampaigns.contains(otherCampaign)){
						totalDemand += (double) otherCampaign.impsTogo();
						checkedCampaigns.add(otherCampaign);
					}
				}
				Set<MarketSegment> marketSet = new HashSet<MarketSegment>();
				marketSet.add(s);
				totalSupply += MarketSegment.marketSegmentSize(marketSet);				
			}
			
			return (totalDemand / totalSupply);
		}
		
		// Returns the total impressions still needed across all current campaigns
		private int getTotalReach(){
			int totalReach = 0;
			for (CampaignData campaign : myCampaigns.values()){
				if(campaign.dayEnd - day > 0){
					totalReach = totalReach + (campaign.impsTogo()/((int) campaign.dayEnd - day));
				}
			}
			return totalReach;
		}
		
	}

	private class ImpressionsBidder {
		private CampaignData camp;
		private final double DELTA = 0.0001;
		private boolean adv = true;
		private double budgetCoeff = 0.5;
		private double profitCoeff = 0.8;

		public ImpressionsBidder(CampaignData campaignData) {
			camp = campaignData;
		}

		//Determines the fraction of the required impressions currently reached in campaign camp
		private double getContractCompletionFraction() {
			return (1-camp.impsTogo()/camp.reachImps);
		}

		//Determines bid value based on price index
		public double getImpressionBid() {

			double bid = 0.0;

			bid = getBudget();

			if (camp.impsTogo() != 0) {
				//Return bid per mille imps
				double finalBid = (double) bid / camp.impsTogo() * 1000 * profitCoeff;
				if (finalBid > camp.impsTogo()) {
					finalBid = camp.impsTogo();
				}

				return finalBid;
			} else {
				return (double) bid / camp.reachImps * 1000 * profitCoeff;
			}
		}

		public double getBudget() {
			//Low value coefficients
			long low_budget = 500;
			long low_reach = 500;

			double comp_index = -1;

			try {
				comp_index = imps_competing_indicies.get(camp.id);
			} catch (Exception e) {
				System.out.println("Getting compete index: " + camp.id);
			}

			long dur = camp.dayEnd-day;

			double fractionImpsToGo = getContractCompletionFraction();

			double bid = 0;
			double budget = camp.budget;

			if (adv) {
				if (day < 4) {
					bid = budget * PIPredictor.getPop(camp.targetSegment, day+1, day+1);
				} else {
					bid = budget * PIPredictor.getPop(camp.targetSegment, day + 1, day+1);
				}
			} else {
				bid = budget * budgetCoeff;
			}

			bid = bid * comp_index;

			//If low budget and reach, set bid to max per impression
//			if (budget < low_budget && camp.reachImps < low_reach) {
//				bid = 0.001*budget;
//				if (impressions_printing) { System.out.println("Low budget and Low reach. Bidding max per impression.");}
//			}

//			if (day < 3) {
//				bid = 0.001*budget;
//			}

			//If short duration and not close to required reach, double bid
			if (dur == 1 && fractionImpsToGo > 0.1) {
				bid = budget * PIPredictor.getPop(camp.targetSegment, day+1, day+1)*2;
				if (impressions_printing) { System.out.println("Only 1 day left and many imps to go. Doubling bid. ");}
			}

			return bid;
		}
	}

	private class PIP {

		/**
		 * Determine popularity value of market segment s on day t
		 * @param s Atomic market segment ie FEMALE or YOUNG
		 * @param t Day on which you want popularity
		 * @return popularity of segment on that day
		 */
		public double getPopAtomic(MarketSegment s, int t) {
			double pop = 0.0;

			//Consider only currently running campaigns
			cleanPostedCampaignList();

			for (CampaignData camp : postedCampaigns) {
				if (camp.targetSegment.contains(s)) {
					if (camp.dayEnd > t) {
						pop = pop + (double)camp.reachImps / (double)(MarketSegment.marketSegmentSize(camp.targetSegment) * (camp.dayEnd - t));
					}
				}
			}

			return pop;
		}

		/**
		 * Determine popularity value of valid set of market segments
		 * @param S market segment set ie [MALE, OLD, HIGH]
		 * @param dayEnd end day to be considered
		 * @param dayStart start day to be considered
		 * @return
		 */
		public double getPop(Set<MarketSegment> S, int dayStart, int dayEnd) {
			double pop = 0.0;
			int totalSize = 0;

			//Adds all days in range to array
			List<Integer> T = new ArrayList<>();
			for (int i=dayStart ; i<= dayEnd; i++) {
				T.add(i);
			}

			for (MarketSegment s : S) {
				for (int t : T) {
					Set<MarketSegment> marketSet = new HashSet<MarketSegment>();
					marketSet.add(s);
					pop = pop + (MarketSegment.marketSegmentSize(marketSet) * getPopAtomic(s, t));
					totalSize += MarketSegment.marketSegmentSize(marketSet);
				}
			}

			pop = pop / (T.size() * totalSize);
			//System.out.println("Seg: " + S.toString() + " - pop: " + pop);
			return pop;
		}
	}
}


/*
Note: Seem to easily achieve 1000 imps per day when there is no competition.
	So if getting less than that, obv have competition and need to raise bid.
 */

//TODO: Change bid weights be based on pop value and days left in campaign

		/*
		*  AdNetworkKey
		*  [age=Age_35_44, income=high, gender=male, publisher=weather, device=mobile, adType=video, campaignId=1963806382]
		*  AdNetworkReportEntry AdNetworkKey
		*  [age=Age_35_44, income=high, gender=male, publisher=weather, device=mobile, adType=video, campaignId=1963806382]
		*  [bidCount=25, winCount=2, cost=0.0670725979432718]
		*/
