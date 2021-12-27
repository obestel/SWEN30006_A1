package automail;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import simulation.Clock;
import simulation.IMailDelivery;

public class RegularRobot extends Robot {
    private static final String ROBOT_TYPE = "R";
    private static final double TYPE_BASED_RATE = 0.025;
    private static final int MOVE_SPEED = 1;
    private static final int TUBE_SIZE = 1;
    private static int numBots = 0;
    private static int totalTimeOperated = 0;
    private static ChargeHandlerAdapter chargeHandlerAdapter;



    public RegularRobot(IMailDelivery delivery, MailPool mailPool, int number) throws Exception {
        super(delivery, mailPool, number, ROBOT_TYPE, TUBE_SIZE);
        incrementRobotCount();
        chargeHandlerAdapter = ChargeHandlerAdapterModem.getInstance();
    }

    /**
     * The main operational function of the robot
     * @throws ExcessiveDeliveryException
     * @throws Exception
     */
    public void operate() throws ExcessiveDeliveryException, Exception {
        if (getCurrentState() != RobotState.WAITING) {
            incrementTimeOperated();
        }
        switch(getCurrentState()) {
            /** This state is triggered when the robot is returning to the mailroom after a delivery */
            case RETURNING:
                /** If its current position is at the mailroom, then the robot should change state */
                if(getCurrentFloor() == Building.getInstance().getMailroomLocationFloor()){
                    /** Tell the sorter the robot is ready */
                    getMailPool().registerWaiting(this);
                    changeState(RobotState.WAITING);
                } else {
                    /** If the robot is not at the mailroom floor yet, then move towards it! */
                    moveTowards(Building.getInstance().getMailroomLocationFloor(), MOVE_SPEED);
                    break;
                }
            case WAITING:
                /** If the StorageTube is ready and the Robot is waiting in the mailroom then start the delivery */
                if(!isEmpty() && getReceivedDispatch()){
                    completedDispatch();
                    resetDeliveryCounter(); // reset delivery counter
                    setDestination();
                    changeState(RobotState.DELIVERING);
                }
                break;
            case DELIVERING:
                if(getCurrentFloor() == getDestinationFloor()){ // If already here drop off either way
                    /** Delivery complete, report this to the simulator! */
                    String summary = "";
                    if (feeCharging) {
                        summary = chargeHandlerAdapter.formSummaryString(getDeliveryItem().destination_floor, calcAvgOperatingTime(),TYPE_BASED_RATE);

                    }
                    getDelivery().deliver(this, getDeliveryItem(), summary);
                    setDeliveryItem(null);
                    tickDeliveryCounter();
                    if(getDeliveryCounter() > 2){  // Implies a simulation bug
                        throw new ExcessiveDeliveryException();
                    }
                    /** Check if want to return, i.e. if there is no item in the tube*/
                    if(getCurrentTubeLength() == 0){
                        changeState(RobotState.RETURNING);
                    }
                    else{
                        /** If there is another item, set the robot's route to the location to deliver the item */
                        setDeliveryItem(popFromTube());
                        //setCurrentTubeLength(0);
                        setDestination();
                        changeState(RobotState.DELIVERING);
                    }
                } else {
                    /** The robot is not at the destination yet, move towards it! */
                    moveTowards(getDestinationFloor(), MOVE_SPEED);
                }
                break;
        }
    }

    /**
     * Prints out the change in state
     * @param nextState the state to which the robot is transitioning
     */
    protected void changeState(RobotState nextState){
        assert(!(getDeliveryItem() == null && getCurrentTubeLength() != 0));
        if (getCurrentState() != nextState) {
            System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdTube(), getCurrentState(), nextState);
        }
        setCurrentState(nextState);
        if(nextState == RobotState.DELIVERING){
            System.out.printf("T: %3d > %7s-> [%s]%n", Clock.Time(), getIdTube(), getDeliveryItem().toString());
        }
    }

    public void setDestination() {
        setDestinationFloor(getDeliveryItem().destination_floor);
    }

    public boolean isEmpty() {
        return (getDeliveryItem() == null && getTube() == null);
    }

    public void addToHand(MailItem mailItem) throws ItemTooHeavyException {
        assert(getDeliveryItem() == null);
        setDeliveryItem(mailItem);
        if (getDeliveryItem().weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
    }

    public void loadItem(MailItem mailItem) throws ItemTooHeavyException{
        if (getDeliveryItem() ==  null) {
            addToHand(mailItem);
        }
        else {
            if (getCurrentTubeLength() == 0) {
                addToTube(mailItem);
            }
        }
    }

    /**
     *
     * @return
     */
    public boolean isFull() {
        return (getDeliveryItem() != null && getCurrentTubeLength() != 0);
    }

    public static void incrementRobotCount() {
        numBots++;
    }

    public static void incrementTimeOperated() {
        totalTimeOperated++;
    }

    public static double calcAvgOperatingTime() {
        return totalTimeOperated/(double) numBots;
    }

}
