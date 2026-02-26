package org.rebecalang.transparentactormodelchecker.realtimerebeca.networklevelsosrules;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.transparentactormodelchecker.AbstractRealTimeSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.Action;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.TimeProgressAction;
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
        float firstB = bounds.get(0);
        float secondB = bounds.get(1);
        float minEte = source.getMinETE();
        float minEta = source.getMinETA();

        progress.setFirst(now.getSecond().floatValue());
        if (!source.isEmpty()) {
            progress.setFirst(firstB);
            progress.setSecond(secondB);
        }

        RealTimeRebecaNetworkState backup = HybridRebecaStateSerializationUtils.clone(source);
        backup.setNow(progress);
        TimeProgressAction action = new TimeProgressAction();
        action.setTimeProgress(progress);

        RealTimeRebecaDeterministicTransition<RealTimeRebecaNetworkState> result = new RealTimeRebecaDeterministicTransition<>();
        result.setAction(action);
        result.setDestination(backup);

        return result;
    }

    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaNetworkState> applyRule(Action synchAction, RealTimeRebecaNetworkState source) {
        return null;
    }

}
