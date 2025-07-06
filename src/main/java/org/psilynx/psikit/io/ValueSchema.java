package org.psilynx.psikit.io;

import java.util.Map;

public class ValueSchema {
    public final String name;
    public final String type;
    public final Map<Integer, String> enumData;
    public final Integer bitfieldWidth;
    public final Integer arrayLength;
    public int[] bitRange;

    public ValueSchema(String name, String type, Map<Integer, String> enumData,
                       Integer bitfieldWidth, Integer arrayLength) {
        this.name = name;
        this.type = type;
        this.enumData = enumData;
        this.bitfieldWidth = bitfieldWidth;
        this.arrayLength = arrayLength;
        this.bitRange = new int[] {0, 0};
    }
}

