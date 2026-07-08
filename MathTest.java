public class MathTest {
    public static void main(String[] args) {
        // Known valid IBAN: IR020120000000000976194002
        // So the digits are: 02 0120000000000976194002
        String sanitized = "020120000000000976194002";
        String checkDigits = sanitized.substring(0, 2);
        String bban = sanitized.substring(2);
        String formattedShaba = bban + "1827" + checkDigits;
        
        long remainder = 0;
        for (int i = 0; i < formattedShaba.length(); i++) {
            int digit = Character.getNumericValue(formattedShaba.charAt(i));
            remainder = (remainder * 10 + digit) % 97;
        }
        System.out.println("Remainder for valid IBAN: " + remainder);
    }
}
