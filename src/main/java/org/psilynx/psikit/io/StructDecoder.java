// Copyright (c) 2021-2025 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package org.psilynx.psikit.io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Class to manage decoding WPILib structs.
 *
 * Specification: https://github.com/wpilibsuite/allwpilib/blob/main/wpiutil/doc/struct.adoc
 */
public class StructDecoder {

    private final Map<String, String> schemaStrings = new HashMap<>();
    private final Map<String, Schema> schemas = new HashMap<>();

    public void addSchema(String name, byte[] schema) {
        String schemaStr = new String(schema, StandardCharsets.UTF_8);
        if (schemaStrings.containsKey(name)) return;
        schemaStrings.put(name, schemaStr);

        while (true) {
            boolean compileSuccess = false;
            for (String schemaName : schemaStrings.keySet()) {
                if (!schemas.containsKey(schemaName)) {
                    boolean success = compileSchema(schemaName, schemaStrings.get(schemaName));
                    compileSuccess |= success;
                }
            }
            if (!compileSuccess) break;
        }
    }

    private boolean compileSchema(String name, String schema) {
        String[] schemaStrs = Arrays.stream(schema.trim().split(";"))
                .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        List<ValueSchema> valueSchemas = new ArrayList<>();

        for (String schemaStr : schemaStrs) {
            Map<Integer, String> enumData = null;

            if (schemaStr.startsWith("enum")) {
                enumData = new HashMap<>();
                int start = schemaStr.indexOf("{") + 1;
                int end = schemaStr.indexOf("}");
                String enumBody = schemaStr.substring(start, end).replaceAll(" ", "");
                for (String pairStr : enumBody.split(",")) {
                    if (pairStr.isEmpty()) continue;
                    String[] pair = pairStr.split("=");
                    if (pair.length == 2 && pair[1].matches("\\d+")) {
                        enumData.put(Integer.parseInt(pair[1]), pair[0]);
                    }
                }
                schemaStr = schemaStr.substring(end + 1).trim();
            }

            String[] parts = schemaStr.trim().split(" ");
            String type = parts[0];
            if (!ValueType.isValid(type) && !schemas.containsKey(type)) return false;

            String nameStr = String.join("", Arrays.copyOfRange(parts, 1, parts.length));
            String fieldName;
            Integer bitfieldWidth = null, arrayLength = null;

            if (nameStr.contains(":")) {
                String[] split = nameStr.split(":");
                fieldName = split[0];
                bitfieldWidth = Integer.parseInt(split[1]);
                if (!ValueType.isBitfieldValid(type)) continue;
                if (type.equals("bool") && bitfieldWidth != 1) continue;
            } else if (nameStr.contains("[")) {
                String[] split = nameStr.split("\\[");
                fieldName = split[0];
                arrayLength = Integer.parseInt(split[1].replace("]", ""));
            } else {
                fieldName = nameStr;
            }

            valueSchemas.add(new ValueSchema(fieldName, type, enumData, bitfieldWidth, arrayLength));
        }

        int bitPosition = 0, bitfieldPos = 0, bitfieldLen = 0;
        boolean inBitfield = false;

        for (ValueSchema valueSchema : valueSchemas) {
            int valueBitLength;

            if (!ValueType.isValid(valueSchema.type)) {
                if (inBitfield) {
                    bitPosition += bitfieldLen - bitfieldPos;
                    inBitfield = false;
                }
                int len = schemas.get(valueSchema.type).length;
                if (valueSchema.arrayLength != null) len *= valueSchema.arrayLength;
                valueSchema.bitRange = new int[]{bitPosition, bitPosition + len};
                bitPosition += len;
            } else if (valueSchema.bitfieldWidth == null) {
                if (inBitfield) {
                    bitPosition += bitfieldLen - bitfieldPos;
                    inBitfield = false;
                }
                valueBitLength = ValueType.getBitLength(valueSchema.type);
                if (valueSchema.arrayLength != null) valueBitLength *= valueSchema.arrayLength;
                valueSchema.bitRange = new int[]{bitPosition, bitPosition + valueBitLength};
                bitPosition += valueBitLength;
            } else {
                int typeLength = ValueType.getBitLength(valueSchema.type);
                valueBitLength = Math.min(valueSchema.bitfieldWidth, typeLength);
                if (!inBitfield || (!valueSchema.type.equals("bool") && bitfieldLen != typeLength)
                        || (bitfieldPos + valueBitLength > bitfieldLen)) {
                    if (inBitfield) {
                        bitPosition += bitfieldLen - bitfieldPos;
                    }
                    bitfieldPos = 0;
                    bitfieldLen = typeLength;
                    inBitfield = true;
                }
                valueSchema.bitRange = new int[]{bitPosition, bitPosition + valueBitLength};
                bitfieldPos += valueBitLength;
                bitPosition += valueBitLength;
            }
        }

        if (inBitfield) bitPosition += bitfieldLen - bitfieldPos;

        schemas.put(name, new Schema(bitPosition, valueSchemas));
        return true;
    }

