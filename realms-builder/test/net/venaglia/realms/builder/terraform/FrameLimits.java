package net.venaglia.realms.builder.terraform;

/**
 * User: ed
 * Date: 2/6/15
 * Time: 7:25 PM
 */
public class FrameLimits {

    public static void main(String[] args) {
        printFrameLimit(4322);
        printFrameLimit(60752);
        printFrameLimit(2700002);
    }

    private static void printFrameLimit(int count) {
        System.out.printf("FrameLimit @ %,d acres is %,d frames\n", count, Terraform.calculateFrameLimit(count));
    }
}
