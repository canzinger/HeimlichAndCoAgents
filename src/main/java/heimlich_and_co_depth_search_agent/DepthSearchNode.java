package heimlich_and_co_depth_search_agent;

import heimlich_and_co.HeimlichAndCo;
import heimlich_and_co.actions.HeimlichAndCoAction;
import heimlich_and_co.enums.Agent;

import java.util.*;

public class DepthSearchNode {

    /**
     * Saves how many nodes were created.
     * For info/statistics purposes
     */
    public static int totalNodeCount;

    /**
     * Saves all children. The keys are the possible actions in the current game state and the values are the resulting
     * child nodes.
     */
    private Map<HeimlichAndCoAction, DepthSearchNode> children;

    /**
     * the current game (state)
     */
    private final HeimlichAndCo game;

    /**
     * the depth of this node; 0 for root node
     */
    private final int depth;

    /**
     * the score (in terms of the minimax algorithm) of this node
     */
    private int score;

    public DepthSearchNode(HeimlichAndCo game, int depth) {
        this.game = new HeimlichAndCo(game);
        this.depth = depth;
        this.children = new HashMap<>();
        totalNodeCount++;
    }

    /**
     * Expands the current node until the termination depth is reached.
     * Expands the current node by adding all possible child nodes and then calling expand() recursively.
     *
     * @param terminationDepth depth for stopping expansion (for not timing out)
     */
    public void expand(int terminationDepth) {
        if (this.depth == terminationDepth) {
            return;
        }
        if (this.depth > terminationDepth) {
            throw new RuntimeException("Depth of node is too large.");
        }
        Set<HeimlichAndCoAction> possibleActions = game.getPossibleActions();
        for(HeimlichAndCoAction action : possibleActions) {
            DepthSearchNode newNode = new DepthSearchNode(game.doAction(action), this.depth + 1);
            this.children.put(action, newNode);
            newNode.expand(terminationDepth);
        }
    }

    //sets the score of all nodes in the tree, starting with the leaf nodes

    /**
     * Calculates and sets the score for each node in the tree.
     * Done by calculating the scores from the ground up (i.e. starting with the leaf nodes) until the root is reached.
     *
     * @param maximizingPlayer the playerId of the maximizing player (this should be the id of the AI agent playing)
     */
    public void evaluateTree(int maximizingPlayer) {
        if (children.isEmpty()) {
            this.score = evaluateGameState(maximizingPlayer);
            return;
        }

        for(DepthSearchNode node : children.values()) {
            node.evaluateTree(maximizingPlayer);
        }

        if (this.game.getCurrentPlayer() == maximizingPlayer) {
            DepthSearchNode maxNode = Collections.max(children.values(), Comparator.comparingInt(node -> evaluateGameState(maximizingPlayer)));
            this.score = maxNode.score;
        } else {
            DepthSearchNode minNode = Collections.min(children.values(), Comparator.comparingInt(node -> evaluateGameState(maximizingPlayer)));
            this.score = minNode.score;
        }
    }

    /**
     *
     * @return the best action to take in the current game state (i.e. the action with the highest score)
     */
    public HeimlichAndCoAction getMaxAction() {
        if (children == null || children.isEmpty()) {
            return null;
        }
        return Collections.max(children.keySet(), Comparator.comparingInt(action -> children.get(action).score));
    }

    //returned differenz von player agent zu bestem anderen agenten

    /**
     * Evaluates the current game, meaning finding a way to score a game state for a given player.
     * Returns a high/positive value if the game state is favorable for the maximizing player and a low/negative value
     * if it is not favorable.
     *
     * @param maximizingPlayer the playerId of the maximizing player (this should be the id of the AI agent playing)
     * @return a score depicting whether the game state is good or bad for the maximizing player
     */
    private int evaluateGameState(int maximizingPlayer) {
        Map<Agent, Integer> scores = game.getBoard().getScores();
        Agent playerAgent = game.getPlayersToAgentsMap().get(maximizingPlayer);
        int playerScore = scores.get(playerAgent);

        //get the agent with the highest score that is not the agent of the given player
        int maxScore = Integer.MIN_VALUE;
        for(Agent agent : scores.keySet()) {
            if (agent == game.getPlayersToAgentsMap().get(maximizingPlayer)) {
                continue;
            }
            maxScore= Integer.max(maxScore, scores.get(agent));
        }

        return playerScore - maxScore;
    }

}
