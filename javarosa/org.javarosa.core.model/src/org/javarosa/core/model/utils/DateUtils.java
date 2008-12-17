package org.javarosa.core.model.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import org.javarosa.core.model.data.IntegerData;

/**
 * Static utility methods for Dates in j2me 
 * 
 * @author Clayton Sims
 *
 */

public class DateUtils {
	private static final int MONTH_OFFSET = (1 - Calendar.JANUARY);
		
	public static final int FORMAT_ISO8601 = 1;
	public static final int FORMAT_HUMAN_READABLE_SHORT = 2;
	//public static final int FORMAT_HUMAN_READABLE_LONG = 3;
	
	public DateUtils() {
		super();
	}

	private static class DateFields {
		public DateFields () {
			year = 1970;
			month = 1;
			day = 1;
			hour = 0;
			minute = 0;
			second = 0;
			secTicks = 0;
			
//			tzStr = "Z";
//			tzOffset = 0;
		}
		
		public int year;
		public int month; //1-12
		public int day; //1-31
		public int hour; //0-23
		public int minute; //0-59
		public int second; //0-59
		public int secTicks; //0-999 (ms)
		
//		public String tzStr;
//		public int tzOffset; //s ahead of UTC
		
		public boolean check () {
			return (inRange(month, 1, 12) && inRange(day, 1, daysInMonth(month - MONTH_OFFSET, year)) &&
					inRange(hour, 0, 23) && inRange(minute, 0, 59) && inRange(second, 0, 59) && inRange(secTicks, 0, 999));
		}
	}
	
	private static DateFields getFields (Date d) {
		Calendar cd = Calendar.getInstance();
		cd.setTime(d);
		
		DateFields fields = new DateFields();
		fields.year = cd.get(Calendar.YEAR);
		fields.month = cd.get(Calendar.MONTH) + MONTH_OFFSET;
		fields.day = cd.get(Calendar.DAY_OF_MONTH);
		fields.hour = cd.get(Calendar.HOUR_OF_DAY);
		fields.minute = cd.get(Calendar.MINUTE);
		fields.second = cd.get(Calendar.SECOND);
		fields.secTicks = cd.get(Calendar.MILLISECOND);
		
		return fields;
	}
	
	private static Date getDate (DateFields f) {
		Calendar cd = Calendar.getInstance();
		cd.set(Calendar.YEAR, f.year);
		cd.set(Calendar.MONTH, f.month - MONTH_OFFSET);
		cd.set(Calendar.DAY_OF_MONTH, f.day);
		cd.set(Calendar.HOUR_OF_DAY, f.hour);
		cd.set(Calendar.MINUTE, f.minute);
		cd.set(Calendar.SECOND, f.second);
		cd.set(Calendar.MILLISECOND, f.secTicks);
		
		return cd.getTime();		
	}
	
	/* ==== FORMATTING DATES/TIMES ==== */
	
	public static String formatDateTime (Date d, int format) {
		if (d == null)
			return "";
		
		DateFields fields = getFields(d);

		String delim;
		switch (format) {
		case FORMAT_ISO8601: delim = "T"; break;
		default: delim = " "; break;
		}
		
		return formatDate(fields, format) + delim + formatTime(fields, format);
	}
	
	public static String formatDate (Date d, int format) {
		return (d == null ? "" :formatDate(getFields(d), format));
	}
	
	public static String formatTime (Date d, int format) {
		return (d == null ? "" : formatTime(getFields(d), format));
	}
	
	private static String formatDate (DateFields f, int format) {
		switch (format) {
		case FORMAT_ISO8601: return formatDateISO8601(f);
		case FORMAT_HUMAN_READABLE_SHORT: return formatDateColloquial(f);
		default: return null;
		}	
	}
	
	private static String formatTime (DateFields f, int format) {
		switch (format) {
		case FORMAT_ISO8601: return formatTimeISO8601(f);
		case FORMAT_HUMAN_READABLE_SHORT: return formatTimeColloquial(f);
		default: return null;
		}	
	}
	
	private static String formatDateISO8601 (DateFields f) {
		return f.year + "-" + intPad(f.month, 2) + "-" + intPad(f.day, 2);
	}

	private static String formatDateColloquial (DateFields f) {
		return intPad(f.day, 2) + "/" + intPad(f.month, 2) + "/" + (new Integer(f.year)).toString().substring(2, 4);
	}

	private static String formatTimeISO8601 (DateFields f) {
		return intPad(f.hour, 2) + ":" + intPad(f.minute, 2) + ":" + intPad(f.second, 2) + "." + intPad(f.secTicks, 3);
		//want to add time zone info to be fully ISO-8601 compliant, but API is totally on crack!
	}
	
	private static String formatTimeColloquial (DateFields f) {
		return intPad(f.hour, 2) + ":" + intPad(f.minute, 2);
	}
		
	/* ==== PARSING DATES/TIMES ==== */
	
	public static Date parseDateTime (String str) {
		DateFields fields = new DateFields();
		int i = str.indexOf("T");
		if (!parseDate(str.substring(0, i), fields) || !parseTime(str.substring(i + 1), fields)) {
			return null;
		}
		return getDate(fields);
	}
	
