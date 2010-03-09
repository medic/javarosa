package org.javarosa.core.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.javarosa.core.model.condition.IConditionExpr;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.locale.Localizable;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

public class ItemsetBinding implements Externalizable, Localizable {
	
	public TreeReference nodesetRef;   //absolute ref of itemset source nodes
	public IConditionExpr nodesetExpr; //path expression for source nodes; may be relative, may contain predicates
	
	public IConditionExpr labelExpr;   //path expression for label; absolute, no predicates  
	public boolean labelIsItext;       //if true, content of 'label' is an itext id
	
	public boolean copyMode;         //true = copy subtree; false = copy string value
	public TreeReference copyRef;    //absolute ref to copy
	public IConditionExpr valueExpr; //path expression for value; absolute, no predicates

	private TreeReference destRef; //ref that identifies the repeated nodes resulting from this itemset
								   //not serialized -- set by QuestionDef.setDynamicChoices()
	private Vector<SelectChoice> choices; //dynamic choices -- not serialized, obviously
	
	public Vector<SelectChoice> getChoices () {
		return choices;
	}
	
	public void setChoices (Vector<SelectChoice> choices, Localizer localizer) {
		if (this.choices != null) {
			System.out.println("warning: previous choices not cleared out");
			clearChoices();
		}
		this.choices = choices;
		
		//init localization
		if (localizer != null) {
			String curLocale = localizer.getLocale();
			if (curLocale != null) {
				localeChanged(curLocale, localizer);
			}
		}
	}
	
	public void clearChoices () {
		this.choices = null;
	}
	
	public void localeChanged(String locale, Localizer localizer) {
		if (choices != null) {
			for (int i = 0; i < choices.size(); i++) {
				choices.elementAt(i).localeChanged(locale, localizer);
			}
		}
	}
	
	public void setDestRef (QuestionDef q) {
		if (copyMode) {
			TreeReference destRef = FormInstance.unpackReference(q.getBind()).clone();
			destRef.add(copyRef.getNameLast(), TreeReference.INDEX_UNBOUND);
		} else {
			destRef = null;
		}
	}
	
	public TreeReference getDestRef () {
		return destRef;
	}
	
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		nodesetRef = (TreeReference)ExtUtil.read(in, TreeReference.class, pf);
		nodesetExpr = (IConditionExpr)ExtUtil.read(in, new ExtWrapTagged(), pf);
		labelExpr = (IConditionExpr)ExtUtil.read(in, new ExtWrapTagged(), pf);
		valueExpr = (IConditionExpr)ExtUtil.read(in, new ExtWrapNullable(new ExtWrapTagged()), pf);
		copyRef = (TreeReference)ExtUtil.read(in, new ExtWrapNullable(TreeReference.class), pf);
		labelIsItext = ExtUtil.readBool(in);
		copyMode = ExtUtil.readBool(in);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.write(out, nodesetRef);
		ExtUtil.write(out, new ExtWrapTagged(nodesetExpr));
		ExtUtil.write(out, new ExtWrapTagged(labelExpr));
		ExtUtil.write(out, new ExtWrapNullable(valueExpr == null ? null : new ExtWrapTagged(valueExpr)));
		ExtUtil.write(out, new ExtWrapNullable(copyRef));
		ExtUtil.writeBool(out, labelIsItext);
		ExtUtil.writeBool(out, copyMode);
	}

}
