package tw.nekomimi.nekogram.shamsicalendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import tw.nekomimi.nekogram.NekoConfig;

public class PersianDate {

  /*----- Define Variable ---*/
  private Long timeInMilliSecond;
  public static final int FARVARDIN = 1;
  public static final int ORDIBEHEST = 2;
  public static final int KHORDAD = 3;
  public static final int TIR = 4;
  public static final int MORDAD = 5;
  public static final int SHAHRIVAR = 6;
  public static final int MEHR = 7;
  public static final int ABAN = 8;
  public static final int AZAR = 9;
  public static final int DAY = 10;
  public static final int BAHMAN = 11;
  public static final int ESFAND = 12;
  public static final int AM = 1;
  public static final int PM = 2;
  public static final String AM_SHORT_NAME = "ق.ظ";
  public static final String PM_SHORT_NAME = "ب.ظ";
  public static final String AM_NAME = "قبل از ظهر";
  public static final String PM_NAME = "بعد از ظهر";
  private int shYear;
  private int shMonth;
  private int shDay;
  private int grgYear;
  private int grgMonth;
  private int grgDay;
  private int hour;
  private int minute;
  private int second;

  enum Dialect {
    AFGHAN,
    IRANIAN,
    KURDISH,
    PASHTO,
    LATIN
  }

  /**
   * Contractor
   */
  public PersianDate() {
    this.timeInMilliSecond = new Date().getTime();
    this.changeTime();
  }
  /**
   * Contractor
   */
  public PersianDate(Long timeInMilliSecond) {
    this.timeInMilliSecond = timeInMilliSecond;
    this.changeTime();
  }

  /**
   * Contractor
   */
  public PersianDate(Date date) {
    this.timeInMilliSecond = date.getTime();
    this.changeTime();
  }

  public static boolean displayPersianCalendarByLatin = NekoConfig.displayPersianCalendarByLatin.Bool();
  
  private final String[] dayNames = {"شنبه", "یک‌شنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه",
      "جمعه"};
  private final String[] latindayNames = {"Shanbeh", "YekShanbeh", "DoShanbeh", "SeShanbeh", "CheharShanbeh", "PanjShanbeh",
      "Jomeh"};
  private final String[] monthNames = {"فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
      "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"};
  private final String[] monthNamesLatin = {"Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar",
      "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand"};
  private final String[] AfghanMonthNames = {"حمل", "ثور", "جوزا", "سرطان", "اسد", "سنبله", "میزان",
      "عقرب", "قوس", "جدی", "دلو", "حوت"};
  private final String[] KurdishMonthNames = {"جیژنان", "گولان", "زه ردان", "په رپه ر", "گه لاویژ",
      "نوخشان", "به ران", "خه زان", "ساران", "بفران", "به ندان", "رمشان"};
  private final String[] PashtoMonthNames = {"وری", "غويی", "غبرګولی", "چنګاښ", "زمری", "وږی",
      "تله", "لړم", "ليندۍ", "مرغومی", "سلواغه", "كب"};
  private String delimiter = "/";

  /*---- Setter And getter ----*/
  public int getShYear() {
    return shYear;
  }

  public PersianDate setShYear(int shYear) {
    this.shYear = shYear;
    this.prepareDate2(this.getShYear(), this.getShMonth(), this.getShDay());
    return this;
  }

  public int getShMonth() {
    return shMonth;
  }

  public PersianDate setShMonth(int shMonth) {
    this.shMonth = shMonth;
    this.prepareDate2(this.getShYear(), this.getShMonth(), this.getShDay());
    return this;
  }

  public int getShDay() {
    return shDay;
  }

  public PersianDate setShDay(int shDay) {
    this.shDay = shDay;
    this.prepareDate2(this.getShYear(), this.getShMonth(), this.getShDay());
    return this;
  }

  public int getGrgYear() {
    return grgYear;
  }

  public PersianDate setGrgYear(int grgYear) {
    this.grgYear = grgYear;
    prepareDate();
    return this;
  }

