package heimlich_and_co_depth_search_agent;


import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import heimlich_and_co.HeimlichAndCo;
import heimlich_and_co.actions.HeimlichAndCoAction;
import heimlich_and_co.enums.Agent;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class HeimlichAndCoDepthSearchAgent extends AbstractGameAgent<HeimlichAndCo, HeimlichAndCoAction> implements GameAgent<HeimlichAndCo, HeimlichAndCoAction> {

    /**
     * Determines the depth the tree will be expanded to during the expand() method.
     */
    private static final int TERMINATION_DEPTH = 3;

    public HeimlichAndCoDepthSearchAgent(Logger logger) {
        super(logger);
    }

    @Override
    public HeimlichAndCoAction computeNextAction(HeimlichAndCo game, long l, TimeUnit timeUnit) {
        log.deb("MctsAgent: Computing next action\n");
        super.setTimers(l, timeUnit);

        Set<HeimlichAndCoAction> possibleActions = game.getPossibleActions();
        if (possibleActions.size() == 1) {
            return game.getPossibleActions().iterator().next();
        }

        try {
            log.deb("Adding information to game\n");
            addInformationToGame(game);
            log.deb("Creating and expanding tree\n");
            DepthSearchNode.setTotalNodeCount(0);
            DepthSearchNode root = new DepthSearchNode(game, 0);
            root.expand(TERMINATION_DEPTH);
            log.deb("Evaluating tree\n");
            root.evaluateTree(this.playerId);
            log.deb("Getting max action\n");
            log.inf("Generated tree with " + DepthSearchNode.getTotalNodeCount() + " total nodes.\n");
            return root.getMaxAction();
        } catch (Exception ex) {
            log.err(ex);
            log.err("An error occurred while calculating the best action. Playing a random action.");
        }

        //If an exception is encountered, we play a random action s.t. we do not automatically lose the game
        HeimlichAndCoAction[] actions = game.getPossibleActions().toArray(new HeimlichAndCoAction[0]);
        return actions[super.random.nextInt(actions.length)];
    }

    /**
     * Adds information that was removed by the game (i.e. hidden information).
     * Therefore, adds entries to the map which maps agents to players and entries to the map mapping the cards of players.
     * The agents are randomly assigned to players. And players are assumed to have no cards.
     *
     * @param game
     */
    private void addInformationToGame(HeimlichAndCo game) {
        Map<Integer, Agent> playersToAgentsMap = game.getPlayersToAgentsMap();
        List<Agent> unassignedAgents = Arrays.asList(game.getBoard().getAgents());
        unassignedAgents = new LinkedList<>(unassignedAgents);
        unassignedAgents.remove(playersToAgentsMap.get(this.playerId));
        for (int i = 0; i < game.getNumberOfPlayers(); i++) {
            if (i == this.playerId) {
                continue;
            }
            Agent chosenAgent = unassignedAgents.get(random.nextInt(unassignedAgents.size()));
            playersToAgentsMap.put(i, chosenAgent);
            unassignedAgents.remove(chosenAgent);
            if (game.isWithCards()) {
                game.getCards().put(i, new LinkedList<>()); //other players do not get cards
            }
        }
    }


}
