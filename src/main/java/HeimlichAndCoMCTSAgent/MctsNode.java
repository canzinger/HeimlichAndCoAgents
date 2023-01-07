package HeimlichAndCoMCTSAgent;

import HeimlichAndCo.Actions.HeimlichAndCoAction;
import HeimlichAndCo.Actions.HeimlichAndCoDieRollAction;
import HeimlichAndCo.Enums.HeimlichAndCoPhase;
import HeimlichAndCo.HeimlichAndCo;
import at.ac.tuwien.ifs.sge.util.pair.ImmutablePair;
import at.ac.tuwien.ifs.sge.util.pair.Pair;

import java.util.*;

public class MctsNode {

    /**
     * This constant balances between exploration and exploitation.
     * The usually recommended value for this is square root of 2, but performance may be improved by changing it.
     */
    private static final double C = Math.sqrt(2);

    private final Comparator<HeimlichAndCoAction> ACTION_COMPARATOR_UCT = Comparator.comparingDouble(this::calculateUCT);
    private final Comparator<HeimlichAndCoAction> ACTION_COMPARATOR_Q_s_a = Comparator.comparingDouble(this::calculateQ_s_a_OfChild);

    /**
     * saves how many wins were achieved from this node
     */
    private int wins;
    /**
     * saves how many playouts were done from this node (or descendents of this node)
     */
    private int playouts;
    /**
     * the depth of this node; 0 for root node
     */
    private final int depth;
    /**
     * the current game (state)
     */
    private final HeimlichAndCo game;

    /**
     * All resulting child states that have been explored at least once.
     * A child node is reached by taking (applying) the action that is used as the key.
     */
    private final Map<HeimlichAndCoAction, MctsNode> children;

    /**
     * parent of this node; null for root node
     */
    private final MctsNode parent;

    public MctsNode(int wins, int playouts, HeimlichAndCo game, MctsNode parent) {
        this(game, parent);
        this.wins = wins;
        this.playouts = playouts;
    }

    public MctsNode(HeimlichAndCo game, MctsNode parent) {
        this.game = new HeimlichAndCo(game, false);
        this.parent = parent;
        if (parent != null) {
            this.depth = parent.depth + 1;
        } else {
            this.depth = 0;
        }
        this.children = new HashMap<>();
    }

    public HeimlichAndCo getGame() {
        return new HeimlichAndCo(game);
    }

    /**
     * selects a node with UCT strategy
     * during the first round checks all possible actions before selecting an action twice
     * @return this node and the selected action -> in the expansion phase the action can be taken from this node to get the new node
     */
    public Pair<MctsNode, HeimlichAndCoAction> selection(boolean simulateAllDiceOutcomes) {
        Set<HeimlichAndCoAction> possibleActions = game.getPossibleActions();
        // this means that this is a terminal game state
        if (possibleActions.isEmpty()) {
            return new ImmutablePair<>(this, null);
        }
        HeimlichAndCoAction selectedAction;
        if (simulateAllDiceOutcomes && game.getCurrentPhase() == HeimlichAndCoPhase.DieRollPhase) {
            possibleActions.remove(HeimlichAndCoDieRollAction.getRandomRollAction());
            selectedAction = possibleActions.toArray(new HeimlichAndCoAction[1])[(int)(possibleActions.size() * Math.random())];
        } else {
            List<HeimlichAndCoAction> maximumValuedActions = getMaximumValuedActions(possibleActions, this.ACTION_COMPARATOR_UCT);
            selectedAction = maximumValuedActions.get((int)(Math.random() * maximumValuedActions.size()));
        }

        if (this.children.containsKey(selectedAction)) {
            return this.children.get(selectedAction).selection(simulateAllDiceOutcomes);
        }
        return new ImmutablePair<>(this, selectedAction);
    }

    /**
     * Expands the current node with the given action and returns the created node.
     * Action must be null or a valid action that was not used for expansion with this node already.
     *
     * When action is null, returns this node (useful for doing MCTS when dealing with terminal nodes).
     *
     * @param action to apply
     * @return Game node that
     */
    public MctsNode expansion(HeimlichAndCoAction action) {
        if (action == null) {
            return this;
        }
        if (!game.isValidAction(action)) {
           throw new IllegalArgumentException("The given action must be valid.");
        }
        if (this.children.containsKey(action)) {
            throw new IllegalArgumentException("The current node was already expanded with the given action");
        }
        MctsNode newNode = new MctsNode(game.doAction(action), this);
        this.children.put(action, newNode);
        return newNode;
    }

