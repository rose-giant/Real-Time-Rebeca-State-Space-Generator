package org.rebecalang.transparentactormodelchecker.realtimerebeca.compositionlevelsosrules;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.modelchecker.corerebeca.RebecaRuntimeInterpreterException;
import org.rebecalang.modeltransformer.ril.corerebeca.rilinstruction.EndMethodInstructionBean;
import org.rebecalang.modeltransformer.ril.corerebeca.rilinstruction.InstructionBean;
import org.rebecalang.modeltransformer.ril.corerebeca.rilinstruction.JumpIfNotInstructionBean;
import org.rebecalang.modeltransformer.ril.corerebeca.rilinstruction.PushARInstructionBean;
import org.rebecalang.transparentactormodelchecker.AbstractRealTimeSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.actorlevelsosrules.RealTimeRebecaInternalProgressSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.networklevelsosrules.RealTimeRebecaNetworkReceiveSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.statementlevelsosrules.RealTimeRebecaResumeSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.Action;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.MessageAction;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.TimeProgressAction;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaActorState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaNetworkState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaSystemState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaAbstractTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaDeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaNondeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.utils.HybridRebecaStateSerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class RealTimeRebecaCompositionLevelExecuteStatementSOSRule extends AbstractRealTimeSOSRule<RealTimeRebecaSystemState> {

    @Autowired
    RealTimeRebecaInternalProgressSOSRule hybridRebecaActorLevelExecuteStatementSOSRule = new RealTimeRebecaInternalProgressSOSRule();

    @Autowired
    RealTimeRebecaNetworkReceiveSOSRule hybridRebecaNetworkLevelReceiveMessageSOSRule = new RealTimeRebecaNetworkReceiveSOSRule();

    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaSystemState> applyRule(RealTimeRebecaSystemState source) {
        ArrayList<RealTimeRebecaAbstractTransition> transitions = new ArrayList<>();
        ArrayList<RealTimeRebecaAbstractTransition> execTransitions = new ArrayList<>();
        RealTimeRebecaSystemState backup = HybridRebecaStateSerializationUtils.clone(source);

        for(String actorId : backup.getActorsState().keySet()) {
            RealTimeRebecaActorState realTimeRebecaActorState = source.getActorState(actorId);
            realTimeRebecaActorState.setNow(source.getNow());

            if (realTimeRebecaActorState.isSuspent()) {
                EndMethodInstructionBean endMethodInstructionBean = new EndMethodInstructionBean();
                RealTimeRebecaResumeSOSRule rebecaResumeSOSRule = new RealTimeRebecaResumeSOSRule();
                RealTimeRebecaAbstractTransition<Pair<RealTimeRebecaActorState, InstructionBean>> executionResult2 =
                        rebecaResumeSOSRule.applyRule(new Pair<>(realTimeRebecaActorState, endMethodInstructionBean));

                if(executionResult2 instanceof RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>>) {
                    RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>> transition =
                            (RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>>)executionResult2;
                    transition.setAction(((RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>>) executionResult2).getAction());
                    RealTimeRebecaActorState as =((RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>>) executionResult2).getDestination().getFirst();
                    as.setRILModel(realTimeRebecaActorState.getRILModel());
                    backup.setActorState(actorId, as);
                    transitions.add(new RealTimeRebecaDeterministicTransition(transition.getAction(), backup));
                }
                else if(executionResult2 instanceof RealTimeRebecaNondeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>>) {
                    RealTimeRebecaNondeterministicTransition result = new RealTimeRebecaNondeterministicTransition<RealTimeRebecaSystemState>();
                    Iterator<Pair<? extends Action, Pair<RealTimeRebecaActorState, InstructionBean>>> transitionsIterator = executionResult2.getDestinations().iterator();
                    while(transitionsIterator.hasNext()) {
                        RealTimeRebecaSystemState backup1 = HybridRebecaStateSerializationUtils.clone(source);
                        Pair<? extends Action, Pair<RealTimeRebecaActorState, InstructionBean>> transition = transitionsIterator.next();
                        RealTimeRebecaActorState actorState = transition.getSecond().getFirst();
                        actorState.setRILModel(realTimeRebecaActorState.getRILModel());
                        backup1.setActorState(actorState.getId(), actorState);
                        transitions.add(new RealTimeRebecaDeterministicTransition(transition.getFirst(), backup1));
//                            result.addDestination(transition.getFirst(), backup1);
//                            transitions.add(result);
                    }
                }
                continue;
            }

            else if (realTimeRebecaActorState.noScopeInstructions()) {
                continue;
            }

            if (!realTimeRebecaActorState.isSuspent()) {
                for (int i = 0; i <= realTimeRebecaActorState.getSigma().size() ; i++) {
                    if (realTimeRebecaActorState.isSuspent()) {
                        break;
                    }
                    RealTimeRebecaAbstractTransition<RealTimeRebecaActorState> executionResult =
                            hybridRebecaActorLevelExecuteStatementSOSRule.applyRule(realTimeRebecaActorState);

                    if(executionResult instanceof RealTimeRebecaDeterministicTransition<RealTimeRebecaActorState>) {
                        RealTimeRebecaDeterministicTransition<RealTimeRebecaActorState> transition =
                                (RealTimeRebecaDeterministicTransition<RealTimeRebecaActorState>)executionResult;
                        if(transition.getAction() instanceof MessageAction) {
                            RealTimeRebecaDeterministicTransition<RealTimeRebecaNetworkState> networkTransition =
                                    (RealTimeRebecaDeterministicTransition<RealTimeRebecaNetworkState>)
                                            hybridRebecaNetworkLevelReceiveMessageSOSRule.applyRule(transition.getAction(), backup.getNetworkState());
                            backup.setNetworkState(networkTransition.getDestination());
                        } else {
                            transition.setAction(Action.TAU);
                        }
                        backup.setActorState(actorId, ((RealTimeRebecaDeterministicTransition<RealTimeRebecaActorState>) executionResult).getDestination());
                        execTransitions.add(new RealTimeRebecaDeterministicTransition(transition.getAction(), backup));
                    }
                    else if(executionResult instanceof RealTimeRebecaNondeterministicTransition<RealTimeRebecaActorState>) {
                        RealTimeRebecaNondeterministicTransition result = new RealTimeRebecaNondeterministicTransition<RealTimeRebecaSystemState>();
                        Action action = Action.TAU;
                        Iterator<Pair<? extends Action, RealTimeRebecaActorState>> transitionsIterator = executionResult.getDestinations().iterator();
                        while(transitionsIterator.hasNext()) {
                            RealTimeRebecaSystemState backup1 = HybridRebecaStateSerializationUtils.clone(source);
                            Pair<? extends Action, RealTimeRebecaActorState> transition = transitionsIterator.next();
                            RealTimeRebecaActorState actorState = transition.getSecond();
                            backup1.setActorState(actorState.getId(), actorState);
                            transitions.add(new RealTimeRebecaDeterministicTransition(transition.getFirst(), backup1));
                            if(transition.getFirst() instanceof MessageAction) {
                                action = (MessageAction) transition.getFirst();
                                RealTimeRebecaDeterministicTransition<RealTimeRebecaNetworkState> networkTransition =
                                        (RealTimeRebecaDeterministicTransition<RealTimeRebecaNetworkState>)
                                                hybridRebecaNetworkLevelReceiveMessageSOSRule.applyRule(action, backup1.getNetworkState());
                                backup1.setNetworkState(networkTransition.getDestination());
                            }
                            result.addDestination(action, backup1);
                        }

                        return result;
                    }

                    else {
                        throw new RebecaRuntimeInterpreterException("Unknown actor transition type");
                    }
                }
            }
        }

        if (transitions.isEmpty() && execTransitions.isEmpty()) return new RealTimeRebecaDeterministicTransition<>(Action.TAU, backup);
        if (transitions.size() > 1) {
            RealTimeRebecaNondeterministicTransition result2 = new RealTimeRebecaNondeterministicTransition();
            for (RealTimeRebecaAbstractTransition transition : transitions) {
                RealTimeRebecaDeterministicTransition transition2 = (RealTimeRebecaDeterministicTransition) transition;
                Object dest = transition2.getDestination();
                Action act = transition2.getAction();
                result2.addDestination(act, dest);
            }

            if (!execTransitions.isEmpty()) {
                RealTimeRebecaDeterministicTransition dest = (RealTimeRebecaDeterministicTransition) execTransitions.get(execTransitions.size() - 1);
                result2.addDestination(dest.getAction(), dest.getDestination());
            }
            return result2;
        }

        if(transitions.size() == 1) {
            if (execTransitions.isEmpty()) return transitions.get(0);

            RealTimeRebecaNondeterministicTransition result2 = new RealTimeRebecaNondeterministicTransition();
            RealTimeRebecaDeterministicTransition dest = (RealTimeRebecaDeterministicTransition) execTransitions.get(execTransitions.size() - 1);
            result2.addDestination(Action.TAU ,dest.getDestination());
            RealTimeRebecaDeterministicTransition transition2 = (RealTimeRebecaDeterministicTransition) transitions.get(0);
            result2.addDestination(transition2.getAction(), transition2.getDestination());
            return result2;
        }

        if (!execTransitions.isEmpty()) {
            RealTimeRebecaDeterministicTransition dest = (RealTimeRebecaDeterministicTransition) execTransitions.get(execTransitions.size() - 1);
            return dest;
        }
        return null;
//        return transitions.get(transitions.size() - 1);
    }

    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaSystemState> applyRule(Action synchAction, RealTimeRebecaSystemState source) {
        return null;
    }

}


