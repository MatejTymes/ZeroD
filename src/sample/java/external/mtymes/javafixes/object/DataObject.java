package external.mtymes.javafixes.object;

import org.apache.commons.lang3.builder.ToStringStyle;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.apache.commons.lang3.builder.ToStringStyle.SIMPLE_STYLE;

// todo: taken from JavaFixes - add dependency instead
public abstract class DataObject {

    @Override
    public boolean equals(Object that) {
        return reflectionEquals(this, that);
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
