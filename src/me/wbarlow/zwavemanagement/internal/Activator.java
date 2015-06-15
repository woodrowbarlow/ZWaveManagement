package me.wbarlow.zwavemanagement.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import me.wbarlow.zwavemanagement.logging.Logger;
import me.wbarlow.zwavemanagement.logging.LoggerFactory;

/**
 * Activate the Z-Wave Demo and control the bundle's state.
 * @author Woodrow Barlow
 *
 */
public class Activator implements BundleActivator {

	/*
	 *  == A Description of Bundle States ==
	 *
	 *  # INSTALLED
	 *  When a bundle is first loaded into the execution environment, it is in
	 *  the "installed" state. This means only that the execution environment is
	 *  aware of the bundle's existence. This bundle cannot yet enter an active
	 *  state.
	 *
	 *  # RESOLVED
	 *  A bundle will automatically change from the "Installed" state to the
	 *  "Resolved" state if it does not have any unmet dependencies, as declared
	 *  by that bundle's manifest. Once a bundle has resolved, it can move into
	 *  an active state at will. The bundle can change from "Resolved" to
	 *  "Installed" at any time if any of its dependencies become unavailable.
	 *
	 *  # STARTING
	 *  The "Starting" state is a very brief transition state between "Resolved"
	 *  and "Active". During this state, the bundle can allocate any resources
	 *  it needs. This state indicates that the bundle is starting, but that it
	 *  is not yet ready to be used by other bundles.
	 *
	 *  # ACTIVE
	 *  The bundle becomes "Active" once it has finished allocating any
	 *  resources it needs. The transition from "Starting" to "Active" is
	 *  automatic. When a bundle is active, it should be capable of being used
	 *  by other bundles which might depend upon it.
	 *
	 *  # STOPPING
	 *  The "Stopping" state is a very brief transition state which occurs when
	 *  a bundle leaves the "Active" state. During this state, the bundle
	 *  performs clean-up and de-allocation of resources as appropriate. When
	 *  this state is complete, the bundle will automatically return to the
	 *  "Resolved" state.
	 */

	private static BundleContext context;
	private BundleThread thread;

	private static final Logger logger = LoggerFactory.getLogger(Activator.class);

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * While this method is executing, the bundle is in the "Starting" state,
	 * which is a pre-requisite for entering the "Active" state. The bundle must
	 * be in the "Resolved" state before it can enter the "Starting" state.
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		/*
		 * The "start" method should not be used for actually doing the work,
		 * only for setting up / initializing the bundle. An OSGi bundle's
		 * state does not become "Active" until the start method has
		 * successfully executed; therefore, the best way to do this is actually
		 * to spawn a thread in the start method and let the thread do the
		 * actual work.
		 */
		logger.info("The bundle is now STARTING.");
		this.thread = new BundleThread();
		this.thread.start();
		Activator.context = bundleContext;
	}

	/*
	 * (non-Javadoc)
	 * While this method is executing, the bundle is in the "Stopping" state,
	 * which occurs when returning to the "Resolved" state from the "Active"
	 * state.
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		/*
		 * Basic cleanup to be executed when the bundle changes from "Active" to
		 * any other state. We'll join the thread and stop it.
		 */
		logger.info("The bundle is now STOPPING.");
		this.thread.stopThread();
		this.thread.join();
		LoggerFactory.close();
		Activator.context = null;
	}

}
