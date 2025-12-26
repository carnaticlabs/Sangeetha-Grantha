package com.sangita.grantha.backend.dal.support

import java.util.UUID
import kotlin.uuid.Uuid

fun UUID.toKotlinUuid(): Uuid = Uuid.parse(toString())

fun Uuid.toJavaUuid(): UUID = UUID.fromString(toString())
