package automail;

import com.unimelb.swen30006.wifimodem.WifiModem;

import java.util.HashMap;

public class ChargeHandlerAdapterModem implements ChargeHandlerAdapter {
    private WifiModem wifiModem;
    private HashMap<Integer, Double> previousServiceFees;
    private static ChargeHandlerAdapter chargeHandlerAdapter;

    public ChargeHandlerAdapterModem(WifiModem wifiModem) {
        this.wifiModem = wifiModem;
        this.previousServiceFees = new HashMap<Integer, Double>();
    }

    /**
     * Gets instance of the ChargeHandler, according to the Singleton Pattern
     * @return ChargeHandler instance
     * @throws Exception
     */
    public static ChargeHandlerAdapter getInstance() throws Exception {
        if(chargeHandlerAdapter == null)
        {
            WifiModem wifiModem = WifiModem.getInstance(Building.getInstance().getMailroomLocationFloor());
            chargeHandlerAdapter = new ChargeHandlerAdapterModem(wifiModem);
        }
        return chargeHandlerAdapter;
    }

    /**
     * Performs lookup of service fee to given wifi modem
     * @param floor Floor number where service fee is required
     * @return Service fee for the given floor
     */
    public double calculateServiceFee(int floor) {
        double serviceFee = wifiModem.forwardCallToAPI_LookupPrice(floor);
        if (serviceFee == -1.0) {
            if (previousServiceFees.containsKey(floor)) {
                serviceFee = previousServiceFees.get(floor);
            }
            else {
                serviceFee = 0;
            }
        }
        if (previousServiceFees.get(floor) == null) {
            previousServiceFees.put(floor, serviceFee);
        }
        else {
            previousServiceFees.replace(floor, serviceFee);
        }
        return serviceFee;
    }

    /**
     * Calculates the maintenance fee for a robot type
     * @param avgOperatingTime Average lifetime operating time of all instances of a robot type
     * @param typeBasedRate The maintenance charge rate, specific to a robot type
     * @return Maintenance fee
     */
    public double calculateMaintenanceFee(double avgOperatingTime, double typeBasedRate) {
        return avgOperatingTime * typeBasedRate;
    }

    /**
     * Forms the string, appended to the end of a delivery, that summarises the charge information
     * @param floor Floor number where service fee is required
     * @param avgOperatingTime Average lifetime operating time of all instances of a robot type
     * @param typeBasedRate The maintenance charge rate, specific to a robot type
     * @return String that summarises charge information
     */
    public String formSummaryString(int floor, double avgOperatingTime, double typeBasedRate) {
        double serviceFee = calculateServiceFee(floor);
        double maintenanceFee = calculateMaintenanceFee(avgOperatingTime, typeBasedRate);
        double total = serviceFee + maintenanceFee;

        return String.format(" | Service Fee: %.2f | Maintenance: %.2f | Avg. Operating Time: %.2f | Total Charge: %.2f",
                serviceFee, maintenanceFee, avgOperatingTime, total);

    }


}
