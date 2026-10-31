package org.rebecalang.transparentactormodelchecker.realtimerebeca.networklevelsosrules;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.transparentactormodelchecker.AbstractRealTimeSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.Action;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaMessage;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaNetworkState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaAbstractTransition;

import java.util.ArrayList;

public class RealTimeRebecaNetworkEnvSyncSOSRule extends AbstractRealTimeSOSRule<RealTimeRebecaNetworkState> {
    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaNetworkState> applyRule(RealTimeRebecaNetworkState source) {
        if (source.getReceivedMessages().isEmpty()) {
            RealTimeRebecaNetworkEnvSync3SOSRule envSync3SOSRule = new RealTimeRebecaNetworkEnvSync3SOSRule();
            return envSync3SOSRule.applyRule(source);
        }
        if (source.overlapTransferable(source)) {
            RealTimeRebecaNetworkEnvSync2SOSRule envSync2SOSRule = new RealTimeRebecaNetworkEnvSync2SOSRule();
            return envSync2SOSRule.applyRule(source);
        }
        if(satisfiesTimeProgressCondition(source, source.getNow().getFirst(), source.getNow().getSecond())) {
            RealTimeRebecaNetworkEnvSync1SOSRule envSync1SOSRule = new RealTimeRebecaNetworkEnvSync1SOSRule();
            return envSync1SOSRule.applyRule(source);
        }

        return null;
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