	public static Date parseDate (String str) {
		DateFields fields = new DateFields();
		if (!parseDate(str, fields)) {
			return null;
		}
		return getDate(fields);
	}
	
	public static Date parseTime (String str) {
		DateFields fields = new DateFields();
		if (!parseTime(str, fields)) {
			return null;
		}
		return getDate(fields);
	}
	
	private static boolean parseDate (String dateStr, DateFields f) {
		Vector pieces = split(dateStr, "-", false);
		if (pieces.size() != 3)
			return false;

		try {
			f.year = Integer.parseInt((String)pieces.elementAt(0));
			f.month = Integer.parseInt((String)pieces.elementAt(1));
			f.day = Integer.parseInt((String)pieces.elementAt(2));
		} catch (NumberFormatException nfe) {
			return false;
		}
		
		return f.check();
	}
	
	private static boolean parseTime (String timeStr, DateFields f) {
		Vector pieces = split(timeStr, ":", false);
		if (pieces.size() != 2 && pieces.size() != 3)
			return false;
		
		try {
			f.hour = Integer.parseInt((String)pieces.elementAt(0));
			f.minute = Integer.parseInt((String)pieces.elementAt(1));
	
			if (pieces.size() == 3) {
				String secStr = (String)pieces.elementAt(2);
				int i;
				for (i = 0; i < secStr.length(); i++) {
					char c = secStr.charAt(i);
					if (!Character.isDigit(c) && c != '.')
						break;
				}
				secStr = secStr.substring(0, i);
				
				double fsec = Double.parseDouble(secStr);
				f.second = (int)fsec;
				f.secTicks = (int)(1000.0 * (fsec - f.second));
			}
		} catch (NumberFormatException nfe) {
			return false;
		}
		
		return f.check();	
	}
	
	/* ==== DATE UTILITY FUNCTIONS ==== */
	
	public static Date getDate (int year, int month, int day) {
		DateFields f = new DateFields();
		f.year = year;
		f.month = month;
		f.day = day;
		return (f.check() ? getDate(f) : null);
	}
	
	/**
	 * 
	 * @return new Date object with same date but time set to midnight (in current timezone)
	 */
	public static Date roundDate (Date d) {
		DateFields f = getFields(d);
		return getDate(f.year, f.month, f.day);
	}
	
	/* ==== CALENDAR FUNCTIONS ==== */
	
	/**
	 * Returns the number of days in the month given for
	 * a given year.
	 * @param month The month to be tested
	 * @param year The year in which the month is to be tested
	 * @return the number of days in the given month on the given
	 * year.
	 */
	public static int daysInMonth (int month, int year) {
		if (month == Calendar.APRIL || month == Calendar.JUNE || month == Calendar.SEPTEMBER || month == Calendar.NOVEMBER) {
			return 30;
		} else if (month == Calendar.FEBRUARY) {
			return 28 + (isLeap(year) ? 1 : 0);
		} else {
			return 31;
		}
	}
	
	/**
	 * Determines whether a year is a leap year in the
	 * proleptic Gregorian calendar.
	 * 
	 * @param year The year to be tested
	 * @return True, if the year given is a leap year, 
	 * false otherwise.
	 */
	public static boolean isLeap (int year) {
		return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
	}
	
	/* ==== DATE OPERATIONS ==== */
	
    /**
     * Creates a Date object representing the amount of time between the
     * reference date, and the given parameters.
     * @param ref The starting reference date 
     * @param type "week", or "month", representing the time period which is to be returned. 
     * @param start "sun", "mon", ... etc. representing the start of the time period.
     * @param beginning true=return first day of period, false=return last day of period
     * @param includeToday Whether to include the current date in the returned calculation
     * @param nAgo How many periods ago. 1=most recent period, 0=period in progress 
     * @return a Date object representing the amount of time between the
     * reference date, and the given parameters.
     */
	public static Date getPastPeriodDate (Date ref, String type, String start, boolean beginning, boolean includeToday, int nAgo) {
		Date d = null;
		
		if (type.equals("week")) {
			//1 week period
			//start: day of week that starts period
			//beginning: true=return first day of period, false=return last day of period
			//includeToday: whether today's date can count as the last day of the period
			//nAgo: how many periods ago; 1=most recent period, 0=period in progress
			
			int target_dow = -1, current_dow = -1, diff;
			int offset = (includeToday ? 1 : 0);
			
			if (start.equals("sun")) target_dow = 0;
			else if (start.equals("mon")) target_dow = 1;
			else if (start.equals("tue")) target_dow = 2;
			else if (start.equals("wed")) target_dow = 3;
			else if (start.equals("thu")) target_dow = 4;
			else if (start.equals("fri")) target_dow = 5;				
			else if (start.equals("sat")) target_dow = 6;

			if (target_dow == -1)
				throw new RuntimeException();

			Calendar cd = Calendar.getInstance();
			cd.setTime(ref);
			
			switch(cd.get(Calendar.DAY_OF_WEEK)) {
			case Calendar.SUNDAY: current_dow = 0; break;
			case Calendar.MONDAY: current_dow = 1; break;
			case Calendar.TUESDAY: current_dow = 2; break;
			case Calendar.WEDNESDAY: current_dow = 3; break;
			case Calendar.THURSDAY: current_dow = 4; break;
			case Calendar.FRIDAY: current_dow = 5; break;
			case Calendar.SATURDAY: current_dow = 6; break;
			default: throw new RuntimeException(); //something is wrong
			}

			diff = (((current_dow - target_dow) + (7 + offset)) % 7 - offset) + (7 * nAgo) - (beginning ? 0 : 6); //booyah
			d = new Date(ref.getTime() - diff * 86400000l);
		} else if (type.equals("month")) {
			//not supported
		} else {
			throw new IllegalArgumentException();
		}
		
		return d;
	}
	
