package org.rebecalang.transparentactormodelchecker.realtimerebeca;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class RealTimeRebecaRunner {

    @Autowired
    static Rebeca2RILModelTransformer rebeca2RIL;

    @Autowired
    static RebecaModelCompiler rebecaModelCompiler;

    protected static Pair<RebecaModel, SymbolTable> compileModel(
            File model,
            Set<CompilerExtension> extension,
            CoreVersion coreVersion
    ) {
        return rebecaModelCompiler.compileRebecaFile(model, extension, coreVersion);
    }

    public static void main(String[] args) throws Exception {

        // ---------------------------
        // Argument Handling
        // ---------------------------

        if (args.length < 1) {
            System.out.println("Usage:");
            System.out.println("  java -jar modelchecker.jar <model-file> [startInterval endInterval]");
            System.out.println();
            System.out.println("Example:");
            System.out.println("  java -jar modelchecker.jar examples/motivatingexample.rebeca 0 300");
            System.exit(1);
        }

        String modelPath = args[0];

        float startInterval = 0f;
        float endInterval = 300f;

        if (args.length >= 3) {
            startInterval = Float.parseFloat(args[1]);
            endInterval = Float.parseFloat(args[2]);
        }

        File model = new File(modelPath);

        if (!model.exists()) {
            System.out.println("Error: Model file not found: " + model.getAbsolutePath());
            System.exit(1);
        }

        System.out.println("Model: " + model.getAbsolutePath());
        System.out.println("Input Interval: [" + startInterval + ", " + endInterval + "]");

        // ---------------------------
        // Spring Context Setup
        // ---------------------------

        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(
                        CompilerConfig.class,
                        ModelTransformerConfig.class
                );

        rebecaModelCompiler = context.getBean(RebecaModelCompiler.class);
        rebeca2RIL = context.getBean(Rebeca2RILModelTransformer.class);

        // ---------------------------
        // Compilation Phase
        // ---------------------------

        Set<CompilerExtension> extension = new HashSet<>();
        extension.add(CompilerExtension.HYBRID_REBECA);

        Pair<RebecaModel, SymbolTable> compilationResult =
                compileModel(model, extension, CoreVersion.CORE_2_3);

        // ---------------------------
        // Transformation Phase
        // ---------------------------

        RILModel transformModel =
                rebeca2RIL.transformModel(compilationResult, extension, CoreVersion.CORE_2_3);

        Pair<Float, Float> inputInterval = new Pair<>(startInterval, endInterval);

        // ---------------------------
        // State Space Generation
        // ---------------------------

        GenerateInitialState generateInitialState =
                new GenerateInitialState(transformModel, inputInterval);

        ApplySystemLevelRules applySystemLevelRules =
                new ApplySystemLevelRules(generateInitialState.getInitialState());

        System.out.println("--------------------------------------------------");
        System.out.println("Execution finished successfully.");
        System.out.println("Output written to: output.dot");
        System.out.println("Use Graphviz to visualize the state space.");
        System.out.println("--------------------------------------------------");

        context.close();
    }
}
