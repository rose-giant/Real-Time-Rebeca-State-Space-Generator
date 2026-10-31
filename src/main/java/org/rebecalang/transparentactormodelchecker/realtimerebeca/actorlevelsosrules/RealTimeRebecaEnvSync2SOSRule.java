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
public class RealTimeRebecaEnvSync2SOSRule extends AbstractRealTimeSOSRule<RealTimeRebecaActorState> {
    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaActorState> applyRule(RealTimeRebecaActorState source) {
        Pair<Float, Float> resumeTime = source.getResumeTime();
        Pair<Float, Float> now = source.getNow();
        float lower = resumeTime.getFirst(), upper = resumeTime.getSecond();
//        if (now.getFirst() < resumeTime.getFirst()) {
//            lower = now.getSecond();
//            upper = resumeTime.getSecond();
//        }

        Pair<Float, Float> progressInterval = new Pair<>(lower, upper);
        RealTimeRebecaActorState backup = HybridRebecaStateSerializationUtils.clone(source);
//        backup.setResumeTime(new Pair<>(lower, resumeTime.getSecond()));
        backup.setNow(progressInterval);
        RealTimeRebecaDeterministicTransition<RealTimeRebecaActorState> result = new RealTimeRebecaDeterministicTransition<>();
        result.setDestination(source);
        TimeProgressAction timeProgressAction = new TimeProgressAction();
        timeProgressAction.setTimeProgress(progressInterval);
        result.setAction(timeProgressAction);
        return result;
    }

    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaActorState> applyRule(Action synchAction, RealTimeRebecaActorState source) {
        return null;
    }
}
