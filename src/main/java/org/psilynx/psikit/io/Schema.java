package org.psilynx.psikit.io;

import java.util.List;

public class Schema {
    public final int length;
    public final List<ValueSchema> valueSchemas;

    public Schema(int length, List<ValueSchema> valueSchemas) {
        this.length = length;
        this.valueSchemas = valueSchemas;
    }
}

