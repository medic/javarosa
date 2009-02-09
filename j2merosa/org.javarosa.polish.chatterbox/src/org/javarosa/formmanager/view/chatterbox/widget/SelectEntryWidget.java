package org.javarosa.formmanager.view.chatterbox.widget;

import org.javarosa.core.model.QuestionDef;

import de.enough.polish.ui.ChoiceGroup;
import de.enough.polish.ui.Container;
import de.enough.polish.ui.Item;

public abstract class SelectEntryWidget extends ExpandedWidget {
	private int style;
	protected QuestionDef question;
	
	private ChoiceGroup choicegroup;
	
	public SelectEntryWidget (int style) {
		this.style = style;
	}
	
	protected Item getEntryWidget (QuestionDef question) {
		this.question = question;
		
		//This is a slight UI hack that is in place to make the choicegroup properly
		//intercept 'up' and 'down' inputs. Essentially Polish is very broken when it comes
		//to scrolling nested containers, and this function properly takes the parent (Widget) position
		//into account as well as the choicegroup's
		ChoiceGroup cg = new ChoiceGroup("", style) {
			public int getRelativeScrollYOffset() {
				if (!this.enableScrolling && this.parent instanceof Container) {
					//This line here (The + this.parent.relativeY part) is the fix.
					Item walker = this.parent;
					int offset = 0;
					while(walker instanceof Container) {
						offset += walker.relativeY;
						walker = walker.getParent();
					}
					return ((Container)this.parent).getScrollYOffset() + this.relativeY + offset;
				}
				int offset = this.targetYOffset;
				//#ifdef polish.css.scroll-mode
					if (!this.scrollSmooth) {
						offset = this.yOffset;
					}
				//#endif
				return offset;
			}
		};
		
		for (int i = 0; i < question.getSelectItems().size(); i++)
			cg.append("", null);
		
		this.choicegroup = cg;
		
		return cg;
	}

	protected ChoiceGroup choiceGroup () {
		//return (ChoiceGroup)entryWidget;
		return this.choicegroup;
	}

	protected void updateWidget (QuestionDef question) {
		for (int i = 0; i < choiceGroup().size(); i++) {
			choiceGroup().getItem(i).setText((String)question.getSelectItems().keyAt(i));
		}
	}
}