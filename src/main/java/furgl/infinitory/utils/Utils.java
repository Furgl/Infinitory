package furgl.infinitory.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Utils {
	
	/**Additional slots - must be multiple of 9*/
	public static final int ADDITIONAL_SLOTS = 18;

	/** Gets String from double without trailing zeroes */ 
	public static String formatDouble(double num, int decimalPlaces) {
		BigDecimal decimal = new BigDecimal(num);
		decimal = decimal.setScale(decimalPlaces, RoundingMode.HALF_EVEN);
		if (decimal.doubleValue() == decimal.intValue())
			return String.valueOf(decimal.intValue());
		else
			return String.valueOf(decimal.doubleValue());
	}
	
}