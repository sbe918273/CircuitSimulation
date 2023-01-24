import java.util.Set;
import java.util.HashSet;
import java.util.PriorityQueue;

/**
 * A class representing a simulator that simulates (at the component-level) a basic electronic circuit.
 */
public class CircuitSimulator {
    // the FEL
    protected PriorityQueue<Event> events;
    // `initialEvents` stores the initial contents of the FEL.
    protected final Set<Event> initialEvents = new HashSet<>();

    /**
     * A constructor initialising the time at which each simulation should stop.
     * @param stopTime the time at which each simulation should stop
     */
    public CircuitSimulator(double stopTime) {
        // add a stop event at the given time to `initialEvents`
        initialEvents.add(new StopEvent(stopTime));
    }

    /**
     * Simulates the circuit under the current setup.
     */
    public void run() {
        // initialise the FEL with `initialEvents`
        events = new PriorityQueue<>(initialEvents);
        // We pop an event from the FEL and execute it until the FEL becomes empty.
        while (!events.isEmpty()) {
            Event event = events.remove();
            event.execute();
        }
    }

    /**
     * An abstract class representing an event notice.
     */
   protected static abstract class Event implements Comparable<Event> {
       // the event time
        protected final double time;

        // initialises `time`
        public Event(double time) {
            this.time = time;
        }

        /**
         * Executes this event by altering the system's state and/or putting new events on the FEL.
         */
        public abstract void execute();

        // We compare two events using their times.
        @Override
        public int compareTo(Event other) {
            return Double.compare(time, other.time);
        }
    }

    /**
     * A class representing an update event notice.
     * An update event updates a component's output and propagates any resulting change.
     */
    protected static class UpdateEvent extends Event {
        // the component being updated
        private final Component component;
        // the new output for `component`
        private final Logic newOutput;

        // initialises `time`, `component` and `newOutput`
        public UpdateEvent(double time, Component component, Logic newOutput) {
            super(time);
            this.component = component;
            this.newOutput = newOutput;
        }

        /**
         * Executes this event by updating `component`'s output to `newOutput` and propagating any resulting change.
         */
        @Override
        public void execute() {
            component.updateOutput(time, newOutput);
        }

        // useful for debugging
        @Override
        public String toString() {
            return String.format("[Update] %.2f: %s = %s -> %s", time, component, component.getOutput(), newOutput);
        }
    }

    /**
     * A class representing a refresh event notice.
     * A refresh event re-evaluates component's output, effecting and propagating any resulting change.
     */
    protected static class RefreshEvent extends Event {
        // the component being updated
        private final IOComponent component;
        // the input to that component which was updated
        private final Component updatedInput;

        // initialises `time`, `component` and `newOutput`
        public RefreshEvent(double time, IOComponent component,  Component updatedInput) {
            super(time);
            this.component = component;
            this.updatedInput = updatedInput;
        }

        /**
         * Executes this event by re-evaluating this component's output, effecting and propagating any resulting change.
         */
        @Override
        public void execute() {
            component.updateOutput(time, component.calculateOutput(updatedInput));
        }

        // useful for debugging
        @Override
        public String toString() {
            return String.format("[Refresh] %.2f: %s updated by %s", time, component, updatedInput);
        }
    }

    /**
     * A class representing a stop event notice.
     * A stop event stops the encompassing simulation.
     */
    protected class StopEvent extends Event {
        // initialises `time`
        public StopEvent(double time) {
            super(time);
        }

        /**
         * Executes this event by clearing the FEL, stopping the simulation.
         */
        @Override
        public void execute() {
            events.clear();
        }
    }

    /**
     * A class representing an abstract circuit component.
     */
    abstract public class Component {
        // We initialise `output` (this component's output) to `UNKNOWN`.
        private Logic output = Logic.UNKNOWN;
        // `listeners` is the set of `IOComponent`s whose output can be affected by a change in this component's output.
        private final Set<IOComponent> listeners = new HashSet<>();

        /**
         * @return this component's output
         */
        protected Logic getOutput() {
            return output;
        }

        /**
         * Adds an `IOComponent` to this component's listeners.
         * @param listener an `IOComponent`
         */
        protected void addListener(IOComponent listener) {
            listeners.add(listener);
        }

        /**
         * Removes an `IOComponent` from this component's listeners (if it is such a listener).
         * @param listener an `IOComponent`
         */
        protected void removeListener(IOComponent listener) {
            listeners.remove(listener);
        }

