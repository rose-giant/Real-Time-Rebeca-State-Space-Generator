package org.rebecalang.transparentactormodelchecker.realtimerebeca.statementlevelsosrules;

import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.modeltransformer.ril.corerebeca.rilinstruction.*;
import org.rebecalang.transparentactormodelchecker.AbstractRealTimeSOSRule;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.action.Action;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.state.RealTimeRebecaActorState;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaAbstractTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaDeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.transitionsystem.transition.RealTimeRebecaNondeterministicTransition;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.utils.HybridRebecaStateSerializationUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import static org.rebecalang.transparentactormodelchecker.realtimerebeca.utils.HybridExpressionEvaluator.hybridExpressionEvaluator;

@Component
public class RealTimeRebecaJumpSOSRule extends AbstractRealTimeSOSRule<Pair<RealTimeRebecaActorState, InstructionBean>> {

    //if: pc = 5
    //else: pc = 9
    @Override
    public RealTimeRebecaAbstractTransition<Pair<RealTimeRebecaActorState, InstructionBean>> applyRule(Pair<RealTimeRebecaActorState, InstructionBean> source) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(source.getFirst());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();   // THIS is what we want
        }

        RealTimeRebecaActorState backup1 = HybridRebecaStateSerializationUtils.clone(source.getFirst());
        backup1.setRILModel(source.getFirst().getRILModel());
        RealTimeRebecaActorState backup2 = HybridRebecaStateSerializationUtils.clone(source.getFirst());
        backup2.setRILModel(source.getFirst().getRILModel());

        JumpIfNotInstructionBean jumpIfNotInstructionBean = (JumpIfNotInstructionBean) source.getSecond();
        RealTimeRebecaActorState originalState = HybridRebecaStateSerializationUtils.clone(source.getFirst());
        Pair<RealTimeRebecaActorState, InstructionBean> originalSource = new Pair<>();
        originalState.setRILModel(source.getFirst().getRILModel());
        originalSource.setFirst(originalState);
        originalSource.setSecond(new PushARInstructionBean());
        Object conditionEval = null;
        if (jumpIfNotInstructionBean.getCondition() instanceof Variable) {
            Variable var = (Variable) jumpIfNotInstructionBean.getCondition();
            conditionEval = originalState.getVariableValue(var.getVarName());
        } else {
            conditionEval = hybridExpressionEvaluator(source);
        }

        RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>> ifResult =
                new RealTimeRebecaDeterministicTransition<>();
        backup1.moveToNextStatement();
        source.setFirst(backup1);
        source.setSecond(new PushARInstructionBean());
        ifResult.setDestination(source);
        ifResult.setAction(Action.TAU);

        RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>> elseResult =
                new RealTimeRebecaDeterministicTransition<Pair<RealTimeRebecaActorState,InstructionBean>>();
        backup2.jumpToBranchInstruction(jumpIfNotInstructionBean.getLineNumber());
        originalSource.setFirst(backup2);
        elseResult.setDestination(originalSource);
        elseResult.setAction(Action.TAU);

        if (conditionEval instanceof NonDetValue) {
            RealTimeRebecaNondeterministicTransition<Pair<RealTimeRebecaActorState, InstructionBean>> nondetResult =
                    new RealTimeRebecaNondeterministicTransition<Pair<RealTimeRebecaActorState,InstructionBean>>();
            nondetResult.addDestination(ifResult.getAction(), ifResult.getDestination());
            nondetResult.addDestination(elseResult.getAction(), elseResult.getDestination());
            return nondetResult;
        }

        if (conditionEval instanceof Boolean) {
            if ((Boolean) conditionEval) return ifResult;
            else return elseResult;
        }

        return null;
    }

    @Override
    public RealTimeRebecaAbstractTransition<Pair<RealTimeRebecaActorState, InstructionBean>> applyRule(Action synchAction, Pair<RealTimeRebecaActorState, InstructionBean> source) {
        return null;
    }
}
