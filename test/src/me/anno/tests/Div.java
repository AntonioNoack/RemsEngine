package me.anno.tests;

public class Div {
    public static void main(String[] args) {
        /*long biggest = 0;
        for (long x = 1; x <= 1_000_000_000L; x++) {
            long count = countTeiler(x);
            if (count > biggest) {
                biggest = count;
                System.out.println(x + " hat " + count + " Teiler");
            }
        }*/
        int numThreads = 12;
        final long[] biggest = {0};
        long[] position = new long[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int j = i;
            new Thread(() -> {
                long x = j;
                for (; x <= 1_000_000_000L; x += numThreads) {
                    position[j] = x;
                    long count = countTeiler(x);
                    if (count > biggest[0]) {
                        while (true) {
                            boolean ok = true;
                            for (int k = 0; k < numThreads; k++) {
                                if(position[k] < position[j]){
                                    ok = false;
                                    break;
                                }
                            }
                            if(ok) break;
                            else {
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        if (count > biggest[0]){
                            biggest[0] = count;
                            System.out.println(x + " hat " + count + " Teiler");
                        }
                    }
                }
                position[j] = x;
                System.out.println("Done");
            }).start();
        }
    }

    public static long countTeiler(long x) {
        long numTeiler = 1;
        for (long i = 2; i * i < x; i++) {
            int count = 1;
            while (x % i == 0) {
                x /= i;
                count++;
            }
            numTeiler *= count;
        }
        if (x != 1) numTeiler *= 2;
        return numTeiler;
    }
}
