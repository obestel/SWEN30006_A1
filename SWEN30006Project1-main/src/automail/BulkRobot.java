package automail;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import simulation.Clock;
import simulation.IMailDelivery;

public class BulkRobot extends Robot {
    private static final String ROBOT_TYPE = "B";
    private static final double TYPE_BASED_RATE = 0.01;
    private static final int MOVE_SPEED = 1;
    private static final int TUBE_SIZE = 5;
    private static int operatingTime = 0;
    private static int numBots = 0;
    private static int totalTimeOperated = 0;
    private static ChargeHandlerAdapter chargeHandlerAdapter;


    public BulkRobot(IMailDelivery delivery, MailPool mailPool, int number) throws Exception {
        super(delivery, mailPool, number, ROBOT_TYPE, TUBE_SIZE);
        incrementRobotCount();
        chargeHandlerAdapter = ChargeHandlerAdapterModem.getInstance();
    }

    public void operate() throws ExcessiveDeliveryException, Exception {
        if (getCurrentState() != RobotState.WAITING) {
            incrementTimeOperated();
        }
        switch (getCurrentState()) {
            /** This state is triggered when the robot is returning to the mailroom after a delivery */
            case RETURNING:
                /** If its current position is at the mailroom, then the robot should change state */
                if (getCurrentFloor() == Building.getInstance().getMailroomLocationFloor()) {
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
                if (!isEmpty() && getReceivedDispatch()) {
                    completedDispatch();
                    resetDeliveryCounter(); // reset delivery counter
                    setDestination();
                    changeState(RobotState.DELIVERING);
                }
                break;
            case DELIVERING:
                if (getCurrentFloor() == getDestinationFloor()) { // If already here drop off either way
                    /** Delivery complete, report this to the simulator! */
                    String summary = "";
                    MailItem nextDelivery = popFromTube();
                    if (feeCharging) {
                        summary = chargeHandlerAdapter.formSummaryString(nextDelivery.destination_floor,
                                calcAvgOperatingTime(), TYPE_BASED_RATE);

                    }
                    getDelivery().deliver(this, nextDelivery, summary);
                    tickDeliveryCounter();
                    if (getDeliveryCounter() > 5) {  // Implies a simulation bug
                        throw new ExcessiveDeliveryException();
                    }
                    /** Check if want to return, i.e. if there is no item in the tube*/
                    if (getCurrentTubeLength() == 0) {
                        changeState(RobotState.RETURNING);
                    } else {
                        /** If there is another item, set the robot's route to the location to deliver the item */
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

    protected void changeState(RobotState nextState) {
        assert (getCurrentTubeLength() != 0);
        if (getCurrentState() != nextState) {
            System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdTube(), getCurrentState(), nextState);
        }
        setCurrentState(nextState);
        if (nextState == RobotState.DELIVERING) {
            System.out.printf("T: %3d > %7s-> [%s]%n", Clock.Time(), getIdTube(), getTube()[getCurrentTubeLength() - 1].toString());
        }
    }

    public void setDestination() {
        setDestinationFloor(getTube()[getCurrentTubeLength() - 1].destination_floor);
    }

    public boolean isEmpty() {
        return (getCurrentTubeLength() == 0);
    }

    public void loadItem(MailItem mailItem) throws ItemTooHeavyException {
        assert (getCurrentTubeLength() < TUBE_SIZE);
        addToTube(mailItem);
        if (mailItem.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
    }

    public boolean isFull() {
        return getCurrentTubeLength() == TUBE_SIZE;
    }

    public static void incrementRobotCount() {
        numBots++;
    }

    public static void incrementTimeOperated() {
        totalTimeOperated++;
    }

    public static double calcAvgOperatingTime() {
        return totalTimeOperated / (double) numBots;
    }
}
