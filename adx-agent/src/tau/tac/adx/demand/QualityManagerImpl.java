package tau.tac.adx.demand;

import tau.tac.adx.AdxManager;

import java.util.HashMap;

/**
 * 
 * @author Mariano Schain
 * 
 */
public class QualityManagerImpl implements QualityManager {
	private final static double MU = 0.6;

	private final HashMap<String, Double> advertisersScores;

	public QualityManagerImpl() {
		advertisersScores = new HashMap<String, Double>();
	}

	@Override
	public void addAdvertiser(String advertiser) {
		advertisersScores.put(advertiser, 1.0);
	}

	@Override
	public double updateQualityScore(String advertiser, Double score) {
		Double newScore = (1 - MU) * getQualityScore(advertiser) + MU * score;
		advertisersScores.put(advertiser, newScore);
		AdxManager.getInstance().getSimulation()
				.broadcastAdNetworkQualityRating(advertiser, newScore);
		return newScore;
	}

	@Override
	public double getQualityScore(String advertiser) {
		return advertisersScores.get(advertiser);
	}
	
	@Override
	public String logToString() {
		String ret = new String("Quality Retings ");
		for (String adv : advertisersScores.keySet()) {
			ret = ret + adv + ": " + advertisersScores.get(adv);			
		}
		return ret;
	}


}
