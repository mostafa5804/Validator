public class TransTest {
    public static void main(String[] args) {
        String trimmed = "123456789012345678901234";
        String out = "";
        for (int i = 0; i < trimmed.length(); i++) {
            out += trimmed.charAt(i);
            if (i % 4 == 3 && i != 25) out += " ";
        }
        System.out.println("Len out: " + out.length());
        for (int offset = 0; offset <= trimmed.length(); offset++) {
            int trans = offset + (offset / 4);
            if (trans > out.length()) {
                System.out.println("CRASH originalToTransformed: offset " + offset + " mapped to " + trans + " but out len is " + out.length());
            }
        }
        for (int offset = 0; offset <= out.length(); offset++) {
            int orig = offset - (offset / 5);
            if (orig > trimmed.length()) {
                System.out.println("CRASH transformedToOriginal: offset " + offset + " mapped to " + orig + " but orig len is " + trimmed.length());
            }
        }
        System.out.println("Done.");
    }
}
