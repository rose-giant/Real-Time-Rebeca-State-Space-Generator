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
        return applyLocalRule(source);
    }

    private static RealTimeRebecaAbstractTransition<RealTimeRebecaSystemState> applyLocalRule(RealTimeRebecaSystemState source) {
        // actor intervals are shared by every branch
        List<Pair<Float, Float>> actorIntervals = new ArrayList<>();
        for (String actorId : source.getActorsState().keySet()) {
            actorIntervals.add(getActorSyncInterval(source.getActorState(actorId)));
        }

        List<Pair<Pair<Float, Float>, RealTimeRebecaNetworkState>> networkResults = getNetworkSyncIntervals(source);

        // one system-level transition per network time-sync rule
        List<RealTimeRebecaDeterministicTransition<RealTimeRebecaSystemState>> results = new ArrayList<>();
        if (networkResults.isEmpty()) {
            results.add(buildProgressTransition(source, actorIntervals, null));
        } else {
            for (Pair<Pair<Float, Float>, RealTimeRebecaNetworkState> nr : networkResults) {
                List<Pair<Float, Float>> progressIntervals = new ArrayList<>(actorIntervals);
                progressIntervals.add(nr.getFirst());
                results.add(buildProgressTransition(source, progressIntervals, nr.getSecond()));
            }
        }

        if (results.size() == 1) {
            return results.get(0);
        }
        RealTimeRebecaNondeterministicTransition<RealTimeRebecaSystemState> combined =
                new RealTimeRebecaNondeterministicTransition<>();
        for (RealTimeRebecaDeterministicTransition<RealTimeRebecaSystemState> r : results) {
            combined.addDestination(r.getAction(), r.getDestination());
        }
        return combined;
    }

    private static RealTimeRebecaDeterministicTransition<RealTimeRebecaSystemState> buildProgressTransition(
            RealTimeRebecaSystemState source,
            List<Pair<Float, Float>> progressIntervals,
            RealTimeRebecaNetworkState networkDest) {

        RealTimeRebecaSystemState backup = HybridRebecaStateSerializationUtils.clone(source);

        // compute the intersection (unchanged logic)
        float left = Float.MAX_VALUE;
        float right = Float.MAX_VALUE;
        for (Pair<Float, Float> interval : progressIntervals) {
            if (interval.getSecond() < right) {
                right = interval.getSecond();
            }
        }
        for (Pair<Float, Float> interval : progressIntervals) {
            if (interval.getSecond() < left) {
                left = interval.getFirst();
            }
            if (interval.getFirst().floatValue() < right && interval.getFirst().floatValue() != left) {
                right = interval.getFirst();
            }
        }

        Pair<Float, Float> newNow = new Pair<>(left, right);

        // safety net: a tp must advance time, otherwise exploration recurses forever
        if (newNow.getFirst().floatValue() == source.getNow().getFirst().floatValue()
                && newNow.getSecond().floatValue() == source.getNow().getSecond().floatValue()) {
            throw new IllegalStateException("tp does not advance time: " + newNow);
        }

        // install the network's own destination state (carries Upd(b, tl)), then set now on this branch's copy
        if (networkDest != null) {
            backup.setNetworkState(HybridRebecaStateSerializationUtils.clone(networkDest));
        }
        backup.getNetworkState().setNow(newNow);

        for (String actorId : backup.getActorsState().keySet()) {
            RealTimeRebecaActorState actorState = backup.getActorState(actorId);
            actorState.setNow(newNow);
            if (actorState.getResumeTime().getFirst().floatValue() < newNow.getFirst().floatValue()) {
                actorState.setResumeTime(new Pair<>(newNow.getFirst(), actorState.getResumeTime().getSecond()));
            }
            if (!actorState.isSuspent()) {
                actorState.setResumeTime(newNow);
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
    static List<Pair<Pair<Float, Float>, RealTimeRebecaNetworkState>> getNetworkSyncIntervals(RealTimeRebecaSystemState source) {
        RealTimeRebecaNetworkEnvSyncSOSRule envSyncSOSRule = new RealTimeRebecaNetworkEnvSyncSOSRule();
        RealTimeRebecaAbstractTransition<RealTimeRebecaNetworkState> networkTransition =
                envSyncSOSRule.applyRule(source.getNetworkState());
        List<Pair<Pair<Float, Float>, RealTimeRebecaNetworkState>> result = new ArrayList<>();
        if (networkTransition == null) {
            return result;
        }

        if (networkTransition instanceof RealTimeRebecaDeterministicTransition) {
            RealTimeRebecaDeterministicTransition<RealTimeRebecaNetworkState> t =
                    (RealTimeRebecaDeterministicTransition<RealTimeRebecaNetworkState>) networkTransition;
            TimeProgressAction a = (TimeProgressAction) t.getAction();
            result.add(new Pair<>(a.getIntervalTimeProgress(), t.getDestination()));
        } else if (networkTransition instanceof RealTimeRebecaNondeterministicTransition) {
            RealTimeRebecaNondeterministicTransition<RealTimeRebecaNetworkState> t =
                    (RealTimeRebecaNondeterministicTransition<RealTimeRebecaNetworkState>) networkTransition;
            for (Pair<? extends Action, RealTimeRebecaNetworkState> p : t.getDestinations()) {
                TimeProgressAction a = (TimeProgressAction) p.getFirst();
                result.add(new Pair<>(a.getIntervalTimeProgress(), p.getSecond()));
            }
        } else {
            throw new IllegalStateException("Unsupported transition type: " + networkTransition.getClass().getName());
        }
        return result;
    }

    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaSystemState> applyRule(Action synchAction, RealTimeRebecaSystemState source) {
        return null;
    }

}

