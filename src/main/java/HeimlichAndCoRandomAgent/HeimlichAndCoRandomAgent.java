package HeimlichAndCoRandomAgent;

import HeimlichAndCo.Actions.HeimlichAndCoAction;
import HeimlichAndCo.HeimlichAndCo;
import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;

import java.util.concurrent.TimeUnit;

public class HeimlichAndCoRandomAgent extends AbstractGameAgent<HeimlichAndCo, HeimlichAndCoAction> implements GameAgent<HeimlichAndCo, HeimlichAndCoAction> {

    public HeimlichAndCoRandomAgent(Logger logger) {
        super(logger);
    }

    @Override
    public HeimlichAndCoAction computeNextAction(HeimlichAndCo heimlichAndCo, long l, TimeUnit timeUnit) {
        super.log.inf("Selecting random action");
        HeimlichAndCoAction[] actions = heimlichAndCo.getPossibleActions().toArray(new HeimlichAndCoAction[0]);
        return actions[(int) (Math.random() * actions.length)];
    }

    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Override
    public void ponderStart() {
        super.ponderStart();
    }

    @Override
    public void ponderStop() {
        super.ponderStop();
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}
