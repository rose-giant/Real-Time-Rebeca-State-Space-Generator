package org.rebecalang.transparentactormodelchecker.realtimerebeca.compositionlevelsosrules;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.transparentactormodelchecker.AbstractRealTimeSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.actorlevelsosrules.RealTimeRebecaActorEnvSync;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.networklevelsosrules.RealTimeRebecaNetworkEnvSync1SOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.networklevelsosrules.RealTimeRebecaNetworkEnvSyncSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.Action;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.TimeProgressAction;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaActorState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaNetworkState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaSystemState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaAbstractTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaDeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaNondeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.utils.HybridRebecaStateSerializationUtils;

import java.util.*;

public class RealTimeRebecaCompositionLevelEnvProgressSOSRule extends AbstractRealTimeSOSRule<RealTimeRebecaSystemState> {
    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaSystemState> applyRule(RealTimeRebecaSystemState source) {
//        return applyGlobalRule(source);
        return applyLocalRule(source);
    }

    private static RealTimeRebecaDeterministicTransition<RealTimeRebecaSystemState> applyLocalRule(RealTimeRebecaSystemState source) {
//        ArrayList<Pair<Float, Float>> progressIntervals = new ArrayList<>();
//        progressIntervals.add(getNetworkSyncInterval(source));

        ArrayList<Pair<Float, Float>> progressIntervals = new ArrayList<>();
        progressIntervals.addAll(getNetworkSyncIntervals(source));

        RealTimeRebecaSystemState backup = HybridRebecaStateSerializationUtils.clone(source);
        for(String actorId : backup.getActorsState().keySet()) {
            RealTimeRebecaActorState actorState = source.getActorState(actorId);
            progressIntervals.add(getActorSyncInterval(actorState));
        }

        // compute the correct intersection
        float left = Float.MAX_VALUE;
        float right = Float.MAX_VALUE;
        float maxLeft = Float.MIN_VALUE;
        for (Pair<Float, Float> interval: progressIntervals) {
            if (interval.getSecond() < right) {
                right = interval.getSecond();
            }
        }

        for (Pair<Float, Float> interval: progressIntervals) {
            if (interval.getSecond() < left) {
                left = interval.getFirst();
            }
            if (interval.getFirst().floatValue() < right && interval.getFirst().floatValue() != left) {
                //maxLeft = interval.getFirst();
                right = interval.getFirst();
            }
        }

//        right = Math.min(maxLeft, right);
        Pair<Float, Float> newNow = new Pair<>(left, right);
        RealTimeRebecaNetworkState networkState = source.getNetworkState();
        networkState.setNow(newNow);
        backup.setNetworkState(networkState);

//        TODO: should I remove this??
        for(String actorId : backup.getActorsState().keySet()) {
            RealTimeRebecaActorState actorState = backup.getActorState(actorId);
            actorState.setNow(newNow);
            if (actorState.getResumeTime().getFirst().floatValue() < newNow.getFirst().floatValue()) {
                actorState.setResumeTime(new Pair<>(newNow.getFirst(), actorState.getResumeTime().getSecond()));
            }
            if (!actorState.isSuspent()) {
                actorState.setResumeTime(newNow);
            }
//            if (actorState.isSuspent() && actorState.getResumeTime().getFirst().floatValue() < newNow.getFirst().floatValue()) {
//                actorState.setResumeTime(new Pair<>(newNow.getFirst().floatValue(), actorState.getResumeTime().getSecond().floatValue()));
//            }
        }

        RealTimeRebecaDeterministicTransition<RealTimeRebecaSystemState> result = new RealTimeRebecaDeterministicTransition<>();
        TimeProgressAction timeAction = new TimeProgressAction();
        timeAction.setTimeProgress(newNow);
        result.setAction(timeAction);
        backup.setNow(newNow);
        result.setDestination(backup);

        return result;
    }