    /**
     * Does backpropagation starting from the current node.
     * Therefore, always increases playouts and increases wins depending on win.
     *
     * @param win indicating whether the game was one or not (1 on win, 0 on loss).
     */
    public void backpropagation(int win) {
        if (win != 0 && win != 1) {
            throw new IllegalArgumentException("Win must be either 1 or 0");
        }
        this.playouts++;
        this.wins += win;
        if (this.parent != null) {
            this.parent.backpropagation(win);
        }
    }

    /**
     * Selects the best action to take in the current node.
     * This means taking the best action according to Q(s,a).
     *
     * @return the action with the best expected result.
     */
    public ImmutablePair<MctsNode, HeimlichAndCoAction> getBestChild() {
        Set<HeimlichAndCoAction> possibleActions = children.keySet();
        if (possibleActions.isEmpty()) {
            throw new RuntimeException("Could not find best child, because there are no children.");
        }
        List<HeimlichAndCoAction> maximumValuedActions = getMaximumValuedActions(possibleActions, this.ACTION_COMPARATOR_Q_s_a);
        HeimlichAndCoAction selectedAction = maximumValuedActions.get((int) (maximumValuedActions.size() * Math.random()));
        return new ImmutablePair<>(this.children.get(selectedAction), selectedAction);
    }

    /**
     * Calculates the UCT score of an action.
     * In the case that no playout has been done yet for an action, the maximum Double value is returned. This is in line with exploring
     * each state/action at least once before exploring a state/action twice.
     *
     * Note: The action has to be a valid action in the current game state.
     *
     * @param action for which UCT score should be calculated
     * @return UCT score of action
     */
    private double calculateUCT(HeimlichAndCoAction action) {
        if (action == null || !this.game.isValidAction(action)) {
            throw new IllegalArgumentException("Action must not be null, and allowed in the current state");
        }
        if (this.children.isEmpty()) {
            return Double.MAX_VALUE;
        }

        if(this.children.containsKey(action)) {
            MctsNode child = this.children.get(action);
            if (child.playouts == 0 || this.playouts == 0) { //this should never happen
                throw new RuntimeException("Illegal 0 value in calculateUCT");
            }
            double Q_s_a = ((double) child.wins / child.playouts);
            double N_s = this.playouts;
            double N_s_a = child.playouts;
            return Q_s_a + C * Math.sqrt(Math.log(N_s) / N_s_a);
        }
        return Double.MAX_VALUE;
    }

    /**
     * Calculates the Q(s,a) of a state (i.e. current game state) and an action. This is the expected percentage of wins when taking action a in state s.
     * Formula: #wins/ #playouts
     * Therefore the best score that can be achieved is 1, the worst is 0.
     *
     * Note: The action has to be contained in the children of this node.
     * @param action for which to calculate the percentage
     * @return expected percentage of wins when playing action in the current state
     */
    public double calculateQ_s_a_OfChild(HeimlichAndCoAction action) {
        if (!this.children.containsKey(action)) {
            throw new IllegalArgumentException("Action is not contained in children");
        }
        return ((double)children.get(action).wins) / children.get(action).playouts;
    }



    /**
     * Gets the actions which have the maximum value according to some comparator. If multiple actions have the same
     * (maximum) value, all of them will be contained in the returned list.
     * This method runs in O(n) where n is the number of actions
     *
     * @param actions possible actions in the current game state
     * @param comparator comparator which should be used to compare two actions (e.g. tree policy like UCT)
     * @return a List of actions which have the maximum value when compared with the given Comparator
     */
    private static List<HeimlichAndCoAction> getMaximumValuedActions(Set<HeimlichAndCoAction> actions, Comparator<HeimlichAndCoAction> comparator) {
        List<HeimlichAndCoAction> selectedActions = new LinkedList<>();
        for(HeimlichAndCoAction action: actions) {
            if (selectedActions.isEmpty()) { //this is only true in the first iteration
                selectedActions.add(action);
            } else if (comparator.compare(selectedActions.get(0), action) == 0) { //the current action is equal
                selectedActions.add(action);
            } else if (comparator.compare(selectedActions.get(0), action) < 0) { //the current action has a larger value
                //i.e. clear the list and add the current action to it
                selectedActions = new LinkedList<>();
                selectedActions.add(action);
            }
        }
        return selectedActions;
    }

    public int getPlayouts() {
        return this.playouts;
    }

    public int getWins() {return this.wins;}

}
