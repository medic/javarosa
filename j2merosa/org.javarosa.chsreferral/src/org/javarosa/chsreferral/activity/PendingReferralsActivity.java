/**
 * 
 */
package org.javarosa.chsreferral.activity;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import org.javarosa.chsreferral.model.PatientReferral;
import org.javarosa.chsreferral.storage.PatientReferralRMSUtility;
import org.javarosa.chsreferral.util.IPatientReferralFilter;
import org.javarosa.chsreferral.util.PendingReferralsContext;
import org.javarosa.chsreferral.view.PendingReferralsView;
import org.javarosa.chsreferral.view.ReferralsDetailView;
import org.javarosa.core.Context;
import org.javarosa.core.JavaRosaServiceProvider;
import org.javarosa.core.api.Constants;
import org.javarosa.core.api.IActivity;
import org.javarosa.core.api.ICommand;
import org.javarosa.core.api.IShell;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.patient.model.Patient;
import org.javarosa.patient.storage.PatientRMSUtility;
import org.javarosa.xpath.expr.XPathFuncExpr;

/**
 * The Pending Referrals activity is used to present a list of all of the
 * patient referrals that have not been closed to the user, and allows
 * for the opportunity to resolve those referrals.
 * 
 * @author Clayton Sims
 * @date Jan 23, 2009 
 *
 */
public class PendingReferralsActivity implements IActivity, CommandListener {
	
	IShell shell;
	
	PendingReferralsContext context;
	
	Vector pendingRefs = new Vector();
	
	PendingReferralsView pending;
	ReferralsDetailView details;
	
	private static final Command EXIT = new Command("Back", Command.EXIT, 1);
	private static final Command SELECT = new Command("Select", Command.ITEM, 1);
	
	public static final Command RESOLVE = new Command("Resolve", Command.ITEM, 1);

