/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) 2009-2010 Prosys PMS Ltd., <http://www.prosysopc.com>. 
 * All rights reserved.
 */
package fi.opc.ua.client;

import java.util.Calendar;

import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionAliveListener;

/**
 * A sampler listener for subscription alive events.
 */
public class ASSubscriptionAliveListener implements SubscriptionAliveListener {

	@Override
	public void onAlive(Subscription s) {
		// the client acknowledged that the
		// connection is alive,
		// although there were no changes to send
		AggregateServerConsoleClient.println(String.format(
				"%tc Subscription alive: ID=%d lastAlive=%tc",
				Calendar.getInstance(), s.getSubscriptionId().getValue(),
				s.getLastAlive()));
	}

	@Override
	public void onTimeout(Subscription s) {
		// the client did not acknowledged that the
		// connection is alive,
		// and the maxKeepAliveCount has been
		// exceeded
		AggregateServerConsoleClient.println(String.format(
				"%tc Subscription timeout: ID=%d lastAlive=%tc",
				Calendar.getInstance(), s.getSubscriptionId().getValue(),
				s.getLastAlive()));

	}

};