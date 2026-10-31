package org.rebecalang.transparentactormodelchecker.realtimerebeca.networklevelsosrules;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.transparentactormodelchecker.AbstractRealTimeSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.Action;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.TimeProgressAction;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaMessage;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaNetworkState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaAbstractTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaDeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.utils.HybridRebecaStateSerializationUtils;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.utils.TimeSyncHelper;

import java.util.*;

public class RealTimeRebecaNetworkEnvSync1SOSRule extends AbstractRealTimeSOSRule<RealTimeRebecaNetworkState> {
    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaNetworkState> applyRule(RealTimeRebecaNetworkState source) {
        ArrayList<Float> bounds = source.getAllBounds(source);
        Pair<Float, Float> now = source.getNow();

        Pair<Float, Float> progress = new Pair<>(0f, 0f);
        progress.setFirst(now.getSecond().floatValue());

        RealTimeRebecaDeterministicTransition<RealTimeRebecaNetworkState> result = new RealTimeRebecaDeterministicTransition<>();
        TimeProgressAction action = new TimeProgressAction();
        action.setTimeProgress(now);
        if (!source.isEmpty()) {
            // Make sure the next interval exists.
            if (bounds.size() < 3) {
                return result;
            }

            float secondB = bounds.get(1);
            float thirdB = bounds.get(2);

            float t1 = secondB;
            float t2 = thirdB;

            // Check the logical condition before modifying the messages.
//            if (!satisfiesTimeProgressCondition(source, now.getFirst(), now.getSecond())) {
//                return result; // The rule is not applicable.
//            }

            progress.setFirst(t1);
            progress.setSecond(t2);
        }

        // Clone the original state so the source remains unchanged.
        RealTimeRebecaNetworkState backup = HybridRebecaStateSerializationUtils.clone(source);

        // Update the lower bound of every message's arrival interval.
        for (ArrayList<RealTimeRebecaMessage> messages : backup.getReceivedMessages().values()) {
            for (RealTimeRebecaMessage msg : messages) {
                Pair<Float, Float> arrivalInterval = msg.getMessageArrivalInterval();
                arrivalInterval.setFirst(progress.getFirst());
            }
        }

        backup.setNow(progress);
        action.setTimeProgress(progress);

        result.setAction(action);
        result.setDestination(backup);

        return result;
    }

    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaNetworkState> applyRule(Action synchAction, RealTimeRebecaNetworkState source) {
        return null;
    }


}