    public DecodedStruct decode(String name, byte[] value, Integer bitLengthOverride) {
        if (!schemas.containsKey(name)) throw new RuntimeException("Schema not defined");
        int bitLength = (bitLengthOverride != null) ? bitLengthOverride : value.length * 8;
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, String> schemaTypes = new LinkedHashMap<>();
        Schema schema = schemas.get(name);

        for (ValueSchema valueSchema : schema.valueSchemas) {
            BitUtils.BitRangeSlice slice = BitUtils.sliceBits(value, valueSchema.bitRange);
            if (ValueType.isValid(valueSchema.type)) {
                String type = valueSchema.type;
                if (valueSchema.arrayLength == null) {
                    data.put(valueSchema.name, ValueType.decodeValue(slice.value, type, valueSchema.enumData));
                } else {
                    int itemLength = (valueSchema.bitRange[1] - valueSchema.bitRange[0]) / valueSchema.arrayLength;
                    List<Object> arrayData = new ArrayList<>();
                    for (int i = 0; i < valueSchema.arrayLength; i++) {
                        byte[] chunk = BitUtils.sliceBits(slice.value, new int[]{i * itemLength, (i + 1) * itemLength}).value;
                        arrayData.add(ValueType.decodeValue(chunk, type, valueSchema.enumData));
                    }
                    if (type.equals("char")) {
                        StringBuilder str = new StringBuilder();
                        arrayData.forEach(obj -> str.append((char) obj));
                        data.put(valueSchema.name, str.toString());
                    } else {
                        data.put(valueSchema.name, arrayData);
                    }
                }
            } else {
                boolean isArray = valueSchema.arrayLength != null;
                schemaTypes.put(valueSchema.name, valueSchema.type + (isArray ? "[]" : ""));
                DecodedStruct child = isArray
                        ? decodeArray(valueSchema.type, slice.value, valueSchema.arrayLength)
                        : decode(valueSchema.type, slice.value, slice.bitLength);
                data.put(valueSchema.name, child.data);
                for (Map.Entry<String, String> entry : child.schemaTypes.entrySet()) {
                    schemaTypes.put(valueSchema.name + "/" + entry.getKey(), entry.getValue());
                }
            }
        }

        return new DecodedStruct(data, schemaTypes);
    }

    public DecodedStruct decodeArray(String name, byte[] value, Integer arrayLength) {
        if (!schemas.containsKey(name)) throw new RuntimeException("Schema not defined");

        List<Object> data = new ArrayList<>();
        Map<String, String> schemaTypes = new LinkedHashMap<>();
        int length = arrayLength != null ? arrayLength : value.length / (schemas.get(name).length / 8);
        int schemaLengthBytes = schemas.get(name).length / 8;

        for (int i = 0; i < length; i++) {
            byte[] slice = Arrays.copyOfRange(value, i * schemaLengthBytes, (i + 1) * schemaLengthBytes);
            DecodedStruct item = decode(name, slice, null);
            data.add(item.data);
            for (Map.Entry<String, String> entry : item.schemaTypes.entrySet()) {
                schemaTypes.put(i + "/" + entry.getKey(), entry.getValue());
            }
            schemaTypes.put(String.valueOf(i), name);
        }

        return new DecodedStruct(data, schemaTypes);
    }

    public static StructDecoder fromSerialized(Map<String, Object> serializedData) {
        StructDecoder decoder = new StructDecoder();
        decoder.schemaStrings.putAll((Map<String, String>) serializedData.get("schemaStrings"));
        decoder.schemas.putAll((Map<String, Schema>) serializedData.get("schemas"));
        return decoder;
    }

    // Inner classes

    public static class DecodedStruct {
        public final Object data;
        public final Map<String, String> schemaTypes;

        public DecodedStruct(Object data, Map<String, String> schemaTypes) {
            this.data = data;
            this.schemaTypes = schemaTypes;
        }
    }
}
