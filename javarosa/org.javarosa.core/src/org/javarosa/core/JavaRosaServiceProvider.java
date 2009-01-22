package org.javarosa.core;

import java.util.Hashtable;

import org.javarosa.core.api.IDaemon;
import org.javarosa.core.api.IDisplay;
import org.javarosa.core.api.IView;
import org.javarosa.core.services.IService;
import org.javarosa.core.services.ITransportManager;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.StorageManager;
import org.javarosa.core.services.TransportManager;
import org.javarosa.core.services.UnavailableServiceException;
import org.javarosa.core.services.transport.storage.RmsStorage;
import org.javarosa.core.util.PrefixTree;
import org.javarosa.core.util.externalizable.CannotCreateObjectException;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * JavaRosaServiceProvider is a singleton class that grants access to JavaRosa's
 * core services, Storage, Transport, and Property Management. New services can
 * also be registered with the Service Provider.
 * 
 * @author Brian DeRenzi
 * @author Clayton Sims
 *
 */
public class JavaRosaServiceProvider {
	protected static JavaRosaServiceProvider instance;
	
	private Hashtable daemons;

	private IDisplay display;
	
	private StorageManager storageManager;
    private ITransportManager transportManager;
    private PropertyManager propertyManager;
	
	Hashtable services;
	private PrefixTree prototypes;
	
	public JavaRosaServiceProvider() {
		services = new Hashtable();
		prototypes = new PrefixTree();
		daemons = new Hashtable();
	}
	
	public static JavaRosaServiceProvider instance() {
		if(instance == null) {
			instance = new JavaRosaServiceProvider();
		}
		return instance;
	}

	/**
	 * Initialize the platform.  Setup things like the RMS for the forms, the transport manager...
	 */
	public void initialize() {
		// For right now do nothing, to conserve memory we'll load Providers when they're asked for
	}

	/**
	 * Should be called by the midlet to set the display
	 * @param d - the j2me disply
	 */
	public void setDisplay(IDisplay d) {
		instance.display = d;
	}

	/**
	 * @return the display
	 */
	public IDisplay getDisplay() {
		return instance.display;
	}

	/**
	 * Display the view that is passed in.
	 * @param view
	 */
	public void showView(IView view) {
		instance.display.setView(view);
	}
	
	public StorageManager getStorageManager() {
			if(storageManager == null) {
				storageManager = new StorageManager();
				this.registerService(storageManager);
			}
			return storageManager;
	}
	
	public ITransportManager getTransportManager() {
		if(transportManager == null) {
			String[] classes = {
					"org.javarosa.core.services.transport.ByteArrayPayload",
					"org.javarosa.core.services.transport.MultiMessagePayload",
					"org.javarosa.core.services.transport.DataPointerPayload"
			};		
			registerPrototypes(classes);
			transportManager = new TransportManager(new RmsStorage());
			this.registerService(transportManager);
		}
		return transportManager;
	}
	
	public PropertyManager getPropertyManager() {
		if(propertyManager == null) {
			propertyManager = new PropertyManager();
			this.registerService(propertyManager);
		}
		return propertyManager;
	}
	

	public void registerDaemon(IDaemon daemon, String name) {
		daemons.put(name, daemon);
	}
	
	public IDaemon getDaemon(String name) {
		IDaemon daemon = (IDaemon)daemons.get(name);
		//Do we want to handle the null case with an exception, like with services?
		return daemon;
	}
	
	public void registerService(IService service) {
		services.put(service.getName(), service);
	}
	
	public IService getService(String serviceName) throws UnavailableServiceException {
		IService service = (IService)services.get(serviceName);
		if( service == null) {
			throw new UnavailableServiceException("The JavaRosaServiceProvider received a request for the service " + serviceName + ", which was not registered");
		} else {
			return service; 
		}
	}
	
	public void registerPrototype (String className) {
		prototypes.addString(className);
		
		try {
			PrototypeFactory.getInstance(Class.forName(className));
		} catch (ClassNotFoundException e) {
			throw new CannotCreateObjectException(className + ": not found");
		}
	}
	
	public void registerPrototypes (String[] classNames) {
		for (int i = 0; i < classNames.length; i++)
			registerPrototype(classNames[i]);
	}
	
	public PrefixTree getPrototypes () {
		return prototypes;
	}
}
