import java.util.concurrent.Semaphore;

public class DMV_Sim {

    //Constants
    public static int NumOfCust;
    public static boolean allDone = false;

    //Semaphores
    public static Semaphore waitingRoomCoord = new Semaphore(1, true);
    public static Semaphore agentCoord = new Semaphore(1, true);

    public static Semaphore infoDeskWait = new Semaphore(0, true);
    public static Semaphore infoDeskTakeNum = new Semaphore(0, true);
    public static Semaphore infoDeskTakeNumWait = new Semaphore(0, true);

    public static Semaphore announcerWait = new Semaphore(0, true);
    public static Semaphore spaceInLine = new Semaphore(4, true);
    public static Semaphore waitToGetInAgentLine = new Semaphore(0, true);

    public static Semaphore agentWait = new Semaphore(0, true);
    public static Semaphore agentExamWait = new Semaphore(0, true);
    public static Semaphore custFinishesExam1 = new Semaphore(0, true);
    public static Semaphore custFinishesExam2 = new Semaphore(0, true);
    public static Semaphore waitForLiscense1 = new Semaphore(0, true);
    public static Semaphore waitForLiscense2 = new Semaphore(0, true);

    //initialize global variables
    static int infoNumGiven;
    static int[] customerNums = new int[20];
    static int custShowNum;
    static int custAgentNum;
    static int AgentAllocation;

    //Simulates DMV
    //initalizes all the threads and joins them when they're done
    //Takes a the first command line argument as the number of customers to create
    public static void main(String[] args) {
        //Set Number of Customers
        NumOfCust = strToInt(args[0]);

        //initialize Info Desk
        Thread infoDesk = new Thread(new InfoDesk());
        infoDesk.setDaemon(true);
        infoDesk.start();

        //initialize Announcer
        Thread announcer = new Thread(new Announcer());
        announcer.setDaemon(true);
        announcer.start();

        //initialize Agents
        Thread agent1 = new Thread(new Agent(1));
        agent1.setDaemon(true);
        agent1.start();

        Thread agent2 = new Thread(new Agent(2));
        agent2.setDaemon(true);
        agent2.start();

        //intialize customers
        Thread customers[] = new Thread[NumOfCust];
        for (int k = 0; k < NumOfCust; k++) {
            customers[k] = new Thread(new Customer(k));
        }
        for (int k = 0; k < NumOfCust; k++) {
            customers[k].setDaemon(true);
            customers[k].start();
        }

        for (int i = 0; i < NumOfCust; i++) {
            try {
                customers[i].join();
                System.out.println("Customer " + i + " joined");
            } catch (InterruptedException e) {
            }
        }
        allDone = true;
    }

    //Simulates Customers
    public static class Customer implements Runnable {

        int id;
        int num;

        public Customer(int idIn) {
            id = idIn;
        }

        @Override
        public void run() {
            System.out.println("Customer " + id + " created");
            System.out.println("Customer " + id + " Enters the DMV");
            try {
                //Go to info desk and take a number
                infoDeskWait.release();
                infoDeskTakeNum.acquire();
                num = infoNumGiven;
                customerNums[id] = num;
                infoDeskTakeNumWait.release();
                System.out.println("Customer " + id + " got number " + num);

                //Wait to get into Agent Line
                waitingRoomCoord.acquire();
                custShowNum = num;
                announcerWait.release();
                waitToGetInAgentLine.acquire();
                waitingRoomCoord.release();

                //Wait for an Agent
                System.out.println("Customer " + id + " moves to the agent line");
                agentCoord.acquire();
                custAgentNum = id;
                agentWait.release();
                agentExamWait.acquire();
                int myAgent = AgentAllocation;
                agentCoord.release();

                //Work with Agent
                System.out.println("Customer " + id + " takes the eye exam from Agent " + myAgent);
                if (myAgent == 1) {
                    custFinishesExam1.release();
                    waitForLiscense1.acquire();
                    System.out.println("Customer " + id + " Takes Liscense from Agent " + myAgent);
                } else {
                    custFinishesExam2.release();
                    waitForLiscense2.acquire();
                    System.out.println("Customer " + id + " Takes Liscense from Agent " + myAgent);
                }

                System.out.println("Customer " + id + " Departs");

            } catch (InterruptedException e) {
                System.out.println("Customer " + id + " interrupted");
            }

        }

    }

    //Simulates Info Desk
    public static class InfoDesk implements Runnable {

        public InfoDesk() {
        }

        @Override
        public void run() {
            try {

                System.out.println("Infodesk Initiated");
                int num = 1;

                while (num <= NumOfCust) {

                    infoDeskWait.acquire();

                    //Meets the Customer
                    infoNumGiven = num;
                    num++;
                    infoDeskTakeNum.release();
                    infoDeskTakeNumWait.acquire();
                }

            } catch (InterruptedException e) {

            }

        }

    }

    //Simulates Announcer
    public static class Announcer implements Runnable {

        public Announcer() {
        }

        @Override
        public void run() {
            try {

                System.out.println("Announcer Initiated");

                while (!allDone) {
                    announcerWait.acquire();
                    spaceInLine.acquire();
                    System.out.println("Announcer calls " + custShowNum);
                    waitToGetInAgentLine.release();

                }

            } catch (InterruptedException e) {

            }

        }

    }

    //Simulates Agents
    public static class Agent implements Runnable {

        int id;

        public Agent(int idIn) {
            id = idIn;
        }

        @Override
        public void run() {
            try {

                System.out.println("Agent " + id + " Initiated");

                while (!allDone) {
                    agentWait.acquire();
                    spaceInLine.release();
                    int myCust = custAgentNum;
                    AgentAllocation = id;
                    System.out.println("Agent " + id + " calls Customer " + myCust);
                    System.out.println("Agent " + id + " administers exam to Customer " + myCust);
                    agentExamWait.release();
                    if (id == 1) {
                        custFinishesExam1.acquire();
                        System.out.println("Agent " + id + " gives Customer " + myCust + " a liscense");
                        waitForLiscense1.release();
                    } else {
                        custFinishesExam2.acquire();
                        System.out.println("Agent " + id + " gives Customer " + myCust + " a liscense");
                        waitForLiscense2.release();
                    }

                }

            } catch (InterruptedException e) {

            }

        }

    }

    public static int strToInt(String str) {
        int n = 0;
        for (int i = 0; i < str.length(); i++) {
            n = n + (((int) str.charAt(i) - 48) * (int) Math.pow(10, str.length() - 1 - i));
        }
        return n;
    }

}