        /**
         * Updates this component's output to a new output and propagates any resulting change.
         * @param time the time for the update
         * @param newOutput the new output for this component
         */
        protected void updateOutput(double time, Logic newOutput) {
            // We update `output` and propagate the resulting change iff the new output differs from the current output.
            if (newOutput != output) {
                output = newOutput;
                // We schedule an update event to update each listener's output to reflect this component's new output.
                for (IOComponent listener: listeners) {
                    events.add(new RefreshEvent(
                            // `listener`'s output changes after its propagation delay.
                            time + listener.getDelay(),
                            listener,
                            // This component is the updated input to the listener.
                            this
                    ));
                }
            }
        }
    }

    /**
     * A class representing an abstract circuit component whose output is only affected by its one or more inputs.
     */
    abstract public class IOComponent extends Component {
        /**
         * @return this component's delay
         */
        abstract double getDelay();

        /**
         * Calculates this component's output after an input has been updated.
         * @param updatedInput the updated input
         * @return the new output for this component
         */
        abstract Logic calculateOutput(Component updatedInput);
    }

    /**
     * A class representing a clock of a fixed period.
     */
    public class Clock extends Component {
        // half of this clock's period
        private final double halfPeriod;

        /**
         * A constructor to initialise this clock's period and its initial value.
         * @param period this clock's period
         */
        public Clock(double period) {
            // initialise `halfPeriod`
            this.halfPeriod = period / 2;
            // put an update event in `initialEvents` which initialises this clock's output to `FALSE`
            initialEvents.add(new UpdateEvent(0, this, Logic.FALSE));
        }

        /**
         * Updates this component's output to a new output and propagates any resulting change.
         * Schedules an event to negate this clock's output at the next half period.
         * @param time the time for the update
         * @param newOutput the new output for this component
         */
        @Override
        public void updateOutput(double time, Logic newOutput) {
            events.add(new UpdateEvent(time + halfPeriod, this, Logic.NOT(newOutput)));
            super.updateOutput(time, newOutput);
        }
    }

    /**
     * A class representing an input for which the user can schedule updates in its value.
     */
    public class ExternalInput extends Component {
        /**
         * Schedules this input's output to be updated to a new output at a given time.
         * @param time the time for the update
         * @param newOutput the new output for this input
         */
        public void scheduleUpdate(double time, Logic newOutput) {
            // put an update event in `initialEvents` which updates this input's output to `newOutput` at `time`
            initialEvents.add(new UpdateEvent(time, this, newOutput));
        }
    }

    /**
     * A class representing a NOT gate.
     */
    public class NOTGate extends IOComponent {
        // this gate's input
        private Component input = null;
        // this gate's fixed delay
        private final double delay;

        // initialises `delay`
        public NOTGate(double delay) {
            this.delay = delay;
        }

        // initialises `input` and `delay`
        public NOTGate(double delay, Component input) {
            this(delay);
            setInput(input);
        }

        /**
         * Updates this gate's input, removing this gate as a listener of the previous input.
         * @param newInput the new input for this gate
         */
        public void setInput(Component newInput) {
            // remove this gate as a listener of the previous input
            if (input != null) {
                input.removeListener(this);
            }
            // update this gate's input
            input = newInput;
            // add this gate as a listener of the new input
            input.addListener(this);
        }

        @Override
        public double getDelay() {
            return delay;
        }

        /**
         * @return the negation of this gate's input
         */
        @Override
        public Logic calculateOutput(Component updatedInput) {
            return Logic.NOT(input.getOutput());
        }
    }

    /**
     * A class representing an OR gate.
     */
    public class ORGate extends IOComponent {
        // this gate's top input
        private Component topInput = null;
        // this gate's bottom input
        private Component bottomInput = null;
        // this gate's fixed delay
        private final double delay;

        // initialises `delay`
        public ORGate(double delay) {
            this.delay = delay;
        }

        // initialises `topInput`, `bottomInput` and `delay`
        public ORGate(double delay, Component topInput, Component bottomInput) {
            this(delay);
            setTopInput(topInput);
            setBottomInput(bottomInput);
        }

        /**
         * Updates this gate's top input, removing this gate as a listener of the previous top input.
         * @param newTopInput the new top input for this gate
         */
        public void setTopInput(Component newTopInput) {
            // remove this gate as a listener of the previous top input
            if (topInput != null) {
                topInput.removeListener(this);
            }
            // update this gate's top input
            topInput = newTopInput;
            // add this gate as a listener of the new top input
            topInput.addListener(this);
        }

