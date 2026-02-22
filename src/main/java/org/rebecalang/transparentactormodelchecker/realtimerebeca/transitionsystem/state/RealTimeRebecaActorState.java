package org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state;
import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.modeltransformer.ril.RILModel;
import org.rebecalang.modeltransformer.ril.corerebeca.rilinstruction.InstructionBean;
import org.rebecalang.modeltransformer.ril.corerebeca.rilinstruction.Variable;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.rilutils.RILEquivalentActorClass;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

@SuppressWarnings("serial")
public class RealTimeRebecaActorState extends RealTimeRebecaAbstractState implements Serializable {
    private boolean isSuspended;
    public final static String PC = "$PC$";
    public int pc;
    transient RILModel rilModel;
    private String id;
    private Environment environment;
    private ArrayList<ActorScope> scopes;
    private ArrayList<RealTimeRebecaMessage> queue;
//    private ArrayList<InstructionBean> sigma = new ArrayList<>();
    private RILEquivalentActorClass rilEquivalentActorClass = new RILEquivalentActorClass();
    private String activeMode;
    private Pair<Float, Float> resumeTimeInterval;
    private Pair<Float, Float> nowInterval;
    private boolean isSuspent;

    public boolean isSuspent() {
        return isSuspent;
    }

    public void setSuspent(boolean suspent) {
        isSuspent = suspent;
    }

//    public void setSuspended(boolean suspended) {
//        isSuspended = suspended;
//    }
//
//    public boolean isSuspended() {
//        return this.getNow().getFirst().floatValue() < this.getResumeTime().getFirst().floatValue();
//    }

    public RealTimeRebecaActorState(String id) {
        this.scopes = new ArrayList<>();
        this.id = id;
        this.isSuspent = false;
//        ActorScope actorScope = new ActorScope();
//        actorScope.setBlockName();
//        this.scopes.add(actorScope);
//        scope = new ArrayList<HashMap<String,Object>>();
//        scope.add(new HashMap<String, Object>());
        queue = new ArrayList<RealTimeRebecaMessage>();
    }

    public String getId() {
        return id;
    }

    public RILModel getRILModel() {
        return rilModel;
    }
    public void setRILModel(RILModel rilModel) {
        this.rilModel = rilModel;
    }

    public void addVariableToScope(String varName) {
        addVariableToScope(varName, null);
    }

    public void addVariableToScope(String varName, Object value) {
        this.scopes.get(scopes.size()-1).addVariableToScope(varName, value);
    }

    public void setVariableValue(Variable leftVarName, Object value) {
        this.scopes.get(scopes.size()-1).setVariableValue(leftVarName, value);
    }

    public Object getVariableValue(String varName) {
        //look for the value in a reversed order in the scopes, ok?
        for (int i = scopes.size()-1 ; i >= 0; i--) {
            Object object = this.scopes.get(i).getVariableValue(varName);
            if (object != null) {
                return object;
            }
        }

        return environment.getVariableValue(varName);
    }

    public boolean hasVariableInScope(String varName) {
        Object object = this.scopes.get(scopes.size() - 1).hasVariableInScope(varName);
        if ((Boolean) object != false) {
            return true;
        }
        return environment.hasVariableInScope(varName);
    }

    public boolean messageQueueIsEmpty() {
        return queue.isEmpty();
    }

    public RealTimeRebecaMessage getFirstMessage() {
        return queue.remove(0);
    }

    public void receiveMessage(RealTimeRebecaMessage newMessage) {
        queue.add(newMessage);
        queue = sortMessages(queue);
    }

    private ArrayList<RealTimeRebecaMessage> sortMessages(ArrayList<RealTimeRebecaMessage> queue) {
        ArrayList<RealTimeRebecaMessage> sortedQueue = new ArrayList<>(queue);
        sortedQueue.sort(Comparator.comparing(msg -> msg.getMessageArrivalInterval().getFirst()));
        return sortedQueue;
    }

    public void pushToScope() {
        this.scopes.add(new ActorScope());
    }

