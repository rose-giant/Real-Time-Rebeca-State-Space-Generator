package org.rebecalang.transparentactormodelchecker.realtimerebeca;   // same package as StateSpaceGenTest

import org.rebecalang.compiler.CompilerConfig;
import org.rebecalang.compiler.modelcompiler.RebecaModelCompiler;
import org.rebecalang.compiler.modelcompiler.SymbolTable;
import org.rebecalang.compiler.modelcompiler.corerebeca.objectmodel.RebecaModel;
import org.rebecalang.compiler.utils.CompilerExtension;
import org.rebecalang.compiler.utils.CoreVersion;
import org.rebecalang.compiler.utils.Pair;
import org.rebecalang.modeltransformer.ModelTransformerConfig;
import org.rebecalang.modeltransformer.ril.RILModel;
import org.rebecalang.modeltransformer.ril.Rebeca2RILModelTransformer;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.compositionlevelsosrules.ApplySystemLevelRules;
import org.rebecalang.transparentactormodelchecker.realtimerebeca.rilutils.GenerateInitialState;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.rebecalang.transparentactormodelchecker.realtimerebeca.RealTimeRebecaRunner.compileModel;
import static org.rebecalang.transparentactormodelchecker.realtimerebeca.RealTimeRebecaRunner.rebeca2RIL;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(CompilerConfig.class, ModelTransformerConfig.class);

        RebecaModelCompiler compiler = ctx.getBean(RebecaModelCompiler.class);
        Rebeca2RILModelTransformer rebeca2RIL = ctx.getBean(Rebeca2RILModelTransformer.class);

        File model = new File(args[0]);
        float start = Float.parseFloat(args[1]);
        float end = Float.parseFloat(args[2]);

        Set<CompilerExtension> extension = new HashSet<>();
        extension.add(CompilerExtension.HYBRID_REBECA);

        Pair<RebecaModel, SymbolTable> result =
                compiler.compileRebecaFile(model, extension, CoreVersion.CORE_2_3);
        RILModel rilModel = rebeca2RIL.transformModel(result, extension, CoreVersion.CORE_2_3);

        GenerateInitialState init = new GenerateInitialState(rilModel, new Pair<>(start, end));
        new ApplySystemLevelRules(init.getInitialState());
    }
}