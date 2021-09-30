import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: Petr Sváda
 * Date: 28.09.2021
 * Time: 22:00
 */
public class Delivery {
    public static void main(String[] args) throws Exception {
        Console c = System.console();
        if (c == null) {
            System.err.println("No console.");
            System.exit(1);
        }
        List<DeliveryPackage> deliveryPackageList = new ArrayList<DeliveryPackage>();

        String help = "Enter weight and postal code in this format: \n" +
                "<weight: positive number, >0, maximal 3 decimal places, . (dot) as decimal separator><space><postal code: fixed 5 digits>. \n"
                + "Type help to get help. \n" +
                "Type get output to get actual output. \n" +
                "Type quit to exit the program. \n" +
                "Program displays data every one minute. \n";
        System.out.println(help);
        boolean needSchedulePrintOutput = true;
        if (args.length > 0 && args[0] != null) {
            List<String> lines = getAllLinesFromFile(new File(args[0]));
            processFileInput(lines, c, help, deliveryPackageList);
            if (deliveryPackageList.size() > 0) {
                schedulePrintDeliveryPackgageList(deliveryPackageList);
                needSchedulePrintOutput = false;
            }
        }
        System.out.println("Weigth and postal code:");
        programProcess(c, help, deliveryPackageList, needSchedulePrintOutput);
    }

    private static void processFileInput(List<String> lines, Console c, String help, List<DeliveryPackage> deliveryPackageList) {
        for (int i = 0; i < lines.size(); i++) {
            processFileRowInput(i + 1, lines.get(i), help, deliveryPackageList);
        }
    }

    private static void processFileRowInput(int lineNumber, String data, String help, List<DeliveryPackage> deliveryPackageList) {
        String[] splitStr = data.split("\\s+");
        if (data.equals("quit")) {
            System.out.println("Program closing...");
            System.exit(1);
            return;
        }
        if (data.equals("help")) {
            System.out.println(help);
        } else if (data.equals("get output")) {
            printOutput(deliveryPackageList);
        } else if (isDataInputWithoutError(splitStr, lineNumber)) {
            addPackageToDeliveryPackageListAndSort((List<DeliveryPackage>) deliveryPackageList, splitStr);
        }
    }

    private static void schedulePrintDeliveryPackgageList(List<DeliveryPackage> deliveryPackageList) {
        Runnable printDeliveryPackageListRunnable = () ->  printDeliveryPackgageList(deliveryPackageList);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(printDeliveryPackageListRunnable, 0, 1, TimeUnit.MINUTES);
    }

    private static void addPackageToDeliveryPackageListAndSort(List<DeliveryPackage> deliveryPackageList, String[] splitStr) {
        float weight = Float.parseFloat(splitStr[0]);
        int postalCode = Integer.parseInt(splitStr[1]);
        DeliveryPackage deliveryPackageWithPostalCode = getDeliveryPackageWithPostalCode(deliveryPackageList, (DeliveryPackage dp) -> dp.getPostalCode() == postalCode);
        if (deliveryPackageWithPostalCode != null) {
            deliveryPackageWithPostalCode.setWeigth(deliveryPackageWithPostalCode.getWeigth() + weight);
        } else {
            deliveryPackageList.add(new DeliveryPackage(weight, postalCode));
        }
        sortDeliveryPackages(false, deliveryPackageList);
    }

    private static boolean isDataInputWithoutError(String[] splitStr, Integer lineNumber) {
        boolean hasError = false;
        StringBuilder errorMessage = new StringBuilder();
        if (splitStr.length != 2) {
            errorMessage.append("Incorrect format of weight and postal code");
            hasError = true;
        }
        if (!splitStr[0].matches("^\\d+(\\.\\d{0,3})?$")
                && errorMessage.toString().length() == 0) {
            errorMessage.append("Incorrect format of weight");
            hasError = true;
        }
        if ((splitStr.length <= 1 || splitStr[1] == null || splitStr[1].length() != 5
                || !splitStr[1].matches("\\d{5}"))
                && errorMessage.toString().length() == 0) {
            errorMessage.append("Incorrect format of postal code");
            hasError = true;
        }
        if (errorMessage.toString().length() > 0) {
            if (lineNumber != null) {
                errorMessage.append(" on line ").append(lineNumber).append(".");
            } else {
                errorMessage.append(".");
            }
            System.out.println(errorMessage);
        }
        return !hasError;
    }

