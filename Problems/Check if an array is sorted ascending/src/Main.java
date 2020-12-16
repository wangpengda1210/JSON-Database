import java.util.Scanner;

class Main {
    public static void main(String[] args) {
        // put your code here
        Scanner scanner = new Scanner(System.in);

        int count = scanner.nextInt();
        int last = Integer.MIN_VALUE;

        for (int i = 0; i < count; i++) {
            int current = scanner.nextInt();
            if (current < last) {
                System.out.println(false);
                return;
            } else {
                last = current;
            }
        }

        System.out.println(true);
    }
}