	/* (non-Javadoc)
	 * @see org.javarosa.core.api.IActivity#annotateCommand(org.javarosa.core.api.ICommand)
	 */
	public void annotateCommand(ICommand command) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.api.IActivity#contextChanged(org.javarosa.core.Context)
	 */
	public void contextChanged(Context globalContext) {
		this.context.mergeInContext(globalContext);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.api.IActivity#destroy()
	 */
	public void destroy() {
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.api.IActivity#getActivityContext()
	 */
	public Context getActivityContext() {
		return context;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.api.IActivity#halt()
	 */
	public void halt() {
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.api.IActivity#resume(org.javarosa.core.Context)
	 */
	public void resume(Context globalContext) {
		shell.setDisplay(this, pending);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.api.IActivity#setShell(org.javarosa.core.api.IShell)
	 */
	public void setShell(IShell shell) {
		this.shell = shell;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command, javax.microedition.lcdui.Displayable)
	 */
	public void commandAction(Command com, Displayable view) {
		Hashtable returnArgs = new Hashtable();
		if(view.equals(pending)) {
			if(com.equals(EXIT)) {
				shell.returnFromActivity(this, Constants.ACTIVITY_COMPLETE, returnArgs);
			} else if(com.equals(SELECT)) {
				PatientReferral ref = ((PatientReferral)pendingRefs.elementAt(pending.getSelectedIndex()));
				//TODO: Get patient here.
				details = new ReferralsDetailView("Referral for ");
				details.setReferral(ref);
				
				details.addCommand(EXIT);
				details.addCommand(RESOLVE);
				
				details.setCommandListener(this);
				
				shell.setDisplay(this, details);
			}
		} else if(view.equals(details)) {
			if(com.equals(EXIT)) {
				shell.setDisplay(this, pending);
			} else if(com.equals(RESOLVE)) {
				returnArgs.put(RESOLVE, details.getReferral());
				shell.returnFromActivity(this, Constants.ACTIVITY_COMPLETE, returnArgs); //shouldn't this be ACTIVITY_NEEDS_RESOLUTION?
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.api.IActivity#start(org.javarosa.core.Context)
	 */
	public void start(Context context) {
		this.context = new PendingReferralsContext(context);
		PatientReferralRMSUtility ref = (PatientReferralRMSUtility)JavaRosaServiceProvider.instance().getStorageManager().getRMSStorageProvider().getUtility(PatientReferralRMSUtility.getUtilityName());
		PatientRMSUtility pat = (PatientRMSUtility)JavaRosaServiceProvider.instance().getStorageManager().getRMSStorageProvider().getUtility(PatientRMSUtility.getUtilityName());
		IPatientReferralFilter filter = this.context.getReferralFilter();
		
		//TODO: Replace - Maybe with Entity Select Stuff? Seems super-appropriate.
		//droos: yes it does
		pending = new PendingReferralsView("Pending Referrals");
		try {
			Vector pendingVector = ref.getPendingReferrals();
			
			Enumeration en = pendingVector.elements();
			while(en.hasMoreElements()) {
				PatientReferral referral = (PatientReferral)en.nextElement();
				// TODO: Encode patient ID logic in a filter at the Shell Level. Right now this doesn't happen due to
				// my desire to not make the patient code end up in multiple places.
				if (filter.inFilter(referral) && (this.context.getPatientId() == -1 || (referral.getPatientId() == this.context.getPatientId()))) {
					Patient patient = new Patient();
					pat.retrieveFromRMS(referral.getPatientId(), patient);
					
					String ID = patient.getPatientIdentifier();
					String shortID = ID.substring(Math.max(ID.length() - 2, 0)); //this logic is duplicated from CommCarePatientEntity!
					
					int daysAgo = (int)(XPathFuncExpr.toNumeric(new Date()).doubleValue() - XPathFuncExpr.toNumeric(referral.getDateReferred()).doubleValue());
					String daysAgoStr = null;
					//#if commcare.lang.sw
					daysAgoStr = (daysAgo < 0 ? "From the futurrrrrre" : daysAgo == 0 ? "Leo" : daysAgo == 1 ? "Jana" : daysAgo == 2 ? "Juzi" : daysAgo + " days ago");
					//#else
					daysAgoStr = (daysAgo < 0 ? "From the futurrrrrre" : daysAgo == 0 ? "Today" : daysAgo == 1 ? "Yesterday" : daysAgo + " days ago");
					//#endif
					
					pending.append(patient.getInitials() + " - " + shortID + " - "
							+ referral.getType() + " - "
							+ daysAgoStr, null);
					// TODO: Enforce that these two numbers are the same.
					this.pendingRefs.addElement(referral);
				}
			}
			
			pending.addCommand(EXIT);
			if(this.pendingRefs.size() == 0) {
				if(this.context.isReturnOnEmpty()) {
					Hashtable returnArgs = new Hashtable();
					returnArgs.put(Constants.RETURN_ARG_KEY, "EMPTY");
					shell.returnFromActivity(this, Constants.ACTIVITY_COMPLETE, returnArgs);
					System.out.println("Bailing because there are no matching referrals!");
					return;
				} else {
				pending.append("No Pending Referrals!", null);
				pending.setSelectCommand(EXIT);
				}
			} else {
				pending.addCommand(SELECT);
				pending.setSelectCommand(SELECT);
			}
			pending.setCommandListener(this);
			
			shell.setDisplay(this, pending);
			
		} catch (DeserializationException e) {
			e.printStackTrace();
			//Jan 23, 2009 - csims@dimagi.com
			//This should be treated as an assertion, and not caught or handled. If this error
			//is to be handled, it should be done so here, and by rewriting this.
			throw new RuntimeException("Problem deserializing referrals or patients while trying to list pending referrals. Fix this");
		} catch (IOException e) {
			//Jan 23, 2009 - csims@dimagi.com
			//This should be treated as an assertion, and not caught or handled. If this error
			//is to be handled, it should be done so here, and by rewriting this.
			e.printStackTrace();
			throw new RuntimeException("Problem deserializing referrals or patients while trying to list pending referrals. Fix this");
		}
	}
}
