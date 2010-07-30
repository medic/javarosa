/**
 * 
 */
package org.javarosa.formmanager.api;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.services.UnavailableServiceException;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.formmanager.api.transitions.FormEntryTransitions;
import org.javarosa.formmanager.view.IFormEntryView;

/**
 * Extension of {@link FormEntryController} for J2ME.
 * 
 * @author ctsims
 *
 */
public class JrFormEntryController extends FormEntryController {

	FormEntryTransitions transitions;
	IFormEntryView view;
	
	public JrFormEntryController(JrFormEntryModel model) {
		super(model);
		tryToInitDefaultLanguage(model);
	}

	private void tryToInitDefaultLanguage(JrFormEntryModel model) {
		//Try to set the current form locale based on the current app locale
		String[] languages = model.getLanguages();
		if(languages != null) {
			String locale = Localization.getGlobalLocalizerAdvanced().getLocale();
			if(locale != null) {
				for(String language : languages) {
					if(locale.equals(language)) {
						model.getForm().getLocalizer().setLocale(locale);
						break;
					}
				}
			}
		}
	}

	public JrFormEntryModel getModel () {
		return (JrFormEntryModel)super.getModel();
	}
	
	public void setView(IFormEntryView view) {
		this.view = view;
	}
	public void setTransitions(FormEntryTransitions transitions) {
		this.transitions = transitions;
	}
	
	public void start() {
		view.show();
	}
	
	/**
	 * Start from a specific index
	 * @param index
	 */
	public void start(FormIndex index){
		view.show(index);
	}
	
	public void abort() {
		transitions.abort();
	}
	
	public void saveAndExit(boolean formComplete) {
		if (formComplete){
			this.getModel().getForm().postProcessInstance();
		}
		transitions.formEntrySaved(this.getModel().getForm(),this.getModel().getForm().getInstance(),formComplete);
	}
	
	public void suspendActivity(int mediaType) throws UnavailableServiceException {
		transitions.suspendForMediaCapture(mediaType);
	}
	
	public void cycleLanguage () {
		setLanguage(getModel().getForm().getLocalizer().getNextLocale());
	}

}