  public int getGrgMonth() {
    return grgMonth;
  }

  public PersianDate setGrgMonth(int grgMonth) {
    this.grgMonth = grgMonth;
    prepareDate();
    return this;
  }

  public int getGrgDay() {
    return grgDay;
  }

  public PersianDate setGrgDay(int grgDay) {
    this.grgDay = grgDay;
    prepareDate();
    return this;
  }

  public int getHour() {
    return hour;
  }

  public PersianDate setHour(int hour) {
    this.hour = hour;
    prepareDate();
    return this;
  }

  public int getMinute() {
    return minute;
  }

  public PersianDate setMinute(int minute) {
    this.minute = minute;
    prepareDate();
    return this;
  }

  public int getSecond() {
    return second;
  }

  public PersianDate setSecond(int second) {
    this.second = second;
    prepareDate();
    return this;
  }

  public String getDelimiter() {
    return this.delimiter;
}

  public void setDelimiter(String delimiter) {
      this.delimiter = delimiter;
  }

  private String formatToMilitary(int i) {
    return i < 9 ? "0" + i : String.valueOf(i);
  }

  public String getPersianShortDate() {
    if (displayPersianCalendarByLatin) {
        return "" + formatToMilitary(this.getShYear()) + this.delimiter + formatToMilitary(getShMonth() + 1) + this.delimiter + formatToMilitary(this.getShDay());
    } else {
        return LanguageUtils.getPersianNumbers("" + formatToMilitary(this.getShYear()) + this.delimiter + formatToMilitary(this.getShMonth() + 1) + this.delimiter + formatToMilitary(this.getShDay()));
    }
  }

  public String getPersianNormalDate() {
    if (displayPersianCalendarByLatin) {
      return this.getShDay() + " " + this.monthNamesLatin() + " " + this.getShYear();
    } else {
      return LanguageUtils.getPersianNumbers(String.valueOf(this.getShDay())) + " " +  this.monthName() + " " + LanguageUtils.getPersianNumbers(String.valueOf(this.getShYear()));
    }
  }

  //like 9 شهریور
  public String getPersianMonthDay() {
    if (displayPersianCalendarByLatin) {
      return this.getShDay() + " " + this.monthNamesLatin();
    } else {
      return LanguageUtils.getPersianNumbers(String.valueOf(this.getShDay())) + " " + this.monthName();
    }
  }
  /**
   * init without time
   *
   * @param year Year in Grg
   * @param month Month in Grg
   * @param day Day in Grg
   * @return persianDate
   */
  public PersianDate initGrgDate(int year, int month, int day) {
    return this.initGrgDate(year, month, day, 0, 0, 0);
  }

  /**
   * init with Grg data
   *
   * @param year Year in Grg
   * @param month Month in Grg
   * @param day day in Grg
   * @param hour hour
   * @param minute min
   * @param second second
   * @return PersianDate
   */
  public PersianDate initGrgDate(int year, int month, int day, int hour, int minute, int second) {
    this.grgYear = year;
    this.grgMonth = month;
    this.grgDay = day;
    this.hour = hour;
    this.minute = minute;
    this.second = second;
    this.setGrgYear(year)
        .setGrgMonth(month)
        .setGrgDay(day)
        .setHour(hour)
        .setMinute(minute)
        .setSecond(second);
    JalaliDate jalaliDate = JalaliCalendarUtil.toJalali(year, month, day);
    this.shYear = jalaliDate.getYear();
    this.shMonth = jalaliDate.getMonth();
    this.shDay = jalaliDate.getDay();
    this.setShYear(jalaliDate.getYear())
        .setShMonth(jalaliDate.getMonth())
        .setShDay(jalaliDate.getDay());
    return this;
  }

  /**
   * initialize date from Jallali date
   *
   * @param year Year in Jallali date
   * @param month Month in Jallali date
   * @param day day in Jallali date
   * @return PersianDate
   */
  public PersianDate initJalaliDate(int year, int month, int day) {
    return this.initJalaliDate(year, month, day, 0, 0, 0);
  }

