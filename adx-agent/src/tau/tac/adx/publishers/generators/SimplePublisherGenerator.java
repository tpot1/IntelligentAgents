package tau.tac.adx.publishers.generators;

import tau.tac.adx.ads.properties.AdAttributeProbabilityMaps;
import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.devices.Device;
import tau.tac.adx.generators.GenericGenerator;
import tau.tac.adx.publishers.AdxPublisher;
import tau.tac.adx.publishers.reserve.ReservePriceManager;
import tau.tac.adx.publishers.reserve.UserAdTypeReservePriceManager;
import tau.tac.adx.sim.TACAdxConstants;
import tau.tac.adx.users.AdxUser;
import tau.tac.adx.users.properties.AdxUserAttributeProbabilityMaps;
import tau.tac.adx.users.properties.Age;
import tau.tac.adx.users.properties.Gender;
import tau.tac.adx.users.properties.Income;
import tau.tac.adx.util.MapGenerator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * A naive implementation of the {@link GenericGenerator} interface. Randomizes
 * each characteristic of the {@link AdxUser}.
 * 
 * @author greenwald
 * 
 */
public class SimplePublisherGenerator implements AdxPublisherGenerator {

	/**
	 * The maximum {@link ReservePriceManager}'s baseline range.
	 */
	private static final double MAX_BASELINE_RANGE = 0;

	/**
	 * {@link MapGenerator} for {@link AdType}.
	 */
	private MapGenerator<AdType> adTypeMapGenerator = new MapGenerator<AdType>();
	/**
	 * {@link MapGenerator} for {@link Age}.
	 */
	private MapGenerator<Age> ageMapGenerator = new MapGenerator<Age>();
	/**
	 * {@link MapGenerator} for {@link Device}.
	 */
	private MapGenerator<Device> deviceMapGenerator = new MapGenerator<Device>();
	/**
	 * {@link MapGenerator} for {@link Gender}.
	 */
	private MapGenerator<Gender> genderMapGenerator = new MapGenerator<Gender>();
	/**
	 * {@link MapGenerator} for {@link Income}.
	 */
	private MapGenerator<Income> incomeMapGenerator = new MapGenerator<Income>();
	/**
	 * {@link Logger} instance.
	 */
	private final Logger logger = Logger.getLogger(this.getClass()
			.getCanonicalName());

	/**
	 * @see GenericGenerator#generate(int)
	 */
	@Override
	public Collection<AdxPublisher> generate(int amount) {
		Collection<AdxPublisher> publishers = new LinkedList<AdxPublisher>();
		for (int i = 0; i < amount; i++) {
			publishers.add(getRandomPublisher());
		}
		normalizePublisherPopularity(publishers);
		logger.fine("Generated " + amount + " " + AdxUser.class.getName() + "s");
		return publishers;
	}

	/**
	 * @return A {@link Random} {@link AdxUser}.
	 */
	private AdxPublisher getRandomPublisher() {
		AdxUserAttributeProbabilityMaps probabilityMaps = randomAttributeProbabilityMaps();
		AdAttributeProbabilityMaps adAttributeProbabilityMaps = randomAdAttributeProbabilityMaps();
		Map<Device, Double> deviceProbabilityMap = randomDeviceProbabilityMaps();
		double relativePopularity = Math.random();
		double pImpressions = Math.random();
		UserAdTypeReservePriceManager reservePriceManager = randomReservePriceManager();
		String name = randomName();
		AdxPublisher publisher = new AdxPublisher(probabilityMaps,
				adAttributeProbabilityMaps, deviceProbabilityMap,
				relativePopularity, pImpressions, reservePriceManager, name);
		return publisher;
	}

	/**
	 * Normalizes the publishers relative popularity so that their sum will
	 * equal 1.
	 * 
	 * @param publishers
	 *            {@link Collection} of {@link AdxPublisher publishers} to
	 *            normalize their <b>relative popularity</b>.
	 */
	private static void normalizePublisherPopularity(
			Collection<AdxPublisher> publishers) {
		double sum = 0;
		for (AdxPublisher publisher : publishers) {
			sum += publisher.getRelativePopularity();
		}
		for (AdxPublisher publisher : publishers) {
			publisher.setRelativePopularity(publisher.getRelativePopularity()
					/ sum);
		}
	}

	/**
	 * @return Randomized {@link AdAttributeProbabilityMaps}.
	 */
	private AdAttributeProbabilityMaps randomAdAttributeProbabilityMaps() {
		Map<AdType, Double> adTypeDistribution = adTypeMapGenerator
				.randomizeProbabilityMap(AdType.values());
		AdAttributeProbabilityMaps probabilityMaps = new AdAttributeProbabilityMaps(
				adTypeDistribution);
		return probabilityMaps;
	}

	/**
	 * @return Randomized {@link AdxUserAttributeProbabilityMaps}.
	 */
	private AdxUserAttributeProbabilityMaps randomAttributeProbabilityMaps() {
		Map<Age, Double> ageDistribution = ageMapGenerator
				.randomizeProbabilityMap(Age.values());
		Map<Gender, Double> genderDistribution = genderMapGenerator
				.randomizeProbabilityMap(Gender.values());
		Map<Income, Double> incomeDistribution = incomeMapGenerator
				.randomizeProbabilityMap(Income.values());
		AdxUserAttributeProbabilityMaps probabilityMaps = new AdxUserAttributeProbabilityMaps(
				ageDistribution, genderDistribution, incomeDistribution);
		return probabilityMaps;
	}

	/**
	 * @return A random generated {@link Device} proability map.
	 */
	private Map<Device, Double> randomDeviceProbabilityMaps() {
		Map<Device, Double> deviceProbabilityMap = deviceMapGenerator
				.randomizeProbabilityMap(Device.values());
		return deviceProbabilityMap;
	}

	/**
	 * @return A random generated {@link AdxPublisher publisher} name.
	 */
	private static String randomName() {
		Random random = new Random();
		return "Publisher-" + Math.abs(random.nextInt());
	}

	/**
	 * @return A random generated {@link ReservePriceManager}.
	 */
	private static UserAdTypeReservePriceManager randomReservePriceManager() {
		double dailyBaselineAverage = Math.random()
				* TACAdxConstants.MAX_SIMPLE_PUBLISHER_AD_PRICE;
		double baselineRange = Math.random() * MAX_BASELINE_RANGE;
		double updateCoefficient = Math.random();
		UserAdTypeReservePriceManager reservePriceManager = new UserAdTypeReservePriceManager(
				dailyBaselineAverage, baselineRange, updateCoefficient);
		return reservePriceManager;
	}
}