        /**
         * Updates this gate's bottom input, removing this gate as a listener of the previous bottom input.
         * @param newBottomInput the new bottom input for this gate
         */
        public void setBottomInput(Component newBottomInput) {
            // remove this gate as a listener of the previous bottom input
            if (bottomInput != null) {
                bottomInput.removeListener(this);
            }
            // update this gate's bottom input
            bottomInput = newBottomInput;
            // add this gate as a listener of the new bottom input
            bottomInput.addListener(this);
        }

        @Override
        public double getDelay() {
            return delay;
        }

        /**
         * @return the disjunction of this gate's two inputs
         */
        @Override
        public Logic calculateOutput(Component updatedInput) {
            return Logic.OR(topInput.getOutput(), bottomInput.getOutput());
        }
    }

    /**
     * A class representing an AND gate.
     */
    public class ANDGate extends IOComponent {
        // this gate's top input
        private Component topInput = null;
        // this gate's bottom input
        private Component bottomInput = null;
        // this gate's fixed delay
        private final double delay;

        // initialises `delay`
        public ANDGate(double delay) {
            this.delay = delay;
        }

        // initialises `topInput`, `bottomInput` and `delay`
        public ANDGate(double delay, Component topInput, Component bottomInput) {
            this(delay);
            setTopInput(topInput);
            setBottomInput(bottomInput);
        }

        /**
         * Updates this gate's top input, removing this gate as a listener of the previous top input.
         * @param newTopInput the new top input for this gate
         */
        public void setTopInput(Component newTopInput) {
            // remove this gate as a listener of the previous top input
            if (topInput != null) {
                topInput.removeListener(this);
            }
            // update this gate's top input
            topInput = newTopInput;
            // add this gate as a listener of the new top input
            topInput.addListener(this);
        }

        /**
         * Updates this gate's bottom input, removing this gate as a listener of the previous bottom input.
         * @param newBottomInput the new bottom input for this gate
         */
        public void setBottomInput(Component newBottomInput) {
            // remove this gate as a listener of the previous bottom input
            if (bottomInput != null) {
                bottomInput.removeListener(this);
            }
            // update this gate's bottom input
            bottomInput = newBottomInput;
            // add this gate as a listener of the new bottom input
            bottomInput.addListener(this);
        }

        @Override
        public double getDelay() {
            return delay;
        }

        /**
         * @return the conjunction of this gate's two inputs
         */
        @Override
        public Logic calculateOutput(Component updatedInput) {
            return Logic.AND(topInput.getOutput(), bottomInput.getOutput());
        }
    }

    /**
     * A class representing an OR gate.
     */
    public class XORGate extends IOComponent {
        // this gate's top input
        private Component topInput = null;
        // this gate's bottom input
        private Component bottomInput = null;
        // this gate's fixed delay
        private final double delay;

        // initialises `delay`
        public XORGate(double delay) {
            this.delay = delay;
        }

        // initialises `topInput`, `bottomInput` and `delay`
        public XORGate(double delay, Component topInput, Component bottomInput) {
            this(delay);
            setTopInput(topInput);
            setBottomInput(bottomInput);
        }

        /**
         * Updates this gate's top input, removing this gate as a listener of the previous top input.
         * @param newTopInput the new top input for this gate
         */
        public void setTopInput(Component newTopInput) {
            // remove this gate as a listener of the previous top input
            if (topInput != null) {
                topInput.removeListener(this);
            }
            // update this gate's top input
            topInput = newTopInput;
            // add this gate as a listener of the new top input
            topInput.addListener(this);
        }

        /**
         * Updates this gate's bottom input, removing this gate as a listener of the previous bottom input.
         * @param newBottomInput the new bottom input for this gate
         */
        public void setBottomInput(Component newBottomInput) {
            // remove this gate as a listener of the previous bottom input
            if (bottomInput != null) {
                bottomInput.removeListener(this);
            }
            // update this gate's bottom input
            bottomInput = newBottomInput;
            // add this gate as a listener of the new bottom input
            bottomInput.addListener(this);
        }

        @Override
        public double getDelay() {
            return delay;
        }

