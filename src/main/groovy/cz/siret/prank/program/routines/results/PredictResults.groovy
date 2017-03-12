package cz.siret.prank.program.routines.results

import cz.siret.prank.program.params.Parametrized
import cz.siret.prank.program.routines.AbstractEvalRoutine
import cz.siret.prank.score.results.ClassifierStats
import cz.siret.prank.score.results.Evaluation
import cz.siret.prank.utils.CSV
import cz.siret.prank.utils.Writable
import groovy.util.logging.Slf4j

import static cz.siret.prank.utils.Futils.writeFile
import static cz.siret.prank.utils.Futils.mkdirs

/**
 * results for eval-predict routine
 */
@Slf4j
class PredictResults implements Parametrized, Writable {

    Evaluation predictionsEval
    ClassifierStats classifierStats

    PredictResults() {
        predictionsEval = new Evaluation(EvalResults.getDefaultEvalCrtieria())
        classifierStats = new ClassifierStats()
    }

    Map getStats() {
        Map m = predictionsEval.stats

        m.putAll( classifierStats.statsMap )

        return m
    }

    String getMiscStatsCSV() {
        stats.collect { "$it.key, ${AbstractEvalRoutine.fmt ( it.value)}" }.join("\n")
    }

    /**
     *
     * @param outdir
     * @param classifierName
     * @param summaryStats  in summary stats (like over multiple seed runs) we dont want all pocket details multiple times
     */
    void logAndStore(String outdir, String classifierName, Boolean logIndividualCases=null) {

        if (logIndividualCases==null) {
            logIndividualCases = params.log_cases
        }

        mkdirs(outdir)

        List<Integer> tolerances = params.eval_tolerances

        String succ_rates          = predictionsEval.toSuccRatesCSV(tolerances)
        String classifier_stats    = classifierStats.toCSV(" $classifierName ")
        String stats             = getMiscStatsCSV()

        writeFile "$outdir/success_rates.csv", succ_rates
        writeFile "$outdir/classifier.csv", classifier_stats
        writeFile "$outdir/stats.csv", stats

        if (logIndividualCases) {
            predictionsEval.sort()

            String casedir = "$outdir/cases"
            mkdirs(casedir)

            writeFile "$casedir/proteins.csv", predictionsEval.toProteinsCSV()
            writeFile "$casedir/ligands.csv", predictionsEval.toLigandsCSV()
            writeFile "$casedir/pockets.csv", predictionsEval.toPocketsCSV()
            writeFile "$casedir/ranks.csv", predictionsEval.toRanksCSV()
            writeFile "$casedir/ranks_rescored.csv", predictionsEval.toRanksCSV()
        }

        log.info "\n" + CSV.tabulate(classifier_stats) + "\n\n"

        write "\nSuccess Rates:\n" + CSV.tabulate(succ_rates) + "\n"
    }

//===========================================================================================================//

    String pc(double x) {
        return Evaluation.formatPercent(x)
    }

    static String fmt(Object val) {
        if (val==null)
            "--"
        else
            fmtn(val)
    }

    static String fmtn(double x) {
        //return ClassifierStats.format(x)
        //return ClassifierStats.format(x)
        sprintf "%8.2f", x
    }

    static String fmtn(int x) {
        //return ClassifierStats.format(x)
        //return ClassifierStats.format(x)
        sprintf "%8d", x
    }

    String toMainResultsCsv(String outdir, String label) {

        def m = new LinkedHashMap()

        m.dir = new File(outdir).name
        m.label = label

        m.proteins = predictionsEval.proteinCount
        m.ligands =  predictionsEval.ligandCount
        m.pockets =  predictionsEval.pocketCount

        m.DCA_4_0 = pc predictionsEval.calcDefaultCriteriumSuccessRate(0)
        m.DCA_4_2 = pc predictionsEval.calcDefaultCriteriumSuccessRate(2)

        m.P =   fmt classifierStats.stats.p
        m.R =   fmt classifierStats.stats.r
        m.FM =  fmt classifierStats.stats.f1
        m.MCC = fmt classifierStats.stats.MCC

        m.ligSize =    fmt predictionsEval.avgLigandAtoms
        m.pocketVol =  fmt predictionsEval.avgPocketVolume
        m.pocketSurf = fmt predictionsEval.avgPocketSurfAtoms


        return m.keySet().join(",") + "\n" + m.values().join(",")
    }

    void logMainResults(String outdir, String label) {
        String mainRes = toMainResultsCsv(outdir, label)
        writeFile "$outdir/summary.csv", mainRes

        // collecting results
        File collectedf = new File("$outdir/../runs_pred.csv")
        if (!collectedf.exists()) {
            collectedf << mainRes.readLines()[0] + "\n" // add header
        }
        collectedf << mainRes.readLines()[1] + "\n"
    }



}