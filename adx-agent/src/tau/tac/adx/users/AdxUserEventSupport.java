/*
 * UserEventSupport.java
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

import tau.tac.adx.auction.AdxAuctionResult;
import tau.tac.adx.props.AdxQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Patrick Jordan, Lee Callender
 * @param <T>
 *            User view manager type.
 */
public class AdxUserEventSupport {

	private final List<AdxUserEventListener> listeners;

	public AdxUserEventSupport() {
		listeners = new ArrayList<AdxUserEventListener>();
	}

	public boolean addUserEventListener(AdxUserEventListener listener) {
		return listeners.add(listener);
	}

	public boolean containsUserEventListener(AdxUserEventListener listener) {
		return listeners.contains(listener);
	}

	public boolean removeUserEventListener(AdxUserEventListener listener) {
		return listeners.remove(listener);
	}

	public void fireQueryIssued(AdxQuery query) {
		for (AdxUserEventListener listener : listeners) {
			listener.queryIssued(query);
		}
	}

	/**
	 * Auction was performed by the <b>ADX</b> and results are given as
	 * parameters.
	 * 
	 * @param auctionResult
	 *            {@link AdxAuctionResult}.
	 * @param query
	 *            Issuing {@link AdxQuery}.
	 * @param user
	 *            Participating {@link AdxUser}.
	 */
	public void fireAuctionPerformed(AdxAuctionResult auctionResult,
			AdxQuery query, AdxUser user) {
		for (AdxUserEventListener listener : listeners) {
			listener.auctionPerformed(auctionResult, query, user);
		}
	}

}
