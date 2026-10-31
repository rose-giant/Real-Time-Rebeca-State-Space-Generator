package org.rebecalang.transparentactormodelchecker.realtimerebeca.networklevelsosrules;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.transparentactormodelchecker.AbstractRealTimeSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.Action;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.TimeProgressAction;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaMessage;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaNetworkState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaAbstractTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaNondeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.utils.HybridRebecaStateSerializationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RealTimeRebecaNetworkEnvSync2SOSRule extends AbstractRealTimeSOSRule<RealTimeRebecaNetworkState> {
    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaNetworkState> applyRule(RealTimeRebecaNetworkState source) {
        RealTimeRebecaNondeterministicTransition<RealTimeRebecaNetworkState> result = new RealTimeRebecaNondeterministicTransition<>();
        RealTimeRebecaNetworkState backup = HybridRebecaStateSerializationUtils.clone(source);
        Pair<Float, Float> now = source.getNow();
        Pair<Float, Float> progress = new Pair<>(0f, 0f);
        float minETA = source.getMinETA();
        float minETE = source.getMinETE();
        progress.setFirst(now.getSecond());
        progress.setSecond(minETE);
        backup.setNow(progress);

        for (Map.Entry<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> entry : source.getReceivedMessages().entrySet()) {
            ArrayList<RealTimeRebecaMessage> messageList = entry.getValue();
            Iterator<RealTimeRebecaMessage> iterator = messageList.iterator();
            while (iterator.hasNext()) {
                RealTimeRebecaMessage message = iterator.next();
                if (message.getMessageArrivalInterval().getFirst().floatValue() == minETA) {
                    HashMap<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> clonedMap = new HashMap<>();
                    for (Map.Entry<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> e : source.getReceivedMessages().entrySet()) {
                        ArrayList<RealTimeRebecaMessage> clonedList = new ArrayList<>(e.getValue());
                        clonedMap.put(e.getKey(), clonedList);
                    }
                    ArrayList<RealTimeRebecaMessage> ms = clonedMap.get(entry.getKey());
                    ms.remove(message); // safe: source is untouched
                    RealTimeRebecaMessage clonedMessage = HybridRebecaStateSerializationUtils.clone(message);
//                        clonedMessage.setMessageArrivalInterval(new Pair<>(secondB, message.getMessageArrivalInterval().getSecond()));
                    clonedMessage.setMessageArrivalInterval(new Pair<>(now.getSecond().floatValue(), message.getMessageArrivalInterval().getSecond().floatValue()));
                    ms.add(clonedMessage);
                    clonedMap.put(entry.getKey(), ms);
                    backup.setReceivedMessages(clonedMap);
                    TimeProgressAction timeProgressAction = new TimeProgressAction();
                    timeProgressAction.setTimeProgress(progress);
                    result.addDestination(timeProgressAction, backup);
                } else {
                    int a = 12;
                    System.out.println(a);
                }
            }
        }

        return result;
    }

    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaNetworkState> applyRule(Action synchAction, RealTimeRebecaNetworkState source) {
        return null;
    }
}
