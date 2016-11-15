package tau.tac.adx.publishers.reserve;

import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.devices.Device;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.report.adn.MarketSegment;

import java.util.Set;

/**
 * Reserve price pair used to classify different reserve price to different
 * users and ads.
 * 
 * @author Tomer
 * 
 */
class ReservePriceType {

	/** {@link Set} of {@link MarketSegment}s. */
	private Set<MarketSegment> marketSegment;
	/** {@link AdType}. */
	private AdType adType;
	/** {@link Device}}. */
	private Device device;

	/**
	 * Constructor from {@link AdxQuery}.
	 * @param adxQuery {@link AdxQuery}.
	 * @param reservePriceManager TODO
	 */
	public ReservePriceType(AdxQuery adxQuery) {
		this.marketSegment = adxQuery.getMarketSegments();
		this.adType = adxQuery.getAdType();
		this.device = adxQuery.getDevice();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ReservePriceType [marketSegment=" + marketSegment + ", adType="
				+ adType + ", device=" + device + "]";
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((adType == null) ? 0 : adType.hashCode());
		result = prime * result + ((device == null) ? 0 : device.hashCode());
		result = prime * result
				+ ((marketSegment == null) ? 0 : marketSegment.hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReservePriceType other = (ReservePriceType) obj;
		if (adType != other.adType)
			return false;
		if (device != other.device)
			return false;
		if (marketSegment == null) {
			if (other.marketSegment != null)
				return false;
		} else if (!marketSegment.equals(other.marketSegment))
			return false;
		return true;
	}

}