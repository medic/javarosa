package org.javarosa.demo
.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import org.javarosa.core.api.State;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.demo.applogic.JRDemoContext;
import org.javarosa.demo.applogic.JRDemoFormListState;
import org.javarosa.demo.applogic.JRDemoLanguageSelectState;
import org.javarosa.demo.applogic.JRDemoSavedFormListState;
import org.javarosa.demo.applogic.JRDemoSplashScreenState;
import org.javarosa.patient.model.Patient;
import org.javarosa.resources.locale.LanguageUtils;
import org.javarosa.services.transport.http.HttpConnectionROSA;

import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;	
import org.xmlpull.v1.XmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


public class JRDemoUtil {

	static OrderedHashtable formList;
	static OrderedHashtable remoteFormList;
	
	private static OrderedHashtable savedFormList;

	public static String getAppProperty(String key) {
		return JRDemoContext._().getMidlet().getAppProperty(key);
	}

	public static void start() {
		Vector propVal =PropertyManager._().getProperty(JavaRosaPropertyRules.CURRENT_LOCALE);

		
		if (propVal == null || propVal.size() == 0)
		{
			LanguageUtils.initializeLanguage(true, "default");
			new JRDemoLanguageSelectState().start();
		}
		else
		{
			LanguageUtils.initializeLanguage(true, (String) propVal.elementAt(0));
			new JRDemoSplashScreenState().start();
		}
	}

	public static void exit() {
		JRDemoContext._().getMidlet().notifyDestroyed();
	}

	public static void goToList(boolean formList) {
		((State) (formList ? new JRDemoFormListState()
				: new JRDemoSavedFormListState())).start();
	}


	// cache this because the storage utility doesn't yet support quick
	// meta-data iteration
	public static OrderedHashtable getFormList() {
		//if (formList == null) { No caching for now
			formList = new OrderedHashtable();
			IStorageUtility forms = StorageManager
					.getStorage(FormDef.STORAGE_KEY);
			IStorageIterator fi = forms.iterate();
			while (fi.hasMore()) {
				FormDef f = (FormDef) fi.nextRecord();
				formList.put(new Integer(f.getID()), f.getTitle());
			}
		//}
		return formList;
	}


	// cache this because the storage utility doesn't yet support quick
	// meta-data iteration
	public static OrderedHashtable getSavedFormList() {
		savedFormList = new OrderedHashtable();
		IStorageUtility forms = StorageManager
				.getStorage(FormInstance.STORAGE_KEY);
		IStorageIterator fi = forms.iterate();
		while (fi.hasMore()) {
			FormInstance f = (FormInstance) fi.nextRecord();
			System.out.println("adding saved form: " + f.getID() + " - "
					+ f.getName() + " - " + f.getFormId());
			savedFormList.put(new Integer(f.getID()), new SavedFormListItem(f.getName(),f.getFormId(),f.getID()) );
		}
		return savedFormList;
	}

	// cache this because the storage utility doesn't yet support quick
	// meta-data iteration
	public static FormInstance getSavedFormInstance(int formID, int instanceID) {
		IStorageUtility forms = StorageManager
				.getStorage(FormInstance.STORAGE_KEY);
		IStorageIterator fi = forms.iterate();
		FormInstance f,result = null;
		
		while (fi.hasMore()) {
			f = (FormInstance) fi.nextRecord();

			if ( (f.getFormId()==formID) && ( f.getID()==instanceID )  )
			{
				result = f;
			}
		}
		return result;
	}

	
	// cache this because the storage utility doesn't yet support quick
	// meta-data iteration
	public static FormInstance getSavedFormInstance(int formInstanceId) {
			IStorageUtility forms = StorageManager
					.getStorage(FormInstance.STORAGE_KEY);
			IStorageIterator fi = forms.iterate();
			while (fi.hasMore()) {
				FormInstance f = (FormInstance) fi.nextRecord();
				if (f.getID() == formInstanceId)
				{
					return f;
				}
			}
			return null;
	}

	
    public static OrderedHashtable readFormListXML(String formListXmlString)
    {
        OrderedHashtable hashData = new OrderedHashtable();
        String currentResourceUrl = "";
        String currentFormName = "";

        try
        {
            KXmlParser kxmlParser = new KXmlParser();
            kxmlParser.setInput(new InputStreamReader(new ByteArrayInputStream(formListXmlString.getBytes())));
            kxmlParser.nextTag();
            kxmlParser.require(XmlPullParser.START_TAG, null, "forms");

            while ( kxmlParser.nextTag() != XmlPullParser.END_TAG)
            {
                currentResourceUrl = kxmlParser.getAttributeValue("","url");
                currentFormName = kxmlParser.nextText();
                hashData.put(currentFormName, currentResourceUrl);
                kxmlParser.require(XmlPullParser.END_TAG, null, "form");
            }

        }
        catch (XmlPullParserException e)
        {
            System.out.println("Error in parsing formList");
        }
        catch (IOException e)
        {
            System.out.println("Error in input formList");
        }
        return hashData;
    }

	// cache this because the storage utility doesn't yet support quick
	// meta-data iteration
	public static String[] getLanguageList() {
		String[] listLanguages = Localization.getGlobalLocalizerAdvanced().getAvailableLocales();
		return listLanguages;
	}



}
