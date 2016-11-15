/**
 * 
 */
package tau.tac.adx.util;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import tau.tac.adx.ads.properties.generators.AdTypeGenerator;
import tau.tac.adx.ads.properties.generators.SimpleAdTypeGenerator;
import tau.tac.adx.auction.AuctionManager;
import tau.tac.adx.auction.SimpleAuctionManager;
import tau.tac.adx.auction.manager.AdxBidManager;
import tau.tac.adx.auction.manager.AdxBidManagerImpl;
import tau.tac.adx.auction.tracker.AdxBidTracker;
import tau.tac.adx.auction.tracker.AdxBidTrackerImpl;
import tau.tac.adx.auction.tracker.AdxSpendTracker;
import tau.tac.adx.auction.tracker.AdxSpendTrackerImpl;
import tau.tac.adx.devices.generators.DeviceGenerator;
import tau.tac.adx.devices.generators.SimpleDeviceGenerator;
import tau.tac.adx.publishers.generators.AdxPublisherGenerator;
import tau.tac.adx.publishers.generators.SimplePublisherGenerator;
import tau.tac.adx.sim.AdxAuctioneer;
import tau.tac.adx.sim.SimpleAdxAuctioneer;

/**
 * A simple {@link AbstractModule} implementation used for testing. All vlaues
 * are generated randomly.
 * 
 * @author greenwald
 * 
 */
public class AdxModule extends AbstractModule {

	/**
	 * @see com.google.inject.AbstractModule#configure()
	 */
	@Override
	protected void configure() {
		bind(AdxPublisherGenerator.class).to(SimplePublisherGenerator.class);
		bind(AdTypeGenerator.class).to(SimpleAdTypeGenerator.class);
		bind(DeviceGenerator.class).to(SimpleDeviceGenerator.class);
		bind(AdxAuctioneer.class).to(SimpleAdxAuctioneer.class).in(
				Singleton.class);
		bind(AdxBidManager.class).to(AdxBidManagerImpl.class).in(
				Singleton.class);
		bind(AdxSpendTracker.class).to(AdxSpendTrackerImpl.class).in(
				Singleton.class);
		bind(AdxBidTracker.class).to(AdxBidTrackerImpl.class).in(
				Singleton.class);
		bind(AuctionManager.class).to(SimpleAuctionManager.class).in(
				Singleton.class);
	}
}
