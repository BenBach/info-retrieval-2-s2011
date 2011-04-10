import com.google.common.base.Objects;

/**
* @author patrick
*/
public class DocumentSimilarity implements Comparable<DocumentSimilarity> {
    private double distance;
    private String sourceDocument;
    private String targetDocument;
    private String index;

    public DocumentSimilarity(double distance, String sourceDocument, String targetDocument, String index) {
        this.distance = distance;
        this.sourceDocument = sourceDocument;
        this.targetDocument = targetDocument;
        this.index = index;
    }

    public double getDistance() {
        return distance;
    }

    public String getSourceDocument() {
        return sourceDocument;
    }

    public String getTargetDocument() {
        return targetDocument;
    }

    public String getIndex() {
        return index;
    }

    @Override
    public int compareTo(DocumentSimilarity o) {
        return Double.compare(distance, o.distance);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(distance, sourceDocument, targetDocument, index);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!obj.getClass().equals(getClass())) return false;
        DocumentSimilarity other = (DocumentSimilarity) obj;

        return Objects.equal(distance, other.distance) &&
                Objects.equal(sourceDocument, other.sourceDocument) &&
                Objects.equal(targetDocument, other.targetDocument) &&
                Objects.equal(index, other.index);
    }
}
