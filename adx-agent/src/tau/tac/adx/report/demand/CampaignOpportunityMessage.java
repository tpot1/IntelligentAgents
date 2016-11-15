package tau.tac.adx.report.demand;

import se.sics.isl.transport.TransportReader;
import se.sics.isl.transport.TransportWriter;
import se.sics.tasim.props.SimpleContent;
import tau.tac.adx.demand.Campaign;
import tau.tac.adx.report.adn.MarketSegment;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author Mariano Schain
 * 
 */
public class CampaignOpportunityMessage extends SimpleContent {
	private static final long serialVersionUID = -2501549665857756943L;

	/**
	 * The publisher key.
	 */
	private static final String MARKET_SEGMENT_KEY = "USER_KEY";

	private int id;
	private Long reachImps;
	private int dayStart;
	private int dayEnd;
	private Set<MarketSegment> targetSegment = new HashSet<MarketSegment>();
	private double videoCoef;
	private double mobileCoef;
	private int day;

	public CampaignOpportunityMessage() {
	}

	public CampaignOpportunityMessage(Campaign campaign, int day) {
		this.id = campaign.getId();
		this.reachImps = campaign.getReachImps();
		this.dayStart = campaign.getDayStart();
		this.dayEnd = campaign.getDayEnd();
		this.targetSegment = campaign.getTargetSegment();
		this.videoCoef = campaign.getVideoCoef();
		this.mobileCoef = campaign.getMobileCoef();
		this.day = day;
	}

	public int getId() {
		return id;
	}

	public Long getReachImps() {
		return reachImps;
	}

	public long getDayStart() {
		return dayStart;
	}

	public long getDayEnd() {
		return dayEnd;
	}

	public Set<MarketSegment> getTargetSegment() {
		return targetSegment;
	}

	public double getVideoCoef() {
		return videoCoef;
	}

	public double getMobileCoef() {
		return mobileCoef;
	}

	public int getDay() {
		return day;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer().append(getTransportName())
				.append('[').append(id).append(',').append(reachImps)
				.append(',').append(dayStart).append(',').append(dayEnd)
				.append(',').append(targetSegment).append(',')
				.append(videoCoef).append(',').append(mobileCoef).append(',')
				.append(day).append(',');
		return params(buf).append(']').toString();
	}

	// -------------------------------------------------------------------
	// Transportable (externalization support)
	// -------------------------------------------------------------------

	/**
	 * Returns the transport name used for externalization.
	 */
	@Override
	public String getTransportName() {
		return getClass().getName();
	}

	@Override
	public void read(TransportReader reader) throws ParseException {
		if (isLocked()) {
			throw new IllegalStateException("locked");
		}
		id = reader.getAttributeAsInt("id");
		reachImps = reader.getAttributeAsLong("reachImps");
		dayStart = reader.getAttributeAsInt("dayStart");
		dayEnd = reader.getAttributeAsInt("dayEnd");
		videoCoef = reader.getAttributeAsDouble("videoCoef");
		mobileCoef = reader.getAttributeAsDouble("mobileCoef");
		day = reader.getAttributeAsInt("day");
		while (reader.nextNode(MARKET_SEGMENT_KEY, false)) {
			targetSegment.add(MarketSegment.valueOf(reader
					.getAttribute(MARKET_SEGMENT_KEY)));
		}
		super.read(reader);
	}

	@Override
	public void write(TransportWriter writer) {
		writer.attr("id", id).attr("reachImps", reachImps)
				.attr("dayStart", dayStart).attr("dayEnd", dayEnd)
				.attr("videoCoef", videoCoef).attr("mobileCoef", mobileCoef)
				.attr("day", day);
		for (MarketSegment marketSegment : targetSegment) {
			writer.node(MARKET_SEGMENT_KEY)
					.attr(MARKET_SEGMENT_KEY, marketSegment.toString())
					.endNode(MARKET_SEGMENT_KEY);
		}
		super.write(writer);
	}

}
