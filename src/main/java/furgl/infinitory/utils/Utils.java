package furgl.infinitory.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Utils {

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