//            HybridRebecaNetworkState hybridRebecaNetworkState = source.getNetworkState();
//            HybridRebecaAbstractTransition<HybridRebecaNetworkState> executionResult =
//                    hybridRebecaNetworkLevelReceiveMessageSOSRule.applyRule(hybridRebecaNetworkState);
//            if(executionResult instanceof HybridRebecaDeterministicTransition<HybridRebecaNetworkState>) {
//                HybridRebecaDeterministicTransition<HybridRebecaNetworkState> transition =
//                        (HybridRebecaDeterministicTransition<HybridRebecaNetworkState>)executionResult;
//                if(transition.getAction() instanceof MessageAction) {
//                    hybridRebecaNetworkTransferSOSRule.applyRule(
//                            transition.getAction(), source.getNetworkState());
//                }
//                transitions.addDestination(transition.getAction(), source);
//            } else if(executionResult instanceof HybridRebecaNondeterministicTransition<HybridRebecaNetworkState>) {
//                Iterator<Pair<? extends Action, HybridRebecaNetworkState>> transitionsIterator =
//                        ((HybridRebecaNondeterministicTransition<HybridRebecaNetworkState>) executionResult).getDestinations().iterator();
//                while(transitionsIterator.hasNext()) {
//                    Pair<? extends Action, HybridRebecaNetworkState> transition = transitionsIterator.next();
//                    HybridRebecaNetworkState networkState = transition.getSecond();
//                    source.setNetworkState(networkState);
//                    transitions.addDestination(transition.getFirst(), source);
//                    if(transitionsIterator.hasNext()) {
//                        source = HybridRebecaStateSerializationUtils.clone(backup);
//                    }
//                }
//            } else {
//                throw new RebecaRuntimeInterpreterException("Unknown network transition type");
//            }