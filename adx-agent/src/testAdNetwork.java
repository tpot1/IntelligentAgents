import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;
import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.demand.Campaign;
import tau.tac.adx.demand.CampaignImpl;
import tau.tac.adx.demand.CampaignStats;
import tau.tac.adx.devices.Device;
import tau.tac.adx.messages.Contract;
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
	 * The targeted service level for the user classification service
	 */
	private double ucsTargetLevel;

	/**
	 * current day of simulation
	 */
	private int day;
	private String[] publisherNames;
	private CampaignData currCampaign;

	/**
	 * Unused variable used to hold the daily publisher report.
	 */
	private AdxPublisherReport pubReport;
	private boolean verbose_printing = false;

	/**
	 * Keeps list of all currently running campaigns allocated to any agent.
	 */
	private List<CampaignData> postedCampaigns;
	private Map<Integer, Long> campaignWinningBids;
	private List<UcsBidObject> ucsBidHistory;
	private List<ImpBidTrackingObject> impBidHistory;
	private Map<Integer, List<CampaignStats>> myCampaignStatsHistory;

	private double currQuality;

	private double meanVidCoeff;
	private double meanMobCoeff;

	public testAdNetwork() {
		campaignReports = new LinkedList<CampaignReport>();
		postedCampaigns = new ArrayList<CampaignData>();
		campaignWinningBids = new HashMap<>();
		ucsBidHistory = new ArrayList<>();
		impBidHistory = new ArrayList<>();
		myCampaignStatsHistory = new HashMap<>();
	}

	@Override
	protected void messageReceived(Message message) {
		try {
			Transportable content = message.getContent();

			// log.fine(message.getContent().getClass().toString());

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
		if (verbose_printing) { System.out.println("Day " + day + " :" + content.toString()); }
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

		meanVidCoeff = (meanVidCoeff + com.getVideoCoef())/2; //TODO: Also store variances for use when deciding if good or not
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
		Random random = new Random();
		long cmpBidMillis = evaluateCampaignOp(com);

		if (verbose_printing) { System.out.println("Day " + day + ": Campaign total budget bid (millis): " + cmpBidMillis); }

		/*
		 * Adjust ucs bid s.t. target level is achieved. Note: The bid for the
		 * user classification service is piggybacked
		 */
		if (adNetworkDailyNotification != null) {
			double ucsLevel = adNetworkDailyNotification.getServiceLevel();
			if (haveActiveCampaigns()) {
				ucsBid = 0.3 + random.nextDouble() / 10.0; //XXX UCS bid 0.3 wins against random ops
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

			/* add campaign to list of won campaigns */
			pendingCampaign.setBudget(notificationMessage.getCostMillis()/1000.0);
			currCampaign = pendingCampaign;
			genCampaignQueries(currCampaign);
			myCampaigns.put(pendingCampaign.id, pendingCampaign);

			campaignAllocatedTo = " WON at cost (Millis)"
					+ notificationMessage.getCostMillis();
		}

		//Stores the winning bid for each campaign
		campaignWinningBids.put(notificationMessage.getCampaignId(), notificationMessage.getCostMillis());

		//Stores our current quality rating
		currQuality = notificationMessage.getQualityScore();

		//Update ucs bid history with new result
		ucsBidHistory.add(new UcsBidObject(day, notificationMessage.getPrice(), notificationMessage.getQualityScore()));

		if (verbose_printing) {
			for (MarketSegment s : MarketSegment.values()) {
				double pop = getPIPPopAtomic(s, day+1);
				System.out.println("Segment: " + s + " - Atomic pop: " + pop);

			}
			for (Set<MarketSegment> S : MarketSegment.marketSegments()) {
				double pop = getPIPPop(S, day+2,day+1);
				System.out.println("Seg: " + S.toString() + " - pop: " + pop);
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
		Random random = new Random();

		int dayBiddingFor = day + 1;

		int pop = 1; //defaults to 1 if no pop value found
		double reservePrice = 0.0;

		int tempsum = 0;
		if (pubReport != null) {
			for (PublisherCatalogEntry temppubKey : pubReport.keys()) {
				tempsum = tempsum + pubReport.getEntry(temppubKey).getPopularity();
			}
		}
//TODO: Determine what is going on here - this changes each day. Maybe to do with how many users visit each day? Remove soon
		System.out.println("Total Pop Value: " + tempsum);


		/*
		 * A random bid, fixed for all queries of the campaign
		 * Note: bidding per 1000 imps (CPM) - no more than average budget
		 * revenue per imp
		 */
		double rbid = 1*random.nextDouble(); // XXX impressions bid

		for (int campKey : myCampaigns.keySet()) {
			CampaignData thisCampaign = myCampaigns.get(campKey);
			if (thisCampaign.dayEnd < day) { //TODO: Should be <= ?
				//Inactive campaign
				continue;
			}

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

			/*
			* 	Example of AdxQuery:
			*	AdxQuery [publisher=ehow, marketSegments=[LOW_INCOME, MALE], device=pc, adType=video]
			*	AdxQuery [publisher=msn, marketSegments=[LOW_INCOME, MALE], device=pc, adType=text]
			*	AdxQuery [publisher=msn, marketSegments=[LOW_INCOME, MALE], device=mobile, adType=video]
			*	AdxQuery [publisher=bestbuy, marketSegments=[LOW_INCOME, MALE], device=pc, adType=video]
			*/
				for (AdxQuery query : thisCampaign.campaignQueries) { //all possible targets  for each publisher
					if (thisCampaign.impsTogo() - entCount > 0) { //Only bid for as many impressions as is needed
					/*
					 * among matching entries with the same campaign id, the AdX
					 * randomly chooses an entry according to the designated
					 * weight. by setting a constant weight 1, we create a
					 * uniform probability over active campaigns (irrelevant because we are bidding only on one campaign)
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
						rbid = evaluateImpressionsBid(thisCampaign, query, reservePrice, pop);

						System.out.println("Bid bundle bid: " + rbid);
						System.out.println("Budget and 1k budget by reach:" + thisCampaign.budget + " - " + 1000*thisCampaign.budget/thisCampaign.reachImps);


						//Weight the bids based on popularity of the publisher
						bidBundle.addQuery(query, rbid, new Ad(null), thisCampaign.id, pop, thisCampaign.budget);
						if (verbose_printing) {System.out.println("day: " + day + " - camp id: " + thisCampaign.id + " - bid: " + rbid + " - site: " + query.getPublisher());}
					}
				}

				double impressionLimit = thisCampaign.impsTogo();
				System.out.println("Imps to go: " + thisCampaign.impsTogo());
				if (thisCampaign.impsTogo() == 0) {
					impressionLimit = thisCampaign.reachImps*1.2;
				} else if (thisCampaign.impsTogo() < 0) {
					impressionLimit = 0;
				}

				double budgetLimit = thisCampaign.budget;
				bidBundle.setCampaignDailyLimit(thisCampaign.id,
						(int) impressionLimit, budgetLimit);

				if (verbose_printing) {
					System.out.println("Day " + day + ": Updated " + entCount
							+ " Bid Bundle entries for Campaign id " + thisCampaign.id);
				}
			}
		}
		//end looping over campaigns

		//Store bid bundle in history
		impBidHistory.add(new ImpBidTrackingObject(day, bidBundle, new HashMap<>()));

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

			//Updates the campaign stats history for each campaign
			List<CampaignStats> newStatsList;
			if (myCampaignStatsHistory.get(cmpId) != null) {
				newStatsList = myCampaignStatsHistory.get(cmpId);
			} else {
				newStatsList = new ArrayList<>();
			}

			newStatsList.add(cstats);
			myCampaignStatsHistory.put(cmpId, newStatsList);

			if (verbose_printing) { System.out.println("Day " + day + ": Updating campaign " + cmpId + " stats: "
					+ cstats.getTargetedImps() + " tgtImps "
					+ cstats.getOtherImps() + " nonTgtImps. Cost of imps is "
					+ cstats.getCost());
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

		updateBidHistory(adnetReport);
	}

	@Override
	protected void simulationSetup() {
		Random random = new Random();

		day = 0;
		bidBundle = new AdxBidBundle();

		/* initial bid between 0.1 and 0.2 */
		ucsBid = 0.2 + random.nextDouble()/10.0;

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
	 * Evaluates the campaign opportunity given and returns the suggested bid
	 *	@param com Campaign op message
	 *  @return long finBid - the suggested bid
	 */
	private long evaluateCampaignOp(CampaignOpportunityMessage com) {
		long reach = com.getReachImps();
 		double vidCoeff = com.getVideoCoef();
		double mobileCoeff = com.getMobileCoef();
		long dur = com.getDayEnd() - com.getDayStart(); //Duration of campaign
		long finBid = reach; 							//Final bid size

		//Population size of target market segment as a number relative to 10,000
		int campSegPop = getSegmentPopularity(com.getTargetSegment());

		//Intermediate vars required
		double reachCoeff = 0.3; //Some coeff to multiply reach by for final bid
		long impsPerDay = reach/dur; //impressions per day required
		int sum = 0; boolean fullClash = false;

		//Get stats on clashes with posted campaigns
		ClashObject clashes = numClashingCampaigns(new CampaignData(com));
		List<CampaignData> clashingCamps = clashes.getClashCamps();
		List<Integer> clashingExtents = clashes.getClashExtents();


		if (verbose_printing) {
			System.out.println("Number of clashing campaigns:" + clashingCamps.size());
			System.out.println("Clashing extent:" + Arrays.toString(clashingExtents.toArray()));
			System.out.println("Size of target segment: " + campSegPop);
		}

		//Determines if there is a already running campaign that has exactly the same target
		for (int i : clashingExtents) {
			sum += i;
			if (i >= 2) {
				//If our campaign
				//if (clashingCamps.size() > 0) {
					//if (myCampaigns.containsKey(clashingCamps.get(i).id)) {
						//TODO: What to do when its our campaign clash + test this
					//} else {
						fullClash = true;
					//}
				//}
			}
		}

		//TODO: Factor in the popularity of market seg into campaign op bids

		//Rudimentary evaluation of campaign
		if (impsPerDay <= 1500 ||
				(clashingCamps.size() == 0 && impsPerDay <= 2000) ||
				(sum < 3 && impsPerDay <= 2000) ||
				(!fullClash && impsPerDay <= 2000)) {
			finBid = (long)(reach*reachCoeff); //Maybe some lincomb of reach other stuff eg market seg pop?
		}

		return finBid;
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

	/**
	 * Evaulates the bid to be made for a given query
	 * @param camp the campaign for which the impressions are to be bid for
	 * @param query specific site/target
	 * @param reservePrice reserve price for query publisher
	 * @param pop population of query publisher
	 * @return bid - value to bid on specific query
	 */
	private double evaluateImpressionsBid(CampaignData camp, AdxQuery query, double reservePrice, int pop) {
		long dur = camp.dayEnd-day;
		double budget = camp.budget;
		double bid = 0.0;

		double fractionImpsToGo = 1- camp.impsTogo() / camp.reachImps;

		//Coeff that decreses bid to make profit - 1 = no profit, <1 = profit
		double profitCoeff = 0.7;

		long low_budget = 500;
		long low_reach = 500;

		bid = budget * getPIPPop(camp.targetSegment, day+1, day+1);
		//System.out.println("Bid Estimate: " + bid/camp.reachImps);
		//System.out.println("Budget: " + budget);
		System.out.println("Pop coeff: " + getPIPPop(camp.targetSegment, day+1, day+1));

		if (budget < low_budget && camp.reachImps < low_reach) {
			bid = 0.001*budget;
		}

		if (dur == 1 && fractionImpsToGo > 0.1) { //TODO: This or == 0?
			bid = bid*2;
		}

		return (double)bid/camp.reachImps*1000*profitCoeff;
	}

	private int getSegmentPopularity(Set<MarketSegment> seg) {
		return MarketSegment.marketSegmentSize(seg);
	}

	/**
	 * Function updates the bid history list with a new number of impressions won
	 * @param adnetReport - the network report that is issued on a given day
	 */
	private void updateBidHistory(AdNetworkReport adnetReport) {
		//Loop over all entries in report (see example at bottom of file)
		for (AdNetworkKey adnetKey : adnetReport.keys()) {
			AdNetworkReportEntry entry = adnetReport.getEntry(adnetKey);
			//Find the corresponding day in bid history
			for (ImpBidTrackingObject bid : impBidHistory) {
				if (bid.getDay() == day-1) {
					//Update impressions won on that day
					int newImpsWon = entry.getWinCount();
					int currBidsWon = bid.getBidsWon(adnetKey.getCampaignId());
					bid.setImpsWon(adnetKey.getCampaignId(), currBidsWon + newImpsWon);
				}
			}
		}

		if (verbose_printing) {
			for (ImpBidTrackingObject bid : impBidHistory) {
				if (bid.getDay() == day - 1) {
					for (int campKey : bid.getImpsMap().keySet()) {
						System.out.println("Day: " + (day-1) + " - Camp: " + campKey + " - Imps won: " + bid.getBidsWon(campKey));
					}
				}
			}
		}
	}

	private double getPIPPopAtomic(MarketSegment s, int t) {
		double pop = 0.0;

		cleanPostedCampaignList();

		for (CampaignData camp : postedCampaigns) {
			int campKey = camp.id;

			if (camp.targetSegment.contains(s)) {
				if (camp.dayEnd > t) {
					pop = pop + (double)camp.reachImps / (double)(MarketSegment.marketSegmentSize(camp.targetSegment) * (camp.dayEnd - t));
					//System.out.println("Day End: " + camp.dayEnd + " - t: " + t + " - Seg size: " + MarketSegment.marketSegmentSize(camp.targetSegment));
					//System.out.println("Partial POP; " + pop);
				}
			}
		}

		//System.out.println(Arrays.toString(postedCampaigns.toArray()));
		//System.out.println("Segment: " + s + " - Atomic pop: " + pop);

		return pop;
	}

	private double getPIPPop(Set<MarketSegment> S, int dayEnd, int dayStart) {
		double pop = 0.0;

		List<Integer> T = new ArrayList<>();
		for (int i=dayStart ; i<= dayEnd; i++) {
			T.add(i);
		}

		for (MarketSegment s : S) {
			for (int t : T) {
				Set<MarketSegment> marketSet = new HashSet<MarketSegment>();
				marketSet.add(s);
				pop = pop + MarketSegment.marketSegmentSize(marketSet) * getPIPPopAtomic(s, t);
			}
		}

		System.out.println(Arrays.toString(T.toArray()));
		System.out.println("T size: "+T.size() + " - S soze: " + MarketSegment.marketSegmentSize(S) + " - pop: " + pop);
		//pop = pop / (T.size() * MarketSegment.marketSegmentSize(S));
		return pop / (T.size() * MarketSegment.marketSegmentSize(S));
	}

	/**
	 * Class represents a pair:
	 * bidBundle - the bid bundle sent on a given day
	 * impsMap - map of campaign id to imps won
	 */
	private class ImpBidTrackingObject {
		int day;
		AdxBidBundle bundle;
		Map<Integer,Integer> impsMap;

		public ImpBidTrackingObject(int day, AdxBidBundle bundle, Map<Integer, Integer> impsMap) {
			this.day = day;
			this.bundle = bundle;
			this.impsMap = impsMap;
		}

		public int getBidsWon(int campID) {
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

	/**
	 * Class represents a single Ucs bid history item with variables:
	 * bid		- The bid value for the day
	 * ucsLevel - The ucs level achieved
	 */
	private class UcsBidObject {
		int day;
		double bid;
		double ucsLevel;

		public UcsBidObject(int day, double bid, double ucsLevel) {
			this.day = day;
			this.bid = bid;
			this.ucsLevel = ucsLevel;
		}

		public double getBid() {
			return this.bid;
		}

		public double getUcsLevel() {
			return this.ucsLevel;
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
