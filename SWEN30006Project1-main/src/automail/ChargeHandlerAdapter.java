package automail;

public interface ChargeHandlerAdapter {

    /**
     * Performs lookup of service fee to given wifi modem
     * @param floor Floor number where service fee is required
     * @return Service fee for the given floor
     */
    public double calculateServiceFee(int floor);

    /**
     * Calculates the maintenance fee for a robot type
     * @param avgOperatingTime Average lifetime operating time of all instances of a robot type
     * @param typeBasedRate The maintenance charge rate, specific to a robot type
     * @return Maintenance fee
     */
    public double calculateMaintenanceFee(double avgOperatingTime, double typeBasedRate);

    /**
     * Forms the string, appended to the end of a delivery, that summarises the charge information
     * @param floor Floor number where service fee is required
     * @param avgOperatingTime Average lifetime operating time of all instances of a robot type
     * @param typeBasedRate The maintenance charge rate, specific to a robot type
     * @return String that summarises charge information
     */
    public String formSummaryString(int floor, double avgOperatingTime, double typeBasedRate);

}
