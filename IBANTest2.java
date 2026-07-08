public class IBANTest2 {
    public static void main(String[] args) {
        String iban = "IR730140040000000000012345"; // fake but maybe valid? Wait.
        // Let's generate a valid check digit for IR XX 0140040000000000012345
        String bban = "0140040000000000012345";
        String formatted = bban + "182700";
        int remainder = 0;
        for (int i = 0; i < formatted.length(); i++) {
            remainder = (remainder * 10 + Character.getNumericValue(formatted.charAt(i))) % 97;
        }
        int check = 98 - remainder;
        System.out.println("Check digit should be: " + check);
    }
}
