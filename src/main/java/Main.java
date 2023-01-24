/**
 * Runs a simulation for a circuit which implements a 2-bit counter.
 */
public class Main {
    // the time at which the simulation should stop
    static final double STOP_TIME = 8;
    // the clock's period
    static final double CLOCK_PERIOD = 1;
    // the delay for a D-FF's output to change
    static final double DFF_DELAY = 0.02;
    // the delay for a gate's output to change
    static final double GATE_DELAY = 0.005;

    public static void main(String[] args) {
        // `simulator` is a simulator that stops at `STOP_TIME`
        CircuitSimulator simulator = new CircuitSimulator(STOP_TIME);
        // `clock` is a clock with the period `CLOCK_PERIOD`
        CircuitSimulator.Clock clock = simulator.new Clock(CLOCK_PERIOD);

        // `reset` is an input that is high (and otherwise low) for 0.1 time units at the start of each simulation.
        CircuitSimulator.ExternalInput reset = simulator.new ExternalInput();
        reset.scheduleUpdate(0, Logic.TRUE);
        reset.scheduleUpdate(0.1, Logic.FALSE);

        // `Q0` and `Q1` are the D-FFs that respectively store the LSB and MSB.
        CircuitSimulator.DFlipFlop Q0 = simulator.new DFlipFlop(DFF_DELAY);
        CircuitSimulator.DFlipFlop Q1 = simulator.new DFlipFlop(DFF_DELAY);
        // initialise the clock and reset inputs for `Q0` and `Q1`
        Q0.setClock(clock); Q0.setReset(reset);
        Q1.setClock(clock); Q1.setReset(reset);

        // initialise the combinatorial logic for the data inputs to `Q0` and `Q1`
        Q0.setData(simulator.new NOTGate(GATE_DELAY, Q0));
        Q1.setData(simulator.new XORGate(GATE_DELAY, Q0, Q1));

        // initialise watchpoints for the outputs of `Q0` and `Q1`
        simulator.new Watchpoint("Q0", Q0);
        simulator.new Watchpoint("Q1", Q1);

        // run the simulation under the above setup
        simulator.run();
    }
}