	/**
	 * Gets the number of months separating the two dates.
	 * @param earlierDate The earlier date, chronologically
	 * @param laterDate The later date, chronologically
	 * @return the number of months separating the two dates.
	 */
	public static int getMonthsDifference(Date earlierDate, Date laterDate) {
		Date span = new Date(laterDate.getTime() - earlierDate.getTime());
		Date firstDate = new Date(0);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(firstDate);
		int firstYear = calendar.get(Calendar.YEAR);
		int firstMonth = calendar.get(Calendar.MONTH);				
		
		calendar.setTime(span);
		int spanYear = calendar.get(Calendar.YEAR);
		int spanMonth = calendar.get(Calendar.MONTH);
		int months = (spanYear - firstYear)*12 + (spanMonth - firstMonth);
		return months;
	}
	
	/**
	 * @param earlierDate The earlier of the two dates to Diff
	 * @param laterDate The later of the two dates to Diff
	 * @return The approximate difference, in days, between the two dates given.
	 * The estimate is most likely to be a small underestimate.
	 */
	//who added this shit!?!
	public static int getApproxDaysDifference(Date earlierDate, Date laterDate) {
		Date span = new Date(laterDate.getTime() - earlierDate.getTime());
		Date firstDate = new Date(0);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(firstDate);
		int firstYear = calendar.get(Calendar.YEAR);
		int firstMonth = calendar.get(Calendar.MONTH);
		int firstDay = calendar.get(Calendar.DAY_OF_MONTH);
		
		calendar.setTime(span);
		int spanYear = calendar.get(Calendar.YEAR);
		int spanMonth = calendar.get(Calendar.MONTH);
		int spanDay = calendar.get(Calendar.DAY_OF_MONTH);
		
		int days = (spanYear - firstYear)*365 + (spanMonth - firstMonth)*30 + (spanDay - firstDay);
		
		return days;
	}
	
	/* ==== UTILITY ==== */
	
	/**
	 * Tokenizes a string based on the given delimeter string
	 * @param original The string to be split
	 * @param delimiter The delimeter to be used
	 * @return An array of strings contained in original which were
	 * seperated by the delimeter
	 */
    public static Vector split (String str, String delimiter, boolean combineMultipleDelimiters) {
    	Vector pieces = new Vector();
    	
    	int index = str.indexOf(delimiter);
        while (index >= 0) {
            pieces.addElement(str.substring(0, index));
            str = str.substring(index + delimiter.length());
            index = str.indexOf(delimiter);
        }
        pieces.addElement(str);

        if (combineMultipleDelimiters) {
        	for (int i = 0; i < pieces.size(); i++) {
        		if (((String)pieces.elementAt(i)).length() == 0) {
        			pieces.removeElementAt(i);
        			i--;
        		}
        	}
        }

        return pieces;
    }	
	
	/**
	 * Converts an integer to a string, ensuring that the string
	 * contains a certain number of digits
	 * @param n The integer to be converted
	 * @param pad The length of the string to be returned
	 * @return A string representing n, which has pad - #digits(n)
	 * 0's preceding the number.
	 */
	private static String intPad (int n, int pad) {
		String s = String.valueOf(n);
		while (s.length() < pad)
			s = "0" + s;
		return s;
	}
	
	private static boolean inRange(int x, int min, int max) {
		return (x >= min && x <= max);
	}
	
	/* ==== GARBAGE (backward compatibility; too lazy to remove them now) ==== */
	
	public static String formatDateToTimeStamp(Date date) {
		return formatDateTime(date, FORMAT_ISO8601);
	}

	public static String getShortStringValue(Date val) {
		return formatDate(val, FORMAT_HUMAN_READABLE_SHORT);
	}

	public static String getXMLStringValue(Date val) {
		return formatDate(val, FORMAT_ISO8601);
	}

	public static String get24HourTimeFromDate(Date d) {
		return formatTime(d, FORMAT_HUMAN_READABLE_SHORT);
	}

	public static Date getDateFromString(String value) {
		return parseDate(value);
	}
	
	public static Date getDateTimeFromString(String value) {
		return parseDateTime(value);
	}	
}