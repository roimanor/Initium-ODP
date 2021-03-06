package com.universeprojects.miniup.server;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.cheffo.jeplite.JEP;

import com.google.appengine.api.datastore.Key;
import com.universeprojects.cacheddatastore.CachedDatastoreService;
import com.universeprojects.cacheddatastore.CachedEntity;
import com.universeprojects.cacheddatastore.ShardedCounterService;
import com.universeprojects.miniup.server.ODPDBAccess.CharacterMode;
import com.universeprojects.miniup.server.commands.framework.UserErrorMessage;


public class GameUtils 
{
	final static Logger log = Logger.getLogger(GameUtils.class.getName());

	final static DecimalFormat doubleDigitFormat = new DecimalFormat("#,##0.00");
	final static DecimalFormat noDigitFormat = new DecimalFormat("#,###");
	final static DateFormat longDateFormat = new SimpleDateFormat("MMM, dd, yyyy HH:mm:ss");
	final static Random rnd = new Random();

	public GameUtils() 
	{
		
	}

	public static String formatNumber(Object value)
	{
		return formatNumber(value, true);
	}
	
	public static String formatNumber(Object value, boolean decimals)
	{
		if (value==null)
			return "";
		
		if (decimals==true)
		{
			String result = doubleDigitFormat.format(value);
			if (result.endsWith(".00"))
				return result.substring(0, result.length()-3);
			else
				return result;
		}
		else
			return noDigitFormat.format(value);
	}
	
	public static String formatPercent(Double value)
	{
		if (value==null)
			return "";
		value*=100;
		
		return doubleDigitFormat.format(value)+"%";
	}
	
	public static boolean between(int value, int start, int end)
	{
		if (value>=start && value<end)
			return true;
		else
			return false;
	}
	
	
	public static boolean roll(int difficulty)
	{
		return roll(((Integer) difficulty).doubleValue());
	}
	
	public static boolean roll(long difficulty)
	{
		return roll(((Long) difficulty).doubleValue());
	}

	public static boolean roll(double chance)
	{
		if (chance<=0) return false;
		if (chance>=100) return true;
		double roll = rnd.nextDouble()*100;
		if (roll<chance)
			return true;
		
		return false;
	}
	
	public static List<CachedEntity> roll(List<CachedEntity> entities, String chanceFieldName, String typeFieldName, String onlyUseType)
	{
		List<CachedEntity> result = new ArrayList<CachedEntity>();
		for(CachedEntity entity:entities)
		{
			// If we care about the types of entities we're picking AND this is the wrong type of entity, skip it
			if (typeFieldName!=null && onlyUseType!=null)
				if (entity.getProperty(typeFieldName).equals(onlyUseType)==false)
					continue;
			
			if (entity.getProperty(chanceFieldName) instanceof Long)
			{
				if (GameUtils.roll((Long)entity.getProperty(chanceFieldName)))
				{
					result.add(entity);
				}
			}
			else if ((entity.getProperty(chanceFieldName) instanceof Double))
			{
				if (GameUtils.roll((Double)entity.getProperty(chanceFieldName)))
				{
					result.add(entity);
				}
			}
		}
		return result;
	}

	/**
	 * Use this method to cause the game to wait for the specified
	 * amount of time before continuing. This is generally used to slow down
	 * user inputs and make the game have a bit of slower feel.
	 * 
	 * Best practice: Call this method BEFORE doing any action (like accessing the database).
	 * 
	 * @param seconds
	 */
	public static void timePasses(int seconds) 
	{
		if (seconds<=0)
			return;
		Object waiter = new Object();
		
		try 
		{
			synchronized(waiter)
			{
				waiter.wait(seconds*1000);
			}
		} 
		catch (InterruptedException e) 
		{
		}
		
	}
	
	public static void addMessageForClient(HttpServletRequest request, String message)
	{
		String oldMessage = (String)request.getAttribute("midMessage");
		if (oldMessage!=null)
			message = oldMessage + "<br>"+message;
			
		
		request.setAttribute("midMessage", message);
		
	}
	
	/**
	 * Checks the given comma separated list of items to see if the given value is in the list.
	 * @param commaSeparatedList
	 * @param value
	 * @return
	 */
	public static boolean isContainedInList(String commaSeparatedList, String value)
	{
		if (commaSeparatedList==null) return false;
		if (value==null) return false;
		
		String[] list = commaSeparatedList.split(",");
		for(String entry:list)
			if (value.equals(entry))
				return true;
		
		return false;
	}
	
    /**
	 * A value of 1 indicates night, a value of 0 indicates day.
	 * @return
	 */
	public static double getDayNight()
	{
		double serverTime = System.currentTimeMillis(); 

		//318.47133757961783439490445859873 = 1 second per day
		
		serverTime/=(318.47133757961783439490445859873d*60d*60d*1.5d);
		double amount = Math.sin(serverTime);
		if (amount<0) amount*=-1;
		amount*=3;
		amount-=1.56;
		
		if (amount>1) amount = 1d;
		if (amount<0) amount = 0d;
		
		return amount;
	}
	

	public static int xorShift32(int seed) {
	    seed ^= (seed << 11);
	    seed ^= (seed >>> 25);
	    seed ^= (seed << 8);
	    int out = (int) seed % 127521;     
	    return (out < 0) ? -out : out;
	}	
	
	private static double rnd(int seed, double min, double max)
	{
		Integer rand = xorShift32(seed);
		double dbl = (rand.doubleValue()/127521d);
	 
	    return (dbl*(max-min))+min;
	}	
	
	public static void main(String[] args)
	{
		for(int i = 1000; i<1100; i++)
			System.out.println(getWeather());
	}
	
	public static double getWeather()
	{
		
		Date date = new Date();
		
		long behindMs = new Date(date.getYear(), date.getMonth(), date.getDate(), date.getHours(), 0).getTime();
		long aheadMs = new Date(date.getYear(), date.getMonth(), date.getDate(), date.getHours()+1, 0).getTime();
	
		
		double behindHourWeather = rnd((int)(behindMs/3600000), 0d, 1d);
		double aheadHourWeather = rnd((int)(aheadMs/3600000), 0d, 1d);
		
		// Now interpolate...
		double weatherDifference = aheadHourWeather-behindHourWeather;
		
		double hourProgression = (((double)date.getTime())-behindMs)/3600000d;
		
		double interpolationDelta = weatherDifference*hourProgression;
		
		return behindHourWeather+interpolationDelta;

//		// Weather calculator
//		function getWeather()
//		{
//			var serverTime = getCurrentServerTime();
//			
//			var date = new Date(serverTime);
//			
//			var behindHour = new Date(date.getFullYear(), date.getMonth(), date.getDate(), date.getHours());
//			var aheadHour = new Date(date.getFullYear(), date.getMonth(), date.getDate(), date.getHours()+1);
//			var behindMs = behindHour.getTime();
//			var aheadMs = aheadHour.getTime();
//		
//			
//			var behindHourWeather = rnd(behindMs, 0, 1);
//			var aheadHourWeather = rnd(aheadMs, 0, 1);
//			
//			// Now interpolate...
//			var weatherDifference = aheadHourWeather-behindHourWeather;
//			
//			var hourProgression = (serverTime-behindHour)/3600000;
//			
//			var interpolationDelta = weatherDifference*hourProgression;
//			
//			return behindHourWeather+interpolationDelta;
//		}	
		
	}

	public static Long getTimePassed(GregorianCalendar startDate, int calendarField)
    {
        if (startDate == null)
        {
            return null;
        }

        return elapsed(new GregorianCalendar(), startDate, calendarField);
    }

    public static String getTimePassedString(GregorianCalendar startDate)
    {
        if (startDate == null)
        {
            return "unknown";
        }

        return Convert.SecondsToStandardString2(elapsed(new GregorianCalendar(), startDate, Calendar.SECOND));
    }

    public static String getTimePassedShortString(Date startDate)
    {
        return getTimePassedShortString(Convert.DateToCalendar(startDate));
    }
    
    public static String getTimePassedShortString(GregorianCalendar startDate)
    {
        if (startDate == null)
        {
            return "unknown";
        }

        return Convert.SecondsToStandardShortString(elapsed(new GregorianCalendar(), startDate, Calendar.SECOND));
    }

    /**
     * Elapsed days based on current time
     *
     * @param date Date
     *
     * @return int number of days
     */
    public static long getElapsedDays(Date date)
    {
        return elapsed(date, Calendar.DATE);
    }

    /**
     * Elapsed days based on two Date objects
     *
     * @param d1 Date
     * @param d2 Date
     *
     * @return int number of days
     */
    public static long getElapsedDays(Date d1, Date d2)
    {
        return elapsed(d1, d2, Calendar.DATE);
    }

    /**
     * Elapsed months based on current time
     *
     * @param date Date
     *
     * @return int number of months
     */
    public static long getElapsedMonths(Date date)
    {
        return elapsed(date, Calendar.MONTH);
    }

