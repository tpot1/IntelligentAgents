/*
 * DefaultUserQueryManagerBuilder.java
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
package tau.tac.adx.users;

import edu.umich.eecs.tac.util.config.ConfigProxy;
import tau.tac.adx.sim.AdxAgentRepository;

import java.util.Random;

/**
 * {@link AdxUserBehaviorBuilder} implementation.
 * 
 * @author greenwald
 */
public class AdxUserQueryManagerBuilder implements
		AdxUserBehaviorBuilder<AdxUserQueryManager> {
	/**
	 * @see tau.tac.adx.users.AdxUserBehaviorBuilder#build(edu.umich.eecs.tac.util.config.ConfigProxy,
	 *      tau.tac.adx.sim.AdxAgentRepository, java.util.Map, java.util.Map,
	 *      java.util.Random)
	 */
	@Override
	public DefaultAdxUserQueryManager build(ConfigProxy userConfigProxy,
			AdxAgentRepository repository, Random random) {
		return new DefaultAdxUserQueryManager(repository.getPublisherCatalog(),
				repository.getUserPopulation(),
				repository.getDeviceDistributionMap(),
				repository.getAdTypeDistributionMap(), random);
	}
}