    public RealTimeRebecaAbstractTransition<RealTimeRebecaSystemState> applyGlobalRule(RealTimeRebecaSystemState source) {
        RealTimeRebecaSystemState backup = HybridRebecaStateSerializationUtils.clone(source);
        RealTimeRebecaNetworkEnvSync1SOSRule networkEnvSync1SOSRule = new RealTimeRebecaNetworkEnvSync1SOSRule();
        RealTimeRebecaNetworkState networkState = source.getNetworkState();
        ArrayList<Float> bounds = networkState.getAllBounds(networkState);
        for(String actorId: source.getActorsIds()) {
            RealTimeRebecaActorState actorState = source.getActorState(actorId);
            if (actorState.isSuspent()) {
                bounds.add(actorState.getResumeTime().getFirst());
                bounds.add(actorState.getResumeTime().getSecond());
            }
        }

        List<Float> uniqueSortedBounds = bounds.stream().distinct().sorted().toList();
        bounds.clear();
        for (Float element: uniqueSortedBounds) {
            bounds.add(element);
        }

        float left = Float.MAX_VALUE;
        float right = Float.MAX_VALUE;

        if (bounds.size() > 1) {
//            if (backup.getNow().getFirst().floatValue() == bounds.get(0).floatValue() && backup.getNow().getSecond().floatValue() != 0) {
//                left = bounds.get(1);
//                right = bounds.get(2);
//            } else
            {
                left = bounds.get(0);
                right = bounds.get(1);
            }
        }
//        if (left < Float.MAX_VALUE)
//            left = Math.round(left * 10f) / 10f;
//        if (right < Float.MAX_VALUE)
//            right = Math.round(right * 10f) / 10f;

        Pair<Float, Float> newNow = new Pair<>(left, right);
        networkState.setNow(newNow);
        backup.setNetworkState(networkState);

        for(String actorId : backup.getActorsState().keySet()) {
            RealTimeRebecaActorState actorState = backup.getActorState(actorId);
            actorState.setNow(newNow);
            if (actorState.getResumeTime().getFirst().floatValue() < newNow.getFirst().floatValue()) {
                actorState.setResumeTime(new Pair<>(newNow.getFirst(), actorState.getResumeTime().getSecond()));
            }
            if (!actorState.isSuspent()) {
                actorState.setResumeTime(newNow);
            } else if(actorState.getResumeTime().getFirst().floatValue() == left &&
                    actorState.getResumeTime().getSecond().floatValue() == right) {
                actorState.setSuspent(false);
            }
        }

        RealTimeRebecaDeterministicTransition<RealTimeRebecaSystemState> result = new RealTimeRebecaDeterministicTransition<>();
        TimeProgressAction timeAction = new TimeProgressAction();
        timeAction.setTimeProgress(newNow);
        result.setAction(timeAction);
        backup.setNow(newNow);
        result.setDestination(backup);

        return result;
    }

    private static Pair<Float, Float> getActorSyncInterval(RealTimeRebecaActorState source) {
        RealTimeRebecaActorEnvSync envSync = new RealTimeRebecaActorEnvSync();
        RealTimeRebecaDeterministicTransition actorTransition = (RealTimeRebecaDeterministicTransition) envSync.applyRule(source);
        TimeProgressAction timeAction = (TimeProgressAction) actorTransition.getAction();
        return timeAction.getIntervalTimeProgress();
    }

//    private static Pair<Float, Float> getNetworkSyncInterval(RealTimeRebecaSystemState source) {
//        RealTimeRebecaNetworkEnvSyncSOSRule envSyncSOSRule = new RealTimeRebecaNetworkEnvSyncSOSRule();
//        RealTimeRebecaAbstractTransition<RealTimeRebecaNetworkState> networkTransition =
//                envSyncSOSRule.applyRule(source.getNetworkState());
//        RealTimeRebecaDeterministicTransition detNetworkTransition = (RealTimeRebecaDeterministicTransition) networkTransition;
//        TimeProgressAction timeAction = (TimeProgressAction) detNetworkTransition.getAction();
//        return timeAction.getIntervalTimeProgress();
//    }
    private static List<Pair<Float, Float>> getNetworkSyncIntervals(RealTimeRebecaSystemState source) {
        RealTimeRebecaNetworkEnvSyncSOSRule envSyncSOSRule =
                new RealTimeRebecaNetworkEnvSyncSOSRule();

        RealTimeRebecaAbstractTransition<RealTimeRebecaNetworkState> networkTransition = envSyncSOSRule.applyRule(source.getNetworkState());
        List<Pair<Float, Float>> intervals = new ArrayList<>();
        // No applicable transition
        if (networkTransition == null) {
            return intervals;
        }

        // Case 1: Deterministic transition
        if (networkTransition instanceof RealTimeRebecaDeterministicTransition) {
            RealTimeRebecaDeterministicTransition<RealTimeRebecaNetworkState> t =
                    (RealTimeRebecaDeterministicTransition<RealTimeRebecaNetworkState>) networkTransition;
            TimeProgressAction timeAction = (TimeProgressAction) t.getAction();
            intervals.add(timeAction.getIntervalTimeProgress());
        }

        // Case 2: Nondeterministic transition
        else if (networkTransition instanceof RealTimeRebecaNondeterministicTransition) {
            RealTimeRebecaNondeterministicTransition<RealTimeRebecaNetworkState> t =
                    (RealTimeRebecaNondeterministicTransition<RealTimeRebecaNetworkState>) networkTransition;
            for (Pair<? extends Action, RealTimeRebecaNetworkState> p : t.getDestinations()) {
                TimeProgressAction timeAction = (TimeProgressAction) p.getFirst();
                intervals.add(timeAction.getIntervalTimeProgress());
            }
        }

        else {
            throw new IllegalStateException("Unsupported transition type: " + networkTransition.getClass().getName());
        }

        return intervals;
    }

    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaSystemState> applyRule(Action synchAction, RealTimeRebecaSystemState source) {
        return null;
    }

}

