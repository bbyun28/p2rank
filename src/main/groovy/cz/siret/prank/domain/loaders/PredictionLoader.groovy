package cz.siret.prank.domain.loaders

import com.sun.istack.internal.Nullable
import cz.siret.prank.domain.*
import cz.siret.prank.features.api.ProcessedItemContext
import cz.siret.prank.features.implementation.conservation.ConservationScore
import cz.siret.prank.utils.Futils
import groovy.util.logging.Slf4j

import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Function

/**
 * Loader for predictions produced by some pocket prediction tool
 */
@Slf4j
abstract class PredictionLoader {

    LoaderParams loaderParams = new LoaderParams()

    /**
     *
     * @param ligandedPdbFile path to control pdb file with ligands
     * @param predictionOutputFile main pocket prediction output file (from the second column in the dataset file)
     * @return
     */
    PredictionPair loadPredictionPair(String liganatedPdbFile, String predictionOutputFile,
                                      ProcessedItemContext itemContext) {
        File ligf = new File(liganatedPdbFile)

        PredictionPair res = new PredictionPair()
        res.name = ligf.name
        res.liganatedProtein = Protein.load(liganatedPdbFile, loaderParams)

        if (predictionOutputFile != null) {
            res.prediction = loadPrediction(predictionOutputFile, res.liganatedProtein)
        } else {
            res.prediction = new Prediction(res.liganatedProtein, [])
        }

        Path parentDir = Paths.get(liganatedPdbFile).parent
        if (loaderParams.load_conservation_paths) {
            if (itemContext.datsetColumnValues.get(Dataset.COLUMN_CONSERVATION_FILES_PATTERN) == null) {
                log.info("Setting conservation path. Origin: {}", loaderParams.conservation_origin)
                Function<String, File> conserPathForChain = { String chainId ->
                    parentDir.resolve(ConservationScore.scoreFileForPdbFile(
                            Futils.shortName(liganatedPdbFile), chainId,
                            loaderParams.conservation_origin)).toFile()
                }
                itemContext.auxData.put(ConservationScore.conservationScoreKey, conserPathForChain)
            } else {
                String pattern = itemContext.datsetColumnValues.get(Dataset.COLUMN_CONSERVATION_FILES_PATTERN);
                Function<String, File> conserPathForChain = { String chainId ->
                    parentDir.resolve(pattern.replaceAll("%chainID%", chainId)).toFile()
                }
                itemContext.auxData.put(ConservationScore.conservationScoreKey, conserPathForChain)
            }
            if (loaderParams.load_conservation) {
                res.liganatedProtein.loadConservationScores()
            }
        }
        return res
    }

    /**
     * @param predictionOutputFile main pocket prediction output file
     * @param protein to which this prediction is related. may be null!
     * @return
     */
    abstract Prediction loadPrediction(String predictionOutputFile,
                                       @Nullable Protein liganatedProtein)

    /**
     * used when running 'prank rescore' on a dataset with one column 'predictionOutputFile'
     * @param predictionOutputFile
     * @return
     */
    Prediction loadPredictionWithoutProtein(String predictionOutputFile) {
        loadPrediction(predictionOutputFile, null)
    }

}