    public void popFromScope() {
        this.scopes.remove(scopes.size() - 1);
    }

    @SuppressWarnings("unchecked")
    public InstructionBean getEnabledInstruction() {
        Pair<String, Integer> pc = (Pair<String, Integer>) getVariableValue(PC);
        ArrayList<InstructionBean> instructionsList =
                rilModel.getInstructionList(pc.getFirst());

        return instructionsList.get(pc.getSecond());
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

//    public String toString() {
//        return id + "\n[scope:(" + scope + "),\n queue:(" + queue + ")]";
//    }

    @SuppressWarnings("unchecked")
    public void movePCtoTheNextInstruction() {
        Pair<String, Integer> pc = (Pair<String, Integer>) getVariableValue(PC);
        pc.setSecond(pc.getSecond() + 1);
    }

    //ask Ehsan --> is line number the same as pc??
    public void jumpToBranchInstruction(int lineNumber) {
//        Pair<String, Integer> pc = (Pair<String, Integer>) getVariableValue(PC);
//        pc.setSecond(lineNumber);
        scopes.get(scopes.size() - 1).setPC(lineNumber);
    }



    public Pair<Float, Float> getNow() {
        return nowInterval;
    }

    public float getUpperBound(Pair<Float, Float> interval) {
        return interval.getSecond();
    }

    public float getLowerBound(Pair<Float, Float> interval) {
        return interval.getFirst();
    }

    public Pair<Float, Float> getResumeTime() {
        return resumeTimeInterval;
    }

    public void setResumeTime(Pair<Float, Float> resumeTimeInterval) {
        this.resumeTimeInterval = resumeTimeInterval;
    }

    public void setNow(Pair<Float, Float> nowInterval) {
        this.nowInterval = nowInterval;
    }

    public String getActiveMode() {
        return activeMode;
    }

    public void setActiveMode(String activeMode) {
        this.activeMode = activeMode;
    }

    public float getMinETA() {
       return this.getFirstMessage().getMessageArrivalInterval().getFirst();
    }

    public RILEquivalentActorClass getRilEquivalentActorClass() {
        return rilEquivalentActorClass;
    }

    public void setRilEquivalentActorClass(RILEquivalentActorClass rilEquivalentActorClass) {
        this.rilEquivalentActorClass = rilEquivalentActorClass;
    }

    public void addScope(String blockName) {
        ActorScope actorScope = new ActorScope();
        actorScope.setBlockName(blockName);
        scopes.add(actorScope);
    }

    public ArrayList<InstructionBean> getSigma() {
        if (scopes.isEmpty()) return new ArrayList<>();
        return rilModel.getInstructionList(scopes.get(scopes.size() - 1).getBlockName());
    }

    public String getFullName(String blockName) {
        for (String key : rilModel.getMethodNames()) {
            if (key.startsWith(blockName)) {
                return key;
            }
        }

        return "";
    }

    public boolean noScopeInstructions() {
        if (scopes.isEmpty()) return true;
        boolean noInstructions = scopes.get(scopes.size()-1).getPC() == this.getSigma().size() - 1 ;
//        if (noInstructions) {
//            scopes.remove(scopes.size()-1);
//        }
        return noInstructions;
    }

    public InstructionBean getInstruction() {
        return rilModel.getInstructionList(scopes.get(scopes.size() - 1).getBlockName()).get(scopes.get(scopes.size()-1).getPC());
    }

    public void addInstruction() {
        rilModel.getInstructionList(scopes.get(scopes.size() - 1).getBlockName()).add(null);
    }

    public void setCurrentBlockName(String name) {
        scopes.get(scopes.size() - 1).setBlockName(name);
        scopes.get(scopes.size() - 1).setPC(0);
    }

    public void moveToNextStatement() {
       scopes.get(scopes.size() - 1).incrementPC();
    }

}
//    public void addToSigma(ArrayList<InstructionBean> sigma) {
//        this.sigma.addAll(sigma);
//    }
//
//    public void setSigma(ArrayList<InstructionBean> sigma) {
//        this.sigma = sigma;
//    }
//