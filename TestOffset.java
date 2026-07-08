public class TestOffset {
    public static int originalToTransformed(int offset, int len) {
        int lastIndex = len - 1;
        int spaces = 0;
        for (int i = 0; i < offset; i++) {
            if (i % 4 == 3 && i != lastIndex) spaces++;
        }
        return offset + spaces;
    }
    public static int transformedToOriginal(int offset, String out) {
        int chars = 0;
        for (int i = 0; i < offset; i++) {
            if (i < out.length() && out.charAt(i) != ' ') chars++;
        }
        return chars;
    }
    public static void main(String[] args) {
        String trimmed = "12345";
        int lastIndex = trimmed.length() - 1;
        String out = "";
        for (int i = 0; i < trimmed.length(); i++) {
            out += trimmed.charAt(i);
            if (i % 4 == 3 && i != lastIndex) out += " ";
        }
        System.out.println("out: '" + out + "' len: " + out.length());
        
        for (int i = 0; i <= trimmed.length(); i++) {
            int trans = originalToTransformed(i, trimmed.length());
            System.out.println("orig " + i + " -> trans " + trans);
        }
        for (int i = 0; i <= out.length(); i++) {
            int orig = transformedToOriginal(i, out);
            System.out.println("trans " + i + " -> orig " + orig);
        }
    }
}
