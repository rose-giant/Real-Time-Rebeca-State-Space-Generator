package org.rebecalang.transparentactormodelchecker.realtimerebeca.statementlevelsosrules;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.modeltransformer.ril.RILModel;
import org.rebecalang.modeltransformer.ril.corerebeca.rilinstruction.InstructionBean;
import org.rebecalang.transparentactormodelchecker.AbstractRealTimeSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.Action;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.TimeProgressAction;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaActorState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaSystemState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaAbstractTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaDeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaNondeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.utils.HybridRebecaStateSerializationUtils;

import java.util.ArrayList;
import java.util.Iterator;

import static java.lang.Math.min;

public class RealTimeRebecaResumeSOSRule extends AbstractRealTimeSOSRule<Pair<RealTimeRebecaActorState, InstructionBean>> {
    @Override
    public RealTimeRebecaAbstractTransition<Pair<RealTimeRebecaActorState, InstructionBean>> applyRule(Pair<RealTimeRebecaActorState, InstructionBean> source) {
        ArrayList<RealTimeRebecaAbstractTransition> transitions = new ArrayList<>();
        Pair<Float, Float> now = source.getFirst().getNow();
        Pair<Float, Float> resumeTime = source.getFirst().getResumeTime();
        if (resumeTime.getFirst().floatValue() == now.getFirst().floatValue() &&
                now.getSecond().floatValue() < resumeTime.getSecond().floatValue() ) {

            float lower = now.getSecond();
            float upper = resumeTime.getSecond();
            Pair<Float, Float> progressInterval = new Pair<>(lower, upper);
            RealTimeRebecaActorState backup = HybridRebecaStateSerializationUtils.clone(source.getFirst());
            backup.setResumeTime(new Pair<>(lower, resumeTime.getSecond()));
//            backup.setNow(progressInterval);
            RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>> result = new RealTimeRebecaDeterministicTransition<>();
            result.setDestination(new Pair<>(backup, source.getSecond()));
//            TimeProgressAction timeProgressAction = new TimeProgressAction();
//            timeProgressAction.setTimeProgress(progressInterval);
            result.setAction(Action.TAU);
            transitions.add(result);
        }

        if ((resumeTime.getFirst().floatValue() == now.getFirst().floatValue())) {
            RealTimeRebecaActorState backup = HybridRebecaStateSerializationUtils.clone(source.getFirst());
            backup.setResumeTime(backup.getNow());
            backup.setSuspent(false);
            RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>> result = new RealTimeRebecaDeterministicTransition<>();
            result.setDestination(new Pair<>(backup, source.getSecond()));
            result.setAction(Action.TAU);
            transitions.add(result);
        }

        if (transitions.size() == 1) {
            return transitions.get(0);
        }

        if (transitions.isEmpty()) return null;

        if (transitions.size() > 1) {
            RealTimeRebecaNondeterministicTransition result2 = new RealTimeRebecaNondeterministicTransition();
            for (RealTimeRebecaAbstractTransition transition : transitions) {
                RealTimeRebecaDeterministicTransition transition2 = (RealTimeRebecaDeterministicTransition) transition;
                Object dest = transition2.getDestination();
                Action act = transition2.getAction();
                result2.addDestination(act, dest);
            }

            return result2;
        }

        return null;
        //TODO: what if none is applicable?
//        RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>> result = new RealTimeRebecaDeterministicTransition<>();
//        result.setAction(null);
//        result.setDestination(source);
//        return result;
    }

    public RealTimeRebecaAbstractTransition<RealTimeRebecaSystemState> systemLevelResumePostpone(RealTimeRebecaSystemState source) {
        RealTimeRebecaAbstractTransition<RealTimeRebecaSystemState> result = null;
        ArrayList<RealTimeRebecaAbstractTransition> transitions = new ArrayList<>();

        for(String actorId : source.getActorsState().keySet()) {
            RealTimeRebecaActorState realTimeRebecaActorState = source.getActorState(actorId);
            RILModel rilModel = realTimeRebecaActorState.getRILModel();
            if (realTimeRebecaActorState.isSuspent()){
                RealTimeRebecaActorState backupActor = HybridRebecaStateSerializationUtils.clone(realTimeRebecaActorState);
                backupActor.setRILModel(realTimeRebecaActorState.getRILModel());
                RealTimeRebecaResumeSOSRule rebecaResumeSOSRule = new RealTimeRebecaResumeSOSRule();
                RealTimeRebecaAbstractTransition<Pair<RealTimeRebecaActorState, InstructionBean>> resumePostponeResult = rebecaResumeSOSRule.applyRule(new Pair<>(backupActor, new InstructionBean() {}));

                if (resumePostponeResult instanceof RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>>) {
//                    if (((RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>>) resumePostponeResult).getAction() == null){
////                        return null;
//                        continue;
//                    }
                    RealTimeRebecaSystemState backup = HybridRebecaStateSerializationUtils.clone(source);
                    result = new RealTimeRebecaDeterministicTransition<>();
                    RealTimeRebecaActorState newState = ((RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>>) resumePostponeResult).getDestination().getFirst();
                    newState.setRILModel(rilModel);
                    backup.setActorState(actorId, newState);
                    ((RealTimeRebecaDeterministicTransition<RealTimeRebecaSystemState>) result).setDestination(backup);
                    ((RealTimeRebecaDeterministicTransition<RealTimeRebecaSystemState>) result).setAction(((RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>>) resumePostponeResult).getAction());
//                    return result;
                    transitions.add(result);
                }
                else if(resumePostponeResult instanceof RealTimeRebecaNondeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>>) {
                    result = new RealTimeRebecaNondeterministicTransition<>();
                    Iterator<Pair<? extends Action, Pair<RealTimeRebecaActorState, InstructionBean>>> transitionsIterator = resumePostponeResult.getDestinations().iterator();
                    while(transitionsIterator.hasNext()) {
                        RealTimeRebecaSystemState backupb = HybridRebecaStateSerializationUtils.clone(source);
                        Pair<? extends Action, Pair<RealTimeRebecaActorState, InstructionBean>> transition = transitionsIterator.next();
                        RealTimeRebecaActorState actorState = transition.getSecond().getFirst();
                        actorState.setRILModel(rilModel);
                        backupb.setActorState(actorState.getId(), actorState);
                        RealTimeRebecaDeterministicTransition deterministicTransition = new RealTimeRebecaDeterministicTransition();
                        deterministicTransition.setAction(transition.getFirst());
                        deterministicTransition.setDestination(backupb);
                        transitions.add(deterministicTransition);
//                        ((RealTimeRebecaNondeterministicTransition<RealTimeRebecaSystemState>) result).addDestination(Action.TAU, backupb);
                    }
                }
            }
        }

        if (transitions.size() == 0) {
            return null;
        }

        if (transitions.size() == 1) {
            return transitions.get(0);
        }

        RealTimeRebecaNondeterministicTransition result2 = new RealTimeRebecaNondeterministicTransition();
        for (RealTimeRebecaAbstractTransition transition : transitions) {
            RealTimeRebecaDeterministicTransition transition2 = (RealTimeRebecaDeterministicTransition) transition;
            Object dest = transition2.getDestination();
            Action act = transition2.getAction();
            result2.addDestination(act, dest);
        }

        return result2;
    }

    @Override
    public RealTimeRebecaAbstractTransition<Pair<RealTimeRebecaActorState, InstructionBean>> applyRule(Action synchAction, Pair<RealTimeRebecaActorState, InstructionBean> source) {
        return null;
    }
}