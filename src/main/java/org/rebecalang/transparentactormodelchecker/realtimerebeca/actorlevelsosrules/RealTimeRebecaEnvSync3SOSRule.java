package org.rebecalang.transparentactormodelchecker.realtimerebeca.actorlevelsosrules;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.transparentactormodelchecker.AbstractRealTimeSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.Action;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.TimeProgressAction;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaActorState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaAbstractTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaDeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.utils.HybridRebecaStateSerializationUtils;
import org.springframework.stereotype.Component;

@Component
public class RealTimeRebecaEnvSync3SOSRule extends AbstractRealTimeSOSRule<RealTimeRebecaActorState> {

    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaActorState> applyRule(RealTimeRebecaActorState source) {
        Pair<Float, Float> now = source.getNow();
//        TODO: check it here?? or somewhere at a higher level
        if (!source.isSuspent() && source.messageQueueIsEmpty()) {

        }
        RealTimeRebecaActorState backup = HybridRebecaStateSerializationUtils.clone(source);
        float upperBound = Float.MAX_VALUE;
        float lowerBound = Float.MAX_VALUE;
        backup.setNow(new Pair<>(lowerBound, upperBound));
        RealTimeRebecaDeterministicTransition<RealTimeRebecaActorState> result = new RealTimeRebecaDeterministicTransition<>();
        result.setDestination(backup);
        TimeProgressAction timeAction = new TimeProgressAction();
        timeAction.setTimeProgress(new Pair<>(lowerBound, upperBound));
        result.setAction(timeAction);
        return result;
    }

    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaActorState> applyRule(Action synchAction, RealTimeRebecaActorState source) {
        return null;
    }
}
