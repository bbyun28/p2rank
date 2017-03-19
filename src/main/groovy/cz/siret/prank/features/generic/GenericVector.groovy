package cz.siret.prank.features.generic

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class GenericVector {

    final GenericHeader header
    final double[] data

    GenericVector(GenericHeader header) {
        this.header = header
        this.data = new double[header.size]
    }

    GenericVector(GenericHeader header, double[] data) {
        this.header = header
        this.data = data
    }

    int getSize() {
        return data.length
    }

    double get(String colName) {
        return data[header.getColIndex(colName)]
    }

    void set(String colName, double value) {
        data[header.getColIndex(colName)] = value
    }

    void multiply(String colName, double a) {
        data[header.getColIndex(colName)] *= a
    }

    void setValues(List<String> valuesHeader, double[] values) {
        String firstColName = valuesHeader[0]
        int start = header.getColIndex(firstColName)
        System.arraycopy(values, 0, data, start, values.length)
    }

    List<Double> toList() {
        return data.toList()
    }

    void addTo(List<Double> list) {
        for (int i=0; i!=data.length; i++) {
            list.add(data[i])
        }
    }

    /**
     * @return new instance
     */
    GenericVector copy() {
        return new GenericVector(header, Arrays.copyOf(data, data.length))     //(double[]) data.clone()
    }

    /**
     * modifies instance
     */
    GenericVector add(GenericVector gv) {
        for (int i=0; i!=data.length; ++i) {
            double toadd = gv.data[i]
            if (!Double.isNaN(toadd)) {
                data[i] += toadd
            }
        }
        return this
    }

    /**
     * modifies instance
     */
    GenericVector multiply(double a) {
        for (int i=0; i!=data.length; ++i) {
            data[i] *= a
        }
        return this
    }

}
