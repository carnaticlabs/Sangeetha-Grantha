package com.sangita.grantha.backend.dal.support

import com.sangita.grantha.backend.dal.enums.DbEnum
import com.sangita.grantha.backend.dal.enums.enumByDbValue
import kotlin.reflect.KClass
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.postgresql.util.PGobject

class PostgreSqlEnumColumnType<T : Enum<T>>(
    private val enumTypeName: String,
    private val enumClass: KClass<T>,
    private val toDb: (T) -> String,
    private val fromDb: (String) -> T
) : ColumnType<T>() {
    override fun sqlType(): String = enumTypeName

    override fun valueFromDB(value: Any): T = when (value) {
        is PGobject -> fromDb(value.value ?: error("Null value for $enumTypeName"))
        is String -> fromDb(value)
        is Enum<*> -> enumClass.java.enumConstants.first { it.name == value.name }
        else -> error("Unexpected value type ${value::class.qualifiedName} for $enumTypeName")
    }

    override fun notNullValueToDB(value: T): Any = PGobject().apply {
        type = enumTypeName
        this.value = toDb(value)
    }

    override fun nonNullValueToString(value: T): String = "'${toDb(value)}'::$enumTypeName"
}

inline fun <reified T> Table.pgEnum(name: String, enumType: String): Column<T>
    where T : Enum<T>, T : DbEnum =
    registerColumn(name, PostgreSqlEnumColumnType(enumType, T::class, { it.dbValue }, { value -> enumByDbValue(value) }))