  /**
   * initialize date from Jallali date
   *
   * @param year Year in jallali date
   * @param month Month in Jallali date
   * @param day day in Jallali date
   * @param hour Hour
   * @param minute Minute
   * @param second Second
   * @return PersianDate
   */
  public PersianDate initJalaliDate(int year, int month, int day, int hour, int minute,
      int second) {
    this.setShYear(year)
        .setShMonth(month)
        .setShDay(day)
        .setHour(hour)
        .setMinute(minute)
        .setSecond(second);
    GregorianDate gregorianDate = JalaliCalendarUtil.toGregorian(year, month, day);
    this.setGrgYear(gregorianDate.getYear())
        .setGrgMonth(gregorianDate.getMonth())
        .setGrgDay(gregorianDate.getDay());
    return this;
  }

  /**
   * Helper function for initialize jalali date
   *
   * @param year Year
   * @param month Month
   * @param day Day
   * @return PersianDate
   */
  private PersianDate prepareDate2(int year, int month, int day) {
    GregorianDate gregorianDate = JalaliCalendarUtil.toGregorian(year, month, day);
    this.grgYear = gregorianDate.getYear();
    this.grgMonth = gregorianDate.getMonth();
    this.setGrgDay(gregorianDate.getDay());
    return this;
  }

  /**
   * Helper function for initialize
   */
  private void prepareDate() {
    String dtStart = "" + this.textNumberFilter("" + this.getGrgYear()) + "-" + this
        .textNumberFilter("" + this.getGrgMonth()) + "-" + this
        .textNumberFilter("" + this.getGrgDay())
        + "T" + this.textNumberFilter("" + this.getHour()) + ":" + this
        .textNumberFilter("" + this.getMinute()) + ":" + this
        .textNumberFilter("" + this.getSecond()) + "Z";
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    JalaliDate jalaliDate = JalaliCalendarUtil.toJalali(this.getGrgYear(), this.getGrgMonth(), this.getGrgDay());
    this.shYear = jalaliDate.getYear();
    this.shMonth = jalaliDate.getMonth();
    this.shDay = jalaliDate.getDay();
    Date date = null;
    try {
      date = format.parse(dtStart);
      this.timeInMilliSecond = date.getTime();
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  /**
   * return time in long value
   *
   * @return Value of time in mile
   */
  public Long getTime() {
    return this.timeInMilliSecond;
  }

  /**
   * Check year in Leap
   *
   * @return true or false
   */
  public boolean isLeap() {
    return JalaliCalendarUtil.isJalaliLeap(this.shYear);
  }

  /**
   * Check custom year is leap
   *
   * @param year int year
   * @return true or false
   */
  public boolean isLeap(int year) {
    return JalaliCalendarUtil.isJalaliLeap(year);
  }

  /**
   * Check static is leap year for Jalali Date
   *
   * @param year Jalali year
   * @return true if year is leap
   */
  public static boolean isJalaliLeap(int year) {
    return JalaliCalendarUtil.isJalaliLeap(year);
  }

  /**
   * calc day of week
   *
   * @return int
   */
  public int dayOfWeek() {
    return this.dayOfWeek(this);
  }

  /**
   * Get day of week from PersianDate object
   *
   * @param date persianDate
   * @return int
   */
  public int dayOfWeek(PersianDate date) {
    return this.dayOfWeek(date.toDate());
  }

  /**
   * Get day of week from Date object
   *
   * @param date Date
   * @return int
   */
  public int dayOfWeek(Date date) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			return 0;
		}
    return (cal.get(Calendar.DAY_OF_WEEK));
  }

