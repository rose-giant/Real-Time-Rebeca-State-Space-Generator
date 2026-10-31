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
public class RealTimeRebecaEnvSync1SOSRule extends AbstractRealTimeSOSRule<RealTimeRebecaActorState> {

    public RealTimeRebecaDeterministicTransition<RealTimeRebecaActorState> applyRule(RealTimeRebecaActorState source) {
        RealTimeRebecaActorState backup = HybridRebecaStateSerializationUtils.clone(source);
        Pair<Float, Float> now = backup.getNow();
        Pair<Float, Float> resumeTime = backup.getResumeTime();
        Pair<Float, Float> progress = new Pair<>();

//        if (
//                (now.getFirst().floatValue() != resumeTime.getFirst().floatValue()) ||
//                (now.getFirst().floatValue() == resumeTime.getFirst().floatValue() &&
//                        now.getFirst().floatValue() == now.getSecond().floatValue() &&
//                        resumeTime.getFirst().floatValue() != resumeTime.getSecond().floatValue())
//        ) {
//            progress.setFirst(resumeTime.getFirst());
//            progress.setSecond(resumeTime.getSecond());
        progress.setFirst(now.getSecond());
        progress.setSecond(resumeTime.getSecond());
//        }

        //considered the maximum time progress:
        backup.setNow(progress);
//        backup.setResumeTime(new Pair<>(progress.getFirst(), resumeTime.getSecond()));
        backup.setResumeTime(progress);
        TimeProgressAction action = new TimeProgressAction();
        action.setTimeProgress(progress);

        RealTimeRebecaDeterministicTransition<RealTimeRebecaActorState> result =
                new RealTimeRebecaDeterministicTransition<RealTimeRebecaActorState>();
        result.setAction(action);
        result.setDestination(backup);

        return result;
    }

    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaActorState> applyRule(Action synchAction, RealTimeRebecaActorState source) {
        return null;
    }
}
