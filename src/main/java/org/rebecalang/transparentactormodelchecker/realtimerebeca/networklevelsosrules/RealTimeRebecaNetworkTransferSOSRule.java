package org.rebecalang.transparentactormodelchecker.realtimerebeca.networklevelsosrules;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.transparentactormodelchecker.AbstractRealTimeSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.Action;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.MessageAction;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaMessage;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaNetworkState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaAbstractTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaDeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaNondeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.utils.HybridRebecaStateSerializationUtils;

import java.util.*;

public class RealTimeRebecaNetworkTransferSOSRule extends AbstractRealTimeSOSRule<RealTimeRebecaNetworkState> {

    /**
     * Transfer rule (paper, Table 4): a message m can be delivered when m.arvl == t1.
     * Postponement is NOT handled here; it is the network TimeSync I rule (EnvSync1),
     * which is coupled with time advancement.
     */
    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaNetworkState> applyRule(RealTimeRebecaNetworkState source) {
        RealTimeRebecaNondeterministicTransition<RealTimeRebecaNetworkState> result =
                new RealTimeRebecaNondeterministicTransition<>();

        for (Map.Entry<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> entry : source.getReceivedMessages().entrySet()) {
            for (RealTimeRebecaMessage message : entry.getValue()) {

                // transfer case
                if (message.getMessageArrivalInterval().getFirst().floatValue() == source.getNow().getFirst().floatValue()) {
                    RealTimeRebecaNetworkState backup2 = HybridRebecaStateSerializationUtils.clone(source);

                    HashMap<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> clonedMap = new HashMap<>();
                    for (Map.Entry<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> e : source.getReceivedMessages().entrySet()) {
                        clonedMap.put(e.getKey(), new ArrayList<>(e.getValue()));
                    }
                    ArrayList<RealTimeRebecaMessage> ms = clonedMap.get(entry.getKey());
                    ms.remove(message); // safe: source is untouched
                    clonedMap.put(entry.getKey(), ms);

                    List<Pair<String, String>> keysToRemove = new ArrayList<>();
                    for (Map.Entry<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> entry2 : clonedMap.entrySet()) {
                        if (entry2.getValue().isEmpty()) {
                            keysToRemove.add(entry2.getKey());
                        }
                    }
                    for (Pair<String, String> key : keysToRemove) {
                        clonedMap.remove(key);
                    }
                    backup2.setReceivedMessages(clonedMap);

                    RealTimeRebecaMessage clonedMessage = HybridRebecaStateSerializationUtils.clone(message);
                    clonedMessage.setMessageArrivalInterval(new Pair<>(
                            message.getMessageArrivalInterval().getFirst().floatValue(),
                            Math.min(source.getNow().getSecond().floatValue(),
                                    message.getMessageArrivalInterval().getSecond().floatValue())));
                    result.addDestination(new MessageAction(clonedMessage), backup2);
                }
            }
        }

        if (result.getDestinations().isEmpty()) {
            return null;
        }

        if (result.getDestinations().size() == 1) {
            RealTimeRebecaDeterministicTransition<RealTimeRebecaNetworkState> single =
                    new RealTimeRebecaDeterministicTransition<>();
            single.setDestination(result.getDestinations().get(0).getSecond());
            single.setAction(result.getDestinations().get(0).getFirst());
            return single;
        }

        return result;
    }

    public static List<RealTimeRebecaMessage> getHighPriorityMessages(RealTimeRebecaNetworkState source) {
        float minETA = Float.MAX_VALUE;
        List<RealTimeRebecaMessage> earliestMessages = new ArrayList<>();

        for (Map.Entry<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> entry : source.getReceivedMessages().entrySet()) {
            for (RealTimeRebecaMessage message : entry.getValue()) {
                float etaLowerBound = message.getMessageArrivalInterval().getFirst();
                if (etaLowerBound < minETA) {
                    minETA = etaLowerBound;
                    earliestMessages.clear();
                    earliestMessages.add(message);
                } else if (etaLowerBound == minETA) {
                    earliestMessages.add(message);
                }
            }
        }
        return earliestMessages;
    }

    private ArrayList<RealTimeRebecaMessage> removeAndUpdateNetworkMessages(ArrayList<RealTimeRebecaMessage> messages, Float arrival_l) {
        for (RealTimeRebecaMessage msg : messages) {
            msg.setMessageArrivalInterval(new Pair<>(
                    Math.max(arrival_l.floatValue(), msg.getMessageArrivalInterval().getFirst().floatValue()),
                    msg.getMessageArrivalInterval().getSecond().floatValue()));
        }
        return messages;
    }

    public static Float getSecondEarliestEtaLowerBound(RealTimeRebecaNetworkState source) {
        Float min = Float.MAX_VALUE;
        Float secondMin = Float.MAX_VALUE;
        for (Map.Entry<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> entry : source.getReceivedMessages().entrySet()) {
            for (RealTimeRebecaMessage message : entry.getValue()) {
                float etaLowerBound = message.getMessageArrivalInterval().getFirst();
                if (etaLowerBound < min) {
                    secondMin = min;
                    min = etaLowerBound;
                } else if (etaLowerBound > min && etaLowerBound < secondMin) {
                    secondMin = etaLowerBound;
                }
            }
        }
        return (secondMin == Float.MAX_VALUE) ? null : secondMin;
    }

    public static List<RealTimeRebecaMessage> sortMessages(RealTimeRebecaNetworkState source) {
        List<RealTimeRebecaMessage> sortedMessages = new ArrayList<>();
        for (Map.Entry<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> entry : source.getReceivedMessages().entrySet()) {
            sortedMessages.addAll(entry.getValue());
        }
        sortedMessages.sort(Comparator.comparingDouble(msg -> msg.getMessageArrivalInterval().getFirst()));
        return sortedMessages;
    }

    @Override
    public RealTimeRebecaAbstractTransition<RealTimeRebecaNetworkState> applyRule(Action synchAction, RealTimeRebecaNetworkState source) {
        return null;
    }
}