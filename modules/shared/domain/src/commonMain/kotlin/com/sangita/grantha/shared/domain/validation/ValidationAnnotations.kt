package com.sangita.grantha.shared.domain.validation

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ValueRange(val min: Long = Long.MIN_VALUE, val max: Long = Long.MAX_VALUE)
