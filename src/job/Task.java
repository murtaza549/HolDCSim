package job;

import java.util.Vector;

import constants.Constants;
import constants.Constants.TaskPriority;
import debug.Sim;
import event.ComputationFinishEvent;
import event.TaskFinishEvent;
import event.TaskTimerEvent;
import infrastructure.Server;

/**
 * A Task is the basic unit of work that servers process. The amount of "work"
 * they represent is quantified in seconds. For example, if a Task is 2 seconds
 * big. It will complete in 2 seconds from starting, given that nothing
 * interrupts it. This may be modulated by many things (e.g., slowing the CPU).
 * 
 * 
 */
public class Task implements Comparable<Task> {

	public static enum TaskStatus {
		CREATED, ARRIVED, STARTED, PAUSED, IN_COMPUTATION, IN_COMMUNICATION
	}

	private static int TASK_QOS = 9;
	private Job aJob;
	private long JobId;
	// workload this task belongs to
	//private int jobType;
	// the server that the task would run
	private Server mServer = null;

	// private TaskStatus taskStatus;

	/**
	 * When the Task arrived in the system This variable should be set only
	 * once.
	 */
	private double arrivalTime;

	/**
	 * When the Task actually started (i.e. after queuing delays). This variable
	 * should be set only once.
	 */
	private double startTime;

	private double beginExecuteTime;
	private boolean doneExecuting;

	private double compFinishTime;

	/**
	 * When the Task completes This variable should be set only once.
	 */
	private double finishTime;

	/**
	 * Every Task has a unique monotonically increasing id.
	 */
	private long TaskId;

	/**
	 * The "length" of the Task. Divide by server rate to get the time to
	 * completion.
	 */
	private double taskSize;

	/**
	 * If the Task is suspended, this will give the amount that has been
	 * completed.
	 */
	private double amountCompleted;

	/**
	 * Cumulatively tracks how many seconds this Task has been delayed.
	 */
	private double amountDelayed;

	/**
	 * A static incrementing variable for creating Task IDs.
	 */
	private static long currentId = 1;

	// /**
	// *
	// */
	// private boolean atLimit;

	/**
	 * The event that will occur when the Task finishes.
	 */
	private ComputationFinishEvent mCFEvent;
	private TaskFinishEvent finishEvent;

	/**
	 * The last time this Task was resumed. Useful when Tasks are paused.
	 */
	private double lastResumeTime;

	// the time when the task was generated by the job
	private double taskGenTime;

	// latest time to start
	private double earliestTimeToStart; // 9x of the original task size
	
	// parent or child task
	private boolean isParentTask;
	
	// task status
	private TaskStatus taskStatus;
	
	private Task parentTask;
	private Vector<Task> childTask;
	
	// Schedule task if timer is exceeded even if server workload is not
	private TaskTimerEvent taskTimerEvent = null;

	/**
	 * @author fan this function get the job the task belongs to
	 * @return
	 */
	public Job getaJob() {
		return aJob;
	}
	
//	public Job getaJob() {
//		return aJob;
//	}

	/**
	 * Constructs a new Task.
	 * 
	 * @param theTaskSize
	 *            - The size of the Task in seconds.
	 */
	public Task(final double theTaskSize, Job job) {
		this.amountCompleted = 0.0;
		this.amountDelayed = 0.0;
		this.taskSize = theTaskSize;
		this.TaskId = assignId();
		// this.atLimit = false;
		this.mCFEvent = null;
		this.lastResumeTime = 0.0;
		this.aJob = job;
		this.JobId = job.getJobId();
		this.compFinishTime = 0.0;
		this.beginExecuteTime = 0.0;
		this.doneExecuting = false;
		this.mServer = null;
	//	this.jobType = 0;
		this.earliestTimeToStart = 0.0;
		this.parentTask = null;
		this.childTask = null;
	}
	
//	public Task(final double theTaskSize, Job job) {
//		this.amountCompleted = 0.0;
//		this.amountDelayed = 0.0;
//		this.taskSize = theTaskSize;
//		this.TaskId = assignId();
//		// this.atLimit = false;
//		this.mCFEvent = null;
//		this.lastResumeTime = 0.0;
//		this.aJob = job;
//		this.JobId = job.getJobId();
//		this.compFinishTime = 0.0;
//		this.beginExecuteTime = 0.0;
//		this.mServer = null;
//	//	this.jobType = 0;
//		this.earliestTimeToStart = 0.0;
//		this.isPlaced = false;
//	}