    private static void processDataInput(String data, Console c, String help, List<DeliveryPackage> deliveryPackageList, boolean needSchedulePrintOutput) {
        String[] splitStr = data.split("\\s+");
        if (data.equals("quit")) {
            System.out.println("Program closing...");
            System.exit(1);
            return;
        }
        if (data.equals("help")) {
            System.out.println(help);
        } else if (data.equals("get output")) {
            printOutput(deliveryPackageList);
        } else if (isDataInputWithoutError(splitStr, null)) {
            addPackageToDeliveryPackageListAndSort((List<DeliveryPackage>) deliveryPackageList, splitStr);
            System.out.println("Data saved...");

            if (needSchedulePrintOutput) {
                Runnable printDeliveryPackageListRunnable = () ->  printDeliveryPackgageList(deliveryPackageList);
                ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                executor.scheduleAtFixedRate(printDeliveryPackageListRunnable, 1, 1, TimeUnit.MINUTES);
                needSchedulePrintOutput = false;
            }
        }
        System.out.println("Weigth and postal code:");
        programProcess(c, help, deliveryPackageList, needSchedulePrintOutput);
    }

    private static void programProcess(Console c, String help, List<DeliveryPackage> deliveryPackageList, boolean needSchedulePrintOutput) {
        String data = "";
        data = c.readLine();
        processDataInput(data, c, help, deliveryPackageList, needSchedulePrintOutput);
    }

    private static DeliveryPackage getDeliveryPackageWithPostalCode(
            List<DeliveryPackage> deliveryPackageList, CheckDeliveryPackage tester) {
        for (DeliveryPackage p : deliveryPackageList) {
            if (tester.test(p)) {
                return p;
            }
        }
        return null;
    }

    interface CheckDeliveryPackage {
        boolean test(DeliveryPackage p);
    }

    private static void printDeliveryPackgageList(List<DeliveryPackage> deliveryPackages) {
        System.out.print("\n");
        System.out.println("Output:");
        for (DeliveryPackage deliveryPackgage : deliveryPackages) {
            System.out.println(deliveryPackgage.getPostalCode() + " " + deliveryPackgage.getWeigth());
        }
        System.out.println("Weigth and postal code:");
    }

    private static void printOutput(List<DeliveryPackage> deliveryPackages) {
        System.out.println("Output:");
        for (DeliveryPackage deliveryPackgage : deliveryPackages) {
            System.out.println(deliveryPackgage.getPostalCode() + " " + deliveryPackgage.getWeigth());
        }
    }

    private static class DeliveryPackage {
        private float weigth = 0F;
        private int postalCode;

        public DeliveryPackage(float weigth, int postalCode) {
            this.weigth = weigth;
            this.postalCode = postalCode;
        }

        public float getWeigth() {
            return weigth;
        }

        public void setWeigth(float weigth) {
            this.weigth = weigth;
        }

        public int getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(int postalCode) {
            this.postalCode = postalCode;
        }
    }

    private static void sortDeliveryPackages(boolean sortAsc, List<? extends DeliveryPackage> deliveryPackages) {
        final Comparator<DeliveryPackage> comparator;
        comparator = deliveryPackageComparator();
        deliveryPackages.sort(comparator);
        if (!sortAsc) Collections.reverse(deliveryPackages);
    }

    private static Comparator<DeliveryPackage> deliveryPackageComparator() {
        return new Comparator<DeliveryPackage>() {
            public int compare(DeliveryPackage o1, DeliveryPackage o2) {
                return Float.compare(o1.getWeigth(), o2.getWeigth());
            }
        };
    }

    private static List<String> getAllLinesFromFile(File file) throws Exception {
        List<String> lines = new ArrayList<String>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
                // process the line.
            }
        }
        return lines;
    }
}
