public class IBANTest {
    public static void main(String[] args) {
        // A valid Iran IBAN (from web): IR020120000000000976194002
        // So the digits are: 02 0120000000000976194002
        String sanitized = "020120000000000976194002";
        // Method 1: wrong algorithm in the code
        String formattedWrong = sanitized + "271800";
        int remainder1 = 0;
        for (int i = 0; i < formattedWrong.length(); i++) {
            remainder1 = (remainder1 * 10 + Character.getNumericValue(formattedWrong.charAt(i))) % 97;
        }
        System.out.println("Wrong algorithm mod: " + remainder1);

        // Method 2: correct algorithm
        String bban = sanitized.substring(2);
        String checkDigits = sanitized.substring(0, 2);
        String formattedRight = bban + "1827" + checkDigits;
        int remainder2 = 0;
        for (int i = 0; i < formattedRight.length(); i++) {
            remainder2 = (remainder2 * 10 + Character.getNumericValue(formattedRight.charAt(i))) % 97;
        }
        System.out.println("Correct algorithm mod: " + remainder2);
    }
}
