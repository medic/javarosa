/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.xpath;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.javarosa.core.log.FatalException;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IConditionExpr;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathBinaryOpExpr;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.expr.XPathUnaryOpExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathConditional implements IConditionExpr {
	private XPathExpression expr;
	public String xpath; //not serialized!
	
	public XPathConditional (String xpath) throws XPathSyntaxException {
		this.expr = XPathParseTool.parseXPath(xpath);
		this.xpath = xpath;
	}
	
	public XPathConditional (XPathExpression expr) {
		this.expr = expr;
	}
	
	public XPathConditional () {
		
	}
	
	public XPathExpression getExpr () {
		return expr;
	}
	
	public Object evalRaw (FormInstance model, EvaluationContext evalContext) {
		return XPathFuncExpr.unpack(expr.eval(model, evalContext));
	}
	
	public boolean eval (FormInstance model, EvaluationContext evalContext) {
		return XPathFuncExpr.toBoolean(evalRaw(model, evalContext)).booleanValue();
	}
	
	public String evalReadable (FormInstance model, EvaluationContext evalContext) {
		return XPathFuncExpr.toString(evalRaw(model, evalContext));
	}
	
	public Vector<TreeReference> evalNodeset (FormInstance model, EvaluationContext evalContext) {
		if (expr instanceof XPathPathExpr) {
			return ((XPathPathExpr)expr).eval(model, evalContext).nodes;
		} else {
			throw new FatalException("evalNodeset: must be path expression");
		}
	}
	
	public Vector getTriggers () {
		Vector triggers = new Vector();
		getTriggers(expr, triggers);
		return triggers;
	}
	
	private static void getTriggers (XPathExpression x, Vector v) {
		if (x instanceof XPathPathExpr) {
			TreeReference ref = ((XPathPathExpr)x).getReference();
			if (!v.contains(ref))
				v.addElement(ref);
		} else if (x instanceof XPathBinaryOpExpr) {
			getTriggers(((XPathBinaryOpExpr)x).a, v);
			getTriggers(((XPathBinaryOpExpr)x).b, v);			
		} else if (x instanceof XPathUnaryOpExpr) {
			getTriggers(((XPathUnaryOpExpr)x).a, v);
		} else if (x instanceof XPathFuncExpr) {
			XPathFuncExpr fx = (XPathFuncExpr)x;
			for (int i = 0; i < fx.args.length; i++)
				getTriggers(fx.args[i], v);
		}
	}
	
	public boolean equals (Object o) {
		if (o instanceof XPathConditional) {
			XPathConditional cond = (XPathConditional)o;
			return expr.equals(cond.expr);
		} else {
			return false;
		}
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		expr = (XPathExpression)ExtUtil.read(in, new ExtWrapTagged(), pf);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.write(out, new ExtWrapTagged(expr));
	}
	
	public String toString () {
		return "xpath[" + expr.toString() + "]";
	}
}
