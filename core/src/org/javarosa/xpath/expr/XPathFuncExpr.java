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

package org.javarosa.xpath.expr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;

import me.regexp.RE;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.util.MathUtils;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.IExprDataType;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.XPathUnhandledException;

/**
 * Representation of an xpath function expression.
 * 
 * All of the built-in xpath functions are included here, as well as the xpath type conversion logic
 * 
 * Evaluation of functions can delegate out to custom function handlers that must be registered at
 * runtime.
 * 
 * @author Drew Roos
 *
 */
public class XPathFuncExpr extends XPathExpression {
	public XPathQName id;			//name of the function
	public XPathExpression[] args;	//argument list

	public XPathFuncExpr () { } //for deserialization
	
	public XPathFuncExpr (XPathQName id, XPathExpression[] args) {
		this.id = id;
		this.args = args;
	}
	
	public String toString () {
		StringBuffer sb = new StringBuffer();
		
		sb.append("{func-expr:");	
		sb.append(id.toString());
		sb.append(",{");
		for (int i = 0; i < args.length; i++) {
			sb.append(args[i].toString());
			if (i < args.length - 1)
				sb.append(",");
		}
		sb.append("}}");
		
		return sb.toString();
	}
	
	public boolean equals (Object o) {
		if (o instanceof XPathFuncExpr) {
			XPathFuncExpr x = (XPathFuncExpr)o;
			
			//Shortcuts for very easily comprable values
			if(!id.equals(x.id) || args.length != x.args.length) {
				return false;
			}
			
			return ExtUtil.arrayEquals(args, x.args);
		} else {
			return false;
		}
	}
	
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		id = (XPathQName)ExtUtil.read(in, XPathQName.class);
		Vector v = (Vector)ExtUtil.read(in, new ExtWrapListPoly(), pf);
		
		args = new XPathExpression[v.size()];
		for (int i = 0; i < args.length; i++)
			args[i] = (XPathExpression)v.elementAt(i);		
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		Vector v = new Vector();
		for (int i = 0; i < args.length; i++)
			v.addElement(args[i]);