    /**
     * Elapsed months based on two Date objects
     *
     * @param d1 Date
     * @param d2 Date
     *
     * @return int number of months
     */
    public static long getElapsedMonths(Date d1, Date d2)
    {
        return elapsed(d1, d2, Calendar.MONTH);
    }

    /**
     * Elapsed years based on current time
     *
     * @param date Date
     *
     * @return int number of years
     */
    public static long getElapsedYears(Date date)
    {
        return elapsed(date, Calendar.YEAR);
    }

    /**
     * Elapsed years based on two Date objects
     *
     * @param d1 Date
     * @param d2 Date
     *
     * @return int number of years
     */
    public static long getElapsedYears(Date d1, Date d2)
    {
        return elapsed(d1, d2, Calendar.YEAR);
    }

    /**
     * All elaspsed types
     *
     * @param g1 GregorianCalendar
     * @param g2 GregorianCalendar
     * @param type int (Calendar.FIELD_NAME)
     *
     * @return int number of elapsed "type"
     */
    public static long elapsed(Calendar g1, Calendar g2, int type)
    {
        long milis1 = g1.getTimeInMillis();
        long milis2 = g2.getTimeInMillis();

        long diff = milis2 - milis1;
        if (diff < 0)
        {
            diff *= -1;
        }

        switch (type)
        {
			case(Calendar.SECOND):
			    return diff / 1000;
			case (Calendar.MINUTE):
			    return diff / 60000;
			case (Calendar.HOUR):
			    return diff / 3600000;
			case (Calendar.DATE):
			    return diff / 86400000;
			case (Calendar.YEAR):
			    return diff / 31536000000l;
			default:
				throw new RuntimeException("Utils.elapsed() was given a Calendar type that it does not support. (Type = " + type + ")");
        }
    }

    /**
     * All elaspsed types based on date and current Date
     *
     * @param date Date
     * @param type int (Calendar.FIELD_NAME)
     *
     * @return int number of elapsed "type"
     */
    public static long elapsed(Date date, int type)
    {
        return elapsed(date, new Date(), type);
    }