	/**
	 * static priority is set based on job type
	 * used by prioritytask queue for mixed-job workload
	 * @param time
	 */
	public void setTaskGenTime(double time){
    	taskGenTime = time;
    	
    	
    	/***************************************/
		if (Constants.PRIORITIZE_JOB) {
			/* tasks with priority */
			if (Constants.taskPriority == TaskPriority.EARLIEST_STARTTIME)
				earliestTimeToStart = taskGenTime + TASK_QOS * taskSize;
			// always dispatch short job first
			else if (Constants.taskPriority == TaskPriority.SHORTJOB_FIRST) {
				if (aJob.getJobType() == 0) {
					earliestTimeToStart = 1;
				} else {
					earliestTimeToStart = 100;
				}
			}
		}
		
		//this.earliestTimeToStart = 0.0;
    	/***************************************/
    }

	public double getEarliestStartTime() {
		return earliestTimeToStart;
	}

	// public void setTaskStatus(TaskStatus _taskStatus){
	// this.taskStatus = _taskStatus;
	// }

	// public void setAtLimit(boolean atLimit) {
	// this.atLimit = atLimit;
	// }
	//
	// public boolean getAtLimit() {
	// return this.atLimit;
	// }

	/**
	 * Gets the amount (in seconds) the Task has been delayed.
	 * 
	 * @return the amount (in seconds) the Task has been delayed.
	 */
	public final double getAmountDelayed() {
		return this.amountDelayed;
	}

	/**
	 * Set the amount the Task has been delayed (in seconds).
	 * 
	 * @param amount
	 *            - how much the Task has been delayed (in seconds).
	 */
	public final void setAmountDelayed(final double amount) {
		this.amountDelayed = amount;
	}

	/**
	 * Sets how much of the Task has been completed (in seconds).
	 * 
	 * @param completed
	 *            - how much of the Task has been completed (in seconds).
	 */
	public final void setAmountCompleted(final double completed) {
		this.amountCompleted = completed;
	}

	/**
	 * Gets how much of the Task has been completed (in seconds).
	 * 
	 * @return how much of the Task has been completed (in seconds).
	 */
	public final double getAmountCompleted() {
		return this.amountCompleted;
	}

	/**
	 * Assigns an id to this Task. Increments the current id for future Tasks.
	 * 
	 * @return the id assigned to the calling Task
	 */
	private long assignId() {
		long toReturn = Task.currentId;
		Task.currentId++;
		return toReturn;
	}

	/**
	 * Gets the Task id of the Task.
	 * 
	 * @return the Task's Task id
	 */
	public final long getTaskId() {
		return this.TaskId;
	}

	/**
	 * Gets the id of the Job that the Task belongs to.
	 * 
	 * @return the JobId
	 */
	public final long getJobId() {
		return this.JobId;
	}

	public final double getStartExecutionTime() {
		return this.beginExecuteTime;
	}

	/**
	 * Marks the arrival time of the Task to a server. Should only ever need to be called
	 * once and the simulation will fail if this is called twice or more on a
	 * given Task.
	 * 
	 * @param time
	 *            - the time the Task arrives
	 */
	public final void markArrival(final double time) {
		if (this.arrivalTime > 0) {
			Sim.fatalError("Task arrival marked twice!");
		}
		this.arrivalTime = time;
		setTaskStatus(TaskStatus.ARRIVED);
	}

	public final void markComputationFinish(final double time) {
		if (this.compFinishTime > 0) {
			Sim.fatalError("Task computation finish event marked twice!");
		}
		this.compFinishTime = time;
	}

	public double getComputationFinishTime() {
		return this.compFinishTime;
	}

	/**
	 * Mark the start time of the Task. Should only ever need to be called once
	 * and the simulation will fail if this is called twice or more on a given
	 * Task.
	 * 
	 * @param time
	 *            - the start time of the Task
	 */
	public final void markStart(final double time) {
		if (this.startTime > 0) {
			Sim.fatalError("Task start marked twice!");
		}
		this.startTime = time;
	}

	/**
	 * Marks the time the Task finished. Should only ever need to be called once
	 * and the simulation will fail if this is called twice or more on a given
	 * Task.
	 * 
	 * @param time
	 *            - the finish time of the Task
	 */
	public final void markFinish(final double time) {
		if (this.finishTime > 0) {
			Sim.fatalError("Task " + this.getTaskId() + " finish marked twice!");
		}
		this.finishTime = time;
	}

	public final void markBeginExecute(final double time) {
		this.beginExecuteTime = time;
	}
	
	/**
	 * True if task is done executing, false otherwise
	 */
	public final void markDoneExecuting() {
		if(doneExecuting) {
			Sim.fatalError("Task " + this.getTaskId() + " finished execution twice!");
		}
		this.doneExecuting = true;
	}
	
