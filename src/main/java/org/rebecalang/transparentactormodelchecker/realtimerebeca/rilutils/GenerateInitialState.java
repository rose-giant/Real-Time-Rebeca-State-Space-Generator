package org.rebecalang.transparentactormodelchecker.realtimerebeca.rilutils;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.modeltransformer.ril.RILModel;
import org.rebecalang.modeltransformer.ril.corerebeca.rilinstruction.InstructionBean;
import org.rebecalang.modeltransformer.ril.corerebeca.rilinstruction.RebecInstantiationInstructionBean;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.Environment;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaActorState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaNetworkState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaSystemState;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class GenerateInitialState {

//    ActorClassMakerFromRIL maker;sda
    RILModel rilModel = new RILModel();
    RealTimeRebecaSystemState initialState = new RealTimeRebecaSystemState();
    Pair<Float, Float> inputInterval;
    public GenerateInitialState(RILModel rilModel, Pair<Float, Float> inputInterval2) {
//        this.maker = actorClassMakerFromRIL;
        this.rilModel = rilModel;
        this.inputInterval = inputInterval2;
        this.initialState = this.processMainBlock();
    }

    public RealTimeRebecaSystemState getInitialState() {
        return initialState;
    }

    public RealTimeRebecaSystemState processMainBlock() {
        ArrayList<InstructionBean> main = rilModel.getInstructionList("main");
        ArrayList<RebecInstantiationInstructionBean> instantiations =
                (ArrayList<RebecInstantiationInstructionBean>)
                 main.subList(1, main.size() - 1).stream()
                .filter(i -> i instanceof RebecInstantiationInstructionBean)
                .map(i -> (RebecInstantiationInstructionBean) i)
                .collect(Collectors.toList());

        RealTimeRebecaSystemState systemStateZero = new RealTimeRebecaSystemState();
        RealTimeRebecaNetworkState networkState = new RealTimeRebecaNetworkState();
        systemStateZero.setNetworkState(networkState);

        Pair<Float, Float> now = new Pair<>((float)0, (float)0);
        systemStateZero.setNow(now);
        systemStateZero.getNetworkState().setNow(now);
        systemStateZero.setInputInterval(inputInterval);

        Environment environment = new Environment();
        systemStateZero.setEnvironment(environment);

        for (RebecInstantiationInstructionBean instantiation: instantiations) {
            RealTimeRebecaActorState newActor = new RealTimeRebecaActorState(instantiation.getInstanceName());
            newActor.setResumeTime(new Pair<>(0f, 0f));
            newActor.setNow(new Pair<>(0f, 0f));
            String actorType = instantiation.getType().getTypeName();
            newActor.setRILModel(rilModel);
//            TODO: add the constructor to the scope

            newActor.addScope(newActor.getFullName(actorType + "." + actorType));

            Map<String, Object> knownRebecs = instantiation.getBindings();
            for (Map.Entry<String, Object> entry : knownRebecs.entrySet()) {
                newActor.addVariableToScope(entry.getKey(), entry.getValue());
            }
            Map<String, Object> constructorArgs = instantiation.getConstructorParameters();
            for (Map.Entry<String, Object> entry : constructorArgs.entrySet()) {
                newActor.addVariableToScope(entry.getKey(), entry.getValue());
            }

            systemStateZero.setActorState(newActor.getId(), newActor);
        }

        //TODO: add the envVars to the system state's environment

        return systemStateZero;
    }
}
