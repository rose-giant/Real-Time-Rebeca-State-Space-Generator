package org.rebecalang.transparentactormodelchecker.realtimerebeca.networklevelsosrules;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.transparentactormodelchecker.AbstractRealTimeSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.Action;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaMessage;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaNetworkState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaAbstractTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaDeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaNondeterministicTransition;

import java.util.ArrayList;
import java.util.List;

public class RealTimeRebecaNetworkEnvSyncSOSRule extends AbstractRealTimeSOSRule<RealTimeRebecaNetworkState> {
    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaNetworkState> applyRule(RealTimeRebecaNetworkState source) {
        // Sync3 is exclusive: it only applies when there are no messages (and throws otherwise).
        if (source.getReceivedMessages().isEmpty()) {
            return new RealTimeRebecaNetworkEnvSync3SOSRule().applyRule(source);
        }

        RealTimeRebecaNondeterministicTransition<RealTimeRebecaNetworkState> combined =
                new RealTimeRebecaNondeterministicTransition<>();

        if (satisfiesTimeProgressCondition(source, source.getNow().getFirst(), source.getNow().getSecond())) {
            collect(new RealTimeRebecaNetworkEnvSync1SOSRule().applyRule(source), combined);
        }
        if (source.overlapTransferable(source)) {
            collect(new RealTimeRebecaNetworkEnvSync2SOSRule().applyRule(source), combined);
        }

        List<Pair<? extends Action, RealTimeRebecaNetworkState>> dests = combined.getDestinations();
        if (dests.isEmpty()) {
            return null;                       // no rule applicable
        }
        if (dests.size() == 1) {               // keep the deterministic shape when there is no real choice
            Pair<? extends Action, RealTimeRebecaNetworkState> only = dests.get(0);
            return new RealTimeRebecaDeterministicTransition<>(only.getFirst(), only.getSecond());
        }
        return combined;
    }

    /** Flattens a rule's result (null, deterministic or nondeterministic) into the combined transition. */
    private void collect(RealTimeRebecaAbstractTransition<RealTimeRebecaNetworkState> t,
                         RealTimeRebecaNondeterministicTransition<RealTimeRebecaNetworkState> out) {
        if (t == null) {
            return;
        }
        if (t instanceof RealTimeRebecaDeterministicTransition<RealTimeRebecaNetworkState> d) {
            // Skip the "empty" results (e.g. Sync1's early return) that caused the NPE.
            if (d.getAction() != null && d.getDestination() != null) {
                out.addDestination(d.getAction(), d.getDestination());
            }
        } else if (t instanceof RealTimeRebecaNondeterministicTransition<RealTimeRebecaNetworkState> n) {
            out.addAllDestinations(n.getDestinations());
        }
    }

    private boolean satisfiesTimeProgressCondition(RealTimeRebecaNetworkState source, float t1, float t2) {
        boolean allMessagesSatisfyFirstCondition = true;
        boolean existsMessageWithLowerBoundBetween = false;

        for (ArrayList<RealTimeRebecaMessage> messages : source.getReceivedMessages().values()) {
            if (messages == null) {
                continue;
            }

            for (RealTimeRebecaMessage msg : messages) {

                Pair<Float, Float> arrivalInterval =
                        msg.getMessageArrivalInterval();

                float arvL = arrivalInterval.getFirst();
                float arvU = arrivalInterval.getSecond();

                // Condition A for each message:
                // arvL != t1 OR arvU > t2
                if (Float.compare(arvL, t1) == 0 && !(arvU > t2)) {
                    allMessagesSatisfyFirstCondition = false;
                }

                // Condition B:
                // t1 < arvL < t2
                if (arvL > t1 && arvL < t2) {
                    existsMessageWithLowerBoundBetween = true;
                }
            }
        }

        // A OR B
        return allMessagesSatisfyFirstCondition || existsMessageWithLowerBoundBetween;
    }

    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaNetworkState> applyRule(Action synchAction, RealTimeRebecaNetworkState source) {
        return null;
    }
}