	/**
	 * Return list of month
	 * @param dialect dialect
	 * @return month names
	 */
	public String[] monthList(Dialect dialect) {
  	switch (dialect){
			case AFGHAN:
				return this.AfghanMonthNames;
			case KURDISH:
				return this.KurdishMonthNames;
			case PASHTO:
				return this.PashtoMonthNames;
      			case LATIN:
        			return this.monthNamesLatin;
			default:
				return this.monthNames;
		}
	}
	/**
	 * Return list of month
	 *
	 * @return month names
	 */
	public String[] monthList() {
  	return monthList(Dialect.IRANIAN);
	}
		/**
     * return month name
     *
     * @return string
     */
  public String monthName(Dialect dialect) {
    return monthName(this.getShMonth(),dialect);
  }

  /**
   * Return month name
   *
   * @param month Month
   */
  public String monthName(int month, Dialect dialect) {
    switch (dialect) {
      case AFGHAN:
        return this.AfghanMonthNames[month - 1];
      case KURDISH:
        return this.KurdishMonthNames[month - 1];
      case PASHTO:
        return this.PashtoMonthNames[month - 1];
      case LATIN:
        return this.monthNamesLatin[month - 1];
      default:
        return this.monthNames[month - 1];
    }
  }

  /**
   * Get current month name in Persian
   */
  public String monthName() {
    return monthName(Dialect.IRANIAN);
  }

  /**
   * Get month name in Afghan
   */
  public String AfghanMonthName(int month) {
    return this.AfghanMonthNames[month - 1];
  }

  /**
   * Get current date Afghan month name
   */
  public String AfghanMonthName() {
    return this.AfghanMonthName(this.getShMonth());
  }

  /**
   * Get month name in Kurdish
   */
  public String KurdishMonthName(int month) {
    return this.KurdishMonthNames[month - 1];
  }

  /**
   * Get current date Kurdish month name
   */
  public String KurdishMonthName() {
    return this.KurdishMonthName(this.getShMonth());
  }

  /**
   * Get month name in Pashto
   */
  public String PashtoMonthName(int month) {
    return this.PashtoMonthNames[month - 1];
  }

  /**
   * Get current date Pashto month name
   */
  public String PashtoMonthName() {
    return this.PashtoMonthName(this.getShMonth());
  }
  

    /**
   * Get month name in monthNamesLatin
   */
  public String monthNamesLatin(int month) {
    return this.monthNamesLatin[month - 1];
  }

  /**
   * Get current date monthNamesLatin month name
   */
  public String monthNamesLatin() {
    return this.monthNamesLatin(this.getShMonth());
  }
    

  /**
   * get day name
   */
  public String dayName() {
    return this.dayName(this);
  }

  /**
   * Get Day Name
   */
  public String dayName(PersianDate date) {
    return this.dayNames[this.dayOfWeek(date)];
  }
  
  public String latindayName(PersianDate date) {
    return this.latindayNames[this.dayOfWeek(date)];
  }

  /**
   * Number days of month
   *
   * @return return days
   */
  public int getMonthDays() {
    return this.getMonthDays(this.getShYear(), this.getShMonth());
  }

  /**
   * calc count of day in month
   */
  public int getMonthDays(int Year, int month) {
    if (month == 12 && !isLeap(Year)) {
      return 29;
    }
    if (month <= 6) {
      return 31;
    } else {
      return 30;
    }
  }

  /**
   * calculate day in year
   */
  public int getDayInYear() {
    return this.getDayInYear(this.getShMonth(), getShDay());
  }

  /**
   * Calc day of the year
   *
   * @param month Month
   * @param day Day
   */
  public int getDayInYear(int month, int day) {
    for (int i = 1; i < month; i++) {
      if (i <= 6) {
        day += 31;
      } else {
        day += 30;
      }
    }
    return day;
  }

