package org.rebecalang.transparentactormodelchecker.realtimerebeca.actorlevelsosrules;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.transparentactormodelchecker.AbstractRealTimeSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.Action;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaActorState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaAbstractTransition;

public class RealTimeRebecaActorEnvSync extends AbstractRealTimeSOSRule<RealTimeRebecaActorState> {
    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaActorState> applyRule(RealTimeRebecaActorState source) {
        Pair<Float, Float> now = source.getNow();
        Pair<Float, Float> resumeTime = source.getResumeTime();
        if (source.messageQueueIsEmpty() && now.getFirst().floatValue() == resumeTime.getFirst().floatValue()) {
            RealTimeRebecaEnvSync3SOSRule envSync3SOSRule = new RealTimeRebecaEnvSync3SOSRule();
            return envSync3SOSRule.applyRule(source);
        }
        if (now.getFirst().floatValue() < resumeTime.getFirst().floatValue() ) {
            RealTimeRebecaEnvSync2SOSRule envSync2SOSRule = new RealTimeRebecaEnvSync2SOSRule();
            return envSync2SOSRule.applyRule(source);
        }
        if (now.getFirst().floatValue() == resumeTime.getFirst().floatValue()) {
            RealTimeRebecaEnvSync1SOSRule envSync1SOSRule = new RealTimeRebecaEnvSync1SOSRule();
            return envSync1SOSRule.applyRule(source);
        }

        return null;
    }

    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaActorState> applyRule(Action synchAction, RealTimeRebecaActorState source) {
        return null;
    }
}
