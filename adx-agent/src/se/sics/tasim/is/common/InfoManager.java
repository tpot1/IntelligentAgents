/**
 * TAC Supply Chain Management Simulator
 * http://www.sics.se/tac/    tac-dev@sics.se
 *
 * Copyright (c) 2001-2003 SICS AB. All rights reserved.
 *
 * SICS grants you the right to use, modify, and redistribute this
 * software for noncommercial purposes, on the conditions that you:
 * (1) retain the original headers, including the copyright notice and
 * this text, (2) clearly document the difference between any derived
 * software and the original, and (3) acknowledge your use of this
 * software in pertaining publications and reports.  SICS provides
 * this software "as is", without any warranty of any kind.  IN NO
 * EVENT SHALL SICS BE LIABLE FOR ANY DIRECT, SPECIAL OR INDIRECT,
 * PUNITIVE, INCIDENTAL OR CONSEQUENTIAL LOSSES OR DAMAGES ARISING OUT
 * OF THE USE OF THE SOFTWARE.
 *
 * -----------------------------------------------------------------
 *
 * InfoManager
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : Fri Feb 28 10:45:53 2003
 * Updated : $Date: 2008-04-04 20:42:56 -0500 (Fri, 04 Apr 2008) $
 *           $Revision: 3981 $
 * Purpose :
 *
 */
package se.sics.tasim.is.common;

public abstract class InfoManager {

	private InfoServer infoServer;
	private String name;

	protected InfoManager() {
	}

	final void init(InfoServer infoServer, String name) {
		this.infoServer = infoServer;
		this.name = name;
		init();
	}

	protected abstract void init();

	protected void registerType(String type) {
		infoServer.addInfoManager(type, this);
	}

	public abstract ResultManager createResultManager(String simulationType);

	public abstract ViewerCache createViewerCache(String simulationType);

} // InfoManager
