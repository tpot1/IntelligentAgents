/*
 * AgentRepository.java
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
package edu.umich.eecs.tac.sim;

import edu.umich.eecs.tac.props.AdvertiserInfo;
import se.sics.tasim.sim.SimulationAgent;

import java.util.Map;

/**
 * The agent repository holds references to all agents in the TAC/AA simulation.
 * 
 * @author Patrick Jordan
 */
public interface AgentRepository {

	/**
	 * Get the advertiser information mapping.
	 * 
	 * @return the advertiser information mapping.
	 */
	Map<String, AdvertiserInfo> getAdvertiserInfo();

	/**
	 * Get the list of publisher agents.
	 * 
	 * @return the list of publisher agents.
	 */
	SimulationAgent[] getPublishers();

	/**
	 * Get the sales analyst.
	 * 
	 * @return the sales analyst.
	 */
	SalesAnalyst getSalesAnalyst();

	/**
	 * Returns the number of advertisers in the simulation.
	 * 
	 * @return the number of advertisers in the simulation.
	 */
	int getNumberOfAdvertisers();

	/**
	 * Returns the addresses of advertisers in the simulation.
	 * 
	 * @return the addresses of advertisers in the simulation.
	 */
	String[] getAdvertiserAddresses();
}