	public final boolean getDoneExecuting() {
		return this.doneExecuting;
	}

	// public final void markCompFinish(final double time) {
	// if (this.finishTime > 0) {
	// Sim.fatalError("Task " + this.getTaskId() + " finsih marked twice!");
	// }
	// this.finishTime = time;
	// }
	/**
	 * Gets the time the Task arrived at its server.
	 * 
	 * @return the arrival time of the Task at its server
	 */
	public final double getArrivalTime() {
		return this.arrivalTime;
	}

	/**
	 * Gets the time at which the Task started.
	 * 
	 * @return the start time of the Task
	 */
	public final double getStartTime() {
		return this.startTime;
	}

	/**
	 * Gets the time at which the Task finished.
	 * 
	 * @return the finish time of the Task
	 */
	public final double getFinishTime() {
		return this.finishTime;
	}

	/**
	 * Gets the size of the Task (in seconds).
	 * 
	 * @return the size of the Task (in seconds)
	 */
	public final double getSize() {
		return this.taskSize;
	}

	/**
	 * Checks if another Task is equal to this one.
	 * 
	 * @param obj
	 *            - the object that is compared to this one.
	 * @return true if the Task is equal to this one.
	 */
	@Override
	public final boolean equals(final Object obj) {
		boolean objectEqual = super.equals(obj);
		if (!objectEqual) {
			return false;
		}

		// TODO (meisner@umich.edu) this may be exessive
		boolean idEqual = ((Task) obj).getTaskId() == this.TaskId;

		if (!idEqual) {
			return false;
		}

		return true;
	}

	/**
	 * Function to hash the object by.
	 * 
	 * @return the hash code of the object
	 */
	@Override
	public final int hashCode() {
		return (int) this.TaskId;
	}

	/**
	 * Set the even that finishes the Task.
	 * 
	 * @param aTaskFinishEvent
	 *            - The even that finishes the Task.
	 */
	public final void setComputationFinishEvent(
			final ComputationFinishEvent aCFEvent) {
		this.mCFEvent = aCFEvent;
	}

	/**
	 * Gets the even representing when the Task finishes. May be null initially
	 * if it hasn't been set yet.
	 * 
	 * @return the Task finish event.
	 */
	public final ComputationFinishEvent getComputationFinishEvent() {
		return this.mCFEvent;
	}

	public final TaskFinishEvent getTaskFinishEvent() {
		return finishEvent;
	}

	public final void setTaskFinishEvent(TaskFinishEvent finishEvent) {
		this.finishEvent = finishEvent;
	}

	/**
	 * Sets the last time the Task was resumed. (Set this when the Task is
	 * resumed)
	 * 
	 * @param time
	 *            - the time the Task is resumed.
	 */
	public final void setLastResumeTime(final double time) {
		this.lastResumeTime = time;
	}

	/**
	 * Gets that last time Task was resumed.
	 * 
	 * @return the last resume time
	 */
	public final double getLastResumeTime() {
		return this.lastResumeTime;
	}

	public void setServer(Server server) {
		// TODO Auto-generated method stub
		this.mServer = server;

	}

	public Server getServer() {
		// TODO Auto-generated method stub
		return mServer;
	}

	@Override
	public int compareTo(Task anotherTask) {
		// TODO Auto-generated method stub
		// compare normalized latency
		Double firstNLatency = (this.finishTime - this.arrivalTime)
				/ this.taskSize;
		Double secondNLatency = (anotherTask.finishTime - anotherTask.arrivalTime)
				/ anotherTask.taskSize;
		return firstNLatency.compareTo(secondNLatency);
	}

	public int getJobType() {
		// TODO Auto-generated method stub
		return aJob.getJobType();
	}
	
	public void setParentTask(boolean isParentTask) {
		this.isParentTask = isParentTask;
	}
	
	public boolean isParentTask() {
		return isParentTask;
	}
	
	public TaskStatus getTaskStatus() {
		return taskStatus;
	}

	public void setTaskStatus(TaskStatus taskStatus) {
		this.taskStatus = taskStatus;
	}
	
	public void setParentTask(Task task) {
		parentTask = task;
	}
	
	public Task getParentTask() {
		return parentTask;
	}
	
	public void setChildTask(Vector<Task> task) {
		childTask = task;
	}
	
	public Vector<Task> getChildTask() {
		return childTask;
	}
	
	public void setTaskTimerEvent(TaskTimerEvent taskTimerEvent) {
		this.taskTimerEvent = taskTimerEvent;
	}
	
	public TaskTimerEvent getTaskTimerEvent() {
		return taskTimerEvent;
	}
}