		ExtUtil.write(out, id);
		ExtUtil.write(out, new ExtWrapListPoly(v));
	}

	/**
	 * Evaluate the function call.
	 * 
	 * First check if the function is a member of the built-in function suite. If not, then check
	 * for any custom handlers registered to handler the function. If not, throw and exception.
	 * 
	 * Both function name and appropriate arguments are taken into account when finding a suitable
	 * handler. For built-in functions, the number of arguments must match; for custom functions,
	 * the supplied arguments must match one of the function prototypes defined by the handler.
	 * 
	 */
	public Object eval (FormInstance model, EvaluationContext evalContext) {
		String name = id.toString();
		Object[] argVals = new Object[args.length];
		
		Hashtable funcHandlers = evalContext.getFunctionHandlers();
		
		for (int i = 0; i < args.length; i++) {
			argVals[i] = args[i].eval(model, evalContext);
		}
		
		//check built-in functions
		if (name.equals("true") && args.length == 0) {
			return Boolean.TRUE;
		} else if (name.equals("false") && args.length == 0) {
			return Boolean.FALSE;
		} else if (name.equals("boolean") && args.length == 1) {
			return toBoolean(argVals[0]);
		} else if (name.equals("number") && args.length == 1) {
			return toNumeric(argVals[0]);
		} else if (name.equals("int") && args.length == 1) { //non-standard
			return toInt(argVals[0]);
		} else if (name.equals("string") && args.length == 1) {
			return toString(argVals[0]);			
		} else if (name.equals("date") && args.length == 1) { //non-standard
			return toDate(argVals[0]);				
		} else if (name.equals("not") && args.length == 1) {
			return boolNot(argVals[0]);
		} else if (name.equals("boolean-from-string") && args.length == 1) {
			return boolStr(argVals[0]);
		} else if (name.equals("if") && args.length == 3) { //non-standard
			return ifThenElse(argVals[0], argVals[1], argVals[2]);	
		} else if ((name.equals("selected") || name.equals("is-selected")) && args.length == 2) { //non-standard
			return multiSelected(argVals[0], argVals[1]);
		} else if (name.equals("count-selected") && args.length == 1) { //non-standard
			return countSelected(argVals[0]);		
		} else if (name.equals("count") && args.length == 1) {
			return count(argVals[0]);
		} else if (name.equals("sum") && args.length == 1) {
			if (argVals[0] instanceof XPathNodeset) {
				return sum(((XPathNodeset)argVals[0]).toArgList());
			} else {
				throw new XPathTypeMismatchException("not a nodeset");				
			}
		} else if (name.equals("today") && args.length == 0) {
			return DateUtils.roundDate(new Date());
		} else if (name.equals("now") && args.length == 0) {
			return new Date();
		} else if (name.equals("concat")) {
			if (args.length == 1 && argVals[0] instanceof XPathNodeset) {
				return join("", ((XPathNodeset)argVals[0]).toArgList());
			} else {
				return join("", argVals);
			}
		} else if (name.equals("join") && args.length >= 1) {
			if (args.length == 2 && argVals[1] instanceof XPathNodeset) {
				return join(argVals[0], ((XPathNodeset)argVals[1]).toArgList());
			} else {
				return join(argVals[0], subsetArgList(argVals, 1));
			}
		} else if (name.equals("checklist") && args.length >= 2) { //non-standard
			if (args.length == 3 && argVals[2] instanceof XPathNodeset) {
				return checklist(argVals[0], argVals[1], ((XPathNodeset)argVals[2]).toArgList());
			} else {
				return checklist(argVals[0], argVals[1], subsetArgList(argVals, 2));
			}
		} else if (name.equals("weighted-checklist") && args.length >= 2 && args.length % 2 == 0) { //non-standard
			if (args.length == 4 && argVals[2] instanceof XPathNodeset && argVals[3] instanceof XPathNodeset) {
				Object[] factors = ((XPathNodeset)argVals[2]).toArgList();
				Object[] weights = ((XPathNodeset)argVals[3]).toArgList();
				if (factors.length != weights.length) {
					throw new XPathTypeMismatchException("weighted-checklist: nodesets not same length");
				}
				return checklistWeighted(argVals[0], argVals[1], factors, weights);
			} else {
				return checklistWeighted(argVals[0], argVals[1], subsetArgList(argVals, 2, 2), subsetArgList(argVals, 3, 2));
			}
		} else if (name.equals("regex") && args.length == 2) { //non-standard
			return regex(argVals[0], argVals[1]);
			
		} else {
			//check for custom handler
			IFunctionHandler handler = (IFunctionHandler)funcHandlers.get(name);
			if (handler != null) {
				return evalCustomFunction(handler, argVals);
			} else {
				throw new XPathUnhandledException("function \'" + name + "\'");
			}
		}
	}
	
	/**
	 * Given a handler registered to handle the function, try to coerce the function arguments into
	 * one of the prototypes defined by the handler. If no suitable prototype found, throw an eval
	 * exception. Otherwise, evaluate.
	 * 
	 * Note that if the handler supports 'raw args', it will receive the full, unaltered argument
	 * list if no prototype matches. (this lets functions support variable-length argument lists)
	 * 
	 * @param handler
	 * @param args
	 * @return
	 */
	private static Object evalCustomFunction (IFunctionHandler handler, Object[] args) {
		Vector prototypes = handler.getPrototypes();
		Enumeration e = prototypes.elements();
		Object[] typedArgs = null;

		while (typedArgs == null && e.hasMoreElements()) {
			typedArgs = matchPrototype(args, (Class[])e.nextElement());
		}

		if (typedArgs != null) {
			return handler.eval(typedArgs);
		} else if (handler.rawArgs()) {
			return handler.eval(args);  //should we have support for expanding nodesets here?
		} else {
			throw new XPathTypeMismatchException("for function \'" + handler.getName() + "\'");
		}
	}
	
	/**
	 * Given a prototype defined by the function handler, attempt to coerce the function arguments
	 * to match that prototype (checking # args, type conversion, etc.). If it is coercible, return
	 * the type-converted argument list -- these will be the arguments used to evaluate the function.
	 * If not coercible, return null.
	 * 
	 * @param args
	 * @param prototype
	 * @return
	 */
	private static Object[] matchPrototype (Object[] args, Class[] prototype) {
		Object[] typed = null;

		if (prototype.length == args.length) {
			typed = new Object[args.length];

			for (int i = 0; i < prototype.length; i++) {
				typed[i] = null;

				//how to handle type conversions of custom types?
				if (prototype[i].isAssignableFrom(args[i].getClass())) {
					typed[i] = args[i];
				} else {
					try {
						if (prototype[i] == Boolean.class) {
							typed[i] = toBoolean(args[i]);
						} else if (prototype[i] == Double.class) {
							typed[i] = toNumeric(args[i]);
						} else if (prototype[i] == String.class) {
							typed[i] = toString(args[i]);
						} else if (prototype[i] == Date.class) {
							typed[i] = toDate(args[i]);
						}
					} catch (XPathTypeMismatchException xptme) { /* swallow type mismatch exception */ }
				}

				if (typed[i] == null)
					return null;
			}
		}

		return typed;
	}
	
	/******** HANDLERS FOR BUILT-IN FUNCTIONS ********
	 * 
	 * the functions below are the handlers for the built-in xpath function suite
	 * 
	 * if you add a function to the suite, it should adhere to the following pattern:
	 * 
	 *   * the function takes in its arguments as objects (DO NOT cast the arguments when calling
	 *     the handler up in eval() (i.e., return stringLength((String)argVals[0])  <--- NO!)
	 *     
	 *   * the function converts the generic argument(s) to the desired type using the built-in
	 *     xpath type conversion functions (toBoolean(), toNumeric(), toString(), toDate())
	 *     
	 *   * the function MUST return an object of type Boolean, Double, String, or Date; it may
	 *     never return null (instead return the empty string or NaN)
	 *   
	 *   * the function may throw exceptions, but should try as hard as possible not to, and if
	 *     it must, strive to make it an XPathException
	 * 
	 */
	
	/**
	 * convert a value to a boolean using xpath's type conversion rules
     *
	 * @param o
	 * @return
	 */
	public static Boolean toBoolean (Object o) {
		Boolean val = null;
		
		o = unpack(o);
		
		if (o instanceof Boolean) {
			val = (Boolean)o;
		} else if (o instanceof Double) {
			double d = ((Double)o).doubleValue();
			val = new Boolean(Math.abs(d) > 1.0e-12 && !Double.isNaN(d));
		} else if (o instanceof String) {
			String s = (String)o;
			val = new Boolean(s.length() > 0);
		} else if (o instanceof Date) {
			val = Boolean.TRUE;
		} else if (o instanceof IExprDataType) {
			val = ((IExprDataType)o).toBoolean();
		}
		
		if (val != null) {
			return val;
		} else {
			throw new XPathTypeMismatchException("converting to boolean");
		}
	}
	
	/**
	 * convert a value to a number using xpath's type conversion rules (note that xpath itself makes
	 * no distinction between integer and floating point numbers)
	 * 
	 * @param o
	 * @return
	 */
	public static Double toNumeric (Object o) {
		Double val = null;
		
		o = unpack(o);
		
		if (o instanceof Boolean) {
			val = new Double(((Boolean)o).booleanValue() ? 1 : 0);
		} else if (o instanceof Double) {
			val = (Double)o;
		} else if (o instanceof String) {
			/* annoying, but the xpath spec doesn't recognize scientific notation, or +/-Infinity
			 * when converting a string to a number
			 */
			
			String s = (String)o;
			double d;
			try {
				s = s.trim();
				for (int i = 0; i < s.length(); i++) {
					char c = s.charAt(i);
					if (c != '-' && c != '.' && (c < '0' || c > '9'))
						throw new NumberFormatException();
				}
				
				d = Double.parseDouble(s);
				val = new Double(d);
			} catch (NumberFormatException nfe) {
				val = new Double(Double.NaN);
			}
		} else if (o instanceof Date) {
			val = new Double(DateUtils.daysSinceEpoch((Date)o));
		} else if (o instanceof IExprDataType) {
			val = ((IExprDataType)o).toNumeric();
		}
		
		if (val != null) {
			return val;
		} else {
			throw new XPathTypeMismatchException("converting to numeric");
		}
	}

	/**
	 * convert a number to an integer by truncating the fractional part. if non-numeric, coerce the
	 * value to a number first. note that the resulting return value is still a Double, as required
	 * by the xpath engine
	 * 
	 * @param o
	 * @return
	 */
	public static Double toInt (Object o) {
		Double val = toNumeric(o);
		
		if (val.isInfinite() || val.isNaN()) {
			return val;
		} else if (val.doubleValue() >= Long.MAX_VALUE || val.doubleValue() <= Long.MIN_VALUE) {
			return val;
		} else {
			long l = val.longValue();
			Double dbl = new Double(l);
			if (l == 0 && (val.doubleValue() < 0. || val.equals(new Double(-0.)))) {
				dbl = new Double(-0.);
			}
			return dbl;
		}
	}
	
	/**
	 * convert a value to a string using xpath's type conversion rules
	 * 
	 * @param o
	 * @return
	 */
	public static String toString (Object o) {
		String val = null;
		
		o = unpack(o);
		
		if (o instanceof Boolean) {
			val = (((Boolean)o).booleanValue() ? "true" : "false");
		} else if (o instanceof Double) {
			double d = ((Double)o).doubleValue();
			if (Double.isNaN(d)) {
				val = "NaN";
			} else if (Math.abs(d) < 1.0e-12) {
				val = "0";
			} else if (Double.isInfinite(d)) {
				val = (d < 0 ? "-" : "") + "Infinity";
			} else if (Math.abs(d - (int)d) < 1.0e-12) {
				val = String.valueOf((int)d);
			} else {
				val = String.valueOf(d);
			}
		} else if (o instanceof String) {
			val = (String)o;
		} else if (o instanceof Date) {
			val = DateUtils.formatDate((Date)o, DateUtils.FORMAT_ISO8601);
		} else if (o instanceof IExprDataType) {
			val = ((IExprDataType)o).toString();
		}
			
		if (val != null) {
			return val;
		} else {
			throw new XPathTypeMismatchException("converting to string");
		}
	}

	/**
	 * convert a value to a date. note that xpath has no intrinsic representation of dates, so this
	 * is off-spec. dates convert to strings as 'yyyy-mm-dd', convert to numbers as # of days since
	 * the unix epoch, and convert to booleans always as 'true'
	 * 
	 * string and int conversions are reversable, however:
	 *   * cannot convert bool to date
	 *   * empty string and NaN (xpath's 'null values') go unchanged, instead of being converted
	 *     into a date (which would cause an error, since Date has no null value (other than java
	 *     null, which the xpath engine can't handle))
	 *   * note, however, than non-empty strings that aren't valid dates _will_ cause an error
	 *     during conversion
	 * 
	 * @param o
	 * @return
	 */
	public static Object toDate (Object o) {
		o = unpack(o);
		
		if (o instanceof Double) {
			Double n = toInt(o);
			
			if (n.isNaN()) {
				return n;
			}
			
			if (n.isInfinite() || n.doubleValue() > Integer.MAX_VALUE || n.doubleValue() < Integer.MIN_VALUE) {
				throw new XPathTypeMismatchException("converting out-of-range value to date");				
			}

			return DateUtils.dateAdd(DateUtils.getDate(1970, 1, 1), n.intValue());
		} else if (o instanceof String) {
			String s = (String)o;
			
			if (s.length() == 0) {
				return s;
			}
			
			Date d = DateUtils.parseDate(s);
			if (d == null) {
				throw new XPathTypeMismatchException("converting to date");
			} else {
				return d;
			}
		} else if (o instanceof Date) {
			return DateUtils.roundDate((Date)o);
		} else {
			throw new XPathTypeMismatchException("converting to date");
		}
	}

	public static Boolean boolNot (Object o) {
		boolean b = toBoolean(o).booleanValue();
		return new Boolean(!b);
	}
	
	public static Boolean boolStr (Object o) {
		String s = toString(o);
		if (s.equalsIgnoreCase("true") || s.equals("1"))
			return Boolean.TRUE;
		else
			return Boolean.FALSE;
	}

	public static Object ifThenElse (Object o1, Object o2, Object o3) {
		boolean b = toBoolean(o1).booleanValue();
		return (b ? o2 : o3);
	}
	
	/**
	 * return whether a particular choice of a multi-select is selected
	 * 
	 * @param o1 XML-serialized answer to multi-select question (i.e, space-delimited choice values)
	 * @param o2 choice to look for
	 * @return
	 */
	public static Boolean multiSelected (Object o1, Object o2) {
		String s1 = (String)unpack(o1);
		String s2 = ((String)unpack(o2)).trim();
		
		return new Boolean((" " + s1 + " ").indexOf(" " + s2 + " ") != -1);
	}

	/**
	 * return the number of choices in a multi-select answer
	 * 
	 * @param o XML-serialized answer to multi-select question (i.e, space-delimited choice values)
	 * @return
	 */
	public static Double countSelected (Object o) {
		String s = (String)unpack(o);

		return new Double(DateUtils.split(s, " ", true).size());
	}
	
	/**
	 * count the number of nodes in a nodeset
	 * 
	 * @param o
	 * @return
	 */
	public static Double count (Object o) {
		if (o instanceof XPathNodeset) {
			return new Double(((XPathNodeset)o).size());
		} else {
			throw new XPathTypeMismatchException("not a nodeset");
		}	
	}

	/**
	 * sum the values in a nodeset; each element is coerced to a numeric value
	 * 
	 * @param model
	 * @param o
	 * @return
	 */
	public static Double sum (Object argVals[]) {
		double sum = 0.0;
		for (int i = 0; i < argVals.length; i++) {
			sum += toNumeric(argVals[i]).doubleValue();
		}
		return new Double(sum);
	}

	/**
	 * concatenate an abritrary-length argument list of string values together
	 * 
	 * @param argVals
	 * @return
	 */
	public static String join (Object oSep, Object[] argVals) {
		String sep = toString(oSep);
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < argVals.length; i++) {
			sb.append(toString(argVals[i]));
			if (i < argVals.length - 1)
				sb.append(sep);
		}
		
		return sb.toString();
	}
	
	/**
	 * perform a 'checklist' computation, enabling expressions like 'if there are at least 3 risk
	 * factors active'
	 * 
	 * @param argVals
	 *   the first argument is a numeric value expressing the minimum number of factors required.
	 *     if -1, no minimum is applicable
	 *   the second argument is a numeric value expressing the maximum number of allowed factors.
	 *     if -1, no maximum is applicalbe
	 *   arguments 3 through the end are the individual factors, each coerced to a boolean value
	 * @return true if the count of 'true' factors is between the applicable minimum and maximum,
	 *   inclusive
	 */
	public static Boolean checklist (Object oMin, Object oMax, Object[] factors) {
		int min = toNumeric(oMin).intValue();
		int max = toNumeric(oMax).intValue();
		
		int count = 0;
		for (int i = 0; i < factors.length; i++) {
			if (toBoolean(factors[i]).booleanValue())
				count++;
		}
		
		return new Boolean((min < 0 || count >= min) && (max < 0 || count <= max));
	}

	/**
	 * very similar to checklist, only each factor is assigned a real-number 'weight'.
	 * 
	 * the first and second args are again the minimum and maximum, but -1 no longer means
	 * 'not applicable'.
	 * 
	 * subsequent arguments come in pairs: first the boolean value, then the floating-point
	 * weight for that value
	 * 
	 * the weights of all the 'true' factors are summed, and the function returns whether
	 * this sum is between the min and max
	 * 
	 * @param argVals
	 * @return
	 */
	public static Boolean checklistWeighted (Object oMin, Object oMax, Object[] flags, Object[] weights) {
		double min = toNumeric(oMin).doubleValue();
		double max = toNumeric(oMax).doubleValue();
		
		double sum = 0.;
		for (int i = 0; i < flags.length; i++) {
			boolean flag = toBoolean(flags[i]).booleanValue();
			double weight = toNumeric(weights[i]).doubleValue();
			
			if (flag)
				sum += weight;
		}
		
		return new Boolean(sum >= min && sum <= max);
	}
	
	/**
	 * determine if a string matches a regular expression. 
	 * 
	 * @param o1 string being matched
	 * @param o2 regular expression
	 * @return
	 */
	public static Boolean regex (Object o1, Object o2) {
		String str = toString(o1);
		String re = toString(o2);
		
		RE regexp = new RE(re);
		boolean result = regexp.match(str);
		return new Boolean(result);
	}

	private static Object[] subsetArgList (Object[] args, int start) {
		return subsetArgList(args, start, 1);
	}
	
	/**
	 * return a subset of an argument list as a new arguments list
	 * 
	 * @param args
	 * @param start index to start at
	 * @param skip sub-list will contain every nth argument, where n == skip (default: 1)
	 * @return
	 */
	private static Object[] subsetArgList (Object[] args, int start, int skip) {
		if (start > args.length || skip < 1) {
			throw new RuntimeException("error in subsetting arglist");
		}
		
		Object[] subargs = new Object[(int)MathUtils.divLongNotSuck(args.length - start - 1, skip) + 1];
		for (int i = start, j = 0; i < args.length; i += skip, j++) {
			subargs[j] = args[i];
		}
		
		return subargs;
	}
	
	public static Object unpack (Object o) {
		if (o instanceof XPathNodeset) {
			return ((XPathNodeset)o).unpack();
		} else {
			return o;
		}
	}
}
