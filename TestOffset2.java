public class TestOffset2 {
    public static void main(String[] args) {
        String trimmed = "1234";
        String out = "";
        for (int i = 0; i < trimmed.length(); i++) {
            out += trimmed.charAt(i);
            if (i % 4 == 3 && i != 25) out += " ";
        }
        System.out.println("out: '" + out + "' len: " + out.length());
        
        for (int i = 0; i <= trimmed.length(); i++) {
            int trans = i + (i / 4);
            System.out.println("orig " + i + " -> trans " + trans);
        }
        for (int i = 0; i <= out.length(); i++) {
            int orig = i - (i / 5);
            System.out.println("trans " + i + " -> orig " + orig);
        }
    }
}
