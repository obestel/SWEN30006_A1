package automail;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import simulation.Clock;
import simulation.IMailDelivery;
import util.Configuration;

/**
 * The robot delivers mail!
 */
public abstract class Robot {

    protected static final int INDIVIDUAL_MAX_WEIGHT = 2000;

    private IMailDelivery delivery;
    private final String id;
    /** Possible states the robot can be in */
    public enum RobotState { DELIVERING, WAITING, RETURNING }
    private RobotState current_state;
    private int current_floor;
    private int destination_floor;
    private MailPool mailPool;
    private boolean receivedDispatch;
    private final int TUBE_SIZE;
    private int currentTubeLength = 0;
    protected boolean feeCharging;

    private MailItem deliveryItem = null;
    private MailItem[] tube;
    private int deliveryCounter;

    public Robot() {
        this.id = null;
        this.delivery = null;
        this.current_state = null;
        this.delivery = null;
        this.current_floor = 0;
        this.receivedDispatch = false;
        this.mailPool = null;
        this.TUBE_SIZE = 0;

    }
    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     * @param delivery governs the final delivery
     * @param mailPool is the source of mail items
     */
    public Robot(IMailDelivery delivery, MailPool mailPool, int number, String robotType, int TUBE_SIZE){
    	this.id = robotType + number;
        // current_state = RobotState.WAITING;
    	current_state = RobotState.RETURNING;
        current_floor = Building.getInstance().getMailroomLocationFloor();
        this.delivery = delivery;
        this.mailPool = mailPool;
        this.receivedDispatch = false;
        this.deliveryCounter = 0;
        this.TUBE_SIZE = TUBE_SIZE;
        this.tube = new MailItem[TUBE_SIZE];
        this.feeCharging = Boolean.parseBoolean(Configuration.getInstance().getProperty(
                Configuration.FEE_CHARGING_KEY));

    }


    /************************ GETTERS AND SETTERS ****************************************************/

    public String getId() {
        return this.id;
    }

    public MailItem getDeliveryItem() {
        return this.deliveryItem;
    }

    public void setDeliveryItem(MailItem deliveryItem) {
        this.deliveryItem = deliveryItem;
    }

    public IMailDelivery getDelivery() {
        return this.delivery;
    }

    public void tickDeliveryCounter() {
        this.deliveryCounter++;
    }

    public void resetDeliveryCounter() {
        this.deliveryCounter = 0;
    }

    public int getDeliveryCounter() {
        return deliveryCounter;
    }

    public void setCurrentTubeLength(int i) {
        this.currentTubeLength = i;
    }

    public int getCurrentTubeLength() {
        return currentTubeLength;
    }

    public RobotState getCurrentState() {
        return this.current_state;
    }

    public int getCurrentFloor() {
        return this.current_floor;
    }

    public void incrementCurrentFloor(int increment) {
        this.current_floor += increment;
    }

    public void decrementCurrentFloor(int decrement) {
        this.current_floor -= decrement;
    }

    public MailPool getMailPool() {
        return this.mailPool;
    }

    public boolean getReceivedDispatch() {
        return receivedDispatch;
    }

    public void completedDispatch() {
        this.receivedDispatch = false;
    }

    public int getDestinationFloor() {
        return this.destination_floor;
    }

    public void setDestinationFloor(int i) {
        this.destination_floor = i;
    }

    public void setCurrentState(RobotState nextState) {
        this.current_state = nextState;
    }

    
    /**
     * This is called when a robot is assigned the mail items and ready to dispatch for the delivery 
     */
    public void dispatch() {
    	receivedDispatch = true;
    }

    /**
     * This is called on every time step
     * @throws ExcessiveDeliveryException if robot delivers more than the capacity of the tube without refilling
     */
    public abstract void operate() throws ExcessiveDeliveryException, Exception;

    /**
     * Sets the route for the robot
     */
    public abstract void setDestination();

    /**
     * Generic function that moves the robot towards the destination
     * @param destination the floor towards which the robot is moving
     */
    protected void moveTowards(int destination, int moveSpeed) {
        /** If Robot cannot reach destination (going up) in one step, add full possible movement **/
        if (getCurrentFloor() < destination && (getCurrentFloor() + moveSpeed < destination)) {
            incrementCurrentFloor(moveSpeed);
        }
        /** If Robot can reach destination (going up) in one step, add as much movement as necessary **/
        else if (getCurrentFloor() < destination && (getCurrentFloor() + moveSpeed < destination)) {
            incrementCurrentFloor(destination - getCurrentFloor());
        }
        /** If Robot cannot reach destination (going down) in one step, minus full possible movement **/
        else if (getCurrentFloor() > destination && (getCurrentFloor() - moveSpeed > destination)) {
            decrementCurrentFloor(moveSpeed);
        }
        /** If Robot can reach destination (going down) in one step, add as much movement as necessary **/
        else {
            decrementCurrentFloor(getCurrentFloor() - destination);
        }
    }
    public String getIdTube() {
    	return String.format("%s(%1d)", this.id, currentTubeLength);
    }
    
    /**
     * Prints out the change in state
     * @param nextState the state to which the robot is transitioning
     */
    protected abstract void changeState(RobotState nextState);

	public MailItem[] getTube() {
		return tube;
	}

	public abstract boolean isEmpty();

	public void addToHand(MailItem mailItem) throws ItemTooHeavyException{}


	public void addToTube(MailItem mailItem) throws ItemTooHeavyException {
	    assert(tube != null);
	    assert(currentTubeLength <= TUBE_SIZE);
	    tube[currentTubeLength] = mailItem;
	    currentTubeLength++;
	    if (mailItem.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
    };

	public MailItem popFromTube() {
	    MailItem item = tube[currentTubeLength - 1];
	    currentTubeLength--;
	    return item;
    }

	public abstract void loadItem(MailItem mailItem) throws ItemTooHeavyException;

	public abstract boolean isFull();

}
