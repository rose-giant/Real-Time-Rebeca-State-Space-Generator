package org.rebecalang.transparentactormodelchecker.realtimerebeca.compositionlevelsosrules;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.transparentactormodelchecker.TransparentActorStateSpace;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.networklevelsosrules.RealTimeRebecaNetworkEnvSync3SOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.statementlevelsosrules.RealTimeRebecaResumeSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.Action;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.MessageAction;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.TimeProgressAction;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaActorState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaSystemState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaAbstractTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaDeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaNondeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.utils.HybridRebecaStateSerializationUtils;

import java.util.*;

public class ApplySystemLevelRules {
    Map<RealTimeRebecaSystemState, Integer> stateIds = new IdentityHashMap<>();
    int nextStateId = 0;

    private int getStateId(RealTimeRebecaSystemState s) {
        return stateIds.computeIfAbsent(s, k -> nextStateId++);
    }

    ArrayList<RealTimeRebecaSystemState> states = new ArrayList<>();
    TransparentActorStateSpace transparentActorStateSpace = new TransparentActorStateSpace();
    RealTimeRebecaSystemState initialState;
    public ApplySystemLevelRules(RealTimeRebecaSystemState initialState) {
        levelExecuteStatementSOSRule = new RealTimeRebecaCompositionLevelExecuteStatementSOSRule();
        this.initialState = initialState;
        startApplyingRules(initialState);
        dot.writeToFile("output.dot");
        System.out.println(currentStateIdx);
    }

    RealTimeRebecaCompositionLevelExecuteStatementSOSRule levelExecuteStatementSOSRule;
    RealTimeRebecaCompositionLevelNetworkDeliverySOSRule networkDeliverySOSRule =
            new RealTimeRebecaCompositionLevelNetworkDeliverySOSRule();

    public void startApplyingRules(RealTimeRebecaSystemState initialState) {
        RealTimeRebecaDeterministicTransition<RealTimeRebecaSystemState> t =
                new RealTimeRebecaDeterministicTransition<>();

        t.setDestination(initialState);
        t.setAction(Action.TAU);

        getStateId(initialState); // ensure s0 exists

        runSystemRules(initialState, t);
    }

    private DotExporter dot = new DotExporter();
    public void printState(
            int sourceId,
            String transitionType,
            Object action,
            RealTimeRebecaSystemState destState
    ) {
        int destId = getStateId(destState);
        currentStateIdx++;

        String actionStr = "TAU";
        if (action instanceof MessageAction) {
            actionStr = ((MessageAction) action).getActionLabel();
        } else if (action instanceof TimeProgressAction) {
            actionStr = ((TimeProgressAction) action)
                    .getIntervalTimeProgress().toString();
        }

        dot.addTransition(sourceId, transitionType, actionStr, destId);
    }

    public void runSystemRules(RealTimeRebecaSystemState sourceState,
                               RealTimeRebecaAbstractTransition<RealTimeRebecaSystemState> executionResult) {

        if (executionResult instanceof RealTimeRebecaDeterministicTransition) {
            RealTimeRebecaDeterministicTransition<RealTimeRebecaSystemState> t =
                    (RealTimeRebecaDeterministicTransition<RealTimeRebecaSystemState>) executionResult;
            RealTimeRebecaSystemState dest = HybridRebecaStateSerializationUtils.clone(t.getDestination());
            printState(getStateId(sourceState), "", t.getAction(), dest);
            if (dest.getNow().getFirst() >= dest.getInputInterval().getSecond()) {
                return;
            }

            RealTimeRebecaAbstractTransition<RealTimeRebecaSystemState> next = runApplicableRule(dest);
            runSystemRules(dest, next);
        }

        else if (executionResult instanceof RealTimeRebecaNondeterministicTransition) {
            RealTimeRebecaNondeterministicTransition<RealTimeRebecaSystemState> t =
                    (RealTimeRebecaNondeterministicTransition<RealTimeRebecaSystemState>) executionResult;

            int sourceId = getStateId(sourceState);

            List<RealTimeRebecaSystemState> successors = new ArrayList<>();

            // Phase 1: print all nondet edges
            for (Pair<? extends Action, RealTimeRebecaSystemState> p : t.getDestinations()) {
                RealTimeRebecaSystemState dest = HybridRebecaStateSerializationUtils.clone(p.getSecond());
                printState(sourceId, "", p.getFirst(), dest);
                if (dest.getNow().getFirst() <= dest.getInputInterval().getSecond()) {
                    successors.add(dest);
                }
            }

            // Phase 2: explore
            for (RealTimeRebecaSystemState dest : successors) {
                RealTimeRebecaAbstractTransition<RealTimeRebecaSystemState> next = runApplicableRule(dest);
                runSystemRules(dest, next);
            }
        }

    }

    int currentStateIdx = 0;
    public RealTimeRebecaAbstractTransition<RealTimeRebecaSystemState> runApplicableRule(RealTimeRebecaSystemState state) {
        RealTimeRebecaAbstractTransition<RealTimeRebecaSystemState> result;

        // ===== highest priority: resume =====
//        if (state.thereIsSuspension()) {
//            RealTimeRebecaResumeSOSRule rule = new RealTimeRebecaResumeSOSRule();
//            result = rule.systemLevelResumePostpone(state);
//            if (result != null) {
//                return result;
//            }
//        }

        // ===== then execute statements =====
        if (systemCanExecuteStatements(state) || systemCanResume(state)) {
            result = levelExecuteStatementSOSRule.applyRule(state);
            if (result != null) {
                return result;
            }
        }

        // ===== take message =====
        RealTimeRebecaCompositionLevelTakeMessageSOSRule takeRule =
                new RealTimeRebecaCompositionLevelTakeMessageSOSRule();
        result = takeRule.applyRule(state);
        if (result != null) {
            return result;
        }

        // ===== network delivery =====
        if (!state.getNetworkState().getReceivedMessages().isEmpty()) {
            result = networkDeliverySOSRule.applyRule(state);
            if (result != null) {
                return result;
            }
        }

        // ===== environment progress =====
        RealTimeRebecaCompositionLevelEnvProgressSOSRule envRule = new RealTimeRebecaCompositionLevelEnvProgressSOSRule();
        result = envRule.applyRule(state);
        return result;
    }

    public boolean systemCanExecuteStatements(RealTimeRebecaSystemState initialState) {
        for(String actorId : initialState.getActorsState().keySet()) {
            RealTimeRebecaActorState realTimeRebecaActorState = initialState.getActorState(actorId);
            if ((!realTimeRebecaActorState.noScopeInstructions()) && !realTimeRebecaActorState.isSuspent()) {
                return true;
            }
        }

        return false;
    }

    public boolean systemCanResume(RealTimeRebecaSystemState initialState) {
        for(String actorId : initialState.getActorsState().keySet()) {
            RealTimeRebecaActorState realTimeRebecaActorState = initialState.getActorState(actorId);
            if (realTimeRebecaActorState.getResumeTime().getFirst().floatValue() ==
                    realTimeRebecaActorState.getNow().getFirst().floatValue() && ( (!realTimeRebecaActorState.noScopeInstructions()) ||
                    realTimeRebecaActorState.isSuspent()) ) {
                return true;
            }
        }

        return false;
    }
}