        /**
         * @return the exclusive disjunction of this gate's two inputs
         */
        @Override
        public Logic calculateOutput(Component updatedInput) {
            return Logic.XOR(topInput.getOutput(), bottomInput.getOutput());
        }
    }

    /**
     * A class representing a D Flip-flop.
     */
    public class DFlipFlop extends IOComponent {
        // this D-FF's clock input
        private Component clock = null;
        // this D-FF's active-high reset input
        private Component reset = null;
        // this D-FF's data input
        private Component data = null;
        // this D-FF's fixed delay (for both resetting and propagating data)
        private final double delay;

        // initialises `delay`
        public DFlipFlop(double delay) {
            this.delay = delay;
        }

        // initialises `delay`, `clock`, `reset` and `data`
        public DFlipFlop(double delay, Component clock, Component reset, Component data) {
            this.delay = delay;
            setClock(clock);
            setReset(reset);
            setData(data);
        }

        /**
         * Updates this D-FF's clock input, removing this D-FF as a listener of the previous clock input.
         * @param newClock the new clock input for this D-FF
         */
        public void setClock(Component newClock) {
            // remove this D-FF as a listener of the previous clock input
            if (clock != null) {
                clock.removeListener(this);
            }
            // update this D-FF's clock input
            clock = newClock;
            // add this D-FF as a listener of the new clock input
            clock.addListener(this);
        }

        /**
         * Updates this D-FF's reset input, removing this D-FF as a listener of the previous reset input.
         * @param newReset the new reset input for this D-FF
         */
        public void setReset(Component newReset) {
            // remove this D-FF as a listener of the previous reset input
            if (reset != null) {
                reset.removeListener(this);
            }
            // update this D-FF's reset input
            reset = newReset;
            // add this D-FF as a listener of the new reset input
            reset.addListener(this);
        }

        /**
         * Updates this D-FF's data input, removing this D-FF as a listener of the previous data input.
         * @param newData the new data input for this D-FF
         */
        public void setData(Component newData) {
            // remove this D-FF as a listener of the previous data input
            if (data != null) {
                data.removeListener(this);
            }
            // update this D-FF's data input
            data = newData;
            // add this D-FF as a listener of the new data input
            data.addListener(this);
        }

        /**
         * Calculates this D-FF's output after an input has been updated.
         * @param updatedInput the updated input
         * @return the new output for this D-FF
         */
        @Override
        public Logic calculateOutput(Component updatedInput) {
            if (reset.getOutput() == Logic.TRUE) {
                // If `reset` is `TRUE` then we output `FALSE`.
                return Logic.FALSE;
            } else if (updatedInput == clock && clock.getOutput() == Logic.TRUE) {
                // Otherwise, if it is a positive clock edge then we output the current value of data input.
                return data.getOutput();
            } else {
                // Otherwise, we output our previous output.
                return getOutput();
            }
        }

        @Override
        public double getDelay() {
            return delay;
        }
    }

    /**
     * A class representing a watchpoint: it propagates its input with zero delay and prints every update of that input.
     */
    public class Watchpoint extends IOComponent {
        // this watchpoint's name
        private final String name;
        // this watchpoint's input
        private Component input;

        // initialises `name` and `input`
        public Watchpoint(String name, Component input) {
            this.name = name;
            setInput(input);
        }

        /**
         * Updates this watchpoint's input, removing this watchpoint as a listener of the previous input.
         * @param newInput the new input for this watchpoint
         */
        public void setInput(Component newInput) {
            // remove this watchpoint as a listener of the previous  input
            if (input != null) {
                input.removeListener(this);
            }
            // update this watchpoint's input
            input = newInput;
            // add this watchpoint as a listener of the new input
            input.addListener(this);
        }

        /**
         * Updates this watchpoint's output to a new output and propagates any resulting change.
         * Prints any resulting change.
         * @param time the time for the update
         * @param newOutput the new output for this component
         */
        @Override
        public void updateOutput(double time, Logic newOutput) {
            Logic previousOutput = getOutput();
            if (previousOutput != newOutput) {
                System.out.printf("%.2f: %s = %s -> %s\n", time, name, previousOutput, newOutput);
            }
            super.updateOutput(time, newOutput);
        }

        /**
         * @return this watchpoint's input
         */
        @Override
        public Logic calculateOutput(Component updatedInput) {
            return input.getOutput();
        }

        // This watchpoint propagates its input with zero delay.
        @Override
        public double getDelay() {
            return 0;
        }
    }
}
