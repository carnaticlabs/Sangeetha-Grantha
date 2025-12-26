package com.sangita.grantha.backend.dal.support

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.postgresql.util.PGobject

private class JsonbTextColumnType : ColumnType<String>() {
    override fun sqlType(): String = "jsonb"

    override fun valueFromDB(value: Any): String = when (value) {
        is PGobject -> value.value ?: "null"
        is String -> value
        else -> value.toString()
    }

    override fun notNullValueToDB(value: String): Any = PGobject().apply {
        type = "jsonb"
        this.value = value
    }

    override fun nonNullValueToString(value: String): String =
        "'${value.replace("'", "''")}'::jsonb"
}

fun Table.jsonbText(name: String): Column<String> =
    registerColumn(name, JsonbTextColumnType())