    /**
     * All elaspsed types
     *
     * @param d1 Date
     * @param d2 Date
     * @param type int (Calendar.FIELD_NAME)
     *
     * @return int number of elapsed "type"
     */
    private static long elapsed(Date d1, Date d2, int type)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d1);
        GregorianCalendar g1 = new GregorianCalendar(
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
        cal.setTime(d2);
        GregorianCalendar g2 = new GregorianCalendar(
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
        return elapsed(g1, g2, type);
    }
    

    //TODO: Redo this whole thing. Needs to be simpler now that we don't need custom junk and it should be more precise.
    public static long determineQualityScore(Map<String, Object> entityProperties)
    {
    	entityProperties = new HashMap<String, Object>(entityProperties);
    	
    	long result = 0;
    	List<Double> qualityNumbers = new ArrayList<Double>();
    	String qualityUnit = (String)entityProperties.get("qualityUnit");
    	
    	// Override for the quality unit. I'm testing a global quality determination now...
    	if ("Weapon".equals(entityProperties.get("itemType")))
    	{
    		qualityUnit = "_weaponMaxDamage[0..50]&&blockChance[5..40]";
    		
    		String damageFormula = (String)entityProperties.get("weaponDamage");
    		Double critChance = 0d;
    		if (entityProperties.get("weaponDamageCriticalChance")!=null)
    			critChance = ((Long)entityProperties.get("weaponDamageCriticalChance")).doubleValue();
    		Double critMultiplier = 1d;
    		if (entityProperties.get("weaponDamageCriticalMultiplier")!=null)
    			critMultiplier = (Double)entityProperties.get("weaponDamageCriticalMultiplier");
    		Double weaponMaxDamage = getWeaponMaxDamage(damageFormula, critMultiplier, critChance); 
    		Double weaponAverageDamage = getWeaponAverageDamage(damageFormula, critMultiplier, critChance);
    		
    		entityProperties.put("_weaponMaxDamage", weaponMaxDamage.intValue());
    		entityProperties.put("_weaponAverageDamage", weaponAverageDamage.intValue());
    	}
    	else if ("Armor".equals(entityProperties.get("itemType")))
    	{
    		qualityUnit = "blockChance[50..95]&&dexterityPenalty[10..0]&&damageReduction[5..25]";
    	} 
    	else if ("Shield".equals(entityProperties.get("itemType")))
    	{
    		qualityUnit = "blockChance[15..50]&&dexterityPenalty[10..0]&&damageReduction[5..25]";
    	} 
    	
    	String[] qualityUnitParts = qualityUnit.split("&&");
    	
    	try
    	{
	    	for(String qualityDescPart:qualityUnitParts)
	    	{
	    		boolean inverted = false;
	    		qualityDescPart = qualityDescPart.replace(" ", "");
	    		
	    		// There are 2 versions of the quality unit. The simplified version is parsed into the more complex version here...
	    		if (qualityDescPart.matches("[_A-Za-z]+\\[(-?\\d+\\.\\.-?\\d+)+\\]"))
	    		{
	    			String simpleFormula = qualityDescPart.replaceAll(".*?(-?\\d+\\.\\.-?\\d+).*", "$1");
	    			String[] parts = simpleFormula.split("\\.\\.");
	    			int min = Integer.parseInt(parts[0]);
	    			int max = Integer.parseInt(parts[1]);
	    			int junkMax = 0;
	    			int regularMax = 0;
	    			int rareMax = 0;
	    			if (min<max)
	    			{
		    			junkMax = new Double(min+((max-min)*.20)).intValue();
		    			regularMax = new Double(min+((max-min)*.65)).intValue();
		    			rareMax = new Double(min+((max-min)*.88)).intValue();

		    			String complexFormula = min+".."+(junkMax-1)+","+junkMax+".."+(regularMax-1)+","+regularMax+".."+(rareMax-1)+","+rareMax+".."+max;
	    				qualityDescPart = qualityDescPart.replaceAll("\\[.*", "")+"("+complexFormula+")";
	    			}
	    			else
	    			{
		    			junkMax = new Double(Math.round(min-((min-max)*.20))).intValue();
		    			regularMax = new Double(Math.round(min-((min-max)*.65))).intValue();
		    			rareMax = new Double(Math.round(min-((min-max)*.88))).intValue();

		    			inverted = true;
		    			
		    			String complexFormula = min+".."+(junkMax+1)+","+junkMax+".."+(regularMax+1)+","+regularMax+".."+(rareMax+1)+","+rareMax+".."+max;
	    				qualityDescPart = qualityDescPart.replaceAll("\\[.*", "")+"("+complexFormula+")";
	    			}
	    		}
	    		
	    		
	    		if (qualityDescPart.matches("[_A-Za-z]+\\((-?\\d+\\.\\.-?\\d+,?)+\\)")==false)
	    			return result;
	    		
	    		qualityDescPart = qualityDescPart.substring(0, qualityDescPart.length()-1);
	    		String[] parts = qualityDescPart.split("\\(");
	    		String propertyName = parts[0];
	    		String rangesPart = parts[1];
	    		
	    		
	    		double propertyValue = 0d;
	    		Object valueObj = entityProperties.get(propertyName);
				if (valueObj!=null)
				{
					String valueStr = valueObj.toString();
					if (valueStr.length()>0)
					{
						if (valueStr.startsWith("DD"))
						{
							valueStr = valueStr.substring(2);
							String[] formulaParts = valueStr.toUpperCase().split("D");
							int dice = Integer.parseInt(formulaParts[0]);
							int sides = Integer.parseInt(formulaParts[1]);
							propertyValue = dice*sides;
						}
						else
						{
							propertyValue = Integer.parseInt(valueStr);
							
						}
					}
				}
	    		
	    		
	    		
	    		String[] ranges = rangesPart.split(",");
	    		
	    		int step = 0;
	    		for(String range:ranges)
	    		{
	    			String[] values = range.split("\\.\\.");
	    			double min = Integer.parseInt(values[0]);
	    			double max = Integer.parseInt(values[1]);
	    			
	    			if ((propertyValue>=min && propertyValue<=max) || 
	    					(propertyValue<=min && propertyValue>=max) ||
	    					
	    					(step==0 && inverted==false && propertyValue<min) || 
	    					(step==3 && inverted==false && propertyValue>max) ||
	    					
	    					(step==0 && inverted && propertyValue>min) || 
	    					(step==3 && inverted && propertyValue<max))
	    			{
	    				// Here we're tracking the quality where best is 0 and worst is 3, little math to do that from the step
	    				qualityNumbers.add((step-3)*-1d);
	    				break;
	    			}
	    			step++;
	    		}
	    	}	    		
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    	
    	// GO through the quality numbers and calculate the result
    	if (qualityNumbers.size()>0)
    	{
	    	/*
	    	 * In this version of the algorithm, the first qualityNumber defines the start point. Every other qualityNumber is
	    	 * weighed in at only 25% each. Ah whatever, ask nik for details.
	    	 */
	    	double relativeQualityStartPoint = qualityNumbers.get(0)*0.24;
	    	double finalQuality = 3.5d-qualityNumbers.get(0);
	    	for(int i = 1; i<qualityNumbers.size(); i++)
	    	{
	    		double qualityAdjustment = qualityNumbers.get(i)*0.24-relativeQualityStartPoint;
	    		finalQuality-=qualityAdjustment;
	    		
	    	}
	    	result = new Double(finalQuality*100).longValue();
    	}    	
    	
    	return result;
    }
    
    public static String determineQuality(Map<String, Object> entityProperties)
    {
    	entityProperties = new HashMap<String, Object>(entityProperties);
    	
    	String result = "";
    	
    	
    	// Override the quality class if it is set...
    	{
	    	String qualityClass = null;
	    	String qualityClassOverride = (String)entityProperties.get("forcedItemQuality");
	    	if ("Junk".equals(qualityClassOverride))
	    		qualityClass = "item-junk";
	    	else if ("Average".equals(qualityClassOverride))
	    		qualityClass = "";
	    	else if ("Rare".equals(qualityClassOverride))
	    		qualityClass = "item-rare";
	    	else if ("Unique".equals(qualityClassOverride))
	    		qualityClass = "item-unique";
	    	else if ("Epic".equals(qualityClassOverride))
	    		qualityClass = "item-epic";
	    	else if ("Custom".equals(qualityClassOverride))
	    		qualityClass = "item-custom";
	    	else if ("Magic".equals(qualityClassOverride))
	    		qualityClass = "item-magic";
	    	
	    	if (qualityClass!=null)
	    		return qualityClass;
    	}

    	
    	
    	List<Double> qualityNumbers = new ArrayList<Double>();
    	String qualityUnit = (String)entityProperties.get("qualityUnit");
    	
    	// Override for the quality unit. I'm testing a global quality determination now...
    	if ("Weapon".equals(entityProperties.get("itemType")))
    	{
    		qualityUnit = "_weaponMaxDamage[0..50]&&blockChance[5..40]";
    		
    		String damageFormula = (String)entityProperties.get("weaponDamage");
    		Double critChance = 0d;
    		if (entityProperties.get("weaponDamageCriticalChance")!=null)
    		{
    			try
    			{
    				critChance = ((Long)entityProperties.get("weaponDamageCriticalChance")).doubleValue();
    			}
    			catch(Exception e)
    			{}
    		}
    		Double critMultiplier = 1d;
    		if (entityProperties.get("weaponDamageCriticalMultiplier")!=null)
    		{
    			try
    			{
    				critMultiplier = (Double)entityProperties.get("weaponDamageCriticalMultiplier");
    			}
    			catch(Exception e)
    			{}
    		}
    		Double weaponMaxDamage = getWeaponMaxDamage(damageFormula, critMultiplier, critChance); 
    		Double weaponAverageDamage = getWeaponAverageDamage(damageFormula, critMultiplier, critChance);
    		
    		entityProperties.put("_weaponMaxDamage", weaponMaxDamage.intValue());
    		entityProperties.put("_weaponAverageDamage", weaponAverageDamage.intValue());
    	}
    	else if ("Armor".equals(entityProperties.get("itemType")))
    	{
    		qualityUnit = "blockChance[50..95]&&dexterityPenalty[10..0]&&damageReduction[5..25]";
    	} 
    	else if ("Shield".equals(entityProperties.get("itemType")))
    	{
    		qualityUnit = "blockChance[15..50]&&dexterityPenalty[10..0]&&damageReduction[5..25]";
    	} 
    	else
    		return "";
    	
    	String[] qualityUnitParts = qualityUnit.split("&&");
    	
    	try
    	{
	    	for(String qualityDescPart:qualityUnitParts)
	    	{
	    		boolean inverted = false;
	    		qualityDescPart = qualityDescPart.replace(" ", "");
	    		
	    		// There are 2 versions of the quality unit. The simplified version is parsed into the more complex version here...
	    		if (qualityDescPart.matches("[_A-Za-z]+\\[(-?\\d+\\.\\.-?\\d+)+\\]"))
	    		{
	    			String simpleFormula = qualityDescPart.replaceAll(".*?(-?\\d+\\.\\.-?\\d+).*", "$1");
	    			String[] parts = simpleFormula.split("\\.\\.");
	    			int min = Integer.parseInt(parts[0]);
	    			int max = Integer.parseInt(parts[1]);
	    			int junkMax = 0;
	    			int regularMax = 0;
	    			int rareMax = 0;
	    			if (min<max)
	    			{
		    			junkMax = new Double(min+((max-min)*.20)).intValue();
		    			regularMax = new Double(min+((max-min)*.65)).intValue();
		    			rareMax = new Double(min+((max-min)*.88)).intValue();

		    			String complexFormula = min+".."+(junkMax-1)+","+junkMax+".."+(regularMax-1)+","+regularMax+".."+(rareMax-1)+","+rareMax+".."+max;
	    				qualityDescPart = qualityDescPart.replaceAll("\\[.*", "")+"("+complexFormula+")";
	    			}
	    			else
	    			{
		    			junkMax = new Double(Math.round(min-((min-max)*.20))).intValue();
		    			regularMax = new Double(Math.round(min-((min-max)*.65))).intValue();
		    			rareMax = new Double(Math.round(min-((min-max)*.88))).intValue();

		    			inverted = true;
		    			
		    			String complexFormula = min+".."+(junkMax+1)+","+junkMax+".."+(regularMax+1)+","+regularMax+".."+(rareMax+1)+","+rareMax+".."+max;
	    				qualityDescPart = qualityDescPart.replaceAll("\\[.*", "")+"("+complexFormula+")";
	    			}
	    		}
	    		
	    		
	    		if (qualityDescPart.matches("[_A-Za-z]+\\((-?\\d+\\.\\.-?\\d+,?)+\\)")==false)
	    			return result;
	    		
	    		qualityDescPart = qualityDescPart.substring(0, qualityDescPart.length()-1);
	    		String[] parts = qualityDescPart.split("\\(");
	    		String propertyName = parts[0];
	    		String rangesPart = parts[1];
	    		
	    		
	    		double propertyValue = 0d;
	    		Object valueObj = entityProperties.get(propertyName);
				if (valueObj!=null)
				{
					String valueStr = valueObj.toString();
					if (valueStr.length()>0)
					{
						if (valueStr.startsWith("DD"))
						{
							valueStr = valueStr.substring(2);
							String[] formulaParts = valueStr.toUpperCase().split("D");
							int dice = Integer.parseInt(formulaParts[0]);
							int sides = Integer.parseInt(formulaParts[1]);
							propertyValue = dice*sides;
						}
						else
						{
							propertyValue = Integer.parseInt(valueStr);
							
						}
					}
				}
	    		
	    		
	    		
	    		String[] ranges = rangesPart.split(",");
	    		
	    		int step = 0;
	    		for(String range:ranges)
	    		{
	    			String[] values = range.split("\\.\\.");
	    			double min = Integer.parseInt(values[0]);
	    			double max = Integer.parseInt(values[1]);
	    			
	    			if ((propertyValue>=min && propertyValue<=max) || 
	    					(propertyValue<=min && propertyValue>=max) ||
	    					
	    					(step==0 && inverted==false && propertyValue<min) || 
	    					(step==3 && inverted==false && propertyValue>max) ||
	    					
	    					(step==0 && inverted && propertyValue>min) || 
	    					(step==3 && inverted && propertyValue<max))
	    			{
	    				// Here we're tracking the quality where best is 0 and worst is 3, little math to do that from the step
	    				qualityNumbers.add((step-3)*-1d);
	    				break;
	    			}
	    			step++;
	    		}
	    	}	    		
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    	
    	// GO through the quality numbers and calculate the result
    	if (qualityNumbers.size()>0)
    	{
	    	/*
	    	 * In this version of the algorithm, the first qualityNumber defines the start point. Every other qualityNumber is
	    	 * weighed in at only 25% each. Ah whatever, ask nik for details.
	    	 */
	    	double relativeQualityStartPoint = qualityNumbers.get(0)*0.24;
	    	double finalQuality = 3.5d-qualityNumbers.get(0);
	    	for(int i = 1; i<qualityNumbers.size(); i++)
	    	{
	    		double qualityAdjustment = qualityNumbers.get(i)*0.24-relativeQualityStartPoint;
	    		finalQuality-=qualityAdjustment;
	    		
	    	}
	    	
			if (finalQuality<1)
				result = "item-junk";
			else if (finalQuality<2)
				result = "";
			else if (finalQuality<3)
				result = "item-rare";
			else 
				result = "item-unique";
    	}    	
    	
    	return result;
    }
    
//    public static String renderCharacterIcon(CachedEntity character, List<CachedEntity> equipment)
//    {
//    	
//    }
    
    public static String renderItem(CachedEntity item)
    {
    	return renderItem(null, null, null, item, false);
    }
    
    public static String renderItem(ODPDBAccess db, CachedEntity character, CachedEntity item)
    {
    	return renderItem(db, null, character, item, false);
    }
    
    public static String renderItem(ODPDBAccess db, HttpServletRequest request, CachedEntity character, CachedEntity item, boolean popupEmbedded)
    {
		if (item==null)
			return "";
		
		boolean hasRequiredStrength = true;
		if (character!=null)
		{
			Double characterStrength = db.getCharacterStrength(character);
			
			Double strengthRequirement = null;
			try
			{
				strengthRequirement = (Double)item.getProperty("strengthRequirement");
			}
			catch(Exception e)
			{
				// Ignore exceptions
			}
			
			if (strengthRequirement!=null && characterStrength<strengthRequirement)
				hasRequiredStrength = false;
		}
        String notEnoughStrengthClass = "";
        if (hasRequiredStrength==false)
        	notEnoughStrengthClass = "not-enough-strength";
			
        String lowDurabilityClass = "";
		Long maxDura = (Long)item.getProperty("maxDurability");
		Long currentDura = (Long)item.getProperty("durability");
		boolean durabilityNotNull = maxDura != null && currentDura != null;
		
		if (durabilityNotNull && currentDura < maxDura * .2)
			lowDurabilityClass = "low-durability ";
		if (durabilityNotNull && currentDura < maxDura * .1)
			lowDurabilityClass = "very-low-durability ";
		
		String qualityClass = determineQuality(item.getProperties());
		String label = (String)item.getProperty("label"); 
		if (label==null || label.trim().equals("") || (label=WebUtils.htmlSafe(label).trim()).equals(""))
			label = (String)item.getProperty("name");

		String iconUrl = (String)item.getProperty("icon");
		if (iconUrl!=null && iconUrl.startsWith("http://"))
			iconUrl = "https://"+iconUrl.substring(7);
		else if (iconUrl!=null && iconUrl.startsWith("http")==false)
			iconUrl = "https://initium-resources.appspot.com/"+iconUrl;
		
		
		
		Long quantity = (Long)item.getProperty("quantity");
		String quantityDiv = "";
		if (quantity!=null){
			quantityDiv="<div class='main-item-quantity-indicator-container'><div class='main-item-quantity-indicator'>"+formatNumber(quantity)+"</div></div>";
		}
		
		if (popupEmbedded)
			return "<span class='"+notEnoughStrengthClass+"'><a class='"+qualityClass+"' onclick='reloadPopup(this, \""+WebUtils.getFullURL(request)+"\", event)' rel='/viewitemmini.jsp?itemId="+item.getKey().getId()+"'><div class='main-item-image-backing'>"+quantityDiv+"<img src='"+iconUrl+"' border=0/></div><div class='"+lowDurabilityClass+"main-item-name'>"+label+"</div></a></span>";
		else
			return "<span class='"+notEnoughStrengthClass+"'><a class='clue "+qualityClass+"' rel='/viewitemmini.jsp?itemId="+item.getKey().getId()+"'><div class='main-item-image-backing'>"+quantityDiv+"<img src='"+iconUrl+"' border=0/></div><div class='"+lowDurabilityClass+"main-item-name'>"+label+"</div></a></span>";
    }

    public static String renderCollectable(CachedEntity item)
    {
    	return renderCollectable(null, item, false);
    }
    
    public static String renderCollectable(HttpServletRequest request, CachedEntity collectable, boolean popupEmbedded)
    {
    	if (collectable==null)
    		return "";
    	
    	if (popupEmbedded)
    		return "<a onclick='reloadPopup(this, \""+WebUtils.getFullURL(request)+"\", event)' rel='/viewitemmini.jsp?itemId="+collectable.getKey().getId()+"'><div class='main-item-image-backing'><img src='https://initium-resources.appspot.com/"+collectable.getProperty("icon")+"' border=0/></div><div class='main-item-name'>"+collectable.getProperty("name")+"</div></a>";
    	else
    		return "<a rel='/viewitemmini.jsp?itemId="+collectable.getKey().getId()+"'><div class='main-item-image-backing'><img src='https://initium-resources.appspot.com/"+collectable.getProperty("icon")+"' border=0/></div><div class='main-item-name'>"+collectable.getProperty("name")+"</div></a>";
    }

    public static String renderCharacter(CachedEntity userOfCharacter, CachedEntity character)
    {
    	return renderCharacter(userOfCharacter, character, true, false);
    }
    
    public static String renderCharacter(CachedEntity userOfCharacter, CachedEntity character, boolean includePopupLink, boolean meStyle)
    {
    	if (character==null)
    		return "";
    	
    	String name = (String)character.getProperty("name");
    	if (name.contains("<"))
    		name = name.replaceAll("<.*?>", "");
    	
    	if (enumEquals(character.getProperty("mode"), CharacterMode.UNCONSCIOUS))
    	{
    		name = "Unconscious "+name;
    	}
    	
    	String nameClass = (String)character.getProperty("nameClass");
    	if ((nameClass==null || nameClass.equals("")) && userOfCharacter!=null && Boolean.TRUE.equals(userOfCharacter.getProperty("premium")))
    		nameClass = "premium-character-name";
    	
    	if (meStyle)
    		nameClass = "chatMessage-text";
    	
    	if (includePopupLink)
    		return "<a class='clue "+nameClass+"' rel='/viewcharactermini.jsp?characterId="+character.getKey().getId()+"'>"+name+"</a>";
    	else
    		return "<span class='"+nameClass+"'>"+name+"</span>";
    }
    
    public static String renderCharacterWidget(HttpServletRequest request, ODPDBAccess db, CachedEntity character, CachedEntity selfUser, boolean leftSide)
    {
    	return renderCharacterWidget(request, db, character, selfUser, null, leftSide, true, false, false);
    }
    
    
    public static String renderCharacterWidget(HttpServletRequest request, ODPDBAccess db, CachedEntity character, CachedEntity selfUser, CachedEntity group, boolean leftSide, boolean showBuffs, boolean largeSize, boolean showGroup)
    {
    	boolean isSelf = false;
    	String lowDurabilityClass = "";
    	if (GameUtils.equals(db.getCurrentCharacterKey(), character.getKey()))
    		isSelf = true;
    	
    	boolean isCloaked = false;
    	if (GameUtils.equals(character.getProperty("cloaked"), true))
    		isCloaked = true;
    	
    	List<CachedEntity> equipment = db.getEntity((Key)character.getProperty("equipmentHelmet"),
    												(Key)character.getProperty("equipmentChest"),
    												(Key)character.getProperty("equipmentLegs"),
    												(Key)character.getProperty("equipmentBoots"),
    												(Key)character.getProperty("equipmentGloves"),
    												(Key)character.getProperty("equipmentLeftHand"),
    												(Key)character.getProperty("equipmentRightHand"),
    												(Key)character.getProperty("equipmentShirt"));
    	
    	boolean hasInvalidEquipment = false;
    	
    	
		CachedEntity equipmentHelmet = equipment.get(0);
		if (character.getProperty("equipmentHelmet")!=null && equipmentHelmet==null)
		{
			hasInvalidEquipment=true;
			character.setProperty("equipmentHelmet", null);
		}
		String equipmentHelmetUrl = null;
		if (equipmentHelmet!=null) 
			equipmentHelmetUrl = GameUtils.getResourceUrl(equipmentHelmet.getProperty(GameUtils.getItemIconToUseFor("equipmentHelmet", equipmentHelmet)));

		
		CachedEntity equipmentChest = equipment.get(1);
		if (character.getProperty("equipmentChest")!=null && equipmentChest==null)
		{
			hasInvalidEquipment=true;
			character.setProperty("equipmentChest", null);
		}
		String equipmentChestUrl = null;
		if (equipmentChest!=null)
			equipmentChestUrl = GameUtils.getResourceUrl(equipmentChest.getProperty(GameUtils.getItemIconToUseFor("equipmentChest", equipmentChest)));

		
		CachedEntity equipmentLegs = equipment.get(2);
		if (character.getProperty("equipmentLegs")!=null && equipmentLegs==null)
		{
			hasInvalidEquipment=true;
			character.setProperty("equipmentLegs", null);
		}
		String equipmentLegsUrl = null;
		if (equipmentLegs!=null)
			equipmentLegsUrl = GameUtils.getResourceUrl(equipmentLegs.getProperty(GameUtils.getItemIconToUseFor("equipmentLegs", equipmentLegs)));

		
		CachedEntity equipmentBoots = equipment.get(3);
		if (character.getProperty("equipmentBoots")!=null && equipmentBoots==null)
		{
			hasInvalidEquipment=true;
			character.setProperty("equipmentBoots", null);
		}
		String equipmentBootsUrl = null;
		if (equipmentBoots!=null)
			equipmentBootsUrl = GameUtils.getResourceUrl(equipmentBoots.getProperty(GameUtils.getItemIconToUseFor("equipmentBoots", equipmentBoots)));

		
		CachedEntity equipmentGloves = equipment.get(4);
		if (character.getProperty("equipmentGloves")!=null && equipmentGloves==null)
		{
			hasInvalidEquipment=true;
			character.setProperty("equipmentGloves", null);
		}
		String equipmentGlovesUrl = null;
		if (equipmentGloves!=null)
			equipmentGlovesUrl = GameUtils.getResourceUrl(equipmentGloves.getProperty(GameUtils.getItemIconToUseFor("equipmentGloves", equipmentGloves)));

		
		CachedEntity equipmentLeftHand = equipment.get(5);
		if (character.getProperty("equipmentLeftHand")!=null && equipmentLeftHand==null)
		{
			hasInvalidEquipment=true;
			character.setProperty("equipmentLeftHand", null);
		}
		String equipmentLeftHandUrl = null;
		if (equipmentLeftHand!=null)
			equipmentLeftHandUrl = GameUtils.getResourceUrl(equipmentLeftHand.getProperty(GameUtils.getItemIconToUseFor("equipmentLeftHand", equipmentLeftHand)));

		
		CachedEntity equipmentRightHand = equipment.get(6);
		if (character.getProperty("equipmentRightHand")!=null && equipmentRightHand==null)
		{
			hasInvalidEquipment=true;
			character.setProperty("equipmentRightHand", null);
		}
		String equipmentRightHandUrl = null;
		if (equipmentRightHand!=null)
			equipmentRightHandUrl = GameUtils.getResourceUrl(equipmentRightHand.getProperty(GameUtils.getItemIconToUseFor("equipmentRightHand", equipmentRightHand)));

		
		CachedEntity equipmentShirt = equipment.get(7);
		if (character.getProperty("equipmentShirt")!=null && equipmentShirt==null)
		{
			hasInvalidEquipment=true;
			character.setProperty("equipmentShirt", null);
		}
		String equipmentShirtUrl = null;
		if (equipmentShirt!=null)
			equipmentShirtUrl = GameUtils.getResourceUrl(equipmentShirt.getProperty(GameUtils.getItemIconToUseFor("equipmentShirt", equipmentShirt)));

		for (int i = 0; i < equipment.size(); i++)
		{
			if (equipment.get(i) != null && isDurabilityVeryLow(equipment.get(i)))
			{
				lowDurabilityClass = "very-low-durability ";
				break;
			}
			else if (equipment.get(i) != null && isDurabilityLow(equipment.get(i)))
			{
				lowDurabilityClass = "low-durability ";
			}
				
		}
		// This is a workaround for the fact that sometimes equipment stays equipped after it has been deleted
		if (hasInvalidEquipment)
		{
			db.getDB().put(character);
			log.log(Level.SEVERE, "The character had an item equipped that was deleted from the database. The workaround fixed it, but this should be fixed in the future. It's likely a problem with double-saving the character somewhere.");
		}
		
		boolean is2Handed = false;
		if (equipmentRightHand!=null && "2Hands".equals(equipmentRightHand.getProperty("equipSlot")))
		{
			is2Handed = true;
		}
		
		StringBuilder nameAndBars = new StringBuilder();

		String characterName = (String)character.getProperty("name");
    	if (enumEquals(character.getProperty("mode"), CharacterMode.UNCONSCIOUS))
    	{
    		characterName = "Unconscious "+characterName;
    	}
    	
		
		int hitpointsPercentage = (int)((double)character.getProperty("hitpoints")/(double)character.getProperty("maxHitpoints")*100d);
		int hitpoints = ((Double)character.getProperty("hitpoints")).intValue();
		int maxHitpoints = ((Double)character.getProperty("maxHitpoints")).intValue();
		if (leftSide)
			nameAndBars.append("<div style='display:inline-block; max-width:230px'>");
		else
			nameAndBars.append("<div style='display:inline-block; text-align:right;max-width:100px; overflow: hidden;'>");
		if (isSelf)
			nameAndBars.append("	<a class='hint' rel='#profile' style='cursor:pointer'>"+characterName+"</a>");
		else
			nameAndBars.append("	<a>"+characterName+"</a>");
		nameAndBars.append("		<div id='hitpointsBar' style='position:relative; display:block; background-color:#777777; width:100px; height:12px;text-align:left'>");
		nameAndBars.append("			<div style='position:absolute; display:inline-block; background-color:#FF0000; width:"+hitpointsPercentage+"px; height:12px;'>");
		nameAndBars.append("			</div>");
		if (leftSide)
			nameAndBars.append("			<p style='margin:0px; padding:0px; width:100px; text-align:left; display:block; font-size:11px;position:absolute;font-family:Sans-serif;'>"+hitpoints+"/"+maxHitpoints+"</p>");
		else
			nameAndBars.append("			<p style='margin:0px; padding:0px; width:100px; text-align:right; display:block; font-size:11px;position:absolute;font-family:Sans-serif;'>"+hitpoints+"/"+maxHitpoints+"</p>");
		nameAndBars.append("		</div>");
		
		// Insert the group stuff if we have one passed in
		if (group!=null)
		{
			nameAndBars.append("<a onclick='viewGroup("+group.getId()+")' class='main-highlight'>"+group.getProperty("name")+"</a>");
			if (character.getProperty("groupRank")!=null)
				nameAndBars.append("<div class='main-highlight' style='font-size:14px'>"+character.getProperty("groupRank")+"</div>");
		}
		
		nameAndBars.append("</div>");
		
		
		
		StringBuilder sb = new StringBuilder();

		if (leftSide==false)
		{
			sb.append("<div class='character-display-box' style='float:right'>");
			sb.append(nameAndBars);
		}
		else
			sb.append("<div class='character-display-box'>");
		
		
		if (isSelf)
			sb.append("<a class='clue' rel='/viewcharactermini.jsp?characterId="+character.getKey().getId()+"'>");
		
		String sizePrepend = "";
		if (largeSize)
			sizePrepend = "-64px";
				
		if (isCloaked==false)
		{
			sb.append("<div class='"+lowDurabilityClass+"avatar-equip-backing"+sizePrepend+" backdrop3d' style='background-color:none;'>");

			sb.append("<div class='avatar-equip-cloak"+sizePrepend+"' style='background-image:url(\"https://initium-resources.appspot.com/images/ui/newui/avatar-silhouette-male1.png\")'></div>");
			
			if (equipmentBootsUrl!=null)
				sb.append("<div class='avatar-equip-boots"+sizePrepend+"' style='background-image:url(\""+equipmentBootsUrl+"\")'></div>");
			if (equipmentLegsUrl!=null)
				sb.append("<div class='avatar-equip-legs"+sizePrepend+"' style='background-image:url(\""+equipmentLegsUrl+"\")'></div>");
			if (equipmentShirtUrl!=null)
				sb.append("<div class='avatar-equip-shirt"+sizePrepend+"' style='background-image:url(\""+equipmentShirtUrl+"\")'></div>");
			if (equipmentChestUrl!=null)
				sb.append("<div class='avatar-equip-chest"+sizePrepend+"' style='background-image:url(\""+equipmentChestUrl+"\")'></div>");
			if (equipmentHelmetUrl!=null)
				sb.append("<div class='avatar-equip-helmet"+sizePrepend+"' style='background-image:url(\""+equipmentHelmetUrl+"\")'></div>");
			if (equipmentGlovesUrl!=null)
			{
				sb.append("<div class='avatar-equip-gloves-left"+sizePrepend+"' style='background-image:url(\""+equipmentGlovesUrl+"\")'></div>");
				sb.append("<div class='avatar-equip-gloves-right"+sizePrepend+"' style='background-image:url(\""+equipmentGlovesUrl+"\")'></div>");
			}
			if (is2Handed==false)
			{
				if (equipmentLeftHandUrl!=null)
					sb.append("<div class='avatar-equip-leftHand"+sizePrepend+"' style='background-image:url(\""+equipmentLeftHandUrl+"\")'></div>");
				if (equipmentRightHandUrl!=null)
					sb.append("<div class='avatar-equip-rightHand"+sizePrepend+"' style='background-image:url(\""+equipmentRightHandUrl+"\")'></div>");
			}
			else
			{
				if (equipmentRightHandUrl!=null)
					sb.append("<div class='avatar-equip-2hands"+sizePrepend+"' style='background-image:url(\""+equipmentRightHandUrl+"\")'></div>");
			}
		}
		else
		{
			sb.append("<div class='avatar-equip-backing"+sizePrepend+" backdrop3d' style='background-color:none;'>");

			sb.append("<div class='avatar-equip-cloak"+sizePrepend+"' style='background-image:url(\"https://initium-resources.appspot.com/images/cloak1.png\")'></div>");
			
		}
		sb.append("</div>");
		if (isSelf)
			sb.append("</a>");
		
		if (isSelf && selfUser!=null)
		{
			ShardedCounterService cs = ShardedCounterService.getInstance(db.getDB());
			Long referralViews = cs.readCounter(selfUser.getKey(), "referralViews");
			Long referralSignups = cs.readCounter(selfUser.getKey(), "referralSignups");
			Long referralDonations = cs.readCounter(selfUser.getKey(), "referralDonations");
			if (referralViews==null) referralViews = 0L;
			if (referralSignups==null) referralSignups = 0L;
			if (referralDonations==null) referralDonations = 0L;
			
			sb.append("<div class='hiddenTooltip' id='profile'>");
			sb.append("<h5 style='margin-top:0px;'>Your Referrals</h5>");
			sb.append("<p><a href='"+determineReferralUrl(selfUser)+"' title='Share this link online and with your friends'>Your referral link (share this!)</a></p>");
			sb.append("<div style='margin-left:10px'>");
			sb.append("<p>Referral views: "+referralViews+"<br>");
			sb.append("Referral signups: "+referralSignups+"<br>");
			sb.append("Referral donations: $"+GameUtils.formatNumber(referralDonations.doubleValue()/100d, true)+"</p>");
			sb.append("</div>");
			sb.append("<br>");
			sb.append("<h5 style='margin-top:0px;'>"+character.getProperty("name")+"'s Options</h5>");
			sb.append("<p><a onclick='viewProfile()'>View "+character.getProperty("name")+"'s profile</a></p>");
			sb.append("<p><a onclick='popupCharacterTransferService("+character.getKey().getId()+", \""+character.getProperty("name")+"\", \""+request.getAttribute("characterToTransfer")+"\")' style='cursor:pointer'>Open the Character Transfer Service</a></p>");
			sb.append("<p><a onclick='logout()'>Logout</a></p>");
			if (request.getAttribute("characterList")!=null)
			{
				sb.append("<h5>Switch Characters</h5>");
				sb.append("<ul class='switch-characters-list'>");
				
				List<CachedEntity> characterList = (List<CachedEntity>)request.getAttribute("characterList");
				Collections.sort(characterList, new Comparator<CachedEntity>()
				{
					@Override
					public int compare(CachedEntity o1, CachedEntity o2)
					{
						return ((String)o1.getProperty("name")).compareTo((String)o2.getProperty("name"));
					}
				});
				for(CachedEntity c:characterList)
				{
					if (c.getProperty("name").toString().startsWith("Dead ")==false && "Zombie".equals(c.getProperty("status"))==false)
						sb.append("<li><a onclick='doDeleteCharacter(event,"+c.getId()+",\""+WebUtils.htmlSafe((String)c.getProperty("name"))+"\")'>X</a> <a onclick='switchCharacter("+c.getKey().getId()+")'>"+c.getProperty("name")+"</a></li>");	
				}
				sb.append("</ul>");
				sb.append("<p><a href='newcharacter.jsp'>Create a new character</a></p>");
			}
			else
			{
				sb.append("<p>To enable multiple character support, <a href='profile.jsp'>upgrade to premium!</a></p>");
			}
			
			
			
			sb.append("</div>");
		}

		if (leftSide)
		{
			sb.append(nameAndBars);
		}

		
		// Show the buffs
		if (showBuffs)
		{
			List<CachedEntity> buffs = db.getBuffsFor(character.getKey());
			if (buffs!=null && buffs.isEmpty()==false)
			{
				sb.append("<div class='buff-pane hint' rel='#buffDetails'>");
				for(CachedEntity buff:buffs)
				{
					sb.append("<img src='"+""+GameUtils.getResourceUrl(buff.getProperty("icon"))+"' border='0'>");
				}
				sb.append("</div>");
				
				sb.append("<div class='hiddenTooltip' id='buffDetails'>");
				sb.append("<h4 style='margin-top:0px;'>Your buffs/debuffs</h4>");
				sb.append(renderBuffsList(buffs));
				sb.append("</div>");
				
			}
		}		
		sb.append("</div>");							
		
		
		
		
		return sb.toString();
    }

    public static String getItemIconToUseFor(String equipmentSlot, CachedEntity itemInSlot)
    {
    	// If we only have 1 icon specified anyway, we'll just return that
    	if (itemInSlot.getProperty("icon2")==null)
    		return "icon";

    	
    	equipmentSlot = equipmentSlot.substring(9);
    	
		String equipSlotRaw = (String)itemInSlot.getProperty("equipSlot");

		if (equipSlotRaw==null)
			return "icon";
		
		equipSlotRaw = equipSlotRaw.replace(" and ", ",");
		if (equipSlotRaw.equals("Ring"))
			equipSlotRaw = "LeftRing, RightRing";

		
		
		String[] equipSlots = equipSlotRaw.split(",");
		
		int i = 1; 
		for(String slot:equipSlots)
		{
			slot = slot.trim();
			if (equipmentSlot.equals(slot))
			{
				String iconToUse = "icon"+i;
				if (itemInSlot.getProperty(iconToUse)!=null)
					return iconToUse;
				else
					return "icon";
			}
				
			i++;
		}
    	
		return "icon";
    }
    
    
    
    public static String renderBuffsList(List<CachedEntity> buffs)
    {
    	StringBuilder sb = new StringBuilder();
		for(CachedEntity buff:buffs)
		{
			sb.append("<div class='buff-detail'>");
			sb.append("<img src='https://initium-resources.appspot.com/"+buff.getProperty("icon")+"' border='0'/>");
			sb.append("<div class='buff-detail-header'>");
			sb.append("<h5>"+buff.getProperty("name")+"</h5>");
			for(int i = 1; i<=3; i++)
			{
				if (buff.getProperty("field"+i+"Name")!=null && ((String)buff.getProperty("field"+i+"Name")).trim().equals("")==false)
				{
					String name = (String)buff.getProperty("field"+i+"Name");
					name = name.replaceAll("([A-Z][a-z])", " $1");
					name = name.toLowerCase();
					sb.append("<div class='buff-detail-effect'> "+buff.getProperty("field"+i+"Effect")+" "+name+"</div>");
				}
			}
			sb.append("</div>");
			String description = (String)buff.getProperty("description");
			if (description!=null)
			{
				sb.append("<div class='buff-detail-description item-flavor-description'>");
				sb.append(description);
				sb.append("</div>");
			}
			Date expiry = (Date)buff.getProperty("expiry");
			if (expiry!=null)
				sb.append("<div class='buff-detail-expiry'>Expires in "+getTimePassedShortString(expiry)+"</div>");
			sb.append("</div>");
		}
		return sb.toString();
    }
    
    public static String renderSimpleBanner(String bannerUrl)
    {
    	return renderSimpleBanner(bannerUrl, null);
    }
    
    public static String renderSimpleBanner(String bannerUrl, String titleText)
    {
    	if (bannerUrl==null)
    		return "";
    	StringBuilder sb = new StringBuilder();
    	
		sb.append("<img class='main-page-banner-image' src='https://initium-resources.appspot.com/images/banner-backing.jpg' border=0 />");
		sb.append("<div class='main-banner-container' style='z-index:1000100'>");
		sb.append("	<img class='main-page-banner-image' src='https://initium-resources.appspot.com/images/banner-backing.jpg' border=0 />");
		sb.append("	<div class='main-banner'>");
		sb.append("		<img class='main-page-banner-image' src='"+bannerUrl+"' border=0 />");
		sb.append("		<div class='banner-shadowbox' style=\"background: url('"+bannerUrl+"') no-repeat center / contain;\">");
		if (titleText!=null)
			sb.append("   <h1 style='text-align: center; font-size:60px'>"+titleText+"</h1>");
		sb.append("		</div>");
		sb.append("	</div>");
		sb.append("</div>");
    	
		return sb.toString();
    }
    
    
    public static String determineLeaveGroupWaitTime(CachedEntity character)
    {
    	Date leaveTime = (Date)character.getProperty("groupLeaveDate");
    	if (leaveTime == null) return null;
    	
    	return GameUtils.getTimePassedShortString(leaveTime);
    }
    
	public static boolean isPlayerIncapacitated(CachedEntity character)
	{
		if (character==null) throw new IllegalArgumentException("Character cannot be null.");
		
		// Only players are incapacitated as Zombie
		if ("NPC".equals(character.getProperty("type"))==false)
			if ("Zombie".equals(character.getProperty("status")))
				return true;
		
		// Dead chars dropped in rest area still get hp set to 1, so check mode first
		if ("DEAD".equals(character.getProperty("mode")))
			return true;
			
		if ((Double)character.getProperty("hitpoints")<=0)
			return true;
		
		return false;
	}
	
	public static boolean normalizeDatabaseState_Character(CachedDatastoreService ds, CachedEntity character, CachedEntity location)
	{
		if (ds==null || character==null) return false;
		if (location==null) location = ds.getIfExists((Key)character.getProperty("locationKey"));
		
		boolean changed = false;
		boolean npc = "NPC".equals(character.getProperty("type"));
		String mode = (String)character.getProperty("mode");
		
		// Check modes: Monster mode should only be null, NORMAL, DEAD or COMBAT
		if (npc && mode!=null && isContainedInList("NORMAL,DEAD,COMBAT", mode)==false)
		{
			character.setProperty("mode", "NORMAL");
			character.setProperty("combatType", null);
			character.setProperty("combatant", null);
			ds.put(character);
			changed = true;
		}
		
		// Check DEAD mode
		if ((Double)character.getProperty("hitpoints")<=0 && "DEAD".equals(mode)==false && (npc || "UNCONSCIOUS".equals(mode)==false))
		{
			character.setProperty("mode", "DEAD");
			character.setProperty("combatType", null);
			character.setProperty("combatant", null);
			String name = (String)character.getProperty("name");
			if (name.startsWith("Dead ")==false)
				character.setProperty("name", "Dead "+name);
			ds.put(character);
			changed = true;
			if (npc && "TRUE".equals(location.getProperty("instanceModeEnabled")) && location.getProperty("instanceRespawnDate")==null)
			{
				Date instanceRespawnDate = (Date)location.getProperty("instanceRespawnDate");
				Long instanceRespawnDelay = (Long)location.getProperty("instanceRespawnDelay");
				if (instanceRespawnDate==null && instanceRespawnDelay!=null)
				{
					GregorianCalendar cal = new GregorianCalendar();
					cal.add(Calendar.MINUTE, instanceRespawnDelay.intValue());
					location.setProperty("instanceRespawnDate", cal.getTime());
					ds.put(location);
				}
			}
		}
		
		// Instance only: Check COMBAT mode
		// instanceModeEnabled doesn't catch the hybrid setup, so test manually
		Key defenceStructure = (Key)location.getProperty("defenceStructure");
		if (defenceStructure!=null || "Instance".equals(location.getProperty("combatType")))
		{
			if ("COMBAT".equals(mode))
			{
				// Combatant should be alive and in combat with character (location can legit be different)
				CachedEntity combatant = ds.getIfExists((Key)character.getProperty("combatant"));
				if (combatant==null || isPlayerIncapacitated(combatant) || "COMBAT".equals(combatant.getProperty("mode"))==false || equals(character.getKey(), combatant.getProperty("combatant"))==false)
				{
					character.setProperty("mode", "NORMAL");
					character.setProperty("combatType", null);
					character.setProperty("combatant", null);
					ds.put(character);
					changed = true;
				}
			}
		}
		return changed;
	}
	
    public static double getAverageFromCurveFormula(String formula)
    {
    	if (formula.startsWith("DD"))
    	{
    		double min = 0d;
    		double max = 0d;
			formula = formula.substring(2);
			formula = formula.toUpperCase();
			String[] formulaParts = formula.toUpperCase().split("D");
			int dice = Integer.parseInt(formulaParts[0]);
			int sides = Integer.parseInt(formulaParts[1]);
			max = dice*sides;
			min = dice;
			
			double average = min + ((max-min)/2);
			return average;
    	}
    	else
    		throw new IllegalArgumentException("Unsupported curve formula type: "+formula);
    }
    
    
    public static String resolveFormulas(String text, boolean simpleMode, boolean editMode)
    {
    	Random rnd = new Random();

        Matcher msg = Pattern.compile("\\{\\{.*\\}\\}").matcher(text);

        
        while(msg.find()==true)
        {
            String formula = msg.group().toLowerCase();
            formula = formula.replaceAll("(\\{|\\})", "");
            String originalFormula = formula.toString();
            try
            {


                // Now within this formula, look for things like 1d6 or 3d20dl1..etc and calculate their random values and
                // drop the lowest rolls based on the number after dl (dl stands for drop lowest)
                Matcher formulaMatcher = Pattern.compile("(?i)\\d+d\\d+d(l|h)\\d+").matcher(formula);
                int count = 0;
                while(formulaMatcher.find()==true)
                {
                	count++;
                	if (count>100) throw new UserErrorMessage("Dice formula has too many parts.");
                    String dice = formulaMatcher.group();
                    String[] diceParts = splitString(dice, "d");
                    Integer part1 = Convert.StrToInteger(diceParts[0]);
                    Integer part2 = Convert.StrToInteger(diceParts[1]);
                    Integer part3 = Convert.StrToInteger(diceParts[2].substring(1));
                    boolean dropHighest = true;
                    if (diceParts[2].toLowerCase().startsWith("l"))
                        dropHighest=false;
                    String result = "";

                    // Perform rolls and remember what the lowest/highest values were...
                    ArrayList<Integer> rolls = new ArrayList<Integer>();
                    Integer lowest = 2000000000;
                    Integer highest = -1;
                    for(int i=0;i<part1; i++)
                    {
                        Integer value = (rnd.nextInt(part2)+1);
                        rolls.add(value);
                        if (value<lowest)
                            lowest = value;
                        if (value>highest)
                            highest = value;
                    }

                    // Drop the highest rolls...
                    for(int dropCount = 0; dropCount<part3; dropCount++)
                    {
                        for(int i=0;i<rolls.size(); i++)
                        {
                            lowest = 2000000000;
                            highest = -1;
                            Integer value = rolls.get(i);
                            if (value<lowest)
                                lowest = value;
                            if (value>highest)
                                highest = value;
                        }
                        for(int i = 0; i<rolls.size(); i++)
                        {
                            if (dropHighest==true && rolls.get(i)>=highest)
                            {
                                highest = rolls.get(i);
                                rolls.remove(i);
                                break;
                            }
                            else if (dropHighest==false && rolls.get(i)<=lowest)
                            {
                                lowest = rolls.get(i);
                                rolls.remove(i);
                                break;
                            }
                        }
                    }

                    // Put the formula together...
                    if (rolls.size()<12)
	                    for(int i=0;i<rolls.size(); i++)
	                    {
	                        if (!result.equals(""))
	                            result+="+";
	                        result += ""+rolls.get(i);
	                    }

                    formula = formula.replaceFirst("(?i)\\d+d\\d+d(l|h)\\d+", "("+result.toString()+")");


                }
                // Now within this formula, look for things like 1d6 or 3d20..etc and calculate their random values...
                formulaMatcher = Pattern.compile("(?i)\\d+d\\d+").matcher(formula);
                while(formulaMatcher.find()==true)
                {
                    String dice = formulaMatcher.group();
                    String[] diceParts = splitString(dice, "d");
                    Integer part1 = Convert.StrToInteger(diceParts[0]);
                    Integer part2 = Convert.StrToInteger(diceParts[1]);
                    String result = "";
                    for(int i=0;i<part1; i++)
                    {
                        if (!result.equals(""))
                            result+="+";
                        result += ""+(rnd.nextInt(part2)+1);
                    }

                    formula = formula.replaceFirst("(?i)\\d+d\\d+", "("+result.toString()+")");


                }
                // Now within this formula, look for things like 1_20 and 0_6..etc and calculate their random values...
                formulaMatcher = Pattern.compile("(?i)\\d+to\\d+").matcher(formula);
                while(formulaMatcher.find()==true)
                {
                    String dice = formulaMatcher.group();
                    String[] diceParts = splitString(dice, "to");
                    Integer part1 = Convert.StrToInteger(diceParts[0]);
                    Integer part2 = Convert.StrToInteger(diceParts[1]);
                    String result = ""+(rnd.nextInt((part2-part1)+1)+part1);

                    formula = formula.replaceFirst("(?i)\\d+to\\d+", "("+result.toString()+")");

                }

                try
                {
                    // Now solve the resulting math formula...
                    JEP jep = new JEP();

                    jep.addStandardFunctions();
                    jep.parseExpression(formula);
                    if (jep.hasError())
                        throw new UserErrorMessage(jep.getErrorInfo().replaceAll("\n", ""));
                    else
                    {
                        if (simpleMode)
                            formula = new Double(jep.getValue()).intValue()+"";
                        else
                        {
                        	if (formula.length()>50)
                        		formula = "<img src='https://initium-resources.appspot.com/images/dice1.png' border=0/> "+originalFormula+" = "+jep.getValue()+"";
                        	else
                        		formula = "<img src='https://initium-resources.appspot.com/images/dice1.png' border=0/> "+originalFormula+" = "+formula+" = "+jep.getValue()+"";
                        }
                    }
                }
                catch (org.cheffo.jeplite.ParseException e) {
				}

            }
            catch(UserErrorMessage use)
            {
                formula = " ["+originalFormula + "]--> "+use.getMessage()+" ";
            }


            text = text.replaceFirst("\\{\\{.*\\}\\}", formula);

        }

        return text;
    }
    

    public static String splitString(String text, String delimiter, int tokenIndex, boolean rightToLeft)
    {
        try
        {
            String[] strings = splitString(text, delimiter);
            if (rightToLeft == false)
            {
                return strings[tokenIndex];
            } else
            {
                return strings[strings.length - 1 - tokenIndex];
            }
        } catch (java.lang.ArrayIndexOutOfBoundsException arrayE)
        {
            return "";
        } catch (java.util.regex.PatternSyntaxException pse)
        {
            System.err.println("Utils.splitString uses a regular expression and the one given was invalid: " + delimiter);
            pse.printStackTrace();
            System.exit(1);
            return text;
        }
    }

    /**
     * This method is a quieter version of String.split and it also will return
     * the token at the given token (index).
     * @param text
     * @param delimiter
     * @param tokenIndex
     * @return
     */
    public static String splitString(String text, String delimiter, int tokenIndex)
    {
        try
        {
            return text.split(delimiter)[tokenIndex];
        } catch (java.lang.ArrayIndexOutOfBoundsException arrayE)
        {
            return "";
        } catch (java.util.regex.PatternSyntaxException pse)
        {
            System.err.println("Utils.splitString uses a regular expression and the one given was invalid: " + delimiter);
            pse.printStackTrace();
            System.exit(1);
            return text;
        }
    }

    public static String splitString(String text, String delimiter, String regexMatcher)
    {
        String[] entries = splitString(text, delimiter);
        for (String entry : entries)
        {
            if (entry.matches(regexMatcher))
            {
                return entry;
            }
        }

        return null;
    }

    public static String[] splitString(String text, String delimiter)
    {
        try
        {
            return text.split(delimiter);
        } catch (java.lang.ArrayIndexOutOfBoundsException arrayE)
        {
            return new String[]
                    {
                        text
                    };
        } catch (java.util.regex.PatternSyntaxException pse)
        {
            System.err.println("Utils.splitString uses a regular expression and the one given was invalid: " + delimiter);
            pse.printStackTrace();
            System.exit(1);
            return new String[]
                    {
                        text
                    };
        }
    }
    
    
    public static boolean isCharacterInParty(CachedEntity character)
    {
		boolean isInParty = true;
		if (character.getProperty("partyCode")==null || character.getProperty("partyCode").equals(""))
			isInParty = false;
		
		return isInParty;
    }
    public static boolean isDurabilityLow(CachedEntity item) {
    	if (item == null)
    		return false;
    	if (item.getProperty("durability") == null || item.getProperty("maxDurability") == null)
    		return false;
    	else
    	{
    		Long maxDura = (Long)item.getProperty("maxDurability");
    		Long currentDura = (Long)item.getProperty("durability");
    		if (currentDura < maxDura * .2)
    			return true;
    		else
    			return false;
    	}
    }
    public static boolean isDurabilityVeryLow(CachedEntity item) {
    	if (item == null)
    		return false;
    	if (item.getProperty("durability") == null || item.getProperty("maxDurability") == null)
    	return false;
    	else
    	{
    		Long maxDura = (Long)item.getProperty("maxDurability");
    		Long currentDura = (Long)item.getProperty("durability");
    		if (currentDura < maxDura * .1)
    			return true;
    		else
    			return false;
    	}
    }

	public static boolean isCharacterPartyLeader(CachedEntity character) {
		if ("TRUE".equals(character.getProperty("partyLeader")))
			return true;
		
		return false;
	}

    public static boolean isStorageItem(CachedEntity item)
    {
		if (item==null)
			return false;
		Long maxSpace = (Long)item.getProperty("maxSpace");
		if (maxSpace==null || maxSpace<=0)
			return false;
		Long maxWeight = (Long)item.getProperty("maxWeight");
		if (maxWeight==null || maxWeight<=0)
			return false;
		return true;
    }

	
	public static boolean enumEquals(Object value, Enum e)
	{
		if (value==null) return false;
		return value.equals(e.toString());
	}

	public static <E extends Enum<E>> boolean enumContains(Class<E> enumClass, String checkValue, boolean caseInsensitive)
	{
		for(E option:enumClass.getEnumConstants())
		{
			if(caseInsensitive ? option.name().equalsIgnoreCase(checkValue) : option.name().equals(checkValue))
				return true;
		}
		return false;
	}
	
	/**
	 * This equals can be used to compare anything, but specifically it's useful
	 * to compare key equality.
	 * @param value1
	 * @param value2
	 * @return
	 */
	public static boolean equals(Object value1, Object value2)
	{
		if (value1==value2)
			return true;
		
		if (value1 instanceof Key && value2 instanceof Key)
		{
			// This is a special case that is necessary for bulkWriteMode. In bulkWriteMode, 
			// the value1==value2 test above would have found equality appropriately, therefore 
			// we must conclude that they are not equal
			if (((Key)value1).isComplete()==false && ((Key)value2).isComplete()==false)
				return false;
			
			if (((Key)value1).getId() == ((Key)value2).getId() && ((Key)value1).getKind().equals(((Key)value2).getKind()))
				return true;
			else 
				return false;
		}
		if (value1!=null && value2!=null && value1.equals(value2))
			return true;
		
		return false;
	}
	
	public static double getWeaponMaxDamage(CachedEntity weapon)
    {
        String damageFormula = (String)weapon.getProperty("weaponDamage");
        Double critChance = null;
        if ((Long)weapon.getProperty("weaponDamageCriticalChance") instanceof Long)
        {
            Long chance = (Long)weapon.getProperty("weaponDamageCriticalChance");            
            critChance = chance.doubleValue()/100d;
        }
        Double critMultiplier = null;
        if (weapon.getProperty("weaponDamageCriticalMultiplier") instanceof Double)
            critMultiplier = (Double)weapon.getProperty("weaponDamageCriticalMultiplier");
        
        return GameUtils.getWeaponMaxDamage(damageFormula, critMultiplier, critChance);
    }
	
	/**
	 * 
	 * @param damageFormula
	 * @param critMultiplier
	 * @param critChance 1 = 100% chance.
	 * @return
	 */
	public static double getWeaponMaxDamage(String damageFormula, Double critMultiplier, Double critChance)
	{
		if (damageFormula==null || damageFormula.trim().equals("")) return 0d;
		if (critMultiplier==null) critMultiplier = 1d;
		if (critChance==null) critChance = 0d;
		
		String[] dmgParts = damageFormula.toString().substring(2).toLowerCase().split("d");
		double firstPart = Double.parseDouble(dmgParts[0]);
		double secondPart = Double.parseDouble(dmgParts[1]);

		
		return firstPart*secondPart*critMultiplier;
	}

	public static double getWeaponAverageDamage(CachedEntity weapon)
	{
		String damageFormula = (String)weapon.getProperty("weaponDamage");
		Double critChance = null;
		if ((Long)weapon.getProperty("weaponDamageCriticalChance") instanceof Long)
		{
			Long chance = (Long)weapon.getProperty("weaponDamageCriticalChance");			
			critChance = chance.doubleValue()/100d;
		}
		Double critMultiplier = null;
		if (weapon.getProperty("weaponDamageCriticalMultiplier") instanceof Double)
			critMultiplier = (Double)weapon.getProperty("weaponDamageCriticalMultiplier");
		
		return GameUtils.getWeaponAverageDamage(damageFormula, critMultiplier, critChance);
	}
	
	/**
	 * 
	 * @param damageFormula
	 * @param critMultiplier
	 * @param critChance 1 = 100% chance.
	 * @return
	 */
	public static double getWeaponAverageDamage(String damageFormula, Double critMultiplier, Double critChance)
	{
		if (damageFormula==null || damageFormula.trim().equals("")) return 0d;
		if (critMultiplier==null) critMultiplier = 1d;
		if (critChance==null) critChance = 0d;
		String[] dmgParts = damageFormula.toString().substring(2).toLowerCase().split("d");
		double firstPart = Double.parseDouble(dmgParts[0]);
		double secondPart = Double.parseDouble(dmgParts[1]);
		
		return firstPart*((secondPart-1d)/2d+1)*(1d+critChance*(critMultiplier-1d));
	}
	
	
//    public static double getWeaponAverageDamage(String damageFormula, Double critMultiplier, Double critChance)
//    {
//        if (damageFormula==null || damageFormula.trim().equals("")) return 0d;
//        if (critMultiplier==null) critMultiplier = 1d;
//        if (critChance==null) critChance = 1d;
//        String[] dmgParts = damageFormula.toString().substring(2).toLowerCase().split("d");
//        double firstPart = Double.parseDouble(dmgParts[0]);
//        double secondPart = Double.parseDouble(dmgParts[1]);
//       
//        //double weaponMaxDamage = firstPart*secondPart*critMultiplier*critChance;
//        //return (weaponMaxDamage-firstPart)/2+firstPart;
//       
//        double avgDamage=(((secondPart-1)/2)+1)*firstPart;
//        double totAvgDmg=avgDamage*(1-critChance)+avgDamage*critMultiplier*critChance;
//       
//        return totAvgDmg;
//    }	
	
	public static void setPopupMessage(HttpServletRequest request, String message)
	{
		request.setAttribute("message", message);
	}

	public static void setPopupError(HttpServletRequest request, String error)
	{
		request.setAttribute("error", error);
	}
	
	
	public static String formatDate_Long(Date joinDate)
	{
		if (joinDate==null)
			return "";
		
		return longDateFormat.format(joinDate);
	}
	
	
	public static String determineReferralUrl(CachedEntity user)
	{
		if (user==null)
			return null;
		
		String email = (String)user.getProperty("email");
		
		if (email!=null)
		{
			return "https://www.playinitium.com/login.jsp?game="+WebUtils.StringToEncryptedForUrl(email);
		}
		return null;
	}
	
	public static String getResourceUrl(Object relativeUrlObj)
	{
		if (relativeUrlObj==null) return null;
		String relativeUrl = (String)relativeUrlObj;
		
		if (relativeUrl.startsWith("http://"))
			relativeUrl = "https://"+relativeUrl.substring(7);
		
		if (relativeUrl.startsWith("https://"))
			return relativeUrl;
		else if (relativeUrl.startsWith("/"))
			return "https://initium-resources.appspot.com"+relativeUrl;
		else
			return "https://initium-resources.appspot.com/"+relativeUrl;
	}

}
