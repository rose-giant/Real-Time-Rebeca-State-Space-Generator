package org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state;

import org.rebecalang.compiler.utils.Pair;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class RealTimeRebecaNetworkState implements Serializable {
    private HashMap<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> receivedMessages;
    private Pair<Float, Float> now;

    public Pair<Float, Float> getNow() {
        return now;
    }

    public void setNow(Pair<Float, Float> now) {
        this.now = now;
    }

    public RealTimeRebecaNetworkState() {
        receivedMessages = new HashMap<Pair<String,String>, ArrayList<RealTimeRebecaMessage>>();
    }

    public HashMap<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> getReceivedMessages() {
        return receivedMessages;
    }
    public void setReceivedMessages(HashMap<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> receivedMessages) {
        this.receivedMessages = receivedMessages;
    }

    public void addMessage(RealTimeRebecaMessage message) {
        Pair<String, String> key = new Pair<String, String>(
                message.getSender().getId(), message.getReceiver().getId());
        if(!receivedMessages.containsKey(key))
            receivedMessages. put(key, new ArrayList<RealTimeRebecaMessage>());
        receivedMessages.get(key).add(message);
    }

    public float getMinETA() {
        float minETA = Float.MAX_VALUE;
        for (Map.Entry<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> entry : this.getReceivedMessages().entrySet()) {
            ArrayList<RealTimeRebecaMessage> messages = entry.getValue();
            for (RealTimeRebecaMessage message : messages) {
                float etaLowerBound = message.getMessageArrivalInterval().getFirst();
                if (etaLowerBound < minETA) minETA = etaLowerBound;
            }
        }

        return minETA;
    }

    public float getSecondMinETA() {
        float min = Float.MAX_VALUE;
        float secondMin = Float.MAX_VALUE;
        for (Map.Entry<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> entry: this.getReceivedMessages().entrySet()) {
            ArrayList<RealTimeRebecaMessage> messages = entry.getValue();
            for (RealTimeRebecaMessage message : messages) {
                float eta = message.getMessageArrivalInterval().getFirst();

                if (eta < min) {
                    secondMin = min;
                    min = eta;
                }
                else if (eta > min && eta < secondMin) {
                    secondMin = eta;
                }
            }
        }

        return secondMin;
    }

    public float getMinETE() {
        float minETE = Float.MAX_VALUE;
        for (Map.Entry<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> entry : this.getReceivedMessages().entrySet()) {
            ArrayList<RealTimeRebecaMessage> messages = entry.getValue();
            for (RealTimeRebecaMessage message : messages) {
                float etaLowerBound = message.getMessageArrivalInterval().getSecond();
                if (etaLowerBound < minETE) minETE = etaLowerBound;
            }
        }

        return minETE;
    }

    public Pair<Float, Float> getTwoSmallestDistinctETAs() {
        float min = Float.MAX_VALUE;
        float secondMin = Float.MAX_VALUE;
        for (Map.Entry<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> entry : this.getReceivedMessages().entrySet()) {
            ArrayList<RealTimeRebecaMessage> messages = entry.getValue();
            for (RealTimeRebecaMessage message : messages) {
                float lower = message.getMessageArrivalInterval().getFirst();
                float upper = message.getMessageArrivalInterval().getSecond();
                // process both bounds
                float[] values = { lower, upper };
                for (float eta : values) {
                    if (eta < min) {
                        secondMin = min;
                        min = eta;
                    }
                    else if (eta > min && eta < secondMin) {
                        secondMin = eta;
                    }
                }
            }
        }
//        if (secondMin == Float.MAX_VALUE) {
//            return null; // or new Pair<>(min, null) depending on your semantics
//        }
        return new Pair<>(min, secondMin);
    }

    public boolean transferable(RealTimeRebecaNetworkState source) {
        if (source == null || source.getReceivedMessages() == null) {
            return false;
        }

        Float nowStart = source.getNow().getFirst();
        if (nowStart == null) {
            return false;
        }

        for (Map.Entry<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> entry
                : source.getReceivedMessages().entrySet()) {

            for (RealTimeRebecaMessage message : entry.getValue()) {

                Float arrivalStart = message.getMessageArrivalInterval().getFirst();
                if (arrivalStart != null && arrivalStart.floatValue() == nowStart.floatValue()) {
                    return true;   // we found at least one transferable message
                }
            }
        }

        return false;  // none found
    }

    public boolean isEmpty() {
        if (receivedMessages == null || receivedMessages.isEmpty()) {
            return true;
        }
        for (ArrayList<RealTimeRebecaMessage> messages : receivedMessages.values()) {
            if (messages != null && !messages.isEmpty()) {
                return false; // found at least one message
            }
        }
        return true; // all lists were empty
    }

    public ArrayList<Float> getAllBounds(RealTimeRebecaNetworkState networkState) {
        ArrayList<Float> bounds = new ArrayList<>();
        for (Map.Entry<Pair<String, String>, ArrayList<RealTimeRebecaMessage>> entry : networkState.getReceivedMessages().entrySet()) {
            ArrayList<RealTimeRebecaMessage> messages = entry.getValue();
            for (RealTimeRebecaMessage message : messages) {
                bounds.add(message.getMessageArrivalInterval().getFirst());
                bounds.add(message.getMessageArrivalInterval().getSecond());
            }
        }
        bounds.add(networkState.getNow().getFirst());
        bounds.add(networkState.getNow().getSecond());
        List<Float> uniqueSortedBounds = bounds.stream().distinct().sorted().toList();
        bounds.clear();
        for (Float element: uniqueSortedBounds) {
            bounds.add(element);
        }
        if (bounds.size() == 1) bounds.add(bounds.get(0));
        return bounds;
    }

}