  /**
   * add date
   *
   * @param year Number of Year you want add
   * @param month Number of month you want add
   * @param day Number of day you want add
   * @param hour Number of hour you want add
   * @param minute Number of minute you want add
   * @param second Number of second you want add
   * @return new date
   */
  public PersianDate addDate(long year, long month, long day, long hour, long minute, long second) {
    if (month >= 12) {
      year += Math.round(month / 12);
      month = month % 12;
    }
    for (long i = (year - 1); i >= 0; i--) {
      if (JalaliCalendarUtil.isJalaliLeap(this.getShYear() + (int) i)) {
        day += 366;
      } else {
        day += 365;
      }
    }
    for (long i = (month - 1); i >= 0; i--) {
      int monthTmp = this.getShMonth() + (int) i;
      int yearTmp = this.getShYear();
      if (monthTmp > 12) {
        monthTmp -= 12;
        yearTmp++;
      }
      day += this.getMonthLength(yearTmp, monthTmp);
    }
    this.timeInMilliSecond += (day * 24 * 3_600 * 1_000);
    this.timeInMilliSecond += ((second + (hour * 3600) + (minute * 60)) * 1_000);
    this.changeTime();
    return this;
  }

  /**
   * add to date
   *
   * @param year Number of Year you want add
   * @param month Number of month you want add
   * @param day Number of day you want add
   */
  public PersianDate addDate(long year, long month, long day) {
    return this.addDate(year, month, day, 0, 0, 0);
  }

  public PersianDate addYear(long year) {
    return this.addDate(year, 0, 0L, 0, 0, 0);
  }

  public PersianDate addMonth(long month) {
    return this.addDate(0, month, 0L, 0, 0, 0);
  }

  public PersianDate addWeek(long week) {
    return this.addDate(0, 0, (week * 7), 0, 0, 0);
  }

  public PersianDate addDay(long day) {
    return this.addDate(0, 0, day, 0, 0, 0);
  }

  /**
   * Compare 2 date
   *
   * @param dateInput PersianDate type
   */
  public Boolean after(PersianDate dateInput) {
    return (this.timeInMilliSecond < dateInput.getTime());
  }

  /**
   * compare to data
   *
   * @param dateInput Input
   */
  public Boolean before(PersianDate dateInput) {
    return (!this.after(dateInput));
  }

  /**
   * Check date equals
   */
  public Boolean equals(PersianDate dateInput) {
    return (this.timeInMilliSecond.equals(dateInput.getTime()));
  }

  /**
   * compare two data
   *
   * @return 0 = equal,1=data1 > anotherDate,-1=data1 > anotherDate
   */
  public int compareTo(PersianDate anotherDate) {
    return (this.timeInMilliSecond.compareTo(anotherDate.getTime()));
  }

  /**
   * Return Day in different date
   */
  public long getDayUntilToday() {
    return this.getDayUntilToday(new PersianDate());
  }

  /**
   * Return different just day in compare 2 date
   *
   * @param date date for compare
   */
  public long getDayUntilToday(PersianDate date) {
    long[] ret = this.untilToday(date);
    return ret[0];
  }

  /**
   * Calc different date until now
   */
  public long[] untilToday() {
    return this.untilToday(new PersianDate());
  }

  /**
   * calculate different between 2 date
   *
   * @param date Date 1
   */
  public long[] untilToday(PersianDate date) {
    long secondsInMilli = 1000;
    long minutesInMilli = secondsInMilli * 60;
    long hoursInMilli = minutesInMilli * 60;
    long daysInMilli = hoursInMilli * 24;
    long different = Math.abs(this.timeInMilliSecond - date.getTime());

    long elapsedDays = different / daysInMilli;
    different = different % daysInMilli;

    long elapsedHours = different / hoursInMilli;
    different = different % hoursInMilli;
    long elapsedMinutes = different / minutesInMilli;
    different = different % minutesInMilli;
    long elapsedSeconds = different / secondsInMilli;
    return new long[]{elapsedDays, elapsedHours, elapsedMinutes, elapsedSeconds};
  }

	@Override
  public String toString() {
    return PersianDateFormat.format(this, null);
  }
  /*----- Helper Function-----*/

  /**
   * convert PersianDate class to date
   */
  public Date toDate() {
    return new Date(this.timeInMilliSecond);
  }

  /**
   * Helper function
   */
  private String textNumberFilter(String date) {
    if (date.length() < 2) {
      return "0" + date;
    }
    return date;
  }

