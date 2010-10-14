package org.javarosa.demo.applogic;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.midlet.MIDlet;

import org.javarosa.core.model.CoreModelModule;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.core.services.transport.payload.IDataPayload;
import org.javarosa.core.util.JavaRosaCoreModule;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.demo.properties.DemoAppProperties;
import org.javarosa.demo.util.MetaPreloadHandler;
import org.javarosa.formmanager.FormManagerModule;
import org.javarosa.j2me.J2MEModule;
import org.javarosa.j2me.util.DumpRMS;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.location.LocationModule;
import org.javarosa.model.xform.XFormsModule;
import org.javarosa.resources.locale.LanguagePackModule;
import org.javarosa.resources.locale.LanguageUtils;
import org.javarosa.services.transport.TransportManagerModule;
import org.javarosa.services.transport.TransportMessage;
import org.javarosa.services.transport.impl.simplehttp.SimpleHttpTransportMessage;
import org.javarosa.user.activity.UserModule;
import org.javarosa.user.model.User;
import org.javarosa.user.utility.UserUtility;

public class JRDemoContext {

	private static JRDemoContext instance;
	
	public static JRDemoContext _ () {
		if (instance == null) {
			instance = new JRDemoContext();
		}
		return instance;
	}
	
	private MIDlet midlet;
	private User user;

	
	public void setMidlet(MIDlet m) {
		this.midlet = m;
		J2MEDisplay.init(m);
	}
	
	public MIDlet getMidlet() {
		return midlet;
	}
	
	public void init (MIDlet m) {
		DumpRMS.RMSRecoveryHook(m);
		
		loadModules();
		
		//After load modules, so polish translations can be inserted.
		setMidlet(m);
		
		addCustomLanguages();
		setProperties();
			
		UserUtility.populateAdminUser();
		loadRootTranslator();
	}	

	private void loadModules() {
		new J2MEModule().registerModule();
		new JavaRosaCoreModule().registerModule();
		new CoreModelModule().registerModule();
		new XFormsModule().registerModule();
		new TransportManagerModule().registerModule();
		new UserModule().registerModule();
		new FormManagerModule().registerModule();
		new LanguagePackModule().registerModule();
		new LocationModule().registerModule();
	}
	
	
	private void addCustomLanguages() {
		Localization.registerLanguageFile("pt", "/messages_jrdemo_pt.txt");		
		Localization.registerLanguageFile("default", "/messages_jrdemo_default.txt");
	}
	
	private void setProperties() {
		final String POST_URL = midlet.getAppProperty("JRDemo-Post-Url");
		final String FORM_URL = midlet.getAppProperty("Form-Server-Url");
		final String LANGUAGE = midlet.getAppProperty("cur_locale");
		PropertyManager._().addRules(new JavaRosaPropertyRules());
		PropertyManager._().addRules(new DemoAppProperties());

		PropertyUtils.initializeProperty("DeviceID", PropertyUtils.genGUID(25));

		PropertyUtils.initializeProperty(DemoAppProperties.POST_URL_PROPERTY, POST_URL);
		PropertyUtils.initializeProperty(DemoAppProperties.FORM_URL_PROPERTY, FORM_URL);
		
		LanguageUtils.initializeLanguage(false, LANGUAGE == null ? "default" : LANGUAGE);

	}
	
	public void setUser (User u) {
		this.user = u;
	}
	
	public User getUser () {
		return user;
	}
	
	
	public TransportMessage buildMessage(IDataPayload payload) {
		//Right now we have to just give the message the stream, rather than the payload,
		//since the transport layer won't take payloads. This should be fixed _as soon 
		//as possible_ so that we don't either (A) blow up the memory or (B) lose the ability
		//to send payloads > than the phones' heap.
		
		try {
			return new SimpleHttpTransportMessage(payload.getPayloadStream(), PropertyManager._().getSingularProperty(DemoAppProperties.POST_URL_PROPERTY));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error Serializing Data to be transported");
		}
	}
	
	public Vector<IPreloadHandler> getPreloaders() {
		Vector<IPreloadHandler> handlers = new Vector<IPreloadHandler>();
		MetaPreloadHandler meta = new MetaPreloadHandler(this.getUser());
		handlers.addElement(meta);
		return handlers;		
	}
	
	public Vector<IFunctionHandler> getFuncHandlers () {
		return null;
	}
	
	public void loadRootTranslator(){
		ReferenceManager._().addRootTranslator(new RootTranslator("jr://images/", "jr://resource/"));
		ReferenceManager._().addRootTranslator(new RootTranslator("jr://audio/", "jr://resource/"));
	}
}
