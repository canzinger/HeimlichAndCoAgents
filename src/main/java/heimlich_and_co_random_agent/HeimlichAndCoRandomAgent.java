package heimlich_and_co_random_agent;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import heimlich_and_co.HeimlichAndCo;
import heimlich_and_co.actions.HeimlichAndCoAction;

import java.util.concurrent.TimeUnit;

public class HeimlichAndCoRandomAgent extends AbstractGameAgent<HeimlichAndCo, HeimlichAndCoAction> implements GameAgent<HeimlichAndCo, HeimlichAndCoAction> {

    public HeimlichAndCoRandomAgent(Logger logger) {
        super(logger);
    }

    @Override
    public HeimlichAndCoAction computeNextAction(HeimlichAndCo heimlichAndCo, long l, TimeUnit timeUnit) {
        super.log.inf("Selecting random action\n");
        HeimlichAndCoAction[] actions = heimlichAndCo.getPossibleActions().toArray(new HeimlichAndCoAction[0]);
        return actions[super.random.nextInt(actions.length)];
    }
}