  /**
   * initialize with time in millisecond
   */
  private void changeTime() {
    this.initGrgDate(Integer.parseInt(new SimpleDateFormat("yyyy").format(this.timeInMilliSecond)),
        Integer.parseInt(new SimpleDateFormat("MM").format(this.timeInMilliSecond)),
        Integer.parseInt(new SimpleDateFormat("dd").format(this.timeInMilliSecond)),
        Integer.parseInt(new SimpleDateFormat("HH").format(this.timeInMilliSecond)),
        Integer.parseInt(new SimpleDateFormat("mm").format(this.timeInMilliSecond)),
        Integer.parseInt(new SimpleDateFormat("ss").format(this.timeInMilliSecond)));
  }

  /**
   * Return today
   */
  public static PersianDate today() {
    PersianDate persianDate = new PersianDate();
		persianDate.setHour(0).setMinute(0).setSecond(0);
    return persianDate;
  }

  /**
   * Get tomorrow
   */
  public static PersianDate tomorrow() {
    PersianDate persianDate = new PersianDate();
		persianDate.addDay(1);
		persianDate.setHour(0).setMinute(0).setSecond(0);
    return persianDate;
  }

  /**
   * Get start of day
   */
  public PersianDate startOfDay(PersianDate persianDate) {
    persianDate.setHour(0).setMinute(0).setSecond(0);
    return persianDate;
  }

  /**
   * Get Start of day
   */
  public PersianDate startOfDay() {
    return this.startOfDay(this);
  }

  /**
   * Get end of day
   */
  public PersianDate endOfDay(PersianDate persianDate) {
    persianDate.setHour(23).setMinute(59).setSecond(59);
    return persianDate;
  }

  /**
   * Get end of day
   */
  public PersianDate endOfDay() {
    return this.endOfDay(this);
  }

  /**
   * Check midnight
   */
  public Boolean isMidNight(PersianDate persianDate) {
    return persianDate.isMidNight();
  }

  /**
   * Check is midNight
   */
  public Boolean isMidNight() {
    return (this.hour < 12);
  }

  /**
   * Get short name time of the day
   */
  public String getShortTimeOfTheDay() {
    return (this.isMidNight()) ? AM_SHORT_NAME : PM_SHORT_NAME;
  }

  /**
   * Get short name time of the day
   */
  public String getShortTimeOfTheDay(PersianDate persianDate) {
    return (persianDate.isMidNight()) ? AM_SHORT_NAME : PM_SHORT_NAME;
  }

  /**
   * Get time of the day
   */
  public String getTimeOfTheDay() {
    return (this.isMidNight()) ? AM_NAME : PM_NAME;
  }

  /**
   * Get time of the day
   */
  public String getTimeOfTheDay(PersianDate persianDate) {
    return (persianDate.isMidNight()) ? AM_NAME : PM_NAME;
  }

  /**
   * Get number of days in month
   *
   * @param year Jalali year
   * @param month Jalali month
   * @return number of days in month
   */
  public Integer getMonthLength(Integer year, Integer month) {
    if (month <= 6) {
      return 31;
    } else if (month <= 11) {
      return 30;
    } else {
      if (JalaliCalendarUtil.isJalaliLeap(year)) {
        return 30;
      } else {
        return 29;
      }
    }
  }

  public Integer getMonthLengthPicker(Integer year, Integer month) {
    if (month <= 5) {
      return 31;
    } else if (month <= 10) {
      return 30;
    } else {
      if (JalaliCalendarUtil.isJalaliLeap(year)) {
        return 30;
      } else {
        return 29;
      }
    }
  }

  /**
   * Get number of days in month
   *
   * @param persianDate persianDate object
   * @return number of days in month
   */
  public Integer getMonthLength(PersianDate persianDate) {
    return this.getMonthLength(persianDate.getShYear(), persianDate.getShMonth());
  }

  /**
   * Get number of days in month
   *
   * @return number of days in month
   */
  public Integer getMonthLength() {
    return this.getMonthLength(this);
  }
}
