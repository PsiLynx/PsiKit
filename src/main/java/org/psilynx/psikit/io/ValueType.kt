package org.psilynx.psikit.io

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet

enum class ValueType(typeName: String, bitLength: Int) {
    Bool("bool", 8),
    Char("char", 8),
    Int8("int8", 8),
    Int16("int16", 16),
    Int32("int32", 32),
    Int64("int64", 64),
    Uint8("uint8", 8),
    Uint16("uint16", 16),
    Uint32("uint32", 32),
    Uint64("uint64", 64),
    Float("float", 32),
    Float32("float32", 32),
    Double("double", 64),
    Float64("float64", 64);

    val typeName: String?
    val bitLength: Int

    init {
        this.typeName = typeName
        this.bitLength = bitLength
    }

    companion object {
        private val lookup: MutableMap<String?, ValueType?> = HashMap<String?, ValueType?>()
        private val bitfieldValid: MutableSet<String?> = HashSet<String?>()

        init {
            for (vt in ValueType.entries) {
                lookup.put(vt.typeName, vt)
            }
            bitfieldValid.addAll(
                mutableListOf<String?>(
                    "bool", "int8", "int16", "int32", "int64",
                    "uint8", "uint16", "uint32", "uint64"
                )
            )
        }

        @JvmStatic
        fun isValid(name: String?): Boolean {
            return lookup.containsKey(name)
        }

        @JvmStatic
        fun isBitfieldValid(name: String?): Boolean {
            return bitfieldValid.contains(name)
        }

        @JvmStatic
        fun getBitLength(name: String?): Int {
            return lookup.get(name)!!.bitLength
        }

        @JvmStatic
        fun decodeValue(bytes: ByteArray, type: String, enumData: MutableMap<Int?, String?>?): Any? {
            val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            buffer.put(bytes.copyOf(8))
            buffer.rewind()

            val value: Any = when (type) {
                "bool" -> (bytes[0].toInt() and 0xFF) > 0
                "char" -> String(bytes, StandardCharsets.UTF_8)
                "int8" -> buffer.get(0)
                "int16" -> buffer.getShort()
                "int32" -> buffer.getInt()
                "int64" -> buffer.getLong()
                "uint8" -> buffer.get(0).toInt() and 0xFF
                "uint16" -> buffer.getShort().toInt() and 0xFFFF
                "uint32" -> buffer.getInt().toLong() and 0xFFFFFFFFL
                "uint64" -> buffer.getLong()
                "float", "float32" -> buffer.getFloat()
                "double", "float64" -> buffer.getDouble()
                else -> throw IllegalArgumentException("Unknown type: " + type)
            }

            if (enumData != null && value is Number && enumData.containsKey(value.toInt())) {
                return enumData.get(value.toInt())
            }
            return value
        }
    }
}